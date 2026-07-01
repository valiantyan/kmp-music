package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

/**
 * 持久化播放快照，并跟踪异步写入任务，供 teardown 安全收口。
 */
internal class PlaybackSnapshotWriter(
    // 运行时播放仓库，提供当前播放状态与队列真相源。
    private val playbackRepository: PlaybackRepository,
    // 快照存储，负责承接最终持久化。
    private val playbackSnapshotStore: PlaybackSnapshotStore,
    // 独立快照写入作用域，避免依赖 facade 是否启动事件收集。
    private val snapshotWriteScope: CoroutineScope,
    // 当前时间提供者，供节流与快照时间戳复用。
    private val nowMillis: () -> Long,
    // 播放进度事件的最小写盘间隔。
    private val snapshotThrottleMs: Long = 5_000L,
) {
    // 当前仍在执行的快照写入任务集合。
    private val pendingWrites: MutableSet<Deferred<Unit>> = linkedSetOf()

    // [pendingWrites] 的同步锁，避免 teardown 与完成回调并发读写。
    private val pendingWritesLock: Any = Any()

    // 最近一次播放进度写入快照的时间。
    private var lastProgressSnapshotAt: Long? = null

    /**
     * 异步写入当前快照，并把任务纳入 pending 集合供后续等待。
     */
    internal fun saveAsync(): Deferred<Unit> {
        val job: Deferred<Unit> = snapshotWriteScope.async(start = CoroutineStart.UNDISPATCHED) {
            saveCurrentSnapshot()
        }
        synchronized(lock = pendingWritesLock) {
            pendingWrites += job
        }
        job.invokeOnCompletion {
            synchronized(lock = pendingWritesLock) {
                pendingWrites.remove(element = job)
            }
        }
        return job
    }

    /**
     * 按引擎事件决定是否写盘；只有进度事件会按节流窗口跳过。
     */
    internal fun saveForEvent(event: PlaybackEngineEvent) {
        if (event is PlaybackEngineEvent.ProgressChanged) {
            val now: Long = nowMillis()
            val previousSnapshotAt: Long? = lastProgressSnapshotAt
            if (previousSnapshotAt != null && now - previousSnapshotAt < snapshotThrottleMs) {
                return
            }
            lastProgressSnapshotAt = now
        }
        saveAsync()
    }

    /**
     * 同步保存当前快照，供进程退出前写入最后一帧恢复点。
     */
    internal suspend fun saveNowAndAwait() {
        saveCurrentSnapshot()
    }

    /**
     * 等待当前已发出的异步写入全部完成，避免宿主先销毁持久化依赖。
     */
    internal suspend fun awaitPendingWrites() {
        while (true) {
            val pendingJobs: List<Deferred<Unit>> = synchronized(lock = pendingWritesLock) {
                pendingWrites.toList()
            }
            if (pendingJobs.isEmpty()) {
                return
            }
            pendingJobs.awaitAll()
        }
    }

    /**
     * 读取仓库中的当前播放状态并保存成最新快照。
     */
    private suspend fun saveCurrentSnapshot() {
        playbackSnapshotStore.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = playbackRepository.getPlaybackState(),
                queueState = playbackRepository.getQueueState(),
                updatedAt = nowMillis(),
            ),
        )
    }
}
