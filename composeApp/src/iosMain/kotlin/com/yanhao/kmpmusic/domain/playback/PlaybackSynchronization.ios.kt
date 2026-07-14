package com.yanhao.kmpmusic.domain.playback

import platform.Foundation.NSRecursiveLock

private val playbackSynchronizationLock: NSRecursiveLock = NSRecursiveLock()

/**
 * iOS 端用 Foundation 锁提供无 JVM 依赖的同步边界。
 */
internal actual fun <T> runSynchronizedBlock(
    lock: Any,
    block: () -> T,
): T {
    playbackSynchronizationLock.lock()
    try {
        return block()
    } finally {
        playbackSynchronizationLock.unlock()
    }
}
