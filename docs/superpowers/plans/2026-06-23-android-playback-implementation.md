# Android Playback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build Android real local-audio playback from scanned `content://` songs, with shared playback state, Room3 recovery, Media3 background playback, and a custom foreground media notification.

**Architecture:** Add a platform-neutral playback engine contract and coordinator in `commonMain`; persist playback/favorite snapshots through Room3; keep Android playback in `androidMain` behind `Media3AudioPlayerEngine` and `MusicPlaybackService`. UI, notification actions, and MediaSession commands all return to `MusicAppController` / `PlaybackCoordinator` so queue, mode, favorites, and failure rules have one truth.

**Tech Stack:** Kotlin Multiplatform 2.0.21, Compose Multiplatform 1.7.3, kotlinx.coroutines 1.9.0, AndroidX Room3 3.0.0-rc01, AndroidX SQLite bundled driver, KSP, AndroidX Media3 ExoPlayer/Session/UI, kotlin.test.

---

## Scope Check

This plan implements one vertical Android P0 slice from the approved spec:

- common playback models, coordinator, fake engine, and tests;
- Room3 persistence for playback snapshot, queue, and favorites;
- UI/controller migration to list-backed queues, seek, mode, and error state;
- Android Media3 service/engine, foreground notification, manifest wiring, and Android verification.

It intentionally does not implement iOS playback, Desktop playback, Quick Settings customization, or lock-screen customization. Room3 common declarations are shaped for iOS and Compose Desktop JVM later, but this plan only verifies Android builder and Android playback.

## Runtime Wiring Invariants

These invariants are P0. Treat any later implementation detail that conflicts with them as a plan bug to fix before coding:

- Android must inject the real playback engine into `MusicAppController` through a stable `AudioPlayerEngine` adapter. The controller must not keep using `FakeAudioPlayerEngine` on Android.
- `MusicAppController` must start `PlaybackCoordinator.start(scope, onStateChanged)` during construction or Android attachment. Engine events are the truth for `Playing`, `Paused`, `Buffering`, progress, current song, errors, and ended transitions.
- UI, custom notification actions, and MediaSession/system commands must all call the same controller/coordinator commands. They must not directly mutate ExoPlayer, Room, favorites, or UI state.
- The custom notification must be refreshed from controller state after current song, playback status, favorite state, or playback mode changes. Building a `Notification` object is not enough.
- `MainActivity` must not call `startForegroundService()` during app startup. The playback service may be started/bound while the Activity is foregrounded, but foreground promotion must happen only when a playable song notification is available and must call `startForeground()` immediately.
- Room playback snapshots must be written on queue/current-song changes, engine status changes, ended auto-skip, failures, seek, pause, stop/service teardown, playback mode changes, and throttled progress updates. Progress-only writes are throttled to 5 seconds.
- Favorites tests must exercise the persistent repository against a fake or real DAO, including a new repository instance loading the saved state.
- Error mapping must distinguish permission errors from missing files. Successful playback of a song resets consecutive-failure counters.

## File Structure

- Modify `gradle/libs.versions.toml`: add Room3, SQLite, KSP, Media3, AndroidX core/appcompat notification dependencies.
- Modify `composeApp/build.gradle.kts`: apply KSP and Room3 plugins; add common Room3/SQLite dependencies, Android Media3 dependencies, KSP processors, and schema directory.
- Create `composeApp/schemas/`: Room3 schema output directory committed with generated schema files after the first successful compile.
- Modify `composeApp/src/androidMain/AndroidManifest.xml`: add foreground service/media playback permissions and register `MusicPlaybackService`.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt` updates: replace the boolean-only model with status, mode, error, media, queue, and snapshot models.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt`: common engine interface and events.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeAudioPlayerEngine.kt`: deterministic fake engine for common tests and non-Android previews.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/PlaybackRepository.kt`: runtime state repository API.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryPlaybackRepository.kt`: runtime state, queue, history, and last error storage.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackDatabase.kt`: Room3 database, constructor, entities, and DAOs.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStore.kt`: platform-neutral persistence facade over Room3 DAOs.
- Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data/AndroidPlaybackDatabase.kt`: Android Room database builder.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt`: queue generation, playback modes, seek, engine-event subscription, failure limits, throttled persistence writes, and state-change callbacks.
- Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt`: queue/mode/failure/seek tests.
- Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStoreTest.kt`: store contract tests using in-memory fake store first; migrate to Room-backed test when dependencies compile.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/FavoritesRepository.kt`: add set/save API needed by Room-backed favorites.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/PersistentFavoritesRepository.kt`: Room-backed favorites repository.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/PlaybackUseCases.kt`: replace old playback use cases or make them delegate to coordinator.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`: add status, position, duration, mode, error, and derived `isPlaying`.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`: inject coordinator/store, restore snapshot, pass list queues, expose seek/mode, subscribe engine events.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`: pass queue lists from screens, wire player progress/mode/error, keep mini-player seek read-only.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/PlayerScreen.kt`: show real progress, seek, mode button, buffering/loading/error.
- Modify screen callback signatures in `HomeScreen.kt`, `LocalMusicScreen.kt`, `SearchScreen.kt`, `FavoritesScreen.kt`, and `DetailScreens.kt`: pass `queueSongs`.
- Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/Media3AudioPlayerEngine.kt`: Android engine implementation.
- Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt`: MediaSessionService host.
- Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt`: Android `AudioPlayerEngine` adapter injected into the controller; it starts/binds the service lazily and forwards commands/events.
- Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackNotificationController.kt`: custom notification builder plus service refresh entrypoint.
- Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackRuntime.kt`: observes controller playback state, refreshes foreground notification, and exposes notification/system commands back to the controller.
- Create Android notification layout XML files under `composeApp/src/androidMain/res/layout/`.
- Create notification drawable vector resources under `composeApp/src/androidMain/res/drawable/`.
- Modify `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/MusicAppViewModel.kt`: inject scanner, database-backed repositories, playback connector, controller scope, notification runtime, and command bridge.
- Modify `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/MainActivity.kt`: attach Android context to the playback connector/runtime and request notification permission when needed; do not start foreground service at launch.

## Task 1: Add Playback Domain Models and Engine Contract

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeAudioPlayerEngine.kt`
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackModelsTest.kt`

- [x] **Step 1: Write the failing model test**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackModelsTest.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackModelsTest {
    @Test
    fun playingStateDerivesIsPlaying(): Unit {
        val playing = PlaybackState(currentSongId = "song-1", status = PlaybackStatus.Playing)
        val paused = PlaybackState(currentSongId = "song-1", status = PlaybackStatus.Paused)

        assertTrue(playing.isPlaying)
        assertFalse(paused.isPlaying)
    }

    @Test
    fun queueStateExposesCurrentSongId(): Unit {
        val queue = QueueState(
            songIds = listOf("song-1", "song-2", "song-3"),
            currentIndex = 1,
            playbackMode = PlaybackMode.LoopAll,
        )

        assertEquals(expected = "song-2", actual = queue.currentSongId)
    }

    @Test
    fun playableMediaRequiresScannerUri(): Unit {
        val media = PlayableMedia(
            songId = "androidMediaStore:42",
            title = "设备里的歌",
            artist = "未知歌手",
            album = "未知专辑",
            durationMs = 180_000L,
            localUri = "content://media/external/audio/media/42",
            coverArt = CoverArt.HeroLocalMusic,
            mimeType = "audio/mpeg",
        )

        assertEquals(expected = "content://media/external/audio/media/42", actual = media.localUri)
    }
}
```

- [x] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackModelsTest"
```

Expected: FAIL with unresolved references for `PlayableMedia`, `PlaybackMode`, `PlaybackStatus`, and the new `QueueState` properties.

- [x] **Step 3: Replace the playback models with the new state contract**

Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt` to this shape:

```kotlin
package com.yanhao.kmpmusic.domain.model

enum class SearchScope {
    All,
    Songs,
    Albums,
    Artists,
}

enum class ThemeMode {
    Light,
    Dark,
    System,
}

enum class PlaybackStatus {
    Idle,
    Loading,
    Playing,
    Paused,
    Buffering,
    Ended,
    Error,
}

enum class PlaybackMode {
    LoopAll,
    LoopOne,
    Shuffle,
}

enum class PlaybackErrorType {
    MissingFile,
    UnsupportedFormat,
    PermissionDenied,
    EngineUnavailable,
    Unknown,
}

data class PlaybackError(
    val type: PlaybackErrorType,
    val songId: String?,
    val message: String,
)

data class PlayableMedia(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long?,
    val localUri: String,
    val coverArt: CoverArt,
    val mimeType: String?,
)

data class PlaybackState(
    val currentSongId: String? = null,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
    val error: PlaybackError? = null,
) {
    val isPlaying: Boolean
        get() = status == PlaybackStatus.Playing
}

data class QueueState(
    val songIds: List<String> = emptyList(),
    val currentIndex: Int = -1,
    val playbackMode: PlaybackMode = PlaybackMode.LoopAll,
    val shuffleHistory: List<Int> = emptyList(),
    val shuffleRemaining: List<Int> = emptyList(),
) {
    val currentSongId: String?
        get() = songIds.getOrNull(index = currentIndex)
}

data class PlaybackHistory(
    val songIds: List<String> = emptyList(),
)

data class PlaybackSnapshot(
    val playbackState: PlaybackState = PlaybackState(),
    val queueState: QueueState = QueueState(),
    val updatedAt: Long = 0L,
)
```

- [x] **Step 4: Add the engine interface and events**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import kotlinx.coroutines.flow.Flow

interface AudioPlayerEngine {
    val events: Flow<PlaybackEngineEvent>

    suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long = 0L,
    )

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun skipToIndex(index: Int)
    fun stop()
}

sealed interface PlaybackEngineEvent {
    data class StatusChanged(
        val status: PlaybackStatus,
        val positionMs: Long,
        val durationMs: Long?,
    ) : PlaybackEngineEvent

    data class CurrentMediaChanged(
        val songId: String,
        val index: Int,
        val durationMs: Long?,
    ) : PlaybackEngineEvent

    data class ProgressChanged(
        val positionMs: Long,
        val durationMs: Long?,
    ) : PlaybackEngineEvent

    data object Ended : PlaybackEngineEvent

    data class Failed(
        val error: PlaybackError,
    ) : PlaybackEngineEvent
}
```

- [x] **Step 5: Add a deterministic fake engine for tests**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeAudioPlayerEngine.kt`:

```kotlin
package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeAudioPlayerEngine : AudioPlayerEngine {
    private val mutableEvents = MutableSharedFlow<PlaybackEngineEvent>(extraBufferCapacity = 64)
    private var queue: List<PlayableMedia> = emptyList()
    private var currentIndex: Int = -1

    override val events: SharedFlow<PlaybackEngineEvent> = mutableEvents.asSharedFlow()

    override suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        queue = items
        currentIndex = startIndex.coerceIn(minimumValue = 0, maximumValue = items.lastIndex)
        val media = queue.getOrNull(index = currentIndex)
        if (media == null) {
            mutableEvents.tryEmit(
                PlaybackEngineEvent.Failed(
                    error = PlaybackError(
                        type = PlaybackErrorType.MissingFile,
                        songId = null,
                        message = "播放队列为空",
                    ),
                ),
            )
            return
        }
        mutableEvents.tryEmit(
            PlaybackEngineEvent.CurrentMediaChanged(
                songId = media.songId,
                index = currentIndex,
                durationMs = media.durationMs,
            ),
        )
        mutableEvents.tryEmit(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Loading,
                positionMs = startPositionMs,
                durationMs = media.durationMs,
            ),
        )
    }

    override fun play() {
        val media = queue.getOrNull(index = currentIndex) ?: return
        mutableEvents.tryEmit(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Playing,
                positionMs = 0L,
                durationMs = media.durationMs,
            ),
        )
    }

    override fun pause() {
        val media = queue.getOrNull(index = currentIndex) ?: return
        mutableEvents.tryEmit(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Paused,
                positionMs = 0L,
                durationMs = media.durationMs,
            ),
        )
    }

    override fun seekTo(positionMs: Long) {
        val media = queue.getOrNull(index = currentIndex) ?: return
        mutableEvents.tryEmit(
            PlaybackEngineEvent.ProgressChanged(
                positionMs = positionMs,
                durationMs = media.durationMs,
            ),
        )
    }

    override fun skipToIndex(index: Int) {
        currentIndex = index.coerceIn(minimumValue = 0, maximumValue = queue.lastIndex)
        val media = queue.getOrNull(currentIndex) ?: return
        mutableEvents.tryEmit(
            PlaybackEngineEvent.CurrentMediaChanged(
                songId = media.songId,
                index = currentIndex,
                durationMs = media.durationMs,
            ),
        )
    }

    override fun stop() {
        mutableEvents.tryEmit(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Idle,
                positionMs = 0L,
                durationMs = null,
            ),
        )
    }

    fun emitEnded() {
        mutableEvents.tryEmit(PlaybackEngineEvent.Ended)
    }

    fun emitFailure(songId: String, message: String = "播放失败") {
        mutableEvents.tryEmit(
            PlaybackEngineEvent.Failed(
                error = PlaybackError(
                    type = PlaybackErrorType.Unknown,
                    songId = songId,
                    message = message,
                ),
            ),
        )
    }
}
```

