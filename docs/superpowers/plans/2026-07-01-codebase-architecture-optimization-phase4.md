# Codebase Architecture Optimization Phase 4 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split Android and Desktop platform playback adapters into focused session, runtime, command, codec, reducer, ticker, and factory files without changing playback behavior or shared controller APIs.

**Architecture:** Keep `AndroidPlaybackSession` and `DesktopPlaybackSession` as the only platform session facades. Android Media3 command definitions, button construction, button-state codec, and command handling move behind focused `playback` modules; Desktop session wiring and vlcj engine internals move behind focused factories and command-loop collaborators. iOS remains out of implementation scope except for verification that no new iOS playback abstraction or common `PlatformSession` is introduced.

**Tech Stack:** Kotlin Multiplatform 2.0.21, Compose Multiplatform 1.7.3, AGP 8.13.2, Android Media3, Room, kotlinx.coroutines, vlcj, Kotlin test, Gradle `:composeApp`.

---

## Source Spec

Implement this plan from:

- `docs/superpowers/specs/2026-07-01-codebase-architecture-optimization-phase4-design.md`

Do not touch `prototypes/kmp-music-hi-fi`. Do not modify third-stage UI files as part of this phase.

## File Structure

### Android Session Files

- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSession.kt`
  - Keep public object facade only: `bootstrap`, `controller`, `attachPlaybackContext`, `attachLocalMusicScanner`, `attachPermissionSettingsOpener`, `ensurePlaybackSnapshotRestoreRequested`, `clearUiBindings`.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackControllerFactory.kt`
  - New Android controller dependency factory.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidUiBindingRegistry.kt`
  - New Activity-scoped scanner and permission-opener proxy registry.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSessionRuntime.kt`
  - New Android process runtime for scope, service connector, playback runtime, controller holder, context attach, and restore-once state.

### Android Media3 Command Files

- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommands.kt`
  - Delete after moving declarations; `AndroidPlaybackMediaButtons` must not remain.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommandActions.kt`
  - Move `PlaybackMediaButtonActions` and `PlaybackMediaCommandDispatcher`.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommandCatalog.kt`
  - New Media3 custom action and `SessionCommand` catalog.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MediaButtonStateCodec.kt`
  - New `MediaButtonState` to `Bundle` encoder/decoder.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaButtonFactory.kt`
  - New command-button factory and playback-mode display/icon mapping.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaCommandHandler.kt`
  - New custom command dispatcher returning `SessionResult` codes.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaSessionCallback.kt`
  - Update imports/calls to catalog, codec, and handler.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaNotificationProvider.kt`
  - Update imports/calls to catalog and button factory.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MediaButtonStateSender.kt`
  - Update imports/calls to catalog and codec.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt`
  - Update imports/calls to button factory.
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackRuntime.kt`
  - Update dispatcher import if needed.

### Android Unit Test Files

- `composeApp/build.gradle.kts`
  - Add explicit `androidUnitTest` dependencies.
- `composeApp/src/androidUnitTest/kotlin/com/yanhao/kmpmusic/playback/MediaButtonStateCodecTest.kt`
  - New codec round-trip and invalid payload tests.
- `composeApp/src/androidUnitTest/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommandCatalogTest.kt`
  - New action classification tests.
- `composeApp/src/androidUnitTest/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaCommandHandlerTest.kt`
  - New handler action routing and invalid-state tests.

### Desktop Session Files

- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt`
  - Keep public object facade only.
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackControllerFactory.kt`
  - Move `createDesktopPlaybackController`.
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSessionRuntime.kt`
  - Move `DesktopPlaybackSessionRuntime`.
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopAudioRuntimeFactory.kt`
  - New libVLC runtime resolution and adapter/engine factory.
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSessionTest.kt`
  - Update imports if moved declarations need explicit imports.

### Desktop Engine Files

- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt`
  - Keep `DesktopVlcjAudioPlayerEngine : AudioPlayerEngine` public entry and delegate internals.
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopPlaybackCommand.kt`
  - Move `EngineCommand`.
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopPlaybackControlIntent.kt`
  - Move `PlaybackControlIntent`.
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopPlaybackEngineState.kt`
  - New mutable engine state holder.
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopSetQueueAckTracker.kt`
  - New pending `setQueue` ack tracker.
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopMediaSourceMapper.kt`
  - New `PlayableMedia` to desktop media URI mapper.
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopProgressTicker.kt`
  - New progress polling collaborator that only sends `ProgressTick` commands.
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopAdapterEventReducer.kt`
  - New pure adapter-event reducer.
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopPlaybackCommandLoop.kt`
  - New command loop owner for serial state mutation.
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngineTest.kt`
  - Keep existing behavior tests; add focused tests only where a new collaborator can be tested cleanly.

## Task 1: Add Android Media Command Tests First

**Files:**
- Modify: `composeApp/build.gradle.kts`
- Create: `composeApp/src/androidUnitTest/kotlin/com/yanhao/kmpmusic/playback/MediaButtonStateCodecTest.kt`
- Create: `composeApp/src/androidUnitTest/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommandCatalogTest.kt`
- Create: `composeApp/src/androidUnitTest/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaCommandHandlerTest.kt`

- [ ] **Step 1: Add Android unit test source-set dependencies**

In `composeApp/build.gradle.kts`, inside the existing `kotlin { sourceSets { } }` block, add the source set declaration beside the existing source-set variables:

```kotlin
val androidUnitTest by getting
```

Inside the same `sourceSets` block, add:

