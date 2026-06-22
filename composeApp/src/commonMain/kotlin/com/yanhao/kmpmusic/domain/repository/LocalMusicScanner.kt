package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult

/**
 * 平台无关本地音乐扫描接口，真实平台实现只能放在对应 source set。
 */
interface LocalMusicScanner {
    /**
     * 执行一次本地音乐扫描，返回可合并到曲库快照的扫描结果。
     */
    suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult
}
