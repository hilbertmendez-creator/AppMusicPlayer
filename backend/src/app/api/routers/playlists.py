from __future__ import annotations

from fastapi import APIRouter, Response

from app.api.deps import PlaylistsDep
from app.api.errors import RESPONSE_401, RESPONSE_404, RESPONSE_422
from app.api.schemas import Playlist, PlaylistCreate, PlaylistList, PlaylistUpdate

router = APIRouter(tags=["playlists"])


@router.get(
    "/playlists",
    response_model=PlaylistList,
    summary="List playlists",
    description="Returns all playlists with their ordered song ids.",
    responses={**RESPONSE_401},
)
def list_playlists(uc: PlaylistsDep) -> PlaylistList:
    return PlaylistList(playlists=[Playlist.of(p) for p in uc.list_playlists()])


@router.post(
    "/playlists",
    response_model=Playlist,
    status_code=201,
    summary="Create a playlist",
    description="Creates a playlist. Unknown song ids are rejected with 422.",
    responses={**RESPONSE_401, **RESPONSE_422},
)
def create_playlist(body: PlaylistCreate, uc: PlaylistsDep) -> Playlist:
    return Playlist.of(uc.create(body.name, body.song_ids))


@router.patch(
    "/playlists/{playlist_id}",
    response_model=Playlist,
    summary="Edit a playlist",
    description=(
        "Renames the playlist and/or replaces its ordered song ids (use this to add, remove or "
        "reorder songs). Unknown song ids are rejected with 422."
    ),
    responses={**RESPONSE_401, **RESPONSE_404, **RESPONSE_422},
)
def update_playlist(playlist_id: str, body: PlaylistUpdate, uc: PlaylistsDep) -> Playlist:
    return Playlist.of(uc.update(playlist_id, body.name, body.song_ids))


@router.delete(
    "/playlists/{playlist_id}",
    status_code=204,
    summary="Delete a playlist",
    description="Deletes the playlist only; its songs stay in the library.",
    responses={**RESPONSE_401, **RESPONSE_404, **RESPONSE_422},
)
def delete_playlist(playlist_id: str, uc: PlaylistsDep) -> Response:
    uc.delete(playlist_id)
    return Response(status_code=204)
