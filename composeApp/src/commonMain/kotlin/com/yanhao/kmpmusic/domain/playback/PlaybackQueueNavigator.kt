package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.QueueState

/**
 * 纯队列导航器，只负责计算下一个 [QueueState]，不触碰仓库和播放引擎。
 */
internal class PlaybackQueueNavigator(
    // 随机模式协作者，集中维护随机历史和剩余集合。
    private val shufflePolicy: ShuffleQueuePolicy,
) {
    /**
     * 计算下一首目标；自然结束时可选择在单曲循环里保持当前歌曲。
     */
    fun next(
        queueState: QueueState,
        keepLoopOneCurrent: Boolean = false,
    ): QueueNavigationResult? {
        if (queueState.songIds.isEmpty()) {
            return null
        }
        val targetIndex: Int = when {
            keepLoopOneCurrent && queueState.playbackMode == PlaybackMode.LoopOne -> queueState.currentIndex
            queueState.playbackMode == PlaybackMode.Shuffle -> shufflePolicy.nextIndex(queueState = queueState)
            else -> (queueState.currentIndex + 1 + queueState.songIds.size) % queueState.songIds.size
        }
        return exactIndex(
            queueState = queueState,
            targetIndex = targetIndex,
            isMovingBackward = false,
        )
    }

    /**
     * 计算上一首目标；随机模式优先消费历史，保证回退可预测。
     */
    fun previous(queueState: QueueState): QueueNavigationResult? {
        if (queueState.songIds.isEmpty()) {
            return null
        }
        val targetIndex: Int = if (
            queueState.playbackMode == PlaybackMode.Shuffle &&
            queueState.shuffleHistory.isNotEmpty()
        ) {
            queueState.shuffleHistory.last()
        } else {
            (queueState.currentIndex - 1 + queueState.songIds.size) % queueState.songIds.size
        }
        return exactIndex(
            queueState = queueState,
            targetIndex = targetIndex,
            isMovingBackward = true,
        )
    }

    /**
     * 迁移到精确下标；随机模式委托 [shufflePolicy] 维护历史和剩余集合。
     */
    fun exactIndex(
        queueState: QueueState,
        targetIndex: Int,
        isMovingBackward: Boolean = false,
    ): QueueNavigationResult? {
        if (targetIndex !in queueState.songIds.indices) {
            return null
        }
        if (targetIndex == queueState.currentIndex) {
            return QueueNavigationResult(
                queueState = queueState.copy(currentIndex = targetIndex),
                targetIndex = targetIndex,
            )
        }
        val nextQueueState: QueueState = if (queueState.playbackMode == PlaybackMode.Shuffle) {
            shufflePolicy.migrateQueueState(
                queueState = queueState,
                targetIndex = targetIndex,
                isMovingBackward = isMovingBackward,
            )
        } else {
            queueState.copy(currentIndex = targetIndex)
        }
        return QueueNavigationResult(
            queueState = nextQueueState,
            targetIndex = targetIndex,
        )
    }

    /**
     * 处理外部引擎切歌，保证 common 层随机导航状态与引擎 current 对齐。
     */
    fun engineTransition(
        queueState: QueueState,
        targetIndex: Int,
    ): QueueNavigationResult? {
        if (targetIndex !in queueState.songIds.indices) {
            return null
        }
        if (queueState.playbackMode != PlaybackMode.Shuffle || targetIndex == queueState.currentIndex) {
            return QueueNavigationResult(
                queueState = queueState.copy(currentIndex = targetIndex),
                targetIndex = targetIndex,
            )
        }
        return exactIndex(
            queueState = queueState,
            targetIndex = targetIndex,
            isMovingBackward = false,
        )
    }

    /**
     * 切换播放模式时统一重建随机相关状态，避免沿用旧模式遗留轨迹。
     */
    fun changePlaybackMode(
        queueState: QueueState,
        playbackMode: PlaybackMode,
    ): QueueState {
        return queueState.copy(
            playbackMode = playbackMode,
            shuffleHistory = emptyList(),
            shuffleRemaining = buildShuffleRemaining(
                playbackMode = playbackMode,
                queueSize = queueState.songIds.size,
                currentIndex = queueState.currentIndex,
            ),
        )
    }

    /**
     * 从队列删除歌曲后重新解析 current，并重建随机状态，避免旧索引污染新队列。
     */
    fun removeSong(
        queueState: QueueState,
        removedSongId: String,
        currentSongId: String?,
        nextSongIds: List<String>,
    ): QueueNavigationResult? {
        if (removedSongId !in queueState.songIds || nextSongIds.isEmpty()) {
            return null
        }
        val nextCurrentSongId: String = if (currentSongId == removedSongId) {
            nextSongIds.first()
        } else {
            currentSongId?.takeIf { songId: String -> songId in nextSongIds } ?: nextSongIds.first()
        }
        val nextCurrentIndex: Int = nextSongIds.indexOf(nextCurrentSongId).coerceAtLeast(minimumValue = 0)
        val nextQueueState: QueueState = queueState.copy(
            songIds = nextSongIds,
            currentIndex = nextCurrentIndex,
            shuffleHistory = emptyList(),
            shuffleRemaining = buildShuffleRemaining(
                playbackMode = queueState.playbackMode,
                queueSize = nextSongIds.size,
                currentIndex = nextCurrentIndex,
            ),
        )
        return QueueNavigationResult(
            queueState = nextQueueState,
            targetIndex = nextCurrentIndex,
        )
    }

    /**
     * 判断自动前进是否真的存在不同歌曲目标，避免单曲队列误判成可恢复。
     */
    fun hasDifferentNextTarget(queueState: QueueState): Boolean {
        if (queueState.songIds.size <= 1) {
            return false
        }
        if (queueState.playbackMode == PlaybackMode.LoopOne) {
            return false
        }
        return queueState.songIds.indices.any { index: Int -> index != queueState.currentIndex }
    }

    /**
     * 只有随机模式才需要构建首轮 remaining，其它模式保持空集合更贴近当前协调器语义。
     */
    private fun buildShuffleRemaining(
        playbackMode: PlaybackMode,
        queueSize: Int,
        currentIndex: Int,
    ): List<Int> {
        if (playbackMode != PlaybackMode.Shuffle) {
            return emptyList()
        }
        return shufflePolicy.buildInitialRemaining(
            queueSize = queueSize,
            currentIndex = currentIndex,
        )
    }
}

/**
 * 单次队列导航的纯结果。
 *
 * @property queueState 迁移后的队列状态。
 * @property targetIndex 本次应跳转到的目标下标。
 */
internal data class QueueNavigationResult(
    val queueState: QueueState,
    val targetIndex: Int,
)
