package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.feature.app.RootTab

enum class DesktopRailDestination {
    Home,
    Favorites,
    Me,
    Settings,
}

@Composable
fun DesktopTitleBar(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.TitleBarHeight)
            .background(Color(0xB8F7F9FB))
            .border(width = 1.dp, color = Color(0xB8C7CFD6)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .width(DesktopMusicDimens.RailWidth)
                .padding(start = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TrafficLight(color = Color(0xFFFF5F57))
            TrafficLight(color = Color(0xFFFEBC2E))
            TrafficLight(color = Color(0xFF28C840))
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "KMP Music",
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.AppTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Surface(
            modifier = Modifier
                .width(520.dp)
                .height(30.dp)
                .padding(end = 18.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.84f),
            border = BorderStroke(width = 1.dp, color = Color(0xFFD7DDE3)),
            onClick = onSearch,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color(0xFF8A95A3),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "搜索歌曲、专辑、歌手",
                    color = Color(0xFF8A95A3),
                    fontSize = DesktopMusicType.Body,
                )
            }
        }
    }
}

@Composable
private fun TrafficLight(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color),
    )
}

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

@Composable
fun DesktopPageHeader(
    title: String,
    eyebrow: String,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.PageTitle,
                lineHeight = DesktopMusicType.PageTitle,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = eyebrow,
                color = DesktopMusicColors.Muted,
                fontSize = DesktopMusicType.Eyebrow,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 7.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions()
        }
    }
}

@Composable
fun DesktopPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(DesktopMusicDimens.PrimaryButtonHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1AC0A8),
                            DesktopMusicColors.AccentDeep,
                        ),
                    ),
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = DesktopMusicType.Eyebrow,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun DesktopStatCard(
    icon: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(DesktopMusicDimens.StatCardMinHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.64f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = icon,
                    color = Color(0xFF303845),
                    fontSize = 24.sp,
                )
            }
            Column {
                Text(
                    text = title,
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.StatTitle,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = value,
                    color = Color(0xFF5E6A78),
                    fontSize = DesktopMusicType.Eyebrow,
                )
            }
        }
    }
}
