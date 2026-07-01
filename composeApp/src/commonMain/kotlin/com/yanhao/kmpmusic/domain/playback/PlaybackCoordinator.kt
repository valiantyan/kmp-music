package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * common 层播放协调器，统一管理队列、模式、失败恢复和引擎状态回写。
 */
class PlaybackCoordinator(
    // 运行时播放仓库，供 UI 读取即时状态。
    private val playbackRepository: PlaybackRepository,
    // 平台无关的音频引擎入口。
    private val audioPlayerEngine: AudioPlayerEngine,
    // 快照存储，负责承接持久化与冷启动恢复规则。
    private val playbackSnapshotStore: PlaybackSnapshotStore = InMemoryPlaybackSnapshotStore(),
    // 快照写入作用域，独立于引擎事件订阅，避免未调用 [start] 时丢失持久化。
    private val snapshotWriteScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    // 当前时间提供者，方便后续节流测试。
    private val nowMillis: () -> Long = { 0L },
    // 播放中进度快照的节流窗口。
    private val snapshotThrottleMs: Long = 5_000L,
    // 随机模式下的下标选择器，方便测试固定随机结果。
    private val randomIndex: (List<Int>) -> Int = { candidates -> candidates.random() },
) {
    // Shuffle 纯策略只在协调器内部协作，不暴露给公共构造签名。
    private val shuffleQueuePolicy: ShuffleQueuePolicy = ShuffleQueuePolicy(randomIndex = randomIndex)
    // 队列导航协作者，集中维护前进、后退和精确跳转的状态迁移。
    private val queueNavigator: PlaybackQueueNavigator = PlaybackQueueNavigator(shufflePolicy = shuffleQueuePolicy)
    // 失败恢复协作者，集中维护失败计数和自动恢复决策。
    private val failurePolicy: PlaybackFailurePolicy = PlaybackFailurePolicy()
    // 快照写入协作者，集中维护节流和 teardown 收口逻辑。
    private val snapshotWriter: PlaybackSnapshotWriter = PlaybackSnapshotWriter(
        playbackRepository = playbackRepository,
        playbackSnapshotStore = playbackSnapshotStore,
        snapshotWriteScope = snapshotWriteScope,
        nowMillis = nowMillis,
        snapshotThrottleMs = snapshotThrottleMs,
    )
    // 最近播放历史协作者，统一处理去重和数量上限。
    private val historyRecorder: PlaybackHistoryRecorder = PlaybackHistoryRecorder(
        playbackRepository = playbackRepository,
    )

    // 引擎事件订阅任务。
    private var eventJob: Job? = null

    // 状态变化回调，供上层刷新 UI。
    private var onStateChanged: () -> Unit = {}

    /**
     * 启动引擎事件采集，把真实播放事件折返到运行时仓库。
     */
    fun start(scope: CoroutineScope, onStateChanged: () -> Unit = {}) {
        eventJob?.cancel()
        this.onStateChanged = onStateChanged
        eventJob = scope.launch(
            context = Dispatchers.Unconfined,
            start = CoroutineStart.UNDISPATCHED,
        ) {
            audioPlayerEngine.events.collect { event ->
                handleEngineEvent(event = event)
                saveSnapshotForEvent(event = event)
                this@PlaybackCoordinator.onStateChanged()
            }
        }
    }

    /**
     * 按当前列表生成完整队列，并把目标歌曲置为 loading。
     */
    suspend fun playSong(song: Song, queueSongs: List<Song>) {
        val matchingQueueSongs: List<Song> = queueSongs.takeIf { songs ->
            songs.any { candidate -> candidate.id == song.id }
        } ?: listOf(song)
        val startIndex: Int = matchingQueueSongs.indexOfFirst { candidate -> candidate.id == song.id }.takeIf { index ->
            index >= 0
        } ?: 0
        val currentPlaybackMode: PlaybackMode = playbackRepository.getQueueState().playbackMode
        playbackRepository.saveQueueState(
            state = QueueState(
                songIds = matchingQueueSongs.map { queueSong -> queueSong.id },
                currentIndex = startIndex,
                playbackMode = currentPlaybackMode,
                shuffleRemaining = buildInitialShuffleRemaining(
                    playbackMode = currentPlaybackMode,
                    queueSize = matchingQueueSongs.size,
                    currentIndex = startIndex,
                ),
            ),
        )
        playbackRepository.savePlaybackState(
            state = PlaybackState(
                currentSongId = song.id,
                status = PlaybackStatus.Loading,
                positionMs = 0L,
                durationMs = song.durationMs,
            ),
        )
        historyRecorder.record(songId = song.id)
        saveSnapshotNow()
        onStateChanged()
        audioPlayerEngine.setPlaybackMode(playbackMode = currentPlaybackMode)
        audioPlayerEngine.setQueue(
            items = matchingQueueSongs.map { queueSong -> queueSong.toPlayableMedia() },
            startIndex = startIndex,
            startPositionMs = 0L,
        )
        audioPlayerEngine.play()
    }

    /**
     * 冷启动恢复最近一次快照，并把引擎预热到相同队列与进度，最终停在暂停态。
     */
    suspend fun restoreSnapshot(availableSongs: List<Song>) {
        val snapshot: PlaybackSnapshot = playbackSnapshotStore.restoreSnapshot(
            availableSongIds = availableSongs.map { song -> song.id }.toSet(),
        )
        val songsById: Map<String, Song> = availableSongs.associateBy { song -> song.id }
        val restoredQueueSongs: List<Song> = snapshot.queueState.songIds.mapNotNull { songId ->
            songsById[songId]
        }
        if (restoredQueueSongs.isEmpty()) {
            playbackRepository.saveQueueState(state = QueueState())
            playbackRepository.savePlaybackState(state = PlaybackState())
            saveSnapshotNow()
            onStateChanged()
            audioPlayerEngine.stop()
            return
        }
        val restoredIndex: Int = snapshot.queueState.currentIndex.coerceIn(
            minimumValue = 0,
            maximumValue = restoredQueueSongs.lastIndex,
        )
        val restoredSong: Song = restoredQueueSongs[restoredIndex]
        val restoredQueueState: QueueState = snapshot.queueState.copy(
            songIds = restoredQueueSongs.map { song -> song.id },
            currentIndex = restoredIndex,
            shuffleHistory = emptyList(),
            shuffleRemaining = buildInitialShuffleRemaining(
                playbackMode = snapshot.queueState.playbackMode,
                queueSize = restoredQueueSongs.size,
                currentIndex = restoredIndex,
            ),
        )
        val restoredPlaybackState: PlaybackState = snapshot.playbackState.copy(
            currentSongId = restoredSong.id,
            status = PlaybackStatus.Paused,
            durationMs = restoredSong.durationMs,
            error = null,
        )
        playbackRepository.saveQueueState(state = restoredQueueState)
        playbackRepository.savePlaybackState(state = restoredPlaybackState)
        saveSnapshotNow()
        onStateChanged()
        audioPlayerEngine.setPlaybackMode(playbackMode = restoredQueueState.playbackMode)
        audioPlayerEngine.setQueue(
            items = restoredQueueSongs.map { song -> song.toPlayableMedia() },
            startIndex = restoredIndex,
            startPositionMs = restoredPlaybackState.positionMs,
        )
        audioPlayerEngine.pause()
    }

    /**
     * 在播放与暂停间切换，其他状态交给引擎自行校正。
     */
    fun togglePlayback() {
        if (playbackRepository.getPlaybackState().isPlaying) {
            pause()
            return
        }
        play()
    }

    /**
     * 显式开始或继续播放当前媒体，供系统命令绕过 toggle 推断。
     */
    fun play() {
        audioPlayerEngine.play()
    }

    /**
     * 显式暂停当前媒体，避免 buffering/loading 态下的 toggle 误判。
     */
    fun pause() {
        audioPlayerEngine.pause()
    }

    /**
     * Service 即将销毁时，把播放器最后一帧进度折返成暂停快照，避免进程恢复回到旧位置。
     */
    fun persistSnapshotForServiceTeardown(positionMs: Long, durationMs: Long?) {
        val currentPlaybackState: PlaybackState = playbackRepository.getPlaybackState()
        val queueState: QueueState = playbackRepository.getQueueState()
        if (currentPlaybackState.currentSongId == null || queueState.songIds.isEmpty()) {
            playbackRepository.savePlaybackState(state = PlaybackState())
            saveSnapshotNow()
            onStateChanged()
            return
        }
        playbackRepository.savePlaybackState(
            state = currentPlaybackState.copy(
                currentSongId = currentPlaybackState.currentSongId,
                status = PlaybackStatus.Paused,
                positionMs = positionMs.coerceAtLeast(minimumValue = 0L),
                durationMs = durationMs ?: currentPlaybackState.durationMs,
            ),
        )
        saveSnapshotNow()
        onStateChanged()
    }

    /**
     * 进程级宿主退出前等待旧写入收口，再同步写入最后一份暂停快照。
     */
    suspend fun persistSnapshotForProcessTeardown(positionMs: Long, durationMs: Long?) {
        awaitPendingSnapshotWrites()
        val currentPlaybackState: PlaybackState = playbackRepository.getPlaybackState()
        val queueState: QueueState = playbackRepository.getQueueState()
        if (currentPlaybackState.currentSongId == null || queueState.songIds.isEmpty()) {
            playbackRepository.savePlaybackState(state = PlaybackState())
            saveSnapshotNowAndAwait()
            onStateChanged()
            return
        }
        playbackRepository.savePlaybackState(
            state = currentPlaybackState.copy(
                currentSongId = currentPlaybackState.currentSongId,
                status = PlaybackStatus.Paused,
                positionMs = positionMs.coerceAtLeast(minimumValue = 0L),
                durationMs = durationMs ?: currentPlaybackState.durationMs,
            ),
        )
        saveSnapshotNowAndAwait()
        onStateChanged()
    }

    /**
     * 按当前播放模式移动到下一首。
     */
    fun moveNext() {
        val queueState: QueueState = playbackRepository.getQueueState()
        val navigationResult: QueueNavigationResult? = queueNavigator.next(queueState = queueState)
        if (navigationResult != null) {
            moveToNavigationResult(navigationResult = navigationResult)
            return
        }
        playbackRepository.savePlaybackState(
            state = playbackRepository.getPlaybackState().copy(status = PlaybackStatus.Ended),
        )
    }

    /**
     * 回到上一首；随机模式优先走已记录的随机历史。
     */
    fun movePrevious() {
        val queueState: QueueState = playbackRepository.getQueueState()
        val navigationResult: QueueNavigationResult = queueNavigator.previous(queueState = queueState) ?: return
        moveToNavigationResult(navigationResult = navigationResult)
    }

    /**
     * 跳转播放进度，并立即把运行时状态更新为目标进度。
     */
    fun seekTo(positionMs: Long) {
        val safePositionMs: Long = positionMs.coerceAtLeast(minimumValue = 0L)
        playbackRepository.savePlaybackState(
            state = playbackRepository.getPlaybackState().copy(positionMs = safePositionMs),
        )
        saveSnapshotNow()
        onStateChanged()
        audioPlayerEngine.seekTo(positionMs = safePositionMs)
    }

    /**
     * 直接切到共享队列中的精确下标，并可带入系统命令给出的起始进度。
     */
    fun skipToQueueIndex(index: Int, positionMs: Long = 0L) {
        val playbackState: PlaybackState = playbackRepository.getPlaybackState()
        val shouldResumePlayback: Boolean = playbackState.status == PlaybackStatus.Playing ||
            playbackState.status == PlaybackStatus.Loading ||
            playbackState.status == PlaybackStatus.Buffering
        moveToIndex(
            targetIndex = index,
            positionMs = positionMs,
            shouldResumePlayback = shouldResumePlayback,
        )
    }

    /**
     * 按设计稿顺序切换顺序播放、单曲循环和随机播放。
     */
    fun cyclePlaybackMode() {
        val queueState: QueueState = playbackRepository.getQueueState()
        val nextMode: PlaybackMode = when (queueState.playbackMode) {
            PlaybackMode.LoopAll -> PlaybackMode.LoopOne
            PlaybackMode.LoopOne -> PlaybackMode.Shuffle
            PlaybackMode.Shuffle -> PlaybackMode.LoopAll
        }
        playbackRepository.saveQueueState(
            state = queueNavigator.changePlaybackMode(
                queueState = queueState,
                playbackMode = nextMode,
            ),
        )
        saveSnapshotNow()
        onStateChanged()
        audioPlayerEngine.setPlaybackMode(playbackMode = nextMode)
    }

    /**
     * 归一化音量后下发到平台引擎，避免 UI 层直接依赖具体播放器实现。
     */
    fun setVolume(volume: Float) {
        audioPlayerEngine.setVolume(volume = volume.coerceIn(minimumValue = 0f, maximumValue = 1f))
    }

    /**
     * 从当前播放队列移除指定歌曲，并同步替换引擎里的真实队列。
     */
    suspend fun removeFromQueue(songId: String, availableSongs: List<Song>) {
        val queueState: QueueState = playbackRepository.getQueueState()
        if (queueState.songIds.size <= 1 || songId !in queueState.songIds) {
            return
        }
        val playbackState: PlaybackState = playbackRepository.getPlaybackState()
        val songsById: Map<String, Song> = availableSongs.associateBy { song -> song.id }
        val nextQueueSongs: List<Song> = queueState.songIds
            .filterNot { queuedSongId -> queuedSongId == songId }
            .mapNotNull { queuedSongId -> songsById[queuedSongId] }
        if (nextQueueSongs.isEmpty()) {
            playbackRepository.saveQueueState(state = QueueState())
            playbackRepository.savePlaybackState(state = PlaybackState())
            saveSnapshotNow()
            onStateChanged()
            audioPlayerEngine.stop()
            return
        }
        val currentSongWasRemoved: Boolean = playbackState.currentSongId == songId
        val navigationResult: QueueNavigationResult = queueNavigator.removeSong(
            queueState = queueState,
            removedSongId = songId,
            currentSongId = playbackState.currentSongId,
            nextSongIds = nextQueueSongs.map { song: Song -> song.id },
        ) ?: return
        val nextCurrentSong: Song = nextQueueSongs[navigationResult.targetIndex]
        val shouldResumePlayback: Boolean = playbackState.status == PlaybackStatus.Playing ||
            playbackState.status == PlaybackStatus.Loading
        playbackRepository.saveQueueState(state = navigationResult.queueState)
        playbackRepository.savePlaybackState(
            state = playbackState.copy(
                currentSongId = nextCurrentSong.id,
                status = if (shouldResumePlayback) PlaybackStatus.Loading else PlaybackStatus.Paused,
                positionMs = if (currentSongWasRemoved) 0L else playbackState.positionMs,
                durationMs = nextCurrentSong.durationMs,
                error = null,
            ),
        )
        saveSnapshotNow()
        onStateChanged()
        audioPlayerEngine.setPlaybackMode(playbackMode = queueState.playbackMode)
        audioPlayerEngine.setQueue(
            items = nextQueueSongs.map { song -> song.toPlayableMedia() },
            startIndex = navigationResult.targetIndex,
            startPositionMs = playbackRepository.getPlaybackState().positionMs,
        )
        if (shouldResumePlayback) {
            audioPlayerEngine.play()
            return
        }
        audioPlayerEngine.pause()
    }

    /**
     * 暴露测试专用入口，避免共享测试依赖私有反射。
     */
    internal fun handleEngineEventForTest(event: PlaybackEngineEvent) {
        handleEngineEvent(event = event)
    }

    /**
     * 统一处理引擎回传事件，确保 repository 始终以真实事件为准。
     */
    private fun handleEngineEvent(event: PlaybackEngineEvent) {
        when (event) {
            is PlaybackEngineEvent.CurrentMediaChanged -> handleCurrentMediaChanged(event = event)
            is PlaybackEngineEvent.ProgressChanged -> updateProgress(event = event)
            is PlaybackEngineEvent.StatusChanged -> updateStatus(event = event)
            PlaybackEngineEvent.Ended -> handleEnded()
            is PlaybackEngineEvent.Failed -> handleFailure(error = event.error)
        }
    }

    /** 根据引擎当前媒体事件同步歌曲标识、队列下标和时长。 */
    private fun handleCurrentMediaChanged(event: PlaybackEngineEvent.CurrentMediaChanged) {
        val playbackState: PlaybackState = playbackRepository.getPlaybackState()
        val queueState: QueueState = playbackRepository.getQueueState()
        val nextQueueState: QueueState = queueNavigator.engineTransition(
            queueState = queueState,
            targetIndex = event.index,
        )?.queueState ?: queueState
        playbackRepository.saveQueueState(state = nextQueueState)
        playbackRepository.savePlaybackState(
            state = playbackState.copy(
                currentSongId = event.songId,
                durationMs = event.durationMs,
            ),
        )
    }

    /** 根据引擎进度事件更新运行时进度。 */
    private fun updateProgress(event: PlaybackEngineEvent.ProgressChanged) {
        val playbackState: PlaybackState = playbackRepository.getPlaybackState()
        playbackRepository.savePlaybackState(
            state = playbackState.copy(
                positionMs = event.positionMs,
                durationMs = event.durationMs,
            ),
        )
    }

    /** 根据引擎状态事件校正当前播放态，并在成功播放后清空失败计数。 */
    private fun updateStatus(event: PlaybackEngineEvent.StatusChanged) {
        val playbackState: PlaybackState = playbackRepository.getPlaybackState()
        if (event.status == PlaybackStatus.Playing) {
            failurePolicy.reset()
        }
        playbackRepository.savePlaybackState(
            state = playbackState.copy(
                status = event.status,
                positionMs = event.positionMs,
                durationMs = event.durationMs,
                error = if (event.status == PlaybackStatus.Playing) null else playbackState.error,
            ),
        )
    }

    /** 自然播完后按模式推进到目标歌曲。 */
    private fun handleEnded() {
        val queueState: QueueState = playbackRepository.getQueueState()
        val navigationResult: QueueNavigationResult? = queueNavigator.next(
            queueState = queueState,
            keepLoopOneCurrent = true,
        )
        if (navigationResult != null) {
            moveToNavigationResult(navigationResult = navigationResult)
            return
        }
        playbackRepository.savePlaybackState(
            state = playbackRepository.getPlaybackState().copy(status = PlaybackStatus.Ended),
        )
    }

    /** 只有随机模式才需要初始化待播集合，其他模式必须保持空列表以延续既有语义。 */
    private fun buildInitialShuffleRemaining(
        playbackMode: PlaybackMode,
        queueSize: Int,
        currentIndex: Int,
    ): List<Int> {
        if (playbackMode != PlaybackMode.Shuffle) {
            return emptyList()
        }
        return shuffleQueuePolicy.buildInitialRemaining(
            queueSize = queueSize,
            currentIndex = currentIndex,
        )
    }

    /** 先写入错误态，再按失败阈值决定是否自动跳过。 */
    private fun handleFailure(error: PlaybackError) {
        playbackRepository.savePlaybackState(
            state = playbackRepository.getPlaybackState().copy(
                status = PlaybackStatus.Error,
                error = error,
            ),
        )
        val queueState: QueueState = playbackRepository.getQueueState()
        when (
            failurePolicy.onFailure(
                error = error,
                playbackMode = queueState.playbackMode,
                hasRecoverableTarget = queueNavigator.hasDifferentNextTarget(queueState = queueState),
            )
        ) {
            PlaybackFailureDecision.RetryCurrent -> {
                val retryResult: QueueNavigationResult = queueNavigator.exactIndex(
                    queueState = queueState,
                    targetIndex = queueState.currentIndex,
                ) ?: return
                moveToNavigationResult(
                    navigationResult = retryResult,
                    clearError = false,
                )
            }
            PlaybackFailureDecision.SkipToNext -> {
                val nextResult: QueueNavigationResult? = queueNavigator.next(queueState = queueState)
                if (nextResult != null) {
                    moveToNavigationResult(
                        navigationResult = nextResult,
                        clearError = false,
                    )
                }
            }
            PlaybackFailureDecision.StayError -> Unit
        }
    }

    /**
     * 直接切到共享队列中的精确下标，并把导航纯结果统一交给 [moveToNavigationResult] 执行。
     */
    private fun moveToIndex(
        targetIndex: Int,
        isMovingBackward: Boolean = false,
        positionMs: Long = 0L,
        shouldResumePlayback: Boolean = true,
        clearError: Boolean = true,
    ) {
        val queueState: QueueState = playbackRepository.getQueueState()
        val navigationResult: QueueNavigationResult = queueNavigator.exactIndex(
            queueState = queueState,
            targetIndex = targetIndex,
            isMovingBackward = isMovingBackward,
        ) ?: return
        moveToNavigationResult(
            navigationResult = navigationResult,
            positionMs = positionMs,
            shouldResumePlayback = shouldResumePlayback,
            clearError = clearError,
        )
    }

    /**
     * 把纯导航结果落到仓库和引擎，避免 next/previous/failure 各自复制迁移逻辑。
     */
    private fun moveToNavigationResult(
        navigationResult: QueueNavigationResult,
        positionMs: Long = 0L,
        shouldResumePlayback: Boolean = true,
        clearError: Boolean = true,
    ) {
        val safePositionMs: Long = positionMs.coerceAtLeast(minimumValue = 0L)
        val currentPlaybackState: PlaybackState = playbackRepository.getPlaybackState()
        val songId: String = navigationResult.queueState.songIds[navigationResult.targetIndex]
        playbackRepository.saveQueueState(state = navigationResult.queueState)
        playbackRepository.savePlaybackState(
            state = currentPlaybackState.copy(
                currentSongId = songId,
                status = if (shouldResumePlayback) PlaybackStatus.Loading else PlaybackStatus.Paused,
                positionMs = safePositionMs,
                error = if (clearError) null else currentPlaybackState.error,
            ),
        )
        historyRecorder.record(songId = songId)
        saveSnapshotNow()
        onStateChanged()
        audioPlayerEngine.skipToIndex(index = navigationResult.targetIndex)
        if (safePositionMs > 0L) {
            audioPlayerEngine.seekTo(positionMs = safePositionMs)
        }
        if (shouldResumePlayback) {
            audioPlayerEngine.play()
            return
        }
        audioPlayerEngine.pause()
    }

    /** 只对播放进度做节流快照，其他关键事件立即补写。 */
    private fun saveSnapshotForEvent(event: PlaybackEngineEvent) {
        snapshotWriter.saveForEvent(event = event)
    }

    /** 把当前运行时播放状态写成最新快照。 */
    private fun saveSnapshotNow() {
        snapshotWriter.saveAsync()
    }

    /** 在退出前同步落盘最后一份快照，避免宿主提前销毁。 */
    private suspend fun saveSnapshotNowAndAwait() {
        snapshotWriter.saveNowAndAwait()
    }

    /** 等待当前已发出的快照写入全部完成，供数据库关闭前收口。 */
    suspend fun awaitPendingSnapshotWrites() {
        snapshotWriter.awaitPendingWrites()
    }

    /** 把 [Song] 转成可交给引擎的媒体项，避免 UI 自己拼媒体信息。 */
    private fun Song.toPlayableMedia(): PlayableMedia {
        return PlayableMedia(
            songId = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            localUri = localUri,
            coverArt = coverArt,
            mimeType = mimeType,
        )
    }
}
