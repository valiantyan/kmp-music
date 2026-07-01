from __future__ import annotations

import os
from pathlib import Path
from typing import Optional

ROOT_MARKERS = (".codex", ".agent-memory", "AGENTS.md")


def find_repo_root(start: Optional[str] = None) -> Path:
    current = Path(start or os.getcwd()).resolve()
    if current.is_file():
        current = current.parent
    for candidate in [current, *current.parents]:
        if (candidate / ".codex").exists() and (candidate / ".agent-memory").exists():
            return candidate
        if (candidate / "AGENTS.md").exists() and (candidate / ".agent-memory").exists():
            return candidate
    # Fallback: if invoked from a hook file, walk from this file's parents.
    here = Path(__file__).resolve()
    for candidate in [here.parent, *here.parents]:
        if (candidate / ".codex").exists() and (candidate / ".agent-memory").exists():
            return candidate
    return current


def memory_dir(root: Optional[Path] = None) -> Path:
    r = root or find_repo_root()
    return r / ".agent-memory"
