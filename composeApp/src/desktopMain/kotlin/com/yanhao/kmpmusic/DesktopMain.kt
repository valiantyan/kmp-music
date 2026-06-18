package com.yanhao.kmpmusic

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/**
 * Desktop 入口。
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KMP Music",
    ) {
        App()
    }
}
