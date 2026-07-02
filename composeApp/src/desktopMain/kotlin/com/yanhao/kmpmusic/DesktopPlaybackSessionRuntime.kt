package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.feature.app.MusicAppController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Desktop 进程级播放会话运行时，统一管理恢复幂等、关闭时序和底层资源收口。
 */
internal class DesktopPlaybackSessionRuntime(
    val controller: MusicAppController,
    private val sessionScope: CoroutineScope,
    private val releaseAudioEngineAndAwait: suspend () -> Unit,
    private val closePlaybackDatabase: () -> Unit,
    private val persistPlaybackSnapshotForProcessTeardown: suspend (Long, Long?) -> Unit = { positionMs, durationMs ->
        controller.persistPlaybackSnapshotForProcessTeardown(
            positionMs = positionMs,
            durationMs = durationMs,
        )
    },
) {
    // [sessionScope] 必须带 Job，关闭时才能等待长生命周期协程完整收口。
    private val sessionJob: Job = sessionScope.coroutineContext[Job]
        ?: error("DesktopPlaybackSessionRuntime 需要带 Job 的会话作用域")

    // 冷启动恢复只允许请求一次，避免窗口重组或多次 attach 覆盖活跃播放态。
    private var hasRequestedPlaybackRestore: Boolean = false

    // close 只允许执行一次，避免重复释放数据库或原生播放器。
    private var isClosed: Boolean = false

    /** 只在 Desktop 进程生命周期内第一次窗口接入时请求快照恢复。 */
    fun ensurePlaybackSnapshotRestoreRequested() {
        synchronized(this) {
            if (hasRequestedPlaybackRestore || isClosed) {
                return
            }
            hasRequestedPlaybackRestore = true
        }
        sessionScope.launch {
            controller.restorePlaybackSnapshot()
        }
    }

    /**
     * 进程关闭前按顺序释放桌面播放器、停止长生命周期协程并同步持久化最终快照。
     */
    fun close() {
        val shouldClose: Boolean = synchronized(this) {
            if (isClosed) {
                false
            } else {
                isClosed = true
                true
            }
        }
        if (!shouldClose) {
            return
        }
        runBlocking {
            var teardownFailure: Throwable? = null
            try {
                releaseAudioEngineAndAwait()
            } catch (throwable: Throwable) {
                teardownFailure = throwable
            } finally {
                try {
                    sessionJob.cancelAndJoin()
                } catch (throwable: Throwable) {
                    teardownFailure = teardownFailure?.also { it.addSuppressed(throwable) } ?: throwable
                }
            }
            val finalPositionMs: Long = controller.uiState.playbackPositionMs
            val finalDurationMs: Long? = controller.uiState.playbackDurationMs
            try {
                persistPlaybackSnapshotForProcessTeardown(
                    finalPositionMs,
                    finalDurationMs,
                )
            } catch (throwable: Throwable) {
                teardownFailure = teardownFailure?.also { it.addSuppressed(throwable) } ?: throwable
            } finally {
                try {
                    closePlaybackDatabase()
                } catch (throwable: Throwable) {
                    teardownFailure = teardownFailure?.also { it.addSuppressed(throwable) } ?: throwable
                }
            }
            teardownFailure?.let { throwable ->
                throw throwable
            }
        }
    }
}
