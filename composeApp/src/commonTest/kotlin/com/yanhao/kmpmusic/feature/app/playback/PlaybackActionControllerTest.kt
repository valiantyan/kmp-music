package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.data.FakeAudioPlayerEngine
import com.yanhao.kmpmusic.data.FakeLocalMusicScanner
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.playback.PlaybackCoordinator
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackActionControllerTest {
    /**
     * 未传入队列且目标歌曲在当前队列时，应复用当前队列实体。
     */
    @Test
    fun playSongReusesCurrentQueueWhenTargetExists(): Unit = runTest {
        val libraryRepository = InMemoryMusicLibraryRepository()
        libraryRepository.applyScanResult(
            request = LocalMusicScanRequest.Refresh,
            scanResult = FakeLocalMusicScanner(demoSongCount = 4).scan(request = LocalMusicScanRequest.Refresh),
            likedSongIds = emptySet(),
        )
        val songs: List<Song> = libraryRepository.getHomePreview(limit = 4)
        val fixture = playbackFixture()
        val state = baseState().copy(
            queueSongsSnapshot = songs,
            queueSongIds = songs.map { song: Song -> song.id },
        )

        val action: PlaybackActionController.PreparedPlaySong = fixture.controller.preparePlaySong(
            state = state,
            song = songs[2],
            queueSongs = emptyList(),
        )

        assertEquals(
            expected = songs.map { song: Song -> song.id },
            actual = action.state.queueSongsSnapshot.map { song: Song -> song.id },
        )
        assertEquals(expected = songs[2].id, actual = action.song.id)
    }

    /**
     * 音量归一化后要同时写 UI 状态和播放引擎。
     */
    @Test
    fun setVolumeCoercesStateAndEngine(): Unit {
        val fixture = playbackFixture()

        val nextState: MusicAppUiState = fixture.controller.setVolume(
            state = baseState(),
            volume = 2f,
        )

        assertEquals(expected = 1f, actual = nextState.playbackVolume)
        assertEquals(expected = 1f, actual = fixture.audioPlayerEngine.volume)
    }
}

private data class PlaybackFixture(
    val controller: PlaybackActionController,
    val audioPlayerEngine: FakeAudioPlayerEngine,
)

private fun playbackFixture(): PlaybackFixture {
    val playbackRepository = InMemoryPlaybackRepository()
    val audioPlayerEngine = FakeAudioPlayerEngine()
    val playbackSnapshotStore = InMemoryPlaybackSnapshotStore()
    val playbackCoordinator = PlaybackCoordinator(
        playbackRepository = playbackRepository,
        audioPlayerEngine = audioPlayerEngine,
        playbackSnapshotStore = playbackSnapshotStore,
    )
    return PlaybackFixture(
        controller = PlaybackActionController(
            playbackCoordinator = playbackCoordinator,
            playbackRepository = playbackRepository,
            playbackSnapshotStore = playbackSnapshotStore,
            controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            nowMillis = { 1_719_360_000_000L },
        ),
        audioPlayerEngine = audioPlayerEngine,
    )
}

private fun baseState(): MusicAppUiState {
    return MusicAppUiState(
        likedSongIds = emptySet(),
        currentSongId = null,
        playbackStatus = PlaybackStatus.Idle,
        queueSongIds = emptyList(),
    )
}
