# Handoff: Android Playback Bugfix Handoff

## Session Metadata
- Created: 2026-06-24 10:51:27
- Project: /Users/yanhao/Desktop/demo/kmp-music
- Branch: main
- Session duration: about 1 hour

### Recent Commits (for context)
  - 4b114bd 修复 Android 播放恢复与快照终审问题
  - 4efd8fc 防止界面重建重复恢复播放快照
  - 483e97e 修复 Android 播放恢复与通知补发
  - 8fc3cde 连接 Android 播放运行时
  - 803147b 新增 Android 自定义播放通知

## Handoff Chain

- **Continues from**: [2026-06-23-182533-android-playback-plan-review.md](./2026-06-23-182533-android-playback-plan-review.md)
  - Previous title: Android Playback Plan Cross-Review
- **Supersedes**: None

> Review the previous handoff for the Android playback design and implementation-plan context before continuing.

## Current State Summary

This session fixed three Android playback bugs reported against `docs/superpowers/specs/2026-06-23-android-playback-design.md` and `docs/superpowers/plans/2026-06-23-android-playback-implementation.md`: mini player progress was fake, Android playback could stop after two consecutive failed songs instead of continuing until the documented three-failure threshold, and the custom media notification looked unlike the QQ Music-style reference. The fixes are implemented but not committed. Shared tests, Android Kotlin compile, debug APK build, install, and cold launch probe passed. The remaining risk is visual/manual notification verification after starting real playback and expanding the notification shade on device.

## Codebase Understanding

## Architecture Overview

Playback state flows from Android Media3/ExoPlayer through `Media3AudioPlayerEngine` events into common `PlaybackCoordinator`, then into `PlaybackRepository`, `MusicAppController`, and finally `MusicAppUiState`. UI, notification actions, and MediaSession/system controls are expected to return to the same controller/coordinator path instead of mutating ExoPlayer or UI state directly. The mini player is part of common global chrome in `MusicApp.kt`; the Android notification is custom `RemoteViews` built by `AndroidPlaybackNotificationController` and refreshed by `AndroidPlaybackRuntime` through `PlaybackServiceConnector` and `MusicPlaybackService`.

## Critical Files

| File | Purpose | Relevance |
|------|---------|-----------|
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt | Shared app shell and global mini player UI | Mini progress now uses real `playbackPositionMs` / `playbackDurationMs` instead of a hardcoded visual fraction |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/Media3AudioPlayerEngine.kt | Android `AudioPlayerEngine` implementation around ExoPlayer | Error recovery now prepares the player after Media3 enters idle, allowing documented consecutive-failure skip rules to keep advancing |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackRuntime.kt | Bridges common controller state to Android notification/system command runtime | Now passes true playback progress/duration into notification refresh |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt | Lazy service connector and notification command cache | Notification state now carries progress/duration through pending command replay |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt | MediaSessionService host and foreground notification lifecycle | Notification creation now receives progress/duration |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackNotificationController.kt | Custom `RemoteViews` notification builder | Expanded/collapsed notification now sets cover, actions, progress, and time labels |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackNotificationAssets.kt | Android notification helper for Compose asset cover decoding and time/progress formatting | New file; keeps RemoteViews-specific asset/progress logic out of the controller |
| composeApp/src/androidMain/res/layout/notification_playback_expanded.xml | Expanded custom notification layout | Reworked toward reference style: cover/title/artist, progress bar, times, five actions |
| composeApp/src/androidMain/res/layout/notification_playback_collapsed.xml | Collapsed custom notification layout | Tuned dimensions/text/action icons and now receives a cover |

## Key Patterns Discovered

- Root fixes should keep state single-sourced in common `MusicAppUiState` and `PlaybackCoordinator`; avoid page-local or notification-local derived playback state.
- `RemoteViews` cannot use Compose `Painter` or common resources directly. Compose Multiplatform resources are copied to Android assets, so notification cover decoding reads `composeResources/kmpmusic.composeapp.generated.resources/drawable/*.png`.
- Media3 player errors can leave ExoPlayer in `STATE_IDLE`. After failure, `seekToDefaultPosition()` plus `play()` is not enough; prepare must happen before retry/skip playback can resume.
- Existing untracked `docs/superpowers/plans/` and older `handoff/` files predate this handoff. Do not clean them unless the user explicitly asks.

## Work Completed

## Tasks Finished