```kotlin
androidUnitTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 2: Create codec tests**

Create `composeApp/src/androidUnitTest/kotlin/com/yanhao/kmpmusic/playback/MediaButtonStateCodecTest.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaButtonStateCodecTest {
    @Test
    fun updateButtonArgsRoundTripMediaButtonState(): Unit {
        val state = MediaButtonState(
            shouldShowPauseButton = true,
            isFavorite = true,
            playbackMode = PlaybackMode.Shuffle,
            playbackStatus = PlaybackStatus.Playing,
            hasActivePlaybackSession = true,
        )

        val args = MediaButtonStateCodec.createUpdateButtonsArgs(state = state)

        assertEquals(expected = state, actual = MediaButtonStateCodec.resolveUpdateButtonsState(args = args))
    }

    @Test
    fun invalidPlaybackModeReturnsNull(): Unit {
        val args = MediaButtonStateCodec.createUpdateButtonsArgs(
            state = MediaButtonState(
                shouldShowPauseButton = true,
                isFavorite = false,
                playbackMode = PlaybackMode.LoopAll,
                playbackStatus = PlaybackStatus.Playing,
                hasActivePlaybackSession = true,
            ),
        )
        args.putString("playback_mode", "BrokenMode")

        assertNull(actual = MediaButtonStateCodec.resolveUpdateButtonsState(args = args))
    }

    @Test
    fun invalidPlaybackStatusReturnsNull(): Unit {
        val args = MediaButtonStateCodec.createUpdateButtonsArgs(
            state = MediaButtonState(
                shouldShowPauseButton = false,
                isFavorite = false,
                playbackMode = PlaybackMode.LoopOne,
                playbackStatus = PlaybackStatus.Paused,
                hasActivePlaybackSession = true,
            ),
        )
        args.putString("playback_status", "BrokenStatus")

        assertNull(actual = MediaButtonStateCodec.resolveUpdateButtonsState(args = args))
    }
}
```

- [ ] **Step 3: Create catalog tests**

Create `composeApp/src/androidUnitTest/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommandCatalogTest.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import androidx.media3.common.util.UnstableApi
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@UnstableApi
class PlaybackMediaCommandCatalogTest {
    @Test
    fun recognizesUpdateButtonsCommand(): Unit {
        val command = PlaybackMediaCommandCatalog.updateButtonsCommand()

        assertTrue(
            actual = PlaybackMediaCommandCatalog.isUpdateButtonsCommand(
                customAction = command.customAction,
            ),
        )
        assertFalse(
            actual = PlaybackMediaCommandCatalog.isUpdateButtonsCommand(
                customAction = "com.yanhao.kmpmusic.playback.UNKNOWN",
            ),
        )
    }

    @Test
    fun exposesFavoriteAndPlaybackModeCommands(): Unit {
        assertTrue(
            actual = PlaybackMediaCommandCatalog.isToggleFavoriteAction(
                customAction = PlaybackMediaCommandCatalog.toggleFavoriteCommand().customAction,
            ),
        )
        assertTrue(
            actual = PlaybackMediaCommandCatalog.isCycleModeAction(
                customAction = PlaybackMediaCommandCatalog.cycleModeCommand().customAction,
            ),
        )
    }
}
```

- [ ] **Step 4: Create command handler tests**

Create `composeApp/src/androidUnitTest/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaCommandHandlerTest.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionResult
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@UnstableApi
class AndroidPlaybackMediaCommandHandlerTest {
    @BeforeTest
    fun clearDispatcher(): Unit {
        PlaybackMediaCommandDispatcher.clear()
    }

    @Test
    fun customCommandWithoutAttachedActionsReturnsInvalidState(): Unit {
        val resultCode = AndroidPlaybackMediaCommandHandler.handleCustomCommand(
            customAction = PlaybackMediaCommandCatalog.toggleFavoriteCommand().customAction,
        )

        assertEquals(expected = SessionResult.RESULT_ERROR_INVALID_STATE, actual = resultCode)
    }

    @Test
    fun favoriteCommandOnlyCallsFavoriteAction(): Unit {
        val actions = RecordingPlaybackMediaButtonActions()
        PlaybackMediaCommandDispatcher.attach(actions = actions)

        val resultCode = AndroidPlaybackMediaCommandHandler.handleCustomCommand(
            customAction = PlaybackMediaCommandCatalog.toggleFavoriteCommand().customAction,
        )

        assertEquals(expected = SessionResult.RESULT_SUCCESS, actual = resultCode)
        assertEquals(expected = 1, actual = actions.toggleFavoriteCalls)
        assertEquals(expected = 0, actual = actions.cycleModeCalls)
    }

    @Test
    fun cycleModeCommandOnlyCallsModeAction(): Unit {
        val actions = RecordingPlaybackMediaButtonActions()
        PlaybackMediaCommandDispatcher.attach(actions = actions)

        val resultCode = AndroidPlaybackMediaCommandHandler.handleCustomCommand(
            customAction = PlaybackMediaCommandCatalog.cycleModeCommand().customAction,
        )

        assertEquals(expected = SessionResult.RESULT_SUCCESS, actual = resultCode)
        assertEquals(expected = 0, actual = actions.toggleFavoriteCalls)
        assertEquals(expected = 1, actual = actions.cycleModeCalls)
    }

    @Test
    fun updateButtonsCommandIsRejectedByHandler(): Unit {
        val actions = RecordingPlaybackMediaButtonActions()
        PlaybackMediaCommandDispatcher.attach(actions = actions)

        val resultCode = AndroidPlaybackMediaCommandHandler.handleCustomCommand(
            customAction = PlaybackMediaCommandCatalog.updateButtonsCommand().customAction,
        )

        assertEquals(expected = SessionResult.RESULT_ERROR_BAD_VALUE, actual = resultCode)
        assertEquals(expected = 0, actual = actions.toggleFavoriteCalls)
        assertEquals(expected = 0, actual = actions.cycleModeCalls)
    }
}

private class RecordingPlaybackMediaButtonActions : PlaybackMediaButtonActions {
    var toggleFavoriteCalls: Int = 0
        private set

    var cycleModeCalls: Int = 0
        private set

    override fun toggleFavorite() {
        toggleFavoriteCalls += 1
    }

    override fun cycleMode() {
        cycleModeCalls += 1
    }
}
```

- [ ] **Step 5: Run Android unit tests and verify they fail**

Run:

```bash
./gradlew :composeApp:testDebugUnitTest --tests com.yanhao.kmpmusic.playback.MediaButtonStateCodecTest --tests com.yanhao.kmpmusic.playback.PlaybackMediaCommandCatalogTest --tests com.yanhao.kmpmusic.playback.AndroidPlaybackMediaCommandHandlerTest
```

Expected: FAIL with unresolved references to `MediaButtonStateCodec`, `PlaybackMediaCommandCatalog`, `AndroidPlaybackMediaCommandHandler`, or `PlaybackMediaCommandDispatcher.clear`.

- [ ] **Step 6: Commit**

Do not commit this task yet. These failing tests are committed with Task 2 after the implementation passes, so `main` never contains intentionally failing tests.

## Task 2: Split Android Media3 Command Modules

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommandActions.kt`
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommandCatalog.kt`
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MediaButtonStateCodec.kt`
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaButtonFactory.kt`
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaCommandHandler.kt`
- Delete: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommands.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaSessionCallback.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaNotificationProvider.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MediaButtonStateSender.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackRuntime.kt`
- Test: Android unit tests from Task 1.

- [ ] **Step 1: Move command actions and add test reset**

Create `PlaybackMediaCommandActions.kt` with:

```kotlin
package com.yanhao.kmpmusic.playback

