package com.yanhao.kmpmusic.feature.desktop.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 六个入口顺序与 Figma 一致，设置入口后续由“我的”页承接。
@Composable
internal fun DesktopRailItems(
    activeDestination: DesktopRailDestination,
    onMusic: () -> Unit,
    onAlbums: () -> Unit,
    onArtists: () -> Unit,
    onPlaylists: () -> Unit,
    onFavorites: () -> Unit,
    onMe: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DesktopRailItem(
            destination = DesktopRailDestination.Music,
            activeDestination = activeDestination,
            icon = Icons.Rounded.LibraryMusic,
            label = "音乐",
            onClick = onMusic,
        )
        DesktopRailItem(
            destination = DesktopRailDestination.Albums,
            activeDestination = activeDestination,
            icon = Icons.Rounded.Album,
            label = "专辑",
            onClick = onAlbums,
        )
        DesktopRailItem(
            destination = DesktopRailDestination.Artists,
            activeDestination = activeDestination,
            icon = Icons.Rounded.Person,
            label = "歌手",
            onClick = onArtists,
        )
        DesktopRailItem(
            destination = DesktopRailDestination.Playlists,
            activeDestination = activeDestination,
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            label = "歌单",
            onClick = onPlaylists,
        )
        DesktopRailItem(
            destination = DesktopRailDestination.Favorites,
            activeDestination = activeDestination,
            icon = Icons.Rounded.Favorite,
            label = "收藏",
            onClick = onFavorites,
        )
        DesktopRailItem(
            destination = DesktopRailDestination.Me,
            activeDestination = activeDestination,
            icon = Icons.Rounded.Person,
            label = "我的",
            onClick = onMe,
        )
    }
}

/**
 * 单个左栏入口固定高度和圆角，保证选中态变化不挤压文案和图标。
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
    val contentColor: Color = if (isActive) Color(0xFF006B5C) else Color(0xFF3C4A46)
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(42.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) Color(0x1A006B5C) else Color.Transparent,
        onClick = onClick,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                color = contentColor,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
