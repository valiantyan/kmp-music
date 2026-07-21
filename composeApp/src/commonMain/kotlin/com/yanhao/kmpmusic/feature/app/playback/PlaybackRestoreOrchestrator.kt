package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshotIdentity
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 待加载上次播放数据请求，绑定首次读取到的保存快照身份。
 */
data class PendingPlaybackSnapshotRequest(
    val identity: PlaybackSnapshotIdentity,
)

/**
 * 冷启动恢复前先补齐可用歌曲实体，并把可提交的恢复负载交回门面做最后一跳 gate。
 */
class PlaybackRestoreOrchestrator(
    private val playbackSnapshotStore: PlaybackSnapshotStore,
    private val availableSongsResolver: (songIds: List<String>, preferredSongs: List<Song>) -> List<Song>,
) {
    /**
     * 恢复执行结果，只回传需要并入最新 UI 状态的实体快照和请求状态。
     *
     * @property queueSongsSnapshot 本次恢复解析出的完整队列实体快照。
     * @property pendingRequest 当前是否仍需等待后续曲库加载。
     * @property didHydrateSnapshot 是否已经真正执行恢复。
     */
    data class Result(
        val queueSongsSnapshot: List<Song>?,
        val restoredSnapshot: PlaybackSnapshot?,
        val pendingRequest: PendingPlaybackSnapshotRequest?,
        val didHydrateSnapshot: Boolean,
    )

    /**
     * 为本轮显式恢复请求读取持久化身份；没有可恢复队列时返回 null。
     */
    suspend fun createPendingRequest(): PendingPlaybackSnapshotRequest? {
        val savedIdentity: PlaybackSnapshotIdentity =
            playbackSnapshotStore.getSavedSnapshotIdentity()
                ?: return null
        return PendingPlaybackSnapshotRequest(identity = savedIdentity)
    }

    /**
     * 按当前已知歌曲尝试恢复快照，若实体尚不可用则只保留原请求，不主动触发扫描。
     */
    suspend fun restore(
        state: MusicAppUiState,
        preferredSongs: List<Song>,
        pendingRequest: PendingPlaybackSnapshotRequest?,
        isRequestCurrent: (PendingPlaybackSnapshotRequest) -> Boolean,
    ): Result {
        val request: PendingPlaybackSnapshotRequest =
            pendingRequest
                ?: return Result(
                    queueSongsSnapshot = null,
                    restoredSnapshot = null,
                    pendingRequest = null,
                    didHydrateSnapshot = false,
                )
        val savedIdentity: PlaybackSnapshotIdentity =
            playbackSnapshotStore.getSavedSnapshotIdentity()
                ?: return Result(
                    queueSongsSnapshot = null,
                    restoredSnapshot = null,
                    pendingRequest = null,
                    didHydrateSnapshot = false,
                )
        if (savedIdentity != request.identity) {
            return Result(
                queueSongsSnapshot = null,
                restoredSnapshot = null,
                pendingRequest = null,
                didHydrateSnapshot = false,
            )
        }
        if (request.identity.currentSongId == null) {
            return Result(
                queueSongsSnapshot = null,
                restoredSnapshot = null,
                pendingRequest = null,
                didHydrateSnapshot = false,
            )
        }
        val availableSongs: List<Song> =
            availableSongsResolver(
                request.identity.queueSongIds,
                preferredSongs,
            )
        val availableSongIds: Set<String> = availableSongs.map { song: Song -> song.id }.toSet()
        val hasCompleteQueue: Boolean =
            request.identity.queueSongIds.all { songId: String ->
                availableSongIds.contains(element = songId)
            }
        val hasCurrentSong: Boolean =
            request.identity.currentSongId.let { songId: String ->
                availableSongIds.contains(element = songId)
            }
        if (!hasCompleteQueue || !hasCurrentSong) {
            return Result(
                queueSongsSnapshot = null,
                restoredSnapshot = null,
                pendingRequest = request,
                didHydrateSnapshot = false,
            )
        }
        if (!isRequestCurrent(request) || playbackSnapshotStore.getSavedSnapshotIdentity() != request.identity) {
            return Result(
                queueSongsSnapshot = null,
                restoredSnapshot = null,
                pendingRequest = null,
                didHydrateSnapshot = false,
            )
        }
        val restoredSnapshot: PlaybackSnapshot =
            playbackSnapshotStore.restoreSnapshot(
                availableSongIds = availableSongIds,
            )
        if (!isRequestCurrent(request) || playbackSnapshotStore.getSavedSnapshotIdentity() != request.identity) {
            return Result(
                queueSongsSnapshot = null,
                restoredSnapshot = null,
                pendingRequest = null,
                didHydrateSnapshot = false,
            )
        }
        return Result(
            queueSongsSnapshot = availableSongs,
            restoredSnapshot = restoredSnapshot,
            pendingRequest = null,
            didHydrateSnapshot = true,
        )
    }
}
