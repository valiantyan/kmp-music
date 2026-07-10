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
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * 新建歌单弹窗只处理名称校验和确认，成功后由控制器关闭整条添加流程。
 */
@Composable
internal fun CreatePlaylistDialog(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
) {
    Dialog(onDismissRequest = controller::closeAddToPlaylistFlow) {
        Surface(
            modifier = Modifier.widthIn(max = CreatePlaylistDialogDesignSpec.maxWidth),
            shape = RoundedCornerShape(size = CreatePlaylistDialogDesignSpec.cornerRadius),
            color = CreatePlaylistDialogDesignSpec.containerColor.copy(
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
                    flow = flow,
                    controller = controller,
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
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().height(height = CreatePlaylistDialogDesignSpec.inputHeight),
        value = flow.newPlaylistName,
        onValueChange = controller::setNewPlaylistName,
        singleLine = true,
        isError = flow.newPlaylistNameError != null,
        shape = RoundedCornerShape(size = CreatePlaylistDialogDesignSpec.inputRadius),
        placeholder = { Text(text = "请输入歌单名称") },
    )
}

/**
 * 新建弹窗底部动作保持在自绘容器内，避免回退到默认 [androidx.compose.material3.AlertDialog] 视觉。
 */
@Composable
private fun RowActions(controller: MusicAppController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = CreatePlaylistDialogDesignSpec.actionGap),
    ) {
        TextButton(
            modifier = Modifier.weight(weight = 1f).height(height = CreatePlaylistDialogDesignSpec.buttonHeight),
            onClick = controller::closeAddToPlaylistFlow,
        ) {
            Text(text = "取消")
        }
        Button(
            modifier = Modifier.weight(weight = 1f).height(height = CreatePlaylistDialogDesignSpec.buttonHeight),
            shape = RoundedCornerShape(size = CreatePlaylistDialogDesignSpec.actionButtonRadius),
            onClick = controller::createPlaylistWithCurrentSong,
        ) {
            Text(text = "创建")
        }
    }
}

// 新建弹窗描边集中使用规格对象，避免颜色透明度散落在布局中。
private fun createPlaylistDialogBorder(): BorderStroke {
    return BorderStroke(
        width = CreatePlaylistDialogDesignSpec.borderWidth,
        color = CreatePlaylistDialogDesignSpec.containerColor.copy(
            alpha = CreatePlaylistDialogDesignSpec.borderAlpha,
        ),
    )
}
