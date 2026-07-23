package com.yanhao.kmpmusic.feature.desktop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.desktop.DesktopNavigationToolbarTokens

/** 返回 toolbar 的页面背景模式，悬浮详情页与普通二级页共用返回语义。 */
enum class DesktopBackTitleToolbarStyle {
    Standard,
    Overlay,
    Content,
}

/**
 * Desktop 返回式二级页顶栏，统一搜索和管理歌单的返回热区、标题位置与稳定高度。
 */
@Composable
fun DesktopBackTitleToolbar(
    title: String?,
    onBack: () -> Unit,
    style: DesktopBackTitleToolbarStyle = DesktopBackTitleToolbarStyle.Standard,
    contentBackgroundColor: Color = Color.Transparent,
    modifier: Modifier = Modifier,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isHovered: Boolean by interactionSource.collectIsHoveredAsState()
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height = DesktopNavigationToolbarTokens.Height)
                .background(
                    color =
                        resolveDesktopBackTitleToolbarBackground(
                            style = style,
                            contentBackgroundColor = contentBackgroundColor,
                        ),
                ).padding(horizontal = DesktopNavigationToolbarTokens.HorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier =
                Modifier
                    .size(size = 32.dp)
                    .hoverable(interactionSource = interactionSource)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onBack,
                    ),
            shape = CircleShape,
            color =
                resolveDesktopBackTitleToolbarButtonBackground(
                    style = style,
                    isHovered = isHovered,
                    isPressed = isPressed,
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = resolveDesktopBackTitleToolbarIconColor(style = style),
                    modifier = Modifier.size(size = 20.dp),
                )
            }
        }
        if (title != null) {
            Spacer(modifier = Modifier.size(size = 8.dp))
            Text(
                text = title,
                color = DesktopNavigationToolbarTokens.Title,
                fontSize = DesktopNavigationToolbarTokens.TitleSize,
                lineHeight = DesktopNavigationToolbarTokens.TitleLineHeight,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 仅悬浮状态透明，滚出 hero 后由内容背景托住返回按钮以保持可读性。 */
private fun resolveDesktopBackTitleToolbarBackground(
    style: DesktopBackTitleToolbarStyle,
    contentBackgroundColor: Color,
): Color =
    if (style == DesktopBackTitleToolbarStyle.Content) {
        contentBackgroundColor
    } else {
        Color.Transparent
    }

/** 悬浮返回按钮使用 Figma 的半透明圆形背景，其余页面保持原有无底色按钮。 */
private fun resolveDesktopBackTitleToolbarButtonBackground(
    style: DesktopBackTitleToolbarStyle,
    isHovered: Boolean,
    isPressed: Boolean,
): Color =
    when {
        style == DesktopBackTitleToolbarStyle.Overlay && isPressed -> Color.White.copy(alpha = 0.3f)
        style == DesktopBackTitleToolbarStyle.Overlay && isHovered -> Color.White.copy(alpha = 0.26f)
        style == DesktopBackTitleToolbarStyle.Overlay -> Color.White.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

/** hero 上使用白色返回图标，内容区与普通二级页继续使用深色图标。 */
private fun resolveDesktopBackTitleToolbarIconColor(style: DesktopBackTitleToolbarStyle): Color =
    if (style == DesktopBackTitleToolbarStyle.Overlay) {
        Color.White
    } else {
        DesktopNavigationToolbarTokens.Title
    }
