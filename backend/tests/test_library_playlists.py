from __future__ import annotations

from datetime import UTC, datetime

import pytest

from app.application.library import ManageLibrary
from app.application.playlists import ManagePlaylists
from app.domain.errors import InvalidInputError, NotFoundError
from app.domain.models import Lyrics, Song


def add_song(repo, storage, sid: str) -> Song:
    tmp = storage.root.parent / f"{sid}.m4a"
    tmp.write_bytes(b"audio")
    filename = storage.store(tmp, sid)
    song = Song(
        id=sid,
        source_id=f"src-{sid}",
        title=f"Title {sid}",
        artist="Artist",
        duration_seconds=100,
        thumbnail_url=None,
        audio_filename=filename,
        created_at=datetime.now(UTC),
        synced_lrc="[00:01.00]x",
        plain_lyrics="x",
    )
    repo.add_song(song)
    return song


@pytest.fixture
def lib(repo, storage, lyrics_provider):
    return ManageLibrary(repo, storage, lyrics_provider)


@pytest.fixture
def pls(repo):
    return ManagePlaylists(repo)


def test_update_song_fields(repo, storage, lib):
    add_song(repo, storage, "s1")
    out = lib.update_song("s1", title="New", lyrics_offset_ms=-250)
    assert (out.title, out.artist, out.lyrics_offset_ms) == ("New", "Artist", -250)
    assert repo.get_song("s1").title == "New"


def test_get_unknown_song(lib):
    with pytest.raises(NotFoundError):
        lib.get_song("nope")


def test_refresh_lyrics_overwrites(repo, storage, lib, lyrics_provider):
    add_song(repo, storage, "s1")
    lyrics_provider.lyrics = Lyrics(synced_lrc="[00:02.00]new", plain="new")
    out = lib.refresh_lyrics("s1")
    assert out.synced_lrc == "[00:02.00]new"
    assert repo.get_song("s1").plain_lyrics == "new"


def test_refresh_lyrics_not_found_clears_nothing_new(repo, storage, lib, lyrics_provider):
    add_song(repo, storage, "s1")
    lyrics_provider.lyrics = None
    out = lib.refresh_lyrics("s1")
    assert out.synced_lrc is None and out.plain_lyrics is None


def test_playlist_create_and_validate(repo, storage, pls):
    add_song(repo, storage, "s1")
    p = pls.create("Mornings", ["s1"])
    assert p.song_ids == ["s1"]
    with pytest.raises(InvalidInputError):
        pls.create("Bad", ["ghost"])
    with pytest.raises(InvalidInputError):
        pls.update(p.id, song_ids=["s1", "ghost"])
    with pytest.raises(InvalidInputError):
        pls.create("   ")


def test_playlist_update_rename_and_reorder(repo, storage, pls):
    for s in ("s1", "s2", "s3"):
        add_song(repo, storage, s)
    p = pls.create("L", ["s1", "s2", "s3"])
    p = pls.update(p.id, name="Renamed", song_ids=["s3", "s1"])
    assert (p.name, p.song_ids) == ("Renamed", ["s3", "s1"])


def test_playlist_helpers_add_remove_move(repo, storage, pls):
    for s in ("s1", "s2", "s3"):
        add_song(repo, storage, s)
    p = pls.create("L", ["s1", "s2"])
    assert pls.add_song(p.id, "s3").song_ids == ["s1", "s2", "s3"]
    assert pls.add_song(p.id, "s3").song_ids == ["s1", "s2", "s3"]
    assert pls.move_song(p.id, "s3", "up").song_ids == ["s1", "s3", "s2"]
    assert pls.move_song(p.id, "s1", "up").song_ids == ["s1", "s3", "s2"]
    assert pls.move_song(p.id, "s1", "down").song_ids == ["s3", "s1", "s2"]
    assert pls.remove_song(p.id, "s1").song_ids == ["s3", "s2"]


def test_unknown_playlist(pls):
    with pytest.raises(NotFoundError):
        pls.get_playlist("nope")
    with pytest.raises(NotFoundError):
        pls.delete("nope")


def test_delete_song_cascades_out_of_playlists_and_removes_files(repo, storage, lib, pls):
    for s in ("s1", "s2"):
        add_song(repo, storage, s)
    p = pls.create("L", ["s1", "s2"])
    path = storage.path_of(repo.get_song("s1").audio_filename)
    assert path.exists()
    lib.delete_song("s1")
    assert not path.exists()
    assert pls.get_playlist(p.id).song_ids == ["s2"]
    with pytest.raises(NotFoundError):
        lib.get_song("s1")


def test_repo_roundtrips_timezone_aware_timestamps(repo, storage):
    s = add_song(repo, storage, "s1")
    loaded = repo.get_song("s1")
    assert loaded.created_at.tzinfo is not None
    assert loaded.created_at == s.created_at
