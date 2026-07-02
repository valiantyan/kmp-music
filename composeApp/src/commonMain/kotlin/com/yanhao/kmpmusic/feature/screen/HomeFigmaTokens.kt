package com.yanhao.kmpmusic.feature.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Figma 首页歌曲页主色，来自节点 871:477。
internal val homeAccentColor: Color = Color(0xFF006A62)

// Figma 首页普通 chip 背景色。
internal val homeChipColor: Color = Color(0xFFECEEEF)

// Figma 首页正文次级文字色。
internal val homeMutedColor: Color = Color(0xFF6D7A77)

// Figma 首页当前歌曲背景最终色，设计确认值为 #DDFCF7。
internal val homeActiveRowColor: Color = Color(0xFFDDFCF7)

// Figma 首页当前歌曲描边色，设计标注为 #006A6233。
internal val homeActiveBorderColor: Color = Color(0x33006A62)

// Figma 首页专辑封面底色，来自节点 883:514。
internal val homeAlbumCoverBackgroundColor: Color = Color(0xFFE1E3E4)

// Figma 首页当前专辑覆盖色，来自节点 883:514 的 10% 主色 overlay。
internal val homeActiveAlbumOverlayColor: Color = Color(0x1A006A62)

// Figma 首页专辑网格列间距。
internal val homeAlbumGridGap: Dp = 24.dp

// Figma 首页专辑封面圆角。
internal val homeAlbumCoverRadius: Dp = 24.dp

// Figma 首页当前专辑底部强调线高度。
internal val homeAlbumActiveBorderHeight: Dp = 4.dp

// 首页歌手列表行高按视觉复核继续从 90dp 收紧到 84dp。
internal val homeArtistRowHeight: Dp = 84.dp

// 首页歌手列表行内垂直留白跟随行高收紧，保持 64dp 头像完整。
internal val homeArtistRowVerticalPadding: Dp = 10.dp

// 首页歌手列表行间距按视觉复核从 6dp 收紧到 0dp。
internal val homeArtistListGap: Dp = 0.dp

// 首页歌手姓名字号按视觉复核从 24sp 调整为 20sp。
internal val homeArtistNameFontSize: TextUnit = 20.sp

// 首页歌手姓名行高跟随字号从 32sp 调整为 28sp。
internal val homeArtistNameLineHeight: TextUnit = 28.sp

// Figma 首页歌手头像外圈尺寸。
internal val homeArtistAvatarOuterSize: Dp = 64.dp

// Figma 首页歌手头像外圈描边宽度。
internal val homeArtistAvatarBorderWidth: Dp = 2.dp

// Figma 首页歌手头像外圈到图片的内边距。
internal val homeArtistAvatarInset: Dp = 2.dp

// Figma 首页歌手头像外圈描边色。
internal val homeArtistAvatarBorderColor: Color = Color(0x3326A69A)

// Figma 首页歌手列表右箭头色。
internal val homeArtistChevronColor: Color = Color(0xFFB8C7C4)
