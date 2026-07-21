package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    activeQuery: String,
    scope: SearchScope,
    history: List<String>,
    result: SearchResult,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    currentAlbumTitle: String?,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onCommitSearch: () -> Unit,
    onScope: (SearchScope) -> Unit,
    onHistorySelect: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var isSearchInputFocused: Boolean by remember { mutableStateOf(value = false) }
    val keyboardDismissConnection: NestedScrollConnection =
        remember(
            keyboardController,
            focusManager,
            isSearchInputFocused,
        ) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (shouldDismissSearchKeyboardOnScroll(
                            isUserInput = source == NestedScrollSource.UserInput,
                            isSearchInputFocused = isSearchInputFocused,
                            horizontalDelta = available.x,
                            verticalDelta = available.y,
                        )
                    ) {
                        isSearchInputFocused = false
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                    }
                    return Offset.Zero
                }
            }
        }
    val selectedTab: SearchResultTab = visibleSearchResultTab(scope = scope)
    val hasSearchHistory: Boolean =
        shouldShowSearchHistorySection(
            history = history,
        )
    val shouldShowResults: Boolean =
        shouldShowSearchResultContent(
            query = query,
            activeQuery = activeQuery,
        )
    val resultRows: List<SearchResultLazyRow> =
        buildVisibleSearchResultLazyRows(
            query = query,
            activeQuery = activeQuery,
            selectedTab = selectedTab,
            result = result,
        )
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(color = searchBackgroundColor),
    ) {
        SearchTopBar(
            query = query,
            onBack = onBack,
            onQuery = onQuery,
            onCommitSearch = onCommitSearch,
            onInputFocusChanged = { isFocused: Boolean ->
                isSearchInputFocused = isFocused
            },
        )
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(connection = keyboardDismissConnection),
            contentPadding =
                PaddingValues(
                    top = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 48.dp,
                ),
        ) {
            if (hasSearchHistory) {
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
            }
            item(key = "search-results-header", contentType = "search-results-header") {
                SearchResultsHeader(
                    selectedTab = selectedTab,
                    onScope = onScope,
                )
            }
            if (shouldShowResults) {
                itemsIndexed(
                    items = resultRows,
                    key = { _: Int, row: SearchResultLazyRow -> searchResultLazyRowKey(row = row) },
                    contentType = { _: Int, row: SearchResultLazyRow ->
                        searchResultLazyRowContentType(row = row)
                    },
                ) { index: Int, row: SearchResultLazyRow ->
                    SearchResultRowFrame(index = index) {
                        SearchResultLazyRowContent(
                            row = row,
                            currentSongId = currentSongId,
                            currentPlaybackStatus = currentPlaybackStatus,
                            currentAlbumTitle = currentAlbumTitle,
                            onSongPlay = { song: Song, queueSongs: List<Song> ->
                                onSongPlay(song, queueSongs)
                            },
                            onCurrentSongToggle = onCurrentSongToggle,
                            onMore = onMore,
                            onLike = onLike,
                            onAlbumOpen = { album: Album ->
                                onAlbumOpen(album)
                            },
                            onArtistOpen = { artist: Artist ->
                                onArtistOpen(artist)
                            },
                        )
                    }
                }
            } else {
                item(key = "search-empty-suggestion", contentType = "search-empty-suggestion") {
                    SearchResultRowFrame(index = 0) {
                        if (shouldShowSearchPendingState(query = query, activeQuery = activeQuery)) {
                            SearchNoResultState(message = "正在准备“${query.trim()}”的搜索结果")
                        } else {
                            SearchEmptySuggestion()
                        }
                    }
                }
            }
        }
    }
}
