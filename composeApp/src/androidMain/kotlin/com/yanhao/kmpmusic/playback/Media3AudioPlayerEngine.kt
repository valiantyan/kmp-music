package com.yanhao.kmpmusic.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 基于 Media3 [ExoPlayer] 的 Android 真播放引擎，实现 shared [AudioPlayerEngine] 契约。
 */
class Media3AudioPlayerEngine(
    // 读取 Compose resources assets 封面数据时使用的 application context。
    private val context: Context,
    // Service 内长期持有的 Media3 播放器实例。
    private val player: ExoPlayer,
    // 进度轮询与事件桥接使用的协程作用域。
    private val scope: CoroutineScope,
) : AudioPlayerEngine {
    // 对外暴露的引擎事件流。
    private val mutableEvents: MutableSharedFlow<PlaybackEngineEvent> = MutableSharedFlow(
        extraBufferCapacity = 128,
    )

    // 播放中定期上报进度的任务。
    private var progressJob: Job? = null

    // 当前引擎持有的共享队列镜像，用于把 Media3 下标翻译回 songId。
    private var queue: List<PlayableMedia> = emptyList()

    // Media3 播放错误会让播放器进入 idle，下一次重试或跳歌前必须重新 prepare。
    private var shouldPrepareAfterError: Boolean = false

    /**
     * 真实引擎事件流，供 [com.yanhao.kmpmusic.domain.playback.PlaybackCoordinator] 回写仓库。
     */
    override val events: Flow<PlaybackEngineEvent> = mutableEvents.asSharedFlow()

    init {
        player.addListener(
            object : Player.Listener {
                /** 监听 Media3 状态变化，并同步 shared 播放状态。 */
                override fun onPlaybackStateChanged(playbackState: Int) {
                    emitStatus()
                    if (playbackState == Player.STATE_ENDED) {
                        mutableEvents.tryEmit(PlaybackEngineEvent.Ended)
                    }
                }

                /** 仅在真正进入或退出播放时切换进度轮询，避免暂停后继续刷快照。 */
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    emitStatus()
                    if (isPlaying) {
                        startProgressUpdates()
                        return
                    }
                    stopProgressUpdates()
                }

                /** 每次切歌都把当前媒体和队列下标折返给 shared 协调器。 */
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val index: Int = player.currentMediaItemIndex
                    val media: PlayableMedia = queue.getOrNull(index = index) ?: return
                    mutableEvents.tryEmit(
                        PlaybackEngineEvent.CurrentMediaChanged(
                            songId = media.songId,
                            index = index,
                            durationMs = media.durationMs,
                        ),
                    )
                }

                /** 把 Media3 错误统一映射为 shared 可消费的显式错误类型。 */
                override fun onPlayerError(error: PlaybackException) {
                    val media: PlayableMedia? = queue.getOrNull(index = player.currentMediaItemIndex)
                    shouldPrepareAfterError = true
                    stopProgressUpdates()
                    mutableEvents.tryEmit(
                        PlaybackEngineEvent.Failed(
                            error = PlaybackError(
                                type = error.toPlaybackErrorType(),
                                songId = media?.songId,
                                message = error.message ?: "播放失败",
                            ),
                        ),
                    )
                }
            },
        )
    }

    /**
     * 用 shared 队列替换 Media3 播放列表，并把起始下标和进度一次性对齐。
     */
    override suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        queue = items
        shouldPrepareAfterError = false
        player.setMediaItems(
            items.map { item: PlayableMedia ->
                item.toMediaItem()
            },
            startIndex.coerceIn(
                minimumValue = 0,
                maximumValue = items.lastIndex.coerceAtLeast(minimumValue = 0),
            ),
            startPositionMs.coerceAtLeast(minimumValue = 0L),
        )
        player.prepare()
    }

    /** 继续播放当前媒体。 */
    override fun play() {
        prepareIfNeeded()
        player.play()
    }

    /** 暂停当前媒体。 */
    override fun pause() {
        player.pause()
    }

    /** 直接跳到目标进度。 */
    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(minimumValue = 0L))
    }

    /** 直接切换到 shared 队列中的目标歌曲。 */
    override fun skipToIndex(index: Int) {
        if (index !in 0 until player.mediaItemCount) {
            return
        }
        player.seekToDefaultPosition(index)
        prepareIfNeeded()
    }

    /** 停止播放时同时结束进度轮询，避免 service 空转。 */
    override fun stop() {
        stopProgressUpdates()
        shouldPrepareAfterError = false
        player.stop()
    }

    // 统一发出当前状态事件，避免多个 listener 分支重复拼装状态。
    private fun emitStatus() {
        mutableEvents.tryEmit(
            PlaybackEngineEvent.StatusChanged(
                status = player.toPlaybackStatus(),
                positionMs = player.currentPosition.coerceAtLeast(minimumValue = 0L),
                durationMs = player.duration.takeIf { durationMs: Long -> durationMs > 0L },
            ),
        )
    }

    // 只有真正进入播放态时才启动轮询，保证同一时刻只有一个任务存在。
    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) {
            return
        }
        progressJob = scope.launch(context = Dispatchers.Main.immediate) {
            while (isActive) {
                mutableEvents.emit(
                    PlaybackEngineEvent.ProgressChanged(
                        positionMs = player.currentPosition.coerceAtLeast(minimumValue = 0L),
                        durationMs = player.duration.takeIf { durationMs: Long -> durationMs > 0L },
                    ),
                )
                delay(timeMillis = 500L)
            }
        }
    }

    // 退出播放态后立即取消旧轮询，防止重复上报。
    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    // 错误恢复或 idle 状态切歌后需要重新 prepare，保证连续坏文件能继续推进队列。
    private fun prepareIfNeeded() {
        if (player.mediaItemCount <= 0) {
            return
        }
        if (!shouldPrepareAfterError && player.playbackState != Player.STATE_IDLE) {
            return
        }
        player.prepare()
        shouldPrepareAfterError = false
    }

    // 把 Media3 运行时状态翻译成 shared 枚举，供 UI 与持久化统一消费。
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

    // 把 Media3 错误映射到产品定义的显式错误类别，避免 UI 只能显示 Unknown。
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

    // 把平台无关媒体项映射为 Media3 媒体项，让系统媒体通知读取标题、歌手、时长和封面。
    private fun PlayableMedia.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(Uri.parse(localUri))
            .setMediaId(songId)
            .setMediaMetadata(toMediaMetadata())
            .build()
    }

    // 系统媒体通知只消费 Media3 metadata，不能再依赖应用自绘布局填充文案和封面。
    private fun PlayableMedia.toMediaMetadata(): MediaMetadata {
        val metadataBuilder: MediaMetadata.Builder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setDurationMs(durationMs?.takeIf { value: Long -> value >= 0L })
        AndroidPlaybackMediaMetadataAssets.artworkData(
            context = context,
            coverArt = coverArt,
        )?.let { artworkData: ByteArray ->
            metadataBuilder.setArtworkData(
                artworkData,
                MediaMetadata.PICTURE_TYPE_FRONT_COVER,
            )
        }
        return metadataBuilder.build()
    }
}
