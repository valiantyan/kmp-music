package com.yanhao.kmpmusic.data

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

/**
 * Android 端 Room 查询继续使用 IO dispatcher，保持现有数据库线程语义。
 */
internal actual fun providePlaybackDatabaseQueryCoroutineContext(): CoroutineContext {
    return Dispatchers.IO
}
