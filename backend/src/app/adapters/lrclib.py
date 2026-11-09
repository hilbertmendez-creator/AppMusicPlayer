"""LyricsProvider adapter for LRCLIB (https://lrclib.net)."""

from __future__ import annotations

import re

import httpx

from app.domain.models import Lyrics

BASE_URL = "https://lrclib.net"
MAX_DURATION_DRIFT_SECONDS = 15
# Timestamps only line up with the audio when it is (nearly) the same recording. A version that is
# several seconds longer/shorter has a different intro or tempo, so its LRC drifts out of sync.
MAX_SYNC_DRIFT_SECONDS = 3


def _to_lyrics(item: dict) -> Lyrics | None:
    synced = (item.get("syncedLyrics") or "").strip() or None
    plain = (item.get("plainLyrics") or "").strip() or None
    if synced is None and plain is None:
        return None
    return Lyrics(synced_lrc=synced, plain=plain)


_LRC_TIMESTAMP = re.compile(r"\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?\]")
_LRC_SECONDS = re.compile(r"\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]")


def _last_timestamp(synced: str) -> float:
    """Time of the last line of an LRC body, in seconds (0 when it has no timestamps)."""
    last = 0.0
    for m in _LRC_SECONDS.finditer(synced):
        fraction = float(f"0.{m.group(3)}") if m.group(3) else 0.0
        last = max(last, int(m.group(1)) * 60 + int(m.group(2)) + fraction)
    return last


def _fits(synced: str, duration_seconds: int) -> bool:
    """False when the lyrics have lines after the audio ends: they were timed on a longer recording.

    LRCLIB entries often carry the timestamps of another version while declaring the duration of
    this one, so the declared duration alone cannot be trusted.
    """
    return _last_timestamp(synced) <= duration_seconds + MAX_SYNC_DRIFT_SECONDS


def _strip_lrc(synced: str) -> str:
    """Plain text from an LRC body: drop every [mm:ss.xx] tag and the blank lines they leave."""
    lines = (_LRC_TIMESTAMP.sub("", line).strip() for line in synced.splitlines())
    return "\n".join(line for line in lines if line)


def _plain_only(lyrics: Lyrics) -> Lyrics | None:
    """Same words without timestamps (used when the recording is not the same as our audio)."""
    plain = lyrics.plain or (_strip_lrc(lyrics.synced_lrc) if lyrics.synced_lrc else None)
    return Lyrics(synced_lrc=None, plain=plain) if plain else None


def _drift(item: dict, duration_seconds: int | None) -> float:
    if not duration_seconds:
        return float("inf")
    return abs((item.get("duration") or 10**6) - duration_seconds)


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
        item = resp.json()
        lyrics = _to_lyrics(item)
        if lyrics is None or not lyrics.synced_lrc or not duration_seconds:
            return lyrics
        # /api/get already matches on duration; re-check the length only when LRCLIB tells us.
        too_far = item.get("duration") and _drift(item, duration_seconds) > MAX_SYNC_DRIFT_SECONDS
        if too_far or not _fits(lyrics.synced_lrc, duration_seconds):
            return _plain_only(lyrics)
        return lyrics

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
        if not duration_seconds:
            return _to_lyrics(candidates[0])

        # Synced lyrics only from a recording of (almost) the same length as our audio.
        in_sync = [
            c
            for c in candidates
            if c.get("syncedLyrics")
            and _drift(c, duration_seconds) <= MAX_SYNC_DRIFT_SECONDS
            and _fits(c["syncedLyrics"], duration_seconds)
        ]
        if in_sync:
            return _to_lyrics(min(in_sync, key=lambda c: _drift(c, duration_seconds)))

        # Otherwise the words are still useful (plain lyrics scroll along with the song).
        best = min(candidates, key=lambda c: _drift(c, duration_seconds))
        if _drift(best, duration_seconds) > MAX_DURATION_DRIFT_SECONDS:
            return None
        lyrics = _to_lyrics(best)
        return _plain_only(lyrics) if lyrics else None

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
        return _plain_only(best)
