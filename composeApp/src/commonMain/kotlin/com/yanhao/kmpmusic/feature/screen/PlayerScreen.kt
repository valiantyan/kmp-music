package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.coverArtPainter

/**
 * 播放页，提供封面、进度、控制和队列入口。
 */
@Composable
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLike: (String) -> Unit,
    onQueue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        AppHeader(title = "正在播放", onBack = onBack)
        Image(
            painter = coverArtPainter(song.coverArt),
            contentDescription = "${song.title} 封面",
            modifier = Modifier.size(328.dp).clip(RoundedCornerShape(30.dp)).align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Crop,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(text = song.title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            LinearProgressIndicator(progress = { 0.38f }, modifier = Modifier.fillMaxWidth(), color = MusicColors.Accent)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "1:26", color = MusicColors.Muted, fontSize = 12.sp)
                Text(text = song.duration, color = MusicColors.Muted, fontSize = 12.sp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}) { Icon(Icons.Rounded.Shuffle, contentDescription = "随机播放") }
            IconButton(onClick = onPrev) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首") }
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.onBackground) {
                IconButton(onClick = onToggle, modifier = Modifier.size(68.dp)) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (isPlaying) "暂停" else "播放", tint = MaterialTheme.colorScheme.background)
                }
            }
            IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, contentDescription = "下一首") }
            IconButton(onClick = {}) { Icon(Icons.Rounded.Repeat, contentDescription = "循环播放") }
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
