package com.yanhao.kmpmusic.feature.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors

// 搜索页跟随统一的移动端页面纯白背景。
internal val searchBackgroundColor: Color = MusicColors.PageBackground

// 搜索页顶栏与页面背景保持纯白一致。
internal val searchTopBarColor: Color = MusicColors.PageBackground

// Figma 搜索页 Toolbar 主文字与图标色。
internal val searchToolbarContentColor: Color = MusicColors.MobileToolbarContent

// 用户指定搜索提交文字和输入光标使用当前 App 绿色。
internal val searchToolbarAccentColor: Color = MusicColors.Accent

// Figma 搜索页 Toolbar 输入框底色。
internal val searchInputColor: Color = Color(0x14000000)

// Figma 搜索页 chip 底色。
internal val searchChipColor: Color = Color(0xFFECEEEF)

// Figma 搜索页主标题色。
internal val searchPrimaryTextColor: Color = Color(0xFF191C1D)

// Figma 搜索页正文次级色。
internal val searchSecondaryTextColor: Color = Color(0xFF3D4947)

// Figma 搜索页输入占位色，按节点 54% 黑色透明度还原。
internal val searchPlaceholderTextColor: Color = Color(0x8A000000)

// Figma 搜索页主强调色。
internal val searchAccentColor: Color = Color(0xFF006A62)

// Figma 搜索页分隔线色。
internal val searchDividerColor: Color = Color(0xFFBCC9C6)

// Figma 搜索页空态图标色。
internal val searchEmptyIconColor: Color = Color(0xFFD3DBD9)

// Figma 搜索页空态文字色。
internal val searchEmptyTextColor: Color = Color(0xFF8F9A98)

// Figma 搜索页 Toolbar 分隔线色。
internal val searchToolbarDividerColor: Color = Color(0x29000000)

// Figma 搜索页输入清除按钮底色。
internal val searchClearButtonColor: Color = Color(0x29000000)

// Figma 搜索页输入清除按钮图标色。
internal val searchClearIconColor: Color = Color.White

// Figma 搜索页顶栏高度。
internal val searchTopBarHeight: Dp = 52.dp

// Figma 搜索页顶栏左侧留白。
internal val searchTopBarStartPadding: Dp = 4.dp

// Figma 搜索页顶栏右侧留白。
internal val searchTopBarEndPadding: Dp = 16.dp

// Figma 搜索页返回槽宽度。
internal val searchTopBarBackSlotWidth: Dp = 48.dp

// 搜索页正文内容水平页边距。
internal val searchHorizontalPadding: Dp = 20.dp

// Figma 搜索页搜索框高度。
internal val searchInputHeight: Dp = 40.dp

// Figma 搜索页搜索框横向内边距。
internal val searchInputHorizontalPadding: Dp = 4.dp

// Figma 搜索页搜索框纵向内边距。
internal val searchInputVerticalPadding: Dp = 2.dp

// Figma 搜索页搜索图标槽尺寸。
internal val searchInputIconSlotSize: Dp = 36.dp

// Figma 搜索页搜索图标尺寸。
internal val searchInputIconSize: Dp = 20.dp

// Figma 搜索页预留语音槽尺寸。
internal val searchInputMicSlotSize: Dp = 36.dp

// Figma 搜索页清除按钮圆形底尺寸。
internal val searchClearButtonSize: Dp = 24.dp

// Figma 搜索页清除按钮图标尺寸。
internal val searchClearIconSize: Dp = 16.dp

// Figma 搜索页分隔线外层槽宽度。
internal val searchInputDividerSlotWidth: Dp = 9.dp

// Figma 搜索页分隔线高度。
internal val searchInputDividerHeight: Dp = 16.dp

// Figma 搜索页提交按钮宽度。
internal val searchSubmitButtonWidth: Dp = 67.dp

// Figma 搜索页提交按钮高度。
internal val searchSubmitButtonHeight: Dp = 28.dp

// Figma 搜索页历史区 chips 高度。
internal val searchHistoryChipHeight: Dp = 32.dp
