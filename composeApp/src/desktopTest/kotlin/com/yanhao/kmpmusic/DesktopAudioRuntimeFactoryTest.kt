package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.data.FakeLocalMusicScanner
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.playback.PlaybackCoordinator
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.playback.DesktopAppleAudioPlayerEngine
import com.yanhao.kmpmusic.playback.FakeApplePlaybackBridge
import com.yanhao.kmpmusic.playback.FakeMacosAvFoundationLibraryLoader
import com.yanhao.kmpmusic.playback.FakeMacosAvFoundationSessionFactory
import com.yanhao.kmpmusic.playback.MacosAvFoundationNativeLibraryLoadResult
import com.yanhao.kmpmusic.playback.MacosAvFoundationPlaybackBridge
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopAudioRuntimeFactoryTest {
    /** 验证桌面默认运行时装配 Apple engine，而不是继续选择 vlcj engine。 */
    @Test
    fun createBuildsAppleEngineForDefaultDesktopRuntime(): Unit =
        runTest {
            val runtime: DesktopAudioRuntime =
                DesktopAudioRuntimeFactory.create(
                    sessionScope = this,
                    dispatcher = StandardTestDispatcher(testScheduler),
                )
            assertIs<DesktopAppleAudioPlayerEngine>(value = runtime.audioEngine)
            runtime.audioEngine.releaseAndAwait()
            advanceUntilIdle()
        }

    /** 验证从本地曲库播放会通过默认桌面 Apple engine 完成播放、暂停、seek 和停止。 */
    @Test
    fun localLibraryPlaybackControlsUseAppleBridgeThroughDesktopRuntime(): Unit =
        runTest {
            val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
            val runtime: DesktopAudioRuntime = createRuntime(bridge = bridge)
            val controller: MusicAppController = createController(runtime = runtime)
            val queueSongs: List<Song> = loadQueueSongs(controller = controller)
            val firstSong: Song = queueSongs[0]
            controller.playSong(song = firstSong, queueSongs = queueSongs)
            advanceUntilIdle()
            assertEquals(expected = "speed:1.0", actual = bridge.commands.first())
            assertEquals(
                expected = "prepare:${firstSong.id}:${firstSong.localUri}:1:0",
                actual = bridge.commands.last(),
            )
            bridge.emitPrepared(generation = 1L, durationMs = firstSong.durationMs)
            bridge.emitPlaying(generation = 1L, positionMs = 1_000L, durationMs = firstSong.durationMs)
            advanceUntilIdle()
            assertEquals(expected = "play:1", actual = bridge.commands.last())
            assertEquals(expected = firstSong.id, actual = controller.uiState.currentSongId)
            assertEquals(expected = PlaybackStatus.Playing, actual = controller.uiState.playbackStatus)
            controller.seekTo(positionMs = 42_000L)
            advanceUntilIdle()
            assertEquals(expected = "seek:1:42000", actual = bridge.commands.last())
            controller.pause()
            advanceUntilIdle()
            assertEquals(expected = "pause:1", actual = bridge.commands.last())
            bridge.emitPaused(generation = 1L, positionMs = 42_000L, durationMs = firstSong.durationMs)
            advanceUntilIdle()
            assertEquals(expected = PlaybackStatus.Paused, actual = controller.uiState.playbackStatus)
            runtime.audioEngine.stop()
            advanceUntilIdle()
            assertEquals(expected = "stop:1", actual = bridge.commands.last())
            assertEquals(expected = PlaybackStatus.Idle, actual = controller.uiState.playbackStatus)
            runtime.audioEngine.releaseAndAwait()
            advanceUntilIdle()
        }

    /** 验证 common 上一首/下一首规则会通过默认桌面运行时驱动 Apple bridge。 */
    @Test
    fun previousAndNextUseAppleBridgeThroughDefaultRuntime(): Unit =
        runTest {
            val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
            val runtime: DesktopAudioRuntime = createRuntime(bridge = bridge)
            val playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
            val coordinator: PlaybackCoordinator =
                PlaybackCoordinator(
                    playbackRepository = playbackRepository,
                    audioPlayerEngine = runtime.audioEngine,
                )
            val queueSongs: List<Song> =
                listOf(
                    testSong(id = "song-1", uri = "file:///Users/test/Music/one.m4a"),
                    testSong(id = "song-2", uri = "file:///Users/test/Music/two.m4a"),
                )
            coordinator.start(scope = backgroundScope)
            val playJob: Job =
                launch {
                    coordinator.playSong(song = queueSongs[0], queueSongs = queueSongs)
                }
            advanceUntilIdle()
            playJob.join()
            bridge.emitPrepared(generation = 1L, durationMs = queueSongs[0].durationMs)
            bridge.emitPlaying(generation = 1L, positionMs = 0L, durationMs = queueSongs[0].durationMs)
            advanceUntilIdle()
            coordinator.moveNext()
            advanceUntilIdle()
            assertEquals(
                expected = "prepare:${queueSongs[1].id}:${queueSongs[1].localUri}:2:0",
                actual = bridge.commands.last(),
            )
            bridge.emitPrepared(generation = 2L, durationMs = queueSongs[1].durationMs)
            bridge.emitPlaying(generation = 2L, positionMs = 0L, durationMs = queueSongs[1].durationMs)
            advanceUntilIdle()
            assertEquals(expected = queueSongs[1].id, actual = playbackRepository.getPlaybackState().currentSongId)
            coordinator.movePrevious()
            bridge.emitProgress(generation = 2L, positionMs = 88_000L, durationMs = queueSongs[1].durationMs)
            advanceUntilIdle()
            assertEquals(
                expected = "prepare:${queueSongs[0].id}:${queueSongs[0].localUri}:3:0",
                actual = bridge.commands.last(),
            )
            bridge.emitPrepared(generation = 3L, durationMs = queueSongs[0].durationMs)
            bridge.emitPlaying(generation = 3L, positionMs = 0L, durationMs = queueSongs[0].durationMs)
            advanceUntilIdle()
            assertEquals(expected = queueSongs[0].id, actual = playbackRepository.getPlaybackState().currentSongId)
            assertFalse(actual = playbackRepository.getPlaybackState().positionMs == 88_000L)
            runtime.audioEngine.releaseAndAwait()
            advanceUntilIdle()
        }

    /** 验证 bridge 初始化失败只映射为统一失败事件，不允许 runtime 回退到 vlcj。 */
    @Test
    fun bridgeInitializationFailureStaysInAppleEngineAndEmitsFailure(): Unit =
        runTest {
            val runtime: DesktopAudioRuntime =
                DesktopAudioRuntimeFactory.create(
                    sessionScope = this,
                    bridgeFactory = {
                        MacosAvFoundationPlaybackBridge.create(
                            libraryLoader =
                                FakeMacosAvFoundationLibraryLoader(
                                    result = MacosAvFoundationNativeLibraryLoadResult.Failed(reason = "missing dylib"),
                                ),
                            sessionFactory = FakeMacosAvFoundationSessionFactory(),
                        )
                    },
                    dispatcher = StandardTestDispatcher(testScheduler),
                )
            val events: MutableList<PlaybackEngineEvent> = mutableListOf()
            val collectJob: Job =
                launch {
                    runtime.audioEngine.events.toList(destination = events)
                }
            val expectedFailure: PlaybackError =
                PlaybackError(
                    type = PlaybackErrorType.EngineUnavailable,
                    songId = null,
                    message = "macOS AVFoundation bridge 加载失败：missing dylib",
                )
            runCurrent()
            assertIs<DesktopAppleAudioPlayerEngine>(value = runtime.audioEngine)
            assertEquals(expected = PlaybackEngineEvent.Failed(error = expectedFailure), actual = events.single())
            runtime.audioEngine.releaseAndAwait()
            collectJob.cancel()
            advanceUntilIdle()
        }

    /** 创建注入 fake bridge 的默认桌面运行时，确保测试仍走生产工厂。 */
    private fun TestScope.createRuntime(bridge: FakeApplePlaybackBridge): DesktopAudioRuntime =
        DesktopAudioRuntimeFactory.create(
            sessionScope = backgroundScope,
            bridgeFactory = { bridge },
            dispatcher = StandardTestDispatcher(testScheduler),
        )

    /** 创建使用当前桌面运行时的控制器，模拟用户从本地曲库入口播放。 */
    private fun TestScope.createController(runtime: DesktopAudioRuntime): MusicAppController =
        MusicAppController(
            localMusicScanner = FakeLocalMusicScanner(demoSongCount = 3),
            audioPlayerEngine = runtime.audioEngine,
            controllerScope = backgroundScope,
        )

    /** 加载本地曲库队列，作为桌面点击歌曲的公开入口输入。 */
    private suspend fun loadQueueSongs(controller: MusicAppController): List<Song> {
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        return controller.uiState.homeLocalSongPreview.take(n = 3)
    }

    /** 构造 coordinator 层可播放歌曲，避免上一首/下一首测试依赖 UI facade。 */
    private fun testSong(
        id: String,
        uri: String,
    ): Song =
        Song(
            id = id,
            title = "Title $id",
            artist = "Artist",
            album = "Album",
            duration = "03:00",
            coverArt = CoverArt.HeroLocalMusic,
            isLiked = false,
            lastPlayed = "",
            quality = "AAC",
            lyric = "",
            trackNumber = 1,
            durationMs = 180_000L,
            sourceKind = LocalMusicSourceKind.DesktopFolder,
            localUri = uri,
            mimeType = "audio/mp4",
        )
}
