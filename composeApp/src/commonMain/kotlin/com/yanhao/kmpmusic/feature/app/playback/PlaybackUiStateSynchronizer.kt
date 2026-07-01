package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 把播放仓库事实投影为共享 UI 状态，避免 facade 重复感知播放与队列细节。
 */
class PlaybackUiStateSynchronizer(
    private val playbackRepository: PlaybackRepository,
    private val recentSongsBuilder: (state: MusicAppUiState, extraSongs: List<Song>) -> List<Song>,
) {
    /**
     * 统一按仓库中的播放与队列事实刷新共享播放 UI，保证所有入口使用同一条投影规则。
     */
    fun syncPlaybackState(
        state: MusicAppUiState,
        playbackState: PlaybackState,
    ): MusicAppUiState {
        val queueState: QueueState = playbackRepository.getQueueState()
        return state.copy(
            currentSongId = playbackState.currentSongId,
            playbackStatus = playbackState.status,
            playbackPositionMs = playbackState.positionMs,
            playbackDurationMs = playbackState.durationMs,
            playbackMode = queueState.playbackMode,
            playbackError = playbackState.error,
            queueSongIds = queueState.songIds,
            recentSongs = recentSongsBuilder(state, emptyList()),
        )
    }
}
