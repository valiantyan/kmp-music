package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 主按钮保留 Figma 的数量文案，数量来自当前歌手全部歌曲。
@Composable
internal fun ArtistDetailPlayAllButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 56.dp)
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(size = 12.dp),
        color = if (enabled) artistDetailActionColor else artistDetailActionColor.copy(alpha = 0.42f),
        shadowElevation = if (enabled) 6.dp else 0.dp,
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(size = 20.dp),
                tint = Color.White,
            )
            Spacer(modifier = Modifier.width(width = 8.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// 标题沿用 Figma “热门歌曲”文案，数据规则由 [ArtistDetailContent] 保证为全量歌曲。
@Composable
internal fun ArtistDetailSectionTitle() {
    Text(
        text = "热门歌曲",
        color = artistDetailTextColor,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(
            start = 20.dp,
            top = 32.dp,
            end = 20.dp,
            bottom = 16.dp,
        ),
    )
}
