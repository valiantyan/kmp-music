package com.yanhao.kmpmusic.feature.desktop.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

enum class DesktopRailDestination {
    Home,
    Favorites,
    Me,
    Settings,
}

/**
 * 桌面左侧导航栏承接一级页和设置页入口，保持桌面导航入口只关心导航目标。
 */
@Composable
fun DesktopRail(
    activeDestination: DesktopRailDestination,
    onRootTab: (RootTab) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(DesktopMusicDimens.RailWidth)
            .fillMaxHeight()
            .background(Color(0xB3F8FBFC))
            .border(width = 1.dp, color = DesktopMusicColors.Line)
            .padding(top = 20.dp, start = 12.dp, end = 12.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(DesktopMusicDimens.BrandSize)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1DC6AD),
                            Color(0xFF0CA58F),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "♪",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(modifier = Modifier.height(30.dp))
        DesktopRailItem(
            destination = DesktopRailDestination.Home,
            activeDestination = activeDestination,
            icon = Icons.Rounded.Home,
            label = "首页",
        ) {
            onRootTab(RootTab.Home)
        }
        DesktopRailItem(
            destination = DesktopRailDestination.Favorites,
            activeDestination = activeDestination,
            icon = Icons.Rounded.Favorite,
            label = "收藏",
        ) {
            onRootTab(RootTab.Favorites)
        }
        DesktopRailItem(
            destination = DesktopRailDestination.Me,
            activeDestination = activeDestination,
            icon = Icons.Rounded.Person,
            label = "我的",
        ) {
            onRootTab(RootTab.Me)
        }
        DesktopRailItem(
            destination = DesktopRailDestination.Settings,
            activeDestination = activeDestination,
            icon = Icons.Rounded.Settings,
            label = "设置",
            onClick = onSettings,
        )
    }
}

/**
 * 单个 rail 项固定尺寸，保证选中态变化不会引发布局跳动。
 */
@Composable
private fun DesktopRailItem(
    destination: DesktopRailDestination,
    activeDestination: DesktopRailDestination,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val isActive: Boolean = destination == activeDestination
    Surface(
        modifier = Modifier
            .size(DesktopMusicDimens.RailItemSize)
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) DesktopMusicColors.Accent.copy(alpha = 0.10f) else Color.Transparent,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) DesktopMusicColors.Accent else DesktopMusicColors.MutedStrong,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                color = if (isActive) DesktopMusicColors.Accent else DesktopMusicColors.MutedStrong,
                fontSize = DesktopMusicType.RailLabel,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** 桌面左侧 rail 需要把根页面与设置页映射成唯一选中态。 */
fun MusicAppUiState.desktopRailDestination(): DesktopRailDestination {
    return when (navigationState.secondaryScreen) {
        SecondaryScreen.Settings -> DesktopRailDestination.Settings
        else -> when (navigationState.rootTab) {
            RootTab.Home -> DesktopRailDestination.Home
            RootTab.Favorites -> DesktopRailDestination.Favorites
            RootTab.Me -> DesktopRailDestination.Me
        }
    }
}
