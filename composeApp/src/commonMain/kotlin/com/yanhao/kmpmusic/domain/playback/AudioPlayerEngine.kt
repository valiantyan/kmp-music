package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import kotlinx.coroutines.flow.Flow

/**
 * common 层和平台播放器之间的播放接口。
 *
 * 调用方只能通过队列、播放模式、音量和播放命令表达产品语义；真实解码、
 * 媒体会话、原生库加载和平台权限都属于平台 adapter 的实现细节。
 * 播放事实必须通过 [events] 回流 common 层，避免 UI、平台层和 repository
 * 同时成为播放状态真相源。
 */
interface AudioPlayerEngine {
    /**
     * 平台播放器主动上报的真实播放事件。
     *
     * [PlaybackCoordinator] 订阅该事件流后统一更新队列、进度、错误和快照。
     */
    val events: Flow<PlaybackEngineEvent>

    /**
     * 用新的媒体队列替换当前引擎状态。
     *
     * [items] 已经由 common 层整理好 metadata 和 [com.yanhao.kmpmusic.domain.model.AudioSource]；
     * 平台实现只负责把媒体项映射为对应平台播放器可消费的对象。
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
