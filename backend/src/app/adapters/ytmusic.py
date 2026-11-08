"""MusicSource adapter: ytmusicapi for search, yt-dlp for audio download."""

from __future__ import annotations

from collections.abc import Callable
from pathlib import Path
from typing import Any

from app.domain.models import DownloadedAudio, SearchResult

AUDIO_FORMAT = "bestaudio[ext=m4a]/bestaudio"  # m4a avoids needing ffmpeg for conversion
WATCH_URL = "https://music.youtube.com/watch?v={}"


class YtMusicSource:
    def __init__(
        self, ytmusic: Any | None = None, ydl_factory: Callable[[dict], Any] | None = None
    ):
        self._ytmusic = ytmusic
        self._ydl_factory = ydl_factory

    def _client(self) -> Any:
        if self._ytmusic is None:
            from ytmusicapi import YTMusic

            self._ytmusic = YTMusic()
        return self._ytmusic

    def search(self, query: str, limit: int) -> list[SearchResult]:
        raw = self._client().search(query, filter="songs", limit=limit)
        out: list[SearchResult] = []
        for item in raw:
            video_id = item.get("videoId")
            if not video_id:
                continue
            artists = ", ".join(a["name"] for a in item.get("artists") or [] if a.get("name"))
            thumbs = item.get("thumbnails") or []
            best = max(thumbs, key=lambda t: t.get("width") or 0) if thumbs else None
            out.append(
                SearchResult(
                    source_id=video_id,
                    title=item.get("title") or "Unknown title",
                    artist=artists or "Unknown artist",
                    album=(item.get("album") or {}).get("name"),
                    duration_seconds=item.get("duration_seconds"),
                    thumbnail_url=best["url"] if best else None,
                )
            )
        return out[:limit]

    def download_audio(self, source_id: str, dest_dir: Path) -> DownloadedAudio:
        dest_dir.mkdir(parents=True, exist_ok=True)
        opts = {
            "format": AUDIO_FORMAT,
            "outtmpl": str(dest_dir / "%(id)s.%(ext)s"),
            "noplaylist": True,
            "quiet": True,
            "no_warnings": True,
        }
        factory = self._ydl_factory
        if factory is None:
            from yt_dlp import YoutubeDL

            factory = YoutubeDL
        with factory(opts) as ydl:
            info = ydl.extract_info(WATCH_URL.format(source_id), download=True)
            path = Path(ydl.prepare_filename(info))
        if not path.exists():
            matches = sorted(dest_dir.glob(f"{source_id}.*"))
            if not matches:
                raise FileNotFoundError("yt-dlp produced no audio file")
            path = matches[0]
        uploader = (info.get("uploader") or "").removesuffix(" - Topic")
        duration = info.get("duration")
        return DownloadedAudio(
            path=path,
            duration_seconds=int(duration) if duration else None,
            title=info.get("track") or info.get("title") or "Unknown title",
            artist=info.get("artist") or info.get("creator") or uploader or "Unknown artist",
            thumbnail_url=info.get("thumbnail"),
        )
