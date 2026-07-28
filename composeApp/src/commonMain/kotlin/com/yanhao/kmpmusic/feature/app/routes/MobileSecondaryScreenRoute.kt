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
import com.yanhao.kmpmusic.feature.app.SongMoreSourceContext
import com.yanhao.kmpmusic.feature.screen.AboutScreen
import com.yanhao.kmpmusic.feature.screen.AlbumDetailScreen
import com.yanhao.kmpmusic.feature.screen.ArtistDetailScreen
import com.yanhao.kmpmusic.feature.screen.AudioScanScreen
import com.yanhao.kmpmusic.feature.screen.LocalMusicDiscoveryPlatform
import com.yanhao.kmpmusic.feature.screen.LocalMusicScreen
import com.yanhao.kmpmusic.feature.screen.LocalPlaylistDetailScreen
import com.yanhao.kmpmusic.feature.screen.LocalPlaylistListScreen
import com.yanhao.kmpmusic.feature.screen.LocalPlaylistManagementScreen
import com.yanhao.kmpmusic.feature.screen.LoginScreen
import com.yanhao.kmpmusic.feature.screen.MissingLibraryItemScreen
import com.yanhao.kmpmusic.feature.screen.PlayerScreen
import com.yanhao.kmpmusic.feature.screen.RecentPlayedScreen
import com.yanhao.kmpmusic.feature.screen.SearchScreen
import com.yanhao.kmpmusic.feature.screen.SettingsScreen
import com.yanhao.kmpmusic.feature.screen.StandardMissingLibraryItemScreen

/**
 * 渲染手机端二级页面路由，各页面自行管理固定 Toolbar 与正文滚动。
 */
