package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus

/**
 * 媒体按钮偏好状态，通过官方 custom command 从 controller 同步到 session。
 */
internal data class MediaButtonState(
    val isPlaying: Boolean,
    val isFavorite: Boolean,
    val playbackMode: PlaybackMode,
    val playbackStatus: PlaybackStatus,
)
