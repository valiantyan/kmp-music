from __future__ import annotations

import re
from typing import List, Tuple

from memory_core.types import Layer, MemoryEvent

PREFERENCE_RE = re.compile(
    r"(?i)(i prefer|my preference|please remember|remember that|always\s+(answer|respond|reply|use|write|format|start|show|explain|return|prefer|keep|include|be)|以后默认|以后都|请记住|帮我记住|我偏好|我的偏好|希望以后|输出风格)"
)
LEARNING_RE = re.compile(
    r"(?i)(lesson|learned|avoid|do not repeat|bug|failed|failure|mistake|fixed|fix|root cause|resolved|踩坑|教训|不要再|失败|错误|纠正|修复|根因|解决)"
)
WIKI_RE = re.compile(
    r"(?i)(project fact|canonical|decision|business rule|term|glossary|产品定位|业务规则|关键决策|术语|长期共识)"
)
WORKING_RE = re.compile(
    r"(?i)(next step|handoff|in progress|blocked|blocker|pending|follow up|完成|未完成|下一步|待确认|交接|阻塞)"
)
LEARNING_PRIORITY_RE = re.compile(
    r"(?i)(root cause|fixed|resolved|lesson|learned|avoid|failed|failure|mistake|踩坑|教训|失败|错误|纠正|根因|已修复|修复完成|完成修复|已解决)"
)


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

    working_match = WORKING_RE.search(text)
    learning_match = LEARNING_RE.search(text)

    if learning_match:
        if event.source == "tool":
            reasons.append("learning_candidate_tool_evidence_only")
            return "working", reasons
        if not working_match or LEARNING_PRIORITY_RE.search(text):
            reasons.append("learning_pattern_validated")
            return "learning", reasons

    if working_match:
        reasons.append("working_handoff_pattern")
        return "working", reasons

    if learning_match:
        reasons.append("learning_pattern_validated")
        return "learning", reasons

    if event.source == "tool":
        reasons.append("tool_default_working")
        return "working", reasons

    reasons.append("low_trust_default_working")
    return "working", reasons
