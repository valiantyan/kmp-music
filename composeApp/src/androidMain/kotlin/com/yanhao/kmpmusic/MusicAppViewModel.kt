package com.yanhao.kmpmusic

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener

/**
 * Android UI 层 ViewModel，只负责把当前 Activity 依赖接到进程级播放会话。
 */
class MusicAppViewModel(
    application: Application,
) : AndroidViewModel(application) {
    /**
     * 暴露进程级共享控制器，后台播放和系统命令都复用同一份状态。
     */
    val controller: MusicAppController

    init {
        AndroidPlaybackSession.bootstrap(context = application.applicationContext)
        controller = AndroidPlaybackSession.controller
    }

    /**
     * 注入当前 Activity 可用的 Android scanner。
     */
    fun attachLocalMusicScanner(scanner: LocalMusicScanner) {
        AndroidPlaybackSession.attachLocalMusicScanner(scanner = scanner)
    }

    /**
     * 注入当前 Activity 可用的系统权限设置入口。
     */
    fun attachPermissionSettingsOpener(opener: PermissionSettingsOpener) {
        AndroidPlaybackSession.attachPermissionSettingsOpener(opener = opener)
    }

    /**
     * 注入 applicationContext，让播放 connector 能按需拉起 Android 播放服务。
     */
    fun attachPlaybackContext(context: Context) {
        AndroidPlaybackSession.attachPlaybackContext(context = context)
    }

    /**
     * 当前 Activity 对应的 ViewModel 销毁时，清空 Activity 绑定依赖，避免泄漏。
     */
    override fun onCleared() {
        AndroidPlaybackSession.clearUiBindings()
        super.onCleared()
    }
}
