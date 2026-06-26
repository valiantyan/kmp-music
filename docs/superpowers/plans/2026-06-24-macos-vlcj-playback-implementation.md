# macOS vlcj Playback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable real local-audio playback on macOS Apple Silicon Desktop through vlcj/LibVLC while preserving the existing shared `MusicAppController -> PlaybackCoordinator -> AudioPlayerEngine` playback semantics.

**Architecture:** Keep all vlcj, LibVLC discovery, macOS file-system paths, and packaging logic in `desktopMain` or Gradle packaging code. Add a desktop audio-only engine built around a serial command loop, media generation tokens, stale-event dropping, and a fake adapter seam for deterministic desktop tests. Reuse shared `PlaybackCoordinator`, Room snapshot storage, favorites persistence, and UI state instead of creating a parallel Desktop queue model.

**Tech Stack:** Kotlin Multiplatform 2.0.21, Compose Multiplatform 1.7.3, Kotlin/JVM Desktop target, kotlinx.coroutines 1.9.0, Room3 3.0.0-rc01, SQLite bundled driver, vlcj 4.12.1, LibVLC 3.0.23 arm64, Gradle Kotlin DSL, kotlin.test.

---

## Scope Check

This plan implements one vertical Desktop playback slice:

- desktop vlcj dependency and audio-only adapter seam;
- serial command processing for rapid seek/skip/play/pause/release races;
- Desktop session lifetime and Room-backed persistence;
- bundle-first LibVLC discovery with dev fallback;
- macOS Apple Silicon LibVLC supply-chain and packaging tasks;
- user-facing playback error messages;
- automated desktop tests, packaging checks, and manual DMG acceptance.

It does not implement macOS Now Playing, system media keys, menu-bar controls, tray controls, Intel/Universal macOS, Windows/Linux Desktop playback, lyrics, equalizer, speed control, fade in/out, or video rendering.

## File Structure

- Modify `gradle/libs.versions.toml`: add `vlcj = "4.12.1"` and `vlcj` library alias.
- Modify `composeApp/build.gradle.kts`: add `desktopMain` vlcj dependency and macOS LibVLC packaging/verification task wiring.
- Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopMediaPlayerAdapter.kt`: desktop-only audio adapter contract and adapter event model.
- Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt`: `AudioPlayerEngine` implementation with serial command loop, generation tokens, latest-wins seek, progress polling, stale-event drop, and `release()`.
- Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/VlcjMediaPlayerAdapter.kt`: vlcj/LibVLC adapter using `CallbackMediaPlayerComponent`, with callbacks forwarded to the engine event flow.
- Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/MacosLibVlcRuntime.kt`: app-bundle-first LibVLC path resolution and development fallback discovery.
- Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/data/DesktopPlaybackDatabase.kt`: Room database builder at `~/Library/Application Support/KMP Music/kmp_music_playback.db`.
- Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt`: process-level Desktop owner for scope, scanner, engine, Room store, favorites repository, controller, snapshot restore, and shutdown release.
- Modify `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopMain.kt`: use `DesktopPlaybackSession.controller`, request restore once, and release resources from `onCloseRequest`.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/PlaybackErrorMessage.kt`: shared user-facing playback error copy.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/PlayerScreen.kt`: render `playbackError.userMessage(songTitle)` instead of raw diagnostic `message`.
- Create `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/FakeDesktopMediaPlayerAdapter.kt`: deterministic adapter for desktop engine tests.
- Create `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngineTest.kt`: command serialization, stale event, seek, pending play/pause, and release tests.
- Create `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/MacosLibVlcRuntimeTest.kt`: bundle-first path resolution and dev fallback tests.
- Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/PlaybackErrorMessageTest.kt`: error copy tests.
- Create `composeApp/src/desktopMain/packaging/macos-libvlc/README.md`: source URL, version, SHA-256, extraction date field, architecture, license files, and source-code access policy.
- Create `composeApp/src/desktopMain/packaging/macos-libvlc/download-macos-arm64-libvlc.sh`: download and SHA-256 validation helper.
- Create `composeApp/src/desktopMain/packaging/macos-libvlc/extract-macos-arm64-libvlc.sh`: DMG attach/copy/detach helper that stages `lib/`, `plugins/`, and VLC license files.
- Create `composeApp/src/desktopMain/packaging/macos-libvlc/verify-macos-app-libvlc.sh`: `codesign`, `spctl`, `otool -L`, and outside-bundle dependency checks.
- Create `composeApp/src/desktopMain/packaging/macos-libvlc/SOURCE_RECORD.md`: checked-in supply-chain record for `vlc-3.0.23-arm64.dmg`.

## Runtime Invariants

- `commonMain` must not import vlcj, JNA, JavaFX, AWT/Swing media surfaces, macOS paths, or Desktop packaging paths.
- The Desktop engine must use `CallbackMediaPlayerComponent` or lower-level audio-only vlcj APIs. It must not use `EmbeddedMediaPlayerComponent`.
- Every public engine command enters one private serial command loop. UI threads, controller scope, and vlcj callback threads must not call LibVLC control APIs directly.
- `setQueue`, `skipToIndex`, and media preparation create a new media generation. Events from old generations are ignored.
- `skipToIndex` cancels pending seek/play for the previous media. `seekTo` is latest-wins within the current generation.
- `play()` before media readiness sets `pendingPlay = true`; `pause()` clears it. If `play()` and `pause()` arrive before readiness, final state is paused.
- `stop()` and `release()` invalidate the current generation, cancel progress polling, and ignore later callbacks.
- `PlaybackCoordinator` remains responsible for queue mode, natural-ended advancement, shuffle, loop-one, failure thresholds, snapshot writes, and UI state sync.
- Release builds must load LibVLC from the app bundle. Development fallback to `/Applications/VLC.app` is allowed only for local verification.

## Task 1: Add Desktop vlcj Dependency and Packaging Constants

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`
- Create: `composeApp/src/desktopMain/packaging/macos-libvlc/SOURCE_RECORD.md`
- Create: `composeApp/src/desktopMain/packaging/macos-libvlc/README.md`

- [x] **Step 1: Add vlcj to the version catalog**

Modify `gradle/libs.versions.toml`:

```toml
[versions]
vlcj = "4.12.1"

[libraries]
vlcj = { module = "uk.co.caprica:vlcj", version.ref = "vlcj" }
```

Keep the existing `[versions]` and `[libraries]` entries in place; insert `vlcj` beside the other dependency versions and libraries.

- [x] **Step 2: Wire vlcj into the Desktop source set**

Modify `composeApp/build.gradle.kts` inside `desktopMain.dependencies`:

```kotlin
desktopMain.dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.vlcj)
}
```

- [x] **Step 3: Create the checked-in LibVLC source record**

Create `composeApp/src/desktopMain/packaging/macos-libvlc/SOURCE_RECORD.md`:

```markdown
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
```

- [x] **Step 4: Create packaging operator notes**

Create `composeApp/src/desktopMain/packaging/macos-libvlc/README.md`:

```markdown
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
```

- [x] **Step 5: Verify Gradle sees vlcj**

Run:

```bash
./gradlew :composeApp:dependencyInsight --configuration desktopCompileClasspath --dependency vlcj
```

Expected: output includes `uk.co.caprica:vlcj:4.12.1`.

- [x] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts composeApp/src/desktopMain/packaging/macos-libvlc/SOURCE_RECORD.md composeApp/src/desktopMain/packaging/macos-libvlc/README.md
git commit -m "配置 macOS vlcj 播放依赖"
```

## Task 2: Add Desktop Adapter Seam and Deterministic Fake

**Files:**
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopMediaPlayerAdapter.kt`
- Create: `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/FakeDesktopMediaPlayerAdapter.kt`
- Test: `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/FakeDesktopMediaPlayerAdapterTest.kt`

- [x] **Step 1: Write the fake adapter test**

Create `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/FakeDesktopMediaPlayerAdapterTest.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeDesktopMediaPlayerAdapterTest {
    @Test
    fun recordsCommandsAndEmitsGenerationEvents(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val events = mutableListOf<DesktopMediaPlayerEvent>()
        val collectJob = launch {
            adapter.events.take(count = 2).toList(destination = events)
        }

        adapter.prepare(
            mediaUri = "file:///Users/test/Music/song.mp3",
            generation = 7L,
            startPositionMs = 12_000L,
            pluginPath = "/Applications/KMP Music.app/Contents/Frameworks/LibVLC/plugins",
        )
        adapter.emitPrepared(generation = 7L, durationMs = 180_000L)
        adapter.play(generation = 7L)
        adapter.emitPlaying(generation = 7L, positionMs = 12_000L, durationMs = 180_000L)
        collectJob.join()

        assertEquals(
            expected = listOf(
                "prepare:file:///Users/test/Music/song.mp3:7:12000",
                "play:7",
            ),
            actual = adapter.commands,
        )
        assertEquals(
            expected = listOf(
                DesktopMediaPlayerEvent.Prepared(
                    generation = 7L,
                    durationMs = 180_000L,
                ),
                DesktopMediaPlayerEvent.Playing(
                    generation = 7L,
                    positionMs = 12_000L,
                    durationMs = 180_000L,
                ),
            ),
            actual = events,
        )
    }
}
```