/**
 * 系统媒体通知可触发的自定义播放动作集合，标准播放命令由 Media3 委托给 Player。
 */
interface PlaybackMediaButtonActions {
    /** 切换当前播放歌曲的收藏状态。 */
    fun toggleFavorite()

    /** 切换到下一种播放模式。 */
    fun cycleMode()
}

/**
 * 进程内 Media3 自定义命令派发器，确保通知按钮和系统媒体命令共享 controller 命令路径。
 */
object PlaybackMediaCommandDispatcher {
    private var actions: PlaybackMediaButtonActions? = null

    /** 在 Android 播放会话就绪后挂入同一份命令实现。 */
    fun attach(actions: PlaybackMediaButtonActions) {
        this.actions = actions
    }

    /** 返回当前按钮动作实现；尚未接线时返回 null。 */
    fun current(): PlaybackMediaButtonActions? {
        return actions
    }

    /** 清空当前动作实现，供 Android unit tests 隔离单例状态。 */
    internal fun clear() {
        actions = null
    }
}
```

- [ ] **Step 2: Create Media3 command catalog**

Create `PlaybackMediaCommandCatalog.kt` with the custom action strings and command functions:

```kotlin
package com.yanhao.kmpmusic.playback

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands

private const val CUSTOM_ACTION_TOGGLE_FAVORITE: String = "com.yanhao.kmpmusic.playback.TOGGLE_FAVORITE"
private const val CUSTOM_ACTION_CYCLE_MODE: String = "com.yanhao.kmpmusic.playback.CYCLE_MODE"
private const val CUSTOM_ACTION_UPDATE_BUTTONS: String = "com.yanhao.kmpmusic.playback.UPDATE_BUTTONS"

/**
 * Android Media3 custom command 定义的唯一来源。
 */
@UnstableApi
internal object PlaybackMediaCommandCatalog {
    private val toggleFavoriteCommand: SessionCommand = SessionCommand(CUSTOM_ACTION_TOGGLE_FAVORITE, Bundle.EMPTY)
    private val cycleModeCommand: SessionCommand = SessionCommand(CUSTOM_ACTION_CYCLE_MODE, Bundle.EMPTY)
    private val updateButtonsSessionCommand: SessionCommand = SessionCommand(CUSTOM_ACTION_UPDATE_BUTTONS, Bundle.EMPTY)

    fun availableSessionCommands(): SessionCommands {
        return SessionCommands.Builder()
            .addSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.commands)
            .add(toggleFavoriteCommand)
            .add(cycleModeCommand)
            .add(updateButtonsSessionCommand)
            .build()
    }

    fun toggleFavoriteCommand(): SessionCommand = toggleFavoriteCommand

    fun cycleModeCommand(): SessionCommand = cycleModeCommand

    fun updateButtonsCommand(): SessionCommand = updateButtonsSessionCommand

    fun isUpdateButtonsCommand(customAction: String): Boolean {
        return customAction == CUSTOM_ACTION_UPDATE_BUTTONS
    }

    fun isToggleFavoriteAction(customAction: String): Boolean {
        return customAction == CUSTOM_ACTION_TOGGLE_FAVORITE
    }

    fun isCycleModeAction(customAction: String): Boolean {
        return customAction == CUSTOM_ACTION_CYCLE_MODE
    }

    fun isToggleFavoriteButton(commandButton: CommandButton): Boolean {
        return commandButton.sessionCommand?.customAction == CUSTOM_ACTION_TOGGLE_FAVORITE
    }

    fun isPlaybackModeButton(commandButton: CommandButton): Boolean {
        return commandButton.sessionCommand?.customAction == CUSTOM_ACTION_CYCLE_MODE
    }
}
```

- [ ] **Step 3: Create button-state codec**

Create `MediaButtonStateCodec.kt` by moving the Bundle keys and encoder/decoder from `AndroidPlaybackMediaButtons`:

```kotlin
package com.yanhao.kmpmusic.playback

import android.os.Bundle
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus

private const val ARG_SHOULD_SHOW_PAUSE_BUTTON: String = "should_show_pause_button"
private const val ARG_IS_FAVORITE: String = "is_favorite"
private const val ARG_PLAYBACK_MODE: String = "playback_mode"
private const val ARG_PLAYBACK_STATUS: String = "playback_status"
private const val ARG_HAS_ACTIVE_PLAYBACK_SESSION: String = "has_active_playback_session"

/**
 * 把 shared 媒体按钮状态编码到 Media3 custom command 参数，解析失败时返回 null。
 */
internal object MediaButtonStateCodec {
    fun createUpdateButtonsArgs(state: MediaButtonState): Bundle {
        return Bundle().apply {
            putBoolean(ARG_SHOULD_SHOW_PAUSE_BUTTON, state.shouldShowPauseButton)
            putBoolean(ARG_IS_FAVORITE, state.isFavorite)
            putString(ARG_PLAYBACK_MODE, state.playbackMode.name)
            putString(ARG_PLAYBACK_STATUS, state.playbackStatus.name)
            putBoolean(ARG_HAS_ACTIVE_PLAYBACK_SESSION, state.hasActivePlaybackSession)
        }
    }

    fun resolveUpdateButtonsState(args: Bundle): MediaButtonState? {
        val playbackMode: PlaybackMode = args.getString(ARG_PLAYBACK_MODE)
            ?.let { value: String -> runCatching { PlaybackMode.valueOf(value) }.getOrNull() }
            ?: return null
        val playbackStatus: PlaybackStatus = args.getString(ARG_PLAYBACK_STATUS)
            ?.let { value: String -> runCatching { PlaybackStatus.valueOf(value) }.getOrNull() }
            ?: return null
        return MediaButtonState(
            shouldShowPauseButton = args.getBoolean(ARG_SHOULD_SHOW_PAUSE_BUTTON),
            isFavorite = args.getBoolean(ARG_IS_FAVORITE),
            playbackMode = playbackMode,
            playbackStatus = playbackStatus,
            hasActivePlaybackSession = args.getBoolean(ARG_HAS_ACTIVE_PLAYBACK_SESSION),
        )
    }
}
```

- [ ] **Step 4: Create button factory**

Create `AndroidPlaybackMediaButtonFactory.kt` by moving `mediaButtonPreferences`, previous/play-pause/next creation, favorite/mode creation, and playback mode icon/display mapping. The public functions must be:

```kotlin
@UnstableApi
internal object AndroidPlaybackMediaButtonFactory {
    fun mediaButtonPreferences(
        shouldShowPauseButton: Boolean,
        isFavorite: Boolean,
        playbackMode: PlaybackMode,
    ): List<CommandButton>