- [x] **Step 6: Run the model test**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackModelsTest"
```

Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeAudioPlayerEngine.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackModelsTest.kt
git commit -m "新增播放状态模型和引擎接口"
```

## Task 2: Implement PlaybackCoordinator Queue, Modes, and Failure Rules

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/PlaybackRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryPlaybackRepository.kt`
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt`

- [x] **Step 1: Write coordinator tests for queue generation and modes**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.data.FakeAudioPlayerEngine
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackCoordinatorTest {
    @Test
    fun playSongUsesWholeCurrentListAsQueue(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val engine = FakeAudioPlayerEngine()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = engine,
        )
        val songs = buildSongs(count = 5)

        coordinator.playSong(song = songs[2], queueSongs = songs)

        val queue = repository.getQueueState()
        assertEquals(expected = songs.map { song -> song.id }, actual = queue.songIds)
        assertEquals(expected = 2, actual = queue.currentIndex)
        assertEquals(expected = songs[2].id, actual = repository.getPlaybackState().currentSongId)
    }

    @Test
    fun cyclePlaybackModeLoopsThroughThreeModes(): Unit {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
        )

        assertEquals(PlaybackMode.LoopAll, repository.getQueueState().playbackMode)
        coordinator.cyclePlaybackMode()
        assertEquals(PlaybackMode.LoopOne, repository.getQueueState().playbackMode)
        coordinator.cyclePlaybackMode()
        assertEquals(PlaybackMode.Shuffle, repository.getQueueState().playbackMode)
        coordinator.cyclePlaybackMode()
        assertEquals(PlaybackMode.LoopAll, repository.getQueueState().playbackMode)
    }

    @Test
    fun loopAllMovesFromLastSongToFirstSong(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
        )
        val songs = buildSongs(count = 3)

        coordinator.playSong(song = songs[2], queueSongs = songs)
        coordinator.handleEngineEventForTest(PlaybackEngineEvent.Ended)

        assertEquals(expected = 0, actual = repository.getQueueState().currentIndex)
        assertEquals(expected = songs[0].id, actual = repository.getPlaybackState().currentSongId)
    }

    @Test
    fun loopOneKeepsCurrentSongOnEnded(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
        )
        val songs = buildSongs(count = 3)

        coordinator.playSong(song = songs[1], queueSongs = songs)
        coordinator.cyclePlaybackMode()
        coordinator.handleEngineEventForTest(PlaybackEngineEvent.Ended)

        assertEquals(expected = 1, actual = repository.getQueueState().currentIndex)
        assertEquals(expected = songs[1].id, actual = repository.getPlaybackState().currentSongId)
    }

    @Test
    fun shufflePreviousUsesHistory(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
            randomIndex = { candidates -> candidates.first() },
        )
        val songs = buildSongs(count = 4)

        coordinator.playSong(song = songs[0], queueSongs = songs)
        coordinator.cyclePlaybackMode()
        coordinator.cyclePlaybackMode()
        coordinator.moveNext()
        val shuffledIndex = repository.getQueueState().currentIndex
        coordinator.movePrevious()

        assertEquals(expected = 1, actual = shuffledIndex)
        assertEquals(expected = 0, actual = repository.getQueueState().currentIndex)
    }

    @Test
    fun startCollectsEngineEventsIntoRepository(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val engine = FakeAudioPlayerEngine()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = engine,
        )
        val songs = buildSongs(count = 2)
        var updateCount = 0

        coordinator.start(scope = backgroundScope) {
            updateCount += 1
        }
        coordinator.playSong(song = songs[0], queueSongs = songs)
        engine.seekTo(positionMs = 24_000L)
        advanceUntilIdle()

        assertEquals(expected = PlaybackStatus.Playing, actual = repository.getPlaybackState().status)
        assertEquals(expected = 24_000L, actual = repository.getPlaybackState().positionMs)
        assertEquals(expected = 2, actual = repository.getQueueState().songIds.size)
        assertEquals(expected = true, actual = updateCount > 0)
    }

    @Test
    fun loopOneStopsAfterThreeFailuresForSameSong(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
        )
        val songs = buildSongs(count = 1)

        coordinator.playSong(song = songs[0], queueSongs = songs)
        coordinator.cyclePlaybackMode()
        repeat(times = 3) {
            coordinator.handleEngineEventForTest(
                PlaybackEngineEvent.Failed(
                    error = com.yanhao.kmpmusic.domain.model.PlaybackError(
                        type = com.yanhao.kmpmusic.domain.model.PlaybackErrorType.Unknown,
                        songId = songs[0].id,
                        message = "坏文件",
                    ),
                ),
            )
        }

        assertEquals(expected = PlaybackStatus.Error, actual = repository.getPlaybackState().status)
    }

    @Test
    fun successfulPlaybackResetsFailureCounters(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
        )
        val songs = buildSongs(count = 4)

        coordinator.playSong(song = songs[0], queueSongs = songs)
        repeat(times = 2) {
            coordinator.handleEngineEventForTest(
                PlaybackEngineEvent.Failed(
                    error = com.yanhao.kmpmusic.domain.model.PlaybackError(
                        type = com.yanhao.kmpmusic.domain.model.PlaybackErrorType.Unknown,
                        songId = songs[it].id,
                        message = "坏文件",
                    ),
                ),
            )
        }
        coordinator.handleEngineEventForTest(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Playing,
                positionMs = 0L,
                durationMs = 180_000L,
            ),
        )
        coordinator.handleEngineEventForTest(
            PlaybackEngineEvent.Failed(
                error = com.yanhao.kmpmusic.domain.model.PlaybackError(
                    type = com.yanhao.kmpmusic.domain.model.PlaybackErrorType.Unknown,
                    songId = songs[2].id,
                    message = "坏文件",
                ),
            ),
        )

        assertEquals(expected = PlaybackStatus.Loading, actual = repository.getPlaybackState().status)
        assertEquals(expected = songs[3].id, actual = repository.getPlaybackState().currentSongId)
    }

    private fun buildSongs(count: Int): List<Song> {
        return (0 until count).map { index ->
            Song(
                id = "song-$index",
                title = "Song $index",
                artist = "Artist",
                album = "Album",
                duration = "3:00",
                coverArt = CoverArt.HeroLocalMusic,
                isLiked = false,
                lastPlayed = "未播放",
                quality = "本地 MP3",
                lyric = "Local",
                trackNumber = index + 1,
                durationMs = 180_000L,
                sourceId = "$index",
                sourceKind = LocalMusicSourceKind.AndroidMediaStore,
                localUri = "content://media/external/audio/media/$index",
                mimeType = "audio/mpeg",
            )
        }
    }
}
```

- [x] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest"
```

Expected: FAIL with unresolved reference `PlaybackCoordinator` and missing repository methods if Task 1 is complete.

- [x] **Step 3: Update the runtime repository contract**

Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/PlaybackRepository.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.QueueState

interface PlaybackRepository {
    fun getPlaybackState(): PlaybackState
    fun savePlaybackState(state: PlaybackState)
    fun getQueueState(): QueueState
    fun saveQueueState(state: QueueState)
    fun getPlaybackHistory(): PlaybackHistory
    fun savePlaybackHistory(history: PlaybackHistory)
}
```

- [x] **Step 4: Update the in-memory repository**

Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryPlaybackRepository.kt`:

```kotlin
package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository

class InMemoryPlaybackRepository : PlaybackRepository {
    private var playbackState: PlaybackState = PlaybackState()
    private var queueState: QueueState = QueueState()
    private var playbackHistory: PlaybackHistory = PlaybackHistory()

    override fun getPlaybackState(): PlaybackState = playbackState

    override fun savePlaybackState(state: PlaybackState) {
        playbackState = state
    }

    override fun getQueueState(): QueueState = queueState

    override fun saveQueueState(state: QueueState) {
        queueState = state
    }

    override fun getPlaybackHistory(): PlaybackHistory = playbackHistory

    override fun savePlaybackHistory(history: PlaybackHistory) {
        playbackHistory = history
    }
}
```