- [x] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.playback.FakeDesktopMediaPlayerAdapterTest"
```

Expected: FAIL with unresolved references for `DesktopMediaPlayerEvent` and `FakeDesktopMediaPlayerAdapter`.

- [x] **Step 3: Add the desktop adapter contract**

Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopMediaPlayerAdapter.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import kotlinx.coroutines.flow.Flow

/**
 * Desktop-only audio player seam used by [DesktopVlcjAudioPlayerEngine].
 */
interface DesktopMediaPlayerAdapter {
    /** Adapter events are tagged with the engine media generation that requested the work. */
    val events: Flow<DesktopMediaPlayerEvent>

    /** Prepare a single audio item for playback. */
    suspend fun prepare(
        mediaUri: String,
        generation: Long,
        startPositionMs: Long,
        pluginPath: String?,
    )

    /** Start or resume the prepared media. */
    suspend fun play(generation: Long)

    /** Pause the prepared media. */
    suspend fun pause(generation: Long)

    /** Seek within the prepared media. */
    suspend fun seekTo(generation: Long, positionMs: Long)

    /** Stop the adapter without releasing native resources. */
    suspend fun stop(generation: Long)

    /** Return the adapter's best known current playback position. */
    suspend fun currentPositionMs(): Long

    /** Return the adapter's best known current duration. */
    suspend fun currentDurationMs(): Long?

    /** Release native resources. */
    suspend fun release()
}

/**
 * Desktop adapter events forwarded from vlcj callbacks or deterministic fakes.
 */
sealed interface DesktopMediaPlayerEvent {
    val generation: Long

    data class Prepared(
        override val generation: Long,
        val durationMs: Long?,
    ) : DesktopMediaPlayerEvent

    data class Playing(
        override val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : DesktopMediaPlayerEvent

    data class Paused(
        override val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : DesktopMediaPlayerEvent

    data class Finished(
        override val generation: Long,
    ) : DesktopMediaPlayerEvent

    data class Failed(
        override val generation: Long,
        val error: PlaybackError,
    ) : DesktopMediaPlayerEvent
}
```

- [x] **Step 4: Add the deterministic fake adapter**

Create `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/FakeDesktopMediaPlayerAdapter.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class FakeDesktopMediaPlayerAdapter : DesktopMediaPlayerAdapter {
    private val eventChannel = Channel<DesktopMediaPlayerEvent>(capacity = Channel.UNLIMITED)
    private var positionMs: Long = 0L
    private var durationMs: Long? = null

    val commands: MutableList<String> = mutableListOf()
    override val events: Flow<DesktopMediaPlayerEvent> = eventChannel.receiveAsFlow()

    override suspend fun prepare(
        mediaUri: String,
        generation: Long,
        startPositionMs: Long,
        pluginPath: String?,
    ) {
        positionMs = startPositionMs
        commands += "prepare:$mediaUri:$generation:$startPositionMs"
    }

    override suspend fun play(generation: Long) {
        commands += "play:$generation"
    }

    override suspend fun pause(generation: Long) {
        commands += "pause:$generation"
    }

    override suspend fun seekTo(generation: Long, positionMs: Long) {
        this.positionMs = positionMs
        commands += "seek:$generation:$positionMs"
    }

    override suspend fun stop(generation: Long) {
        positionMs = 0L
        commands += "stop:$generation"
    }

    override suspend fun currentPositionMs(): Long {
        return positionMs
    }

    override suspend fun currentDurationMs(): Long? {
        return durationMs
    }

    override suspend fun release() {
        commands += "release"
    }

    fun emitPrepared(generation: Long, durationMs: Long?) {
        this.durationMs = durationMs
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Prepared(
                generation = generation,
                durationMs = durationMs,
            ),
        )
    }

    fun emitPlaying(generation: Long, positionMs: Long, durationMs: Long?) {
        this.positionMs = positionMs
        this.durationMs = durationMs
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Playing(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    fun emitPaused(generation: Long, positionMs: Long, durationMs: Long?) {
        this.positionMs = positionMs
        this.durationMs = durationMs
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Paused(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    fun emitFinished(generation: Long) {
        eventChannel.trySend(DesktopMediaPlayerEvent.Finished(generation = generation))
    }

    fun emitFailure(generation: Long, error: PlaybackError) {
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Failed(
                generation = generation,
                error = error,
            ),
        )
    }
}
```

- [x] **Step 5: Run the fake adapter test**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.playback.FakeDesktopMediaPlayerAdapterTest"
```

Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopMediaPlayerAdapter.kt composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/FakeDesktopMediaPlayerAdapter.kt composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/FakeDesktopMediaPlayerAdapterTest.kt
git commit -m "添加桌面播放适配器测试缝"
```

## Task 3: Implement Serial Desktop Playback Engine

**Files:**
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt`
- Test: `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngineTest.kt`

- [x] **Step 1: Write the engine race tests**

Create `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngineTest.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopVlcjAudioPlayerEngineTest {
    @Test
    fun twentySeeksKeepOnlyLatestProgress(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val engine = testEngine(adapter = adapter)
        val events = mutableListOf<PlaybackEngineEvent>()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }

        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        adapter.emitPrepared(generation = 1L, durationMs = 180_000L)
        repeat(times = 20) { index ->
            engine.seekTo(positionMs = (index + 1) * 1_000L)
        }
        advanceUntilIdle()

        assertEquals(
            expected = PlaybackEngineEvent.ProgressChanged(
                positionMs = 20_000L,
                durationMs = 180_000L,
            ),
            actual = events.last(),
        )
        assertEquals(
            expected = "seek:1:20000",
            actual = adapter.commands.last { command -> command.startsWith(prefix = "seek:") },
        )
        collectJob.cancel()
    }

    @Test
    fun staleEventsFromSkippedMediaAreIgnored(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val engine = testEngine(adapter = adapter)
        val events = mutableListOf<PlaybackEngineEvent>()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }

        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        adapter.emitPrepared(generation = 1L, durationMs = 180_000L)
        engine.skipToIndex(index = 1)
        engine.seekTo(positionMs = 90_000L)
        engine.skipToIndex(index = 2)
        engine.play()
        adapter.emitPlaying(generation = 1L, positionMs = 90_000L, durationMs = 180_000L)
        adapter.emitPrepared(generation = 3L, durationMs = 220_000L)
        adapter.emitPlaying(generation = 3L, positionMs = 0L, durationMs = 220_000L)
        advanceUntilIdle()

        assertFalse(events.contains(PlaybackEngineEvent.ProgressChanged(positionMs = 90_000L, durationMs = 180_000L)))
        assertEquals(
            expected = PlaybackEngineEvent.CurrentMediaChanged(
                songId = "song-3",
                index = 2,
                durationMs = 220_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.CurrentMediaChanged>().last(),
        )
        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Playing,
                positionMs = 0L,
                durationMs = 220_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.StatusChanged>().last(),
        )
        collectJob.cancel()
    }

    @Test
    fun playThenPauseBeforeReadyLeavesMediaPaused(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val engine = testEngine(adapter = adapter)
        val events = mutableListOf<PlaybackEngineEvent>()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }

        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 30_000L)
        engine.play()
        engine.pause()
        adapter.emitPrepared(generation = 1L, durationMs = 180_000L)
        adapter.emitPaused(generation = 1L, positionMs = 30_000L, durationMs = 180_000L)
        advanceUntilIdle()

        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Paused,
                positionMs = 30_000L,
                durationMs = 180_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.StatusChanged>().last(),
        )
        assertFalse(adapter.commands.contains("play:1"))
        collectJob.cancel()
    }

    @Test
    fun releaseIgnoresDelayedCallbacks(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val engine = testEngine(adapter = adapter)
        val events = mutableListOf<PlaybackEngineEvent>()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }

        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        adapter.emitPrepared(generation = 1L, durationMs = 180_000L)
        engine.release()
        adapter.emitFailure(
            generation = 1L,
            error = PlaybackError(
                type = PlaybackErrorType.Unknown,
                songId = "song-1",
                message = "delayed native failure",
            ),
        )
        advanceUntilIdle()

        assertFalse(events.any { event -> event is PlaybackEngineEvent.Failed })
        assertEquals(expected = "release", actual = adapter.commands.last())
        collectJob.cancel()
    }

    private fun TestScope.testEngine(adapter: FakeDesktopMediaPlayerAdapter): DesktopVlcjAudioPlayerEngine {
        return DesktopVlcjAudioPlayerEngine(
            adapter = adapter,
            scope = this,
            dispatcher = StandardTestDispatcher(testScheduler),
            libVlcPluginPath = null,
            progressIntervalMs = 500L,
        )
    }

    private fun mediaItems(): List<PlayableMedia> {
        return listOf(
            media(songId = "song-1", uri = "file:///Users/test/Music/one.mp3", durationMs = 180_000L),
            media(songId = "song-2", uri = "file:///Users/test/Music/two.flac", durationMs = 200_000L),
            media(songId = "song-3", uri = "file:///Users/test/Music/three.aac", durationMs = 220_000L),
        )
    }

    private fun media(songId: String, uri: String, durationMs: Long): PlayableMedia {
        return PlayableMedia(
            songId = songId,
            title = "Title $songId",
            artist = "Artist",
            album = "Album",
            durationMs = durationMs,
            localUri = uri,
            coverArt = CoverArt.HeroLocalMusic,
            mimeType = "audio/mpeg",
        )
    }
}
```

- [x] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngineTest"
```

