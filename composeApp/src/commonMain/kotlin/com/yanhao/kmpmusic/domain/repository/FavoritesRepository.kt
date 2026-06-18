package com.yanhao.kmpmusic.domain.repository

/**
 * 收藏状态接口，保证歌曲列表、播放页和收藏页使用同一状态来源。
 */
interface FavoritesRepository {
    /**
     * 获取收藏歌曲标识集合。
     */
    fun getLikedSongIds(): Set<String>

    /**
     * 切换单首歌曲收藏状态。
     */
    fun toggleSong(songId: String): Set<String>
}
