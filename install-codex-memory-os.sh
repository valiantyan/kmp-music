#!/usr/bin/env bash
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT"
mkdir -p .agent-memory/archive .codex/hooks memory_core
chmod +x .codex/hooks/*.py 2>/dev/null || true
export PYTHONPYCACHEPREFIX="${PYTHONPYCACHEPREFIX:-${TMPDIR:-/tmp}/codex-memory-os-pycache}"
python3 -m py_compile memory_core/*.py .codex/hooks/*.py
python3 .codex/hooks/audit_memory_kit.py --quick
printf '\nCodex Memory OS v3.5 installed. Next: run /hooks in Codex and review/trust project hooks.\n'
