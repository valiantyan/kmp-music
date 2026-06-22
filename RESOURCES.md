# KMP Music Audio Playback Resources

## Knowledge

- [Android Developers: Jetpack Media3 简介](https://developer.android.com/media/media3?hl=zh-cn)
  Official Android overview of Media3 components. Use for: Android `Player`, `ExoPlayer`, `MediaSession`, `MediaSessionService`, and background playback architecture.
- [Android Developers: Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer?hl=zh-cn)
  Official Android ExoPlayer page. Use for: local and streaming playback, playlists, events, supported formats, and Android playback behavior.
- [Apple Developer: AVFoundation overview](https://developer.apple.com/av-foundation/)
  Official Apple overview of AVFoundation across iOS, iPadOS, macOS, tvOS, visionOS, and watchOS. Use for: Apple-native media playback capabilities.
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
