from __future__ import annotations

from fastapi import APIRouter

from app.api.deps import LibraryDep
from app.api.errors import RESPONSE_401
from app.api.schemas import LibraryIdentity

router = APIRouter(tags=["library"])


@router.get(
    "/library",
    response_model=LibraryIdentity,
    summary="Library identity",
    description=(
        "Returns the persistent id of this server's library. Clients use it before a sync to "
        "avoid deleting local data when they are pointed at a different (e.g. brand new) server."
    ),
    responses={
        200: {
            "content": {
                "application/json": {"example": {"library_id": "3f2b8c1e9d4a4b6f8a7c5d2e1f0a9b8c"}}
            }
        },
        **RESPONSE_401,
    },
)
def get_library_identity(uc: LibraryDep) -> LibraryIdentity:
    return LibraryIdentity(library_id=uc.library_id())
