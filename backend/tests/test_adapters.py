from __future__ import annotations

from pathlib import Path

import httpx
import pytest

from app.adapters.fs_storage import FileSystemStorage
from app.adapters.genius import GeniusLyricsSearch
from app.adapters.lrclib import LrclibProvider
from app.adapters.sql_repository import SqlAlchemyRepository
from app.adapters.ytmusic import YtMusicSource
from app.domain.models import TrackHint

UA = "MiMusica/test"


def lrclib(handler) -> LrclibProvider:
    client = httpx.Client(transport=httpx.MockTransport(handler), base_url="https://lrclib.net")
    return LrclibProvider(user_agent=UA, client=client)


def test_lrclib_get_exact_match_sends_params_and_user_agent():
    seen = {}

    def handler(request: httpx.Request) -> httpx.Response:
        seen["url"] = request.url
        seen["ua"] = request.headers["user-agent"]
        return httpx.Response(200, json={"syncedLyrics": "[00:01.00]hi", "plainLyrics": "hi"})

    out = lrclib(handler).get("Song", "Artist", 200)
    assert out.synced_lrc == "[00:01.00]hi" and out.plain == "hi"
    assert seen["url"].path == "/api/get"
    assert dict(seen["url"].params) == {
        "track_name": "Song",
        "artist_name": "Artist",
        "duration": "200",
    }
    assert seen["ua"] == UA


def test_lrclib_falls_back_to_search_and_picks_closest_duration():
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/get":
            return httpx.Response(404, json={"message": "not found"})
        assert request.url.path == "/api/search"
        return httpx.Response(
            200,
            json=[
                {"duration": 100, "syncedLyrics": "far", "plainLyrics": "far"},
                {"duration": 203, "syncedLyrics": None, "plainLyrics": "near"},
                {"duration": 400, "syncedLyrics": "farther", "plainLyrics": "x"},
            ],
        )

    out = lrclib(handler).get("Song", "Artist", 200)
    assert out.synced_lrc is None and out.plain == "near"


def test_lrclib_returns_none_when_nothing_found():
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/get":
            return httpx.Response(404, json={})
        return httpx.Response(200, json=[])

    assert lrclib(handler).get("Song", "Artist", 200) is None


def _title_only_handler(candidates):
    """/api/get 404; artist+title search empty; title-only search returns `candidates`."""
    seen = []

    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/get":
            return httpx.Response(404, json={})
        seen.append(dict(request.url.params))
        if "artist_name" in request.url.params:
            return httpx.Response(200, json=[])
        return httpx.Response(200, json=candidates)

    return handler, seen


def test_lrclib_title_only_fallback_returns_plain_lyrics_without_synced():
    handler, seen = _title_only_handler(
        [
            {"duration": 230, "syncedLyrics": "[00:01.00]otra", "plainLyrics": "letra otra"},
            {"duration": 100, "syncedLyrics": None, "plainLyrics": "lejos"},
        ]
    )
    out = lrclib(handler).get("Cielito Lindo", "Mariachi Mexico Joven", 243)
    assert out is not None
    assert out.synced_lrc is None  # another recording's timestamps would not match
    assert out.plain == "letra otra"
    assert seen[-1] == {"track_name": "Cielito Lindo"}


def test_lrclib_title_only_fallback_rejects_large_duration_drift():
    handler, _ = _title_only_handler([{"duration": 400, "syncedLyrics": None, "plainLyrics": "x"}])
    assert lrclib(handler).get("Cielito Lindo", "Nadie", 243) is None


def test_lrclib_title_only_fallback_needs_known_duration():
    handler, _ = _title_only_handler([{"duration": 230, "syncedLyrics": None, "plainLyrics": "x"}])
    assert lrclib(handler).get("Cielito Lindo", "Nadie", None) is None


def test_lrclib_title_only_fallback_strips_timestamps_from_synced_only_candidate():
    synced = "[00:01.00]uno\n[00:05.50] dos\n[00:09.00]"
    candidate = {"duration": 240, "syncedLyrics": synced, "plainLyrics": None}
    handler, _ = _title_only_handler([candidate])
    out = lrclib(handler).get("Cielito Lindo", "Nadie", 243)
    assert out is not None
    assert out.synced_lrc is None
    assert out.plain == "uno\ndos"


def test_lrclib_instrumental_or_empty_lyrics_is_none():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"syncedLyrics": None, "plainLyrics": None})

    assert lrclib(handler).get("Song", "Artist", 200) is None


def test_lrclib_server_error_raises():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500)

    with pytest.raises(httpx.HTTPStatusError):
        lrclib(handler).get("Song", "Artist", 200)


