"""FastAPI dependency providers for the container and use cases."""

from __future__ import annotations

from typing import Annotated

from fastapi import Depends, Request

from app.application.downloads import RequestDownload
from app.application.library import ManageLibrary
from app.application.playlists import ManagePlaylists
from app.application.search import SearchSongs
from app.container import Container


def get_container(request: Request) -> Container:
    return request.app.state.container


def get_search_songs(c: Annotated[Container, Depends(get_container)]) -> SearchSongs:
    return c.search_songs


def get_request_download(c: Annotated[Container, Depends(get_container)]) -> RequestDownload:
    return c.request_download


def get_library(c: Annotated[Container, Depends(get_container)]) -> ManageLibrary:
    return c.manage_library


def get_playlists(c: Annotated[Container, Depends(get_container)]) -> ManagePlaylists:
    return c.manage_playlists


SearchDep = Annotated[SearchSongs, Depends(get_search_songs)]
DownloadDep = Annotated[RequestDownload, Depends(get_request_download)]
LibraryDep = Annotated[ManageLibrary, Depends(get_library)]
PlaylistsDep = Annotated[ManagePlaylists, Depends(get_playlists)]
