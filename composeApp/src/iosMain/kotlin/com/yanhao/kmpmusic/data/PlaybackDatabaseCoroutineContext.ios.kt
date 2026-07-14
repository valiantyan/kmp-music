package com.yanhao.kmpmusic.data

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

/**
 * iOS 端没有公开 IO dispatcher，使用 Default 保证 Room 查询上下文可编译运行。
 */
internal actual fun providePlaybackDatabaseQueryCoroutineContext(): CoroutineContext {
    return Dispatchers.Default
}