- [x] **Step 5: Implement the coordinator**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlaybackCoordinator(
    private val playbackRepository: PlaybackRepository,
    private val audioPlayerEngine: AudioPlayerEngine,
    private val playbackSnapshotStore: PlaybackSnapshotStore = InMemoryPlaybackSnapshotStore(),
    private val nowMillis: () -> Long = { 0L },
    private val snapshotThrottleMs: Long = 5_000L,
    private val randomIndex: (List<Int>) -> Int = { candidates -> candidates.random() },
) {
    private var eventJob: Job? = null
    private var loopOneFailureCount: Int = 0
    private var consecutiveFailedSongCount: Int = 0
    private var lastFailedSongId: String? = null
    private var lastProgressSnapshotAt: Long = Long.MIN_VALUE
    private var onStateChanged: () -> Unit = {}
    private var coordinatorScope: CoroutineScope? = null

    fun start(scope: CoroutineScope, onStateChanged: () -> Unit = {}) {
        eventJob?.cancel()
        this.onStateChanged = onStateChanged
        coordinatorScope = scope
        eventJob = scope.launch {
            audioPlayerEngine.events.collect { event ->
                handleEngineEvent(event)
                saveSnapshotForEvent(event = event)
                this@PlaybackCoordinator.onStateChanged()
            }
        }
    }

    suspend fun playSong(song: Song, queueSongs: List<Song>) {
        val safeQueue = queueSongs.ifEmpty { listOf(song) }
        val startIndex = safeQueue.indexOfFirst { candidate -> candidate.id == song.id }
            .takeIf { index -> index >= 0 } ?: 0
        val queueState = QueueState(
            songIds = safeQueue.map { queueSong -> queueSong.id },
            currentIndex = startIndex,
            playbackMode = playbackRepository.getQueueState().playbackMode,
        )
        playbackRepository.saveQueueState(state = queueState)
        playbackRepository.savePlaybackState(
            state = PlaybackState(
                currentSongId = song.id,
                status = PlaybackStatus.Loading,
                positionMs = 0L,
                durationMs = song.durationMs,
            ),
        )
        recordHistory(songId = song.id)
        saveSnapshotNow()
        onStateChanged()
        audioPlayerEngine.setQueue(
            items = safeQueue.map { queueSong -> queueSong.toPlayableMedia() },
            startIndex = startIndex,
            startPositionMs = 0L,
        )
        audioPlayerEngine.play()
    }

    fun togglePlayback() {
        if (playbackRepository.getPlaybackState().isPlaying) {
            audioPlayerEngine.pause()
        } else {
            audioPlayerEngine.play()
        }
    }

    fun moveNext() {
        val queue = playbackRepository.getQueueState()
        moveToIndex(index = nextIndex(queue = queue))
    }

    fun movePrevious() {
        val queue = playbackRepository.getQueueState()
        if (queue.songIds.isEmpty()) {
            return
        }
        val previousIndex = if (queue.playbackMode == PlaybackMode.Shuffle && queue.shuffleHistory.isNotEmpty()) {
            queue.shuffleHistory.last()
        } else {
            (queue.currentIndex - 1 + queue.songIds.size) % queue.songIds.size
        }
        moveToIndex(index = previousIndex)
    }

    fun seekTo(positionMs: Long) {
        audioPlayerEngine.seekTo(positionMs = positionMs.coerceAtLeast(minimumValue = 0L))
    }

    fun cyclePlaybackMode() {
        val queue = playbackRepository.getQueueState()
        val nextMode = when (queue.playbackMode) {
            PlaybackMode.LoopAll -> PlaybackMode.LoopOne
            PlaybackMode.LoopOne -> PlaybackMode.Shuffle
            PlaybackMode.Shuffle -> PlaybackMode.LoopAll
        }
        playbackRepository.saveQueueState(
            state = queue.copy(
                playbackMode = nextMode,
                shuffleHistory = emptyList(),
                shuffleRemaining = if (nextMode == PlaybackMode.Shuffle) {
                    queue.songIds.indices.filterNot { index -> index == queue.currentIndex }
                } else {
                    emptyList()
                },
            ),
        )
        saveSnapshotNow()
        onStateChanged()
    }

    internal fun handleEngineEventForTest(event: PlaybackEngineEvent) {
        handleEngineEvent(event = event)
    }

    private fun handleEngineEvent(event: PlaybackEngineEvent) {
        when (event) {
            is PlaybackEngineEvent.CurrentMediaChanged -> handleCurrentMediaChanged(event)
            is PlaybackEngineEvent.ProgressChanged -> updateProgress(event.positionMs, event.durationMs)
            is PlaybackEngineEvent.StatusChanged -> updateStatus(event)
            PlaybackEngineEvent.Ended -> handleEnded()
            is PlaybackEngineEvent.Failed -> handleFailure(error = event.error)
        }
    }

    private fun handleCurrentMediaChanged(event: PlaybackEngineEvent.CurrentMediaChanged) {
        val queue = playbackRepository.getQueueState()
        playbackRepository.saveQueueState(state = queue.copy(currentIndex = event.index))
        playbackRepository.savePlaybackState(
            state = playbackRepository.getPlaybackState().copy(
                currentSongId = event.songId,
                durationMs = event.durationMs,
                error = null,
            ),
        )
        resetFailureCounters()
    }

    private fun updateProgress(positionMs: Long, durationMs: Long?) {
        playbackRepository.savePlaybackState(
            state = playbackRepository.getPlaybackState().copy(
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    private fun updateStatus(event: PlaybackEngineEvent.StatusChanged) {
        if (event.status == PlaybackStatus.Playing) {
            resetFailureCounters()
        }
        playbackRepository.savePlaybackState(
            state = playbackRepository.getPlaybackState().copy(
                status = event.status,
                positionMs = event.positionMs,
                durationMs = event.durationMs,
                error = null,
            ),
        )
    }

    private fun handleEnded() {
        val queue = playbackRepository.getQueueState()
        val targetIndex = when (queue.playbackMode) {
            PlaybackMode.LoopOne -> queue.currentIndex
            PlaybackMode.LoopAll,
            PlaybackMode.Shuffle,
            -> nextIndex(queue = queue)
        }
        moveToIndex(index = targetIndex)
    }

    private fun handleFailure(error: PlaybackError) {
        playbackRepository.savePlaybackState(
            state = playbackRepository.getPlaybackState().copy(
                status = PlaybackStatus.Error,
                error = error,
            ),
        )
        val queue = playbackRepository.getQueueState()
        if (queue.playbackMode == PlaybackMode.LoopOne) {
            loopOneFailureCount = if (lastFailedSongId == error.songId) loopOneFailureCount + 1 else 1
            lastFailedSongId = error.songId
            if (loopOneFailureCount >= 3) {
                return
            }
            moveToIndex(index = queue.currentIndex)
            return
        }
        consecutiveFailedSongCount += 1
        if (consecutiveFailedSongCount >= 3) {
            return
        }
        moveToIndex(index = nextIndex(queue = queue))
    }

    private fun nextIndex(queue: QueueState): Int {
        if (queue.songIds.isEmpty()) {
            return -1
        }
        if (queue.playbackMode != PlaybackMode.Shuffle) {
            return (queue.currentIndex + 1) % queue.songIds.size
        }
        val remaining = queue.shuffleRemaining.ifEmpty {
            queue.songIds.indices.filterNot { index -> index == queue.currentIndex }
        }
        return randomIndex(remaining)
    }

    private fun moveToIndex(index: Int) {
        val queue = playbackRepository.getQueueState()
        if (index !in queue.songIds.indices) {
            return
        }
        val nextQueue = if (queue.playbackMode == PlaybackMode.Shuffle) {
            queue.copy(
                currentIndex = index,
                shuffleHistory = queue.shuffleHistory + queue.currentIndex,
                shuffleRemaining = queue.shuffleRemaining.filterNot { remainingIndex -> remainingIndex == index },
            )
        } else {
            queue.copy(currentIndex = index)
        }
        playbackRepository.saveQueueState(state = nextQueue)
        val songId = nextQueue.songIds[index]
        playbackRepository.savePlaybackState(
            state = playbackRepository.getPlaybackState().copy(
                currentSongId = songId,
                status = PlaybackStatus.Loading,
                positionMs = 0L,
                error = null,
            ),
        )
        recordHistory(songId = songId)
        audioPlayerEngine.skipToIndex(index = index)
        audioPlayerEngine.play()
    }

    private fun saveSnapshotForEvent(event: PlaybackEngineEvent) {
        if (event is PlaybackEngineEvent.ProgressChanged) {
            val now = nowMillis()
            if (now - lastProgressSnapshotAt < snapshotThrottleMs) {
                return
            }
            lastProgressSnapshotAt = now
        }
        saveSnapshotNow()
    }

    private fun saveSnapshotNow() {
        val scope = coordinatorScope ?: return
        scope.launch {
            playbackSnapshotStore.saveSnapshot(
                snapshot = PlaybackSnapshot(
                    playbackState = playbackRepository.getPlaybackState(),
                    queueState = playbackRepository.getQueueState(),
                    updatedAt = nowMillis(),
                ),
            )
        }
    }

    private fun resetFailureCounters() {
        loopOneFailureCount = 0
        consecutiveFailedSongCount = 0
        lastFailedSongId = null
    }

    private fun recordHistory(songId: String) {
        val current = playbackRepository.getPlaybackHistory().songIds
        playbackRepository.savePlaybackHistory(
            history = PlaybackHistory(songIds = (listOf(songId) + current.filterNot { id -> id == songId }).take(50)),
        )
    }

    private fun Song.toPlayableMedia(): PlayableMedia {
        return PlayableMedia(
            songId = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            localUri = localUri,
            coverArt = coverArt,
            mimeType = mimeType,
        )
    }
}
```

- [x] **Step 6: Run coordinator tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest"
```

Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/PlaybackRepository.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryPlaybackRepository.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt
git commit -m "实现播放协调器队列规则"
```

## Task 3: Add Room3 Dependencies and Playback Persistence Schema

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackDatabase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStore.kt`
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data/AndroidPlaybackDatabase.kt`
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStoreTest.kt`

Snapshot write policy required by this task and Task 2:

| Trigger | Persist immediately? | Notes |
| --- | --- | --- |
| New queue/current song from `playSong` | Yes | Store queue order, index, mode, position `0`, status restored later as paused. |
| Engine `CurrentMediaChanged` | Yes | Captures auto-transition and system/controller skip results. |
| Engine `StatusChanged` to `Playing`, `Paused`, `Buffering`, `Ended`, `Error`, `Idle` | Yes | Also preserves pause/stop/service teardown. |
| Engine `ProgressChanged` only | Throttled | Save at most once every 5 seconds. Seek uses immediate command path below. |
| User seek | Yes | Save the target position immediately, then engine progress can correct it. |
| Auto-skip after ended/failure | Yes | Save the next queue index/song after coordinator chooses it. |
| Playback mode change | Yes | Notification and cold restore must keep mode. |
| Favorite change | Handled by `PersistentFavoritesRepository` | Not part of playback snapshot. |

- [x] **Step 1: Write the persistence contract test against an in-memory fake store**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStoreTest.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.persistence

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackSnapshotStoreTest {
    @Test
    fun saveAndRestoreSnapshotKeepsQueueOrderAndPausedStatus(): Unit = runTest {
        val store = InMemoryPlaybackSnapshotStore()
        val snapshot = PlaybackSnapshot(
            playbackState = PlaybackState(
                currentSongId = "song-2",
                status = PlaybackStatus.Playing,
                positionMs = 42_000L,
                durationMs = 180_000L,
            ),
            queueState = QueueState(
                songIds = listOf("song-1", "song-2", "song-3"),
                currentIndex = 1,
                playbackMode = PlaybackMode.Shuffle,
                shuffleHistory = listOf(0),
                shuffleRemaining = listOf(2),
            ),
            updatedAt = 1_000L,
        )

        store.saveSnapshot(snapshot = snapshot)
        val restored = store.restoreSnapshot(availableSongIds = setOf("song-1", "song-2", "song-3"))

        assertEquals(expected = listOf("song-1", "song-2", "song-3"), actual = restored.queueState.songIds)
        assertEquals(expected = 1, actual = restored.queueState.currentIndex)
        assertEquals(expected = PlaybackMode.Shuffle, actual = restored.queueState.playbackMode)
        assertEquals(expected = PlaybackStatus.Paused, actual = restored.playbackState.status)
        assertEquals(expected = 42_000L, actual = restored.playbackState.positionMs)
    }

    @Test
    fun restoreFiltersMissingSongs(): Unit = runTest {
        val store = InMemoryPlaybackSnapshotStore()
        store.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(currentSongId = "song-2", status = PlaybackStatus.Paused),
                queueState = QueueState(songIds = listOf("song-1", "song-2"), currentIndex = 1),
            ),
        )

        val restored = store.restoreSnapshot(availableSongIds = setOf("song-1"))

        assertEquals(expected = listOf("song-1"), actual = restored.queueState.songIds)
        assertEquals(expected = 0, actual = restored.queueState.currentIndex)
        assertEquals(expected = "song-1", actual = restored.playbackState.currentSongId)
    }
}
```

- [x] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStoreTest"
```

Expected: FAIL with unresolved references for `InMemoryPlaybackSnapshotStore` and `PlaybackSnapshotStore`.

- [x] **Step 3: Add Room3 and Media build catalog entries**

Modify `gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.13.2"
composeMultiplatform = "1.7.3"
kotlin = "2.0.21"
kotlinxCoroutines = "1.9.0"
androidxCore = "1.17.0"
ksp = "2.0.21-1.0.28"
media3 = "1.8.0"
room3 = "3.0.0-rc01"
sqlite = "2.6.2"

[libraries]
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
androidx-core = { module = "androidx.core:core", version.ref = "androidxCore" }
androidx-sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
androidx-room3-runtime = { module = "androidx.room3:room3-runtime", version.ref = "room3" }
androidx-room3-compiler = { module = "androidx.room3:room3-compiler", version.ref = "room3" }
androidx-media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "media3" }
androidx-media3-session = { module = "androidx.media3:media3-session", version.ref = "media3" }
androidx-media3-ui = { module = "androidx.media3:media3-ui", version.ref = "media3" }

[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidxRoom3 = { id = "androidx.room3", version.ref = "room3" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [x] **Step 4: Wire Gradle plugins and dependencies**

Modify `composeApp/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidxRoom3)
}
```

Add dependencies:

```kotlin
commonMain.dependencies {
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
}
commonTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinx.coroutines.test)
}
androidMain.dependencies {
    implementation(compose.preview)
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation(libs.androidx.core)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
}
```

Add KSP dependencies after the `kotlin { ... }` block:

```kotlin
dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspIosX64", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
    add("kspDesktop", libs.androidx.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
```

- [x] **Step 5: Add Room database entities and DAOs**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackDatabase.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.persistence

import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Entity(tableName = "playback_snapshot")
data class PlaybackSnapshotEntity(
    @PrimaryKey val id: Int = 1,
    val currentSongId: String?,
    val currentIndex: Int,
    val playbackMode: String,
    val positionMs: Long,
    val durationMs: Long?,
    val updatedAt: Long,
)

@Entity(tableName = "playback_queue_item", primaryKeys = ["position"])
data class PlaybackQueueItemEntity(
    val position: Int,
    val songId: String,
)

@Entity(tableName = "favorite_song")
data class FavoriteSongEntity(
    @PrimaryKey val songId: String,
    val updatedAt: Long,
)

@Dao
interface PlaybackSnapshotDao {
    @Query("SELECT * FROM playback_snapshot WHERE id = 1")
    suspend fun getSnapshot(): PlaybackSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSnapshot(entity: PlaybackSnapshotEntity)

    @Query("DELETE FROM playback_snapshot")
    suspend fun clearSnapshot()
}

@Dao
interface PlaybackQueueDao {
    @Query("SELECT * FROM playback_queue_item ORDER BY position ASC")
    suspend fun getQueueItems(): List<PlaybackQueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlaybackQueueItemEntity>)

    @Query("DELETE FROM playback_queue_item")
    suspend fun clearQueue()
}

@Dao
interface FavoriteSongDao {
    @Query("SELECT songId FROM favorite_song")
    suspend fun getFavoriteSongIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFavorite(entity: FavoriteSongEntity)

    @Query("DELETE FROM favorite_song WHERE songId = :songId")
    suspend fun deleteFavorite(songId: String)
}

@Database(
    entities = [
        PlaybackSnapshotEntity::class,
        PlaybackQueueItemEntity::class,
        FavoriteSongEntity::class,
    ],
    version = 1,
)
@androidx.room3.ConstructedBy(PlaybackDatabaseConstructor::class)
abstract class PlaybackDatabase : RoomDatabase() {
    abstract fun playbackSnapshotDao(): PlaybackSnapshotDao
    abstract fun playbackQueueDao(): PlaybackQueueDao
    abstract fun favoriteSongDao(): FavoriteSongDao
}

@Suppress("KotlinNoActualForExpect")
expect object PlaybackDatabaseConstructor : RoomDatabaseConstructor<PlaybackDatabase> {
    override fun initialize(): PlaybackDatabase
}
```

- [x] **Step 6: Add snapshot store interfaces and in-memory implementation**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStore.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.persistence

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState

interface PlaybackSnapshotStore {
    suspend fun saveSnapshot(snapshot: PlaybackSnapshot)
    suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot
}

class InMemoryPlaybackSnapshotStore : PlaybackSnapshotStore {
    private var snapshot: PlaybackSnapshot = PlaybackSnapshot()

    override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
        this.snapshot = snapshot
    }

    override suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot {
        return snapshot.filterForRestore(availableSongIds = availableSongIds)
    }
}

