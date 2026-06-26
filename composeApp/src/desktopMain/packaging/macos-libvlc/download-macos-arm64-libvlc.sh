#!/usr/bin/env bash
set -euo pipefail

download_dir="$1"
url="https://download.videolan.org/pub/videolan/vlc/last/macosx/vlc-3.0.23-arm64.dmg"
expected_sha256="fc6fac08d87f538517d44aca0c5e7a244b67c8c4cb589bf478363a7315fd5e0d"
dmg_path="${download_dir}/vlc-3.0.23-arm64.dmg"

mkdir -p "${download_dir}"

if [[ ! -f "${dmg_path}" ]]; then
  curl -L "${url}" -o "${dmg_path}"
fi

actual_sha256="$(shasum -a 256 "${dmg_path}" | awk '{print $1}')"
if [[ "${actual_sha256}" != "${expected_sha256}" ]]; then
  echo "LibVLC SHA-256 mismatch"
  echo "expected=${expected_sha256}"
  echo "actual=${actual_sha256}"
  exit 1
fi

echo "${dmg_path}"
