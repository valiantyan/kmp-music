package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 新建歌单弹窗的 Figma 节点 974:672 视觉规格。
 */
internal object CreatePlaylistDialogDesignSpec {
    // 新建弹窗最大宽度，对应节点截图约 358px 宽度。
    val maxWidth: Dp = 358.dp

    // 新建弹窗圆角，对应节点 974:672 的 32px 圆角。
    val cornerRadius: Dp = 32.dp

    // 新建弹窗内容内边距，对应节点 974:672 的 33px padding。
    val contentPadding: Dp = 33.dp

    // 输入框高度，对齐 Figma 的 56px 输入区。
    val inputHeight: Dp = 56.dp

    // 底部按钮高度，对齐 Figma 的 56px 胶囊按钮。
    val buttonHeight: Dp = 56.dp

    // 半透明白底透明度，对应节点 974:672 的 85% 白底。
    val backgroundAlpha: Float = 0.85f

    // 新建弹窗底色，集中表达半透明白底来源。
    val containerColor: Color = Color.White

    // 新建弹窗描边宽度。
    val borderWidth: Dp = 1.dp

    // 新建弹窗白色描边透明度。
    val borderAlpha: Float = 0.4f

    // 输入框圆角，对齐节点中的 16px 输入容器。
    val inputRadius: Dp = 16.dp

    // 标题和副标题间距。
    val headerGap: Dp = 8.dp

    // 主内容分组间距。
    val contentGap: Dp = 24.dp

    // 底部按钮横向间距。
    val actionGap: Dp = 16.dp

    // 确认按钮胶囊圆角。
    val actionButtonRadius: Dp = 999.dp
}
