package com.yanhao.kmpmusic.playback

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UnavailableDesktopMediaPlayerAdapterTest {
    @Test
    fun prepareEmitsEngineUnavailableFailureAndOperationsRemainNoOp(): Unit = runTest {
        val adapter = UnavailableDesktopMediaPlayerAdapter()

        adapter.prepare(
            songId = "song-1",
            mediaUri = "file:///Users/test/Music/song.mp3",
            generation = 3L,
            startPositionMs = 12_000L,
            pluginPath = null,
        )

        assertEquals(
            expected = DesktopMediaPlayerEvent.Failed(
                generation = 3L,
                error = buildEngineUnavailableError(songId = "song-1"),
            ),
            actual = adapter.events.first(),
        )
        adapter.play(generation = 3L)
        adapter.pause(generation = 3L)
        adapter.seekTo(generation = 3L, positionMs = 24_000L)
        adapter.stop(generation = 3L)
        assertEquals(expected = 0L, actual = adapter.currentPositionMs())
        assertNull(actual = adapter.currentDurationMs())
        adapter.release()
    }
}
