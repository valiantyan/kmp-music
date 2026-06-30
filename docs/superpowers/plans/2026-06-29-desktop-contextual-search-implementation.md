# Desktop Contextual Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement macOS desktop contextual search so Home searches local scanned music, Favorites searches favorite data, Profile/Settings hide the titlebar search, and the Search page owns the only active search input.

**Architecture:** Add a shared `SearchContext` to the app state and route `SecondaryScreen.Search` with that context. Keep search result generation as a pure derivation from the current local library or favorite collection, while the desktop shell decides whether the titlebar search is visible from navigation state. The desktop search UI consumes controller state and renders empty/history, result, and empty-result states without introducing another data source.

**Tech Stack:** Kotlin Multiplatform 2.0.21, Compose Multiplatform 1.7.3, shared `MusicAppController`, `SearchMusicUseCase`, common tests with Kotlin test and coroutines.

---

## File Structure

- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/SearchMusicUseCase.kt`
  - Keep `SearchResult` and `SearchMusicUseCase`.
  - Make `buildSearchResult` public so controller tests and app code can use it across packages.
  - Keep result derivation pure and list-based.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`
  - Add `SearchContext`.
  - Change `SecondaryScreen.Search` from `data object` to `data class Search(val context: SearchContext)`.
  - Add context-aware search history and titlebar-search visibility helpers to `MusicAppUiState`.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
  - Add `openSearch(context: SearchContext = SearchContext.LocalLibrary)`.
  - Route Home/Favorites searches to different datasets.
  - Add search-history mutation methods.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`
  - Show titlebar search only on Home and Favorites top-level pages.
  - Route titlebar search using the active root tab.
  - Rename sidebar search action to a local filter-only prop.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`
  - Add `showSearch` to `DesktopTitleBar`.
  - Change sidebar input copy from `搜索本地库` to `筛选本地库`.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt`
  - Render contextual Search page with back label, subtitle, scope tabs, history, and results.
- Modify `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`
  - Add tests for contextual search routing, result scope, titlebar visibility, history isolation, and sidebar filter non-navigation.

## Task 1: Add Contextual Search Models And Navigation Tests

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [x] **Step 1: Add failing tests for contextual search navigation**

Append these tests near the existing search/navigation tests in `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`:

```kotlin
/**
 * 首页顶部搜索应进入本地曲库搜索上下文。
 */
@Test
fun homeSearchOpensLocalLibrarySearchContext(): Unit {
    val controller = createController()

    controller.navigateToRoot(tab = RootTab.Home)
    controller.openSearch(context = SearchContext.LocalLibrary)

    assertEquals(
        expected = SecondaryScreen.Search(context = SearchContext.LocalLibrary),
        actual = controller.uiState.navigationState.secondaryScreen,
    )
    assertEquals(expected = RootTab.Home, actual = controller.uiState.navigationState.previousRootTab)
    assertEquals(expected = SearchContext.LocalLibrary, actual = controller.uiState.searchContext)
}

/**
 * 收藏顶部搜索应进入收藏搜索上下文。
 */
@Test
fun favoritesSearchOpensFavoritesSearchContext(): Unit {
    val controller = createController()

    controller.navigateToRoot(tab = RootTab.Favorites)
    controller.openSearch(context = SearchContext.Favorites)

    assertEquals(
        expected = SecondaryScreen.Search(context = SearchContext.Favorites),
        actual = controller.uiState.navigationState.secondaryScreen,
    )
    assertEquals(expected = RootTab.Favorites, actual = controller.uiState.navigationState.previousRootTab)
    assertEquals(expected = SearchContext.Favorites, actual = controller.uiState.searchContext)
}
```

- [x] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.homeSearchOpensLocalLibrarySearchContext" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.favoritesSearchOpensFavoritesSearchContext"
```

Expected: compilation fails because `SearchContext` and `SecondaryScreen.Search(context = ...)` do not exist.

- [x] **Step 3: Add `SearchContext` and route Search with context**

In `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`, add this enum after `LocalMusicSection`:

```kotlin
/**
 * 搜索页的数据上下文。
 */
enum class SearchContext {
    LocalLibrary,
    Favorites,
}
```

Replace the current `SecondaryScreen.Search` declaration:

```kotlin
data object Search : SecondaryScreen
```

with:

```kotlin
data class Search(val context: SearchContext = SearchContext.LocalLibrary) : SecondaryScreen
```

Update `NavigationState.chromeMode`:

```kotlin
is SecondaryScreen.Search,
SecondaryScreen.AlbumDetail,
SecondaryScreen.ArtistDetail,
SecondaryScreen.Login,
is SecondaryScreen.LocalMusic,
-> AppChromeMode.SecondaryWithMiniPlayer
```

Update `SecondaryScreen.routeName()`:

