package com.yanhao.kmpmusic.feature.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.core.theme.KmpMusicTheme
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.feature.app.layout.MobileAppLayout
import com.yanhao.kmpmusic.feature.screen.LocalMusicDiscoveryPlatform

/**
 * KMP Music 共享 App 入口。
 */
@Composable
fun MusicApp(
    controller: MusicAppController,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
) {
    val state: MusicAppUiState = controller.uiState
    val scanLocalMusic: () -> Unit = {
        controller.requestLocalMusicScan(request = LocalMusicScanRequest.Refresh)
    }
    LaunchedEffect(
        state.navigationState.rootTab,
        state.navigationState.secondaryScreen,
        state.libraryStats.songCount,
        state.localSongs.size,
    ) {
        if (state.navigationState.rootTab == RootTab.Home &&
            state.navigationState.secondaryScreen == null &&
            state.libraryStats.songCount > 0 &&
            state.localSongs.isEmpty()
        ) {
            controller.loadLocalMusicLibrary()
        }
    }
    KmpMusicTheme(themeMode = state.themeMode) {
        PlatformBackHandler(
            enabled = state.canHandleSystemBack,
            onBack = { controller.handleSystemBack() },
        )
        MobileAppLayout(
            state = state,
            controller = controller,
            discoveryPlatform = discoveryPlatform,
            onScanLocalMusic = scanLocalMusic,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
