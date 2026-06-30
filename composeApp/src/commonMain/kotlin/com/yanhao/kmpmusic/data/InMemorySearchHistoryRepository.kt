package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.repository.SearchHistoryRepository

/**
 * 搜索历史内存实现，供共享测试和未接入持久化的平台使用。
 */
class InMemorySearchHistoryRepository : SearchHistoryRepository {
    // 本地曲库搜索历史。
    private var localLibraryHistory: List<String> = emptyList()

    // 收藏页搜索历史。
    private var favoritesHistory: List<String> = emptyList()

    /** 读取指定上下文的搜索历史。 */
    override fun getSearchHistory(context: SearchContext): List<String> {
        return when (context) {
            SearchContext.LocalLibrary -> localLibraryHistory
            SearchContext.Favorites -> favoritesHistory
        }
    }

    /** 覆盖保存指定上下文的搜索历史。 */
    override fun saveSearchHistory(context: SearchContext, history: List<String>) {
        when (context) {
            SearchContext.LocalLibrary -> localLibraryHistory = history
            SearchContext.Favorites -> favoritesHistory = history
        }
    }
}
