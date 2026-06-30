package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.AudioSource
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
        val loading: PlaybackState = PlaybackState(
            currentSongId = "song-1",
            status = PlaybackStatus.Loading,
        )
        val buffering: PlaybackState = PlaybackState(
            currentSongId = "song-1",
            status = PlaybackStatus.Buffering,
        )
        val paused: PlaybackState = PlaybackState(
            currentSongId = "song-1",
            status = PlaybackStatus.Paused,
        )
        assertTrue(actual = playing.isPlaying)
        assertFalse(actual = loading.isPlaying)
        assertFalse(actual = buffering.isPlaying)
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

    @Test
    fun playableMediaDerivesLocalAudioSourceFromAndroidContentUri(): Unit {
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
            expected = AudioSource.Local(uri = "content://media/external/audio/media/42"),
            actual = media.audioSource,
        )
    }

    @Test
    fun playableMediaDerivesLocalAudioSourceFromDesktopFileUri(): Unit {
        val media: PlayableMedia = PlayableMedia(
            songId = "desktop:/Users/tester/Music/song.flac",
            title = "Desktop Song",
            artist = "Artist",
            album = "Album",
            durationMs = 240_000L,
            localUri = "file:///Users/tester/Music/song.flac",
            coverArt = CoverArt.HeroLocalMusic,
            mimeType = "audio/flac",
        )

        assertEquals(
            expected = AudioSource.Local(uri = "file:///Users/tester/Music/song.flac"),
            actual = media.audioSource,
        )
    }
}
