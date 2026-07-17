package com.yanhao.kmpmusic

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicApp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.awt.desktop.AppReopenedListener
import javax.swing.JRootPane
import javax.swing.SwingUtilities

/**
 * Desktop 入口。
 */
fun main(): Unit = application {
    var isMainWindowVisible: Boolean by remember { mutableStateOf(value = true) }
    val windowState = rememberWindowState(
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
        val reopenRegistration: AutoCloseable? = registerMacosAppReopenHandler(
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
 * macOS 音乐类应用关闭主窗口后仍应保留播放进程，其他桌面平台暂不改变传统退出语义。
 */
private fun shouldKeepRunningAfterMainWindowClose(): Boolean {
    return isMacosHost()
}

/**
 * 监听 Dock 图标重新打开事件，让隐藏后的主窗口可以被系统入口恢复。
 */
private fun registerMacosAppReopenHandler(onReopen: () -> Unit): AutoCloseable? {
    if (!shouldRegisterMacosAppReopenHandler()) {
        return null
    }
    val desktop: Desktop = Desktop.getDesktop()
    val listener = AppReopenedListener {
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
private fun shouldRegisterMacosAppReopenHandler(): Boolean {
    return isMacosHost() && !GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()
}

/**
 * 判断当前宿主是否是 macOS，避免把 macOS traffic lights 语义套到其他桌面系统。
 */
private fun isMacosHost(): Boolean {
    return System.getProperty("os.name").contains(
        other = "mac",
        ignoreCase = true,
    )
}

/**
 * 启用 macOS 原生 traffic lights，同时允许 Compose 内容延伸到透明标题栏。
 */
private fun JRootPane.applyMacosNativeTitleBar(): Unit {
    putClientProperty("apple.awt.fullWindowContent", true)
    putClientProperty("apple.awt.transparentTitleBar", true)
    putClientProperty("apple.awt.windowTitleVisible", false)
}

/**
 * 还原 macOS 标题栏属性，避免窗口销毁后属性残留影响后续测试窗口。
 */
private fun JRootPane.clearMacosNativeTitleBar(): Unit {
    putClientProperty("apple.awt.fullWindowContent", false)
    putClientProperty("apple.awt.transparentTitleBar", false)
    putClientProperty("apple.awt.windowTitleVisible", true)
}
