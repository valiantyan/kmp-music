#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

usage() {
  cat <<'USAGE'
Usage: ./scripts/verify-local.sh [mode]

Modes:
  default, quick       Run Android Kotlin compile and desktop tests.
  android              Run Android Kotlin compile only.
  android-unit         Run Android JVM unit tests.
  apk                  Build the Android debug APK.
  lint                 Run Android lint for the debug variant.
  desktop              Run desktop tests.
  macos-avfoundation   Run desktop tests plus core macOS AVFoundation smokes.
  tasks                List :composeApp Gradle tasks.
  help                 Show this help.
USAGE
}

require_project_root() {
  if [[ ! -f "settings.gradle.kts" || ! -f "composeApp/build.gradle.kts" ]]; then
    echo "verify-local: run from the KMP Music project root or keep this script under scripts/." >&2
    exit 64
  fi

  if [[ ! -x "./gradlew" ]]; then
    echo "verify-local: ./gradlew is missing or not executable." >&2
    exit 64
  fi
}

run_gradle() {
  echo "verify-local: ./gradlew $*"
  ./gradlew "$@"
}

main() {
  local mode="${1:-default}"
  require_project_root

  case "$mode" in
    default|quick)
      run_gradle :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
      ;;
    android)
      run_gradle :composeApp:compileDebugKotlinAndroid
      ;;
    android-unit)
      run_gradle :composeApp:testDebugUnitTest
      ;;
    apk)
      run_gradle :composeApp:assembleDebug
      ;;
    lint)
      run_gradle :composeApp:lintDebug
      ;;
    desktop)
      run_gradle :composeApp:desktopTest
      ;;
    macos-avfoundation)
      run_gradle \
        :composeApp:desktopTest \
        :composeApp:macosAvFoundationBridgeSmoke \
        :composeApp:macosAvFoundationDefaultRuntimeSmoke
      ;;
    tasks)
      run_gradle :composeApp:tasks
      ;;
    help|--help|-h)
      usage
      ;;
    *)
      echo "verify-local: unknown mode '$mode'." >&2
      usage >&2
      exit 64
      ;;
  esac
}

main "$@"