Expected: FAIL with unresolved reference `DesktopVlcjAudioPlayerEngine`.

- [x] **Step 3: Implement the serial engine**

Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * macOS/Desktop audio engine backed by a desktop media adapter.
 */
class DesktopVlcjAudioPlayerEngine(
    private val adapter: DesktopMediaPlayerAdapter,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineContext = Dispatchers.Default,
    private val libVlcPluginPath: String?,
    private val progressIntervalMs: Long = 500L,
) : AudioPlayerEngine {
    private val eventChannel = Channel<PlaybackEngineEvent>(capacity = Channel.UNLIMITED)
    private val commandChannel = Channel<EngineCommand>(capacity = Channel.UNLIMITED)
    private var queue: List<PlayableMedia> = emptyList()
    private var currentIndex: Int = -1
    private var generation: Long = 0L
    private var pendingPlay: Boolean = false
    private var pendingSeekMs: Long? = null
    private var isPrepared: Boolean = false
    private var isReleased: Boolean = false
    private var progressJob: Job? = null

    override val events: Flow<PlaybackEngineEvent> = eventChannel.receiveAsFlow()

    init {
        scope.launch(context = dispatcher) {
            adapter.events.collect { event ->
                commandChannel.send(EngineCommand.AdapterEventReceived(event = event))
            }
        }
        scope.launch(context = dispatcher) {
            for (command in commandChannel) {
                handle(command = command)
            }
        }
    }

    override suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        val ack = CompletableDeferred<Unit>()
        commandChannel.send(
            EngineCommand.SetQueue(
                items = items,
                startIndex = startIndex,
                startPositionMs = startPositionMs.coerceAtLeast(minimumValue = 0L),
                ack = ack,
            ),
        )
        ack.await()
    }

    override fun play() {
        commandChannel.trySend(EngineCommand.Play)
    }

    override fun pause() {
        commandChannel.trySend(EngineCommand.Pause)
    }

    override fun seekTo(positionMs: Long) {
        commandChannel.trySend(EngineCommand.SeekTo(positionMs = positionMs.coerceAtLeast(minimumValue = 0L)))
    }

    override fun skipToIndex(index: Int) {
        commandChannel.trySend(EngineCommand.SkipToIndex(index = index))
    }

    override fun setPlaybackMode(playbackMode: PlaybackMode) {
        commandChannel.trySend(EngineCommand.SetPlaybackMode(playbackMode = playbackMode))
    }

    override fun stop() {
        commandChannel.trySend(EngineCommand.Stop)
    }

    fun release() {
        commandChannel.trySend(EngineCommand.Release)
    }

    private suspend fun handle(command: EngineCommand) {
        if (isReleased) {
            if (command is EngineCommand.SetQueue) {
                command.ack.complete(Unit)
            }
            return
        }
        when (command) {
            is EngineCommand.SetQueue -> handleSetQueue(command = command)
            EngineCommand.Play -> handlePlay()
            EngineCommand.Pause -> handlePause()
            is EngineCommand.SeekTo -> handleSeekTo(positionMs = command.positionMs)
            is EngineCommand.SkipToIndex -> handleSkipToIndex(index = command.index)
            is EngineCommand.SetPlaybackMode -> Unit
            EngineCommand.Stop -> handleStop()
            EngineCommand.Release -> handleRelease()
            is EngineCommand.AdapterEventReceived -> handleAdapterEvent(event = command.event)
            EngineCommand.ProgressTick -> handleProgressTick()
        }
    }

    private suspend fun handleSetQueue(command: EngineCommand.SetQueue) {
        queue = command.items
        pendingPlay = false
        pendingSeekMs = null
        isPrepared = false
        if (queue.isEmpty()) {
            currentIndex = -1
            nextGeneration()
            eventChannel.send(
                PlaybackEngineEvent.Failed(
                    error = PlaybackError(
                        type = PlaybackErrorType.MissingFile,
                        songId = null,
                        message = "播放队列为空",
                    ),
                ),
            )
            command.ack.complete(Unit)
            return
        }
        currentIndex = command.startIndex.coerceIn(minimumValue = 0, maximumValue = queue.lastIndex)
        pendingSeekMs = command.startPositionMs
        prepareCurrentMedia(startPositionMs = command.startPositionMs)
        command.ack.complete(Unit)
    }

    private suspend fun handlePlay() {
        if (!isPrepared) {
            pendingPlay = true
            return
        }
        pendingPlay = false
        adapter.play(generation = generation)
    }

    private suspend fun handlePause() {
        pendingPlay = false
        if (isPrepared) {
            adapter.pause(generation = generation)
        }
    }

    private suspend fun handleSeekTo(positionMs: Long) {
        pendingSeekMs = positionMs
        if (!isPrepared) {
            return
        }
        adapter.seekTo(generation = generation, positionMs = positionMs)
        eventChannel.send(
            PlaybackEngineEvent.ProgressChanged(
                positionMs = positionMs,
                durationMs = adapter.currentDurationMs(),
            ),
        )
    }

    private suspend fun handleSkipToIndex(index: Int) {
        if (queue.isEmpty() || index !in queue.indices) {
            return
        }
        pendingPlay = false
        pendingSeekMs = null
        currentIndex = index
        prepareCurrentMedia(startPositionMs = 0L)
    }

    private suspend fun handleStop() {
        val activeGeneration = nextGeneration()
        pendingPlay = false
        pendingSeekMs = null
        isPrepared = false
        stopProgressPolling()
        adapter.stop(generation = activeGeneration)
        eventChannel.send(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Idle,
                positionMs = 0L,
                durationMs = null,
            ),
        )
    }

    private suspend fun handleRelease() {
        nextGeneration()
        isReleased = true
        pendingPlay = false
        pendingSeekMs = null
        isPrepared = false
        stopProgressPolling()
        adapter.release()
    }

    private suspend fun handleAdapterEvent(event: DesktopMediaPlayerEvent) {
        if (isReleased || event.generation != generation) {
            return
        }
        if (!isPrepared && event !is DesktopMediaPlayerEvent.Prepared && event !is DesktopMediaPlayerEvent.Failed) {
            return
        }
        when (event) {
            is DesktopMediaPlayerEvent.Prepared -> handlePrepared(event = event)
            is DesktopMediaPlayerEvent.Playing -> {
                startProgressPolling()
                eventChannel.send(
                    PlaybackEngineEvent.StatusChanged(
                        status = PlaybackStatus.Playing,
                        positionMs = event.positionMs,
                        durationMs = event.durationMs,
                    ),
                )
            }
            is DesktopMediaPlayerEvent.Paused -> {
                stopProgressPolling()
                eventChannel.send(
                    PlaybackEngineEvent.StatusChanged(
                        status = PlaybackStatus.Paused,
                        positionMs = event.positionMs,
                        durationMs = event.durationMs,
                    ),
                )
            }
            is DesktopMediaPlayerEvent.Finished -> {
                stopProgressPolling()
                eventChannel.send(PlaybackEngineEvent.Ended)
            }
            is DesktopMediaPlayerEvent.Failed -> {
                stopProgressPolling()
                eventChannel.send(PlaybackEngineEvent.Failed(error = event.error))
            }
        }
    }

    private suspend fun handlePrepared(event: DesktopMediaPlayerEvent.Prepared) {
        isPrepared = true
        val media = queue.getOrNull(index = currentIndex) ?: return
        eventChannel.send(
            PlaybackEngineEvent.CurrentMediaChanged(
                songId = media.songId,
                index = currentIndex,
                durationMs = media.durationMs,
            ),
        )
        val seekMs = pendingSeekMs
        if (seekMs != null && seekMs > 0L) {
            adapter.seekTo(generation = generation, positionMs = seekMs)
            eventChannel.send(
                PlaybackEngineEvent.ProgressChanged(
                    positionMs = seekMs,
                    durationMs = event.durationMs ?: media.durationMs,
                ),
            )
        }
        if (pendingPlay) {
            pendingPlay = false
            adapter.play(generation = generation)
            return
        }
        adapter.pause(generation = generation)
    }

    private suspend fun handleProgressTick() {
        if (!isPrepared || isReleased) {
            return
        }
        eventChannel.send(
            PlaybackEngineEvent.ProgressChanged(
                positionMs = adapter.currentPositionMs(),
                durationMs = adapter.currentDurationMs(),
            ),
        )
    }

    private suspend fun prepareCurrentMedia(startPositionMs: Long) {
        val media = queue.getOrNull(index = currentIndex) ?: return
        val activeGeneration = nextGeneration()
        isPrepared = false
        stopProgressPolling()
        eventChannel.send(
            PlaybackEngineEvent.CurrentMediaChanged(
                songId = media.songId,
                index = currentIndex,
                durationMs = media.durationMs,
            ),
        )
        eventChannel.send(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Loading,
                positionMs = startPositionMs,
                durationMs = media.durationMs,
            ),
        )
        adapter.prepare(
            mediaUri = media.localUri,
            generation = activeGeneration,
            startPositionMs = startPositionMs,
            pluginPath = libVlcPluginPath,
        )
    }

    private fun startProgressPolling() {
        stopProgressPolling()
        progressJob = scope.launch(context = dispatcher) {
            while (isActive) {
                delay(timeMillis = progressIntervalMs)
                commandChannel.send(EngineCommand.ProgressTick)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun nextGeneration(): Long {
        generation += 1L
        return generation
    }
}

private sealed interface EngineCommand {
    data class SetQueue(
        val items: List<PlayableMedia>,
        val startIndex: Int,
        val startPositionMs: Long,
        val ack: CompletableDeferred<Unit>,
    ) : EngineCommand

    data object Play : EngineCommand
    data object Pause : EngineCommand
    data class SeekTo(val positionMs: Long) : EngineCommand
    data class SkipToIndex(val index: Int) : EngineCommand
    data class SetPlaybackMode(val playbackMode: PlaybackMode) : EngineCommand
    data object Stop : EngineCommand
    data object Release : EngineCommand
    data class AdapterEventReceived(val event: DesktopMediaPlayerEvent) : EngineCommand
    data object ProgressTick : EngineCommand
}
```

- [x] **Step 4: Run the desktop engine tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngineTest"
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngineTest.kt
git commit -m "实现桌面播放命令串行引擎"
```

## Task 4: Implement Bundle-First LibVLC Runtime Resolution

**Files:**
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/MacosLibVlcRuntime.kt`
- Test: `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/MacosLibVlcRuntimeTest.kt`

- [x] **Step 1: Write runtime path tests**

Create `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/MacosLibVlcRuntimeTest.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MacosLibVlcRuntimeTest {
    @Test
    fun resolvesBundleRuntimeBeforeDevelopmentFallback(): Unit {
        val tempDir = Files.createTempDirectory("kmp-music-libvlc-test")
        val appDir = tempDir.resolve("KMP Music.app")
        val bundled = appDir.resolve("Contents/Frameworks/LibVLC")
        val plugins = bundled.resolve("plugins")
        plugins.createDirectories()

        val runtime = MacosLibVlcRuntime.resolve(
            appDirectory = appDir,
            developmentVlcApp = tempDir.resolve("VLC.app"),
            allowDevelopmentFallback = true,
        )

        assertEquals(expected = bundled.pathString, actual = runtime.libraryDirectory)
        assertEquals(expected = plugins.pathString, actual = runtime.pluginDirectory)
        assertTrue(runtime.isBundled)
    }

    @Test
    fun resolvesDevelopmentFallbackWhenBundleIsMissing(): Unit {
        val tempDir = Files.createTempDirectory("kmp-music-vlc-fallback-test")
        val vlcApp = tempDir.resolve("VLC.app")
        val lib = vlcApp.resolve("Contents/MacOS/lib")
        val plugins = vlcApp.resolve("Contents/MacOS/plugins")
        lib.createDirectories()
        plugins.createDirectories()

        val runtime = MacosLibVlcRuntime.resolve(
            appDirectory = tempDir.resolve("KMP Music.app"),
            developmentVlcApp = vlcApp,
            allowDevelopmentFallback = true,
        )

        assertEquals(expected = lib.pathString, actual = runtime.libraryDirectory)
        assertEquals(expected = plugins.pathString, actual = runtime.pluginDirectory)
        assertEquals(expected = false, actual = runtime.isBundled)
    }

    @Test
    fun returnsNullWhenReleaseBundleRuntimeIsMissing(): Unit {
        val tempDir = Files.createTempDirectory("kmp-music-vlc-missing-test")

        val runtime = MacosLibVlcRuntime.resolve(
            appDirectory = tempDir.resolve("KMP Music.app"),
            developmentVlcApp = tempDir.resolve("VLC.app"),
            allowDevelopmentFallback = false,
        )

        assertNull(actual = runtime)
    }
}
```

- [x] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.playback.MacosLibVlcRuntimeTest"
```

Expected: FAIL with unresolved reference `MacosLibVlcRuntime`.

- [x] **Step 3: Implement runtime resolution**

Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/MacosLibVlcRuntime.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.pathString

/**
 * Resolves LibVLC from the app bundle first, with an explicit development fallback.
 */
object MacosLibVlcRuntime {
    fun resolve(
        appDirectory: Path = defaultAppDirectory(),
        developmentVlcApp: Path = Path.of("/Applications/VLC.app"),
        allowDevelopmentFallback: Boolean = isDevelopmentRun(),
    ): MacosLibVlcRuntimePath? {
        val bundled = appDirectory.resolve("Contents/Frameworks/LibVLC")
        val bundledPlugins = bundled.resolve("plugins")
        if (Files.isDirectory(bundled) && Files.isDirectory(bundledPlugins)) {
            return MacosLibVlcRuntimePath(
                libraryDirectory = bundled.pathString,
                pluginDirectory = bundledPlugins.pathString,
                isBundled = true,
            )
        }
        if (!allowDevelopmentFallback) {
            return null
        }
        val developmentLib = developmentVlcApp.resolve("Contents/MacOS/lib")
        val developmentPlugins = developmentVlcApp.resolve("Contents/MacOS/plugins")
        if (Files.isDirectory(developmentLib) && Files.isDirectory(developmentPlugins)) {
            return MacosLibVlcRuntimePath(
                libraryDirectory = developmentLib.pathString,
                pluginDirectory = developmentPlugins.pathString,
                isBundled = false,
            )
        }
        return null
    }

    private fun defaultAppDirectory(): Path {
        val codeSource = MacosLibVlcRuntime::class.java.protectionDomain.codeSource?.location?.toURI()
        val startPath = codeSource?.let { uri -> Path.of(uri) } ?: Path.of(".").toAbsolutePath()
        return generateSequence(startPath) { path -> path.parent }
            .firstOrNull { path -> path.fileName?.toString()?.endsWith(".app") == true }
            ?: startPath.toAbsolutePath()
    }

    private fun isDevelopmentRun(): Boolean {
        return System.getProperty("compose.application.resources.dir").isNullOrBlank()
    }
}

data class MacosLibVlcRuntimePath(
    val libraryDirectory: String,
    val pluginDirectory: String,
    val isBundled: Boolean,
)
```

- [x] **Step 4: Run runtime tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.playback.MacosLibVlcRuntimeTest"
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/MacosLibVlcRuntime.kt composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/MacosLibVlcRuntimeTest.kt
git commit -m "添加 macOS LibVLC 运行时发现"
```

## Task 5: Implement vlcj Adapter and Error Mapping

**Files:**
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/VlcjMediaPlayerAdapter.kt`
- Modify: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt`

- [x] **Step 1: Add compile-only usage of the vlcj adapter**

Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/VlcjMediaPlayerAdapter.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.sun.jna.NativeLibrary
import java.io.File
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent

/**
 * vlcj adapter for audio-only Desktop playback.
 */
class VlcjMediaPlayerAdapter(
    private val runtimePath: MacosLibVlcRuntimePath?,
) : DesktopMediaPlayerAdapter {
    private val eventChannel = Channel<DesktopMediaPlayerEvent>(capacity = Channel.UNLIMITED)
    private val component: CallbackMediaPlayerComponent
    private var activeGeneration: Long = 0L
    private var activeSongId: String? = null

    override val events: Flow<DesktopMediaPlayerEvent> = eventChannel.receiveAsFlow()

    init {
        val resolvedRuntime = runtimePath
        val libVlcArgs: Array<String> = buildList {
            add("--no-video")
            if (resolvedRuntime != null) {
                add("--plugin-path=${resolvedRuntime.pluginDirectory}")
            }
        }.toTypedArray()
        if (resolvedRuntime != null) {
            NativeLibrary.addSearchPath(
                RuntimeUtil.getLibVlcLibraryName(),
                resolvedRuntime.libraryDirectory,
            )
        } else {
            NativeDiscovery().discover()
        }
        component = CallbackMediaPlayerComponent(*libVlcArgs)
        component.mediaPlayer().events().addMediaPlayerEventListener(
            object : MediaPlayerEventAdapter() {
                override fun playing(mediaPlayer: MediaPlayer) {
                    eventChannel.trySend(
                        DesktopMediaPlayerEvent.Playing(
                            generation = activeGeneration,
                            positionMs = currentPositionMsValue(mediaPlayer = mediaPlayer),
                            durationMs = currentDurationMsValue(mediaPlayer = mediaPlayer),
                        ),
                    )
                }

                override fun paused(mediaPlayer: MediaPlayer) {
                    eventChannel.trySend(
                        DesktopMediaPlayerEvent.Paused(
                            generation = activeGeneration,
                            positionMs = currentPositionMsValue(mediaPlayer = mediaPlayer),
                            durationMs = currentDurationMsValue(mediaPlayer = mediaPlayer),
                        ),
                    )
                }

                override fun finished(mediaPlayer: MediaPlayer) {
                    eventChannel.trySend(DesktopMediaPlayerEvent.Finished(generation = activeGeneration))
                }

                override fun error(mediaPlayer: MediaPlayer) {
                    eventChannel.trySend(
                        DesktopMediaPlayerEvent.Failed(
                            generation = activeGeneration,
                            error = PlaybackError(
                                type = PlaybackErrorType.Unknown,
                                songId = activeSongId,
                                message = "播放失败，已尝试播放下一首。",
                            ),
                        ),
                    )
                }
            },
        )
    }

    override suspend fun prepare(
        mediaUri: String,
        generation: Long,
        startPositionMs: Long,
        pluginPath: String?,
    ) {
        activeGeneration = generation
        activeSongId = null
        val media = mediaUri.toVlcjMediaLocation()
        val mediaPlayer = component.mediaPlayer()
        val accepted = mediaPlayer.media().prepare(media)
        if (!accepted) {
            eventChannel.trySend(
                DesktopMediaPlayerEvent.Failed(
                    generation = generation,
                    error = mediaUri.toPlaybackError(),
                ),
            )
            return
        }
        if (startPositionMs > 0L) {
            mediaPlayer.controls().setTime(startPositionMs)
        }
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Prepared(
                generation = generation,
                durationMs = currentDurationMsValue(mediaPlayer = mediaPlayer),
            ),
        )
    }

    override suspend fun play(generation: Long) {
        activeGeneration = generation
        component.mediaPlayer().controls().play()
    }

    override suspend fun pause(generation: Long) {
        activeGeneration = generation
        component.mediaPlayer().controls().pause()
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Paused(
                generation = generation,
                positionMs = currentPositionMs(),
                durationMs = currentDurationMs(),
            ),
        )
    }

    override suspend fun seekTo(generation: Long, positionMs: Long) {
        activeGeneration = generation
        component.mediaPlayer().controls().setTime(positionMs)
    }

    override suspend fun stop(generation: Long) {
        activeGeneration = generation
        component.mediaPlayer().controls().stop()
    }

    override suspend fun currentPositionMs(): Long {
        return currentPositionMsValue(mediaPlayer = component.mediaPlayer())
    }

    override suspend fun currentDurationMs(): Long? {
        return currentDurationMsValue(mediaPlayer = component.mediaPlayer())
    }

    override suspend fun release() {
        component.release()
    }

    private fun currentPositionMsValue(mediaPlayer: MediaPlayer): Long {
        return mediaPlayer.status().time().coerceAtLeast(minimumValue = 0L)
    }

    private fun currentDurationMsValue(mediaPlayer: MediaPlayer): Long? {
        return mediaPlayer.status().length().takeIf { duration -> duration > 0L }
    }

    private fun String.toVlcjMediaLocation(): String {
        if (startsWith(prefix = "file:")) {
            return File(java.net.URI(this)).absolutePath
        }
        return this
    }

    private fun String.toPlaybackError(): PlaybackError {
        val file = runCatching { File(java.net.URI(this)) }.getOrNull()
        val type = when {
            file != null && !file.exists() -> PlaybackErrorType.MissingFile
            file != null && !file.canRead() -> PlaybackErrorType.PermissionDenied
            else -> PlaybackErrorType.Unknown
        }
        return PlaybackError(
            type = type,
            songId = null,
            message = when (type) {
                PlaybackErrorType.MissingFile -> "文件不存在或已移动，请重新扫描本地音乐。"
                PlaybackErrorType.PermissionDenied -> "无法访问该音乐文件，请在系统设置或文件夹授权中允许访问后重试。"
                PlaybackErrorType.UnsupportedFormat -> "当前音频格式暂不支持，已尝试播放下一首。"
                PlaybackErrorType.EngineUnavailable -> "播放器组件不可用，请重新安装应用或联系开发者。"
                PlaybackErrorType.Unknown -> "播放失败，已尝试播放下一首。"
            },
        )
    }
}
```

- [x] **Step 2: Add song id propagation from the engine**

Modify `DesktopMediaPlayerAdapter.prepare` in `DesktopMediaPlayerAdapter.kt` to include `songId`:

```kotlin
suspend fun prepare(
    songId: String,
    mediaUri: String,
    generation: Long,
    startPositionMs: Long,
    pluginPath: String?,
)
```

Modify every call site:

```kotlin
adapter.prepare(
    songId = media.songId,
    mediaUri = media.localUri,
    generation = activeGeneration,
    startPositionMs = startPositionMs,
    pluginPath = libVlcPluginPath,
)
```

Modify `FakeDesktopMediaPlayerAdapter.prepare` to record the song id:

```kotlin
override suspend fun prepare(
    songId: String,
    mediaUri: String,
    generation: Long,
    startPositionMs: Long,
    pluginPath: String?,
) {
    positionMs = startPositionMs
    commands += "prepare:$songId:$mediaUri:$generation:$startPositionMs"
}
```

Modify `FakeDesktopMediaPlayerAdapterTest` prepare call and expected command:

```kotlin
adapter.prepare(
    songId = "song-1",
    mediaUri = "file:///Users/test/Music/song.mp3",
    generation = 7L,
    startPositionMs = 12_000L,
    pluginPath = "/Applications/KMP Music.app/Contents/Frameworks/LibVLC/plugins",
)
```

```kotlin
assertEquals(
    expected = listOf(
        "prepare:song-1:file:///Users/test/Music/song.mp3:7:12000",
        "play:7",
    ),
    actual = adapter.commands,
)
```

Modify `VlcjMediaPlayerAdapter.prepare` to save the active song id:

```kotlin
override suspend fun prepare(
    songId: String,
    mediaUri: String,
    generation: Long,
    startPositionMs: Long,
    pluginPath: String?,
) {
    activeGeneration = generation
    activeSongId = songId
    val media = mediaUri.toVlcjMediaLocation()
    val mediaPlayer = component.mediaPlayer()
    val accepted = mediaPlayer.media().prepare(media)
    if (!accepted) {
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Failed(
                generation = generation,
                error = mediaUri.toPlaybackError(songId = songId),
            ),
        )
        return
    }
    if (startPositionMs > 0L) {
        mediaPlayer.controls().setTime(startPositionMs)
    }
    eventChannel.trySend(
        DesktopMediaPlayerEvent.Prepared(
            generation = generation,
            durationMs = currentDurationMsValue(mediaPlayer = mediaPlayer),
        ),
    )
}
```

Change `toPlaybackError` signature:

```kotlin
private fun String.toPlaybackError(songId: String): PlaybackError {
    val file = runCatching { File(java.net.URI(this)) }.getOrNull()
    val type = when {
        file != null && !file.exists() -> PlaybackErrorType.MissingFile
        file != null && !file.canRead() -> PlaybackErrorType.PermissionDenied
        else -> PlaybackErrorType.Unknown
    }
    return PlaybackError(
        type = type,
        songId = songId,
        message = when (type) {
            PlaybackErrorType.MissingFile -> "文件不存在或已移动，请重新扫描本地音乐。"
            PlaybackErrorType.PermissionDenied -> "无法访问该音乐文件，请在系统设置或文件夹授权中允许访问后重试。"
            PlaybackErrorType.UnsupportedFormat -> "当前音频格式暂不支持，已尝试播放下一首。"
            PlaybackErrorType.EngineUnavailable -> "播放器组件不可用，请重新安装应用或联系开发者。"
            PlaybackErrorType.Unknown -> "播放失败，已尝试播放下一首。"
        },
    )
}
```

- [x] **Step 3: Compile Desktop main**

Run:

```bash
./gradlew :composeApp:compileKotlinDesktop
```

Expected: PASS.

- [x] **Step 4: Rerun desktop playback tests after signature changes**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.playback.*"
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback
git commit -m "接入 macOS vlcj 音频适配器"
```

