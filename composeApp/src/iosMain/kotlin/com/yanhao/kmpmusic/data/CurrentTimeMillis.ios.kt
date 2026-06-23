package com.yanhao.kmpmusic.data

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

/**
 * 通过 iOS/POSIX 时钟返回 Unix 毫秒时间，避免共享层依赖 JVM API。
 */
@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeMillis(): Long {
    return time(null) * 1_000L
}
