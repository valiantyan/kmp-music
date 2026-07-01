from __future__ import annotations

import re
from typing import Iterable, List, Tuple

from memory_core.embedding import semantic_similarity
from memory_core.types import GraphEdge, MemoryItem

NEGATIVE_MARKERS = ["not", "never", "no longer", "avoid", "do not", "don't", "禁止", "不要", "不再", "不能"]
POSITIVE_MARKERS = ["always", "must", "should", "prefer", "use", "要", "必须", "应该", "优先", "喜欢"]


def polarity(text: str) -> int:
    lower = (text or "").lower()
    neg = sum(1 for x in NEGATIVE_MARKERS if x in lower)
    pos = sum(1 for x in POSITIVE_MARKERS if x in lower)
    if neg > pos:
        return -1
    if pos > neg:
        return 1
    return 0


def detect_relation(a: MemoryItem, b: MemoryItem, cfg: dict) -> Tuple[str, float, str]:
    sim = semantic_similarity(a.content, b.content, cfg)
    if sim < 0.45:
        return "related", sim, "low_similarity"
    pa, pb = polarity(a.content), polarity(b.content)
    if sim >= 0.62 and pa != 0 and pb != 0 and pa != pb:
        return "contradicts", sim, "similar_content_opposite_polarity"
    if sim >= 0.82:
        return "duplicates", sim, "high_similarity"
    if sim >= 0.62:
        return "supports", sim, "semantic_support"
    return "related", sim, "semantic_related"


def build_edges(new_item: MemoryItem, existing: Iterable[MemoryItem], cfg: dict) -> List[GraphEdge]:
    edges: List[GraphEdge] = []
    for old in existing:
        if old.id == new_item.id or old.status not in {"active", "candidate", "review"}:
            continue
        relation, weight, reason = detect_relation(new_item, old, cfg)
        if weight >= 0.55 or relation in {"contradicts", "duplicates"}:
            edges.append(GraphEdge(src=new_item.id, dst=old.id, relation=relation, weight=weight, reason=reason))
    return edges


def choose_conflict_winner(a: MemoryItem, b: MemoryItem) -> MemoryItem:
    score_a = a.authority * 0.55 + a.trust * 0.35 + a.confidence * 0.10
    score_b = b.authority * 0.55 + b.trust * 0.35 + b.confidence * 0.10
    return a if score_a >= score_b else b
