package com.yanhao.kmpmusic.feature.desktop.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

/**
 * 中间控制区只展示控制器回读的播放状态，避免桌面端出现第二套播放器逻辑。
 */
@Composable
internal fun DesktopPlayerControls(
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    playbackMode: PlaybackMode,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressModel: DesktopPlaybackProgressDisplayModel =
        buildDesktopPlaybackProgressDisplayModel(
            playbackPositionMs = playbackPositionMs,
            playbackDurationMs = playbackDurationMs,
            isPlaying = isPlaying,
        )
    val modeIcon: PlaybackModeIcon = playbackMode.toPlaybackModeIcon()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMode) {
            Icon(
                imageVector = modeIcon.imageVector,
                contentDescription = modeIcon.contentDescription,
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
                    imageVector =
                        if (isPlaying) {
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
        Text(
            text = formatDesktopPlayerTime(valueMs = progressModel.positionMs),
            color = DesktopMusicColors.MutedStrong,
            fontSize = DesktopMusicType.Body,
        )
        DesktopThinSlider(
            value = progressModel.sliderValue,
            valueRange = progressModel.sliderRange,
            enabled = progressModel.isSeekEnabled,
            onValueChange = { value: Float -> onSeek(value.toLong()) },
            modifier =
                Modifier
                    .weight(1f)
                    .height(26.dp),
        )
        Text(
            text = formatDesktopPlayerTime(valueMs = progressModel.durationMs),
            color = DesktopMusicColors.MutedStrong,
            fontSize = DesktopMusicType.Body,
        )
    }
}

/**
 * 把播放模式映射为底栏图标和文案，顺序播放使用循环图标表示队列有序循环。
 */
private fun PlaybackMode.toPlaybackModeIcon(): PlaybackModeIcon =
    when (this) {
        PlaybackMode.LoopAll -> {
            PlaybackModeIcon(
                imageVector = Icons.Rounded.Repeat,
                contentDescription = "顺序播放",
            )
        }

        PlaybackMode.LoopOne -> {
            PlaybackModeIcon(
                imageVector = Icons.Rounded.RepeatOne,
                contentDescription = "单曲循环",
            )
        }

        PlaybackMode.Shuffle -> {
            PlaybackModeIcon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "随机播放",
            )
        }
    }

/**
 * 播放模式按钮需要同时更新图标和可访问文案，封装后避免分支散落在 UI 中。
 */
private data class PlaybackModeIcon(
    val imageVector: ImageVector,
    val contentDescription: String,
)
