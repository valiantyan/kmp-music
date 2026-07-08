package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证首页歌曲行被搜索页复用时，视觉复用不破坏当前播放点击语义。
 */
class HomeSongRowBehaviorTest {
    @Test
    fun currentActiveOrPausedSongUsesPlaybackToggleWhenHandlerExists(): Unit {
        val toggleStatuses: List<PlaybackStatus> = listOf(
            PlaybackStatus.Playing,
            PlaybackStatus.Loading,
            PlaybackStatus.Buffering,
            PlaybackStatus.Paused,
        )

        toggleStatuses.forEach { status: PlaybackStatus ->
            assertEquals(
                expected = HomeSongRowClickAction.ToggleCurrentPlayback,
                actual = resolveHomeSongRowClickAction(
                    isCurrentSong = true,
                    currentPlaybackStatus = status,
                    hasCurrentSongToggle = true,
                ),
            )
        }
    }

    @Test
    fun currentIdleEndedOrErrorSongFallsBackToPlay(): Unit {
        val playStatuses: List<PlaybackStatus> = listOf(
            PlaybackStatus.Idle,
            PlaybackStatus.Ended,
            PlaybackStatus.Error,
        )

        playStatuses.forEach { status: PlaybackStatus ->
            assertEquals(
                expected = HomeSongRowClickAction.PlaySelectedSong,
                actual = resolveHomeSongRowClickAction(
                    isCurrentSong = true,
                    currentPlaybackStatus = status,
                    hasCurrentSongToggle = true,
                ),
            )
        }
    }

    @Test
    fun missingToggleHandlerKeepsHomeDefaultPlayBehavior(): Unit {
        assertEquals(
            expected = HomeSongRowClickAction.PlaySelectedSong,
            actual = resolveHomeSongRowClickAction(
                isCurrentSong = true,
                currentPlaybackStatus = PlaybackStatus.Playing,
                hasCurrentSongToggle = false,
            ),
        )
    }

    @Test
    fun nonCurrentSongAlwaysPlaysSelectedSong(): Unit {
        assertEquals(
            expected = HomeSongRowClickAction.PlaySelectedSong,
            actual = resolveHomeSongRowClickAction(
                isCurrentSong = false,
                currentPlaybackStatus = PlaybackStatus.Playing,
                hasCurrentSongToggle = true,
            ),
        )
    }
}
