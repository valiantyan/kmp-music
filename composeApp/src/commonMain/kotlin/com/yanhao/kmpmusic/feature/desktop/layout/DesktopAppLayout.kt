package com.yanhao.kmpmusic.feature.desktop.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.app.surfaces.AppDialogs
import com.yanhao.kmpmusic.feature.app.surfaces.AppPanels
import com.yanhao.kmpmusic.feature.desktop.DesktopLibrarySidebar
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.navigation.DesktopRail
import com.yanhao.kmpmusic.feature.desktop.navigation.desktopRailDestination
import com.yanhao.kmpmusic.feature.desktop.player.DesktopBottomPlayer
import com.yanhao.kmpmusic.feature.desktop.player.DesktopPlayerDetailScreen

/**
 * 桌面顶层 layout 负责窗口布局、全屏播放器和全局弹层，不持有具体页面路由。
 */
@Composable
fun DesktopAppLayout(
    state: MusicAppUiState,
    controller: MusicAppController,
    saveableStateHolder: SaveableStateHolder,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (state.navigationState.secondaryScreen == SecondaryScreen.Player) {
            saveableStateHolder.SaveableStateProvider(key = state.navigationState.scrollStateKey) {
                DesktopPlayerDetailScreen(
                    song = state.currentSong,
                    queueSongs = state.queueSongs,
                    isPlaying = state.shouldShowPauseControl,
                    playbackPositionMs = state.playbackPositionMs,
                    playbackDurationMs = state.playbackDurationMs,
                    playbackMode = state.playbackMode,
                    volume = state.playbackVolume,
                    onBack = controller::navigateBack,
                    onToggle = controller::togglePlayback,
                    onPrev = { controller.moveTrack(direction = -1) },
                    onNext = { controller.moveTrack(direction = 1) },
                    onMode = controller::cyclePlaybackMode,
                    onLike = controller::toggleFavorite,
                    onSeek = controller::seekTo,
                    onVolumeChange = controller::setVolume,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            AppDialogs(state = state, controller = controller)
            AppPanels(state = state, controller = controller)
            return@Box
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DesktopMusicColors.WindowBackground),
        ) {
            DesktopTitleBar(
                showSearch = state.shouldShowTitlebarMusicSearch,
                onSearch = {
                    val context: SearchContext = when (state.navigationState.rootTab) {
                        RootTab.Favorites -> SearchContext.Favorites
                        RootTab.Home,
                        RootTab.Me,
                        -> SearchContext.LocalLibrary
                    }
                    controller.openSearch(context = context)
                },
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
                        onSection = controller::openLocalMusic,
                        onSongPlay = { song, queueSongs ->
                            controller.playSong(
                                song = song,
                                queueSongs = queueSongs,
                            )
                        },
                        onRecentClear = controller::clearRecentPlaybackHistory,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(DesktopMusicColors.Paper),
                ) {
                    DesktopWorkspaceLayout(
                        state = state,
                        controller = controller,
                        saveableStateHolder = saveableStateHolder,
                        onScanLocalMusic = onScanLocalMusic,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            DesktopBottomPlayer(
                song = state.currentSong,
                isPlaying = state.shouldShowPauseControl,
                playbackPositionMs = state.playbackPositionMs,
                playbackDurationMs = state.playbackDurationMs,
                playbackMode = state.playbackMode,
                volume = state.playbackVolume,
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
        AppDialogs(state = state, controller = controller)
        AppPanels(state = state, controller = controller)
    }
}

/** 首页保持效果图中的资料库侧栏，二级页让内容区获得完整宽度。 */
private fun MusicAppUiState.shouldShowLibrarySidebar(): Boolean {
    return navigationState.secondaryScreen == null && navigationState.rootTab == RootTab.Home
}
