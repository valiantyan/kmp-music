package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionHeader

/**
 * 桌面“我的”页静态设置菜单项，只表达当前个人中心信息结构，不承载导航目标。
 *
 * @property icon 菜单行左侧图标。
 * @property title 菜单行标题。
 * @property subtitle 菜单行说明。
 * @property isNavigationEnabled 是否允许从该行触发页面导航；当前 PRD 要求固定为 false。
 */
internal data class DesktopMeStaticSettingsMenuItemDisplayModel(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val isNavigationEnabled: Boolean,
)

/**
 * 构造桌面“我的”页静态设置菜单；这些行不能接入旧设置、关于或来源管理路由。
 */
internal fun buildDesktopMeStaticSettingsMenuItemDisplayModels(): List<DesktopMeStaticSettingsMenuItemDisplayModel> =
    listOf(
        DesktopMeStaticSettingsMenuItemDisplayModel(
            icon = Icons.Rounded.Storage,
            title = "存储管理",
            subtitle = "本地音乐空间与缓存概览",
            isNavigationEnabled = false,
        ),
        DesktopMeStaticSettingsMenuItemDisplayModel(
            icon = Icons.Rounded.Palette,
            title = "主题与外观",
            subtitle = "界面颜色与显示偏好",
            isNavigationEnabled = false,
        ),
        DesktopMeStaticSettingsMenuItemDisplayModel(
            icon = Icons.Rounded.Info,
            title = "关于",
            subtitle = "版本信息与项目说明",
            isNavigationEnabled = false,
        ),
    )

/**
 * 静态设置菜单复用桌面卡片层级，但行本身不声明点击回调。
 */
@Composable
internal fun DesktopMeStaticSettingsMenu() {
    DesktopSectionHeader(title = "设置")
    Spacer(modifier = Modifier.height(14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        buildDesktopMeStaticSettingsMenuItemDisplayModels().forEach { item: DesktopMeStaticSettingsMenuItemDisplayModel ->
            DesktopMeStaticSettingsMenuRow(item = item)
        }
    }
}

/**
 * 单行只展示箭头视觉，不使用 [Surface] 的 onClick 重载，避免跳转半成品页面。
 */
@Composable
private fun DesktopMeStaticSettingsMenuRow(
    item: DesktopMeStaticSettingsMenuItemDisplayModel,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesktopMeStaticSettingsIcon(item = item)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.title,
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.StatTitle,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.subtitle,
                    color = DesktopMusicColors.MutedStrong,
                    fontSize = DesktopMusicType.Body,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = DesktopMusicColors.MutedStrong,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * 图标容器复用桌面入口卡片视觉，但不提供独立交互热区。
 */
@Composable
private fun DesktopMeStaticSettingsIcon(
    item: DesktopMeStaticSettingsMenuItemDisplayModel,
) {
    Surface(
        modifier = Modifier.size(38.dp),
        shape = RoundedCornerShape(12.dp),
        color = DesktopMusicColors.AccentSoft,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = DesktopMusicColors.AccentDeep,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
