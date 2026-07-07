package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.Song

/**
 * 最近播放歌曲行状态，集中保存当前播放标识，避免摘要页和完整页各自判断出分叉。
 *
 * @property song 行内歌曲。
 * @property isCurrentSong 是否为全局当前播放歌曲。
 */
internal data class RecentPlayedSongRowDisplayModel(
    val song: Song,
    val isCurrentSong: Boolean,
)

/**
 * 为最近播放歌曲列表补充当前播放标识，非当前歌曲必须保持普通视觉状态。
 */
internal fun buildRecentPlayedSongRowDisplayModels(
    songs: List<Song>,
    currentSongId: String?,
): List<RecentPlayedSongRowDisplayModel> {
    return songs.map { song: Song ->
        RecentPlayedSongRowDisplayModel(
            song = song,
            isCurrentSong = song.id == currentSongId,
        )
    }
}
