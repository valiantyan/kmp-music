package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors

/**
 * 添加到歌单弹窗的 Figma 节点 982:881 视觉规格。
 */
internal object AddToPlaylistDialogDesignSpec {
    // 弹窗基准宽度，对应节点 982:882 的 358px 容器。
    val width: Dp = 358.dp
    // 弹窗默认高度，按 Android 标准对话框上限收敛，列表内容在内部滚动。
    val height: Dp = 560.dp
    // 弹窗最大高度，避免自定义容器超过 Android 标准对话框高度。
    val maxHeight: Dp = 560.dp
    // 主容器圆角，对应节点 982:882 的 24px 圆角。
    val cornerRadius: Dp = 24.dp
    // 头部高度，对齐标题所在 68px 区域。
    val headerHeight: Dp = 68.dp
    // 列表左右内边距，对齐 Figma 中 x=24 的内容起点。
    val horizontalPadding: Dp = 24.dp
    // 新建歌单行高度，对齐 68px 首行。
    val newPlaylistRowHeight: Dp = 68.dp
    // 已有歌单首行高度，对齐分隔线后的 68px 行。
    val firstPlaylistRowHeight: Dp = 68.dp
    // 已有歌单普通行高度，对齐后续 64px 行。
    val playlistRowHeight: Dp = 64.dp
    // 列表项封面尺寸，对齐 40px 方形缩略图。
    val playlistCoverSize: Dp = 40.dp
    // 列表项封面圆角，对齐 8px 圆角。
    val playlistCoverRadius: Dp = 8.dp
    // 新建入口加号图标内边距，对齐 24px 图标在 40px 容器里的视觉中心。
    val newPlaylistIconPadding: Dp = 9.dp
    // 图标列总宽度，保留 40px 封面与右侧 16px 间距。
    val leadingSlotWidth: Dp = 56.dp
    // 单选圆环尺寸，对齐 20px 选择控件。
    val radioSize: Dp = 20.dp
    // 单选圆环边框宽度，对齐 2px 描边。
    val radioBorderWidth: Dp = 2.dp
    // 选中圆点尺寸，保持单选反馈不改变外圈尺寸。
    val radioDotSize: Dp = 10.dp
    // 分隔线高度，对齐 1px 列表分割。
    val dividerHeight: Dp = 1.dp
    // 底部动作区外层高度，对齐节点 982:972 的 61.06px。
    val footerHeight: Dp = 61.dp
    // 底部动作实际点击区高度，对齐节点 982:973 的 54.06px。
    val footerActionHeight: Dp = 54.dp
    // 底部与列表之间的 7px 间隔。
    val footerTopPadding: Dp = 7.dp
    // 底部分栏竖线高度，对齐 32px 分隔线。
    val footerDividerHeight: Dp = 32.dp
    // 弹窗白色底色，集中避免页面内散落硬编码颜色。
    val containerColor: Color = Color.White
    // 设计稿背景浅灰，复用在占位封面和新建图标块中。
    val softContainerColor: Color = MusicColors.DialogSoft
    // 列表分隔线颜色。
    val dividerColor: Color = MusicColors.DialogDivider
    // 主文字颜色。
    val primaryTextColor: Color = MusicColors.DialogText
    // 辅助文字颜色。
    val secondaryTextColor: Color = MusicColors.DialogMuted
    // 单选未选中描边色。
    val radioBorderColor: Color = MusicColors.DialogControlBorder
    // 底部按钮文字和新建图标色。
    val actionColor: Color = MusicColors.DialogAction

    /**
     * 根据窗口高度解析实际弹窗高度，保证默认高度和最大高度都遵守 Android 对话框规范。
     */
    fun resolveHeight(maxHeight: Dp): Dp {
        return minOf(a = height, b = this.maxHeight, c = maxHeight)
    }
}
