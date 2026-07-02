package com.yanhao.kmpmusic.feature.desktop.layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

/**
 * 桌面标题栏保留全局标题和可选搜索入口，避免桌面布局入口混入具体视觉实现。
 */
@Composable
fun DesktopTitleBar(
    showSearch: Boolean,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.TitleBarHeight)
            .background(Color(0xB8F7F9FB))
            .border(width = 1.dp, color = Color(0xB8C7CFD6)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .width(DesktopMusicDimens.RailWidth)
                .padding(start = 18.dp),
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "KMP Music",
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.AppTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        if (showSearch) {
            Surface(
                modifier = Modifier
                    .width(520.dp)
                    .height(30.dp)
                    .padding(end = 18.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.84f),
                border = BorderStroke(width = 1.dp, color = Color(0xFFD7DDE3)),
                onClick = onSearch,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color(0xFF8A95A3),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "搜索歌曲、专辑、歌手",
                        color = Color(0xFF8A95A3),
                        fontSize = DesktopMusicType.Body,
                    )
                }
            }
        } else {
            Spacer(
                modifier = Modifier
                    .width(520.dp)
                    .height(30.dp)
                    .padding(end = 18.dp),
            )
        }
    }
}
