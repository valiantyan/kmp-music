package com.yanhao.kmpmusic.domain.usecase

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
     * 按方向移动队列，`direction` 为 1 表示下一首，-1 表示上一首。
     */
    operator fun invoke(direction: Int, fallbackSongs: List<Song>): PlaybackState
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
            isPlaying = true,
        )
        playbackRepository.saveQueueState(state = QueueState(songIds = nextQueueIds))
        playbackRepository.savePlaybackState(state = nextState)
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
        val nextState: PlaybackState = currentState.copy(isPlaying = !currentState.isPlaying)
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
    /** 基于队列循环切歌，队列为空时退回全量歌曲。 */
    override operator fun invoke(direction: Int, fallbackSongs: List<Song>): PlaybackState {
        val queueIds: List<String> = playbackRepository.getQueueState().songIds
        val playbackSource: List<String> = if (queueIds.isEmpty()) {
            fallbackSongs.map { song -> song.id }
        } else {
            queueIds
        }
        if (playbackSource.isEmpty()) {
            return playbackRepository.getPlaybackState()
        }
        val currentState: PlaybackState = playbackRepository.getPlaybackState()
        val currentIndex: Int = playbackSource.indexOf(currentState.currentSongId).let { index ->
            if (index >= 0) index else 0
        }
        val nextIndex: Int = (currentIndex + direction + playbackSource.size) % playbackSource.size
        val nextState: PlaybackState = PlaybackState(
            currentSongId = playbackSource[nextIndex],
            isPlaying = true,
        )
        playbackRepository.savePlaybackState(state = nextState)
        return nextState
    }
}
