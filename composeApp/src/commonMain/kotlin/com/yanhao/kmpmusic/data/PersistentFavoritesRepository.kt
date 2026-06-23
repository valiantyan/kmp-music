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
    initialLikedSongIds: Set<String>,
    private val nowMillis: () -> Long = { 0L },
) : FavoritesRepository {
    // 当前内存中的收藏集合，供 UI 同步读取最新结果。
    private var likedSongIds: Set<String> = initialLikedSongIds

    /** 返回当前收藏歌曲集合。 */
    override fun getLikedSongIds(): Set<String> = likedSongIds

    /** 切换单首歌曲收藏状态，并把结果同步写入 [favoriteSongDao]。 */
    override fun toggleSong(songId: String): Set<String> {
        likedSongIds = if (likedSongIds.contains(element = songId)) {
            runBlocking {
                favoriteSongDao.deleteFavorite(songId = songId)
            }
            likedSongIds - songId
        } else {
            runBlocking {
                favoriteSongDao.saveFavorite(
                    entity = FavoriteSongEntity(
                        songId = songId,
                        updatedAt = nowMillis(),
                    ),
                )
            }
            likedSongIds + songId
        }
        return likedSongIds
    }

    /** 用完整集合覆盖收藏状态，并把集合差异同步到 [favoriteSongDao]。 */
    override fun replaceLikedSongIds(songIds: Set<String>) {
        val songIdsToDelete: Set<String> = likedSongIds - songIds
        val songIdsToSave: Set<String> = songIds - likedSongIds
        runBlocking {
            songIdsToDelete.forEach { songId: String ->
                favoriteSongDao.deleteFavorite(songId = songId)
            }
            songIdsToSave.forEach { songId: String ->
                favoriteSongDao.saveFavorite(
                    entity = FavoriteSongEntity(
                        songId = songId,
                        updatedAt = nowMillis(),
                    ),
                )
            }
        }
        likedSongIds = songIds
    }

    companion object {
        /**
         * 从 [favoriteSongDao] 读取已保存的收藏集合，供仓库初始化时恢复状态。
         */
        suspend fun loadInitialLikedSongIds(favoriteSongDao: FavoriteSongDao): Set<String> {
            return favoriteSongDao.getFavoriteSongIds().toSet()
        }
    }
}
