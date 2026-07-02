package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlayableMedia

/**
 * 桌面播放引擎的可变状态容器，集中管理当前队列与播放代际。
 */
internal class DesktopPlaybackEngineState {
    // 当前引擎持有的播放队列。
    var queue: List<PlayableMedia> = emptyList()

    // 当前激活的队列下标，没有活动媒体时为 -1。
    var currentIndex: Int = -1

    // 当前媒体代号，每次切歌/停止/释放都会递增以屏蔽旧回调。
    var generation: Long = 0L
        private set

    // 最近一次明确播放控制意图，用来过滤换媒体期间的杂散状态回调。
    var playbackControlIntent: DesktopPlaybackControlIntent = DesktopPlaybackControlIntent.None

    // 当前代尚未兑现的 seek 请求，遵循 latest-wins 规则。
    var pendingSeekMs: Long? = null

    // 当前 generation 是否已经进入 prepared 可控状态。
    var isPrepared: Boolean = false

    /** 判断当前下标是否仍指向有效媒体，避免访问已失效队列。 */
    fun isCurrentIndexValid(): Boolean {
        return currentIndex in queue.indices
    }

    /** 读取当前媒体，供命令处理逻辑复用统一访问入口。 */
    fun currentMedia(): PlayableMedia? {
        return queue.getOrNull(index = currentIndex)
    }

    /** 生成状态快照，便于需要原子视图的逻辑读取同一时刻状态。 */
    fun snapshot(): DesktopPlaybackEngineSnapshot {
        return DesktopPlaybackEngineSnapshot(
            queue = queue,
            currentIndex = currentIndex,
            generation = generation,
            playbackControlIntent = playbackControlIntent,
            pendingSeekMs = pendingSeekMs,
            isPrepared = isPrepared,
        )
    }

    /** 递增媒体代号，用最简单的方式让旧回调天然失效。 */
    fun nextGeneration(): Long {
        generation += 1L
        return generation
    }

    /** 新队列入场时重置上一代的控制意图与准备态，避免旧状态泄漏。 */
    fun resetForNewQueue(items: List<PlayableMedia>) {
        queue = items
        playbackControlIntent = DesktopPlaybackControlIntent.None
        pendingSeekMs = null
        isPrepared = false
    }

    /** 停止、失败或释放时清空播放态标记，确保后续回调不会继续推进状态。 */
    fun resetPlaybackFlags() {
        playbackControlIntent = DesktopPlaybackControlIntent.None
        pendingSeekMs = null
        isPrepared = false
    }
}

/**
 * 桌面播放引擎状态快照，供需要一致读取多项字段的场景使用。
 */
internal data class DesktopPlaybackEngineSnapshot(
    val queue: List<PlayableMedia>,
    val currentIndex: Int,
    val generation: Long,
    val playbackControlIntent: DesktopPlaybackControlIntent,
    val pendingSeekMs: Long?,
    val isPrepared: Boolean,
) {
    /** 判断快照中的当前下标是否仍然有效。 */
    fun isCurrentIndexValid(): Boolean {
        return currentIndex in queue.indices
    }

    /** 读取快照对应的当前媒体，避免多次手写越界保护。 */
    fun currentMedia(): PlayableMedia? {
        return queue.getOrNull(index = currentIndex)
    }
}
