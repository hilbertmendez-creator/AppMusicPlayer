from __future__ import annotations

from fastapi import APIRouter

from app.api.schemas import HealthResponse

router = APIRouter(tags=["health"])


@router.get(
    "/health",
    response_model=HealthResponse,
    summary="Health check",
    description="Liveness probe. Public: it is the only endpoint that needs no credentials.",
    responses={200: {"content": {"application/json": {"example": {"status": "ok"}}}}},
)
def health() -> HealthResponse:
    return HealthResponse(status="ok")
