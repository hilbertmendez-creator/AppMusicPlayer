"""Write the OpenAPI spec to a JSON file (default: <repo>/docs/api/openapi.json).

Usage: uv run python scripts/export_openapi.py [output_path]
"""

from __future__ import annotations

import json
import sys
import tempfile
from pathlib import Path

BACKEND = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND / "src"))

from app.config import Settings  # noqa: E402
from app.main import create_app  # noqa: E402


def main() -> None:
    out = (
        Path(sys.argv[1]) if len(sys.argv) > 1 else BACKEND.parent / "docs" / "api" / "openapi.json"
    )
    with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmp:
        settings = Settings(
            _env_file=None, app_token="export", admin_password="export", data_dir=Path(tmp)
        )
        spec = create_app(settings).openapi()
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(spec, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Wrote {out}")


if __name__ == "__main__":
    main()
