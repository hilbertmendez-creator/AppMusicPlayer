from __future__ import annotations

import pytest

from tests.conftest import APP_TOKEN, LRC
from tests.fakes import result

API = "/api/v1"
UUID = "00000000-0000-0000-0000-000000000000"

PROTECTED = [
    ("GET", "/search?q=x"),
    ("POST", "/downloads"),
    ("GET", f"/downloads/{UUID}"),
    ("GET", "/library"),
    ("GET", "/songs"),
    ("GET", f"/songs/{UUID}"),
    ("PATCH", f"/songs/{UUID}"),
    ("DELETE", f"/songs/{UUID}"),
    ("GET", f"/songs/{UUID}/audio"),
    ("GET", f"/songs/{UUID}/lyrics"),
    ("POST", f"/songs/{UUID}/lyrics/refresh"),
    ("GET", "/playlists"),
    ("POST", "/playlists"),
    ("PATCH", f"/playlists/{UUID}"),
    ("DELETE", f"/playlists/{UUID}"),
]


def download(client, source_id="vid1", **extra) -> dict:
    job = client.post(f"{API}/downloads", json={"source_id": source_id, **extra})
    assert job.status_code == 202, job.text
    return client.get(f"{API}/downloads/{job.json()['id']}").json()


def make_song(client, source_id="vid1", **extra) -> dict:
    job = download(client, source_id, **extra)
    assert job["status"] == "ready", job
    return client.get(f"{API}/songs/{job['song_id']}").json()


# ---------- auth ----------


def test_health_is_public(anon):
    r = anon.get(f"{API}/health")
    assert r.status_code == 200 and r.json() == {"status": "ok"}


@pytest.mark.parametrize(("method", "path"), PROTECTED)
def test_requires_bearer_token(anon, method, path):
    r = anon.request(method, API + path, json={} if method in ("POST", "PATCH") else None)
    assert r.status_code == 401
    assert isinstance(r.json()["detail"], str)


@pytest.mark.parametrize(
    "header", ["Bearer wrong", "Basic abc", "Bearer", "bearer " + APP_TOKEN + "x"]
)
def test_rejects_bad_credentials(anon, header):
    r = anon.get(f"{API}/songs", headers={"Authorization": header})
    assert r.status_code == 401


# ---------- search ----------


def test_search_shape_and_in_library(client, music):
    music.catalog["hello"] = [
        result("vid1", "Hello", "Adele", album="25", duration_seconds=295, thumbnail_url="u"),
        result("other"),
    ]
    make_song(client, "vid1")
    r = client.get(f"{API}/search", params={"q": "hello", "mode": "title", "limit": 5})
    assert r.status_code == 200
    first, second = r.json()["results"]
    assert first == {
        "source_id": "vid1",
        "title": "Hello",
        "artist": "Adele",
        "album": "25",
        "duration_seconds": 295,
        "thumbnail_url": "u",
        "match": "title",
        "in_library": True,
    }
    assert second["in_library"] is False and second["album"] is None


def test_search_defaults_to_auto_mode_limit_5(client, music):
    music.catalog["x"] = [result(f"v{i}") for i in range(9)]
    r = client.get(f"{API}/search", params={"q": "x"})
    assert len(r.json()["results"]) == 5


@pytest.mark.parametrize(
    "params",
    [
        {"q": ""},
        {"q": "a" * 201},
        {"q": "ok", "limit": 0},
        {"q": "ok", "limit": 21},
        {"q": "ok", "mode": "bogus"},
        {},
    ],
)
def test_search_validation_422(client, params):
    r = client.get(f"{API}/search", params=params)
    assert r.status_code == 422
    assert isinstance(r.json()["detail"], str)


# ---------- downloads ----------


def test_download_flow(client):
    r = client.post(f"{API}/downloads", json={"source_id": "vid1", "title": "T", "artist": "A"})
    assert r.status_code == 202
    body = r.json()
    assert set(body) == {"id", "source_id", "status", "song_id", "error", "created_at"}
    assert body["status"] == "pending" and body["song_id"] is None
    assert body["created_at"].endswith(("Z", "+00:00"))
    done = client.get(f"{API}/downloads/{body['id']}").json()
    assert done["status"] == "ready" and done["song_id"]
    song = client.get(f"{API}/songs/{done['song_id']}").json()
    assert (song["title"], song["artist"]) == ("T", "A")


