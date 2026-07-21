package com.yanhao.kmpmusic.feature.app.library

import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanProgress
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCase
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [LocalMusicScanController] 会话取消与旧事件丢弃测试。
 */
class LocalMusicScanControllerTest {
    /**
     * 运行中再次触发扫描只取消当前会话，不启动第二个扫描，并发布取消态。
     */
    @Test
    fun runningScanSecondEntryCancelsCurrentSessionOnly(): Unit =
        runTest {
            var state: MusicAppUiState = baseState()
            val useCase = BlockingScanUseCase()
            val controller =
                LocalMusicScanController(
                    scanLocalMusicUseCase = useCase,
                    permissionSettingsOpener = PermissionSettingsOpener {},
                    controllerScope = backgroundScope,
                    nowMillis = { 10L },
                    resolveLikedSongIdsForScan = { currentState: MusicAppUiState -> currentState.likedSongIds },
                    shouldConfirmPermissionSettingsBeforeScan = { false },
                    publishStateUpdate = { reducer: (MusicAppUiState) -> MusicAppUiState -> state = reducer(state) },
                )

            val job =
                launch {
                    controller.scanLocalMusic(
                        state = state,
                        request = LocalMusicScanRequest.Refresh,
                        onLibrarySnapshot = {},
                    )
                }
            useCase.awaitStarted()

            controller.scanLocalMusic(
                state = state,
                request = LocalMusicScanRequest.Refresh,
                onLibrarySnapshot = {},
            )

            assertEquals(expected = 1, actual = useCase.callCount)
            assertTrue(actual = state.scanState is LocalMusicScanState.Cancelled)
            job.cancel()
        }

    /**
     * 取消后旧成功晚到必须被丢弃，不能同步曲库快照或覆盖取消态。
     */
    @Test
    fun lateSuccessAfterCancellationIsIgnored(): Unit =
        runTest {
            var state: MusicAppUiState = baseState()
            var syncedSnapshotCount: Int = 0
            val useCase = LateSuccessAfterCancellationUseCase()
            val controller =
                LocalMusicScanController(
                    scanLocalMusicUseCase = useCase,
                    permissionSettingsOpener = PermissionSettingsOpener {},
                    controllerScope = backgroundScope,
                    nowMillis = { 10L },
                    resolveLikedSongIdsForScan = { currentState: MusicAppUiState -> currentState.likedSongIds },
                    shouldConfirmPermissionSettingsBeforeScan = { false },
                    publishStateUpdate = { reducer: (MusicAppUiState) -> MusicAppUiState -> state = reducer(state) },
                )

            val job =
                launch {
                    controller.scanLocalMusic(
                        state = state,
                        request = LocalMusicScanRequest.Refresh,
                        onLibrarySnapshot = { syncedSnapshotCount += 1 },
                    )
                }
            useCase.awaitStarted()
            controller.scanLocalMusic(
                state = state,
                request = LocalMusicScanRequest.Refresh,
                onLibrarySnapshot = { syncedSnapshotCount += 1 },
            )
            useCase.releaseLateResult()
            job.join()

            assertEquals(expected = 0, actual = syncedSnapshotCount)
            assertTrue(actual = state.scanState is LocalMusicScanState.Cancelled)
        }

    /**
     * 取消后旧错误晚到也必须被丢弃，不能把取消态改成错误态。
     */
    @Test
    fun lateErrorAfterCancellationIsIgnored(): Unit =
        runTest {
            var state: MusicAppUiState = baseState()
            val useCase = LateErrorAfterCancellationUseCase()
            val controller =
                LocalMusicScanController(
                    scanLocalMusicUseCase = useCase,
                    permissionSettingsOpener = PermissionSettingsOpener {},
                    controllerScope = backgroundScope,
                    nowMillis = { 10L },
                    resolveLikedSongIdsForScan = { currentState: MusicAppUiState -> currentState.likedSongIds },
                    shouldConfirmPermissionSettingsBeforeScan = { false },
                    publishStateUpdate = { reducer: (MusicAppUiState) -> MusicAppUiState -> state = reducer(state) },
                )

            val job =
                launch {
                    controller.scanLocalMusic(
                        state = state,
                        request = LocalMusicScanRequest.Refresh,
                        onLibrarySnapshot = {},
                    )
                }
            useCase.awaitStarted()
            controller.scanLocalMusic(
                state = state,
                request = LocalMusicScanRequest.Refresh,
                onLibrarySnapshot = {},
            )
            useCase.releaseLateError()
            job.join()

            assertTrue(actual = state.scanState is LocalMusicScanState.Cancelled)
        }

