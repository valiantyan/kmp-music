package com.yanhao.kmpmusic.feature.app.playerbar

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 手机端固定底栏布局指标测试，保护 iOS Home Indicator 安全区和 Android 既有布局差异。
 */
class MobileFixedPlayerBarLayoutMetricsTest {
    /** Android 保持旧路径：底部安全区仍在 Tab 外单独绘制，避免改坏现有正常表现。 */
    @Test
    fun androidLayoutKeepsNavigationBarOutsideBottomTab() {
        val metrics: MobileFixedPlayerBarLayoutMetrics =
            buildMobileFixedPlayerBarLayoutMetrics(
                hasSong = true,
                miniPlayerHeight = 68.dp,
                bottomNavigationHeight = 78.dp,
                navigationBarHeight = 34.dp,
                integratesBottomNavigationInset = false,
            )

        assertEquals(expected = 146.dp, actual = metrics.stackHeight)
        assertEquals(expected = 180.dp, actual = metrics.containerHeight)
        assertEquals(expected = 78.dp, actual = metrics.bottomNavigationContentHeight)
        assertEquals(expected = 0.dp, actual = metrics.bottomNavigationInsetHeight)
        assertEquals(expected = 34.dp, actual = metrics.navigationBarUnderlayHeight)
        assertEquals(expected = 78.dp, actual = metrics.miniPlayerOnlyOffset)
    }

    /** iOS 把 Home Indicator 安全区纳入底部 Tab，但不能把 Android 内容高度整体加厚。 */
    @Test
    fun iosLayoutUsesCompactBottomTabWithIntegratedHomeIndicator() {
        val metrics: MobileFixedPlayerBarLayoutMetrics =
            buildMobileFixedPlayerBarLayoutMetrics(
                hasSong = true,
                miniPlayerHeight = 68.dp,
                bottomNavigationHeight = 78.dp,
                navigationBarHeight = 34.dp,
                integratesBottomNavigationInset = true,
            )

        assertEquals(expected = 158.dp, actual = metrics.stackHeight)
        assertEquals(expected = 158.dp, actual = metrics.containerHeight)
        assertEquals(expected = 56.dp, actual = metrics.bottomNavigationContentHeight)
        assertEquals(expected = 34.dp, actual = metrics.bottomNavigationInsetHeight)
        assertEquals(expected = 0.dp, actual = metrics.navigationBarUnderlayHeight)
        assertEquals(expected = 90.dp, actual = metrics.miniPlayerOnlyOffset)
    }

    /** iOS 二级页只显示迷你播放器时，整个底部 Tab 都必须滑出裁剪区域。 */
    @Test
    fun iosMiniPlayerOnlyHidesEntireBottomTab() {
        val metrics: MobileFixedPlayerBarLayoutMetrics =
            buildMobileFixedPlayerBarLayoutMetrics(
                hasSong = true,
                miniPlayerHeight = 68.dp,
                bottomNavigationHeight = 78.dp,
                navigationBarHeight = 34.dp,
                integratesBottomNavigationInset = true,
            )

        assertEquals(expected = 90.dp, actual = metrics.miniPlayerOnlyOffset)
        assertEquals(expected = 158.dp, actual = metrics.hiddenOffset)
    }
}