class RoomPlaybackSnapshotStore(
    private val database: PlaybackDatabase,
    private val nowMillis: () -> Long = { 0L },
) : PlaybackSnapshotStore {
    override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
        database.playbackQueueDao().clearQueue()
        database.playbackQueueDao().insertAll(
            items = snapshot.queueState.songIds.mapIndexed { index, songId ->
                PlaybackQueueItemEntity(position = index, songId = songId)
            },
        )
        database.playbackSnapshotDao().saveSnapshot(
            entity = PlaybackSnapshotEntity(
                currentSongId = snapshot.playbackState.currentSongId,
                currentIndex = snapshot.queueState.currentIndex,
                playbackMode = snapshot.queueState.playbackMode.name,
                positionMs = snapshot.playbackState.positionMs,
                durationMs = snapshot.playbackState.durationMs,
                updatedAt = snapshot.updatedAt.takeIf { value -> value > 0L } ?: nowMillis(),
            ),
        )
    }

    override suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot {
        val snapshotEntity = database.playbackSnapshotDao().getSnapshot() ?: return PlaybackSnapshot()
        val queueIds = database.playbackQueueDao().getQueueItems().map { item -> item.songId }
        val mode = PlaybackMode.entries.firstOrNull { mode -> mode.name == snapshotEntity.playbackMode }
            ?: PlaybackMode.LoopAll
        return PlaybackSnapshot(
            playbackState = PlaybackState(
                currentSongId = snapshotEntity.currentSongId,
                status = PlaybackStatus.Paused,
                positionMs = snapshotEntity.positionMs,
                durationMs = snapshotEntity.durationMs,
            ),
            queueState = QueueState(
                songIds = queueIds,
                currentIndex = snapshotEntity.currentIndex,
                playbackMode = mode,
            ),
            updatedAt = snapshotEntity.updatedAt,
        ).filterForRestore(availableSongIds = availableSongIds)
    }
}

private fun PlaybackSnapshot.filterForRestore(availableSongIds: Set<String>): PlaybackSnapshot {
    val filteredIds = queueState.songIds.filter { songId -> availableSongIds.contains(songId) }
    if (filteredIds.isEmpty()) {
        return PlaybackSnapshot()
    }
    val requestedSongId = playbackState.currentSongId
    val restoredIndex = filteredIds.indexOf(requestedSongId).takeIf { index -> index >= 0 }
        ?: queueState.currentIndex.coerceIn(0, filteredIds.lastIndex)
    val restoredSongId = filteredIds[restoredIndex]
    return copy(
        playbackState = playbackState.copy(
            currentSongId = restoredSongId,
            status = PlaybackStatus.Paused,
            positionMs = if (restoredSongId == requestedSongId) playbackState.positionMs else 0L,
            error = null,
        ),
        queueState = queueState.copy(
            songIds = filteredIds,
            currentIndex = restoredIndex,
            shuffleHistory = emptyList(),
            shuffleRemaining = emptyList(),
        ),
    )
}
```

- [x] **Step 7: Add Android Room database builder**

Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data/AndroidPlaybackDatabase.kt`:

```kotlin
package com.yanhao.kmpmusic.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import kotlinx.coroutines.Dispatchers

fun createAndroidPlaybackDatabase(context: Context): PlaybackDatabase {
    return createPlaybackDatabase(
        builder = Room.databaseBuilder<PlaybackDatabase>(
            context = context.applicationContext,
            name = context.applicationContext.getDatabasePath("kmp_music_playback.db").absolutePath,
        ),
    )
}

fun createPlaybackDatabase(
    builder: RoomDatabase.Builder<PlaybackDatabase>,
): PlaybackDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
```

- [x] **Step 8: Run persistence tests and Android compile**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStoreTest"
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: both PASS.

- [x] **Step 9: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackDatabase.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStore.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data/AndroidPlaybackDatabase.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStoreTest.kt composeApp/schemas
git commit -m "接入 Room3 播放快照持久化"
```

## Task 4: Persist Favorites Through Room3

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/FavoritesRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/PersistentFavoritesRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryFavoritesRepository.kt`
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/data/PersistentFavoritesRepositoryTest.kt`

- [x] **Step 1: Write the favorites contract test**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/data/PersistentFavoritesRepositoryTest.kt`:

```kotlin
package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.persistence.FavoriteSongDao
import com.yanhao.kmpmusic.domain.persistence.FavoriteSongEntity
import com.yanhao.kmpmusic.domain.repository.FavoritesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentFavoritesRepositoryTest {
    @Test
    fun toggleSongPersistsFavoriteSetAcrossRepositoryInstances(): Unit = runTest {
        val dao = FakeFavoriteSongDao()
        val repository: FavoritesRepository = PersistentFavoritesRepository(
            favoriteSongDao = dao,
            initialLikedSongIds = PersistentFavoritesRepository.loadInitialLikedSongIds(favoriteSongDao = dao),
            nowMillis = { 123L },
        )

        assertTrue(repository.toggleSong(songId = "song-1").contains("song-1"))

        val restoredRepository: FavoritesRepository = PersistentFavoritesRepository(
            favoriteSongDao = dao,
            initialLikedSongIds = PersistentFavoritesRepository.loadInitialLikedSongIds(favoriteSongDao = dao),
        )

        assertTrue(restoredRepository.getLikedSongIds().contains("song-1"))
        assertFalse(restoredRepository.toggleSong(songId = "song-1").contains("song-1"))
        assertFalse(dao.getFavoriteSongIds().contains("song-1"))
    }

    private class FakeFavoriteSongDao : FavoriteSongDao {
        private val rows = linkedMapOf<String, FavoriteSongEntity>()

        override suspend fun getFavoriteSongIds(): List<String> {
            return rows.keys.toList()
        }

        override suspend fun saveFavorite(entity: FavoriteSongEntity) {
            rows[entity.songId] = entity
        }

        override suspend fun deleteFavorite(songId: String) {
            rows.remove(songId)
        }
    }
}
```

- [x] **Step 2: Run the test**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.data.PersistentFavoritesRepositoryTest"
```

Expected: FAIL until `PersistentFavoritesRepository` exists and writes through `FavoriteSongDao`. This test must not use `InMemoryFavoritesRepository`; it proves the persistent implementation can restore favorites into a fresh repository instance.

- [x] **Step 3: Extend the repository API without breaking callers**

Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/FavoritesRepository.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.repository

interface FavoritesRepository {
    fun getLikedSongIds(): Set<String>
    fun toggleSong(songId: String): Set<String>
    fun replaceLikedSongIds(songIds: Set<String>)
}
```

- [x] **Step 4: Update the in-memory implementation**

Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryFavoritesRepository.kt`:

```kotlin
package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.repository.FavoritesRepository

class InMemoryFavoritesRepository(
    initialLikedSongIds: Set<String>,
) : FavoritesRepository {
    private var likedSongIds: Set<String> = initialLikedSongIds

    override fun getLikedSongIds(): Set<String> = likedSongIds

    override fun toggleSong(songId: String): Set<String> {
        likedSongIds = if (likedSongIds.contains(songId)) {
            likedSongIds - songId
        } else {
            likedSongIds + songId
        }
        return likedSongIds
    }

    override fun replaceLikedSongIds(songIds: Set<String>) {
        likedSongIds = songIds
    }
}
```

- [x] **Step 5: Add the Room-backed repository**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/PersistentFavoritesRepository.kt`:

```kotlin
package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.persistence.FavoriteSongDao
import com.yanhao.kmpmusic.domain.persistence.FavoriteSongEntity
import com.yanhao.kmpmusic.domain.repository.FavoritesRepository
import kotlinx.coroutines.runBlocking

class PersistentFavoritesRepository(
    private val favoriteSongDao: FavoriteSongDao,
    initialLikedSongIds: Set<String>,
    private val nowMillis: () -> Long = { 0L },
) : FavoritesRepository {
    private var likedSongIds: Set<String> = initialLikedSongIds

    override fun getLikedSongIds(): Set<String> = likedSongIds

    override fun toggleSong(songId: String): Set<String> {
        likedSongIds = if (likedSongIds.contains(songId)) {
            runBlocking { favoriteSongDao.deleteFavorite(songId = songId) }
            likedSongIds - songId
        } else {
            runBlocking {
                favoriteSongDao.saveFavorite(
                    entity = FavoriteSongEntity(songId = songId, updatedAt = nowMillis()),
                )
            }
            likedSongIds + songId
        }
        return likedSongIds
    }

    override fun replaceLikedSongIds(songIds: Set<String>) {
        likedSongIds = songIds
    }

    companion object {
        suspend fun loadInitialLikedSongIds(favoriteSongDao: FavoriteSongDao): Set<String> {
            return favoriteSongDao.getFavoriteSongIds().toSet()
        }
    }
}
```