    fun createPreviousButton(): CommandButton

    fun createPlayPauseButton(shouldShowPauseButton: Boolean): CommandButton

    fun createNextButton(): CommandButton
}
```

Inside this file, `createFavoriteButton`, `createPlaybackModeButton`, `PlaybackMode.resolveIcon`, and `PlaybackMode.resolveDisplayName` stay private. Use `PlaybackMediaCommandCatalog.toggleFavoriteCommand()` and `PlaybackMediaCommandCatalog.cycleModeCommand()` for custom buttons.

- [ ] **Step 5: Create command handler**

Create `AndroidPlaybackMediaCommandHandler.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionResult

/**
 * Media3 custom command dispatcher. It never reads or mutates MusicAppUiState directly.
 */
@UnstableApi
internal object AndroidPlaybackMediaCommandHandler {
    fun handleCustomCommand(customAction: String): Int {
        val actions: PlaybackMediaButtonActions = PlaybackMediaCommandDispatcher.current()
            ?: return SessionResult.RESULT_ERROR_INVALID_STATE
        return when {
            PlaybackMediaCommandCatalog.isToggleFavoriteAction(customAction = customAction) -> {
                actions.toggleFavorite()
                SessionResult.RESULT_SUCCESS
            }
            PlaybackMediaCommandCatalog.isCycleModeAction(customAction = customAction) -> {
                actions.cycleMode()
                SessionResult.RESULT_SUCCESS
            }
            PlaybackMediaCommandCatalog.isUpdateButtonsCommand(customAction = customAction) -> {
                SessionResult.RESULT_ERROR_BAD_VALUE
            }
            else -> SessionResult.RESULT_ERROR_NOT_SUPPORTED
        }
    }
}
```

- [ ] **Step 6: Migrate Android call sites**

Replace these references:

```text
AndroidPlaybackMediaButtons.availableSessionCommands()
  -> PlaybackMediaCommandCatalog.availableSessionCommands()
AndroidPlaybackMediaButtons.isUpdateButtonsCommand(customAction = customAction)
  -> PlaybackMediaCommandCatalog.isUpdateButtonsCommand(customAction = customAction)
AndroidPlaybackMediaButtons.handleCustomCommand(customAction = customAction)
  -> AndroidPlaybackMediaCommandHandler.handleCustomCommand(customAction = customAction)
AndroidPlaybackMediaButtons.resolveUpdateButtonsState(args = args)
  -> MediaButtonStateCodec.resolveUpdateButtonsState(args = args)
AndroidPlaybackMediaButtons.updateButtonsCommand()
  -> PlaybackMediaCommandCatalog.updateButtonsCommand()
AndroidPlaybackMediaButtons.createUpdateButtonsArgs(state = state)
  -> MediaButtonStateCodec.createUpdateButtonsArgs(state = state)
AndroidPlaybackMediaButtons.mediaButtonPreferences(shouldShowPauseButton, isFavorite, playbackMode)
  -> AndroidPlaybackMediaButtonFactory.mediaButtonPreferences(shouldShowPauseButton, isFavorite, playbackMode)
AndroidPlaybackMediaButtons.createPreviousButton()
  -> AndroidPlaybackMediaButtonFactory.createPreviousButton()
AndroidPlaybackMediaButtons.createPlayPauseButton(shouldShowPauseButton = shouldShowPauseButton)
  -> AndroidPlaybackMediaButtonFactory.createPlayPauseButton(shouldShowPauseButton = shouldShowPauseButton)
AndroidPlaybackMediaButtons.createNextButton()
  -> AndroidPlaybackMediaButtonFactory.createNextButton()
AndroidPlaybackMediaButtons.isToggleFavoriteButton(commandButton = commandButton)
  -> PlaybackMediaCommandCatalog.isToggleFavoriteButton(commandButton = commandButton)
AndroidPlaybackMediaButtons.isPlaybackModeButton(commandButton = commandButton)
  -> PlaybackMediaCommandCatalog.isPlaybackModeButton(commandButton = commandButton)
```

- [ ] **Step 7: Delete old aggregate file**

Delete `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackMediaCommands.kt`. Do not leave an `AndroidPlaybackMediaButtons` forwarding object.

- [ ] **Step 8: Verify old aggregate references are gone**

Run:

```bash
rg "AndroidPlaybackMediaButtons|PlaybackMediaCommands" composeApp/src/androidMain composeApp/src/androidUnitTest
```

Expected: no output.

- [ ] **Step 9: Run Android tests and compile**

Run:

```bash
./gradlew :composeApp:testDebugUnitTest --tests com.yanhao.kmpmusic.playback.MediaButtonStateCodecTest --tests com.yanhao.kmpmusic.playback.PlaybackMediaCommandCatalogTest --tests com.yanhao.kmpmusic.playback.AndroidPlaybackMediaCommandHandlerTest
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: both commands PASS.

- [ ] **Step 10: Commit**

```bash
git add composeApp/build.gradle.kts composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback composeApp/src/androidUnitTest/kotlin/com/yanhao/kmpmusic/playback
git commit -m "refactor: 拆分 Android 媒体命令模块"
```

