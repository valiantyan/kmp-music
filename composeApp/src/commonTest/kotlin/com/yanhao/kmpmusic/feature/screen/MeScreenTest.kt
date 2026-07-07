package com.yanhao.kmpmusic.feature.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 我的页测试，锁住最近播放摘要当前切片的空态骨架和范围边界。
 */
class MeScreenTest {
    /**
     * 最近播放摘要必须展示固定标题、查看全部占位和轻量空态，不能因为没有歌曲而留白。
     */
    @Test
    fun recentPlayedSummaryDisplayModelShowsEmptySkeleton(): Unit {
        val model: RecentPlayedSummaryDisplayModel = buildRecentPlayedSummaryDisplayModel()

        assertEquals(expected = "最近播放", actual = model.title)
        assertEquals(expected = "查看全部", actual = model.actionLabel)
        assertTrue(actual = model.emptyMessage.contains(other = "最近听过的音乐"))
    }

    /**
     * 当前切片不能提前接入查看全部跳转、真实歌曲列表、播放队列或更多菜单。
     */
    @Test
    fun recentPlayedSummaryDisplayModelKeepsFutureBehaviorDisabled(): Unit {
        val model: RecentPlayedSummaryDisplayModel = buildRecentPlayedSummaryDisplayModel()

        assertFalse(actual = model.isActionEnabled)
        assertFalse(actual = model.emptyMessage.contains(other = "播放队列"))
        assertFalse(actual = model.emptyMessage.contains(other = "更多菜单"))
    }
}
