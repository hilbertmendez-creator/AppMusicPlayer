"""ManagePlaylists use case."""

from __future__ import annotations

import uuid
from typing import Literal

from app.domain.errors import InvalidInputError, NotFoundError
from app.domain.models import Playlist, utcnow
from app.ports import LibraryRepository


class ManagePlaylists:
    def __init__(self, library: LibraryRepository) -> None:
        self._library = library

    def list_playlists(self) -> list[Playlist]:
        return self._library.list_playlists()

    def get_playlist(self, playlist_id: str) -> Playlist:
        playlist = self._library.get_playlist(playlist_id)
        if playlist is None:
            raise NotFoundError("Playlist not found")
        return playlist

    def create(self, name: str, song_ids: list[str] | None = None) -> Playlist:
        name = self._clean_name(name)
        ids = list(song_ids or [])
        self._check_songs(ids)
        playlist = Playlist(id=str(uuid.uuid4()), name=name, song_ids=ids, updated_at=utcnow())
        self._library.add_playlist(playlist)
        return playlist

    def update(
        self, playlist_id: str, name: str | None = None, song_ids: list[str] | None = None
    ) -> Playlist:
        playlist = self.get_playlist(playlist_id)
        if name is not None:
            playlist.name = self._clean_name(name)
        if song_ids is not None:
            self._check_songs(song_ids)
            playlist.song_ids = list(song_ids)
        playlist.updated_at = utcnow()
        self._library.update_playlist(playlist)
        return playlist

    def delete(self, playlist_id: str) -> None:
        self.get_playlist(playlist_id)
        self._library.delete_playlist(playlist_id)

    def add_song(self, playlist_id: str, song_id: str) -> Playlist:
        playlist = self.get_playlist(playlist_id)
        if song_id in playlist.song_ids:
            return playlist
        return self.update(playlist_id, song_ids=[*playlist.song_ids, song_id])

    def remove_song(self, playlist_id: str, song_id: str) -> Playlist:
        playlist = self.get_playlist(playlist_id)
        return self.update(playlist_id, song_ids=[s for s in playlist.song_ids if s != song_id])

    def move_song(
        self, playlist_id: str, song_id: str, direction: Literal["up", "down"]
    ) -> Playlist:
        playlist = self.get_playlist(playlist_id)
        ids = list(playlist.song_ids)
        if song_id not in ids:
            return playlist
        i = ids.index(song_id)
        j = i - 1 if direction == "up" else i + 1
        if 0 <= j < len(ids):
            ids[i], ids[j] = ids[j], ids[i]
        return self.update(playlist_id, song_ids=ids)

    @staticmethod
    def _clean_name(name: str) -> str:
        name = name.strip()
        if not 1 <= len(name) <= 80:
            raise InvalidInputError("Playlist name must be 1 to 80 characters")
        return name

    def _check_songs(self, song_ids: list[str]) -> None:
        unknown = [s for s in set(song_ids) if self._library.get_song(s) is None]
        if unknown:
            raise InvalidInputError(f"Unknown song ids: {', '.join(sorted(unknown))}")
