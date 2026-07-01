from __future__ import annotations

import hashlib
from datetime import datetime, timedelta, timezone
from typing import List

from memory_core.classifier import classify_event
from memory_core.security import firewall
from memory_core.types import MemoryEvent, MemoryItem, ValidationResult


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def content_hash(text: str) -> str:
    return hashlib.sha256((text or "").encode("utf-8")).hexdigest()


def make_id(text: str, source: str, timestamp: str) -> str:
    return hashlib.sha256(f"{source}\n{timestamp}\n{text}".encode("utf-8")).hexdigest()[:16]


def expires_at(layer: str, cfg: dict) -> str | None:
    ttl = int(cfg.get("ttl_days", {}).get(layer, 30))
    if ttl >= 90000:
        return None
    return (datetime.now(timezone.utc) + timedelta(days=ttl)).isoformat().replace("+00:00", "Z")


def compute_trust(event: MemoryEvent, fw_score: float, evidence_count: int, cfg: dict) -> float:
    source_weight = float(cfg.get("source_weights", {}).get(event.source, 0.4))
    trust = source_weight * max(0.0, min(1.0, event.confidence))
    trust += min(0.12, evidence_count * 0.04)
    trust -= min(0.35, fw_score * 0.30)
    if event.source == "tool":
        trust *= 0.85
    return max(0.0, min(1.0, trust))


def validate_event(event: MemoryEvent, cfg: dict) -> ValidationResult:
    fw = firewall(event.content, event.source, cfg, context=event.event_type)
    evidence_count = len(event.metadata.get("evidence", []) or []) + (1 if event.raw_ref else 0)
    trust = compute_trust(event, fw.score, evidence_count, cfg)
    layer, class_reasons = classify_event(event, trust, cfg)
    reasons: List[str] = [*fw.reasons, *class_reasons]

    trust_min = float(cfg.get("thresholds", {}).get("trust_min", 0.35))
    authority = trust * 0.85 + event.confidence * 0.15

    if fw.action == "block":
        return ValidationResult(False, "rejected", layer, trust, authority, reasons + ["firewall_block"], fw.sanitized_text)

    if event.source == "tool" and layer == "wiki" and not cfg.get("promotion", {}).get("tool_to_canonical_allowed", False):
        layer = "working"
        reasons.append("tool_canonical_promotion_forbidden")

    if layer == "wiki" and not cfg.get("promotion", {}).get("wiki_auto_write", False):
        return ValidationResult(True, "review", layer, trust, authority, reasons + ["wiki_auto_write_disabled"], fw.sanitized_text)

    if fw.action == "review":
        return ValidationResult(True, "review", layer, trust, authority, reasons + ["firewall_review"], fw.sanitized_text)

    if trust < trust_min:
        return ValidationResult(False, "rejected", layer, trust, authority, reasons + ["below_trust_min"], fw.sanitized_text)

    if event.source == "tool":
        # Tool outputs are useful context, but not direct validated rules.
        return ValidationResult(True, "candidate", "working", trust, authority, reasons + ["tool_buffer_candidate_only"], fw.sanitized_text)

    if layer == "preferences" and event.source == "user" and trust >= float(cfg["thresholds"].get("preference_min", 0.68)):
        return ValidationResult(True, "active", layer, trust, authority, reasons, fw.sanitized_text)

    if layer == "learning" and trust >= float(cfg["thresholds"].get("learning_min", 0.55)):
        return ValidationResult(True, "active", layer, trust, authority, reasons, fw.sanitized_text)

    if layer == "working":
        return ValidationResult(True, "active" if trust >= trust_min else "candidate", layer, trust, authority, reasons, fw.sanitized_text)

    return ValidationResult(True, "candidate", layer, trust, authority, reasons, fw.sanitized_text)


def item_from_event(event: MemoryEvent, result: ValidationResult) -> MemoryItem:
    ts = event.timestamp or now_iso()
    ch = content_hash(result.sanitized_text)
    item = MemoryItem(
        id=make_id(result.sanitized_text, event.source, ts),
        content=result.sanitized_text,
        layer=result.layer,
        source=event.source,
        confidence=event.confidence,
        trust=result.trust,
        authority=result.authority,
        timestamp=ts,
        evidence=[{"kind": event.event_type, "ref": event.raw_ref or event.tool_name or event.session_id, "trust": result.trust}],
        status=result.status,
        tags=[event.event_type],
        expires_at=expires_at(result.layer, {"ttl_days": {}}) if False else None,
        reasons=result.reasons,
        content_hash=ch,
    )
    return item
