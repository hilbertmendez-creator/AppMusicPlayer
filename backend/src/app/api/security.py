"""Authentication dependencies (constant-time comparisons)."""

from __future__ import annotations

import secrets
from typing import Annotated
from urllib.parse import urlparse

from fastapi import Depends, HTTPException, Request
from fastapi.security import (
    HTTPAuthorizationCredentials,
    HTTPBasic,
    HTTPBasicCredentials,
    HTTPBearer,
)

bearer_scheme = HTTPBearer(
    scheme_name="BearerAuth",
    description="Static device token configured as `APP_TOKEN` on the server.",
    auto_error=False,
)
basic_scheme = HTTPBasic(realm="Mi Musica admin", auto_error=False)


def _same(a: str, b: str) -> bool:
    return secrets.compare_digest(a.encode("utf-8"), b.encode("utf-8"))


def require_app_token(
    request: Request,
    creds: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer_scheme)],
) -> None:
    expected = request.app.state.container.settings.app_token
    if creds is None or not _same(creds.credentials, expected):
        raise HTTPException(
            status_code=401,
            detail="Missing or invalid bearer token",
            headers={"WWW-Authenticate": "Bearer"},
        )


def require_admin(
    request: Request,
    creds: Annotated[HTTPBasicCredentials | None, Depends(basic_scheme)],
) -> None:
    settings = request.app.state.container.settings
    ok_user = creds is not None and _same(creds.username, settings.admin_user)
    ok_pass = creds is not None and _same(creds.password, settings.admin_password)
    if not (ok_user and ok_pass):
        raise HTTPException(
            status_code=401,
            detail="Authentication required",
            headers={"WWW-Authenticate": 'Basic realm="Mi Musica admin"'},
        )
    # Basic credentials are sent automatically by browsers, so block cross-site form posts.
    if request.method not in ("GET", "HEAD", "OPTIONS"):
        origin = request.headers.get("origin")
        if origin and urlparse(origin).netloc != request.headers.get("host", ""):
            raise HTTPException(status_code=403, detail="Cross-origin request blocked")
