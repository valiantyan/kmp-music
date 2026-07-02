package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.AudioSource
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
    private val commandChannel: Channel<DesktopPlaybackCommand> = Channel(capacity = Channel.UNLIMITED)

    // 引擎内部可变状态，集中承载当前队列、代际和准备态。
    private val state: DesktopPlaybackEngineState = DesktopPlaybackEngineState()

    // 跟踪所有待完成的 [setQueue] 确认，确保 release 与异常退出都能统一收口。
    private val setQueueAckTracker: DesktopSetQueueAckTracker = DesktopSetQueueAckTracker()

    // 适配器事件订阅任务，释放后取消以阻止 native 回调继续灌入命令通道。
    private val adapterEventJob: Job = engineScope.launch {
        adapter.events.collect { event: DesktopMediaPlayerEvent ->
            commandChannel.send(
                element = DesktopPlaybackCommand.AdapterEventReceived(event = event),
            )
        }
    }

    // 串行命令循环任务，负责把所有状态变更压到同一条执行序列中。
    private val commandLoopJob: Job = engineScope.launch {
        try {
            for (command: DesktopPlaybackCommand in commandChannel) {
                handle(command = command)
            }
        } finally {
            setQueueAckTracker.completeAll()
        }
    }

    // 释放流程是否已开始；一旦开始就不再接受新的外部命令。
    @Volatile
    private var isReleasing: Boolean = false

    // 引擎是否已经释放；释放后只丢弃后续命令与回调。
    @Volatile
    private var isReleased: Boolean = false

    // 进度轮询任务，便于在暂停/切歌/释放时精准取消。
    private var progressJob: Job? = null

    // 仅供桌面测试精确编排 release/setQueue 竞态，不参与生产流程判断。
    private var testHooks: DesktopVlcjAudioPlayerEngineTestHooks = DesktopVlcjAudioPlayerEngineTestHooks()

    override val events: Flow<PlaybackEngineEvent> = eventChannel.receiveAsFlow()

    /** 仅供测试注入时序钩子，避免用 sleep 猜测竞态窗口。 */
    internal fun installTestHooks(testHooks: DesktopVlcjAudioPlayerEngineTestHooks) {
        this.testHooks = testHooks
    }

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
        testHooks.beforeSetQueueCommandEnqueue()
        val sendResult = commandChannel.trySend(
            element = DesktopPlaybackCommand.SetQueue(
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

    /** 记录播放意图；若媒体已准备好则立刻下发到底层适配器。 */
    override fun play() {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(element = DesktopPlaybackCommand.Play)
    }

    /** 清空待播放意图，并在已准备时立即下发暂停命令。 */
    override fun pause() {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(element = DesktopPlaybackCommand.Pause)
    }

    /** 记录或执行 seek，请求始终只保留当前代最后一次位置。 */
    override fun seekTo(positionMs: Long) {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(
            element = DesktopPlaybackCommand.SeekTo(
                positionMs = positionMs.coerceAtLeast(minimumValue = 0L),
            ),
        )
    }

    /** 直接切到目标下标，并让旧媒体的后续回调全部失效。 */
    override fun skipToIndex(index: Int) {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(element = DesktopPlaybackCommand.SkipToIndex(index = index))
    }

    /** 当前任务阶段无需向桌面底层同步播放模式，保留命令以保持契约完整。 */
    override fun setPlaybackMode(playbackMode: PlaybackMode) {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(
            element = DesktopPlaybackCommand.SetPlaybackMode(playbackMode = playbackMode),
        )
    }

    /** 将归一化音量交给命令循环串行处理，避免 UI 线程直接触碰 vlcj。 */
    override fun setVolume(volume: Float) {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(
            element = DesktopPlaybackCommand.SetVolume(
                volume = volume.coerceIn(minimumValue = 0f, maximumValue = 1f),
            ),
        )
    }

    /** 停止当前媒体并把引擎推回 idle。 */
    override fun stop() {
        if (isReleased || isReleasing) {
            return
        }
        commandChannel.trySend(element = DesktopPlaybackCommand.Stop)
    }

    /** 桌面端显式释放原生资源，并屏蔽后续延迟回调。 */
    fun release() {
        if (isReleased || isReleasing) {
            return
        }
        isReleasing = true
        commandChannel.trySend(element = DesktopPlaybackCommand.Release)
    }

    /** 释放并等待命令循环完全收尾，供 Desktop 进程退出前安全关闭原生资源。 */
    suspend fun releaseAndAwait() {
        release()
        commandLoopJob.join()
    }

    /** 串行消费所有外部命令与适配器回调，避免并发改写引擎状态。 */
    private suspend fun handle(command: DesktopPlaybackCommand) {
        if (isReleased) {
            if (command is DesktopPlaybackCommand.SetQueue) {
                setQueueAckTracker.complete(ack = command.ack)
            }
            return
        }
        when (command) {
            is DesktopPlaybackCommand.SetQueue -> handleSetQueue(command = command)
            DesktopPlaybackCommand.Play -> handlePlay()
            DesktopPlaybackCommand.Pause -> handlePause()
            is DesktopPlaybackCommand.SeekTo -> handleSeekTo(positionMs = command.positionMs)
            is DesktopPlaybackCommand.SkipToIndex -> handleSkipToIndex(index = command.index)
            is DesktopPlaybackCommand.SetPlaybackMode -> Unit
            is DesktopPlaybackCommand.SetVolume -> handleSetVolume(volume = command.volume)
            DesktopPlaybackCommand.Stop -> handleStop()
            DesktopPlaybackCommand.Release -> handleRelease()
            is DesktopPlaybackCommand.AdapterEventReceived -> handleAdapterEvent(event = command.event)
            DesktopPlaybackCommand.ProgressTick -> handleProgressTick()
        }
    }

    /** 统一处理队列替换，空队列直接回传失败，非空队列则进入 loading。 */
    private suspend fun handleSetQueue(command: DesktopPlaybackCommand.SetQueue) {
        try {
            state.resetForNewQueue(items = command.items)
            stopProgressPolling()
            if (state.queue.isEmpty()) {
                state.currentIndex = -1
                state.nextGeneration()
                eventChannel.send(
                    element = PlaybackEngineEvent.Failed(
                        error = PlaybackError(
                            type = PlaybackErrorType.MissingFile,
                            songId = null,
                            message = "播放队列为空",
                        ),
                    ),
                )
                return
            }
            state.currentIndex = command.startIndex.coerceIn(
                minimumValue = 0,
                maximumValue = state.queue.lastIndex,
            )
            state.pendingSeekMs = command.startPositionMs
            prepareCurrentMedia(startPositionMs = command.startPositionMs)
        } finally {
            setQueueAckTracker.complete(ack = command.ack)
        }
    }

    /** 在媒体未 ready 时只记住播放意图，避免跨线程直接触发底层播放。 */
    private suspend fun handlePlay() {
        if (!state.isCurrentIndexValid()) {
            return
        }
        state.playbackControlIntent = DesktopPlaybackControlIntent.Play
        if (!state.isPrepared) {
            return
        }
        adapter.play(generation = state.generation)
    }

    /** 暂停优先级高于之前的待播放意图，确保最终状态以最后一次命令为准。 */
    private suspend fun handlePause() {
        state.playbackControlIntent = DesktopPlaybackControlIntent.Pause
        if (!state.isPrepared || !state.isCurrentIndexValid()) {
            return
        }
        adapter.pause(generation = state.generation)
    }

    /** 当前代 seek 采用 latest-wins；未准备完成时只缓存最后一个目标位置。 */
    private suspend fun handleSeekTo(positionMs: Long) {
        if (!state.isCurrentIndexValid()) {
            return
        }
        state.pendingSeekMs = positionMs
        if (!state.isPrepared) {
            return
        }
        adapter.seekTo(
            generation = state.generation,
            positionMs = positionMs,
        )
        eventChannel.send(
            element = PlaybackEngineEvent.ProgressChanged(
                positionMs = positionMs,
                durationMs = adapter.currentDurationMs(),
            ),
        )
    }

    /** 将 0.0-1.0 的共享音量映射成 vlcj 需要的 0-100 平台音量。 */
    private suspend fun handleSetVolume(volume: Float) {
        adapter.setVolume(volumePercent = (volume.coerceIn(minimumValue = 0f, maximumValue = 1f) * 100).toInt())
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
        state.nextGeneration()
        state.resetPlaybackFlags()
        isReleased = true
        stopProgressPolling()
        setQueueAckTracker.completeAll()
        adapterEventJob.cancel()
        adapter.release()
        commandChannel.close()
        engineJob.cancel()
    }

    /** 只消费当前 generation 的有效回调，旧媒体与释放后的回调全部丢弃。 */
    private suspend fun handleAdapterEvent(event: DesktopMediaPlayerEvent) {
        if (isReleased || event.generation != state.generation) {
            return
        }
        if (!state.isPrepared &&
            event !is DesktopMediaPlayerEvent.Prepared &&
            event !is DesktopMediaPlayerEvent.Failed
        ) {
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

    /** 准备完成后兑现待 seek 与待播放控制意图；没有控制意图时等待下一条命令。 */
    private suspend fun handlePrepared(event: DesktopMediaPlayerEvent.Prepared) {
        val snapshot: DesktopPlaybackEngineSnapshot = state.snapshot()
        if (!snapshot.isCurrentIndexValid()) {
            return
        }
        state.isPrepared = true
        val seekMs: Long = snapshot.pendingSeekMs ?: 0L
        if (seekMs > 0L) {
            adapter.seekTo(
                generation = snapshot.generation,
                positionMs = seekMs,
            )
            eventChannel.send(
                element = PlaybackEngineEvent.ProgressChanged(
                    positionMs = seekMs,
                    durationMs = event.durationMs ?: requireNotNull(snapshot.currentMedia()).durationMs,
                ),
            )
        }
        when (snapshot.playbackControlIntent) {
            DesktopPlaybackControlIntent.Play -> {
                adapter.play(generation = snapshot.generation)
            }
            DesktopPlaybackControlIntent.Pause -> {
                adapter.pause(generation = snapshot.generation)
            }
            DesktopPlaybackControlIntent.None -> Unit
        }
    }

    /** 播放开始后启动轮询，并把当前 position/duration 同步给协调器。 */
    private suspend fun handlePlaying(event: DesktopMediaPlayerEvent.Playing) {
        if (state.playbackControlIntent == DesktopPlaybackControlIntent.Pause) {
            return
        }
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
        if (state.playbackControlIntent != DesktopPlaybackControlIntent.Pause) {
            return
        }
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
        state.nextGeneration()
        state.resetPlaybackFlags()
        eventChannel.send(element = PlaybackEngineEvent.Failed(error = event.error))
    }

    /** 轮询命中时读取适配器当前进度，保持桌面播放中的位置持续更新。 */
    private suspend fun handleProgressTick() {
        if (!state.isPrepared || isReleased) {
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
        val media: PlayableMedia = state.currentMedia() ?: return
        val activeGeneration: Long = state.nextGeneration()
        state.isPrepared = false
        stopProgressPolling()
        eventChannel.send(
            element = PlaybackEngineEvent.CurrentMediaChanged(
                songId = media.songId,
                index = state.currentIndex,
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
            songId = media.songId,
            mediaUri = media.playbackUri(),
            generation = activeGeneration,
            startPositionMs = startPositionMs,
            pluginPath = libVlcPluginPath,
        )
    }

    // phase 1 只支持本地播放来源；网络来源进入模型时必须在桌面适配层显式处理。
    private fun PlayableMedia.playbackUri(): String {
        return when (val source: AudioSource = audioSource) {
            is AudioSource.Local -> source.uri
        }
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
                commandChannel.send(element = DesktopPlaybackCommand.ProgressTick)
            }
        }
    }

    /** 取消进度轮询，避免切歌/暂停后的旧 tick 混入新状态。 */
    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }
}

/**
 * 桌面引擎的测试时序钩子，专门用于稳定复现协程竞态，不暴露给生产调用方。
 */
internal class DesktopVlcjAudioPlayerEngineTestHooks(
    beforeSetQueueCommandEnqueue: suspend () -> Unit = {},
) {
    // 在 [setQueue] 真正入队前执行，测试可借此把命令卡在最危险的竞态窗口。
    val beforeSetQueueCommandEnqueue: suspend () -> Unit = beforeSetQueueCommandEnqueue
}
