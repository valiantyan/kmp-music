#!/usr/bin/env python3
from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from memory_core.audit import run_audit


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--quick", action="store_true")
    args = ap.parse_args()
    ok, report = run_audit(ROOT, quick=args.quick)
    report_path = ROOT / "RED_TEAM_REPORT.md"
    report_path.write_text(report + "\n", encoding="utf-8")
    print(report)
    return 0 if ok else 1

if __name__ == "__main__":
    raise SystemExit(main())
