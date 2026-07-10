package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            title = { Text(text = state.transientMessageTitle) },
            text = { Text(text = message) },
        )
    }
    if (state.isDeleteLocalPlaylistsDialogOpen) {
        AlertDialog(
            onDismissRequest = controller::closeDeleteLocalPlaylistsDialog,
            confirmButton = {
                Button(onClick = controller::confirmDeleteLocalPlaylists) {
                    Text(text = "删除")
                }
            },
            dismissButton = {
                Button(onClick = controller::closeDeleteLocalPlaylistsDialog) {
                    Text(text = "取消")
                }
            },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MusicColors.Danger) },
            title = { Text(text = "确认删除选中的 ${state.selectedManagedLocalPlaylistIds.size} 个歌单？") },
            text = { Text(text = "歌单会被删除，歌曲文件不会被删除。") },
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
