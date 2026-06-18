package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.repository.FavoritesRepository

/**
 * 收藏状态内存实现，保证所有页面看到同一份收藏结果。
 */
class InMemoryFavoritesRepository(
    initialLikedSongIds: Set<String>,
) : FavoritesRepository {
    // 收藏歌曲集合，第一阶段不落盘。
    private var likedSongIds: Set<String> = initialLikedSongIds

    /** 获取收藏歌曲集合。 */
    override fun getLikedSongIds(): Set<String> = likedSongIds

    /** 切换收藏歌曲并返回更新后的集合。 */
    override fun toggleSong(songId: String): Set<String> {
        likedSongIds = if (likedSongIds.contains(songId)) {
            likedSongIds - songId
        } else {
            likedSongIds + songId
        }
        return likedSongIds
    }
}
