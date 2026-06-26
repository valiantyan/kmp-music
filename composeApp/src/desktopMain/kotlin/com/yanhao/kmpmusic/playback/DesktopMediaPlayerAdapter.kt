package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import kotlinx.coroutines.flow.Flow

/**
 * 桌面端播放器适配缝，供后续的 vlcj 引擎和测试假实现共同遵守。
 */
interface DesktopMediaPlayerAdapter {
    /** 适配器向外发出的事件流，事件都带有请求它工作的媒体代号。 */
    val events: Flow<DesktopMediaPlayerEvent>

    /**
     * 准备单个音频媒体。
     *
     * @param songId 当前媒体对应的歌曲标识，用于错误归因。
     * @param mediaUri 媒体 URI。
     * @param generation 请求该工作的媒体代号。
     * @param startPositionMs 起始进度，单位毫秒。
     * @param pluginPath vlcj 插件目录路径，桌面端可为空。
     */
    suspend fun prepare(
        songId: String,
        mediaUri: String,
        generation: Long,
        startPositionMs: Long,
        pluginPath: String?,
    )

    /** 开始或继续播放已准备的媒体。 */
    suspend fun play(generation: Long)

    /** 暂停已准备的媒体。 */
    suspend fun pause(generation: Long)

    /**
     * 跳转已准备媒体的进度。
     *
     * @param positionMs 目标进度，单位毫秒。
     */
    suspend fun seekTo(
        generation: Long,
        positionMs: Long,
    )

    /** 停止当前适配器工作，但不主动释放底层原生资源。 */
    suspend fun stop(generation: Long)

    /** 返回适配器当前已知的进度。 */
    suspend fun currentPositionMs(): Long

    /** 返回适配器当前已知的总时长，未知时返回 null。 */
    suspend fun currentDurationMs(): Long?

    /** 设置底层播放器音量，传入值为 0 到 100 的平台音量。 */
    suspend fun setVolume(volumePercent: Int)

    /** 释放底层原生资源。 */
    suspend fun release()
}

/**
 * 桌面适配器向上层透出的事件，来源可以是真实 vlcj 回调，也可以是确定性的 fake。
 */
sealed interface DesktopMediaPlayerEvent {
    /** 事件对应的媒体代号。 */
    val generation: Long

    /**
     * 媒体准备完成。
     *
     * @property generation 事件对应的媒体代号。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Prepared(
        override val generation: Long,
        val durationMs: Long?,
    ) : DesktopMediaPlayerEvent

    /**
     * 媒体进入播放中。
     *
     * @property generation 事件对应的媒体代号。
     * @property positionMs 当前进度，单位毫秒。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Playing(
        override val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : DesktopMediaPlayerEvent

    /**
     * 媒体进入暂停。
     *
     * @property generation 事件对应的媒体代号。
     * @property positionMs 当前进度，单位毫秒。
     * @property durationMs 已知总时长，未知时为 null。
     */
    data class Paused(
        override val generation: Long,
        val positionMs: Long,
        val durationMs: Long?,
    ) : DesktopMediaPlayerEvent

    /**
     * 媒体自然播放结束。
     *
     * @property generation 事件对应的媒体代号。
     */
    data class Finished(
        override val generation: Long,
    ) : DesktopMediaPlayerEvent

    /**
     * 媒体播放失败。
     *
     * @property generation 事件对应的媒体代号。
     * @property error 标准化后的播放错误。
     */
    data class Failed(
        override val generation: Long,
        val error: PlaybackError,
    ) : DesktopMediaPlayerEvent
}
