package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository

/**
 * 阶段一播放状态内存实现，后续真实播放器可无痛替换。
 */
class InMemoryPlaybackRepository : PlaybackRepository {
    // 当前播放状态，未播放前不指向任何歌曲。
    private var playbackState: PlaybackState = PlaybackState()

    // 当前队列状态，扫描结果不会自动写入队列。
    private var queueState: QueueState = QueueState()

    // 真实播放历史，只由播放动作写入。
    private var playbackHistory: PlaybackHistory = PlaybackHistory()

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

    /** 获取真实播放历史。 */
    override fun getPlaybackHistory(): PlaybackHistory = playbackHistory

    /** 保存真实播放历史。 */
    override fun savePlaybackHistory(history: PlaybackHistory) {
        playbackHistory = history
    }
}
