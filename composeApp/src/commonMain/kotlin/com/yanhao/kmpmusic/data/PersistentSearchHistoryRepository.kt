package com.yanhao.kmpmusic.data

import androidx.room3.withWriteTransaction
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.domain.persistence.SearchHistoryDao
import com.yanhao.kmpmusic.domain.persistence.SearchHistoryEntity
import com.yanhao.kmpmusic.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.runBlocking

/**
 * 基于 [SearchHistoryDao] 的搜索历史仓库，实现跨进程恢复和上下文隔离。
 */
class PersistentSearchHistoryRepository(
    private val searchHistoryDao: SearchHistoryDao,
    private val runInWriteTransaction: suspend (suspend () -> Unit) -> Unit = { block -> block() },
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
            runInWriteTransaction {
                searchHistoryDao.clearHistory(context = context.name)
                searchHistoryDao.insertAll(history = history.toEntities(context = context))
            }
        }
    }

    // 将 UI 历史顺序固化为数据库位置，保证读取时能还原最近搜索顺序。
    private fun List<String>.toEntities(context: SearchContext): List<SearchHistoryEntity> {
        return mapIndexed { index: Int, query: String ->
            SearchHistoryEntity(
                context = context.name,
                query = query,
                position = index,
                updatedAt = nowMillis(),
            )
        }
    }

    companion object {
        /**
         * 从 [PlaybackDatabase] 创建仓库，确保覆盖保存时清空与写入在同一 Room 事务内完成。
         */
        fun create(
            playbackDatabase: PlaybackDatabase,
            nowMillis: () -> Long = { currentTimeMillis() },
        ): PersistentSearchHistoryRepository {
            return PersistentSearchHistoryRepository(
                searchHistoryDao = playbackDatabase.searchHistoryDao(),
                runInWriteTransaction = { block: suspend () -> Unit ->
                    playbackDatabase.withWriteTransaction {
                        block()
                    }
                },
                nowMillis = nowMillis,
            )
        }
    }
}
