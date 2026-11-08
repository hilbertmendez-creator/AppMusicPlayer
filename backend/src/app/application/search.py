"""SearchSongs use case: title search, lyrics-fragment search, or both."""

from __future__ import annotations

import logging
from dataclasses import replace
from typing import Literal

from app.domain.models import SearchResult
from app.ports import LibraryRepository, LyricsSearch, MusicSource

log = logging.getLogger(__name__)

SearchMode = Literal["auto", "title", "lyrics"]


class SearchSongs:
    def __init__(
        self,
        music: MusicSource,
        lyrics_search: LyricsSearch | None,
        library: LibraryRepository,
    ) -> None:
        self._music = music
        self._lyrics_search = lyrics_search
        self._library = library

    def execute(self, query: str, mode: SearchMode = "auto", limit: int = 5) -> list[SearchResult]:
        if mode == "title":
            found = self._by_title(query, limit)
        elif mode == "lyrics":
            found = self._by_lyrics(query, limit, fallback=True)
        else:
            found = self._by_title(query, limit)
            if self._lyrics_search is not None:
                found += self._by_lyrics(query, limit, fallback=False)
        unique = self._dedupe(found)[:limit]
        present = self._library.source_ids_in_library([r.source_id for r in unique])
        return [replace(r, in_library=r.source_id in present) for r in unique]

    def _by_title(self, query: str, limit: int) -> list[SearchResult]:
        return [replace(r, match="title") for r in self._music.search(query, limit)]

    def _by_lyrics(self, fragment: str, limit: int, *, fallback: bool) -> list[SearchResult]:
        found: list[SearchResult] = []
        if self._lyrics_search is not None:
            try:
                for hint in self._lyrics_search.find_by_fragment(fragment, limit):
                    found += self._music.search(f"{hint.title} {hint.artist}", 1)
            except Exception:
                log.warning("Lyrics fragment search failed", exc_info=True)
                found = []
        if not found and fallback:
            found = self._music.search(fragment, limit)
        return [replace(r, match="lyrics") for r in found]

    @staticmethod
    def _dedupe(results: list[SearchResult]) -> list[SearchResult]:
        seen: set[str] = set()
        out: list[SearchResult] = []
        for r in results:
            if r.source_id not in seen:
                seen.add(r.source_id)
                out.append(r)
        return out
