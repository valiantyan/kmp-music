from __future__ import annotations

import argparse
import json
from pathlib import Path

from memory_core.compiler import build_context
from memory_core.governor import process_memory_event
from memory_core.healer import heal
from memory_core.paths import find_repo_root
from memory_core.types import MemoryEvent
from memory_core.validator import now_iso


def main() -> int:
    ap = argparse.ArgumentParser(description="Codex Memory OS v3.5 CLI")
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("compile")
    sub.add_parser("heal")
    add = sub.add_parser("add")
    add.add_argument("content")
    add.add_argument("--source", default="user", choices=["system", "user", "tool", "inference"])
    add.add_argument("--confidence", type=float, default=0.8)
    sub.add_parser("redteam")
    args = ap.parse_args()
    root = find_repo_root()

    if args.cmd == "compile":
        print(build_context(root))
        return 0
    if args.cmd == "heal":
        print(json.dumps(heal(root), ensure_ascii=False, indent=2))
        return 0
    if args.cmd == "add":
        event = MemoryEvent(event_type="manual", source=args.source, content=args.content, timestamp=now_iso(), confidence=args.confidence, raw_ref="cli")
        print(json.dumps(process_memory_event(event, root), ensure_ascii=False, indent=2))
        return 0
    if args.cmd == "redteam":
        from memory_core.audit import run_audit
        ok, report = run_audit(root, quick=False)
        print(report)
        return 0 if ok else 1
    return 1

if __name__ == "__main__":
    raise SystemExit(main())
