package com.yanhao.kmpmusic.feature.desktop.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

/**
 * 最近播放与局部内容区为空时的轻提示，避免用全库内容冒充当前分段数据。
 */
@Composable
fun DesktopSectionEmptyMessage(
    message: String,
) {
    Text(
        text = message,
        color = DesktopMusicColors.MutedStrong,
        fontSize = DesktopMusicType.Eyebrow,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
