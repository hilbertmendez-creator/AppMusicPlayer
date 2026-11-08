from __future__ import annotations

import pytest

from app.application.downloads import RequestDownload
from app.domain.models import JobStatus, Lyrics


@pytest.fixture
def uc(music, lyrics_provider, repo, storage, tmp_path):
    return RequestDownload(music, lyrics_provider, repo, storage, tmp_path / "staging")


def test_submit_creates_pending_job(uc, repo):
    job = uc.submit("vid1")
    assert job.status is JobStatus.PENDING
    assert job.song_id is None
    assert repo.get_job(job.id) == job


def test_happy_path_persists_song_with_lyrics_and_audio(uc, repo, storage, lyrics_provider):
    job = uc.submit("vid1")
    uc.run(job.id, "Nice Song", "Nice Artist")
    done = repo.get_job(job.id)
    assert done.status is JobStatus.READY
    assert done.error is None
    song = repo.get_song(done.song_id)
    assert (song.title, song.artist, song.source_id) == ("Nice Song", "Nice Artist", "vid1")
    assert song.duration_seconds == 215
    assert song.has_synced_lyrics and song.has_plain_lyrics
    assert storage.path_of(song.audio_filename).read_bytes().startswith(b"FAKE-M4A-")
    assert lyrics_provider.calls == [("Nice Song", "Nice Artist", 215)]


def test_metadata_falls_back_to_downloaded_audio(uc, repo):
    job = uc.submit("vid1")
    uc.run(job.id)
    song = repo.get_song(repo.get_job(job.id).song_id)
    assert song.title == "yt title vid1"
    assert song.artist == "yt artist vid1"


def test_download_failure_marks_job_failed_with_readable_error(uc, repo, music):
    music.fail_download = RuntimeError("boom: HTTP 403")
    job = uc.submit("vid1")
    uc.run(job.id)
    failed = repo.get_job(job.id)
    assert failed.status is JobStatus.FAILED
    assert failed.song_id is None
    assert failed.error and "boom" not in failed.error
    assert repo.list_songs() == []


def test_missing_lyrics_still_saves_song(uc, repo, lyrics_provider):
    lyrics_provider.lyrics = None
    job = uc.submit("vid1")
    uc.run(job.id)
    song = repo.get_song(repo.get_job(job.id).song_id)
    assert song.has_synced_lyrics is False
    assert song.has_plain_lyrics is False


def test_lyrics_provider_error_still_saves_song(uc, repo, lyrics_provider):
    lyrics_provider.error = RuntimeError("lrclib down")
    job = uc.submit("vid1")
    uc.run(job.id)
    assert repo.get_job(job.id).status is JobStatus.READY


def test_plain_only_lyrics(uc, repo, lyrics_provider):
    lyrics_provider.lyrics = Lyrics(synced_lrc=None, plain="la la")
    job = uc.submit("vid1")
    uc.run(job.id)
    song = repo.get_song(repo.get_job(job.id).song_id)
    assert (song.has_synced_lyrics, song.has_plain_lyrics) == (False, True)


def test_idempotent_when_already_in_library(uc, repo, music):
    first = uc.submit("vid1")
    uc.run(first.id)
    song_id = repo.get_job(first.id).song_id
    again = uc.submit("vid1")
    assert again.status is JobStatus.READY
    assert again.song_id == song_id
    assert again.id != first.id
    assert repo.get_job(again.id).song_id == song_id
    assert music.download_calls == ["vid1"]
    assert len(repo.list_songs()) == 1


def test_inflight_job_is_reused(uc):
    first = uc.submit("vid1")
    second = uc.submit("vid1")
    assert second.id == first.id


def test_staging_dir_is_cleaned(uc, tmp_path):
    job = uc.submit("vid1")
    uc.run(job.id)
    staging = tmp_path / "staging"
    assert not staging.exists() or not any(staging.iterdir())
