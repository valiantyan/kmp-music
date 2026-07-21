package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.playback.PlaybackEngineEvent

/**
 * Apple bridge 事件规整结果，供引擎在串行命令循环里统一应用。
 */
internal data class ApplePlaybackBridgeEventReduction(
    val events: List<PlaybackEngineEvent> = emptyList(),
    val bridgeActions: List<ApplePlaybackBridgeAction> = emptyList(),
    val stateUpdates: List<ApplePlaybackEngineStateUpdate> = emptyList(),
)

/**
 * 规整后交给引擎执行的 Apple bridge 动作。
 */
internal sealed interface ApplePlaybackBridgeAction {
    /** 在 prepared 后兑现当前代待处理的 seek 请求。 */
    data class SeekTo(
        val generation: Long,
        val positionMs: Long,
    ) : ApplePlaybackBridgeAction

    /** 在 prepared 后兑现待播放意图。 */
    data class Play(
        val generation: Long,
    ) : ApplePlaybackBridgeAction

    /** 在 prepared 后兑现待暂停意图。 */
    data class Pause(
        val generation: Long,
    ) : ApplePlaybackBridgeAction
}

/**
 * 规整后交给引擎应用的状态更新。
 */
internal sealed interface ApplePlaybackEngineStateUpdate {
    /** 标记当前代已经进入 prepared 可控状态。 */
    data object MarkPrepared : ApplePlaybackEngineStateUpdate

    /** 失败后清空播放态标记，避免旧状态污染下一代。 */
    data object ResetPlaybackFlags : ApplePlaybackEngineStateUpdate

    /** 失败后推进媒体代号，让同代后续回调全部失效。 */
    data object AdvanceGeneration : ApplePlaybackEngineStateUpdate
}

/**
 * 把 Apple bridge 回调规整成纯结果，保持 native 回调与共享播放事实解耦。
 */
internal class ApplePlaybackBridgeEventReducer {
    /** 根据快照与事件推导引擎下一步动作，不直接触碰任何运行时依赖。 */
    fun reduce(
        snapshot: DesktopPlaybackEngineSnapshot,
        event: ApplePlaybackBridgeEvent,
    ): ApplePlaybackBridgeEventReduction {
        if (event is ApplePlaybackBridgeEvent.InitializationFailed) {
            return reduceInitializationFailed(event = event)
        }
        val generation: Long = generationOf(event = event) ?: return ApplePlaybackBridgeEventReduction()
        if (generation != snapshot.generation) {
            return ApplePlaybackBridgeEventReduction()
        }
        if (!snapshot.isPrepared &&
            event !is ApplePlaybackBridgeEvent.Prepared &&
            event !is ApplePlaybackBridgeEvent.Failed
        ) {
            return ApplePlaybackBridgeEventReduction()
        }
        return when (event) {
            is ApplePlaybackBridgeEvent.Prepared -> reducePrepared(snapshot = snapshot, event = event)
            is ApplePlaybackBridgeEvent.Buffering -> reduceBuffering(snapshot = snapshot, event = event)
            is ApplePlaybackBridgeEvent.Playing -> reducePlaying(snapshot = snapshot, event = event)
            is ApplePlaybackBridgeEvent.Paused -> reducePaused(snapshot = snapshot, event = event)
            is ApplePlaybackBridgeEvent.Progress -> reduceProgress(event = event)
            is ApplePlaybackBridgeEvent.Ended -> reduceEnded()
            is ApplePlaybackBridgeEvent.Failed -> reduceFailed(event = event)
            is ApplePlaybackBridgeEvent.InitializationFailed -> reduceInitializationFailed(event = event)
        }
    }

    /** prepared 只负责兑现待 seek 与待播放控制意图。 */
    private fun reducePrepared(
        snapshot: DesktopPlaybackEngineSnapshot,
        event: ApplePlaybackBridgeEvent.Prepared,
    ): ApplePlaybackBridgeEventReduction {
        val fallbackDurationMs: Long? = snapshot.currentMedia()?.durationMs ?: return ApplePlaybackBridgeEventReduction()
        val bridgeActions: MutableList<ApplePlaybackBridgeAction> = mutableListOf()
        val events: MutableList<PlaybackEngineEvent> = mutableListOf()
        val seekMs: Long = snapshot.pendingSeekMs ?: 0L
        if (seekMs > 0L) {
            bridgeActions +=
                ApplePlaybackBridgeAction.SeekTo(
                    generation = snapshot.generation,
                    positionMs = seekMs,
                )
            events +=
                PlaybackEngineEvent.ProgressChanged(
                    positionMs = seekMs,
                    durationMs = event.durationMs ?: fallbackDurationMs,
                )
        }
        when (snapshot.playbackControlIntent) {
            DesktopPlaybackControlIntent.Play -> {
                bridgeActions += ApplePlaybackBridgeAction.Play(generation = snapshot.generation)
            }

            DesktopPlaybackControlIntent.Pause -> {
                bridgeActions += ApplePlaybackBridgeAction.Pause(generation = snapshot.generation)
            }

            DesktopPlaybackControlIntent.None -> {
                Unit
            }
        }
        return ApplePlaybackBridgeEventReduction(
            events = events,
            bridgeActions = bridgeActions,
            stateUpdates = listOf(ApplePlaybackEngineStateUpdate.MarkPrepared),
        )
    }

