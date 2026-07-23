#!/usr/bin/env bash

set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
scenario="${1:-artists}"

case "$scenario" in
    home|home-playing|albums|artists|favorites|me|album-detail|album-detail-playing|artist-detail-compact|artist-detail|artist-detail-wide|artist-detail-playing|artist-detail-no-cover|artist-detail-interaction|playlists|playlist-management|search|search-playing|search-albums|search-artists|search-playlists|search-empty)
        ;;
    *)
        echo "不支持的场景: $scenario" >&2
        echo "用法: ./scripts/desktop-ui-qa.sh [home|home-playing|albums|artists|favorites|me|album-detail|album-detail-playing|artist-detail-compact|artist-detail|artist-detail-wide|artist-detail-playing|artist-detail-no-cover|artist-detail-interaction|playlists|playlist-management|search|search-playing|search-albums|search-artists|search-playlists|search-empty]" >&2
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

if [[ "$scenario" == "artist-detail-interaction" && ! -s "$output_directory/04-pressed.png" ]]; then
    echo "缺少 Desktop UI QA 交互按下证据: $output_directory/04-pressed.png" >&2
    exit 1
fi

echo "Desktop UI QA 通过: $scenario"
echo "证据目录: $output_directory"
