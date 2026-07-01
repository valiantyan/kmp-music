package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackHistoryRecorderTest {
    @Test
    fun newSongIsPlacedAtFront(): Unit {
        val repository = InMemoryPlaybackRepository()
        repository.savePlaybackHistory(history = PlaybackHistory(songIds = listOf("b", "c")))
        val recorder = PlaybackHistoryRecorder(playbackRepository = repository)

        recorder.record(songId = "a")

        assertEquals(expected = listOf("a", "b", "c"), actual = repository.getPlaybackHistory().songIds)
    }

    @Test
    fun duplicateSongMovesToFrontAndKeepsSingleEntry(): Unit {
        val repository = InMemoryPlaybackRepository()
        repository.savePlaybackHistory(history = PlaybackHistory(songIds = listOf("a", "b", "c")))
        val recorder = PlaybackHistoryRecorder(playbackRepository = repository)

        recorder.record(songId = "b")

        assertEquals(expected = listOf("b", "a", "c"), actual = repository.getPlaybackHistory().songIds)
    }

    @Test
    fun historyKeepsAtMostFiftySongs(): Unit {
        val repository = InMemoryPlaybackRepository()
        repository.savePlaybackHistory(
            history = PlaybackHistory(songIds = (1..55).map { index: Int -> "song-$index" }),
        )
        val recorder = PlaybackHistoryRecorder(playbackRepository = repository)

        recorder.record(songId = "new")

        val history = repository.getPlaybackHistory().songIds
        assertEquals(expected = 50, actual = history.size)
        assertEquals(expected = "new", actual = history.first())
        assertEquals(expected = "song-49", actual = history.last())
    }
}
