package com.yanhao.kmpmusic.feature.app.search

import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.repository.SearchHistoryRepository
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 负责搜索输入态、active query 防抖，以及按上下文隔离的搜索历史。
 */
class SearchSessionController(
    private val searchHistoryRepository: SearchHistoryRepository,
    private val controllerScope: CoroutineScope,
    private val debounceMillis: Long,
    private val publishStateUpdate: ((MusicAppUiState) -> MusicAppUiState) -> Unit,
) {
    // 防抖任务必须集中托管，避免 facade 和子控制器各自维护搜索发布时间线。
    private var debounceJob: Job? = null

    /** 打开搜索时重置输入态，并切换到指定搜索上下文。 */
    fun openSearch(state: MusicAppUiState, context: SearchContext): MusicAppUiState {
        return syncActiveSearchQueryImmediately(state = state, query = "").copy(
            searchContext = context,
            searchQuery = "",
            activeSearchQuery = "",
            searchScope = SearchScope.All,
        )
    }

    /** 更新搜索词；若用户清空搜索框，则先提交旧词再清空输入。 */
    fun setSearchQuery(state: MusicAppUiState, query: String): MusicAppUiState {
        val previousQuery: String = state.searchQuery
        if (shouldCommitSearchQueryBeforeClearing(
                state = state,
                previousQuery = previousQuery,
                nextQuery = query,
            )
        ) {
            val committedState: MusicAppUiState = commitSearchQueryToHistory(
                state = state,
                query = previousQuery,
                context = state.searchContext,
            )
            return syncActiveSearchQueryImmediately(
                state = committedState,
                query = "",
            ).copy(searchQuery = query)
        }
        val nextState: MusicAppUiState = state.copy(searchQuery = query)
        return scheduleActiveSearchQuerySync(
            state = nextState,
            query = query,
        )
    }

    /** 更新搜索范围。 */
    fun setSearchScope(state: MusicAppUiState, scope: SearchScope): MusicAppUiState {
        return state.copy(searchScope = scope)
    }

    /** 立即同步 active query，并把当前搜索词写入当前上下文历史。 */
    fun commitSearchQueryToHistory(state: MusicAppUiState): MusicAppUiState {
        val syncedState: MusicAppUiState = syncActiveSearchQueryImmediately(
            state = state,
            query = state.searchQuery,
        )
        return commitSearchQueryToHistory(
            state = syncedState,
            query = syncedState.searchQuery,
            context = syncedState.searchContext,
        )
    }

    /** 点击历史词时立即回填搜索态，并把该词提到历史顶部。 */
    fun selectSearchHistory(state: MusicAppUiState, query: String): MusicAppUiState {
        val nextState: MusicAppUiState = syncActiveSearchQueryImmediately(
            state = state,
            query = query,
        ).copy(searchQuery = query)
        return commitSearchQueryToHistory(state = nextState)
    }

    /** 删除指定上下文中的单条搜索历史。 */
    fun removeSearchHistoryItem(
        state: MusicAppUiState,
        context: SearchContext,
        query: String,
    ): MusicAppUiState {
        return updateSearchHistory(
            state = state,
            context = context,
            history = state.searchHistoryFor(context = context).filterNot { item: String -> item == query },
        )
    }

    /** 清空指定上下文的搜索历史。 */
    fun clearSearchHistory(state: MusicAppUiState, context: SearchContext): MusicAppUiState {
        return updateSearchHistory(
            state = state,
            context = context,
            history = emptyList(),
        )
    }

    /** 离开搜索页前集中提交非空搜索词，避免平台 UI 自己维护这条规则。 */
    fun commitActiveSearchQueryToHistoryIfNeeded(state: MusicAppUiState): MusicAppUiState {
        if (state.navigationState.secondaryScreen !is SecondaryScreen.Search) {
            return state
        }
        return commitSearchQueryToHistory(state = state)
    }

    // 只有真实处于搜索页、且用户把非空词清空时，才需要先提交旧词。
    private fun shouldCommitSearchQueryBeforeClearing(
        state: MusicAppUiState,
        previousQuery: String,
        nextQuery: String,
    ): Boolean {
        return state.navigationState.secondaryScreen is SecondaryScreen.Search &&
            previousQuery.trim().isNotBlank() &&
            nextQuery.isBlank()
    }

    // 支持在覆盖 UI 输入前提交指定 query，避免清空输入时丢失旧搜索词。
    private fun commitSearchQueryToHistory(
        state: MusicAppUiState,
        query: String,
        context: SearchContext,
    ): MusicAppUiState {
        val normalizedQuery: String = query.trim()
        if (normalizedQuery.isBlank()) {
            return state
        }
        return updateSearchHistory(
            state = state,
            context = context,
            history = moveQueryToHistoryTop(
                query = normalizedQuery,
                currentHistory = state.searchHistoryFor(context = context),
            ),
        )
    }

    // 非空 query 通过发布 reducer 的方式延迟生效，保持 facade 仍是唯一 Compose 状态持有者。
    private fun scheduleActiveSearchQuerySync(
        state: MusicAppUiState,
        query: String,
    ): MusicAppUiState {
        debounceJob?.cancel()
        if (query.isBlank()) {
            return syncActiveSearchQueryImmediately(
                state = state,
                query = query,
            )
        }
        debounceJob = controllerScope.launch {
            delay(timeMillis = debounceMillis)
            publishStateUpdate { currentState: MusicAppUiState ->
                currentState.copy(activeSearchQuery = query)
            }
        }
        return state
    }

    // 显式提交、清空和历史点击必须立刻同步 active query。
    private fun syncActiveSearchQueryImmediately(
        state: MusicAppUiState,
        query: String,
    ): MusicAppUiState {
        debounceJob?.cancel()
        debounceJob = null
        return state.copy(activeSearchQuery = query)
    }

    // 最新搜索词需要去重并置顶，同时限制历史长度。
    private fun moveQueryToHistoryTop(query: String, currentHistory: List<String>): List<String> {
        return (listOf(query) + currentHistory.filterNot { item: String -> item == query })
            .take(n = 10)
    }

    // 按上下文写回仓库和 UI state，保证不同入口的历史互不串联。
    private fun updateSearchHistory(
        state: MusicAppUiState,
        context: SearchContext,
        history: List<String>,
    ): MusicAppUiState {
        searchHistoryRepository.saveSearchHistory(
            context = context,
            history = history,
        )
        return when (context) {
            SearchContext.LocalLibrary -> state.copy(localLibrarySearchHistory = history)
            SearchContext.Favorites -> state.copy(favoritesSearchHistory = history)
        }
    }
}
