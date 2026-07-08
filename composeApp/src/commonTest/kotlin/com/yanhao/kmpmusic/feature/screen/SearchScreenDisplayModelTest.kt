package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.SearchScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 验证新版 Figma 搜索页的轻量展示规则，避免 UI 直接消费空 query 的全量搜索结果。
 */
class SearchScreenDisplayModelTest {
    @Test
    fun emptyQueryKeepsResultContentHiddenUntilUserTypes(): Unit {
        assertFalse(actual = shouldShowSearchResultContent(query = ""))
        assertFalse(actual = shouldShowSearchResultContent(query = "   "))
        assertTrue(actual = shouldShowSearchResultContent(query = "周杰伦"))
    }

    @Test
    fun allScopeUsesSongsTabAsFigmaDefaultTab(): Unit {
        assertEquals(
            expected = SearchResultTab.Songs,
            actual = visibleSearchResultTab(scope = SearchScope.All),
        )
        assertEquals(
            expected = SearchResultTab.Songs,
            actual = visibleSearchResultTab(scope = SearchScope.Songs),
        )
    }

    @Test
    fun playlistTabIsVisualOnlyUntilDomainSupportsPlaylistSearch(): Unit {
        assertNull(actual = SearchResultTab.Playlists.scope)
    }

    @Test
    fun emptyHistoryUsesFigmaFallbackChipsForInitialVisualParity(): Unit {
        assertEquals(
            expected = listOf("周杰伦", "陈奕迅", "轻音乐", "Lo-fi Beats", "流行摇滚"),
            actual = visibleSearchHistoryChips(history = emptyList()),
        )
        assertEquals(
            expected = listOf("One Summer"),
            actual = visibleSearchHistoryChips(history = listOf("One Summer")),
        )
    }
}
