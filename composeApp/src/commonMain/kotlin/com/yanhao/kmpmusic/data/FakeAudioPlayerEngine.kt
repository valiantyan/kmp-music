package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 供 common 测试使用的确定性假播放引擎。
 */
class FakeAudioPlayerEngine : AudioPlayerEngine {
    // 对外事件流的内部可写实现。
    private val mutableEvents: MutableSharedFlow<PlaybackEngineEvent> = MutableSharedFlow(
        extraBufferCapacity = 64,
    )

    // 当前引擎持有的媒体队列。
    private var queue: List<PlayableMedia> = emptyList()

    // 当前引擎指向的队列下标。
    private var currentIndex: Int = -1

    /** 对外暴露确定性事件流。 */
    override val events: SharedFlow<PlaybackEngineEvent> = mutableEvents.asSharedFlow()

    /** 注入完整队列并先发出媒体切换和 loading 事件。 */
    override suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        queue = items
        currentIndex = startIndex.coerceIn(minimumValue = 0, maximumValue = items.lastIndex)
        val media: PlayableMedia? = queue.getOrNull(index = currentIndex)
        if (media == null) {
            emitMissingQueueFailure()
            return
        }
        mutableEvents.tryEmit(
            PlaybackEngineEvent.CurrentMediaChanged(
                songId = media.songId,
                index = currentIndex,
                durationMs = media.durationMs,
            ),
        )
        mutableEvents.tryEmit(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Loading,
                positionMs = startPositionMs,
                durationMs = media.durationMs,
            ),
        )
    }

    /** 对当前媒体发出播放事件。 */
    override fun play() {
        val media: PlayableMedia = queue.getOrNull(index = currentIndex) ?: return
        mutableEvents.tryEmit(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Playing,
                positionMs = 0L,
                durationMs = media.durationMs,
            ),
        )
    }

    /** 对当前媒体发出暂停事件。 */
    override fun pause() {
        val media: PlayableMedia = queue.getOrNull(index = currentIndex) ?: return
        mutableEvents.tryEmit(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Paused,
                positionMs = 0L,
                durationMs = media.durationMs,
            ),
        )
    }

    /** 对当前媒体发出进度跳转事件。 */
    override fun seekTo(positionMs: Long) {
        val media: PlayableMedia = queue.getOrNull(index = currentIndex) ?: return
        mutableEvents.tryEmit(
            PlaybackEngineEvent.ProgressChanged(
                positionMs = positionMs,
                durationMs = media.durationMs,
            ),
        )
    }

    /** 对当前媒体发出切歌事件。 */
    override fun skipToIndex(index: Int) {
        currentIndex = index.coerceIn(minimumValue = 0, maximumValue = queue.lastIndex)
        val media: PlayableMedia = queue.getOrNull(index = currentIndex) ?: return
        mutableEvents.tryEmit(
            PlaybackEngineEvent.CurrentMediaChanged(
                songId = media.songId,
                index = currentIndex,
                durationMs = media.durationMs,
            ),
        )
    }

    /** 发出停止后的 idle 事件。 */
    override fun stop() {
        mutableEvents.tryEmit(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Idle,
                positionMs = 0L,
                durationMs = null,
            ),
        )
    }

    /** 供后续测试显式模拟播放结束。 */
    fun emitEnded() {
        mutableEvents.tryEmit(PlaybackEngineEvent.Ended)
    }

    /**
     * 供后续测试显式模拟播放失败。
     *
     * @param songId 出错歌曲标识。
     * @param message 失败信息。
     */
    fun emitFailure(songId: String, message: String = "播放失败") {
        mutableEvents.tryEmit(
            PlaybackEngineEvent.Failed(
                error = PlaybackError(
                    type = PlaybackErrorType.Unknown,
                    songId = songId,
                    message = message,
                ),
            ),
        )
    }

    // 用统一错误形状表达空队列。
    private fun emitMissingQueueFailure() {
        mutableEvents.tryEmit(
            PlaybackEngineEvent.Failed(
                error = PlaybackError(
                    type = PlaybackErrorType.MissingFile,
                    songId = null,
                    message = "播放队列为空",
                ),
            ),
        )
    }
}
