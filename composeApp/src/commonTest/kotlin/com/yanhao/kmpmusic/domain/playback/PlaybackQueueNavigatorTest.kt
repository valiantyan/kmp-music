package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.QueueState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 验证 [PlaybackQueueNavigator] 在顺序、单曲循环、随机和删歌场景下的纯队列迁移规则。
 */
class PlaybackQueueNavigatorTest {
    // 固定随机候选选择顺序，确保测试稳定复现。
    private val navigator: PlaybackQueueNavigator = PlaybackQueueNavigator(
        shufflePolicy = ShuffleQueuePolicy(
            randomIndex = { candidates: List<Int> -> candidates.first() },
        ),
    )

    /**
     * 顺序播放点下一首时，最后一首需要回到第一首。
     */
    @Test
    fun sequenceNextLoopsFromLastToFirst(): Unit {
        val result: QueueNavigationResult? = navigator.next(
            queueState = QueueState(
                songIds = listOf("a", "b", "c"),
                currentIndex = 2,
                playbackMode = PlaybackMode.LoopAll,
            ),
        )
        assertEquals(expected = 0, actual = result?.targetIndex)
        assertEquals(expected = 0, actual = result?.queueState?.currentIndex)
    }

    /**
     * 顺序播放点上一首时，第一首需要回到最后一首。
     */
    @Test
    fun sequencePreviousLoopsFromFirstToLast(): Unit {
        val result: QueueNavigationResult? = navigator.previous(
            queueState = QueueState(
                songIds = listOf("a", "b", "c"),
                currentIndex = 0,
                playbackMode = PlaybackMode.LoopAll,
            ),
        )
        assertEquals(expected = 2, actual = result?.targetIndex)
        assertEquals(expected = 2, actual = result?.queueState?.currentIndex)
    }

    /**
     * 单曲循环遇到自然结束时，保持当前歌曲，避免被下一首逻辑覆盖。
     */
    @Test
    fun loopOneNextKeepsCurrentWhenRequested(): Unit {
        val result: QueueNavigationResult? = navigator.next(
            queueState = QueueState(
                songIds = listOf("a", "b", "c"),
                currentIndex = 1,
                playbackMode = PlaybackMode.LoopOne,
            ),
            keepLoopOneCurrent = true,
        )
        assertEquals(expected = 1, actual = result?.targetIndex)
        assertEquals(expected = 1, actual = result?.queueState?.currentIndex)
    }

    /**
     * 精确跳转越界时必须直接拒绝，避免 facade 继续推进错误索引。
     */
    @Test
    fun exactIndexRejectsOutOfRangeIndex(): Unit {
        val result: QueueNavigationResult? = navigator.exactIndex(
            queueState = QueueState(
                songIds = listOf("a", "b"),
                currentIndex = 0,
            ),
            targetIndex = 4,
        )
        assertNull(actual = result)
    }

    /**
     * 随机模式精确跳回当前歌曲时，不应污染随机历史和剩余集合。
     */
    @Test
    fun exactIndexForCurrentShuffleItemDoesNotPolluteHistory(): Unit {
        val result: QueueNavigationResult? = navigator.exactIndex(
            queueState = QueueState(
                songIds = listOf("a", "b", "c"),
                currentIndex = 1,
                playbackMode = PlaybackMode.Shuffle,
                shuffleHistory = listOf(0),
                shuffleRemaining = listOf(2),
            ),
            targetIndex = 1,
        )
        assertEquals(expected = 1, actual = result?.targetIndex)
        assertEquals(expected = listOf(0), actual = result?.queueState?.shuffleHistory)
        assertEquals(expected = listOf(2), actual = result?.queueState?.shuffleRemaining)
    }

