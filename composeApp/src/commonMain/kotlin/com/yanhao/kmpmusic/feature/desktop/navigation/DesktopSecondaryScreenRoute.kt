package com.yanhao.kmpmusic.feature.desktop.navigation

import androidx.compose.runtime.Composable
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.desktop.screens.DesktopAlbumDetailScreen
import com.yanhao.kmpmusic.feature.desktop.screens.DesktopArtistDetailScreen
import com.yanhao.kmpmusic.feature.desktop.screens.DesktopEmptyStateScreen
import com.yanhao.kmpmusic.feature.desktop.screens.DesktopLocalMusicScreen
import com.yanhao.kmpmusic.feature.desktop.screens.DesktopLoginScreen
import com.yanhao.kmpmusic.feature.desktop.screens.DesktopRecentPlayedScreen
import com.yanhao.kmpmusic.feature.desktop.screens.DesktopSearchScreen
import com.yanhao.kmpmusic.feature.desktop.screens.DesktopSettingsScreen

/**
 * 桌面二级路由集中处理非全屏播放器页面，播放器由顶层 [com.yanhao.kmpmusic.feature.desktop.layout.DesktopAppLayout] 拦截。
 */
@Composable
fun DesktopSecondaryScreenRoute(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
) {
    when (state.navigationState.secondaryScreen) {
        is SecondaryScreen.Search -> {
            val searchResult: SearchResult = controller.search()
            DesktopSearchScreen(
                context = state.searchContext,
                query = state.searchQuery,
                scope = state.searchScope,
                result = searchResult,
                history = state.searchHistoryFor(),
                currentSongId = state.currentSongId,
                currentPlaybackStatus = state.playbackStatus,
                onQuery = controller::setSearchQuery,
                onScope = controller::setSearchScope,
                onBack = controller::navigateBack,
                onCommitSearch = controller::commitSearchQueryToHistory,
                onHistoryClick = controller::selectSearchHistory,
                onHistoryRemove = { query: String ->
                    controller.removeSearchHistoryItem(
                        context = state.searchContext,
                        query = query,
                    )
                },
                onHistoryClear = { controller.clearSearchHistory(context = state.searchContext) },
                onSongPlay = { song: Song, queueSongs: List<Song> ->
                    controller.commitSearchQueryToHistory()
                    controller.playSong(
                        song = song,
                        queueSongs = queueSongs,
                    )
                },
                onMore = controller::openMore,
                onAlbumOpen = { album: Album ->
                    controller.commitSearchQueryToHistory()
                    controller.openAlbum(album = album)
                },
                onArtistOpen = { artist: Artist ->
                    controller.commitSearchQueryToHistory()
                    controller.openArtist(artist = artist)
                },
            )
        }
        SecondaryScreen.Player -> Unit
        SecondaryScreen.AlbumDetail -> DesktopAlbumDetailScreen(
            album = state.selectedAlbum,
            songs = state.localSongs,
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            onBack = controller::navigateBack,
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(
                    song = song,
                    queueSongs = queueSongs,
                )
            },
            onMore = controller::openMore,
        )
        SecondaryScreen.ArtistDetail -> DesktopArtistDetailScreen(
            artist = state.selectedArtist,
            songs = state.localSongs,
            albums = state.localAlbums,
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            onBack = controller::navigateBack,
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(
                    song = song,
                    queueSongs = queueSongs,
                )
            },
            onMore = controller::openMore,
        )
        SecondaryScreen.Settings -> DesktopSettingsScreen(
            themeMode = state.themeMode,
            scanState = state.scanState,
            onThemeMode = controller::setThemeMode,
            onBack = controller::navigateBack,
            onScan = onScanLocalMusic,
            onLocalMusicSources = {
                controller.openLocalMusic(section = LocalMusicSection.Sources)
            },
            onClearCache = controller::openClearCacheDialog,
        )
        SecondaryScreen.About -> DesktopEmptyStateScreen(
            title = "关于 KMP Music",
            subtitle = "版本 1.0 · 本地音乐优先",
        )
        SecondaryScreen.Login -> DesktopLoginScreen(
            email = state.email,
            isMailSent = state.isMailSent,
            onEmail = controller::setEmail,
            onSend = controller::sendLoginMail,
            onBack = controller::navigateBack,
        )
        SecondaryScreen.AudioScan -> DesktopEmptyStateScreen(
            title = "扫描音频文件",
            subtitle = "桌面端请使用首页或设置里的添加文件夹入口。",
        )
        SecondaryScreen.RecentPlayed -> DesktopRecentPlayedScreen(
            songs = state.recentSongs,
            onBack = controller::navigateBack,
        )
        is SecondaryScreen.LocalMusic -> DesktopLocalMusicScreen(
            initialSection = state.navigationState.secondaryScreen.initialSection,
            songs = state.localSongs,
            albums = state.localAlbums,
            artists = state.localArtists,
            sources = state.localMusicSources,
            problems = state.localMusicProblems,
            scanState = state.scanState,
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            onBack = controller::navigateBack,
            onScan = onScanLocalMusic,
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(
                    song = song,
                    queueSongs = queueSongs,
                )
            },
            onMore = controller::openMore,
            onAlbumOpen = controller::openAlbum,
            onArtistOpen = controller::openArtist,
        )
        null -> DesktopEmptyStateScreen(
            title = "本地音乐",
            subtitle = "桌面首页",
        )
    }
}
