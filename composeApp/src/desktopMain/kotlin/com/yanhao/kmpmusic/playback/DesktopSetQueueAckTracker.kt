package com.yanhao.kmpmusic.playback

import kotlinx.coroutines.CompletableDeferred

/**
 * 跟踪所有待完成的 [setQueue] 确认，避免 release 或异常路径把调用方永久挂起。
 */
internal class DesktopSetQueueAckTracker {
    // 保护 [pendingAcks] 的互斥锁，避免并发改写确认集合。
    private val lock: Any = Any()

    // 所有尚未完成的 [setQueue] 确认。
    private val pendingAcks: MutableSet<CompletableDeferred<Unit>> = linkedSetOf()

    /** 注册新的 [setQueue] 确认，让 release 收尾可以统一处理。 */
    fun register(ack: CompletableDeferred<Unit>) {
        synchronized(lock = lock) {
            pendingAcks += ack
        }
    }

    /** 完成单个确认，并从挂起集合中摘除，避免重复收尾。 */
    fun complete(ack: CompletableDeferred<Unit>) {
        val shouldComplete: Boolean =
            synchronized(lock = lock) {
                pendingAcks.remove(element = ack)
            }
        if (shouldComplete) {
            ack.complete(value = Unit)
        }
    }

    /** 统一完成所有遗留确认，确保命令循环退出时没有调用方失联。 */
    fun completeAll() {
        val snapshot: List<CompletableDeferred<Unit>> =
            synchronized(lock = lock) {
                val acks: List<CompletableDeferred<Unit>> = pendingAcks.toList()
                pendingAcks.clear()
                acks
            }
        snapshot.forEach { ack: CompletableDeferred<Unit> ->
            ack.complete(value = Unit)
        }
    }
}
