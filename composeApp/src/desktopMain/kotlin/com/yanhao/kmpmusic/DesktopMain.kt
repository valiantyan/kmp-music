package com.yanhao.kmpmusic

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicApp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens

/**
 * Desktop 入口。
 */
fun main() = application {
    Window(
        onCloseRequest = {
            DesktopPlaybackSession.close()
            exitApplication()
        },
        title = "KMP Music",
        state = WindowState(
            width = DesktopMusicDimens.DefaultWindowWidth,
            height = DesktopMusicDimens.DefaultWindowHeight,
        ),
    ) {
        LaunchedEffect(Unit) {
            DesktopPlaybackSession.ensurePlaybackSnapshotRestoreRequested()
        }
        DesktopMusicApp(controller = DesktopPlaybackSession.controller)
    }
}
