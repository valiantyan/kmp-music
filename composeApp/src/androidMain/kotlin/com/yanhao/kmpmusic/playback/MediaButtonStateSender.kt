package com.yanhao.kmpmusic.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController

/**
 * 媒体按钮偏好发送器，封装 App controller 到 session 的 custom command。
 */
@UnstableApi
internal object MediaButtonStateSender {
    /**
     * 通过官方 [MediaController.sendCustomCommand] 请求 service 更新媒体按钮偏好。
     */
    fun send(controller: MediaController, state: MediaButtonState?) {
        val safeState: MediaButtonState = state ?: return
        if (!controller.isSessionCommandAvailable(AndroidPlaybackMediaButtons.updateButtonsCommand())) {
            return
        }
        controller.sendCustomCommand(
            AndroidPlaybackMediaButtons.updateButtonsCommand(),
            AndroidPlaybackMediaButtons.createUpdateButtonsArgs(state = safeState),
        )
    }
}
