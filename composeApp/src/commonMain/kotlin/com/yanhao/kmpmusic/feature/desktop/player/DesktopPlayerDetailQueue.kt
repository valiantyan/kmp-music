package com.yanhao.kmpmusic.feature.desktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

/**
 * 桌面播放页队列行状态。
 *
 * @property song 行内展示的歌曲。
 * @property queueIndex [song] 在共享播放队列中的真实下标。
 */
internal data class DesktopPlayerQueueRowState(
    val song: Song,
    val queueIndex: Int,
)

// 队列预览用单个轻量分组承载，避免播放页变成多层卡片。
@Composable
internal fun DesktopPlayerQueuePreview(
    queueRows: List<DesktopPlayerQueueRowState>,
    currentSongId: String,
    onQueueIndexClick: (Int) -> Unit,
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
            itemsIndexed(items = queueRows) { index: Int, row: DesktopPlayerQueueRowState ->
                DesktopPlayerQueueRow(
                    displayIndex = index,
                    song = row.song,
                    isCurrentSong = row.song.id == currentSongId,
                    onClick = { onQueueIndexClick(row.queueIndex) },
                )
            }
            if (queueRows.isEmpty()) {
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
    displayIndex: Int,
    song: Song,
    isCurrentSong: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (displayIndex + 1).toString().padStart(length = 2, padChar = '0'),
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

/**
 * 队列按共享播放队列原始顺序展示，同时保留原始队列下标供点击播放使用。
 */
internal fun buildPlayerQueueRowStates(
    queueSongs: List<Song>,
): List<DesktopPlayerQueueRowState> {
    return queueSongs.mapIndexed { index: Int, queueSong: Song ->
        DesktopPlayerQueueRowState(
            song = queueSong,
            queueIndex = index,
        )
    }
}
