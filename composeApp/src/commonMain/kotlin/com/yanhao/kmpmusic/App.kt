package com.yanhao.kmpmusic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.yanhao.kmpmusic.feature.app.MusicApp
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * 跨平台共享 UI 入口。
 */
@Composable
fun App(
    controller: MusicAppController? = null,
) {
    val controllerScope = rememberCoroutineScope()
    val appController: MusicAppController = controller ?: remember(controllerScope) {
        MusicAppController(controllerScope = controllerScope)
    }
    MusicApp(controller = appController)
}