    /** 缓冲回调只在没有暂停意图时推进 UI 状态。 */
    private fun reduceBuffering(
        snapshot: DesktopPlaybackEngineSnapshot,
        event: ApplePlaybackBridgeEvent.Buffering,
    ): ApplePlaybackBridgeEventReduction {
        if (snapshot.playbackControlIntent == DesktopPlaybackControlIntent.Pause) {
            return ApplePlaybackBridgeEventReduction()
        }
        return ApplePlaybackBridgeEventReduction(
            events =
                listOf(
                    PlaybackEngineEvent.StatusChanged(
                        status = PlaybackStatus.Buffering,
                        positionMs = event.positionMs,
                        durationMs = event.durationMs,
                    ),
                ),
        )
    }

    /** 播放回调只在没有暂停意图时推进 UI 状态。 */
    private fun reducePlaying(
        snapshot: DesktopPlaybackEngineSnapshot,
        event: ApplePlaybackBridgeEvent.Playing,
    ): ApplePlaybackBridgeEventReduction {
        if (snapshot.playbackControlIntent == DesktopPlaybackControlIntent.Pause) {
            return ApplePlaybackBridgeEventReduction()
        }
        return ApplePlaybackBridgeEventReduction(
            events =
                listOf(
                    PlaybackEngineEvent.StatusChanged(
                        status = PlaybackStatus.Playing,
                        positionMs = event.positionMs,
                        durationMs = event.durationMs,
                    ),
                ),
        )
    }

    /** 暂停回调只在明确暂停意图下生效，避免杂散回调覆盖真实播放态。 */
    private fun reducePaused(
        snapshot: DesktopPlaybackEngineSnapshot,
        event: ApplePlaybackBridgeEvent.Paused,
    ): ApplePlaybackBridgeEventReduction {
        if (snapshot.playbackControlIntent != DesktopPlaybackControlIntent.Pause) {
            return ApplePlaybackBridgeEventReduction()
        }
        return ApplePlaybackBridgeEventReduction(
            events =
                listOf(
                    PlaybackEngineEvent.StatusChanged(
                        status = PlaybackStatus.Paused,
                        positionMs = event.positionMs,
                        durationMs = event.durationMs,
                    ),
                ),
        )
    }

    /** 进度回调只更新共享进度事实，不改写播放状态。 */
    private fun reduceProgress(event: ApplePlaybackBridgeEvent.Progress): ApplePlaybackBridgeEventReduction =
        ApplePlaybackBridgeEventReduction(
            events =
                listOf(
                    PlaybackEngineEvent.ProgressChanged(
                        positionMs = event.positionMs,
                        durationMs = event.durationMs,
                    ),
                ),
        )

    /** 自然结束后把推进规则交回 [PlaybackCoordinator]。 */
    private fun reduceEnded(): ApplePlaybackBridgeEventReduction = ApplePlaybackBridgeEventReduction(events = listOf(PlaybackEngineEvent.Ended))

    /** 失败后让当前代立刻失效，避免同代旧回调继续推进状态机。 */
    private fun reduceFailed(event: ApplePlaybackBridgeEvent.Failed): ApplePlaybackBridgeEventReduction =
        ApplePlaybackBridgeEventReduction(
            events = listOf(PlaybackEngineEvent.Failed(error = event.error)),
            stateUpdates =
                listOf(
                    ApplePlaybackEngineStateUpdate.AdvanceGeneration,
                    ApplePlaybackEngineStateUpdate.ResetPlaybackFlags,
                ),
        )

    /** 初始化失败不归因到具体媒体，但仍统一进入共享失败策略。 */
    private fun reduceInitializationFailed(
        event: ApplePlaybackBridgeEvent.InitializationFailed,
    ): ApplePlaybackBridgeEventReduction =
        ApplePlaybackBridgeEventReduction(
            events = listOf(PlaybackEngineEvent.Failed(error = event.error)),
            stateUpdates =
                listOf(
                    ApplePlaybackEngineStateUpdate.AdvanceGeneration,
                    ApplePlaybackEngineStateUpdate.ResetPlaybackFlags,
                ),
        )

    /** 读取带 generation 事件的代号，初始化失败由调用方单独处理。 */
    private fun generationOf(event: ApplePlaybackBridgeEvent): Long? =
        when (event) {
            is ApplePlaybackBridgeEvent.Prepared -> event.generation
            is ApplePlaybackBridgeEvent.Buffering -> event.generation
            is ApplePlaybackBridgeEvent.Playing -> event.generation
            is ApplePlaybackBridgeEvent.Paused -> event.generation
            is ApplePlaybackBridgeEvent.Progress -> event.generation
            is ApplePlaybackBridgeEvent.Ended -> event.generation
            is ApplePlaybackBridgeEvent.Failed -> event.generation
            is ApplePlaybackBridgeEvent.InitializationFailed -> null
        }
}
