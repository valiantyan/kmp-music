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

    // 自动搜索历史草稿用于替换同一轮输入产生的中间词，避免历史被防抖过程污染。
    private var automaticSearchHistoryDraft: AutomaticSearchHistoryDraft? = null

    /** 打开搜索时重置输入态，并切换到指定搜索上下文。 */
    fun openSearch(
        state: MusicAppUiState,
        context: SearchContext,
    ): MusicAppUiState =
        syncActiveSearchQueryImmediately(state = state, query = "").copy(
            searchContext = context,
            searchQuery = "",
            activeSearchQuery = "",
            searchScope = SearchScope.All,
        )

    /** 更新搜索词；清空会结束当前自动搜索草稿，非空词仍通过防抖驱动结果。 */
    fun setSearchQuery(
        state: MusicAppUiState,
        query: String,
    ): MusicAppUiState {
        val nextState: MusicAppUiState = state.copy(searchQuery = query)
        return scheduleActiveSearchQuerySync(
            state = nextState,
            query = query,
        )
    }

    /** 更新搜索范围。 */
    fun setSearchScope(
        state: MusicAppUiState,
        scope: SearchScope,
    ): MusicAppUiState = state.copy(searchScope = scope)

    /** 立即同步 active query，并把当前搜索词写入当前上下文历史。 */
    fun commitSearchQueryToHistory(state: MusicAppUiState): MusicAppUiState {
        val syncedState: MusicAppUiState =
            syncActiveSearchQueryImmediately(
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
    fun selectSearchHistory(
        state: MusicAppUiState,
        query: String,
    ): MusicAppUiState {
        val nextState: MusicAppUiState =
            syncActiveSearchQueryImmediately(
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
        automaticSearchHistoryDraft = null
        return updateSearchHistory(
            state = state,
            context = context,
            history = state.searchHistoryFor(context = context).filterNot { item: String -> item == query },
        )
    }

    /** 清空指定上下文的搜索历史。 */
    fun clearSearchHistory(
        state: MusicAppUiState,
        context: SearchContext,
    ): MusicAppUiState {
        automaticSearchHistoryDraft = null
        return updateSearchHistory(
            state = state,
            context = context,
            history = emptyList(),
        )
    }

    /** 点击搜索结果前集中提交非空搜索词，避免平台 UI 自己维护这条规则。 */
    fun commitActiveSearchQueryToHistoryIfNeeded(state: MusicAppUiState): MusicAppUiState {
        if (state.navigationState.secondaryScreen !is SecondaryScreen.Search) {
            return state
        }
        return commitSearchQueryToHistory(state = state)
    }

    // 显式搜索动作仍可立即提交，避免等待防抖才能记录用户主动搜索。
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
            history =
                moveQueryToHistoryTop(
                    query = normalizedQuery,
                    currentHistory = state.searchHistoryFor(context = context),
                ),
        )
    }

    // 非空 query 通过发布 reducer 延迟生效，自动历史在同一轮输入中只保留最后一个词。
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
        debounceJob =
            controllerScope.launch {
                delay(timeMillis = debounceMillis)
                publishStateUpdate { currentState: MusicAppUiState ->
                    if (!shouldPublishDebouncedSearchQuery(state = currentState, query = query)) {
                        return@publishStateUpdate currentState
                    }
                    publishDebouncedSearchQuery(
                        state = currentState,
                        query = query,
                    )
                }
            }
        return state
    }

    // 防抖搜索已驱动可见结果时才更新自动历史草稿；离开搜索页后醒来的任务只同步 active query。
    private fun publishDebouncedSearchQuery(
        state: MusicAppUiState,
        query: String,
    ): MusicAppUiState {
        val nextState: MusicAppUiState = state.copy(activeSearchQuery = query)
        val secondaryScreen: SecondaryScreen = state.navigationState.secondaryScreen ?: return nextState
        if (secondaryScreen !is SecondaryScreen.Search) {
            return nextState
        }
        return commitAutomaticSearchQueryToHistory(
            state = nextState,
            query = query,
            context = secondaryScreen.context,
        )
    }

    // 旧防抖任务醒来时必须确认输入仍匹配，避免把已清空或已替换的词发布为 active query。
    private fun shouldPublishDebouncedSearchQuery(
        state: MusicAppUiState,
        query: String,
    ): Boolean = state.searchQuery == query

    // 显式提交、清空和历史点击必须立刻同步 active query。
    private fun syncActiveSearchQueryImmediately(
        state: MusicAppUiState,
        query: String,
    ): MusicAppUiState {
        debounceJob?.cancel()
        debounceJob = null
        automaticSearchHistoryDraft = null
        return state.copy(activeSearchQuery = query)
    }

    // 自动搜索基于本轮输入开始前的历史重建列表，从而替换中间词并保留旧历史。
    private fun commitAutomaticSearchQueryToHistory(
        state: MusicAppUiState,
        query: String,
        context: SearchContext,
    ): MusicAppUiState {
        val normalizedQuery: String = query.trim()
        if (normalizedQuery.isBlank()) {
            return state
        }
        val currentDraft: AutomaticSearchHistoryDraft? = automaticSearchHistoryDraft
        val baseHistory: List<String> =
            if (currentDraft?.context == context) {
                currentDraft.baseHistory
            } else {
                state.searchHistoryFor(context = context)
            }
        automaticSearchHistoryDraft =
            AutomaticSearchHistoryDraft(
                context = context,
                baseHistory = baseHistory,
            )
        return updateSearchHistory(
            state = state,
            context = context,
            history =
                moveQueryToHistoryTop(
                    query = normalizedQuery,
                    currentHistory = baseHistory,
                ),
        )
    }

    // 最新搜索词需要去重并置顶，同时限制历史长度。
    private fun moveQueryToHistoryTop(
        query: String,
        currentHistory: List<String>,
    ): List<String> =
        (listOf(query) + currentHistory.filterNot { item: String -> item == query })
            .take(n = 10)

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

/**
 * 自动搜索历史草稿，保存同一轮输入开始前的历史基线。
 *
 * @property context 草稿所属搜索上下文，避免不同入口的历史互相替换。
 * @property baseHistory 本轮自动搜索开始前的历史，用来替换中间词并恢复既有历史顺序。
 */
private data class AutomaticSearchHistoryDraft(
    val context: SearchContext,
    val baseHistory: List<String>,
)
