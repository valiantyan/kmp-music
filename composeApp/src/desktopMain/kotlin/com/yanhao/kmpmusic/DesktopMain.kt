package com.yanhao.kmpmusic

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicApp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import java.awt.Desktop
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.desktop.AppReopenedListener
import javax.swing.SwingUtilities

/**
 * Desktop 入口。
 */
fun main(): Unit =
    application {
        var isMainWindowVisible: Boolean by remember { mutableStateOf(value = true) }
        val windowState =
            rememberWindowState(
                width = DesktopMusicDimens.DefaultWindowWidth,
                height = DesktopMusicDimens.DefaultWindowHeight,
            )
        val showMainWindow: () -> Unit = {
            isMainWindowVisible = true
        }
        val closeMainWindow: () -> Unit = {
            if (shouldKeepRunningAfterMainWindowClose()) {
                isMainWindowVisible = false
            } else {
                exitApplication()
            }
        }
        DisposableEffect(Unit) {
            val reopenRegistration: AutoCloseable? =
                registerMacosAppReopenHandler(
                    onReopen = showMainWindow,
                )
            onDispose {
                reopenRegistration?.close()
                DesktopPlaybackSession.close()
            }
        }
        Window(
            onCloseRequest = closeMainWindow,
            title = "KMP Music",
            state = windowState,
            visible = isMainWindowVisible,
        ) {
            LaunchedEffect(Unit) {
                DesktopPlaybackSession.ensurePlaybackSnapshotRestoreRequested()
            }
            LaunchedEffect(isMainWindowVisible) {
                if (isMainWindowVisible) {
                    window.toFront()
                    window.requestFocus()
                }
            }
            DisposableEffect(window) {
                window.applyMacosMinimumWindowSize()
                if (isMacosHost()) {
                    window.rootPane.applyMacosNativeTitleBar()
                }
                onDispose {
                    if (isMacosHost()) {
                        window.rootPane.clearMacosNativeTitleBar()
                    }
                }
            }
            DesktopMusicApp(
                controller = DesktopPlaybackSession.controller,
                showTitleBarBrand = isMacosHost(),
                titleBarDragArea = {
                    if (isMacosHost()) {
                        WindowDraggableArea(modifier = Modifier.fillMaxSize())
                    }
                },
            )
        }
    }

/**
 * macOS 设计截图是当前桌面壳的最小可用尺寸，防止窗口缩小后内容互相挤压。
 */
private fun java.awt.Window.applyMacosMinimumWindowSize() {
    if (!isMacosHost()) {
        return
    }
    minimumSize =
        Dimension().apply {
            width = DesktopMusicDimens.MinWindowWidth.value.toInt()
            height = DesktopMusicDimens.MinWindowHeight.value.toInt()
        }
}

/**
 * macOS 音乐类应用关闭主窗口后仍应保留播放进程，其他桌面平台暂不改变传统退出语义。
 */
private fun shouldKeepRunningAfterMainWindowClose(): Boolean = isMacosHost()

/**
 * 监听 Dock 图标重新打开事件，让隐藏后的主窗口可以被系统入口恢复。
 */
private fun registerMacosAppReopenHandler(onReopen: () -> Unit): AutoCloseable? {
    if (!shouldRegisterMacosAppReopenHandler()) {
        return null
    }
    val desktop: Desktop = Desktop.getDesktop()
    val listener =
        AppReopenedListener {
            SwingUtilities.invokeLater {
                onReopen()
                desktop.requestForeground(true)
            }
        }
    desktop.addAppEventListener(listener)
    return AutoCloseable {
        desktop.removeAppEventListener(listener)
    }
}

/**
 * 只在支持 AWT 桌面事件的 macOS 图形环境中注册 App reopen 监听。
 */
private fun shouldRegisterMacosAppReopenHandler(): Boolean = isMacosHost() && !GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()
