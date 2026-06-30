package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.SearchContext

/**
 * 搜索历史仓库，负责按入口上下文隔离并保存最近搜索词。
 */
interface SearchHistoryRepository {
    /**
     * 读取指定上下文的搜索历史。
     */
    fun getSearchHistory(context: SearchContext): List<String>

    /**
     * 覆盖保存指定上下文的搜索历史。
     */
    fun saveSearchHistory(context: SearchContext, history: List<String>)
}
