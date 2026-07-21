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
    fun calculatePlayerDragOffsetAccumulatesDownwardDrag() {
        val offsetPx: Float =
            calculatePlayerDragOffset(
                currentOffsetPx = 120f,
                dragDeltaPx = 80f,
            )
        assertEquals(
            expected = 200f,
            actual = offsetPx,
        )
    }

    @Test
    fun calculatePlayerDragOffsetClampsUpwardDragAtZero() {
        val offsetPx: Float =
            calculatePlayerDragOffset(
                currentOffsetPx = 32f,
                dragDeltaPx = -80f,
            )
        assertEquals(
            expected = 0f,
            actual = offsetPx,
        )
    }

    @Test
    fun isPlayerOverlayDismissDragRequiresAtLeastHalfScreen() {
        assertFalse(
            actual =
                isPlayerOverlayDismissDrag(
                    dragOffsetPx = 499f,
                    screenHeightPx = 1_000f,
                ),
        )
        assertTrue(
            actual =
                isPlayerOverlayDismissDrag(
                    dragOffsetPx = 500f,
                    screenHeightPx = 1_000f,
                ),
        )
        assertTrue(
            actual =
                isPlayerOverlayDismissDrag(
                    dragOffsetPx = 640f,
                    screenHeightPx = 1_000f,
                ),
        )
    }

    @Test
    fun isPlayerOverlayDismissDragRejectsInvalidHeight() {
        assertFalse(
            actual =
                isPlayerOverlayDismissDrag(
                    dragOffsetPx = 500f,
                    screenHeightPx = 0f,
                ),
        )
    }

    @Test
    fun calculatePlayerDismissTargetOffsetContinuesToFullScreenHeight() {
        val targetOffsetPx: Float =
            calculatePlayerDismissTargetOffset(
                dragOffsetPx = 640f,
                screenHeightPx = 1_000f,
            )
        assertEquals(
            expected = 1_000f,
            actual = targetOffsetPx,
        )
    }

    @Test
    fun calculatePlayerDismissTargetOffsetKeepsOvershootOffscreen() {
        val targetOffsetPx: Float =
            calculatePlayerDismissTargetOffset(
                dragOffsetPx = 1_120f,
                screenHeightPx = 1_000f,
            )
        assertEquals(
            expected = 1_120f,
            actual = targetOffsetPx,
        )
    }

    @Test
    fun calculatePlayerDismissDistanceIncludesNavigationBarArea() {
        val dismissDistancePx: Float =
            calculatePlayerDismissDistance(
                contentHeightPx = 1_000f,
                navigationBarBottomPx = 96f,
            )
        assertEquals(
            expected = 1_096f,
            actual = dismissDistancePx,
        )
    }

    @Test
    fun calculatePlayerDismissDistanceIgnoresInvalidInsets() {
        assertEquals(
            expected = 1_000f,
            actual =
                calculatePlayerDismissDistance(
                    contentHeightPx = 1_000f,
                    navigationBarBottomPx = -24f,
                ),
        )
        assertEquals(
            expected = 0f,
            actual =
                calculatePlayerDismissDistance(
                    contentHeightPx = 0f,
                    navigationBarBottomPx = 96f,
                ),
        )
    }

    @Test
    fun calculatePlayerDismissSettleMillisUsesRemainingDistance() {
        val durationMillis: Int =
            calculatePlayerDismissSettleMillis(
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
    fun calculatePlayerDismissSettleMillisSkipsWhenAlreadyOffscreen() {
        val durationMillis: Int =
            calculatePlayerDismissSettleMillis(
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
    fun calculatePlayerOverlayExitOffsetYIncludesNavigationBarArea() {
        val offsetY: Int =
            calculatePlayerOverlayExitOffsetY(
                fullHeight = 1_000,
                navigationBarBottomPx = 96,
            )
        assertEquals(
            expected = 1_096,
            actual = offsetY,
        )
    }

    @Test
    fun calculatePlayerOverlayExitOffsetYIgnoresInvalidInset() {
        val offsetY: Int =
            calculatePlayerOverlayExitOffsetY(
                fullHeight = 1_000,
                navigationBarBottomPx = -96,
            )
        assertEquals(
            expected = 1_000,
            actual = offsetY,
        )
    }

    @Test
    fun shouldSkipPlayerExitTransitionAfterDragOnlyForDraggedPlayerExit() {
        assertTrue(
            actual =
                shouldSkipPlayerExitTransitionAfterDrag(
                    initialScreen = SecondaryScreen.Player,
                    wasDismissedByDrag = true,
                ),
        )
        assertFalse(
            actual =
                shouldSkipPlayerExitTransitionAfterDrag(
                    initialScreen = SecondaryScreen.Player,
                    wasDismissedByDrag = false,
                ),
        )
        assertFalse(
            actual =
                shouldSkipPlayerExitTransitionAfterDrag(
                    initialScreen = SecondaryScreen.About,
                    wasDismissedByDrag = true,
                ),
        )
    }

    @Test
    fun shouldHidePlayerOverlayContentAfterDragOnlyForOutgoingPlayer() {
        assertTrue(
            actual =
                shouldHidePlayerOverlayContentAfterDrag(
                    overlayScreen = SecondaryScreen.Player,
                    targetOverlayScreen = null,
                    wasDismissedByDrag = true,
                ),
        )
        assertTrue(
            actual =
                shouldHidePlayerOverlayContentAfterDrag(
                    overlayScreen = SecondaryScreen.Player,
                    targetOverlayScreen = SecondaryScreen.LocalMusic(),
                    wasDismissedByDrag = true,
                ),
        )
        assertFalse(
            actual =
                shouldHidePlayerOverlayContentAfterDrag(
                    overlayScreen = SecondaryScreen.Player,
                    targetOverlayScreen = SecondaryScreen.Player,
                    wasDismissedByDrag = true,
                ),
        )
        assertFalse(
            actual =
                shouldHidePlayerOverlayContentAfterDrag(
                    overlayScreen = SecondaryScreen.About,
                    targetOverlayScreen = null,
                    wasDismissedByDrag = true,
                ),
        )
        assertFalse(
            actual =
                shouldHidePlayerOverlayContentAfterDrag(
                    overlayScreen = SecondaryScreen.Player,
                    targetOverlayScreen = null,
                    wasDismissedByDrag = false,
                ),
        )
    }

    @Test
    fun resolveOverlaySaveableStateKeyUsesTargetKeyForCurrentOverlay() {
        val stateKey: String =
            resolveOverlaySaveableStateKey(
                overlayScreen = SecondaryScreen.Player,
                targetOverlayScreen = SecondaryScreen.Player,
                targetScrollStateKey = "secondary:Player:2",
                retainedOverlayScrollStateKey = "secondary:Player:1",
            )
        assertEquals(
            expected = "secondary:Player:2",
            actual = stateKey,
        )
    }

    @Test
    fun resolveOverlaySaveableStateKeyKeepsRetainedKeyForOutgoingOverlay() {
        val stateKey: String =
            resolveOverlaySaveableStateKey(
                overlayScreen = SecondaryScreen.Player,
                targetOverlayScreen = null,
                targetScrollStateKey = "root:Home",
                retainedOverlayScrollStateKey = "secondary:Player:2",
            )
        assertEquals(
            expected = "secondary:Player:2",
            actual = stateKey,
        )
    }

    @Test
    fun resolveOverlaySaveableStateKeyFallsBackWhenRetainedKeyIsMissing() {
        val stateKey: String =
            resolveOverlaySaveableStateKey(
                overlayScreen = SecondaryScreen.Player,
                targetOverlayScreen = null,
                targetScrollStateKey = "root:Home",
                retainedOverlayScrollStateKey = null,
            )
        assertEquals(
            expected = "root:Home",
            actual = stateKey,
        )
    }

    /**
     * 自行管理 Toolbar 和正文滚动的覆盖页应直接渲染，避免被外层滚动容器二次包裹。
     */
    @Test
    fun shouldRenderOverlayScreenDirectlyForSelfManagedPages() {
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
