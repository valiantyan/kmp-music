package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.LocalPlaylist
import com.yanhao.kmpmusic.feature.app.AddToPlaylistFlowState
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 跨端复用的全局确认对话框。
 */
@Composable
fun AppDialogs(
    state: MusicAppUiState,
    controller: MusicAppController,
) {
    state.addToPlaylistFlow?.let { flow: AddToPlaylistFlowState ->
        AddToPlaylistDialog(
            flow = flow,
            controller = controller,
        )
    }
    state.transientMessage?.let { message: String ->
        AlertDialog(
            onDismissRequest = controller::clearTransientMessage,
            confirmButton = {
                Button(onClick = controller::clearTransientMessage) {
                    Text(text = "知道了")
                }
            },
            icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = MusicColors.Accent) },
            title = { Text(text = "已添加") },
            text = { Text(text = message) },
        )
    }
    if (state.isClearCacheDialogOpen) {
        AlertDialog(
            onDismissRequest = controller::closeClearCacheDialog,
            confirmButton = {
                Button(onClick = controller::confirmClearCache) {
                    Text(text = "清理")
                }
            },
            dismissButton = {
                Button(onClick = controller::closeClearCacheDialog) {
                    Text(text = "取消")
                }
            },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MusicColors.Danger) },
            title = { Text(text = "清理 428 MB 缓存？") },
            text = { Text(text = "只会删除封面缓存和临时文件，本地歌曲不会受到影响。") },
        )
    }
    if (state.isPermissionSettingsDialogOpen) {
        AlertDialog(
            onDismissRequest = controller::closePermissionSettingsDialog,
            confirmButton = {
                Button(onClick = controller::confirmPermissionSettings) {
                    Text(text = "去设置")
                }
            },
            dismissButton = {
                Button(onClick = controller::closePermissionSettingsDialog) {
                    Text(text = "取消")
                }
            },
            icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = MusicColors.Accent) },
            title = { Text(text = "开启音频权限") },
            text = { Text(text = "需要在系统设置中开启音频权限，才能扫描本机歌曲。") },
        )
    }
}

/**
 * 添加到歌单流程弹窗，承载已有歌单搜索单选和“新建歌单”入口。
 */
@Composable
private fun AddToPlaylistDialog(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
) {
    Dialog(onDismissRequest = controller::closeAddToPlaylistFlow) {
        BoxWithConstraints {
            val dialogWidth: Dp = minOf(a = 440.dp, b = maxWidth)
            val maxDialogHeight: Dp = maxHeight * 2f / 3f
            val dialogHeight: Dp = minOf(a = 679.dp, b = maxDialogHeight)
            Surface(
                modifier = Modifier
                    .width(width = dialogWidth)
                    .height(height = dialogHeight),
                shape = RoundedCornerShape(size = 32.dp),
                color = MusicColors.Paper,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(text = "添加到歌单")
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = flow.playlistSearchQuery,
                        onValueChange = controller::setAddToPlaylistSearchQuery,
                        singleLine = true,
                        label = { Text(text = "搜索歌单") },
                    )
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = controller::openCreatePlaylistDialog,
                    ) {
                        Text(text = "新建歌单")
                    }
                    ExistingPlaylistList(
                        flow = flow,
                        controller = controller,
                        modifier = Modifier.weight(weight = 1f),
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = flow.canCompleteExistingPlaylist,
                        onClick = controller::addCurrentSongToSelectedPlaylist,
                    ) {
                        Text(text = "完成")
                    }
                }
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
 * 已有歌单列表在弹窗内部滚动，避免超过规格要求的最大弹窗高度。
 */
@Composable
private fun ExistingPlaylistList(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
    modifier: Modifier = Modifier,
) {
    if (flow.availablePlaylists.isEmpty()) {
        Text(
            modifier = modifier.fillMaxWidth(),
            text = resolveAddToPlaylistEmptyStateText(flow = flow),
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        items(
            items = flow.availablePlaylists,
            key = { playlist: LocalPlaylist -> playlist.id },
            contentType = { "local-playlist-option" },
        ) { playlist: LocalPlaylist ->
            ExistingPlaylistRow(
                playlist = playlist,
                isSelected = playlist.id == flow.selectedPlaylistId,
                onSelect = { controller.selectAddToPlaylistTarget(playlistId = playlist.id) },
            )
        }
    }
}

/**
 * 空搜索和无歌单空态文案不同，帮助用户区分“还没创建”和“当前关键词无匹配”。
 */
internal fun resolveAddToPlaylistEmptyStateText(flow: AddToPlaylistFlowState): String {
    return if (!flow.hasAnyPlaylist || flow.playlistSearchQuery.trim().isEmpty()) {
        "暂无歌单"
    } else {
        "未找到相关歌单"
    }
}

/**
 * 单个已有歌单选项，使用熟悉的单选控件表达“一次只能选一个”。
 */
@Composable
private fun ExistingPlaylistRow(
    playlist: LocalPlaylist,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = 16.dp),
        color = MusicColors.Soft,
        onClick = onSelect,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
            )
            Text(
                modifier = Modifier.weight(weight = 1f),
                text = playlist.name,
            )
        }
    }
}

/**
 * 新建歌单弹窗只处理名称校验和确认，成功后由控制器关闭整条添加流程。
 */
@Composable
private fun CreatePlaylistDialog(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
) {
    Dialog(onDismissRequest = controller::closeAddToPlaylistFlow) {
        Surface(
            modifier = Modifier.widthIn(max = 340.dp),
            shape = RoundedCornerShape(size = 32.dp),
            color = MusicColors.Paper.copy(alpha = 0.92f),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    modifier = Modifier.size(size = 32.dp),
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = MusicColors.Accent,
                )
                Text(text = "新建歌单")
                OutlinedTextField(
                    value = flow.newPlaylistName,
                    onValueChange = controller::setNewPlaylistName,
                    singleLine = true,
                    isError = flow.newPlaylistNameError != null,
                    label = { Text(text = "歌单名称") },
                )
                flow.newPlaylistNameError?.let { message: String ->
                    Text(text = message, color = MusicColors.Danger)
                }
                RowActions(controller = controller)
            }
        }
    }
}

/**
 * 新建弹窗底部动作保持在自绘容器内，避免回退到默认 [AlertDialog] 视觉。
 */
@Composable
private fun RowActions(controller: MusicAppController) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = controller::closeAddToPlaylistFlow) {
            Text(text = "取消")
        }
        Button(onClick = controller::createPlaylistWithCurrentSong) {
            Text(text = "创建")
        }
    }
}
