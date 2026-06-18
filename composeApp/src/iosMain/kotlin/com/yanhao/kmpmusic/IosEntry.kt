package com.yanhao.kmpmusic

import androidx.compose.ui.window.ComposeUIViewController

/**
 * iOS 入口，供 SwiftUI/UIKit 宿主调用。
 */
fun MainViewController() = ComposeUIViewController {
    App()
}