    /**
     * 取消发布后即使旧扫描还没真正退出，再次触发也必须启动新会话，并继续丢弃旧成功。
     */
    @Test
    fun restartAfterCancellationStartsNewSessionBeforeOldSessionFinishes(): Unit =
        runTest {
            var state: MusicAppUiState = baseState()
            val syncedSongCounts: MutableList<Int> = mutableListOf()
            val useCase = RestartableLateSuccessUseCase()
            val controller =
                LocalMusicScanController(
                    scanLocalMusicUseCase = useCase,
                    permissionSettingsOpener = PermissionSettingsOpener {},
                    controllerScope = backgroundScope,
                    nowMillis = { 10L },
                    resolveLikedSongIdsForScan = { currentState: MusicAppUiState -> currentState.likedSongIds },
                    shouldConfirmPermissionSettingsBeforeScan = { false },
                    publishStateUpdate = { reducer: (MusicAppUiState) -> MusicAppUiState -> state = reducer(state) },
                )

            val firstJob =
                launch {
                    controller.scanLocalMusic(
                        state = state,
                        request = LocalMusicScanRequest.Refresh,
                        onLibrarySnapshot = { snapshot: LibrarySnapshot ->
                            syncedSongCounts += snapshot.stats.songCount
                            state = state.copy(scanState = snapshot.scanState)
                        },
                    )
                }
            useCase.awaitFirstStarted()

            controller.scanLocalMusic(
                state = state,
                request = LocalMusicScanRequest.Refresh,
                onLibrarySnapshot = {},
            )
            assertTrue(actual = state.scanState is LocalMusicScanState.Cancelled)

            val secondJob =
                launch {
                    controller.scanLocalMusic(
                        state = state,
                        request = LocalMusicScanRequest.Refresh,
                        onLibrarySnapshot = { snapshot: LibrarySnapshot ->
                            syncedSongCounts += snapshot.stats.songCount
                            state = state.copy(scanState = snapshot.scanState)
                        },
                    )
                }
            useCase.awaitSecondStarted()

            assertEquals(expected = 2, actual = useCase.callCount)
            assertTrue(actual = state.scanState is LocalMusicScanState.Scanning)

            useCase.releaseFirstLateResult()
            firstJob.join()

            assertTrue(actual = state.scanState is LocalMusicScanState.Scanning)
            assertTrue(actual = syncedSongCounts.isEmpty())

            useCase.releaseSecondResult()
            secondJob.join()

            assertEquals(expected = listOf(22), actual = syncedSongCounts)
            assertTrue(actual = state.scanState is LocalMusicScanState.Done)
        }
}

private class BlockingScanUseCase : ScanLocalMusicUseCase {
    // 记录第一次扫描已经进入用例，避免断言抢跑。
    private val started: CompletableDeferred<Unit> = CompletableDeferred()

    // 由测试释放扫描结果，模拟长时间工作流。
    private val release: CompletableDeferred<Unit> = CompletableDeferred()

    // 记录调用次数，验证不会启动第二个会话。
    var callCount: Int = 0
        private set

    /**
     * 挂起直到测试释放，模拟还没结束的扫描任务。
     */
    override suspend fun invoke(
        request: LocalMusicScanRequest,
        likedSongIds: Set<String>,
        preferences: LocalMusicDiscoveryPreferences,
    ): LibrarySnapshot {
        callCount += 1
        started.complete(value = Unit)
        release.await()
        return LibrarySnapshot(
            songs = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
            stats = LibraryStats(),
            sources = emptyList(),
            scanState =
                LocalMusicScanState.Done(
                    summary =
                        LocalMusicLastScanSummary(
                            addedCount = 0,
                            updatedCount = 0,
                            removedCount = 0,
                            problemCount = 0,
                            completedAt = 10L,
                        ),
                ),
            lastScanSummary = null,
            problems = emptyList(),
        )
    }

    /** 等待第一次扫描真正进入用例。 */
    suspend fun awaitStarted() {
        started.await()
    }
}

private class LateErrorAfterCancellationUseCase : ScanLocalMusicUseCase {
    // 记录扫描已启动，便于测试在取消后释放旧错误。
    private val started: CompletableDeferred<Unit> = CompletableDeferred()

    // 即使收到取消也要继续等待该信号，用来制造旧错误晚到。
    private val release: CompletableDeferred<Unit> = CompletableDeferred()

    /**
     * 忽略取消并晚到抛错，验证控制器会丢弃旧会话结果。
     */
    override suspend fun invoke(
        request: LocalMusicScanRequest,
        likedSongIds: Set<String>,
        preferences: LocalMusicDiscoveryPreferences,
    ): LibrarySnapshot {
        started.complete(value = Unit)
        try {
            release.await()
        } catch (cancellationException: CancellationException) {
            withContext(NonCancellable) {
                release.await()
            }
        }
        throw LocalMusicScanException(
            error =
                LocalMusicScanError(
                    type = LocalMusicScanErrorType.Unknown,
                    message = "旧扫描错误晚到",
                ),
        )
    }

    /** 等待扫描启动。 */
    suspend fun awaitStarted() {
        started.await()
    }

    /** 释放晚到错误。 */
    fun releaseLateError() {
        release.complete(value = Unit)
    }
}

