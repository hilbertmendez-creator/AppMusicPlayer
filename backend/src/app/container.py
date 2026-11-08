"""Composition root: wires adapters into use cases."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from app.adapters.fs_storage import FileSystemStorage
from app.adapters.genius import GeniusLyricsSearch
from app.adapters.lrclib import LrclibProvider
from app.adapters.sql_repository import SqlAlchemyRepository
from app.adapters.ytmusic import YtMusicSource
from app.application.downloads import RequestDownload
from app.application.library import ManageLibrary
from app.application.playlists import ManagePlaylists
from app.application.search import SearchSongs
from app.config import Settings
from app.ports import (
    AudioStorage,
    LibraryRepository,
    LyricsProvider,
    LyricsSearch,
    MusicSource,
)


@dataclass
class Container:
    settings: Settings
    repo: LibraryRepository
    storage: AudioStorage
    music_source: MusicSource
    lyrics_provider: LyricsProvider
    lyrics_search: LyricsSearch | None
    search_songs: SearchSongs
    request_download: RequestDownload
    manage_library: ManageLibrary
    manage_playlists: ManagePlaylists


def build_container(settings: Settings, overrides: dict[str, Any] | None = None) -> Container:
    """Build the object graph. `overrides` may replace any outbound port (used by tests)."""
    ov = overrides or {}
    data_dir = settings.data_dir
    data_dir.mkdir(parents=True, exist_ok=True)

    repo = ov.get("repo") or SqlAlchemyRepository(settings.db_url)
    storage = ov.get("storage") or FileSystemStorage(data_dir / "audio")
    music = ov.get("music_source") or YtMusicSource()
    lyrics = ov.get("lyrics_provider") or LrclibProvider(user_agent=settings.lrclib_user_agent)
    if "lyrics_search" in ov:
        lyrics_search = ov["lyrics_search"]
    elif settings.genius_token:
        lyrics_search = GeniusLyricsSearch(settings.genius_token)
    else:
        lyrics_search = None

    return Container(
        settings=settings,
        repo=repo,
        storage=storage,
        music_source=music,
        lyrics_provider=lyrics,
        lyrics_search=lyrics_search,
        search_songs=SearchSongs(music, lyrics_search, repo),
        request_download=RequestDownload(music, lyrics, repo, storage, data_dir / "staging"),
        manage_library=ManageLibrary(repo, storage, lyrics),
        manage_playlists=ManagePlaylists(repo),
    )
