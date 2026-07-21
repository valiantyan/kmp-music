package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors

// 当前歌曲真实播放时显示等高器动画，填充 Figma 收藏按钮左侧 28dp 固定位。
@Composable
internal fun DesktopHomePlayingIndicator(modifier: Modifier = Modifier) {
    val transition: InfiniteTransition = rememberInfiniteTransition(label = "desktopHomePlayingIndicator")
    val firstBarHeight: Float =
        transition.animateDesktopHomePlayingBarHeight(
            initialHeight = 5f,
            targetHeight = 15f,
            durationMillis = 420,
            label = "desktopHomePlayingIndicatorFirstBar",
        )
    val secondBarHeight: Float =
        transition.animateDesktopHomePlayingBarHeight(
            initialHeight = 14f,
            targetHeight = 6f,
            durationMillis = 520,
            label = "desktopHomePlayingIndicatorSecondBar",
        )
    val thirdBarHeight: Float =
        transition.animateDesktopHomePlayingBarHeight(
            initialHeight = 8f,
            targetHeight = 16f,
            durationMillis = 460,
            label = "desktopHomePlayingIndicatorThirdBar",
        )
    Row(
        modifier = modifier,
        horizontalArrangement =
            Arrangement.spacedBy(
                space = 3.dp,
                alignment = Alignment.CenterHorizontally,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopHomePlayingIndicatorBar(height = firstBarHeight.dp)
        DesktopHomePlayingIndicatorBar(height = secondBarHeight.dp)
        DesktopHomePlayingIndicatorBar(height = thirdBarHeight.dp)
    }
}

// 单根动画柱只负责绘制自身高度，播放节奏由父级统一控制。
@Composable
private fun DesktopHomePlayingIndicatorBar(height: Dp) {
    Box(
        modifier =
            Modifier
                .size(
                    width = 3.dp,
                    height = height,
                ).clip(RoundedCornerShape(2.dp))
                .background(DesktopMusicColors.PlayerRed),
    )
}

// 每根柱子使用不同周期，避免三根柱子同步跳动看起来像静态图标。
@Composable
private fun InfiniteTransition.animateDesktopHomePlayingBarHeight(
    initialHeight: Float,
    targetHeight: Float,
    durationMillis: Int,
    label: String,
): Float {
    val height: Float by animateFloat(
        initialValue = initialHeight,
        targetValue = targetHeight,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = durationMillis),
                repeatMode = RepeatMode.Reverse,
            ),
        label = label,
    )
    return height
}
