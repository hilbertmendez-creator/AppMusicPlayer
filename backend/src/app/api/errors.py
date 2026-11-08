"""Exception handlers: every error body is `{"detail": "<message>"}` (HTML for /admin)."""

from __future__ import annotations

from html import escape

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import HTMLResponse, JSONResponse, Response

from app.api.schemas import ErrorResponse
from app.domain.errors import InvalidInputError, LyricsUnavailableError, NotFoundError

RESPONSE_401 = {401: {"model": ErrorResponse, "description": "Missing or invalid bearer token."}}
RESPONSE_404 = {404: {"model": ErrorResponse, "description": "Resource not found."}}
RESPONSE_422 = {422: {"model": ErrorResponse, "description": "Invalid input."}}
RESPONSE_502 = {502: {"model": ErrorResponse, "description": "Upstream service unavailable."}}


def _respond(request: Request, status: int, detail: str) -> Response:
    if request.url.path.startswith("/admin"):
        page = (
            "<!doctype html><meta charset=utf-8><title>Mi Música</title>"
            "<body style='font:22px sans-serif;margin:2rem'>"
            f"<h1>No se pudo completar la acción</h1><p>{escape(detail)}</p>"
            "<p><a href='javascript:history.back()'>Volver</a></p></body>"
        )
        return HTMLResponse(page, status_code=status)
    return JSONResponse({"detail": detail}, status_code=status)


def _validation_message(exc: RequestValidationError) -> str:
    parts = []
    for err in exc.errors():
        loc = ".".join(str(p) for p in err["loc"] if p not in ("body", "query", "path", "form"))
        parts.append(f"{loc}: {err['msg']}" if loc else err["msg"])
    return "; ".join(parts) or "Invalid request"


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(NotFoundError)
    async def _not_found(request: Request, exc: NotFoundError):
        return _respond(request, 404, str(exc))

    @app.exception_handler(InvalidInputError)
    async def _invalid(request: Request, exc: InvalidInputError):
        return _respond(request, 422, str(exc))

    @app.exception_handler(LyricsUnavailableError)
    async def _upstream(request: Request, exc: LyricsUnavailableError):
        return _respond(request, 502, str(exc))

    @app.exception_handler(RequestValidationError)
    async def _validation(request: Request, exc: RequestValidationError):
        return _respond(request, 422, _validation_message(exc))
