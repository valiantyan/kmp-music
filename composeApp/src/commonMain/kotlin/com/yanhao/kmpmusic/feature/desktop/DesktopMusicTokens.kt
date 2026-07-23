package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.layout.PaddingValues
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
    val MinWindowWidth: Dp = 1240.dp
    val MinWindowHeight: Dp = 824.dp
    val DefaultWindowWidth: Dp = 1240.dp
    val DefaultWindowHeight: Dp = 824.dp
    val TitleBarHeight: Dp = 42.dp
    val RailWidth: Dp = 240.dp
    val LibrarySidebarWidth: Dp = 304.dp
    val PlayerHeight: Dp = 96.dp
    val PagePaddingTop: Dp = 34.dp
    val PagePaddingMinHorizontal: Dp = 34.dp
    val PagePaddingMaxHorizontal: Dp = 68.dp
    val RailItemSize: Dp = 64.dp
    val BrandSize: Dp = 40.dp
    val PrimaryButtonHeight: Dp = 40.dp
    val StatCardMinHeight: Dp = 84.dp
    val TableHeaderHeight: Dp = 40.dp
    val TableRowHeight: Dp = 48.dp
    val TableCoverSize: Dp = 34.dp
    val TableColumnGap: Dp = 20.dp
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
    val TableHeader: TextUnit = 12.sp
    val TableTitle: TextUnit = 14.sp
    val StatTitle: TextUnit = 15.sp
    val SidebarTitle: TextUnit = 17.sp
    val SidebarBody: TextUnit = 13.sp
    val RailLabel: TextUnit = 12.sp
}

/** 搜索与管理歌单共用的返回式顶栏尺寸和文字规格。 */
internal object DesktopNavigationToolbarTokens {
    val Height: Dp = 64.dp
    val HorizontalPadding: Dp = 24.dp
    val Title: Color = Color(0xFF111C2D)
    val TitleSize: TextUnit = 20.sp
    val TitleLineHeight: TextUnit = 28.sp
}

/** Figma `1113:1481` 搜索页面专用的视觉令牌。 */
internal object DesktopSearchTokens {
    val Background: Color = Color(0xFFF9F9FF)
    val Title: Color = Color(0xFF111C2D)
    val SupportingText: Color = Color(0xFF3C4A46)
    val MutedText: Color = Color(0xFF3C4A46).copy(alpha = 0.4f)
    val Accent: Color = Color(0xFF006B5C)
    val InputContainer: Color = Color(0xFFF0F3FF)
    val HistoryChip: Color = Color(0xFFDEE8FF)
    val Line: Color = Color(0xFFBBCAC4).copy(alpha = 0.2f)
    val FaintLine: Color = Color(0xFFBBCAC4).copy(alpha = 0.1f)

    /** 搜索页只保留顶部和左右间距，使结果列表贴合全局播放器上边界。 */
    val ContentPadding: PaddingValues = PaddingValues(start = 32.dp, top = 32.dp, end = 32.dp)
    val InputMaxWidth: Dp = 672.dp
}

/** 歌单与管理歌单 Figma 页面专用的颜色和稳定尺寸，避免影响既有桌面页面。 */
internal object DesktopPlaylistTokens {
    val Background: Color = Color(0xFFF9F9FF)
    val Title: Color = Color(0xFF111C2D)
    val SupportingText: Color = Color(0xFF3C4A46)
    val MutedText: Color = Color(0xFF3C4A46).copy(alpha = 0.6f)
    val Accent: Color = Color(0xFF006B5C)
    val SelectionOutline: Color = Color(0xFFBBCAC4)
    val ManagementBorder: Color = Color(0xFFBBCAC4).copy(alpha = 0.2f)
    val DangerContainer: Color = Color(0xFFFFDAD6)
    val Danger: Color = Color(0xFFBA1A1A)
    val CreateBorder: Color = Color(0xFFBBCAC4).copy(alpha = 0.3f)
    val HeaderHeight: Dp = 64.dp
    val ContentPadding: Dp = 24.dp
    val GridGap: Dp = 32.dp
    val CardCoverSize: Dp = 173.dp
    val CardCornerRadius: Dp = 16.dp
    val ManagementContentMaxWidth: Dp = 896.dp
    val ManagementHorizontalPadding: Dp = 72.dp
    val ManagementRowHeight: Dp = 96.dp
    val ManagementRowCornerRadius: Dp = 12.dp
    val ManagementDeleteBottomPadding: Dp = 32.dp
}

@Composable
fun desktopPageHorizontalPadding(width: Dp): Dp {
    val dynamicPadding = width * 0.04f
    return dynamicPadding.coerceIn(
        minimumValue = DesktopMusicDimens.PagePaddingMinHorizontal,
        maximumValue = DesktopMusicDimens.PagePaddingMaxHorizontal,
    )
}
