#!/usr/bin/env bash
set -euo pipefail

app_path="$1"
libvlc_dir="${app_path}/Contents/Frameworks/LibVLC"

if [[ ! -d "${libvlc_dir}/lib" || ! -d "${libvlc_dir}/plugins" ]]; then
  echo "LibVLC runtime is missing from ${libvlc_dir}"
  exit 1
fi

codesign --verify --deep --strict --verbose=2 "${app_path}"
spctl -a -t exec -vv "${app_path}"

outside_paths="$(
  find "${app_path}/Contents" -type f \( -perm -111 -o -name '*.dylib' -o -name '*.so' \) -print0 |
    while IFS= read -r -d '' binary; do
      if file "${binary}" | grep -q 'Mach-O'; then
        otool -L "${binary}" |
          awk 'NR > 1 {print $1}' |
          grep -v '^/System/Library/' |
          grep -v '^/usr/lib/' |
          grep -v '^@rpath/' |
          grep -v '^@loader_path/' |
          grep -v '^@executable_path/' || true
      fi
    done
)"

if [[ -n "${outside_paths}" ]]; then
  echo "Found non-system dynamic library paths outside the app bundle:"
  printf '%s\n' "${outside_paths}"
  exit 1
fi

echo "macOS app LibVLC verification passed"
