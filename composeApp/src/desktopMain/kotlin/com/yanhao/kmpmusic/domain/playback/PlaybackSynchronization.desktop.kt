package com.yanhao.kmpmusic.domain.playback

/**
 * Desktop 端沿用 JVM 同步块，保持快照 pending 集合的并发保护。
 */
internal actual fun <T> runSynchronizedBlock(
    lock: Any,
    block: () -> T,
): T = kotlin.synchronized(lock = lock, block = block)
