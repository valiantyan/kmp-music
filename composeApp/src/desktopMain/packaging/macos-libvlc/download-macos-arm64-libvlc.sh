#!/usr/bin/env bash
set -euo pipefail

download_dir="$1"
legacy_download_dir="${2:-}"
url="${3:-https://download.videolan.org/pub/videolan/vlc/last/macosx/vlc-3.0.23-arm64.dmg}"
expected_sha256="fc6fac08d87f538517d44aca0c5e7a244b67c8c4cb589bf478363a7315fd5e0d"
dmg_path="${download_dir}/vlc-3.0.23-arm64.dmg"
partial_path="${dmg_path}.part"

mkdir -p "${download_dir}"

sha256_of() {
  shasum -a 256 "$1" | awk '{print $1}'
}

seed_from_legacy_download_dir() {
  if [[ -z "${legacy_download_dir}" || ! -d "${legacy_download_dir}" ]]; then
    return
  fi

  local legacy_dmg_path="${legacy_download_dir}/vlc-3.0.23-arm64.dmg"
  local legacy_partial_path="${legacy_dmg_path}.part"
  if [[ -f "${dmg_path}" || -f "${partial_path}" ]]; then
    return
  fi

  if [[ -f "${legacy_dmg_path}" ]]; then
    echo "Seeding LibVLC DMG cache from legacy build directory"
    cp -p "${legacy_dmg_path}" "${dmg_path}"
    return
  fi

  if [[ -f "${legacy_partial_path}" ]]; then
    echo "Seeding partial LibVLC DMG cache from legacy build directory"
    cp -p "${legacy_partial_path}" "${partial_path}"
  fi
}

seed_from_legacy_download_dir

if [[ -f "${dmg_path}" ]]; then
  actual_sha256="$(sha256_of "${dmg_path}")"
  if [[ "${actual_sha256}" == "${expected_sha256}" ]]; then
    echo "${dmg_path}"
    exit 0
  fi

  echo "Existing LibVLC DMG checksum mismatch, resuming download"
  mv -f "${dmg_path}" "${partial_path}"
fi

for attempt in 1 2; do
  curl \
    --fail \
    --http1.1 \
    --location \
    --continue-at - \
    --retry 5 \
    --retry-delay 5 \
    --retry-all-errors \
    --connect-timeout 30 \
    --speed-time 300 \
    --speed-limit 128 \
    "${url}" \
    --output "${partial_path}"

  actual_sha256="$(sha256_of "${partial_path}")"
  if [[ "${actual_sha256}" == "${expected_sha256}" ]]; then
    mv -f "${partial_path}" "${dmg_path}"
    echo "${dmg_path}"
    exit 0
  fi

  echo "Downloaded LibVLC DMG checksum mismatch"
  echo "expected=${expected_sha256}"
  echo "actual=${actual_sha256}"

  if [[ "${attempt}" == "1" ]]; then
    echo "Retrying with a fresh download"
    rm -f "${partial_path}"
  fi
done

if [[ -f "${partial_path}" ]]; then
  actual_sha256="$(sha256_of "${partial_path}")"
else
  actual_sha256="missing"
fi

if [[ "${actual_sha256}" != "${expected_sha256}" ]]; then
  echo "LibVLC SHA-256 mismatch"
  echo "expected=${expected_sha256}"
  echo "actual=${actual_sha256}"
  exit 1
fi
