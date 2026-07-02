package com.yanhao.kmpmusic.playback

/**
 * 最近一次上层播放控制意图，用来区分真实暂停和底层换媒体噪音。
 */
internal enum class DesktopPlaybackControlIntent {
    None,
    Play,
    Pause,
}
