#!/usr/bin/env bash
set -euo pipefail

dmg_path="$1"
output_dir="$2"
mount_output="$(hdiutil attach -nobrowse -readonly "${dmg_path}")"
mount_point="$(printf '%s\n' "${mount_output}" | awk '/\/Volumes\// {for (i=3; i<=NF; i++) printf "%s%s", $i, (i<NF ? OFS : ORS)}' | tail -n 1)"

if [[ -z "${mount_point}" || ! -d "${mount_point}" ]]; then
  echo "Unable to mount VLC DMG"
  exit 1
fi

trap 'hdiutil detach "${mount_point}" >/dev/null' EXIT

vlc_app="${mount_point}/VLC.app"
lib_source="${vlc_app}/Contents/MacOS/lib"
plugins_source="${vlc_app}/Contents/MacOS/plugins"
license_source="${vlc_app}/Contents/Resources"

add_rpath_if_missing() {
  local file_path="$1"
  local rpath="$2"
  local tool_output
  if ! otool -l "${file_path}" | grep -F "path ${rpath} " >/dev/null; then
    if ! tool_output="$(install_name_tool -add_rpath "${rpath}" "${file_path}" 2>&1)"; then
      printf '%s\n' "${tool_output}" >&2
      return 1
    fi
  fi
}

repair_runtime_rpaths() {
  find "${output_dir}/lib" -maxdepth 1 -type f -name '*.dylib' -print0 |
    while IFS= read -r -d '' file_path; do
      add_rpath_if_missing "${file_path}" "@loader_path"
    done

  find "${output_dir}/plugins" -type f -name '*.dylib' -print0 |
    while IFS= read -r -d '' file_path; do
      add_rpath_if_missing "${file_path}" "@loader_path/../lib"
    done
}

sign_runtime_libraries() {
  local sign_output
  find "${output_dir}/lib" "${output_dir}/plugins" -type f -name '*.dylib' -print0 |
    while IFS= read -r -d '' file_path; do
      if ! sign_output="$(codesign --force --sign - "${file_path}" 2>&1)"; then
        printf '%s\n' "${sign_output}" >&2
        return 1
      fi
    done
}

rm -rf "${output_dir}"
mkdir -p "${output_dir}/lib" "${output_dir}/plugins" "${output_dir}/licenses"
cp -R "${lib_source}/." "${output_dir}/lib/"
cp -R "${plugins_source}/." "${output_dir}/plugins/"
repair_runtime_rpaths
sign_runtime_libraries

if [[ -d "${license_source}" ]]; then
  find "${license_source}" -maxdepth 2 \( -iname '*copying*' -o -iname '*license*' -o -iname '*notice*' \) -print0 |
    while IFS= read -r -d '' file_path; do
      cp "${file_path}" "${output_dir}/licenses/"
    done
fi

echo "${output_dir}"
