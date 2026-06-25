# macOS Apple Silicon LibVLC Source Record

Runtime: VLC / LibVLC
Version: 3.0.23
Architecture: arm64
Official download URL: https://download.videolan.org/pub/videolan/vlc/last/macosx/vlc-3.0.23-arm64.dmg
Official SHA-256 URL: https://download.videolan.org/pub/videolan/vlc/last/macosx/vlc-3.0.23-arm64.dmg.sha256
Expected SHA-256: fc6fac08d87f538517d44aca0c5e7a244b67c8c4cb589bf478363a7315fd5e0d
Project extraction directory: composeApp/build/macos-libvlc/runtime/LibVLC
App bundle directory: KMP Music.app/Contents/Frameworks/LibVLC

License obligations:

- Include VLC/LibVLC COPYING files in the packaged app.
- Include third-party notices shipped by the official VLC package.
- Preserve a source-code access note pointing users to VideoLAN source distribution.
- Treat license review as a release blocker before public DMG distribution.

Release rule:

The app must not require users to install VLC media player. Release builds must load LibVLC from the app bundle first and fail with EngineUnavailable when the bundled runtime is missing or invalid.
