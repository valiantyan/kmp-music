package com.yanhao.kmpmusic.domain.model

/**
 * 搜索页的数据上下文，用于隔离本地曲库与收藏页的搜索历史和结果来源。
 */
enum class SearchContext {
    LocalLibrary,
    Favorites,
}
