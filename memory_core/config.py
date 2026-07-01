from __future__ import annotations

from pathlib import Path
from typing import Any, Dict, Optional

from memory_core.io_utils import load_json
from memory_core.paths import find_repo_root

DEFAULT_CONFIG: Dict[str, Any] = {
    "version": "3.5.0",
    "thresholds": {
        "trust_min": 0.35,
        "learning_min": 0.55,
        "canonical_min": 0.82,
        "preference_min": 0.68,
        "drift_review": 0.65,
        "authority_min": 0.45,
        "semantic_firewall_block": 0.78,
        "semantic_firewall_review": 0.62,
    },
    "limits": {
        "context_max_chars": 12000,
        "items_per_layer": 24,
        "buffer_max_lines": 2000,
        "jsonl_max_bytes": 2000000,
        "review_queue_max_lines": 500,
        "markdown_auto_items": 80,
    },
    "source_weights": {"system": 0.98, "user": 0.74, "tool": 0.24, "inference": 0.46},
    "promotion": {
        "wiki_auto_write": False,
        "tool_to_canonical_allowed": False,
        "preferences_require_explicit_user_signal": True,
        "min_supporting_evidence_for_wiki": 2,
    },
    "ttl_days": {"ephemeral": 2, "working": 14, "learning": 90, "preferences": 365, "wiki": 99999},
    "security": {"redaction_enabled": True, "sensitive_path_patterns": [], "extra_secret_patterns": [], "extra_injection_templates": []},
    "embedding": {"backend": "local_ngrams", "ngram_min": 3, "ngram_max": 5, "max_features": 4096},
}


def deep_merge(a: Dict[str, Any], b: Dict[str, Any]) -> Dict[str, Any]:
    out = dict(a)
    for k, v in b.items():
        if isinstance(v, dict) and isinstance(out.get(k), dict):
            out[k] = deep_merge(out[k], v)
        else:
            out[k] = v
    return out


def load_config(root: Optional[Path] = None) -> Dict[str, Any]:
    r = root or find_repo_root()
    cfg = load_json(r / ".agent-memory" / "config.json", {})
    if not isinstance(cfg, dict):
        cfg = {}
    return deep_merge(DEFAULT_CONFIG, cfg)