private class LateSuccessAfterCancellationUseCase : ScanLocalMusicUseCase {
    // 记录扫描已启动，便于测试在取消后释放旧成功。
    private val started: CompletableDeferred<Unit> = CompletableDeferred()

    // 即使收到取消也要继续等待该信号，用来制造旧成功晚到。
    private val release: CompletableDeferred<Unit> = CompletableDeferred()

    /**
     * 忽略取消并晚到返回成功，验证控制器会丢弃旧会话结果。
     */
    override suspend fun invoke(
        request: LocalMusicScanRequest,
        likedSongIds: Set<String>,
        preferences: LocalMusicDiscoveryPreferences,
    ): LibrarySnapshot {
        started.complete(value = Unit)
        try {
            release.await()
        } catch (cancellationException: CancellationException) {
            withContext(NonCancellable) {
                release.await()
            }
        }
        return LibrarySnapshot(
            songs = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
            stats = LibraryStats(songCount = 99),
            sources = emptyList(),
            scanState =
                LocalMusicScanState.Done(
                    summary =
                        LocalMusicLastScanSummary(
                            addedCount = 99,
                            updatedCount = 0,
                            removedCount = 0,
                            problemCount = 0,
                            completedAt = 10L,
                        ),
                ),
            lastScanSummary = null,
            problems = emptyList(),
        )
    }

    /** 等待扫描启动。 */
    suspend fun awaitStarted() {
        started.await()
    }

    /** 释放晚到成功。 */
    fun releaseLateResult() {
        release.complete(value = Unit)
    }
}

private class RestartableLateSuccessUseCase : ScanLocalMusicUseCase {
    // 第一次扫描进入用例的信号，保证取消前旧会话已在运行。
    private val firstStarted: CompletableDeferred<Unit> = CompletableDeferred()

    // 第二次扫描进入用例的信号，证明取消后可以立刻重试。
    private val secondStarted: CompletableDeferred<Unit> = CompletableDeferred()

    // 第一次扫描即使取消也要等到这里才晚到返回。
    private val firstRelease: CompletableDeferred<Unit> = CompletableDeferred()

    // 第二次扫描的正常完成信号。
    private val secondRelease: CompletableDeferred<Unit> = CompletableDeferred()

    // 记录调用次数，验证新会话已经真正启动。
    var callCount: Int = 0
        private set

    /**
     * 第一次调用忽略取消后晚到成功，第二次调用代表取消后的新会话。
     */
    override suspend fun invoke(
        request: LocalMusicScanRequest,
        likedSongIds: Set<String>,
        preferences: LocalMusicDiscoveryPreferences,
    ): LibrarySnapshot {
        callCount += 1
        return if (callCount == 1) {
            firstStarted.complete(value = Unit)
            awaitFirstRelease()
            createSnapshot(songCount = 11)
        } else {
            secondStarted.complete(value = Unit)
            secondRelease.await()
            createSnapshot(songCount = 22)
        }
    }

    /** 等待第一次扫描进入用例。 */
    suspend fun awaitFirstStarted() {
        firstStarted.await()
    }

    /** 等待第二次扫描进入用例。 */
    suspend fun awaitSecondStarted() {
        secondStarted.await()
    }

    /** 释放第一次扫描的晚到成功。 */
    fun releaseFirstLateResult() {
        firstRelease.complete(value = Unit)
    }

    /** 释放第二次扫描的正常成功。 */
    fun releaseSecondResult() {
        secondRelease.complete(value = Unit)
    }

    /** 在第一次调用里忽略取消，稳定复现旧成功晚到。 */
    private suspend fun awaitFirstRelease() {
        try {
            firstRelease.await()
        } catch (cancellationException: CancellationException) {
            withContext(NonCancellable) {
                firstRelease.await()
            }
        }
    }

    /** 构造带可识别歌曲数的快照，方便断言只有新会话被同步。 */
    private fun createSnapshot(songCount: Int): LibrarySnapshot =
        LibrarySnapshot(
            songs = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
            stats = LibraryStats(songCount = songCount),
            sources = emptyList(),
            scanState =
                LocalMusicScanState.Done(
                    summary =
                        LocalMusicLastScanSummary(
                            addedCount = songCount,
                            updatedCount = 0,
                            removedCount = 0,
                            problemCount = 0,
                            completedAt = 10L,
                        ),
                ),
            lastScanSummary = null,
            problems = emptyList(),
        )
}

/**
 * 构造带运行中扫描态的最小 UI 状态，专门服务扫描会话测试。
 */
private fun baseState(): MusicAppUiState =
    MusicAppUiState(
        likedSongIds = emptySet(),
        currentSongId = null,
        playbackStatus = PlaybackStatus.Idle,
        queueSongIds = emptyList(),
        scanState =
            LocalMusicScanState.Scanning(
                progress = LocalMusicScanProgress(currentSourceName = "上一次"),
            ),
    )
