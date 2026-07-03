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
            scrollOffset = 400.dp,
            pullOffset = 0.dp,
        )

        assertTrue(actual = halfCollapsed.toolbarAlpha > expanded.toolbarAlpha)
        assertTrue(actual = collapsed.toolbarAlpha > halfCollapsed.toolbarAlpha)
        assertEquals(expected = 1f, actual = collapsed.toolbarAlpha)
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

    /**
     * 列表普通滚动不应改变 LazyColumn 布局尺寸，避免快速滑动时重组整页内容。
     */
    @Test
    fun layoutStateMatchesScrollStateWithoutListScrollOffset(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec(maxPullStretchHeight = 96.dp)
        val layoutState: ArtistDetailLayoutState = calculateArtistDetailLayoutState(
            spec = spec,
            pullOffset = 120.dp,
        )
        val scrollState: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 240.dp,
            pullOffset = 120.dp,
        )

        assertEquals(expected = scrollState.collapsedToolbarHeight, actual = layoutState.collapsedToolbarHeight)
        assertEquals(expected = scrollState.pullStretchHeight, actual = layoutState.pullStretchHeight)
        assertEquals(expected = scrollState.heroImageHeight, actual = layoutState.heroImageHeight)
        assertEquals(expected = scrollState.contentTopBarrier, actual = layoutState.contentTopBarrier)
        assertEquals(expected = scrollState.contentGroupSpacerHeight, actual = layoutState.contentGroupSpacerHeight)
    }

    /**
     * 展开头图高度由视口驱动，保证首屏视觉约占屏幕一半。
     */
    @Test
    fun expandedHeaderHeightUsesHalfOfViewport(): Unit {
        val spec: ArtistDetailScrollSpec = createArtistDetailScrollSpec(
            statusBarInset = 24.dp,
            viewportHeight = 880.dp,
        )

        assertEquals(expected = 440.dp, actual = spec.expandedHeaderHeight)
    }

    /**
     * 展开态内容组应锚定在头图中下部，而不是从头图底部才开始。
     */
    @Test
    fun contentGroupStartsInsideHeroLowerMiddle(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec(
            expandedHeaderHeight = 440.dp,
            statusBarInset = 24.dp,
            toolbarContentHeight = 56.dp,
        )

        val state: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 0.dp,
            pullOffset = 0.dp,
        )
        val contentTopInHero = state.collapsedToolbarHeight + state.contentGroupSpacerHeight

        assertEquals(expected = 255.2.dp, actual = contentTopInHero)
        assertTrue(actual = contentTopInHero > spec.expandedHeaderHeight * 0.55f)
        assertTrue(actual = contentTopInHero < spec.expandedHeaderHeight * 0.65f)
    }

    /**
     * Toolbar 渐显应跟随内容组锚点慢慢完成，避免第一个 spacer 离屏时突然跳变。
     */
    @Test
    fun toolbarRevealCompletesWhenContentGroupReachesTopBarrier(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec(
            expandedHeaderHeight = 440.dp,
            statusBarInset = 24.dp,
            toolbarContentHeight = 56.dp,
        )
        val expanded: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 0.dp,
            pullOffset = 0.dp,
        )
        val revealed: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 175.2.dp,
            pullOffset = 0.dp,
        )

        assertEquals(expected = 0f, actual = expanded.toolbarAlpha)
        assertEquals(expected = 1f, actual = revealed.toolbarAlpha)
        assertEquals(expected = 1f, actual = revealed.toolbarTitleAlpha)
    }

    /**
     * Toolbar 渐显中途应保持连续进度，让慢滑反馈自然过渡。
     */
    @Test
    fun toolbarRevealProgressFollowsContentGroupSpacer(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec(
            expandedHeaderHeight = 440.dp,
            statusBarInset = 24.dp,
            toolbarContentHeight = 56.dp,
        )

        val state: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 87.6.dp,
            pullOffset = 0.dp,
        )

        assertEquals(expected = 0.5f, actual = state.collapseProgress)
        assertEquals(expected = 0.5f, actual = state.toolbarAlpha)
        assertEquals(expected = 0f, actual = state.toolbarTitleAlpha)
    }

    /**
     * 歌手图应跟随慢滑一起向上移动，而不是固定在背景层等待内容滑过。
     */
    @Test
    fun heroImageMovesUpWithScrollOffset(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec(
            expandedHeaderHeight = 440.dp,
            statusBarInset = 24.dp,
            toolbarContentHeight = 56.dp,
        )

        val state: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 87.6.dp,
            pullOffset = 0.dp,
        )

        assertEquals(expected = (-87.6).dp, actual = state.heroImageOffset)
        assertEquals(expected = spec.expandedHeaderHeight, actual = state.heroImageHeight)
    }

    /**
     * 播放入口锚点成为首个可见项时，滚动距离应连续累加，不能跳到整张头图高度。
     */
    @Test
    fun scrollOffsetAccumulatesHeaderItemsBeforePlayAllAnchor(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec(
            expandedHeaderHeight = 440.dp,
            statusBarInset = 24.dp,
            toolbarContentHeight = 56.dp,
        )

        val scrollOffset: androidx.compose.ui.unit.Dp = calculateArtistDetailScrollOffsetFromListPosition(
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffset = 12.dp,
            scrollSpec = spec,
        )

        assertEquals(expected = 251.2.dp, actual = scrollOffset)
    }

    /**
     * 播放全部标题行成为首个可见项时，歌手图仍应按连续距离上移，避免标题行背后露空。
     */
    @Test
    fun scrollOffsetAccumulatesHeaderItemsBeforePlayAllSectionHeader(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec(
            expandedHeaderHeight = 440.dp,
            statusBarInset = 24.dp,
            toolbarContentHeight = 56.dp,
        )

        val scrollOffset: androidx.compose.ui.unit.Dp = calculateArtistDetailScrollOffsetFromListPosition(
            firstVisibleItemIndex = 3,
            firstVisibleItemScrollOffset = 20.dp,
            scrollSpec = spec,
        )

        assertEquals(expected = 307.2.dp, actual = scrollOffset)
    }

    /**
     * 继续向上滚动时歌手图可以自然离场，但不能靠高度裁切突然消失。
     */
    @Test
    fun heroImageScrollsOutByOffsetInsteadOfHeightCollapse(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec(
            expandedHeaderHeight = 420.dp,
            statusBarInset = 24.dp,
            toolbarContentHeight = 56.dp,
        )

        val state: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 620.dp,
            pullOffset = 0.dp,
        )

        assertEquals(expected = (-420).dp, actual = state.heroImageOffset)
        assertEquals(expected = spec.expandedHeaderHeight, actual = state.heroImageHeight)
    }

    /**
     * 头图可见高度仍按折叠距离收缩，内容组可以叠在头图上形成沉浸层级。
     */
    @Test
    fun headerVisibleHeightCollapsesIndependentlyFromContentGroup(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec(
            expandedHeaderHeight = 420.dp,
            statusBarInset = 24.dp,
            toolbarContentHeight = 56.dp,
        )
        val scrollOffset = 180.dp

        val state: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = scrollOffset,
            pullOffset = 0.dp,
        )

        assertEquals(expected = 240.dp, actual = state.headerVisibleHeight)
        assertTrue(actual = state.contentGroupSpacerHeight < state.listHeaderSpacerHeight)
    }

    /**
     * 慢慢上滑只应让 Toolbar 和 Toolbar 标题渐显，不能把歌手图高度突然裁掉。
     */
    @Test
    fun heroImageHeightStaysExpandedWhileToolbarAppears(): Unit {
        val spec: ArtistDetailScrollSpec = testScrollSpec(
            expandedHeaderHeight = 420.dp,
            statusBarInset = 24.dp,
            toolbarContentHeight = 56.dp,
        )

        val state: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = spec,
            scrollOffset = 420.dp,
            pullOffset = 0.dp,
        )

        assertEquals(expected = 1f, actual = state.toolbarAlpha)
        assertEquals(expected = 1f, actual = state.toolbarTitleAlpha)
        assertEquals(expected = spec.expandedHeaderHeight, actual = state.heroImageHeight)
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
