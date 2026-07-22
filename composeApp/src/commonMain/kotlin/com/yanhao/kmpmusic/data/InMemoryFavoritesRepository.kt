package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.repository.FavoritesRepository

/**
 * 收藏状态内存实现，保证所有页面看到同一份收藏结果。
 */
class InMemoryFavoritesRepository(
    initialLikedSongIds: Collection<String>,
) : FavoritesRepository {
    // 收藏歌曲按最近添加顺序保存，第一阶段不落盘。
    private var likedSongIds: List<String> = initialLikedSongIds.distinct()

    /** 按最近收藏时间从新到旧获取歌曲标识。 */
    override fun getLikedSongIds(): List<String> = likedSongIds

    /** 切换收藏歌曲并把新收藏插入列表首位。 */
    override fun toggleSong(songId: String): List<String> {
        likedSongIds =
            if (likedSongIds.contains(songId)) {
                likedSongIds.filterNot { likedSongId: String -> likedSongId == songId }
            } else {
                listOf(songId) + likedSongIds
            }
        return likedSongIds
    }

    /** 用外部给定的完整集合覆盖当前收藏状态。 */
    override fun replaceLikedSongIds(songIds: Set<String>) {
        likedSongIds = songIds.toList()
    }
}
