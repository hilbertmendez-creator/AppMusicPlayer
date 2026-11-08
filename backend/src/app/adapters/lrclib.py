"""LyricsProvider adapter for LRCLIB (https://lrclib.net)."""

from __future__ import annotations

import re

import httpx

from app.domain.models import Lyrics

BASE_URL = "https://lrclib.net"
MAX_DURATION_DRIFT_SECONDS = 15


def _to_lyrics(item: dict) -> Lyrics | None:
    synced = (item.get("syncedLyrics") or "").strip() or None
    plain = (item.get("plainLyrics") or "").strip() or None
    if synced is None and plain is None:
        return None
    return Lyrics(synced_lrc=synced, plain=plain)


_LRC_TIMESTAMP = re.compile(r"\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?\]")


def _strip_lrc(synced: str) -> str:
    """Plain text from an LRC body: drop every [mm:ss.xx] tag and the blank lines they leave."""
    lines = (_LRC_TIMESTAMP.sub("", line).strip() for line in synced.splitlines())
    return "\n".join(line for line in lines if line)


class LrclibProvider:
    def __init__(
        self,
        user_agent: str = "MiMusica/0.1 (personal use)",
        client: httpx.Client | None = None,
    ) -> None:
        self._headers = {"User-Agent": user_agent}
        self._client = client or httpx.Client(base_url=BASE_URL, timeout=10.0)

    def get(self, title: str, artist: str, duration_seconds: int | None) -> Lyrics | None:
        params: dict[str, str | int] = {"track_name": title, "artist_name": artist}
        if duration_seconds:
            params["duration"] = int(duration_seconds)
        resp = self._client.get("/api/get", params=params, headers=self._headers)
        if resp.status_code == 404:
            return self._search(title, artist, duration_seconds)
        resp.raise_for_status()
        return _to_lyrics(resp.json())

    def _search(self, title: str, artist: str, duration_seconds: int | None) -> Lyrics | None:
        resp = self._client.get(
            "/api/search",
            params={"track_name": title, "artist_name": artist},
            headers=self._headers,
        )
        resp.raise_for_status()
        candidates = [c for c in resp.json() if _to_lyrics(c) is not None]
        if not candidates:
            return self._search_title_only(title, duration_seconds)
        if duration_seconds:

            def drift(c: dict) -> float:
                return abs((c.get("duration") or 10**6) - duration_seconds)

            best = min(candidates, key=drift)
            if drift(best) > MAX_DURATION_DRIFT_SECONDS:
                return None
        else:
            best = candidates[0]
        return _to_lyrics(best)

    def _search_title_only(self, title: str, duration_seconds: int | None) -> Lyrics | None:
        """Last resort when LRCLIB does not know the artist (typical for covers/compilations).

        Returns plain lyrics only: another recording's timestamps would not line up with this audio.
        Without a known duration versions cannot be told apart, so we give up rather than guess.
        """
        if not duration_seconds:
            return None
        resp = self._client.get("/api/search", params={"track_name": title}, headers=self._headers)
        resp.raise_for_status()
        scored = [
            (abs(c["duration"] - duration_seconds), lyrics)
            for c in resp.json()
            if c.get("duration") and (lyrics := _to_lyrics(c)) is not None
        ]
        if not scored:
            return None
        gap, best = min(scored, key=lambda pair: pair[0])
        if gap > MAX_DURATION_DRIFT_SECONDS:
            return None
        plain = best.plain or (_strip_lrc(best.synced_lrc) if best.synced_lrc else None)
        return Lyrics(synced_lrc=None, plain=plain) if plain else None
