package com.yanhao.kmpmusic.feature.app.search

import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.domain.usecase.buildSearchResult
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 搜索结果派生器，按搜索上下文选择数据源并避免 pending query 泄漏全量曲库。
 */
internal class SearchResultController(
    // 搜索结果在本地库上下文需要兜底读取完整曲库。
    private val musicLibraryRepository: MusicLibraryRepository,
) {
    /**
     * 按当前输入态、active query 和搜索范围派生结果。
     */
    fun search(state: MusicAppUiState): SearchResult {
        if (!shouldResolveCurrentSearchResult(state = state)) {
            return emptySearchResult()
        }
        return buildSearchResult(
            query = state.activeSearchQuery,
            scope = state.searchScope,
            allSongs = searchSourceSongs(state = state),
        )
    }

    /**
     * 只有防抖词追平当前输入后才允许派生结果，避免把 pending 输入误当成全量搜索。
     */
    private fun shouldResolveCurrentSearchResult(state: MusicAppUiState): Boolean {
        val normalizedQuery: String = state.searchQuery.trim()
        val normalizedActiveQuery: String = state.activeSearchQuery.trim()
        return normalizedQuery.isNotEmpty() && normalizedQuery == normalizedActiveQuery
    }

    /**
     * pending 或空搜索统一返回空结果，避免 UI 消费不该出现的全量数据。
     */
    private fun emptySearchResult(): SearchResult =
        SearchResult(
            songs = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
        )

    /**
     * 按搜索上下文选择数据源，保证收藏搜索不会回退到完整曲库。
     */
    private fun searchSourceSongs(state: MusicAppUiState): List<Song> =
        when (state.searchContext) {
            SearchContext.LocalLibrary -> {
                if (state.localSongs.isNotEmpty()) {
                    state.localSongs
                } else {
                    musicLibraryRepository.getAllAvailableSongs()
                }
            }

            SearchContext.Favorites -> {
                state.favoriteSongs
            }
        }
}
