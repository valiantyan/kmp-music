from __future__ import annotations

import re
from typing import List, Tuple

from memory_core.types import Layer, MemoryEvent

PREFERENCE_RE = re.compile(r"(?i)(i prefer|i like|i want|my preference|请|希望|偏好|喜欢|以后.*(用|要)|输出风格)")
LEARNING_RE = re.compile(r"(?i)(lesson|learned|avoid|do not repeat|bug|failed|failure|mistake|踩坑|教训|不要再|失败|错误|纠正)")
WIKI_RE = re.compile(r"(?i)(project fact|canonical|decision|business rule|term|glossary|产品定位|业务规则|关键决策|术语|长期共识)")
WORKING_RE = re.compile(r"(?i)(next step|todo|handoff|in progress|blocked|完成|未完成|下一步|待确认|交接)")


def classify_event(event: MemoryEvent, trust: float, cfg: dict) -> Tuple[Layer, List[str]]:
    text = event.content or ""
    reasons: List[str] = []

    if PREFERENCE_RE.search(text):
        if event.source == "user" and trust >= float(cfg["thresholds"].get("preference_min", 0.68)):
            reasons.append("explicit_user_preference")
            return "preferences", reasons
        reasons.append("preference_like_but_not_explicit_or_low_trust")
        return "working", reasons

    if WIKI_RE.search(text):
        if cfg.get("promotion", {}).get("wiki_auto_write", False) and trust >= float(cfg["thresholds"].get("canonical_min", 0.82)) and event.source in {"system", "user"}:
            reasons.append("canonical_pattern_high_trust")
            return "wiki", reasons
        reasons.append("canonical_pattern_review_only")
        return "working", reasons

    if LEARNING_RE.search(text):
        if trust >= float(cfg["thresholds"].get("learning_min", 0.55)) and event.source != "tool":
            reasons.append("learning_pattern_validated")
            return "learning", reasons
        reasons.append("learning_candidate_low_trust")
        return "working", reasons

    if WORKING_RE.search(text):
        reasons.append("working_handoff_pattern")
        return "working", reasons

    if event.source == "tool":
        reasons.append("tool_default_working")
        return "working", reasons

    if trust >= float(cfg["thresholds"].get("learning_min", 0.55)):
        reasons.append("medium_trust_default_learning")
        return "learning", reasons

    reasons.append("low_trust_default_working")
    return "working", reasons
