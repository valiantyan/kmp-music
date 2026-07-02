package com.yanhao.kmpmusic.feature.app.routes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.screen.AlbumDetailScreen
import com.yanhao.kmpmusic.feature.screen.ArtistDetailScreen
import com.yanhao.kmpmusic.feature.screen.LocalMusicScreen
import com.yanhao.kmpmusic.feature.screen.LoginScreen
import com.yanhao.kmpmusic.feature.screen.MissingLibraryItemScreen
import com.yanhao.kmpmusic.feature.screen.PlayerScreen
import com.yanhao.kmpmusic.feature.screen.SearchScreen
import com.yanhao.kmpmusic.feature.screen.SettingsScreen

/**
 * 渲染手机端二级页面路由，保留 [LocalMusicScreen] 独立于纵向滚动容器的列表行为。
 */
@Composable
fun MobileSecondaryScreenRoute(
    secondaryScreen: SecondaryScreen,
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    when (secondaryScreen) {
        is SecondaryScreen.LocalMusic -> LocalMusicScreen(
            songs = state.localSongs,
            albums = state.localAlbums,
            artists = state.localArtists,
            sources = state.localMusicSources,
            problems = state.localMusicProblems,
            initialSection = secondaryScreen.initialSection,
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            onBack = controller::navigateBack,
            onSongOpen = { song: Song, queueSongs: List<Song> ->
                controller.openSong(song = song, queueSongs = queueSongs)
            },
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(song = song, queueSongs = queueSongs)
            },
            onCurrentSongToggle = controller::togglePlayback,
            onMore = controller::openMore,
            onAlbumOpen = controller::openAlbum,
            onArtistOpen = controller::openArtist,
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = contentPadding,
        )
        is SecondaryScreen.Search -> SearchScreen(
            query = state.searchQuery,
            scope = state.searchScope,
            result = controller.search(),
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            onBack = controller::navigateBack,
            onQuery = controller::setSearchQuery,
            onScope = controller::setSearchScope,
            onSongOpen = { song: Song, queueSongs: List<Song> ->
                controller.openSong(song = song, queueSongs = queueSongs)
            },
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(song = song, queueSongs = queueSongs)
            },
            onCurrentSongToggle = controller::togglePlayback,
            onMore = controller::openMore,
            onAlbumOpen = controller::openAlbum,
            onArtistOpen = controller::openArtist,
        )
        SecondaryScreen.Player -> state.currentSong?.let { song ->
            PlayerScreen(
                song = song,
                isPlaying = state.shouldShowPauseControl,
                playbackPositionMs = state.playbackPositionMs,
                playbackDurationMs = state.playbackDurationMs,
                playbackMode = state.playbackMode,
                playbackError = state.playbackError,
                onBack = controller::navigateBack,
                onToggle = controller::togglePlayback,
                onPrev = { controller.moveTrack(direction = -1) },
                onNext = { controller.moveTrack(direction = 1) },
                onSeek = controller::seekTo,
                onMode = controller::cyclePlaybackMode,
                onLike = controller::toggleFavorite,
                onQueue = controller::openQueue,
            )
        } ?: MissingLibraryItemScreen(
            title = "暂无播放",
            subtitle = "播放一首本地歌曲后会在这里显示。",
            onBack = controller::navigateBack,
        )
        SecondaryScreen.AlbumDetail -> state.selectedAlbum?.let { album ->
            AlbumDetailScreen(
                album = album,
                songs = state.localSongs,
                currentSongId = state.currentSongId,
                currentPlaybackStatus = state.playbackStatus,
                onBack = controller::navigateBack,
                onSongOpen = { song: Song, queueSongs: List<Song> ->
                    controller.openSong(song = song, queueSongs = queueSongs)
                },
                onSongPlay = { song: Song, queueSongs: List<Song> ->
                    controller.playSong(song = song, queueSongs = queueSongs)
                },
                onCurrentSongToggle = controller::togglePlayback,
                onMore = controller::openMore,
                onLike = controller::toggleFavorite,
            )
        } ?: MissingLibraryItemScreen(
            title = "专辑不可用",
            onBack = controller::navigateBack,
        )
        SecondaryScreen.ArtistDetail -> state.selectedArtist?.let { artist ->
            ArtistDetailScreen(
                artist = artist,
                songs = state.localSongs,
                albums = state.localAlbums,
                currentSongId = state.currentSongId,
                currentPlaybackStatus = state.playbackStatus,
                onBack = controller::navigateBack,
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
            )
        } ?: MissingLibraryItemScreen(
            title = "歌手不可用",
            onBack = controller::navigateBack,
        )
        SecondaryScreen.Settings -> SettingsScreen(
            themeMode = state.themeMode,
            onThemeMode = controller::setThemeMode,
            onBack = controller::navigateBack,
            onScan = onScanLocalMusic,
            onLocalMusicSources = {
                controller.openLocalMusic(section = LocalMusicSection.Sources)
            },
            onClearCache = controller::openClearCacheDialog,
        )
        SecondaryScreen.Login -> LoginScreen(
            email = state.email,
            isMailSent = state.isMailSent,
            onEmail = controller::setEmail,
            onSend = controller::sendLoginMail,
            onBack = controller::navigateBack,
        )
    }
}
