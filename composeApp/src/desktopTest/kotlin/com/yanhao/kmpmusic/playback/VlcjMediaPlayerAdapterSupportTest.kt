package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import kotlin.test.Test
import kotlin.test.assertEquals

class VlcjMediaPlayerAdapterSupportTest {
    @Test
    fun engineUnavailableErrorKeepsSongIdAndType(): Unit {
        val error = buildEngineUnavailableError(songId = "song-1")
        assertEquals(expected = PlaybackErrorType.EngineUnavailable, actual = error.type)
        assertEquals(expected = "song-1", actual = error.songId)
        assertEquals(
            expected = "播放器组件不可用，请重新安装应用或联系开发者。",
            actual = error.message,
        )
    }

    @Test
    fun callbackSnapshotKeepsItsOwnGenerationAndSongId(): Unit {
        val firstSnapshot = VlcjMediaCallbackSnapshot(
            generation = 7L,
            songId = "song-7",
        )
        val secondSnapshot = VlcjMediaCallbackSnapshot(
            generation = 8L,
            songId = "song-8",
        )
        assertEquals(expected = 7L, actual = firstSnapshot.playing(positionMs = 12L, durationMs = 34L).generation)
        assertEquals(expected = "song-7", actual = firstSnapshot.failed().error.songId)
        assertEquals(expected = 8L, actual = secondSnapshot.paused(positionMs = 56L, durationMs = null).generation)
        assertEquals(expected = "song-8", actual = secondSnapshot.failed().error.songId)
    }
}
