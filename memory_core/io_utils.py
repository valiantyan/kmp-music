from __future__ import annotations

import contextlib
import json
import os
import time
from pathlib import Path
from typing import Any, Dict, Iterable, Iterator, List, Optional


@contextlib.contextmanager
def file_lock(path: Path, timeout: float = 5.0) -> Iterator[None]:
    lock = path.with_suffix(path.suffix + ".lock")
    start = time.time()
    while True:
        try:
            fd = os.open(str(lock), os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            os.write(fd, str(os.getpid()).encode("utf-8"))
            os.close(fd)
            break
        except FileExistsError:
            if time.time() - start > timeout:
                # Stale-lock tolerant: remove after timeout.
                try:
                    lock.unlink()
                except FileNotFoundError:
                    pass
                continue
            time.sleep(0.05)
    try:
        yield
    finally:
        try:
            lock.unlink()
        except FileNotFoundError:
            pass


def atomic_write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_name(path.name + ".tmp")
    with file_lock(path):
        tmp.write_text(text, encoding="utf-8")
        os.replace(tmp, path)


def read_jsonl(path: Path, limit: Optional[int] = None) -> List[Dict[str, Any]]:
    if not path.exists():
        return []
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    if limit is not None:
        lines = lines[-limit:]
    out: List[Dict[str, Any]] = []
    for line in lines:
        if not line.strip():
            continue
        try:
            obj = json.loads(line)
            if isinstance(obj, dict):
                out.append(obj)
        except json.JSONDecodeError:
            out.append({"_corrupt_line": line[:500]})
    return out


def append_jsonl(path: Path, obj: Dict[str, Any], max_lines: Optional[int] = None, max_bytes: Optional[int] = None) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    line = json.dumps(obj, ensure_ascii=False, sort_keys=True) + "\n"
    with file_lock(path):
        with path.open("a", encoding="utf-8") as f:
            f.write(line)
    # Rotate after releasing the append lock; rotate_jsonl obtains its own lock.
    rotate_jsonl(path, max_lines=max_lines, max_bytes=max_bytes)


def write_jsonl(path: Path, rows: Iterable[Dict[str, Any]]) -> None:
    text = "".join(json.dumps(row, ensure_ascii=False, sort_keys=True) + "\n" for row in rows)
    atomic_write_text(path, text)


def rotate_jsonl(path: Path, max_lines: Optional[int] = None, max_bytes: Optional[int] = None) -> None:
    if not path.exists():
        return
    if max_bytes is not None and path.stat().st_size <= max_bytes and max_lines is None:
        return
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    keep = lines
    if max_lines is not None and len(keep) > max_lines:
        keep = keep[-max_lines:]
    if max_bytes is not None:
        while keep and sum(len(x.encode("utf-8")) + 1 for x in keep) > max_bytes:
            keep = keep[len(keep)//10 + 1:]
    atomic_write_text(path, "\n".join(keep) + ("\n" if keep else ""))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return default


def save_json(path: Path, obj: Any) -> None:
    atomic_write_text(path, json.dumps(obj, ensure_ascii=False, indent=2, sort_keys=True) + "\n")
