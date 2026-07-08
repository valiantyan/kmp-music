package com.yanhao.kmpmusic.feature.app.routes

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.screen.FavoritesScreen
import com.yanhao.kmpmusic.feature.screen.HomeScreen
import com.yanhao.kmpmusic.feature.screen.LocalMusicDiscoveryPlatform
import com.yanhao.kmpmusic.feature.screen.MeScreen

/**
 * 渲染手机端一级页面路由，外层滚动和底部避让由 [MobileContentLayout] 统一负责。
 */
@Composable
fun MobileRootScreenRoute(
    state: MusicAppUiState,
    controller: MusicAppController,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when (state.navigationState.rootTab) {
        RootTab.Home -> HomeScreen(
            songs = state.songs,
            albums = state.localAlbums,
            artists = state.localArtists,
            libraryStats = state.libraryStats,
            scanState = state.scanState,
            discoveryPlatform = discoveryPlatform,
            selectedSection = state.homeContentSection,
            currentSongId = state.currentSongId,
            onSearch = controller::openSearch,
            onScan = controller::openAudioScan,
            onSection = controller::setHomeContentSection,
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(song = song, queueSongs = queueSongs)
            },
            onMore = controller::openMore,
            onLike = controller::toggleFavorite,
            onAlbumOpen = controller::openAlbum,
            onArtistOpen = controller::openArtist,
            modifier = modifier,
            contentPadding = contentPadding,
        )
        RootTab.Favorites -> FavoritesScreen(
            songs = state.favoriteSongs,
            currentSongId = state.currentSongId,
            section = state.favoriteSection,
            onSection = controller::setFavoriteSection,
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(song = song, queueSongs = queueSongs)
            },
            onMore = controller::openMore,
            onLike = controller::toggleFavorite,
            onSearch = { controller.openSearch(context = SearchContext.Favorites) },
            onAlbumOpen = controller::openAlbum,
            onArtistOpen = controller::openArtist,
            modifier = modifier,
            contentPadding = contentPadding,
        )
        RootTab.Me -> MeScreen(
            recentSongs = state.recentSongs,
            currentSongId = state.currentSongId,
            libraryStats = state.libraryStats,
            onScanMusic = controller::openAudioScan,
            onSongsStatClick = controller::openHomeSongs,
            onRecentPlayedViewAll = controller::openRecentPlayed,
            onRecentSongPlay = controller::playRecentSong,
            onRecentSongMore = controller::openMore,
            modifier = modifier,
            contentPadding = contentPadding,
        )
    }
}
