"""Pydantic request/response schemas for the REST API (snake_case JSON)."""

from __future__ import annotations

from datetime import datetime
from typing import Annotated, Literal

from pydantic import BaseModel, Field, StringConstraints

from app.domain import models as m

SourceId = Annotated[str, StringConstraints(pattern=r"^[A-Za-z0-9_-]{1,64}$")]
Text200 = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=200)]
PlaylistName = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=80)]
OffsetMs = Annotated[int, Field(ge=-600_000, le=600_000)]


class ErrorResponse(BaseModel):
    detail: str = Field(description="Human-readable error message.", examples=["Song not found"])


class HealthResponse(BaseModel):
    status: Literal["ok"] = Field(description="Always `ok` when the server is up.", examples=["ok"])


class LibraryIdentity(BaseModel):
    library_id: str = Field(
        description=(
            "Random id generated when this server's database was created. It never changes for "
            "that database, and a new or empty database gets a different one. Clients compare it "
            "with the last id they saw to detect they are talking to a different library."
        ),
        examples=["3f2b8c1e9d4a4b6f8a7c5d2e1f0a9b8c"],
    )


class SearchResult(BaseModel):
    source_id: str = Field(
        description="Opaque id of the track at the music source.", examples=["fJ9rUzIMcZQ"]
    )
    title: str = Field(description="Track title.", examples=["Bohemian Rhapsody"])
    artist: str = Field(description="Artist name(s).", examples=["Queen"])
    album: str | None = Field(
        description="Album name, if known.", examples=["A Night at the Opera"]
    )
    duration_seconds: int | None = Field(description="Track length in seconds.", examples=[355])
    thumbnail_url: str | None = Field(
        description="Cover image URL.", examples=["https://img.example/c.jpg"]
    )
    match: Literal["title", "lyrics"] = Field(
        description="How this result was found: by title/artist or by a lyrics fragment.",
        examples=["title"],
    )
    in_library: bool = Field(
        description="True when the song is already downloaded.", examples=[False]
    )

    @classmethod
    def of(cls, r: m.SearchResult) -> SearchResult:
        return cls(**r.__dict__)


class SearchResponse(BaseModel):
    results: list[SearchResult] = Field(description="At most `limit` results, best first.")


class DownloadRequest(BaseModel):
    source_id: SourceId = Field(
        description="`source_id` from a search result.", examples=["fJ9rUzIMcZQ"]
    )
    title: Text200 | None = Field(
        default=None,
        description="Optional clean title (overrides the downloaded metadata).",
        examples=["Bohemian Rhapsody"],
    )
    artist: Text200 | None = Field(
        default=None,
        description="Optional clean artist (overrides the downloaded metadata).",
        examples=["Queen"],
    )


class DownloadJob(BaseModel):
    id: str = Field(description="Job id (UUID).", examples=["3f2b8c1e-9d4a-4b7e-8a51-0c2f6d9e1a77"])
    source_id: str = Field(description="Track being downloaded.", examples=["fJ9rUzIMcZQ"])
    status: Literal["pending", "downloading", "ready", "failed"] = Field(
        description="Job state. Poll until `ready` or `failed`.", examples=["pending"]
    )
    song_id: str | None = Field(description="Library song id once `ready`.", examples=[None])
    error: str | None = Field(description="Readable reason when `failed`.", examples=[None])
    created_at: datetime = Field(
        description="Creation time (ISO-8601 UTC).", examples=["2026-11-07T18:30:00Z"]
    )

    @classmethod
    def of(cls, j: m.DownloadJob) -> DownloadJob:
        return cls(
            id=j.id,
            source_id=j.source_id,
            status=j.status.value,
            song_id=j.song_id,
            error=j.error,
            created_at=j.created_at,
        )


