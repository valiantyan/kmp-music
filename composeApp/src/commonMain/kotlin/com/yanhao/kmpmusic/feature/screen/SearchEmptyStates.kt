package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Figma 空态提示用户输入关键词，避免空搜索直接铺满结果列表。
@Composable
internal fun SearchEmptySuggestion() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, bottom = 48.dp)
                .alpha(alpha = 0.4f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        SearchEmptyIcon()
        Text(
            text = "输入关键词开始探索\n美妙的音乐世界",
            color = searchEmptyTextColor,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

// 空态图标用现有 Material 图标组合，避免引入依赖本地 Figma 服务的运行时资源。
@Composable
private fun SearchEmptyIcon() {
    Box(
        modifier = Modifier.size(size = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(size = 54.dp),
            tint = searchEmptyIconColor,
        )
        Box(
            modifier =
                Modifier
                    .align(alignment = Alignment.BottomStart)
                    .size(size = 26.dp)
                    .clip(shape = CircleShape)
                    .background(color = searchBackgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(size = 18.dp),
                tint = searchEmptyIconColor,
            )
        }
    }
}

// 无结果文案保持轻量，不引入额外卡片以免偏离 Figma 空白节奏。
@Composable
internal fun SearchNoResultState(message: String) {
    Text(
        text = message,
        color = searchPlaceholderTextColor,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
        textAlign = TextAlign.Center,
    )
}
