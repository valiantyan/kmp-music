from __future__ import annotations

from datetime import datetime, timezone
from pathlib import Path
from typing import List, Optional

from memory_core.config import load_config
from memory_core.drift import update_drift
from memory_core.io_utils import read_jsonl
from memory_core.store import load_items, materialize_markdown, paths, save_items
from memory_core.trust_graph import build_graph_for_item, optimize_conflicts, recompute_authority
from memory_core.types import GraphEdge, MemoryItem


def parse_ts(ts: str) -> datetime:
    try:
        return datetime.fromisoformat(ts.replace("Z", "+00:00"))
    except Exception:
        return datetime.now(timezone.utc)


def garbage_collect(items: List[MemoryItem], cfg: dict) -> List[MemoryItem]:
    now = datetime.now(timezone.utc)
    drift_review = float(cfg.get("thresholds", {}).get("drift_review", 0.65))
    for item in items:
        if item.status not in {"active", "candidate", "review"}:
            continue
        if item.expires_at:
            try:
                if parse_ts(item.expires_at) < now:
                    item.status = "archived"
                    item.reasons.append("ttl_expired")
            except Exception:
                pass
        if item.status == "active" and item.layer != "wiki" and item.drift_score >= drift_review and item.authority < 0.70:
            item.status = "review"
            item.reasons.append("high_drift_demoted_for_review")
    return items


def heal(root: Optional[Path] = None) -> dict:
    cfg = load_config(root)
    p = paths(root)
    items = load_items(root)
    recent = read_jsonl(p["buffer"], limit=200)
    recent_texts = []
    for row in recent:
        text = row.get("sanitized_content") or row.get("summary") or row.get("content") or ""
        if text:
            recent_texts.append(str(text))

    items = update_drift(items, recent_texts, cfg)

    all_edges: List[GraphEdge] = []
    for item in items:
        all_edges.extend(build_graph_for_item(item, items, cfg))
    items = recompute_authority(items, all_edges)
    items, conflict_notes = optimize_conflicts(items, all_edges)
    items = garbage_collect(items, cfg)

    save_items(items, root)
    materialize_markdown(root)
    return {
        "items": len(items),
        "edges": len(all_edges),
        "conflict_notes": conflict_notes,
        "active": sum(1 for x in items if x.status == "active"),
        "review": sum(1 for x in items if x.status == "review"),
    }