class Song(BaseModel):
    id: str = Field(
        description="Song id (UUID).", examples=["7b0e5f1a-2c3d-4e5f-9a8b-1c2d3e4f5a6b"]
    )
    source_id: str = Field(description="Id at the music source.", examples=["fJ9rUzIMcZQ"])
    title: str = Field(description="Song title.", examples=["Bohemian Rhapsody"])
    artist: str = Field(description="Artist name(s).", examples=["Queen"])
    duration_seconds: int | None = Field(description="Length in seconds.", examples=[355])
    thumbnail_url: str | None = Field(
        description="Cover image URL.", examples=["https://img.example/c.jpg"]
    )
    has_synced_lyrics: bool = Field(
        description="True if time-synced (LRC) lyrics exist.", examples=[True]
    )
    has_plain_lyrics: bool = Field(description="True if plain lyrics exist.", examples=[True])
    lyrics_offset_ms: int = Field(
        description=(
            "Shift applied to the synced lyrics, LRC convention: positive shows lines "
            "earlier, negative shows them later."
        ),
        examples=[0],
    )
    created_at: datetime = Field(
        description="When the song was added (ISO-8601 UTC).", examples=["2026-11-07T18:31:12Z"]
    )

    @classmethod
    def of(cls, s: m.Song) -> Song:
        return cls(
            id=s.id,
            source_id=s.source_id,
            title=s.title,
            artist=s.artist,
            duration_seconds=s.duration_seconds,
            thumbnail_url=s.thumbnail_url,
            has_synced_lyrics=s.has_synced_lyrics,
            has_plain_lyrics=s.has_plain_lyrics,
            lyrics_offset_ms=s.lyrics_offset_ms,
            created_at=s.created_at,
        )


class SongList(BaseModel):
    songs: list[Song] = Field(description="All songs in the library.")


class SongUpdate(BaseModel):
    title: Text200 | None = Field(
        default=None, description="New title.", examples=["Bohemian Rhapsody"]
    )
    artist: Text200 | None = Field(default=None, description="New artist.", examples=["Queen"])
    lyrics_offset_ms: OffsetMs | None = Field(
        default=None,
        description=("New lyrics offset in ms (-600000..600000); positive shows lines earlier."),
        examples=[250],
    )


class LyricsResponse(BaseModel):
    synced_lrc: str | None = Field(
        description="Time-synced lyrics in LRC format.",
        examples=["[00:12.30]Is this the real life?"],
    )
    plain: str | None = Field(description="Plain lyrics.", examples=["Is this the real life?"])
    offset_ms: int = Field(
        description="Song's lyrics offset in ms; positive shows lines earlier.", examples=[0]
    )

    @classmethod
    def of(cls, s: m.Song) -> LyricsResponse:
        return cls(synced_lrc=s.synced_lrc, plain=s.plain_lyrics, offset_ms=s.lyrics_offset_ms)


class Playlist(BaseModel):
    id: str = Field(
        description="Playlist id (UUID).", examples=["0a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d"]
    )
    name: str = Field(description="Playlist name.", examples=["Morning"])
    song_ids: list[str] = Field(
        description="Ordered song ids.", examples=[["7b0e5f1a-2c3d-4e5f-9a8b-1c2d3e4f5a6b"]]
    )
    updated_at: datetime = Field(
        description="Last change (ISO-8601 UTC).", examples=["2026-11-07T18:40:00Z"]
    )

    @classmethod
    def of(cls, p: m.Playlist) -> Playlist:
        return cls(id=p.id, name=p.name, song_ids=p.song_ids, updated_at=p.updated_at)


class PlaylistList(BaseModel):
    playlists: list[Playlist] = Field(description="All playlists.")


class PlaylistCreate(BaseModel):
    name: PlaylistName = Field(description="Playlist name (1-80 chars).", examples=["Morning"])
    song_ids: list[str] = Field(
        default_factory=list,
        max_length=5000,
        description="Initial ordered song ids.",
        examples=[["7b0e5f1a-2c3d-4e5f-9a8b-1c2d3e4f5a6b"]],
    )


class PlaylistUpdate(BaseModel):
    name: PlaylistName | None = Field(
        default=None, description="New name (1-80 chars).", examples=["Evening"]
    )
    song_ids: list[str] | None = Field(
        default=None,
        max_length=5000,
        description="Replaces the ordered song ids.",
        examples=[["7b0e5f1a-2c3d-4e5f-9a8b-1c2d3e4f5a6b"]],
    )
