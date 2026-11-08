"""LibraryRepository adapter on SQLAlchemy 2 + SQLite."""

from __future__ import annotations

import uuid
from datetime import UTC, datetime

from sqlalchemy import (
    DateTime,
    ForeignKey,
    Integer,
    String,
    Text,
    TypeDecorator,
    create_engine,
    delete,
    event,
    select,
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, sessionmaker

from app.domain.models import DownloadJob, JobStatus, Playlist, Song, utcnow


class UtcDateTime(TypeDecorator):
    """Stores naive UTC, returns timezone-aware UTC."""

    impl = DateTime
    cache_ok = True

    def process_bind_param(self, value: datetime | None, dialect):
        if value is None:
            return None
        return value.astimezone(UTC).replace(tzinfo=None)

    def process_result_value(self, value: datetime | None, dialect):
        return None if value is None else value.replace(tzinfo=UTC)


class Base(DeclarativeBase):
    pass


class MetaRow(Base):
    """Key/value facts about this library (e.g. its persistent identity)."""

    __tablename__ = "meta"
    key: Mapped[str] = mapped_column(String(64), primary_key=True)
    value: Mapped[str] = mapped_column(Text)


class SongRow(Base):
    __tablename__ = "songs"
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    source_id: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    title: Mapped[str] = mapped_column(String(200))
    artist: Mapped[str] = mapped_column(String(200))
    duration_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    thumbnail_url: Mapped[str | None] = mapped_column(Text, nullable=True)
    audio_filename: Mapped[str] = mapped_column(String(255))
    synced_lrc: Mapped[str | None] = mapped_column(Text, nullable=True)
    plain_lyrics: Mapped[str | None] = mapped_column(Text, nullable=True)
    lyrics_offset_ms: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(UtcDateTime)


class PlaylistRow(Base):
    __tablename__ = "playlists"
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    name: Mapped[str] = mapped_column(String(80))
    updated_at: Mapped[datetime] = mapped_column(UtcDateTime)


class PlaylistSongRow(Base):
    __tablename__ = "playlist_songs"
    playlist_id: Mapped[str] = mapped_column(
        ForeignKey("playlists.id", ondelete="CASCADE"), primary_key=True
    )
    position: Mapped[int] = mapped_column(Integer, primary_key=True)
    song_id: Mapped[str] = mapped_column(ForeignKey("songs.id", ondelete="CASCADE"), index=True)


class JobRow(Base):
    __tablename__ = "download_jobs"
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    source_id: Mapped[str] = mapped_column(String(64), index=True)
    status: Mapped[str] = mapped_column(String(16))
    song_id: Mapped[str | None] = mapped_column(String(36), nullable=True)
    error: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(UtcDateTime)


def _song(row: SongRow) -> Song:
    return Song(
        id=row.id,
        source_id=row.source_id,
        title=row.title,
        artist=row.artist,
        duration_seconds=row.duration_seconds,
        thumbnail_url=row.thumbnail_url,
        audio_filename=row.audio_filename,
        created_at=row.created_at,
        synced_lrc=row.synced_lrc,
        plain_lyrics=row.plain_lyrics,
        lyrics_offset_ms=row.lyrics_offset_ms,
    )


def _job(row: JobRow) -> DownloadJob:
    return DownloadJob(
        id=row.id,
        source_id=row.source_id,
        status=JobStatus(row.status),
        song_id=row.song_id,
        error=row.error,
        created_at=row.created_at,
    )


class SqlAlchemyRepository:
    def __init__(self, url: str) -> None:
        self._engine = create_engine(url, connect_args={"check_same_thread": False})

        @event.listens_for(self._engine, "connect")
        def _fk_on(dbapi_conn, _):  # pragma: no cover - trivial pragma
            dbapi_conn.execute("PRAGMA foreign_keys=ON")

        Base.metadata.create_all(self._engine)
        self._sessions = sessionmaker(self._engine, expire_on_commit=False)
        self._ensure_library_id()

    # ---- identity ----
    def _ensure_library_id(self) -> None:
        with self._sessions.begin() as s:
            if s.get(MetaRow, "library_id") is None:
                s.add(MetaRow(key="library_id", value=uuid.uuid4().hex))

    def library_id(self) -> str:
        """Random id created with this database; a new/empty database gets a new one."""
        with self._sessions() as s:
            row = s.get(MetaRow, "library_id")
            assert row is not None
            return row.value

    # ---- songs ----
    def add_song(self, song: Song) -> None:
        with self._sessions.begin() as s:
            s.add(SongRow(**self._song_fields(song)))

    def get_song(self, song_id: str) -> Song | None:
        with self._sessions() as s:
            row = s.get(SongRow, song_id)
            return _song(row) if row else None

    def get_song_by_source_id(self, source_id: str) -> Song | None:
        with self._sessions() as s:
            row = s.scalars(select(SongRow).where(SongRow.source_id == source_id)).first()
            return _song(row) if row else None

    def list_songs(self) -> list[Song]:
        with self._sessions() as s:
            rows = s.scalars(select(SongRow).order_by(SongRow.created_at, SongRow.title)).all()
            return [_song(r) for r in rows]

    def update_song(self, song: Song) -> None:
        with self._sessions.begin() as s:
            row = s.get(SongRow, song.id)
            if row is not None:
                for key, value in self._song_fields(song).items():
                    setattr(row, key, value)

    def delete_song(self, song_id: str) -> None:
        with self._sessions.begin() as s:
            affected = s.scalars(
                select(PlaylistSongRow.playlist_id).where(PlaylistSongRow.song_id == song_id)
            ).all()
            for pid in set(affected):
                ids = [x for x in self._playlist_song_ids(s, pid) if x != song_id]
                self._write_playlist_songs(s, pid, ids)
                prow = s.get(PlaylistRow, pid)
                prow.updated_at = utcnow()
            s.execute(delete(SongRow).where(SongRow.id == song_id))

    def source_ids_in_library(self, source_ids: list[str]) -> set[str]:
        if not source_ids:
            return set()
        with self._sessions() as s:
            rows = s.scalars(select(SongRow.source_id).where(SongRow.source_id.in_(source_ids)))
            return set(rows)

    # ---- playlists ----
    def add_playlist(self, playlist: Playlist) -> None:
        with self._sessions.begin() as s:
            s.add(PlaylistRow(id=playlist.id, name=playlist.name, updated_at=playlist.updated_at))
            s.flush()
            self._write_playlist_songs(s, playlist.id, playlist.song_ids)

    def get_playlist(self, playlist_id: str) -> Playlist | None:
        with self._sessions() as s:
            row = s.get(PlaylistRow, playlist_id)
            return self._playlist(s, row) if row else None

    def list_playlists(self) -> list[Playlist]:
        with self._sessions() as s:
            rows = s.scalars(select(PlaylistRow).order_by(PlaylistRow.name)).all()
            return [self._playlist(s, r) for r in rows]

    def update_playlist(self, playlist: Playlist) -> None:
        with self._sessions.begin() as s:
            row = s.get(PlaylistRow, playlist.id)
            if row is None:
                return
            row.name, row.updated_at = playlist.name, playlist.updated_at
            self._write_playlist_songs(s, playlist.id, playlist.song_ids)

    def delete_playlist(self, playlist_id: str) -> None:
        with self._sessions.begin() as s:
            s.execute(delete(PlaylistSongRow).where(PlaylistSongRow.playlist_id == playlist_id))
            s.execute(delete(PlaylistRow).where(PlaylistRow.id == playlist_id))

    # ---- jobs ----
    def add_job(self, job: DownloadJob) -> None:
        with self._sessions.begin() as s:
            s.add(JobRow(**self._job_fields(job)))

    def get_job(self, job_id: str) -> DownloadJob | None:
        with self._sessions() as s:
            row = s.get(JobRow, job_id)
            return _job(row) if row else None

    def get_active_job_for_source(self, source_id: str) -> DownloadJob | None:
        active = [JobStatus.PENDING.value, JobStatus.DOWNLOADING.value]
        with self._sessions() as s:
            row = s.scalars(
                select(JobRow).where(JobRow.source_id == source_id, JobRow.status.in_(active))
            ).first()
            return _job(row) if row else None

    def list_jobs(self) -> list[DownloadJob]:
        with self._sessions() as s:
            rows = s.scalars(select(JobRow).order_by(JobRow.created_at.desc())).all()
            return [_job(r) for r in rows]

    def update_job(self, job: DownloadJob) -> None:
        with self._sessions.begin() as s:
            row = s.get(JobRow, job.id)
            if row is not None:
                for key, value in self._job_fields(job).items():
                    setattr(row, key, value)

    # ---- helpers ----
    @staticmethod
    def _song_fields(song: Song) -> dict:
        return {
            "id": song.id,
            "source_id": song.source_id,
            "title": song.title,
            "artist": song.artist,
            "duration_seconds": song.duration_seconds,
            "thumbnail_url": song.thumbnail_url,
            "audio_filename": song.audio_filename,
            "synced_lrc": song.synced_lrc,
            "plain_lyrics": song.plain_lyrics,
            "lyrics_offset_ms": song.lyrics_offset_ms,
            "created_at": song.created_at,
        }

    @staticmethod
    def _job_fields(job: DownloadJob) -> dict:
        return {
            "id": job.id,
            "source_id": job.source_id,
            "status": job.status.value,
            "song_id": job.song_id,
            "error": job.error,
            "created_at": job.created_at,
        }

    @staticmethod
    def _playlist_song_ids(s, playlist_id: str) -> list[str]:
        return list(
            s.scalars(
                select(PlaylistSongRow.song_id)
                .where(PlaylistSongRow.playlist_id == playlist_id)
                .order_by(PlaylistSongRow.position)
            )
        )

    def _playlist(self, s, row: PlaylistRow) -> Playlist:
        return Playlist(
            id=row.id,
            name=row.name,
            song_ids=self._playlist_song_ids(s, row.id),
            updated_at=row.updated_at,
        )

    @staticmethod
    def _write_playlist_songs(s, playlist_id: str, song_ids: list[str]) -> None:
        s.execute(delete(PlaylistSongRow).where(PlaylistSongRow.playlist_id == playlist_id))
        s.flush()
        for pos, sid in enumerate(song_ids):
            s.add(PlaylistSongRow(playlist_id=playlist_id, position=pos, song_id=sid))
