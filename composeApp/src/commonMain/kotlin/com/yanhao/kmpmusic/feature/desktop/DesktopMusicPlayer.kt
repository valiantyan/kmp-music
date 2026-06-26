package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import com.yanhao.kmpmusic.feature.components.coverArtPainter

/**
 * 桌面端底部播放器，直接复用控制器中的真实播放状态与动作。
 */
@Composable
fun DesktopBottomPlayer(
    song: Song?,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onLike: (String) -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.PlayerHeight)
            .background(Color.White.copy(alpha = 0.86f))
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopPlayerTrack(
            song = song,
            onOpen = onOpen,
            onLike = onLike,
        )
        DesktopPlayerControls(
            isPlaying = isPlaying,
            playbackPositionMs = playbackPositionMs,
            playbackDurationMs = playbackDurationMs,
            onToggle = onToggle,
            onPrev = onPrev,
            onNext = onNext,
            onMode = onMode,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.width(DesktopMusicDimens.PlayerActionsColumnWidth),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "♩",
                fontSize = DesktopMusicType.PageTitle,
                color = DesktopMusicColors.Ink,
            )
            Box(
                modifier = Modifier
                    .width(118.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(DesktopMusicColors.Accent),
            )
            IconButton(onClick = onQueue) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "播放队列",
                    tint = DesktopMusicColors.Ink,
                )
            }
        }
    }
}

/**
 * 左侧曲目信息区域在有歌和空态之间保持固定宽度，避免底部栏跳动。
 */
@Composable
private fun DesktopPlayerTrack(
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
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                onClick = onOpen,
            ) {
                Image(
                    painter = coverArtPainter(song.coverArt),
                    contentDescription = "${song.title} 封面",
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = DesktopMusicColors.Ink,
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
            IconButton(onClick = { onLike(song.id) }) {
                Icon(
                    imageVector = if (song.isLiked) {
                        Icons.Rounded.Favorite
                    } else {
                        Icons.Rounded.FavoriteBorder
                    },
                    contentDescription = if (song.isLiked) "取消收藏" else "收藏",
                    tint = if (song.isLiked) DesktopMusicColors.PlayerRed else DesktopMusicColors.Ink,
                )
            }
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

/**
 * 中间控制区只展示控制器回读的播放状态，避免桌面端出现第二套播放器逻辑。
 */
@Composable
private fun DesktopPlayerControls(
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMode) {
            Icon(
                imageVector = Icons.Rounded.Repeat,
                contentDescription = "播放模式",
                tint = DesktopMusicColors.Accent,
            )
        }
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "上一首",
                tint = DesktopMusicColors.Ink,
            )
        }
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = DesktopMusicColors.Ink,
            onClick = onToggle,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) {
                        Icons.Rounded.Pause
                    } else {
                        Icons.Rounded.PlayArrow
                    },
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "下一首",
                tint = DesktopMusicColors.Ink,
            )
        }
        DesktopProgressText(
            positionMs = playbackPositionMs,
            durationMs = playbackDurationMs,
        )
    }
}

/**
 * 进度文本只负责把真实播放时间格式化为桌面原型需要的文案。
 */
@Composable
private fun DesktopProgressText(
    positionMs: Long,
    durationMs: Long?,
) {
    Text(
        text = "${formatTime(valueMs = positionMs)} / ${formatTime(valueMs = durationMs ?: 0L)}",
        color = DesktopMusicColors.Ink,
        fontSize = DesktopMusicType.Body,
    )
}

/**
 * 底部栏统一使用分:秒格式，保证空态和负值都安全回落到 0。
 */
private fun formatTime(valueMs: Long): String {
    val totalSeconds: Long = (valueMs / 1000).coerceAtLeast(minimumValue = 0L)
    val minutes: Long = totalSeconds / 60
    val seconds: Long = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(length = 2, padChar = '0')}"
}
