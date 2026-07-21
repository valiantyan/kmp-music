package com.yanhao.kmpmusic.feature.app.playerbar

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicDimens

/**
 * 手机端固定底栏布局指标，隔离 iOS 安全区与 Android 既有布局差异。
 *
 * @property stackHeight 迷你播放器和底部 Tab 组成的可动画内容高度。
 * @property containerHeight 固定底栏整体占位高度。
 * @property bottomNavigationContentHeight 底部 Tab 图标和文字所在的内容区高度。
 * @property bottomNavigationInsetHeight 底部 Tab 自身吸收的系统底部安全区高度。
 * @property navigationBarUnderlayHeight 底部 Tab 之外额外绘制的系统底部安全区高度。
 * @property miniPlayerOnlyOffset 仅显示迷你播放器时内容栈需要下移的距离。
 * @property hiddenOffset 完全隐藏固定底栏时内容栈需要下移的距离。
 */
internal data class MobileFixedPlayerBarLayoutMetrics(
    val stackHeight: Dp,
    val containerHeight: Dp,
    val bottomNavigationContentHeight: Dp,
    val bottomNavigationInsetHeight: Dp,
    val navigationBarUnderlayHeight: Dp,
    val miniPlayerOnlyOffset: Dp,
    val hiddenOffset: Dp,
)

/**
 * 构造固定底栏布局指标，保证 Android 保持旧的安全区外置路径，iOS 则把安全区归入底部 Tab。
 */
internal fun buildMobileFixedPlayerBarLayoutMetrics(
    hasSong: Boolean,
    miniPlayerHeight: Dp,
    bottomNavigationHeight: Dp,
    navigationBarHeight: Dp,
    integratesBottomNavigationInset: Boolean,
): MobileFixedPlayerBarLayoutMetrics {
    val bottomNavigationInsetHeight: Dp =
        if (integratesBottomNavigationInset) {
            navigationBarHeight
        } else {
            0.dp
        }
    val navigationBarUnderlayHeight: Dp =
        if (integratesBottomNavigationInset) {
            0.dp
        } else {
            navigationBarHeight
        }
    val bottomNavigationContentHeight: Dp =
        if (integratesBottomNavigationInset) {
            (bottomNavigationHeight - navigationBarHeight).coerceAtLeast(
                minimumValue = MusicDimens.BottomNavIntegratedContentMinHeight,
            )
        } else {
            bottomNavigationHeight
        }
    val visibleBottomNavigationHeight: Dp = bottomNavigationContentHeight + bottomNavigationInsetHeight
    val stackHeight: Dp =
        if (hasSong) {
            miniPlayerHeight + visibleBottomNavigationHeight
        } else {
            visibleBottomNavigationHeight
        }
    val containerHeight: Dp = stackHeight + navigationBarUnderlayHeight
    val miniPlayerOnlyOffset: Dp =
        if (hasSong) {
            visibleBottomNavigationHeight
        } else {
            containerHeight
        }
    return MobileFixedPlayerBarLayoutMetrics(
        stackHeight = stackHeight,
        containerHeight = containerHeight,
        bottomNavigationContentHeight = bottomNavigationContentHeight,
        bottomNavigationInsetHeight = bottomNavigationInsetHeight,
        navigationBarUnderlayHeight = navigationBarUnderlayHeight,
        miniPlayerOnlyOffset = miniPlayerOnlyOffset,
        hiddenOffset = containerHeight,
    )
}
