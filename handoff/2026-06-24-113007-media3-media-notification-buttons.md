# Handoff: Media3 Media Notification Buttons

## Session Metadata
- Created: 2026-06-24 11:30:07
- Project: /Users/yanhao/Desktop/demo/kmp-music
- Branch: main
- Session duration: about 45 minutes

### Recent Commits (for context)
  - 16832d9 修复 Android 播放进度、失败恢复与通知样式
  - 4b114bd 修复 Android 播放恢复与快照终审问题
  - 4efd8fc 防止界面重建重复恢复播放快照
  - 483e97e 修复 Android 播放恢复与通知补发
  - 8fc3cde 连接 Android 播放运行时

## Handoff Chain

- **Continues from**: [2026-06-24-105127-android-playback-bugfix-handoff.md](./2026-06-24-105127-android-playback-bugfix-handoff.md)
- **Supersedes**: None

> Read the previous handoff for the Android playback bugfix context. This handoff captures the follow-up decision to stop custom RemoteViews notification rendering and move media notification buttons to the official Media3 system media controls path.

## Current State Summary

The user challenged the earlier assumption that Media3 could not meaningfully customize media notification buttons and pointed to the official Android Media3 documentation for `CommandButton`, `SessionCommand`, and `setMediaButtonPreferences`. After discussion, the agreed product/technical direction is: do not try to visually clone QQ Music; let Android/System UI draw the system media notification, and declare KMP Music's button preferences through Media3 official APIs. The migration is implemented but not committed. `:composeApp:compileDebugKotlinAndroid` and `git diff --check` passed. Remaining work is device verification, optional broader build/tests, then staging/committing this migration.

## Codebase Understanding

## Architecture Overview

Android real playback still flows through `MusicPlaybackService` -> `Media3AudioPlayerEngine` -> common `PlaybackCoordinator` / `MusicAppController`. Standard system media commands are intercepted by `CoordinatorForwardingPlayer` and routed back to the shared controller path instead of mutating ExoPlayer directly. This session changes only the Android notification/button surface: custom app-drawn notification layouts and broadcast intents were removed, while Media3 `MediaSession.Callback` now exposes official media button preferences and handles custom commands for favorite and playback mode.

## Critical Files

| File | Purpose | Relevance |
|------|---------|-----------|
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommands.kt | Defines Media3 notification button preferences and custom command dispatch | New central place for favorite, previous, play/pause, next, and playback-mode buttons |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaSessionCallback.kt | MediaSession callback for notification controller connections and custom commands | Adds `isMediaNotificationController` handling and `onCustomCommand` dispatch |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt | MediaSessionService host for ExoPlayer and session lifecycle | Now installs the Media3 callback and refreshes button preferences rather than building a custom notification |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/Media3AudioPlayerEngine.kt | Android ExoPlayer-backed engine | Now populates `MediaItem.MediaMetadata` so system notifications can show title, artist, album, duration, and artwork |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaMetadataAssets.kt | Reads Compose resource artwork bytes for Media3 metadata | Replaces the old RemoteViews bitmap helper with metadata-oriented artwork bytes |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackRuntime.kt | Connects shared controller to Android playback service and commands | Now implements `PlaybackMediaButtonActions` and attaches `PlaybackMediaCommandDispatcher` |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt | Lazy service connector and state replay bridge | Now refreshes Media3 media button preferences instead of replaying custom notification render state |
| composeApp/src/androidMain/AndroidManifest.xml | Android component declaration | Removed the old notification broadcast receiver |

## Key Patterns Discovered

- Media3 1.8.0 includes `CommandButton`, slot constants, `SessionCommand`, `SessionCommands`, `SessionResult`, and `MediaSession.ConnectionResult.AcceptedResultBuilder`.
- For custom notification buttons, declare buttons through `CommandButton` and `setMediaButtonPreferences`; do not build duplicate notification buttons with `RemoteViews`.
- Standard playback buttons should use `Player` commands so they continue through `CoordinatorForwardingPlayer`; app-specific actions such as favorite and playback mode should use custom `SessionCommand`.
- System media notification text and artwork come from `MediaItem.MediaMetadata`; the engine must populate metadata when it builds Media3 queue items.
- The UI surface is system-owned: slot preferences can influence placement, but System UI/OEM policy can still reorder, hide, or overflow buttons.

