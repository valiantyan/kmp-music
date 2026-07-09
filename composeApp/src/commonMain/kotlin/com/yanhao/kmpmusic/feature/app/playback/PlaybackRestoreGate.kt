package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.playback.PlaybackCoordinator
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 托管播放恢复请求的并发门控，确保旧恢复不会覆盖用户新的播放事实。
 */
internal class PlaybackRestoreGate(
    private val playbackRestoreOrchestrator: PlaybackRestoreOrchestrator,
    private val playbackCoordinator: PlaybackCoordinator,
    private val controllerScope: CoroutineScope,
    private val stateHost: StateHost,
) {
    /**
     * 提供恢复 gate 需要的最新 UI 状态和状态发布能力，避免 gate 直接持有 facade。
     */
    interface StateHost {
        /**
         * 读取最新 UI 状态，恢复时必须拿到调用瞬间的状态而不是构造期快照。
         */
        fun getState(): MusicAppUiState

        /**
         * 读取当前已知歌曲，恢复快照时优先复用已有实体实例。
         */
        fun getPreferredKnownSongs(): List<Song>

        /**
         * 经由 facade 的统一 reducer 发布 UI 状态，避免绕过状态所有权。
         */
        fun reduceState(reducer: (MusicAppUiState) -> MusicAppUiState)
    }

    // 冷启动恢复请求只保存身份，不在门面里缓存整份持久化快照。
    private var pendingPlaybackSnapshotRequest: PendingPlaybackSnapshotRequest? = null

    // 防止扫描完成、首次全量加载和用户显式恢复同时并发触发多次 hydration。
    private var playbackSnapshotHydrationJob: Job? = null

    // 每次用户显式改变播放事实时递增，用来丢弃已经过期的恢复结果。
    private var playbackSnapshotHydrationGeneration: Long = 0L

    // 恢复提交与显式改写播放事实的入口必须串行，避免旧恢复在新动作之后继续落地。
    private val playbackFactMutationMutex: Mutex = Mutex()

    /**
     * 按可用曲库恢复持久化播放快照，并始终以暂停态回填共享 UI。
     */
    suspend fun restorePlaybackSnapshot() {
        if (playbackSnapshotHydrationJob?.isActive == true) {
            return
        }
        val request: PendingPlaybackSnapshotRequest = pendingPlaybackSnapshotRequest
            ?: playbackRestoreOrchestrator.createPendingRequest()
            ?: run {
                pendingPlaybackSnapshotRequest = null
                return
            }
        pendingPlaybackSnapshotRequest = request
        val activeJob: Job = currentCoroutineContext()[Job] ?: run {
            hydratePendingPlaybackSnapshot(request = request)
            return
        }
        playbackSnapshotHydrationJob = activeJob
        try {
            hydratePendingPlaybackSnapshot(request = request)
        } finally {
            if (playbackSnapshotHydrationJob == activeJob) {
                playbackSnapshotHydrationJob = null
            }
        }
    }

    // 只有启动期显式请求过恢复时，扫描成功后才续上真正的快照恢复。
    fun restorePlaybackSnapshotIfPending() {
        if (pendingPlaybackSnapshotRequest == null) {
            return
        }
        if (playbackSnapshotHydrationJob?.isActive == true) {
            return
        }
        controllerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val activeJob: Job = coroutineContext[Job] ?: return@launch
            playbackSnapshotHydrationJob = activeJob
            try {
                val request: PendingPlaybackSnapshotRequest = pendingPlaybackSnapshotRequest
                    ?: return@launch
                hydratePendingPlaybackSnapshot(request = request)
            } finally {
                if (playbackSnapshotHydrationJob == activeJob) {
                    playbackSnapshotHydrationJob = null
                }
            }
        }
    }

    // 所有会改写当前播放事实的公开入口都走同一串行域，并先作废旧恢复请求。
    fun launchPlaybackFactMutation(block: suspend () -> Unit) {
        controllerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            playbackFactMutationMutex.withLock {
                clearPendingPlaybackSnapshotRequest()
                block()
            }
        }
    }

    /**
     * 真正执行一次挂起恢复，只把解析出的队列实体并回最新 [MusicAppUiState]。
     */
    private suspend fun hydratePendingPlaybackSnapshot(request: PendingPlaybackSnapshotRequest) {
        val generationAtStart: Long = playbackSnapshotHydrationGeneration
        val result: PlaybackRestoreOrchestrator.Result = playbackRestoreOrchestrator.restore(
            state = stateHost.getState(),
            preferredSongs = stateHost.getPreferredKnownSongs(),
            pendingRequest = request,
            isRequestCurrent = { currentRequest: PendingPlaybackSnapshotRequest ->
                isPendingPlaybackSnapshotRequestCurrent(
                    request = currentRequest,
                    generation = generationAtStart,
                )
            },
        )
        playbackFactMutationMutex.withLock {
            if (!isPendingPlaybackSnapshotRequestCurrent(request = request, generation = generationAtStart)) {
                return
            }
            result.restoredSnapshot?.let { restoredSnapshot: PlaybackSnapshot ->
                val queueSongsSnapshot: List<Song> = result.queueSongsSnapshot ?: return@let
                playbackCoordinator.applyRestoredSnapshot(
                    snapshot = restoredSnapshot,
                    availableSongs = queueSongsSnapshot,
                )
            }
            result.queueSongsSnapshot?.let { queueSongsSnapshot: List<Song> ->
                stateHost.reduceState { currentState: MusicAppUiState ->
                    currentState.copy(queueSongsSnapshot = queueSongsSnapshot)
                }
            }
            pendingPlaybackSnapshotRequest = result.pendingRequest
        }
    }

    // 只有请求身份和代际都未变化时，恢复结果才允许继续提交到运行时播放事实。
    private fun isPendingPlaybackSnapshotRequestCurrent(
        request: PendingPlaybackSnapshotRequest,
        generation: Long,
    ): Boolean {
        return pendingPlaybackSnapshotRequest == request &&
            playbackSnapshotHydrationGeneration == generation
    }

    // 用户显式改变播放事实后，旧恢复请求必须作废，避免晚到结果覆盖最新意图。
    private fun clearPendingPlaybackSnapshotRequest() {
        pendingPlaybackSnapshotRequest = null
        playbackSnapshotHydrationGeneration += 1
        playbackSnapshotHydrationJob?.cancel()
        playbackSnapshotHydrationJob = null
    }
}
