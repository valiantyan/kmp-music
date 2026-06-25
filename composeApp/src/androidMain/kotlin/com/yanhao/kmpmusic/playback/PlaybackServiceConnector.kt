package com.yanhao.kmpmusic.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Android 播放客户端适配器，通过官方 [MediaController] 连接 [MusicPlaybackService]。
 */
@UnstableApi
class PlaybackServiceConnector(
    // controller 连接、事件转发和进度轮询共用的进程级作用域。
    scope: CoroutineScope,
) : AudioPlayerEngine {
    // 对 common 层暴露的真实播放事件。
    private val mutableEvents: MutableSharedFlow<PlaybackEngineEvent> = MutableSharedFlow(
        extraBufferCapacity = 128,
    )

    // 最近一次同步给平台播放器的播放模式。
    private var playbackMode: PlaybackMode = PlaybackMode.LoopAll

    // applicationContext 供媒体项 mapper 读取 Compose resources assets。
    private var appContext: Context? = null

    // 最近一次媒体按钮状态；controller 就绪后会通过自定义 session 命令补发。
    private var pendingMediaButtonState: MediaButtonState? = null

    // Media3 Player 事件桥，专门负责状态和进度翻译。
    private val eventBridge: MediaControllerEventBridge = MediaControllerEventBridge(
        scope = scope,
        emitEvent = { event: PlaybackEngineEvent -> mutableEvents.tryEmit(event) },
    )

    // Media3 controller 连接管理，专门负责 SessionToken 绑定和断线清理。
    private val controllerConnection: MediaControllerConnection = MediaControllerConnection(
        onConnected = ::handleControllerConnected,
        onDisconnected = eventBridge::detachController,
        onConnectionFailed = ::emitEngineUnavailable,
    )

    /** 对 common 层暴露的播放事件流。 */
    override val events: Flow<PlaybackEngineEvent> = mutableEvents.asSharedFlow()

    /**
     * 注入 Android applicationContext，供 [MediaController] 绑定 [MusicPlaybackService]。
     */
    fun attachContext(context: Context) {
        appContext = context.applicationContext
        controllerConnection.attachContext(context = context)
    }

    /**
     * 通过 [MediaController] 给 service session 下发完整队列。
     */
    override suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        val controller: MediaController = controllerConnection.awaitController() ?: return emitEngineUnavailable()
        val context: Context = appContext ?: return emitEngineUnavailable()
        val mediaItems: List<MediaItem> = AndroidPlayableMediaMapper.toMediaItems(
            context = context,
            items = items,
        )
        if (!controller.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) {
            emitEngineUnavailable(message = "Android 播放控制器不允许替换媒体队列")
            return
        }
        controller.setMediaItems(
            mediaItems,
            startIndex.coerceIn(
                minimumValue = 0,
                maximumValue = mediaItems.lastIndex.coerceAtLeast(minimumValue = 0),
            ),
            startPositionMs.coerceAtLeast(minimumValue = 0L),
        )
        MediaControllerPlaybackModeMapper.apply(
            controller = controller,
            playbackMode = playbackMode,
        )
        if (controller.isCommandAvailable(Player.COMMAND_PREPARE)) {
            controller.prepare()
        }
    }

    /** 通过 [MediaController] 发送播放命令。 */
    override fun play() {
        executeWithController { controller: MediaController ->
            controller.play()
        }
    }

    /** 通过 [MediaController] 发送暂停命令。 */
    override fun pause() {
        executeWithController { controller: MediaController ->
            controller.pause()
        }
    }

    /** 通过 [MediaController] 发送 seek 命令。 */
    override fun seekTo(positionMs: Long) {
        executeWithController { controller: MediaController ->
            controller.seekTo(positionMs.coerceAtLeast(minimumValue = 0L))
        }
    }

    /** 通过 [MediaController] 跳到指定媒体下标。 */
    override fun skipToIndex(index: Int) {
        executeWithController { controller: MediaController ->
            if (!controller.isCommandAvailable(Player.COMMAND_SEEK_TO_MEDIA_ITEM)) {
                emitEngineUnavailable(message = "Android 播放控制器不允许切换媒体项")
                return@executeWithController
            }
            controller.seekToDefaultPosition(index)
        }
    }

    /** 同步 common 播放模式到 Media3，确保系统媒体按钮和应用内按钮行为一致。 */
    override fun setPlaybackMode(playbackMode: PlaybackMode) {
        this.playbackMode = playbackMode
        controllerConnection.currentController?.let { controller: MediaController ->
            MediaControllerPlaybackModeMapper.apply(
                controller = controller,
                playbackMode = playbackMode,
            )
        }
    }

    /** 通过 [MediaController] 停止播放。 */
    override fun stop() {
        executeWithController { controller: MediaController ->
            controller.stop()
        }
    }

    /**
     * 通过官方自定义 session 命令刷新媒体按钮偏好。
     */
    fun refreshMediaButtonPreferences(
        isPlaying: Boolean,
        isFavorite: Boolean,
        playbackMode: PlaybackMode,
        playbackStatus: PlaybackStatus,
        hasActivePlaybackSession: Boolean,
    ) {
        pendingMediaButtonState = MediaButtonState(
            isPlaying = isPlaying,
            isFavorite = isFavorite,
            playbackMode = playbackMode,
            playbackStatus = playbackStatus,
            hasActivePlaybackSession = hasActivePlaybackSession,
        )
        executeWithController { controller: MediaController ->
            MediaButtonStateSender.send(
                controller = controller,
                state = pendingMediaButtonState,
            )
        }
    }

    /**
     * 没有活动歌曲时只清理已连接 session；未连接时不主动拉起播放服务。
     */
    fun clearNotification() {
        pendingMediaButtonState = MediaButtonState(
            isPlaying = false,
            isFavorite = false,
            playbackMode = playbackMode,
            playbackStatus = PlaybackStatus.Idle,
            hasActivePlaybackSession = false,
        )
        controllerConnection.currentController?.let { controller: MediaController ->
            MediaButtonStateSender.send(
                controller = controller,
                state = pendingMediaButtonState,
            )
        }
    }

    // 执行需要 controller 的非挂起命令，缺少 context 或连接失败时发出统一错误。
    private fun executeWithController(command: (MediaController) -> Unit) {
        if (!controllerConnection.execute(command = command)) {
            emitEngineUnavailable()
        }
    }

    // controller 连接成功后注册事件桥、同步模式，并补发最新媒体按钮状态。
    private fun handleControllerConnected(controller: MediaController) {
        eventBridge.attachController(controller = controller)
        MediaControllerPlaybackModeMapper.apply(
            controller = controller,
            playbackMode = playbackMode,
        )
        MediaButtonStateSender.send(
            controller = controller,
            state = pendingMediaButtonState,
        )
    }

    // 统一抛出 controller 不可用错误，供 shared 协调器和 UI 显式兜底。
    private fun emitEngineUnavailable(message: String = "Android 播放控制器尚未就绪") {
        mutableEvents.tryEmit(
            PlaybackEngineEvent.Failed(
                error = PlaybackError(
                    type = PlaybackErrorType.EngineUnavailable,
                    songId = null,
                    message = message,
                ),
            ),
        )
    }
}
