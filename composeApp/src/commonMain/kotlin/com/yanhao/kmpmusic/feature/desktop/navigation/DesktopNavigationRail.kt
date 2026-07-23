package com.yanhao.kmpmusic.feature.desktop.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens

/**
 * 桌面左侧导航栏按 Figma 固定入口渲染，底部账户区承接“我的”页的既有导航动作。
 */
@Composable
fun DesktopRail(
    activeDestination: DesktopRailDestination,
    onMusic: () -> Unit,
    onAlbums: () -> Unit,
    onArtists: () -> Unit,
    onPlaylists: () -> Unit,
    onFavorites: () -> Unit,
    onMe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .width(DesktopMusicDimens.RailWidth)
                .fillMaxHeight()
                .background(Color(0xFFF0F3FF))
                .border(width = 1.dp, color = Color(0x33BBCAC4))
                .padding(start = 16.dp, top = 16.dp, end = 17.dp, bottom = 16.dp),
    ) {
        DesktopRailBrand()
        DesktopRailItems(
            activeDestination = activeDestination,
            onMusic = onMusic,
            onAlbums = onAlbums,
            onArtists = onArtists,
            onPlaylists = onPlaylists,
            onFavorites = onFavorites,
        )
        Spacer(modifier = Modifier.weight(1f))
        DesktopRailAccount(
            activeDestination = activeDestination,
            onClick = onMe,
        )
    }
}

// 品牌区按设计稿保留纯文字，避免与 macOS 原生按钮区域发生额外交互。
@Composable
private fun DesktopRailBrand() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 32.dp),
    ) {
        Text(
            text = "KMP-MUSIC",
            color = Color(0xFF006B5C),
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
