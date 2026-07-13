package com.yanhao.kmpmusic.feature.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors

// 收藏页跟随统一的移动端页面纯白背景。
internal val favoritesBackgroundColor: Color = MusicColors.PageBackground

// Figma 收藏页主操作色，来自播放全部按钮。
internal val favoritesActionColor: Color = Color(0xFF26A69A)

// Figma 收藏页主标题色，复用收藏顶栏标注。
internal val favoritesTitleColor: Color = Color(0xFF006A62)

// Figma 收藏页正文主文字色。
internal val favoritesTextColor: Color = Color(0xFF191C1D)

// Figma 收藏页正文次级文字色。
internal val favoritesMetaColor: Color = Color(0xFF3D4947)

// Figma 收藏页弱化操作色，来自更多按钮和分段切换图标。
internal val favoritesMutedIconColor: Color = Color(0xFFB8C7C4)

// Figma 收藏页水平页边距。
internal val favoritesHorizontalPadding: Dp = 20.dp

// Figma 收藏页操作栏高度。
internal val favoritesActionHeaderHeight: Dp = 44.dp

// Figma 收藏页操作栏到歌曲列表的间距。
internal val favoritesActionToListGap: Dp = 24.dp

// Figma 收藏页歌曲卡片高度。
internal val favoritesSongRowHeight: Dp = 80.dp

// Figma 收藏页歌曲卡片圆角。
internal val favoritesSongRowRadius: Dp = 24.dp

// Figma 收藏页歌曲卡片内边距。
internal val favoritesSongRowPadding: Dp = 12.dp

// Figma 收藏页歌曲卡片间距。
internal val favoritesSongRowGap: Dp = 16.dp

// Figma 收藏页封面尺寸。
internal val favoritesSongCoverSize: Dp = 56.dp

// Figma 收藏页封面圆角。
internal val favoritesSongCoverRadius: Dp = 16.dp

// Figma 收藏页尾部图标按钮尺寸。
internal val favoritesSongActionSize: Dp = 40.dp
