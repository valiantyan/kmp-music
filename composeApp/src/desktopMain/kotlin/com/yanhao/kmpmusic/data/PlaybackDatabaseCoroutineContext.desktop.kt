package com.yanhao.kmpmusic.data

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Desktop 端 Room 查询继续使用 IO dispatcher，保持现有数据库线程语义。
 */
internal actual fun providePlaybackDatabaseQueryCoroutineContext(): CoroutineContext = Dispatchers.IO
