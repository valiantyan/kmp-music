package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository

/**
 * 播放指定歌曲接口。
 */
interface PlaySongUseCase {
    /**
     * 将歌曲设为当前播放，并把当前列表显式写成播放队列。
     */
    operator fun invoke(song: Song, queueSongs: List<Song> = listOf(song)): PlaybackState
}

/**
 * 切换播放暂停接口。
 */
interface TogglePlaybackUseCase {
    /**
     * 切换当前播放状态。
     */
    operator fun invoke(): PlaybackState
}

/**
 * 上一首/下一首接口。
 */
interface MoveQueueUseCase {
    /**
     * 按方向移动已有队列，队列为空或当前歌曲缺失时保持原状态。
     */
    operator fun invoke(direction: Int): PlaybackState
}

/**
 * 播放指定歌曲实现。
 */
class PlaySongUseCaseImpl(
    private val playbackRepository: PlaybackRepository,
) : PlaySongUseCase {
    /** 播放歌曲时优先使用当前列表生成显式队列，避免继续沿用过期队列。 */
    override operator fun invoke(song: Song, queueSongs: List<Song>): PlaybackState {
        val queueState: QueueState = playbackRepository.getQueueState()
        val currentPlaybackMode: PlaybackMode = queueState.playbackMode
        val matchingQueueSongs: List<Song> = queueSongs.takeIf { songs: List<Song> ->
            songs.any { candidate: Song -> candidate.id == song.id }
        } ?: listOf(song)
        val nextQueueIds: List<String> = matchingQueueSongs.map { queueSong: Song -> queueSong.id }
        val nextIndex: Int = nextQueueIds.indexOf(element = song.id).takeIf { index: Int ->
            index >= 0
        } ?: 0
        val nextState: PlaybackState = PlaybackState(
            currentSongId = song.id,
            status = PlaybackStatus.Playing,
            durationMs = song.durationMs,
        )
        playbackRepository.saveQueueState(
            state = QueueState(
                songIds = nextQueueIds,
                currentIndex = nextIndex,
                playbackMode = currentPlaybackMode,
            ),
        )
        playbackRepository.savePlaybackState(state = nextState)
        val currentHistory: List<String> = playbackRepository.getPlaybackHistory().songIds
        val nextHistory: List<String> = listOf(song.id) + currentHistory.filterNot { songId ->
            songId == song.id
        }
        playbackRepository.savePlaybackHistory(
            history = PlaybackHistory(songIds = nextHistory.take(n = 50)),
        )
        return nextState
    }
}

/**
 * 切换播放暂停实现。
 */
class TogglePlaybackUseCaseImpl(
    private val playbackRepository: PlaybackRepository,
) : TogglePlaybackUseCase {
    /** 仅在显式可切换的播放态之间转换，避免为其他状态伪造播放开始。 */
    override operator fun invoke(): PlaybackState {
        val currentState: PlaybackState = playbackRepository.getPlaybackState()
        val nextState: PlaybackState = currentState.copy(
            status = currentState.toggledStatus(),
        )
        playbackRepository.savePlaybackState(state = nextState)
        return nextState
    }
}

/**
 * 队列切歌实现。
 */
class MoveQueueUseCaseImpl(
    private val playbackRepository: PlaybackRepository,
) : MoveQueueUseCase {
    /** 基于显式队列循环切歌，不用全曲库静默替换缺失歌曲。 */
    override operator fun invoke(direction: Int): PlaybackState {
        val currentState: PlaybackState = playbackRepository.getPlaybackState()
        val queueIds: List<String> = playbackRepository.getQueueState().songIds
        if (queueIds.isEmpty()) {
            return currentState
        }
        val currentSongId: String = currentState.currentSongId ?: return currentState
        val currentIndex: Int = queueIds.indexOf(currentSongId)
        if (currentIndex < 0) {
            return currentState
        }
        val nextIndex: Int = (currentIndex + direction + queueIds.size) % queueIds.size
        val nextState: PlaybackState = PlaybackState(
            currentSongId = queueIds[nextIndex],
            status = PlaybackStatus.Playing,
        )
        playbackRepository.saveQueueState(
            state = playbackRepository.getQueueState().copy(
                currentIndex = nextIndex,
            ),
        )
        playbackRepository.savePlaybackState(state = nextState)
        return nextState
    }
}

// 在旧控制器仍使用 [isPlaying] 的阶段，用显式状态兼容切换语义。
private fun PlaybackState.toggledStatus(): PlaybackStatus {
    return when (status) {
        PlaybackStatus.Playing -> PlaybackStatus.Paused
        PlaybackStatus.Paused -> PlaybackStatus.Playing
        PlaybackStatus.Idle,
        PlaybackStatus.Loading,
        PlaybackStatus.Buffering,
        PlaybackStatus.Ended,
        PlaybackStatus.Error,
        -> status
    }
}
