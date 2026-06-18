package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository

/**
 * 阶段一播放状态内存实现，后续真实播放器可无痛替换。
 */
class InMemoryPlaybackRepository : PlaybackRepository {
    // 当前播放状态，默认对齐原型首屏。
    private var playbackState: PlaybackState = PlaybackState(
        currentSongId = "sea-dream",
        isPlaying = true,
    )

    // 当前队列状态，默认包含原型最近播放和重点歌曲。
    private var queueState: QueueState = QueueState(
        songIds = listOf("sea-dream", "summer-waltz", "river", "best", "forest", "long-night"),
    )

    /** 获取当前播放状态。 */
    override fun getPlaybackState(): PlaybackState = playbackState

    /** 保存当前播放状态。 */
    override fun savePlaybackState(state: PlaybackState) {
        playbackState = state
    }

    /** 获取当前播放队列。 */
    override fun getQueueState(): QueueState = queueState

    /** 保存当前播放队列。 */
    override fun saveQueueState(state: QueueState) {
        queueState = state
    }
}
