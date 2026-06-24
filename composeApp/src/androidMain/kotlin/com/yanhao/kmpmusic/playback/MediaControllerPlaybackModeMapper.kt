package com.yanhao.kmpmusic.playback

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.yanhao.kmpmusic.domain.model.PlaybackMode

/**
 * 播放模式映射器，把 common 播放模式同步到 Media3 repeat/shuffle 设置。
 */
@UnstableApi
internal object MediaControllerPlaybackModeMapper {
    /**
     * 应用播放模式，让系统媒体按钮和 App 内按钮使用同一套循环规则。
     */
    fun apply(controller: MediaController, playbackMode: PlaybackMode) {
        if (controller.isCommandAvailable(Player.COMMAND_SET_REPEAT_MODE)) {
            controller.repeatMode = when (playbackMode) {
                PlaybackMode.LoopAll -> Player.REPEAT_MODE_ALL
                PlaybackMode.LoopOne -> Player.REPEAT_MODE_ONE
                PlaybackMode.Shuffle -> Player.REPEAT_MODE_ALL
            }
        }
        if (controller.isCommandAvailable(Player.COMMAND_SET_SHUFFLE_MODE)) {
            controller.shuffleModeEnabled = playbackMode == PlaybackMode.Shuffle
        }
    }
}
