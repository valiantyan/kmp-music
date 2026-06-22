package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.QueueState

/**
 * 播放能力接口，第一阶段使用内存实现，平台真实播放器后续接入这里。
 */
interface PlaybackRepository {
    /**
     * 读取当前播放状态。
     */
    fun getPlaybackState(): PlaybackState

    /**
     * 保存当前播放状态。
     */
    fun savePlaybackState(state: PlaybackState)

    /**
     * 读取播放队列。
     */
    fun getQueueState(): QueueState

    /**
     * 保存播放队列。
     */
    fun saveQueueState(state: QueueState)

    /**
     * 读取真实播放历史。
     */
    fun getPlaybackHistory(): PlaybackHistory

    /**
     * 保存真实播放历史。
     */
    fun savePlaybackHistory(history: PlaybackHistory)
}
