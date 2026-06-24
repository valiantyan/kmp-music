package com.yanhao.kmpmusic.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * [MediaController] 事件桥，把 Media3 Player 回调翻译成 common 层播放事件。
 */
@UnstableApi
internal class MediaControllerEventBridge(
    private val scope: CoroutineScope,
    private val emitEvent: (PlaybackEngineEvent) -> Unit,
) {
    // 当前正在观察的 controller，用于避免重复注册 listener。
    private var controller: MediaController? = null

    // 播放中定期上报进度的任务。
    private var progressJob: Job? = null

    // 监听官方 Player 状态，并翻译成 common 层事件。
    private val playerListener: Player.Listener = object : Player.Listener {
        /** Media3 状态变化时同步 common 播放状态。 */
        override fun onPlaybackStateChanged(playbackState: Int) {
            emitStatus()
            if (playbackState == Player.STATE_ENDED) {
                emitEvent(PlaybackEngineEvent.Ended)
            }
        }

        /** 真正进入或退出播放时切换进度轮询。 */
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            emitStatus()
            if (isPlaying) {
                startProgressUpdates()
                return
            }
            stopProgressUpdates()
        }

        /** 当前媒体变化由 MediaController 回流，确保系统按钮切歌也能更新 common 队列。 */
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val currentController: MediaController = controller ?: return
            val songId: String = mediaItem?.mediaId ?: return
            emitEvent(
                PlaybackEngineEvent.CurrentMediaChanged(
                    songId = songId,
                    index = currentController.currentMediaItemIndex,
                    durationMs = currentController.duration.takeIf { durationMs: Long -> durationMs > 0L },
                ),
            )
        }

        /** seek 后立即补发进度，避免 UI 等待下一次轮询。 */
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            emitProgress()
        }

        /** 播放错误统一映射为 common 错误模型。 */
        override fun onPlayerError(error: PlaybackException) {
            val currentController: MediaController = controller ?: return
            val mediaItem: MediaItem? = currentController.currentMediaItem
            stopProgressUpdates()
            emitEvent(
                PlaybackEngineEvent.Failed(
                    error = PlaybackError(
                        type = error.toPlaybackErrorType(),
                        songId = mediaItem?.mediaId,
                        message = error.message ?: "播放失败",
                    ),
                ),
            )
        }
    }

    /**
     * 观察最新 controller，重复连接同一对象时保持幂等。
     */
    fun attachController(controller: MediaController) {
        if (this.controller === controller) {
            return
        }
        this.controller?.removeListener(playerListener)
        this.controller = controller
        controller.addListener(playerListener)
    }

    /**
     * controller 断开时停止事件桥，防止进度轮询继续读旧对象。
     */
    fun detachController(controller: MediaController) {
        if (this.controller !== controller) {
            return
        }
        stopProgressUpdates()
        controller.removeListener(playerListener)
        this.controller = null
    }

    // 发出当前 controller 状态，供 common 仓库校正 UI。
    private fun emitStatus() {
        val currentController: MediaController = controller ?: return
        emitEvent(
            PlaybackEngineEvent.StatusChanged(
                status = currentController.toPlaybackStatus(),
                positionMs = currentController.currentPosition.coerceAtLeast(minimumValue = 0L),
                durationMs = currentController.duration.takeIf { durationMs: Long -> durationMs > 0L },
            ),
        )
    }

    // 发出当前播放进度，供 UI 和快照保持新鲜。
    private fun emitProgress() {
        val currentController: MediaController = controller ?: return
        emitEvent(
            PlaybackEngineEvent.ProgressChanged(
                positionMs = currentController.currentPosition.coerceAtLeast(minimumValue = 0L),
                durationMs = currentController.duration.takeIf { durationMs: Long -> durationMs > 0L },
            ),
        )
    }

    // 播放态轮询进度，避免仅依赖离散事件导致迷你播放器进度停滞。
    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) {
            return
        }
        progressJob = scope.launch(context = Dispatchers.Main.immediate) {
            while (isActive) {
                emitProgress()
                delay(timeMillis = 500L)
            }
        }
    }

    // 暂停或失败后停止进度轮询，避免后台空转。
    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    // 把 Media3 运行时状态翻译成 shared 枚举。
    private fun Player.toPlaybackStatus(): PlaybackStatus {
        if (isPlaying) {
            return PlaybackStatus.Playing
        }
        return when (playbackState) {
            Player.STATE_IDLE -> PlaybackStatus.Idle
            Player.STATE_BUFFERING -> PlaybackStatus.Buffering
            Player.STATE_READY -> PlaybackStatus.Paused
            Player.STATE_ENDED -> PlaybackStatus.Ended
            else -> PlaybackStatus.Idle
        }
    }

    // 把 Media3 错误映射到产品定义的显式错误类别。
    private fun PlaybackException.toPlaybackErrorType(): PlaybackErrorType {
        return when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> PlaybackErrorType.PermissionDenied
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> PlaybackErrorType.MissingFile
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            -> PlaybackErrorType.UnsupportedFormat
            else -> PlaybackErrorType.Unknown
        }
    }
}