- [x] Investigated mini player progress root cause: hardcoded `fillMaxWidth(fraction = 0.44f)` in `MusicApp.kt`.
- [x] Rewired mini player progress to use true playback state and render a real read-only progress bar.
- [x] Investigated consecutive failure behavior and identified Android engine recovery gap after Media3 errors.
- [x] Added ExoPlayer prepare-on-error-recovery behavior in `Media3AudioPlayerEngine`.
- [x] Reworked Android custom notification data flow so progress/duration reach `RemoteViews`.
- [x] Reworked expanded notification layout to include cover, progress, elapsed/duration time labels, and five fixed controls.
- [x] Added Android notification drawables for previous/play/pause/next, cover placeholder, and progress bar.
- [x] Verified shared tests, Android compile, debug APK build, install, and app launch.

## Files Modified

| File | Changes | Rationale |
|------|---------|-----------|
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt | Added `playbackPositionMs` and `playbackDurationMs` through `BottomChrome` into `MiniPlayer`; replaced hardcoded progress fraction with `calculateMiniPlayerProgressFraction` | Fix mini player fake progress and keep it synchronized with player page/common state |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/Media3AudioPlayerEngine.kt | Tracks `shouldPrepareAfterError`; stops progress polling on error; prepares before play/skip when player is idle or recovering from error | Ensure two failed songs do not strand ExoPlayer before the documented three-failure stop threshold |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackRuntime.kt | Passes `playbackPositionMs` and `playbackDurationMs` from `MusicAppUiState` to connector | Notification progress must be driven by the same state as playback page/mini player |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt | Adds progress/duration to cached notification state and service delivery | Preserve latest notification progress even when service starts lazily and pending notification commands are replayed |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt | Adds progress/duration parameters to notification creation | Keep foreground notification lifecycle intact while carrying richer state |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackNotificationController.kt | Sets cover images, progress bar, elapsed/duration labels, app-owned playback icons, notification category/priority/visibility | Fix empty cover and improve custom notification toward QQ Music reference |
| composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackNotificationAssets.kt | New helper for cover bitmap cache, progress conversion, and `mm:ss` formatting | Isolate Android asset decoding and formatting from notification assembly |
| composeApp/src/androidMain/res/layout/notification_playback_collapsed.xml | Adjusted height/padding/text styling/icon sizes | Improve collapsed notification density and make cover/text/actions coherent |
| composeApp/src/androidMain/res/layout/notification_playback_expanded.xml | Rebuilt layout with cover/title/artist, progress/time row, and five weighted action buttons | Match the intended expanded media notification structure |
| composeApp/src/androidMain/res/drawable/ic_notification_previous.xml | New vector icon | Use app-owned action icon instead of Android built-in media drawable |
| composeApp/src/androidMain/res/drawable/ic_notification_next.xml | New vector icon | Use app-owned action icon instead of Android built-in media drawable |
| composeApp/src/androidMain/res/drawable/ic_notification_play.xml | New vector icon | Use app-owned action icon with consistent style |
| composeApp/src/androidMain/res/drawable/ic_notification_pause.xml | New vector icon | Use app-owned action icon with consistent style |
| composeApp/src/androidMain/res/drawable/ic_notification_cover_placeholder.xml | New vector placeholder | Avoid empty cover if asset decoding fails |
| composeApp/src/androidMain/res/drawable/notification_progress_drawable.xml | New layer-list progress drawable | Gives expanded notification a stable dark progress track |

## Decisions Made

| Decision | Options Considered | Rationale |
|----------|-------------------|-----------|
| Drive mini progress from `MusicAppUiState` | Keep visual fraction, compute locally from song duration string, use real state | Real state is already available and is the same source used by player page, so this removes the fake UI without creating a second truth |
| Fix consecutive failures in Android engine rather than common failure counters | Change `PlaybackCoordinator` failure threshold logic, alter fake engine tests, prepare ExoPlayer after error | Shared coordinator tests already cover the three-failure rule; the observed Android symptom is consistent with Media3 remaining idle after error |
| Decode notification covers from Android assets copied by Compose resources | Duplicate PNGs into `androidMain/res/drawable`, leave cover empty, read Compose assets | Reading assets avoids duplicating image files and keeps domain `CoverArt` mapping consistent with common UI |
| Keep notification actions routed through existing broadcast/controller bridge | Let notification buttons directly control ExoPlayer, add separate notification controller state, reuse `PlaybackNotificationActionReceiver` | Existing architecture requires UI/notification/system commands to return to coordinator, preserving queue/mode/favorite semantics |
| Keep manual notification visual verification as residual risk | Attempt full notification shade automation, only run build/install/launch probe | Playback and notification expansion require device media permission and real song playback; build/install/launch proves package/runtime viability but not final visual layout |

