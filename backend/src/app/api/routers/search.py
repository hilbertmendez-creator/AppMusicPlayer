from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Query

from app.api.deps import SearchDep
from app.api.errors import RESPONSE_401, RESPONSE_422
from app.api.schemas import SearchResponse, SearchResult
from app.application.search import SearchMode
from app.domain.errors import InvalidInputError

router = APIRouter(tags=["search"])


@router.get(
    "/search",
    response_model=SearchResponse,
    summary="Search songs",
    description=(
        "Searches for songs to download. `mode=title` searches by title/artist, `mode=lyrics` "
        "treats `q` as a fragment of the lyrics (falling back to a plain search when the lyrics "
        "service is unavailable) and `mode=auto` merges both, title matches first. Results "
        "already in the library have `in_library=true`."
    ),
    responses={**RESPONSE_401, **RESPONSE_422},
)
def search(
    uc: SearchDep,
    q: Annotated[
        str,
        Query(
            min_length=1,
            max_length=200,
            description="Title, artist or lyrics fragment.",
            examples=["bohemian rhapsody"],
        ),
    ],
    mode: Annotated[SearchMode, Query(description="Search strategy.")] = "auto",
    limit: Annotated[int, Query(ge=1, le=20, description="Maximum number of results.")] = 5,
) -> SearchResponse:
    query = q.strip()
    if not query:
        raise InvalidInputError("q must not be blank")
    return SearchResponse(results=[SearchResult.of(r) for r in uc.execute(query, mode, limit)])
