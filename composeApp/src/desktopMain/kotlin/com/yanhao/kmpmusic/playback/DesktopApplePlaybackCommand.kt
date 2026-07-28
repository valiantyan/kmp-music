package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import kotlinx.coroutines.CompletableDeferred

/**
 * Apple 桌面播放引擎的串行命令模型，确保 UI 命令和 bridge 回调共享同一条状态通道。
 */
internal sealed interface DesktopApplePlaybackCommand {
    /**
     * 用新队列替换当前引擎状态，并在准备命令入队后回 ACK。
     */
    data class SetQueue(
        val items: List<PlayableMedia>,
        val startIndex: Int,
        val startPositionMs: Long,
        val ack: CompletableDeferred<Unit>,
    ) : DesktopApplePlaybackCommand

    /** 请求开始或继续播放当前媒体代。 */
    data object Play : DesktopApplePlaybackCommand

    /** 请求暂停当前媒体代。 */
    data object Pause : DesktopApplePlaybackCommand

    /** 请求跳转当前媒体代进度。 */
    data class SeekTo(
        val positionMs: Long,
    ) : DesktopApplePlaybackCommand

    /** 请求切到队列中的目标下标。 */
    data class SkipToIndex(
        val index: Int,
    ) : DesktopApplePlaybackCommand

    /** 保留播放模式同步命令，业务语义仍由 common 协调器拥有。 */
    data class SetPlaybackMode(
        val playbackMode: PlaybackMode,
    ) : DesktopApplePlaybackCommand

    /** 请求设置当前播放器音量，值为 0.0 到 1.0。 */
    data class SetVolume(
        val volume: Float,
    ) : DesktopApplePlaybackCommand

    /** 请求设置当前播放器倍速。 */
    data class SetPlaybackSpeed(
        val playbackSpeed: PlaybackSpeed,
    ) : DesktopApplePlaybackCommand

    /** 请求停止当前媒体并回到 idle。 */
    data object Stop : DesktopApplePlaybackCommand

    /** 请求释放 Apple bridge 底层资源。 */
    data object Release : DesktopApplePlaybackCommand

    /** 把 Apple bridge 回调重新包装回串行命令流。 */
    data class BridgeEventReceived(
        val event: ApplePlaybackBridgeEvent,
    ) : DesktopApplePlaybackCommand
}
