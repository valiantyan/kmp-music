package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.persistence.FavoriteSongDao
import com.yanhao.kmpmusic.domain.persistence.FavoriteSongEntity
import com.yanhao.kmpmusic.domain.repository.FavoritesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 验证 [PersistentFavoritesRepository] 会把收藏状态持久化到 DAO，并能被新实例恢复。
 */
class PersistentFavoritesRepositoryTest {
    /**
     * 收藏切换后应写入持久层，后续仓库实例必须从 [FavoriteSongDao] 恢复同一状态。
     */
    @Test
    fun toggleSongPersistsFavoriteSetAcrossRepositoryInstances(): Unit = runTest {
        val dao: FakeFavoriteSongDao = FakeFavoriteSongDao()
        val repository: FavoritesRepository = PersistentFavoritesRepository(
            favoriteSongDao = dao,
            initialLikedSongIds = PersistentFavoritesRepository.loadInitialLikedSongIds(
                favoriteSongDao = dao,
            ),
            nowMillis = { 123L },
        )

        assertTrue(actual = repository.toggleSong(songId = "song-1").contains(element = "song-1"))
        assertEquals(expected = 123L, actual = dao.getFavorite(songId = "song-1")?.updatedAt)

        val restoredRepository: FavoritesRepository = PersistentFavoritesRepository(
            favoriteSongDao = dao,
            initialLikedSongIds = PersistentFavoritesRepository.loadInitialLikedSongIds(
                favoriteSongDao = dao,
            ),
        )

        assertTrue(actual = restoredRepository.getLikedSongIds().contains(element = "song-1"))
        assertFalse(actual = restoredRepository.toggleSong(songId = "song-1").contains(element = "song-1"))
        assertFalse(actual = dao.getFavoriteSongIds().contains(element = "song-1"))
    }

    /**
     * 默认时间提供者必须产生真实时间戳，避免生产环境把收藏更新时间写成 Unix epoch。
     */
    @Test
    fun toggleSongUsesRealClockWhenNoNowMillisIsInjected(): Unit = runTest {
        val dao: FakeFavoriteSongDao = FakeFavoriteSongDao()
        val repository: FavoritesRepository = PersistentFavoritesRepository(
            favoriteSongDao = dao,
            initialLikedSongIds = emptySet(),
        )

        repository.toggleSong(songId = "song-2")

        val savedFavorite: FavoriteSongEntity? = dao.getFavorite(songId = "song-2")
        assertNotNull(actual = savedFavorite)
        assertTrue(actual = savedFavorite.updatedAt > 0L)
    }

    private class FakeFavoriteSongDao : FavoriteSongDao {
        // 用插入顺序模拟数据库中的收藏记录集合，方便断言恢复结果。
        private val rows: LinkedHashMap<String, FavoriteSongEntity> = linkedMapOf()

        /** 返回当前保存的收藏歌曲标识。 */
        override suspend fun getFavoriteSongIds(): List<String> {
            return rows.keys.toList()
        }

        /** 保存或覆盖单首歌曲的收藏记录。 */
        override suspend fun saveFavorite(entity: FavoriteSongEntity) {
            rows[entity.songId] = entity
        }

        /** 删除指定歌曲的收藏记录。 */
        override suspend fun deleteFavorite(songId: String) {
            rows.remove(key = songId)
        }

        /** 返回指定歌曲当前保存的收藏记录，供测试断言更新时间。 */
        fun getFavorite(songId: String): FavoriteSongEntity? {
            return rows[songId]
        }
    }
}
