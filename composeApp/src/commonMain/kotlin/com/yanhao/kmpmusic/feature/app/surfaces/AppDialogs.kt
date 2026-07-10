package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yanhao.kmpmusic.core.theme.MusicColors
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
 * 添加到歌单流程弹窗，本切片只承载“新建歌单”入口，已有歌单选择留给后续任务。
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
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = controller::openCreatePlaylistDialog,
                    ) {
                        Text(text = "新建歌单")
                    }
                    Text(text = "暂无歌单")
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