```kotlin
is SecondaryScreen.Search -> "Search:${context.name}"
```

Add this property to `MusicAppUiState` next to `searchScope`:

```kotlin
val searchContext: SearchContext = SearchContext.LocalLibrary,
```

- [x] **Step 4: Update controller search entry**

In `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`, import `SearchContext` if the IDE does not add it automatically, then replace `openSearch()` with:

```kotlin
/** 搜索页应按入口上下文拿到对应数据集合，避免搜索结果跨页面串联。 */
fun openSearch(context: SearchContext = SearchContext.LocalLibrary) {
    if (context == SearchContext.LocalLibrary) {
        loadLocalMusicLibrary()
    }
    uiState = uiState.copy(
        searchContext = context,
        searchQuery = "",
        searchScope = SearchScope.All,
    )
    navigateToSecondary(screen = SecondaryScreen.Search(context = context))
}
```

- [x] **Step 5: Update existing references to `SecondaryScreen.Search`**

Replace object-style references in app and tests:

```kotlin
SecondaryScreen.Search
```

with default local-library route:

```kotlin
SecondaryScreen.Search(context = SearchContext.LocalLibrary)
```

For `NavigationState.chromeMode` and `routeName`, use `is SecondaryScreen.Search` as shown above.

- [x] **Step 6: Run tests to verify the task passes**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.homeSearchOpensLocalLibrarySearchContext" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.favoritesSearchOpensFavoritesSearchContext"
```

Expected: both tests pass.

- [x] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "实现桌面搜索上下文导航"
```

## Task 2: Implement Context-Specific Search Results

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/SearchMusicUseCase.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [x] **Step 1: Add failing tests for result isolation**

Append these tests near existing search tests in `MusicAppControllerTest.kt`:

```kotlin
/**
 * 首页搜索应搜索完整本地曲库，不受收藏集合限制。
 */
@Test
fun localLibrarySearchReturnsNonFavoriteLocalSongs(): Unit = runBlocking {
    val controller = createController()
    controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
    controller.openSearch(context = SearchContext.LocalLibrary)
    controller.setSearchQuery(query = "One Summer")
    controller.setSearchScope(scope = SearchScope.Songs)

    assertEquals(
        expected = listOf("One Summer's Day"),
        actual = controller.search().songs.map { song -> song.title },
    )
}

/**
 * 收藏搜索只返回已收藏歌曲，不应返回本地曲库全部内容。
 */
@Test
fun favoritesSearchOnlyReturnsFavoriteSongs(): Unit = runBlocking {
    val controller = createController()
    controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
    val favoriteSong: Song = controller.uiState.localSongs.first { song -> song.title == "One Summer's Day" }
    controller.toggleFavorite(songId = favoriteSong.id)

    controller.openSearch(context = SearchContext.Favorites)
    controller.setSearchQuery(query = "One Summer")
    controller.setSearchScope(scope = SearchScope.Songs)

    assertEquals(
        expected = listOf("One Summer's Day"),
        actual = controller.search().songs.map { song -> song.title },
    )

    controller.setSearchQuery(query = "The Best of Me")

    assertTrue(actual = controller.search().songs.isEmpty())
}
```

