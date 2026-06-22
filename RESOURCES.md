# KMP Music Audio Playback Resources

## Knowledge

- [Android Developers: Jetpack Media3 简介](https://developer.android.com/media/media3?hl=zh-cn)
  Official Android overview of Media3 components. Use for: Android `Player`, `ExoPlayer`, `MediaSession`, `MediaSessionService`, and background playback architecture.
- [Android Developers: Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer?hl=zh-cn)
  Official Android ExoPlayer page. Use for: local and streaming playback, playlists, events, supported formats, and Android playback behavior.
- [Android Developers: Access media files from shared storage](https://developer.android.com/training/data-storage/shared/media)
  Official Android storage guide for media collections. Use for: `MediaStore.Audio`, `ContentResolver` queries, `READ_MEDIA_AUDIO`, and media-store rescan/cache strategy.
- [Apple Developer: AVFoundation overview](https://developer.apple.com/av-foundation/)
  Official Apple overview of AVFoundation across iOS, iPadOS, macOS, tvOS, visionOS, and watchOS. Use for: Apple-native media playback capabilities.
- [Apple Documentation Archive: File System Basics](https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/FileSystemProgrammingGuide/FileSystemOverview/FileSystemOverview.html)
  Apple file-system guide explaining iOS sandbox boundaries. Use for: why iOS apps cannot scan arbitrary device storage like Android MediaStore.
- [Apple Documentation Archive: Document Picker Programming Guide](https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/DocumentPickerProgrammingGuide/Introduction/Introduction.html)
  Apple guide for accessing files outside an app sandbox through user choice. Use for: iOS file import/open flows, document providers, and security-scoped URLs.
- [Apple Developer: MPMediaLibrary](https://developer.apple.com/documentation/mediaplayer/mpmedialibrary)
  Official MediaPlayer API reference for the user's media library. Use for: deciding whether to support system Music library authorization and library-change observation.
- [Apple Developer: MPMediaQuery](https://developer.apple.com/documentation/mediaplayer/mpmediaquery)
  Official MediaPlayer API reference for querying media collections. Use for: optional iOS system music-library scanning and its limits.
- [Apple Developer: NSAppleMusicUsageDescription](https://developer.apple.com/documentation/bundleresources/information_property_list/nsapplemusicusagedescription)
  Official Info.plist privacy key reference. Use for: iOS media-library permission prompts when using MediaPlayer library APIs.
- [Apple Developer: UTType.audio](https://developer.apple.com/documentation/uniformtypeidentifiers/uttype/audio)
  Official Uniform Type Identifiers reference for audio content. Use for: filtering iOS document picker selections to audio files.
- [Apple Documentation Archive: Building a Basic Playback App](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/MediaPlaybackGuide/Contents/Resources/en.lproj/GettingStarted/GettingStarted.html)
  Apple playback guide showing AVKit and AVFoundation setup for iOS, tvOS, and macOS. Use for: seeing `AVPlayer` as the basic Apple playback object.
- [Apple Documentation Archive: Configuring Audio Settings for iOS and tvOS](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/MediaPlaybackGuide/Contents/Resources/en.lproj/ConfiguringAudioSettings/ConfiguringAudioSettings.html)
  Apple guide to `AVAudioSession` category and background audio configuration. Use for: iOS background playback and silent-switch behavior.
- [Apple Developer: MPNowPlayingInfoCenter](https://developer.apple.com/documentation/mediaplayer/mpnowplayinginfocenter)
  Apple API reference for lock-screen and Control Center now-playing metadata. Use for: title, artist, artwork, duration, and elapsed time.
- [Apple Developer: MPRemoteCommandCenter](https://developer.apple.com/documentation/mediaplayer/mpremotecommandcenter)
  Apple API reference for receiving remote play, pause, next, previous, and seek commands. Use for: iOS lock-screen, headset, and external media controls.
- [Kotlin Documentation: Swift/Objective-C interop](https://kotlinlang.org/docs/native-objc-interop.html)
  Official Kotlin/Native interop guide. Use for: calling Apple Objective-C frameworks from `iosMain` or deciding when to wrap Apple APIs in Swift.
- [Microsoft Learn: Media Foundation](https://learn.microsoft.com/en-us/windows/win32/medfound/microsoft-media-foundation-sdk)
  Official Windows multimedia platform documentation. Use for: native Windows playback if the Desktop target later moves toward JNI/JNA or a Windows-specific implementation.
- [Microsoft Learn: Media players for Windows apps](https://learn.microsoft.com/en-us/windows/apps/develop/ui/controls/media-playback)
  Official Windows App SDK and WinUI media playback guidance. Use for: understanding `MediaPlayerElement`, transport controls, and system media controls in native Windows apps.
- [Microsoft Learn: System Media Transport Controls](https://learn.microsoft.com/en-us/windows/apps/develop/media-playback/system-media-transport-controls)
  Official Windows system media controls documentation. Use for: understanding Windows desktop-style play/pause/next/previous metadata and background audio integration.
- [Oracle Java SE 17: `Files`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/Files.html)
  Official Java API reference for walking file trees and probing content types. Use for: Compose Desktop folder scanning with `Files.walk`, readable-file checks, and MIME hints.
- [Oracle Java SE 17: `JFileChooser`](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/swing/JFileChooser.html)
  Official Swing file chooser reference. Use for: JVM Desktop folder selection before recursively scanning a user-selected music folder.
- [Spotify Support: Keyboard shortcuts](https://support.spotify.com/us/article/keyboard-shortcuts/)
  Official Spotify desktop shortcut list. Use for: desktop product pattern references such as app-level play/pause, navigation shortcuts, queue access, and now-playing view toggles.
- [GStreamer: What is GStreamer?](https://gstreamer.freedesktop.org/documentation/application-development/introduction/gstreamer.html)
  Official GStreamer introduction. Use for: pipeline-based desktop playback and plugin-based codec support across platforms.
- [GStreamer: Basic tutorial 1](https://gstreamer.freedesktop.org/documentation/tutorials/basic/hello-world.html)
  Official tutorial showing `playbin` and pipeline state. Use for: understanding why GStreamer can be powerful but integration-heavy.
- [vlcj project](https://capricasoftware.co.uk/projects/vlcj)
  Primary vlcj project page. Use for: Java/JVM desktop playback via LibVLC in Compose Desktop, plus licensing and packaging considerations.

## Wisdom (Communities)

- [Apple Developer Forums: AVFoundation](https://developer.apple.com/forums/tags/avfoundation)
  Use for: Apple playback edge cases, interruptions, background audio, and App Store behavior.
- [AndroidX Media GitHub](https://github.com/androidx/media)
  Use for: Media3 issues, samples, changelogs, and migration questions.
- [Kotlin Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up)
  Use for: Kotlin Multiplatform interop and source-set design questions.
- [GStreamer Discourse](https://discourse.gstreamer.org/)
  Use for: pipeline, packaging, plugin, and codec troubleshooting.

## Gaps

- We still need a project-specific spike to decide whether Desktop should use simple JavaFX media, vlcj/LibVLC, GStreamer, or native bridges. That choice depends on codec requirements, license posture, installer size, and how much OS-level media-key integration the first release needs.
