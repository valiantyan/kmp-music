package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.feature.app.AddToPlaylistFlowState
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * 底部动作区固定在弹窗内容末尾，完成按钮状态直接反映是否已单选目标歌单。
 */
@Composable
internal fun AddToPlaylistFooter(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(height = AddToPlaylistDialogDesignSpec.footerHeight),
        horizontalArrangement = Arrangement.spacedBy(
            space = AddToPlaylistDialogDesignSpec.footerButtonGap,
            alignment = Alignment.End,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = controller::closeAddToPlaylistFlow) {
            Text(text = "取消")
        }
        Button(
            enabled = flow.canCompleteExistingPlaylist,
            onClick = controller::addCurrentSongToSelectedPlaylist,
        ) {
            Text(text = "完成")
        }
    }
}