## Work Completed

## Tasks Finished

- [x] Committed the previous Android playback bugfix as `16832d9 修复 Android 播放进度、失败恢复与通知样式`.
- [x] Re-read the handoff chain and verified the current branch/worktree state.
- [x] Consulted official Android Media3 docs for custom playback commands and background media notification behavior.
- [x] Implemented Media3 `CommandButton` button preferences for favorite, previous, play/pause, next, and playback mode.
- [x] Added `SessionCommand` handling for favorite and playback mode, routed through the existing controller-backed command dispatcher.
- [x] Added Media3 metadata population for title, artist, album, duration, and artwork.
- [x] Removed the old custom RemoteViews notification controller, broadcast receiver, layouts, and notification-only drawables from the production source tree.
- [x] Ran Android Kotlin compile and whitespace validation.

## Files Modified

| File | Changes | Rationale |
|------|---------|-----------|
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommands.kt | New file defining Media3 button preferences, custom commands, command dispatcher, and icon/slot mapping | Keep official media button logic focused and separate from service lifecycle |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaSessionCallback.kt | New file handling notification-controller connection and `onCustomCommand` | Keep MediaSession callback logic out of the service file and route commands through shared controller |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaMetadataAssets.kt | New file reading artwork bytes from Compose resource assets | System media notification consumes Media3 metadata artwork, not RemoteViews bitmaps |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt | Removed custom notification controller usage; installs `AndroidPlaybackMediaSessionCallback`; refreshes `setMediaButtonPreferences` | Let Media3/System UI own notification rendering and foreground notification lifecycle |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/Media3AudioPlayerEngine.kt | `MediaItem` creation now includes `MediaMetadata` and artwork data | System media card needs metadata to render song details correctly |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackRuntime.kt | Switched from old notification actions dispatcher to `PlaybackMediaButtonActions` / `PlaybackMediaCommandDispatcher` | Custom Media3 commands must return to the same shared controller semantics |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt | Renamed notification refresh path to media button preference refresh; simplified cached state | Connector now replays button state, not app-drawn notification state |
| composeApp/src/androidMain/AndroidManifest.xml | Removed old notification receiver declaration | Broadcast receiver is obsolete after moving custom buttons into Media3 session commands |

Removed legacy files: the previous RemoteViews notification controller/assets, notification action receiver, two custom notification XML layouts, and notification-specific drawable XML resources were deleted because system Media3 notification rendering replaces them.

## Decisions Made

| Decision | Options Considered | Rationale |
|----------|-------------------|-----------|
| Use Media3 official media notification buttons | Continue polishing RemoteViews, clone QQ Music visually, use `CommandButton`/`SessionCommand` | User clarified UI should be system-rendered and buttons should follow official docs; Media3 path is the root fix |
| Keep standard controls as Player commands | Make all buttons custom `SessionCommand`, leave previous/next as broadcast intents, use Player commands | Player commands keep compatibility with system/BT/lockscreen controls and already flow through `CoordinatorForwardingPlayer` |
| Use custom SessionCommand only for favorite and playback mode | Add favorite/mode as RemoteViews intents, remove them entirely, custom SessionCommand | These are app-specific commands, so official custom command mechanism is the right boundary |
| Populate MediaMetadata in the engine | Let UI state feed notification text, leave metadata empty, populate metadata from `PlayableMedia` | System notification consumes Media3 queue metadata; `PlayableMedia` already has title/artist/album/duration/cover |
| Delete old notification layouts/resources | Keep as fallback, leave unused resources, remove now | Two notification systems create confusion and risk regressions; compile/search verifies old refs are gone |

## Pending Work

## Immediate Next Steps

