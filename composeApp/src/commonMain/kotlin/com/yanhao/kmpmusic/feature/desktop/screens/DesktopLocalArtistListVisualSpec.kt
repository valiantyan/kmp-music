package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Artist

/**
 * 桌面歌手列表 Figma `1085:709` 的关键视觉参数。
 *
 * @property pageBackgroundColor 页面底色。
 * @property pageHorizontalPadding 页面左右内边距。
 * @property pageTopPadding 页面顶部内边距。
 * @property pageBottomPadding 页面底部内边距。
 * @property listColor 列表容器底色。
 * @property listBorderColor 列表容器描边色。
 * @property listRadius 列表容器圆角。
 * @property listBorderPadding 容器边框内的 1dp 内容起点。
 * @property regularRowHeight 普通行高度，包含底部分隔线。
 * @property lastRowHeight 最后一行高度，不再额外携带分隔线。
 * @property rowHorizontalPadding 行内左右内边距。
 * @property avatarOuterSize 头像外圈尺寸。
 * @property avatarBorderWidth 头像白色描边宽度。
 * @property avatarInset 头像图片相对外圈的内缩。
 * @property avatarShadowElevation 头像轻阴影。
 * @property contentGap 头像、文字和箭头之间的固定间距。
 * @property artistNameFontSize 歌手名字号。
 * @property artistNameLineHeight 歌手名行高。
 * @property artistSubtitleFontSize 歌手统计字号。
 * @property artistSubtitleLineHeight 歌手统计行高。
 * @property chevronWidth 行尾箭头宽度。
 * @property chevronHeight 行尾箭头高度。
 */
internal data class DesktopLocalArtistListVisualSpec(
    val pageBackgroundColor: Color,
    val pageHorizontalPadding: Dp,
    val pageTopPadding: Dp,
    val pageBottomPadding: Dp,
    val listColor: Color,
    val listBorderColor: Color,
    val listRadius: Dp,
    val listBorderPadding: Dp,
    val regularRowHeight: Dp,
    val lastRowHeight: Dp,
    val rowHorizontalPadding: Dp,
    val avatarOuterSize: Dp,
    val avatarBorderWidth: Dp,
    val avatarInset: Dp,
    val avatarShadowElevation: Dp,
    val contentGap: Dp,
    val artistNameFontSize: TextUnit,
    val artistNameLineHeight: TextUnit,
    val artistSubtitleFontSize: TextUnit,
    val artistSubtitleLineHeight: TextUnit,
    val chevronWidth: Dp,
    val chevronHeight: Dp,
)

/** 集中解析歌手列表视觉规格，方便测试锁住 Figma 的关键数字。 */
internal fun resolveDesktopLocalArtistListVisualSpec(): DesktopLocalArtistListVisualSpec =
    DesktopLocalArtistListVisualSpec(
        pageBackgroundColor = Color(0xFFF9F9FF),
        pageHorizontalPadding = 24.dp,
        pageTopPadding = 16.dp,
        pageBottomPadding = 0.dp,
        listColor = Color(0x66F0F3FF),
        listBorderColor = Color(0x1ABBCAC4),
        listRadius = 12.dp,
        listBorderPadding = 1.dp,
        regularRowHeight = 97.dp,
        lastRowHeight = 96.dp,
        rowHorizontalPadding = 16.dp,
        avatarOuterSize = 64.dp,
        avatarBorderWidth = 2.dp,
        avatarInset = 2.dp,
        avatarShadowElevation = 1.dp,
        contentGap = 16.dp,
        artistNameFontSize = 20.sp,
        artistNameLineHeight = 28.sp,
        artistSubtitleFontSize = 13.sp,
        artistSubtitleLineHeight = 18.sp,
        chevronWidth = 7.4.dp,
        chevronHeight = 12.dp,
    )

/** 歌手列表副标题必须同时展示歌曲数和专辑数，匹配 Figma 文案结构。 */
internal fun formatDesktopLocalArtistSubtitle(artist: Artist): String = "${artist.songCount} 首歌曲 · ${artist.albumCount} 张专辑"
