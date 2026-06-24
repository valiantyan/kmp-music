package com.yanhao.kmpmusic.playback

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Media3 session 回调负责声明系统媒体通知按钮，并把自定义按钮命令回流到 shared controller。
 *
 * @param mediaButtonPreferencesProvider 提供最新按钮状态，确保收藏和播放模式变化能同步到新连接的通知控制器。
 */
@UnstableApi
internal class AndroidPlaybackMediaSessionCallback(
    private val mediaButtonPreferencesProvider: () -> List<CommandButton>,
) : MediaSession.Callback {
    /** 连接媒体通知控制器时，按官方 [CommandButton] 机制声明按钮与自定义命令。 */
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val builder: MediaSession.ConnectionResult.AcceptedResultBuilder =
            MediaSession.ConnectionResult.AcceptedResultBuilder(session)
        if (session.isMediaNotificationController(controller)) {
            builder
                .setAvailableSessionCommands(AndroidPlaybackMediaButtons.availableSessionCommands())
                .setMediaButtonPreferences(mediaButtonPreferencesProvider())
        }
        return builder.build()
    }

    /** 自定义按钮不直接触碰播放器，统一交给 controller-backed dispatcher。 */
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        return Futures.immediateFuture(
            SessionResult(
                AndroidPlaybackMediaButtons.handleCustomCommand(
                    customAction = customCommand.customAction,
                ),
            ),
        )
    }
}
