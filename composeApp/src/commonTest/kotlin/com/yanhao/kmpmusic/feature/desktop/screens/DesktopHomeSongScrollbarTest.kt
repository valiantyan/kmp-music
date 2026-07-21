package com.yanhao.kmpmusic.feature.desktop.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 首页歌曲列表滚动条测试，长列表必须提供可见且可拖动的快速滑动入口。
 */
class DesktopHomeSongScrollbarTest {
    @Test
    fun longSongListShowsScrollbar() {
        assertTrue(
            actual =
                shouldShowDesktopHomeSongScrollbar(
                    totalItemsCount = 92,
                    visibleItemsCount = 6,
                    canScrollForward = true,
                    canScrollBackward = false,
                ),
        )
    }

    @Test
    fun shortSongListHidesScrollbar() {
        assertFalse(
            actual =
                shouldShowDesktopHomeSongScrollbar(
                    totalItemsCount = 4,
                    visibleItemsCount = 4,
                    canScrollForward = false,
                    canScrollBackward = false,
                ),
        )
    }

    @Test
    fun draggingScrollbarMovesTowardLaterSongs() {
        val targetIndex: Int =
            resolveDesktopHomeSongScrollbarTargetIndex(
                currentFirstVisibleItemIndex = 0,
                totalItemsCount = 92,
                visibleItemsCount = 6,
                viewportHeightPx = 600f,
                dragAmountPx = 300f,
            )

        assertEquals(expected = 46, actual = targetIndex)
    }

    @Test
    fun scrollingSongListRendersScrollbarImmediately() {
        assertTrue(
            actual =
                shouldRenderDesktopHomeSongScrollbar(
                    hasScrollableContent = true,
                    isScrollInProgress = true,
                    idleDurationMillis = DESKTOP_HOME_SONG_SCROLLBAR_HIDE_DELAY_MILLIS,
                ),
        )
    }

    @Test
    fun idleSongListKeepsScrollbarBeforeFiveSecondDelay() {
        assertTrue(
            actual =
                shouldRenderDesktopHomeSongScrollbar(
                    hasScrollableContent = true,
                    isScrollInProgress = false,
                    idleDurationMillis = DESKTOP_HOME_SONG_SCROLLBAR_HIDE_DELAY_MILLIS - 1L,
                ),
        )
    }

    @Test
    fun idleSongListHidesScrollbarAfterFiveSecondDelay() {
        assertFalse(
            actual =
                shouldRenderDesktopHomeSongScrollbar(
                    hasScrollableContent = true,
                    isScrollInProgress = false,
                    idleDurationMillis = DESKTOP_HOME_SONG_SCROLLBAR_HIDE_DELAY_MILLIS,
                ),
        )
    }
}
