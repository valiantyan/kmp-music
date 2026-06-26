package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.yanhao.kmpmusic.core.theme.KmpMusicTheme
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.feature.app.AppOverlays
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(DesktopMusicDimens.WindowCornerRadius)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DesktopMusicColors.WindowBackground),
            ) {
                DesktopTitleBar(
                    onSearch = controller::openSearch,
                )
                Row(modifier = Modifier.weight(1f)) {
                    DesktopRail(
                        activeDestination = state.desktopRailDestination(),
                        onRootTab = controller::navigateToRoot,
                        onSettings = { controller.navigateToSecondary(SecondaryScreen.Settings) },
                    )
                    if (state.shouldShowLibrarySidebar()) {
                        DesktopLibrarySidebar(
                            libraryStats = state.libraryStats,
                            recentSongs = state.recentSongs,
                            onSearch = controller::openSearch,
                            onSection = controller::openLocalMusic,
                            onSongPlay = { song, queueSongs ->
                                controller.playSong(
                                    song = song,
                                    queueSongs = queueSongs,
                                )
                            },
                        )
                    }
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
                    playbackMode = state.playbackMode,
                    onOpen = controller::openPlayer,
                    onToggle = controller::togglePlayback,
                    onPrev = { controller.moveTrack(direction = -1) },
                    onNext = { controller.moveTrack(direction = 1) },
                    onMode = controller::cyclePlaybackMode,
                    onLike = controller::toggleFavorite,
                    onSeek = controller::seekTo,
                    onVolumeChange = controller::setVolume,
                    onQueue = controller::openQueue,
                )
            }
            AppOverlays(state = state, controller = controller)
        }
    }
}

/**
 * 桌面工作区统一承接一级页和二级页，保持外层 shell 不需要感知具体页面细节。
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
                        recentSongs = state.recentSongs,
                        libraryStats = state.libraryStats,
                        currentSongId = state.currentSongId,
                        currentPlaybackStatus = state.playbackStatus,
                        onScan = onScanLocalMusic,
                        onBrowseLibrary = {
                            controller.openLocalMusic(section = LocalMusicSection.Songs)
                        },
                        onBrowseAlbums = {
                            controller.openLocalMusic(section = LocalMusicSection.Albums)
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
                        currentPlaybackStatus = state.playbackStatus,
                        onSection = controller::setFavoriteSection,
                        onSongPlay = { song, queueSongs ->
                            controller.playSong(
                                song = song,
                                queueSongs = queueSongs,
                            )
                        },
                        onCurrentSongToggle = controller::togglePlayback,
                        onMore = controller::openMore,
                        onLike = controller::toggleFavorite,
                        onAlbumOpen = controller::openAlbum,
                        onArtistOpen = controller::openArtist,
                    )
                    RootTab.Me -> DesktopMeRootScreen(
                        albums = state.albums,
                        recentSongs = state.recentSongs,
                        artists = state.artists,
                        libraryStats = state.libraryStats,
                        favoriteCount = state.likedSongIds.size,
                        onLogin = { controller.navigateToSecondary(SecondaryScreen.Login) },
                        onFavorites = { controller.navigateToRoot(RootTab.Favorites) },
                        onFolders = {
                            controller.openLocalMusic(section = LocalMusicSection.Sources)
                        },
                        onSettings = { controller.navigateToSecondary(SecondaryScreen.Settings) },
                        onBrowseAlbums = {
                            controller.openLocalMusic(section = LocalMusicSection.Albums)
                        },
                        onAlbumOpen = controller::openAlbum,
                        onArtistOpen = controller::openArtist,
                    )
                }
                return@BoxWithConstraints
            }
            DesktopSecondaryScreen(
                state = state,
                controller = controller,
                onScanLocalMusic = onScanLocalMusic,
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

/** 首页保持效果图中的资料库侧栏，二级页让内容区获得完整宽度。 */
private fun MusicAppUiState.shouldShowLibrarySidebar(): Boolean {
    return navigationState.secondaryScreen == null && navigationState.rootTab == RootTab.Home
}
