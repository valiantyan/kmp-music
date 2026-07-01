package com.yanhao.kmpmusic.feature.app.routes

import androidx.compose.runtime.Composable
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.screen.FavoritesScreen
import com.yanhao.kmpmusic.feature.screen.HomeScreen
import com.yanhao.kmpmusic.feature.screen.MeScreen

/**
 * 渲染手机端一级页面路由，外层滚动和底部避让由 [MobileContentLayout] 统一负责。
 */
@Composable
fun MobileRootScreenRoute(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
) {
    when (state.navigationState.rootTab) {
        RootTab.Home -> HomeScreen(
            songs = state.homeLocalSongPreview,
            albums = state.localAlbums,
            libraryStats = state.libraryStats,
            scanState = state.scanState,
            recentSongs = state.recentSongs,
            localSongPreview = state.homeLocalSongPreview,
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            onSearch = controller::openSearch,
            onScan = onScanLocalMusic,
            onLocalMusic = { controller.openLocalMusic(section = LocalMusicSection.Songs) },
            onSongOpen = { song: Song, queueSongs: List<Song> ->
                controller.openSong(song = song, queueSongs = queueSongs)
            },
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(song = song, queueSongs = queueSongs)
            },
            onCurrentSongToggle = controller::togglePlayback,
            onMore = controller::openMore,
            onAlbumOpen = controller::openAlbum,
        )
        RootTab.Favorites -> FavoritesScreen(
            songs = state.favoriteSongs,
            albums = state.favoriteAlbums,
            artists = state.favoriteArtists,
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            section = state.favoriteSection,
            onSection = controller::setFavoriteSection,
            onSongOpen = { song: Song, queueSongs: List<Song> ->
                controller.openSong(song = song, queueSongs = queueSongs)
            },
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(song = song, queueSongs = queueSongs)
            },
            onCurrentSongToggle = controller::togglePlayback,
            onMore = controller::openMore,
            onLike = controller::toggleFavorite,
            onAlbumOpen = controller::openAlbum,
            onArtistOpen = controller::openArtist,
        )
        RootTab.Me -> MeScreen(
            albums = state.albums,
            artists = state.artists,
            libraryStats = state.libraryStats,
            favoriteCount = state.likedSongIds.size,
            onSettings = { controller.navigateToSecondary(SecondaryScreen.Settings) },
            onLogin = { controller.navigateToSecondary(SecondaryScreen.Login) },
            onAlbumOpen = controller::openAlbum,
            onArtistOpen = controller::openArtist,
        )
    }
}
