package com.yanhao.kmpmusic.feature.app.search

import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.repository.SearchHistoryRepository
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MusicAppSearchControllerTest {
    @Test
    fun openSearchResetsQueryAndScopeForContext(): Unit = runTest {
        val controller = SearchSessionController(
            searchHistoryRepository = FakeSearchHistoryRepository(),
            controllerScope = this,
            debounceMillis = 300L,
            publishStateUpdate = { _ -> },
        )
        val state = testState().copy(
            searchQuery = "old",
            activeSearchQuery = "old",
            searchScope = SearchScope.Songs,
        )

        val nextState = controller.openSearch(
            state = state,
            context = SearchContext.Favorites,
        )

        assertEquals(expected = SearchContext.Favorites, actual = nextState.searchContext)
        assertEquals(expected = "", actual = nextState.searchQuery)
        assertEquals(expected = "", actual = nextState.activeSearchQuery)
        assertEquals(expected = SearchScope.All, actual = nextState.searchScope)
    }

    @Test
    fun clearingSearchQueryCommitsPreviousQueryBeforeItIsLost(): Unit = runTest {
        val repository = FakeSearchHistoryRepository()
        val controller = SearchSessionController(
            searchHistoryRepository = repository,
            controllerScope = this,
            debounceMillis = 300L,
            publishStateUpdate = { _ -> },
        )
        val state = testState().copy(
            navigationState = testState().navigationState.copy(
                secondaryScreen = SecondaryScreen.Search(context = SearchContext.LocalLibrary),
            ),
            searchContext = SearchContext.LocalLibrary,
            searchQuery = "summer",
            activeSearchQuery = "summer",
        )

        val nextState = controller.setSearchQuery(state = state, query = "")

        assertEquals(expected = "", actual = nextState.searchQuery)
        assertEquals(expected = listOf("summer"), actual = nextState.localLibrarySearchHistory)
        assertEquals(expected = listOf("summer"), actual = repository.getSearchHistory(SearchContext.LocalLibrary))
    }

    @Test
    fun nonBlankSearchQueryDebouncesActiveQuery(): Unit = runTest {
        var state = testState()
        val controller = SearchSessionController(
            searchHistoryRepository = FakeSearchHistoryRepository(),
            controllerScope = this,
            debounceMillis = 300L,
            publishStateUpdate = { reducer -> state = reducer(state) },
        )

        state = controller.setSearchQuery(state = state, query = "river")
        assertEquals(expected = "", actual = state.activeSearchQuery)

        advanceTimeBy(299L)
        assertEquals(expected = "", actual = state.activeSearchQuery)

        advanceTimeBy(1L)
        advanceUntilIdle()
        assertEquals(expected = "river", actual = state.activeSearchQuery)
    }

    @Test
    fun searchHistoryIsIsolatedByContextAndDeduplicatesLatestFirst(): Unit = runTest {
        val controller = SearchSessionController(
            searchHistoryRepository = FakeSearchHistoryRepository(),
            controllerScope = this,
            debounceMillis = 300L,
            publishStateUpdate = { _ -> },
        )
        var state = testState().copy(searchContext = SearchContext.LocalLibrary, searchQuery = "river")
        state = controller.commitSearchQueryToHistory(state = state)
        state = state.copy(searchContext = SearchContext.Favorites, searchQuery = "river")
        state = controller.commitSearchQueryToHistory(state = state)
        state = state.copy(searchContext = SearchContext.LocalLibrary, searchQuery = "sea")
        state = controller.commitSearchQueryToHistory(state = state)
        state = state.copy(searchContext = SearchContext.LocalLibrary, searchQuery = "river")
        state = controller.commitSearchQueryToHistory(state = state)

        assertEquals(expected = listOf("river", "sea"), actual = state.localLibrarySearchHistory)
        assertEquals(expected = listOf("river"), actual = state.favoritesSearchHistory)
    }

    private fun testState(): MusicAppUiState {
        return MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        )
    }
}

private class FakeSearchHistoryRepository : SearchHistoryRepository {
    private val histories: MutableMap<SearchContext, List<String>> = mutableMapOf()

    override fun getSearchHistory(context: SearchContext): List<String> {
        return histories[context].orEmpty()
    }

    override fun saveSearchHistory(context: SearchContext, history: List<String>) {
        histories[context] = history
    }
}
