package com.yanhao.kmpmusic.domain.model

/**
 * 搜索结果的范围过滤。
 */
enum class SearchScope {
    All,
    Songs,
    Albums,
    Artists,
}

/**
 * App 外观模式。
 */
enum class ThemeMode {
    Light,
    Dark,
    System,
}

/**
 * 当前播放状态，供迷你播放器和播放页共享。
 *
 * @property currentSongId 当前歌曲标识。
 * @property isPlaying 是否正在播放。
 */
data class PlaybackState(
    val currentSongId: String,
    val isPlaying: Boolean,
)

/**
 * 播放队列状态。
 *
 * @property songIds 当前播放队列中的歌曲标识。
 */
data class QueueState(
    val songIds: List<String>,
)