def test_download_failure_reported(client, music):
    music.fail_download = RuntimeError("nope")
    body = client.post(f"{API}/downloads", json={"source_id": "vid1"}).json()
    job = client.get(f"{API}/downloads/{body['id']}").json()
    assert job["status"] == "failed" and job["error"] and job["song_id"] is None


def test_download_idempotent(client, music):
    first = make_song(client, "vid1")
    again = client.post(f"{API}/downloads", json={"source_id": "vid1"}).json()
    assert again["status"] == "ready" and again["song_id"] == first["id"]
    assert music.download_calls == ["vid1"]


def test_unknown_job_404(client):
    r = client.get(f"{API}/downloads/{UUID}")
    assert r.status_code == 404 and isinstance(r.json()["detail"], str)


@pytest.mark.parametrize(
    "body",
    [
        {},
        {"source_id": ""},
        {"source_id": "has space"},
        {"source_id": "x" * 65},
        {"source_id": "ok", "title": ""},
    ],
)
def test_download_validation_422(client, body):
    assert client.post(f"{API}/downloads", json=body).status_code == 422


# ---------- songs ----------


def test_song_shape_and_listing(client):
    song = make_song(client)
    assert set(song) == {
        "id",
        "source_id",
        "title",
        "artist",
        "duration_seconds",
        "thumbnail_url",
        "has_synced_lyrics",
        "has_plain_lyrics",
        "lyrics_offset_ms",
        "created_at",
    }
    assert song["has_synced_lyrics"] is True and song["lyrics_offset_ms"] == 0
    assert client.get(f"{API}/songs").json() == {"songs": [song]}


def test_song_without_lyrics(client, lyrics_provider):
    lyrics_provider.lyrics = None
    song = make_song(client)
    assert song["has_synced_lyrics"] is False and song["has_plain_lyrics"] is False
    lyr = client.get(f"{API}/songs/{song['id']}/lyrics").json()
    assert lyr == {"synced_lrc": None, "plain": None, "offset_ms": 0}


def test_patch_song(client):
    song = make_song(client)
    r = client.patch(f"{API}/songs/{song['id']}", json={"title": "New", "lyrics_offset_ms": 300})
    assert r.status_code == 200
    assert (r.json()["title"], r.json()["lyrics_offset_ms"]) == ("New", 300)
    assert client.get(f"{API}/songs/{song['id']}/lyrics").json()["offset_ms"] == 300


@pytest.mark.parametrize(
    "body",
    [
        {"title": ""},
        {"artist": "x" * 201},
        {"lyrics_offset_ms": "abc"},
        {"lyrics_offset_ms": 10**9},
    ],
)
def test_patch_song_validation(client, body):
    song = make_song(client)
    assert client.patch(f"{API}/songs/{song['id']}", json=body).status_code == 422


def test_unknown_song_404s(client):
    for method, path in [
        ("GET", f"/songs/{UUID}"),
        ("PATCH", f"/songs/{UUID}"),
        ("DELETE", f"/songs/{UUID}"),
        ("GET", f"/songs/{UUID}/audio"),
        ("GET", f"/songs/{UUID}/lyrics"),
        ("POST", f"/songs/{UUID}/lyrics/refresh"),
    ]:
        r = client.request(method, API + path, json={} if method == "PATCH" else None)
        assert r.status_code == 404, (method, path)


def test_lyrics_endpoint_and_refresh(client, lyrics_provider):
    from app.domain.models import Lyrics

    song = make_song(client)
    assert client.get(f"{API}/songs/{song['id']}/lyrics").json() == {
        "synced_lrc": LRC,
        "plain": "Hello\nWorld",
        "offset_ms": 0,
    }
    lyrics_provider.lyrics = Lyrics(synced_lrc="[00:09.00]fresh", plain="fresh")
    r = client.post(f"{API}/songs/{song['id']}/lyrics/refresh")
    assert r.status_code == 200
    assert r.json() == {"synced_lrc": "[00:09.00]fresh", "plain": "fresh", "offset_ms": 0}


