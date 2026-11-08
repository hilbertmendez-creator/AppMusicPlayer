from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

import pytest
from pydantic import ValidationError

from app.config import Settings

BACKEND = Path(__file__).resolve().parents[1]


def test_settings_require_secrets(monkeypatch):
    monkeypatch.delenv("APP_TOKEN", raising=False)
    monkeypatch.delenv("ADMIN_PASSWORD", raising=False)
    with pytest.raises(ValidationError):
        Settings(_env_file=None)


def test_settings_defaults_from_env(monkeypatch):
    monkeypatch.setenv("APP_TOKEN", "t")
    monkeypatch.setenv("ADMIN_PASSWORD", "p")
    s = Settings(_env_file=None)
    assert s.admin_user == "admin"
    assert s.genius_token is None
    assert str(s.data_dir) == "data"
    assert "MiMusica" in s.lrclib_user_agent


def test_openapi_metadata(client):
    spec = client.get("/openapi.json").json()
    assert spec["info"]["title"] and spec["info"]["description"]
    assert spec["info"]["version"] == "0.1.0"
    tags = {t["name"] for t in spec["tags"]}
    assert {"health", "search", "downloads", "songs", "playlists"} <= tags
    scheme = spec["components"]["securitySchemes"]
    assert any(s.get("type") == "http" and s.get("scheme") == "bearer" for s in scheme.values())


def test_every_api_operation_is_documented(client):
    spec = client.get("/openapi.json").json()
    for path, ops in spec["paths"].items():
        if not path.startswith("/api/v1"):
            continue
        for method, op in ops.items():
            where = f"{method.upper()} {path}"
            assert op.get("summary"), where
            assert op.get("description"), where
            assert op.get("tags"), where
            if path != "/api/v1/health":
                assert "401" in op["responses"], where
                assert op.get("security"), where
            if "{" in path:
                assert "404" in op["responses"], where
            if method in ("post", "patch") or "parameters" in op:
                assert "422" in op["responses"], where


def test_admin_not_in_openapi(client):
    spec = client.get("/openapi.json").json()
    assert not [p for p in spec["paths"] if p.startswith("/admin")]


def test_schemas_have_descriptions_and_examples(client):
    spec = client.get("/openapi.json").json()
    for name in ("SearchResult", "DownloadJob", "Song", "Playlist"):
        props = spec["components"]["schemas"][name]["properties"]
        for prop, schema in props.items():
            assert schema.get("description"), f"{name}.{prop}"
            assert schema.get("examples") or "$ref" in schema or schema.get("anyOf"), (
                f"{name}.{prop}"
            )


def test_export_script_writes_spec(tmp_path):
    out = tmp_path / "openapi.json"
    env_ok = subprocess.run(
        [sys.executable, str(BACKEND / "scripts" / "export_openapi.py"), str(out)],
        cwd=BACKEND,
        capture_output=True,
        text=True,
    )
    assert env_ok.returncode == 0, env_ok.stderr
    spec = json.loads(out.read_text(encoding="utf-8"))
    assert spec["info"]["title"] and "/api/v1/songs" in spec["paths"]
