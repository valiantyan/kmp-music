package com.yanhao.kmpmusic

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp

/**
 * Desktop 入口。
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KMP Music",
        state = WindowState(width = 430.dp, height = 930.dp),
    ) {
        App()
    }
}