def test_refresh_provider_failure_is_502(client, lyrics_provider):
    song = make_song(client)
    lyrics_provider.error = RuntimeError("down")
    r = client.post(f"{API}/songs/{song['id']}/lyrics/refresh")
    assert r.status_code == 502 and isinstance(r.json()["detail"], str)


def test_audio_serves_bytes_with_media_type(client, music):
    song = make_song(client)
    r = client.get(f"{API}/songs/{song['id']}/audio")
    assert r.status_code == 200
    assert r.headers["content-type"].startswith("audio/mp4")
    assert r.content == music.audio_bytes


def test_audio_supports_range(client, music):
    song = make_song(client)
    r = client.get(f"{API}/songs/{song['id']}/audio", headers={"Range": "bytes=2-9"})
    assert r.status_code == 206
    assert r.content == music.audio_bytes[2:10]
    assert r.headers["content-range"] == f"bytes 2-9/{len(music.audio_bytes)}"
    assert r.headers["accept-ranges"] == "bytes"


def test_delete_song_removes_from_playlists_and_files(client, settings):
    a, b = make_song(client, "vid1"), make_song(client, "vid2")
    pl = client.post(f"{API}/playlists", json={"name": "L", "song_ids": [a["id"], b["id"]]}).json()
    files_before = list((settings.data_dir / "audio").iterdir())
    assert len(files_before) == 2
    assert client.delete(f"{API}/songs/{a['id']}").status_code == 204
    assert client.get(f"{API}/songs/{a['id']}").status_code == 404
    assert len(list((settings.data_dir / "audio").iterdir())) == 1
    pls = client.get(f"{API}/playlists").json()["playlists"]
    assert [p["song_ids"] for p in pls if p["id"] == pl["id"]] == [[b["id"]]]


# ---------- playlists ----------


def test_playlist_crud(client):
    a, b = make_song(client, "vid1"), make_song(client, "vid2")
    r = client.post(f"{API}/playlists", json={"name": "Morning"})
    assert r.status_code == 201
    pl = r.json()
    assert set(pl) == {"id", "name", "song_ids", "updated_at"} and pl["song_ids"] == []
    r = client.patch(
        f"{API}/playlists/{pl['id']}", json={"name": "Evening", "song_ids": [b["id"], a["id"]]}
    )
    assert r.status_code == 200
    assert (r.json()["name"], r.json()["song_ids"]) == ("Evening", [b["id"], a["id"]])
    assert client.get(f"{API}/playlists").json()["playlists"][0]["name"] == "Evening"
    assert client.delete(f"{API}/playlists/{pl['id']}").status_code == 204
    assert client.get(f"{API}/playlists").json() == {"playlists": []}


def test_playlist_unknown_song_ids_422(client):
    assert (
        client.post(f"{API}/playlists", json={"name": "x", "song_ids": [UUID]}).status_code == 422
    )
    pl = client.post(f"{API}/playlists", json={"name": "x"}).json()
    r = client.patch(f"{API}/playlists/{pl['id']}", json={"song_ids": [UUID]})
    assert r.status_code == 422 and isinstance(r.json()["detail"], str)


@pytest.mark.parametrize("body", [{}, {"name": ""}, {"name": "   "}, {"name": "x" * 81}])
def test_playlist_name_validation(client, body):
    assert client.post(f"{API}/playlists", json=body).status_code == 422


def test_playlist_404s(client):
    assert client.patch(f"{API}/playlists/{UUID}", json={"name": "x"}).status_code == 404
    assert client.delete(f"{API}/playlists/{UUID}").status_code == 404


def test_library_identity_endpoint(client):
    r = client.get(f"{API}/library")
    assert r.status_code == 200
    body = r.json()
    assert set(body) == {"library_id"}
    assert body["library_id"] == client.get(f"{API}/library").json()["library_id"]
    assert body["library_id"] == client.app.state.container.repo.library_id()


def test_library_identity_requires_token(anon):
    assert anon.get(f"{API}/library").status_code == 401
