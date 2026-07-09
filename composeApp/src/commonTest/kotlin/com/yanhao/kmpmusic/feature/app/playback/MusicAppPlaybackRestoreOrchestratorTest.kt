package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshotIdentity
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MusicAppPlaybackRestoreOrchestratorTest {
    @Test
    fun snapshotIdentityIncludesCurrentSongPositionAndUpdatedAt(): Unit = runTest {
        val firstStore: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore()
        firstStore.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = "song-1",
                    positionMs = 1_000L,
                ),
                queueState = QueueState(
                    songIds = listOf("song-1", "song-2"),
                    currentIndex = 0,
                ),
                updatedAt = 10L,
            ),
        )
        val secondStore: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore()
        secondStore.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = "song-2",
                    positionMs = 2_000L,
                ),
                queueState = QueueState(
                    songIds = listOf("song-1", "song-2"),
                    currentIndex = 1,
                ),
                updatedAt = 20L,
            ),
        )

        assertTrue(actual = firstStore.getSavedSnapshotIdentity() != secondStore.getSavedSnapshotIdentity())
    }

    @Test
    fun restoreReturnsEmptyResultWithoutPendingRequest(): Unit = runTest {
        val orchestrator: PlaybackRestoreOrchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = snapshotStoreWithQueue(songIds = listOf("missing")),
            availableSongsResolver = { _, _ -> emptyList() },
        )

        val result: PlaybackRestoreOrchestrator.Result = orchestrator.restore(
            state = testState(),
            preferredSongs = emptyList(),
            pendingRequest = null,
            isRequestCurrent = { true },
        )

        assertEquals(expected = null, actual = result.pendingRequest)
        assertEquals(expected = null, actual = result.queueSongsSnapshot)
        assertFalse(actual = result.didHydrateSnapshot)
    }

    @Test
    fun restoreWaitsUntilEntireSavedQueueAndCurrentSongAreResolvable(): Unit = runTest {
        val store: PlaybackSnapshotStore = snapshotStoreWithQueue(songIds = listOf("song-1", "song-2"))
        val orchestrator: PlaybackRestoreOrchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = store,
            availableSongsResolver = { _, preferredSongs: List<Song> -> preferredSongs },
        )
        val request: PendingPlaybackSnapshotRequest = orchestrator.createPendingRequest()
            ?: error("保存快照应创建待加载请求")

        val result: PlaybackRestoreOrchestrator.Result = orchestrator.restore(
            state = testState(),
            preferredSongs = listOf(testSong(id = "song-1")),
            pendingRequest = request,
            isRequestCurrent = { true },
        )

        assertEquals(expected = request, actual = result.pendingRequest)
        assertNull(actual = result.restoredSnapshot)
        assertEquals(expected = null, actual = result.queueSongsSnapshot)
        assertFalse(actual = result.didHydrateSnapshot)
    }

    @Test
    fun restoreClearsPendingRequestWhenSavedIdentityChanges(): Unit = runTest {
        val store: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore()
        store.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = "song-1",
                    positionMs = 1_000L,
                ),
                queueState = QueueState(
                    songIds = listOf("song-1"),
                    currentIndex = 0,
                ),
                updatedAt = 10L,
            ),
        )
        val orchestrator: PlaybackRestoreOrchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = store,
            availableSongsResolver = { _, preferredSongs: List<Song> -> preferredSongs },
        )
        val request: PendingPlaybackSnapshotRequest = orchestrator.createPendingRequest()
            ?: error("保存快照应创建待加载请求")
        store.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = "song-2",
                    positionMs = 2_000L,
                ),
                queueState = QueueState(
                    songIds = listOf("song-2"),
                    currentIndex = 0,
                ),
                updatedAt = 20L,
            ),
        )

        val result: PlaybackRestoreOrchestrator.Result = orchestrator.restore(
            state = testState(),
            preferredSongs = listOf(testSong(id = "song-2")),
            pendingRequest = request,
            isRequestCurrent = { true },
        )

        assertEquals(expected = null, actual = result.pendingRequest)
        assertEquals(expected = null, actual = result.queueSongsSnapshot)
        assertFalse(actual = result.didHydrateSnapshot)
    }

    @Test
    fun restoreDropsHydratedPayloadWhenRequestInvalidatesDuringSnapshotLoad(): Unit = runTest {
        val availableSongs: List<Song> = listOf(testSong(id = "song-1"))
        val identity: PlaybackSnapshotIdentity = PlaybackSnapshotIdentity(
            queueSongIds = listOf("song-1"),
            currentSongId = "song-1",
            currentIndex = 0,
            positionMs = 1_000L,
            updatedAt = 10L,
        )
        val restoreStarted: CompletableDeferred<Unit> = CompletableDeferred()
        val allowRestoreToFinish: CompletableDeferred<Unit> = CompletableDeferred()
        val store: PlaybackSnapshotStore = object : PlaybackSnapshotStore {
            override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
                error("测试不需要写入快照")
            }
            override suspend fun hasSavedSnapshot(): Boolean {
                return true
            }
            override suspend fun getSavedQueueSongIds(): List<String> {
                return identity.queueSongIds
            }
            override suspend fun getSavedSnapshotIdentity(): PlaybackSnapshotIdentity? {
                return identity
            }
            override suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot {
                restoreStarted.complete(value = Unit)
                allowRestoreToFinish.await()
                return PlaybackSnapshot(
                    playbackState = PlaybackState(
                        currentSongId = "song-1",
                        positionMs = 1_000L,
                    ),
                    queueState = QueueState(
                        songIds = listOf("song-1"),
                        currentIndex = 0,
                    ),
                    updatedAt = 10L,
                )
            }
        }
        val orchestrator: PlaybackRestoreOrchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = store,
            availableSongsResolver = { _, _ -> availableSongs },
        )
        val request: PendingPlaybackSnapshotRequest = PendingPlaybackSnapshotRequest(identity = identity)
        var isCurrent: Boolean = true

        val deferredResult = backgroundScope.async {
            orchestrator.restore(
                state = testState(),
                preferredSongs = availableSongs,
                pendingRequest = request,
                isRequestCurrent = { isCurrent },
            )
        }
        restoreStarted.await()
        isCurrent = false
        allowRestoreToFinish.complete(value = Unit)

        val result: PlaybackRestoreOrchestrator.Result = deferredResult.await()

        assertNull(actual = result.restoredSnapshot)
        assertNull(actual = result.queueSongsSnapshot)
        assertNull(actual = result.pendingRequest)
        assertFalse(actual = result.didHydrateSnapshot)
    }

    @Test
    fun restoreClearsPendingRequestWhenIdentityHasNoCurrentSong(): Unit = runTest {
        val request: PendingPlaybackSnapshotRequest = PendingPlaybackSnapshotRequest(
            identity = PlaybackSnapshotIdentity(
                queueSongIds = listOf("song-1"),
                currentSongId = null,
                currentIndex = 0,
                positionMs = 0L,
                updatedAt = 10L,
            ),
        )
        val store: PlaybackSnapshotStore = object : PlaybackSnapshotStore {
            override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
                error("测试不需要写入快照")
            }
            override suspend fun hasSavedSnapshot(): Boolean {
                return true
            }
            override suspend fun getSavedQueueSongIds(): List<String> {
                return request.identity.queueSongIds
            }
            override suspend fun getSavedSnapshotIdentity(): PlaybackSnapshotIdentity? {
                return request.identity
            }
            override suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot {
                error("没有当前歌曲时不应触发快照恢复")
            }
        }
        val orchestrator: PlaybackRestoreOrchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = store,
            availableSongsResolver = { _, preferredSongs: List<Song> -> preferredSongs },
        )

        val result: PlaybackRestoreOrchestrator.Result = orchestrator.restore(
            state = testState(),
            preferredSongs = listOf(testSong(id = "song-1")),
            pendingRequest = request,
            isRequestCurrent = { true },
        )

        assertNull(actual = result.restoredSnapshot)
        assertNull(actual = result.queueSongsSnapshot)
        assertNull(actual = result.pendingRequest)
        assertFalse(actual = result.didHydrateSnapshot)
    }

    @Test
    fun restoreResolvesAvailableSongsAndReturnsHydratedPayload(): Unit = runTest {
        val song: Song = testSong(id = "song-1")
        val orchestrator: PlaybackRestoreOrchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = snapshotStoreWithQueue(songIds = listOf("song-1")),
            availableSongsResolver = { songIds: List<String>, preferredSongs: List<Song> ->
                preferredSongs.filter { candidate: Song -> songIds.contains(element = candidate.id) }
            },
        )
        val request: PendingPlaybackSnapshotRequest = orchestrator.createPendingRequest()
            ?: error("保存快照应创建待加载请求")

        val result: PlaybackRestoreOrchestrator.Result = orchestrator.restore(
            state = testState(),
            preferredSongs = listOf(song),
            pendingRequest = request,
            isRequestCurrent = { candidate: PendingPlaybackSnapshotRequest -> candidate == request },
        )

        assertFalse(actual = result.pendingRequest != null)
        assertTrue(actual = result.didHydrateSnapshot)
        assertEquals(expected = "song-1", actual = result.restoredSnapshot?.playbackState?.currentSongId)
        assertEquals(
            expected = listOf("song-1"),
            actual = result.queueSongsSnapshot?.map { restoredSong: Song -> restoredSong.id },
        )
    }

    @Test
    fun createPendingRequestReturnsNullWhenNoSavedQueueExists(): Unit = runTest {
        val orchestrator: PlaybackRestoreOrchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = InMemoryPlaybackSnapshotStore(),
            availableSongsResolver = { _, preferredSongs: List<Song> -> preferredSongs },
        )

        val request: PendingPlaybackSnapshotRequest? = orchestrator.createPendingRequest()

        assertEquals(expected = null, actual = request)
    }

    private suspend fun snapshotStoreWithQueue(songIds: List<String>): PlaybackSnapshotStore {
        val store: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore()
        store.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = songIds.firstOrNull(),
                    status = PlaybackStatus.Playing,
                ),
                queueState = QueueState(
                    songIds = songIds,
                    currentIndex = 0,
                ),
                updatedAt = 0L,
            ),
        )
        return store
    }

    private fun testState(): MusicAppUiState {
        return MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        )
    }

    private fun testSong(id: String): Song {
        return Song(
            id = id,
            title = id,
            artist = "Artist",
            album = "Album",
            duration = "03:00",
            coverArt = CoverArt.HeroLocalMusic,
            isLiked = false,
            lastPlayed = "",
            quality = "Lossless",
            lyric = "",
            trackNumber = 1,
            durationMs = 180_000L,
        )
    }
}
