package com.yanhao.kmpmusic

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicApp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import javax.swing.JRootPane

/**
 * Desktop 入口。
 */
fun main() = application {
    val windowState = rememberWindowState(
        width = DesktopMusicDimens.DefaultWindowWidth,
        height = DesktopMusicDimens.DefaultWindowHeight,
    )
    val closeWindow: () -> Unit = {
        DesktopPlaybackSession.close()
        exitApplication()
    }
    Window(
        onCloseRequest = closeWindow,
        title = "KMP Music",
        state = windowState,
    ) {
        LaunchedEffect(Unit) {
            DesktopPlaybackSession.ensurePlaybackSnapshotRestoreRequested()
        }
        DisposableEffect(window) {
            window.rootPane.applyMacosNativeTitleBar()
            onDispose {
                window.rootPane.clearMacosNativeTitleBar()
            }
        }
        DesktopMusicApp(
            controller = DesktopPlaybackSession.controller,
        )
    }
}

/**
 * 启用 macOS 原生 traffic lights，同时允许 Compose 内容延伸到透明标题栏。
 */
private fun JRootPane.applyMacosNativeTitleBar() {
    putClientProperty("apple.awt.fullWindowContent", true)
    putClientProperty("apple.awt.transparentTitleBar", true)
    putClientProperty("apple.awt.windowTitleVisible", false)
}

/**
 * 还原 macOS 标题栏属性，避免窗口销毁后属性残留影响后续测试窗口。
 */
private fun JRootPane.clearMacosNativeTitleBar() {
    putClientProperty("apple.awt.fullWindowContent", false)
    putClientProperty("apple.awt.transparentTitleBar", false)
    putClientProperty("apple.awt.windowTitleVisible", true)
}