- [x] **Step 6: Run tests and compile**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.data.PersistentFavoritesRepositoryTest"
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/FavoritesRepository.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryFavoritesRepository.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/PersistentFavoritesRepository.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/data/PersistentFavoritesRepositoryTest.kt
git commit -m "持久化收藏歌曲状态"
```

## Task 5: Migrate Controller to PlaybackCoordinator and Snapshot Restore

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/PlaybackUseCases.kt`
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [x] **Step 1: Add controller tests for list-backed queues and restored pause**

Append to `MusicAppControllerTest`:

```kotlin
@Test
fun playSongUsesProvidedQueueSongs(): Unit = runTest {
    val controller = MusicAppController(controllerScope = backgroundScope)
    controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
    val queueSongs = controller.uiState.songs
    val targetSong = queueSongs[3]

    controller.playSong(song = targetSong, queueSongs = queueSongs)

    assertEquals(expected = queueSongs.map { song -> song.id }, actual = controller.uiState.queueSongIds)
    assertEquals(expected = targetSong.id, actual = controller.uiState.currentSongId)
}

@Test
fun cyclePlaybackModeUpdatesUiState(): Unit = runTest {
    val controller = MusicAppController(controllerScope = backgroundScope)

    assertEquals(expected = PlaybackMode.LoopAll, actual = controller.uiState.playbackMode)
    controller.cyclePlaybackMode()
    assertEquals(expected = PlaybackMode.LoopOne, actual = controller.uiState.playbackMode)
    controller.cyclePlaybackMode()
    assertEquals(expected = PlaybackMode.Shuffle, actual = controller.uiState.playbackMode)
}
```

Add imports:

```kotlin
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import kotlinx.coroutines.test.runTest
```

- [x] **Step 2: Run the controller tests to verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest"
```

Expected: FAIL because `playSong(song, queueSongs)`, `cyclePlaybackMode`, and `uiState.playbackMode` do not exist yet.

- [x] **Step 3: Add playback fields to UI state**

Modify `MusicAppUiState` in `MusicAppModels.kt` to include:

```kotlin
val playbackStatus: PlaybackStatus,
val playbackPositionMs: Long = 0L,
val playbackDurationMs: Long? = null,
val playbackMode: PlaybackMode = PlaybackMode.LoopAll,
val playbackError: PlaybackError? = null,
```

Replace the stored `isPlaying: Boolean` constructor property with:

```kotlin
val isPlaying: Boolean
    get() = playbackStatus == PlaybackStatus.Playing
```

Keep existing callers compiling by ensuring every `MusicAppUiState(...)` creation passes `playbackStatus = playbackState.status`.

- [x] **Step 4: Inject and use PlaybackCoordinator**

Modify `MusicAppController` constructor:

```kotlin
class MusicAppController(
    private val musicLibraryRepository: MusicLibraryRepository = InMemoryMusicLibraryRepository(),
    private val localMusicScanner: LocalMusicScanner = FakeLocalMusicScanner(),
    private val playbackRepository: PlaybackRepository = InMemoryPlaybackRepository(),
    private val audioPlayerEngine: AudioPlayerEngine = FakeAudioPlayerEngine(),
    private val playbackSnapshotStore: PlaybackSnapshotStore = InMemoryPlaybackSnapshotStore(),
    private val injectedFavoritesRepository: FavoritesRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository = InMemoryUserPreferencesRepository(),
    private val permissionSettingsOpener: PermissionSettingsOpener = PermissionSettingsOpener {},
    private val controllerScope: kotlinx.coroutines.CoroutineScope,
    private val nowMillis: () -> Long = { 0L },
)
```

Add imports:

```kotlin
import com.yanhao.kmpmusic.data.FakeAudioPlayerEngine
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackCoordinator
```

Add property:

```kotlin
private val playbackCoordinator: PlaybackCoordinator = PlaybackCoordinator(
    playbackRepository = playbackRepository,
    audioPlayerEngine = audioPlayerEngine,
    playbackSnapshotStore = playbackSnapshotStore,
    nowMillis = nowMillis,
)
```

In `init`, start engine event collection and make every engine event refresh UI:

```kotlin
init {
    playbackCoordinator.start(scope = controllerScope) {
        syncPlaybackState(playbackState = playbackRepository.getPlaybackState())
    }
    favoritesRepository = injectedFavoritesRepository ?: InMemoryFavoritesRepository(
        initialLikedSongIds = uiState.likedSongIds,
    )
    toggleFavoriteUseCase = ToggleFavoriteUseCaseImpl(
        favoritesRepository = favoritesRepository,
    )
}
```

Add an observer hook for Android notification refresh:

```kotlin
private var playbackUiObserver: (MusicAppUiState) -> Unit = {}

fun attachPlaybackUiObserver(observer: (MusicAppUiState) -> Unit) {
    playbackUiObserver = observer
    playbackUiObserver(uiState)
}

private fun publishPlaybackUiState() {
    playbackUiObserver(uiState)
}
```

- [x] **Step 5: Replace playback methods**

Replace old methods in `MusicAppController`:

```kotlin
suspend fun restorePlaybackSnapshot() {
    val availableIds = uiState.songs.map { song -> song.id }.toSet()
    val snapshot = playbackSnapshotStore.restoreSnapshot(availableSongIds = availableIds)
    playbackRepository.savePlaybackState(state = snapshot.playbackState)
    playbackRepository.saveQueueState(state = snapshot.queueState)
    syncPlaybackState(playbackState = snapshot.playbackState)
    publishPlaybackUiState()
}

fun playSong(song: Song, queueSongs: List<Song> = listOf(song)) {
    controllerScope.launch {
        playbackCoordinator.playSong(song = song, queueSongs = queueSongs)
        syncPlaybackState(playbackState = playbackRepository.getPlaybackState())
        publishPlaybackUiState()
    }
}

fun openSong(song: Song, queueSongs: List<Song> = listOf(song)) {
    playSong(song = song, queueSongs = queueSongs)
    navigateToSecondary(screen = SecondaryScreen.Player)
}

fun togglePlayback() {
    playbackCoordinator.togglePlayback()
}

fun moveTrack(direction: Int) {
    if (direction < 0) {
        playbackCoordinator.movePrevious()
    } else {
        playbackCoordinator.moveNext()
    }
}

fun seekTo(positionMs: Long) {
    playbackCoordinator.seekTo(positionMs = positionMs)
    controllerScope.launch {
        playbackSnapshotStore.saveSnapshot(
            snapshot = com.yanhao.kmpmusic.domain.model.PlaybackSnapshot(
                playbackState = playbackRepository.getPlaybackState().copy(positionMs = positionMs.coerceAtLeast(0L)),
                queueState = playbackRepository.getQueueState(),
                updatedAt = nowMillis(),
            ),
        )
    }
}

fun cyclePlaybackMode() {
    playbackCoordinator.cyclePlaybackMode()
    syncPlaybackState(playbackState = playbackRepository.getPlaybackState())
    publishPlaybackUiState()
}
```

- [x] **Step 6: Update syncPlaybackState**

Replace `syncPlaybackState` body:

```kotlin
private fun syncPlaybackState(playbackState: PlaybackState) {
    val queueState = playbackRepository.getQueueState()
    uiState = uiState.copy(
        currentSongId = playbackState.currentSongId,
        playbackStatus = playbackState.status,
        playbackPositionMs = playbackState.positionMs,
        playbackDurationMs = playbackState.durationMs,
        playbackMode = queueState.playbackMode,
        playbackError = playbackState.error,
        queueSongIds = queueState.songIds,
        recentSongs = buildRecentSongs(songs = uiState.songs),
    )
    publishPlaybackUiState()
}
```

At the end of `toggleFavorite(songId)`, after assigning `uiState = uiState.copy(...)`, call:

```kotlin
publishPlaybackUiState()
```

This is required so the foreground notification favorite button refreshes when favorites change inside the App.

- [x] **Step 7: Run controller tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest"
```

Expected: PASS after updating imports and constructor arguments.

- [x] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/PlaybackUseCases.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "控制器接入播放协调器"
```

## Task 6: Update UI Callbacks, Player Progress, Seek, Mode, and Error State

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/PlayerScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/SearchScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/FavoritesScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/DetailScreens.kt`

- [x] **Step 1: Update screen callback types**

In each screen file, change:

```kotlin
onSongOpen: (Song) -> Unit,
onSongPlay: (Song) -> Unit,
```

to:

```kotlin
onSongOpen: (Song, List<Song>) -> Unit,
onSongPlay: (Song, List<Song>) -> Unit,
```

Update each `SongRow` call from:

```kotlin
onOpen = onSongOpen,
onPlay = onSongPlay,
```

to:

```kotlin
onOpen = { song -> onSongOpen(song, songs) },
onPlay = { song -> onSongPlay(song, songs) },
```

For filtered lists, pass the rendered list variable, for example in search:

```kotlin
val resultSongs = result.songs
SongRow(
    song = song,
    isPlaying = song.id == currentSongId,
    onOpen = { selectedSong -> onSongOpen(selectedSong, resultSongs) },
    onPlay = { selectedSong -> onSongPlay(selectedSong, resultSongs) },
    onMore = onMore,
    dense = true,
)
```

- [x] **Step 2: Update App wiring**

In `MusicApp.kt`, replace direct method references:

```kotlin
onSongOpen = controller::openSong,
onSongPlay = controller::playSong,
```

with:

```kotlin
onSongOpen = { song, queueSongs -> controller.openSong(song = song, queueSongs = queueSongs) },
onSongPlay = { song, queueSongs -> controller.playSong(song = song, queueSongs = queueSongs) },
```

- [x] **Step 3: Update PlayerScreen signature**

Modify `PlayerScreen` signature:

```kotlin
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    playbackMode: PlaybackMode,
    playbackError: PlaybackError?,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onMode: () -> Unit,
    onLike: (String) -> Unit,
    onQueue: () -> Unit,
)
```

Add imports:

```kotlin
import androidx.compose.material3.Slider
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackMode
```

- [x] **Step 4: Replace static progress with Slider**

Replace the current static `LinearProgressIndicator` block with:

```kotlin
val duration = playbackDurationMs ?: song.durationMs ?: 0L
val safeProgress = if (duration > 0L) {
    playbackPositionMs.coerceIn(0L, duration).toFloat()
} else {
    0f
}
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Slider(
        value = safeProgress,
        onValueChange = { value -> onSeek(value.toLong()) },
        valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = formatPlaybackTime(playbackPositionMs), color = MusicColors.Muted, fontSize = 12.sp)
        Text(text = formatPlaybackTime(duration), color = MusicColors.Muted, fontSize = 12.sp)
    }
}
```

Add helper at bottom of `PlayerScreen.kt`:

```kotlin
private fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = (positionMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(length = 2, padChar = '0')}"
}
```

- [x] **Step 5: Wire playback mode and error UI**

Replace the repeat button:

```kotlin
IconButton(onClick = {}) { Icon(Icons.Rounded.Repeat, contentDescription = "循环播放") }
```

with:

```kotlin
IconButton(onClick = onMode) {
    Icon(
        imageVector = when (playbackMode) {
            PlaybackMode.LoopAll -> Icons.Rounded.Repeat
            PlaybackMode.LoopOne -> Icons.Rounded.RepeatOne
            PlaybackMode.Shuffle -> Icons.Rounded.Shuffle
        },
        contentDescription = when (playbackMode) {
            PlaybackMode.LoopAll -> "列表循环"
            PlaybackMode.LoopOne -> "单曲循环"
            PlaybackMode.Shuffle -> "随机播放"
        },
    )
}
```

Add error text below the progress block:

```kotlin
if (playbackError != null) {
    Text(
        text = playbackError.message,
        color = MaterialTheme.colorScheme.error,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
    )
}
```

- [x] **Step 6: Pass new player props in MusicApp.kt**

