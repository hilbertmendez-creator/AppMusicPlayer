"""AudioStorage adapter backed by a local directory."""

from __future__ import annotations

import shutil
from pathlib import Path


class FileSystemStorage:
    def __init__(self, root: Path) -> None:
        self.root = Path(root)
        self.root.mkdir(parents=True, exist_ok=True)

    def store(self, src: Path, song_id: str) -> str:
        filename = f"{song_id}{Path(src).suffix.lower()}"
        shutil.move(str(src), str(self.path_of(filename)))
        return filename

    def path_of(self, filename: str) -> Path:
        root = self.root.resolve()
        path = (root / filename).resolve()
        if path.parent != root:
            raise ValueError("Invalid audio filename")
        return path

    def delete(self, filename: str) -> None:
        self.path_of(filename).unlink(missing_ok=True)
