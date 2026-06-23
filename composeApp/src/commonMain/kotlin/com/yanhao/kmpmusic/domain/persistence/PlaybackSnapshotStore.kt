package com.yanhao.kmpmusic.domain.persistence

import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot

/**
 * 播放快照存储接口，供协调器在 common 层保存和恢复运行时快照。
 */
interface PlaybackSnapshotStore {
    /**
     * 保存最新播放快照，供后续恢复使用。
     */
    suspend fun saveSnapshot(snapshot: PlaybackSnapshot)

    /**
     * 读取最近一次保存的播放快照，没有快照时返回 null。
     */
    suspend fun readSnapshot(): PlaybackSnapshot?
}

/**
 * 供当前任务和共享测试使用的内存版播放快照存储。
 */
class InMemoryPlaybackSnapshotStore : PlaybackSnapshotStore {
    // 最近一次保存的快照。
    private var snapshot: PlaybackSnapshot? = null

    /** 在内存中覆盖保存最新快照。 */
    override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
        this.snapshot = snapshot
    }

    /** 读取内存中的最后一份快照。 */
    override suspend fun readSnapshot(): PlaybackSnapshot? = snapshot
}
