package com.yanhao.kmpmusic.feature.desktop.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
