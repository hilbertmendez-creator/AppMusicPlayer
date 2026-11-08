"""Caregiver web panel (Jinja2 + HTMX). Reuses the same use cases as the REST API."""

from __future__ import annotations

from pathlib import Path
from typing import Annotated, Literal

from fastapi import APIRouter, BackgroundTasks, Depends, Form, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates

from app.api.deps import DownloadDep, LibraryDep, PlaylistsDep, SearchDep
from app.api.security import require_admin
from app.domain.models import JobStatus

templates = Jinja2Templates(directory=str(Path(__file__).parent / "templates"))

router = APIRouter(prefix="/admin", dependencies=[Depends(require_admin)], include_in_schema=False)

SourceIdForm = Annotated[str, Form(pattern=r"^[A-Za-z0-9_-]{1,64}$")]


def _back(page: str) -> RedirectResponse:
    return RedirectResponse(f"/admin/{page}", status_code=303)


@router.get("")
def index() -> RedirectResponse:
    return _back("search")


@router.get("/search", response_class=HTMLResponse)
def search_page(request: Request):
    return templates.TemplateResponse(request, "search.html", {"active": "search"})


@router.get("/search/results", response_class=HTMLResponse)
def search_results(
    request: Request,
    uc: SearchDep,
    q: str = "",
    mode: Literal["auto", "title", "lyrics"] = "auto",
):
    query = q.strip()[:200]
    results = uc.execute(query, mode, 5) if query else []
    return templates.TemplateResponse(
        request, "_results.html", {"results": results, "query": query}
    )


@router.post("/downloads", response_class=HTMLResponse)
def start_download(
    request: Request,
    uc: DownloadDep,
    tasks: BackgroundTasks,
    source_id: SourceIdForm,
    title: Annotated[str, Form(max_length=200)] = "",
    artist: Annotated[str, Form(max_length=200)] = "",
):
    job = uc.submit(source_id)
    if job.status is JobStatus.PENDING:
        tasks.add_task(uc.run, job.id, title.strip() or None, artist.strip() or None)
    return templates.TemplateResponse(request, "_job.html", {"job": job})


@router.get("/downloads/{job_id}", response_class=HTMLResponse)
def job_status(request: Request, uc: DownloadDep, job_id: str):
    return templates.TemplateResponse(request, "_job.html", {"job": uc.get_job(job_id)})


@router.get("/library", response_class=HTMLResponse)
def library_page(request: Request, lib: LibraryDep):
    return templates.TemplateResponse(
        request, "library.html", {"active": "library", "songs": lib.list_songs()}
    )


@router.post("/library/{song_id}/offset")
def set_offset(
    lib: LibraryDep, song_id: str, offset_ms: Annotated[int, Form(ge=-600000, le=600000)]
):
    lib.update_song(song_id, lyrics_offset_ms=offset_ms)
    return _back("library")


@router.post("/library/{song_id}/refresh-lyrics")
def refresh_lyrics(lib: LibraryDep, song_id: str):
    lib.refresh_lyrics(song_id)
    return _back("library")


@router.post("/library/{song_id}/delete")
def delete_song(lib: LibraryDep, song_id: str):
    lib.delete_song(song_id)
    return _back("library")


@router.get("/playlists", response_class=HTMLResponse)
def playlists_page(request: Request, lib: LibraryDep, pls: PlaylistsDep):
    songs = lib.list_songs()
    return templates.TemplateResponse(
        request,
        "playlists.html",
        {
            "active": "playlists",
            "playlists": pls.list_playlists(),
            "songs": songs,
            "by_id": {s.id: s for s in songs},
        },
    )


@router.post("/playlists")
def create_playlist(pls: PlaylistsDep, name: Annotated[str, Form()]):
    pls.create(name)
    return _back("playlists")


@router.post("/playlists/{playlist_id}/rename")
def rename_playlist(pls: PlaylistsDep, playlist_id: str, name: Annotated[str, Form()]):
    pls.update(playlist_id, name=name)
    return _back("playlists")


@router.post("/playlists/{playlist_id}/songs")
def add_song(pls: PlaylistsDep, playlist_id: str, song_id: Annotated[str, Form()]):
    pls.add_song(playlist_id, song_id)
    return _back("playlists")


@router.post("/playlists/{playlist_id}/songs/{song_id}/remove")
def remove_song(pls: PlaylistsDep, playlist_id: str, song_id: str):
    pls.remove_song(playlist_id, song_id)
    return _back("playlists")


@router.post("/playlists/{playlist_id}/songs/{song_id}/move")
def move_song(
    pls: PlaylistsDep,
    playlist_id: str,
    song_id: str,
    direction: Annotated[Literal["up", "down"], Form()],
):
    pls.move_song(playlist_id, song_id, direction)
    return _back("playlists")


@router.post("/playlists/{playlist_id}/delete")
def delete_playlist(pls: PlaylistsDep, playlist_id: str):
    pls.delete(playlist_id)
    return _back("playlists")
