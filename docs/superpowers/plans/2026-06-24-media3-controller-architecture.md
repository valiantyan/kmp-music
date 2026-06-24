# Media3 Controller Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Android playback follow the official Media3 architecture by controlling `MusicPlaybackService` through `SessionToken` and `MediaController`.

**Architecture:** `MusicPlaybackService` owns `ExoPlayer` and `MediaSession`. The Android `AudioPlayerEngine` adapter becomes a `MediaController` client that sends commands to the session and translates controller/player callbacks into common `PlaybackEngineEvent`s. Common playback business rules stay in `PlaybackCoordinator`, with playback mode synchronized to the Media3 player so system media controls follow the same mode.

**Tech Stack:** Kotlin Multiplatform, AndroidX Media3 `MediaSessionService`, `MediaController`, `SessionToken`, ExoPlayer, Compose shared playback state.

---

### Task 1: Extend Common Engine Contract

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeAudioPlayerEngine.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt`

- [ ] Add `setPlaybackMode(playbackMode: PlaybackMode)` to `AudioPlayerEngine`.
- [ ] Implement the fake engine method as a deterministic no-op with last-mode tracking.
- [ ] Call `audioPlayerEngine.setPlaybackMode(...)` from `playSong`, `restoreSnapshot`, `cyclePlaybackMode`, and queue replacement paths.
- [ ] Update shuffle external-transition handling so MediaController-driven transitions keep common queue state coherent.

### Task 2: Convert Android Connector to MediaController Client

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt`

- [ ] Build a `SessionToken` with `ComponentName(context, MusicPlaybackService::class.java)`.
- [ ] Lazily create `MediaController.Builder(context, sessionToken).buildAsync()`.
- [ ] Await the controller from suspend methods and reuse the resolved controller for non-suspend methods.
- [ ] Map `PlayableMedia` to Media3 `MediaItem` in the client adapter.
- [ ] Add a `Player.Listener` to emit status, progress, current-media, ended, and failure events.
- [ ] Send button-preference updates through an official custom session command instead of a service registry call.

### Task 3: Simplify Service to Official MediaSession Host

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaSessionCallback.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommands.kt`

- [ ] Bind `MediaSession.Builder` directly to the real `ExoPlayer`.
- [ ] Remove the playback-engine service registry and `CoordinatorForwardingPlayer`.
- [ ] Keep `addSession(session)` and safe `removeSession(session)`.
- [ ] Handle the custom update-buttons command in `MediaSession.Callback` by calling `setMediaButtonPreferences`.
- [ ] Keep favorite and playback-mode custom buttons on the official Media3 custom-command path.

### Task 4: Verify

**Files:**
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt`
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/FakeAudioPlayerEngineTest.kt`

- [ ] Add focused common tests for playback-mode sync and external shuffle transition state.
- [ ] Run `./gradlew :composeApp:compileDebugKotlinAndroid`.
- [ ] Run `./gradlew :composeApp:desktopTest`.
- [ ] Run `git diff --check`.

### Self-Review

- Spec coverage: Covers official `SessionToken`/`MediaController` client, `MediaSessionService` host, command flow, state flow, playback-mode sync, and verification.
- Placeholder scan: No `TBD` or incomplete implementation steps remain.
- Type consistency: Uses existing project names plus the new `setPlaybackMode(playbackMode: PlaybackMode)` engine method consistently.
