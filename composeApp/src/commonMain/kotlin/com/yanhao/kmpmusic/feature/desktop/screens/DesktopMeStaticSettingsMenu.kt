package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 桌面“我的”页静态设置行，只表达设计信息结构，不暴露未完成路由。 */
internal data class DesktopMeStaticSettingsMenuItemDisplayModel(
    val icon: ImageVector,
    val title: String,
    val isNavigationEnabled: Boolean,
)

/** 构造固定的三项展示入口，任何一项都不允许连接设置或关于页面。 */
internal fun buildDesktopMeStaticSettingsMenuItemDisplayModels(): List<DesktopMeStaticSettingsMenuItemDisplayModel> =
    listOf(
        DesktopMeStaticSettingsMenuItemDisplayModel(
            icon = Icons.Rounded.Storage,
            title = "存储管理",
            isNavigationEnabled = false,
        ),
        DesktopMeStaticSettingsMenuItemDisplayModel(
            icon = Icons.Rounded.Palette,
            title = "主题外观",
            isNavigationEnabled = false,
        ),
        DesktopMeStaticSettingsMenuItemDisplayModel(
            icon = Icons.Rounded.Info,
            title = "关于软件",
            isNavigationEnabled = false,
        ),
    )

/** 以节点的三分之一列宽呈现静态设置菜单。 */
@Composable
internal fun DesktopMeStaticSettingsMenu(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "系统设置",
            modifier = Modifier.padding(horizontal = 16.dp),
            color = DesktopMeFigmaTokens.Muted.copy(alpha = 0.4f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            buildDesktopMeStaticSettingsMenuItemDisplayModels().forEach { item: DesktopMeStaticSettingsMenuItemDisplayModel ->
                DesktopMeStaticSettingsMenuRow(item = item)
            }
        }
    }
}

/** 保留箭头的视觉暗示，但不添加点击修饰符，防止进入半成品页面。 */
@Composable
private fun DesktopMeStaticSettingsMenuRow(item: DesktopMeStaticSettingsMenuItemDisplayModel) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDEE8FF),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = DesktopMeFigmaTokens.Muted,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
            Text(
                text = item.title,
                modifier = Modifier.weight(1f),
                color = Color.Black,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = DesktopMeFigmaTokens.Muted.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
