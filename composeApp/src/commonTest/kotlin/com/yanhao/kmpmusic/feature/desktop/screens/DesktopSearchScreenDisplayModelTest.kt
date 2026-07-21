package com.yanhao.kmpmusic.feature.desktop.screens

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 桌面搜索结果显示规则测试，避免 pending 防抖阶段误报“没有找到”。
 */
class DesktopSearchScreenDisplayModelTest {
    @Test
    fun pendingQueryKeepsDesktopResultsHidden() {
        assertFalse(actual = shouldShowDesktopSearchResults(query = "雨", activeQuery = ""))
        assertFalse(actual = shouldShowDesktopSearchResults(query = "雨声", activeQuery = "雨"))
    }

    @Test
    fun matchingActiveQueryShowsDesktopResults() {
        assertTrue(actual = shouldShowDesktopSearchResults(query = "雨", activeQuery = "雨"))
    }
}
