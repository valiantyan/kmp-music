package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import kotlinx.coroutines.flow.Flow

/**
 * Apple 原生播放器 bridge 契约，约束 JVM/Kotlin 层与 AVFoundation bridge 的唯一交互边界。
 *
 * 契约规则：
 * - Kotlin 引擎负责队列、generation 和共享事件归一化，native bridge 只负责原生播放器调用。
 * - 每个带 generation 的回调只能归因到发起该命令的媒体代；旧 generation 必须被引擎丢弃。
 * - bridge 可以从任意 native 回调线程发出 [events]，引擎会重新串行化后再改写状态。
 * - 每个 suspend 命令返回 [ApplePlaybackBridgeCommandAck] 即视为命令 ack；native 拒绝返回 [ApplePlaybackBridgeCommandAck.Failed]，超时返回 [ApplePlaybackBridgeCommandAck.TimedOut]。
 * - [release] 返回后 native bridge 释放其拥有的 AVPlayer、observer、token 和其他原生资源；释放后的延迟回调不再具备语义。
 */
internal interface ApplePlaybackBridge {
    /** Apple bridge 上报的原生播放事实流。 */
    val events: Flow<ApplePlaybackBridgeEvent>

    /**
     * 准备当前 generation 对应的媒体。
     *
     * @param request 当前媒体准备请求，包含归因和起始进度。
     * @return 命令 ack，失败时引擎会发出共享失败事件。
     */
    suspend fun prepare(request: ApplePlaybackBridgePrepareRequest): ApplePlaybackBridgeCommandAck

    /**
     * 开始或继续播放当前 generation。
     *
     * @param generation 当前媒体代号。
     * @return 命令 ack，失败时引擎会发出共享失败事件。
     */
    suspend fun play(generation: Long): ApplePlaybackBridgeCommandAck

    /**
     * 暂停当前 generation。
     *
     * @param generation 当前媒体代号。
     * @return 命令 ack，失败时引擎会发出共享失败事件。
     */
    suspend fun pause(generation: Long): ApplePlaybackBridgeCommandAck

    /**
     * 跳转当前 generation 的播放进度。
     *
     * @param request seek 请求，包含 generation 与目标进度。
     * @return 命令 ack，失败时引擎会发出共享失败事件。
     */
    suspend fun seekTo(request: ApplePlaybackBridgeSeekRequest): ApplePlaybackBridgeCommandAck

    /**
     * 停止当前 generation 的播放。
     *
     * @param generation 当前媒体代号。
     * @return 命令 ack，失败时引擎会发出共享失败事件。
     */
    suspend fun stop(generation: Long): ApplePlaybackBridgeCommandAck

    /**
     * 设置原生播放器音量。
     *
     * @param volume 归一化音量，范围 0.0 到 1.0。
     * @return 命令 ack，失败时引擎会发出共享失败事件。
     */
    suspend fun setVolume(volume: Float): ApplePlaybackBridgeCommandAck

    /**
     * 设置原生播放器播放倍速。
     *
     * @param playbackSpeed 产品支持的离散倍速。
     * @return 命令 ack，失败时引擎会发出共享失败事件。
     */
    suspend fun setPlaybackSpeed(playbackSpeed: PlaybackSpeed): ApplePlaybackBridgeCommandAck

    /**
     * 释放 native bridge 拥有的全部原生资源。
     *
     * @return 命令 ack；释放失败只作为诊断结果，不允许阻塞引擎收口。
     */
    suspend fun release(): ApplePlaybackBridgeCommandAck
}

/**
 * Apple bridge 的准备请求。
 *
 * @property songId 当前媒体对应的歌曲标识。
 * @property mediaUri 可交给 Apple 原生播放器消费的 URI。
 * @property generation 请求该工作的媒体代号。
 * @property startPositionMs 起始进度，单位毫秒。
 */
internal data class ApplePlaybackBridgePrepareRequest(
    val songId: String,
    val mediaUri: String,
    val generation: Long,
    val startPositionMs: Long,
)

/**
 * Apple bridge 的 seek 请求。
 *
 * @property generation 请求该工作的媒体代号。
 * @property positionMs 目标进度，单位毫秒。
 */
internal data class ApplePlaybackBridgeSeekRequest(
    val generation: Long,
    val positionMs: Long,
)

/**
 * Apple bridge 命令确认结果，避免 native 调用失败时让 suspend 调用方永久等待。
 */
internal sealed interface ApplePlaybackBridgeCommandAck {
    /** 命令已被 native bridge 接受并完成同步部分，异步播放事实仍通过 [ApplePlaybackBridge.events] 回调。 */
    data object Accepted : ApplePlaybackBridgeCommandAck

    /**
     * 命令在 native bridge 层失败或超时。
     *
     * @property error 已归一化的播放错误。
     */
    data class Failed(
        val error: PlaybackError,
    ) : ApplePlaybackBridgeCommandAck

    /**
     * 命令未在 native bridge 约定窗口内完成。
     *
     * @property error 已归一化的播放错误，通常映射为引擎不可用或未知错误。
     */
    data class TimedOut(
        val error: PlaybackError,
    ) : ApplePlaybackBridgeCommandAck
}

/**
 * Apple bridge 上报给平台引擎的原生播放事件。
 */
internal sealed interface ApplePlaybackBridgeEvent {
    /**
     * 媒体准备完成。
     *
     * @property generation 事件对应的媒体代号。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Prepared(
        val generation: Long,
        val durationMs: Long?,
    ) : ApplePlaybackBridgeEvent

    /**
     * 媒体正在缓冲。
     *
     * @property generation 事件对应的媒体代号。
     * @property positionMs 当前进度，单位毫秒。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Buffering(
        val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : ApplePlaybackBridgeEvent

    /**
     * 媒体进入播放中。
     *
     * @property generation 事件对应的媒体代号。
     * @property positionMs 当前进度，单位毫秒。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Playing(
        val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : ApplePlaybackBridgeEvent

    /**
     * 媒体进入暂停。
     *
     * @property generation 事件对应的媒体代号。
     * @property positionMs 当前进度，单位毫秒。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Paused(
        val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : ApplePlaybackBridgeEvent

    /**
     * 当前媒体进度变化。
     *
     * @property generation 事件对应的媒体代号。
     * @property positionMs 当前进度，单位毫秒。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Progress(
        val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : ApplePlaybackBridgeEvent

    /**
     * 当前媒体自然播放结束。
     *
     * @property generation 事件对应的媒体代号。
     */
    data class Ended(
        val generation: Long,
    ) : ApplePlaybackBridgeEvent

    /**
     * 当前媒体播放失败。
     *
     * @property generation 事件对应的媒体代号。
     * @property error 已归一化的播放错误。
     */
    data class Failed(
        val generation: Long,
        val error: PlaybackError,
    ) : ApplePlaybackBridgeEvent

    /**
     * bridge 初始化失败，尚未绑定到具体媒体 generation。
     *
     * @property error 已归一化的播放错误。
     */
    data class InitializationFailed(
        val error: PlaybackError,
    ) : ApplePlaybackBridgeEvent
}
