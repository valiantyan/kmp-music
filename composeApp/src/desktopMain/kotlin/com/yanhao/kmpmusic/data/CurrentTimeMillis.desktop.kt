package com.yanhao.kmpmusic.data

/**
 * 复用桌面 JVM 时钟返回 Unix 毫秒时间，供共享层持久化时间戳使用。
 */
actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}
