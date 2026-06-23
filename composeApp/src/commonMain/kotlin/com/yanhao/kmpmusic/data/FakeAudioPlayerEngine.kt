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

    // 当前媒体已经推进到的进度，供状态事件保持一致。
    private var currentPositionMs: Long = 0L

    /** 对外暴露确定性事件流。 */
    override val events: SharedFlow<PlaybackEngineEvent> = mutableEvents.asSharedFlow()

    /** 注入完整队列；空队列直接回传统一失败事件。 */
    override suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        queue = items
        currentPositionMs = startPositionMs
        if (items.isEmpty()) {
            currentIndex = -1
            emitMissingQueueFailure()
            return
        }
        currentIndex = startIndex.coerceIn(minimumValue = 0, maximumValue = items.lastIndex)
        val media: PlayableMedia = queue.getOrNull(index = currentIndex) ?: run {
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
                positionMs = currentPositionMs,
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
                positionMs = currentPositionMs,
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
                positionMs = currentPositionMs,
                durationMs = media.durationMs,
            ),
        )
    }

    /** 对当前媒体发出进度跳转事件。 */
    override fun seekTo(positionMs: Long) {
        val media: PlayableMedia = queue.getOrNull(index = currentIndex) ?: return
        currentPositionMs = positionMs
        mutableEvents.tryEmit(
            PlaybackEngineEvent.ProgressChanged(
                positionMs = currentPositionMs,
                durationMs = media.durationMs,
            ),
        )
    }

    /** 对当前媒体发出切歌事件；空队列直接回传统一失败事件。 */
    override fun skipToIndex(index: Int) {
        if (queue.isEmpty()) {
            currentIndex = -1
            currentPositionMs = 0L
            emitMissingQueueFailure()
            return
        }
        currentIndex = index.coerceIn(minimumValue = 0, maximumValue = queue.lastIndex)
        currentPositionMs = 0L
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
        currentPositionMs = 0L
        mutableEvents.tryEmit(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Idle,
                positionMs = currentPositionMs,
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
