package com.yanhao.kmpmusic.playback

import android.content.Context
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * Android 进程级播放运行时，负责把共享控制器接到通知刷新和系统命令桥。
 */
class AndroidPlaybackRuntime(
    // 负责惰性拉起播放服务并把通知刷新转发给 service。
    private val serviceConnector: PlaybackServiceConnector,
) : PlaybackMediaButtonActions {
    // 当前进程级共享控制器，供通知与系统命令复用。
    private var controller: MusicAppController? = null

    /**
     * 注入 Android applicationContext，让播放服务能被惰性拉起。
     */
    fun attachContext(context: Context) {
        serviceConnector.attachContext(context = context)
    }

    /**
     * 接入共享控制器，并把通知动作和系统媒体命令都回流到同一条命令路径。
     */
    fun attachController(controller: MusicAppController) {
        this.controller = controller
        PlaybackMediaCommandDispatcher.attach(actions = this)
        PlaybackCommandBridgeRegistry.attach(bridge = this)
        controller.attachPlaybackUiObserver(observer = ::onPlaybackUiStateChanged)
    }

    /**
     * 控制器状态变化后刷新或移除通知，保证前台服务状态始终跟随共享状态。
     */
    private fun onPlaybackUiStateChanged(uiState: MusicAppUiState) {
        val song = uiState.currentSong
        if (song == null || uiState.playbackStatus == com.yanhao.kmpmusic.domain.model.PlaybackStatus.Idle) {
            serviceConnector.clearNotification()
            return
        }
        serviceConnector.refreshMediaButtonPreferences(
            isPlaying = uiState.isPlaying,
            isFavorite = uiState.likedSongIds.contains(element = song.id),
            playbackMode = uiState.playbackMode,
            playbackStatus = uiState.playbackStatus,
        )
    }

    /** 系统播放命令显式走 shared 控制器，避免依赖 toggle 猜状态。 */
    override fun play() {
        controller?.play()
    }

    /** 系统暂停命令显式走 shared 控制器，避免 buffering/loading 态被忽略。 */
    override fun pause() {
        controller?.pause()
    }

    /** 上一首命令始终走共享控制器，避免系统直接改 ExoPlayer。 */
    override fun previous() {
        controller?.moveTrack(direction = -1)
    }

    /** 下一首命令始终走共享控制器，避免系统直接改 ExoPlayer。 */
    override fun next() {
        controller?.moveTrack(direction = 1)
    }

    /** Seek 命令统一先改 shared 状态，再由协调器驱动真引擎。 */
    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs = positionMs)
    }

    /** 精确下标切歌必须经共享控制器更新完整队列状态。 */
    override fun skipToQueueIndex(index: Int, positionMs: Long) {
        controller?.skipToQueueIndex(
            index = index,
            positionMs = positionMs,
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
