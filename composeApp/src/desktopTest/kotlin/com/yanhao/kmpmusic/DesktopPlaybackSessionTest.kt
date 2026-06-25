package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.data.createDesktopPlaybackDatabaseAtPath
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.persistence.LocalSongEntity
import com.yanhao.kmpmusic.domain.persistence.RoomPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import com.yanhao.kmpmusic.feature.app.MusicAppController
import java.nio.file.Files
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopPlaybackSessionTest {
    @Test
    fun ensurePlaybackSnapshotRestoreRequestedRestoresPersistedSongOnlyOnce(): Unit = runBlocking {
        val tempDir = Files.createTempDirectory("kmp-music-desktop-session-restore")
        val playbackDatabase = createDesktopPlaybackDatabaseAtPath(
            databasePath = tempDir.resolve("playback.db").toString(),
        )
        val sessionScope = CoroutineScope(SupervisorJob() + Default)
        val audioEngine = RecordingAudioPlayerEngine()
        val expectedSong = persistedSongEntity()
        playbackDatabase.localSongDao().upsertSongs(songs = listOf(expectedSong))
        RoomPlaybackSnapshotStore(
            database = playbackDatabase,
            nowMillis = { 1L },
        ).saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = expectedSong.id,
                    status = PlaybackStatus.Playing,
                    positionMs = 48_000L,
                    durationMs = 180_000L,
                ),
                queueState = QueueState(
                    songIds = listOf(expectedSong.id),
                    currentIndex = 0,
                    playbackMode = PlaybackMode.LoopAll,
                ),
                updatedAt = 1L,
            ),
        )
        val controller = createDesktopPlaybackController(
            playbackDatabase = playbackDatabase,
            audioPlayerEngine = audioEngine,
            controllerScope = sessionScope,
            nowMillis = { 1L },
        )
        val runtime = DesktopPlaybackSessionRuntime(
            controller = controller,
            sessionScope = sessionScope,
            releaseAudioEngineAndAwait = {},
            closePlaybackDatabase = {
                playbackDatabase.close()
            },
        )

        runtime.ensurePlaybackSnapshotRestoreRequested()
        runtime.ensurePlaybackSnapshotRestoreRequested()
        withTimeout(timeMillis = 2_000L) {
            while (controller.uiState.currentSongId != expectedSong.id) {
                delay(timeMillis = 10L)
            }
        }

        assertEquals(expected = expectedSong.id, actual = controller.uiState.currentSongId)
        assertEquals(expected = PlaybackStatus.Paused, actual = controller.uiState.playbackStatus)
        assertEquals(expected = 48_000L, actual = controller.uiState.playbackPositionMs)
        assertEquals(expected = listOf(expectedSong.id), actual = controller.uiState.queueSongIds)
        assertEquals(expected = 1, actual = audioEngine.setQueueCalls)

        runtime.close()
    }

    @Test
    fun closeWaitsForPersistenceThenClosesDatabaseAfterCancellingSessionScope(): Unit = runTest {
        val sessionScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val controller = MusicAppController(
            controllerScope = sessionScope,
        )
        val order = mutableListOf<String>()
        val persistStarted = CompletableDeferred<Unit>()
        val allowPersistFinish = CompletableDeferred<Unit>()
        val sessionJob = checkNotNull(sessionScope.coroutineContext[Job])
        val runtime = DesktopPlaybackSessionRuntime(
            controller = controller,
            sessionScope = sessionScope,
            releaseAudioEngineAndAwait = {
                order += "release"
            },
            closePlaybackDatabase = {
                val isCancelled = sessionScope.coroutineContext[Job]?.isCancelled == true
                order += "close-db:$isCancelled"
            },
            persistPlaybackSnapshotForProcessTeardown = { _, _ ->
                order += "persist-start"
                persistStarted.complete(value = Unit)
                allowPersistFinish.await()
                order += "persist-end"
            },
        )

        val closeJob = launch(context = Dispatchers.Default) {
            runtime.close()
            order += "close-returned"
        }

        persistStarted.await()

        assertEquals(expected = listOf("release", "persist-start"), actual = order)
        assertFalse(actual = closeJob.isCompleted)
        assertTrue(actual = sessionScope.coroutineContext[Job]?.isCancelled == true)

        allowPersistFinish.complete(value = Unit)
        closeJob.join()

        assertEquals(
            expected = listOf("release", "persist-start", "persist-end", "close-db:true", "close-returned"),
            actual = order,
        )
        assertTrue(actual = sessionJob?.isCancelled == true)
    }

    @Test
    fun closeStillPersistsSnapshotAndClosesDatabaseWhenAudioReleaseFails(): Unit = runTest {
        val sessionScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val controller = MusicAppController(
            controllerScope = sessionScope,
        )
        val order = mutableListOf<String>()
        val runtime = DesktopPlaybackSessionRuntime(
            controller = controller,
            sessionScope = sessionScope,
            releaseAudioEngineAndAwait = {
                order += "release"
                error("release failed")
            },
            closePlaybackDatabase = {
                order += "close-db:${sessionScope.coroutineContext[Job]?.isCancelled == true}"
            },
            persistPlaybackSnapshotForProcessTeardown = { _, _ ->
                order += "persist"
            },
        )

        val failure = assertFailsWith<IllegalStateException> {
            runtime.close()
        }

        assertEquals(expected = "release failed", actual = failure.message)
        assertEquals(
            expected = listOf("release", "persist", "close-db:true"),
            actual = order,
        )
        assertTrue(actual = sessionScope.coroutineContext[Job]?.isCancelled == true)
    }

    @Test
    fun closePersistsPositionAfterAudioReleaseDrainsUpdates(): Unit = runTest {
        val sessionScope = CoroutineScope(SupervisorJob() + Default)
        val controller = MusicAppController(
            controllerScope = sessionScope,
        )
        val persistedPositions = mutableListOf<Long>()
        controller.seekTo(positionMs = 12_000L)
        val runtime = DesktopPlaybackSessionRuntime(
            controller = controller,
            sessionScope = sessionScope,
            releaseAudioEngineAndAwait = {
                controller.seekTo(positionMs = 77_000L)
            },
            closePlaybackDatabase = {},
            persistPlaybackSnapshotForProcessTeardown = { positionMs, _ ->
                persistedPositions += positionMs
            },
        )

        runtime.close()

        assertEquals(expected = listOf(77_000L), actual = persistedPositions)
    }

    private fun persistedSongEntity(): LocalSongEntity {
        return LocalSongEntity(
            id = "desktopFolder:restored-song",
            sourceId = "restored-song",
            sourceKind = LocalMusicSourceKind.DesktopFolder.value,
            localUri = "file:///Users/tester/Music/restored-song.mp3",
            fileName = "restored-song.mp3",
            title = "Restored Song",
            artist = "Desktop Artist",
            album = "Desktop Album",
            durationMs = 180_000L,
            mimeType = "audio/mpeg",
            sizeBytes = 1_024L,
            modifiedAt = 1L,
            coverArt = CoverArt.HeroLocalMusic.name,
            lastScannedAt = 1L,
            isAvailable = true,
        )
    }
}

private class RecordingAudioPlayerEngine : AudioPlayerEngine {
    override val events: Flow<PlaybackEngineEvent> = emptyFlow()

    var setQueueCalls: Int = 0
        private set

    override suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        setQueueCalls += 1
    }

    override fun play() = Unit

    override fun pause() = Unit

    override fun seekTo(positionMs: Long) = Unit

    override fun skipToIndex(index: Int) = Unit

    override fun setPlaybackMode(playbackMode: PlaybackMode) = Unit

    override fun stop() = Unit
}
