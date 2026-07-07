package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalAudioFileRulesTest {
    @Test
    fun matchAudioTypeRecognizesSupportedAudioExtensions() {
        val mp3Type: LocalAudioType? = LocalAudioFileRules.matchAudioType(fileName = "Track 01.MP3")
        val flacType: LocalAudioType? = LocalAudioFileRules.matchAudioType(fileName = "live.flac")

        assertEquals(expected = "audio/mpeg", actual = mp3Type?.mimeType)
        assertEquals(expected = "audio/flac", actual = flacType?.mimeType)
    }

    @Test
    fun matchAudioTypeRejectsNonAudioFiles() {
        val textType: LocalAudioType? = LocalAudioFileRules.matchAudioType(fileName = "notes.txt")
        val missingExtensionType: LocalAudioType? = LocalAudioFileRules.matchAudioType(fileName = "README")

        assertNull(actual = textType)
        assertNull(actual = missingExtensionType)
    }

    @Test
    fun titleFromFileNameRemovesOnlyLastExtension() {
        val title: String = LocalAudioFileRules.titleFromFileName(fileName = "artist.session.take1.m4a")

        assertEquals(expected = "artist.session.take1", actual = title)
    }

    @Test
    fun coverForSourceIdReturnsExplicitLocalMusicPlaceholder() {
        val firstCover = LocalAudioFileRules.coverForSourceId(sourceId = "/Music/song.mp3")
        val secondCover = LocalAudioFileRules.coverForSourceId(sourceId = "/Music/song.mp3")

        assertEquals(expected = CoverArt.HeroLocalMusic, actual = firstCover)
        assertEquals(expected = firstCover, actual = secondCover)
    }

    /** 短音频过滤只在用户开启偏好且时长已知时生效。 */
    @Test
    fun shouldIncludeByDurationUsesShortAudioPreference(): Unit {
        val defaultPreferences: LocalMusicDiscoveryPreferences = LocalMusicDiscoveryPreferences()
        val disabledPreferences: LocalMusicDiscoveryPreferences = defaultPreferences.copy(
            shouldIgnoreShortAudio = false,
        )

        assertFalse(
            actual = LocalAudioFileRules.shouldIncludeByDuration(
                durationMs = 29_999L,
                preferences = defaultPreferences,
            ),
        )
        assertTrue(
            actual = LocalAudioFileRules.shouldIncludeByDuration(
                durationMs = 30_000L,
                preferences = defaultPreferences,
            ),
        )
        assertTrue(
            actual = LocalAudioFileRules.shouldIncludeByDuration(
                durationMs = null,
                preferences = defaultPreferences,
            ),
        )
        assertTrue(
            actual = LocalAudioFileRules.shouldIncludeByDuration(
                durationMs = 1_000L,
                preferences = disabledPreferences,
            ),
        )
    }
}
