package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.data.FakeAudioPlayerEngine
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证 [PlaybackCoordinator] 的队列、模式切换和失败处理规则。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackCoordinatorTest {
    /**
     * 点击列表中的歌曲时，应把整个列表写成播放队列。
     */
    @Test
    fun playSongUsesWholeCurrentListAsQueue(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val engine = FakeAudioPlayerEngine()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = engine,
            snapshotWriteScope = backgroundScope,
        )
        val songs = buildSongs(count = 5)

        coordinator.playSong(song = songs[2], queueSongs = songs)

        val queue = repository.getQueueState()
        assertEquals(expected = songs.map { song -> song.id }, actual = queue.songIds)
        assertEquals(expected = 2, actual = queue.currentIndex)
        assertEquals(expected = songs[2].id, actual = repository.getPlaybackState().currentSongId)
    }

    /**
     * 当点击歌曲不在当前列表里时，应退化为只播放这首歌，而不是误播旧列表首项。
     */
    @Test
    fun playSongFallsBackToSingleSongWhenQueueDoesNotContainTarget(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
            snapshotWriteScope = backgroundScope,
        )
        val songs = buildSongs(count = 4)

        coordinator.playSong(song = songs[3], queueSongs = songs.take(n = 2))

        assertEquals(expected = listOf(songs[3].id), actual = repository.getQueueState().songIds)
        assertEquals(expected = 0, actual = repository.getQueueState().currentIndex)
        assertEquals(expected = songs[3].id, actual = repository.getPlaybackState().currentSongId)
    }

    /**
     * 播放模式按钮应按设计稿循环切换三种模式。
     */
    @Test
    fun cyclePlaybackModeLoopsThroughThreeModes(): Unit {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
        )

        assertEquals(PlaybackMode.LoopAll, repository.getQueueState().playbackMode)
        coordinator.cyclePlaybackMode()
        assertEquals(PlaybackMode.LoopOne, repository.getQueueState().playbackMode)
        coordinator.cyclePlaybackMode()
        assertEquals(PlaybackMode.Shuffle, repository.getQueueState().playbackMode)
        coordinator.cyclePlaybackMode()
        assertEquals(PlaybackMode.LoopAll, repository.getQueueState().playbackMode)
    }

    /**
     * 列表循环在最后一首结束后应回到第一首。
     */
    @Test
    fun loopAllMovesFromLastSongToFirstSong(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
            snapshotWriteScope = backgroundScope,
        )
        val songs = buildSongs(count = 3)

        coordinator.playSong(song = songs[2], queueSongs = songs)
        coordinator.handleEngineEventForTest(PlaybackEngineEvent.Ended)

        assertEquals(expected = 0, actual = repository.getQueueState().currentIndex)
        assertEquals(expected = songs[0].id, actual = repository.getPlaybackState().currentSongId)
    }

    /**
     * 单曲循环在自然播完后应继续停留在当前歌曲。
     */
    @Test
    fun loopOneKeepsCurrentSongOnEnded(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
            snapshotWriteScope = backgroundScope,
        )
        val songs = buildSongs(count = 3)

        coordinator.playSong(song = songs[1], queueSongs = songs)
        coordinator.cyclePlaybackMode()
        coordinator.handleEngineEventForTest(PlaybackEngineEvent.Ended)

        assertEquals(expected = 1, actual = repository.getQueueState().currentIndex)
        assertEquals(expected = songs[1].id, actual = repository.getPlaybackState().currentSongId)
    }

    /**
     * 随机模式回退上一首时应优先使用历史，而不是重新随机。
     */
    @Test
    fun shufflePreviousUsesHistory(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
            snapshotWriteScope = backgroundScope,
            randomIndex = { candidates: List<Int> -> candidates.first() },
        )
        val songs = buildSongs(count = 4)

        coordinator.playSong(song = songs[0], queueSongs = songs)
        coordinator.cyclePlaybackMode()
        coordinator.cyclePlaybackMode()
        coordinator.moveNext()
        val shuffledIndex = repository.getQueueState().currentIndex
        coordinator.movePrevious()

        assertEquals(expected = 1, actual = shuffledIndex)
        assertEquals(expected = 0, actual = repository.getQueueState().currentIndex)
    }

    /**
     * 随机模式回退后再次前进时，待播集合不应把当前歌曲重新抽中。
     */
    @Test
    fun shuffleNextAfterPreviousDoesNotReplayCurrentSong(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
            snapshotWriteScope = backgroundScope,
            randomIndex = { candidates: List<Int> ->
                candidates.find { candidate -> candidate == 1 } ?: candidates.first()
            },
        )
        val songs = buildSongs(count = 3)

        coordinator.playSong(song = songs[0], queueSongs = songs)
        coordinator.cyclePlaybackMode()
        coordinator.cyclePlaybackMode()
        coordinator.moveNext()
        coordinator.moveNext()
        coordinator.movePrevious()
        coordinator.moveNext()

        assertEquals(expected = 0, actual = repository.getQueueState().currentIndex)
        assertEquals(expected = songs[0].id, actual = repository.getPlaybackState().currentSongId)
    }

    /**
     * 协调器启动后应把引擎事件持续折返到运行时仓库。
     */
    @Test
    fun startCollectsEngineEventsIntoRepository(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val engine = FakeAudioPlayerEngine()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = engine,
            snapshotWriteScope = backgroundScope,
        )
        val songs = buildSongs(count = 2)
        var updateCount = 0

        coordinator.start(scope = backgroundScope) {
            updateCount += 1
        }
        coordinator.playSong(song = songs[0], queueSongs = songs)
        engine.seekTo(positionMs = 24_000L)
        advanceUntilIdle()

        assertEquals(expected = PlaybackStatus.Playing, actual = repository.getPlaybackState().status)
        assertEquals(expected = 24_000L, actual = repository.getPlaybackState().positionMs)
        assertEquals(expected = 2, actual = repository.getQueueState().songIds.size)
        assertTrue(actual = updateCount > 0)
    }

    /**
     * 快照写入不应依赖 [PlaybackCoordinator.start] 先建立事件采集作用域。
     */
    @Test
    fun playSongPersistsSnapshotBeforeStart(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val snapshotStore = InMemoryPlaybackSnapshotStore()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
            playbackSnapshotStore = snapshotStore,
            snapshotWriteScope = backgroundScope,
        )
        val songs = buildSongs(count = 2)

        coordinator.playSong(song = songs[1], queueSongs = songs)
        advanceUntilIdle()

        val snapshot = snapshotStore.restoreSnapshot(
            availableSongIds = songs.map { song -> song.id }.toSet(),
        )

        assertEquals(expected = songs[1].id, actual = snapshot.playbackState.currentSongId)
        assertEquals(expected = 1, actual = snapshot.queueState.currentIndex)
    }

    /**
     * 恢复后的暂停快照应先把引擎预热到同一队列，再允许 toggle 直接继续播放。
     */
    @Test
    fun restoreSnapshotPrimesEngineForResume(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val snapshotStore = InMemoryPlaybackSnapshotStore()
        val engine = FakeAudioPlayerEngine()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = engine,
            playbackSnapshotStore = snapshotStore,
            snapshotWriteScope = backgroundScope,
        )
        val songs = buildSongs(count = 3)
        val restoredSong = songs[1]
        snapshotStore.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = restoredSong.id,
                    status = PlaybackStatus.Playing,
                    positionMs = 18_000L,
                    durationMs = restoredSong.durationMs,
                ),
                queueState = QueueState(
                    songIds = songs.map { song -> song.id },
                    currentIndex = 1,
                    playbackMode = PlaybackMode.LoopAll,
                ),
            ),
        )

        coordinator.start(scope = backgroundScope)
        coordinator.restoreSnapshot(availableSongs = songs)
        advanceUntilIdle()

        assertEquals(expected = PlaybackStatus.Paused, actual = repository.getPlaybackState().status)
        assertEquals(expected = restoredSong.id, actual = repository.getPlaybackState().currentSongId)
        assertEquals(expected = 18_000L, actual = repository.getPlaybackState().positionMs)

        coordinator.togglePlayback()
        advanceUntilIdle()

        assertEquals(expected = PlaybackStatus.Playing, actual = repository.getPlaybackState().status)
        assertEquals(expected = restoredSong.id, actual = repository.getPlaybackState().currentSongId)
        assertEquals(expected = 18_000L, actual = repository.getPlaybackState().positionMs)
    }

    /**
     * 单曲循环同一首连续失败三次后应停止自动重试。
     */
    @Test
    fun loopOneStopsAfterThreeFailuresForSameSong(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
            snapshotWriteScope = backgroundScope,
        )
        val songs = buildSongs(count = 1)

        coordinator.playSong(song = songs[0], queueSongs = songs)
        coordinator.cyclePlaybackMode()
        repeat(times = 3) {
            coordinator.handleEngineEventForTest(
                PlaybackEngineEvent.Failed(
                    error = PlaybackError(
                        type = PlaybackErrorType.Unknown,
                        songId = songs[0].id,
                        message = "坏文件",
                    ),
                ),
            )
        }

        assertEquals(expected = PlaybackStatus.Error, actual = repository.getPlaybackState().status)
    }

    /**
     * 一旦恢复到成功播放，跨歌曲的失败计数应被清零。
     */
    @Test
    fun successfulPlaybackResetsFailureCounters(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val coordinator = PlaybackCoordinator(
            playbackRepository = repository,
            audioPlayerEngine = FakeAudioPlayerEngine(),
            snapshotWriteScope = backgroundScope,
        )
        val songs = buildSongs(count = 4)

        coordinator.playSong(song = songs[0], queueSongs = songs)
        repeat(times = 2) { index ->
            coordinator.handleEngineEventForTest(
                PlaybackEngineEvent.Failed(
                    error = PlaybackError(
                        type = PlaybackErrorType.Unknown,
                        songId = songs[index].id,
                        message = "坏文件",
                    ),
                ),
            )
        }
        coordinator.handleEngineEventForTest(
            PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Playing,
                positionMs = 0L,
                durationMs = 180_000L,
            ),
        )
        coordinator.handleEngineEventForTest(
            PlaybackEngineEvent.Failed(
                error = PlaybackError(
                    type = PlaybackErrorType.Unknown,
                    songId = songs[2].id,
                    message = "坏文件",
                ),
            ),
        )

        assertEquals(expected = PlaybackStatus.Loading, actual = repository.getPlaybackState().status)
        assertEquals(expected = songs[3].id, actual = repository.getPlaybackState().currentSongId)
    }

    /**
     * 构造本地歌曲样本，避免测试依赖 UI seed 数据。
     */
    private fun buildSongs(count: Int): List<Song> {
        return (0 until count).map { index ->
            Song(
                id = "song-$index",
                title = "Song $index",
                artist = "Artist",
                album = "Album",
                duration = "3:00",
                coverArt = CoverArt.HeroLocalMusic,
                isLiked = false,
                lastPlayed = "未播放",
                quality = "本地 MP3",
                lyric = "Local",
                trackNumber = index + 1,
                durationMs = 180_000L,
                sourceId = "$index",
                sourceKind = LocalMusicSourceKind.AndroidMediaStore,
                localUri = "content://media/external/audio/media/$index",
                mimeType = "audio/mpeg",
            )
        }
    }
}
