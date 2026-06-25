package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent
import kotlinx.coroutines.CompletableDeferred
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
class DesktopVlcjAudioPlayerEngineTest {
    /** 验证高频 seek 只保留当前代最新进度，避免旧命令回放污染 UI。 */
    @Test
    fun twentySeeksKeepOnlyLatestProgress(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val engine = testEngine(adapter = adapter)
        val events = mutableListOf<PlaybackEngineEvent>()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }

        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        adapter.emitPrepared(generation = 1L, durationMs = 180_000L)
        repeat(times = 20) { index ->
            engine.seekTo(positionMs = (index + 1) * 1_000L)
        }
        advanceUntilIdle()

        assertEquals(
            expected = PlaybackEngineEvent.ProgressChanged(
                positionMs = 20_000L,
                durationMs = 180_000L,
            ),
            actual = events.last(),
        )
        assertEquals(
            expected = "seek:1:20000",
            actual = adapter.commands.last { command -> command.startsWith(prefix = "seek:") },
        )
        engine.release()
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 验证旧 generation 的播放回调不会覆盖已经切走的新媒体状态。 */
    @Test
    fun staleEventsFromSkippedMediaAreIgnored(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val engine = testEngine(adapter = adapter)
        val events = mutableListOf<PlaybackEngineEvent>()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }

        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        adapter.emitPrepared(generation = 1L, durationMs = 180_000L)
        engine.skipToIndex(index = 1)
        engine.seekTo(positionMs = 90_000L)
        engine.skipToIndex(index = 2)
        engine.play()
        adapter.emitPlaying(generation = 1L, positionMs = 90_000L, durationMs = 180_000L)
        adapter.emitPrepared(generation = 3L, durationMs = 220_000L)
        adapter.emitPlaying(generation = 3L, positionMs = 0L, durationMs = 220_000L)
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

