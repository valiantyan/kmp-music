package com.yanhao.kmpmusic

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.screen.LocalMusicDiscoveryPlatform
import platform.UIKit.UIViewController

/**
 * iOS 入口，供 SwiftUI/UIKit 宿主调用。
 */
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController =
    ComposeUIViewController {
        val controller: MusicAppController =
            remember {
                IosPlaybackSession.controller
            }
        LaunchedEffect(Unit) {
            IosPlaybackSession.ensurePlaybackSnapshotRestoreRequested()
        }
        App(
            controller = controller,
            discoveryPlatform = LocalMusicDiscoveryPlatform.Ios,
        )
    }
