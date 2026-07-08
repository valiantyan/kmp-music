package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 我的页静态设置菜单项，只表达当前 Figma 信息结构，不承载未完成导航。
 *
 * @property icon 菜单行左侧图标。
 * @property title 菜单行标题。
 */
private data class MeSettingsMenuItem(
    val icon: ImageVector,
    val title: String,
)

/**
 * 设置菜单按 Figma 使用三条独立白底描边入口。
 */
@Composable
internal fun MeSettingsMenuSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = meSettingsRowGap),
    ) {
        buildMeSettingsMenuItems().forEach { item: MeSettingsMenuItem ->
            MeSettingsMenuRow(item = item)
        }
    }
}

// 构造静态菜单，避免视觉还原时误接半成品设置路由。
private fun buildMeSettingsMenuItems(): List<MeSettingsMenuItem> {
    return listOf(
        MeSettingsMenuItem(
            icon = Icons.Rounded.Storage,
            title = "存储管理",
        ),
        MeSettingsMenuItem(
            icon = Icons.Rounded.Palette,
            title = "主题与外观",
        ),
        MeSettingsMenuItem(
            icon = Icons.Rounded.Info,
            title = "关于",
        ),
    )
}

// 单行保留 Figma 的图标、标题和右箭头，不声明点击回调。
@Composable
private fun MeSettingsMenuRow(
    item: MeSettingsMenuItem,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = meSettingsRowHeight),
        shape = RoundedCornerShape(size = meSettingsRowRadius),
        color = meBackgroundColor,
        border = BorderStroke(width = 1.dp, color = meOutlineColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = meSettingsRowPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(size = 20.dp),
                    tint = meMetaColor,
                )
                Text(
                    text = item.title,
                    color = meTextColor,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(size = 14.dp),
                tint = meMetaColor,
            )
        }
    }
}
