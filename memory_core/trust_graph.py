from __future__ import annotations

import math
from collections import defaultdict
from datetime import datetime, timezone
from typing import Dict, Iterable, List, Tuple

from memory_core.conflict import build_edges, choose_conflict_winner
from memory_core.types import GraphEdge, MemoryItem


def parse_ts(ts: str) -> datetime:
    try:
        return datetime.fromisoformat(ts.replace("Z", "+00:00"))
    except Exception:
        return datetime.now(timezone.utc)


def recency_factor(item: MemoryItem) -> float:
    now = datetime.now(timezone.utc)
    dt = parse_ts(item.timestamp)
    age_days = max(0.0, (now - dt).total_seconds() / 86400)
    return 0.65 + 0.35 * math.exp(-age_days / 90.0)


def recompute_authority(items: List[MemoryItem], edges: List[GraphEdge]) -> List[MemoryItem]:
    support = defaultdict(float)
    contradict = defaultdict(float)
    duplicate = defaultdict(float)
    for e in edges:
        if e.relation == "supports":
            support[e.src] += e.weight
            support[e.dst] += e.weight
        elif e.relation == "contradicts":
            contradict[e.src] += e.weight
            contradict[e.dst] += e.weight
        elif e.relation == "duplicates":
            duplicate[e.src] += e.weight * 0.5
            duplicate[e.dst] += e.weight * 0.5
    for item in items:
        base = max(0.0, min(1.0, item.trust * 0.55 + item.confidence * 0.35 + 0.10))
        sup = min(0.25, support[item.id] * 0.05 + duplicate[item.id] * 0.03)
        con = min(0.40, contradict[item.id] * 0.12)
        item.authority = max(0.0, min(1.0, (base + sup - con) * recency_factor(item)))
    return items


def optimize_conflicts(items: List[MemoryItem], edges: List[GraphEdge]) -> Tuple[List[MemoryItem], List[str]]:
    by_id: Dict[str, MemoryItem] = {x.id: x for x in items}
    notes: List[str] = []
    for edge in edges:
        if edge.relation != "contradicts":
            continue
        a, b = by_id.get(edge.src), by_id.get(edge.dst)
        if not a or not b:
            continue
        winner = choose_conflict_winner(a, b)
        loser = b if winner.id == a.id else a
        if loser.status == "active" and winner.status in {"active", "candidate"}:
            loser.status = "review"
            loser.reasons.append(f"conflict_with:{winner.id}")
            notes.append(f"demoted {loser.id} due to conflict with {winner.id}")
    return list(by_id.values()), notes


def build_graph_for_item(new_item: MemoryItem, existing: List[MemoryItem], cfg: dict) -> List[GraphEdge]:
    return build_edges(new_item, existing, cfg)
