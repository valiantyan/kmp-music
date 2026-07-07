package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.LocalMusicScanState

/**
 * 本地音乐扫描摘要展示模型，只承载用户需要看到的稳定状态。
 *
 * @property playableSongTotalText 当前可播放歌曲总数文案。
 * @property lastScanTimeText 最近扫描时间文案。
 */
internal data class LocalMusicScanSummaryDisplayModel(
    val playableSongTotalText: String,
    val lastScanTimeText: String,
) {
    /** 页头副标题复用同一展示模型，避免重新拼接扫描增删改计数。 */
    val headerSubtitle: String = "$playableSongTotalText · $lastScanTimeText"
}

/** 根据曲库总数和扫描状态生成本地音乐入口摘要，隐藏内部增删改统计。 */
internal fun buildLocalMusicScanSummaryDisplayModel(
    playableSongCount: Int,
    scanState: LocalMusicScanState,
): LocalMusicScanSummaryDisplayModel {
    val completedAt: Long? = findLastScanCompletedAt(scanState = scanState)
    return LocalMusicScanSummaryDisplayModel(
        playableSongTotalText = "$playableSongCount 首可播放歌曲",
        lastScanTimeText = completedAt?.let { timestampMillis: Long ->
            "最近扫描：${formatLocalMusicScanDate(timestampMillis = timestampMillis)}"
        } ?: "尚未记录扫描时间",
    )
}

// 失败或取消也代表一次明确扫描结果，页面仍可展示它的结果时间。
private fun findLastScanCompletedAt(scanState: LocalMusicScanState): Long? {
    return when (scanState) {
        LocalMusicScanState.Idle,
        LocalMusicScanState.WaitingForPermission,
        -> null
        is LocalMusicScanState.Importing -> scanState.previousSummary?.completedAt
        is LocalMusicScanState.Scanning -> scanState.previousSummary?.completedAt
        is LocalMusicScanState.Done -> scanState.summary.completedAt
        is LocalMusicScanState.Cancelled -> scanState.summary.completedAt
        is LocalMusicScanState.Error -> scanState.summary?.completedAt
    }
}
