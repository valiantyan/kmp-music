package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.PlaybackStatus

/**
 * 首页歌曲行的状态样式，集中约束 normal 与 active 的视觉差异。
 */
internal data class HomeSongRowStyle(
    val containerColor: Color,
    val border: BorderStroke?,
    val shadowElevation: Dp,
    val textColor: Color,
    val showsCoverPlaybackBadge: Boolean,
)

// 当前歌曲行保留选中背景；只有真实播放中才把文字切到播放红色。
internal fun resolveHomeSongRowStyle(
    isCurrentSong: Boolean,
    currentPlaybackStatus: PlaybackStatus,
): HomeSongRowStyle {
    val border: BorderStroke? = if (isCurrentSong) {
        BorderStroke(
            width = 1.dp,
            color = homeActiveBorderColor,
        )
    } else {
        null
    }
    val containerColor: Color = if (isCurrentSong) homeActiveRowColor else Color.White
    val shadowElevation: Dp = 0.dp
    val textColor: Color = if (isCurrentSong && currentPlaybackStatus == PlaybackStatus.Playing) {
        MusicColors.PlayingRed
    } else {
        homeAccentColor
    }
    return HomeSongRowStyle(
        containerColor = containerColor,
        border = border,
        shadowElevation = shadowElevation,
        textColor = textColor,
        showsCoverPlaybackBadge = false,
    )
}
