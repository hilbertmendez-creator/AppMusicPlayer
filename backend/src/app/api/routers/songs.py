from __future__ import annotations

from pathlib import Path

from fastapi import APIRouter, Response
from fastapi.responses import FileResponse

from app.api.deps import LibraryDep
from app.api.errors import RESPONSE_401, RESPONSE_404, RESPONSE_422, RESPONSE_502
from app.api.schemas import LyricsResponse, Song, SongList, SongUpdate

router = APIRouter(tags=["songs"])

MEDIA_TYPES = {
    ".m4a": "audio/mp4",
    ".mp4": "audio/mp4",
    ".webm": "audio/webm",
    ".opus": "audio/ogg",
    ".ogg": "audio/ogg",
    ".mp3": "audio/mpeg",
}


@router.get(
    "/songs",
    response_model=SongList,
    summary="List songs",
    description="Returns every song in the library, oldest first.",
    responses={**RESPONSE_401},
)
def list_songs(lib: LibraryDep) -> SongList:
    return SongList(songs=[Song.of(s) for s in lib.list_songs()])


@router.get(
    "/songs/{song_id}",
    response_model=Song,
    summary="Get a song",
    description="Returns one song's metadata.",
    responses={**RESPONSE_401, **RESPONSE_404, **RESPONSE_422},
)
def get_song(song_id: str, lib: LibraryDep) -> Song:
    return Song.of(lib.get_song(song_id))


@router.patch(
    "/songs/{song_id}",
    response_model=Song,
    summary="Edit a song",
    description="Updates the title, artist and/or lyrics offset. Omitted fields are unchanged.",
    responses={**RESPONSE_401, **RESPONSE_404, **RESPONSE_422},
)
def patch_song(song_id: str, body: SongUpdate, lib: LibraryDep) -> Song:
    return Song.of(lib.update_song(song_id, body.title, body.artist, body.lyrics_offset_ms))


@router.delete(
    "/songs/{song_id}",
    status_code=204,
    summary="Delete a song",
    description=(
        "Deletes the song, removes it from every playlist and deletes its audio file. "
        "This cannot be undone."
    ),
    responses={**RESPONSE_401, **RESPONSE_404, **RESPONSE_422},
)
def delete_song(song_id: str, lib: LibraryDep) -> Response:
    lib.delete_song(song_id)
    return Response(status_code=204)


@router.get(
    "/songs/{song_id}/audio",
    response_class=FileResponse,
    summary="Download song audio",
    description=(
        "Streams the audio file with its media type (for example `audio/mp4` for m4a). "
        "HTTP `Range` requests are supported so players can seek."
    ),
    responses={
        200: {"description": "Full audio file.", "content": {"audio/mp4": {}, "audio/*": {}}},
        206: {"description": "Partial audio (Range request).", "content": {"audio/*": {}}},
        **RESPONSE_401,
        **RESPONSE_404,
        **RESPONSE_422,
    },
)
def get_audio(song_id: str, lib: LibraryDep) -> FileResponse:
    path = lib.audio_path(song_id)
    media_type = MEDIA_TYPES.get(Path(path).suffix.lower(), "application/octet-stream")
    return FileResponse(path, media_type=media_type)


@router.get(
    "/songs/{song_id}/lyrics",
    response_model=LyricsResponse,
    summary="Get song lyrics",
    description="Returns synced (LRC) and plain lyrics (either may be null) plus the offset.",
    responses={**RESPONSE_401, **RESPONSE_404, **RESPONSE_422},
)
def get_lyrics(song_id: str, lib: LibraryDep) -> LyricsResponse:
    return LyricsResponse.of(lib.get_song(song_id))


@router.post(
    "/songs/{song_id}/lyrics/refresh",
    response_model=LyricsResponse,
    summary="Re-fetch lyrics",
    description="Looks the lyrics up again at LRCLIB and replaces the stored ones.",
    responses={**RESPONSE_401, **RESPONSE_404, **RESPONSE_422, **RESPONSE_502},
)
def refresh_lyrics(song_id: str, lib: LibraryDep) -> LyricsResponse:
    return LyricsResponse.of(lib.refresh_lyrics(song_id))
