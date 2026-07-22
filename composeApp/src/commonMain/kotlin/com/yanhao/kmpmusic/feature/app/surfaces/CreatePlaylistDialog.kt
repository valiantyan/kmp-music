package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.feature.app.AddToPlaylistFlowState
import com.yanhao.kmpmusic.feature.app.EmptyPlaylistDialogState
import com.yanhao.kmpmusic.feature.app.MusicAppController

/** 新建歌单弹窗复用名称校验和确认 UI，具体创建语义由调用方控制器决定。 */
@Composable
internal fun CreatePlaylistDialog(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
) = PlaylistNameDialog(
    name = flow.newPlaylistName,
    nameError = flow.newPlaylistNameError,
    onDismiss = controller::closeAddToPlaylistFlow,
    onNameChange = controller::setNewPlaylistName,
    onCreate = controller::createPlaylistWithCurrentSong,
)

/** 歌单页创建空歌单时复用名称输入外观，但提交到不绑定歌曲的控制器动作。 */
@Composable
internal fun EmptyPlaylistDialog(
    dialog: EmptyPlaylistDialogState,
    controller: MusicAppController,
) = PlaylistNameDialog(
    name = dialog.name,
    nameError = dialog.nameError,
    onDismiss = controller::closeEmptyPlaylistDialog,
    onNameChange = controller::setEmptyPlaylistName,
    onCreate = controller::createEmptyPlaylist,
)

/** 名称输入弹窗只负责视觉和回调分发，让不同创建语义共享相同的校验呈现。 */
@Composable
private fun PlaylistNameDialog(
    name: String,
    nameError: String?,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = CreatePlaylistDialogDesignSpec.maxWidth),
            shape = RoundedCornerShape(size = CreatePlaylistDialogDesignSpec.cornerRadius),
            color =
                CreatePlaylistDialogDesignSpec.containerColor.copy(
                    alpha = CreatePlaylistDialogDesignSpec.backgroundAlpha,
                ),
            border = createPlaylistDialogBorder(),
        ) {
            Column(
                modifier = Modifier.padding(all = CreatePlaylistDialogDesignSpec.contentPadding),
                verticalArrangement = Arrangement.spacedBy(space = CreatePlaylistDialogDesignSpec.contentGap),
            ) {
                CreatePlaylistHeader()
                CreatePlaylistNameField(
                    name = name,
                    nameError = nameError,
                    onNameChange = onNameChange,
                )
                nameError?.let { message: String ->
                    Text(text = message, color = MusicColors.Danger)
                }
                PlaylistNameDialogActions(
                    onDismiss = onDismiss,
                    onCreate = onCreate,
                )
            }
        }
    }
}

/**
 * 新建弹窗标题和说明文案对应节点 974:672 的头部结构。
 */
@Composable
private fun CreatePlaylistHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(space = CreatePlaylistDialogDesignSpec.headerGap)) {
        Text(text = "新建歌单")
        Text(text = "为你的心情整理一个新的音乐集合", color = MusicColors.Muted)
    }
}

/**
 * 名称输入框保持 56dp 高度，便于错误文案出现时主体控件不跳动。
 */
@Composable
private fun CreatePlaylistNameField(
    name: String,
    nameError: String?,
    onNameChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().height(height = CreatePlaylistDialogDesignSpec.inputHeight),
        value = name,
        onValueChange = onNameChange,
        singleLine = true,
        isError = nameError != null,
        shape = RoundedCornerShape(size = CreatePlaylistDialogDesignSpec.inputRadius),
        placeholder = { Text(text = "请输入歌单名称") },
    )
}

/**
 * 新建弹窗底部动作保持在自绘容器内，避免回退到默认 [androidx.compose.material3.AlertDialog] 视觉。
 */
@Composable
private fun PlaylistNameDialogActions(
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = CreatePlaylistDialogDesignSpec.actionGap),
    ) {
        TextButton(
            modifier = Modifier.weight(weight = 1f).height(height = CreatePlaylistDialogDesignSpec.buttonHeight),
            onClick = onDismiss,
        ) {
            Text(text = "取消")
        }
        Button(
            modifier = Modifier.weight(weight = 1f).height(height = CreatePlaylistDialogDesignSpec.buttonHeight),
            shape = RoundedCornerShape(size = CreatePlaylistDialogDesignSpec.actionButtonRadius),
            onClick = onCreate,
        ) {
            Text(text = "创建")
        }
    }
}

// 新建弹窗描边集中使用规格对象，避免颜色透明度散落在布局中。
private fun createPlaylistDialogBorder(): BorderStroke =
    BorderStroke(
        width = CreatePlaylistDialogDesignSpec.borderWidth,
        color =
            CreatePlaylistDialogDesignSpec.containerColor.copy(
                alpha = CreatePlaylistDialogDesignSpec.borderAlpha,
            ),
    )
