# macOS LibVLC Packaging

This directory contains helper scripts and records for the Apple Silicon Desktop playback runtime.

Inputs:

- Official DMG: `vlc-3.0.23-arm64.dmg`
- Expected SHA-256: `fc6fac08d87f538517d44aca0c5e7a244b67c8c4cb589bf478363a7315fd5e0d`
- Target app path: `KMP Music.app/Contents/Frameworks/LibVLC`

Required release checks:

- `shasum -a 256` matches `SOURCE_RECORD.md`.
- App launches and plays without `/Applications/VLC.app`.
- `codesign --verify --deep --strict --verbose=2` accepts the app.
- `spctl -a -t exec -vv` accepts the app.
- `otool -L` does not show non-system dynamic libraries outside the app bundle.
- DMG is notarized and stapled before external distribution.
