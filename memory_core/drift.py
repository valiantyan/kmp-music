from __future__ import annotations

from datetime import datetime, timezone
from typing import Iterable, List

from memory_core.embedding import semantic_similarity
from memory_core.types import MemoryItem


def compute_drift(item: MemoryItem, recent_texts: Iterable[str], cfg: dict) -> float:
    texts = [t for t in recent_texts if t]
    if not texts:
        return item.drift_score
    best = max(semantic_similarity(item.content, t, cfg) for t in texts)
    return max(0.0, min(1.0, 1.0 - best))


def update_drift(items: List[MemoryItem], recent_texts: Iterable[str], cfg: dict) -> List[MemoryItem]:
    for item in items:
        if item.status != "active":
            continue
        # Wiki is stable and should not drift based on recent behavior alone.
        if item.layer == "wiki":
            continue
        new_drift = compute_drift(item, recent_texts, cfg)
        # Smooth drift to avoid one-turn noise.
        item.drift_score = max(0.0, min(1.0, item.drift_score * 0.7 + new_drift * 0.3))
    return items
