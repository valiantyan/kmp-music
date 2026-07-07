package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanProgress
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
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

    /**
     * 正在扫描时仍应展示上一轮扫描时间，避免重新扫描期间回退成“尚未记录扫描时间”。
     */
    @Test
    fun scanningSummaryDisplayModelKeepsPreviousLastScanTime(): Unit {
        val model: LocalMusicScanSummaryDisplayModel = buildLocalMusicScanSummaryDisplayModel(
            playableSongCount = 42,
            scanState = LocalMusicScanState.Scanning(
                progress = LocalMusicScanProgress(currentSourceName = "本地音乐"),
                previousSummary = lastScanSummary(),
            ),
        )

        assertEquals(expected = "最近扫描：1970-01-02", actual = model.lastScanTimeText)
    }

    /**
     * 扫描入口动作必须跟随平台来源模型，避免 iOS 和 Desktop 复用 Android 扫描文案。
     */
    @Test
    fun scanActionLabelsUsePlatformSpecificEntryCopy(): Unit {
        assertEquals(
            expected = "开始扫描",
            actual = localMusicScanActionLabel(
                scanState = LocalMusicScanState.Idle,
                platform = LocalMusicDiscoveryPlatform.Android,
            ),
        )
        assertEquals(
            expected = "添加文件夹",
            actual = localMusicScanActionLabel(
                scanState = LocalMusicScanState.Idle,
                platform = LocalMusicDiscoveryPlatform.Desktop,
            ),
        )
        assertEquals(
            expected = "导入音频",
            actual = localMusicScanActionLabel(
                scanState = LocalMusicScanState.Idle,
                platform = LocalMusicDiscoveryPlatform.Ios,
            ),
        )
        assertEquals(
            expected = "重新扫描",
            actual = localMusicScanActionLabel(
                scanState = LocalMusicScanState.Done(summary = lastScanSummary()),
                platform = LocalMusicDiscoveryPlatform.Desktop,
            ),
        )
        assertEquals(
            expected = "扫描曲库",
            actual = localMusicScanActionLabel(
                scanState = LocalMusicScanState.Error(
                    error = LocalMusicScanError(
                        type = LocalMusicScanErrorType.Unknown,
                        message = "未知错误",
                    ),
                ),
                platform = LocalMusicDiscoveryPlatform.Ios,
            ),
        )
    }

    /**
     * 来源标签按平台用户语言展示，不把内部来源类型名泄露到来源页。
     */
    @Test
    fun sourceKindLabelsUsePlatformSpecificSourceCopy(): Unit {
        assertEquals(
            expected = "Android 媒体库",
            actual = localMusicSourceKindLabel(
                sourceKind = LocalMusicSourceKind.AndroidMediaStore,
                platform = LocalMusicDiscoveryPlatform.Android,
            ),
        )
        assertEquals(
            expected = "扫描目录",
            actual = localMusicSourceKindLabel(
                sourceKind = LocalMusicSourceKind.DesktopFolder,
                platform = LocalMusicDiscoveryPlatform.Desktop,
            ),
        )
        assertEquals(
            expected = "已添加音频",
            actual = localMusicSourceKindLabel(
                sourceKind = LocalMusicSourceKind.IosImportedFile,
                platform = LocalMusicDiscoveryPlatform.Ios,
            ),
        )
    }

    // 构造完成态摘要，让平台文案测试只关注动作映射。
    private fun lastScanSummary(): LocalMusicLastScanSummary {
        return LocalMusicLastScanSummary(
            addedCount = 0,
            updatedCount = 0,
            removedCount = 0,
            problemCount = 0,
            completedAt = 86_400_000L,
        )
    }
}
