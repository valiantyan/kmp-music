package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.feature.app.MusicAppController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSRecursiveLock

/**
 * iOS 平台播放会话运行时，集中管理 controller、播放器和 audio session 的进程级生命周期。
 */
internal class IosPlaybackSessionRuntime(
    /** iOS 进程级共享控制器，供 Compose UI 重组后继续复用同一份播放事实。 */
    val controller: MusicAppController,
    // 脱离 Compose composition 的长生命周期作用域。
    private val sessionScope: CoroutineScope,
    // 冷启动恢复入口，测试可替换以验证幂等。
    private val restorePlaybackSnapshot: suspend () -> Unit = {
        controller.restorePlaybackSnapshot()
    },
    // 真实播放器释放入口。
    private val releaseAudioEngine: suspend () -> Unit,
    // audio session 释放入口。
    private val releaseAudioSession: () -> Unit,
) {
    // [sessionScope] 必须带 Job，关闭时才能等待长生命周期协程完整收口。
    private val sessionJob: Job = sessionScope.coroutineContext[Job]
        ?: error("IosPlaybackSessionRuntime 需要带 Job 的会话作用域")

    // 保护宿主和 Compose 入口可能并发触发的一次性状态。
    private val stateLock: NSRecursiveLock = NSRecursiveLock()

    // 冷启动恢复只允许请求一次，避免 UI 重建覆盖活跃播放态。
    private var hasRequestedPlaybackRestore: Boolean = false

    // close 只允许执行一次，避免重复释放 native 观察器。
    private var isClosed: Boolean = false

    /** 首次 UI 接入时请求快照恢复；重复调用不会打断正在播放的会话。 */
    fun ensurePlaybackSnapshotRestoreRequested() {
        val shouldRequestRestore: Boolean = withStateLock {
            if (hasRequestedPlaybackRestore || isClosed) {
                return@withStateLock false
            }
            hasRequestedPlaybackRestore = true
            true
        }
        if (!shouldRequestRestore) {
            return
        }
        sessionScope.launch {
            restorePlaybackSnapshot()
        }
    }

    /** 关闭 iOS 播放会话，按播放器、audio session、协程作用域的顺序收口。 */
    fun close() {
        val shouldClose: Boolean = withStateLock {
            if (isClosed) {
                return@withStateLock false
            }
            isClosed = true
            true
        }
        if (!shouldClose) {
            return
        }
        runBlocking {
            var teardownFailure: Throwable? = null
            try {
                releaseAudioEngine()
            } catch (throwable: Throwable) {
                teardownFailure = throwable
            } finally {
                try {
                    releaseAudioSession()
                } catch (throwable: Throwable) {
                    teardownFailure = teardownFailure?.also { it.addSuppressed(throwable) } ?: throwable
                }
                try {
                    sessionJob.cancelAndJoin()
                } catch (throwable: Throwable) {
                    teardownFailure = teardownFailure?.also { it.addSuppressed(throwable) } ?: throwable
                }
            }
            teardownFailure?.let { throwable ->
                throw throwable
            }
        }
    }

    // 在 iOS native 侧用 Foundation 锁保护极小的一次性状态读写。
    private inline fun <T> withStateLock(block: () -> T): T {
        stateLock.lock()
        return try {
            block()
        } finally {
            stateLock.unlock()
        }
    }
}
