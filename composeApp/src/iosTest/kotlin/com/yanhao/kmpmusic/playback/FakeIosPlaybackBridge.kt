package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * iOS 播放 bridge fake，测试只通过平台内部契约观察 native 事实回流。
 */
internal class FakeIosPlaybackBridge(
    // 命令执行顺序记录，帮助验证 audio session 必须早于播放命令。
    private val order: MutableList<String>,
) : IosPlaybackBridge {
    // native 事件通道，由测试主动注入确定性事实。
    private val eventChannel: Channel<IosPlaybackBridgeEvent> = Channel(capacity = Channel.UNLIMITED)

    /** 所有准备请求，确保首版只准备当前媒体而非系统队列。 */
    val prepareRequests: MutableList<IosPlaybackBridgePrepareRequest> = mutableListOf()

    /** 对外暴露 fake native 事件流。 */
    override val events: Flow<IosPlaybackBridgeEvent> = eventChannel.receiveAsFlow()

    /** 记录准备请求并同步确认。 */
    override suspend fun prepare(request: IosPlaybackBridgePrepareRequest): IosPlaybackBridgeCommandAck {
        prepareRequests += request
        order += "prepare:${request.songId}:${request.generation}:${request.startPositionMs}"
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 记录播放命令并同步确认。 */
    override suspend fun play(generation: Long): IosPlaybackBridgeCommandAck {
        order += "play:$generation"
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 记录暂停命令并同步确认。 */
    override suspend fun pause(generation: Long): IosPlaybackBridgeCommandAck {
        order += "pause:$generation"
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 记录 seek 命令并同步确认。 */
    override suspend fun seekTo(request: IosPlaybackBridgeSeekRequest): IosPlaybackBridgeCommandAck {
        order += "seek:${request.generation}:${request.positionMs}"
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 记录停止命令并同步确认。 */
    override suspend fun stop(generation: Long): IosPlaybackBridgeCommandAck {
        order += "stop:$generation"
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 记录归一化音量并同步确认。 */
    override suspend fun setVolume(volume: Float): IosPlaybackBridgeCommandAck {
        order += "volume:$volume"
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 记录释放命令并同步确认。 */
    override suspend fun release(): IosPlaybackBridgeCommandAck {
        order += "release"
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 注入准备完成事实。 */
    fun emitPrepared(
        generation: Long,
        durationMs: Long?,
    ) {
        eventChannel.trySend(
            IosPlaybackBridgeEvent.Prepared(
                generation = generation,
                durationMs = durationMs,
            ),
        )
    }

    /** 注入播放中事实。 */
    fun emitPlaying(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        eventChannel.trySend(
            IosPlaybackBridgeEvent.Playing(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    /** 注入缓冲事实。 */
    fun emitBuffering(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        eventChannel.trySend(
            IosPlaybackBridgeEvent.Buffering(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    /** 注入进度事实。 */
    fun emitProgress(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        eventChannel.trySend(
            IosPlaybackBridgeEvent.Progress(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    /** 注入自然结束事实。 */
    fun emitEnded(generation: Long) {
        eventChannel.trySend(IosPlaybackBridgeEvent.Ended(generation = generation))
    }

    /** 注入系统中断开始事实。 */
    fun emitInterruptionBegan(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        eventChannel.trySend(
            IosPlaybackBridgeEvent.InterruptionBegan(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    /** 注入系统中断结束事实。 */
    fun emitInterruptionEnded(
        generation: Long,
        shouldResume: Boolean,
    ) {
        eventChannel.trySend(
            IosPlaybackBridgeEvent.InterruptionEnded(
                generation = generation,
                shouldResume = shouldResume,
            ),
        )
    }

    /** 注入输出设备断开事实。 */
    fun emitOutputDisconnected(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        eventChannel.trySend(
            IosPlaybackBridgeEvent.OutputDisconnected(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    /** 注入失败事实。 */
    fun emitFailure(
        generation: Long,
        error: PlaybackError,
    ) {
        eventChannel.trySend(
            IosPlaybackBridgeEvent.Failed(
                generation = generation,
                error = error,
            ),
        )
    }
}
