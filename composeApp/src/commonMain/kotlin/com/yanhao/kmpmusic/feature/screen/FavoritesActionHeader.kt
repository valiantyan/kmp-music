package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.feature.app.FavoriteSection

/**
 * 收藏页操作栏，保持 Figma 的播放全部按钮和右侧分段入口。
 */
@Composable
internal fun FavoritesActionHeader(
    songCount: Int,
    section: FavoriteSection,
    onPlayAll: () -> Unit,
    onSection: (FavoriteSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FavoritesPlayAllButton(
            songCount = songCount,
            onPlayAll = onPlayAll,
            modifier = Modifier.weight(weight = 1f, fill = false),
        )
        FavoritesSectionButton(
            section = section,
            onSection = onSection,
        )
    }
}

// 播放全部按钮用内容宽度贴近 Figma 156px，同时支持三位数收藏数量自然扩展。
@Composable
private fun FavoritesPlayAllButton(
    songCount: Int,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(height = favoritesActionHeaderHeight)
            .widthIn(min = 156.dp),
        shape = RoundedCornerShape(size = 16.dp),
        color = favoritesActionColor,
        shadowElevation = 1.dp,
        onClick = onPlayAll,
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(width = 14.dp, height = 14.dp),
                tint = Color.White,
            )
            Text(
                text = "播放全部",
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "($songCount)",
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// Figma 右侧筛选图标在当前信息架构中承载歌曲/专辑/歌手分段切换。
@Composable
private fun FavoritesSectionButton(
    section: FavoriteSection,
    onSection: (FavoriteSection) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size = favoritesSongActionSize)
            .clip(shape = CircleShape)
            .clickable { onSection(section.next()) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Sort,
            contentDescription = "切换到${section.next().label()}收藏",
            modifier = Modifier.size(width = 18.dp, height = 18.dp),
            tint = favoritesTextColor,
        )
    }
}

// 收藏页右上角分段按钮按固定顺序循环，避免引入非 Figma 的 chip 行。
private fun FavoriteSection.next(): FavoriteSection {
    return when (this) {
        FavoriteSection.Songs -> FavoriteSection.Albums
        FavoriteSection.Albums -> FavoriteSection.Artists
        FavoriteSection.Artists -> FavoriteSection.Songs
    }
}

// 分段文案只用于无障碍描述，不在 Figma 页面中额外显示。
private fun FavoriteSection.label(): String {
    return when (this) {
        FavoriteSection.Songs -> "歌曲"
        FavoriteSection.Albums -> "专辑"
        FavoriteSection.Artists -> "歌手"
    }
}
