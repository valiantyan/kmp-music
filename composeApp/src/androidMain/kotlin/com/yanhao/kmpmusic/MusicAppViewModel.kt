package com.yanhao.kmpmusic

import androidx.lifecycle.ViewModel
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * Android 配置变化期间持有共享控制器，避免旋转、深浅色或字体变化重建页面状态。
 */
class MusicAppViewModel : ViewModel() {
    /**
     * 共享 App 控制器，生命周期跟随 [MusicAppViewModel]。
     */
    val controller: MusicAppController = MusicAppController()
}
