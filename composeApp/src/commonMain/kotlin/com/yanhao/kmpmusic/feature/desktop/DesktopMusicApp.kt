package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.core.theme.KmpMusicTheme
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import kotlinx.coroutines.launch

/**
 * Desktop-only app surface. Mobile Android/iOS continue to call MusicApp.
 */
@Composable
fun DesktopMusicApp(
    controller: MusicAppController,
) {
    val state: MusicAppUiState = controller.uiState
    val coroutineScope = rememberCoroutineScope()
    val scanLocalMusic: () -> Unit = {
        coroutineScope.launch {
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        }
    }
    KmpMusicTheme(themeMode = state.themeMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DesktopMusicColors.WindowBackground),
        ) {
            DesktopTitleBar(onSearch = controller::openSearch)
            Row(modifier = Modifier.weight(1f)) {
                DesktopRail(
                    activeDestination = state.desktopRailDestination(),
                    onRootTab = controller::navigateToRoot,
                    onSettings = { controller.navigateToSecondary(SecondaryScreen.Settings) },
                )
                DesktopWorkspace(
                    state = state,
                    controller = controller,
                    onScanLocalMusic = scanLocalMusic,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(DesktopMusicColors.Paper),
                )
            }
            DesktopBottomPlayer(
                song = state.currentSong,
                isPlaying = state.isPlaying,
                playbackPositionMs = state.playbackPositionMs,
                playbackDurationMs = state.playbackDurationMs,
                onOpen = controller::openPlayer,
                onToggle = controller::togglePlayback,
                onPrev = { controller.moveTrack(direction = -1) },
                onNext = { controller.moveTrack(direction = 1) },
                onMode = controller::cyclePlaybackMode,
                onLike = controller::toggleFavorite,
                onQueue = controller::openQueue,
            )
        }
    }
}

/**
 * 桌面工作区统一承接一级页和临时二级页占位，后续任务可以继续沿着这里扩展。
 */
@Composable
private fun DesktopWorkspace(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalPadding = desktopPageHorizontalPadding(maxWidth)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DesktopMusicColors.Paper)
                .padding(
                    start = horizontalPadding,
                    top = DesktopMusicDimens.PagePaddingTop,
                    end = horizontalPadding,
                    bottom = DesktopMusicDimens.PagePaddingBottom,
                ),
        ) {
            if (state.navigationState.secondaryScreen == null) {
                when (state.navigationState.rootTab) {
                    RootTab.Home -> DesktopLocalMusicRootScreen(
                        songs = state.songs,
                        albums = state.albums,
                        libraryStats = state.libraryStats,
                        currentSongId = state.currentSongId,
                        currentPlaybackStatus = state.playbackStatus,
                        onScan = onScanLocalMusic,
                        onSongOpen = { song, queueSongs ->
                            controller.openSong(
                                song = song,
                                queueSongs = queueSongs,
                            )
                        },
                        onSongPlay = { song, queueSongs ->
                            controller.playSong(
                                song = song,
                                queueSongs = queueSongs,
                            )
                        },
                        onCurrentSongToggle = controller::togglePlayback,
                        onMore = controller::openMore,
                        onAlbumOpen = controller::openAlbum,
                    )
                    RootTab.Favorites -> DesktopFavoritesRootScreen(
                        songs = state.favoriteSongs,
                        albums = state.favoriteAlbums,
                        artists = state.favoriteArtists,
                        section = state.favoriteSection,
                        currentSongId = state.currentSongId,
                        onSection = controller::setFavoriteSection,
                        onSongOpen = { song, queueSongs ->
                            controller.openSong(
                                song = song,
                                queueSongs = queueSongs,
                            )
                        },
                        onSongPlay = { song, queueSongs ->
                            controller.playSong(
                                song = song,
                                queueSongs = queueSongs,
                            )
                        },
                        onCurrentSongToggle = controller::togglePlayback,
                        onMore = controller::openMore,
                        onLike = controller::toggleFavorite,
                    )
                    RootTab.Me -> DesktopMeRootScreen(
                        albums = state.albums,
                        artists = state.artists,
                        libraryStats = state.libraryStats,
                        favoriteCount = state.likedSongIds.size,
                        onLogin = { controller.navigateToSecondary(SecondaryScreen.Login) },
                        onSettings = { controller.navigateToSecondary(SecondaryScreen.Settings) },
                    )
                }
                return@BoxWithConstraints
            }
            DesktopEmptyStateScreen(
                title = "二级页面",
                subtitle = "下一任务接入桌面二级页",
            )
        }
    }
}

/** 桌面左侧 rail 需要把根页面与设置页映射成唯一选中态。 */
private fun MusicAppUiState.desktopRailDestination(): DesktopRailDestination {
    return when (navigationState.secondaryScreen) {
        SecondaryScreen.Settings -> DesktopRailDestination.Settings
        else -> when (navigationState.rootTab) {
            RootTab.Home -> DesktopRailDestination.Home
            RootTab.Favorites -> DesktopRailDestination.Favorites
            RootTab.Me -> DesktopRailDestination.Me
        }
    }
}
