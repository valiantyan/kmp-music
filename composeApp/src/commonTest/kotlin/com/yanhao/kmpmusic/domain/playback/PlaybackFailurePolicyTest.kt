package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证 [PlaybackFailurePolicy] 对失败次数窗口和恢复决策的纯规则。
 */
class PlaybackFailurePolicyTest {
    /**
     * 单曲循环允许同一首歌重试两次，第三次失败后必须停留在错误态。
     */
    @Test
    fun loopOneRetriesSameSongUntilThirdFailure(): Unit {
        val policy: PlaybackFailurePolicy = PlaybackFailurePolicy()
        val error: PlaybackError = playbackError(songId = "a")

        assertEquals(
            expected = PlaybackFailureDecision.RetryCurrent,
            actual = policy.onFailure(
                error = error,
                playbackMode = PlaybackMode.LoopOne,
                hasRecoverableTarget = true,
            ),
        )
        assertEquals(
            expected = PlaybackFailureDecision.RetryCurrent,
            actual = policy.onFailure(
                error = error,
                playbackMode = PlaybackMode.LoopOne,
                hasRecoverableTarget = true,
            ),
        )
        assertEquals(
            expected = PlaybackFailureDecision.StayError,
            actual = policy.onFailure(
                error = error,
                playbackMode = PlaybackMode.LoopOne,
                hasRecoverableTarget = true,
            ),
        )
    }

    /**
     * 单曲循环切到新歌后，失败窗口必须按歌曲重新计数。
     */
    @Test
    fun loopOneNewSongStartsOwnFailureWindow(): Unit {
        val policy: PlaybackFailurePolicy = PlaybackFailurePolicy()

        policy.onFailure(
            error = playbackError(songId = "a"),
            playbackMode = PlaybackMode.LoopOne,
            hasRecoverableTarget = true,
        )

        assertEquals(
            expected = PlaybackFailureDecision.RetryCurrent,
            actual = policy.onFailure(
                error = playbackError(songId = "b"),
                playbackMode = PlaybackMode.LoopOne,
                hasRecoverableTarget = true,
            ),
        )
    }

    /**
     * 非单曲循环应连续跳过前两首坏歌，第三首继续失败时再停止恢复。
     */
    @Test
    fun nonLoopOneSkipsUntilThirdConsecutiveFailure(): Unit {
        val policy: PlaybackFailurePolicy = PlaybackFailurePolicy()

        assertEquals(
            expected = PlaybackFailureDecision.SkipToNext,
            actual = policy.onFailure(
                error = playbackError(songId = "a"),
                playbackMode = PlaybackMode.LoopAll,
                hasRecoverableTarget = true,
            ),
        )
        assertEquals(
            expected = PlaybackFailureDecision.SkipToNext,
            actual = policy.onFailure(
                error = playbackError(songId = "b"),
                playbackMode = PlaybackMode.LoopAll,
                hasRecoverableTarget = true,
            ),
        )
        assertEquals(
            expected = PlaybackFailureDecision.StayError,
            actual = policy.onFailure(
                error = playbackError(songId = "c"),
                playbackMode = PlaybackMode.LoopAll,
                hasRecoverableTarget = true,
            ),
        )
    }

    /**
     * 非单曲循环如果根本没有可恢复目标，就不应假装还能继续跳歌。
     */
    @Test
    fun nonLoopOneStaysErrorWhenNoNextSongExists(): Unit {
        val policy: PlaybackFailurePolicy = PlaybackFailurePolicy()

        assertEquals(
            expected = PlaybackFailureDecision.StayError,
            actual = policy.onFailure(
                error = playbackError(songId = "a"),
                playbackMode = PlaybackMode.LoopAll,
                hasRecoverableTarget = false,
            ),
        )
    }

    /**
     * 成功恢复后重置策略，避免旧失败窗口污染后续歌曲。
     */
    @Test
    fun resetAfterSuccessfulPlayingClearsCounters(): Unit {
        val policy: PlaybackFailurePolicy = PlaybackFailurePolicy()

        policy.onFailure(
            error = playbackError(songId = "a"),
            playbackMode = PlaybackMode.LoopAll,
            hasRecoverableTarget = true,
        )
        policy.onFailure(
            error = playbackError(songId = "b"),
            playbackMode = PlaybackMode.LoopAll,
            hasRecoverableTarget = true,
        )
        policy.reset()

        assertEquals(
            expected = PlaybackFailureDecision.SkipToNext,
            actual = policy.onFailure(
                error = playbackError(songId = "c"),
                playbackMode = PlaybackMode.LoopAll,
                hasRecoverableTarget = true,
            ),
        )
    }

    /**
     * 构造最小失败对象，专注验证恢复决策而不是错误来源细节。
     */
    private fun playbackError(songId: String): PlaybackError {
        return PlaybackError(
            type = PlaybackErrorType.Unknown,
            songId = songId,
            message = "坏文件",
        )
    }
}
