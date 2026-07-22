package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.persistence.FavoriteSongDao
import com.yanhao.kmpmusic.domain.persistence.FavoriteSongEntity
import com.yanhao.kmpmusic.domain.repository.FavoritesRepository
import kotlinx.coroutines.runBlocking

/**
 * 基于 [FavoriteSongDao] 的收藏仓库实现，保证收藏状态既能立刻返回，也能跨进程恢复。
 */
class PersistentFavoritesRepository(
    private val favoriteSongDao: FavoriteSongDao,
    initialLikedSongIds: Collection<String>,
    private val nowMillis: () -> Long = { currentTimeMillis() },
) : FavoritesRepository {
    // 当前内存中的收藏按最近添加顺序保存，供 UI 同步读取最新结果。
    private var likedSongIds: List<String> = initialLikedSongIds.distinct()

    /** 按最近收藏时间从新到旧返回歌曲标识。 */
    override fun getLikedSongIds(): List<String> = likedSongIds

    /** 切换单首歌曲收藏状态，并把新收藏同步插入列表首位。 */
    override fun toggleSong(songId: String): List<String> {
        likedSongIds =
            if (likedSongIds.contains(element = songId)) {
                runBlocking {
                    favoriteSongDao.deleteFavorite(songId = songId)
                }
                likedSongIds.filterNot { likedSongId: String -> likedSongId == songId }
            } else {
                runBlocking {
                    favoriteSongDao.saveFavorite(
                        entity =
                            FavoriteSongEntity(
                                songId = songId,
                                updatedAt = nowMillis(),
                            ),
                    )
                }
                listOf(songId) + likedSongIds
            }
        return likedSongIds
    }

    /** 用完整集合覆盖收藏状态，并把集合差异同步到 [favoriteSongDao]。 */
    override fun replaceLikedSongIds(songIds: Set<String>) {
        val currentLikedSongIds: Set<String> = likedSongIds.toSet()
        val songIdsToDelete: Set<String> = currentLikedSongIds - songIds
        val songIdsToSave: Set<String> = songIds - currentLikedSongIds
        runBlocking {
            songIdsToDelete.forEach { songId: String ->
                favoriteSongDao.deleteFavorite(songId = songId)
            }
            songIdsToSave.forEach { songId: String ->
                favoriteSongDao.saveFavorite(
                    entity =
                        FavoriteSongEntity(
                            songId = songId,
                            updatedAt = nowMillis(),
                        ),
                )
            }
        }
        likedSongIds = songIds.toList()
    }

    companion object {
        /**
         * 从 [favoriteSongDao] 按最近收藏顺序读取歌曲标识，供仓库初始化时恢复状态。
         */
        suspend fun loadInitialLikedSongIds(favoriteSongDao: FavoriteSongDao): List<String> = favoriteSongDao.getFavoriteSongIds()
    }
}
