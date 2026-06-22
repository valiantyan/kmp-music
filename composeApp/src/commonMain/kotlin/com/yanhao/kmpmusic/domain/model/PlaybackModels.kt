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
 * @property currentSongId 当前歌曲标识，没有播放过歌曲时为 null。
 * @property isPlaying 是否正在播放。
 */
data class PlaybackState(
    val currentSongId: String? = null,
    val isPlaying: Boolean = false,
)

/**
 * 播放队列状态。
 *
 * @property songIds 当前播放队列中的歌曲标识。
 */
data class QueueState(
    val songIds: List<String> = emptyList(),
)

/**
 * 真实播放历史，扫描结果不会自动进入这里。
 *
 * @property songIds 最近播放歌曲标识，新播放的歌曲排在最前。
 */
data class PlaybackHistory(
    val songIds: List<String> = emptyList(),
)
