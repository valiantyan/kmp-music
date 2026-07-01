from __future__ import annotations

from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

from memory_core.config import load_config
from memory_core.io_utils import append_jsonl, atomic_write_text, read_jsonl, write_jsonl
from memory_core.paths import find_repo_root
from memory_core.types import GraphEdge, MemoryItem

AUTO_BEGIN = "<!-- MEMORY_OS_AUTO_BEGIN -->"
AUTO_END = "<!-- MEMORY_OS_AUTO_END -->"


def paths(root: Optional[Path] = None) -> Dict[str, Path]:
    r = root or find_repo_root()
    md = r / ".agent-memory"
    return {
        "root": r,
        "memory_dir": md,
        "items": md / "memory_items.jsonl",
        "buffer": md / "buffer.jsonl",
        "review": md / "review_queue.jsonl",
        "security": md / "security_events.jsonl",
        "errors": md / "hook_errors.jsonl",
        "graph": md / "trust_graph.json",
        "working_md": md / "working.md",
        "learning_md": md / "learning.md",
        "wiki_md": md / "wiki.md",
        "preferences_md": md / "preferences.md",
    }


def append_buffer(event: Dict, root: Optional[Path] = None) -> None:
    cfg = load_config(root)
    p = paths(root)["buffer"]
    limits = cfg.get("limits", {})
    append_jsonl(p, event, max_lines=int(limits.get("buffer_max_lines", 2000)), max_bytes=int(limits.get("jsonl_max_bytes", 2000000)))


def append_security(event: Dict, root: Optional[Path] = None) -> None:
    cfg = load_config(root)
    append_jsonl(paths(root)["security"], event, max_lines=1000, max_bytes=int(cfg.get("limits", {}).get("jsonl_max_bytes", 2000000)))


def append_error(event: Dict, root: Optional[Path] = None) -> None:
    append_jsonl(paths(root)["errors"], event, max_lines=1000, max_bytes=1000000)


def load_items(root: Optional[Path] = None) -> List[MemoryItem]:
    rows = read_jsonl(paths(root)["items"])
    items: List[MemoryItem] = []
    for row in rows:
        if "_corrupt_line" in row:
            continue
        try:
            items.append(MemoryItem.from_dict(row))
        except TypeError:
            # tolerate older rows
            continue
    return items


def save_items(items: Iterable[MemoryItem], root: Optional[Path] = None) -> None:
    write_jsonl(paths(root)["items"], [x.to_dict() for x in items])


def upsert_item(item: MemoryItem, root: Optional[Path] = None) -> None:
    items = load_items(root)
    by_hash = {x.content_hash: i for i, x in enumerate(items) if x.content_hash}
    if item.content_hash in by_hash:
        old = items[by_hash[item.content_hash]]
        # Preserve highest authority/status and merge evidence.
        old.confidence = max(old.confidence, item.confidence)
        old.trust = max(old.trust, item.trust)
        old.authority = max(old.authority, item.authority)
        old.usage_count += 1
        old.last_used = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
        old.evidence.extend(item.evidence)
        old.reasons = sorted(set(old.reasons + item.reasons))
        if old.status in {"rejected", "archived"} and item.status != old.status:
            old.status = item.status
    else:
        items.append(item)
    save_items(items, root)


def append_review(item: MemoryItem, root: Optional[Path] = None) -> None:
    cfg = load_config(root)
    append_jsonl(paths(root)["review"], item.to_dict(), max_lines=int(cfg.get("limits", {}).get("review_queue_max_lines", 500)), max_bytes=1000000)


def markdown_for_items(items: List[MemoryItem], layer: str, limit: int) -> str:
    active = [x for x in items if x.layer == layer and x.status == "active"]
    active.sort(key=lambda x: (x.authority, x.trust, x.timestamp), reverse=True)
    if not active:
        return "_No auto-managed items yet._\n"
    lines = []
    for item in active[:limit]:
        reasons = ", ".join(item.reasons[:3])
        lines.append(f"- `{item.id}` authority={item.authority:.2f} trust={item.trust:.2f} source={item.source}: {item.content}")
        if reasons:
            lines.append(f"  - reasons: {reasons}")
    return "\n".join(lines) + "\n"


def replace_auto_section(text: str, generated: str) -> str:
    if AUTO_BEGIN not in text or AUTO_END not in text:
        return text.rstrip() + f"\n\n## Auto-Managed Items\n\n{AUTO_BEGIN}\n{generated}{AUTO_END}\n"
    before = text.split(AUTO_BEGIN, 1)[0]
    after = text.split(AUTO_END, 1)[1]
    return before + AUTO_BEGIN + "\n" + generated + AUTO_END + after


def materialize_markdown(root: Optional[Path] = None) -> None:
    cfg = load_config(root)
    limit = int(cfg.get("limits", {}).get("markdown_auto_items", 80))
    items = load_items(root)
    p = paths(root)
    mapping = {
        "working": p["working_md"],
        "learning": p["learning_md"],
        "wiki": p["wiki_md"],
        "preferences": p["preferences_md"],
    }
    for layer, file_path in mapping.items():
        existing = file_path.read_text(encoding="utf-8") if file_path.exists() else f"# {layer}\n\n{AUTO_BEGIN}\n{AUTO_END}\n"
        generated = markdown_for_items(items, layer, limit)
        atomic_write_text(file_path, replace_auto_section(existing, generated))
