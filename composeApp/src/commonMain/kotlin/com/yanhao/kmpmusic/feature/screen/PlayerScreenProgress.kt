package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp

// 进度区使用自绘轨道贴近 Figma，同时保留点击和拖动 seek。
@Composable
internal fun PlayerProgress(
    value: Float,
    durationMs: Long,
    playbackPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressFraction: Float = calculatePlayerProgressFraction(
        value = value,
        durationMs = durationMs,
    )
    Box(
        modifier = modifier
            .height(height = scaledDp(30.dp))
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    seekToPlayerProgressOffset(
                        offsetX = offset.x,
                        width = size.width,
                        durationMs = durationMs,
                        onSeek = onSeek,
                    )
                }
            }
            .pointerInput(durationMs) {
                detectDragGestures(
                    onDragStart = { offset ->
                        seekToPlayerProgressOffset(
                            offsetX = offset.x,
                            width = size.width,
                            durationMs = durationMs,
                            onSeek = onSeek,
                        )
                    },
                    onDrag = { change, _ ->
                        seekToPlayerProgressOffset(
                            offsetX = change.position.x,
                            width = size.width,
                            durationMs = durationMs,
                            onSeek = onSeek,
                        )
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = scaledDp(6.dp))
                .clip(shape = CircleShape)
                .background(color = MusicColors.Accent.copy(alpha = 0.12f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progressFraction)
                .height(height = scaledDp(6.dp))
                .clip(shape = CircleShape)
                .background(color = MusicColors.Accent),
        )
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PlayerTimeText(text = formatPlaybackTime(positionMs = playbackPositionMs))
        PlayerTimeText(text = formatPlaybackTime(positionMs = durationMs))
    }
}

// 时间文本固定层级，避免比歌曲信息更抢眼。
@Composable
private fun PlayerTimeText(text: String) {
    Text(
        text = text,
        color = MusicColors.Ink.copy(alpha = 0.54f),
        fontSize = scaledSp(12.sp),
        lineHeight = scaledSp(16.sp),
        fontWeight = FontWeight.Medium,
    )
}

// 进度比例需要容错未知时长，避免自绘轨道出现 NaN 宽度。
private fun calculatePlayerProgressFraction(
    value: Float,
    durationMs: Long,
): Float {
    if (durationMs <= 0L) {
        return 0f
    }
    return (value / durationMs.toFloat()).coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
    )
}

// 把触控横坐标映射到播放进度，宽度为 0 时直接忽略。
private fun seekToPlayerProgressOffset(
    offsetX: Float,
    width: Int,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    if (width <= 0 || durationMs <= 0L) {
        return
    }
    val fraction: Float = (offsetX / width.toFloat()).coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
    )
    onSeek((durationMs * fraction).toLong())
}
