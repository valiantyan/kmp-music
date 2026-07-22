#!/usr/bin/env bash

set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
scenario="${1:-artists}"

case "$scenario" in
    home|home-playing|albums|artists)
        ;;
    *)
        echo "不支持的场景: $scenario" >&2
        echo "用法: ./scripts/desktop-ui-qa.sh [home|home-playing|albums|artists]" >&2
        exit 2
        ;;
esac

evidence_root="$project_root/build/desktop-ui-qa"
mkdir -p "$evidence_root"
output_directory="$(mktemp -d "$evidence_root/${scenario}.XXXXXX")"

"$project_root/gradlew" \
    -p "$project_root" \
    :composeApp:desktopUiQa \
    -PdesktopUiQaScenario="$scenario" \
    -PdesktopUiQaOutputDir="$output_directory"

for frame in 01-initial.png 02-active.png 03-settled.png; do
    if [[ ! -s "$output_directory/$frame" ]]; then
        echo "缺少 Desktop UI QA 证据: $output_directory/$frame" >&2
        exit 1
    fi
done

echo "Desktop UI QA 通过: $scenario"
echo "证据目录: $output_directory"