## Task 6: Add Desktop Room Database and Process Session

**Files:**
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/data/DesktopPlaybackDatabase.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt`
- Modify: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopMain.kt`
- Test: `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/data/DesktopPlaybackDatabaseTest.kt`

- [x] **Step 1: Write the Desktop database path test**

Create `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/data/DesktopPlaybackDatabaseTest.kt`:

```kotlin
package com.yanhao.kmpmusic.data

import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopPlaybackDatabaseTest {
    @Test
    fun databasePathUsesMacosApplicationSupport(): Unit {
        val path = defaultDesktopPlaybackDatabasePath(
            userHome = "/Users/tester",
        )

        assertTrue(path.endsWith("Library/Application Support/KMP Music/kmp_music_playback.db"))
    }
}
```

- [x] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.data.DesktopPlaybackDatabaseTest"
```

Expected: FAIL with unresolved reference `defaultDesktopPlaybackDatabasePath`.

- [x] **Step 3: Add the Desktop Room builder**

Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/data/DesktopPlaybackDatabase.kt`:

```kotlin
package com.yanhao.kmpmusic.data

import androidx.room3.Room
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import java.io.File

/**
 * Creates the Desktop playback database in the user's app support directory.
 */
fun createDesktopPlaybackDatabase(
    userHome: String = System.getProperty("user.home"),
): PlaybackDatabase {
    val databasePath = defaultDesktopPlaybackDatabasePath(userHome = userHome)
    File(databasePath).parentFile.mkdirs()
    return createPlaybackDatabase(
        builder = Room.databaseBuilder<PlaybackDatabase>(
            name = databasePath,
        ),
    )
}

/**
 * Returns the macOS application support database path.
 */
fun defaultDesktopPlaybackDatabasePath(userHome: String): String {
    return File(
        File(userHome, "Library/Application Support/KMP Music"),
        "kmp_music_playback.db",
    ).absolutePath
}
```

