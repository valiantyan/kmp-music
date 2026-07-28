package com.yanhao.kmpmusic.feature.desktop.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

/**
 * 桌面播放倍速弹出菜单，锚定在触发按钮附近，选择后立即收起。
 */
@Composable
internal fun DesktopPlaybackSpeedMenu(
    selectedSpeed: PlaybackSpeed,
    onSpeedChange: (PlaybackSpeed) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded: Boolean by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            modifier =
                Modifier
                    .width(DesktopPlaybackSpeedMenuDesignSpec.triggerWidth)
                    .height(DesktopPlaybackSpeedMenuDesignSpec.triggerHeight),
            shape = RoundedCornerShape(size = DesktopPlaybackSpeedMenuDesignSpec.triggerCornerRadius),
            color = DesktopMusicColors.Soft,
            border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
            onClick = { isExpanded = true },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = DesktopPlaybackSpeedMenuDesignSpec.triggerHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(space = DesktopPlaybackSpeedMenuDesignSpec.triggerLabelGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = DesktopMusicColors.MutedStrong,
                    modifier = Modifier.size(size = DesktopPlaybackSpeedMenuDesignSpec.triggerIconSize),
                )
                Text(
                    text = formatDesktopPlaybackSpeedTriggerLabel(playbackSpeed = selectedSpeed),
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.Body,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    softWrap = false,
                    modifier = Modifier.width(width = DesktopPlaybackSpeedMenuDesignSpec.triggerLabelWidth),
                )
            }
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            PlaybackSpeed.entries.forEach { playbackSpeed: PlaybackSpeed ->
                DesktopPlaybackSpeedMenuItem(
                    playbackSpeed = playbackSpeed,
                    isSelected = playbackSpeed == selectedSpeed,
                    onClick = {
                        onSpeedChange(playbackSpeed)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

// 选中项保持同宽 leading 区域，避免切换时菜单文本横向跳动。
@Composable
private fun DesktopPlaybackSpeedMenuItem(
    playbackSpeed: PlaybackSpeed,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier =
            if (isSelected) {
                Modifier.background(color = DesktopMusicColors.AccentSoft)
            } else {
                Modifier
            },
        text = {
            Text(
                text = playbackSpeed.label,
                color = if (isSelected) DesktopMusicColors.AccentDeep else DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.Body,
                fontWeight = FontWeight.Bold,
            )
        },
        leadingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "已选中",
                    tint = DesktopMusicColors.AccentDeep,
                    modifier = Modifier.size(size = 18.dp),
                )
            } else {
                Spacer(modifier = Modifier.size(size = 18.dp))
            }
        },
        onClick = onClick,
    )
}

/**
 * 桌面倍速按钮尺寸锁住最长 `1.25x`/`0.75x`，避免在底栏和播放页被压成两行。
 */
internal object DesktopPlaybackSpeedMenuDesignSpec {
    /** 触发按钮宽度，给最长倍速文本保留完整单行空间。 */
    val triggerWidth: Dp = 96.dp

    /** 触发按钮高度。 */
    val triggerHeight: Dp = 36.dp

    /** 触发按钮圆角。 */
    val triggerCornerRadius: Dp = 10.dp

    /** 触发按钮左右内边距。 */
    val triggerHorizontalPadding: Dp = 10.dp

    /** 速度图标尺寸。 */
    val triggerIconSize: Dp = 17.dp

    /** 图标和倍速文本之间的间距。 */
    val triggerLabelGap: Dp = 6.dp

    /** 倍速文本固定宽度，防止不同倍率切换时跳动或换行。 */
    val triggerLabelWidth: Dp = 48.dp

    /** 单行展示所需的最小理论宽度。 */
    val minimumSingleLineWidth: Dp
        get() = triggerHorizontalPadding * 2 + triggerIconSize + triggerLabelGap + triggerLabelWidth
}

// 桌面触发按钮始终显示倍率单位，避免只看到裸数值。
internal fun formatDesktopPlaybackSpeedTriggerLabel(playbackSpeed: PlaybackSpeed): String = "${playbackSpeed.label}x"
