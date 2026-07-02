package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import kotlinx.coroutines.CompletableDeferred

/**
 * 桌面播放引擎的串行命令模型，确保所有状态变更都经过同一入口。
 */
internal sealed interface DesktopPlaybackCommand {
    /**
     * 用新队列替换当前引擎状态，并在准备入队完成后回 ACK。
     */
    data class SetQueue(
        val items: List<PlayableMedia>,
        val startIndex: Int,
        val startPositionMs: Long,
        val ack: CompletableDeferred<Unit>,
    ) : DesktopPlaybackCommand

    /**
     * 请求开始或继续播放当前代媒体。
     */
    data object Play : DesktopPlaybackCommand

    /**
     * 请求暂停当前代媒体。
     */
    data object Pause : DesktopPlaybackCommand

    /**
     * 请求跳转当前代媒体进度。
     */
    data class SeekTo(val positionMs: Long) : DesktopPlaybackCommand

    /**
     * 请求切到队列中的目标下标。
     */
    data class SkipToIndex(val index: Int) : DesktopPlaybackCommand

    /**
     * 保留播放模式同步接口，便于后续桌面能力继续接线。
     */
    data class SetPlaybackMode(val playbackMode: PlaybackMode) : DesktopPlaybackCommand

    /**
     * 请求设置当前播放器音量，值为 0.0 到 1.0。
     */
    data class SetVolume(val volume: Float) : DesktopPlaybackCommand

    /**
     * 请求停止当前媒体并回到 idle。
     */
    data object Stop : DesktopPlaybackCommand

    /**
     * 请求释放桌面底层资源。
     */
    data object Release : DesktopPlaybackCommand

    /**
     * 把桌面适配器回调重新包装回串行命令流。
     */
    data class AdapterEventReceived(val event: DesktopMediaPlayerEvent) : DesktopPlaybackCommand

    /**
     * 进度轮询转化成的内部命令。
     */
    data object ProgressTick : DesktopPlaybackCommand
}
