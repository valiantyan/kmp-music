package com.yanhao.kmpmusic.feature.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.yanhao.kmpmusic.core.theme.MobileToolbarTitleStyle
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.MusicDimens

/**
 * 一级页 Toolbar，移除返回槽并保留 Figma 规格的右侧搜索操作。
 */
@Composable
fun MobilePrimaryToolbar(
    title: String,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MobileToolbarRow(modifier = modifier) {
        MobileToolbarTitle(
            title = title,
            modifier =
                Modifier
                    .weight(weight = 1f)
                    .padding(
                        start =
                            MusicDimens.MobilePrimaryToolbarTitleStart -
                                MusicDimens.MobileToolbarOuterPadding,
                    ),
        )
        MobileToolbarIconButton(
            contentDescription = "搜索",
            onClick = onSearch,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(size = MusicDimens.MobileToolbarIconSize),
                tint = MusicColors.MobileToolbarContent,
            )
        }
    }
}

/**
 * 二级页 Toolbar，固定使用 Figma 返回槽和单行标题节奏。
 */
@Composable
fun MobileSecondaryToolbar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 1f,
    titleAlpha: Float = 1f,
) {
    MobileToolbarRow(
        modifier = modifier,
        backgroundAlpha = backgroundAlpha,
    ) {
        MobileToolbarIconButton(
            contentDescription = "返回",
            onClick = onBack,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(size = MusicDimens.MobileToolbarIconSize),
                tint = MusicColors.MobileToolbarContent,
            )
        }
        MobileToolbarTitle(
            title = title,
            modifier =
                Modifier
                    .weight(weight = 1f)
                    .alpha(alpha = titleAlpha),
        )
        Spacer(modifier = Modifier.width(width = MusicDimens.MobileToolbarActionSlotWidth))
    }
}

// 共享行严格保留 52dp 高度、4dp 外边距和 4dp 内容间距。
@Composable
private fun MobileToolbarRow(
    modifier: Modifier,
    backgroundAlpha: Float = 1f,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height = MusicDimens.MobileToolbarHeight)
                .background(
                    color = MusicColors.MobileToolbarBackground.copy(alpha = backgroundAlpha),
                ).padding(horizontal = MusicDimens.MobileToolbarOuterPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            androidx.compose.foundation.layout.Arrangement.spacedBy(
                space = MusicDimens.MobileToolbarContentGap,
            ),
        content = content,
    )
}

// 标题槽保持单行省略，避免动态专辑、歌手和歌单名称挤压操作区。
@Composable
private fun MobileToolbarTitle(
    title: String,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.height(height = MusicDimens.MobileToolbarHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = title,
            color = MusicColors.MobileToolbarContent,
            style = MobileToolbarTitleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 图标按钮使用 48dp 稳定槽位和 40dp 状态层，动态图标不会改变 Toolbar 宽度。
@Composable
private fun MobileToolbarIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier
                .width(width = MusicDimens.MobileToolbarActionSlotWidth)
                .height(height = MusicDimens.MobileToolbarHeight)
                .semantics {
                    this.contentDescription = contentDescription
                }.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(size = MusicDimens.MobileToolbarStateLayerSize)
                    .indication(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
