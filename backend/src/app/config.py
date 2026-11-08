"""Environment-driven settings."""

from __future__ import annotations

from pathlib import Path

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore", case_sensitive=False)

    app_token: str = Field(min_length=1, description="Bearer token the Android app must send.")
    admin_user: str = "admin"
    admin_password: str = Field(min_length=1, description="Password for the /admin panel.")
    genius_token: str | None = None
    data_dir: Path = Path("./data")
    lrclib_user_agent: str = "MiMusica/0.1 (personal use)"

    @field_validator("genius_token", mode="before")
    @classmethod
    def _blank_token_is_none(cls, value):
        return value or None

    @property
    def db_url(self) -> str:
        return f"sqlite:///{(self.data_dir / 'library.sqlite3').resolve().as_posix()}"