## Task 3: Split Android Playback Session Runtime

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackControllerFactory.kt`
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidUiBindingRegistry.kt`
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSessionRuntime.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSession.kt`

- [ ] **Step 1: Create Android UI binding registry**

Move `MutableLocalMusicScanner`, `MutablePermissionSettingsOpener`, and `MissingAndroidLocalMusicScanner` out of `AndroidPlaybackSession.kt` into `AndroidUiBindingRegistry.kt`. The file must expose:

```kotlin
internal class AndroidUiBindingRegistry {
    val localMusicScanner: LocalMusicScanner
    val permissionSettingsOpener: PermissionSettingsOpener
    fun attachLocalMusicScanner(scanner: LocalMusicScanner)
    fun attachPermissionSettingsOpener(opener: PermissionSettingsOpener)
    fun clear()
}
```

`localMusicScanner` returns the mutable scanner proxy. `permissionSettingsOpener` returns the mutable opener proxy. The missing scanner keeps the current `LocalMusicScanException` with `LocalMusicScanErrorType.Unknown` and message `Android 本地音乐扫描器尚未初始化`.

- [ ] **Step 2: Create Android controller factory**

Create `AndroidPlaybackControllerFactory.kt` and move the controller dependency graph from `AndroidPlaybackSession.bootstrap` into:

```kotlin
internal fun createAndroidPlaybackController(
    context: Context,
    localMusicScanner: LocalMusicScanner,
    audioPlayerEngine: PlaybackServiceConnector,
    permissionSettingsOpener: PermissionSettingsOpener,
    controllerScope: CoroutineScope,
    nowMillis: () -> Long = { System.currentTimeMillis() },
): MusicAppController
```

This function must create the Android Room database, repositories, `RoomPlaybackSnapshotStore`, and `MusicAppController` exactly as the current `bootstrap` does. It must not attach the controller to `AndroidPlaybackRuntime`; runtime attachment remains in `AndroidPlaybackSessionRuntime`.

- [ ] **Step 3: Create Android playback session runtime**

Create `AndroidPlaybackSessionRuntime.kt`:

```kotlin
internal class AndroidPlaybackSessionRuntime(
    private val uiBindings: AndroidUiBindingRegistry = AndroidUiBindingRegistry(),
    private val playbackScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val playbackServiceConnector: PlaybackServiceConnector = PlaybackServiceConnector(scope = playbackScope),
    private val playbackRuntime: AndroidPlaybackRuntime = AndroidPlaybackRuntime(
        serviceConnector = playbackServiceConnector,
    ),
) {
    private var controllerHolder: MusicAppController? = null
    private var hasRequestedPlaybackRestore: Boolean = false

    val controller: MusicAppController
        get() = controllerHolder ?: error("AndroidPlaybackSession 尚未 bootstrap")

    fun bootstrap(context: Context)
    fun attachPlaybackContext(context: Context)
    fun ensurePlaybackSnapshotRestoreRequested()
    fun attachLocalMusicScanner(scanner: LocalMusicScanner)
    fun attachPermissionSettingsOpener(opener: PermissionSettingsOpener)
    fun clearUiBindings()
}
```

Implementation rules:

- `bootstrap(context)` calls `playbackRuntime.attachContext(context.applicationContext)` before the `controllerHolder` early return, preserving service attach behavior.
- Controller construction is synchronized exactly as today.
- After creating the controller, call `playbackRuntime.attachController(controller = controller)`.
- `attachLocalMusicScanner(scanner)` delegates to `uiBindings.attachLocalMusicScanner(scanner)` and then calls `ensurePlaybackSnapshotRestoreRequested()`.
- `clearUiBindings()` delegates to `uiBindings.clear()`.

- [ ] **Step 4: Thin `AndroidPlaybackSession.kt`**

Replace object internals with:

```kotlin
object AndroidPlaybackSession {
    private val runtime: AndroidPlaybackSessionRuntime = AndroidPlaybackSessionRuntime()

    val controller: MusicAppController
        get() = runtime.controller

    fun bootstrap(context: Context) {
        runtime.bootstrap(context = context)
    }

    fun attachPlaybackContext(context: Context) {
        runtime.attachPlaybackContext(context = context)
    }

    fun ensurePlaybackSnapshotRestoreRequested() {
        runtime.ensurePlaybackSnapshotRestoreRequested()
    }

    fun attachLocalMusicScanner(scanner: LocalMusicScanner) {
        runtime.attachLocalMusicScanner(scanner = scanner)
    }

    fun attachPermissionSettingsOpener(opener: PermissionSettingsOpener) {
        runtime.attachPermissionSettingsOpener(opener = opener)
    }

    fun clearUiBindings() {
        runtime.clearUiBindings()
    }
}
```

Keep the existing public method names and error message.

- [ ] **Step 5: Verify Android session file is thin**

Run:

```bash
rg -n "PersistentFavoritesRepository|PersistentPlaybackRepository|PersistentMusicLibraryRepository|MutableLocalMusicScanner|MutablePermissionSettingsOpener|MissingAndroidLocalMusicScanner|SupervisorJob|PlaybackServiceConnector\\(" composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSession.kt
```

Expected: no output.

- [ ] **Step 6: Compile Android**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSession.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackControllerFactory.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidUiBindingRegistry.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSessionRuntime.kt
git commit -m "refactor: 拆分 Android 播放会话运行时"
```

## Task 4: Split Desktop Session Wiring

**Files:**
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackControllerFactory.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSessionRuntime.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopAudioRuntimeFactory.kt`
- Modify: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt`
- Modify: `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSessionTest.kt`

- [ ] **Step 1: Move desktop controller factory**

Move `createDesktopPlaybackController` unchanged from `DesktopPlaybackSession.kt` to `DesktopPlaybackControllerFactory.kt`. Keep the function signature:

```kotlin
internal fun createDesktopPlaybackController(
    playbackDatabase: PlaybackDatabase,
    audioPlayerEngine: AudioPlayerEngine,
    controllerScope: CoroutineScope,
    localMusicScanner: LocalMusicScanner = DesktopFolderMusicScanner(),
    nowMillis: () -> Long = { System.currentTimeMillis() },
): MusicAppController
```

- [ ] **Step 2: Move desktop session runtime**

Move `DesktopPlaybackSessionRuntime` unchanged from `DesktopPlaybackSession.kt` to `DesktopPlaybackSessionRuntime.kt`. Keep its primary constructor and `ensurePlaybackSnapshotRestoreRequested` / `close` behavior unchanged so existing `DesktopPlaybackSessionTest` keeps asserting the same lifecycle order.

- [ ] **Step 3: Create desktop audio runtime factory**

Create `DesktopAudioRuntimeFactory.kt`:

```kotlin
package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngine
import com.yanhao.kmpmusic.playback.MacosLibVlcRuntime
import com.yanhao.kmpmusic.playback.UnavailableDesktopMediaPlayerAdapter
import com.yanhao.kmpmusic.playback.VlcjMediaPlayerAdapter
import kotlinx.coroutines.CoroutineScope

internal data class DesktopAudioRuntime(
    val audioEngine: DesktopVlcjAudioPlayerEngine,
)

internal object DesktopAudioRuntimeFactory {
    fun create(sessionScope: CoroutineScope): DesktopAudioRuntime {
        val runtimePath = MacosLibVlcRuntime.resolve()
        val audioEngine = if (runtimePath == null) {
            DesktopVlcjAudioPlayerEngine(
                adapter = UnavailableDesktopMediaPlayerAdapter(),
                scope = sessionScope,
                libVlcPluginPath = null,
            )
        } else {
            DesktopVlcjAudioPlayerEngine(
                adapter = VlcjMediaPlayerAdapter(runtimePath = runtimePath),
                scope = sessionScope,
                libVlcPluginPath = runtimePath.pluginDirectory,
            )
        }
        return DesktopAudioRuntime(audioEngine = audioEngine)
    }
}
```

- [ ] **Step 4: Thin `DesktopPlaybackSession.kt`**

