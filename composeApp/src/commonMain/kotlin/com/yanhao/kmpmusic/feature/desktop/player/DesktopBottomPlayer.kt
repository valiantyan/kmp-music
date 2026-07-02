package com.yanhao.kmpmusic.feature.desktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens

/**
 * 桌面端底部播放器，直接复用控制器中的真实播放状态与动作。
 */
@Composable
fun DesktopBottomPlayer(
    song: Song?,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    playbackMode: PlaybackMode,
    volume: Float,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onLike: (String) -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
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
            playbackMode = playbackMode,
            onToggle = onToggle,
            onPrev = onPrev,
            onNext = onNext,
            onMode = onMode,
            onSeek = onSeek,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.width(DesktopMusicDimens.PlayerActionsColumnWidth),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "音量",
                tint = DesktopMusicColors.MutedStrong,
                modifier = Modifier.size(22.dp),
            )
            DesktopThinSlider(
                value = volume,
                valueRange = 0f..1f,
                enabled = true,
                onValueChange = onVolumeChange,
                modifier = Modifier.width(92.dp),
            )
            IconButton(onClick = onQueue) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "播放队列",
                    tint = DesktopMusicColors.MutedStrong,
                )
            }
        }
    }
}
