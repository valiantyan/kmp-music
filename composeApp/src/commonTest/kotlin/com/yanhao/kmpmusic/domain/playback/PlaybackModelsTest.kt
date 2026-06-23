package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackModelsTest {
    @Test
    fun playingStateDerivesIsPlaying(): Unit {
        val playing: PlaybackState = PlaybackState(
            currentSongId = "song-1",
            status = PlaybackStatus.Playing,
        )
        val paused: PlaybackState = PlaybackState(
            currentSongId = "song-1",
            status = PlaybackStatus.Paused,
        )
        assertTrue(actual = playing.isPlaying)
        assertFalse(actual = paused.isPlaying)
    }

    @Test
    fun queueStateExposesCurrentSongId(): Unit {
        val queue: QueueState = QueueState(
            songIds = listOf("song-1", "song-2", "song-3"),
            currentIndex = 1,
            playbackMode = PlaybackMode.LoopAll,
        )
        assertEquals(expected = "song-2", actual = queue.currentSongId)
    }

    @Test
    fun queueStateDefaultsToLoopAll(): Unit {
        assertEquals(
            expected = PlaybackMode.LoopAll,
            actual = QueueState().playbackMode,
        )
    }

    @Test
    fun playableMediaRequiresScannerUri(): Unit {
        val media: PlayableMedia = PlayableMedia(
            songId = "androidMediaStore:42",
            title = "设备里的歌",
            artist = "未知歌手",
            album = "未知专辑",
            durationMs = 180_000L,
            localUri = "content://media/external/audio/media/42",
            coverArt = CoverArt.HeroLocalMusic,
            mimeType = "audio/mpeg",
        )
        assertEquals(
            expected = "content://media/external/audio/media/42",
            actual = media.localUri,
        )
    }
}
