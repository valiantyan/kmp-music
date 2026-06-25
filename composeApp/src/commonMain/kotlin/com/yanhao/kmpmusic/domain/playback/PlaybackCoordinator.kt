package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
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
import kotlinx.coroutines.joinAll
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
    // 快照写入任务集合，供进程级 teardown 在关闭数据库前等待所有异步写入完成。
    private val pendingSnapshotWritesLock: Any = Any()

    // 当前仍在执行的快照写入任务，避免 Desktop 退出时和数据库 close 发生竞态。
    private val pendingSnapshotWrites: MutableSet<Job> = linkedSetOf()

    // 引擎事件订阅任务。
    private var eventJob: Job? = null

    // 状态变化回调，供上层刷新 UI。
    private var onStateChanged: () -> Unit = {}

    // 单曲循环同一首连续失败次数。
    private var loopOneFailureCount: Int = 0

    // 非单曲循环下连续失败次数。
    private var consecutiveFailedSongCount: Int = 0

    // 最近一次失败歌曲，用于单曲循环限流。
    private var lastFailedSongId: String? = null

    // 最近一次播放进度写入快照的时间。
    private var lastProgressSnapshotAt: Long? = null

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
        recordHistory(songId = song.id)
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
                currentSongId = currentPlaybackState.currentSongId ?: queueState.currentSongId,
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
                currentSongId = currentPlaybackState.currentSongId ?: queueState.currentSongId,
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
        val targetIndex: Int = nextIndex(queueState = queueState)
        if (targetIndex >= 0) {
            moveToIndex(targetIndex = targetIndex)
        }
    }

    /**
     * 回到上一首；随机模式优先走已记录的随机历史。
     */
    fun movePrevious() {
        val queueState: QueueState = playbackRepository.getQueueState()
        if (queueState.songIds.isEmpty()) {
            return
        }
        val targetIndex: Int = if (
            queueState.playbackMode == PlaybackMode.Shuffle &&
            queueState.shuffleHistory.isNotEmpty()
        ) {
            queueState.shuffleHistory.last()
        } else {
            (queueState.currentIndex - 1 + queueState.songIds.size) % queueState.songIds.size
        }
        moveToIndex(targetIndex = targetIndex, isMovingBackward = true)
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
     * 按设计稿顺序切换列表循环、单曲循环和随机播放。
     */
    fun cyclePlaybackMode() {
        val queueState: QueueState = playbackRepository.getQueueState()
        val nextMode: PlaybackMode = when (queueState.playbackMode) {
            PlaybackMode.LoopAll -> PlaybackMode.LoopOne
            PlaybackMode.LoopOne -> PlaybackMode.Shuffle
            PlaybackMode.Shuffle -> PlaybackMode.LoopAll
        }
        playbackRepository.saveQueueState(
            state = queueState.copy(
                playbackMode = nextMode,
                shuffleHistory = emptyList(),
                shuffleRemaining = buildInitialShuffleRemaining(
                    playbackMode = nextMode,
                    queueSize = queueState.songIds.size,
                    currentIndex = queueState.currentIndex,
                ),
            ),
        )
        saveSnapshotNow()
        onStateChanged()
        audioPlayerEngine.setPlaybackMode(playbackMode = nextMode)
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
        val nextCurrentSongId: String = if (currentSongWasRemoved) {
            nextQueueSongs.first().id
        } else {
            playbackState.currentSongId?.takeIf { currentSongId ->
                nextQueueSongs.any { song -> song.id == currentSongId }
            } ?: nextQueueSongs.first().id
        }
        val nextCurrentIndex: Int = nextQueueSongs.indexOfFirst { song ->
            song.id == nextCurrentSongId
        }.coerceAtLeast(minimumValue = 0)
        val nextCurrentSong: Song = nextQueueSongs[nextCurrentIndex]
        val shouldResumePlayback: Boolean = playbackState.status == PlaybackStatus.Playing ||
            playbackState.status == PlaybackStatus.Loading
        playbackRepository.saveQueueState(
            state = queueState.copy(
                songIds = nextQueueSongs.map { song -> song.id },
                currentIndex = nextCurrentIndex,
                shuffleHistory = emptyList(),
                shuffleRemaining = buildInitialShuffleRemaining(
                    playbackMode = queueState.playbackMode,
                    queueSize = nextQueueSongs.size,
                    currentIndex = nextCurrentIndex,
                ),
            ),
        )
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
            startIndex = nextCurrentIndex,
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
        val nextQueueState: QueueState = buildQueueStateForEngineTransition(
            queueState = queueState,
            targetIndex = event.index,
        )
        playbackRepository.saveQueueState(state = nextQueueState)
        playbackRepository.savePlaybackState(
            state = playbackState.copy(
                currentSongId = event.songId,
                durationMs = event.durationMs,
            ),
        )
    }

    /** 外部 MediaController 切歌时，同步维护 common 层随机历史和剩余集合。 */
    private fun buildQueueStateForEngineTransition(queueState: QueueState, targetIndex: Int): QueueState {
        if (targetIndex !in queueState.songIds.indices) {
            return queueState
        }
        if (queueState.playbackMode != PlaybackMode.Shuffle || targetIndex == queueState.currentIndex) {
            return queueState.copy(currentIndex = targetIndex)
        }
        return buildShuffleQueueState(
            queueState = queueState,
            targetIndex = targetIndex,
            isMovingBackward = false,
        )
    }

    /** 根据引擎进度事件更新运行时进度。 */
    private fun updateProgress(event: PlaybackEngineEvent.ProgressChanged) {
        playbackRepository.savePlaybackState(
            state = playbackRepository.getPlaybackState().copy(
                positionMs = event.positionMs,
                durationMs = event.durationMs,
            ),
        )
    }

    /** 根据引擎状态事件校正当前播放态，并在成功播放后清空失败计数。 */
    private fun updateStatus(event: PlaybackEngineEvent.StatusChanged) {
        val playbackState: PlaybackState = playbackRepository.getPlaybackState()
        if (event.status == PlaybackStatus.Playing) {
            resetFailureCounters()
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
        if (queueState.songIds.isEmpty()) {
            playbackRepository.savePlaybackState(
                state = playbackRepository.getPlaybackState().copy(status = PlaybackStatus.Ended),
            )
            return
        }
        val targetIndex: Int = if (queueState.playbackMode == PlaybackMode.LoopOne) {
            queueState.currentIndex
        } else {
            nextIndex(queueState = queueState)
        }
        if (targetIndex >= 0) {
            moveToIndex(targetIndex = targetIndex)
        }
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
        if (queueState.playbackMode == PlaybackMode.LoopOne) {
            loopOneFailureCount = if (lastFailedSongId == error.songId) loopOneFailureCount + 1 else 1
            lastFailedSongId = error.songId
            if (loopOneFailureCount < 3) {
                moveToIndex(
                    targetIndex = queueState.currentIndex,
                    clearError = false,
                )
            }
            return
        }
        consecutiveFailedSongCount += 1
        lastFailedSongId = error.songId
        if (consecutiveFailedSongCount < 3) {
            val targetIndex: Int = nextIndex(queueState = queueState)
            if (targetIndex >= 0) {
                moveToIndex(
                    targetIndex = targetIndex,
                    clearError = false,
                )
            }
        }
    }

    /** 计算下一首下标；随机模式保证一轮内不重复。 */
    private fun nextIndex(queueState: QueueState): Int {
        if (queueState.songIds.isEmpty()) {
            return -1
        }
        if (queueState.playbackMode != PlaybackMode.Shuffle) {
            return (queueState.currentIndex + 1 + queueState.songIds.size) % queueState.songIds.size
        }
        val candidates: List<Int> = queueState.shuffleRemaining.ifEmpty {
            queueState.songIds.indices.filterNot { index -> index == queueState.currentIndex }
        }
        return candidates.firstOrNull()?.let {
            randomIndex(candidates)
        } ?: queueState.currentIndex.coerceAtLeast(minimumValue = 0)
    }

    /** 切换到目标下标，并把引擎和仓库一起推进到 loading。 */
    private fun moveToIndex(
        targetIndex: Int,
        isMovingBackward: Boolean = false,
        positionMs: Long = 0L,
        shouldResumePlayback: Boolean = true,
        clearError: Boolean = true,
    ) {
        val queueState: QueueState = playbackRepository.getQueueState()
        if (targetIndex !in queueState.songIds.indices) {
            return
        }
        val safePositionMs: Long = positionMs.coerceAtLeast(minimumValue = 0L)
        val currentPlaybackState: PlaybackState = playbackRepository.getPlaybackState()
        val nextQueueState: QueueState = if (queueState.playbackMode == PlaybackMode.Shuffle) {
            buildShuffleQueueState(
                queueState = queueState,
                targetIndex = targetIndex,
                isMovingBackward = isMovingBackward,
            )
        } else {
            queueState.copy(currentIndex = targetIndex)
        }
        val songId: String = nextQueueState.songIds[targetIndex]
        playbackRepository.saveQueueState(state = nextQueueState)
        playbackRepository.savePlaybackState(
            state = currentPlaybackState.copy(
                currentSongId = songId,
                status = if (shouldResumePlayback) PlaybackStatus.Loading else PlaybackStatus.Paused,
                positionMs = safePositionMs,
                error = if (clearError) null else currentPlaybackState.error,
            ),
        )
        recordHistory(songId = songId)
        saveSnapshotNow()
        onStateChanged()
        audioPlayerEngine.skipToIndex(index = targetIndex)
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
        if (event is PlaybackEngineEvent.ProgressChanged) {
            val now: Long = nowMillis()
            val previousSnapshotAt: Long? = lastProgressSnapshotAt
            if (previousSnapshotAt != null && now - previousSnapshotAt < snapshotThrottleMs) {
                return
            }
            lastProgressSnapshotAt = now
        }
        saveSnapshotNow()
    }

    /** 把当前运行时播放状态写成最新快照。 */
    private fun saveSnapshotNow() {
        launchSnapshotWrite()
    }

    /** 在退出前同步落盘最后一份快照，避免宿主提前销毁。 */
    private suspend fun saveSnapshotNowAndAwait() {
        launchSnapshotWrite().join()
    }

    /** 等待当前已发出的快照写入全部完成，供数据库关闭前收口。 */
    suspend fun awaitPendingSnapshotWrites() {
        while (true) {
            val pendingJobs: List<Job> = synchronized(lock = pendingSnapshotWritesLock) {
                pendingSnapshotWrites.toList()
            }
            if (pendingJobs.isEmpty()) {
                return
            }
            pendingJobs.joinAll()
        }
    }

    /** 启动单次快照写入并纳入 pending 集合，避免 teardown 和异步写入交错。 */
    private fun launchSnapshotWrite(): Job {
        val job: Job = snapshotWriteScope.launch(start = CoroutineStart.UNDISPATCHED) {
            playbackSnapshotStore.saveSnapshot(
                snapshot = PlaybackSnapshot(
                    playbackState = playbackRepository.getPlaybackState(),
                    queueState = playbackRepository.getQueueState(),
                    updatedAt = nowMillis(),
                ),
            )
        }
        synchronized(lock = pendingSnapshotWritesLock) {
            pendingSnapshotWrites += job
        }
        job.invokeOnCompletion {
            synchronized(lock = pendingSnapshotWritesLock) {
                pendingSnapshotWrites.remove(element = job)
            }
        }
        return job
    }

    /** 成功进入播放态后清空失败计数，避免旧错误污染新一轮播放。 */
    private fun resetFailureCounters() {
        loopOneFailureCount = 0
        consecutiveFailedSongCount = 0
        lastFailedSongId = null
    }

    /** 维护最近播放历史，重复歌曲只保留最新位置。 */
    private fun recordHistory(songId: String) {
        val currentSongIds: List<String> = playbackRepository.getPlaybackHistory().songIds
        playbackRepository.savePlaybackHistory(
            history = PlaybackHistory(
                songIds = (listOf(songId) + currentSongIds.filterNot { currentId -> currentId == songId }).take(50),
            ),
        )
    }

    /** 为随机模式构建首轮待播集合，避免一开始就重复当前歌曲。 */
    private fun buildInitialShuffleRemaining(playbackMode: PlaybackMode, queueSize: Int, currentIndex: Int): List<Int> {
        if (playbackMode != PlaybackMode.Shuffle) {
            return emptyList()
        }
        return (0 until queueSize).filterNot { index -> index == currentIndex }
    }

    /** 随机模式切歌时同步维护历史栈和未播放集合。 */
    private fun buildShuffleQueueState(
        queueState: QueueState,
        targetIndex: Int,
        isMovingBackward: Boolean,
    ): QueueState {
        if (isMovingBackward && queueState.shuffleHistory.isNotEmpty()) {
            val rebuiltRemaining: List<Int> = (queueState.shuffleRemaining + queueState.currentIndex)
                .distinct()
                .filterNot { index -> index == targetIndex }
            return queueState.copy(
                currentIndex = targetIndex,
                shuffleHistory = queueState.shuffleHistory.dropLast(1),
                shuffleRemaining = rebuiltRemaining,
            )
        }
        val history: List<Int> = queueState.currentIndex.takeIf { index -> index >= 0 }?.let { index ->
            queueState.shuffleHistory + index
        } ?: queueState.shuffleHistory
        val remaining: List<Int> = queueState.shuffleRemaining.filterNot { index -> index == targetIndex }.ifEmpty {
            queueState.songIds.indices.filterNot { index -> index == targetIndex }
        }
        return queueState.copy(
            currentIndex = targetIndex,
            shuffleHistory = history,
            shuffleRemaining = remaining,
        )
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
