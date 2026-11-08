"""ManageLibrary use case: songs, lyrics and audio files."""

from __future__ import annotations

from pathlib import Path

from app.domain.errors import LyricsUnavailableError, NotFoundError
from app.domain.models import Song
from app.ports import AudioStorage, LibraryRepository, LyricsProvider


class ManageLibrary:
    def __init__(
        self, library: LibraryRepository, storage: AudioStorage, lyrics: LyricsProvider
    ) -> None:
        self._library = library
        self._storage = storage
        self._lyrics = lyrics

    def library_id(self) -> str:
        return self._library.library_id()

    def list_songs(self) -> list[Song]:
        return self._library.list_songs()

    def get_song(self, song_id: str) -> Song:
        song = self._library.get_song(song_id)
        if song is None:
            raise NotFoundError("Song not found")
        return song

    def update_song(
        self,
        song_id: str,
        title: str | None = None,
        artist: str | None = None,
        lyrics_offset_ms: int | None = None,
    ) -> Song:
        song = self.get_song(song_id)
        if title is not None:
            song.title = title
        if artist is not None:
            song.artist = artist
        if lyrics_offset_ms is not None:
            song.lyrics_offset_ms = lyrics_offset_ms
        self._library.update_song(song)
        return song

    def delete_song(self, song_id: str) -> None:
        song = self.get_song(song_id)
        self._library.delete_song(song_id)  # also removes it from playlists
        self._storage.delete(song.audio_filename)

    def audio_path(self, song_id: str) -> Path:
        path = self._storage.path_of(self.get_song(song_id).audio_filename)
        if not path.is_file():
            raise NotFoundError("Audio file is missing")
        return path

    def refresh_lyrics(self, song_id: str) -> Song:
        song = self.get_song(song_id)
        try:
            lyrics = self._lyrics.get(song.title, song.artist, song.duration_seconds)
        except Exception as exc:
            raise LyricsUnavailableError("The lyrics service is unavailable right now") from exc
        song.synced_lrc = lyrics.synced_lrc if lyrics else None
        song.plain_lyrics = lyrics.plain if lyrics else None
        self._library.update_song(song)
        return song
