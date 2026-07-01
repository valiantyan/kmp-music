from __future__ import annotations

import json
import os
import subprocess
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, Optional

from memory_core.config import load_config
from memory_core.healer import heal
from memory_core.paths import find_repo_root
from memory_core.security import references_sensitive_path, redact_secrets
from memory_core.store import append_buffer, append_review, append_security, materialize_markdown, upsert_item
from memory_core.types import MemoryEvent
from memory_core.validator import item_from_event, validate_event


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def summarize_value(value: Any, max_chars: int = 1600) -> str:
    try:
        if isinstance(value, str):
            text = value
        else:
            text = json.dumps(value, ensure_ascii=False, sort_keys=True)
    except Exception:
        text = repr(value)
    text = text.replace("\x00", "")
    if len(text) > max_chars:
        return text[:max_chars] + "...[truncated]"
    return text


def git_snapshot(root: Path) -> Dict[str, Any]:
    def run(args):
        try:
            p = subprocess.run(args, cwd=str(root), text=True, capture_output=True, timeout=3)
            if p.returncode == 0:
                return p.stdout.splitlines()[:200]
        except Exception:
            pass
        return []
    return {
        "status_short": run(["git", "status", "--porcelain=v1"]),
        "diff_name_only": run(["git", "diff", "--name-only"]),
    }


def process_memory_event(event: MemoryEvent, root: Optional[Path] = None) -> Dict[str, Any]:
    root = root or find_repo_root()
    cfg = load_config(root)
    result = validate_event(event, cfg)

    buffer_row = {
        "timestamp": event.timestamp,
        "event_type": event.event_type,
        "source": event.source,
        "session_id": event.session_id,
        "turn_id": event.turn_id,
        "tool_name": event.tool_name,
        "sanitized_content": result.sanitized_text,
        "validation": {
            "allowed": result.allowed,
            "status": result.status,
            "layer": result.layer,
            "trust": result.trust,
            "authority": result.authority,
            "reasons": result.reasons,
        },
        "metadata": event.metadata,
    }
    append_buffer(buffer_row, root)

    if not result.allowed or result.status == "rejected":
        append_security({**buffer_row, "security_action": "rejected"}, root)
        return {"recorded": False, "status": "rejected", "layer": result.layer, "reasons": result.reasons}

    item = item_from_event(event, result)
    # Set TTL from config.
    from memory_core.validator import expires_at
    item.expires_at = expires_at(item.layer, cfg)

    if item.status == "review":
        append_review(item, root)
    else:
        upsert_item(item, root)

    return {"recorded": item.status != "review", "status": item.status, "layer": item.layer, "trust": item.trust, "authority": item.authority, "reasons": item.reasons}


def process_user_prompt(payload: Dict[str, Any], root: Optional[Path] = None) -> Dict[str, Any]:
    root = root or find_repo_root()
    prompt = payload.get("prompt") or payload.get("user_prompt") or payload.get("message") or ""
    prompt = summarize_value(prompt, 2000)
    event = MemoryEvent(
        event_type="UserPromptSubmit",
        source="user",
        content=prompt,
        timestamp=now_iso(),
        session_id=str(payload.get("session_id", "")),
        turn_id=str(payload.get("turn_id", "")),
        raw_ref="user_prompt",
        confidence=0.92 if any(x in prompt.lower() for x in ["i prefer", "my preference", "以后", "偏好", "请记住"]) else 0.62,
        metadata={"hook_event_name": payload.get("hook_event_name", "UserPromptSubmit")},
    )
    return process_memory_event(event, root)


def process_tool_use(payload: Dict[str, Any], root: Optional[Path] = None) -> Dict[str, Any]:
    root = root or find_repo_root()
    cfg = load_config(root)
    tool_input = payload.get("tool_input", {})
    tool_response = payload.get("tool_response", {})
    input_text = summarize_value(tool_input, 1600)
    response_text = summarize_value(tool_response, 1800)
    sensitive = references_sensitive_path(input_text, cfg.get("security", {}).get("sensitive_path_patterns", []))
    if sensitive:
        response_text = "[REDACTED: response omitted because tool input referenced a sensitive path]"

    extra_patterns = cfg.get("security", {}).get("extra_secret_patterns", []) or []
    content = redact_secrets(
        f"Tool `{payload.get('tool_name', 'unknown')}` ran. Input summary: {input_text}. Output summary: {response_text}.",
        extra_patterns,
    )
    snap = git_snapshot(root)
    event = MemoryEvent(
        event_type="PostToolUse",
        source="tool",
        content=content,
        timestamp=now_iso(),
        session_id=str(payload.get("session_id", "")),
        turn_id=str(payload.get("turn_id", "")),
        tool_name=str(payload.get("tool_name", "unknown")),
        raw_ref=str(payload.get("tool_use_id", "tool")),
        confidence=0.52,
        metadata={"git_snapshot": snap, "sensitive_path_referenced": sensitive},
    )
    return process_memory_event(event, root)


def process_stop(payload: Dict[str, Any], root: Optional[Path] = None) -> Dict[str, Any]:
    root = root or find_repo_root()
    last = payload.get("last_assistant_message") or ""
    summary = summarize_value(last, 1400)
    if summary:
        event = MemoryEvent(
            event_type="Stop",
            source="inference",
            content=f"Turn-level handoff summary: {summary}",
            timestamp=now_iso(),
            session_id=str(payload.get("session_id", "")),
            turn_id=str(payload.get("turn_id", "")),
            raw_ref="last_assistant_message",
            confidence=0.58,
            metadata={"stop_hook_active": payload.get("stop_hook_active", False)},
        )
        processed = process_memory_event(event, root)
    else:
        processed = {"recorded": False, "status": "empty"}
    healed = heal(root)
    materialize_markdown(root)
    return {"processed": processed, "healed": healed}
