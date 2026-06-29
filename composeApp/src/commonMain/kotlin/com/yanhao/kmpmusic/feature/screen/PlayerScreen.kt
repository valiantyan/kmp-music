package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.userMessage
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.CoverArtImage

/**
 * 播放页，提供封面、进度、控制和队列入口。
 */
@Composable
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    playbackMode: PlaybackMode,
    playbackError: PlaybackError?,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onMode: () -> Unit,
    onLike: (String) -> Unit,
    onQueue: () -> Unit,
) {
    val duration: Long = playbackDurationMs ?: song.durationMs ?: 0L
    val safeProgress: Float = if (duration > 0L) {
        playbackPositionMs.coerceIn(minimumValue = 0L, maximumValue = duration).toFloat()
    } else {
        0f
    }
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        AppHeader(title = "正在播放", onBack = onBack)
        CoverArtImage(
            coverArt = song.coverArt,
            coverImageUri = song.coverImageUri,
            contentDescription = "${song.title} 封面",
            modifier = Modifier.size(328.dp).clip(RoundedCornerShape(30.dp)).align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Crop,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    text = song.title,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = "${song.artist} · ${song.album}", color = MusicColors.Muted, fontSize = 15.sp)
            }
            IconButton(onClick = { onLike(song.id) }) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "收藏当前歌曲",
                    tint = if (song.isLiked) MusicColors.Accent else MusicColors.Muted,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Slider(
                value = safeProgress,
                onValueChange = { value: Float -> onSeek(value.toLong()) },
                valueRange = 0f..duration.coerceAtLeast(minimumValue = 1L).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = formatPlaybackTime(playbackPositionMs), color = MusicColors.Muted, fontSize = 12.sp)
                Text(text = formatPlaybackTime(duration), color = MusicColors.Muted, fontSize = 12.sp)
            }
        }
        if (playbackError != null) {
            Text(
                text = playbackError.userMessage(songTitle = song.title),
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.size(48.dp))
            IconButton(onClick = onPrev) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首") }
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.onBackground) {
                IconButton(onClick = onToggle, modifier = Modifier.size(68.dp)) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (isPlaying) "暂停" else "播放", tint = MaterialTheme.colorScheme.background)
                }
            }
            IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, contentDescription = "下一首") }
            IconButton(onClick = onMode) {
                Icon(
                    imageVector = when (playbackMode) {
                        PlaybackMode.LoopAll -> Icons.Rounded.Repeat
                        PlaybackMode.LoopOne -> Icons.Rounded.RepeatOne
                        PlaybackMode.Shuffle -> Icons.Rounded.Shuffle
                    },
                    contentDescription = when (playbackMode) {
                        PlaybackMode.LoopAll -> "顺序播放"
                        PlaybackMode.LoopOne -> "单曲循环"
                        PlaybackMode.Shuffle -> "随机播放"
                    },
                )
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = MusicColors.Paper, tonalElevation = 1.dp) {
            Column(modifier = Modifier.fillMaxWidth().size(width = 1.dp, height = 96.dp), verticalArrangement = Arrangement.Center) {
                Text(text = song.quality, color = MusicColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = song.lyric, color = MusicColors.Muted, fontSize = 14.sp)
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = MusicColors.Soft, onClick = onQueue) {
            Row(modifier = Modifier.fillMaxWidth().size(width = 1.dp, height = 56.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null)
                Text(text = "播放队列", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "›", color = MusicColors.Muted, fontSize = 24.sp)
            }
        }
    }
}

// 播放时间统一按 mm:ss 输出，避免未知时长和拖动进度出现负值文本。
private fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds: Long = (positionMs / 1_000L).coerceAtLeast(minimumValue = 0L)
    val minutes: Long = totalSeconds / 60L
    val seconds: Long = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(length = 2, padChar = '0')}"
}
