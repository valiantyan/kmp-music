package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.feature.app.AddToPlaylistFlowState
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * 底部动作区固定在弹窗底部，按 Figma 使用左右等分文本按钮。
 */
@Composable
internal fun AddToPlaylistFooter(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = AddToPlaylistDialogDesignSpec.footerHeight)
            .padding(top = AddToPlaylistDialogDesignSpec.footerTopPadding),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = AddToPlaylistDialogDesignSpec.dividerHeight)
                .background(color = AddToPlaylistDialogDesignSpec.softContainerColor),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = AddToPlaylistDialogDesignSpec.footerActionHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AddToPlaylistFooterButton(
                modifier = Modifier.weight(weight = 1f),
                text = "取消",
                enabled = true,
                onClick = controller::closeAddToPlaylistFlow,
            )
            AddToPlaylistFooterDivider()
            AddToPlaylistFooterButton(
                modifier = Modifier.weight(weight = 1f),
                text = "确认",
                enabled = flow.canCompleteExistingPlaylist,
                onClick = controller::addCurrentSongToSelectedPlaylist,
            )
        }
    }
}

/**
 * 文本按钮用自定义实现，避免 Material 默认高度、圆角和背景偏离设计稿。
 */
@Composable
private fun AddToPlaylistFooterButton(
    modifier: Modifier,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) {
                AddToPlaylistDialogDesignSpec.actionColor
            } else {
                AddToPlaylistDialogDesignSpec.actionColor.copy(alpha = 0.38f)
            },
            fontSize = 15.sp,
            lineHeight = 22.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
        )
    }
}

/**
 * 中间竖线只占 32px 高度，对齐 Figma 的按钮分隔线。
 */
@Composable
private fun AddToPlaylistFooterDivider() {
    Box(
        modifier = Modifier
            .width(width = AddToPlaylistDialogDesignSpec.dividerHeight)
            .height(height = AddToPlaylistDialogDesignSpec.footerDividerHeight)
            .background(color = AddToPlaylistDialogDesignSpec.softContainerColor),
    )
}
