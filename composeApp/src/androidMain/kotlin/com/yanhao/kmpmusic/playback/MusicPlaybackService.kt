package com.yanhao.kmpmusic.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.yanhao.kmpmusic.AndroidPlaybackSession
import com.yanhao.kmpmusic.MainActivity
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus

/**
 * 通知正文点击打开播放页的 requestCode，保持固定值以便系统复用同一个 [PendingIntent]。
 */
private const val OPEN_PLAYER_REQUEST_CODE = 1001

/**
 * Android Media3 播放服务，按官方架构承载 [ExoPlayer] 与 [MediaSession]。
 */
@UnstableApi
class MusicPlaybackService : MediaSessionService() {
    // 当前 service 暴露给 MediaController 的 session。
    private var mediaSession: MediaSession? = null

    // 当前 service 持有的真实 ExoPlayer。
    private var player: ExoPlayer? = null

    // 最近一次给系统媒体通知控制器声明的按钮偏好。
    private var latestMediaButtonPreferences: List<CommandButton> =
        AndroidPlaybackMediaButtons.mediaButtonPreferences(
            isPlaying = false,
            isFavorite = false,
            playbackMode = PlaybackMode.LoopAll,
        )

    /** 初始化真实播放器和 MediaSession，播放命令由 [MediaController] 连接进入。 */
    override fun onCreate() {
        super.onCreate()
        AndroidPlaybackSession.bootstrap(context = applicationContext)
        setMediaNotificationProvider(
            AndroidPlaybackMediaNotificationProvider(context = applicationContext),
        )
        val exoPlayer: ExoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        val session: MediaSession = MediaSession.Builder(
            /* context = */ this,
            /* player = */ exoPlayer,
        )
            .setCallback(
                AndroidPlaybackMediaSessionCallback(
                    mediaButtonPreferencesProvider = { latestMediaButtonPreferences },
                    updateMediaButtonPreferences = ::refreshMediaButtonPreferences,
                    clearMediaNotification = ::clearMediaNotification,
                ),
            )
            .setMediaButtonPreferences(latestMediaButtonPreferences)
            .setSessionActivity(createOpenPlayerPendingIntent())
            .build()
        mediaSession = session
        addSession(session)
    }

    /** 统一返回当前 session，允许 App、系统和外部控制器连接到此 service。 */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * 用户从最近任务划掉 App 时，音乐产品语义是播放中继续，非播放中停止后台 service。
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (isPlaybackOngoing() && player?.isPlaying == true) {
            return
        }
        pauseAllPlayersAndStopSelf()
    }

    /** 释放 service 资源，确保 MediaSession 和 ExoPlayer 不泄漏。 */
    override fun onDestroy() {
        player?.let { exoPlayer: ExoPlayer ->
            AndroidPlaybackSession.controller.persistPlaybackSnapshotForServiceTeardown(
                positionMs = exoPlayer.currentPosition.coerceAtLeast(minimumValue = 0L),
                durationMs = exoPlayer.duration.takeIf { durationMs: Long -> durationMs > 0L },
            )
        }
        mediaSession?.let { session: MediaSession ->
            if (isSessionAdded(session)) {
                removeSession(session)
            }
            session.release()
        }
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    /** 依据 shared 状态刷新 Media3 媒体按钮偏好。 */
    private fun refreshMediaButtonPreferences(state: MediaButtonState) {
        latestMediaButtonPreferences = AndroidPlaybackMediaButtons.mediaButtonPreferences(
            isPlaying = state.isPlaying,
            isFavorite = state.isFavorite,
            playbackMode = state.playbackMode,
        )
        mediaSession?.setMediaButtonPreferences(latestMediaButtonPreferences)
        if (state.playbackStatus == PlaybackStatus.Idle) {
            clearMediaNotification()
        }
    }

    /** 创建 Media3 session activity，系统通知正文点击后回到当前播放页。 */
    private fun createOpenPlayerPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            OPEN_PLAYER_REQUEST_CODE,
            MainActivity.createOpenPlayerIntent(context = this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * 当前没有活动歌曲时清空 Media3 播放列表，让默认系统媒体通知随 service 空闲自然撤下。
     */
    private fun clearMediaNotification() {
        player?.clearMediaItems()
        stopSelf()
    }
}
