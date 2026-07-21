package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.feature.app.shouldShowPauseControl

/**
 * 首页歌曲行点击行为，允许搜索页复用首页视觉时保留当前播放 toggle 语义。
 */
internal enum class HomeSongRowClickAction {
    PlaySelectedSong,
    ToggleCurrentPlayback,
}

// 当前歌曲处于可切换状态时走全局播放控制，其余状态保留首页默认行点击播放。
internal fun resolveHomeSongRowClickAction(
    isCurrentSong: Boolean,
    currentPlaybackStatus: PlaybackStatus?,
    hasCurrentSongToggle: Boolean,
): HomeSongRowClickAction {
    if (!isCurrentSong || !hasCurrentSongToggle) {
        return HomeSongRowClickAction.PlaySelectedSong
    }
    if (currentPlaybackStatus?.shouldShowPauseControl == true || currentPlaybackStatus == PlaybackStatus.Paused) {
        return HomeSongRowClickAction.ToggleCurrentPlayback
    }
    return HomeSongRowClickAction.PlaySelectedSong
}
