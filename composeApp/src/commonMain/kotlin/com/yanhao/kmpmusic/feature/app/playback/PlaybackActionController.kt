package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.playback.PlaybackCoordinator
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * 播放动作工作流，集中处理会改变播放事实、队列事实或播放持久快照的入口。
 */
class PlaybackActionController(
    private val playbackCoordinator: PlaybackCoordinator,
    private val playbackRepository: PlaybackRepository,
    private val playbackSnapshotStore: PlaybackSnapshotStore,
    private val controllerScope: CoroutineScope,
    private val nowMillis: () -> Long,
) {
    /**
     * 已解析的播放动作。门面必须先写入 [state]，再调用 [startPlayback] 执行副作用。
     */
    data class PreparedPlaySong(
        val state: MusicAppUiState,
        val song: Song,
        val queueSongs: List<Song>,
    )

    /** 播放歌曲但留在当前页面，未显式传列表时优先复用当前队列上下文。 */
    fun preparePlaySong(
        state: MusicAppUiState,
        song: Song,
        queueSongs: List<Song>,
    ): PreparedPlaySong {
        val resolvedQueueSongs: List<Song> = resolvePlaybackQueueSongs(
            state = state,
            song = song,
            queueSongs = queueSongs,
        )
        return PreparedPlaySong(
            state = state.copy(queueSongsSnapshot = resolvedQueueSongs),
            song = song,
            queueSongs = resolvedQueueSongs,
        )
    }

    /** 门面写入实体队列快照后，再启动真正播放副作用。 */
    fun startPlayback(action: PreparedPlaySong) {
        controllerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            playbackCoordinator.playSong(
                song = action.song,
                queueSongs = action.queueSongs,
            )
        }
    }

    /** 最近播放入口必须复用完整最近播放列表。 */
    fun preparePlayRecentSong(
        state: MusicAppUiState,
        song: Song,
    ): PreparedPlaySong {
        return preparePlaySong(
            state = state,
            song = song,
            queueSongs = state.recentSongs,
        )
    }

    /** 切换播放暂停。 */
    fun togglePlayback() {
        playbackCoordinator.togglePlayback()
    }

    /** 显式恢复或开始播放。 */
    fun play() {
        playbackCoordinator.play()
    }

    /** 显式暂停播放。 */
    fun pause() {
        playbackCoordinator.pause()
    }

    /** 切换上一首或下一首。 */
    fun moveTrack(direction: Int) {
        if (direction < 0) {
            playbackCoordinator.movePrevious()
            return
        }
        playbackCoordinator.moveNext()
    }

    /** 按精确队列下标切歌。 */
    fun skipToQueueIndex(index: Int, positionMs: Long = 0L) {
        playbackCoordinator.skipToQueueIndex(
            index = index,
            positionMs = positionMs,
        )
    }

    /** 拖动播放进度时同时更新运行态与持久化快照。 */
    fun seekTo(positionMs: Long) {
        playbackCoordinator.seekTo(positionMs = positionMs)
        controllerScope.launch {
            playbackSnapshotStore.saveSnapshot(
                snapshot = PlaybackSnapshot(
                    playbackState = playbackRepository.getPlaybackState().copy(
                        positionMs = positionMs.coerceAtLeast(minimumValue = 0L),
                    ),
                    queueState = playbackRepository.getQueueState(),
                    updatedAt = nowMillis(),
                ),
            )
        }
    }

    /** 播放模式按钮只负责触发协调器切换。 */
    fun cyclePlaybackMode() {
        playbackCoordinator.cyclePlaybackMode()
    }

    /** 调整共享播放器音量。 */
    fun setVolume(
        state: MusicAppUiState,
        volume: Float,
    ): MusicAppUiState {
        val safeVolume: Float = volume.coerceIn(minimumValue = 0f, maximumValue = 1f)
        playbackCoordinator.setVolume(volume = safeVolume)
        return state.copy(playbackVolume = safeVolume)
    }

    /** Android 播放 service 退出前补写最终暂停快照。 */
    fun persistPlaybackSnapshotForServiceTeardown(positionMs: Long, durationMs: Long?) {
        playbackCoordinator.persistSnapshotForServiceTeardown(
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    /** Desktop 进程退出前同步固化最后进度。 */
    suspend fun persistPlaybackSnapshotForProcessTeardown(positionMs: Long, durationMs: Long?) {
        playbackCoordinator.persistSnapshotForProcessTeardown(
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    /** 从队列移除歌曲，至少保留一首。 */
    fun removeFromQueue(
        state: MusicAppUiState,
        songId: String,
    ) {
        controllerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            playbackCoordinator.removeFromQueue(
                songId = songId,
                availableSongs = state.queueSongs,
            )
        }
    }

    /** 清空真实最近播放历史，并立即同步当前页面列表。 */
    fun clearRecentPlaybackHistory(state: MusicAppUiState): MusicAppUiState {
        playbackRepository.savePlaybackHistory(history = PlaybackHistory())
        return state.copy(recentSongs = emptyList())
    }

    private fun resolvePlaybackQueueSongs(
        state: MusicAppUiState,
        song: Song,
        queueSongs: List<Song>,
    ): List<Song> {
        if (queueSongs.any { candidate: Song -> candidate.id == song.id }) {
            return queueSongs
        }
        val currentQueueSongs: List<Song> = state.queueSongs
        if (currentQueueSongs.any { candidate: Song -> candidate.id == song.id }) {
            return currentQueueSongs
        }
        return listOf(song)
    }
}