Keep only the facade and lazy runtime construction:

```kotlin
object DesktopPlaybackSession {
    private val runtime: DesktopPlaybackSessionRuntime by lazy {
        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val playbackDatabase: PlaybackDatabase = createDesktopPlaybackDatabase()
        val audioRuntime: DesktopAudioRuntime = DesktopAudioRuntimeFactory.create(
            sessionScope = sessionScope,
        )
        DesktopPlaybackSessionRuntime(
            controller = createDesktopPlaybackController(
                playbackDatabase = playbackDatabase,
                audioPlayerEngine = audioRuntime.audioEngine,
                controllerScope = sessionScope,
            ),
            sessionScope = sessionScope,
            releaseAudioEngineAndAwait = {
                audioRuntime.audioEngine.releaseAndAwait()
            },
            closePlaybackDatabase = {
                playbackDatabase.close()
            },
        )
    }

    val controller: MusicAppController
        get() = runtime.controller

    fun ensurePlaybackSnapshotRestoreRequested() {
        runtime.ensurePlaybackSnapshotRestoreRequested()
    }

    fun close() {
        runtime.close()
    }
}
```

- [ ] **Step 5: Verify desktop session file is thin**

Run:

```bash
rg -n "PersistentFavoritesRepository|PersistentPlaybackRepository|MacosLibVlcRuntime|VlcjMediaPlayerAdapter|UnavailableDesktopMediaPlayerAdapter|class DesktopPlaybackSessionRuntime|fun createDesktopPlaybackController" composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt
```

Expected: no output.

- [ ] **Step 6: Run desktop session tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.DesktopPlaybackSessionTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackControllerFactory.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSessionRuntime.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopAudioRuntimeFactory.kt composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSessionTest.kt
git commit -m "refactor: 拆分桌面播放会话装配"
```

## Task 5: Extract Desktop Engine Command State And Ack Tracker

**Files:**
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopPlaybackCommand.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopPlaybackControlIntent.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopPlaybackEngineState.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopSetQueueAckTracker.kt`
- Modify: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt`
- Test: `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngineTest.kt`

- [ ] **Step 1: Move engine command model**

Move private `EngineCommand` out of `DesktopVlcjAudioPlayerEngine.kt` into `DesktopPlaybackCommand.kt` and rename it to `DesktopPlaybackCommand`:

```kotlin
internal sealed interface DesktopPlaybackCommand {
    data class SetQueue(
        val items: List<PlayableMedia>,
        val startIndex: Int,
        val startPositionMs: Long,
        val ack: CompletableDeferred<Unit>,
    ) : DesktopPlaybackCommand

    data object Play : DesktopPlaybackCommand
    data object Pause : DesktopPlaybackCommand
    data class SeekTo(val positionMs: Long) : DesktopPlaybackCommand
    data class SkipToIndex(val index: Int) : DesktopPlaybackCommand
    data class SetPlaybackMode(val playbackMode: PlaybackMode) : DesktopPlaybackCommand
    data class SetVolume(val volume: Float) : DesktopPlaybackCommand
    data object Stop : DesktopPlaybackCommand
    data object Release : DesktopPlaybackCommand
    data class AdapterEventReceived(val event: DesktopMediaPlayerEvent) : DesktopPlaybackCommand
    data object ProgressTick : DesktopPlaybackCommand
}
```

Update every `EngineCommand` reference to `DesktopPlaybackCommand`.

- [ ] **Step 2: Move playback control intent**

Move private `PlaybackControlIntent` to `DesktopPlaybackControlIntent.kt` as:

```kotlin
internal enum class DesktopPlaybackControlIntent {
    None,
    Play,
    Pause,
}
```

Update engine references from `PlaybackControlIntent` to `DesktopPlaybackControlIntent`.

- [ ] **Step 3: Create engine state holder**

Create `DesktopPlaybackEngineState.kt`:

```kotlin
internal class DesktopPlaybackEngineState {
    var queue: List<PlayableMedia> = emptyList()
    var currentIndex: Int = -1
    var generation: Long = 0L
        private set
    var playbackControlIntent: DesktopPlaybackControlIntent = DesktopPlaybackControlIntent.None
    var pendingSeekMs: Long? = null
    var isPrepared: Boolean = false

    fun isCurrentIndexValid(): Boolean {
        return currentIndex in queue.indices
    }

    fun currentMedia(): PlayableMedia? {
        return queue.getOrNull(index = currentIndex)
    }

    fun snapshot(): DesktopPlaybackEngineSnapshot {
        return DesktopPlaybackEngineSnapshot(
            queue = queue,
            currentIndex = currentIndex,
            generation = generation,
            playbackControlIntent = playbackControlIntent,
            pendingSeekMs = pendingSeekMs,
            isPrepared = isPrepared,
        )
    }

    fun nextGeneration(): Long {
        generation += 1L
        return generation
    }

    fun resetForNewQueue(items: List<PlayableMedia>): Unit {
        queue = items
        playbackControlIntent = DesktopPlaybackControlIntent.None
        pendingSeekMs = null
        isPrepared = false
    }

    fun resetPlaybackFlags(): Unit {
        playbackControlIntent = DesktopPlaybackControlIntent.None
        pendingSeekMs = null
        isPrepared = false
    }
}

internal data class DesktopPlaybackEngineSnapshot(
    val queue: List<PlayableMedia>,
    val currentIndex: Int,
    val generation: Long,
    val playbackControlIntent: DesktopPlaybackControlIntent,
    val pendingSeekMs: Long?,
    val isPrepared: Boolean,
) {
    fun isCurrentIndexValid(): Boolean {
        return currentIndex in queue.indices
    }

    fun currentMedia(): PlayableMedia? {
        return queue.getOrNull(index = currentIndex)
    }
}
```

Move `queue`, `currentIndex`, `generation`, `playbackControlIntent`, `pendingSeekMs`, `isPrepared`, and `nextGeneration()` usage from `DesktopVlcjAudioPlayerEngine` into this state holder. Keep `isReleasing`, `isReleased`, jobs, channels, and test hooks in the engine for now.

In `DesktopVlcjAudioPlayerEngine`, add:

```kotlin
private val state: DesktopPlaybackEngineState = DesktopPlaybackEngineState()
```

Then update existing handlers to read/write `state.queue`, `state.currentIndex`, `state.generation`, `state.playbackControlIntent`, `state.pendingSeekMs`, and `state.isPrepared`.

- [ ] **Step 4: Create setQueue ack tracker**

Create `DesktopSetQueueAckTracker.kt`:

```kotlin
internal class DesktopSetQueueAckTracker {
    private val lock: Any = Any()
    private val pendingAcks: MutableSet<CompletableDeferred<Unit>> = linkedSetOf()

