package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * 桌面 Apple 播放引擎，把 [ApplePlaybackBridge] 的原生事实规整成共享 [AudioPlayerEngine] 事件。
 */
internal class DesktopAppleAudioPlayerEngine(
    // Apple native bridge，真实实现和 fake 测试实现都只通过该契约接入。
    private val bridge: ApplePlaybackBridge,
    // 外部注入的协程作用域，承接串行命令循环与 bridge 回调订阅生命周期。
    private val scope: CoroutineScope,
    // 串行命令处理使用的上下文，测试可注入可控调度器。
    private val dispatcher: CoroutineContext = Dispatchers.Default,
) : AudioPlayerEngine {
    // 引擎内部生命周期，确保 release 时能完整回收常驻协程。
    private val engineJob: Job = SupervisorJob(parent = scope.coroutineContext[Job])

    // 引擎私有作用域，隔离常驻协程与外部调用方的业务协程。
    private val engineScope: CoroutineScope = CoroutineScope(context = dispatcher + engineJob)

    // 向协调器暴露的平台事件流。
    private val eventChannel: Channel<PlaybackEngineEvent> = Channel(capacity = Channel.UNLIMITED)

    // 引擎唯一命令入口，确保所有状态转换串行执行。
    private val commandChannel: Channel<DesktopApplePlaybackCommand> = Channel(capacity = Channel.UNLIMITED)

    // 引擎内部可变状态，集中承载当前队列、代际和准备态。
    private val state: DesktopPlaybackEngineState = DesktopPlaybackEngineState()

    // 跟踪所有待完成的 [setQueue] 确认，确保 release 与异常退出都能统一收口。
    private val setQueueAckTracker: DesktopSetQueueAckTracker = DesktopSetQueueAckTracker()

    // bridge 事件规整器，只负责从快照推导副作用意图。
    private val bridgeEventReducer: ApplePlaybackBridgeEventReducer = ApplePlaybackBridgeEventReducer()

    // bridge 事件订阅任务，释放后取消以阻止 native 回调继续灌入命令通道。
    private val bridgeEventJob: Job =
        engineScope.launch {
            bridge.events.collect { event: ApplePlaybackBridgeEvent ->
                commandChannel.send(
                    element = DesktopApplePlaybackCommand.BridgeEventReceived(event = event),
                )
            }
        }

    // 串行命令循环任务，负责把所有状态变更压到同一条执行序列中。
    private val commandLoopJob: Job =
        engineScope.launch {
            DesktopApplePlaybackCommandLoop(
                commands = commandChannel,
                handleCommand = ::handle,
                onFinally = setQueueAckTracker::completeAll,
            ).run()
        }

    // 释放流程是否已开始；一旦开始就不再接受新的外部命令。
    @Volatile
    private var isReleasing: Boolean = false

    // 引擎是否已经释放；释放后只丢弃后续命令与回调。
    @Volatile
    private var isReleased: Boolean = false

    override val events: Flow<PlaybackEngineEvent> = eventChannel.receiveAsFlow()

    /**
     * 用新的媒体队列替换当前引擎状态，并等待串行命令循环完成入队准备。
     */
    override suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        if (isReleased || isReleasing) {
            return
        }
        val ack: CompletableDeferred<Unit> = CompletableDeferred()
        setQueueAckTracker.register(ack = ack)
        val sendResult =
            commandChannel.trySend(
                element =
                    DesktopApplePlaybackCommand.SetQueue(
                        items = items,
                        startIndex = startIndex,
                        startPositionMs = startPositionMs.coerceAtLeast(minimumValue = 0L),
                        ack = ack,
                    ),
            )
        if (sendResult.isFailure) {
            setQueueAckTracker.complete(ack = ack)
        }
        ack.await()
    }

    /** 记录播放意图；若媒体已准备好则立刻下发到底层 bridge。 */
    override fun play() {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(element = DesktopApplePlaybackCommand.Play)
    }

    /** 清空待播放意图，并在已准备时立即下发暂停命令。 */
    override fun pause() {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(element = DesktopApplePlaybackCommand.Pause)
    }

    /** 记录或执行 seek，请求始终只保留当前代最后一次位置。 */
    override fun seekTo(positionMs: Long) {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(
            element =
                DesktopApplePlaybackCommand.SeekTo(
                    positionMs = positionMs.coerceAtLeast(minimumValue = 0L),
                ),
        )
    }

    /** 直接切到目标下标，并让旧媒体的后续回调全部失效。 */
    override fun skipToIndex(index: Int) {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(element = DesktopApplePlaybackCommand.SkipToIndex(index = index))
    }

    /** 当前任务阶段无需向 Apple bridge 同步播放模式，保留命令以保持契约完整。 */
    override fun setPlaybackMode(playbackMode: PlaybackMode) {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(
            element = DesktopApplePlaybackCommand.SetPlaybackMode(playbackMode = playbackMode),
        )
    }

    /** 将归一化音量交给命令循环串行处理，避免 UI 线程直接触碰 native bridge。 */
    override fun setVolume(volume: Float) {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(
            element =
                DesktopApplePlaybackCommand.SetVolume(
                    volume = volume.coerceIn(minimumValue = 0f, maximumValue = 1f),
                ),
        )
    }

    /** 停止当前媒体并把引擎推回 idle。 */
    override fun stop() {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(element = DesktopApplePlaybackCommand.Stop)
    }

    /** 桌面端显式释放 Apple bridge 资源，并屏蔽后续延迟回调。 */
    fun release() {
        if (isReleased || isReleasing) {
            return
        }
        isReleasing = true
        commandChannel.trySend(element = DesktopApplePlaybackCommand.Release)
    }

    /** 释放并等待命令循环完全收尾，供 Desktop 进程退出前安全关闭原生资源。 */
    suspend fun releaseAndAwait() {
        release()
        commandLoopJob.join()
    }

    /** 串行消费所有外部命令与 bridge 回调，避免并发改写引擎状态。 */
    private suspend fun handle(command: DesktopApplePlaybackCommand) {
        if (isReleased) {
            if (command is DesktopApplePlaybackCommand.SetQueue) {
                setQueueAckTracker.complete(ack = command.ack)
            }
            return
        }
        when (command) {
            is DesktopApplePlaybackCommand.SetQueue -> handleSetQueue(command = command)
            DesktopApplePlaybackCommand.Play -> handlePlay()
            DesktopApplePlaybackCommand.Pause -> handlePause()
            is DesktopApplePlaybackCommand.SeekTo -> handleSeekTo(positionMs = command.positionMs)
            is DesktopApplePlaybackCommand.SkipToIndex -> handleSkipToIndex(index = command.index)
            is DesktopApplePlaybackCommand.SetPlaybackMode -> Unit
            is DesktopApplePlaybackCommand.SetVolume -> handleSetVolume(volume = command.volume)
            DesktopApplePlaybackCommand.Stop -> handleStop()
            DesktopApplePlaybackCommand.Release -> handleRelease()
            is DesktopApplePlaybackCommand.BridgeEventReceived -> handleBridgeEvent(event = command.event)
        }
    }

    /** 统一处理队列替换，空队列直接回传失败，非空队列则进入 loading。 */
    private suspend fun handleSetQueue(command: DesktopApplePlaybackCommand.SetQueue) {
        try {
            state.resetForNewQueue(items = command.items)
            if (state.queue.isEmpty()) {
                state.currentIndex = -1
                state.nextGeneration()
                eventChannel.send(element = PlaybackEngineEvent.Failed(error = buildEmptyQueueError()))
                return
            }
            state.currentIndex =
                command.startIndex.coerceIn(
                    minimumValue = 0,
                    maximumValue = state.queue.lastIndex,
                )
            val startPositionMs: Long = coercePlaybackPositionToCurrentMedia(positionMs = command.startPositionMs)
            state.pendingSeekMs = startPositionMs
            prepareCurrentMedia(startPositionMs = startPositionMs)
        } finally {
            setQueueAckTracker.complete(ack = command.ack)
        }
    }

    /** 在媒体未 ready 时只记住播放意图，避免跨线程直接触发 bridge 播放。 */
    private suspend fun handlePlay() {
        if (!state.isCurrentIndexValid()) {
            return
        }
        state.playbackControlIntent = DesktopPlaybackControlIntent.Play
        if (!state.isPrepared) {
            return
        }
        handleBridgeAck(ack = bridge.play(generation = state.generation))
    }

    /** 暂停优先级高于之前的待播放意图，确保最终状态以最后一次命令为准。 */
    private suspend fun handlePause() {
        state.playbackControlIntent = DesktopPlaybackControlIntent.Pause
        if (!state.isPrepared || !state.isCurrentIndexValid()) {
            return
        }
        handleBridgeAck(ack = bridge.pause(generation = state.generation))
    }

    /** 当前代 seek 采用 latest-wins；未准备完成时只缓存最后一个目标位置。 */
    private suspend fun handleSeekTo(positionMs: Long) {
        if (!state.isCurrentIndexValid()) {
            return
        }
        val seekPositionMs: Long = coercePlaybackPositionToCurrentMedia(positionMs = positionMs)
        state.pendingSeekMs = seekPositionMs
        if (!state.isPrepared) {
            return
        }
        val isSeekAccepted: Boolean =
            handleBridgeAck(
                ack =
                    bridge.seekTo(
                        request =
                            ApplePlaybackBridgeSeekRequest(
                                generation = state.generation,
                                positionMs = seekPositionMs,
                            ),
                    ),
            )
        if (!isSeekAccepted) {
            return
        }
        eventChannel.send(
            element =
                PlaybackEngineEvent.ProgressChanged(
                    positionMs = seekPositionMs,
                    durationMs = state.currentMedia()?.durationMs,
                ),
        )
    }

    /** 将 0.0-1.0 的共享音量交给 Apple bridge。 */
    private suspend fun handleSetVolume(volume: Float) {
        handleBridgeAck(ack = bridge.setVolume(volume = volume.coerceIn(minimumValue = 0f, maximumValue = 1f)))
    }

    /** 切歌会重置上一代待播放/待 seek 状态，并让新媒体从头开始准备。 */
    private suspend fun handleSkipToIndex(index: Int) {
        if (state.queue.isEmpty() || index !in state.queue.indices) {
            return
        }
        state.currentIndex = index
        state.playbackControlIntent = DesktopPlaybackControlIntent.None
        state.pendingSeekMs = 0L
        prepareCurrentMedia(startPositionMs = 0L)
    }

    /** 停止当前媒体时先让旧代失效，再回传 idle 给协调器做状态回写。 */
    private suspend fun handleStop() {
        val activeGeneration: Long = state.generation
        state.nextGeneration()
        state.resetPlaybackFlags()
        handleBridgeAck(ack = bridge.stop(generation = activeGeneration))
        eventChannel.send(
            element =
                PlaybackEngineEvent.StatusChanged(
                    status = PlaybackStatus.Idle,
                    positionMs = 0L,
                    durationMs = null,
                ),
        )
    }

    /** 释放时彻底屏蔽后续事件，并停止所有后台订阅。 */
    private suspend fun handleRelease() {
        state.nextGeneration()
        state.resetPlaybackFlags()
        isReleased = true
        setQueueAckTracker.completeAll()
        bridgeEventJob.cancel()
        bridge.release()
        commandChannel.close()
        engineJob.cancel()
    }

    /** 只消费当前 generation 的有效回调，旧媒体与释放后的回调全部丢弃。 */
    private suspend fun handleBridgeEvent(event: ApplePlaybackBridgeEvent) {
        val reduction: ApplePlaybackBridgeEventReduction =
            bridgeEventReducer.reduce(
                snapshot = state.snapshot(),
                event = event,
            )
        reduction.stateUpdates.forEach { update: ApplePlaybackEngineStateUpdate ->
            when (update) {
                ApplePlaybackEngineStateUpdate.MarkPrepared -> state.isPrepared = true
                ApplePlaybackEngineStateUpdate.ResetPlaybackFlags -> state.resetPlaybackFlags()
                ApplePlaybackEngineStateUpdate.AdvanceGeneration -> state.nextGeneration()
            }
        }
        var areBridgeActionsAccepted: Boolean = true
        reduction.bridgeActions.forEach { action: ApplePlaybackBridgeAction ->
            if (!executeBridgeAction(action = action)) {
                areBridgeActionsAccepted = false
            }
        }
        if (!areBridgeActionsAccepted) {
            return
        }
        reduction.events.forEach { reducedEvent: PlaybackEngineEvent ->
            eventChannel.send(element = reducedEvent)
        }
    }

    /** 执行规整出的 bridge 动作，并让失败 ack 阻断后续乐观事件。 */
    private suspend fun executeBridgeAction(action: ApplePlaybackBridgeAction): Boolean =
        when (action) {
            is ApplePlaybackBridgeAction.SeekTo -> {
                handleBridgeAck(
                    ack =
                        bridge.seekTo(
                            request =
                                ApplePlaybackBridgeSeekRequest(
                                    generation = action.generation,
                                    positionMs = action.positionMs,
                                ),
                        ),
                )
            }

            is ApplePlaybackBridgeAction.Play -> {
                handleBridgeAck(ack = bridge.play(generation = action.generation))
            }

            is ApplePlaybackBridgeAction.Pause -> {
                handleBridgeAck(ack = bridge.pause(generation = action.generation))
            }
        }

    /** 为当前下标生成新媒体代号，并用 loading 状态通知上层开始切歌。 */
    private suspend fun prepareCurrentMedia(startPositionMs: Long) {
        val media: PlayableMedia = state.currentMedia() ?: return
        val activeGeneration: Long = state.nextGeneration()
        state.isPrepared = false
        eventChannel.send(
            element =
                PlaybackEngineEvent.CurrentMediaChanged(
                    songId = media.songId,
                    index = state.currentIndex,
                    durationMs = media.durationMs,
                ),
        )
        eventChannel.send(
            element =
                PlaybackEngineEvent.StatusChanged(
                    status = PlaybackStatus.Loading,
                    positionMs = startPositionMs,
                    durationMs = media.durationMs,
                ),
        )
        handleBridgeAck(
            ack =
                bridge.prepare(
                    request =
                        ApplePlaybackBridgePrepareRequest(
                            songId = media.songId,
                            mediaUri = media.audioSource.uri,
                            generation = activeGeneration,
                            startPositionMs = startPositionMs,
                        ),
                ),
        )
    }

    /** 将恢复/拖动位置限制在当前媒体声明时长内，避免持久化不可恢复的越界进度。 */
    private fun coercePlaybackPositionToCurrentMedia(positionMs: Long): Long {
        val safePositionMs: Long = positionMs.coerceAtLeast(minimumValue = 0L)
        val durationMs: Long = state.currentMedia()?.durationMs ?: return safePositionMs
        if (durationMs <= 0L) {
            return safePositionMs
        }
        return safePositionMs.coerceAtMost(maximumValue = durationMs)
    }

    /** 把 bridge 命令失败统一折返成共享失败事件，并让当前 generation 失效。 */
    private suspend fun handleBridgeAck(ack: ApplePlaybackBridgeCommandAck): Boolean {
        when (ack) {
            ApplePlaybackBridgeCommandAck.Accepted -> {
                return true
            }

            is ApplePlaybackBridgeCommandAck.Failed -> {
                state.nextGeneration()
                state.resetPlaybackFlags()
                eventChannel.send(element = PlaybackEngineEvent.Failed(error = ack.error))
                return false
            }

            is ApplePlaybackBridgeCommandAck.TimedOut -> {
                state.nextGeneration()
                state.resetPlaybackFlags()
                eventChannel.send(element = PlaybackEngineEvent.Failed(error = ack.error))
                return false
            }
        }
    }

    /** 构造空队列错误，避免平台引擎抛出越界异常。 */
    private fun buildEmptyQueueError(): PlaybackError =
        PlaybackError(
            type = PlaybackErrorType.MissingFile,
            songId = null,
            message = "播放队列为空",
        )
}
