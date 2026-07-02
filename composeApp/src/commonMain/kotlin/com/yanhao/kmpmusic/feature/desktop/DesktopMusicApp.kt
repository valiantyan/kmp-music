package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.core.theme.KmpMusicTheme
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.desktop.layout.DesktopAppLayout
import kotlinx.coroutines.launch

/**
 * 桌面端公开入口，仅负责主题、扫描回调和首屏资料库加载。
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
    val saveableStateHolder: SaveableStateHolder = rememberSaveableStateHolder()
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
        DesktopAppLayout(
            state = state,
            controller = controller,
            saveableStateHolder = saveableStateHolder,
            onScanLocalMusic = scanLocalMusic,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
