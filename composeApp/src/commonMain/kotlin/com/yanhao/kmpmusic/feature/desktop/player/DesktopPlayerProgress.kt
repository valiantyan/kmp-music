package com.yanhao.kmpmusic.feature.desktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

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

// 播放时间统一按 mm:ss 输出，避免未知时长或负值显示异常。
internal fun formatDesktopPlayerTime(valueMs: Long): String {
    val totalSeconds: Long = (valueMs / 1000L).coerceAtLeast(minimumValue = 0L)
    val minutes: Long = totalSeconds / 60L
    val seconds: Long = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(length = 2, padChar = '0')}"
}

// 时间文本集中设置，保证进度条两端对齐一致。
@Composable
internal fun DesktopPlayerTimeText(text: String) {
    Text(
        text = text,
        color = DesktopMusicColors.MutedStrong,
        fontSize = DesktopMusicType.Body,
        fontWeight = FontWeight.SemiBold,
    )
}
