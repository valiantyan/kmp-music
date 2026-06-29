# macOS LibVLC Packaging

This directory contains helper scripts and records for the Apple Silicon Desktop playback runtime.

Inputs:

- Official DMG: `vlc-3.0.23-arm64.dmg`
- Expected SHA-256: `fc6fac08d87f538517d44aca0c5e7a244b67c8c4cb589bf478363a7315fd5e0d`
- Target app path: `KMP Music.app/Contents/Resources/LibVLC`

Required release checks:

- `shasum -a 256` matches `SOURCE_RECORD.md`.
- App launches and plays without `/Applications/VLC.app`.
- `codesign --verify --deep --strict --verbose=2` accepts the app.
- `spctl -a -t exec -vv` accepts the app.
- `otool -L` does not show non-system dynamic libraries outside the app bundle.
- DMG is notarized and stapled before external distribution.

Development notes:

- `desktopRun` reuses the project runtime when it already exists, but does not download LibVLC automatically.
- Run `./gradlew :composeApp:prepareMacosArm64LibVlc` before `desktopRun` when local Desktop playback must use the bundled runtime.
- Downloaded DMGs are cached under Gradle user home by default: `~/.gradle/caches/kmp-music/macos-libvlc/download`.
- Use `-Pkmp.music.libvlc.download.dir=/path/to/download-dir` when reusing a manually downloaded and verified DMG directory.
- Use `-Pkmp.music.libvlc.download.url=https://.../vlc-3.0.23-arm64.dmg` only when switching to a trusted mirror for the same file.
- Release packaging still depends on staging LibVLC into the app bundle, so test DMGs remain usable without a system VLC install.
