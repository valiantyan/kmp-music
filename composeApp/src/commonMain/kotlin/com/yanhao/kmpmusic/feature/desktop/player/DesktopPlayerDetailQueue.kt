package com.yanhao.kmpmusic.feature.desktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

// 队列预览用单个轻量分组承载，避免播放页变成多层卡片。
@Composable
internal fun DesktopPlayerQueuePreview(
    queueSongs: List<Song>,
    currentSongId: String,
) {
    Column {
        Text(
            text = "播放队列",
            color = DesktopMusicColors.Ink,
            fontSize = DesktopMusicType.SidebarTitle,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(174.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.58f)),
        ) {
            itemsIndexed(items = queueSongs) { index: Int, song: Song ->
                DesktopPlayerQueueRow(
                    index = index,
                    song = song,
                    isCurrentSong = song.id == currentSongId,
                )
            }
            if (queueSongs.isEmpty()) {
                item {
                    Text(
                        text = "当前队列没有更多歌曲",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        color = DesktopMusicColors.Muted,
                        fontSize = DesktopMusicType.Body,
                    )
                }
            }
        }
    }
}

// 队列行保持紧凑高度，让右侧控制区仍以当前歌曲为视觉中心。
@Composable
private fun DesktopPlayerQueueRow(
    index: Int,
    song: Song,
    isCurrentSong: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (index + 1).toString().padStart(length = 2, padChar = '0'),
            color = DesktopMusicColors.Muted,
            fontSize = DesktopMusicType.Body,
            fontWeight = FontWeight.Bold,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.Body,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                color = DesktopMusicColors.Muted,
                fontSize = DesktopMusicType.TableHeader,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatDesktopPlayerTime(valueMs = song.durationMs ?: 0L),
            color = DesktopMusicColors.Muted,
            fontSize = DesktopMusicType.Body,
        )
    }
}

// 队列从当前歌曲开始展示完整播放顺序，列表本身负责滚动承载全部数据。
internal fun buildPlayerQueueRows(
    song: Song,
    queueSongs: List<Song>,
): List<Song> {
    if (queueSongs.isEmpty()) {
        return emptyList()
    }
    val currentIndex: Int = queueSongs.indexOfFirst { candidate: Song -> candidate.id == song.id }
    if (currentIndex < 0) {
        return queueSongs
    }
    return queueSongs.drop(n = currentIndex) + queueSongs.take(n = currentIndex)
}
