package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun controllerInitialStateHasNoPlaybackBeforeUserActionEvenWhenLibraryPreviewExists(): Unit {
        val controller = MusicAppController(
            musicLibraryRepository = PreviewMusicLibraryRepository(),
            controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )
        val state: MusicAppUiState = controller.uiState

        assertNull(actual = state.currentSongId)
        assertNull(actual = state.currentSong)
        assertFalse(actual = state.isPlaying)
        assertTrue(actual = state.queueSongIds.isEmpty())
        assertTrue(actual = state.localSongs.isEmpty())
        assertEquals(expected = listOf("preview-song"), actual = state.homeLocalSongPreview.map { song: Song -> song.id })
        assertTrue(actual = state.recentSongs.isEmpty())
    }

    @Test
    fun idleStatusWithCurrentQueueKeepsPlaybackSessionActive(): Unit {
        val state: MusicAppUiState = MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = "song-1",
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = listOf("song-1", "song-2"),
        )

        assertTrue(actual = state.hasActivePlaybackSession)
    }

    @Test
    fun emptyIdleStatusHasNoActivePlaybackSession(): Unit {
        val state: MusicAppUiState = testState()

        assertFalse(actual = state.hasActivePlaybackSession)
    }

    @Test
    fun loadingAndBufferingExposePauseControlWithoutChangingPlayingState(): Unit {
        val loadingState: MusicAppUiState = testState().copy(
            currentSongId = "song-1",
            playbackStatus = PlaybackStatus.Loading,
            queueSongIds = listOf("song-1"),
        )
        val bufferingState: MusicAppUiState = loadingState.copy(
            playbackStatus = PlaybackStatus.Buffering,
        )

        assertFalse(actual = loadingState.isPlaying)
        assertFalse(actual = bufferingState.isPlaying)
        assertTrue(actual = loadingState.shouldShowPauseControl)
        assertTrue(actual = bufferingState.shouldShowPauseControl)
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

private class PreviewMusicLibraryRepository : MusicLibraryRepository {
    private val previewSong: Song = Song(
        id = "preview-song",
        title = "Preview Song",
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

    override fun getSnapshot(): LibrarySnapshot {
        return LibrarySnapshot(
            songs = listOf(previewSong),
            albums = emptyList(),
            artists = emptyList(),
            stats = getLibraryStats(),
            sources = emptyList(),
            scanState = LocalMusicScanState.Idle,
            lastScanSummary = null,
            problems = emptyList(),
        )
    }

    override fun getHomePreview(limit: Int): List<Song> {
        return listOf(previewSong).take(n = limit)
    }

    override fun getAllAvailableSongs(): List<Song> {
        return listOf(previewSong)
    }

    override fun getAvailableSongsByIds(songIds: List<String>): List<Song> {
        return listOf(previewSong).filter { song: Song -> songIds.contains(element = song.id) }
    }

    override fun getLibraryStats(): LibraryStats {
        return LibraryStats(songCount = 1)
    }

    override fun applyScanResult(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot {
        return getSnapshot()
    }
}
