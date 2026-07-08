package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors

/**
 * 首页歌曲行的状态样式，集中约束收藏页同款卡片和当前歌曲标识。
 */
internal data class HomeSongRowStyle(
    val containerColor: Color,
    val border: BorderStroke?,
    val shadowElevation: Dp,
    val textColor: Color,
    val metaColor: Color,
    val showsCoverPlaybackBadge: Boolean,
)

// 首页歌曲行跟收藏页卡片统一，只用红字和封面标识表达当前歌曲。
internal fun resolveHomeSongRowStyle(
    isCurrentSong: Boolean,
): HomeSongRowStyle {
    val textColor: Color = if (isCurrentSong) MusicColors.PlayingRed else favoritesTextColor
    val metaColor: Color = if (isCurrentSong) MusicColors.PlayingRed else favoritesMetaColor
    return HomeSongRowStyle(
        containerColor = Color.White,
        border = null,
        shadowElevation = 2.dp,
        textColor = textColor,
        metaColor = metaColor,
        showsCoverPlaybackBadge = isCurrentSong,
    )
}
