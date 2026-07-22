package com.yanhao.kmpmusic.feature.desktop.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 桌面长列表共享滚动条测试，所有页面通过同一接口继承显示、拖拽和隐藏规则。
 */
class DesktopAutoHideLazyScrollbarTest {
    /** 长列表具备滚动能力时必须显示滚动条。 */
    @Test
    fun longListShowsScrollbar() {
        assertTrue(
            actual =
                shouldShowDesktopLazyScrollbar(
                    totalItemsCount = 92,
                    visibleItemsCount = 6,
                    canScrollForward = true,
                    canScrollBackward = false,
                ),
        )
    }

    /** 内容完全落在视口内时不应制造滚动条噪音。 */
    @Test
    fun shortListHidesScrollbar() {
        assertFalse(
            actual =
                shouldShowDesktopLazyScrollbar(
                    totalItemsCount = 4,
                    visibleItemsCount = 4,
                    canScrollForward = false,
                    canScrollBackward = false,
                ),
        )
    }

    /** 拖动半个视口应把长列表推进到后半段。 */
    @Test
    fun draggingScrollbarMovesTowardLaterItems() {
        val targetIndex: Int =
            resolveDesktopLazyScrollbarTargetIndex(
                currentFirstVisibleItemIndex = 0,
                totalItemsCount = 92,
                visibleItemsCount = 6,
                viewportHeightPx = 600f,
                accumulatedDragAmountPx = 300f,
            )
        assertEquals(expected = 46, actual = targetIndex)
    }

    /** 连续的小幅拖动不能因逐次取整而全部丢失。 */
    @Test
    fun repeatedSmallDragEventsStillMoveList() {
        var dragState: DesktopLazyScrollbarDragState = startDesktopLazyScrollbarDrag(initialFirstVisibleItemIndex = 0)
        repeat(times = 20) {
            dragState =
                accumulateDesktopLazyScrollbarDrag(
                    state = dragState,
                    dragAmountPx = 3f,
                )
        }
        val targetIndex: Int =
            resolveDesktopLazyScrollbarTargetIndex(
                currentFirstVisibleItemIndex = dragState.initialFirstVisibleItemIndex,
                totalItemsCount = 92,
                visibleItemsCount = 6,
                viewportHeightPx = 600f,
                accumulatedDragAmountPx = dragState.accumulatedDragAmountPx,
            )
        assertEquals(expected = 9, actual = targetIndex)
    }

    /** 自动隐藏延迟必须与五秒验收契约一致。 */
    @Test
    fun autoHideDelayMatchesAcceptanceContract() {
        assertEquals(expected = 5_000L, actual = DESKTOP_AUTO_HIDE_LAZY_SCROLLBAR_DELAY_MILLIS)
    }
}