In the `SecondaryScreen.Player` branch:

```kotlin
PlayerScreen(
    song = song,
    isPlaying = state.isPlaying,
    playbackPositionMs = state.playbackPositionMs,
    playbackDurationMs = state.playbackDurationMs,
    playbackMode = state.playbackMode,
    playbackError = state.playbackError,
    onBack = controller::navigateBack,
    onToggle = controller::togglePlayback,
    onPrev = { controller.moveTrack(direction = -1) },
    onNext = { controller.moveTrack(direction = 1) },
    onSeek = controller::seekTo,
    onMode = controller::cyclePlaybackMode,
    onLike = controller::toggleFavorite,
    onQueue = controller::openQueue,
)
```

- [x] **Step 7: Run common compile and tests**

Run:

```bash
./gradlew :composeApp:desktopTest
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [x] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/PlayerScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/SearchScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/FavoritesScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/DetailScreens.kt
git commit -m "接入播放页进度和列表队列"
```

## Task 7: Add Android Media3 Engine and Service

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/Media3AudioPlayerEngine.kt`
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt`
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt`
- Modify: `composeApp/src/androidMain/AndroidManifest.xml`

- [x] **Step 1: Add manifest permissions and service**

Modify `composeApp/src/androidMain/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

    <application
        android:allowBackup="true"
        android:label="KMP Music"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".playback.MusicPlaybackService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

- [x] **Step 2: Implement Media3 engine**

Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/Media3AudioPlayerEngine.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class Media3AudioPlayerEngine(
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
) : AudioPlayerEngine {
    private val mutableEvents = MutableSharedFlow<PlaybackEngineEvent>(extraBufferCapacity = 128)
    private var progressJob: Job? = null
    private var queue: List<PlayableMedia> = emptyList()

    override val events: SharedFlow<PlaybackEngineEvent> = mutableEvents.asSharedFlow()

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    emitStatus()
                    if (playbackState == Player.STATE_ENDED) {
                        mutableEvents.tryEmit(PlaybackEngineEvent.Ended)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    emitStatus()
                    if (isPlaying) startProgressUpdates() else stopProgressUpdates()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val index = player.currentMediaItemIndex
                    val media = queue.getOrNull(index)
                    if (media != null) {
                        mutableEvents.tryEmit(
                            PlaybackEngineEvent.CurrentMediaChanged(
                                songId = media.songId,
                                index = index,
                                durationMs = media.durationMs,
                            ),
                        )
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    val media = queue.getOrNull(player.currentMediaItemIndex)
                    mutableEvents.tryEmit(
                        PlaybackEngineEvent.Failed(
                            error = PlaybackError(
                                type = error.toPlaybackErrorType(),
                                songId = media?.songId,
                                message = error.message ?: "播放失败",
                            ),
                        ),
                    )
                }
            },
        )
    }

    override suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        queue = items
        player.setMediaItems(
            items.map { item ->
                MediaItem.Builder()
                    .setUri(Uri.parse(item.localUri))
                    .setMediaId(item.songId)
                    .build()
            },
            startIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0)),
            startPositionMs.coerceAtLeast(0L),
        )
        player.prepare()
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    override fun skipToIndex(index: Int) {
        player.seekToDefaultPosition(index)
    }

    override fun stop() {
        stopProgressUpdates()
        player.stop()
    }

    private fun emitStatus() {
        mutableEvents.tryEmit(
            PlaybackEngineEvent.StatusChanged(
                status = player.toPlaybackStatus(),
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeIf { duration -> duration > 0L },
            ),
        )
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mutableEvents.emit(
                    PlaybackEngineEvent.ProgressChanged(
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.takeIf { duration -> duration > 0L },
                    ),
                )
                delay(500L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun Player.toPlaybackStatus(): PlaybackStatus {
        if (isPlaying) return PlaybackStatus.Playing
        return when (playbackState) {
            Player.STATE_IDLE -> PlaybackStatus.Idle
            Player.STATE_BUFFERING -> PlaybackStatus.Buffering
            Player.STATE_READY -> PlaybackStatus.Paused
            Player.STATE_ENDED -> PlaybackStatus.Ended
            else -> PlaybackStatus.Idle
        }
    }

    private fun PlaybackException.toPlaybackErrorType(): PlaybackErrorType {
        return when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> PlaybackErrorType.PermissionDenied
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> PlaybackErrorType.MissingFile
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            -> PlaybackErrorType.UnsupportedFormat
            else -> PlaybackErrorType.Unknown
        }
    }
}
```

- [x] **Step 3: Add MediaSessionService**

Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MusicPlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob())
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var engine: Media3AudioPlayerEngine? = null

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        engine = Media3AudioPlayerEngine(player = exoPlayer, scope = serviceScope)
        mediaSession = MediaSession.Builder(this, CoordinatorForwardingPlayer(player = exoPlayer)).build()
        PlaybackServiceRegistry.attach(service = this, engine = requireNotNull(engine))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        PlaybackServiceRegistry.detach()
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }
}

object PlaybackServiceRegistry {
    private var service: MusicPlaybackService? = null
    private var engine: Media3AudioPlayerEngine? = null

    fun attach(service: MusicPlaybackService, engine: Media3AudioPlayerEngine) {
        this.service = service
        this.engine = engine
    }

    fun detach() {
        service = null
        engine = null
    }

    fun currentService(): MusicPlaybackService? = service

    fun currentEngine(): Media3AudioPlayerEngine? = engine
}

interface PlaybackCommandBridge {
    fun previous()
    fun togglePlayback()
    fun next()
    fun seekTo(positionMs: Long)
}

object PlaybackCommandBridgeRegistry {
    private var bridge: PlaybackCommandBridge? = null

    fun attach(bridge: PlaybackCommandBridge) {
        this.bridge = bridge
    }

    fun detach() {
        bridge = null
    }

    fun current(): PlaybackCommandBridge? = bridge
}

private class CoordinatorForwardingPlayer(
    player: Player,
) : ForwardingPlayer(player) {
    override fun play() {
        PlaybackCommandBridgeRegistry.current()?.togglePlayback() ?: super.play()
    }

    override fun pause() {
        PlaybackCommandBridgeRegistry.current()?.togglePlayback() ?: super.pause()
    }

    override fun seekToNextMediaItem() {
        PlaybackCommandBridgeRegistry.current()?.next() ?: super.seekToNextMediaItem()
    }

    override fun seekToPreviousMediaItem() {
        PlaybackCommandBridgeRegistry.current()?.previous() ?: super.seekToPreviousMediaItem()
    }

    override fun seekTo(positionMs: Long) {
        PlaybackCommandBridgeRegistry.current()?.seekTo(positionMs = positionMs) ?: super.seekTo(positionMs)
    }
}
```

- [x] **Step 4: Add connector**

Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import android.content.Context
import android.content.Intent
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class PlaybackServiceConnector(
    private val scope: CoroutineScope,
) : AudioPlayerEngine {
    private val mutableEvents = MutableSharedFlow<PlaybackEngineEvent>(extraBufferCapacity = 128)
    private var appContext: Context? = null
    private var eventBridgeJob: Job? = null
    private var bridgedEngine: AudioPlayerEngine? = null

    override val events: SharedFlow<PlaybackEngineEvent> = mutableEvents.asSharedFlow()

    fun attachContext(context: Context) {
        appContext = context.applicationContext
    }

    override suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        val engine = requireEngineOrEmitError() ?: return
        bridgeEvents(engine = engine)
        engine.setQueue(items = items, startIndex = startIndex, startPositionMs = startPositionMs)
    }

    override fun play() {
        requireEngineOrEmitError()?.play()
    }

    override fun pause() {
        requireEngineOrEmitError()?.pause()
    }

    override fun seekTo(positionMs: Long) {
        requireEngineOrEmitError()?.seekTo(positionMs = positionMs)
    }

    override fun skipToIndex(index: Int) {
        requireEngineOrEmitError()?.skipToIndex(index = index)
    }

    override fun stop() {
        requireEngineOrEmitError()?.stop()
    }

    private fun ensureServiceStarted() {
        val context = appContext ?: return
        context.startService(Intent(context, MusicPlaybackService::class.java))
    }

    private fun requireEngineOrEmitError(): AudioPlayerEngine? {
        ensureServiceStarted()
        val engine = PlaybackServiceRegistry.currentEngine()
        if (engine == null) {
            mutableEvents.tryEmit(
                PlaybackEngineEvent.Failed(
                    error = PlaybackError(
                        type = PlaybackErrorType.EngineUnavailable,
                        songId = null,
                        message = "Android 播放服务尚未就绪",
                    ),
                ),
            )
        }
        return engine
    }

    private fun bridgeEvents(engine: AudioPlayerEngine) {
        if (bridgedEngine === engine && eventBridgeJob?.isActive == true) {
            return
        }
        eventBridgeJob?.cancel()
        bridgedEngine = engine
        eventBridgeJob = scope.launch {
            engine.events.collect { event ->
                mutableEvents.emit(event)
            }
        }
    }
}
```

- [x] **Step 5: Compile Android**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add composeApp/src/androidMain/AndroidManifest.xml composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/Media3AudioPlayerEngine.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt
git commit -m "接入 Android Media3 播放服务"
```

## Task 8: Build Custom Android Media Notification Actions

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackNotificationController.kt`
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackNotificationActionReceiver.kt`
- Create: `composeApp/src/androidMain/res/layout/notification_playback_collapsed.xml`
- Create: `composeApp/src/androidMain/res/layout/notification_playback_expanded.xml`
- Create: `composeApp/src/androidMain/res/drawable/ic_notification_favorite.xml`
- Create: `composeApp/src/androidMain/res/drawable/ic_notification_favorite_border.xml`
- Create: `composeApp/src/androidMain/res/drawable/ic_notification_loop_all.xml`
- Create: `composeApp/src/androidMain/res/drawable/ic_notification_loop_one.xml`
- Create: `composeApp/src/androidMain/res/drawable/ic_notification_shuffle.xml`
- Modify: `composeApp/src/androidMain/AndroidManifest.xml`

- [x] **Step 1: Add action constants and receiver**

Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackNotificationActionReceiver.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

const val ACTION_TOGGLE_FAVORITE = "com.yanhao.kmpmusic.playback.TOGGLE_FAVORITE"
const val ACTION_PREVIOUS = "com.yanhao.kmpmusic.playback.PREVIOUS"
const val ACTION_TOGGLE_PLAYBACK = "com.yanhao.kmpmusic.playback.TOGGLE_PLAYBACK"
const val ACTION_NEXT = "com.yanhao.kmpmusic.playback.NEXT"
const val ACTION_CYCLE_MODE = "com.yanhao.kmpmusic.playback.CYCLE_MODE"

class PlaybackNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dispatcher = PlaybackNotificationDispatcher.current()
        when (intent.action) {
            ACTION_TOGGLE_FAVORITE -> dispatcher?.toggleFavorite()
            ACTION_PREVIOUS -> dispatcher?.previous()
            ACTION_TOGGLE_PLAYBACK -> dispatcher?.togglePlayback()
            ACTION_NEXT -> dispatcher?.next()
            ACTION_CYCLE_MODE -> dispatcher?.cycleMode()
        }
    }
}

interface PlaybackNotificationActions : PlaybackCommandBridge {
    fun toggleFavorite()
    fun cycleMode()
}

object PlaybackNotificationDispatcher {
    private var actions: PlaybackNotificationActions? = null

    fun attach(actions: PlaybackNotificationActions) {
        this.actions = actions
    }

    fun detach() {
        actions = null
    }

    fun current(): PlaybackNotificationActions? = actions
}
```

`PlaybackNotificationActions` extends `PlaybackCommandBridge`, so notification and MediaSession/system controls share the same controller-backed command implementation. Do not add direct ExoPlayer calls to the receiver or notification controller.

- [x] **Step 2: Register receiver**

Add to `AndroidManifest.xml` inside `<application>`:

```xml
<receiver
    android:name=".playback.PlaybackNotificationActionReceiver"
    android:exported="false" />
