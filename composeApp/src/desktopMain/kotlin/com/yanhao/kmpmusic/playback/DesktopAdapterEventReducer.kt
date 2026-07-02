package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent

/**
 * 适配器事件规整结果，供引擎在串行命令循环里统一应用。
 */
internal data class DesktopAdapterEventReduction(
    val events: List<PlaybackEngineEvent> = emptyList(),
    val adapterActions: List<DesktopAdapterAction> = emptyList(),
    val stateUpdates: List<DesktopEngineStateUpdate> = emptyList(),
    val shouldStartProgressTicker: Boolean = false,
    val shouldStopProgressTicker: Boolean = false,
)

/**
 * 规整后交给引擎执行的适配器动作。
 */
internal sealed interface DesktopAdapterAction {
    /**
     * 在 prepared 后兑现当前代待处理的 seek 请求。
     */
    data class SeekTo(
        val generation: Long,
        val positionMs: Long,
    ) : DesktopAdapterAction

    /**
     * 在 prepared 后兑现待播放意图。
     */
    data class Play(val generation: Long) : DesktopAdapterAction

    /**
     * 在 prepared 后兑现待暂停意图。
     */
    data class Pause(val generation: Long) : DesktopAdapterAction
}

/**
 * 规整后交给引擎应用的状态更新。
 */
internal sealed interface DesktopEngineStateUpdate {
    /**
     * 标记当前代已经进入 prepared 可控状态。
     */
    data object MarkPrepared : DesktopEngineStateUpdate

    /**
     * 失败后清空播放态标记，避免旧状态污染下一代。
     */
    data object ResetPlaybackFlags : DesktopEngineStateUpdate

    /**
     * 失败后推进媒体代号，让同代后续回调全部失效。
     */
    data object AdvanceGeneration : DesktopEngineStateUpdate
}

/**
 * 把桌面适配器回调规整成纯结果，保持决策与副作用分离。
 */
internal class DesktopAdapterEventReducer {
    /** 根据快照与事件推导引擎下一步动作，不直接触碰任何运行时依赖。 */
    fun reduce(
        snapshot: DesktopPlaybackEngineSnapshot,
        event: DesktopMediaPlayerEvent,
    ): DesktopAdapterEventReduction {
        if (event.generation != snapshot.generation) {
            return DesktopAdapterEventReduction()
        }
        if (!snapshot.isPrepared &&
            event !is DesktopMediaPlayerEvent.Prepared &&
            event !is DesktopMediaPlayerEvent.Failed
        ) {
            return DesktopAdapterEventReduction()
        }
        return when (event) {
            is DesktopMediaPlayerEvent.Prepared -> reducePrepared(
                snapshot = snapshot,
                event = event,
            )
            is DesktopMediaPlayerEvent.Playing -> reducePlaying(
                snapshot = snapshot,
                event = event,
            )
            is DesktopMediaPlayerEvent.Paused -> reducePaused(
                snapshot = snapshot,
                event = event,
            )
            is DesktopMediaPlayerEvent.Finished -> reduceFinished()
            is DesktopMediaPlayerEvent.Failed -> reduceFailed(event = event)
        }
    }

    /** prepared 只负责兑现待 seek 与待播放控制意图。 */
    private fun reducePrepared(
        snapshot: DesktopPlaybackEngineSnapshot,
        event: DesktopMediaPlayerEvent.Prepared,
    ): DesktopAdapterEventReduction {
        val currentMediaDurationMs: Long = snapshot.currentMedia()?.durationMs ?: return DesktopAdapterEventReduction()
        val adapterActions: MutableList<DesktopAdapterAction> = mutableListOf()
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val seekMs: Long = snapshot.pendingSeekMs ?: 0L
        if (seekMs > 0L) {
            adapterActions += DesktopAdapterAction.SeekTo(
                generation = snapshot.generation,
                positionMs = seekMs,
            )
            events += PlaybackEngineEvent.ProgressChanged(
                positionMs = seekMs,
                durationMs = event.durationMs ?: currentMediaDurationMs,
            )
        }
        when (snapshot.playbackControlIntent) {
            DesktopPlaybackControlIntent.Play -> {
                adapterActions += DesktopAdapterAction.Play(generation = snapshot.generation)
            }
            DesktopPlaybackControlIntent.Pause -> {
                adapterActions += DesktopAdapterAction.Pause(generation = snapshot.generation)
            }
            DesktopPlaybackControlIntent.None -> Unit
        }
        return DesktopAdapterEventReduction(
            events = events,
            adapterActions = adapterActions,
            stateUpdates = listOf(DesktopEngineStateUpdate.MarkPrepared),
        )
    }

    /** 播放回调只在没有暂停意图时推进 UI 和进度轮询。 */
    private fun reducePlaying(
        snapshot: DesktopPlaybackEngineSnapshot,
        event: DesktopMediaPlayerEvent.Playing,
    ): DesktopAdapterEventReduction {
        if (snapshot.playbackControlIntent == DesktopPlaybackControlIntent.Pause) {
            return DesktopAdapterEventReduction()
        }
        return DesktopAdapterEventReduction(
            events = listOf(
                PlaybackEngineEvent.StatusChanged(
                    status = PlaybackStatus.Playing,
                    positionMs = event.positionMs,
                    durationMs = event.durationMs,
                ),
            ),
            shouldStartProgressTicker = true,
        )
    }

    /** 暂停回调只在明确暂停意图下生效，避免杂散回调覆盖真实播放态。 */
    private fun reducePaused(
        snapshot: DesktopPlaybackEngineSnapshot,
        event: DesktopMediaPlayerEvent.Paused,
    ): DesktopAdapterEventReduction {
        if (snapshot.playbackControlIntent != DesktopPlaybackControlIntent.Pause) {
            return DesktopAdapterEventReduction()
        }
        return DesktopAdapterEventReduction(
            events = listOf(
                PlaybackEngineEvent.StatusChanged(
                    status = PlaybackStatus.Paused,
                    positionMs = event.positionMs,
                    durationMs = event.durationMs,
                ),
            ),
            shouldStopProgressTicker = true,
        )
    }

    /** 自然结束后停止轮询，并把是否续播交回上层协调器。 */
    private fun reduceFinished(): DesktopAdapterEventReduction {
        return DesktopAdapterEventReduction(
            events = listOf(PlaybackEngineEvent.Ended),
            shouldStopProgressTicker = true,
        )
    }

    /** 失败后让当前代立刻失效，避免同代旧回调继续推进状态机。 */
    private fun reduceFailed(event: DesktopMediaPlayerEvent.Failed): DesktopAdapterEventReduction {
        return DesktopAdapterEventReduction(
            events = listOf(PlaybackEngineEvent.Failed(error = event.error)),
            stateUpdates = listOf(
                DesktopEngineStateUpdate.AdvanceGeneration,
                DesktopEngineStateUpdate.ResetPlaybackFlags,
            ),
            shouldStopProgressTicker = true,
        )
    }
}
