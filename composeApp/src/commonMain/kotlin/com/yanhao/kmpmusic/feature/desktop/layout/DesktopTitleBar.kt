package com.yanhao.kmpmusic.feature.desktop.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens

/**
 * 桌面标题栏只保留原生窗口按钮和拖拽区背景，页面搜索交给首页自身渲染。
 */
@Composable
fun DesktopTitleBar(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(DesktopMusicDimens.TitleBarHeight)
                .border(width = 1.dp, color = Color(0xB8C7CFD6)),
    ) {
        Box(
            modifier =
                Modifier
                    .width(DesktopMusicDimens.RailWidth)
                    .fillMaxHeight()
                    .background(Color(0xFFF0F3FF)),
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFF9F9FF)),
        )
    }
}