- [x] **Step 2: Run tests to verify the Favorites test fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.localLibrarySearchReturnsNonFavoriteLocalSongs" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.favoritesSearchOnlyReturnsFavoriteSongs"
```

Expected: `favoritesSearchOnlyReturnsFavoriteSongs` fails because `search()` still reads local songs.

- [x] **Step 3: Make `buildSearchResult` public**

In `SearchMusicUseCase.kt`, change:

```kotlin
internal fun buildSearchResult(
```

to:

```kotlin
fun buildSearchResult(
```

Keep the existing KDoc immediately above it. If KDoc still says “供控制器复用已加载曲库”, it remains accurate.

- [x] **Step 4: Add context-specific source selection in controller**

Replace `search()` in `MusicAppController.kt` with:

```kotlin
/** 执行搜索，供 UI 渲染派生结果。 */
fun search(): com.yanhao.kmpmusic.domain.usecase.SearchResult {
    val sourceSongs: List<Song> = when (uiState.searchContext) {
        SearchContext.LocalLibrary -> {
            if (uiState.localSongs.isNotEmpty()) {
                uiState.localSongs
            } else {
                musicLibraryRepository.getAllAvailableSongs()
            }
        }
        SearchContext.Favorites -> uiState.favoriteSongs
    }
    return buildSearchResult(
        query = uiState.searchQuery,
        scope = uiState.searchScope,
        allSongs = sourceSongs,
    )
}
```

- [x] **Step 5: Run focused tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.localLibrarySearchReturnsNonFavoriteLocalSongs" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.favoritesSearchOnlyReturnsFavoriteSongs" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchScopeLimitsResultTypes"
```

Expected: all selected tests pass.

- [x] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/SearchMusicUseCase.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "按入口上下文隔离搜索结果"
```

## Task 3: Add Runtime Search History Per Context

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [x] **Step 1: Add failing history tests**

Append these tests near the search tests:

```kotlin
/**
 * 搜索历史应按上下文隔离，避免收藏页出现本地库历史。
 */
@Test
fun searchHistoryIsIsolatedByContext(): Unit {
    val controller = createController()

    controller.openSearch(context = SearchContext.LocalLibrary)
    controller.setSearchQuery(query = "城市")
    controller.commitSearchQueryToHistory()

    controller.openSearch(context = SearchContext.Favorites)
    controller.setSearchQuery(query = "周杰伦")
    controller.commitSearchQueryToHistory()

    assertEquals(
        expected = listOf("城市"),
        actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
    )
    assertEquals(
        expected = listOf("周杰伦"),
        actual = controller.uiState.searchHistoryFor(context = SearchContext.Favorites),
    )
}

/**
 * 搜索历史应去重并把最新搜索放到最前面。
 */
@Test
fun searchHistoryDeduplicatesAndMovesLatestFirst(): Unit {
    val controller = createController()

    controller.openSearch(context = SearchContext.LocalLibrary)
    listOf("城市", "周杰伦", "城市").forEach { query ->
        controller.setSearchQuery(query = query)
        controller.commitSearchQueryToHistory()
    }

    assertEquals(
        expected = listOf("城市", "周杰伦"),
        actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
    )
}

/**
 * 清空历史只影响当前搜索上下文。
 */
@Test
fun clearSearchHistoryOnlyClearsCurrentContext(): Unit {
    val controller = createController()

    controller.openSearch(context = SearchContext.LocalLibrary)
    controller.setSearchQuery(query = "城市")
    controller.commitSearchQueryToHistory()
    controller.openSearch(context = SearchContext.Favorites)
    controller.setSearchQuery(query = "周杰伦")
    controller.commitSearchQueryToHistory()

    controller.clearSearchHistory(context = SearchContext.Favorites)

    assertEquals(
        expected = listOf("城市"),
        actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
    )
    assertTrue(actual = controller.uiState.searchHistoryFor(context = SearchContext.Favorites).isEmpty())
}
```

- [x] **Step 2: Run tests to verify compilation fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchHistoryIsIsolatedByContext" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchHistoryDeduplicatesAndMovesLatestFirst" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.clearSearchHistoryOnlyClearsCurrentContext"
```

Expected: compilation fails because history APIs do not exist.

- [x] **Step 3: Add history state helpers**

In `MusicAppModels.kt`, add these fields to `MusicAppUiState` next to `searchContext`, `searchQuery`, and `searchScope`:

```kotlin
val localLibrarySearchHistory: List<String> = emptyList(),
val favoritesSearchHistory: List<String> = emptyList(),
```

Add this function inside `MusicAppUiState`:

```kotlin
/**
 * 当前搜索上下文对应的历史记录。
 */
fun searchHistoryFor(context: SearchContext = searchContext): List<String> {
    return when (context) {
        SearchContext.LocalLibrary -> localLibrarySearchHistory
        SearchContext.Favorites -> favoritesSearchHistory
    }
}
```

- [x] **Step 4: Add controller history mutations**

In `MusicAppController.kt`, add these methods near `setSearchScope`:

```kotlin
/** 将当前搜索词写入当前上下文历史。 */
fun commitSearchQueryToHistory() {
    val normalizedQuery: String = uiState.searchQuery.trim()
    if (normalizedQuery.isBlank()) {
        return
    }
    updateSearchHistory(
        context = uiState.searchContext,
        history = moveQueryToHistoryTop(
            query = normalizedQuery,
            currentHistory = uiState.searchHistoryFor(context = uiState.searchContext),
        ),
    )
}

/** 点击历史词时回填搜索框并刷新该词位置。 */
fun selectSearchHistory(query: String) {
    uiState = uiState.copy(searchQuery = query)
    commitSearchQueryToHistory()
}

/** 删除当前上下文中的单条搜索历史。 */
fun removeSearchHistoryItem(context: SearchContext, query: String) {
    updateSearchHistory(
        context = context,
        history = uiState.searchHistoryFor(context = context).filterNot { item -> item == query },
    )
}

/** 清空指定上下文的搜索历史。 */
fun clearSearchHistory(context: SearchContext = uiState.searchContext) {
    updateSearchHistory(context = context, history = emptyList())
}

private fun moveQueryToHistoryTop(query: String, currentHistory: List<String>): List<String> {
    return (listOf(query) + currentHistory.filterNot { item -> item == query })
        .take(n = 10)
}

private fun updateSearchHistory(context: SearchContext, history: List<String>) {
    uiState = when (context) {
        SearchContext.LocalLibrary -> uiState.copy(localLibrarySearchHistory = history)
        SearchContext.Favorites -> uiState.copy(favoritesSearchHistory = history)
    }
}
```

- [x] **Step 5: Run history tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchHistoryIsIsolatedByContext" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchHistoryDeduplicatesAndMovesLatestFirst" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.clearSearchHistoryOnlyClearsCurrentContext"
```

Expected: all selected tests pass.

- [x] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "按搜索上下文维护历史记录"
```

## Task 4: Implement Desktop Titlebar Search Visibility And Entry Routing

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [x] **Step 1: Add failing tests for titlebar visibility rules**

Append these tests near navigation tests:

```kotlin
/**
 * 顶部音乐搜索只应在首页和收藏一级页展示。
 */
@Test
fun titlebarSearchOnlyShowsOnHomeAndFavoritesRootPages(): Unit {
    val controller = createController()

    controller.navigateToRoot(tab = RootTab.Home)
    assertTrue(actual = controller.uiState.shouldShowTitlebarMusicSearch)

    controller.navigateToRoot(tab = RootTab.Favorites)
    assertTrue(actual = controller.uiState.shouldShowTitlebarMusicSearch)

    controller.navigateToRoot(tab = RootTab.Me)
    assertFalse(actual = controller.uiState.shouldShowTitlebarMusicSearch)

    controller.navigateToSecondary(screen = SecondaryScreen.Settings)
    assertFalse(actual = controller.uiState.shouldShowTitlebarMusicSearch)
}

/**
 * 搜索页自身应隐藏标题栏搜索框，避免两个搜索输入源。
 */
@Test
fun searchScreenHidesTitlebarSearch(): Unit {
    val controller = createController()

    controller.openSearch(context = SearchContext.LocalLibrary)

    assertFalse(actual = controller.uiState.shouldShowTitlebarMusicSearch)
}
```

- [x] **Step 2: Run tests to verify compilation fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.titlebarSearchOnlyShowsOnHomeAndFavoritesRootPages" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchScreenHidesTitlebarSearch"
```

Expected: compilation fails because `shouldShowTitlebarMusicSearch` does not exist.

- [x] **Step 3: Add titlebar visibility helper**

In `MusicAppModels.kt`, add this property inside `MusicAppUiState`:

```kotlin
/**
 * Desktop 顶部音乐搜索只在内容型一级页出现。
 */
val shouldShowTitlebarMusicSearch: Boolean
    get() = navigationState.secondaryScreen == null &&
        (navigationState.rootTab == RootTab.Home || navigationState.rootTab == RootTab.Favorites)
```

- [x] **Step 4: Update DesktopTitleBar signature and layout**

In `DesktopMusicComponents.kt`, change the function signature:

```kotlin
fun DesktopTitleBar(
    showSearch: Boolean,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Wrap the existing `Surface` search field with:

```kotlin
if (showSearch) {
    Surface(
        modifier = Modifier
            .width(520.dp)
            .height(30.dp)
            .padding(end = 18.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.84f),
        border = BorderStroke(width = 1.dp, color = Color(0xFFD7DDE3)),
        onClick = onSearch,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = Color(0xFF8A95A3),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "搜索歌曲、专辑、歌手",
                color = Color(0xFF8A95A3),
                fontSize = DesktopMusicType.Body,
            )
        }
    }
} else {
    Spacer(
        modifier = Modifier
            .width(520.dp)
            .height(30.dp)
            .padding(end = 18.dp),
    )
}
```

- [x] **Step 5: Route titlebar search by active root tab**

In `DesktopMusicApp.kt`, replace:

```kotlin
DesktopTitleBar(
    onSearch = controller::openSearch,
)
```

with:

```kotlin
DesktopTitleBar(
    showSearch = state.shouldShowTitlebarMusicSearch,
    onSearch = {
        val context: SearchContext = when (state.navigationState.rootTab) {
            RootTab.Favorites -> SearchContext.Favorites
            RootTab.Home,
            RootTab.Me,
            -> SearchContext.LocalLibrary
        }
        controller.openSearch(context = context)
    },
)
```

Add import:

```kotlin
import com.yanhao.kmpmusic.feature.app.SearchContext
```

- [x] **Step 6: Rename sidebar search entry to local filter semantics**

In `DesktopMusicApp.kt`, inside `DesktopLibrarySidebar(...)`, change:

```kotlin
onSearch = controller::openSearch,
```

to:

```kotlin
onSearch = {},
```

In `DesktopMusicComponents.kt`, find the sidebar input placeholder text and replace:

```kotlin
搜索本地库
```

with:

```kotlin
筛选本地库
```

If the sidebar search field is currently clickable through `onSearch`, leaving `onSearch = {}` makes click focus/visual-only until local filtering is implemented. Do not route it to `openSearch`.

- [x] **Step 7: Run focused tests and compile**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.titlebarSearchOnlyShowsOnHomeAndFavoritesRootPages" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchScreenHidesTitlebarSearch"
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: tests pass and Android shared compilation succeeds.

- [x] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "限定桌面顶部音乐搜索入口"
```

## Task 5: Build Contextual Desktop Search Page UI

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`

- [x] **Step 1: Update DesktopSecondaryScreen wiring**

In `DesktopMusicScreens.kt`, replace the `SecondaryScreen.Search -> DesktopSearchScreen(...)` branch with:

```kotlin
is SecondaryScreen.Search -> {
    val searchResult = controller.search()
    DesktopSearchScreen(
        context = state.searchContext,
        query = state.searchQuery,
        scope = state.searchScope,
        result = searchResult,
        history = state.searchHistoryFor(),
        currentSongId = state.currentSongId,
        currentPlaybackStatus = state.playbackStatus,
        onQuery = controller::setSearchQuery,
        onScope = controller::setSearchScope,
        onBack = controller::navigateBack,
        onCommitSearch = controller::commitSearchQueryToHistory,
        onHistoryClick = controller::selectSearchHistory,
        onHistoryRemove = { query -> controller.removeSearchHistoryItem(context = state.searchContext, query = query) },
        onHistoryClear = { controller.clearSearchHistory(context = state.searchContext) },
        onSongPlay = { song: Song, queueSongs: List<Song> ->
            controller.commitSearchQueryToHistory()
            controller.playSong(
                song = song,
                queueSongs = queueSongs,
            )
        },
        onMore = controller::openMore,
        onAlbumOpen = { album: Album ->
            controller.commitSearchQueryToHistory()
            controller.openAlbum(album = album)
        },
        onArtistOpen = { artist: Artist ->
            controller.commitSearchQueryToHistory()
            controller.openArtist(artist = artist)
        },
    )
}
```

Add imports:

```kotlin
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.feature.app.SearchContext
```

- [x] **Step 2: Replace DesktopSearchScreen signature**

Replace the existing `DesktopSearchScreen` signature with:

```kotlin
@Composable
private fun DesktopSearchScreen(
    context: SearchContext,
    query: String,
    scope: SearchScope,
    result: SearchResult,
    history: List<String>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onQuery: (String) -> Unit,
    onScope: (SearchScope) -> Unit,
    onBack: () -> Unit,
    onCommitSearch: () -> Unit,
    onHistoryClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onHistoryClear: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
)
```

- [x] **Step 3: Replace DesktopSearchScreen body**

Use this body for `DesktopSearchScreen`:

```kotlin
{
    val backLabel: String = when (context) {
        SearchContext.LocalLibrary -> "← 本地音乐"
        SearchContext.Favorites -> "← 收藏"
    }
    val subtitle: String = when (context) {
        SearchContext.LocalLibrary -> "在本地音乐中搜索歌曲、专辑、歌手"
        SearchContext.Favorites -> "在收藏中搜索歌曲、专辑、歌手"
    }
    val isEmptyQuery: Boolean = query.isBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "搜索",
            eyebrow = subtitle,
        ) {
            DesktopSecondaryButton(text = backLabel, onClick = onBack)
        }
        DesktopTextInput(
            value = query,
            onValueChange = onQuery,
            placeholder = "搜索歌曲、专辑、歌手",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Rounded.Search,
            onSubmit = onCommitSearch,
        )
        Spacer(modifier = Modifier.height(18.dp))
        DesktopSearchScopeTabs(
            selectedScope = scope,
            onScope = onScope,
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (isEmptyQuery) {
            DesktopSearchHistorySection(
                history = history,
                onHistoryClick = onHistoryClick,
                onHistoryRemove = onHistoryRemove,
                onHistoryClear = onHistoryClear,
            )
            Spacer(modifier = Modifier.height(44.dp))
            DesktopSectionEmptyMessage(message = "输入关键词后显示匹配歌曲、专辑和歌手")
            return@Column
        }
        DesktopSearchResultsSection(
            query = query,
            scope = scope,
            result = result,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            onSongPlay = onSongPlay,
            onMore = onMore,
            onAlbumOpen = onAlbumOpen,
            onArtistOpen = onArtistOpen,
        )
    }
}
```

- [x] **Step 4: Extend DesktopTextInput with submit support**

In `DesktopMusicComponents.kt`, add imports:

```kotlin
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
```

Change `DesktopTextInput` signature:

```kotlin
fun DesktopTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onSubmit: () -> Unit = {},
)
```

Add these parameters to `BasicTextField`:

```kotlin
keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
```

- [x] **Step 5: Add scope tabs and history composables**

In `DesktopMusicScreens.kt`, add these private composables after `DesktopSearchScreen`:

```kotlin
@Composable
private fun DesktopSearchScopeTabs(
    selectedScope: SearchScope,
    onScope: (SearchScope) -> Unit,
) {
    DesktopSegmentedControl(
        labels = listOf("全部", "歌曲", "专辑", "歌手"),
        selectedIndex = when (selectedScope) {
            SearchScope.All -> 0
            SearchScope.Songs -> 1
            SearchScope.Albums -> 2
            SearchScope.Artists -> 3
        },
        onSelect = { index: Int ->
            onScope(
                when (index) {
                    0 -> SearchScope.All
                    1 -> SearchScope.Songs
                    2 -> SearchScope.Albums
                    else -> SearchScope.Artists
                },
            )
        },
    )
}

@Composable
private fun DesktopSearchHistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onHistoryClear: () -> Unit,
) {
    if (history.isEmpty()) {
        DesktopSectionEmptyMessage(message = "暂无最近搜索")
        return
    }
    DesktopSectionHeader(
        title = "最近搜索",
        actionLabel = "清空",
        onAction = onHistoryClear,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        history.forEach { item: String ->
            DesktopSearchHistoryChip(
                text = item,
                onClick = { onHistoryClick(item) },
                onRemove = { onHistoryRemove(item) },
            )
        }
    }
}
```

If `DesktopSegmentedControl`, `DesktopSectionHeader`, or `DesktopSectionEmptyMessage` signatures differ, adapt only argument names while preserving labels and behavior above.

- [x] **Step 6: Add history chip composable**

Add this private composable below `DesktopSearchHistorySection`:

```kotlin
@Composable
private fun DesktopSearchHistoryChip(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.78f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                color = DesktopMusicColors.MutedStrong,
                fontSize = DesktopMusicType.Body,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            DesktopTinyTextButton(text = "×", onClick = onRemove)
        }
    }
}
```

If `DesktopTinyTextButton` does not exist, add this private composable:

```kotlin
@Composable
private fun DesktopTinyTextButton(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = DesktopMusicColors.Muted,
            fontSize = DesktopMusicType.Body,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}
```

- [x] **Step 7: Add result rendering composable**

Add this private composable after history chip:

```kotlin
@Composable
private fun DesktopSearchResultsSection(
    query: String,
    scope: SearchScope,
    result: SearchResult,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    val hasResults: Boolean = result.songs.isNotEmpty() || result.albums.isNotEmpty() || result.artists.isNotEmpty()
    if (!hasResults) {
        DesktopSectionEmptyMessage(message = "没有找到“$query”相关内容，请尝试搜索歌曲名、专辑名或歌手名。")
        return
    }
    Text(
        text = "找到 ${result.songs.size} 首歌曲、${result.albums.size} 张专辑、${result.artists.size} 位歌手",
        color = DesktopMusicColors.Muted,
        fontSize = DesktopMusicType.Eyebrow,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(18.dp))
    if (scope == SearchScope.All || scope == SearchScope.Songs) {
        DesktopSongTable(
            songs = result.songs,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongPlay = onSongPlay,
            onCurrentSongToggle = {},
            onMore = onMore,
        )
    }
    if ((scope == SearchScope.All || scope == SearchScope.Albums) && result.albums.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        DesktopSectionHeader(title = "匹配专辑")
        DesktopAlbumGrid(albums = result.albums, onAlbumOpen = onAlbumOpen)
    }
    if ((scope == SearchScope.All || scope == SearchScope.Artists) && result.artists.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        DesktopSectionHeader(title = "匹配歌手")
        DesktopArtistStrip(artists = result.artists, onArtistOpen = onArtistOpen)
    }
}
```

If `DesktopArtistStrip` has a different signature, use the existing artist grid/list composable in `DesktopMusicScreens.kt` and pass `result.artists` plus `onArtistOpen`.

- [x] **Step 8: Run desktop compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: compilation succeeds. If Compose desktop-only functions are in `commonMain`, Android compilation still checks them.

- [x] **Step 9: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt
git commit -m "实现桌面上下文搜索页面"
```

## Task 6: Final Verification And Regression Pass

**Files:**
- Verify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`
- Verify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- Verify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`
- Verify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt`
- Verify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`
- Verify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [x] **Step 1: Run full relevant verification**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
```

Expected: both tasks pass.

- [x] **Step 2: Run source checks for stale Search object usage**

Run:

```bash
rg -n "SecondaryScreen\\.Search(?!\\()" composeApp/src/commonMain/kotlin composeApp/src/commonTest/kotlin
```

Expected: no results. Every Search route should be `SecondaryScreen.Search(context = ...)` or a type check `is SecondaryScreen.Search`.

- [x] **Step 3: Run source checks for accidental sidebar search routing**

Run:

```bash
rg -n "筛选本地库|搜索本地库|onSearch = controller::openSearch|DesktopTitleBar\\(" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop
```

Expected:

```text
DesktopMusicComponents.kt contains 筛选本地库
No result contains 搜索本地库
No result contains onSearch = controller::openSearch inside DesktopLibrarySidebar
DesktopTitleBar call passes showSearch = state.shouldShowTitlebarMusicSearch
```

- [ ] **Step 4: Manual visual smoke check**

Start the desktop app using the repository's existing desktop run task. If the exact run task is unknown, inspect tasks first:

```bash
./gradlew :composeApp:tasks
```

Then run the desktop app task exposed by Compose Desktop, commonly:

```bash
./gradlew :composeApp:run
```

Expected visual states:

```text
首页: titlebar shows 搜索歌曲、专辑、歌手; sidebar says 筛选本地库.
首页 -> titlebar search: Search page opens, titlebar search hides, breadcrumb says ← 本地音乐.
收藏: titlebar shows 搜索歌曲、专辑、歌手.
收藏 -> titlebar search: Search page opens, titlebar search hides, breadcrumb says ← 收藏.
我的: titlebar search hidden.
设置: titlebar search hidden.
Search page empty query: shows 最近搜索 and no local-library sidebar/right overview/search suggestions.
```

- [ ] **Step 5: Commit final verification fixes**

If Step 1-4 required code fixes, commit those files:

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic
git commit -m "完善桌面搜索验收修正"
```

If Step 1-4 required no code fixes, skip this commit step and record the passing commands in the final handoff.

## Self-Review

**Spec coverage:** Covered entrance visibility, Home/Favorites contexts, Profile/Settings hiding, Search page duplicate-input removal, sidebar filter boundary, history isolation, no persistent result cache, result data ranges, visual acceptance, and controller tests.

**Placeholder scan:** This plan contains no unresolved markers or unspecified implementation steps. Every code-changing step includes concrete Kotlin snippets or exact replacement instructions.

**Type consistency:** The plan consistently uses `SearchContext.LocalLibrary`, `SearchContext.Favorites`, `SecondaryScreen.Search(context = ...)`, `MusicAppUiState.searchContext`, `MusicAppUiState.searchHistoryFor(...)`, and `MusicAppUiState.shouldShowTitlebarMusicSearch`.

## Adversarial Review Before Execution

### Blocking Findings

1. **Task 4 points the sidebar placeholder change at the wrong file.**
   The plan says to replace `搜索本地库` in `DesktopMusicComponents.kt`, but the current sidebar input lives in `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopLibrarySidebar.kt`. The file list also omits `DesktopLibrarySidebar.kt`, so an executor following the plan literally may leave the sidebar copy and action stale. Root fix: add `DesktopLibrarySidebar.kt` to Task 4 files and update Step 6 to change `DesktopLibrarySearch`.

2. **Task 4 leaves the sidebar "filter" as a dead clickable surface.**
   The design says the sidebar input should be a local filter that focuses and filters the current local library list. The plan changes `onSearch = controller::openSearch` to `onSearch = {}`, which prevents navigation but does not implement filtering or focus. This is acceptable only if the plan explicitly scopes local filtering out. Otherwise it violates the design contract. Root fix: either implement local filter state and tests, or rename the step to "temporarily disable sidebar navigation" and mark real filtering as a follow-up.

3. **Favorites search is implemented as favorite songs only, but the design says favorite entities.**
   Task 2 selects `uiState.favoriteSongs` and runs `buildSearchResult` over songs, which means favorite albums/artists are derived from favorite songs. The design explicitly says Favorites searches "已收藏歌曲、已收藏专辑、已收藏歌手" and that a favorite song must not implicitly make its album/artist a favorite album/artist. Root fix: either search `favoriteSongs`, `favoriteAlbums`, and `favoriteArtists` as separate entity inputs, or change the product/design document before implementation.

4. **The plan makes `buildSearchResult` public instead of fixing the domain API shape.**
   Exposing a helper from `SearchMusicUseCase.kt` solves reuse quickly, but it leaks a list-based implementation detail into app state. Since the new behavior needs alternate sources, a cleaner root fix is to extend the use case with a list-backed overload or a `SearchCorpus` model that accepts songs/albums/artists. Public helper is tolerable for MVP, but the plan should call this a deliberate boundary tradeoff.

5. **Android/common UI references are under-specified after `SecondaryScreen.Search` becomes a data class.**
   The plan mentions replacing object-style references, but mobile/common `MusicApp.kt` also switches on `SecondaryScreen.Search`. The implementation must update those branches to `is SecondaryScreen.Search`, or Android compilation will fail. Task 1 should explicitly list `MusicApp.kt` in affected files or state that the source check covers common UI references.

6. **History tests do not cover the design's real write conditions.**
   Task 3 tests manual commit, dedupe, and clear. It does not prove that history is written on Enter, result click, album open, or artist open, nor that typing without commit is ignored. Task 5 wires some of these calls in UI, but there are no controller-level tests for the rule. Root fix: add tests for "typing only does not record" and "select/open result records".

7. **The final regex may reject valid type checks.**
   `rg -n "SecondaryScreen\\.Search(?!\\()"` will match valid Kotlin type checks like `is SecondaryScreen.Search`, even though the expected text says type checks are allowed. Root fix: use a narrower stale-object check, for example search for assignment/comparison patterns manually, or document that `is SecondaryScreen.Search` matches are expected and must be inspected.

### Non-Blocking Risks

- `openSearch(context = LocalLibrary)` calls `loadLocalMusicLibrary()`, while `Favorites` does not. If favorite albums/artists eventually depend on a full local snapshot, Favorites may need a different preload path.
- `searchContext` is stored both in `MusicAppUiState` and in `SecondaryScreen.Search(context)`. The plan should keep them synchronized and preferably treat the route context as the source of truth when rendering.
- The titlebar route maps `RootTab.Me` to `LocalLibrary` even though `shouldShowTitlebarMusicSearch` hides the search on Me. It is currently unreachable but still semantically misleading; better to early-return or map only Home/Favorites.
- The visual smoke check is manual only. If execution changes layout substantially, add a desktop screenshot or at least document that visual verification was not performed.

## Generalization Questions And Document Answerability

- **Can the document answer whether Home search uses local scanned data rather than mock/seed data?** Yes. The design and plan both say LocalLibrary searches current local scanned music, and the plan loads local music before Home search.
- **Can it answer whether Favorites search should include favorite albums/artists that were explicitly favorited?** Partially. The design says yes, but the implementation plan contradicts it by deriving albums/artists from favorite songs only. This must be resolved before coding.
- **Can it answer whether My/Profile shows titlebar search?** Yes. The plan goal says Profile/Settings hide the titlebar search, and Task 4 tests `RootTab.Me` as hidden.
- **Can it answer whether Settings can search settings items?** No for implementation, yes for product boundary. The design says future settings search must be separate; the implementation plan correctly does not implement it.
- **Can it answer whether the search page has one or two inputs?** Yes. Both documents require the search page to hide titlebar search and use the page input only.
- **Can it answer whether sidebar filtering should actually filter?** No. The design says it filters locally; the plan only disables navigation and changes copy. This is an implementation gap.
- **Can it answer whether search history is persisted?** Yes. The plan explicitly implements runtime history only.
- **Can it answer what happens when switching root tabs while search is open?** Partially. The design lists it as a suggested test; the implementation plan relies on existing `navigateToRoot` behavior but does not add a regression test for contextual search.
- **Can it answer how empty query results should behave?** Mostly. The UI plan shows history on blank query, while `buildSearchResult` returns all content for blank query. The screen avoids calling results for blank query, so this is safe if UI remains the only caller.
- **Can it answer how album/artist result clicks affect navigation context?** Partially. Task 5 commits history then opens detail pages, but it does not specify return behavior from detail back to search versus root. Current controller likely returns to previous root, so this should be accepted or changed explicitly.

## Three-Round Cross Review

### Round 1: Product Contract

- The core entry rule is sound: Home and Favorites are searchable content roots; Me and Settings are not.
- The largest product mismatch is Favorites entity semantics. Searching favorite songs only is not the same as searching favorite songs, favorite albums, and favorite artists.
- The sidebar filter remains ambiguous: users will see "筛选本地库", but the plan does not make it a working filter. A disabled click may feel broken.

### Round 2: Architecture And State

- `SearchContext` belongs in shared app/navigation state; that part is a good root fix.
- Duplicating context in route and `MusicAppUiState.searchContext` is manageable but needs invariant tests. A safer pattern is deriving UI context from `SecondaryScreen.Search.context` whenever the secondary screen is Search.
- Making `buildSearchResult` public is a boundary shortcut. A domain-level corpus/search-source API would better support Favorites entity search without forcing albums/artists to be rebuilt from songs.
- Titlebar visibility belongs in shared state or shell-level derivation. The plan's helper is aligned with the architecture.

### Round 3: Test And Execution

- The TDD sequencing is useful, but Task 4 and Task 5 need compile-aware file lists that match the current codebase.
- Tests cover entry, result isolation, and history isolation, but miss sidebar non-navigation, sidebar local filtering, stale-context sync, and history write conditions.
- The verification grep must be fixed before use; otherwise it can report allowed `is SecondaryScreen.Search` type checks as failures.
- Before implementation starts, update the plan for `DesktopLibrarySidebar.kt` and resolve Favorites entity search. Otherwise the executor is likely to ship a behavior that passes narrow tests but violates the design.
