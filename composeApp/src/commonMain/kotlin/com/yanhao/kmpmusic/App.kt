package com.yanhao.kmpmusic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yanhao.kmpmusic.feature.app.MusicApp
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * 跨平台共享 UI 入口。
 */
@Composable
fun App(
    controller: MusicAppController? = null,
) {
    val appController: MusicAppController = controller ?: remember { MusicAppController() }
    MusicApp(controller = appController)
}
