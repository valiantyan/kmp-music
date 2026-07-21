package com.yanhao.kmpmusic.feature.app.library

import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanProgress
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCase
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

// 本地扫描日志统一前缀，方便从平台日志中过滤扫描生命周期。
private const val LOCAL_MUSIC_SCAN_LOG_PREFIX = "[LocalMusicScan]"

/**
 * 本地音乐扫描工作流，把一次扫描视为独立会话并丢弃旧会话晚到事件。
 */
internal class LocalMusicScanController(
    private val scanLocalMusicUseCase: ScanLocalMusicUseCase,
    private val permissionSettingsOpener: PermissionSettingsOpener,
    private val controllerScope: CoroutineScope,
    private val nowMillis: () -> Long,
    private val resolveLikedSongIdsForScan: (MusicAppUiState) -> Set<String>,
    private val shouldConfirmPermissionSettingsBeforeScan: (MusicAppUiState) -> Boolean,
    private val publishStateUpdate: ((MusicAppUiState) -> MusicAppUiState) -> Unit,
) {
    // 单调递增的会话编号，用于区分新旧扫描结果。
    private var nextSessionId: Long = 0L

    // 当前运行中的扫描会话编号；为空表示没有运行中的扫描。
    private var runningSessionId: Long? = null

    // 已取消会话集合，避免旧扫描晚到后覆盖取消态。
    private val cancelledSessionIds: MutableSet<Long> = mutableSetOf()

    // 当前扫描协程句柄，二次点击时通过它发出取消信号。
    private var currentLocalMusicScanJob: Job? = null

    /** 使用控制器生命周期启动扫描，避免 UI 层协程取消后把扫描卡在运行态。 */
    fun requestLocalMusicScan(
        state: MusicAppUiState,
        request: LocalMusicScanRequest,
        onLibrarySnapshot: (LibrarySnapshot) -> Unit,
    ) {
        controllerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            scanLocalMusic(
                state = state,
                request = request,
                onLibrarySnapshot = onLibrarySnapshot,
            )
        }
    }

    /** 扫描本地音乐并在成功时把曲库快照交还给门面同步。 */
    suspend fun scanLocalMusic(
        state: MusicAppUiState,
        request: LocalMusicScanRequest,
        onLibrarySnapshot: (LibrarySnapshot) -> Unit,
    ) {
        if (runningSessionId != null) {
            cancelRunningLocalMusicScan()
            return
        }
        if (shouldConfirmPermissionSettingsBeforeScan(state)) {
            openPermissionSettingsDialog()
            return
        }
        val sessionId: Long = createRunningSession()
        val previousSummary: LocalMusicLastScanSummary? = findLastScanSummary(scanState = state.scanState)
        logLocalMusicScan(message = "开始扫描: request=$request, sessionId=$sessionId, previousState=${state.scanState}")
        publishScanningState(previousSummary = previousSummary)
        try {
            val snapshot: LibrarySnapshot =
                scanLocalMusicUseCase(
                    request = request,
                    likedSongIds = resolveLikedSongIdsForScan(state),
                    preferences = state.localMusicDiscoveryPreferences,
                )
            logLocalMusicScan(
                message = "扫描用例完成: request=$request, sessionId=$sessionId, songCount=${snapshot.stats.songCount}, scanState=${snapshot.scanState}",
            )
            if (!shouldAcceptResult(sessionId = sessionId)) {
                logLocalMusicScan(message = "扫描结果已忽略: request=$request, sessionId=$sessionId")
                return
            }
            onLibrarySnapshot(snapshot)
        } catch (cancellationException: CancellationException) {
            logLocalMusicScan(
                message = "扫描协程被取消: request=$request, sessionId=$sessionId, reason=${cancellationException.message.orEmpty()}",
            )
            publishCancelledLocalMusicScanIfRunning(sessionId = sessionId)
            throw cancellationException
        } catch (scanException: LocalMusicScanException) {
            if (!shouldAcceptResult(sessionId = sessionId)) {
                logLocalMusicScan(message = "扫描错误已忽略: request=$request, sessionId=$sessionId")
                return
            }
            if (scanException.error.type == LocalMusicScanErrorType.UserCancelled) {
                logLocalMusicScan(message = "扫描被平台报告为用户取消: request=$request, sessionId=$sessionId")
                publishCancelledLocalMusicScan(sessionId = sessionId)
                return
            }
            logLocalMusicScan(
                message = "扫描失败: request=$request, sessionId=$sessionId, errorType=${scanException.error.type}, message=${scanException.error.message}",
            )
            publishErrorState(
                error = scanException,
                previousSummary = previousSummary,
            )
        } finally {
            finishSession(sessionId = sessionId)
        }
    }

    /** 打开权限设置确认框，由用户决定是否跳去系统设置。 */
    fun openPermissionSettingsDialog() {
        publishStateUpdate { state: MusicAppUiState ->
            state.copy(
                isPermissionSettingsDialogOpen = true,
                isQueueOpen = false,
                moreSongId = null,
            )
        }
    }

    /** 关闭权限设置确认框，保留当前错误态供稍后重试。 */
    fun closePermissionSettingsDialog() {
        publishStateUpdate { state: MusicAppUiState ->
            state.copy(isPermissionSettingsDialogOpen = false)
        }
    }

    /** 用户确认后再打开系统权限设置页，避免永久拒绝时突然跳出 App。 */
    fun confirmPermissionSettings() {
        publishStateUpdate { state: MusicAppUiState ->
            state.copy(
                isPermissionSettingsDialogOpen = false,
                scanState = LocalMusicScanState.WaitingForPermission,
            )
        }
        permissionSettingsOpener.openPermissionSettings()
    }

    /** 创建新的运行会话，并把当前协程句柄绑定到该会话。 */
    private suspend fun createRunningSession(): Long {
        val sessionId: Long = nextSessionId + 1L
        nextSessionId = sessionId
        runningSessionId = sessionId
        cancelledSessionIds.remove(element = sessionId)
        currentLocalMusicScanJob = currentCoroutineContext()[Job]
        return sessionId
    }

    /** 发布运行中状态，并保留上一轮结果摘要。 */
    private fun publishScanningState(previousSummary: LocalMusicLastScanSummary?) {
        publishStateUpdate { state: MusicAppUiState ->
            state.copy(
                scanState =
                    LocalMusicScanState.Scanning(
                        progress = LocalMusicScanProgress(currentSourceName = "本地音乐"),
                        previousSummary = previousSummary,
                    ),
                isQueueOpen = false,
                moreSongId = null,
            )
        }
    }

    /** 运行中再次触发时只取消当前会话，不启动第二个扫描。 */
    private fun cancelRunningLocalMusicScan() {
        val sessionId: Long = runningSessionId ?: return
        val runningJob: Job? = currentLocalMusicScanJob
        cancelledSessionIds += sessionId
        logLocalMusicScan(message = "用户请求取消当前扫描: sessionId=$sessionId")
        runningSessionId = null
        currentLocalMusicScanJob = null
        runningJob?.cancel(
            cause = CancellationException("用户取消了本地音乐扫描"),
        )
        publishCancelledState()
    }

    /** 外部协程取消时，只在 UI 仍显示运行中时发布取消态。 */
    private fun publishCancelledLocalMusicScanIfRunning(sessionId: Long) {
        if (!isCurrentSession(sessionId = sessionId)) {
            return
        }
        publishStateUpdate { state: MusicAppUiState ->
            val scanState: LocalMusicScanState = state.scanState
            if (scanState !is LocalMusicScanState.Scanning && scanState !is LocalMusicScanState.Importing) {
                state
            } else {
                buildCancelledState(state = state)
            }
        }
    }

    /** 生成取消结果态，保证 UI 稳定展示“已取消”和曲库保留说明。 */
    private fun publishCancelledLocalMusicScan(sessionId: Long) {
        if (!isCurrentSession(sessionId = sessionId)) {
            return
        }
        publishCancelledState()
    }

    /** 发布统一取消态，让主动取消后可以马上开启下一次扫描。 */
    private fun publishCancelledState() {
        publishStateUpdate { state: MusicAppUiState ->
            buildCancelledState(state = state)
        }
    }

    /** 发布非取消错误，保留上一轮摘要给扫描页继续展示。 */
    private fun publishErrorState(
        error: LocalMusicScanException,
        previousSummary: LocalMusicLastScanSummary?,
    ) {
        publishStateUpdate { state: MusicAppUiState ->
            state.copy(
                scanState =
                    LocalMusicScanState.Error(
                        error = error.error,
                        summary = previousSummary,
                    ),
                isQueueOpen = false,
                moreSongId = null,
            )
        }
    }

    /** 构造统一取消态，避免不同取消路径生成不一致的结果。 */
    private fun buildCancelledState(state: MusicAppUiState): MusicAppUiState =
        state.copy(
            scanState =
                LocalMusicScanState.Cancelled(
                    summary =
                        LocalMusicLastScanSummary(
                            addedCount = 0,
                            updatedCount = 0,
                            removedCount = 0,
                            problemCount = 0,
                            completedAt = scanResultTimeMillis(),
                        ),
                ),
            isQueueOpen = false,
            moreSongId = null,
            isPermissionSettingsDialogOpen = false,
        )

    /** 只接受当前运行且未被标记取消的会话结果。 */
    private fun shouldAcceptResult(sessionId: Long): Boolean =
        isCurrentSession(sessionId = sessionId) &&
            !cancelledSessionIds.contains(element = sessionId)

    /** 判断给定会话是否仍是当前运行会话。 */
    private fun isCurrentSession(sessionId: Long): Boolean = runningSessionId == sessionId

    /** 收尾当前会话，并释放取消标记避免集合无界增长。 */
    private fun finishSession(sessionId: Long) {
        cancelledSessionIds.remove(element = sessionId)
        if (!isCurrentSession(sessionId = sessionId)) {
            return
        }
        runningSessionId = null
        currentLocalMusicScanJob = null
        logLocalMusicScan(message = "扫描流程结束: sessionId=$sessionId")
    }

    /** 测试环境可能不注入时钟，取消结果仍需要一个可展示的结果时间。 */
    private fun scanResultTimeMillis(): Long {
        val currentTimeMillis: Long = nowMillis()
        if (currentTimeMillis > 0L) {
            return currentTimeMillis
        }
        return 1L
    }

    /** 进入运行中状态前保留上一轮结果，避免扫描页把“上次扫描”回退为空。 */
    private fun findLastScanSummary(scanState: LocalMusicScanState): LocalMusicLastScanSummary? =
        when (scanState) {
            LocalMusicScanState.Idle,
            LocalMusicScanState.WaitingForPermission,
            -> null

            is LocalMusicScanState.Importing -> scanState.previousSummary

            is LocalMusicScanState.Scanning -> scanState.previousSummary

            is LocalMusicScanState.Done -> scanState.summary

            is LocalMusicScanState.Cancelled -> scanState.summary

            is LocalMusicScanState.Error -> scanState.summary
        }

    /** commonMain 先用标准输出保留轻量扫描诊断。 */
    private fun logLocalMusicScan(message: String) {
        println("$LOCAL_MUSIC_SCAN_LOG_PREFIX $message")
    }
}
