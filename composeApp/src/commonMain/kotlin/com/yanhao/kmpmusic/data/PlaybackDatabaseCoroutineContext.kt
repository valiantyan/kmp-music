package com.yanhao.kmpmusic.data

import kotlin.coroutines.CoroutineContext

/**
 * 提供 Room 查询协程上下文，避免 common 层直接引用非全平台可用的 dispatcher。
 */
internal expect fun providePlaybackDatabaseQueryCoroutineContext(): CoroutineContext
