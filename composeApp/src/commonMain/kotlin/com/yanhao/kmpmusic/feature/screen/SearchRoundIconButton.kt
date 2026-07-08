package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 圆形图标按钮去掉额外背景，保持 Figma 图标式操作按钮的轻量感。
@Composable
internal fun SearchRoundIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 40.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size = size)
            .clip(shape = CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier, contentAlignment = Alignment.Center) {
            content()
        }
    }
}
