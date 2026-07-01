package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.QueueState

/**
 * 纯随机队列策略，集中维护随机模式下的历史栈、剩余集合和下一首选择规则。
 */
class ShuffleQueuePolicy(
    // 随机候选下标选择器，保留可注入能力以便测试固定结果。
    private val randomIndex: (List<Int>) -> Int = { candidates: List<Int> -> candidates.random() },
) {
    /**
     * 为随机模式生成首轮待播集合，避免一开始就重复当前歌曲。
     */
    fun buildInitialRemaining(queueSize: Int, currentIndex: Int): List<Int> {
        return (0 until queueSize).filterNot { index: Int -> index == currentIndex }
    }

    /**
     * 计算随机模式下的下一首；若当前轮已耗尽，则开启新一轮但避开当前歌曲。
     */
    fun nextIndex(queueState: QueueState): Int {
        if (queueState.songIds.isEmpty()) {
            return -1
        }
        val candidates: List<Int> = queueState.shuffleRemaining.ifEmpty {
            buildInitialRemaining(
                queueSize = queueState.songIds.size,
                currentIndex = queueState.currentIndex,
            )
        }
        return candidates.firstOrNull()?.let {
            randomIndex(candidates)
        } ?: queueState.currentIndex.coerceAtLeast(minimumValue = 0)
    }

    /**
     * 迁移随机模式队列状态，统一维护前进和后退时的历史与剩余集合。
     */
    fun migrateQueueState(
        queueState: QueueState,
        targetIndex: Int,
        isMovingBackward: Boolean,
    ): QueueState {
        if (isMovingBackward && queueState.shuffleHistory.isNotEmpty()) {
            return queueState.copy(
                currentIndex = targetIndex,
                shuffleHistory = queueState.shuffleHistory.dropLast(n = 1),
                shuffleRemaining = rebuildRemainingForBackward(
                    queueState = queueState,
                    targetIndex = targetIndex,
                ),
            )
        }
        return queueState.copy(
            currentIndex = targetIndex,
            shuffleHistory = rebuildHistoryForForward(queueState = queueState),
            shuffleRemaining = rebuildRemainingForForward(
                queueState = queueState,
                targetIndex = targetIndex,
            ),
        )
    }

    /**
     * 前进到新歌曲时记录旧 current，保证上一首回退能复用同一条历史链。
     */
    private fun rebuildHistoryForForward(queueState: QueueState): List<Int> {
        return queueState.currentIndex.takeIf { index: Int -> index >= 0 }?.let { index: Int ->
            queueState.shuffleHistory + index
        } ?: queueState.shuffleHistory
    }

    /**
     * 后退离开当前歌曲时把它放回 remaining，避免本轮剩余集合丢失可回放项。
     */
    private fun rebuildRemainingForBackward(queueState: QueueState, targetIndex: Int): List<Int> {
        return (queueState.shuffleRemaining + queueState.currentIndex)
            .distinct()
            .filterNot { index: Int -> index == targetIndex }
    }

    /**
     * 前进后剔除目标歌曲；若上一轮耗尽，则基于新 current 重建下一轮 remaining。
     */
    private fun rebuildRemainingForForward(queueState: QueueState, targetIndex: Int): List<Int> {
        return queueState.shuffleRemaining
            .filterNot { index: Int -> index == targetIndex }
            .ifEmpty {
                buildInitialRemaining(
                    queueSize = queueState.songIds.size,
                    currentIndex = targetIndex,
                )
            }
    }
}
