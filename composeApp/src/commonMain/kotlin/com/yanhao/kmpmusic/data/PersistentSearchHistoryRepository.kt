package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.persistence.SearchHistoryDao
import com.yanhao.kmpmusic.domain.persistence.SearchHistoryEntity
import com.yanhao.kmpmusic.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.runBlocking

/**
 * 基于 [SearchHistoryDao] 的搜索历史仓库，实现跨进程恢复和上下文隔离。
 */
class PersistentSearchHistoryRepository(
    private val searchHistoryDao: SearchHistoryDao,
    private val nowMillis: () -> Long = { currentTimeMillis() },
) : SearchHistoryRepository {
    /** 读取指定上下文的搜索历史。 */
    override fun getSearchHistory(context: SearchContext): List<String> = runBlocking {
        searchHistoryDao.getHistory(context = context.name).map { entity: SearchHistoryEntity ->
            entity.query
        }
    }

    /** 覆盖保存指定上下文的搜索历史，并删除旧的多余记录。 */
    override fun saveSearchHistory(context: SearchContext, history: List<String>) {
        runBlocking {
            searchHistoryDao.clearHistory(context = context.name)
            searchHistoryDao.insertAll(
                history.mapIndexed { index: Int, query: String ->
                    SearchHistoryEntity(
                        context = context.name,
                        query = query,
                        position = index,
                        updatedAt = nowMillis(),
                    )
                },
            )
        }
    }
}
