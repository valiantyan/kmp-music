package com.yanhao.kmpmusic.feature.desktop.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

@Composable
fun DesktopPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val content: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .background(
                    brush = resolveDesktopPrimaryButtonBrush(enabled = enabled),
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (enabled) Color.White else DesktopMusicColors.MutedStrong,
                fontSize = DesktopMusicType.Eyebrow,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    if (enabled) {
        Surface(
            modifier = modifier.height(DesktopMusicDimens.PrimaryButtonHeight),
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent,
            onClick = onClick,
        ) {
            content()
        }
        return
    }
    Surface(
        modifier = modifier.height(DesktopMusicDimens.PrimaryButtonHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
    ) {
        content()
    }
}

// 主按钮置灰仍保留同样形状，避免空歌单详情页工具栏布局跳动。
private fun resolveDesktopPrimaryButtonBrush(enabled: Boolean): Brush {
    if (!enabled) {
        return Brush.verticalGradient(
            listOf(
                DesktopMusicColors.Muted.copy(alpha = 0.38f),
                DesktopMusicColors.Muted.copy(alpha = 0.32f),
            ),
        )
    }
    return Brush.verticalGradient(
        listOf(
            Color(0xFF1AC0A8),
            DesktopMusicColors.AccentDeep,
        ),
    )
}

@Composable
fun DesktopSecondaryButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(DesktopMusicDimens.PrimaryButtonHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.84f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DesktopMusicColors.Ink,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = text,
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.Eyebrow,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun DesktopMoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(DesktopMusicDimens.PrimaryButtonHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.84f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = "更多",
                tint = DesktopMusicColors.Ink,
            )
        }
    }
}

@Composable
fun DesktopSortButton(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(DesktopMusicDimens.PrimaryButtonHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.84f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.Eyebrow,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = DesktopMusicColors.Muted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * 小尺寸文本按钮专供紧凑区域的轻量操作，避免引入与主命令同级的视觉重量。
 */
@Composable
fun DesktopTinyTextButton(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = DesktopMusicColors.Muted,
            fontSize = DesktopMusicType.Body,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

val DesktopScanIcon: ImageVector
    get() = Icons.Rounded.Refresh
