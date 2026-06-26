package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Desktop UI colors copied from prototypes/kmp-music-desktop-uiux/index.html.
 */
object DesktopMusicColors {
    val Accent: Color = Color(0xFF17B59E)
    val AccentDeep: Color = Color(0xFF0FA890)
    val AccentSoft: Color = Color(0xFFE8F8F5)
    val Ink: Color = Color(0xFF07090C)
    val Muted: Color = Color(0xFF7F8A99)
    val MutedStrong: Color = Color(0xFF64707E)
    val Line: Color = Color(0xFFE7ECF0)
    val Paper: Color = Color(0xFFFBFCFD)
    val Soft: Color = Color(0xFFF4F7F8)
    val PlayerRed: Color = Color(0xFFEF3F42)
    val WindowBackground: Color = Color(0xFFEEF2F5)
}

/**
 * Desktop UI dimensions copied from the HTML prototype.
 */
object DesktopMusicDimens {
    val MinWindowWidth: Dp = 1120.dp
    val MinWindowHeight: Dp = 760.dp
    val DefaultWindowWidth: Dp = 1240.dp
    val DefaultWindowHeight: Dp = 820.dp
    val TitleBarHeight: Dp = 42.dp
    val RailWidth: Dp = 88.dp
    val PlayerHeight: Dp = 96.dp
    val PagePaddingTop: Dp = 34.dp
    val PagePaddingBottom: Dp = 30.dp
    val PagePaddingMinHorizontal: Dp = 34.dp
    val PagePaddingMaxHorizontal: Dp = 68.dp
    val RailItemSize: Dp = 64.dp
    val BrandSize: Dp = 40.dp
    val PrimaryButtonHeight: Dp = 40.dp
    val StatCardMinHeight: Dp = 76.dp
    val TableHeaderHeight: Dp = 40.dp
    val TableRowHeight: Dp = 48.dp
    val TableCoverSize: Dp = 34.dp
    val AlbumMinWidth: Dp = 120.dp
    val SettingNavWidth: Dp = 210.dp
    val PlayerTrackColumnWidth: Dp = 310.dp
    val PlayerActionsColumnWidth: Dp = 330.dp
}

object DesktopMusicType {
    val AppTitle: TextUnit = 13.sp
    val PageTitle: TextUnit = 36.sp
    val Eyebrow: TextUnit = 14.sp
    val Body: TextUnit = 13.sp
    val StatTitle: TextUnit = 15.sp
    val RailLabel: TextUnit = 12.sp
}

@Composable
fun desktopPageHorizontalPadding(width: Dp): Dp {
    val dynamicPadding = width * 0.04f
    return dynamicPadding.coerceIn(
        minimumValue = DesktopMusicDimens.PagePaddingMinHorizontal,
        maximumValue = DesktopMusicDimens.PagePaddingMaxHorizontal,
    )
}
