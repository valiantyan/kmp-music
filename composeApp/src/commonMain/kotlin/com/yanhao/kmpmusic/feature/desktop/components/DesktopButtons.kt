package com.yanhao.kmpmusic.feature.desktop.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

/**
 * 小尺寸文本按钮专供紧凑区域的轻量操作，避免引入与主命令同级的视觉重量。
 */
@Composable
fun DesktopTinyTextButton(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = DesktopMusicColors.Muted,
            fontSize = DesktopMusicType.Body,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}
