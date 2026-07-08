package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.SearchScope

/**
 * Figma 搜索结果 tab，其中歌单只保留视觉占位，等待后续领域模型支持。
 */
internal enum class SearchResultTab(
    val label: String,
    val scope: SearchScope?,
) {
    Songs(label = "歌曲", scope = SearchScope.Songs),
    Albums(label = "专辑", scope = SearchScope.Albums),
    Artists(label = "歌手", scope = SearchScope.Artists),
    Playlists(label = "歌单", scope = null),
}

// Figma 节点给出的初始 chips，用于新用户还没有历史时保持首屏视觉完整。
private val searchFallbackHistoryChips: List<String> = listOf(
    "周杰伦",
    "陈奕迅",
    "轻音乐",
    "Lo-fi Beats",
    "流行摇滚",
)

/** 空 query 时只显示探索空态，避免把底层“空词浏览全部”的能力误渲染成搜索结果。 */
internal fun shouldShowSearchResultContent(query: String): Boolean {
    return query.trim().isNotEmpty()
}

/** 将旧的 [SearchScope.All] 兼容到新版 Figma 的默认“歌曲”tab。 */
internal fun visibleSearchResultTab(scope: SearchScope): SearchResultTab {
    return when (scope) {
        SearchScope.All,
        SearchScope.Songs,
        -> SearchResultTab.Songs
        SearchScope.Albums -> SearchResultTab.Albums
        SearchScope.Artists -> SearchResultTab.Artists
    }
}

/** 历史为空时使用 Figma 示例 chips，避免首屏历史区塌陷。 */
internal fun visibleSearchHistoryChips(history: List<String>): List<String> {
    if (history.isNotEmpty()) {
        return history
    }
    return searchFallbackHistoryChips
}
