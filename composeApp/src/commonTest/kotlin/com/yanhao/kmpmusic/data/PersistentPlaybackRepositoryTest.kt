package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.persistence.PlaybackHistoryDao
import com.yanhao.kmpmusic.domain.persistence.PlaybackHistoryItemEntity
import com.yanhao.kmpmusic.domain.persistence.PlaybackQueueDao
import com.yanhao.kmpmusic.domain.persistence.PlaybackQueueItemEntity
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotDao
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotEntity
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证 [PersistentPlaybackRepository] 的运行态与冷启动恢复态不会互相覆盖。
 */
class PersistentPlaybackRepositoryTest {
    /**
     * 同一进程内保存播放中状态后，读取必须返回精确运行态，不能被持久化恢复规则压成暂停。
     */
    @Test
    fun savePlaybackStatePreservesActiveRuntimeStatus(): Unit = runTest {
        val fixture: PersistentPlaybackRepositoryFixture = PersistentPlaybackRepositoryFixture()
        val repository: PlaybackRepository = fixture.createRepository()

        repository.savePlaybackState(
            state = PlaybackState(
                currentSongId = "song-1",
                status = PlaybackStatus.Playing,
                positionMs = 32_000L,
                durationMs = 52_000L,
            ),
        )

        assertEquals(expected = PlaybackStatus.Playing, actual = repository.getPlaybackState().status)
        assertEquals(expected = 32_000L, actual = repository.getPlaybackState().positionMs)
    }

    /**
     * 新仓库实例代表冷启动，应从持久化快照恢复歌曲和进度，但状态统一回到暂停。
     */
    @Test
    fun newRepositoryRestoresPersistedSnapshotAsPaused(): Unit = runTest {
        val fixture: PersistentPlaybackRepositoryFixture = PersistentPlaybackRepositoryFixture()
        val repository: PlaybackRepository = fixture.createRepository()
        repository.savePlaybackState(
            state = PlaybackState(
                currentSongId = "song-1",
                status = PlaybackStatus.Playing,
                positionMs = 24_000L,
                durationMs = 180_000L,
            ),
        )

        val restoredRepository: PlaybackRepository = fixture.createRepository()
        val restoredState: PlaybackState = restoredRepository.getPlaybackState()

        assertEquals(expected = "song-1", actual = restoredState.currentSongId)
        assertEquals(expected = PlaybackStatus.Paused, actual = restoredState.status)
        assertEquals(expected = 24_000L, actual = restoredState.positionMs)
    }

    /**
     * 保存队列会重写播放快照，但不能顺带把当前运行态折叠成暂停。
     */
    @Test
    fun saveQueueStateKeepsActiveRuntimePlaybackStatus(): Unit = runTest {
        val fixture: PersistentPlaybackRepositoryFixture = PersistentPlaybackRepositoryFixture()
        val repository: PlaybackRepository = fixture.createRepository()
        repository.savePlaybackState(
            state = PlaybackState(
                currentSongId = "song-2",
                status = PlaybackStatus.Loading,
                positionMs = 0L,
                durationMs = null,
            ),
        )

        repository.saveQueueState(
            state = QueueState(
                songIds = listOf("song-1", "song-2"),
                currentIndex = 1,
                playbackMode = PlaybackMode.Shuffle,
            ),
        )

        assertEquals(expected = PlaybackStatus.Loading, actual = repository.getPlaybackState().status)
        assertEquals(expected = 1, actual = repository.getQueueState().currentIndex)
        assertEquals(expected = PlaybackMode.Shuffle, actual = repository.getQueueState().playbackMode)
    }

    private class PersistentPlaybackRepositoryFixture {
        // 播放快照 DAO 复用同一实例，用来模拟跨仓库实例的数据库持久化。
        private val playbackSnapshotDao: FakePlaybackSnapshotDao = FakePlaybackSnapshotDao()

        // 播放队列 DAO 复用同一实例，用来模拟跨仓库实例的数据库持久化。
        private val playbackQueueDao: FakePlaybackQueueDao = FakePlaybackQueueDao()

        // 播放历史 DAO 对本组测试不是重点，但仓库构造需要完整依赖。
        private val playbackHistoryDao: FakePlaybackHistoryDao = FakePlaybackHistoryDao()

        /** 创建新的仓库实例，模拟应用进程内重建或冷启动后的新 repository。 */
        fun createRepository(): PersistentPlaybackRepository {
            return PersistentPlaybackRepository(
                playbackSnapshotDao = playbackSnapshotDao,
                playbackQueueDao = playbackQueueDao,
                playbackHistoryDao = playbackHistoryDao,
                runInWriteTransaction = { block: suspend () -> Unit -> block() },
                nowMillis = { 123L },
            )
        }
    }

    private class FakePlaybackSnapshotDao : PlaybackSnapshotDao {
        // 单行播放快照记录。
        private var snapshot: PlaybackSnapshotEntity? = null

        /** 读取当前保存的播放快照。 */
        override suspend fun getSnapshot(): PlaybackSnapshotEntity? {
            return snapshot
        }

        /** 覆盖保存播放快照。 */
        override suspend fun saveSnapshot(entity: PlaybackSnapshotEntity) {
            snapshot = entity
        }

        /** 清空播放快照。 */
        override suspend fun clearSnapshot() {
            snapshot = null
        }
    }

    private class FakePlaybackQueueDao : PlaybackQueueDao {
        // 用 position 主键模拟队列表。
        private val rows: LinkedHashMap<Int, PlaybackQueueItemEntity> = linkedMapOf()

        /** 按队列位置读取所有歌曲。 */
        override suspend fun getQueueItems(): List<PlaybackQueueItemEntity> {
            return rows.values.sortedBy { item: PlaybackQueueItemEntity -> item.position }
        }

        /** 批量写入队列项。 */
        override suspend fun insertAll(items: List<PlaybackQueueItemEntity>) {
            items.forEach { item: PlaybackQueueItemEntity ->
                rows[item.position] = item
            }
        }

        /** 清空旧队列。 */
        override suspend fun clearQueue() {
            rows.clear()
        }
    }

    private class FakePlaybackHistoryDao : PlaybackHistoryDao {
        // 用 position 主键模拟最近播放历史。
        private val rows: LinkedHashMap<Int, PlaybackHistoryItemEntity> = linkedMapOf()

        /** 按位置读取最近播放历史。 */
        override suspend fun getHistoryItems(): List<PlaybackHistoryItemEntity> {
            return rows.values.sortedBy { item: PlaybackHistoryItemEntity -> item.position }
        }

        /** 批量写入最近播放历史。 */
        override suspend fun insertAll(items: List<PlaybackHistoryItemEntity>) {
            items.forEach { item: PlaybackHistoryItemEntity ->
                rows[item.position] = item
            }
        }

        /** 清空旧播放历史。 */
        override suspend fun clearHistory() {
            rows.clear()
        }
    }
}
