package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 桌面端串行播放引擎，负责把 UI 命令和桌面播放器回调收敛到同一条命令通道。
 */
class DesktopVlcjAudioPlayerEngine(
    // 底层桌面播放器适配缝，真实 vlcj 与测试 fake 共用。
    private val adapter: DesktopMediaPlayerAdapter,
    // 外部注入的协程作用域，承接串行命令循环与回调订阅生命周期。
    private val scope: CoroutineScope,
    // 串行命令处理使用的上下文，测试可注入可控调度器。
    private val dispatcher: CoroutineContext = Dispatchers.Default,
    // LibVLC 插件目录，桌面打包场景由上层传入。
    private val libVlcPluginPath: String?,
    // 播放中进度轮询间隔。
    private val progressIntervalMs: Long = 500L,
) : AudioPlayerEngine {
    // 引擎内部生命周期，确保 release 时能完整回收常驻协程。
    private val engineJob: Job = SupervisorJob(parent = scope.coroutineContext[Job])

    // 引擎私有作用域，隔离常驻协程与外部调用方的业务协程。
    private val engineScope: CoroutineScope = CoroutineScope(context = dispatcher + engineJob)

    // 向协调器暴露的平台事件流。
    private val eventChannel: Channel<PlaybackEngineEvent> = Channel(capacity = Channel.UNLIMITED)

    // 引擎唯一命令入口，确保所有状态转换串行执行。
    private val commandChannel: Channel<EngineCommand> = Channel(capacity = Channel.UNLIMITED)

    // 适配器事件订阅任务，释放后取消以阻止 native 回调继续灌入命令通道。
    private val adapterEventJob: Job = engineScope.launch {
        adapter.events.collect { event: DesktopMediaPlayerEvent ->
            commandChannel.send(
                element = EngineCommand.AdapterEventReceived(event = event),
            )
        }
    }

    // 串行命令循环任务，负责把所有状态变更压到同一条执行序列中。
    private val commandLoopJob: Job = engineScope.launch {
        for (command: EngineCommand in commandChannel) {
            handle(command = command)
        }
    }

    // 当前引擎持有的播放队列。
    private var queue: List<PlayableMedia> = emptyList()

    // 当前激活的队列下标，没有活动媒体时为 -1。
    private var currentIndex: Int = -1

    // 当前媒体代号，每次切歌/停止/释放都会递增以屏蔽旧回调。
    private var generation: Long = 0L

    // 媒体未 ready 前是否应在 ready 后自动开始播放。
    private var pendingPlay: Boolean = false

    // 当前代尚未兑现的 seek 请求，遵循 latest-wins 规则。
    private var pendingSeekMs: Long? = null

    // 当前 generation 是否已经进入 prepared 可控状态。
    private var isPrepared: Boolean = false

    // 释放流程是否已开始；一旦开始就不再接受新的外部命令。
    @Volatile
    private var isReleasing: Boolean = false

    // 引擎是否已经释放；释放后只丢弃后续命令与回调。
    @Volatile
    private var isReleased: Boolean = false

    // 进度轮询任务，便于在暂停/切歌/释放时精准取消。
    private var progressJob: Job? = null

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
        val sendResult = commandChannel.trySend(
            element = EngineCommand.SetQueue(
                items = items,
                startIndex = startIndex,
                startPositionMs = startPositionMs.coerceAtLeast(minimumValue = 0L),
                ack = ack,
            ),
        )
        if (sendResult.isFailure) {
            ack.complete(value = Unit)
        }
        ack.await()
    }

    /** 记录播放意图；若媒体已准备好则立刻下发到底层适配器。 */
    override fun play() {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(element = EngineCommand.Play)
    }

    /** 清空待播放意图，并在已准备时立即下发暂停命令。 */
    override fun pause() {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(element = EngineCommand.Pause)
    }

    /** 记录或执行 seek，请求始终只保留当前代最后一次位置。 */
    override fun seekTo(positionMs: Long) {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(
            element = EngineCommand.SeekTo(
                positionMs = positionMs.coerceAtLeast(minimumValue = 0L),
            ),
        )
    }

    /** 直接切到目标下标，并让旧媒体的后续回调全部失效。 */
    override fun skipToIndex(index: Int) {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(element = EngineCommand.SkipToIndex(index = index))
    }

    /** 当前任务阶段无需向桌面底层同步播放模式，保留命令以保持契约完整。 */
    override fun setPlaybackMode(playbackMode: PlaybackMode) {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(
            element = EngineCommand.SetPlaybackMode(playbackMode = playbackMode),
        )
    }

    /** 停止当前媒体并把引擎推回 idle。 */
    override fun stop() {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(element = EngineCommand.Stop)
    }

    /** 桌面端显式释放原生资源，并屏蔽后续延迟回调。 */
    fun release() {
        if (isReleased || isReleasing) {
            return
        }
        isReleasing = true
        commandChannel.trySend(element = EngineCommand.Release)
    }

    /** 串行消费所有外部命令与适配器回调，避免并发改写引擎状态。 */
    private suspend fun handle(command: EngineCommand) {
        if (isReleased) {
            if (command is EngineCommand.SetQueue) {
                command.ack.complete(value = Unit)
            }
            return
        }
        when (command) {
            is EngineCommand.SetQueue -> handleSetQueue(command = command)
            EngineCommand.Play -> handlePlay()
            EngineCommand.Pause -> handlePause()
            is EngineCommand.SeekTo -> handleSeekTo(positionMs = command.positionMs)
            is EngineCommand.SkipToIndex -> handleSkipToIndex(index = command.index)
            is EngineCommand.SetPlaybackMode -> Unit
            EngineCommand.Stop -> handleStop()
            EngineCommand.Release -> handleRelease()
            is EngineCommand.AdapterEventReceived -> handleAdapterEvent(event = command.event)
            EngineCommand.ProgressTick -> handleProgressTick()
        }
    }

    /** 统一处理队列替换，空队列直接回传失败，非空队列则进入 loading。 */
    private suspend fun handleSetQueue(command: EngineCommand.SetQueue) {
        queue = command.items
        pendingPlay = false
        pendingSeekMs = null
        isPrepared = false
        stopProgressPolling()
        if (queue.isEmpty()) {
            currentIndex = -1
            nextGeneration()
            eventChannel.send(
                element = PlaybackEngineEvent.Failed(
                    error = PlaybackError(
                        type = PlaybackErrorType.MissingFile,
                        songId = null,
                        message = "播放队列为空",
                    ),
                ),
            )
            command.ack.complete(value = Unit)
            return
        }
        currentIndex = command.startIndex.coerceIn(minimumValue = 0, maximumValue = queue.lastIndex)
        pendingSeekMs = command.startPositionMs
        prepareCurrentMedia(startPositionMs = command.startPositionMs)
        command.ack.complete(value = Unit)
    }

    /** 在媒体未 ready 时只记住播放意图，避免跨线程直接触发底层播放。 */
    private suspend fun handlePlay() {
        if (currentIndex !in queue.indices) {
            return
        }
        if (!isPrepared) {
            pendingPlay = true
            return
        }
        pendingPlay = false
        adapter.play(generation = generation)
    }

    /** 暂停优先级高于之前的待播放意图，确保最终状态以最后一次命令为准。 */
    private suspend fun handlePause() {
        pendingPlay = false
        if (!isPrepared || currentIndex !in queue.indices) {
            return
        }
        adapter.pause(generation = generation)
    }

    /** 当前代 seek 采用 latest-wins；未准备完成时只缓存最后一个目标位置。 */
    private suspend fun handleSeekTo(positionMs: Long) {
        if (currentIndex !in queue.indices) {
            return
        }
        pendingSeekMs = positionMs
        if (!isPrepared) {
            return
        }
        adapter.seekTo(
            generation = generation,
            positionMs = positionMs,
        )
        eventChannel.send(
            element = PlaybackEngineEvent.ProgressChanged(
                positionMs = positionMs,
                durationMs = adapter.currentDurationMs(),
            ),
        )
    }

    /** 切歌会重置上一代待播放/待 seek 状态，并让新媒体从头开始准备。 */
    private suspend fun handleSkipToIndex(index: Int) {
        if (queue.isEmpty() || index !in queue.indices) {
            return
        }
        currentIndex = index
        pendingPlay = false
        pendingSeekMs = 0L
        prepareCurrentMedia(startPositionMs = 0L)
    }

    /** 停止当前媒体时先让旧代失效，再回传 idle 给协调器做状态回写。 */
    private suspend fun handleStop() {
        val activeGeneration: Long = generation
        nextGeneration()
        pendingPlay = false
        pendingSeekMs = null
        isPrepared = false
        stopProgressPolling()
        adapter.stop(generation = activeGeneration)
        eventChannel.send(
            element = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Idle,
                positionMs = 0L,
                durationMs = null,
            ),
        )
    }

    /** 释放时彻底屏蔽后续事件，并停止所有后台轮询。 */
    private suspend fun handleRelease() {
        nextGeneration()
        pendingPlay = false
        pendingSeekMs = null
        isPrepared = false
        isReleased = true
        stopProgressPolling()
        adapterEventJob.cancel()
        adapter.release()
        commandChannel.close()
        engineJob.cancel()
    }

    /** 只消费当前 generation 的有效回调，旧媒体与释放后的回调全部丢弃。 */
    private suspend fun handleAdapterEvent(event: DesktopMediaPlayerEvent) {
        if (isReleased || event.generation != generation) {
            return
        }
        if (!isPrepared && event !is DesktopMediaPlayerEvent.Prepared && event !is DesktopMediaPlayerEvent.Failed) {
            return
        }
        when (event) {
            is DesktopMediaPlayerEvent.Prepared -> handlePrepared(event = event)
            is DesktopMediaPlayerEvent.Playing -> handlePlaying(event = event)
            is DesktopMediaPlayerEvent.Paused -> handlePaused(event = event)
            is DesktopMediaPlayerEvent.Finished -> handleFinished()
            is DesktopMediaPlayerEvent.Failed -> handleFailed(event = event)
        }
    }

    /** 准备完成后兑现待 seek 与待播放意图，否则显式停在暂停态。 */
    private suspend fun handlePrepared(event: DesktopMediaPlayerEvent.Prepared) {
        if (currentIndex !in queue.indices) {
            return
        }
        isPrepared = true
        val seekMs: Long = pendingSeekMs ?: 0L
        if (seekMs > 0L) {
            adapter.seekTo(
                generation = generation,
                positionMs = seekMs,
            )
            eventChannel.send(
                element = PlaybackEngineEvent.ProgressChanged(
                    positionMs = seekMs,
                    durationMs = event.durationMs ?: queue[currentIndex].durationMs,
                ),
            )
        }
        if (pendingPlay) {
            pendingPlay = false
            adapter.play(generation = generation)
            return
        }
        adapter.pause(generation = generation)
    }

    /** 播放开始后启动轮询，并把当前 position/duration 同步给协调器。 */
    private suspend fun handlePlaying(event: DesktopMediaPlayerEvent.Playing) {
        startProgressPolling()
        eventChannel.send(
            element = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Playing,
                positionMs = event.positionMs,
                durationMs = event.durationMs,
            ),
        )
    }

    /** 暂停时立即停止轮询，避免暂停态继续上报进度噪音。 */
    private suspend fun handlePaused(event: DesktopMediaPlayerEvent.Paused) {
        stopProgressPolling()
        eventChannel.send(
            element = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Paused,
                positionMs = event.positionMs,
                durationMs = event.durationMs,
            ),
        )
    }

    /** 自然结束后交回协调器决定是否继续下一首。 */
    private suspend fun handleFinished() {
        stopProgressPolling()
        eventChannel.send(element = PlaybackEngineEvent.Ended)
    }

    /** 失败后停止轮询，并把底层统一错误形状透传给 common 层。 */
    private suspend fun handleFailed(event: DesktopMediaPlayerEvent.Failed) {
        stopProgressPolling()
        nextGeneration()
        pendingPlay = false
        pendingSeekMs = null
        isPrepared = false
        eventChannel.send(element = PlaybackEngineEvent.Failed(error = event.error))
    }

    /** 轮询命中时读取适配器当前进度，保持桌面播放中的位置持续更新。 */
    private suspend fun handleProgressTick() {
        if (!isPrepared || isReleased) {
            return
        }
        eventChannel.send(
            element = PlaybackEngineEvent.ProgressChanged(
                positionMs = adapter.currentPositionMs(),
                durationMs = adapter.currentDurationMs(),
            ),
        )
    }

    /** 为当前下标生成新媒体代号，并用 loading 状态通知上层开始切歌。 */
    private suspend fun prepareCurrentMedia(startPositionMs: Long) {
        val media: PlayableMedia = queue.getOrNull(index = currentIndex) ?: return
        val activeGeneration: Long = nextGeneration()
        isPrepared = false
        stopProgressPolling()
        eventChannel.send(
            element = PlaybackEngineEvent.CurrentMediaChanged(
                songId = media.songId,
                index = currentIndex,
                durationMs = media.durationMs,
            ),
        )
        eventChannel.send(
            element = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Loading,
                positionMs = startPositionMs,
                durationMs = media.durationMs,
            ),
        )
        adapter.prepare(
            mediaUri = media.localUri,
            generation = activeGeneration,
            startPositionMs = startPositionMs,
            pluginPath = libVlcPluginPath,
        )
    }

    /** 启动单个协程轮询，把真实时间上的进度采样折返到串行命令循环。 */
    private fun startProgressPolling() {
        stopProgressPolling()
        if (progressIntervalMs <= 0L) {
            return
        }
        progressJob = engineScope.launch {
            while (isActive) {
                delay(timeMillis = progressIntervalMs)
                commandChannel.send(element = EngineCommand.ProgressTick)
            }
        }
    }

    /** 取消进度轮询，避免切歌/暂停后的旧 tick 混入新状态。 */
    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    /** 递增媒体代号，用最简单的方式让旧回调天然失效。 */
    private fun nextGeneration(): Long {
        generation += 1L
        return generation
    }

}