1. On a connected Android device, install/run the app, play a real local song, expand the notification shade, and verify the system media card appears with title/artist/artwork/progress and Media3 buttons.
2. Test button behavior from the notification: favorite toggles shared liked state, previous/play-pause/next follow queue semantics, playback mode cycles, and no command bypasses common controller/coordinator rules.
3. If device behavior is acceptable, stage only the Media3 migration files plus this handoff if desired, then commit with a concise Chinese message such as `按 Media3 官方方式接入系统媒体通知按钮`.

## Blockers/Open Questions

- [ ] Manual notification verification is still pending because this session only ran compile/static checks.
- [ ] Actual System UI button placement is not guaranteed; slot preferences are advisory and Android/OEM surfaces may overflow favorite or playback mode.
- [ ] Need decide whether to commit this handoff with the migration or leave handoff files untracked as local session notes.

## Deferred Items

- Full debug APK build, install, and screenshot capture are deferred to the next device-verification pass.
- Quick Settings and lock-screen UI pixel-level customization remains out of scope because those surfaces are system-rendered.
- iOS/Desktop playback changes remain out of scope for this Android notification migration.

## Context for Resuming Agent

## Important Context

The current migration intentionally abandons app-drawn media notification UI. Do not restore the deleted RemoteViews layouts/resources to chase QQ Music pixels. The target is Media3 official behavior: System UI draws the media notification, while the app provides metadata, standard Player commands, and custom SessionCommands. Standard commands still pass through `CoordinatorForwardingPlayer`, and custom favorite/mode commands go through `PlaybackMediaCommandDispatcher` into `AndroidPlaybackRuntime`, which calls `MusicAppController`. The migration is uncommitted but Android Kotlin compile passed.

## Assumptions Made

- The user wants the notification to look like the system media card on the device, not a custom clone of QQ Music.
- Media3 1.8.0 APIs in the Gradle cache match the official docs used here.
- Compose Multiplatform drawable assets are available in Android assets at the same copied path used by the previous notification helper.
- `PlayableMedia` metadata is the correct single source for system notification title/artist/album/duration/artwork.
- Existing shared controller/coordinator behavior for favorites, playback mode, and queue navigation should remain the behavioral source of truth.

## Potential Gotchas

- `git diff --stat` does not show untracked new files; always use `git status --short` before staging.
- The deleted legacy files no longer exist; if a validator or reviewer asks about them, the deletion is intentional, not accidental cleanup.
- `setMediaButtonPreferences` controls preference/availability, not exact rendering. System UI may choose not to show all five buttons.
- If notification artwork is missing, first check whether Compose resource assets are packaged at `composeResources/kmpmusic.composeapp.generated.resources/drawable/`.
- `MusicPlaybackService.clearMediaNotification()` currently clears player media items and calls `stopSelf()`. If this causes service/session lifecycle surprises on device, investigate MediaSessionService default notification lifecycle before adding app-owned notifications back.
- The old `PlaybackNotificationDispatcher` no longer exists; search for `PlaybackMediaCommandDispatcher` instead.

## Environment State

## Tools/Services Used

- Skill used: `session-handoff`.
- Official docs consulted: Android Media3 control playback commands and MediaSessionService background playback docs.
- Verification commands passed: `./gradlew :composeApp:compileDebugKotlinAndroid` and `git diff --check`.
- Static search passed for old notification implementation terms: `RemoteViews`, `DecoratedCustomViewStyle`, `NotificationCompat`, `PlaybackNotificationActionReceiver`, `notification_playback_`, and `ic_notification_`.

## Active Processes

- No local dev server, Gradle watcher, emulator, or adb session was left running by this handoff step.

## Environment Variables

- No environment variables were required or inspected.

## Related Resources

- handoff/2026-06-24-105127-android-playback-bugfix-handoff.md
- composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommands.kt
- composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaSessionCallback.kt
- composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaMetadataAssets.kt
- composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt
- composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/Media3AudioPlayerEngine.kt
- composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackRuntime.kt
- composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt
- composeApp/src/androidMain/AndroidManifest.xml
- https://developer.android.com/media/media3/session/control-playback?hl=zh-cn#commands
- https://developer.android.com/media/media3/session/background-playback

---

**Security Reminder**: Validation completed after filling this handoff; rerun it if this file is edited again.
