package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.persistence.SearchHistoryDao
import com.yanhao.kmpmusic.domain.persistence.SearchHistoryEntity
import com.yanhao.kmpmusic.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证 [PersistentSearchHistoryRepository] 会按搜索上下文持久化最近搜索词。
 */
class PersistentSearchHistoryRepositoryTest {
    /**
     * 保存历史时必须按上下文隔离，并允许新仓库实例恢复同一份数据。
     */
    @Test
    fun saveSearchHistoryPersistsByContextAcrossRepositoryInstances(): Unit = runTest {
        val dao: FakeSearchHistoryDao = FakeSearchHistoryDao()
        val repository: SearchHistoryRepository = PersistentSearchHistoryRepository(
            searchHistoryDao = dao,
            nowMillis = { 123L },
        )

        repository.saveSearchHistory(
            context = SearchContext.LocalLibrary,
            history = listOf("三", "城市"),
        )
        repository.saveSearchHistory(
            context = SearchContext.Favorites,
            history = listOf("收藏"),
        )

        val restoredRepository: SearchHistoryRepository = PersistentSearchHistoryRepository(
            searchHistoryDao = dao,
        )

        assertEquals(
            expected = listOf("三", "城市"),
            actual = restoredRepository.getSearchHistory(context = SearchContext.LocalLibrary),
        )
        assertEquals(
            expected = listOf("收藏"),
            actual = restoredRepository.getSearchHistory(context = SearchContext.Favorites),
        )
    }

    /**
     * 覆盖保存应删除旧历史，避免清空或缩短列表后残留旧搜索词。
     */
    @Test
    fun saveSearchHistoryReplacesOldContextRows(): Unit = runTest {
        val dao: FakeSearchHistoryDao = FakeSearchHistoryDao()
        val repository: SearchHistoryRepository = PersistentSearchHistoryRepository(
            searchHistoryDao = dao,
            nowMillis = { 456L },
        )

        repository.saveSearchHistory(
            context = SearchContext.LocalLibrary,
            history = listOf("三", "城市"),
        )
        repository.saveSearchHistory(
            context = SearchContext.LocalLibrary,
            history = listOf("周杰伦"),
        )

        assertEquals(
            expected = listOf("周杰伦"),
            actual = repository.getSearchHistory(context = SearchContext.LocalLibrary),
        )
    }

    private class FakeSearchHistoryDao : SearchHistoryDao {
        // 用上下文和搜索词模拟数据库主键。
        private val rows: LinkedHashMap<Pair<String, String>, SearchHistoryEntity> = linkedMapOf()

        /** 按位置读取指定上下文的历史。 */
        override suspend fun getHistory(context: String): List<SearchHistoryEntity> {
            return rows.values
                .filter { entity: SearchHistoryEntity -> entity.context == context }
                .sortedWith(
                    compareBy<SearchHistoryEntity> { entity: SearchHistoryEntity -> entity.position }
                        .thenByDescending { entity: SearchHistoryEntity -> entity.updatedAt },
                )
        }

        /** 批量写入历史。 */
        override suspend fun insertAll(history: List<SearchHistoryEntity>) {
            history.forEach { entity: SearchHistoryEntity ->
                rows[entity.context to entity.query] = entity
            }
        }

        /** 清空指定上下文历史。 */
        override suspend fun clearHistory(context: String) {
            rows.keys
                .filter { key: Pair<String, String> -> key.first == context }
                .forEach { key: Pair<String, String> -> rows.remove(key = key) }
        }
    }
}
