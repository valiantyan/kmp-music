package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class MusicAppPlaybackUiStateSynchronizerTest {
    @Test
    fun syncPlaybackStateProjectsPlaybackAndQueueIntoUiState(): Unit {
        val playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
        playbackRepository.saveQueueState(
            state = QueueState(
                songIds = listOf("song-1", "song-2"),
                currentIndex = 1,
                playbackMode = PlaybackMode.Shuffle,
            ),
        )
        playbackRepository.savePlaybackHistory(history = PlaybackHistory(songIds = listOf("song-2")))
        val synchronizer: PlaybackUiStateSynchronizer = PlaybackUiStateSynchronizer(
            playbackRepository = playbackRepository,
            recentSongsBuilder = { _, _ -> listOf(testSong(id = "song-2")) },
        )

        val nextState: MusicAppUiState = synchronizer.syncPlaybackState(
            state = testState(),
            playbackState = PlaybackState(
                currentSongId = "song-2",
                status = PlaybackStatus.Playing,
                positionMs = 12_000L,
                durationMs = 180_000L,
            ),
        )

        assertEquals(expected = "song-2", actual = nextState.currentSongId)
        assertEquals(expected = PlaybackStatus.Playing, actual = nextState.playbackStatus)
        assertEquals(expected = PlaybackMode.Shuffle, actual = nextState.playbackMode)
        assertEquals(expected = listOf("song-1", "song-2"), actual = nextState.queueSongIds)
        assertEquals(expected = listOf("song-2"), actual = nextState.recentSongs.map { song: Song -> song.id })
    }

    private fun testState(): MusicAppUiState {
        return MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        )
    }

    private fun testSong(id: String): Song {
        return Song(
            id = id,
            title = id,
            artist = "Artist",
            album = "Album",
            duration = "03:00",
            coverArt = CoverArt.HeroLocalMusic,
            isLiked = false,
            lastPlayed = "",
            quality = "Lossless",
            lyric = "",
            trackNumber = 1,
            durationMs = 180_000L,
        )
    }
}
