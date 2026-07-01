package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.QueueState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证 [ShuffleQueuePolicy] 对随机历史、剩余集合和候选选择的不变量。
 */
class ShuffleQueuePolicyTest {
    /**
     * 首轮随机候选不能包含当前歌曲，否则进入随机模式后会原地重复。
     */
    @Test
    fun buildInitialRemainingExcludesCurrentIndex() {
        val policy = ShuffleQueuePolicy()

        val remaining: List<Int> = policy.buildInitialRemaining(
            queueSize = 4,
            currentIndex = 2,
        )

        assertEquals(expected = listOf(0, 1, 3), actual = remaining)
    }

    /**
     * 随机前进后需要记录离开的 current，并把目标歌曲从 remaining 中剔除。
     */
    @Test
    fun migrateQueueStateForwardUpdatesHistoryAndRemaining() {
        val policy = ShuffleQueuePolicy()
        val queueState: QueueState = buildQueueState(
            currentIndex = 0,
            shuffleHistory = emptyList(),
            shuffleRemaining = listOf(1, 2, 3),
        )

        val migratedState: QueueState = policy.migrateQueueState(
            queueState = queueState,
            targetIndex = 2,
            isMovingBackward = false,
        )

        assertEquals(expected = 2, actual = migratedState.currentIndex)
        assertEquals(expected = listOf(0), actual = migratedState.shuffleHistory)
        assertEquals(expected = listOf(1, 3), actual = migratedState.shuffleRemaining)
    }

    /**
     * 随机后退应优先消费 history，并把离开的歌曲补回 remaining 供本轮后续播放。
     */
    @Test
    fun migrateQueueStateBackwardUsesHistoryAndRestoresLeavingCurrentIndex() {
        val policy = ShuffleQueuePolicy()
        val queueState: QueueState = buildQueueState(
            currentIndex = 2,
            shuffleHistory = listOf(0),
            shuffleRemaining = listOf(1, 3),
        )

        val migratedState: QueueState = policy.migrateQueueState(
            queueState = queueState,
            targetIndex = 0,
            isMovingBackward = true,
        )

        assertEquals(expected = 0, actual = migratedState.currentIndex)
        assertEquals(expected = emptyList(), actual = migratedState.shuffleHistory)
        assertEquals(expected = listOf(1, 3, 2), actual = migratedState.shuffleRemaining)
    }

    /**
     * 当上一轮 remaining 已耗尽时，新一轮的候选仍不能立刻把当前歌曲再抽中。
     */
    @Test
    fun nextIndexStartsNewRoundWithoutImmediatelyRepeatingCurrentSong() {
        val policy = ShuffleQueuePolicy(
            randomIndex = { candidates: List<Int> -> candidates.first() },
        )
        val queueState: QueueState = buildQueueState(
            currentIndex = 1,
            shuffleHistory = listOf(0, 2),
            shuffleRemaining = emptyList(),
        )

        val nextIndex: Int = policy.nextIndex(queueState = queueState)

        assertEquals(expected = 0, actual = nextIndex)
    }

    /**
     * 空队列不应伪造可播放目标，避免协调器误触发跳转。
     */
    @Test
    fun nextIndexReturnsInvalidTargetForEmptyQueue() {
        val policy = ShuffleQueuePolicy()

        val nextIndex: Int = policy.nextIndex(queueState = QueueState())

        assertEquals(expected = -1, actual = nextIndex)
    }

    /**
     * 构造最小可用的随机队列状态，避免每个测试都重复样板 songId 列表。
     */
    private fun buildQueueState(
        currentIndex: Int,
        shuffleHistory: List<Int>,
        shuffleRemaining: List<Int>,
    ): QueueState {
        return QueueState(
            songIds = listOf("song-0", "song-1", "song-2", "song-3"),
            currentIndex = currentIndex,
            shuffleHistory = shuffleHistory,
            shuffleRemaining = shuffleRemaining,
        )
    }
}
