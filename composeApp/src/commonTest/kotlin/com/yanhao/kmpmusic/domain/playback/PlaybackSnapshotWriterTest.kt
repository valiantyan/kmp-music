package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 验证 [PlaybackSnapshotWriter] 的节流、异步写入跟踪和错误透传规则。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackSnapshotWriterTest {
    /**
     * 首次进度事件必须立刻落下一份快照，避免播放刚开始就没有恢复点。
     */
    @Test
    fun firstProgressEventPersistsSnapshot(): Unit = runTest {
        val repository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
        val store: RecordingPlaybackSnapshotStore = RecordingPlaybackSnapshotStore()
        var currentTimeMs: Long = 1_000L
        val writer: PlaybackSnapshotWriter = PlaybackSnapshotWriter(
            playbackRepository = repository,
            playbackSnapshotStore = store,
            snapshotWriteScope = backgroundScope,
            nowMillis = { currentTimeMs },
        )
        repository.saveQueueState(state = QueueState(songIds = listOf("a"), currentIndex = 0))
        repository.savePlaybackState(state = PlaybackState(currentSongId = "a", positionMs = 10L))

        writer.saveForEvent(event = PlaybackEngineEvent.ProgressChanged(positionMs = 10L, durationMs = 100L))
        advanceUntilIdle()
        assertEquals(expected = 1, actual = store.savedSnapshots.size)
        assertEquals(expected = 10L, actual = store.savedSnapshots.single().playbackState.positionMs)
    }

    /**
     * 节流窗口内的连续进度事件不应重复写盘，避免高频进度把 store 压爆。
     */
    @Test
    fun progressEventsInsideThrottleWindowAreSkipped(): Unit = runTest {
        val repository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
        val store: RecordingPlaybackSnapshotStore = RecordingPlaybackSnapshotStore()
        var currentTimeMs: Long = 1_000L
        val writer: PlaybackSnapshotWriter = PlaybackSnapshotWriter(
            playbackRepository = repository,
            playbackSnapshotStore = store,
            snapshotWriteScope = backgroundScope,
            nowMillis = { currentTimeMs },
            snapshotThrottleMs = 5_000L,
        )

        writer.saveForEvent(event = PlaybackEngineEvent.ProgressChanged(positionMs = 10L, durationMs = 100L))
        advanceUntilIdle()
        currentTimeMs = 2_000L
        writer.saveForEvent(event = PlaybackEngineEvent.ProgressChanged(positionMs = 20L, durationMs = 100L))
        advanceUntilIdle()
        assertEquals(expected = 1, actual = store.savedSnapshots.size)
    }

    /**
     * 关键状态事件不能被进度节流误伤，否则暂停、失败等时刻会丢失最新快照。
     */
    @Test
    fun nonProgressEventsAreNotThrottled(): Unit = runTest {
        val repository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
        val store: RecordingPlaybackSnapshotStore = RecordingPlaybackSnapshotStore()
        val writer: PlaybackSnapshotWriter = PlaybackSnapshotWriter(
            playbackRepository = repository,
            playbackSnapshotStore = store,
            snapshotWriteScope = backgroundScope,
            nowMillis = { 1_000L },
            snapshotThrottleMs = 5_000L,
        )

        writer.saveForEvent(event = PlaybackEngineEvent.ProgressChanged(positionMs = 10L, durationMs = 100L))
        advanceUntilIdle()
        writer.saveForEvent(
            event = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Playing,
                positionMs = 10L,
                durationMs = 100L,
            ),
        )
        advanceUntilIdle()
        assertEquals(expected = 2, actual = store.savedSnapshots.size)
    }

    /**
     * teardown 等待必须覆盖已发出的异步写入，避免宿主先销毁再丢最后一笔快照。
     */
    @Test
    fun awaitPendingWritesWaitsForAsyncSaveCompletion(): Unit = runTest {
        val repository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
        val store: RecordingPlaybackSnapshotStore = RecordingPlaybackSnapshotStore(writeDelayMs = 100L)
        val writer: PlaybackSnapshotWriter = PlaybackSnapshotWriter(
            playbackRepository = repository,
            playbackSnapshotStore = store,
            snapshotWriteScope = backgroundScope,
            nowMillis = { 1_000L },
        )

        writer.saveAsync()
        assertEquals(expected = 0, actual = store.savedSnapshots.size)

        val awaitJob: Deferred<Unit> = async {
            writer.awaitPendingWrites()
        }
        advanceTimeBy(delayTimeMillis = 100L)
        advanceUntilIdle()
        awaitJob.await()
        assertEquals(expected = 1, actual = store.savedSnapshots.size)
    }

    /**
     * 同步收口写入失败时必须把异常抛给调用方，不能悄悄吞掉 teardown 风险。
     */
    @Test
    fun saveNowAndAwaitPropagatesSnapshotStoreFailure(): Unit = runTest {
        val repository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
        val writer: PlaybackSnapshotWriter = PlaybackSnapshotWriter(
            playbackRepository = repository,
            playbackSnapshotStore = FailingPlaybackSnapshotStore(),
            snapshotWriteScope = backgroundScope,
            nowMillis = { 1_000L },
        )

        val failure: IllegalStateException = assertFailsWith<IllegalStateException> {
            writer.saveNowAndAwait()
        }
        assertEquals(expected = "snapshot write failed", actual = failure.message)
    }

    /**
     * 记录写入结果并可选制造延迟，便于覆盖异步等待语义。
     */
    private class RecordingPlaybackSnapshotStore(
        private val writeDelayMs: Long = 0L,
    ) : PlaybackSnapshotStore {
        val savedSnapshots: MutableList<PlaybackSnapshot> = mutableListOf()

        /** 保存快照并按需延迟，模拟真实异步持久化。 */
        override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
            if (writeDelayMs > 0L) {
                delay(timeMillis = writeDelayMs)
            }
            savedSnapshots += snapshot
        }
        /** 只要写过一份快照，就视为存在可恢复数据。 */
        override suspend fun hasSavedSnapshot(): Boolean {
            return savedSnapshots.isNotEmpty()
        }

        /** 返回最近一次保存的队列，供接口契约完整实现。 */
        override suspend fun getSavedQueueSongIds(): List<String> {
            return savedSnapshots.lastOrNull()?.queueState?.songIds ?: emptyList()
        }

        /** 返回最近一次保存的快照。 */
        override suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot {
            return savedSnapshots.lastOrNull() ?: PlaybackSnapshot()
        }
    }

    /**
     * 固定抛错的 store，用于验证同步写入时的异常透传。
     */
    private class FailingPlaybackSnapshotStore : PlaybackSnapshotStore {
        /** 每次写入都抛固定异常。 */
        override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
            throw IllegalStateException("snapshot write failed")
        }

        /** 失败 store 不提供历史快照。 */
        override suspend fun hasSavedSnapshot(): Boolean {
            return false
        }

        /** 失败 store 不提供历史队列。 */
        override suspend fun getSavedQueueSongIds(): List<String> {
            return emptyList()
        }

        /** 恢复实现对本测试无影响，返回默认空快照即可。 */
        override suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot {
            return PlaybackSnapshot()
        }
    }
}
