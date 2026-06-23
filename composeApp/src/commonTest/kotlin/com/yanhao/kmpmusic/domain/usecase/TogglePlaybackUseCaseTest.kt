package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [TogglePlaybackUseCaseImpl] 的状态切换测试，保护显式状态模型的临时语义。
 */
class TogglePlaybackUseCaseTest {
    /**
     * 已播放中的歌曲应能切到暂停态。
     */
    @Test
    fun togglePlaybackPausesWhenPlaying(): Unit {
        val playbackRepository = InMemoryPlaybackRepository()
        playbackRepository.savePlaybackState(
            state = PlaybackState(
                currentSongId = "song-1",
                status = PlaybackStatus.Playing,
            ),
        )
        val useCase = TogglePlaybackUseCaseImpl(playbackRepository = playbackRepository)
        val nextState: PlaybackState = useCase()
        assertEquals(expected = PlaybackStatus.Paused, actual = nextState.status)
        assertEquals(expected = PlaybackStatus.Paused, actual = playbackRepository.getPlaybackState().status)
    }

    /**
     * 尚未具备真实播放控制器前，非 [PlaybackStatus.Playing]/[PlaybackStatus.Paused] 不能被伪装成开始播放。
     */
    @Test
    fun togglePlaybackKeepsNonInteractiveStatusesUnchanged(): Unit {
        val lockedStatuses: List<PlaybackStatus> = listOf(
            PlaybackStatus.Idle,
            PlaybackStatus.Loading,
            PlaybackStatus.Buffering,
            PlaybackStatus.Ended,
            PlaybackStatus.Error,
        )
        lockedStatuses.forEach { status ->
            val playbackRepository = InMemoryPlaybackRepository()
            playbackRepository.savePlaybackState(
                state = PlaybackState(
                    currentSongId = "song-1",
                    status = status,
                ),
            )
            val useCase = TogglePlaybackUseCaseImpl(playbackRepository = playbackRepository)
            val nextState: PlaybackState = useCase()
            assertEquals(expected = status, actual = nextState.status)
            assertEquals(expected = status, actual = playbackRepository.getPlaybackState().status)
        }
    }
}