@Composable
fun MobileSecondaryScreenRoute(
    secondaryScreen: SecondaryScreen,
    state: MusicAppUiState,
    controller: MusicAppController,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    when (secondaryScreen) {
        is SecondaryScreen.LocalMusic -> {
            LocalMusicScreen(
                songs = state.localSongs,
                albums = state.localAlbums,
                artists = state.localArtists,
                sources = state.localMusicSources,
                problems = state.localMusicProblems,
                scanState = state.scanState,
                discoveryPlatform = discoveryPlatform,
                initialSection = secondaryScreen.initialSection,
                currentSongId = state.currentSongId,
                currentPlaybackStatus = state.playbackStatus,
                onBack = controller::navigateBack,
                onSongPlay = { song: Song, queueSongs: List<Song> ->
                    controller.playSong(song = song, queueSongs = queueSongs)
                },
                onCurrentSongToggle = controller::togglePlayback,
                onMore = controller::openMore,
                onAlbumOpen = controller::openAlbum,
                onArtistOpen = controller::openArtist,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }

        is SecondaryScreen.Search -> {
            SearchScreen(
                query = state.searchQuery,
                activeQuery = state.activeSearchQuery,
                scope = state.searchScope,
                history = state.searchHistoryFor(context = secondaryScreen.context),
                result = controller.search(),
                currentSongId = state.currentSongId,
                currentPlaybackStatus = state.playbackStatus,
                currentAlbumTitle = state.currentSong?.album,
                onBack = controller::navigateBack,
                onQuery = controller::setSearchQuery,
                onCommitSearch = controller::commitSearchQueryToHistory,
                onScope = controller::setSearchScope,
                onHistorySelect = controller::selectSearchHistory,
                onClearHistory = {
                    controller.clearSearchHistory(context = secondaryScreen.context)
                },
                onSongPlay = { song: Song, queueSongs: List<Song> ->
                    controller.playSong(song = song, queueSongs = queueSongs)
                },
                onCurrentSongToggle = {
                    controller.commitSearchQueryToHistory()
                    controller.togglePlayback()
                },
                onMore = controller::openMore,
                onLike = controller::toggleFavorite,
                onAlbumOpen = controller::openAlbum,
                onArtistOpen = controller::openArtist,
                modifier =
                    modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                contentPadding = contentPadding,
            )
        }

        SecondaryScreen.Player -> {
            state.currentSong?.let { song ->
                PlayerScreen(
                    song = song,
                    isPlaying = state.shouldShowPauseControl,
                    playbackPositionMs = state.playbackPositionMs,
                    playbackDurationMs = state.playbackDurationMs,
                    playbackMode = state.playbackMode,
                    playbackSpeed = state.playbackSpeed,
                    playbackError = state.playbackError,
                    onBack = controller::navigateBack,
                    onToggle = controller::togglePlayback,
                    onPrev = { controller.moveTrack(direction = -1) },
                    onNext = { controller.moveTrack(direction = 1) },
                    onSeek = controller::seekTo,
                    onMode = controller::cyclePlaybackMode,
                    onSpeed = controller::openPlaybackSpeedPanel,
                    onLike = controller::toggleFavorite,
                    onQueue = controller::openQueue,
                    modifier = modifier.fillMaxSize(),
                )
            } ?: MissingLibraryItemScreen(
                title = "暂无播放",
                subtitle = "播放一首本地歌曲后会在这里显示。",
                onBack = controller::navigateBack,
            )
        }

        SecondaryScreen.AlbumDetail -> {
            state.selectedAlbum?.let { album ->
                AlbumDetailScreen(
                    album = album,
                    songs = state.localSongs,
                    currentSongId = state.currentSongId,
                    currentPlaybackStatus = state.playbackStatus,
                    onBack = controller::navigateBack,
                    onSongPlay = { song: Song, queueSongs: List<Song> ->
                        controller.playSong(song = song, queueSongs = queueSongs)
                    },
                    onCurrentSongToggle = controller::togglePlayback,
                    onMore = controller::openMore,
                    modifier = modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                )
            } ?: StandardMissingLibraryItemScreen(
                title = "专辑不可用",
                onBack = controller::navigateBack,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }

        SecondaryScreen.ArtistDetail -> {
            state.selectedArtist?.let { artist ->
                ArtistDetailScreen(
                    artist = artist,
                    songs = state.localSongs,
                    currentSongId = state.currentSongId,
                    currentPlaybackStatus = state.playbackStatus,
                    onBack = controller::navigateBack,
                    onSongPlay = { song: Song, queueSongs: List<Song> ->
                        controller.playSong(song = song, queueSongs = queueSongs)
                    },
                    onCurrentSongToggle = controller::togglePlayback,
                    onMore = controller::openMore,
                    onLike = controller::toggleFavorite,
                    modifier =
                        modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                    contentPadding = contentPadding,
                )
            } ?: StandardMissingLibraryItemScreen(
                title = "歌手不可用",
                onBack = controller::navigateBack,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }

        SecondaryScreen.Settings -> {
            SettingsScreen(
                themeMode = state.themeMode,
                scanState = state.scanState,
                discoveryPlatform = discoveryPlatform,
                onThemeMode = controller::setThemeMode,
                onBack = controller::navigateBack,
                onScan = controller::openAudioScan,
                onLocalMusicSources = {
                    controller.openLocalMusic(section = LocalMusicSection.Sources)
                },
                onClearCache = controller::openClearCacheDialog,
                onAbout = { controller.navigateToSecondary(screen = SecondaryScreen.About) },
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }

        SecondaryScreen.About -> {
            AboutScreen(
                onBack = controller::navigateBack,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }

        SecondaryScreen.Login -> {
            LoginScreen(
                email = state.email,
                isMailSent = state.isMailSent,
                onEmail = controller::setEmail,
                onSend = controller::sendLoginMail,
                onBack = controller::navigateBack,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }

        SecondaryScreen.AudioScan -> {
            AudioScanScreen(
                playableSongCount = state.audioScanPlayableSongCount,
                sources = state.localMusicSources,
                scanState = state.scanState,
                discoveryPreferences = state.localMusicDiscoveryPreferences,
                discoveryPlatform = discoveryPlatform,
                onBack = controller::navigateBack,
                onScan = onScanLocalMusic,
                onAutoScanOnLaunchChange = controller::setLocalMusicAutoScanOnLaunchEnabled,
                onShortAudioIgnoredChange = controller::setLocalMusicShortAudioIgnored,
                onSystemFoldersExcludedChange = controller::setLocalMusicSystemFoldersExcluded,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }

        SecondaryScreen.RecentPlayed -> {
            RecentPlayedScreen(
                songs = state.recentSongs,
                currentSongId = state.currentSongId,
                onBack = controller::navigateBack,
                onSongPlay = controller::playRecentSong,
                onSongMore = controller::openMore,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }

        SecondaryScreen.LocalPlaylists -> {
            LocalPlaylistListScreen(
                playlists = state.localPlaylists,
                onBack = controller::navigateBack,
                onManage = controller::openLocalPlaylistManagement,
                onPlaylistOpen = controller::openLocalPlaylistDetail,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }

        SecondaryScreen.LocalPlaylistManagement -> {
            LocalPlaylistManagementScreen(
                playlists = state.localPlaylists,
                selectedPlaylistIds = state.selectedManagedLocalPlaylistIds,
                canDelete = state.canDeleteManagedLocalPlaylists,
                onBack = controller::navigateBack,
                onPlaylistToggle = controller::toggleManagedLocalPlaylistSelection,
                onDelete = controller::openDeleteLocalPlaylistsDialog,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }

        SecondaryScreen.LocalPlaylistDetail -> {
            state.selectedLocalPlaylistDetail?.let { detail ->
                LocalPlaylistDetailScreen(
                    detail = detail,
                    currentSongId = state.currentSongId,
                    currentPlaybackStatus = state.playbackStatus,
                    onBack = controller::navigateBack,
                    onPlayAll = controller::playSelectedLocalPlaylistAll,
                    onSongPlay = controller::playSelectedLocalPlaylistSong,
                    onCurrentSongToggle = controller::togglePlayback,
                    onMore = { song: Song ->
                        controller.openMore(
                            song = song,
                            sourceContext = SongMoreSourceContext.LocalPlaylistDetail,
                        )
                    },
                    modifier = modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                )
            } ?: StandardMissingLibraryItemScreen(
                title = "歌单不可用",
                onBack = controller::navigateBack,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }
    }
}