- [x] **Step 4: Add process-level Desktop session**

Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt`:

```kotlin
package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.data.DesktopFolderMusicScanner
import com.yanhao.kmpmusic.data.PersistentFavoritesRepository
import com.yanhao.kmpmusic.data.createDesktopPlaybackDatabase
import com.yanhao.kmpmusic.domain.persistence.RoomPlaybackSnapshotStore
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngine
import com.yanhao.kmpmusic.playback.MacosLibVlcRuntime
import com.yanhao.kmpmusic.playback.VlcjMediaPlayerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Desktop process-level session that owns playback resources across Compose recompositions.
 */
object DesktopPlaybackSession {
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val playbackDatabase = createDesktopPlaybackDatabase()
    private val runtimePath = MacosLibVlcRuntime.resolve()
    private val audioEngine = DesktopVlcjAudioPlayerEngine(
        adapter = VlcjMediaPlayerAdapter(runtimePath = runtimePath),
        scope = sessionScope,
        libVlcPluginPath = runtimePath?.pluginDirectory,
    )
    private var hasRequestedPlaybackRestore: Boolean = false

    val controller: MusicAppController by lazy {
        val favoriteSongDao = playbackDatabase.favoriteSongDao()
        val favoritesRepository = runBlocking {
            PersistentFavoritesRepository(
                favoriteSongDao = favoriteSongDao,
                initialLikedSongIds = PersistentFavoritesRepository.loadInitialLikedSongIds(
                    favoriteSongDao = favoriteSongDao,
                ),
                nowMillis = { System.currentTimeMillis() },
            )
        }
        MusicAppController(
            localMusicScanner = DesktopFolderMusicScanner(),
            audioPlayerEngine = audioEngine,
            playbackSnapshotStore = RoomPlaybackSnapshotStore(
                database = playbackDatabase,
                nowMillis = { System.currentTimeMillis() },
            ),
            injectedFavoritesRepository = favoritesRepository,
            controllerScope = sessionScope,
            nowMillis = { System.currentTimeMillis() },
        )
    }

