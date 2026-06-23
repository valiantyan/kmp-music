package com.yanhao.kmpmusic.domain.persistence

import androidx.room3.withWriteTransaction
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState

/**
 * 播放快照存储接口，负责保存最新运行态并按冷启动规则恢复。
 */
interface PlaybackSnapshotStore {
    /**
     * 保存最新播放快照，供后续恢复使用。
     *
     * @param snapshot 需要持久化的播放快照。
     */
    suspend fun saveSnapshot(snapshot: PlaybackSnapshot)

    /**
     * 按当前可用歌曲集合恢复快照，确保冷启动不会引用失效歌曲。
     *
     * @param availableSongIds 当前仍然可用的歌曲标识集合。
     * @return 可安全用于恢复的播放快照。
     */
    suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot
}

/**
 * 供共享测试和纯内存场景使用的快照存储实现。
 */
class InMemoryPlaybackSnapshotStore : PlaybackSnapshotStore {
    // 最近一次保存的快照。
    private var snapshot: PlaybackSnapshot = PlaybackSnapshot()

    /**
     * 在内存中覆盖保存最新快照。
     *
     * @param snapshot 需要保存的播放快照。
     */
    override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
        this.snapshot = snapshot
    }

    /**
     * 按恢复规则返回当前内存中的快照。
     *
     * @param availableSongIds 当前仍可恢复的歌曲集合。
     * @return 已过滤不可用歌曲且重置冷启动状态的快照。
     */
    override suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot {
        return snapshot.filterForRestore(availableSongIds = availableSongIds)
    }
}

/**
 * 基于 [PlaybackDatabase] 的 Room3 快照存储实现。
 */
class RoomPlaybackSnapshotStore(
    // 播放快照数据库访问入口。
    private val database: PlaybackDatabase,
    // 当前时间提供者，便于测试验证更新时间。
    private val nowMillis: () -> Long = { 0L },
) : PlaybackSnapshotStore {
    /**
     * 把队列与播放主记录一起写入数据库，避免顺序和当前项分离。
     *
     * @param snapshot 需要持久化的播放快照。
     */
    override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
        database.withWriteTransaction {
            database.playbackQueueDao().clearQueue()
            database.playbackQueueDao().insertAll(
                items = snapshot.queueState.songIds.mapIndexed { index: Int, songId: String ->
                    PlaybackQueueItemEntity(
                        position = index,
                        songId = songId,
                    )
                },
            )
            database.playbackSnapshotDao().saveSnapshot(
                entity = PlaybackSnapshotEntity(
                    currentSongId = snapshot.playbackState.currentSongId,
                    currentIndex = snapshot.queueState.currentIndex,
                    playbackMode = snapshot.queueState.playbackMode.name,
                    positionMs = snapshot.playbackState.positionMs,
                    durationMs = snapshot.playbackState.durationMs,
                    updatedAt = snapshot.updatedAt.takeIf { value: Long -> value > 0L } ?: nowMillis(),
                ),
            )
        }
    }

    /**
     * 从数据库读取原始快照，再统一套用恢复过滤规则。
     *
     * @param availableSongIds 当前仍可恢复的歌曲集合。
     * @return 可安全恢复的播放快照，没有记录时返回默认空快照。
     */
    override suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot {
        val snapshotEntity: PlaybackSnapshotEntity = database.playbackSnapshotDao().getSnapshot()
            ?: return PlaybackSnapshot()
        val queueIds: List<String> = database.playbackQueueDao().getQueueItems().map { item: PlaybackQueueItemEntity ->
            item.songId
        }
        val playbackMode: PlaybackMode = PlaybackMode.entries.firstOrNull { mode: PlaybackMode ->
            mode.name == snapshotEntity.playbackMode
        } ?: PlaybackMode.LoopAll
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
                playbackMode = playbackMode,
            ),
            updatedAt = snapshotEntity.updatedAt,
        ).filterForRestore(availableSongIds = availableSongIds)
    }
}

/**
 * 统一实现冷启动恢复规则，避免多个 store 各自拷贝过滤逻辑。
 *
 * @param availableSongIds 当前仍可恢复的歌曲集合。
 * @return 可安全恢复的播放快照；没有任何可用歌曲时返回空快照。
 */
private fun PlaybackSnapshot.filterForRestore(availableSongIds: Set<String>): PlaybackSnapshot {
    val filteredSongIds: List<String> = queueState.songIds.filter { songId: String ->
        availableSongIds.contains(songId)
    }
    if (filteredSongIds.isEmpty()) {
        return PlaybackSnapshot()
    }
    val requestedSongId: String? = playbackState.currentSongId
    val restoredIndex: Int = filteredSongIds.indexOf(requestedSongId).takeIf { index: Int ->
        index >= 0
    } ?: queueState.currentIndex.coerceIn(
        minimumValue = 0,
        maximumValue = filteredSongIds.lastIndex,
    )
    val restoredSongId: String = filteredSongIds[restoredIndex]
    return copy(
        playbackState = playbackState.copy(
            currentSongId = restoredSongId,
            status = PlaybackStatus.Paused,
            positionMs = if (restoredSongId == requestedSongId) playbackState.positionMs else 0L,
            error = null,
        ),
        queueState = queueState.copy(
            songIds = filteredSongIds,
            currentIndex = restoredIndex,
            shuffleHistory = emptyList(),
            shuffleRemaining = emptyList(),
        ),
    )
}