def test_genius_maps_hits_and_sends_bearer():
    seen = {}

    def handler(request: httpx.Request) -> httpx.Response:
        seen["auth"] = request.headers["authorization"]
        seen["q"] = request.url.params["q"]
        return httpx.Response(
            200,
            json={
                "response": {
                    "hits": [
                        {"result": {"title": "Song A", "primary_artist": {"name": "Art A"}}},
                        {"result": {"title": "Song B", "primary_artist": {"name": "Art B"}}},
                    ]
                }
            },
        )

    client = httpx.Client(transport=httpx.MockTransport(handler), base_url="https://api.genius.com")
    out = GeniusLyricsSearch("tok", client=client).find_by_fragment("some words", 1)
    assert out == [TrackHint("Song A", "Art A")]
    assert seen == {"auth": "Bearer tok", "q": "some words"}


def test_genius_error_status_raises():
    client = httpx.Client(
        transport=httpx.MockTransport(lambda r: httpx.Response(401)),
        base_url="https://api.genius.com",
    )
    with pytest.raises(httpx.HTTPStatusError):
        GeniusLyricsSearch("bad", client=client).find_by_fragment("x", 3)


class StubYtMusic:
    def __init__(self) -> None:
        self.calls: list[tuple] = []

    def search(self, query, filter=None, limit=20):  # noqa: A002
        self.calls.append((query, filter, limit))
        return [
            {
                "resultType": "song",
                "videoId": "abc123",
                "title": "Hotel California",
                "artists": [{"name": "Eagles"}, {"name": "Guest"}],
                "album": {"name": "Hotel California"},
                "duration_seconds": 391,
                "thumbnails": [
                    {"url": "small", "width": 60},
                    {"url": "big", "width": 226},
                ],
            },
            {"resultType": "song", "videoId": None, "title": "skipped"},
            {
                "resultType": "song",
                "videoId": "zzz",
                "title": "No Extras",
                "artists": None,
                "album": None,
                "thumbnails": [],
            },
        ]


def test_ytmusic_search_maps_results():
    stub = StubYtMusic()
    out = YtMusicSource(ytmusic=stub).search("hotel", 5)
    assert stub.calls == [("hotel", "songs", 5)]
    assert len(out) == 2
    first = out[0]
    assert (first.source_id, first.title, first.artist, first.album) == (
        "abc123",
        "Hotel California",
        "Eagles, Guest",
        "Hotel California",
    )
    assert first.duration_seconds == 391 and first.thumbnail_url == "big"
    assert out[1].artist == "Unknown artist" and out[1].thumbnail_url is None


class FakeYDL:
    last_opts: dict = {}
    last_url: str = ""

    def __init__(self, opts):
        FakeYDL.last_opts = opts
        self.opts = opts

    def __enter__(self):
        return self

    def __exit__(self, *a):
        return False

    def extract_info(self, url, download=True):
        FakeYDL.last_url = url
        Path(
            self.opts["outtmpl"].replace("%(id)s", "abc123").replace("%(ext)s", "m4a")
        ).write_bytes(b"x")
        return {
            "id": "abc123",
            "ext": "m4a",
            "title": "Video Title",
            "track": "Real Track",
            "artist": "Real Artist",
            "duration": 200.4,
            "thumbnail": "https://img/t.jpg",
        }

    def prepare_filename(self, info):
        return self.opts["outtmpl"].replace("%(id)s", info["id"]).replace("%(ext)s", info["ext"])


def test_ytmusic_download_prefers_m4a_and_returns_metadata(tmp_path):
    out = YtMusicSource(ytmusic=StubYtMusic(), ydl_factory=FakeYDL).download_audio(
        "abc123", tmp_path
    )
    assert FakeYDL.last_opts["format"] == "bestaudio[ext=m4a]/bestaudio"
    assert FakeYDL.last_opts["noplaylist"] is True
    assert FakeYDL.last_url.endswith("abc123")
    assert out.path == tmp_path / "abc123.m4a" and out.path.exists()
    assert (out.title, out.artist, out.duration_seconds) == ("Real Track", "Real Artist", 200)
    assert out.thumbnail_url == "https://img/t.jpg"


def test_fs_storage_store_path_delete(tmp_path):
    st = FileSystemStorage(tmp_path / "audio")
    src = tmp_path / "in.m4a"
    src.write_bytes(b"abc")
    name = st.store(src, "song-1")
    assert name == "song-1.m4a" and not src.exists()
    assert st.path_of(name).read_bytes() == b"abc"
    st.delete(name)
    st.delete(name)  # idempotent
    assert not st.path_of(name).exists()


def test_fs_storage_rejects_path_traversal(tmp_path):
    st = FileSystemStorage(tmp_path / "audio")
    with pytest.raises(ValueError):
        st.path_of("../secret.txt")


def test_library_id_is_stable_across_reconnects(tmp_path):
    url = f"sqlite:///{tmp_path / 'lib.sqlite3'}"
    first = SqlAlchemyRepository(url).library_id()
    assert len(first) == 32 and int(first, 16) >= 0
    assert SqlAlchemyRepository(url).library_id() == first


def test_library_id_differs_for_a_new_database(tmp_path):
    a = SqlAlchemyRepository(f"sqlite:///{tmp_path / 'a.sqlite3'}").library_id()
    b = SqlAlchemyRepository(f"sqlite:///{tmp_path / 'b.sqlite3'}").library_id()
    assert a != b
