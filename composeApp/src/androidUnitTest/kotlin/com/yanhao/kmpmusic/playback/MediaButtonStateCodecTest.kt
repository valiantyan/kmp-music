package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class MediaButtonStateCodecTest {
    @Test
    fun updateButtonArgsRoundTripMediaButtonState() {
        val state =
            MediaButtonState(
                shouldShowPauseButton = true,
                isFavorite = true,
                playbackMode = PlaybackMode.Shuffle,
                playbackStatus = PlaybackStatus.Playing,
                hasActivePlaybackSession = true,
            )

        val args = MediaButtonStateCodec.createUpdateButtonsArgs(state = state)

        assertEquals(expected = state, actual = MediaButtonStateCodec.resolveUpdateButtonsState(args = args))
    }

    @Test
    fun invalidPlaybackModeReturnsNull() {
        val args =
            MediaButtonStateCodec.createUpdateButtonsArgs(
                state =
                    MediaButtonState(
                        shouldShowPauseButton = true,
                        isFavorite = false,
                        playbackMode = PlaybackMode.LoopAll,
                        playbackStatus = PlaybackStatus.Playing,
                        hasActivePlaybackSession = true,
                    ),
            )
        args.putString("playback_mode", "BrokenMode")

        assertNull(actual = MediaButtonStateCodec.resolveUpdateButtonsState(args = args))
    }

    @Test
    fun invalidPlaybackStatusReturnsNull() {
        val args =
            MediaButtonStateCodec.createUpdateButtonsArgs(
                state =
                    MediaButtonState(
                        shouldShowPauseButton = false,
                        isFavorite = false,
                        playbackMode = PlaybackMode.LoopOne,
                        playbackStatus = PlaybackStatus.Paused,
                        hasActivePlaybackSession = true,
                    ),
            )
        args.putString("playback_status", "BrokenStatus")

        assertNull(actual = MediaButtonStateCodec.resolveUpdateButtonsState(args = args))
    }
}
