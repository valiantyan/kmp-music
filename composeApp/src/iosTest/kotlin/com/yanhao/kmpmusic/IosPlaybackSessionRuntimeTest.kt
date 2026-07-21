package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.feature.app.MusicAppController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IosPlaybackSessionRuntimeTest {
    /** 验证 iOS 会话复用同一个 controller，避免 Compose 重组重建真实播放器。 */
    @Test
    fun controllerRemainsStableAcrossRepeatedAccess(): Unit =
        runTest {
            val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            val controller: MusicAppController = MusicAppController(controllerScope = sessionScope)
            val runtime: IosPlaybackSessionRuntime =
                runtimeWith(
                    controller = controller,
                    sessionScope = sessionScope,
                )
            assertSame(expected = controller, actual = runtime.controller)
            assertSame(expected = runtime.controller, actual = runtime.controller)
            runtime.close()
            advanceUntilIdle()
        }

    /** 验证冷启动恢复只请求一次，避免 UI 多次接入覆盖后台播放状态。 */
    @Test
    fun ensurePlaybackSnapshotRestoreRequestedRunsOnlyOnce(): Unit =
        runTest {
            val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            val restoreStarted: CompletableDeferred<Unit> = CompletableDeferred()
            var restoreRequests = 0
            val runtime: IosPlaybackSessionRuntime =
                runtimeWith(
                    sessionScope = sessionScope,
                    restorePlaybackSnapshot = {
                        restoreRequests += 1
                        restoreStarted.complete(value = Unit)
                    },
                )
            runtime.ensurePlaybackSnapshotRestoreRequested()
            runtime.ensurePlaybackSnapshotRestoreRequested()
            advanceUntilIdle()
            withTimeout(timeMillis = 1_000L) {
                restoreStarted.await()
            }
            assertEquals(expected = 1, actual = restoreRequests)
            runtime.close()
            advanceUntilIdle()
        }

    /** 验证 close 顺序释放播放器、audio session，并取消长生命周期 scope。 */
    @Test
    fun closeReleasesEngineAudioSessionAndCancelsScope(): Unit =
        runTest {
            val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            val order: MutableList<String> = mutableListOf()
            val sessionJob: Job = checkNotNull(sessionScope.coroutineContext[Job])
            val runtime: IosPlaybackSessionRuntime =
                runtimeWith(
                    sessionScope = sessionScope,
                    releaseAudioEngine = {
                        order += "release-engine"
                    },
                    releaseAudioSession = {
                        order += "release-audio-session:${sessionScope.coroutineContext[Job]?.isCancelled == true}"
                    },
                )
            runtime.close()
            advanceUntilIdle()
            assertEquals(
                expected = listOf("release-engine", "release-audio-session:false"),
                actual = order,
            )
            assertTrue(actual = sessionJob.isCancelled)
        }

    /** 验证 close 会等待释放挂起流程结束，不提前返回给宿主。 */
    @Test
    fun closeWaitsForAudioEngineRelease(): Unit =
        runTest {
            val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            val releaseStarted: CompletableDeferred<Unit> = CompletableDeferred()
            val allowReleaseFinish: CompletableDeferred<Unit> = CompletableDeferred()
            var didCloseReturn = false
            val runtime: IosPlaybackSessionRuntime =
                runtimeWith(
                    sessionScope = sessionScope,
                    releaseAudioEngine = {
                        releaseStarted.complete(value = Unit)
                        allowReleaseFinish.await()
                    },
                )
            val closeJob =
                launch(context = Dispatchers.Default) {
                    runtime.close()
                    didCloseReturn = true
                }
            releaseStarted.await()
            delay(timeMillis = 10L)
            assertFalse(actual = didCloseReturn)
            allowReleaseFinish.complete(value = Unit)
            closeJob.join()
            assertTrue(actual = didCloseReturn)
        }

    /** 构造只关注生命周期协作的 iOS runtime。 */
    private fun runtimeWith(
        sessionScope: CoroutineScope,
        controller: MusicAppController = MusicAppController(controllerScope = sessionScope),
        restorePlaybackSnapshot: suspend () -> Unit = {},
        releaseAudioEngine: suspend () -> Unit = {},
        releaseAudioSession: () -> Unit = {},
    ): IosPlaybackSessionRuntime =
        IosPlaybackSessionRuntime(
            controller = controller,
            sessionScope = sessionScope,
            restorePlaybackSnapshot = restorePlaybackSnapshot,
            releaseAudioEngine = releaseAudioEngine,
            releaseAudioSession = releaseAudioSession,
        )
}
