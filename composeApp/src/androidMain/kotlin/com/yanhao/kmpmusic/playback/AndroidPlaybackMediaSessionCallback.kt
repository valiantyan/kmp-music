package com.yanhao.kmpmusic.playback

import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.yanhao.kmpmusic.domain.model.PlaybackStatus

/**
 * Media3 session 回调负责声明系统媒体通知按钮，并把自定义按钮命令回流到 shared controller。
 *
 * @param mediaButtonPreferencesProvider 提供最新按钮状态，确保收藏和播放模式变化能同步到新连接的通知控制器。
 */
@UnstableApi
internal class AndroidPlaybackMediaSessionCallback(
    private val mediaButtonPreferencesProvider: () -> List<CommandButton>,
    private val updateMediaButtonPreferences: (MediaButtonState) -> Unit,
    private val clearMediaNotification: () -> Unit,
) : MediaSession.Callback {
    /** 连接媒体通知控制器时，按官方 [CommandButton] 机制声明按钮与自定义命令。 */
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val builder: MediaSession.ConnectionResult.AcceptedResultBuilder =
            MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(AndroidPlaybackMediaButtons.availableSessionCommands())
        if (session.isMediaNotificationController(controller)) {
            builder.setMediaButtonPreferences(mediaButtonPreferencesProvider())
        }
        return builder.build()
    }

    /**
     * 系统通知或蓝牙触发上一首/下一首时，应遵循 App 内切歌规则：切歌后立即播放。
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onPlayerCommandRequest(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        playerCommand: Int,
    ): Int {
        if (shouldResumeAfterQueueNavigation(playerCommand = playerCommand)) {
            resumePlayerForQueueNavigation(session = session)
        }
        return SessionResult.RESULT_SUCCESS
    }

    /** 自定义按钮不直接触碰播放器，统一交给 controller-backed dispatcher。 */
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        if (AndroidPlaybackMediaButtons.isUpdateButtonsCommand(customAction = customCommand.customAction)) {
            return handleUpdateButtonsCommand(
                session = session,
                args = args,
            )
        }
        return Futures.immediateFuture(
            SessionResult(
                AndroidPlaybackMediaButtons.handleCustomCommand(
                    customAction = customCommand.customAction,
                ),
            ),
        )
    }

    // App 内 controller 通过官方 custom command 刷新媒体按钮偏好。
    private fun handleUpdateButtonsCommand(
        session: MediaSession,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        val state: MediaButtonState = AndroidPlaybackMediaButtons.resolveUpdateButtonsState(args = args)
            ?: return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
        updateMediaButtonPreferences(state)
        if (state.playbackStatus == PlaybackStatus.Idle) {
            clearMediaNotification()
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    // 判断标准 Player 命令是否属于队列导航，而不是普通进度拖动。
    private fun shouldResumeAfterQueueNavigation(playerCommand: Int): Boolean {
        return playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ||
            playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS ||
            playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
            playerCommand == Player.COMMAND_SEEK_TO_NEXT
    }

    // 只在 Player 允许播放/暂停命令时恢复播放，避免越权调用被 Media3 拒绝。
    private fun resumePlayerForQueueNavigation(session: MediaSession) {
        val player: Player = session.player
        if (!player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) {
            return
        }
        player.play()
    }
}
