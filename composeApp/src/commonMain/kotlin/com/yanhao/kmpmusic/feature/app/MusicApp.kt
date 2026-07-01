package com.yanhao.kmpmusic.feature.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.core.theme.KmpMusicTheme
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.feature.app.layout.MobileAppLayout
import kotlinx.coroutines.launch

/**
 * KMP Music 共享 App 入口。
 */
@Composable
fun MusicApp(
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
        PlatformBackHandler(
            enabled = state.canHandleSystemBack,
            onBack = { controller.handleSystemBack() },
        )
        MobileAppLayout(
            state = state,
            controller = controller,
            onScanLocalMusic = scanLocalMusic,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
