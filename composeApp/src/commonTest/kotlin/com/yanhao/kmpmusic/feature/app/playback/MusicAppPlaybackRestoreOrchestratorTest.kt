package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicAppPlaybackRestoreOrchestratorTest {
    @Test
    fun restoreMarksPendingWhenSavedSongsCannotBeResolved(): Unit = runTest {
        val orchestrator: PlaybackRestoreOrchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = snapshotStoreWithQueue(songIds = listOf("missing")),
            availableSongsResolver = { _, _ -> emptyList() },
            restoreSnapshot = { error("restoreSnapshot should not run when songs are unavailable") },
        )

        val result: PlaybackRestoreOrchestrator.Result = orchestrator.restore(
            state = testState(),
            preferredSongs = emptyList(),
        )

        assertTrue(actual = result.isPending)
        assertEquals(expected = emptyList(), actual = result.state.queueSongsSnapshot)
    }

    @Test
    fun restoreResolvesAvailableSongsAndCallsPlaybackCoordinator(): Unit = runTest {
        val restoredCalls: MutableList<List<String>> = mutableListOf()
        val song: Song = testSong(id = "song-1")
        val orchestrator: PlaybackRestoreOrchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = snapshotStoreWithQueue(songIds = listOf("song-1")),
            availableSongsResolver = { songIds: List<String>, preferredSongs: List<Song> ->
                preferredSongs.filter { candidate: Song -> songIds.contains(element = candidate.id) }
            },
            restoreSnapshot = { songs: List<Song> ->
                restoredCalls += songs.map { restoredSong: Song -> restoredSong.id }
            },
        )

        val result: PlaybackRestoreOrchestrator.Result = orchestrator.restore(
            state = testState(),
            preferredSongs = listOf(song),
        )

        assertFalse(actual = result.isPending)
        assertEquals(
            expected = listOf("song-1"),
            actual = result.state.queueSongsSnapshot.map { restoredSong: Song -> restoredSong.id },
        )
        assertEquals(expected = listOf(listOf("song-1")), actual = restoredCalls)
    }

    private suspend fun snapshotStoreWithQueue(songIds: List<String>): PlaybackSnapshotStore {
        val store: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore()
        store.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = songIds.firstOrNull(),
                    status = PlaybackStatus.Playing,
                ),
                queueState = QueueState(
                    songIds = songIds,
                    currentIndex = 0,
                ),
                updatedAt = 0L,
            ),
        )
        return store
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
