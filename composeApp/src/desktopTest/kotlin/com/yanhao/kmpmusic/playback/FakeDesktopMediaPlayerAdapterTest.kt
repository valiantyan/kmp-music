package com.yanhao.kmpmusic.playback

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeDesktopMediaPlayerAdapterTest {
    @Test
    fun recordsCommandsAndEmitsGenerationEvents(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val events = mutableListOf<DesktopMediaPlayerEvent>()
        val collectJob = launch {
            adapter.events.take(count = 2).toList(destination = events)
        }

        adapter.prepare(
            mediaUri = "file:///Users/test/Music/song.mp3",
            generation = 7L,
            startPositionMs = 12_000L,
            pluginPath = "/Applications/KMP Music.app/Contents/Frameworks/LibVLC/plugins",
        )
        adapter.emitPrepared(generation = 7L, durationMs = 180_000L)
        adapter.play(generation = 7L)
        adapter.emitPlaying(generation = 7L, positionMs = 12_000L, durationMs = 180_000L)
        collectJob.join()

        assertEquals(
            expected = listOf(
                "prepare:file:///Users/test/Music/song.mp3:7:12000",
                "play:7",
            ),
            actual = adapter.commands,
        )
        assertEquals(
            expected = listOf(
                DesktopMediaPlayerEvent.Prepared(
                    generation = 7L,
                    durationMs = 180_000L,
                ),
                DesktopMediaPlayerEvent.Playing(
                    generation = 7L,
                    positionMs = 12_000L,
                    durationMs = 180_000L,
                ),
            ),
            actual = events,
        )
    }
}
