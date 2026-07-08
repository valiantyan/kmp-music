package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.SearchResult

/**
 * 搜索页，按 Figma 节点 964:482 还原顶栏、历史、结果 tab 和空态。
 */
@Composable
fun SearchScreen(
    query: String,
    scope: SearchScope,
    history: List<String>,
    result: SearchResult,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onScope: (SearchScope) -> Unit,
    onHistorySelect: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val selectedTab: SearchResultTab = visibleSearchResultTab(scope = scope)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = searchBackgroundColor),
    ) {
        SearchTopBar(
            query = query,
            onBack = onBack,
            onQuery = onQuery,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 48.dp,
            ),
        ) {
            item(key = "search-history-section", contentType = "search-history") {
                SearchHistorySection(
                    history = history,
                    onHistorySelect = onHistorySelect,
                    onClearHistory = onClearHistory,
                )
            }
            item(key = "search-section-gap", contentType = "search-gap") {
                Spacer(modifier = Modifier.height(height = 40.dp))
            }
            item(key = "search-results-section", contentType = "search-results") {
                SearchResultsSection(
                    query = query,
                    selectedTab = selectedTab,
                    result = result,
                    currentSongId = currentSongId,
                    currentPlaybackStatus = currentPlaybackStatus,
                    onScope = onScope,
                    onSongPlay = onSongPlay,
                    onCurrentSongToggle = onCurrentSongToggle,
                    onMore = onMore,
                    onAlbumOpen = onAlbumOpen,
                    onArtistOpen = onArtistOpen,
                )
            }
        }
    }
}
