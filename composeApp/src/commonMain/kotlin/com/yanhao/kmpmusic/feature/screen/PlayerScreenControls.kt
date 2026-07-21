package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.domain.model.PlaybackMode

// 主控制区沿用现有行为，但按 Figma 强化中心播放按钮。
@Composable
internal fun PlayerControlRow(
    isPlaying: Boolean,
    playbackMode: PlaybackMode,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerSmallControlButton(
            icon = playbackMode.toPlayerModeIcon(),
            contentDescription = playbackMode.toPlayerModeDescription(),
            onClick = onMode,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = scaledDp(26.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerSmallControlButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = "上一首",
                onClick = onPrev,
            )
            PlayerPrimaryControlButton(
                isPlaying = isPlaying,
                onToggle = onToggle,
            )
            PlayerSmallControlButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = "下一首",
                onClick = onNext,
            )
        }
        PlayerSmallControlButton(
            icon = Icons.AutoMirrored.Rounded.List,
            contentDescription = "播放队列",
            onClick = onQueue,
        )
    }
}

// 小控制按钮固定命中区域，视觉图标保持克制。
@Composable
private fun PlayerSmallControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(size = scaledDp(44.dp)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MusicColors.Ink.copy(alpha = 0.78f),
            modifier = Modifier.size(size = scaledDp(22.dp)),
        )
    }
}

// 中心播放按钮使用品牌色圆形，符合播放页最高优先级动作。
@Composable
private fun PlayerPrimaryControlButton(
    isPlaying: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.size(size = scaledDp(80.dp)),
        shape = CircleShape,
        color = MusicColors.Accent,
        shadowElevation = scaledDp(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(size = scaledDp(80.dp))
                    .background(color = Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(size = scaledDp(34.dp)),
            )
        }
    }
}

// 播放模式图标跟随现有枚举，不引入新的状态模型。
private fun PlaybackMode.toPlayerModeIcon(): ImageVector =
    when (this) {
        PlaybackMode.LoopAll -> Icons.Rounded.Repeat
        PlaybackMode.LoopOne -> Icons.Rounded.RepeatOne
        PlaybackMode.Shuffle -> Icons.Rounded.Shuffle
    }

// 播放模式描述保持无障碍文案和现有语义一致。
private fun PlaybackMode.toPlayerModeDescription(): String =
    when (this) {
        PlaybackMode.LoopAll -> "顺序播放"
        PlaybackMode.LoopOne -> "单曲循环"
        PlaybackMode.Shuffle -> "随机播放"
    }
