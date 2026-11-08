"""FastAPI application factory."""

from __future__ import annotations

from importlib import metadata
from typing import Any

from fastapi import APIRouter, Depends, FastAPI

from app.admin.router import router as admin_router
from app.api.errors import register_error_handlers
from app.api.routers import downloads, health, library, playlists, search, songs
from app.api.security import require_app_token
from app.config import Settings
from app.container import build_container

DESCRIPTION = """
REST API of **Mi Musica**, a personal music and karaoke app.

* Every endpoint except `/api/v1/health` requires `Authorization: Bearer <APP_TOKEN>`.
* JSON uses snake_case; timestamps are ISO-8601 UTC; ids are UUID strings.
* Errors are returned as `{"detail": "..."}`.
"""

TAGS = [
    {"name": "health", "description": "Liveness probe."},
    {"name": "search", "description": "Find songs by title, artist or lyrics fragment."},
    {"name": "downloads", "description": "Asynchronous download jobs (audio + lyrics)."},
    {"name": "library", "description": "Identity of this server's library."},
    {"name": "songs", "description": "The song library, audio streaming and lyrics."},
    {"name": "playlists", "description": "Ordered playlists of library songs."},
]


def _version() -> str:
    try:
        return metadata.version("mi-musica-backend")
    except metadata.PackageNotFoundError:  # pragma: no cover
        return "0.0.0"


def create_app(
    settings: Settings | None = None, overrides: dict[str, Any] | None = None
) -> FastAPI:
    """Build the app. `overrides` can replace outbound ports (used by tests)."""
    settings = settings or Settings()
    app = FastAPI(
        title="Mi Musica API",
        description=DESCRIPTION,
        version=_version(),
        openapi_tags=TAGS,
    )
    app.state.container = build_container(settings, overrides)
    register_error_handlers(app)

    api = APIRouter(prefix="/api/v1")
    api.include_router(health.router)
    protected = APIRouter(dependencies=[Depends(require_app_token)])
    for module in (search, downloads, library, songs, playlists):
        protected.include_router(module.router)
    api.include_router(protected)
    app.include_router(api)
    app.include_router(admin_router)
    return app


def __getattr__(name: str) -> Any:
    """Lazy module-level `app` so `uvicorn app.main:app` works without import-time env."""
    if name == "app":
        instance = create_app()
        globals()["app"] = instance
        return instance
    raise AttributeError(name)
