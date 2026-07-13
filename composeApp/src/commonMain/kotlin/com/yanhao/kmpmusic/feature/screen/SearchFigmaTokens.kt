package com.yanhao.kmpmusic.feature.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors

// 搜索页跟随统一的移动端页面纯白背景。
internal val searchBackgroundColor: Color = MusicColors.PageBackground

// 搜索页顶栏与页面背景保持纯白一致。
internal val searchTopBarColor: Color = MusicColors.PageBackground

// Figma 搜索页输入框底色。
internal val searchInputColor: Color = Color(0xFFF2F4F5)

// Figma 搜索页 chip 底色。
internal val searchChipColor: Color = Color(0xFFECEEEF)

// Figma 搜索页主标题色。
internal val searchPrimaryTextColor: Color = Color(0xFF191C1D)

// Figma 搜索页正文次级色。
internal val searchSecondaryTextColor: Color = Color(0xFF3D4947)

// Figma 搜索页输入占位色，按节点透明度折算。
internal val searchPlaceholderTextColor: Color = Color(0x996D7A77)

// Figma 搜索页主强调色。
internal val searchAccentColor: Color = Color(0xFF006A62)

// Figma 搜索页分隔线色。
internal val searchDividerColor: Color = Color(0xFFBCC9C6)

// Figma 搜索页空态图标色。
internal val searchEmptyIconColor: Color = Color(0xFFD3DBD9)

// Figma 搜索页空态文字色。
internal val searchEmptyTextColor: Color = Color(0xFF8F9A98)

// Figma 搜索页顶栏高度。
internal val searchTopBarHeight: Dp = 64.dp

// Figma 搜索页水平页边距。
internal val searchHorizontalPadding: Dp = 20.dp

// Figma 搜索页搜索框高度。
internal val searchInputHeight: Dp = 48.dp

// Figma 搜索页历史区 chips 高度。
internal val searchHistoryChipHeight: Dp = 32.dp
