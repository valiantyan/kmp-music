package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopAppleAudioPlayerEngineTest {
    /** 验证 Apple bridge 准备命令只消费平台无关媒体 URI 与当前 generation。 */
    @Test
    fun setQueuePreparesBridgeWithAudioSourceUriAndGeneration(): Unit = runTest {
        val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
        val engine: DesktopAppleAudioPlayerEngine = testEngine(bridge = bridge)
        engine.setQueue(
            items = listOf(
                media(
                    songId = "song-content-uri",
                    uri = "content://media/external/audio/media/42",
                    durationMs = 180_000L,
                ),
            ),
            startIndex = 0,
            startPositionMs = 4_000L,
        )
        advanceUntilIdle()
        assertEquals(
            expected = "prepare:song-content-uri:content://media/external/audio/media/42:1:4000",
            actual = bridge.commands.single(),
        )
        engine.release()
        advanceUntilIdle()
    }

    /** 验证 prepared、buffering、playing 和 progress 都被规整成共享播放事实。 */
    @Test
    fun bridgeEventsMapToPlaybackFacts(): Unit = runTest {
        val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
        val engine: DesktopAppleAudioPlayerEngine = testEngine(bridge = bridge)
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        engine.play()
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        bridge.emitBuffering(generation = 1L, positionMs = 0L, durationMs = 180_000L)
        bridge.emitPlaying(generation = 1L, positionMs = 8_000L, durationMs = 180_000L)
        bridge.emitProgress(generation = 1L, positionMs = 9_000L, durationMs = 180_000L)
        runCurrent()
        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Buffering,
                positionMs = 0L,
                durationMs = 180_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.StatusChanged>()[1],
        )
        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Playing,
                positionMs = 8_000L,
                durationMs = 180_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.StatusChanged>().last(),
        )
        assertEquals(
            expected = PlaybackEngineEvent.ProgressChanged(
                positionMs = 9_000L,
                durationMs = 180_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.ProgressChanged>().last(),
        )
        engine.release()
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 验证准备失败会让当前 generation 失效，后续同代回调不能救活媒体。 */
    @Test
    fun prepareFailureInvalidatesSameGenerationCallbacks(): Unit = runTest {
        val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
        val engine: DesktopAppleAudioPlayerEngine = testEngine(bridge = bridge)
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        val failure: PlaybackError = PlaybackError(
            type = PlaybackErrorType.UnsupportedFormat,
            songId = "song-1",
            message = "AVFoundation 无法播放该媒体。",
        )
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        bridge.emitFailure(generation = 1L, error = failure)
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        bridge.emitPlaying(generation = 1L, positionMs = 12_000L, durationMs = 180_000L)
        runCurrent()
        assertEquals(expected = PlaybackEngineEvent.Failed(error = failure), actual = events.last())
        assertFalse(
            actual = events.any { event: PlaybackEngineEvent ->
                event == PlaybackEngineEvent.StatusChanged(
                    status = PlaybackStatus.Playing,
                    positionMs = 12_000L,
                    durationMs = 180_000L,
                )
            },
        )
        engine.release()
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 验证 seek/skip 竞态下旧 generation 进度不会污染当前媒体。 */
    @Test
    fun seekSkipRaceIgnoresStaleGenerationCallbacks(): Unit = runTest {
        val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
        val engine: DesktopAppleAudioPlayerEngine = testEngine(bridge = bridge)
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        engine.skipToIndex(index = 1)
        engine.seekTo(positionMs = 90_000L)
        engine.skipToIndex(index = 2)
        engine.play()
        bridge.emitProgress(generation = 1L, positionMs = 90_000L, durationMs = 180_000L)
        bridge.emitPrepared(generation = 3L, durationMs = 220_000L)
        bridge.emitPlaying(generation = 3L, positionMs = 0L, durationMs = 220_000L)
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
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Playing,
                positionMs = 0L,
                durationMs = 220_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.StatusChanged>().last(),
        )
        engine.release()
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 验证已 prepared 的 seek 会下发给当前 generation，并立即回传共享进度事实。 */
    @Test
    fun seekAfterPreparedSendsBridgeCommandAndProgress(): Unit = runTest {
        val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
        val engine: DesktopAppleAudioPlayerEngine = testEngine(bridge = bridge)
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        engine.seekTo(positionMs = 42_000L)
        runCurrent()
        assertEquals(
            expected = "seek:1:42000",
            actual = bridge.commands.last(),
        )
        assertEquals(
            expected = PlaybackEngineEvent.ProgressChanged(
                positionMs = 42_000L,
                durationMs = 180_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.ProgressChanged>().last(),
        )
        engine.release()
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 验证 prepare 同步 ack 失败会发出共享失败事件，并屏蔽同代后续回调。 */
    @Test
    fun prepareAckFailureInvalidatesGeneration(): Unit = runTest {
        val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
        val engine: DesktopAppleAudioPlayerEngine = testEngine(bridge = bridge)
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        val failure: PlaybackError = PlaybackError(
            type = PlaybackErrorType.EngineUnavailable,
            songId = "song-1",
            message = "Apple bridge prepare 命令失败。",
        )
        bridge.failNextPrepare(error = failure)
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        bridge.emitPlaying(generation = 1L, positionMs = 10_000L, durationMs = 180_000L)
        runCurrent()
        assertEquals(expected = PlaybackEngineEvent.Failed(error = failure), actual = events.last())
        assertFalse(
            actual = events.any { event: PlaybackEngineEvent ->
                event == PlaybackEngineEvent.StatusChanged(
                    status = PlaybackStatus.Playing,
                    positionMs = 10_000L,
                    durationMs = 180_000L,
                )
            },
        )
        engine.release()
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 验证 seek ack 超时会统一进入失败事件，而不是让调用方或状态机挂起。 */
    @Test
    fun seekAckTimeoutEmitsFailedEvent(): Unit = runTest {
        val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
        val engine: DesktopAppleAudioPlayerEngine = testEngine(bridge = bridge)
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        val failure: PlaybackError = PlaybackError(
            type = PlaybackErrorType.Unknown,
            songId = "song-1",
            message = "Apple bridge seek 命令超时。",
        )
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        bridge.timeoutNextSeek(error = failure)
        engine.seekTo(positionMs = 24_000L)
        runCurrent()
        assertEquals(
            expected = "seek:1:24000",
            actual = bridge.commands.last(),
        )
        assertEquals(expected = PlaybackEngineEvent.Failed(error = failure), actual = events.last())
        engine.release()
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 验证自然结束只回传 [PlaybackEngineEvent.Ended]，队列推进仍交给 common 协调器。 */
    @Test
    fun endedCallbackOnlyEmitsEndedEvent(): Unit = runTest {
        val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
        val engine: DesktopAppleAudioPlayerEngine = testEngine(bridge = bridge)
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        bridge.emitEnded(generation = 1L)
        runCurrent()
        assertEquals(expected = PlaybackEngineEvent.Ended, actual = events.last())
        assertEquals(
            expected = 1,
            actual = bridge.commands.count { command: String -> command.startsWith(prefix = "prepare:") },
        )
        engine.release()
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 验证 stop 只停止当前 generation，并回传 idle 状态给 common 协调器。 */
    @Test
    fun stopSendsBridgeCommandAndEmitsIdle(): Unit = runTest {
        val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
        val engine: DesktopAppleAudioPlayerEngine = testEngine(bridge = bridge)
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob: Job = launch {
            engine.events.toList(destination = events)
        }
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        engine.stop()
        runCurrent()
        assertEquals(expected = "stop:1", actual = bridge.commands.last())
        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Idle,
                positionMs = 0L,
                durationMs = null,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.StatusChanged>().last(),
        )
        engine.release()
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 验证 release 后回调被丢弃，且后续 [setQueue] 不会挂起或重新 prepare。 */
    @Test
    fun releaseIgnoresDelayedCallbacksAndSetQueueReturns(): Unit = runTest {
        val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
        val engine: DesktopAppleAudioPlayerEngine = testEngine(bridge = bridge)
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        bridge.emitPrepared(generation = 1L, durationMs = 180_000L)
        engine.release()
        bridge.emitFailure(
            generation = 1L,
            error = PlaybackError(
                type = PlaybackErrorType.Unknown,
                songId = "song-1",
                message = "release 后的延迟 native failure",
            ),
        )
        advanceUntilIdle()
        val setQueueJob = launch {
            engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        }
        advanceUntilIdle()
        withTimeout(timeMillis = 1_000L) {
            setQueueJob.join()
        }
        assertFalse(actual = events.any { event: PlaybackEngineEvent -> event is PlaybackEngineEvent.Failed })
        assertEquals(expected = "release", actual = bridge.commands.last())
        assertEquals(
            expected = 1,
            actual = bridge.commands.count { command: String -> command.startsWith(prefix = "prepare:") },
        )
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 验证 bridge 初始化失败被归一化为共享失败事件，不绕过协调器策略。 */
    @Test
    fun initializationFailureMapsToFailedEvent(): Unit = runTest {
        val bridge: FakeApplePlaybackBridge = FakeApplePlaybackBridge()
        val engine: DesktopAppleAudioPlayerEngine = testEngine(bridge = bridge)
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        val failure: PlaybackError = PlaybackError(
            type = PlaybackErrorType.EngineUnavailable,
            songId = null,
            message = "Apple native bridge 初始化失败。",
        )
        bridge.emitInitializationFailed(error = failure)
        runCurrent()
        assertEquals(expected = PlaybackEngineEvent.Failed(error = failure), actual = events.single())
        engine.release()
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 为测试作用域构建受控调度器，确保串行命令循环可重复验证。 */
    private fun TestScope.testEngine(bridge: FakeApplePlaybackBridge): DesktopAppleAudioPlayerEngine {
        return DesktopAppleAudioPlayerEngine(
            bridge = bridge,
            scope = this,
            dispatcher = StandardTestDispatcher(testScheduler),
        )
    }

    /** 提供跨用例共用的三首测试媒体，覆盖切歌代际变化。 */
    private fun mediaItems(): List<PlayableMedia> {
        return listOf(
            media(songId = "song-1", uri = "file:///Users/test/Music/one.mp3", durationMs = 180_000L),
            media(songId = "song-2", uri = "file:///Users/test/Music/two.flac", durationMs = 200_000L),
            media(songId = "song-3", uri = "file:///Users/test/Music/three.aac", durationMs = 220_000L),
        )
    }

    /** 构造最小可播放媒体对象，减少各用例样板。 */
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
