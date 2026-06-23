package com.yanhao.kmpmusic.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
    fun coverForSourceIdReturnsStableCover() {
        val firstCover = LocalAudioFileRules.coverForSourceId(sourceId = "/Music/song.mp3")
        val secondCover = LocalAudioFileRules.coverForSourceId(sourceId = "/Music/song.mp3")

        assertNotNull(actual = firstCover)
        assertEquals(expected = firstCover, actual = secondCover)
    }
}
