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
    fun runningScanSecondEntryCancelsCurrentSessionOnly(): Unit = runTest {
        var state: MusicAppUiState = baseState()
        val useCase = BlockingScanUseCase()
        val controller = LocalMusicScanController(
            scanLocalMusicUseCase = useCase,
            permissionSettingsOpener = PermissionSettingsOpener {},
            controllerScope = backgroundScope,
            nowMillis = { 10L },
            resolveLikedSongIdsForScan = { currentState: MusicAppUiState -> currentState.likedSongIds },
            shouldConfirmPermissionSettingsBeforeScan = { false },
            publishStateUpdate = { reducer: (MusicAppUiState) -> MusicAppUiState -> state = reducer(state) },
        )

        val job = launch {
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
    fun lateSuccessAfterCancellationIsIgnored(): Unit = runTest {
        var state: MusicAppUiState = baseState()
        var syncedSnapshotCount: Int = 0
        val useCase = LateSuccessAfterCancellationUseCase()
        val controller = LocalMusicScanController(
            scanLocalMusicUseCase = useCase,
            permissionSettingsOpener = PermissionSettingsOpener {},
            controllerScope = backgroundScope,
            nowMillis = { 10L },
            resolveLikedSongIdsForScan = { currentState: MusicAppUiState -> currentState.likedSongIds },
            shouldConfirmPermissionSettingsBeforeScan = { false },
            publishStateUpdate = { reducer: (MusicAppUiState) -> MusicAppUiState -> state = reducer(state) },
        )

        val job = launch {
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
    fun lateErrorAfterCancellationIsIgnored(): Unit = runTest {
        var state: MusicAppUiState = baseState()
        val useCase = LateErrorAfterCancellationUseCase()
        val controller = LocalMusicScanController(
            scanLocalMusicUseCase = useCase,
            permissionSettingsOpener = PermissionSettingsOpener {},
            controllerScope = backgroundScope,
            nowMillis = { 10L },
            resolveLikedSongIdsForScan = { currentState: MusicAppUiState -> currentState.likedSongIds },
            shouldConfirmPermissionSettingsBeforeScan = { false },
            publishStateUpdate = { reducer: (MusicAppUiState) -> MusicAppUiState -> state = reducer(state) },
        )

        val job = launch {
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
            scanState = LocalMusicScanState.Done(
                summary = LocalMusicLastScanSummary(
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
            error = LocalMusicScanError(
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
            scanState = LocalMusicScanState.Done(
                summary = LocalMusicLastScanSummary(
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

/**
 * 构造带运行中扫描态的最小 UI 状态，专门服务扫描会话测试。
 */
private fun baseState(): MusicAppUiState {
    return MusicAppUiState(
        likedSongIds = emptySet(),
        currentSongId = null,
        playbackStatus = PlaybackStatus.Idle,
        queueSongIds = emptyList(),
        scanState = LocalMusicScanState.Scanning(
            progress = LocalMusicScanProgress(currentSourceName = "上一次"),
        ),
    )
}
