package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 播放按钮在空态保留 Figma 形状和尺寸，但不消费点击。 */
@Composable
internal fun DesktopFavoritesActionButton(
    text: String,
    icon: ImageVector,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val buttonModifier: Modifier =
        Modifier
            .height(if (isPrimary) 48.dp else 50.dp)
            .then(
                if (isPrimary && enabled) {
                    Modifier.shadow(elevation = 4.dp, shape = CircleShape)
                } else {
                    Modifier
                },
            )
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = resolveFavoritesActionContentColor(isPrimary = isPrimary, enabled = enabled),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = text,
                color = resolveFavoritesActionContentColor(isPrimary = isPrimary, enabled = enabled),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
    if (enabled) {
        Surface(
            modifier = buttonModifier,
            shape = CircleShape,
            color = resolveFavoritesActionBackground(isPrimary = isPrimary, enabled = true),
            border = resolveFavoritesActionBorder(isPrimary = isPrimary),
            onClick = onClick,
            content = content,
        )
        return
    }
    Surface(
        modifier = buttonModifier,
        shape = CircleShape,
        color = resolveFavoritesActionBackground(isPrimary = isPrimary, enabled = false),
        border = resolveFavoritesActionBorder(isPrimary = isPrimary),
        content = content,
    )
}

/** 主按钮使用设计强调色，空态降低明度但不改变布局。 */
private fun resolveFavoritesActionBackground(
    isPrimary: Boolean,
    enabled: Boolean,
): Color {
    if (!isPrimary) {
        return Color.Transparent
    }
    return if (enabled) Color(0xFF00BFA5) else Color(0xFFCAE4DF)
}

/** 次按钮保留 Figma 的轻边框，主按钮依靠填充和阴影区分层级。 */
private fun resolveFavoritesActionBorder(isPrimary: Boolean): BorderStroke? =
    if (isPrimary) {
        null
    } else {
        BorderStroke(width = 1.dp, color = Color(0x4DBBCAC4))
    }

/** 空态按钮使用统一的低对比文字，避免看起来仍可操作。 */
private fun resolveFavoritesActionContentColor(
    isPrimary: Boolean,
    enabled: Boolean,
): Color {
    if (!enabled) {
        return Color(0xFF70817C)
    }
    return if (isPrimary) Color(0xFF00473C) else Color(0xFF111C2D)
}