```

- [x] **Step 3: Add collapsed notification layout**

Create `composeApp/src/androidMain/res/layout/notification_playback_collapsed.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="64dp"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingStart="12dp"
    android:paddingEnd="12dp">

    <ImageView
        android:id="@+id/notification_cover"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:contentDescription="@null"
        android:scaleType="centerCrop" />

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="12dp"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/notification_title"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:ellipsize="end"
            android:maxLines="1"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/notification_artist"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:ellipsize="end"
            android:maxLines="1" />
    </LinearLayout>

    <ImageButton
        android:id="@+id/action_previous"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:background="@android:color/transparent"
        android:contentDescription="上一首" />

    <ImageButton
        android:id="@+id/action_play_pause"
        android:layout_width="44dp"
        android:layout_height="44dp"
        android:background="@android:color/transparent"
        android:contentDescription="播放或暂停" />

    <ImageButton
        android:id="@+id/action_next"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:background="@android:color/transparent"
        android:contentDescription="下一首" />
</LinearLayout>
```

- [x] **Step 4: Add expanded notification layout**

Create `composeApp/src/androidMain/res/layout/notification_playback_expanded.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="112dp"
    android:orientation="vertical"
    android:padding="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <ImageView
            android:id="@+id/notification_cover"
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:contentDescription="@null"
            android:scaleType="centerCrop" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/notification_title"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:ellipsize="end"
                android:maxLines="1"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/notification_artist"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:ellipsize="end"
                android:maxLines="1" />
        </LinearLayout>
    </LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="44dp"
        android:gravity="center"
        android:orientation="horizontal">

        <ImageButton android:id="@+id/action_favorite" android:layout_width="44dp" android:layout_height="44dp" android:background="@android:color/transparent" android:contentDescription="收藏或取消收藏" />
        <ImageButton android:id="@+id/action_previous" android:layout_width="44dp" android:layout_height="44dp" android:background="@android:color/transparent" android:contentDescription="上一首" />
        <ImageButton android:id="@+id/action_play_pause" android:layout_width="48dp" android:layout_height="48dp" android:background="@android:color/transparent" android:contentDescription="播放或暂停" />
        <ImageButton android:id="@+id/action_next" android:layout_width="44dp" android:layout_height="44dp" android:background="@android:color/transparent" android:contentDescription="下一首" />
        <ImageButton android:id="@+id/action_mode" android:layout_width="44dp" android:layout_height="44dp" android:background="@android:color/transparent" android:contentDescription="播放模式" />
    </LinearLayout>
</LinearLayout>
```

- [x] **Step 5: Add vector icons**

Create `composeApp/src/androidMain/res/drawable/ic_notification_favorite.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF222222"
        android:pathData="M12,21.35L10.55,20.03C5.4,15.36 2,12.28 2,8.5C2,5.42 4.42,3 7.5,3C9.24,3 10.91,3.81 12,5.08C13.09,3.81 14.76,3 16.5,3C19.58,3 22,5.42 22,8.5C22,12.28 18.6,15.36 13.45,20.04L12,21.35Z" />
</vector>
```

Create `composeApp/src/androidMain/res/drawable/ic_notification_favorite_border.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF222222"
        android:pathData="M16.5,3C14.76,3 13.09,3.81 12,5.08C10.91,3.81 9.24,3 7.5,3C4.42,3 2,5.42 2,8.5C2,12.28 5.4,15.36 10.55,20.04L12,21.35L13.45,20.03C18.6,15.36 22,12.28 22,8.5C22,5.42 19.58,3 16.5,3ZM12.1,18.55L12,18.65L11.9,18.55C7.14,14.24 4,11.39 4,8.5C4,6.5 5.5,5 7.5,5C9.04,5 10.54,5.99 11.07,7.36H12.94C13.46,5.99 14.96,5 16.5,5C18.5,5 20,6.5 20,8.5C20,11.39 16.86,14.24 12.1,18.55Z" />
</vector>
```

Create `composeApp/src/androidMain/res/drawable/ic_notification_loop_all.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF222222"
        android:pathData="M7,7H17L15,5L16.4,3.6L21,8L16.4,12.4L15,11L17,9H7C5.34,9 4,10.34 4,12C4,13.66 5.34,15 7,15H8V17H7C4.24,17 2,14.76 2,12C2,9.24 4.24,7 7,7ZM17,17H7L9,19L7.6,20.4L3,16L7.6,11.6L9,13L7,15H17C18.66,15 20,13.66 20,12C20,10.34 18.66,9 17,9H16V7H17C19.76,7 22,9.24 22,12C22,14.76 19.76,17 17,17Z" />
</vector>
```

Create `composeApp/src/androidMain/res/drawable/ic_notification_loop_one.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF222222"
        android:pathData="M7,7H17L15,5L16.4,3.6L21,8L16.4,12.4L15,11L17,9H7C5.34,9 4,10.34 4,12C4,13.66 5.34,15 7,15H8V17H7C4.24,17 2,14.76 2,12C2,9.24 4.24,7 7,7ZM17,17H7L9,19L7.6,20.4L3,16L7.6,11.6L9,13L7,15H17C18.66,15 20,13.66 20,12C20,10.34 18.66,9 17,9H16V7H17C19.76,7 22,9.24 22,12C22,14.76 19.76,17 17,17ZM12,14V10H10.5V8.5H13.5V14H12Z" />
</vector>
```

Create `composeApp/src/androidMain/res/drawable/ic_notification_shuffle.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF222222"
        android:pathData="M10.59,9.17L5.41,4L4,5.41L9.17,10.59L10.59,9.17ZM14.5,4L16.54,6.04L4,18.59L5.41,20L17.96,7.46L20,9.5V4H14.5ZM14.83,13.41L13.41,14.83L16.55,17.96L14.5,20H20V14.5L17.96,16.54L14.83,13.41Z" />
</vector>
```

- [x] **Step 6: Implement notification controller**

Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackNotificationController.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.yanhao.kmpmusic.R
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song

private const val CHANNEL_ID = "kmp_music_playback"
const val PLAYBACK_NOTIFICATION_ID = 42

class AndroidPlaybackNotificationController(
    private val context: Context,
) {
    fun createNotification(
        song: Song,
        isPlaying: Boolean,
        isFavorite: Boolean,
        playbackMode: PlaybackMode,
    ): Notification {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_loop_all)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setOngoing(isPlaying)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed(song, isPlaying))
            .setCustomBigContentView(expanded(song, isPlaying, isFavorite, playbackMode))
            .build()
    }

    private fun collapsed(song: Song, isPlaying: Boolean): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_playback_collapsed).apply {
            setTextViewText(R.id.notification_title, song.title)
            setTextViewText(R.id.notification_artist, song.artist)
            setImageViewResource(R.id.action_previous, android.R.drawable.ic_media_previous)
            setImageViewResource(R.id.action_play_pause, if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            setImageViewResource(R.id.action_next, android.R.drawable.ic_media_next)
            setOnClickPendingIntent(R.id.action_previous, pending(ACTION_PREVIOUS, 1))
            setOnClickPendingIntent(R.id.action_play_pause, pending(ACTION_TOGGLE_PLAYBACK, 2))
            setOnClickPendingIntent(R.id.action_next, pending(ACTION_NEXT, 3))
        }
    }

    private fun expanded(
        song: Song,
        isPlaying: Boolean,
        isFavorite: Boolean,
        playbackMode: PlaybackMode,
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_playback_expanded).apply {
            setTextViewText(R.id.notification_title, song.title)
            setTextViewText(R.id.notification_artist, song.artist)
            setImageViewResource(R.id.action_favorite, if (isFavorite) R.drawable.ic_notification_favorite else R.drawable.ic_notification_favorite_border)
            setImageViewResource(R.id.action_previous, android.R.drawable.ic_media_previous)
            setImageViewResource(R.id.action_play_pause, if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            setImageViewResource(R.id.action_next, android.R.drawable.ic_media_next)
            setImageViewResource(
                R.id.action_mode,
                when (playbackMode) {
                    PlaybackMode.LoopAll -> R.drawable.ic_notification_loop_all
                    PlaybackMode.LoopOne -> R.drawable.ic_notification_loop_one
                    PlaybackMode.Shuffle -> R.drawable.ic_notification_shuffle
                },
            )
            setOnClickPendingIntent(R.id.action_favorite, pending(ACTION_TOGGLE_FAVORITE, 4))
            setOnClickPendingIntent(R.id.action_previous, pending(ACTION_PREVIOUS, 5))
            setOnClickPendingIntent(R.id.action_play_pause, pending(ACTION_TOGGLE_PLAYBACK, 6))
            setOnClickPendingIntent(R.id.action_next, pending(ACTION_NEXT, 7))
            setOnClickPendingIntent(R.id.action_mode, pending(ACTION_CYCLE_MODE, 8))
        }
    }

    private fun pending(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, PlaybackNotificationActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "音乐播放",
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }
}
```

- [x] **Step 7: Compile Android**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [x] **Step 8: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackNotificationController.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackNotificationActionReceiver.kt composeApp/src/androidMain/res/layout/notification_playback_collapsed.xml composeApp/src/androidMain/res/layout/notification_playback_expanded.xml composeApp/src/androidMain/res/drawable composeApp/src/androidMain/AndroidManifest.xml
git commit -m "新增 Android 自定义播放通知"
```

## Task 9: Wire Android ViewModel, Database, Engine, Notification Actions

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/MusicAppViewModel.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/MainActivity.kt`
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackRuntime.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt`

- [x] **Step 1: Add Android playback runtime**

Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackRuntime.kt`:

```kotlin
package com.yanhao.kmpmusic.playback

import android.content.Context
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

class AndroidPlaybackRuntime(
    private val serviceConnector: PlaybackServiceConnector,
) : PlaybackNotificationActions {
    private var controller: MusicAppController? = null

    fun attachContext(context: Context) {
        serviceConnector.attachContext(context = context)
    }

    fun attachController(controller: MusicAppController) {
        this.controller = controller
        PlaybackNotificationDispatcher.attach(actions = this)
        PlaybackCommandBridgeRegistry.attach(bridge = this)
        controller.attachPlaybackUiObserver(::onPlaybackUiStateChanged)
    }

    fun clear() {
        PlaybackNotificationDispatcher.detach()
        PlaybackCommandBridgeRegistry.detach()
        controller = null
    }

    private fun onPlaybackUiStateChanged(uiState: MusicAppUiState) {
        val song = uiState.currentSong ?: return
        serviceConnector.showOrRefreshNotification(
            song = song,
            isPlaying = uiState.isPlaying,
            isFavorite = uiState.likedSongIds.contains(song.id),
            playbackMode = uiState.playbackMode,
            playbackStatus = uiState.playbackStatus,
        )
    }

    override fun toggleFavorite() {
        val currentController = controller ?: return
        currentController.uiState.currentSongId?.let(currentController::toggleFavorite)
    }

    override fun previous() {
        controller?.moveTrack(direction = -1)
    }

    override fun togglePlayback() {
        controller?.togglePlayback()
    }

    override fun next() {
        controller?.moveTrack(direction = 1)
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs = positionMs)
    }

    override fun cycleMode() {
        controller?.cyclePlaybackMode()
    }
}
```

- [x] **Step 2: Extend the service connector with notification refresh**

Add to `PlaybackServiceConnector`:

```kotlin
fun showOrRefreshNotification(
    song: com.yanhao.kmpmusic.domain.model.Song,
    isPlaying: Boolean,
    isFavorite: Boolean,
    playbackMode: com.yanhao.kmpmusic.domain.model.PlaybackMode,
    playbackStatus: com.yanhao.kmpmusic.domain.model.PlaybackStatus,
) {
    ensureServiceStarted()
    PlaybackServiceRegistry.currentService()?.showOrRefreshNotification(
        song = song,
        isPlaying = isPlaying,
        isFavorite = isFavorite,
        playbackMode = playbackMode,
        playbackStatus = playbackStatus,
    )
}
```

