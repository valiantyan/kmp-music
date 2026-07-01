#!/usr/bin/env python3

from __future__ import annotations

import json
import os
import sys
import traceback
from pathlib import Path

# Ensure repository root is importable.
def _find_root() -> Path:
    start = Path(os.getcwd()).resolve()
    for c in [start, *start.parents]:
        if (c / "memory_core").exists() and (c / ".agent-memory").exists():
            return c
    here = Path(__file__).resolve()
    for c in [here.parent, *here.parents]:
        if (c / "memory_core").exists() and (c / ".agent-memory").exists():
            return c
    return start

ROOT = _find_root()
sys.path.insert(0, str(ROOT))

from memory_core.store import append_error

def read_payload() -> dict:
    raw = sys.stdin.read()
    if not raw.strip():
        return {}
    try:
        return json.loads(raw)
    except Exception:
        return {"_raw_stdin": raw}

def emit(obj: dict) -> None:
    print(json.dumps(obj, ensure_ascii=False))

def fail_safe(event_name: str, exc: BaseException) -> None:
    try:
        append_error({"event": event_name, "error": repr(exc), "traceback": traceback.format_exc()[:4000]}, ROOT)
    except Exception:
        pass
    emit({
        "continue": True,
        "systemMessage": f"Memory OS hook {event_name} failed safely; main Codex flow continues.",
        "hookSpecificOutput": {"hookEventName": event_name, "additionalContext": "Memory hook failed safely; do not rely on new memory for this turn."}
    })


def main() -> int:
    try:
        payload = read_payload()
        from memory_core.governor import process_stop
        result = process_stop(payload, ROOT)
        emit({
            "continue": True,
            "hookSpecificOutput": {
                "hookEventName": "Stop",
                "additionalContext": f"Memory OS turn checkpoint complete: active={result.get('healed',{}).get('active')} review={result.get('healed',{}).get('review')}. Stop is turn-level, not true SessionEnd."
            }
        })
        return 0
    except Exception as e:
        fail_safe("Stop", e)
        return 0

if __name__ == "__main__":
    raise SystemExit(main())
