from __future__ import annotations

from dataclasses import dataclass, field, asdict
from typing import Any, Dict, List, Literal, Optional

Source = Literal["system", "user", "tool", "inference"]
Layer = Literal["ephemeral", "working", "learning", "preferences", "wiki"]
Status = Literal["buffered", "candidate", "active", "review", "rejected", "archived"]
Relation = Literal["supports", "contradicts", "duplicates", "related"]


@dataclass
class Evidence:
    kind: str
    ref: str
    trust: float = 0.5


@dataclass
class MemoryItem:
    id: str
    content: str
    layer: Layer
    source: Source
    confidence: float
    trust: float
    authority: float
    timestamp: str
    evidence: List[Dict[str, Any]] = field(default_factory=list)
    status: Status = "candidate"
    tags: List[str] = field(default_factory=list)
    drift_score: float = 0.0
    usage_count: int = 0
    last_used: Optional[str] = None
    expires_at: Optional[str] = None
    reasons: List[str] = field(default_factory=list)
    content_hash: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "MemoryItem":
        return cls(**data)


@dataclass
class MemoryEvent:
    event_type: str
    source: Source
    content: str
    timestamp: str
    session_id: str = ""
    turn_id: str = ""
    tool_name: str = ""
    raw_ref: str = ""
    confidence: float = 0.5
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass
class FirewallDecision:
    allowed: bool
    action: Literal["allow", "redact", "review", "block"]
    score: float
    reasons: List[str]
    sanitized_text: str


@dataclass
class ValidationResult:
    allowed: bool
    status: Status
    layer: Layer
    trust: float
    authority: float
    reasons: List[str]
    sanitized_text: str


@dataclass
class GraphEdge:
    src: str
    dst: str
    relation: Relation
    weight: float
    reason: str = ""
