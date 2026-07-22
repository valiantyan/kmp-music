package com.yanhao.kmpmusic.domain.repository

/**
 * 收藏状态接口，保证歌曲列表、播放页和收藏页使用同一状态来源。
 */
interface FavoritesRepository {
    /**
     * 按最近收藏时间从新到旧获取歌曲标识。
     */
    fun getLikedSongIds(): List<String>

    /**
     * 切换单首歌曲收藏状态，并按最近收藏时间从新到旧返回歌曲标识。
     */
    fun toggleSong(songId: String): List<String>

    /**
     * 用完整集合覆盖当前收藏状态，供外部恢复或同步收藏结果。
     */
    fun replaceLikedSongIds(songIds: Set<String>)
}