    /** 验证 ready 前的 play/pause 竞争以最后一次意图为准，最终停在暂停态。 */
    @Test
    fun playThenPauseBeforeReadyLeavesMediaPaused(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val engine = testEngine(adapter = adapter)
        val events = mutableListOf<PlaybackEngineEvent>()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }

        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 30_000L)
        engine.play()
        engine.pause()
        adapter.emitPrepared(generation = 1L, durationMs = 180_000L)
        adapter.emitPaused(generation = 1L, positionMs = 30_000L, durationMs = 180_000L)
        advanceUntilIdle()

        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Paused,
                positionMs = 30_000L,
                durationMs = 180_000L,
            ),
            actual = events.filterIsInstance<PlaybackEngineEvent.StatusChanged>().last(),
        )
        assertFalse(actual = adapter.commands.contains(element = "play:1"))
        engine.release()
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 验证 release 后的延迟 native 回调会被丢弃，避免关闭阶段的脏事件泄漏。 */
    @Test
    fun releaseIgnoresDelayedCallbacks(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val engine = testEngine(adapter = adapter)
        val events = mutableListOf<PlaybackEngineEvent>()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }

        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        adapter.emitPrepared(generation = 1L, durationMs = 180_000L)
        engine.release()
        adapter.emitFailure(
            generation = 1L,
            error = PlaybackError(
                type = PlaybackErrorType.Unknown,
                songId = "song-1",
                message = "delayed native failure",
            ),
        )
        advanceUntilIdle()

        assertFalse(actual = events.any { event -> event is PlaybackEngineEvent.Failed })
        assertEquals(expected = "release", actual = adapter.commands.last())
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 验证 pre-check 已通过的 [setQueue] 即使排在 release 后，也不会把调用方卡死。 */
    @Test
    fun releaseCompletesQueuedSetQueueAckFromPreReleaseRace(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val setQueueReachedEnqueueGate = CompletableDeferred<Unit>()
        val allowSetQueueToEnqueue = CompletableDeferred<Unit>()
        val engine = testEngine(adapter = adapter)
        engine.installTestHooks(
            testHooks = DesktopVlcjAudioPlayerEngineTestHooks(
                beforeSetQueueCommandEnqueue = {
                    setQueueReachedEnqueueGate.complete(value = Unit)
                    allowSetQueueToEnqueue.await()
                },
            ),
        )

        val setQueueJob = launch {
            engine.setQueue(
                items = mediaItems(),
                startIndex = 0,
                startPositionMs = 0L,
            )
        }

        setQueueReachedEnqueueGate.await()
        engine.release()
        allowSetQueueToEnqueue.complete(value = Unit)
        advanceUntilIdle()
        withTimeout(timeMillis = 1_000L) {
            setQueueJob.join()
        }

        assertEquals(
            expected = listOf("release"),
            actual = adapter.commands,
        )
    }

    /** 验证 release 后再次 [setQueue] 会安全返回，且不会再触发新的 prepare。 */
    @Test
    fun setQueueAfterReleaseReturnsWithoutPreparing(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val engine = testEngine(adapter = adapter)

        engine.release()
        val setQueueJob = launch {
            engine.setQueue(
                items = mediaItems(),
                startIndex = 0,
                startPositionMs = 0L,
            )
        }
        advanceUntilIdle()
        withTimeout(timeMillis = 1_000L) {
            setQueueJob.join()
        }

        assertEquals(
            expected = listOf("release"),
            actual = adapter.commands,
        )
    }

    /** 验证同一 generation 先失败后又收到旧回调时，不会把播放状态从失败中“救活”。 */
    @Test
    fun failedGenerationIgnoresLaterPreparedAndPlayingCallbacks(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val engine = testEngine(adapter = adapter)
        val events = mutableListOf<PlaybackEngineEvent>()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }
        val failure = PlaybackError(
            type = PlaybackErrorType.Unknown,
            songId = "song-1",
            message = "native failure",
        )

        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        adapter.emitFailure(generation = 1L, error = failure)
        adapter.emitPrepared(generation = 1L, durationMs = 180_000L)
        adapter.emitPlaying(generation = 1L, positionMs = 12_000L, durationMs = 180_000L)
        runCurrent()

        assertEquals(
            expected = PlaybackEngineEvent.Failed(error = failure),
            actual = events.last(),
        )
        assertEquals(
            expected = 1,
            actual = events.filterIsInstance<PlaybackEngineEvent.CurrentMediaChanged>().size,
        )
        assertFalse(
            actual = events.any { event ->
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

    /** 验证 stop 会让旧 generation 失效，避免停止后的延迟回调再次推进状态机。 */
    @Test
    fun stopIgnoresDelayedCallbacksFromOldGeneration(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val engine = testEngine(adapter = adapter)
        val events = mutableListOf<PlaybackEngineEvent>()
        val collectJob = launch {
            engine.events.toList(destination = events)
        }

        engine.setQueue(items = mediaItems(), startIndex = 0, startPositionMs = 0L)
        engine.stop()
        adapter.emitPrepared(generation = 1L, durationMs = 180_000L)
        adapter.emitPlaying(generation = 1L, positionMs = 8_000L, durationMs = 180_000L)
        runCurrent()

        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Idle,
                positionMs = 0L,
                durationMs = null,
            ),
            actual = events.last(),
        )
        assertFalse(
            actual = events.any { event ->
                event == PlaybackEngineEvent.StatusChanged(
                    status = PlaybackStatus.Playing,
                    positionMs = 8_000L,
                    durationMs = 180_000L,
                )
            },
        )
        assertEquals(expected = "stop:1", actual = adapter.commands.last())
        engine.release()
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    /** 为测试作用域构建受控调度器，确保串行命令循环可重复验证。 */
    private fun TestScope.testEngine(adapter: FakeDesktopMediaPlayerAdapter): DesktopVlcjAudioPlayerEngine {
        return DesktopVlcjAudioPlayerEngine(
            adapter = adapter,
            scope = this,
            dispatcher = StandardTestDispatcher(testScheduler),
            libVlcPluginPath = null,
            progressIntervalMs = 500L,
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
            mimeType = "audio/mpeg",
        )
    }
}
