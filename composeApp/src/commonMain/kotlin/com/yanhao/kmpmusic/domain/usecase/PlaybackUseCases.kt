package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.domain.model.PlaybackHistory
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
     * 将歌曲设为当前播放，并按需加入队列。
     */
    operator fun invoke(song: Song): PlaybackState
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
    /** 播放歌曲并把新歌曲插入队首，保持和原型一致。 */
    override operator fun invoke(song: Song): PlaybackState {
        val queueState: QueueState = playbackRepository.getQueueState()
        val nextQueueIds: List<String> = if (queueState.songIds.contains(song.id)) {
            queueState.songIds
        } else {
            listOf(song.id) + queueState.songIds
        }
        val nextState: PlaybackState = PlaybackState(
            currentSongId = song.id,
            status = PlaybackStatus.Playing,
        )
        playbackRepository.saveQueueState(
            state = QueueState(
                songIds = nextQueueIds,
                currentIndex = nextQueueIds.indexOf(element = song.id),
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
    /** 仅切换播放布尔状态，不改变当前歌曲。 */
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
    return if (isPlaying) {
        PlaybackStatus.Paused
    } else {
        PlaybackStatus.Playing
    }
}
