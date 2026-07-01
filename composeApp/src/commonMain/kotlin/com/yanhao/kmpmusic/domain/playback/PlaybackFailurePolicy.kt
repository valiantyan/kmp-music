package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackMode

/**
 * 管理运行时播放失败计数，并把失败事件转换成恢复决策。
 */
internal class PlaybackFailurePolicy {
    // 单曲循环同一首歌的连续失败次数。
    private var loopOneFailureCount: Int = 0

    // 非单曲循环场景下连续失败歌曲数。
    private var consecutiveFailedSongCount: Int = 0

    // 最近一次失败的歌曲标识，用于识别单曲循环是否还是同一首歌。
    private var lastFailedSongId: String? = null

    /**
     * 根据失败歌曲、播放模式和是否存在恢复目标，给出下一步恢复动作。
     */
    internal fun onFailure(
        error: PlaybackError,
        playbackMode: PlaybackMode,
        hasRecoverableTarget: Boolean,
    ): PlaybackFailureDecision {
        if (playbackMode == PlaybackMode.LoopOne) {
            loopOneFailureCount = if (lastFailedSongId == error.songId) {
                loopOneFailureCount + 1
            } else {
                1
            }
            lastFailedSongId = error.songId
            return if (loopOneFailureCount < FAILURE_THRESHOLD) {
                PlaybackFailureDecision.RetryCurrent
            } else {
                PlaybackFailureDecision.StayError
            }
        }
        consecutiveFailedSongCount += 1
        lastFailedSongId = error.songId
        return if (consecutiveFailedSongCount < FAILURE_THRESHOLD && hasRecoverableTarget) {
            PlaybackFailureDecision.SkipToNext
        } else {
            PlaybackFailureDecision.StayError
        }
    }

    /**
     * 在成功播放后清空失败窗口，避免旧错误影响后续恢复决策。
     */
    internal fun reset() {
        loopOneFailureCount = 0
        consecutiveFailedSongCount = 0
        lastFailedSongId = null
    }

    private companion object {
        /**
         * 连续失败达到 3 次后停止自动恢复，避免错误歌曲导致无限重试。
         */
        private const val FAILURE_THRESHOLD: Int = 3
    }
}

/**
 * 播放失败后的恢复动作。
 */
internal enum class PlaybackFailureDecision {
    RetryCurrent,
    SkipToNext,
    StayError,
}
