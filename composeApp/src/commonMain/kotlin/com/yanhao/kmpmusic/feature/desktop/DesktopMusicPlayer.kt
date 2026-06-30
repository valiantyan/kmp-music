package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage

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

/**
 * 中间控制区只展示控制器回读的播放状态，避免桌面端出现第二套播放器逻辑。
 */
@Composable
private fun DesktopPlayerControls(
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
    val durationMs: Long = playbackDurationMs?.coerceAtLeast(minimumValue = 0L) ?: 0L
    val safePositionMs: Long = playbackPositionMs.coerceIn(
        minimumValue = 0L,
        maximumValue = durationMs.takeIf { value: Long -> value > 0L } ?: playbackPositionMs.coerceAtLeast(
            minimumValue = 0L,
        ),
    )
    val progressValue: Float = if (durationMs > 0L) {
        safePositionMs.toFloat()
    } else {
        0f
    }
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
        Text(
            text = formatTime(valueMs = safePositionMs),
            color = DesktopMusicColors.MutedStrong,
            fontSize = DesktopMusicType.Body,
        )
        DesktopThinSlider(
            value = progressValue,
            valueRange = 0f..durationMs.coerceAtLeast(minimumValue = 1L).toFloat(),
            enabled = durationMs > 0L,
            onValueChange = { value: Float -> onSeek(value.toLong()) },
            modifier = Modifier
                .weight(1f)
                .height(26.dp),
        )
        Text(
            text = formatTime(valueMs = durationMs),
            color = DesktopMusicColors.MutedStrong,
            fontSize = DesktopMusicType.Body,
        )
    }
}

/**
 * 桌面播放器细轨道滑杆，统一承接进度和音量，贴近原型里的轻量控件。
 */
@Composable
internal fun DesktopThinSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rangeSpan: Float = (valueRange.endInclusive - valueRange.start).coerceAtLeast(minimumValue = 1f)
    val progressFraction: Float = ((value - valueRange.start) / rangeSpan).coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
    )
    val updateFromX: (Float, Float) -> Unit = { positionX: Float, width: Float ->
        if (enabled && width > 0f) {
            val fraction: Float = (positionX / width).coerceIn(minimumValue = 0f, maximumValue = 1f)
            onValueChange(valueRange.start + rangeSpan * fraction)
        }
    }
    BoxWithConstraints(
        modifier = modifier
            .height(22.dp)
            .pointerInput(enabled, valueRange) {
                detectTapGestures { offset ->
                    updateFromX(offset.x, size.width.toFloat())
                }
            }
            .pointerInput(enabled, valueRange) {
                detectDragGestures(
                    onDragStart = { offset ->
                        updateFromX(offset.x, size.width.toFloat())
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        updateFromX(change.position.x, size.width.toFloat())
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(Color(0xFFDCE3E8)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progressFraction)
                .height(3.dp)
                .clip(CircleShape)
                .background(if (enabled) DesktopMusicColors.Accent else DesktopMusicColors.Accent.copy(alpha = 0.45f)),
        )
        Box(
            modifier = Modifier
                .offset(x = (maxWidth - 10.dp) * progressFraction)
                .size(10.dp)
                .clip(CircleShape)
                .background(if (enabled) DesktopMusicColors.Accent else DesktopMusicColors.Accent.copy(alpha = 0.45f)),
        )
    }
}

/**
 * 把播放模式映射为底栏图标和文案，顺序播放使用循环图标表示队列有序循环。
 */
private fun PlaybackMode.toPlaybackModeIcon(): PlaybackModeIcon {
    return when (this) {
        PlaybackMode.LoopAll -> PlaybackModeIcon(
            imageVector = Icons.Rounded.Repeat,
            contentDescription = "顺序播放",
        )
        PlaybackMode.LoopOne -> PlaybackModeIcon(
            imageVector = Icons.Rounded.RepeatOne,
            contentDescription = "单曲循环",
        )
        PlaybackMode.Shuffle -> PlaybackModeIcon(
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

/**
 * 底部栏统一使用分:秒格式，保证空态和负值都安全回落到 0。
 */
private fun formatTime(valueMs: Long): String {
    val totalSeconds: Long = (valueMs / 1000).coerceAtLeast(minimumValue = 0L)
    val minutes: Long = totalSeconds / 60
    val seconds: Long = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(length = 2, padChar = '0')}"
}
