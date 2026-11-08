"""In-memory fakes for the outbound ports (no network)."""

from __future__ import annotations

from pathlib import Path

from app.domain.models import DownloadedAudio, Lyrics, SearchResult, TrackHint


def result(source_id: str, title: str = "T", artist: str = "A", **kw) -> SearchResult:
    return SearchResult(source_id=source_id, title=title, artist=artist, **kw)


class FakeMusicSource:
    def __init__(self) -> None:
        self.catalog: dict[str, list[SearchResult]] = {}
        self.search_calls: list[tuple[str, int]] = []
        self.download_calls: list[str] = []
        self.fail_download: Exception | None = None
        self.audio_bytes = b"FAKE-M4A-" + bytes(range(256)) * 4
        self.duration = 215

    def search(self, query: str, limit: int) -> list[SearchResult]:
        self.search_calls.append((query, limit))
        return list(self.catalog.get(query, []))[:limit]

    def download_audio(self, source_id: str, dest_dir: Path) -> DownloadedAudio:
        self.download_calls.append(source_id)
        if self.fail_download:
            raise self.fail_download
        dest_dir.mkdir(parents=True, exist_ok=True)
        path = dest_dir / f"{source_id}.m4a"
        path.write_bytes(self.audio_bytes)
        return DownloadedAudio(
            path=path,
            duration_seconds=self.duration,
            title=f"yt title {source_id}",
            artist=f"yt artist {source_id}",
            thumbnail_url="https://img.example/t.jpg",
        )


class FakeLyricsProvider:
    def __init__(self, lyrics: Lyrics | None = None) -> None:
        self.lyrics = lyrics
        self.error: Exception | None = None
        self.calls: list[tuple[str, str, int | None]] = []

    def get(self, title: str, artist: str, duration_seconds: int | None) -> Lyrics | None:
        self.calls.append((title, artist, duration_seconds))
        if self.error:
            raise self.error
        return self.lyrics


class FakeLyricsSearch:
    def __init__(self, hints: list[TrackHint] | None = None) -> None:
        self.hints = hints or []
        self.error: Exception | None = None
        self.calls: list[tuple[str, int]] = []

    def find_by_fragment(self, fragment: str, limit: int) -> list[TrackHint]:
        self.calls.append((fragment, limit))
        if self.error:
            raise self.error
        return self.hints[:limit]
