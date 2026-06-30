package com.yanhao.kmpmusic.data

import androidx.room3.withWriteTransaction
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.domain.persistence.PlaybackHistoryDao
import com.yanhao.kmpmusic.domain.persistence.PlaybackHistoryItemEntity
import com.yanhao.kmpmusic.domain.persistence.PlaybackQueueDao
import com.yanhao.kmpmusic.domain.persistence.PlaybackQueueItemEntity
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotDao
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotEntity
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import kotlinx.coroutines.runBlocking

/**
 * 基于 Room 的播放仓库，把播放状态、队列和真实最近播放历史保存到本地数据库。
 */
class PersistentPlaybackRepository(
    private val playbackSnapshotDao: PlaybackSnapshotDao,
    private val playbackQueueDao: PlaybackQueueDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val runInWriteTransaction: suspend (suspend () -> Unit) -> Unit = { block -> block() },
    private val nowMillis: () -> Long = { currentTimeMillis() },
) : PlaybackRepository {
    /** 读取最近一次播放状态，冷启动时以暂停态恢复，避免自动播放。 */
    override fun getPlaybackState(): PlaybackState = runBlocking {
        readPlaybackState()
    }

    /** 保存当前播放状态，并保留已知队列位置和播放模式。 */
    override fun savePlaybackState(state: PlaybackState) {
        runBlocking {
            val queueState: QueueState = readQueueState()
            playbackSnapshotDao.saveSnapshot(
                entity = state.toEntity(
                    queueState = queueState,
                    updatedAt = nowMillis(),
                ),
            )
        }
    }

    /** 读取持久化播放队列。 */
    override fun getQueueState(): QueueState = runBlocking {
        readQueueState()
    }

    /** 覆盖保存播放队列，同时保留当前播放快照中的歌曲与进度。 */
    override fun saveQueueState(state: QueueState) {
        runBlocking {
            runInWriteTransaction {
                playbackQueueDao.clearQueue()
                playbackQueueDao.insertAll(items = state.toEntities())
                val playbackState: PlaybackState = readPlaybackState()
                playbackSnapshotDao.saveSnapshot(
                    entity = playbackState.toEntity(
                        queueState = state,
                        updatedAt = nowMillis(),
                    ),
                )
            }
        }
    }

    /** 读取真实最近播放历史。 */
    override fun getPlaybackHistory(): PlaybackHistory = runBlocking {
        PlaybackHistory(
            songIds = playbackHistoryDao.getHistoryItems().map { entity: PlaybackHistoryItemEntity ->
                entity.songId
            },
        )
    }

    /** 覆盖保存真实最近播放历史。 */
    override fun savePlaybackHistory(history: PlaybackHistory) {
        runBlocking {
            runInWriteTransaction {
                playbackHistoryDao.clearHistory()
                playbackHistoryDao.insertAll(items = history.toEntities(updatedAt = nowMillis()))
            }
        }
    }

    // 从队列表和快照组合出队列状态，避免播放模式丢失。
    private suspend fun readQueueState(): QueueState {
        val queueItems: List<PlaybackQueueItemEntity> = playbackQueueDao.getQueueItems()
        val snapshot: PlaybackSnapshotEntity? = playbackSnapshotDao.getSnapshot()
        val mode: PlaybackMode = snapshot?.playbackMode?.let { name: String ->
            PlaybackMode.entries.firstOrNull { mode: PlaybackMode -> mode.name == name }
        } ?: PlaybackMode.LoopAll
        return QueueState(
            songIds = queueItems.map { item: PlaybackQueueItemEntity -> item.songId },
            currentIndex = snapshot?.currentIndex ?: queueItems.firstOrNull()?.position ?: 0,
            playbackMode = mode,
        )
    }

    // 事务内部复用的挂起读取，避免嵌套阻塞调用。
    private suspend fun readPlaybackState(): PlaybackState {
        val snapshot: PlaybackSnapshotEntity = playbackSnapshotDao.getSnapshot() ?: return PlaybackState()
        return PlaybackState(
            currentSongId = snapshot.currentSongId,
            status = PlaybackStatus.Paused,
            positionMs = snapshot.positionMs,
            durationMs = snapshot.durationMs,
        )
    }

    // 将播放状态与队列状态合成单行快照。
    private fun PlaybackState.toEntity(
        queueState: QueueState,
        updatedAt: Long,
    ): PlaybackSnapshotEntity {
        return PlaybackSnapshotEntity(
            currentSongId = currentSongId,
            currentIndex = queueState.currentIndex,
            playbackMode = queueState.playbackMode.name,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAt = updatedAt,
        )
    }

    // 将队列顺序固化到数据库位置。
    private fun QueueState.toEntities(): List<PlaybackQueueItemEntity> {
        return songIds.mapIndexed { index: Int, songId: String ->
            PlaybackQueueItemEntity(
                position = index,
                songId = songId,
            )
        }
    }

    // 将最近播放顺序固化到数据库位置。
    private fun PlaybackHistory.toEntities(updatedAt: Long): List<PlaybackHistoryItemEntity> {
        return songIds.distinct().mapIndexed { index: Int, songId: String ->
            PlaybackHistoryItemEntity(
                position = index,
                songId = songId,
                updatedAt = updatedAt,
            )
        }
    }

    companion object {
        /**
         * 从 [PlaybackDatabase] 创建仓库，确保覆盖队列和历史时保持事务一致。
         */
        fun create(
            playbackDatabase: PlaybackDatabase,
            nowMillis: () -> Long = { currentTimeMillis() },
        ): PersistentPlaybackRepository {
            return PersistentPlaybackRepository(
                playbackSnapshotDao = playbackDatabase.playbackSnapshotDao(),
                playbackQueueDao = playbackDatabase.playbackQueueDao(),
                playbackHistoryDao = playbackDatabase.playbackHistoryDao(),
                runInWriteTransaction = { block: suspend () -> Unit ->
                    playbackDatabase.withWriteTransaction {
                        block()
                    }
                },
                nowMillis = nowMillis,
            )
        }
    }
}
