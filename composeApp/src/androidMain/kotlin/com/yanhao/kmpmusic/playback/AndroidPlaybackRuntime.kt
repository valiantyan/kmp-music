package com.yanhao.kmpmusic.playback

import android.content.Context
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * Android 进程级播放运行时，负责把共享控制器接到通知刷新和 Media3 自定义按钮。
 */
class AndroidPlaybackRuntime(
    // 负责惰性拉起播放服务并把通知刷新转发给 service。
    private val serviceConnector: PlaybackServiceConnector,
) : PlaybackMediaButtonActions {
    // 当前进程级共享控制器，供通知自定义按钮复用。
    private var controller: MusicAppController? = null

    /**
     * 注入 Android applicationContext，让播放服务能被惰性拉起。
     */
    fun attachContext(context: Context) {
        serviceConnector.attachContext(context = context)
    }

    /**
     * 接入共享控制器，并把通知自定义动作回流到同一条业务命令路径。
     */
    fun attachController(controller: MusicAppController) {
        this.controller = controller
        PlaybackMediaCommandDispatcher.attach(actions = this)
        controller.attachPlaybackUiObserver(observer = ::onPlaybackUiStateChanged)
    }

    /**
     * 控制器状态变化后刷新或移除通知，保证前台服务状态始终跟随共享状态。
     */
    private fun onPlaybackUiStateChanged(uiState: MusicAppUiState) {
        val song = uiState.currentSong
        if (!uiState.hasActivePlaybackSession) {
            serviceConnector.clearNotification()
            return
        }
        if (song == null) {
            return
        }
        serviceConnector.refreshMediaButtonPreferences(
            shouldShowPauseButton = uiState.shouldShowPauseControl,
            isFavorite = uiState.likedSongIds.contains(element = song.id),
            playbackMode = uiState.playbackMode,
            playbackStatus = uiState.playbackStatus,
            hasActivePlaybackSession = uiState.hasActivePlaybackSession,
        )
    }

    /** 通知收藏动作复用 shared 控制器，避免平台层直接窥探 UI 细节。 */
    override fun toggleFavorite() {
        controller?.toggleCurrentSongFavorite()
    }

    /** 通知播放模式动作复用 shared 控制器，避免直接改队列状态。 */
    override fun cycleMode() {
        controller?.cyclePlaybackMode()
    }
}