/**
 * 引擎内部私有命令模型，确保所有状态转换都经由同一串行入口。
 */
private sealed interface EngineCommand {
    /** 用新队列替换当前引擎状态，并在准备入队完成后回 ACK。 */
    data class SetQueue(
        val items: List<PlayableMedia>,
        val startIndex: Int,
        val startPositionMs: Long,
        val ack: CompletableDeferred<Unit>,
    ) : EngineCommand

    /** 请求开始或继续播放当前代媒体。 */
    data object Play : EngineCommand

    /** 请求暂停当前代媒体。 */
    data object Pause : EngineCommand

    /** 请求跳转当前代媒体进度。 */
    data class SeekTo(val positionMs: Long) : EngineCommand

    /** 请求切到队列中的目标下标。 */
    data class SkipToIndex(val index: Int) : EngineCommand

    /** 保留播放模式同步接口，便于后续桌面能力继续接线。 */
    data class SetPlaybackMode(val playbackMode: PlaybackMode) : EngineCommand

    /** 请求停止当前媒体并回到 idle。 */
    data object Stop : EngineCommand

    /** 请求释放桌面底层资源。 */
    data object Release : EngineCommand

    /** 把桌面适配器回调重新包装回串行命令流。 */
    data class AdapterEventReceived(val event: DesktopMediaPlayerEvent) : EngineCommand

    /** 进度轮询转化成的内部命令。 */
    data object ProgressTick : EngineCommand
}