    fun ensurePlaybackSnapshotRestoreRequested() {
        if (hasRequestedPlaybackRestore) {
            return
        }
        hasRequestedPlaybackRestore = true
        sessionScope.launch {
            controller.restorePlaybackSnapshot()
        }
    }

    fun close() {
        controller.persistPlaybackSnapshotForServiceTeardown(
            positionMs = controller.uiState.playbackPositionMs,
            durationMs = controller.uiState.playbackDurationMs,
        )
        audioEngine.release()
    }
}
```

- [x] **Step 5: Wire DesktopMain to the session**

Modify `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopMain.kt`:

```kotlin
package com.yanhao.kmpmusic

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp

/**
 * Desktop 入口。
 */
fun main() = application {
    Window(
        onCloseRequest = {
            DesktopPlaybackSession.close()
            exitApplication()
        },
        title = "KMP Music",
        state = WindowState(width = 430.dp, height = 930.dp),
    ) {
        LaunchedEffect(Unit) {
            DesktopPlaybackSession.ensurePlaybackSnapshotRestoreRequested()
        }
        App(controller = DesktopPlaybackSession.controller)
    }
}
```

- [x] **Step 6: Run Desktop database and compile checks**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.data.DesktopPlaybackDatabaseTest"
./gradlew :composeApp:compileKotlinDesktop
```

Expected: both PASS.

- [x] **Step 7: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/data/DesktopPlaybackDatabase.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopMain.kt composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/data/DesktopPlaybackDatabaseTest.kt
git commit -m "接入桌面播放会话和持久化"
```

## Task 7: Add User-Facing Playback Error Copy

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/PlaybackErrorMessage.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/PlayerScreen.kt`
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/PlaybackErrorMessageTest.kt`

- [x] **Step 1: Write error copy tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/PlaybackErrorMessageTest.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackErrorMessageTest {
    @Test
    fun engineUnavailableDoesNotAskUserToInstallVlc(): Unit {
        val error = PlaybackError(
            type = PlaybackErrorType.EngineUnavailable,
            songId = "song-1",
            message = "libvlc missing",
        )

        assertEquals(
            expected = "《山海》播放器组件不可用，请重新安装应用或联系开发者。",
            actual = error.userMessage(songTitle = "山海"),
        )
    }

    @Test
    fun unknownSongUsesCurrentSongFallback(): Unit {
        val error = PlaybackError(
            type = PlaybackErrorType.Unknown,
            songId = null,
            message = "native error",
        )

        assertEquals(
            expected = "当前歌曲播放失败，已尝试播放下一首。",
            actual = error.userMessage(songTitle = null),
        )
    }
}
```

- [x] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.PlaybackErrorMessageTest"
```

Expected: FAIL with unresolved reference `userMessage`.

- [x] **Step 3: Add the shared error copy helper**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/PlaybackErrorMessage.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType

/**
 * Converts diagnostic playback failures into user-facing copy.
 */
fun PlaybackError.userMessage(songTitle: String?): String {
    val subject = songTitle?.takeIf { title -> title.isNotBlank() }?.let { title -> "《$title》" } ?: "当前歌曲"
    val detail = when (type) {
        PlaybackErrorType.EngineUnavailable -> "播放器组件不可用，请重新安装应用或联系开发者。"
        PlaybackErrorType.MissingFile -> "文件不存在或已移动，请重新扫描本地音乐。"
        PlaybackErrorType.PermissionDenied -> "无法访问该音乐文件，请在系统设置或文件夹授权中允许访问后重试。"
        PlaybackErrorType.UnsupportedFormat -> "当前音频格式暂不支持，已尝试播放下一首。"
        PlaybackErrorType.Unknown -> "播放失败，已尝试播放下一首。"
    }
    return "$subject$detail"
}
```

- [x] **Step 4: Render friendly copy on the player screen**

Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/PlayerScreen.kt` imports:

```kotlin
import com.yanhao.kmpmusic.feature.app.userMessage
```

Modify the error text:

```kotlin
if (playbackError != null) {
    Text(
        text = playbackError.userMessage(songTitle = song.title),
        color = MaterialTheme.colorScheme.error,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
    )
}
```

- [x] **Step 5: Run error copy tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.PlaybackErrorMessageTest"
./gradlew :composeApp:compileKotlinDesktop
```

Expected: both PASS.

- [x] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/PlaybackErrorMessage.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/PlayerScreen.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/PlaybackErrorMessageTest.kt
git commit -m "统一播放错误用户文案"
```

## Task 8: Add LibVLC Download and Extraction Helpers

**Files:**
- Create: `composeApp/src/desktopMain/packaging/macos-libvlc/download-macos-arm64-libvlc.sh`
- Create: `composeApp/src/desktopMain/packaging/macos-libvlc/extract-macos-arm64-libvlc.sh`
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Add the download helper**

Create `composeApp/src/desktopMain/packaging/macos-libvlc/download-macos-arm64-libvlc.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

download_dir="$1"
url="https://download.videolan.org/pub/videolan/vlc/last/macosx/vlc-3.0.23-arm64.dmg"
expected_sha256="fc6fac08d87f538517d44aca0c5e7a244b67c8c4cb589bf478363a7315fd5e0d"
dmg_path="${download_dir}/vlc-3.0.23-arm64.dmg"

mkdir -p "${download_dir}"

