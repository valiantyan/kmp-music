package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 冷启动恢复前先补齐可用歌曲实体，再把真正恢复动作委托给播放协调器。
 */
class PlaybackRestoreOrchestrator(
    private val playbackSnapshotStore: PlaybackSnapshotStore,
    private val availableSongsResolver: (songIds: List<String>, preferredSongs: List<Song>) -> List<Song>,
    private val restoreSnapshot: suspend (availableSongs: List<Song>) -> Unit,
) {
    /**
     * 恢复执行结果，既回传新的 UI 快照，也指明是否仍需等待后续曲库加载。
     *
     * @property state 恢复尝试后的最新 UI 状态。
     * @property isPending 当前是否仍有待恢复快照。
     */
    data class Result(
        val state: MusicAppUiState,
        val isPending: Boolean,
    )

    /**
     * 按当前已知歌曲尝试恢复快照，若实体尚不可用则只挂起请求，不主动触发扫描。
     */
    suspend fun restore(
        state: MusicAppUiState,
        preferredSongs: List<Song>,
    ): Result {
        val savedQueueSongIds: List<String> = playbackSnapshotStore.getSavedQueueSongIds()
        if (savedQueueSongIds.isEmpty()) {
            return Result(
                state = state,
                isPending = false,
            )
        }
        val availableSongs: List<Song> = availableSongsResolver(savedQueueSongIds, preferredSongs)
        if (availableSongs.isEmpty()) {
            return Result(
                state = state,
                isPending = true,
            )
        }
        restoreSnapshot(availableSongs)
        return Result(
            state = state.copy(queueSongsSnapshot = availableSongs),
            isPending = false,
        )
    }
}
