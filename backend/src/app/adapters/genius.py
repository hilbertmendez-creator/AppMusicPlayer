"""LyricsSearch adapter for the Genius API (search by lyrics fragment)."""

from __future__ import annotations

import httpx

from app.domain.models import TrackHint

BASE_URL = "https://api.genius.com"


class GeniusLyricsSearch:
    def __init__(self, token: str, client: httpx.Client | None = None) -> None:
        self._headers = {"Authorization": f"Bearer {token}"}
        self._client = client or httpx.Client(base_url=BASE_URL, timeout=10.0)

    def find_by_fragment(self, fragment: str, limit: int) -> list[TrackHint]:
        resp = self._client.get("/search", params={"q": fragment}, headers=self._headers)
        resp.raise_for_status()
        hits = resp.json().get("response", {}).get("hits", [])
        out: list[TrackHint] = []
        for hit in hits:
            result = hit.get("result") or {}
            title = result.get("title")
            artist = (result.get("primary_artist") or {}).get("name")
            if title and artist:
                out.append(TrackHint(title=title, artist=artist))
        return out[:limit]
