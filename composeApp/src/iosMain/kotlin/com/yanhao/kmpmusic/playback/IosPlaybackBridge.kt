package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import kotlinx.coroutines.flow.Flow

/**
 * iOS AVFoundation bridge 契约，隔离 [IosAvFoundationAudioPlayerEngine] 与原生播放器生命周期。
 */
internal interface IosPlaybackBridge {
    /** 原生播放器事实流，回调线程不做承诺，由引擎重新串行化处理。 */
    val events: Flow<IosPlaybackBridgeEvent>

    /**
     * 准备当前 generation 对应的单个媒体。
     *
     * @param request 当前媒体准备请求。
     * @return native 命令同步确认结果。
     */
    suspend fun prepare(request: IosPlaybackBridgePrepareRequest): IosPlaybackBridgeCommandAck

    /**
     * 开始或继续当前 generation。
     *
     * @param generation 当前媒体代号。
     * @return native 命令同步确认结果。
     */
    suspend fun play(generation: Long): IosPlaybackBridgeCommandAck

    /**
     * 暂停当前 generation。
     *
     * @param generation 当前媒体代号。
     * @return native 命令同步确认结果。
     */
    suspend fun pause(generation: Long): IosPlaybackBridgeCommandAck

    /**
     * 跳转当前 generation 的播放进度。
     *
     * @param request seek 请求。
     * @return native 命令同步确认结果。
     */
    suspend fun seekTo(request: IosPlaybackBridgeSeekRequest): IosPlaybackBridgeCommandAck

    /**
     * 停止当前 generation。
     *
     * @param generation 当前媒体代号。
     * @return native 命令同步确认结果。
     */
    suspend fun stop(generation: Long): IosPlaybackBridgeCommandAck

    /**
     * 设置 App 内归一化音量。
     *
     * @param volume 0.0 到 1.0 的相对音量。
     * @return native 命令同步确认结果。
     */
    suspend fun setVolume(volume: Float): IosPlaybackBridgeCommandAck

    /**
     * 设置 App 内全局播放倍速。
     *
     * @param playbackSpeed 产品支持的离散倍速。
     * @return native 命令同步确认结果。
     */
    suspend fun setPlaybackSpeed(playbackSpeed: PlaybackSpeed): IosPlaybackBridgeCommandAck

    /**
     * 释放原生播放器和观察器。
     *
     * @return native 命令同步确认结果。
     */
    suspend fun release(): IosPlaybackBridgeCommandAck
}

/**
 * iOS bridge 的准备请求。
 *
 * @property songId 当前歌曲标识。
 * @property mediaUri 可交给 [AVPlayer] 的本地媒体 URI。
 * @property generation 当前媒体代号。
 * @property startPositionMs 起始进度，单位毫秒。
 */
internal data class IosPlaybackBridgePrepareRequest(
    val songId: String,
    val mediaUri: String,
    val generation: Long,
    val startPositionMs: Long,
)

/**
 * iOS bridge 的 seek 请求。
 *
 * @property generation 当前媒体代号。
 * @property positionMs 目标进度，单位毫秒。
 */
internal data class IosPlaybackBridgeSeekRequest(
    val generation: Long,
    val positionMs: Long,
)

/**
 * iOS bridge 命令确认结果。
 */
internal sealed interface IosPlaybackBridgeCommandAck {
    /** 命令已被 native 层接受。 */
    data object Accepted : IosPlaybackBridgeCommandAck

    /**
     * 命令被 native 层拒绝。
     *
     * @property error 已归一化的播放错误。
     */
    data class Failed(
        val error: PlaybackError,
    ) : IosPlaybackBridgeCommandAck
}

/**
 * iOS bridge 上报的原生播放事件。
 */
internal sealed interface IosPlaybackBridgeEvent {
    /**
     * 当前媒体已准备到可控状态。
     *
     * @property generation 当前媒体代号。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Prepared(
        val generation: Long,
        val durationMs: Long?,
    ) : IosPlaybackBridgeEvent

    /**
     * 当前媒体正在缓冲。
     *
     * @property generation 当前媒体代号。
     * @property positionMs 当前进度。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Buffering(
        val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : IosPlaybackBridgeEvent

    /**
     * 当前媒体正在播放。
     *
     * @property generation 当前媒体代号。
     * @property positionMs 当前进度。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Playing(
        val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : IosPlaybackBridgeEvent

    /**
     * 当前媒体已暂停。
     *
     * @property generation 当前媒体代号。
     * @property positionMs 当前进度。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Paused(
        val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : IosPlaybackBridgeEvent

    /**
     * 当前媒体进度更新。
     *
     * @property generation 当前媒体代号。
     * @property positionMs 当前进度。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Progress(
        val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : IosPlaybackBridgeEvent

    /**
     * 当前媒体自然结束。
     *
     * @property generation 当前媒体代号。
     */
    data class Ended(
        val generation: Long,
    ) : IosPlaybackBridgeEvent

    /**
     * 当前媒体播放失败。
     *
     * @property generation 当前媒体代号。
     * @property error 已归一化的播放错误。
     */
    data class Failed(
        val generation: Long,
        val error: PlaybackError,
    ) : IosPlaybackBridgeEvent

    /**
     * 系统音频中断开始。
     *
     * @property generation 当前媒体代号。
     * @property positionMs 中断发生时的进度。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class InterruptionBegan(
        val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : IosPlaybackBridgeEvent

    /**
     * 系统音频中断结束。
     *
     * @property generation 当前媒体代号。
     * @property shouldResume 系统是否提示可以恢复播放。
     */
    data class InterruptionEnded(
        val generation: Long,
        val shouldResume: Boolean,
    ) : IosPlaybackBridgeEvent

    /**
     * 当前输出路线断开。
     *
     * @property generation 当前媒体代号。
     * @property positionMs 断开时的进度。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class OutputDisconnected(
        val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : IosPlaybackBridgeEvent
}
