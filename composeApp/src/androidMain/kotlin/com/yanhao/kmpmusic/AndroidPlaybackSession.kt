package com.yanhao.kmpmusic

import android.content.Context
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener

/**
 * Android 进程级播放会话，统一拥有真实控制器、Room 持久化和播放运行时。
 */
object AndroidPlaybackSession {
    // 进程级播放运行时承接真实依赖和 attach 时序，facade 只保留对外 API。
    private val runtime: AndroidPlaybackSessionRuntime = AndroidPlaybackSessionRuntime()

    /**
     * 当前进程级共享控制器，后台播放和系统命令都复用同一份状态。
     */
    val controller: MusicAppController
        get() = runtime.controller

    /**
     * 确保进程级播放会话已初始化并拿到 applicationContext，供 Activity 与 service 共用。
     */
    fun bootstrap(context: Context) {
        runtime.bootstrap(context = context)
    }

    /**
     * 兼容 Activity 重建时的显式接线入口，内部直接复用 [bootstrap]。
     */
    fun attachPlaybackContext(context: Context) {
        runtime.attachPlaybackContext(context = context)
    }

    /**
     * 仅在当前进程会话的首次 UI 接入时请求冷启动恢复，避免 ViewModel 重建时暂停活动播放。
     */
    fun ensurePlaybackSnapshotRestoreRequested() {
        runtime.ensurePlaybackSnapshotRestoreRequested()
    }

    /**
     * 注入当前 Activity 可用的 Android scanner。
     */
    fun attachLocalMusicScanner(scanner: LocalMusicScanner) {
        runtime.attachLocalMusicScanner(scanner = scanner)
    }

    /**
     * 注入当前 Activity 可用的系统权限设置入口。
     */
    fun attachPermissionSettingsOpener(opener: PermissionSettingsOpener) {
        runtime.attachPermissionSettingsOpener(opener = opener)
    }

    /**
     * 当前 Activity 销毁时清空 UI 依赖，避免持有过期 Activity 引用。
     */
    fun clearUiBindings() {
        runtime.clearUiBindings()
    }
}
