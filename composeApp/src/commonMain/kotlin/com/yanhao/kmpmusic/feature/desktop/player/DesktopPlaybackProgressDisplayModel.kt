package com.yanhao.kmpmusic.feature.desktop.player

/**
 * 桌面播放进度显示模型，统一底栏和播放页对偏短时长的兜底策略。
 *
 * @property positionMs 用于时间文案的当前进度。
 * @property durationMs 用于时间文案和滑杆范围的显示时长。
 * @property sliderValue 滑杆当前位置。
 * @property sliderRange 滑杆可拖动范围。
 * @property isSeekEnabled 当前是否允许 seek。
 */
internal data class DesktopPlaybackProgressDisplayModel(
    val positionMs: Long,
    val durationMs: Long,
    val sliderValue: Float,
    val sliderRange: ClosedFloatingPointRange<Float>,
    val isSeekEnabled: Boolean,
)

/**
 * 构建桌面播放进度显示模型；当平台时长偏短时，临时扩展显示时长避免满格后继续播放。
 */
internal fun buildDesktopPlaybackProgressDisplayModel(
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    isPlaying: Boolean,
    fallbackDurationMs: Long? = null,
): DesktopPlaybackProgressDisplayModel {
    val safePositionMs: Long = playbackPositionMs.coerceAtLeast(minimumValue = 0L)
    val knownDurationMs: Long = (playbackDurationMs ?: fallbackDurationMs)?.coerceAtLeast(minimumValue = 0L) ?: 0L
    val displayDurationMs: Long = resolveDesktopPlaybackDisplayDuration(
        positionMs = safePositionMs,
        durationMs = knownDurationMs,
        isPlaying = isPlaying,
    )
    val isSeekEnabled: Boolean = displayDurationMs > 0L
    return DesktopPlaybackProgressDisplayModel(
        positionMs = safePositionMs,
        durationMs = displayDurationMs,
        sliderValue = if (isSeekEnabled) {
            safePositionMs.coerceAtMost(maximumValue = displayDurationMs).toFloat()
        } else {
            0f
        },
        sliderRange = 0f..displayDurationMs.coerceAtLeast(minimumValue = 1L).toFloat(),
        isSeekEnabled = isSeekEnabled,
    )
}

// 平台 duration 偏短时给滑杆留一点余量，避免播放中状态视觉上停在终点。
private const val STALE_DURATION_HEADROOM_MS = 1_000L

// 只有播放中且已知 duration 被真实进度顶到时才扩展，未知时长仍保持不可 seek。
private fun resolveDesktopPlaybackDisplayDuration(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
): Long {
    if (durationMs <= 0L) {
        return 0L
    }
    if (positionMs < durationMs) {
        return durationMs
    }
    if (!isPlaying) {
        return durationMs
    }
    return positionMs + STALE_DURATION_HEADROOM_MS
}
