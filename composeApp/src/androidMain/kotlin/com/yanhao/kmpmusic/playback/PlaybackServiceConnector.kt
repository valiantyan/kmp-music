package com.yanhao.kmpmusic.playback

import android.content.Context
import android.content.Intent
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Android 宿主注入到 common 层的引擎代理，负责按需拉起 [MusicPlaybackService] 并转发真实事件。
 */
class PlaybackServiceConnector(
    // 事件转发与 service 启动等待使用的作用域。
    private val scope: CoroutineScope,
) : AudioPlayerEngine {
    // 最近一次通知命令，供惰性启动的 service 完成 attach 后重放。
    private var pendingNotificationCommand: PendingNotificationCommand? = null

    // Service 未就绪前暂存给 shared 层的失败或状态事件。
    private val mutableEvents: MutableSharedFlow<PlaybackEngineEvent> = MutableSharedFlow(
        extraBufferCapacity = 128,
    )

    // applicationContext 仅在 Android runtime 注入后可用。
    private var appContext: Context? = null

    // 把真实引擎事件桥接给 shared 层的任务。
    private var eventBridgeJob: Job? = null

    // 当前已经桥接过的真实引擎，避免重复 collect。
    private var bridgedEngine: AudioPlayerEngine? = null

    init {
        PlaybackServiceRegistry.addOnAttachListener(listener = ::deliverPendingNotificationCommand)
    }

    /**
     * 对 common 层暴露的事件流。
     */
    override val events: Flow<PlaybackEngineEvent> = mutableEvents.asSharedFlow()

    /**
     * 注入 Android applicationContext，供 connector 按需启动播放服务。
     */
    fun attachContext(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * 首次设队列时确保 service 真正拉起后再下发，避免 common 层第一次播放就收到 engine unavailable。
     */
    override suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        val engine: AudioPlayerEngine = awaitEngineOrEmitError() ?: return
        bridgeEvents(engine = engine)
        engine.setQueue(
            items = items,
            startIndex = startIndex,
            startPositionMs = startPositionMs,
        )
    }

    /** 播放前保证事件桥已经挂好，避免丢失状态变化。 */
    override fun play() {
        val engine: AudioPlayerEngine = requireEngineOrEmitError() ?: return
        bridgeEvents(engine = engine)
        engine.play()
    }

    /** 暂停前保证事件桥已经挂好，避免丢失状态变化。 */
    override fun pause() {
        val engine: AudioPlayerEngine = requireEngineOrEmitError() ?: return
        bridgeEvents(engine = engine)
        engine.pause()
    }

    /** Seek 前保证事件桥已经挂好，避免 UI 和 service 进度漂移。 */
    override fun seekTo(positionMs: Long) {
        val engine: AudioPlayerEngine = requireEngineOrEmitError() ?: return
        bridgeEvents(engine = engine)
        engine.seekTo(positionMs = positionMs)
    }

    /** 切歌前保证事件桥已经挂好，避免当前媒体变化丢失。 */
    override fun skipToIndex(index: Int) {
        val engine: AudioPlayerEngine = requireEngineOrEmitError() ?: return
        bridgeEvents(engine = engine)
        engine.skipToIndex(index = index)
    }

    /** 停止前保证事件桥已经挂好，避免 shared 状态残留播放中。 */
    override fun stop() {
        val engine: AudioPlayerEngine = requireEngineOrEmitError() ?: return
        bridgeEvents(engine = engine)
        engine.stop()
    }

    /**
     * 让 service 依据共享状态刷新系统媒体通知按钮偏好。
     */
    fun refreshMediaButtonPreferences(
        isPlaying: Boolean,
        isFavorite: Boolean,
        playbackMode: PlaybackMode,
        playbackStatus: PlaybackStatus,
    ) {
        pendingNotificationCommand = PendingNotificationCommand.Show(
            state = NotificationState(
                isPlaying = isPlaying,
                isFavorite = isFavorite,
                playbackMode = playbackMode,
                playbackStatus = playbackStatus,
            ),
        )
        ensureServiceStarted()
        PlaybackServiceRegistry.currentService()?.let(::deliverPendingNotificationCommand)
    }

    /**
     * 当前没有可播放歌曲时撤下通知，避免 service 长时间保留过期前台状态。
     */
    fun clearNotification() {
        pendingNotificationCommand = PendingNotificationCommand.Clear
        PlaybackServiceRegistry.currentService()?.let(::deliverPendingNotificationCommand)
    }

    // 用普通 startService 惰性启动 service，避免 Activity 启动时主动拉前台服务。
    private fun ensureServiceStarted() {
        val context: Context = appContext ?: return
        context.startService(Intent(context, MusicPlaybackService::class.java))
    }

    // 非挂起命令直接尝试读取当前引擎，读取不到时发出统一错误事件。
    private fun requireEngineOrEmitError(): AudioPlayerEngine? {
        ensureServiceStarted()
        val engine: AudioPlayerEngine? = PlaybackServiceRegistry.currentEngine()
        if (engine == null) {
            emitEngineUnavailable()
        }
        return engine
    }

    // 挂起队列下发允许短暂等待 service 初始化，解决首次播放时序竞争。
    private suspend fun awaitEngineOrEmitError(): AudioPlayerEngine? {
        ensureServiceStarted()
        repeat(times = SERVICE_START_ATTEMPTS) {
            val engine: AudioPlayerEngine? = PlaybackServiceRegistry.currentEngine()
            if (engine != null) {
                return engine
            }
            delay(timeMillis = SERVICE_START_RETRY_DELAY_MS)
        }
        emitEngineUnavailable()
        return null
    }

    // 真实引擎变更时重建 collect 任务，确保 connector 始终只转发当前 service 的事件。
    private fun bridgeEvents(engine: AudioPlayerEngine) {
        if (bridgedEngine === engine && eventBridgeJob?.isActive == true) {
            return
        }
        eventBridgeJob?.cancel()
        bridgedEngine = engine
        eventBridgeJob = scope.launch {
            engine.events.collect { event: PlaybackEngineEvent ->
                mutableEvents.emit(event)
            }
        }
    }

    // 统一抛出 service 未就绪错误，供 shared 协调器和 UI 显式兜底。
    private fun emitEngineUnavailable() {
        mutableEvents.tryEmit(
            PlaybackEngineEvent.Failed(
                error = PlaybackError(
                    type = PlaybackErrorType.EngineUnavailable,
                    songId = null,
                    message = "Android 播放服务尚未就绪",
                ),
            ),
        )
    }

    // 惰性启动的 service attach 后补发最近一次通知命令，避免首帧状态在 registry 建立前丢失。
    private fun deliverPendingNotificationCommand(service: MusicPlaybackService) {
        when (val command: PendingNotificationCommand? = pendingNotificationCommand) {
            is PendingNotificationCommand.Show -> {
                val state: NotificationState = command.state
                service.refreshMediaButtonPreferences(
                    isPlaying = state.isPlaying,
                    isFavorite = state.isFavorite,
                    playbackMode = state.playbackMode,
                    playbackStatus = state.playbackStatus,
                )
            }
            PendingNotificationCommand.Clear -> service.clearMediaNotification()
            null -> Unit
        }
    }

    private companion object {
        /** 首次拉起 service 后最多重试读取引擎 30 次。 */
        private const val SERVICE_START_ATTEMPTS: Int = 30

        /** 每次等待 service 初始化的间隔，单位毫秒。 */
        private const val SERVICE_START_RETRY_DELAY_MS: Long = 100L
    }
}

/**
 * 惰性启动期间缓存的通知渲染状态，确保 service attach 后能重放最新共享状态。
 */
private data class NotificationState(
    val isPlaying: Boolean,
    val isFavorite: Boolean,
    val playbackMode: PlaybackMode,
    val playbackStatus: PlaybackStatus,
)

/**
 * 当前待重放到 service 的通知命令。
 */
private sealed interface PendingNotificationCommand {
    /**
     * 用最新共享状态刷新通知。
     */
    data class Show(
        val state: NotificationState,
    ) : PendingNotificationCommand

    /**
     * 移除当前通知。
     */
    data object Clear : PendingNotificationCommand
}
