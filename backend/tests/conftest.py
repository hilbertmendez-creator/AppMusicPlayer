from __future__ import annotations

import base64

import pytest
from fastapi.testclient import TestClient

from app.adapters.fs_storage import FileSystemStorage
from app.adapters.sql_repository import SqlAlchemyRepository
from app.config import Settings
from app.domain.models import Lyrics
from app.main import create_app
from tests.fakes import FakeLyricsProvider, FakeLyricsSearch, FakeMusicSource

APP_TOKEN = "test-app-token"
ADMIN_USER = "admin"
ADMIN_PASSWORD = "s3cret-pass"

LRC = "[00:01.00]Hello\n[00:05.00]World\n"


@pytest.fixture
def settings(tmp_path) -> Settings:
    return Settings(
        _env_file=None,
        app_token=APP_TOKEN,
        admin_user=ADMIN_USER,
        admin_password=ADMIN_PASSWORD,
        genius_token=None,
        data_dir=tmp_path / "data",
    )


@pytest.fixture
def repo(tmp_path) -> SqlAlchemyRepository:
    return SqlAlchemyRepository(f"sqlite:///{tmp_path / 'test.sqlite3'}")


@pytest.fixture
def storage(tmp_path) -> FileSystemStorage:
    return FileSystemStorage(tmp_path / "audio")


@pytest.fixture
def music() -> FakeMusicSource:
    return FakeMusicSource()


@pytest.fixture
def lyrics_provider() -> FakeLyricsProvider:
    return FakeLyricsProvider(Lyrics(synced_lrc=LRC, plain="Hello\nWorld"))


@pytest.fixture
def lyrics_search() -> FakeLyricsSearch:
    return FakeLyricsSearch()


@pytest.fixture
def client(settings, music, lyrics_provider, lyrics_search):
    app = create_app(
        settings,
        overrides={
            "music_source": music,
            "lyrics_provider": lyrics_provider,
            "lyrics_search": lyrics_search,
        },
    )
    with TestClient(app) as c:
        c.headers.update({"Authorization": f"Bearer {APP_TOKEN}"})
        yield c


@pytest.fixture
def anon(client):
    """Same app, no credentials."""
    return TestClient(client.app)


def basic(user: str = ADMIN_USER, password: str = ADMIN_PASSWORD) -> dict[str, str]:
    raw = base64.b64encode(f"{user}:{password}".encode()).decode()
    return {"Authorization": f"Basic {raw}"}
