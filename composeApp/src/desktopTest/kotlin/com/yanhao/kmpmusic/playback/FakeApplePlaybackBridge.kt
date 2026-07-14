package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Apple bridge 的确定性 fake，实现命令记录和手动事件驱动。
 */
internal class FakeApplePlaybackBridge : ApplePlaybackBridge {
    // fake 事件通道，测试通过 emit 方法精确控制回调顺序。
    private val eventChannel: Channel<ApplePlaybackBridgeEvent> = Channel(capacity = Channel.UNLIMITED)

    /** 已收到的 bridge 命令，供测试断言 generation 和 URI。 */
    val commands: MutableList<String> = mutableListOf()

    override val events: Flow<ApplePlaybackBridgeEvent> = eventChannel.receiveAsFlow()

    // 下一次 prepare 命令的 ack，测试可用来模拟同步准备失败。
    private var nextPrepareAck: ApplePlaybackBridgeCommandAck = ApplePlaybackBridgeCommandAck.Accepted

    // 下一次 seek 命令的 ack，测试可用来模拟 native 超时。
    private var nextSeekAck: ApplePlaybackBridgeCommandAck = ApplePlaybackBridgeCommandAck.Accepted

    /** 记录准备命令，并立即返回 accepted ack。 */
    override suspend fun prepare(request: ApplePlaybackBridgePrepareRequest): ApplePlaybackBridgeCommandAck {
        commands += "prepare:${request.songId}:${request.mediaUri}:${request.generation}:${request.startPositionMs}"
        return consumePrepareAck()
    }

    /** 记录播放命令，并立即返回 accepted ack。 */
    override suspend fun play(generation: Long): ApplePlaybackBridgeCommandAck {
        commands += "play:$generation"
        return ApplePlaybackBridgeCommandAck.Accepted
    }

    /** 记录暂停命令，并立即返回 accepted ack。 */
    override suspend fun pause(generation: Long): ApplePlaybackBridgeCommandAck {
        commands += "pause:$generation"
        return ApplePlaybackBridgeCommandAck.Accepted
    }

    /** 记录 seek 命令，并立即返回 accepted ack。 */
    override suspend fun seekTo(request: ApplePlaybackBridgeSeekRequest): ApplePlaybackBridgeCommandAck {
        commands += "seek:${request.generation}:${request.positionMs}"
        return consumeSeekAck()
    }

    /** 记录停止命令，并立即返回 accepted ack。 */
    override suspend fun stop(generation: Long): ApplePlaybackBridgeCommandAck {
        commands += "stop:$generation"
        return ApplePlaybackBridgeCommandAck.Accepted
    }

    /** 记录音量命令，并立即返回 accepted ack。 */
    override suspend fun setVolume(volume: Float): ApplePlaybackBridgeCommandAck {
        commands += "volume:$volume"
        return ApplePlaybackBridgeCommandAck.Accepted
    }

    /** 记录释放命令，并立即返回 accepted ack。 */
    override suspend fun release(): ApplePlaybackBridgeCommandAck {
        commands += "release"
        return ApplePlaybackBridgeCommandAck.Accepted
    }

    /** 手动发出 prepared 事件，模拟 native 准备完成。 */
    fun emitPrepared(generation: Long, durationMs: Long?) {
        eventChannel.trySend(
            element = ApplePlaybackBridgeEvent.Prepared(
                generation = generation,
                durationMs = durationMs,
            ),
        )
    }

    /** 手动发出 buffering 事件，模拟 AVFoundation 缓冲状态。 */
    fun emitBuffering(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        eventChannel.trySend(
            element = ApplePlaybackBridgeEvent.Buffering(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    /** 手动发出 playing 事件，模拟 native 开始播放。 */
    fun emitPlaying(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        eventChannel.trySend(
            element = ApplePlaybackBridgeEvent.Playing(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    /** 手动发出 paused 事件，模拟 native 暂停。 */
    fun emitPaused(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        eventChannel.trySend(
            element = ApplePlaybackBridgeEvent.Paused(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    /** 手动发出 progress 事件，模拟 periodic time observer。 */
    fun emitProgress(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        eventChannel.trySend(
            element = ApplePlaybackBridgeEvent.Progress(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    /** 手动发出自然结束事件，验证推进权仍在 common 协调器。 */
    fun emitEnded(generation: Long) {
        eventChannel.trySend(element = ApplePlaybackBridgeEvent.Ended(generation = generation))
    }

    /** 手动发出失败事件，验证错误归一化和 generation 失效。 */
    fun emitFailure(generation: Long, error: PlaybackError) {
        eventChannel.trySend(
            element = ApplePlaybackBridgeEvent.Failed(
                generation = generation,
                error = error,
            ),
        )
    }

    /** 手动发出初始化失败事件，覆盖 bridge 尚未绑定媒体的失败路径。 */
    fun emitInitializationFailed(error: PlaybackError) {
        eventChannel.trySend(element = ApplePlaybackBridgeEvent.InitializationFailed(error = error))
    }

    /** 配置下一次 prepare 命令返回失败 ack。 */
    fun failNextPrepare(error: PlaybackError) {
        nextPrepareAck = ApplePlaybackBridgeCommandAck.Failed(error = error)
    }

    /** 配置下一次 seek 命令返回超时 ack。 */
    fun timeoutNextSeek(error: PlaybackError) {
        nextSeekAck = ApplePlaybackBridgeCommandAck.TimedOut(error = error)
    }

    /** 读取并重置下一次 prepare ack，保证 fake 行为可重复。 */
    private fun consumePrepareAck(): ApplePlaybackBridgeCommandAck {
        val ack: ApplePlaybackBridgeCommandAck = nextPrepareAck
        nextPrepareAck = ApplePlaybackBridgeCommandAck.Accepted
        return ack
    }

    /** 读取并重置下一次 seek ack，保证 fake 行为可重复。 */
    private fun consumeSeekAck(): ApplePlaybackBridgeCommandAck {
        val ack: ApplePlaybackBridgeCommandAck = nextSeekAck
        nextSeekAck = ApplePlaybackBridgeCommandAck.Accepted
        return ack
    }
}
