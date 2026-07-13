package com.yanhao.kmpmusic.feature.app.layout

import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 播放页手势阈值测试，锁定“底层 App 露出至少半屏才关闭”的交互规则。
 */
class MobilePlayerOverlayGestureTest {
    @Test
    fun calculatePlayerDragOffsetAccumulatesDownwardDrag(): Unit {
        val offsetPx: Float = calculatePlayerDragOffset(
            currentOffsetPx = 120f,
            dragDeltaPx = 80f,
        )
        assertEquals(
            expected = 200f,
            actual = offsetPx,
        )
    }

    @Test
    fun calculatePlayerDragOffsetClampsUpwardDragAtZero(): Unit {
        val offsetPx: Float = calculatePlayerDragOffset(
            currentOffsetPx = 32f,
            dragDeltaPx = -80f,
        )
        assertEquals(
            expected = 0f,
            actual = offsetPx,
        )
    }

    @Test
    fun isPlayerOverlayDismissDragRequiresAtLeastHalfScreen(): Unit {
        assertFalse(
            actual = isPlayerOverlayDismissDrag(
                dragOffsetPx = 499f,
                screenHeightPx = 1_000f,
            ),
        )
        assertTrue(
            actual = isPlayerOverlayDismissDrag(
                dragOffsetPx = 500f,
                screenHeightPx = 1_000f,
            ),
        )
        assertTrue(
            actual = isPlayerOverlayDismissDrag(
                dragOffsetPx = 640f,
                screenHeightPx = 1_000f,
            ),
        )
    }

    @Test
    fun isPlayerOverlayDismissDragRejectsInvalidHeight(): Unit {
        assertFalse(
            actual = isPlayerOverlayDismissDrag(
                dragOffsetPx = 500f,
                screenHeightPx = 0f,
            ),
        )
    }

    @Test
    fun calculatePlayerDismissTargetOffsetContinuesToFullScreenHeight(): Unit {
        val targetOffsetPx: Float = calculatePlayerDismissTargetOffset(
            dragOffsetPx = 640f,
            screenHeightPx = 1_000f,
        )
        assertEquals(
            expected = 1_000f,
            actual = targetOffsetPx,
        )
    }

    @Test
    fun calculatePlayerDismissTargetOffsetKeepsOvershootOffscreen(): Unit {
        val targetOffsetPx: Float = calculatePlayerDismissTargetOffset(
            dragOffsetPx = 1_120f,
            screenHeightPx = 1_000f,
        )
        assertEquals(
            expected = 1_120f,
            actual = targetOffsetPx,
        )
    }

    @Test
    fun calculatePlayerDismissSettleMillisUsesRemainingDistance(): Unit {
        val durationMillis: Int = calculatePlayerDismissSettleMillis(
            currentOffsetPx = 500f,
            targetOffsetPx = 1_000f,
            screenHeightPx = 1_000f,
        )
        assertEquals(
            expected = 160,
            actual = durationMillis,
        )
    }

    @Test
    fun calculatePlayerDismissSettleMillisSkipsWhenAlreadyOffscreen(): Unit {
        val durationMillis: Int = calculatePlayerDismissSettleMillis(
            currentOffsetPx = 1_120f,
            targetOffsetPx = 1_120f,
            screenHeightPx = 1_000f,
        )
        assertEquals(
            expected = 0,
            actual = durationMillis,
        )
    }

    @Test
    fun shouldSkipPlayerExitTransitionAfterDragOnlyForDraggedPlayerExit(): Unit {
        assertTrue(
            actual = shouldSkipPlayerExitTransitionAfterDrag(
                initialScreen = SecondaryScreen.Player,
                wasDismissedByDrag = true,
            ),
        )
        assertFalse(
            actual = shouldSkipPlayerExitTransitionAfterDrag(
                initialScreen = SecondaryScreen.Player,
                wasDismissedByDrag = false,
            ),
        )
        assertFalse(
            actual = shouldSkipPlayerExitTransitionAfterDrag(
                initialScreen = SecondaryScreen.About,
                wasDismissedByDrag = true,
            ),
        )
    }

    @Test
    fun shouldHidePlayerOverlayContentAfterDragOnlyForOutgoingPlayer(): Unit {
        assertTrue(
            actual = shouldHidePlayerOverlayContentAfterDrag(
                overlayScreen = SecondaryScreen.Player,
                targetOverlayScreen = null,
                wasDismissedByDrag = true,
            ),
        )
        assertTrue(
            actual = shouldHidePlayerOverlayContentAfterDrag(
                overlayScreen = SecondaryScreen.Player,
                targetOverlayScreen = SecondaryScreen.LocalMusic(),
                wasDismissedByDrag = true,
            ),
        )
        assertFalse(
            actual = shouldHidePlayerOverlayContentAfterDrag(
                overlayScreen = SecondaryScreen.Player,
                targetOverlayScreen = SecondaryScreen.Player,
                wasDismissedByDrag = true,
            ),
        )
        assertFalse(
            actual = shouldHidePlayerOverlayContentAfterDrag(
                overlayScreen = SecondaryScreen.About,
                targetOverlayScreen = null,
                wasDismissedByDrag = true,
            ),
        )
        assertFalse(
            actual = shouldHidePlayerOverlayContentAfterDrag(
                overlayScreen = SecondaryScreen.Player,
                targetOverlayScreen = null,
                wasDismissedByDrag = false,
            ),
        )
    }

    /**
     * 自行管理 Toolbar 和正文滚动的覆盖页应直接渲染，避免被外层滚动容器二次包裹。
     */
    @Test
    fun shouldRenderOverlayScreenDirectlyForSelfManagedPages(): Unit {
        assertTrue(
            actual = shouldRenderOverlayScreenDirectly(overlayScreen = SecondaryScreen.About),
        )
        assertTrue(
            actual = shouldRenderOverlayScreenDirectly(overlayScreen = SecondaryScreen.AudioScan),
        )
        assertTrue(
            actual = shouldRenderOverlayScreenDirectly(overlayScreen = SecondaryScreen.LocalPlaylistManagement),
        )
        assertFalse(
            actual = shouldRenderOverlayScreenDirectly(overlayScreen = SecondaryScreen.Player),
        )
    }
}
