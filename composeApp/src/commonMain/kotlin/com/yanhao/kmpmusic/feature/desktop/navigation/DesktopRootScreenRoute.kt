package com.yanhao.kmpmusic.feature.desktop.navigation

import androidx.compose.runtime.Composable
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.desktop.screens.DesktopFavoritesRootScreen
import com.yanhao.kmpmusic.feature.desktop.screens.DesktopLocalMusicRootScreen
import com.yanhao.kmpmusic.feature.desktop.screens.DesktopMeRootScreen

/**
 * 桌面一级路由集中转发根 Tab，保证页面动作继续只通过 [MusicAppController]。
 */
@Composable
fun DesktopRootScreenRoute(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
) {
    when (state.navigationState.rootTab) {
        RootTab.Home -> {
            DesktopLocalMusicRootScreen(
                songs = state.songs,
                songCount = maxOf(a = state.libraryStats.songCount, b = state.songs.size),
                scanState = state.scanState,
                currentSongId = state.currentSongId,
                isPlaying = state.isPlaying,
                onSearch = { controller.openSearch(context = SearchContext.LocalLibrary) },
                onScan = onScanLocalMusic,
                onSongPlay = { song: Song, queueSongs: List<Song> ->
                    controller.playSong(
                        song = song,
                        queueSongs = queueSongs,
                    )
                },
                onCurrentSongToggle = controller::togglePlayback,
                onMore = controller::openMore,
                onLike = controller::toggleFavorite,
            )
        }

        RootTab.Favorites -> {
            DesktopFavoritesRootScreen(
                songs = state.favoriteSongs,
                albums = state.favoriteAlbums,
                artists = state.favoriteArtists,
                section = state.favoriteSection,
                currentSongId = state.currentSongId,
                currentPlaybackStatus = state.playbackStatus,
                onSection = controller::setFavoriteSection,
                onSongPlay = { song: Song, queueSongs: List<Song> ->
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
        }

        RootTab.Me -> {
            DesktopMeRootScreen(
                recentSongs = state.recentSongs,
                libraryStats = state.libraryStats,
                localPlaylistCount = state.localPlaylistCount,
                currentSongId = state.currentSongId,
                onScanMusic = onScanLocalMusic,
                onLocalPlaylistsOpen = controller::openLocalPlaylists,
                onRecentPlayedViewAll = controller::openRecentPlayed,
                onRecentSongPlay = controller::playRecentSong,
                onRecentSongMore = controller::openMore,
            )
        }
    }
}
