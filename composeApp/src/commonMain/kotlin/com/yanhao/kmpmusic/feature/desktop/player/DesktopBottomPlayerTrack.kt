package com.yanhao.kmpmusic.feature.desktop.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

/**
 * 左侧曲目信息区域在有歌和空态之间保持固定宽度，避免底部栏跳动。
 */
@Composable
internal fun DesktopPlayerTrack(
    song: Song?,
    onOpen: () -> Unit,
    onLike: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .width(DesktopMusicDimens.PlayerTrackColumnWidth)
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (song != null) {
            CoverArtImage(
                coverArt = song.coverArt,
                coverImageUri = song.coverImageUri,
                contentDescription = "${song.title} 封面",
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.Body,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    color = DesktopMusicColors.Muted,
                    fontSize = DesktopMusicType.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DesktopPlayerTrackActions(
                isLiked = song.isLiked,
                onOpen = onOpen,
                onLike = { onLike(song.id) },
            )
            return
        }
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DesktopMusicColors.Soft),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "暂无播放",
                color = DesktopMusicColors.Ink,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "播放一首本地歌曲后会显示在这里",
                color = DesktopMusicColors.Muted,
                fontSize = DesktopMusicType.Body,
            )
        }
    }
}

// 曲目区动作保持紧凑，避免新增播放页入口挤压中央播放控制。
@Composable
private fun DesktopPlayerTrackActions(
    isLiked: Boolean,
    onOpen: () -> Unit,
    onLike: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopOpenPlayerButton(onOpen = onOpen)
        IconButton(
            onClick = onLike,
            modifier = Modifier.size(38.dp),
        ) {
            Icon(
                imageVector = if (isLiked) {
                    Icons.Rounded.Favorite
                } else {
                    Icons.Rounded.FavoriteBorder
                },
                contentDescription = if (isLiked) "取消收藏" else "收藏",
                tint = if (isLiked) DesktopMusicColors.PlayerRed else DesktopMusicColors.Ink,
            )
        }
    }
}

// 播放页入口使用独立圆形描边，避免和普通裸图标按钮混淆。
@Composable
private fun DesktopOpenPlayerButton(onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
        onClick = onOpen,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = "打开播放页",
                tint = DesktopMusicColors.MutedStrong,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