if [[ ! -f "${dmg_path}" ]]; then
  curl -L "${url}" -o "${dmg_path}"
fi

actual_sha256="$(shasum -a 256 "${dmg_path}" | awk '{print $1}')"
if [[ "${actual_sha256}" != "${expected_sha256}" ]]; then
  echo "LibVLC SHA-256 mismatch"
  echo "expected=${expected_sha256}"
  echo "actual=${actual_sha256}"
  exit 1
fi

echo "${dmg_path}"
```

- [ ] **Step 2: Add the extraction helper**

Create `composeApp/src/desktopMain/packaging/macos-libvlc/extract-macos-arm64-libvlc.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

dmg_path="$1"
output_dir="$2"
mount_output="$(hdiutil attach -nobrowse -readonly "${dmg_path}")"
mount_point="$(printf '%s\n' "${mount_output}" | awk '/\/Volumes\// {for (i=3; i<=NF; i++) printf "%s%s", $i, (i<NF ? OFS : ORS)}' | tail -n 1)"

if [[ -z "${mount_point}" || ! -d "${mount_point}" ]]; then
  echo "Unable to mount VLC DMG"
  exit 1
fi

trap 'hdiutil detach "${mount_point}" >/dev/null' EXIT

vlc_app="${mount_point}/VLC.app"
lib_source="${vlc_app}/Contents/MacOS/lib"
plugins_source="${vlc_app}/Contents/MacOS/plugins"
license_source="${vlc_app}/Contents/Resources"

rm -rf "${output_dir}"
mkdir -p "${output_dir}/lib" "${output_dir}/plugins" "${output_dir}/licenses"
cp -R "${lib_source}/." "${output_dir}/lib/"
cp -R "${plugins_source}/." "${output_dir}/plugins/"

if [[ -d "${license_source}" ]]; then
  find "${license_source}" -maxdepth 2 \( -iname '*copying*' -o -iname '*license*' -o -iname '*notice*' \) -print0 |
    while IFS= read -r -d '' file_path; do
      cp "${file_path}" "${output_dir}/licenses/"
    done
fi

echo "${output_dir}"
```

- [ ] **Step 3: Make helpers executable**

Run:

```bash
chmod +x composeApp/src/desktopMain/packaging/macos-libvlc/download-macos-arm64-libvlc.sh composeApp/src/desktopMain/packaging/macos-libvlc/extract-macos-arm64-libvlc.sh
```

Expected: command exits 0.

- [ ] **Step 4: Add Gradle tasks for download and extraction**

Modify `composeApp/build.gradle.kts` after `compose.desktop`:

```kotlin
val macosLibVlcDownloadDir = layout.buildDirectory.dir("macos-libvlc/download")
val macosLibVlcRuntimeDir = layout.buildDirectory.dir("macos-libvlc/runtime/LibVLC")

tasks.register<Exec>("downloadMacosArm64LibVlc") {
    workingDir = projectDir
    commandLine(
        "bash",
        "$projectDir/src/desktopMain/packaging/macos-libvlc/download-macos-arm64-libvlc.sh",
        macosLibVlcDownloadDir.get().asFile.absolutePath,
    )
}

tasks.register<Exec>("extractMacosArm64LibVlc") {
    dependsOn("downloadMacosArm64LibVlc")
    workingDir = projectDir
    commandLine(
        "bash",
        "$projectDir/src/desktopMain/packaging/macos-libvlc/extract-macos-arm64-libvlc.sh",
        macosLibVlcDownloadDir.get().file("vlc-3.0.23-arm64.dmg").asFile.absolutePath,
        macosLibVlcRuntimeDir.get().asFile.absolutePath,
    )
}
```

- [ ] **Step 5: Run extraction on Apple Silicon macOS**

Run:

```bash
./gradlew :composeApp:extractMacosArm64LibVlc
```

Expected:

- build exits 0;
- `composeApp/build/macos-libvlc/runtime/LibVLC/lib` exists;
- `composeApp/build/macos-libvlc/runtime/LibVLC/plugins` exists;
- SHA-256 mismatch stops the task before extraction.

- [ ] **Step 6: Commit**

```bash
git add composeApp/build.gradle.kts composeApp/src/desktopMain/packaging/macos-libvlc/download-macos-arm64-libvlc.sh composeApp/src/desktopMain/packaging/macos-libvlc/extract-macos-arm64-libvlc.sh
git commit -m "添加 macOS LibVLC 下载提取任务"
```

## Task 9: Stage LibVLC Into the App Bundle and Verify Dynamic Libraries

**Files:**
- Modify: `composeApp/build.gradle.kts`
- Create: `composeApp/src/desktopMain/packaging/macos-libvlc/verify-macos-app-libvlc.sh`

- [ ] **Step 1: Add the verification helper**

Create `composeApp/src/desktopMain/packaging/macos-libvlc/verify-macos-app-libvlc.sh`:

```bash
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
```

- [ ] **Step 2: Make the verification helper executable**

Run:

```bash
chmod +x composeApp/src/desktopMain/packaging/macos-libvlc/verify-macos-app-libvlc.sh
```

Expected: command exits 0.

- [ ] **Step 3: Add staging tasks around Compose Desktop packaging**

Modify `composeApp/build.gradle.kts` after the extraction task:

```kotlin
val releaseAppName = "KMP Music.app"
val releaseAppDir = layout.buildDirectory.dir("compose/binaries/main-release/app/$releaseAppName")

tasks.register<Copy>("stageMacosArm64LibVlcIntoReleaseApp") {
    dependsOn("extractMacosArm64LibVlc", "createReleaseDistributable")
    from(macosLibVlcRuntimeDir)
    into(releaseAppDir.map { directory -> directory.dir("Contents/Frameworks/LibVLC") })
}

tasks.register<Exec>("verifyMacosArm64ReleaseApp") {
    dependsOn("stageMacosArm64LibVlcIntoReleaseApp")
    workingDir = projectDir
    commandLine(
        "bash",
        "$projectDir/src/desktopMain/packaging/macos-libvlc/verify-macos-app-libvlc.sh",
        releaseAppDir.get().asFile.absolutePath,
    )
}

tasks.named("packageReleaseDmg") {
    dependsOn("stageMacosArm64LibVlcIntoReleaseApp")
}
```

- [ ] **Step 4: Add explicit release signing note to the task comments**

Insert this comment above `stageMacosArm64LibVlcIntoReleaseApp`:

```kotlin
// The release signing/notarization pipeline must sign nested LibVLC code after this task and
// sign the outer app last. Running packageReleaseDmg before nested signing invalidates release acceptance.
```

- [ ] **Step 5: Run release app staging**

Run:

```bash
./gradlew :composeApp:stageMacosArm64LibVlcIntoReleaseApp
```

Expected:

- build exits 0;
- `composeApp/build/compose/binaries/main-release/app/KMP Music.app/Contents/Frameworks/LibVLC/lib` exists;
- `composeApp/build/compose/binaries/main-release/app/KMP Music.app/Contents/Frameworks/LibVLC/plugins` exists.

- [ ] **Step 6: Run verification on a signed local build**

Run:

```bash
./gradlew :composeApp:verifyMacosArm64ReleaseApp
```

Expected:

- PASS on a Developer ID signed release app;
- FAIL with a clear `codesign` or `spctl` error on an unsigned local app.

- [ ] **Step 7: Commit**

```bash
git add composeApp/build.gradle.kts composeApp/src/desktopMain/packaging/macos-libvlc/verify-macos-app-libvlc.sh
git commit -m "添加 macOS LibVLC 打包验收任务"
```

## Task 10: Verify App Runtime Behavior With Local VLC Fallback

**Files:**
- Modify: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt`

- [ ] **Step 1: Add EngineUnavailable fallback when LibVLC cannot resolve**

Modify `DesktopPlaybackSession` engine creation:

```kotlin
private val runtimePath = MacosLibVlcRuntime.resolve()
private val audioEngine = if (runtimePath == null) {
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
```

Create `UnavailableDesktopMediaPlayerAdapter` in `VlcjMediaPlayerAdapter.kt`:

```kotlin
class UnavailableDesktopMediaPlayerAdapter : DesktopMediaPlayerAdapter {
    private val eventChannel = Channel<DesktopMediaPlayerEvent>(capacity = Channel.UNLIMITED)
    override val events: Flow<DesktopMediaPlayerEvent> = eventChannel.receiveAsFlow()

    override suspend fun prepare(
        songId: String,
        mediaUri: String,
        generation: Long,
        startPositionMs: Long,
        pluginPath: String?,
    ) {
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Failed(
                generation = generation,
                error = PlaybackError(
                    type = PlaybackErrorType.EngineUnavailable,
                    songId = songId,
                    message = "播放器组件不可用，请重新安装应用或联系开发者。",
                ),
            ),
        )
    }

    override suspend fun play(generation: Long) = Unit
    override suspend fun pause(generation: Long) = Unit
    override suspend fun seekTo(generation: Long, positionMs: Long) = Unit
    override suspend fun stop(generation: Long) = Unit
    override suspend fun currentPositionMs(): Long = 0L
    override suspend fun currentDurationMs(): Long? = null
    override suspend fun release() = Unit
}
```

