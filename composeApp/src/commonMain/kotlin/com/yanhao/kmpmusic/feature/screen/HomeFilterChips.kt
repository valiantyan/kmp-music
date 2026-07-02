package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
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

// 分段 chip 先复刻歌曲页视觉，后续再接入专辑、歌手和文件夹页面状态。
@Composable
internal fun HomeFilterChips() {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "songs-chip") {
            HomeFilterChip(label = "歌曲", selected = true)
        }
        item(key = "albums-chip") {
            HomeFilterChip(label = "专辑", selected = false)
        }
        item(key = "artists-chip") {
            HomeFilterChip(label = "歌手", selected = false)
        }
        item(key = "folders-chip") {
            HomeFilterChip(label = "文件夹", selected = false)
        }
    }
}

// 单个 chip 使用固定高度和圆角，避免未来文字变化撑动首屏节奏。
@Composable
private fun HomeFilterChip(
    label: String,
    selected: Boolean,
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) homeAccentColor else homeChipColor)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFF3D4947),
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}
