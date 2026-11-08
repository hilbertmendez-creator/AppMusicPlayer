from __future__ import annotations

from fastapi import APIRouter, BackgroundTasks

from app.api.deps import DownloadDep
from app.api.errors import RESPONSE_401, RESPONSE_404, RESPONSE_422
from app.api.schemas import DownloadJob, DownloadRequest
from app.domain.models import JobStatus

router = APIRouter(tags=["downloads"])


@router.post(
    "/downloads",
    response_model=DownloadJob,
    status_code=202,
    summary="Request a download",
    description=(
        "Queues the download of a search result (audio + lyrics) and returns immediately with a "
        "job. Poll `GET /downloads/{id}` until `status` is `ready` or `failed`. Idempotent: if "
        "the song is already in the library a `ready` job pointing at it is returned."
    ),
    responses={**RESPONSE_401, **RESPONSE_422},
)
def request_download(body: DownloadRequest, uc: DownloadDep, tasks: BackgroundTasks) -> DownloadJob:
    job = uc.submit(body.source_id)
    if job.status is JobStatus.PENDING:
        tasks.add_task(uc.run, job.id, body.title, body.artist)
    return DownloadJob.of(job)


@router.get(
    "/downloads/{job_id}",
    response_model=DownloadJob,
    summary="Get download job status",
    description="Returns the current state of a download job.",
    responses={**RESPONSE_401, **RESPONSE_404, **RESPONSE_422},
)
def get_download(job_id: str, uc: DownloadDep) -> DownloadJob:
    return DownloadJob.of(uc.get_job(job_id))
