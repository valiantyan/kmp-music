package com.yanhao.kmpmusic.feature.screen

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 歌手详情页折叠滚动模型测试，锁住状态栏接管和下拉放大的边界。
 */
class ArtistDetailScrollBehaviorTest {
    /**
     * 折叠进度必须限制在 0..1，避免列表快速滚动时 Toolbar 透明度溢出。
     */
    @Test
    fun collapseProgressIsClamped(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec()

        val beforeTop: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = (-24).dp,
            pullOffset = 0.dp,
        )
        val afterCollapse: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 800.dp,
            pullOffset = 0.dp,
        )

        assertEquals(expected = 0f, actual = beforeTop.collapseProgress)
        assertEquals(expected = 1f, actual = afterCollapse.collapseProgress)
    }

    /**
     * Toolbar 背景随折叠进度变强，标题在接近完全折叠时才出现。
     */
    @Test
    fun toolbarAlphaAndTitleAlphaFollowCollapseProgress(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec()
        val expanded: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 0.dp,
            pullOffset = 0.dp,
        )
        val halfCollapsed: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 150.dp,
            pullOffset = 0.dp,
        )
        val collapsed: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 300.dp,
            pullOffset = 0.dp,
        )

        assertTrue(actual = halfCollapsed.toolbarAlpha > expanded.toolbarAlpha)
        assertTrue(actual = collapsed.toolbarAlpha > halfCollapsed.toolbarAlpha)
        assertEquals(expected = 0f, actual = expanded.toolbarTitleAlpha)
        assertTrue(actual = collapsed.toolbarTitleAlpha > halfCollapsed.toolbarTitleAlpha)
    }

    /**
     * 折叠 Toolbar 高度必须包含状态栏高度，正文顶部屏障也必须落在 Toolbar 下方。
     */
    @Test
    fun collapsedToolbarHeightIncludesStatusBarInset(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec(
            statusBarInset = 28.dp,
            toolbarContentHeight = 56.dp,
        )

        val state: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 0.dp,
            pullOffset = 0.dp,
        )

        assertEquals(expected = 84.dp, actual = state.collapsedToolbarHeight)
        assertEquals(expected = state.collapsedToolbarHeight, actual = state.contentTopBarrier)
        assertTrue(actual = state.contentTopBarrier > spec.statusBarInset)
    }

    /**
     * 顶部下拉放大必须有最大值，防止头图被无限拉伸。
     */
    @Test
    fun pullStretchHeightIsLimited(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec(maxPullStretchHeight = 96.dp)

        val state: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 0.dp,
            pullOffset = 180.dp,
        )

        assertEquals(expected = 96.dp, actual = state.pullStretchHeight)
        assertEquals(expected = spec.expandedHeaderHeight + 96.dp, actual = state.expandedHeaderHeight)
    }
}

// 构造折叠滚动测试使用的默认尺寸。
private fun testScrollSpec(
    expandedHeaderHeight: androidx.compose.ui.unit.Dp = 420.dp,
    toolbarContentHeight: androidx.compose.ui.unit.Dp = 56.dp,
    statusBarInset: androidx.compose.ui.unit.Dp = 24.dp,
    maxPullStretchHeight: androidx.compose.ui.unit.Dp = 96.dp,
): ArtistDetailScrollSpec {
    return ArtistDetailScrollSpec(
        expandedHeaderHeight = expandedHeaderHeight,
        toolbarContentHeight = toolbarContentHeight,
        statusBarInset = statusBarInset,
        maxPullStretchHeight = maxPullStretchHeight,
    )
}
