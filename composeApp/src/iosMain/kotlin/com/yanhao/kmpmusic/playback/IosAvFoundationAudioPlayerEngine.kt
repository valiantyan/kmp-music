package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/**
 * iOS AVFoundation 播放引擎，使用单个原生播放器承接当前媒体，队列语义仍由 common 层拥有。
 */
internal class IosAvFoundationAudioPlayerEngine(
    // AVFoundation bridge，真实实现和 fake 测试实现都只通过该契约接入。
    private val bridge: IosPlaybackBridge = IosAvFoundationPlaybackBridge(),
    // iOS audio session 控制器，确保真正播放前配置 playback category。
    private val audioSessionController: IosAudioSessionController = IosAvAudioSessionController(),
    // 外部注入的长生命周期作用域，避免播放器跟随 Compose composition 被释放。
    private val scope: CoroutineScope,
    // bridge 事件处理上下文，测试可注入可控调度器。
    private val dispatcher: CoroutineContext = Dispatchers.Default,
) : AudioPlayerEngine {
    // 引擎内部生命周期，release 时会取消 bridge 事件订阅。
    private val engineJob: Job = SupervisorJob(parent = scope.coroutineContext[Job])

    // 引擎私有作用域，承接 native 回调重新串行化。
    private val engineScope: CoroutineScope = CoroutineScope(context = dispatcher + engineJob)

    // 向 common 协调器暴露的播放事实流。
    private val eventChannel: Channel<PlaybackEngineEvent> = Channel(capacity = Channel.UNLIMITED)

    // 串行保护队列、generation、准备态和释放态，避免 UI 命令与 native 回调并发改写。
    private val stateMutex: Mutex = Mutex()

    // iOS 当前播放队列，仅用于定位当前媒体，不让系统队列接管业务规则。
    private var queue: List<PlayableMedia> = emptyList()

    // 当前媒体下标，没有活动媒体时为 -1。
    private var currentIndex: Int = -1

    // 当前媒体代号，切歌、停止和失败都会推进，屏蔽旧回调。
    private var generation: Long = 0L

    // 当前 generation 是否已准备完成。
    private var isPrepared: Boolean = false

    // 当前代尚未兑现的 seek 请求，遵循 latest-wins。
    private var pendingSeekMs: Long? = null

    // 最近一次播放控制意图，用于 prepared 后兑现命令。
    private var playbackIntent: IosPlaybackControlIntent = IosPlaybackControlIntent.None

    // 最近一次播放模式；业务推进仍由 [PlaybackCoordinator] 处理。
    private var playbackMode: PlaybackMode = PlaybackMode.LoopAll

    // 系统中断结束时是否可恢复播放。
    private var shouldResumeAfterInterruption: Boolean = false

    // release 是否已开始，开始后丢弃后续命令和回调。
    private var isReleased: Boolean = false

    // 当前 release 任务，供会话退出前等待 native 资源收口。
    private var releaseJob: Job? = null

    // bridge 事件订阅任务，release 后取消以移除观察路径。
    private val bridgeEventJob: Job =
        engineScope.launch {
            bridge.events.collect { event: IosPlaybackBridgeEvent ->
                handleBridgeEvent(event = event)
            }
        }

    /** 对外暴露 iOS 原生播放事实。 */
    override val events: Flow<PlaybackEngineEvent> = eventChannel.receiveAsFlow()

    /**
     * 替换当前队列并只准备目标媒体，避免形成第二套系统队列。
     */
    override suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        stateMutex.withLock {
            if (isReleased) {
                return
            }
            queue = items
            if (items.isEmpty()) {
                currentIndex = -1
                nextGeneration()
                eventChannel.trySend(element = PlaybackEngineEvent.Failed(error = buildEmptyQueueError()))
                return
            }
            currentIndex =
                startIndex.coerceIn(
                    minimumValue = 0,
                    maximumValue = items.lastIndex,
                )
            playbackIntent = IosPlaybackControlIntent.None
            pendingSeekMs = startPositionMs.coerceAtLeast(minimumValue = 0L)
            prepareCurrentMedia(startPositionMs = pendingSeekMs ?: 0L)
        }
    }

    /** 记录播放意图，媒体 ready 后才配置 audio session 并调用 native play。 */
    override fun play() {
        engineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            stateMutex.withLock {
                if (isReleased || !isCurrentIndexValid()) {
                    return@withLock
                }
                playbackIntent = IosPlaybackControlIntent.Play
                shouldResumeAfterInterruption = true
                if (!isPrepared) {
                    return@withLock
                }
                playCurrent()
            }
        }
    }

    /** 暂停当前媒体，并把共享状态收口到 Paused。 */
    override fun pause() {
        engineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            stateMutex.withLock {
                if (isReleased || !isCurrentIndexValid()) {
                    return@withLock
                }
                playbackIntent = IosPlaybackControlIntent.Pause
                shouldResumeAfterInterruption = false
                if (!isPrepared) {
                    return@withLock
                }
                pauseCurrent()
            }
        }
    }

    /** 缓存或执行当前 generation 的 seek 请求，并立即回传共享进度事实。 */
    override fun seekTo(positionMs: Long) {
        engineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            stateMutex.withLock {
                if (isReleased || !isCurrentIndexValid()) {
                    return@withLock
                }
                val safePositionMs: Long = positionMs.coerceAtLeast(minimumValue = 0L)
                pendingSeekMs = safePositionMs
                if (!isPrepared) {
                    return@withLock
                }
                seekCurrent(positionMs = safePositionMs)
            }
        }
    }

    /** 直接切到目标下标，只准备目标媒体，不让 AVFoundation 管理业务队列。 */
    override fun skipToIndex(index: Int) {
        engineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            stateMutex.withLock {
                if (isReleased || queue.isEmpty() || index !in queue.indices) {
                    return@withLock
                }
                currentIndex = index
                isPrepared = false
                playbackIntent = IosPlaybackControlIntent.None
                pendingSeekMs = 0L
                val activeGeneration: Long = nextGeneration()
                prepareCurrentMedia(
                    startPositionMs = 0L,
                    activeGeneration = activeGeneration,
                )
            }
        }
    }

    /** 只记录播放模式，loop/shuffle 规则继续由 common 层推进。 */
    override fun setPlaybackMode(playbackMode: PlaybackMode) {
        engineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            stateMutex.withLock {
                this@IosAvFoundationAudioPlayerEngine.playbackMode = playbackMode
            }
        }
    }

    /** 将 App 内归一化音量下发给 AVPlayer。 */
    override fun setVolume(volume: Float) {
        engineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            stateMutex.withLock {
                if (isReleased) {
                    return@withLock
                }
                handleBridgeAck(
                    ack = bridge.setVolume(volume = volume.coerceIn(minimumValue = 0f, maximumValue = 1f)),
                )
            }
        }
    }

    /** 停止当前媒体并回到 idle。 */
    override fun stop() {
        engineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            stateMutex.withLock {
                if (isReleased) {
                    return@withLock
                }
                val stoppedGeneration: Long = generation
                nextGeneration()
                resetPlaybackFlags()
                handleBridgeAck(ack = bridge.stop(generation = stoppedGeneration))
                eventChannel.trySend(
                    element =
                        PlaybackEngineEvent.StatusChanged(
                            status = PlaybackStatus.Idle,
                            positionMs = 0L,
                            durationMs = null,
                        ),
                )
            }
        }
    }

    /** 释放 native 播放器和观察器，后续命令全部丢弃。 */
    fun release() {
        if (releaseJob != null) {
            return
        }
        releaseJob =
            scope.launch(context = dispatcher, start = CoroutineStart.UNDISPATCHED) {
                stateMutex.withLock {
                    if (isReleased) {
                        return@withLock
                    }
                    isReleased = true
                    resetPlaybackFlags()
                    nextGeneration()
                    bridgeEventJob.cancel()
                    bridge.release()
                    engineJob.cancel()
                }
            }
    }

    /** 等待 release 完成，供 iOS 进程级会话退出前收口。 */
    suspend fun releaseAndAwait() {
        release()
        releaseJob?.join()
    }

    // 当前下标是否指向有效媒体。
    private fun isCurrentIndexValid(): Boolean = currentIndex in queue.indices

    // 读取当前媒体，避免重复手写越界逻辑。
    private fun currentMedia(): PlayableMedia? = queue.getOrNull(index = currentIndex)

    // 生成新媒体代号，并让旧回调自然失效。
    private fun nextGeneration(): Long {
        generation += 1L
        return generation
    }

    // 清空当前代的准备态和待处理命令。
    private fun resetPlaybackFlags() {
        isPrepared = false
        pendingSeekMs = null
        playbackIntent = IosPlaybackControlIntent.None
        shouldResumeAfterInterruption = false
    }

    // 准备当前媒体并向 common 层回传当前媒体和 loading 事实。
    private suspend fun prepareCurrentMedia(
        startPositionMs: Long,
        activeGeneration: Long = nextGeneration(),
    ) {
        val media: PlayableMedia = currentMedia() ?: return
        isPrepared = false
        eventChannel.trySend(
            element =
                PlaybackEngineEvent.CurrentMediaChanged(
                    songId = media.songId,
                    index = currentIndex,
                    durationMs = media.durationMs,
                ),
        )
        eventChannel.trySend(
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
                        IosPlaybackBridgePrepareRequest(
                            songId = media.songId,
                            mediaUri = media.audioSource.uri,
                            generation = activeGeneration,
                            startPositionMs = startPositionMs,
                        ),
                ),
        )
    }

    // 开始当前媒体，必须先配置 playback audio session。
    private suspend fun playCurrent() {
        if (!audioSessionController.configureForPlayback()) {
            handleFailure(error = buildAudioSessionError())
            return
        }
        handleBridgeAck(ack = bridge.play(generation = generation))
    }

    // 暂停当前媒体，并立即折返 Paused 防止 UI 卡在 Playing。
    private suspend fun pauseCurrent() {
        handleBridgeAck(ack = bridge.pause(generation = generation))
        val media: PlayableMedia = currentMedia() ?: return
        eventChannel.trySend(
            element =
                PlaybackEngineEvent.StatusChanged(
                    status = PlaybackStatus.Paused,
                    positionMs = pendingSeekMs ?: 0L,
                    durationMs = media.durationMs,
                ),
        )
    }

    // 执行当前代 seek 并回传共享进度事实。
    private suspend fun seekCurrent(positionMs: Long) {
        handleBridgeAck(
            ack =
                bridge.seekTo(
                    request =
                        IosPlaybackBridgeSeekRequest(
                            generation = generation,
                            positionMs = positionMs,
                        ),
                ),
        )
        eventChannel.trySend(
            element =
                PlaybackEngineEvent.ProgressChanged(
                    positionMs = positionMs,
                    durationMs = currentMedia()?.durationMs,
                ),
        )
    }

    // 统一处理 bridge 命令失败，不让调用方挂起或静默失败。
    private fun handleBridgeAck(ack: IosPlaybackBridgeCommandAck): Boolean =
        when (ack) {
            IosPlaybackBridgeCommandAck.Accepted -> {
                true
            }

            is IosPlaybackBridgeCommandAck.Failed -> {
                eventChannel.trySend(element = PlaybackEngineEvent.Failed(error = ack.error))
                nextGeneration()
                resetPlaybackFlags()
                false
            }
        }

    // 把 native 回调按 generation 过滤后归一化为 common 播放事件。
    private suspend fun handleBridgeEvent(event: IosPlaybackBridgeEvent) {
        stateMutex.withLock {
            if (isReleased || !isCurrentEvent(event = event)) {
                return@withLock
            }
            when (event) {
                is IosPlaybackBridgeEvent.Prepared -> handlePrepared(event = event)
                is IosPlaybackBridgeEvent.Buffering -> emitStatus(status = PlaybackStatus.Buffering, event = event)
                is IosPlaybackBridgeEvent.Playing -> emitStatus(status = PlaybackStatus.Playing, event = event)
                is IosPlaybackBridgeEvent.Paused -> emitStatus(status = PlaybackStatus.Paused, event = event)
                is IosPlaybackBridgeEvent.Progress -> emitProgress(event = event)
                is IosPlaybackBridgeEvent.Ended -> eventChannel.trySend(element = PlaybackEngineEvent.Ended)
                is IosPlaybackBridgeEvent.Failed -> handleFailure(error = event.error)
                is IosPlaybackBridgeEvent.InterruptionBegan -> handleInterruptionBegan(event = event)
                is IosPlaybackBridgeEvent.InterruptionEnded -> handleInterruptionEnded(event = event)
                is IosPlaybackBridgeEvent.OutputDisconnected -> handleOutputDisconnected(event = event)
            }
        }
    }

    // prepared 后兑现待 seek 和待播放/暂停意图。
    private suspend fun handlePrepared(event: IosPlaybackBridgeEvent.Prepared) {
        isPrepared = true
        val seekMs: Long = pendingSeekMs ?: 0L
        if (seekMs > 0L) {
            seekCurrent(positionMs = seekMs)
        }
        when (playbackIntent) {
            IosPlaybackControlIntent.Play -> playCurrent()
            IosPlaybackControlIntent.Pause -> pauseCurrent()
            IosPlaybackControlIntent.None -> Unit
        }
        if (event.durationMs != null && seekMs == 0L) {
            eventChannel.trySend(
                element =
                    PlaybackEngineEvent.ProgressChanged(
                        positionMs = 0L,
                        durationMs = event.durationMs,
                    ),
            )
        }
    }

    // 系统中断开始时立刻暂停共享状态。
    private suspend fun handleInterruptionBegan(event: IosPlaybackBridgeEvent.InterruptionBegan) {
        shouldResumeAfterInterruption = playbackIntent == IosPlaybackControlIntent.Play
        playbackIntent = IosPlaybackControlIntent.Pause
        handleBridgeAck(ack = bridge.pause(generation = generation))
        eventChannel.trySend(
            element =
                PlaybackEngineEvent.StatusChanged(
                    status = PlaybackStatus.Paused,
                    positionMs = event.positionMs,
                    durationMs = event.durationMs ?: currentMedia()?.durationMs,
                ),
        )
    }

    // 系统给出可恢复提示时恢复播放，否则维持暂停态。
    private suspend fun handleInterruptionEnded(event: IosPlaybackBridgeEvent.InterruptionEnded) {
        if (!event.shouldResume || !shouldResumeAfterInterruption) {
            return
        }
        playbackIntent = IosPlaybackControlIntent.Play
        playCurrent()
    }

    // 输出断开时暂停共享状态，避免界面继续显示 Playing。
    private suspend fun handleOutputDisconnected(event: IosPlaybackBridgeEvent.OutputDisconnected) {
        playbackIntent = IosPlaybackControlIntent.Pause
        shouldResumeAfterInterruption = false
        handleBridgeAck(ack = bridge.pause(generation = generation))
        eventChannel.trySend(
            element =
                PlaybackEngineEvent.StatusChanged(
                    status = PlaybackStatus.Paused,
                    positionMs = event.positionMs,
                    durationMs = event.durationMs ?: currentMedia()?.durationMs,
                ),
        )
    }

    // 播放失败后当前 generation 失效，避免同代后续回调救活旧媒体。
    private fun handleFailure(error: PlaybackError) {
        eventChannel.trySend(element = PlaybackEngineEvent.Failed(error = error))
        nextGeneration()
        resetPlaybackFlags()
    }

    // 回传状态事件。
    private fun emitStatus(
        status: PlaybackStatus,
        event: IosPlaybackBridgeEvent,
    ) {
        eventChannel.trySend(
            element =
                PlaybackEngineEvent.StatusChanged(
                    status = status,
                    positionMs = positionOf(event = event),
                    durationMs = durationOf(event = event) ?: currentMedia()?.durationMs,
                ),
        )
    }

    // 回传进度事件。
    private fun emitProgress(event: IosPlaybackBridgeEvent.Progress) {
        eventChannel.trySend(
            element =
                PlaybackEngineEvent.ProgressChanged(
                    positionMs = event.positionMs,
                    durationMs = event.durationMs ?: currentMedia()?.durationMs,
                ),
        )
    }

    // 判断回调是否属于当前 generation。
    private fun isCurrentEvent(event: IosPlaybackBridgeEvent): Boolean = generationOf(event = event) == generation

    // 读取事件 generation。
    private fun generationOf(event: IosPlaybackBridgeEvent): Long =
        when (event) {
            is IosPlaybackBridgeEvent.Prepared -> event.generation
            is IosPlaybackBridgeEvent.Buffering -> event.generation
            is IosPlaybackBridgeEvent.Playing -> event.generation
            is IosPlaybackBridgeEvent.Paused -> event.generation
            is IosPlaybackBridgeEvent.Progress -> event.generation
            is IosPlaybackBridgeEvent.Ended -> event.generation
            is IosPlaybackBridgeEvent.Failed -> event.generation
            is IosPlaybackBridgeEvent.InterruptionBegan -> event.generation
            is IosPlaybackBridgeEvent.InterruptionEnded -> event.generation
            is IosPlaybackBridgeEvent.OutputDisconnected -> event.generation
        }

    // 读取状态类事件的进度。
    private fun positionOf(event: IosPlaybackBridgeEvent): Long =
        when (event) {
            is IosPlaybackBridgeEvent.Buffering -> event.positionMs

            is IosPlaybackBridgeEvent.Playing -> event.positionMs

            is IosPlaybackBridgeEvent.Paused -> event.positionMs

            is IosPlaybackBridgeEvent.Progress -> event.positionMs

            is IosPlaybackBridgeEvent.InterruptionBegan -> event.positionMs

            is IosPlaybackBridgeEvent.OutputDisconnected -> event.positionMs

            is IosPlaybackBridgeEvent.Prepared,
            is IosPlaybackBridgeEvent.Ended,
            is IosPlaybackBridgeEvent.Failed,
            is IosPlaybackBridgeEvent.InterruptionEnded,
            -> pendingSeekMs ?: 0L
        }

    // 读取状态类事件的时长。
    private fun durationOf(event: IosPlaybackBridgeEvent): Long? =
        when (event) {
            is IosPlaybackBridgeEvent.Buffering -> event.durationMs

            is IosPlaybackBridgeEvent.Playing -> event.durationMs

            is IosPlaybackBridgeEvent.Paused -> event.durationMs

            is IosPlaybackBridgeEvent.Progress -> event.durationMs

            is IosPlaybackBridgeEvent.InterruptionBegan -> event.durationMs

            is IosPlaybackBridgeEvent.OutputDisconnected -> event.durationMs

            is IosPlaybackBridgeEvent.Prepared -> event.durationMs

            is IosPlaybackBridgeEvent.Ended,
            is IosPlaybackBridgeEvent.Failed,
            is IosPlaybackBridgeEvent.InterruptionEnded,
            -> currentMedia()?.durationMs
        }

    // 空队列统一映射为缺失文件，复用 common 失败策略。
    private fun buildEmptyQueueError(): PlaybackError =
        PlaybackError(
            type = PlaybackErrorType.MissingFile,
            songId = null,
            message = "iOS 播放队列为空",
        )

    // audio session 配置失败统一映射为引擎不可用。
    private fun buildAudioSessionError(): PlaybackError =
        PlaybackError(
            type = PlaybackErrorType.EngineUnavailable,
            songId = currentMedia()?.songId,
            message = "iOS 音频会话无法激活 playback category",
        )
}

/**
 * iOS 播放控制意图，避免准备完成前丢失用户最后一次命令。
 */
private enum class IosPlaybackControlIntent {
    None,
    Play,
    Pause,
}