    /**
     * 外部引擎切歌到新随机项时，需要同步维护历史与剩余集合。
     */
    @Test
    fun engineTransitionUpdatesShuffleHistoryAndRemaining(): Unit {
        val result: QueueNavigationResult? = navigator.engineTransition(
            queueState = QueueState(
                songIds = listOf("a", "b", "c", "d"),
                currentIndex = 0,
                playbackMode = PlaybackMode.Shuffle,
                shuffleRemaining = listOf(1, 2, 3),
            ),
            targetIndex = 2,
        )
        assertEquals(expected = 2, actual = result?.queueState?.currentIndex)
        assertEquals(expected = listOf(0), actual = result?.queueState?.shuffleHistory)
        assertEquals(expected = listOf(1, 3), actual = result?.queueState?.shuffleRemaining)
    }

    /**
     * 切换到随机模式时，应重建首轮 remaining 并清空旧的随机轨迹。
     */
    @Test
    fun modeChangeResetsShuffleHistoryAndBuildsRemainingForNewMode(): Unit {
        val result: QueueState = navigator.changePlaybackMode(
            queueState = QueueState(
                songIds = listOf("a", "b", "c"),
                currentIndex = 1,
                playbackMode = PlaybackMode.LoopOne,
                shuffleHistory = listOf(0),
                shuffleRemaining = listOf(2),
            ),
            playbackMode = PlaybackMode.Shuffle,
        )
        assertEquals(expected = PlaybackMode.Shuffle, actual = result.playbackMode)
        assertEquals(expected = emptyList(), actual = result.shuffleHistory)
        assertEquals(expected = listOf(0, 2), actual = result.shuffleRemaining)
    }

    /**
     * 删除当前歌曲后，应选择重建队列中的首个可播放歌曲并重置随机状态。
     */
    @Test
    fun removeCurrentSongSelectsFirstResolvedSongAndResetsShuffleState(): Unit {
        val result: QueueNavigationResult? = navigator.removeSong(
            queueState = QueueState(
                songIds = listOf("a", "b", "c", "d"),
                currentIndex = 2,
                playbackMode = PlaybackMode.Shuffle,
                shuffleHistory = listOf(0, 1),
                shuffleRemaining = listOf(3),
            ),
            removedSongId = "c",
            currentSongId = "c",
            nextSongIds = listOf("a", "b", "d"),
        )
        assertEquals(expected = 0, actual = result?.targetIndex)
        assertEquals(expected = listOf("a", "b", "d"), actual = result?.queueState?.songIds)
        assertEquals(expected = 0, actual = result?.queueState?.currentIndex)
        assertEquals(expected = emptyList(), actual = result?.queueState?.shuffleHistory)
        assertEquals(expected = listOf(1, 2), actual = result?.queueState?.shuffleRemaining)
    }

    /**
     * 删除非当前歌曲时，只要当前歌曲仍存在，就必须保持 current 不变。
     */
    @Test
    fun removeNonCurrentSongKeepsCurrentSongWhenItStillExists(): Unit {
        val result: QueueNavigationResult? = navigator.removeSong(
            queueState = QueueState(
                songIds = listOf("a", "b", "c"),
                currentIndex = 1,
                playbackMode = PlaybackMode.LoopAll,
            ),
            removedSongId = "c",
            currentSongId = "b",
            nextSongIds = listOf("a", "b"),
        )
        assertEquals(expected = 1, actual = result?.targetIndex)
        assertEquals(expected = listOf("a", "b"), actual = result?.queueState?.songIds)
        assertEquals(expected = 1, actual = result?.queueState?.currentIndex)
    }

    /**
     * 可恢复自动前进必须真的有不同目标，避免单曲队列假装还能跳转。
     */
    @Test
    fun recoverableNextTargetRequiresDifferentSongOutsideLoopOne(): Unit {
        assertEquals(
            expected = false,
            actual = navigator.hasDifferentNextTarget(
                queueState = QueueState(
                    songIds = listOf("a"),
                    currentIndex = 0,
                    playbackMode = PlaybackMode.LoopAll,
                ),
            ),
        )
        assertEquals(
            expected = true,
            actual = navigator.hasDifferentNextTarget(
                queueState = QueueState(
                    songIds = listOf("a", "b"),
                    currentIndex = 0,
                    playbackMode = PlaybackMode.LoopAll,
                ),
            ),
        )
    }
}
