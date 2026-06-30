package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult

/**
 * 平台无关本地音乐扫描接口，真实平台实现只能放在对应 source set。
 *
 * Scanner 只回答“当前平台发现了哪些本地歌曲以及它们的可播放 URI”。
 * 它不负责执行播放命令、不维护播放状态，也不处理队列推进；这些职责属于
 * common 播放协调器和平台播放 adapter。
 */
interface LocalMusicScanner {
    /**
     * 执行一次本地音乐扫描，返回可合并到曲库快照的扫描结果。
     */
    suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult
}
