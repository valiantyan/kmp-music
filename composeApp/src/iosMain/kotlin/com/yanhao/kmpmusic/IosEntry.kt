package com.yanhao.kmpmusic

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import com.yanhao.kmpmusic.data.IosFolderMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * iOS 入口，供 SwiftUI/UIKit 宿主调用。
 */
fun MainViewController() = ComposeUIViewController {
    val controllerScope = rememberCoroutineScope()
    val controller: MusicAppController = remember(controllerScope) {
        MusicAppController(
            localMusicScanner = IosFolderMusicScanner(),
            controllerScope = controllerScope,
        )
    }
    App(controller = controller)
}
