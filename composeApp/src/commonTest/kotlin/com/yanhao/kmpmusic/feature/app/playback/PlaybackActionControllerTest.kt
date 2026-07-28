package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.data.FakeAudioPlayerEngine
import com.yanhao.kmpmusic.data.FakeLocalMusicScanner
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
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
     * 收藏页队列动作必须先把完整实体列表写入 UI 快照，再交给协调器播放。
     */
    @Test
    fun preparePlayQueueKeepsSongsAndRequestedMode(): Unit =
        runTest {
            val songs: List<Song> = buildFakeSongs(count = 4)
            val fixture: PlaybackFixture = playbackFixture()

            val action: PlaybackActionController.PreparedPlayQueue? =
                fixture.controller.preparePlayQueue(
                    state = baseState(),
                    songs = songs,
                    playbackMode = PlaybackMode.Shuffle,
                )

            assertEquals(expected = songs, actual = action?.state?.queueSongsSnapshot)
            assertEquals(expected = songs, actual = action?.songs)
            assertEquals(expected = PlaybackMode.Shuffle, actual = action?.playbackMode)
        }

    /**
     * 空收藏列表没有可执行队列动作，防止按钮误触改变当前播放。
     */
    @Test
    fun preparePlayQueueReturnsNullForEmptySongs() {
        val fixture: PlaybackFixture = playbackFixture()

        val action: PlaybackActionController.PreparedPlayQueue? =
            fixture.controller.preparePlayQueue(
                state = baseState(),
                songs = emptyList(),
                playbackMode = PlaybackMode.LoopAll,
            )

        assertEquals(expected = null, actual = action)
    }

    /**
     * 未传入队列且目标歌曲在当前队列时，应复用当前队列实体。
     */
    @Test
    fun playSongReusesCurrentQueueWhenTargetExists(): Unit =
        runTest {
            val libraryRepository = InMemoryMusicLibraryRepository()
            libraryRepository.applyScanResult(
                request = LocalMusicScanRequest.Refresh,
                scanResult = FakeLocalMusicScanner(demoSongCount = 4).scan(request = LocalMusicScanRequest.Refresh),
                likedSongIds = emptySet(),
            )
            val songs: List<Song> = libraryRepository.getHomePreview(limit = 4)
            val fixture = playbackFixture()
            val state =
                baseState().copy(
                    queueSongsSnapshot = songs,
                    queueSongIds = songs.map { song: Song -> song.id },
                )

            val action: PlaybackActionController.PreparedPlaySong =
                fixture.controller.preparePlaySong(
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
    fun setVolumeCoercesStateAndEngine() {
        val fixture = playbackFixture()

        val nextState: MusicAppUiState =
            fixture.controller.setVolume(
                state = baseState(),
                volume = 2f,
            )

        assertEquals(expected = 1f, actual = nextState.playbackVolume)
        assertEquals(expected = 1f, actual = fixture.audioPlayerEngine.volume)
    }

    /**
     * 倍速入口要同时写 UI 状态和播放引擎，避免出现只改按钮文案的假生效。
     */
    @Test
    fun setPlaybackSpeedUpdatesStateAndEngine() {
        val fixture = playbackFixture()

        val nextState: MusicAppUiState =
            fixture.controller.setPlaybackSpeed(
                state = baseState(),
                playbackSpeed = PlaybackSpeed.Double,
            )

        assertEquals(expected = PlaybackSpeed.Double, actual = nextState.playbackSpeed)
        assertEquals(expected = PlaybackSpeed.Double, actual = fixture.audioPlayerEngine.playbackSpeed)
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
    val playbackCoordinator =
        PlaybackCoordinator(
            playbackRepository = playbackRepository,
            audioPlayerEngine = audioPlayerEngine,
            playbackSnapshotStore = playbackSnapshotStore,
        )
    return PlaybackFixture(
        controller =
            PlaybackActionController(
                playbackCoordinator = playbackCoordinator,
                playbackRepository = playbackRepository,
                playbackSnapshotStore = playbackSnapshotStore,
                controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
                nowMillis = { 1_719_360_000_000L },
            ),
        audioPlayerEngine = audioPlayerEngine,
    )
}

private fun baseState(): MusicAppUiState =
    MusicAppUiState(
        likedSongIds = emptySet(),
        currentSongId = null,
        playbackStatus = PlaybackStatus.Idle,
        queueSongIds = emptyList(),
    )

/** 通过共享内存曲库把 fake 扫描元数据投影为真实 [Song]，避免测试伪造领域对象。 */
private suspend fun buildFakeSongs(count: Int): List<Song> {
    val libraryRepository = InMemoryMusicLibraryRepository()
    libraryRepository.applyScanResult(
        request = LocalMusicScanRequest.Refresh,
        scanResult = FakeLocalMusicScanner(demoSongCount = count).scan(request = LocalMusicScanRequest.Refresh),
        likedSongIds = emptySet(),
    )
    return libraryRepository.getHomePreview(limit = count)
}
