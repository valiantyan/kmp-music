package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 添加到歌单弹窗的 Figma 节点 974:690 视觉规格。
 */
internal object AddToPlaylistDialogDesignSpec {
    // 弹窗基准宽度，对应节点 974:690 的 440px 容器。
    val width: Dp = 440.dp
    // 弹窗基准高度，对应节点 974:690 的 679px 容器。
    val height: Dp = 679.dp
    // 主容器圆角，对应节点 974:690 的 24px 圆角。
    val cornerRadius: Dp = 24.dp
    // 主内容内边距，对齐头部、搜索框、入口卡片和底部动作区。
    val contentPadding: Dp = 24.dp
    // 搜索框高度，对齐 Figma 中搜索区域的纵向节奏。
    val searchHeight: Dp = 62.dp
    // 搜索框圆角，对齐节点中的 12px 输入容器。
    val searchRadius: Dp = 12.dp
    // 新建歌单入口圆角，对齐虚线入口卡片。
    val newPlaylistRadius: Dp = 16.dp
    // 已有歌单行圆角，对齐选中和未选列表项。
    val playlistRowRadius: Dp = 16.dp
    // 已有歌单行封面尺寸，保留设计稿列表项的固定节奏。
    val playlistCoverSize: Dp = 56.dp
    // 底部动作区高度，用于保证完成按钮区域稳定。
    val footerHeight: Dp = 70.dp
    // 滚动列表底部留白，避免最后一项被底部动作区遮住。
    val scrollBottomPadding: Dp = 86.dp
    // 小窗口下弹窗最大高度比例，来自规格“三分之二”约束。
    val maxHeightFraction: Float = 2f / 3f
    // 弹窗白色底色，集中避免页面内散落硬编码颜色。
    val containerColor: Color = Color.White
    // 主容器描边宽度，对齐半透明白边。
    val borderWidth: Dp = 1.dp
    // 主容器描边透明度，对齐 Figma 的 40% 白色边。
    val borderAlpha: Float = 0.4f
    // 新建入口图标容器尺寸，对齐节点内 48px 图标块。
    val newPlaylistIconSize: Dp = 48.dp
    // 新建入口图标容器圆角，对齐节点内 12px 图标块。
    val newPlaylistIconRadius: Dp = 12.dp
    // 新建入口整体内边距，对齐节点内 18px 入口卡片。
    val newPlaylistPadding: Dp = 18.dp
    // 入口与文本之间的横向间距。
    val newPlaylistGap: Dp = 16.dp
    // 入口图标内边距，保证图标视觉居中。
    val iconPadding: Dp = 14.dp
    // 列表行横向内边距。
    val rowHorizontalPadding: Dp = 12.dp
    // 列表行纵向内边距。
    val rowVerticalPadding: Dp = 10.dp
    // 列表行内部间距。
    val rowGap: Dp = 8.dp
    // 底部按钮间距。
    val footerButtonGap: Dp = 16.dp

    /**
     * 根据窗口高度解析实际弹窗高度，保证不超过规格中的三分之二限制。
     */
    fun resolveHeight(maxHeight: Dp): Dp {
        return minOf(a = height, b = maxHeight * maxHeightFraction)
    }
}
