package com.yanhao.kmpmusic.data

/**
 * 提供跨平台的当前 Unix 毫秒时间，避免共享层依赖平台专属时钟 API。
 */
expect fun currentTimeMillis(): Long