- [x] **Step 3: Let service foreground lifecycle follow playback state**

In `MusicPlaybackService`, add a notification controller and manager-backed refresh method:

```kotlin
private lateinit var notificationController: AndroidPlaybackNotificationController
```

Initialize in `onCreate`:

```kotlin
notificationController = AndroidPlaybackNotificationController(context = this)
```

Add method:

```kotlin
fun showOrRefreshNotification(
    song: com.yanhao.kmpmusic.domain.model.Song,
    isPlaying: Boolean,
    isFavorite: Boolean,
    playbackMode: com.yanhao.kmpmusic.domain.model.PlaybackMode,
    playbackStatus: com.yanhao.kmpmusic.domain.model.PlaybackStatus,
) {
    val notification = notificationController.createNotification(
        song = song,
        isPlaying = isPlaying,
        isFavorite = isFavorite,
        playbackMode = playbackMode,
    )
    when (playbackStatus) {
        com.yanhao.kmpmusic.domain.model.PlaybackStatus.Loading,
        com.yanhao.kmpmusic.domain.model.PlaybackStatus.Playing,
        com.yanhao.kmpmusic.domain.model.PlaybackStatus.Buffering,
        -> startForeground(PLAYBACK_NOTIFICATION_ID, notification)
        com.yanhao.kmpmusic.domain.model.PlaybackStatus.Paused,
        com.yanhao.kmpmusic.domain.model.PlaybackStatus.Error,
        com.yanhao.kmpmusic.domain.model.PlaybackStatus.Ended,
        -> {
            getSystemService(android.app.NotificationManager::class.java)
                .notify(PLAYBACK_NOTIFICATION_ID, notification)
            stopForeground(false)
        }
        com.yanhao.kmpmusic.domain.model.PlaybackStatus.Idle -> {
            stopForeground(true)
        }
    }
}
```

- [x] **Step 4: Wire ViewModel to Room, real engine adapter, notification runtime, and command bridge**

Modify `MusicAppViewModel` to extend `AndroidViewModel` and construct the controller with real Android dependencies:

```kotlin
class MusicAppViewModel(
    application: android.app.Application,
) : androidx.lifecycle.AndroidViewModel(application) {
    private val localMusicScanner: MutableLocalMusicScanner = MutableLocalMusicScanner()
    private val permissionSettingsOpener: MutablePermissionSettingsOpener = MutablePermissionSettingsOpener()
    private val playbackDatabase = createAndroidPlaybackDatabase(context = application)
    private val playbackServiceConnector = PlaybackServiceConnector(scope = viewModelScope)
    private val playbackRuntime = AndroidPlaybackRuntime(serviceConnector = playbackServiceConnector)
    private val favoritesRepository = kotlinx.coroutines.runBlocking {
        PersistentFavoritesRepository(
            favoriteSongDao = playbackDatabase.favoriteSongDao(),
            initialLikedSongIds = PersistentFavoritesRepository.loadInitialLikedSongIds(
                favoriteSongDao = playbackDatabase.favoriteSongDao(),
            ),
            nowMillis = { System.currentTimeMillis() },
        )
    }

    val controller: MusicAppController = MusicAppController(
        localMusicScanner = localMusicScanner,
        audioPlayerEngine = playbackServiceConnector,
        playbackSnapshotStore = RoomPlaybackSnapshotStore(
            database = playbackDatabase,
            nowMillis = { System.currentTimeMillis() },
        ),
        injectedFavoritesRepository = favoritesRepository,
        permissionSettingsOpener = permissionSettingsOpener,
        controllerScope = viewModelScope,
        nowMillis = { System.currentTimeMillis() },
    )

    init {
        playbackRuntime.attachContext(context = application)
        playbackRuntime.attachController(controller = controller)
    }

    override fun onCleared() {
        playbackRuntime.clear()
        super.onCleared()
    }

    fun attachLocalMusicScanner(scanner: LocalMusicScanner) {
        localMusicScanner.replace(scanner = scanner)
    }

    fun attachPermissionSettingsOpener(opener: PermissionSettingsOpener) {
        permissionSettingsOpener.replace(opener = opener)
    }
}
```

Add imports for `createAndroidPlaybackDatabase`, `PersistentFavoritesRepository`, `RoomPlaybackSnapshotStore`, `PlaybackServiceConnector`, `AndroidPlaybackRuntime`, and `viewModelScope`.

- [x] **Step 5: Attach Android context without starting a foreground service at launch**

In `MainActivity.onCreate`, keep scanner and permission-opener attachment, and add:

```kotlin
musicAppViewModel.attachPlaybackContext(context = applicationContext)
```

If `MusicAppViewModel` already attaches the application context in `init`, this method may be a no-op wrapper kept for Activity recreation clarity:

```kotlin
fun attachPlaybackContext(context: android.content.Context) {
    playbackRuntime.attachContext(context = context)
}
```

Do not call `ContextCompat.startForegroundService()` from `MainActivity`. Notification permission for Android 13+ is still requested from the Activity, but service startup stays lazy:

```kotlin
private val notificationPermissionLauncher = registerForActivityResult(
    androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
) {}

private fun requestPlaybackNotificationPermissionIfNeeded() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}
```

Call `requestPlaybackNotificationPermissionIfNeeded()` after `musicAppViewModel` is obtained.

- [x] **Step 6: Compile Android**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/MusicAppViewModel.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/MainActivity.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackRuntime.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt
git commit -m "连接 Android 播放运行时"
```

## Task 10: Verify End-to-End and Record Manual Android Acceptance

**Files:**
- Modify: `docs/superpowers/plans/2026-06-23-android-playback-implementation.md` only if execution notes are appended.
- No code files should change unless verification exposes a bug.

- [x] **Step 1: Run shared tests**

Run:

```bash
./gradlew :composeApp:desktopTest
```

Expected: PASS.

Result (2026-06-24): PASS. Ran `./gradlew :composeApp:desktopTest`.

- [x] **Step 2: Run Android compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

Result (2026-06-24): PASS. Ran `./gradlew :composeApp:compileDebugKotlinAndroid`.

- [x] **Step 3: Build debug APK**

Run:

```bash
./gradlew :composeApp:assembleDebug
```

Expected: PASS and APK generated under `composeApp/build/outputs/apk/debug/`.

Result (2026-06-24): PASS. Ran `./gradlew :composeApp:assembleDebug`; verified `composeApp/build/outputs/apk/debug/composeApp-debug.apk` exists.

- [x] **Step 4: Install on connected Android device**

Run:

```bash
adb devices
./gradlew :composeApp:installDebug
```

Expected: at least one device listed as `device`; install succeeds.

Result (2026-06-24): PASS. `adb devices` listed `GUKF4DWOYLFU49QW	device`; `./gradlew :composeApp:installDebug` installed successfully on `PFGM00 - 12`.

- [ ] **Step 5: Manual acceptance checklist**

On the Android device:

```text
1. Grant audio permission.
2. Scan Android MediaStore.
3. Tap a song in 本地音乐 where at least one song exists before and after it.
4. Verify audio plays.
5. Open 播放页 and verify progress moves.
6. Drag progress slider and verify audio seeks.
7. Use mini player play/pause and verify it changes real playback.
8. Tap 下一首 and 上一首, verify queue uses the same list.
9. Cycle modes: 列表循环 -> 单曲循环 -> 随机播放 -> 列表循环.
10. Favorite current song in App and verify list/play page state changes.
11. Background App and verify playback continues.
12. Pull notification shade: collapsed notification shows 上一首 / 播放暂停 / 下一首.
13. Expand notification: verify 收藏 / 上一首 / 播放暂停 / 下一首 / 播放模式.
14. Use notification buttons and verify App state matches.
15. Change favorite and playback mode in App; verify expanded notification icon updates without restarting playback.
16. Use Bluetooth/headset/system media controls or an external MediaController test app for play/pause/previous/next; verify queue index, shuffle history, and App UI match coordinator semantics.
17. Pause playback from notification, background the App for 30 seconds, and verify there is no foreground-service crash or missing-notification error.
18. Kill the process, reopen App, verify current song, queue, mode, progress, and favorite restore as paused.
```

Status (2026-06-24): not completed. This run verified install plus a launch probe only: `adb shell am start -W -n com.yanhao.kmpmusic/.MainActivity` returned `Status: ok`, and `adb shell pidof com.yanhao.kmpmusic` returned a live PID. The device-only playback, notification, background, media-controls, and process-death checklist items above were not manually verified in this run.

- [x] **Step 6: Capture residual risk in final handoff**

If any device-only item cannot be verified, include this exact risk block in the implementation summary:

```markdown
Android device verification not fully completed:
- Playback verified: yes/no
- Background playback verified: yes/no
- Custom notification collapsed/expanded verified: yes/no
- Notification refresh after favorite/mode/status changes verified: yes/no
- MediaSession/system controls route through coordinator verified: yes/no
- Foreground-service lifecycle verified without startup crash: yes/no
- Process death restore verified: yes/no
- Device model and Android version:
```

Implementation summary residual risk for this run:

```markdown
Android device verification not fully completed:
- Playback verified: no
- Background playback verified: no
- Custom notification collapsed/expanded verified: no
- Notification refresh after favorite/mode/status changes verified: no
- MediaSession/system controls route through coordinator verified: no
- Foreground-service lifecycle verified without startup crash: no
- Process death restore verified: no
- Device model and Android version: PFGM00 / Android 12
```

- [x] **Step 7: Commit final verification fixes**

If verification changed one or more tracked playback files, inspect the modified paths:

```bash
git status --short
git diff --name-only
```

Then stage the exact modified playback files shown by `git diff --name-only`. For example, if the modified files are the Android service and controller:

```bash
git add composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/MusicPlaybackService.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt
git commit -m "修复 Android 播放验收问题"
```

If verification required no code changes, do not create an empty commit.

Result (2026-06-24): inspected `git status --short` and `git diff --name-only`. No tracked playback fixes were needed, so no empty commit was created.

## Self-Review

Spec coverage:

- Android real playback via Media3: Task 7 and Task 9.
- Background playback via MediaSessionService: Task 7 and manifest in Task 7.
- Custom foreground media notification collapsed three buttons and expanded five buttons: Task 8.
- Notification refresh/display path tied to controller state: Task 5 and Task 9.
- Android foreground-service lifecycle avoids startup `startForegroundService`: Runtime invariants and Task 9.
- MediaSession/system controls route through coordinator: Task 7 and Task 9.
- Queue from current list and currentIndex: Task 2, Task 5, Task 6.
- Playback modes LoopAll/LoopOne/Shuffle: Task 2 and Task 6.
- Shuffle no-repeat and previous history: Task 2.
- Player page progress and seek, mini player no manual seek: Task 6.
- Room3 `3.0.0-rc01` persistence and cold paused restore: Task 3 and Task 5.
- Favorites persistence and notification/App sync: Task 4, Task 8, Task 9.
- Error auto-skip, precise error classification, success reset, and three-failure limits: Task 2 and Task 7.
- Android true-device verification: Task 10.
- Quick Settings and lock screen not customized: Task 8 and Task 10 avoid them.

Placeholder scan:

- No unresolved placeholder markers remain in task instructions.
- Dependency versions are explicit: Room3 `3.0.0-rc01`, Media3 `1.8.0`, SQLite `2.6.2`, and KSP `2.0.21-1.0.28`.

Type consistency:

- `PlaybackStatus`, `PlaybackMode`, `PlaybackError`, `PlayableMedia`, `PlaybackState`, `QueueState`, and `PlaybackSnapshot` are introduced in Task 1 and reused with the same names.
- `PlaybackCoordinator` methods introduced in Task 2 are used by controller/UI tasks with matching names.
- Room entities and DAOs introduced in Task 3 are used by persistence and favorites repositories with matching names.