    fun register(ack: CompletableDeferred<Unit>) {
        synchronized(lock = lock) {
            pendingAcks += ack
        }
    }

    fun complete(ack: CompletableDeferred<Unit>) {
        val shouldComplete: Boolean = synchronized(lock = lock) {
            pendingAcks.remove(element = ack)
        }
        if (shouldComplete) {
            ack.complete(value = Unit)
        }
    }

    fun completeAll() {
        val snapshot: List<CompletableDeferred<Unit>> = synchronized(lock = lock) {
            val acks: List<CompletableDeferred<Unit>> = pendingAcks.toList()
            pendingAcks.clear()
            acks
        }
        snapshot.forEach { ack: CompletableDeferred<Unit> ->
            ack.complete(value = Unit)
        }
    }
}
```

Replace `pendingSetQueueAckLock`, `pendingSetQueueAcks`, `registerPendingSetQueueAck`, `completePendingSetQueueAck`, and `completeAllPendingSetQueueAcks` in the engine with this tracker.

- [ ] **Step 5: Run engine tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngineTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopPlaybackCommand.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopPlaybackControlIntent.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopPlaybackEngineState.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopSetQueueAckTracker.kt
git commit -m "refactor: 拆分桌面引擎命令和状态"
```

## Task 6: Extract Desktop Media Mapper And Progress Ticker

**Files:**
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopMediaSourceMapper.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopProgressTicker.kt`
- Modify: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt`
- Test: `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngineTest.kt`

- [ ] **Step 1: Create desktop media source mapper**

Create `DesktopMediaSourceMapper.kt`:

```kotlin
internal object DesktopMediaSourceMapper {
    fun playbackUri(media: PlayableMedia): String {
        return when (val source: AudioSource = media.audioSource) {
            is AudioSource.Local -> source.uri
        }
    }
}
```

Replace the engine's private `PlayableMedia.playbackUri()` extension with `DesktopMediaSourceMapper.playbackUri(media = media)`.

- [ ] **Step 2: Create desktop progress ticker**

Create `DesktopProgressTicker.kt`:

```kotlin
internal class DesktopProgressTicker(
    private val scope: CoroutineScope,
    private val intervalMs: Long,
    private val sendTick: suspend () -> Unit,
) {
    private var job: Job? = null

    fun start() {
        stop()
        if (intervalMs <= 0L) {
            return
        }
        job = scope.launch {
            while (isActive) {
                delay(timeMillis = intervalMs)
                sendTick()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
```

In `DesktopVlcjAudioPlayerEngine`, replace `progressJob`, `startProgressPolling`, and `stopProgressPolling` with:

```kotlin
private val progressTicker: DesktopProgressTicker = DesktopProgressTicker(
    scope = engineScope,
    intervalMs = progressIntervalMs,
    sendTick = {
        commandChannel.send(element = DesktopPlaybackCommand.ProgressTick)
    },
)
```

Use `progressTicker.start()` and `progressTicker.stop()`. The ticker must not send `PlaybackEngineEvent` directly.

- [ ] **Step 3: Verify ticker and mapper behavior with existing tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngineTest
```

Expected: PASS, including `setQueuePreparesAdapterWithAudioSourceUri`.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopMediaSourceMapper.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopProgressTicker.kt
git commit -m "refactor: 拆分桌面媒体映射和进度轮询"
```

## Task 7: Extract Desktop Adapter Event Reducer And Command Loop

**Files:**
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopAdapterEventReducer.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopPlaybackCommandLoop.kt`
- Modify: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt`
- Test: `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngineTest.kt`

- [ ] **Step 1: Create reducer result models**

Create `DesktopAdapterEventReducer.kt` with:

```kotlin
internal data class DesktopAdapterEventReduction(
    val events: List<PlaybackEngineEvent> = emptyList(),
    val adapterActions: List<DesktopAdapterAction> = emptyList(),
    val stateUpdates: List<DesktopEngineStateUpdate> = emptyList(),
    val shouldStartProgressTicker: Boolean = false,
    val shouldStopProgressTicker: Boolean = false,
)

internal sealed interface DesktopAdapterAction {
    data class SeekTo(
        val generation: Long,
        val positionMs: Long,
    ) : DesktopAdapterAction

    data class Play(val generation: Long) : DesktopAdapterAction

    data class Pause(val generation: Long) : DesktopAdapterAction
}

internal sealed interface DesktopEngineStateUpdate {
    data object MarkPrepared : DesktopEngineStateUpdate
    data object ResetPlaybackFlags : DesktopEngineStateUpdate
    data object AdvanceGeneration : DesktopEngineStateUpdate
}
```

Then create:

```kotlin
internal class DesktopAdapterEventReducer {
    fun reduce(
        snapshot: DesktopPlaybackEngineSnapshot,
        event: DesktopMediaPlayerEvent,
    ): DesktopAdapterEventReduction
}
```

Move the decision logic from `handleAdapterEvent`, `handlePrepared`, `handlePlaying`, `handlePaused`, `handleFinished`, and `handleFailed` into `reduce`. The reducer must not mutate `DesktopPlaybackEngineState`; it receives only `DesktopPlaybackEngineSnapshot` and returns `DesktopAdapterEventReduction`. It must not hold adapter, channels, ticker, or coroutine scope, and it must not call adapter methods.

- [ ] **Step 2: Apply reducer output in engine**

In `DesktopVlcjAudioPlayerEngine`, replace the old adapter event handlers with:

```kotlin
private val adapterEventReducer: DesktopAdapterEventReducer = DesktopAdapterEventReducer()

private suspend fun handleAdapterEvent(event: DesktopMediaPlayerEvent) {
    val reduction: DesktopAdapterEventReduction = adapterEventReducer.reduce(
        snapshot = state.snapshot(),
        event = event,
    )
    reduction.stateUpdates.forEach { update: DesktopEngineStateUpdate ->
        when (update) {
            DesktopEngineStateUpdate.MarkPrepared -> state.isPrepared = true
            DesktopEngineStateUpdate.ResetPlaybackFlags -> state.resetPlaybackFlags()
            DesktopEngineStateUpdate.AdvanceGeneration -> state.nextGeneration()
        }
    }
    if (reduction.shouldStopProgressTicker) {
        progressTicker.stop()
    }
    reduction.adapterActions.forEach { action: DesktopAdapterAction ->
        when (action) {
            is DesktopAdapterAction.SeekTo -> adapter.seekTo(
                generation = action.generation,
                positionMs = action.positionMs,
            )
            is DesktopAdapterAction.Play -> adapter.play(generation = action.generation)
            is DesktopAdapterAction.Pause -> adapter.pause(generation = action.generation)
        }
    }
    if (reduction.shouldStartProgressTicker) {
        progressTicker.start()
    }
    reduction.events.forEach { event: PlaybackEngineEvent ->
        eventChannel.send(element = event)
    }
}
```

