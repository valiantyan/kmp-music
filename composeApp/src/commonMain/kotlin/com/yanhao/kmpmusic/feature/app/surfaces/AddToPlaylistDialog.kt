package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yanhao.kmpmusic.feature.app.AddToPlaylistFlowState
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * 添加到歌单流程弹窗，只保留 Figma 新版中的新建入口、已有歌单单选和底部动作。
 */
@Composable
internal fun AddToPlaylistDialog(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
) {
    Dialog(onDismissRequest = controller::closeAddToPlaylistFlow) {
        BoxWithConstraints {
            val dialogWidth: Dp = minOf(a = AddToPlaylistDialogDesignSpec.width, b = maxWidth)
            val dialogHeight: Dp = AddToPlaylistDialogDesignSpec.resolveHeight(maxHeight = maxHeight)
            Surface(
                modifier = Modifier.width(width = dialogWidth).height(height = dialogHeight),
                shape = RoundedCornerShape(size = AddToPlaylistDialogDesignSpec.cornerRadius),
                color = AddToPlaylistDialogDesignSpec.containerColor,
                border = addToPlaylistDialogBorder(),
            ) {
                AddToPlaylistDialogContent(
                    flow = flow,
                    controller = controller,
                )
            }
        }
    }
    if (flow.isCreateDialogOpen) {
        CreatePlaylistDialog(
            flow = flow,
            controller = controller,
        )
    }
}

/**
 * 主体内容按 Figma 顺序排列：固定标题、滚动列表和底部分栏动作。
 */
@Composable
private fun AddToPlaylistDialogContent(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AddToPlaylistHeader()
        ExistingPlaylistList(
            flow = flow,
            controller = controller,
            modifier = Modifier.weight(weight = 1f),
        )
        AddToPlaylistFooter(
            flow = flow,
            controller = controller,
        )
    }
}

/**
 * 头部只呈现居中标题，对齐 Figma 中没有关闭按钮的弹窗结构。
 */
@Composable
private fun AddToPlaylistHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = AddToPlaylistDialogDesignSpec.headerHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "添加到歌单",
            color = AddToPlaylistDialogDesignSpec.primaryTextColor,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// 主容器描边集中在这里，保持弹窗边界在白底和浅灰遮罩上都稳定。
private fun addToPlaylistDialogBorder(): BorderStroke {
    return BorderStroke(
        width = AddToPlaylistDialogDesignSpec.dividerHeight,
        color = AddToPlaylistDialogDesignSpec.containerColor,
    )
}