Required imports:

```kotlin
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
```

- [ ] **Step 2: Run Desktop app with development VLC fallback**

Run:

```bash
./gradlew :composeApp:desktopRun
```

Expected manual result on Apple Silicon macOS with VLC installed:

- selecting a folder with MP3, FLAC, or AAC songs succeeds;
- clicking a scanned song enters `Loading` then `Playing`;
- pause/play, next/previous, seek, and queue selection update audio and UI consistently;
- closing and reopening restores queue and paused position without auto-playing.

- [ ] **Step 3: Run compile and tests after manual smoke test**

Run:

```bash
./gradlew :composeApp:compileKotlinDesktop :composeApp:desktopTest
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/VlcjMediaPlayerAdapter.kt
git commit -m "验证桌面真实播放开发链路"
```

## Task 11: Run Full Automated Verification

**Files:**
- No source file changes.

- [ ] **Step 1: Run focused playback tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.playback.*"
```

Expected: PASS.

- [ ] **Step 2: Run shared playback regression tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.*"
```

Expected: PASS.

- [ ] **Step 3: Run all Desktop tests and compile**

Run:

```bash
./gradlew :composeApp:compileKotlinDesktop :composeApp:desktopTest
```

Expected: PASS.

- [ ] **Step 4: Run Android compile regression**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS. This proves common UI/error copy changes did not break the Android target.

- [ ] **Step 5: Commit verification-only fixes**

If the verification commands reveal compile or test failures caused by the tasks above, fix the failing source files and commit the fixes:

```bash
git add composeApp/src gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "修复 macOS 播放验证问题"
```

## Task 12: Perform Apple Silicon DMG Acceptance

**Files:**
- Modify: `composeApp/src/desktopMain/packaging/macos-libvlc/SOURCE_RECORD.md`

- [ ] **Step 1: Build the release app and stage LibVLC**

Run:

```bash
./gradlew :composeApp:stageMacosArm64LibVlcIntoReleaseApp
```

Expected: `KMP Music.app/Contents/Frameworks/LibVLC/lib` and `plugins` exist in the release app bundle.

- [ ] **Step 2: Sign nested code and outer app**

Run with the project Developer ID identity:

```bash
DEVELOPER_ID_APPLICATION_IDENTITY="${DEVELOPER_ID_APPLICATION_IDENTITY:?Set DEVELOPER_ID_APPLICATION_IDENTITY to the exact Developer ID Application signing identity}"
codesign --force --options runtime --timestamp --sign "${DEVELOPER_ID_APPLICATION_IDENTITY}" "composeApp/build/compose/binaries/main-release/app/KMP Music.app/Contents/Frameworks/LibVLC"
codesign --force --deep --options runtime --timestamp --sign "${DEVELOPER_ID_APPLICATION_IDENTITY}" "composeApp/build/compose/binaries/main-release/app/KMP Music.app"
```

Expected: both commands exit 0.

- [ ] **Step 3: Verify signed app**

Run:

```bash
./gradlew :composeApp:verifyMacosArm64ReleaseApp
```

Expected: PASS.

- [ ] **Step 4: Package DMG**

Run:

```bash
./gradlew :composeApp:packageReleaseDmg
```

Expected: release DMG is created under `composeApp/build/compose/binaries/main-release/dmg/`.

- [ ] **Step 5: Notarize and staple**

Run:

```bash
xcrun notarytool submit "composeApp/build/compose/binaries/main-release/dmg/KMP Music-1.0.0.dmg" --keychain-profile "KMP_MUSIC_NOTARY" --wait
xcrun stapler staple "composeApp/build/compose/binaries/main-release/dmg/KMP Music-1.0.0.dmg"
```

Expected: notarization returns `Accepted`, and stapler exits 0.

- [ ] **Step 6: Test without system VLC**

On an Apple Silicon macOS machine without `/Applications/VLC.app`, Homebrew VLC, or other system LibVLC:

```bash
xattr -dr com.apple.quarantine "/Applications/KMP Music.app"
open "/Applications/KMP Music.app"
```

Expected manual result:

- scanning local MP3, FLAC, and AAC files succeeds;
- clicking a song plays real audio;
- mini player and player page show real progress;
- seek moves audio and UI to the target position;
- next/previous and playback modes match Android in-app semantics;
- moving the current file shows missing-file copy;
- corrupt or unsupported audio shows unsupported-format or unknown copy;
- restart restores queue and paused position without auto-playing.

- [ ] **Step 7: Test EngineUnavailable acceptance**

Temporarily move the bundled runtime out of the app:

```bash
mv "/Applications/KMP Music.app/Contents/Frameworks/LibVLC" "/Applications/KMP Music.app/Contents/Frameworks/LibVLC.disabled"
open "/Applications/KMP Music.app"
```

Expected: playback fails with “播放器组件不可用，请重新安装应用或联系开发者。” and does not ask the user to install VLC.

Restore the runtime:

```bash
mv "/Applications/KMP Music.app/Contents/Frameworks/LibVLC.disabled" "/Applications/KMP Music.app/Contents/Frameworks/LibVLC"
```

- [ ] **Step 8: Update source record with extraction date**

Append verification dates with an explicit Asia/Shanghai date:

```bash
verification_date="$(TZ=Asia/Shanghai date '+%Y-%m-%d Asia/Shanghai')"
{
  printf '\nExtraction verified at: %s\n' "${verification_date}"
  printf 'DMG notarization verified at: %s\n' "${verification_date}"
} >> composeApp/src/desktopMain/packaging/macos-libvlc/SOURCE_RECORD.md
```

- [ ] **Step 9: Commit release acceptance record**

```bash
git add composeApp/src/desktopMain/packaging/macos-libvlc/SOURCE_RECORD.md
git commit -m "记录 macOS LibVLC 打包验收"
```

## Final Verification Checklist

- [ ] `./gradlew :composeApp:compileKotlinDesktop :composeApp:desktopTest` passes.
- [ ] `./gradlew :composeApp:compileDebugKotlinAndroid` passes.
- [ ] Development run plays through installed VLC fallback.
- [ ] Release app contains `Contents/Frameworks/LibVLC/lib` and `Contents/Frameworks/LibVLC/plugins`.
- [ ] Release app plays without system VLC installed.
- [ ] Removing bundled LibVLC shows `EngineUnavailable` copy.
- [ ] `codesign --verify --deep --strict --verbose=2` accepts the app.
- [ ] `spctl -a -t exec -vv` accepts the app.
- [ ] `otool -L` check finds no app-external non-system dynamic library paths.
- [ ] DMG notarization and stapling succeed.
- [ ] License/source record includes URL, SHA-256, architecture, version, license notes, and extraction verification date.

## 举一反三 Review

- Threading/race generalization: the plan avoids one-off seek or skip fixes by making the Desktop engine a single command owner with generation tokens. The tests cover repeated seek, stale callbacks, pending play/pause, and release callbacks so future adapter changes keep the same invariant.
- Platform-boundary generalization: vlcj, JNA path setup, app bundle lookup, `/Applications/VLC.app` fallback, and packaging paths remain in `desktopMain` or Gradle files. Shared coordinator and UI state stay platform neutral.
- Distribution generalization: the plan treats LibVLC as a release artifact with source record, SHA-256 validation, app bundle staging, signing, `spctl`, `otool`, and no-system-VLC acceptance rather than relying on a developer machine install.
- User-action generalization: errors are not left as native diagnostic strings. The shared user copy covers all common `PlaybackErrorType` values and prevents `EngineUnavailable` from becoming “please install VLC.”

## Three-Round Cross-Review

### Round 1: Architecture and Concurrency

Pass: The plan has a dedicated adapter seam, serial engine loop, stale event dropping, and tests for the four races called out in the spec.

Adjustment made in the plan: `release()` remains desktop-specific on `DesktopVlcjAudioPlayerEngine` so `AudioPlayerEngine` common API does not gain a platform lifecycle method.

### Round 2: Packaging, Signing, and Compliance

Pass: The plan pins `vlc-3.0.23-arm64.dmg`, verifies SHA-256, stages LibVLC into `Contents/Frameworks/LibVLC`, and adds release verification for code signing, Gatekeeper assessment, external dynamic-library paths, and no-system-VLC runtime behavior.

Adjustment made in the plan: packaging tasks are separate from built-in DMG creation so nested code can be staged and signed before final notarized distribution.

### Round 3: Testability and Execution Safety

Pass: Risky playback behavior is tested with a fake desktop adapter under `desktopTest`; shared error copy is tested under common tests; Android compile regression is included because common UI code changes affect Android too.

Adjustment made in the plan: every task has a focused commit command and the final checklist includes both automated and manual acceptance gates.