The command loop remains the only place that calls this function and applies `DesktopEngineStateUpdate`, so runtime state mutation stays serialized in the command loop instead of moving into the reducer.

- [ ] **Step 3: Create command loop class**

Create `DesktopPlaybackCommandLoop.kt`:

```kotlin
internal class DesktopPlaybackCommandLoop(
    private val commands: Channel<DesktopPlaybackCommand>,
    private val handleCommand: suspend (DesktopPlaybackCommand) -> Unit,
    private val onFinally: () -> Unit,
) {
    suspend fun run() {
        try {
            for (command: DesktopPlaybackCommand in commands) {
                handleCommand(command)
            }
        } finally {
            onFinally()
        }
    }
}
```

Update `DesktopVlcjAudioPlayerEngine.commandLoopJob` to:

```kotlin
private val commandLoopJob: Job = engineScope.launch {
    DesktopPlaybackCommandLoop(
        commands = commandChannel,
        handleCommand = ::handle,
        onFinally = ackTracker::completeAll,
    ).run()
}
```

- [ ] **Step 4: Verify command loop remains the only state mutation path**

Run:

```bash
rg -n "adapterEventReducer.reduce|progressTicker.start|progressTicker.stop|state\\." composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback
```

Expected:

- `adapterEventReducer.reduce` appears only in `DesktopVlcjAudioPlayerEngine.kt`.
- `progressTicker.start` and `progressTicker.stop` appear only in `DesktopVlcjAudioPlayerEngine.kt`.
- `DesktopProgressTicker.kt` does not import or reference `PlaybackEngineEvent`.

- [ ] **Step 5: Run engine tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngineTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopAdapterEventReducer.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopPlaybackCommandLoop.kt
git commit -m "refactor: 拆分桌面事件规整和命令循环"
```

## Task 8: Final Platform Boundary Sweep

**Files:**
- Modify imports only where final verification reveals stale references.
- No new production behavior.

- [ ] **Step 1: Verify forbidden platform leaks**

Run:

```bash
rg -n "androidx\\.media3|android\\.os|UIKit|AVFoundation|AVAudioSession|MPRemoteCommandCenter|Vlcj|vlcj|uk\\.co\\.caprica" composeApp/src/commonMain composeApp/src/commonTest
```

Expected: no output.

- [ ] **Step 2: Verify forbidden old Android aggregate names**

Run:

```bash
rg -n "AndroidPlaybackMediaButtons|PlaybackMediaCommands" composeApp/src/androidMain composeApp/src/androidUnitTest composeApp/src/commonMain composeApp/src/commonTest
```

Expected: no output.

- [ ] **Step 3: Verify platform session facades are thin**

Run:

```bash
rg -n "PersistentFavoritesRepository|PersistentPlaybackRepository|PersistentMusicLibraryRepository|MutableLocalMusicScanner|MutablePermissionSettingsOpener|MissingAndroidLocalMusicScanner|createAndroidPlaybackDatabase|SupervisorJob|PlaybackServiceConnector\\(" composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSession.kt
rg -n "PersistentFavoritesRepository|PersistentPlaybackRepository|MacosLibVlcRuntime|VlcjMediaPlayerAdapter|UnavailableDesktopMediaPlayerAdapter|class DesktopPlaybackSessionRuntime|fun createDesktopPlaybackController" composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt
```

Expected: both commands produce no output.

- [ ] **Step 4: Verify iOS scope did not expand**

Run:

```bash
rg -n "IosPlaybackSession|IosAvAudioPlayerEngine|AVFoundation|AVAudioSession|MPRemoteCommandCenter|PlatformSession" composeApp/src/iosMain composeApp/src/commonMain
```

Expected: no output.

- [ ] **Step 5: Run final automated verification**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest :composeApp:desktopTest
```

Expected: PASS.

- [ ] **Step 6: Manual platform checklist**

Manually verify on available platforms:

```text
Android:
- App playback, pause, previous, next, seek, and volume contract remain unchanged.
- Notification favorite, playback mode, play/pause, previous, and next buttons work.
- Notification content click still opens the player page.
- Activity recreate keeps scanner and permission settings wiring alive.
- Service cold start reuses AndroidPlaybackSession.controller.

Desktop:
- Play, pause, previous, next, seek, and volume still work.
- Close persists final snapshot and closes database.
- Missing libVLC still uses unavailable adapter and surfaces playback error.
- Playing, paused, ended, failed, and progress events still reach UI.
```

- [ ] **Step 7: Commit final cleanup**

```bash
git add composeApp/src/androidMain composeApp/src/androidUnitTest composeApp/src/desktopMain composeApp/src/desktopTest composeApp/build.gradle.kts
git commit -m "refactor: 完成第四阶段平台适配治理"
```

## Self-Review

Spec coverage:

- Android Media3 command split is covered by Tasks 1-2.
- Android session facade/runtime/factory/UI binding split is covered by Task 3.
- Desktop session facade/controller factory/runtime/audio factory split is covered by Task 4.
- Desktop engine command, state, ack, mapper, ticker, reducer, and command loop split is covered by Tasks 5-7.
- iOS no-implementation boundary is covered by Task 8.
- Compile, Android unit tests, desktop tests, and manual platform checks are covered by Task 8.

Type consistency:

- Android names are consistently `PlaybackMediaCommandActions`, `PlaybackMediaCommandCatalog`, `MediaButtonStateCodec`, `AndroidPlaybackMediaButtonFactory`, and `AndroidPlaybackMediaCommandHandler`.
- `AndroidPlaybackMediaButtons` and `PlaybackMediaCommands.kt` are not completion-state names.
- Desktop names are consistently `DesktopPlaybackCommand`, `DesktopPlaybackEngineState`, `DesktopSetQueueAckTracker`, `DesktopMediaSourceMapper`, `DesktopProgressTicker`, `DesktopAdapterEventReducer`, and `DesktopPlaybackCommandLoop`.
- `DesktopProgressTicker` only emits `DesktopPlaybackCommand.ProgressTick`; it never emits `PlaybackEngineEvent`.
- Public session facades remain `AndroidPlaybackSession` and `DesktopPlaybackSession`; shared APIs remain unchanged.