## Pending Work

## Immediate Next Steps

1. On the connected Android device, grant audio/notification permissions if needed, scan local music, play a real song, pull and expand the notification shade, and visually verify it matches the QQ Music-style reference closely enough.
2. Re-test consecutive failures on Android using two or three invalid/unreadable local media entries; confirm playback attempts the third song and only stops at the documented threshold.
3. If visual/device behavior is acceptable, inspect `git status --short`, stage the exact playback files plus this handoff if desired, and commit with a concise Chinese commit message.

## Blockers/Open Questions

- [ ] Manual visual verification: expanded notification was not screenshot-verified after real playback. Needs a real playing song and human inspection of the notification shade.
- [ ] Failure threshold device verification: shared tests pass and Android engine recovery was fixed, but the actual two-bad-song Android scenario still needs a device reproduction.
- [ ] Notification layout height: custom expanded layout uses `224dp`. Android/OEM notification surfaces may clamp or decorate custom layouts differently; adjust after screenshot if needed.

## Deferred Items

- Quick Settings and lock-screen media card customization remains explicitly out of scope per the playback design doc.
- iOS and Desktop real playback remain out of scope for this Android playback fix.
- No commit was created in this session because the user asked for handoff after the fix; next agent should confirm whether to stage/commit.

## Context for Resuming Agent

## Important Context

The user reported three concrete bugs after Android playback implementation: mini player progress was fake, two consecutive playback failures could stop playback despite the spec requiring three consecutive failures before stopping, and the media notification visual style was wrong compared with a QQ Music screenshot. The root causes addressed here were: `MiniPlayer` used a hardcoded `0.44f` bar, Android ExoPlayer needed re-prepare after an error before skipping/retrying, and notification RemoteViews did not receive cover/progress/time data. Do not “fix” these by page-local patches or direct ExoPlayer notification controls; the project instructions require root fixes through shared state, controller/coordinator boundaries, and Android platform adapters.

## Assumptions Made

- The connected device is the same previously used Android 12 `PFGM00` device shown by Gradle install output.
- The three-failure rule is already correctly represented by common `PlaybackCoordinator` tests; Android-specific failure was due to Media3 recovery state.
- Compose resources copied to Android assets are available in debug/release packages at `composeResources/kmpmusic.composeapp.generated.resources/drawable/<file>.png`.
- The reference notification style should be approximated inside the app-owned custom foreground notification; Quick Settings and lock-screen system cards are still not acceptance targets.

## Potential Gotchas

- `git diff --name-only` does not list untracked new helper/drawable files. Always check `git status --short` before staging.
- `docs/superpowers/plans/` and several older `handoff/*.md` files were already untracked before this handoff. Do not delete or stage them casually.
- `AndroidPlaybackNotificationAssets` reads from assets, not Android `R.drawable` PNGs. If cover does not show in release, verify `copyReleaseComposeResourcesToAndroidAssets` behavior.
- `Media3AudioPlayerEngine.prepareIfNeeded()` prepares if the player is idle. If future code intentionally keeps an idle prepared queue without playing, revisit this behavior.
- The notification uses `NotificationCompat.DecoratedCustomViewStyle`; OEM skins may still add system header/chrome around the custom content.

## Environment State

### Tools/Services Used

- `./gradlew :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid` passed.
- `./gradlew :composeApp:assembleDebug` passed.
- `./gradlew :composeApp:installDebug` passed and installed on `PFGM00 - 12`.
- `adb shell am start -W -n com.yanhao.kmpmusic/.MainActivity` returned `Status: ok`, `LaunchState: COLD`.
- `adb shell pidof com.yanhao.kmpmusic` returned a live PID immediately after launch probe.

### Active Processes

- No local dev server or watcher was started.
- The Android app process was running on the connected device immediately after launch probe; it may no longer be active by the time this handoff is resumed.

### Environment Variables

- No environment variables were read or required.

## Related Resources

- docs/superpowers/specs/2026-06-23-android-playback-design.md
- docs/superpowers/plans/2026-06-23-android-playback-implementation.md
- handoff/2026-06-23-182533-android-playback-plan-review.md
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt
- composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt
- composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/Media3AudioPlayerEngine.kt
- composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackNotificationController.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt

---

**Security Reminder**: Validation completed after filling this handoff; rerun it if the file is edited again.
