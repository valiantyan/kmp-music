package com.yanhao.kmpmusic.feature.desktop.components

import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 桌面歌曲表格播放位行为测试，保持当前歌曲错误态可重新播放。
 */
class DesktopSongTableBehaviorTest {
    @Test
    fun currentActiveOrPausedSongUsesPlaybackToggle(): Unit {
        assertTrue(
            actual = shouldToggleDesktopSongTableCurrentPlayback(
                isCurrentSong = true,
                currentPlaybackStatus = PlaybackStatus.Playing,
            ),
        )
        assertTrue(
            actual = shouldToggleDesktopSongTableCurrentPlayback(
                isCurrentSong = true,
                currentPlaybackStatus = PlaybackStatus.Loading,
            ),
        )
        assertTrue(
            actual = shouldToggleDesktopSongTableCurrentPlayback(
                isCurrentSong = true,
                currentPlaybackStatus = PlaybackStatus.Buffering,
            ),
        )
        assertTrue(
            actual = shouldToggleDesktopSongTableCurrentPlayback(
                isCurrentSong = true,
                currentPlaybackStatus = PlaybackStatus.Paused,
            ),
        )
    }

    @Test
    fun currentIdleEndedOrErrorSongFallsBackToPlay(): Unit {
        assertFalse(
            actual = shouldToggleDesktopSongTableCurrentPlayback(
                isCurrentSong = true,
                currentPlaybackStatus = PlaybackStatus.Idle,
            ),
        )
        assertFalse(
            actual = shouldToggleDesktopSongTableCurrentPlayback(
                isCurrentSong = true,
                currentPlaybackStatus = PlaybackStatus.Ended,
            ),
        )
        assertFalse(
            actual = shouldToggleDesktopSongTableCurrentPlayback(
                isCurrentSong = true,
                currentPlaybackStatus = PlaybackStatus.Error,
            ),
        )
    }

    @Test
    fun nonCurrentSongAlwaysFallsBackToPlay(): Unit {
        assertFalse(
            actual = shouldToggleDesktopSongTableCurrentPlayback(
                isCurrentSong = false,
                currentPlaybackStatus = PlaybackStatus.Playing,
            ),
        )
    }
}
