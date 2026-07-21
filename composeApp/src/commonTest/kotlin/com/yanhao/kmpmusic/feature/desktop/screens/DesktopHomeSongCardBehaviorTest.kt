package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 桌面新版首页歌曲卡片行为测试，保持当前歌曲整行点击只切换播放状态。
 */
class DesktopHomeSongCardBehaviorTest {
    @Test
    fun currentSongCardUsesPlaybackToggle() {
        assertTrue(actual = shouldToggleDesktopHomeSongCardPlayback(isCurrentSong = true))
    }

    @Test
    fun nonCurrentSongCardStartsSelectedSong() {
        assertFalse(actual = shouldToggleDesktopHomeSongCardPlayback(isCurrentSong = false))
    }

    @Test
    fun currentSongCardVisualSpecMatchesFigmaActiveCard() {
        val spec: DesktopHomeSongCardVisualSpec =
            resolveDesktopHomeSongCardVisualSpec(
                isCurrentSong = true,
                isEvenRow = true,
            )

        assertEquals(expected = Color(0x0D006B5C), actual = spec.cardColor)
        assertEquals(expected = Color(0x33006B5C), actual = spec.cardBorderColor)
        assertEquals(expected = 0.dp, actual = spec.cardShadowElevation)
        assertEquals(expected = Color(0x33006B5C), actual = spec.artworkColor)
        assertEquals(expected = 2.dp, actual = spec.artworkShadowElevation)
        assertEquals(expected = DesktopHomeArtworkIconStyle.FigmaMusicNote, actual = spec.artworkIconStyle)
        assertEquals(expected = 15.dp, actual = spec.artworkIconWidth)
        assertEquals(expected = 22.5.dp, actual = spec.artworkIconHeight)
        assertEquals(expected = 28.dp, actual = spec.actionLeadingSpacerWidth)
    }

    @Test
    fun inactiveSongCardKeepsSamePlaceholderIconShapeAsCurrentSong() {
        val inactiveSpec: DesktopHomeSongCardVisualSpec =
            resolveDesktopHomeSongCardVisualSpec(
                isCurrentSong = false,
                isEvenRow = true,
            )
        val currentSpec: DesktopHomeSongCardVisualSpec =
            resolveDesktopHomeSongCardVisualSpec(
                isCurrentSong = true,
                isEvenRow = true,
            )

        assertEquals(expected = currentSpec.artworkIconStyle, actual = inactiveSpec.artworkIconStyle)
        assertEquals(expected = currentSpec.artworkIconWidth, actual = inactiveSpec.artworkIconWidth)
        assertEquals(expected = currentSpec.artworkIconHeight, actual = inactiveSpec.artworkIconHeight)
    }

    @Test
    fun playingIndicatorOnlyShowsForCurrentPlayingSong() {
        assertTrue(
            actual =
                shouldShowDesktopHomePlayingIndicator(
                    isCurrentSong = true,
                    isPlaying = true,
                ),
        )
        assertFalse(
            actual =
                shouldShowDesktopHomePlayingIndicator(
                    isCurrentSong = true,
                    isPlaying = false,
                ),
        )
        assertFalse(
            actual =
                shouldShowDesktopHomePlayingIndicator(
                    isCurrentSong = false,
                    isPlaying = true,
                ),
        )
    }
}
