"""Pure domain entities (no framework imports)."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import UTC, datetime
from enum import StrEnum
from pathlib import Path
from typing import Literal

MatchKind = Literal["title", "lyrics"]


class JobStatus(StrEnum):
    PENDING = "pending"
    DOWNLOADING = "downloading"
    READY = "ready"
    FAILED = "failed"


def utcnow() -> datetime:
    return datetime.now(UTC)


@dataclass(frozen=True)
class SearchResult:
    source_id: str
    title: str
    artist: str
    album: str | None = None
    duration_seconds: int | None = None
    thumbnail_url: str | None = None
    match: MatchKind = "title"
    in_library: bool = False


@dataclass(frozen=True)
class TrackHint:
    title: str
    artist: str


@dataclass(frozen=True)
class Lyrics:
    synced_lrc: str | None = None
    plain: str | None = None


@dataclass(frozen=True)
class DownloadedAudio:
    path: Path
    duration_seconds: int | None
    title: str
    artist: str
    thumbnail_url: str | None = None


@dataclass
class Song:
    id: str
    source_id: str
    title: str
    artist: str
    duration_seconds: int | None
    thumbnail_url: str | None
    audio_filename: str
    created_at: datetime = field(default_factory=utcnow)
    synced_lrc: str | None = None
    plain_lyrics: str | None = None
    lyrics_offset_ms: int = 0

    @property
    def has_synced_lyrics(self) -> bool:
        return bool(self.synced_lrc)

    @property
    def has_plain_lyrics(self) -> bool:
        return bool(self.plain_lyrics)


@dataclass
class Playlist:
    id: str
    name: str
    song_ids: list[str] = field(default_factory=list)
    updated_at: datetime = field(default_factory=utcnow)


@dataclass
class DownloadJob:
    id: str
    source_id: str
    status: JobStatus = JobStatus.PENDING
    song_id: str | None = None
    error: str | None = None
    created_at: datetime = field(default_factory=utcnow)
