package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Figma 专辑详情页主按钮圆角。
private val albumDetailActionShape: RoundedCornerShape = RoundedCornerShape(size = 16.dp)

/**
 * 专辑详情页播放全部按钮，同时表达队列入口和当前专辑歌曲数量。
 */
@Composable
internal fun AlbumDetailPlayAllButton(
    text: String,
    countText: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 32.dp, end = 18.dp)
            .height(height = 64.dp),
        shape = albumDetailActionShape,
        color = albumDetailActionColor.copy(alpha = if (enabled) 0.90f else 0.42f),
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumDetailPlayIcon()
            AlbumDetailPlayAllText(text = text)
            AlbumDetailPlayAllDivider()
            AlbumDetailPlayAllCount(countText = countText)
        }
    }
}

// 白色圆点让主按钮在深青色背景上保持足够识别度。
@Composable
private fun AlbumDetailPlayIcon() {
    Box(
        modifier = Modifier
            .size(size = 25.dp)
            .clip(shape = CircleShape)
            .background(color = Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = albumDetailActionColor,
            modifier = Modifier.size(size = 18.dp),
        )
    }
}

// 主行动文案保持单行，窄屏时优先让数量仍可见。
@Composable
private fun AlbumDetailPlayAllText(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 12.dp),
    )
}

// Figma 中播放文案和数量之间有一条半透明白色分隔线。
@Composable
private fun AlbumDetailPlayAllDivider() {
    Box(
        modifier = Modifier
            .padding(start = 12.dp)
            .width(width = 1.dp)
            .height(height = 24.dp)
            .background(color = Color.White.copy(alpha = 0.30f)),
    )
}

// 数量文案与“播放全部”共用按钮点击语义，不单独形成子按钮。
@Composable
private fun AlbumDetailPlayAllCount(countText: String) {
    Text(
        text = countText,
        color = Color.White,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 12.dp),
    )
}
