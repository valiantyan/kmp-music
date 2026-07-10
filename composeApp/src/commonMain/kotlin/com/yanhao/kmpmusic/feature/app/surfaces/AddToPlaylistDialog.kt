package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.feature.app.AddToPlaylistFlowState
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * 添加到歌单流程弹窗，承载已有歌单搜索单选和“新建歌单”入口。
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
 * 主体内容按 Figma 顺序排列：头部、搜索、新建入口、已有列表、底部动作。
 */
@Composable
private fun AddToPlaylistDialogContent(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
) {
    Column(
        modifier = Modifier.padding(all = AddToPlaylistDialogDesignSpec.contentPadding),
        verticalArrangement = Arrangement.spacedBy(space = AddToPlaylistDialogDesignSpec.footerButtonGap),
    ) {
        AddToPlaylistHeader(controller = controller)
        AddToPlaylistSearchField(
            flow = flow,
            controller = controller,
        )
        NewPlaylistEntry(controller = controller)
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
 * 头部保留关闭按钮，便于弹窗在桌面和移动端都能直接退出流程。
 */
@Composable
private fun AddToPlaylistHeader(controller: MusicAppController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "添加到歌单")
        IconButton(onClick = controller::closeAddToPlaylistFlow) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "关闭",
                tint = MusicColors.Muted,
            )
        }
    }
}

/**
 * 搜索框独立成组件，避免列表筛选入口和弹窗容器尺寸规则混在一起。
 */
@Composable
private fun AddToPlaylistSearchField(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().height(height = AddToPlaylistDialogDesignSpec.searchHeight),
        value = flow.playlistSearchQuery,
        onValueChange = controller::setAddToPlaylistSearchQuery,
        singleLine = true,
        shape = RoundedCornerShape(size = AddToPlaylistDialogDesignSpec.searchRadius),
        leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
        placeholder = { Text(text = "搜索歌单...") },
    )
}

/**
 * 新建入口使用卡片形态而不是普通文本按钮，贴近节点 974:690 的入口结构。
 */
@Composable
private fun NewPlaylistEntry(controller: MusicAppController) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = AddToPlaylistDialogDesignSpec.newPlaylistRadius),
        border = BorderStroke(
            width = AddToPlaylistDialogDesignSpec.borderWidth,
            color = MusicColors.Line,
        ),
        color = AddToPlaylistDialogDesignSpec.containerColor,
        onClick = controller::openCreatePlaylistDialog,
    ) {
        Row(
            modifier = Modifier.padding(all = AddToPlaylistDialogDesignSpec.newPlaylistPadding),
            horizontalArrangement = Arrangement.spacedBy(space = AddToPlaylistDialogDesignSpec.newPlaylistGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NewPlaylistIcon()
            Column(verticalArrangement = Arrangement.spacedBy(space = AddToPlaylistDialogDesignSpec.rowGap / 2)) {
                Text(text = "新建歌单", color = MusicColors.AccentDeep)
                Text(text = "创建一个新的音乐收藏", color = MusicColors.Muted)
            }
        }
    }
}

/**
 * 新建入口图标块沿用设计稿的浅绿色方块视觉。
 */
@Composable
private fun NewPlaylistIcon() {
    Surface(
        modifier = Modifier.size(size = AddToPlaylistDialogDesignSpec.newPlaylistIconSize),
        shape = RoundedCornerShape(size = AddToPlaylistDialogDesignSpec.newPlaylistIconRadius),
        color = MusicColors.AccentSoft,
    ) {
        Icon(
            modifier = Modifier.padding(all = AddToPlaylistDialogDesignSpec.iconPadding),
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = MusicColors.AccentDeep,
        )
    }
}

// 主容器描边集中在这里，保证颜色和透明度与规格对象保持一致。
private fun addToPlaylistDialogBorder(): BorderStroke {
    return BorderStroke(
        width = AddToPlaylistDialogDesignSpec.borderWidth,
        color = AddToPlaylistDialogDesignSpec.containerColor.copy(
            alpha = AddToPlaylistDialogDesignSpec.borderAlpha,
        ),
    )
}
