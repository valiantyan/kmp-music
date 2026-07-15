package com.yanhao.kmpmusic.feature.desktop.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors

// 进度条始终使用真实播放时长，缺失时回退到歌曲元数据时长。
@Composable
internal fun DesktopPlayerProgress(
    song: Song,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    onSeek: (Long) -> Unit,
) {
    val progressModel: DesktopPlaybackProgressDisplayModel = buildDesktopPlaybackProgressDisplayModel(
        playbackPositionMs = playbackPositionMs,
        playbackDurationMs = playbackDurationMs,
        isPlaying = isPlaying,
        fallbackDurationMs = song.durationMs,
    )
    DesktopThinSlider(
        value = progressModel.sliderValue,
        valueRange = progressModel.sliderRange,
        enabled = progressModel.isSeekEnabled,
        onValueChange = { value: Float -> onSeek(value.toLong()) },
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DesktopPlayerTimeText(text = formatDesktopPlayerTime(valueMs = progressModel.positionMs))
        DesktopPlayerTimeText(text = formatDesktopPlayerTime(valueMs = progressModel.durationMs))
    }
}

// 主控制区只放播放核心动作，模式和切歌使用相同触控尺寸保证布局稳定。
@Composable
internal fun DesktopPlayerControlRow(
    song: Song,
    isPlaying: Boolean,
    playbackMode: PlaybackMode,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onLike: (String) -> Unit,
) {
    val modeIcon: DesktopPlayerModeIcon = playbackMode.toDesktopPlayerModeIcon()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopRoundIconButton(
            icon = if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (song.isLiked) "取消收藏" else "收藏",
            tint = if (song.isLiked) DesktopMusicColors.PlayerRed else DesktopMusicColors.Ink,
            onClick = { onLike(song.id) },
        )
        DesktopRoundIconButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "上一首",
            onClick = onPrev,
        )
        Surface(
            modifier = Modifier.size(74.dp),
            shape = CircleShape,
            color = DesktopMusicColors.Ink,
            onClick = onToggle,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        DesktopRoundIconButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "下一首",
            onClick = onNext,
        )
        DesktopRoundIconButton(
            icon = modeIcon.icon,
            contentDescription = modeIcon.contentDescription,
            tint = DesktopMusicColors.Accent,
            onClick = onMode,
        )
    }
}

// 播放模式映射集中在播放页内部，避免 UI 分支散落在控件调用处。
private fun PlaybackMode.toDesktopPlayerModeIcon(): DesktopPlayerModeIcon {
    return when (this) {
        PlaybackMode.LoopAll -> DesktopPlayerModeIcon(
            icon = Icons.Rounded.Repeat,
            contentDescription = "顺序播放",
        )
        PlaybackMode.LoopOne -> DesktopPlayerModeIcon(
            icon = Icons.Rounded.RepeatOne,
            contentDescription = "单曲循环",
        )
        PlaybackMode.Shuffle -> DesktopPlayerModeIcon(
            icon = Icons.Rounded.Shuffle,
            contentDescription = "随机播放",
        )
    }
}

// 播放模式图标和文案作为一组返回，保证可访问文案随图标同步变化。
private data class DesktopPlayerModeIcon(
    val icon: ImageVector,
    val contentDescription: String,
)
