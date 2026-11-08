"""RequestDownload use case: create a job, then download audio + lyrics in the background."""

from __future__ import annotations

import logging
import shutil
import threading
import uuid
from pathlib import Path

from app.domain.errors import NotFoundError
from app.domain.models import DownloadJob, JobStatus, Lyrics, Song, utcnow
from app.ports import AudioStorage, LibraryRepository, LyricsProvider, MusicSource

log = logging.getLogger(__name__)

FRIENDLY_ERROR = (
    "The audio could not be downloaded. The source may be unavailable; try again later."
)


class RequestDownload:
    def __init__(
        self,
        music: MusicSource,
        lyrics: LyricsProvider,
        library: LibraryRepository,
        storage: AudioStorage,
        staging_dir: Path,
    ) -> None:
        self._music = music
        self._lyrics = lyrics
        self._library = library
        self._storage = storage
        self._staging = staging_dir
        self._claim_lock = threading.Lock()

    def submit(
        self, source_id: str, title: str | None = None, artist: str | None = None
    ) -> DownloadJob:
        """Create (or reuse) a job. Never blocks on the actual download."""
        existing = self._library.get_song_by_source_id(source_id)
        if existing is not None:
            job = DownloadJob(
                id=str(uuid.uuid4()),
                source_id=source_id,
                status=JobStatus.READY,
                song_id=existing.id,
            )
            self._library.add_job(job)
            return job
        active = self._library.get_active_job_for_source(source_id)
        if active is not None:
            return active
        job = DownloadJob(id=str(uuid.uuid4()), source_id=source_id)
        self._library.add_job(job)
        return job

    def get_job(self, job_id: str) -> DownloadJob:
        job = self._library.get_job(job_id)
        if job is None:
            raise NotFoundError("Download job not found")
        return job

    def run(self, job_id: str, title: str | None = None, artist: str | None = None) -> None:
        with self._claim_lock:  # make sure a job is processed by one worker only
            job = self._library.get_job(job_id)
            if job is None or job.status is not JobStatus.PENDING:
                return
            job.status = JobStatus.DOWNLOADING
            self._library.update_job(job)
        work_dir = self._staging / job.id
        try:
            audio = self._music.download_audio(job.source_id, work_dir)
            song_id = str(uuid.uuid4())
            final_title = title or audio.title
            final_artist = artist or audio.artist
            lyrics = self._fetch_lyrics(final_title, final_artist, audio.duration_seconds)
            filename = self._storage.store(audio.path, song_id)
            try:
                self._library.add_song(
                    Song(
                        id=song_id,
                        source_id=job.source_id,
                        title=final_title,
                        artist=final_artist,
                        duration_seconds=audio.duration_seconds,
                        thumbnail_url=audio.thumbnail_url,
                        audio_filename=filename,
                        created_at=utcnow(),
                        synced_lrc=lyrics.synced_lrc if lyrics else None,
                        plain_lyrics=lyrics.plain if lyrics else None,
                    )
                )
            except Exception:
                self._storage.delete(filename)
                raise
            job.status, job.song_id, job.error = JobStatus.READY, song_id, None
        except Exception:
            log.exception("Download failed for %s", job.source_id)
            job.status, job.error = JobStatus.FAILED, FRIENDLY_ERROR
        finally:
            shutil.rmtree(work_dir, ignore_errors=True)
        self._library.update_job(job)

    def _fetch_lyrics(self, title: str, artist: str, duration: int | None) -> Lyrics | None:
        try:
            return self._lyrics.get(title, artist, duration)
        except Exception:
            log.warning("Lyrics lookup failed; saving song without lyrics", exc_info=True)
            return None
