package com.yanhao.kmpmusic.domain.persistence

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证播放快照存储在恢复时的契约规则。
 */
class PlaybackSnapshotStoreTest {
    /**
     * 恢复快照时应保留队列顺序，并统一以暂停态冷启动。
     */
    @Test
    fun saveAndRestoreSnapshotKeepsQueueOrderAndPausedStatus(): Unit = runTest {
        val store: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore()
        val snapshot: PlaybackSnapshot = PlaybackSnapshot(
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
        val restored: PlaybackSnapshot = store.restoreSnapshot(
            availableSongIds = setOf("song-1", "song-2", "song-3"),
        )

        assertEquals(expected = listOf("song-1", "song-2", "song-3"), actual = restored.queueState.songIds)
        assertEquals(expected = 1, actual = restored.queueState.currentIndex)
        assertEquals(expected = PlaybackMode.Shuffle, actual = restored.queueState.playbackMode)
        assertEquals(expected = PlaybackStatus.Paused, actual = restored.playbackState.status)
        assertEquals(expected = 42_000L, actual = restored.playbackState.positionMs)
    }

    /**
     * 恢复时应过滤已不存在的歌曲，并回退到仍可用的当前项。
     */
    @Test
    fun restoreFiltersMissingSongs(): Unit = runTest {
        val store: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore()
        store.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = "song-2",
                    status = PlaybackStatus.Paused,
                ),
                queueState = QueueState(
                    songIds = listOf("song-1", "song-2"),
                    currentIndex = 1,
                ),
            ),
        )

        val restored: PlaybackSnapshot = store.restoreSnapshot(
            availableSongIds = setOf("song-1"),
        )

        assertEquals(expected = listOf("song-1"), actual = restored.queueState.songIds)
        assertEquals(expected = 0, actual = restored.queueState.currentIndex)
        assertEquals(expected = "song-1", actual = restored.playbackState.currentSongId)
    }

    /**
     * 当恢复后没有任何可用歌曲时，应回退为空队列的空闲态。
     */
    @Test
    fun restoreReturnsIdleWhenQueueBecomesEmptyAfterFiltering(): Unit = runTest {
        val store: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore()
        store.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = "song-2",
                    status = PlaybackStatus.Playing,
                    positionMs = 18_000L,
                ),
                queueState = QueueState(
                    songIds = listOf("song-1", "song-2"),
                    currentIndex = 1,
                ),
            ),
        )

        val restored: PlaybackSnapshot = store.restoreSnapshot(
            availableSongIds = emptySet(),
        )

        assertEquals(expected = emptyList(), actual = restored.queueState.songIds)
        assertEquals(expected = PlaybackStatus.Idle, actual = restored.playbackState.status)
        assertEquals(expected = null, actual = restored.playbackState.currentSongId)
        assertEquals(expected = 0L, actual = restored.playbackState.positionMs)
    }

    /**
     * 当当前歌曲已丢失时，应选择合法下标并把进度归零，避免恢复到错误位置。
     */
    @Test
    fun restoreResetsPositionWhenCurrentSongIsMissing(): Unit = runTest {
        val store: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore()
        store.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = "song-3",
                    status = PlaybackStatus.Playing,
                    positionMs = 99_000L,
                ),
                queueState = QueueState(
                    songIds = listOf("song-1", "song-2", "song-3"),
                    currentIndex = 2,
                    playbackMode = PlaybackMode.LoopAll,
                ),
            ),
        )

        val restored: PlaybackSnapshot = store.restoreSnapshot(
            availableSongIds = setOf("song-1", "song-2"),
        )

        assertEquals(expected = listOf("song-1", "song-2"), actual = restored.queueState.songIds)
        assertEquals(expected = 1, actual = restored.queueState.currentIndex)
        assertEquals(expected = "song-2", actual = restored.playbackState.currentSongId)
        assertEquals(expected = 0L, actual = restored.playbackState.positionMs)
        assertEquals(expected = PlaybackStatus.Paused, actual = restored.playbackState.status)
    }
}
