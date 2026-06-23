package com.yanhao.kmpmusic

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.yanhao.kmpmusic.data.IosFolderMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * iOS 入口，供 SwiftUI/UIKit 宿主调用。
 */
fun MainViewController() = ComposeUIViewController {
    val controller: MusicAppController = remember {
        MusicAppController(localMusicScanner = IosFolderMusicScanner())
    }
    App(controller = controller)
}
