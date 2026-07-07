package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 本地音乐扫描摘要展示模型测试，锁住来源页和扫描入口的用户可见统计范围。
 */
class LocalMusicScanSummaryDisplayModelTest {
    /**
     * 扫描摘要只展示当前可播放总数和最近扫描时间，不泄露内部增删改统计。
     */
    @Test
    fun scanSummaryDisplayModelShowsPlayableTotalAndLastScanTimeOnly(): Unit {
        val model: LocalMusicScanSummaryDisplayModel = buildLocalMusicScanSummaryDisplayModel(
            playableSongCount = 42,
            scanState = LocalMusicScanState.Done(
                summary = LocalMusicLastScanSummary(
                    addedCount = 901,
                    updatedCount = 902,
                    removedCount = 903,
                    problemCount = 7,
                    completedAt = 86_400_000L,
                ),
            ),
        )
        val renderedText: String = model.headerSubtitle
        assertEquals(expected = "42 首可播放歌曲", actual = model.playableSongTotalText)
        assertEquals(expected = "最近扫描：1970-01-02", actual = model.lastScanTimeText)
        assertEquals(expected = "42 首可播放歌曲 · 最近扫描：1970-01-02", actual = model.headerSubtitle)
        assertFalse(actual = renderedText.contains(other = "新增"))
        assertFalse(actual = renderedText.contains(other = "更新"))
        assertFalse(actual = renderedText.contains(other = "移除"))
        assertFalse(actual = renderedText.contains(other = "901"))
        assertFalse(actual = renderedText.contains(other = "902"))
        assertFalse(actual = renderedText.contains(other = "903"))
    }
}
