package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import kotlinx.coroutines.flow.Flow

/**
 * 平台音频引擎契约，供 common 层协调真实播放。
 */
interface AudioPlayerEngine {
    /** 引擎主动上报的状态事件流。 */
    val events: Flow<PlaybackEngineEvent>

    /**
     * 用新的媒体队列替换当前引擎状态。
     *
     * @param items 新的可播放媒体列表。
     * @param startIndex 首次激活的队列下标。
     * @param startPositionMs 首次开始的进度。
     */
    suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long = 0L,
    )

    /** 开始或继续播放当前媒体。 */
    fun play()

    /** 暂停当前媒体。 */
    fun pause()

    /**
     * 跳转当前媒体进度。
     *
     * @param positionMs 目标进度，单位毫秒。
     */
    fun seekTo(positionMs: Long)

    /**
     * 直接切换到队列中的目标下标。
     *
     * @param index 目标队列下标。
     */
    fun skipToIndex(index: Int)

    /**
     * 同步当前播放模式，让平台播放器和系统媒体控制遵循同一套队列规则。
     */
    fun setPlaybackMode(playbackMode: PlaybackMode)

    /**
     * 设置当前播放器音量。
     *
     * @param volume 归一化音量，范围 0.0 到 1.0。
     */
    fun setVolume(volume: Float)

    /** 停止当前播放。 */
    fun stop()
}

/**
 * 播放引擎向 common 层发出的事件。
 */
sealed interface PlaybackEngineEvent {
    /**
     * 播放状态发生变化。
     *
     * @property status 当前播放状态。
     * @property positionMs 当前进度。
     * @property durationMs 当前总时长，未知时为 null。
     */
    data class StatusChanged(
        val status: PlaybackStatus,
        val positionMs: Long,
        val durationMs: Long?,
    ) : PlaybackEngineEvent

    /**
     * 当前媒体切换完成。
     *
     * @property songId 当前歌曲标识。
     * @property index 当前队列下标。
     * @property durationMs 当前总时长，未知时为 null。
     */
    data class CurrentMediaChanged(
        val songId: String,
        val index: Int,
        val durationMs: Long?,
    ) : PlaybackEngineEvent

    /**
     * 当前媒体进度变化。
     *
     * @property positionMs 当前进度。
     * @property durationMs 当前总时长，未知时为 null。
     */
    data class ProgressChanged(
        val positionMs: Long,
        val durationMs: Long?,
    ) : PlaybackEngineEvent

    /** 当前媒体自然播放结束。 */
    data object Ended : PlaybackEngineEvent

    /**
     * 当前播放请求失败。
     *
     * @property error 归一化后的播放错误。
     */
    data class Failed(
        val error: PlaybackError,
    ) : PlaybackEngineEvent
}
