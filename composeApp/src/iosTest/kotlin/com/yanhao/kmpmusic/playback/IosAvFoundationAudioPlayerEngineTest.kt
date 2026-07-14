package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class IosAvFoundationAudioPlayerEngineTest {
    /** 验证 iOS 引擎只准备当前媒体，不把业务队列交给系统队列。 */
    @Test
    fun setQueuePreparesOnlyCurrentItemAndEmitsCurrentMedia(): Unit = runTest {
        val order: MutableList<String> = mutableListOf()
        val audioSession: RecordingIosAudioSessionController = RecordingIosAudioSessionController(order = order)
        val bridge: FakeIosPlaybackBridge = FakeIosPlaybackBridge(order = order)
        val engine: IosAvFoundationAudioPlayerEngine = testEngine(
            bridge = bridge,
            audioSessionController = audioSession,
        )
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        engine.setQueue(items = mediaItems(), startIndex = 1, startPositionMs = 5_000L)
        runCurrent()
        assertEquals(expected = 1, actual = bridge.prepareRequests.size)
        assertEquals(expected = "song-2", actual = bridge.prepareRequests.single().songId)
        assertEquals(expected = "file:///app/Documents/two.m4a", actual = bridge.prepareRequests.single().mediaUri)
        assertEquals(expected = 1L, actual = bridge.prepareRequests.single().generation)
        assertEquals(expected = 5_000L, actual = bridge.prepareRequests.single().startPositionMs)
        assertEquals(
            expected = PlaybackEngineEvent.CurrentMediaChanged(
                songId = "song-2",
                index = 1,
                durationMs = 200_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.CurrentMediaChanged>().single(),
        )
        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Loading,
                positionMs = 5_000L,
                durationMs = 200_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.StatusChanged>().single(),
        )
        collectJob.cancel()
        engine.release()
        advanceUntilIdle()
    }

    /** 验证开始播放前一定先配置并激活 playback audio session。 */
    @Test
    fun playConfiguresAudioSessionBeforeBridgePlay(): Unit = runTest {
        val order: MutableList<String> = mutableListOf()
        val audioSession: RecordingIosAudioSessionController = RecordingIosAudioSessionController(order = order)
        val bridge: FakeIosPlaybackBridge = FakeIosPlaybackBridge(order = order)
        val engine: IosAvFoundationAudioPlayerEngine = testEngine(
            bridge = bridge,
            audioSessionController = audioSession,
        )
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        engine.play()
        bridge.emitPlaying(generation = 1L, positionMs = 8_000L, durationMs = 180_000L)
        runCurrent()
        assertEquals(
            expected = listOf(
                "prepare:song-1:1:0",
                "audio-session:configure-playback",
                "play:1",
            ),
            actual = order,
        )
        assertTrue(actual = audioSession.isConfiguredForPlayback)
        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Playing,
                positionMs = 8_000L,
                durationMs = 180_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.StatusChanged>().last(),
        )
        collectJob.cancel()
        engine.release()
        advanceUntilIdle()
    }

    /** 验证 seek/skip 竞态下旧 generation 的进度不会污染当前媒体。 */
    @Test
    fun seekSkipRaceIgnoresStaleGenerationProgress(): Unit = runTest {
        val order: MutableList<String> = mutableListOf()
        val bridge: FakeIosPlaybackBridge = FakeIosPlaybackBridge(order = order)
        val engine: IosAvFoundationAudioPlayerEngine = testEngine(
            bridge = bridge,
            audioSessionController = RecordingIosAudioSessionController(order = order),
        )
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        engine.skipToIndex(index = 2)
        engine.seekTo(positionMs = 90_000L)
        bridge.emitProgress(generation = 1L, positionMs = 90_000L, durationMs = 180_000L)
        bridge.emitPrepared(generation = 2L, durationMs = 220_000L)
        bridge.emitProgress(generation = 2L, positionMs = 91_000L, durationMs = 220_000L)
        runCurrent()
        assertFalse(
            actual = events.contains(
                element = PlaybackEngineEvent.ProgressChanged(
                    positionMs = 90_000L,
                    durationMs = 180_000L,
                ),
            ),
        )
        assertEquals(
            expected = PlaybackEngineEvent.CurrentMediaChanged(
                songId = "song-3",
                index = 2,
                durationMs = 220_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.CurrentMediaChanged>().last(),
        )
        assertEquals(
            expected = PlaybackEngineEvent.ProgressChanged(
                positionMs = 91_000L,
                durationMs = 220_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.ProgressChanged>().last(),
        )
        assertTrue(actual = order.contains(element = "seek:2:90000"))
        collectJob.cancel()
        engine.release()
        advanceUntilIdle()
    }

    /** 验证中断、可恢复提示和输出断开不会让 UI 长期停在播放中。 */
    @Test
    fun audioSessionEventsPauseResumeOrFailCurrentMedia(): Unit = runTest {
        val order: MutableList<String> = mutableListOf()
        val audioSession: RecordingIosAudioSessionController = RecordingIosAudioSessionController(order = order)
        val bridge: FakeIosPlaybackBridge = FakeIosPlaybackBridge(order = order)
        val engine: IosAvFoundationAudioPlayerEngine = testEngine(
            bridge = bridge,
            audioSessionController = audioSession,
        )
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        engine.play()
        bridge.emitInterruptionBegan(generation = 1L, positionMs = 11_000L, durationMs = 180_000L)
        bridge.emitInterruptionEnded(generation = 1L, shouldResume = true)
        bridge.emitOutputDisconnected(generation = 1L, positionMs = 12_000L, durationMs = 180_000L)
        runCurrent()
        assertTrue(actual = order.contains(element = "play:1"))
        assertEquals(expected = 2, actual = order.count { command: String -> command == "play:1" })
        assertTrue(
            actual = events.contains(
                element = PlaybackEngineEvent.StatusChanged(
                    status = PlaybackStatus.Paused,
                    positionMs = 11_000L,
                    durationMs = 180_000L,
                ),
            ),
        )
        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Paused,
                positionMs = 12_000L,
                durationMs = 180_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.StatusChanged>().last(),
        )
        collectJob.cancel()
        engine.release()
        advanceUntilIdle()
    }

    /** 验证自然结束、缓冲和失败都通过统一事件回流 common 层。 */
    @Test
    fun nativePlaybackFactsFlowBackAsCommonEvents(): Unit = runTest {
        val order: MutableList<String> = mutableListOf()
        val bridge: FakeIosPlaybackBridge = FakeIosPlaybackBridge(order = order)
        val engine: IosAvFoundationAudioPlayerEngine = testEngine(
            bridge = bridge,
            audioSessionController = RecordingIosAudioSessionController(order = order),
        )
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        val playbackError = PlaybackError(
            type = PlaybackErrorType.Unknown,
            songId = "song-1",
            message = "AVFoundation fake failure",
        )
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        bridge.emitBuffering(generation = 1L, positionMs = 1_500L, durationMs = 180_000L)
        bridge.emitProgress(generation = 1L, positionMs = 2_000L, durationMs = 180_000L)
        bridge.emitEnded(generation = 1L)
        bridge.emitFailure(generation = 1L, error = playbackError)
        runCurrent()
        assertTrue(
            actual = events.contains(
                element = PlaybackEngineEvent.StatusChanged(
                    status = PlaybackStatus.Buffering,
                    positionMs = 1_500L,
                    durationMs = 180_000L,
                ),
            ),
        )
        assertTrue(
            actual = events.contains(
                element = PlaybackEngineEvent.ProgressChanged(
                    positionMs = 2_000L,
                    durationMs = 180_000L,
                ),
            ),
        )
        assertTrue(actual = events.contains(element = PlaybackEngineEvent.Ended))
        assertEquals(
            expected = PlaybackEngineEvent.Failed(error = playbackError),
            actual = events.filterIsInstance<PlaybackEngineEvent.Failed>().single(),
        )
        collectJob.cancel()
        engine.release()
        advanceUntilIdle()
    }

    /** 验证 release 后移除观察路径，延迟 native 回调不会再污染共享事件。 */
    @Test
    fun releaseIgnoresDelayedCallbacksAndStopsAcceptingCommands(): Unit = runTest {
        val order: MutableList<String> = mutableListOf()
        val bridge: FakeIosPlaybackBridge = FakeIosPlaybackBridge(order = order)
        val engine: IosAvFoundationAudioPlayerEngine = testEngine(
            bridge = bridge,
            audioSessionController = RecordingIosAudioSessionController(order = order),
        )
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        engine.release()
        bridge.emitFailure(
            generation = 1L,
            error = PlaybackError(
                type = PlaybackErrorType.Unknown,
                songId = "song-1",
                message = "release 后的延迟回调",
            ),
        )
        engine.setQueue(items = mediaItems(), startIndex = 1, startPositionMs = 0L)
        runCurrent()
        assertEquals(expected = "release", actual = order.last())
        assertEquals(expected = 1, actual = bridge.prepareRequests.size)
        assertFalse(actual = events.any { event: PlaybackEngineEvent -> event is PlaybackEngineEvent.Failed })
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 为测试构造 iOS 引擎，并把 native 回调固定到可控调度器。 */
    private fun TestScope.testEngine(
        bridge: FakeIosPlaybackBridge,
        audioSessionController: RecordingIosAudioSessionController,
    ): IosAvFoundationAudioPlayerEngine {
        return IosAvFoundationAudioPlayerEngine(
            bridge = bridge,
            audioSessionController = audioSessionController,
            scope = this,
            dispatcher = StandardTestDispatcher(testScheduler),
        )
    }

    /** 提供三首沙盒内音频，覆盖切歌 generation 与不同格式。 */
    private fun mediaItems(): List<PlayableMedia> {
        return listOf(
            media(songId = "song-1", uri = "file:///app/Documents/one.mp3", durationMs = 180_000L),
            media(songId = "song-2", uri = "file:///app/Documents/two.m4a", durationMs = 200_000L),
            media(songId = "song-3", uri = "file:///app/Documents/three.wav", durationMs = 220_000L),
        )
    }

    /** 构造最小 [PlayableMedia]，让测试只关注播放契约。 */
    private fun media(songId: String, uri: String, durationMs: Long): PlayableMedia {
        return PlayableMedia(
            songId = songId,
            title = "Title $songId",
            artist = "Artist",
            album = "Album",
            durationMs = durationMs,
            localUri = uri,
            coverArt = CoverArt.HeroLocalMusic,
            coverImageUri = null,
            mimeType = "audio/mpeg",
        )
    }
}
