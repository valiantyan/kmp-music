package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository

/**
 * 维护最近播放历史的顺序、去重和数量上限。
 */
internal class PlaybackHistoryRecorder(
    private val playbackRepository: PlaybackRepository,
) {
    fun record(songId: String) {
        val currentSongIds: List<String> = playbackRepository.getPlaybackHistory().songIds
        playbackRepository.savePlaybackHistory(
            history = PlaybackHistory(
                songIds = (listOf(songId) + currentSongIds.filterNot { currentId: String ->
                    currentId == songId
                }).take(n = MAX_HISTORY_SIZE),
            ),
        )
    }

    private companion object {
        private const val MAX_HISTORY_SIZE: Int = 50
    }
}
