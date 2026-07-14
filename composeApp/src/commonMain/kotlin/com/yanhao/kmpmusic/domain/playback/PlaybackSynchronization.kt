package com.yanhao.kmpmusic.domain.playback

/**
 * 提供跨平台同步块，避免 common 播放代码直接调用 JVM-only 的 [synchronized]。
 */
internal expect fun <T> runSynchronizedBlock(
    lock: Any,
    block: () -> T,
): T
