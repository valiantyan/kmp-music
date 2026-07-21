package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 首页歌曲卡片占位封面图标类型，避免 active 态继续误用库图标。
 */
internal enum class DesktopHomeArtworkIconStyle {
    LibraryMusic,
    FigmaMusicNote,
}

/**
 * 首页歌曲卡片视觉规格，把 Figma active 态关键参数集中到可测试模型。
 *
 * @property cardColor 卡片背景色。
 * @property cardBorderColor 卡片边框色。
 * @property cardShadowElevation 卡片本体阴影；Figma active 态不应有灰色外阴影。
 * @property artworkColor 占位封面背景色。
 * @property artworkShadowElevation 占位封面阴影。
 * @property artworkIconStyle 占位封面图标类型。
 * @property artworkIconWidth 占位封面图标宽度。
 * @property artworkIconHeight 占位封面图标高度。
 * @property actionLeadingSpacerWidth 收藏按钮前的固定空位宽度。
 */
internal data class DesktopHomeSongCardVisualSpec(
    val cardColor: Color,
    val cardBorderColor: Color,
    val cardShadowElevation: Dp,
    val artworkColor: Color,
    val artworkShadowElevation: Dp,
    val artworkIconStyle: DesktopHomeArtworkIconStyle,
    val artworkIconWidth: Dp,
    val artworkIconHeight: Dp,
    val actionLeadingSpacerWidth: Dp,
)

/** 解析首页歌曲卡片视觉参数，active 分支必须对齐 Figma `1080:498`。 */
internal fun resolveDesktopHomeSongCardVisualSpec(
    isCurrentSong: Boolean,
    isEvenRow: Boolean,
): DesktopHomeSongCardVisualSpec {
    if (isCurrentSong) {
        return DesktopHomeSongCardVisualSpec(
            cardColor = Color(0x0D006B5C),
            cardBorderColor = Color(0x33006B5C),
            cardShadowElevation = 0.dp,
            artworkColor = Color(0x33006B5C),
            artworkShadowElevation = 2.dp,
            artworkIconStyle = DesktopHomeArtworkIconStyle.FigmaMusicNote,
            artworkIconWidth = 15.dp,
            artworkIconHeight = 22.5.dp,
            actionLeadingSpacerWidth = 28.dp,
        )
    }
    return DesktopHomeSongCardVisualSpec(
        cardColor = Color.White,
        cardBorderColor = Color(0x1ABBCAC4),
        cardShadowElevation = 0.dp,
        artworkColor = if (isEvenRow) Color(0x3300BFA5) else Color(0x4DD0E1FB),
        artworkShadowElevation = 1.dp,
        artworkIconStyle = DesktopHomeArtworkIconStyle.FigmaMusicNote,
        artworkIconWidth = 15.dp,
        artworkIconHeight = 22.5.dp,
        actionLeadingSpacerWidth = 0.dp,
    )
}
