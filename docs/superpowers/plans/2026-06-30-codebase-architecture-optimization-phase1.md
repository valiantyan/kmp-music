# Codebase Architecture Optimization Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the first-stage App state trunk so `MusicAppController` remains the public facade while navigation, search, library projection, favorites, playback UI sync, snapshot restore, and lightweight session state move into focused collaborators without changing user-visible behavior.

**Architecture:** Keep `MusicAppController` as the only owner of Compose `mutableStateOf` and the only public entry point used by UI and platform hosts. New collaborators live under `feature/app/*`, accept immutable inputs or repositories, and return updated `MusicAppUiState` or small result objects. Tests move from one large facade file into focused files, while `MusicAppControllerTest` keeps only cross-responsibility acceptance behavior.

**Tech Stack:** Kotlin Multiplatform 2.0.21, Compose Multiplatform state in `commonMain`, Kotlin test, kotlinx.coroutines test, existing domain repositories/use cases/playback coordinator.

---

## Scope Check

This plan implements only the first stage from `docs/superpowers/specs/2026-06-30-codebase-architecture-optimization-design.md`: the App state trunk around `MusicAppController.kt` and `MusicAppControllerTest.kt`.

This plan does not implement:

- `PlaybackCoordinator.kt` internal strategy extraction.
- `MusicApp.kt` or Desktop UI file splitting.
- Android/Desktop/iOS platform adapter splitting.
- Product behavior changes, visual changes, or new features.

## File Structure

Create these focused production files:

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicLibraryProjector.kt`  
  Pure song-to-album/artist projection and detail/favorites projection helpers.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/navigation/NavigationStateController.kt`  
  Pure reducers for root/secondary navigation and queue/more menu closing during navigation.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchSessionController.kt`  
  Search query state, active-query debounce, history persistence, and context-isolated history reducers.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library/LibraryStateSynchronizer.kt`  
  Initial scan state, scan snapshot sync, full-library load sync, recent playback visible list, favorite entity completion, and scan permission gate state.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/favorites/FavoriteStateSynchronizer.kt`  
  Favorite toggling state projection across home preview, full library, favorite list, recent list, current song, and queue snapshot.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackUiStateSynchronizer.kt`  
  Projection from `PlaybackRepository` state and queue state to `MusicAppUiState`.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackRestoreOrchestrator.kt`  
  Snapshot restore orchestration: saved queue ids, known song resolution, empty-library pending state, and calling `PlaybackCoordinator.restoreSnapshot`.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/session/LoginAndDialogStateController.kt`  
  Email, mail-sent state, clear-cache dialog, queue dialog, and more-menu reducers that do not touch library/playback/search repositories.

Modify these production files:

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`  
  Replace duplicate album/artist grouping with `MusicLibraryProjector`.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`  
  Instantiate collaborators, keep public methods stable, and delegate internal state changes.

Create these focused tests:

- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicLibraryProjectorTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/MusicAppNavigationControllerTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/search/MusicAppSearchControllerTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicAppLibraryStateSynchronizerTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/favorites/MusicAppFavoriteStateSynchronizerTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackUiStateSynchronizerTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackRestoreOrchestratorTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/session/LoginAndDialogStateControllerTest.kt`

Modify this existing test file:

- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`  
  Keep facade acceptance tests only; remove duplicated unit-level details after the focused test files cover them.

## Commit Cadence

Use one commit per task. Each task must leave the project compiling and its targeted tests passing. The final task runs full verification.

---

### Task 1: Extract MusicLibraryProjector

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicLibraryProjector.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicLibraryProjectorTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`

- [x] **Step 1: Write the failing projector tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicLibraryProjectorTest.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.library

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals

class MusicLibraryProjectorTest {
    @Test
    fun albumsUseStableLowercaseTrimmedIdsAndPreserveFirstSongArtwork(): Unit {
        val songs: List<Song> = listOf(
            testSong(id = "1", title = "First", album = " River Year ", artist = "Trip", coverImageUri = "file://first.png"),
            testSong(id = "2", title = "Second", album = "river year", artist = "Trip", coverImageUri = "file://second.png"),
            testSong(id = "3", title = "Third", album = "Summer", artist = "Aki", coverImageUri = "file://third.png"),
        )

        val albums = MusicLibraryProjector.buildAlbums(songs = songs)

        assertEquals(expected = listOf("album:river year", "album:summer"), actual = albums.map { album -> album.id })
        assertEquals(expected = listOf(2, 1), actual = albums.map { album -> album.songCount })
        assertEquals(expected = "file://first.png", actual = albums.first().coverImageUri)
        assertEquals(expected = "本地音乐", actual = albums.first().mood)
        assertEquals(expected = "本地", actual = albums.first().year)
    }

    @Test
    fun artistsUseStableLowercaseTrimmedIdsAndPreserveFirstSongArtwork(): Unit {
        val songs: List<Song> = listOf(
            testSong(id = "1", title = "First", album = "One", artist = " Trip ", coverImageUri = "file://first.png"),
            testSong(id = "2", title = "Second", album = "Two", artist = "trip", coverImageUri = "file://second.png"),
            testSong(id = "3", title = "Third", album = "Three", artist = "Aki", coverImageUri = "file://third.png"),
        )

        val artists = MusicLibraryProjector.buildArtists(songs = songs)

        assertEquals(expected = listOf("artist:aki", "artist:trip"), actual = artists.map { artist -> artist.id })
        assertEquals(expected = listOf(1, 2), actual = artists.map { artist -> artist.songCount })
        assertEquals(expected = "file://third.png", actual = artists.first().coverImageUri)
        assertEquals(expected = "本地音乐", actual = artists.first().tag)
    }

    @Test
    fun detailSongsDeduplicateBySongIdInQueueLocalHomeFavoriteOrder(): Unit {
        val queue = listOf(testSong(id = "queue", title = "Queue"), testSong(id = "same", title = "Queue Same"))
        val local = listOf(testSong(id = "same", title = "Local Same"), testSong(id = "local", title = "Local"))
        val home = listOf(testSong(id = "home", title = "Home"))
        val favorites = listOf(testSong(id = "favorite", title = "Favorite"))

        val detailSongs = MusicLibraryProjector.buildDetailSongs(
            queueSongsSnapshot = queue,
            localSongs = local,
            homeLocalSongPreview = home,
            favoriteSongs = favorites,
        )

        assertEquals(expected = listOf("queue", "same", "local", "home", "favorite"), actual = detailSongs.map { song -> song.id })
        assertEquals(expected = "Queue Same", actual = detailSongs[1].title)
    }

    private fun testSong(
        id: String,
        title: String,
        album: String = "Album",
        artist: String = "Artist",
        coverImageUri: String? = null,
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = "03:12",
            durationMs = 192_000L,
            coverArt = CoverArt.Generated,
            coverImageUri = coverImageUri,
            isLiked = false,
        )
    }
}
```

- [x] **Step 2: Run the projector test and verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.library.MusicLibraryProjectorTest"
```

Expected: FAIL with an unresolved reference to `MusicLibraryProjector`.

- [x] **Step 3: Add `MusicLibraryProjector`**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicLibraryProjector.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.library

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.Song

/**
 * Pure projection helpers for deriving album and artist UI models from songs.
 */
object MusicLibraryProjector {
    fun buildAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { song: Song -> song.album.trim().lowercase() }
            .values
            .map { albumSongs: List<Song> ->
                val firstSong: Song = albumSongs.first()
                Album(
                    id = "album:${firstSong.album.trim().lowercase()}",
                    title = firstSong.album,
                    artist = firstSong.artist,
                    songCount = albumSongs.size,
                    coverArt = firstSong.coverArt,
                    coverImageUri = firstSong.coverImageUri,
                    mood = "本地音乐",
                    year = "本地",
                )
            }
            .sortedBy { album: Album -> album.title.lowercase() }
    }

    fun buildArtists(songs: List<Song>): List<Artist> {
        return songs.groupBy { song: Song -> song.artist.trim().lowercase() }
            .values
            .map { artistSongs: List<Song> ->
                val firstSong: Song = artistSongs.first()
                Artist(
                    id = "artist:${firstSong.artist.trim().lowercase()}",
                    name = firstSong.artist,
                    songCount = artistSongs.size,
                    coverArt = firstSong.coverArt,
                    coverImageUri = firstSong.coverImageUri,
                    tag = "本地音乐",
                )
            }
            .sortedBy { artist: Artist -> artist.name.lowercase() }
    }

    fun buildDetailSongs(
        queueSongsSnapshot: List<Song>,
        localSongs: List<Song>,
        homeLocalSongPreview: List<Song>,
        favoriteSongs: List<Song>,
    ): List<Song> {
        return (queueSongsSnapshot + localSongs + homeLocalSongPreview + favoriteSongs)
            .distinctBy { song: Song -> song.id }
    }
}
```

- [x] **Step 4: Replace duplicate projection in `MusicAppModels.kt`**

Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`:

```kotlin
import com.yanhao.kmpmusic.feature.app.library.MusicLibraryProjector
```

Replace the existing `detailSongs`, `detailAlbums`, `detailArtists`, `favoriteAlbums`, and `favoriteArtists` getters with:

```kotlin
    val detailSongs: List<Song>
        get() = MusicLibraryProjector.buildDetailSongs(
            queueSongsSnapshot = queueSongsSnapshot,
            localSongs = localSongs,
            homeLocalSongPreview = homeLocalSongPreview,
            favoriteSongs = favoriteSongs,
        )

    val detailAlbums: List<Album>
        get() = (localAlbums + MusicLibraryProjector.buildAlbums(songs = detailSongs))
            .distinctBy { album -> album.id }

    val detailArtists: List<Artist>
        get() = (localArtists + MusicLibraryProjector.buildArtists(songs = detailSongs))
            .distinctBy { artist -> artist.id }

    val favoriteAlbums: List<Album>
        get() = MusicLibraryProjector.buildAlbums(songs = favoriteSongs)

    val favoriteArtists: List<Artist>
        get() = MusicLibraryProjector.buildArtists(songs = favoriteSongs)
```

Delete the private `buildAlbumsFromSongs` and `buildArtistsFromSongs` functions from `MusicAppUiState`.

- [x] **Step 5: Replace duplicate projection in `MusicAppController.kt`**

Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`:

```kotlin
import com.yanhao.kmpmusic.feature.app.library.MusicLibraryProjector
```

Replace the `syncLibrarySnapshot` local album assignment:

```kotlin
localAlbums = if (shouldRefreshFullLibrary) {
    MusicLibraryProjector.buildAlbums(songs = fullSongsWithLikes)
} else {
    uiState.localAlbums
}
```

Replace the `loadLocalMusicLibrary` local album assignment:

```kotlin
localAlbums = MusicLibraryProjector.buildAlbums(songs = songsWithLikes)
```

Replace the `syncLibrarySnapshot` local artist assignment:

```kotlin
localArtists = if (shouldRefreshFullLibrary) {
    MusicLibraryProjector.buildArtists(songs = fullSongsWithLikes)
} else {
    uiState.localArtists
}
```

Replace the `loadLocalMusicLibrary` local artist assignment:

```kotlin
localArtists = MusicLibraryProjector.buildArtists(songs = songsWithLikes)
```

Delete the private `buildAlbums` and `buildArtists` functions from `MusicAppController`.

- [x] **Step 6: Run focused and facade tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.library.MusicLibraryProjectorTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.favoriteSongsRemainAvailableBeforeFullLibraryLoads"
```

Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicLibraryProjector.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicLibraryProjectorTest.kt
git commit -m "重构曲库专辑歌手投影"
```

---

### Task 2: Extract NavigationStateController

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/navigation/NavigationStateController.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/MusicAppNavigationControllerTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`

- [x] **Step 1: Write failing navigation reducer tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/MusicAppNavigationControllerTest.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.navigation

import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.NavigationState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class MusicAppNavigationControllerTest {
    @Test
    fun navigateToSecondaryStoresPreviousRootAndClosesTransientOverlays(): Unit {
        val state = testState().copy(
            navigationState = NavigationState(rootTab = RootTab.Favorites, previousRootTab = RootTab.Favorites),
            isQueueOpen = true,
            moreSongId = "song-1",
        )

        val nextState = NavigationStateController.navigateToSecondary(
            state = state,
            screen = SecondaryScreen.Search(context = SearchContext.Favorites),
        )

        assertEquals(expected = RootTab.Favorites, actual = nextState.navigationState.previousRootTab)
        assertEquals(expected = SecondaryScreen.Search(context = SearchContext.Favorites), actual = nextState.navigationState.secondaryScreen)
        assertEquals(expected = 1, actual = nextState.navigationState.secondaryEntryId)
        assertFalse(actual = nextState.isQueueOpen)
        assertNull(actual = nextState.moreSongId)
    }

    @Test
    fun navigateToRootClearsSecondaryAndUsesTargetRootAsPreviousRoot(): Unit {
        val state = testState().copy(
            navigationState = NavigationState(
                rootTab = RootTab.Home,
                previousRootTab = RootTab.Home,
                secondaryScreen = SecondaryScreen.Player,
                secondaryEntryId = 4,
            ),
            isQueueOpen = true,
            moreSongId = "song-1",
        )

        val nextState = NavigationStateController.navigateToRoot(state = state, tab = RootTab.Me)

        assertEquals(expected = NavigationState(rootTab = RootTab.Me, previousRootTab = RootTab.Me), actual = nextState.navigationState)
        assertFalse(actual = nextState.isQueueOpen)
        assertNull(actual = nextState.moreSongId)
    }

    @Test
    fun navigateBackReturnsToPreviousRootWithoutChangingEntryId(): Unit {
        val state = testState().copy(
            navigationState = NavigationState(
                rootTab = RootTab.Favorites,
                previousRootTab = RootTab.Me,
                secondaryScreen = SecondaryScreen.Player,
                secondaryEntryId = 3,
            ),
        )

        val nextState = NavigationStateController.navigateBack(state = state)

        assertEquals(expected = RootTab.Me, actual = nextState.navigationState.rootTab)
        assertNull(actual = nextState.navigationState.secondaryScreen)
        assertEquals(expected = 3, actual = nextState.navigationState.secondaryEntryId)
    }

    private fun testState(): MusicAppUiState {
        return MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
            homeLocalSongPreview = listOf(
                Song(
                    id = "song-1",
                    title = "Song",
                    artist = "Artist",
                    album = "Album",
                    duration = "03:00",
                    durationMs = 180_000L,
                ),
            ),
        )
    }
}
```

- [x] **Step 2: Run navigation tests and verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest"
```

Expected: FAIL with an unresolved reference to `NavigationStateController`.

- [x] **Step 3: Add navigation reducer**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/navigation/NavigationStateController.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.navigation

import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.NavigationState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen

/**
 * Pure navigation reducers for App root and secondary routes.
 */
object NavigationStateController {
    fun navigateToSecondary(state: MusicAppUiState, screen: SecondaryScreen): MusicAppUiState {
        return state.copy(
            navigationState = state.navigationState.copy(
                secondaryScreen = screen,
                previousRootTab = state.navigationState.rootTab,
                secondaryEntryId = state.navigationState.secondaryEntryId + 1,
            ),
            isQueueOpen = false,
            moreSongId = null,
        )
    }

    fun navigateToRoot(state: MusicAppUiState, tab: RootTab): MusicAppUiState {
        return state.copy(
            navigationState = NavigationState(
                rootTab = tab,
                previousRootTab = tab,
            ),
            isQueueOpen = false,
            moreSongId = null,
        )
    }

    fun navigateBack(state: MusicAppUiState): MusicAppUiState {
        return state.copy(
            navigationState = state.navigationState.copy(
                rootTab = state.navigationState.previousRootTab,
                secondaryScreen = null,
            ),
        )
    }
}
```

- [x] **Step 4: Wire public navigation methods through the reducer**

Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`:

```kotlin
import com.yanhao.kmpmusic.feature.app.navigation.NavigationStateController
```

Replace the bodies of these methods:

```kotlin
fun navigateToSecondary(screen: SecondaryScreen) {
    commitActiveSearchQueryToHistoryIfNeeded()
    uiState = NavigationStateController.navigateToSecondary(
        state = uiState,
        screen = screen,
    )
}

fun navigateToRoot(tab: RootTab) {
    commitActiveSearchQueryToHistoryIfNeeded()
    uiState = NavigationStateController.navigateToRoot(
        state = uiState,
        tab = tab,
    )
}

fun navigateBack() {
    commitActiveSearchQueryToHistoryIfNeeded()
    uiState = NavigationStateController.navigateBack(state = uiState)
}
```

Do not change `handleSystemBack()` yet; it still coordinates dialog, menu, queue, and navigation closing order through existing public methods.

- [x] **Step 5: Run focused and facade navigation tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.rootNavigationClearsSecondaryScreen" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.systemBackClosesOverlayBeforeSecondaryScreen"
```

Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/navigation/NavigationStateController.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/MusicAppNavigationControllerTest.kt
git commit -m "重构应用导航状态控制"
```

---

### Task 3: Extract SearchSessionController

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchSessionController.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/search/MusicAppSearchControllerTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [x] **Step 1: Write focused search tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/search/MusicAppSearchControllerTest.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.search

import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.repository.SearchHistoryRepository
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
```

- [x] **Step 2: Run search tests and verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.search.MusicAppSearchControllerTest"
```

Expected: FAIL with an unresolved reference to `SearchSessionController`.

- [x] **Step 3: Add `SearchSessionController`**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchSessionController.kt`:

```kotlin
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
 * Handles search input state, active-query debounce, and context-specific history.
 */
class SearchSessionController(
    private val searchHistoryRepository: SearchHistoryRepository,
    private val controllerScope: CoroutineScope,
    private val debounceMillis: Long,
    private val publishStateUpdate: ((MusicAppUiState) -> MusicAppUiState) -> Unit,
) {
    private var debounceJob: Job? = null

    fun openSearch(state: MusicAppUiState, context: SearchContext): MusicAppUiState {
        return syncActiveSearchQueryImmediately(state = state, query = "").copy(
            searchContext = context,
            searchQuery = "",
            activeSearchQuery = "",
            searchScope = SearchScope.All,
        )
    }

    fun setSearchQuery(state: MusicAppUiState, query: String): MusicAppUiState {
        val previousQuery: String = state.searchQuery
        if (shouldCommitSearchQueryBeforeClearing(state = state, previousQuery = previousQuery, nextQuery = query)) {
            val committedState = commitSearchQueryToHistory(
                state = state,
                query = previousQuery,
                context = state.searchContext,
            )
            return syncActiveSearchQueryImmediately(state = committedState, query = "").copy(searchQuery = query)
        }
        val nextState = state.copy(searchQuery = query)
        return scheduleActiveSearchQuerySync(state = nextState, query = query)
    }

    fun setSearchScope(state: MusicAppUiState, scope: SearchScope): MusicAppUiState {
        return state.copy(searchScope = scope)
    }

    fun commitSearchQueryToHistory(state: MusicAppUiState): MusicAppUiState {
        val syncedState = syncActiveSearchQueryImmediately(state = state, query = state.searchQuery)
        return commitSearchQueryToHistory(
            state = syncedState,
            query = syncedState.searchQuery,
            context = syncedState.searchContext,
        )
    }

    fun selectSearchHistory(state: MusicAppUiState, query: String): MusicAppUiState {
        val nextState = syncActiveSearchQueryImmediately(state = state, query = query).copy(searchQuery = query)
        return commitSearchQueryToHistory(state = nextState)
    }

    fun removeSearchHistoryItem(state: MusicAppUiState, context: SearchContext, query: String): MusicAppUiState {
        return updateSearchHistory(
            state = state,
            context = context,
            history = state.searchHistoryFor(context = context).filterNot { item -> item == query },
        )
    }

    fun clearSearchHistory(state: MusicAppUiState, context: SearchContext): MusicAppUiState {
        return updateSearchHistory(state = state, context = context, history = emptyList())
    }

    fun commitActiveSearchQueryToHistoryIfNeeded(state: MusicAppUiState): MusicAppUiState {
        if (state.navigationState.secondaryScreen !is SecondaryScreen.Search) {
            return state
        }
        return commitSearchQueryToHistory(state = state)
    }

    private fun shouldCommitSearchQueryBeforeClearing(
        state: MusicAppUiState,
        previousQuery: String,
        nextQuery: String,
    ): Boolean {
        return state.navigationState.secondaryScreen is SecondaryScreen.Search &&
            previousQuery.trim().isNotBlank() &&
            nextQuery.isBlank()
    }

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

    private fun scheduleActiveSearchQuerySync(state: MusicAppUiState, query: String): MusicAppUiState {
        debounceJob?.cancel()
        if (query.isBlank()) {
            return syncActiveSearchQueryImmediately(state = state, query = query)
        }
        debounceJob = controllerScope.launch {
            delay(timeMillis = debounceMillis)
            publishStateUpdate { currentState ->
                currentState.copy(activeSearchQuery = query)
            }
        }
        return state
    }

    private fun syncActiveSearchQueryImmediately(state: MusicAppUiState, query: String): MusicAppUiState {
        debounceJob?.cancel()
        debounceJob = null
        return state.copy(activeSearchQuery = query)
    }

    private fun moveQueryToHistoryTop(query: String, currentHistory: List<String>): List<String> {
        return (listOf(query) + currentHistory.filterNot { item -> item == query })
            .take(n = 10)
    }

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
```

- [x] **Step 4: Wire `MusicAppController` search methods**

Add a property in `MusicAppController`:

```kotlin
private val searchSessionController: SearchSessionController = SearchSessionController(
    searchHistoryRepository = searchHistoryRepository,
    controllerScope = controllerScope,
    debounceMillis = searchQueryDebounceMillis,
    publishStateUpdate = { reducer -> uiState = reducer(uiState) },
)
```

Replace method bodies:

```kotlin
fun setSearchQuery(query: String) {
    uiState = searchSessionController.setSearchQuery(state = uiState, query = query)
}

fun setSearchScope(scope: SearchScope) {
    uiState = searchSessionController.setSearchScope(state = uiState, scope = scope)
}

fun commitSearchQueryToHistory() {
    uiState = searchSessionController.commitSearchQueryToHistory(state = uiState)
}

fun selectSearchHistory(query: String) {
    uiState = searchSessionController.selectSearchHistory(state = uiState, query = query)
}

fun removeSearchHistoryItem(context: SearchContext, query: String) {
    uiState = searchSessionController.removeSearchHistoryItem(
        state = uiState,
        context = context,
        query = query,
    )
}

fun clearSearchHistory(context: SearchContext = uiState.searchContext) {
    uiState = searchSessionController.clearSearchHistory(
        state = uiState,
        context = context,
    )
}

private fun commitActiveSearchQueryToHistoryIfNeeded() {
    uiState = searchSessionController.commitActiveSearchQueryToHistoryIfNeeded(state = uiState)
}
```

In `openSearch`, replace only the search reset lines:

```kotlin
uiState = searchSessionController.openSearch(
    state = uiState,
    context = context,
)
navigateToSecondary(screen = SecondaryScreen.Search(context = context))
```

Delete `searchQueryDebounceJob` and these private search helpers from `MusicAppController`: `scheduleActiveSearchQuerySync`, `syncActiveSearchQueryImmediately`, `moveQueryToHistoryTop`, `updateSearchHistory`, and the overloaded private `commitSearchQueryToHistory(query, context)`.

- [x] **Step 5: Run search tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.search.MusicAppSearchControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchHistoryIsIsolatedByContext" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.nonBlankSearchQueryCommitsToHistoryWhenLeavingSearch"
```

Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchSessionController.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/search/MusicAppSearchControllerTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "重构搜索会话状态控制"
```

---

### Task 4: Extract LibraryStateSynchronizer

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library/LibraryStateSynchronizer.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicAppLibraryStateSynchronizerTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`

- [x] **Step 1: Write focused library synchronizer tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicAppLibraryStateSynchronizerTest.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.library

import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MusicAppLibraryStateSynchronizerTest {
    @Test
    fun buildInitialScanStateReflectsPersistedLibraryWithoutLoadingSongs(): Unit {
        val synchronizer = createSynchronizer(stats = LibraryStats(songCount = 5))

        val scanState = synchronizer.buildInitialScanState(stats = LibraryStats(songCount = 5))

        assertIs<LocalMusicScanState.Done>(scanState)
        assertEquals(expected = 5, actual = scanState.summary.addedCount)
    }

    @Test
    fun permissionPermanentlyDeniedRequiresConfirmationBeforeScanningAgain(): Unit {
        val synchronizer = createSynchronizer()
        val state = testState().copy(
            scanState = LocalMusicScanState.Error(
                error = LocalMusicScanError(
                    type = LocalMusicScanErrorType.PermissionPermanentlyDenied,
                    message = "permission denied",
                ),
            ),
        )

        assertTrue(actual = synchronizer.shouldConfirmPermissionSettingsBeforeScan(state = state))
    }

    @Test
    fun syncLibrarySnapshotRefreshesFullLibraryWhenLocalSongsAlreadyLoaded(): Unit {
        val repository = FakeMusicLibraryRepository(
            homeSongs = listOf(testSong(id = "home", title = "Home")),
            allSongs = listOf(testSong(id = "liked", title = "Liked", isLiked = false), testSong(id = "local", title = "Local")),
            stats = LibraryStats(songCount = 2),
        )
        val synchronizer = createSynchronizer(repository = repository, likedIds = setOf("liked"))
        val state = testState().copy(localSongs = listOf(testSong(id = "old", title = "Old")))

        val nextState = synchronizer.syncLibrarySnapshot(
            state = state,
            snapshot = LibrarySnapshot(
                songs = repository.getAllAvailableSongs(),
                albums = MusicLibraryProjector.buildAlbums(songs = repository.getAllAvailableSongs()),
                artists = MusicLibraryProjector.buildArtists(songs = repository.getAllAvailableSongs()),
                stats = LibraryStats(songCount = 2),
                sources = emptyList(),
                scanState = LocalMusicScanState.Done(
                    summary = LocalMusicLastScanSummary(
                        addedCount = 2,
                        updatedCount = 0,
                        removedCount = 0,
                        problemCount = 0,
                        completedAt = 0L,
                    ),
                ),
                lastScanSummary = null,
                problems = emptyList(),
            ),
        )

        assertEquals(expected = listOf("liked", "local"), actual = nextState.localSongs.map { song -> song.id })
        assertEquals(expected = listOf("liked"), actual = nextState.favoriteSongs.map { song -> song.id })
        assertTrue(actual = nextState.localSongs.first().isLiked)
    }

    @Test
    fun loadLocalMusicLibraryBuildsRecentSongsFromPlaybackHistory(): Unit {
        val playbackRepository = InMemoryPlaybackRepository()
        playbackRepository.savePlaybackHistory(history = PlaybackHistory(songIds = listOf("song-2")))
        val repository = FakeMusicLibraryRepository(
            allSongs = listOf(testSong(id = "song-1", title = "One"), testSong(id = "song-2", title = "Two")),
        )
        val synchronizer = createSynchronizer(repository = repository, playbackRepository = playbackRepository)

        val nextState = synchronizer.loadLocalMusicLibrary(state = testState())

        assertEquals(expected = listOf("song-1", "song-2"), actual = nextState.localSongs.map { song -> song.id })
        assertEquals(expected = listOf("song-2"), actual = nextState.recentSongs.map { song -> song.id })
    }

    @Test
    fun loadLocalMusicLibraryDoesNothingWhenSongsAlreadyLoaded(): Unit {
        val repository = FakeMusicLibraryRepository(allSongs = listOf(testSong(id = "repo", title = "Repo")))
        val synchronizer = createSynchronizer(repository = repository)
        val state = testState().copy(localSongs = listOf(testSong(id = "existing", title = "Existing")))

        val nextState = synchronizer.loadLocalMusicLibrary(state = state)

        assertEquals(expected = listOf("existing"), actual = nextState.localSongs.map { song -> song.id })
        assertFalse(actual = repository.allSongsRead)
    }

    private fun createSynchronizer(
        repository: FakeMusicLibraryRepository = FakeMusicLibraryRepository(),
        playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository(),
        stats: LibraryStats = LibraryStats(),
        likedIds: Set<String> = emptySet(),
    ): LibraryStateSynchronizer {
        return LibraryStateSynchronizer(
            musicLibraryRepository = repository.copyWithStats(stats = stats),
            favoritesRepository = InMemoryFavoritesRepository(initialLikedSongIds = likedIds),
            playbackRepository = playbackRepository,
        )
    }

    private fun testState(): MusicAppUiState {
        return MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        )
    }

    private fun testSong(id: String, title: String, isLiked: Boolean = false): Song {
        return Song(
            id = id,
            title = title,
            artist = "Artist",
            album = "Album",
            duration = "03:00",
            durationMs = 180_000L,
            isLiked = isLiked,
        )
    }
}

private class FakeMusicLibraryRepository(
    private val homeSongs: List<Song> = emptyList(),
    private val allSongs: List<Song> = emptyList(),
    private val stats: LibraryStats = LibraryStats(songCount = allSongs.size),
) : MusicLibraryRepository {
    var allSongsRead: Boolean = false
        private set

    fun copyWithStats(stats: LibraryStats): FakeMusicLibraryRepository {
        return FakeMusicLibraryRepository(homeSongs = homeSongs, allSongs = allSongs, stats = stats)
    }

    override fun getSnapshot(): LibrarySnapshot {
        return LibrarySnapshot(
            songs = allSongs,
            albums = MusicLibraryProjector.buildAlbums(songs = allSongs),
            artists = MusicLibraryProjector.buildArtists(songs = allSongs),
            stats = stats,
            sources = emptyList(),
            scanState = LocalMusicScanState.Idle,
            lastScanSummary = null,
            problems = emptyList(),
        )
    }

    override fun getHomePreview(limit: Int): List<Song> {
        return homeSongs.take(n = limit)
    }

    override fun getAllAvailableSongs(): List<Song> {
        allSongsRead = true
        return allSongs
    }

    override fun getAvailableSongsByIds(songIds: List<String>): List<Song> {
        return allSongs.filter { song -> songIds.contains(song.id) }
    }

    override fun getLibraryStats(): LibraryStats {
        return stats
    }

    override fun applyScanResult(
        request: com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest,
        scanResult: com.yanhao.kmpmusic.domain.model.LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot {
        return LibrarySnapshot(
            songs = allSongs,
            albums = MusicLibraryProjector.buildAlbums(songs = allSongs),
            artists = MusicLibraryProjector.buildArtists(songs = allSongs),
            stats = stats,
            sources = scanResult.sourceSummaries,
            scanState = LocalMusicScanState.Idle,
            lastScanSummary = null,
            problems = scanResult.failed,
        )
    }
}
```

- [x] **Step 2: Run library tests and verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.library.MusicAppLibraryStateSynchronizerTest"
```

Expected: FAIL with an unresolved reference to `LibraryStateSynchronizer`.

- [x] **Step 3: Add `LibraryStateSynchronizer`**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library/LibraryStateSynchronizer.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.library

import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.FavoritesRepository
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen

/**
 * Synchronizes library repository facts into immutable App UI state.
 */
class LibraryStateSynchronizer(
    private val musicLibraryRepository: MusicLibraryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackRepository: PlaybackRepository,
) {
    companion object {
        fun buildInitialScanStateFromStats(stats: LibraryStats): LocalMusicScanState {
            if (stats.songCount <= 0) {
                return LocalMusicScanState.Idle
            }
            return LocalMusicScanState.Done(
                summary = LocalMusicLastScanSummary(
                    addedCount = stats.songCount,
                    updatedCount = 0,
                    removedCount = 0,
                    problemCount = 0,
                    completedAt = 0L,
                ),
            )
        }
    }

    fun shouldConfirmPermissionSettingsBeforeScan(state: MusicAppUiState): Boolean {
        val scanState: LocalMusicScanState = state.scanState
        return scanState is LocalMusicScanState.Error &&
            scanState.error.type == LocalMusicScanErrorType.PermissionPermanentlyDenied
    }

    fun buildInitialScanState(stats: LibraryStats): LocalMusicScanState {
        return buildInitialScanStateFromStats(stats = stats)
    }

    fun syncLibrarySnapshot(state: MusicAppUiState, snapshot: LibrarySnapshot): MusicAppUiState {
        val likedSongIds: Set<String> = favoritesRepository.getLikedSongIds()
        val previewWithLikes: List<Song> = musicLibraryRepository.getHomePreview(limit = 6).map { song ->
            song.copy(isLiked = likedSongIds.contains(song.id) || song.isLiked)
        }
        val shouldRefreshFullLibrary: Boolean = state.localSongs.isNotEmpty() ||
            state.navigationState.secondaryScreen is SecondaryScreen.LocalMusic
        val fullSongsWithLikes: List<Song> = if (shouldRefreshFullLibrary) {
            musicLibraryRepository.getAllAvailableSongs().map { song ->
                song.copy(isLiked = likedSongIds.contains(song.id) || song.isLiked)
            }
        } else {
            state.localSongs
        }
        return state.copy(
            homeLocalSongPreview = previewWithLikes,
            localSongs = fullSongsWithLikes,
            localAlbums = if (shouldRefreshFullLibrary) {
                MusicLibraryProjector.buildAlbums(songs = fullSongsWithLikes)
            } else {
                state.localAlbums
            },
            localArtists = if (shouldRefreshFullLibrary) {
                MusicLibraryProjector.buildArtists(songs = fullSongsWithLikes)
            } else {
                state.localArtists
            },
            libraryStats = musicLibraryRepository.getLibraryStats(),
            localMusicSources = snapshot.sources,
            localMusicProblems = snapshot.problems,
            scanState = snapshot.scanState,
            likedSongIds = likedSongIds + previewWithLikes.filter { song -> song.isLiked }.map { song -> song.id },
            recentSongs = buildRecentSongs(
                state = state,
                extraSongs = fullSongsWithLikes + previewWithLikes,
            ),
            favoriteSongs = buildFavoriteSongs(
                likedSongIds = likedSongIds,
                preferredSongs = previewWithLikes + fullSongsWithLikes + state.queueSongsSnapshot + state.favoriteSongs,
            ),
        )
    }

    fun loadLocalMusicLibrary(state: MusicAppUiState): MusicAppUiState {
        if (state.localSongs.isNotEmpty()) {
            return state
        }
        val likedSongIds = favoritesRepository.getLikedSongIds()
        val songsWithLikes = musicLibraryRepository.getAllAvailableSongs().map { song ->
            song.copy(isLiked = likedSongIds.contains(song.id) || song.isLiked)
        }
        return state.copy(
            localSongs = songsWithLikes,
            localAlbums = MusicLibraryProjector.buildAlbums(songs = songsWithLikes),
            localArtists = MusicLibraryProjector.buildArtists(songs = songsWithLikes),
            favoriteSongs = buildFavoriteSongs(
                likedSongIds = likedSongIds,
                preferredSongs = state.homeLocalSongPreview + songsWithLikes + state.queueSongsSnapshot + state.favoriteSongs,
            ),
            likedSongIds = likedSongIds + songsWithLikes.filter { song -> song.isLiked }.map { song -> song.id },
            recentSongs = buildRecentSongs(state = state, extraSongs = songsWithLikes),
        )
    }

    fun buildRecentSongs(state: MusicAppUiState, extraSongs: List<Song> = emptyList()): List<Song> {
        val songs: List<Song> = extraSongs +
            state.queueSongsSnapshot +
            state.localSongs +
            state.homeLocalSongPreview +
            state.favoriteSongs
        val songsById: Map<String, Song> = songs.distinctBy { song -> song.id }.associateBy { song -> song.id }
        return playbackRepository.getPlaybackHistory().songIds
            .mapNotNull { songId: String -> songsById[songId] }
    }

    fun buildFavoriteSongs(likedSongIds: Set<String>, preferredSongs: List<Song>): List<Song> {
        return resolveAvailableSongsByIds(
            songIds = likedSongIds.toList(),
            preferredSongs = preferredSongs,
        ).map { song ->
            song.copy(isLiked = true)
        }
    }

    fun resolveAvailableSongsByIds(songIds: List<String>, preferredSongs: List<Song>): List<Song> {
        if (songIds.isEmpty()) {
            return emptyList()
        }
        val preferredById: Map<String, Song> = preferredSongs.associateBy { song -> song.id }
        val fetchedSongs: List<Song> = musicLibraryRepository.getAvailableSongsByIds(songIds = songIds)
        val fetchedSongIds: Set<String> = fetchedSongs.map { song -> song.id }.toSet()
        return (fetchedSongs.map { song -> preferredById[song.id] ?: song } +
            preferredSongs.filter { song -> songIds.contains(song.id) && !fetchedSongIds.contains(song.id) })
            .distinctBy { song -> song.id }
    }
}
```

- [x] **Step 4: Wire library synchronizer into `MusicAppController`**

Add a property after `favoritesRepository` is initialized. Because `favoritesRepository` is assigned in `init`, use `lateinit`:

```kotlin
private lateinit var libraryStateSynchronizer: LibraryStateSynchronizer
```

In `init`, after `favoritesRepository` and `toggleFavoriteUseCase` are assigned:

```kotlin
libraryStateSynchronizer = LibraryStateSynchronizer(
    musicLibraryRepository = musicLibraryRepository,
    favoritesRepository = favoritesRepository,
    playbackRepository = playbackRepository,
)
```

Replace helper calls:

```kotlin
private fun shouldConfirmPermissionSettingsBeforeScan(): Boolean {
    return libraryStateSynchronizer.shouldConfirmPermissionSettingsBeforeScan(state = uiState)
}

private fun buildInitialScanState(stats: LibraryStats): LocalMusicScanState {
    return LibraryStateSynchronizer.buildInitialScanStateFromStats(stats = stats)
}

private fun syncLibrarySnapshot(snapshot: LibrarySnapshot) {
    uiState = libraryStateSynchronizer.syncLibrarySnapshot(
        state = uiState,
        snapshot = snapshot,
    )
    restorePlaybackSnapshotIfPending()
}

fun loadLocalMusicLibrary() {
    val previousLocalSongsLoaded: Boolean = uiState.localSongs.isNotEmpty()
    uiState = libraryStateSynchronizer.loadLocalMusicLibrary(state = uiState)
    if (!previousLocalSongsLoaded && uiState.localSongs.isNotEmpty()) {
        restorePlaybackSnapshotIfPending()
    }
}
```

Keep `resolveAvailableSongsByIds`, `preferredKnownSongs`, and `buildFavoriteSongs` in `MusicAppController` until Task 6 and Task 5 move their remaining call sites.

- [x] **Step 5: Run library and affected facade tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.library.MusicAppLibraryStateSynchronizerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.coldStartWithPersistedSongsShowsDoneStateWithoutFullLibraryLoad" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.scanPermissionPermanentlyDeniedRequiresUserConfirmationBeforeSettings" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.localMusicPageLoadsFullSongsOnDemand"
```

Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library/LibraryStateSynchronizer.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicAppLibraryStateSynchronizerTest.kt
git commit -m "重构曲库状态同步"
```

---

### Task 5: Extract FavoriteStateSynchronizer

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/favorites/FavoriteStateSynchronizer.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/favorites/MusicAppFavoriteStateSynchronizerTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`

- [x] **Step 1: Write focused favorite synchronization tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/favorites/MusicAppFavoriteStateSynchronizerTest.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.favorites

import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCaseImpl
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.library.LibraryStateSynchronizer
import com.yanhao.kmpmusic.feature.app.library.MusicLibraryProjector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MusicAppFavoriteStateSynchronizerTest {
    @Test
    fun toggleFavoriteSyncsHomeLocalQueueFavoriteRecentAndCurrentSongSources(): Unit {
        val playbackRepository = InMemoryPlaybackRepository()
        playbackRepository.savePlaybackHistory(history = PlaybackHistory(songIds = listOf("song-1")))
        val favoritesRepository = InMemoryFavoritesRepository(initialLikedSongIds = emptySet())
        val synchronizer = FavoriteStateSynchronizer(
            toggleFavoriteUseCase = ToggleFavoriteUseCaseImpl(favoritesRepository = favoritesRepository),
            favoriteSongsResolver = { likedSongIds, preferredSongs ->
                preferredSongs.filter { song -> likedSongIds.contains(song.id) }
                    .distinctBy { song -> song.id }
                    .map { song -> song.copy(isLiked = true) }
            },
            recentSongsBuilder = { state, songs ->
                val songsById = songs.distinctBy { song -> song.id }.associateBy { song -> song.id }
                playbackRepository.getPlaybackHistory().songIds.mapNotNull { songId -> songsById[songId] }
            },
        )
        val state = testState().copy(
            homeLocalSongPreview = listOf(testSong(id = "song-1")),
            localSongs = listOf(testSong(id = "song-1")),
            queueSongsSnapshot = listOf(testSong(id = "song-1")),
            currentSongId = "song-1",
            queueSongIds = listOf("song-1"),
        )

        val nextState = synchronizer.toggleFavorite(state = state, songId = "song-1")

        assertEquals(expected = setOf("song-1"), actual = nextState.likedSongIds)
        assertTrue(actual = nextState.homeLocalSongPreview.single().isLiked)
        assertTrue(actual = nextState.localSongs.single().isLiked)
        assertTrue(actual = nextState.queueSongsSnapshot.single().isLiked)
        assertTrue(actual = nextState.favoriteSongs.single().isLiked)
        assertTrue(actual = nextState.recentSongs.single().isLiked)
        assertTrue(actual = nextState.currentSong?.isLiked == true)
    }

    @Test
    fun favoriteAlbumsAndArtistsStillComeFromProjectorAfterToggle(): Unit {
        val favoritesRepository = InMemoryFavoritesRepository(initialLikedSongIds = emptySet())
        val synchronizer = FavoriteStateSynchronizer(
            toggleFavoriteUseCase = ToggleFavoriteUseCaseImpl(favoritesRepository = favoritesRepository),
            favoriteSongsResolver = { likedSongIds, preferredSongs ->
                preferredSongs.filter { song -> likedSongIds.contains(song.id) }
                    .distinctBy { song -> song.id }
                    .map { song -> song.copy(isLiked = true) }
            },
            recentSongsBuilder = { _, _ -> emptyList() },
        )
        val state = testState().copy(homeLocalSongPreview = listOf(testSong(id = "song-1", album = "Album", artist = "Artist")))

        val nextState = synchronizer.toggleFavorite(state = state, songId = "song-1")

        assertEquals(expected = listOf("album:album"), actual = MusicLibraryProjector.buildAlbums(nextState.favoriteSongs).map { album -> album.id })
        assertEquals(expected = listOf("artist:artist"), actual = MusicLibraryProjector.buildArtists(nextState.favoriteSongs).map { artist -> artist.id })
    }

    private fun testState(): MusicAppUiState {
        return MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        )
    }

    private fun testSong(
        id: String,
        album: String = "Album",
        artist: String = "Artist",
    ): Song {
        return Song(
            id = id,
            title = id,
            artist = artist,
            album = album,
            duration = "03:00",
            durationMs = 180_000L,
            isLiked = false,
        )
    }
}
```

- [x] **Step 2: Run favorite tests and verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.favorites.MusicAppFavoriteStateSynchronizerTest"
```

Expected: FAIL with an unresolved reference to `FavoriteStateSynchronizer`.

- [x] **Step 3: Add favorite synchronizer**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/favorites/FavoriteStateSynchronizer.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.favorites

import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCase
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * Applies favorite changes across every visible song collection in UI state.
 */
class FavoriteStateSynchronizer(
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val favoriteSongsResolver: (likedSongIds: Set<String>, preferredSongs: List<Song>) -> List<Song>,
    private val recentSongsBuilder: (state: MusicAppUiState, songs: List<Song>) -> List<Song>,
) {
    fun toggleFavorite(state: MusicAppUiState, songId: String): MusicAppUiState {
        val likedSongIds: Set<String> = toggleFavoriteUseCase(songId = songId)
        fun Song.withFavorite(): Song = copy(isLiked = likedSongIds.contains(id))
        val homePreview = state.homeLocalSongPreview.map { song -> song.withFavorite() }
        val localSongs = state.localSongs.map { song -> song.withFavorite() }
        val queueSnapshot = state.queueSongsSnapshot.map { song -> song.withFavorite() }
        val favoriteSongs = favoriteSongsResolver(
            likedSongIds,
            homePreview + localSongs + queueSnapshot + state.favoriteSongs,
        )
        val stateWithUpdatedCollections = state.copy(
            likedSongIds = likedSongIds,
            homeLocalSongPreview = homePreview,
            localSongs = localSongs,
            favoriteSongs = favoriteSongs,
            queueSongsSnapshot = queueSnapshot,
        )
        return stateWithUpdatedCollections.copy(
            recentSongs = recentSongsBuilder(
                stateWithUpdatedCollections,
                localSongs.ifEmpty { homePreview },
            ),
        )
    }
}
```

- [x] **Step 4: Wire `MusicAppController.toggleFavorite`**

Add a property:

```kotlin
private lateinit var favoriteStateSynchronizer: FavoriteStateSynchronizer
```

In `init`, after `libraryStateSynchronizer` is created:

```kotlin
favoriteStateSynchronizer = FavoriteStateSynchronizer(
    toggleFavoriteUseCase = toggleFavoriteUseCase,
    favoriteSongsResolver = libraryStateSynchronizer::buildFavoriteSongs,
    recentSongsBuilder = { state, songs ->
        libraryStateSynchronizer.buildRecentSongs(
            state = state,
            extraSongs = songs,
        )
    },
)
```

Replace `toggleFavorite`:

```kotlin
fun toggleFavorite(songId: String) {
    uiState = favoriteStateSynchronizer.toggleFavorite(
        state = uiState,
        songId = songId,
    )
    publishPlaybackUiState()
}
```

- [x] **Step 5: Run favorite tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.favorites.MusicAppFavoriteStateSynchronizerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.toggleFavoriteSyncsSongList" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.toggleCurrentSongFavoriteUsesSharedControllerEntry"
```

Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/favorites/FavoriteStateSynchronizer.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/favorites/MusicAppFavoriteStateSynchronizerTest.kt
git commit -m "重构收藏状态同步"
```

---

### Task 6: Extract Playback UI Sync And Restore Orchestration

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackUiStateSynchronizer.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackRestoreOrchestrator.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackUiStateSynchronizerTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackRestoreOrchestratorTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`

- [x] **Step 1: Write playback UI synchronizer tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackUiStateSynchronizerTest.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class MusicAppPlaybackUiStateSynchronizerTest {
    @Test
    fun syncPlaybackStateProjectsPlaybackAndQueueIntoUiState(): Unit {
        val playbackRepository = InMemoryPlaybackRepository()
        playbackRepository.saveQueueState(
            queueState = QueueState(
                songIds = listOf("song-1", "song-2"),
                currentIndex = 1,
                playbackMode = PlaybackMode.Shuffle,
            ),
        )
        playbackRepository.savePlaybackHistory(history = PlaybackHistory(songIds = listOf("song-2")))
        val synchronizer = PlaybackUiStateSynchronizer(
            playbackRepository = playbackRepository,
            recentSongsBuilder = { _, _ -> listOf(testSong(id = "song-2")) },
        )

        val nextState = synchronizer.syncPlaybackState(
            state = testState(),
            playbackState = PlaybackState(
                currentSongId = "song-2",
                status = PlaybackStatus.Playing,
                positionMs = 12_000L,
                durationMs = 180_000L,
            ),
        )

        assertEquals(expected = "song-2", actual = nextState.currentSongId)
        assertEquals(expected = PlaybackStatus.Playing, actual = nextState.playbackStatus)
        assertEquals(expected = PlaybackMode.Shuffle, actual = nextState.playbackMode)
        assertEquals(expected = listOf("song-1", "song-2"), actual = nextState.queueSongIds)
        assertEquals(expected = listOf("song-2"), actual = nextState.recentSongs.map { song -> song.id })
    }

    private fun testState(): MusicAppUiState {
        return MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        )
    }

    private fun testSong(id: String): Song {
        return Song(
            id = id,
            title = id,
            artist = "Artist",
            album = "Album",
            duration = "03:00",
            durationMs = 180_000L,
        )
    }
}
```

- [x] **Step 2: Write playback restore tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackRestoreOrchestratorTest.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicAppPlaybackRestoreOrchestratorTest {
    @Test
    fun restoreMarksPendingWhenSavedSongsCannotBeResolved(): Unit = runTest {
        val orchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = snapshotStoreWithQueue(songIds = listOf("missing")),
            availableSongsResolver = { _, _ -> emptyList() },
            restoreSnapshot = { error("restoreSnapshot should not run when songs are unavailable") },
        )

        val result = orchestrator.restore(state = testState(), preferredSongs = emptyList())

        assertTrue(actual = result.isPending)
        assertEquals(expected = emptyList(), actual = result.state.queueSongsSnapshot)
    }

    @Test
    fun restoreResolvesAvailableSongsAndCallsPlaybackCoordinator(): Unit = runTest {
        val restoredCalls: MutableList<List<String>> = mutableListOf()
        val song = testSong(id = "song-1")
        val orchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = snapshotStoreWithQueue(songIds = listOf("song-1")),
            availableSongsResolver = { songIds, preferredSongs ->
                preferredSongs.filter { candidate -> songIds.contains(candidate.id) }
            },
            restoreSnapshot = { songs -> restoredCalls += songs.map { restoredSong -> restoredSong.id } },
        )

        val result = orchestrator.restore(state = testState(), preferredSongs = listOf(song))

        assertFalse(actual = result.isPending)
        assertEquals(expected = listOf("song-1"), actual = result.state.queueSongsSnapshot.map { restoredSong -> restoredSong.id })
        assertEquals(expected = listOf(listOf("song-1")), actual = restoredCalls)
    }

    private suspend fun snapshotStoreWithQueue(songIds: List<String>): PlaybackSnapshotStore {
        val store = InMemoryPlaybackSnapshotStore()
        store.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = songIds.firstOrNull(),
                    status = PlaybackStatus.Playing,
                ),
                queueState = QueueState(
                    songIds = songIds,
                    currentIndex = 0,
                ),
                updatedAt = 0L,
            ),
        )
        return store
    }

    private fun testState(): MusicAppUiState {
        return MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        )
    }

    private fun testSong(id: String): Song {
        return Song(
            id = id,
            title = id,
            artist = "Artist",
            album = "Album",
            duration = "03:00",
            durationMs = 180_000L,
        )
    }
}
```

- [x] **Step 3: Run playback tests and verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.playback.MusicAppPlaybackUiStateSynchronizerTest" --tests "com.yanhao.kmpmusic.feature.app.playback.MusicAppPlaybackRestoreOrchestratorTest"
```

Expected: FAIL with unresolved references to `PlaybackUiStateSynchronizer` and `PlaybackRestoreOrchestrator`.

- [x] **Step 4: Add playback UI synchronizer**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackUiStateSynchronizer.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * Projects playback repository state into the global App UI state.
 */
class PlaybackUiStateSynchronizer(
    private val playbackRepository: PlaybackRepository,
    private val recentSongsBuilder: (state: MusicAppUiState, extraSongs: List<Song>) -> List<Song>,
) {
    fun syncPlaybackState(state: MusicAppUiState, playbackState: PlaybackState): MusicAppUiState {
        val queueState: QueueState = playbackRepository.getQueueState()
        return state.copy(
            currentSongId = playbackState.currentSongId,
            playbackStatus = playbackState.status,
            playbackPositionMs = playbackState.positionMs,
            playbackDurationMs = playbackState.durationMs,
            playbackMode = queueState.playbackMode,
            playbackError = playbackState.error,
            queueSongIds = queueState.songIds,
            recentSongs = recentSongsBuilder(state, emptyList()),
        )
    }
}
```

- [x] **Step 5: Add playback restore orchestrator**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackRestoreOrchestrator.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * Coordinates snapshot restore prerequisites before delegating playback state restoration.
 */
class PlaybackRestoreOrchestrator(
    private val playbackSnapshotStore: PlaybackSnapshotStore,
    private val availableSongsResolver: (songIds: List<String>, preferredSongs: List<Song>) -> List<Song>,
    private val restoreSnapshot: suspend (availableSongs: List<Song>) -> Unit,
) {
    data class Result(
        val state: MusicAppUiState,
        val isPending: Boolean,
    )

    suspend fun restore(state: MusicAppUiState, preferredSongs: List<Song>): Result {
        val savedQueueSongIds: List<String> = playbackSnapshotStore.getSavedQueueSongIds()
        if (savedQueueSongIds.isEmpty()) {
            return Result(state = state, isPending = false)
        }
        val availableSongs: List<Song> = availableSongsResolver(savedQueueSongIds, preferredSongs)
        if (availableSongs.isEmpty()) {
            return Result(state = state, isPending = true)
        }
        restoreSnapshot(availableSongs)
        return Result(
            state = state.copy(queueSongsSnapshot = availableSongs),
            isPending = false,
        )
    }
}
```

- [x] **Step 6: Wire playback collaborators into `MusicAppController`**

Add properties:

```kotlin
private lateinit var playbackUiStateSynchronizer: PlaybackUiStateSynchronizer
private lateinit var playbackRestoreOrchestrator: PlaybackRestoreOrchestrator
```

In `init`, after `libraryStateSynchronizer` exists:

```kotlin
playbackUiStateSynchronizer = PlaybackUiStateSynchronizer(
    playbackRepository = playbackRepository,
    recentSongsBuilder = libraryStateSynchronizer::buildRecentSongs,
)
playbackRestoreOrchestrator = PlaybackRestoreOrchestrator(
    playbackSnapshotStore = playbackSnapshotStore,
    availableSongsResolver = libraryStateSynchronizer::resolveAvailableSongsByIds,
    restoreSnapshot = playbackCoordinator::restoreSnapshot,
)
```

Replace `syncPlaybackState`:

```kotlin
private fun syncPlaybackState(playbackState: PlaybackState) {
    uiState = playbackUiStateSynchronizer.syncPlaybackState(
        state = uiState,
        playbackState = playbackState,
    )
    publishPlaybackUiState()
}
```

Replace `restorePlaybackSnapshot`:

```kotlin
suspend fun restorePlaybackSnapshot() {
    val result = playbackRestoreOrchestrator.restore(
        state = uiState,
        preferredSongs = preferredKnownSongs(),
    )
    uiState = result.state
    isPlaybackRestorePending = result.isPending
}
```

Replace `preferredKnownSongs` with:

```kotlin
private fun preferredKnownSongs(): List<Song> {
    return MusicLibraryProjector.buildDetailSongs(
        queueSongsSnapshot = uiState.queueSongsSnapshot,
        localSongs = uiState.localSongs,
        homeLocalSongPreview = uiState.homeLocalSongPreview,
        favoriteSongs = uiState.favoriteSongs,
    )
}
```

Delete `resolveAvailableSongsByIds` and `buildFavoriteSongs` from `MusicAppController` after all call sites use collaborators.

- [x] **Step 7: Run playback and restore tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.playback.MusicAppPlaybackUiStateSynchronizerTest" --tests "com.yanhao.kmpmusic.feature.app.playback.MusicAppPlaybackRestoreOrchestratorTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.restorePlaybackSnapshotAllowsResume" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.restorePlaybackSnapshotDoesNotAutoScanWhenLibraryIsEmpty"
```

Expected: PASS.

- [x] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackUiStateSynchronizer.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackRestoreOrchestrator.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackUiStateSynchronizerTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackRestoreOrchestratorTest.kt
git commit -m "重构播放 UI 同步与快照恢复"
```

---

### Task 7: Extract LoginAndDialogStateController

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/session/LoginAndDialogStateController.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/session/LoginAndDialogStateControllerTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`

- [x] **Step 1: Write session controller tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/session/LoginAndDialogStateControllerTest.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.session

import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginAndDialogStateControllerTest {
    @Test
    fun clearCacheDialogOpensAndClosesWithoutChangingUserData(): Unit {
        val controller = LoginAndDialogStateController
        val state = testState().copy(likedSongIds = setOf("liked"))

        val openState = controller.openClearCacheDialog(state = state)
        val closedState = controller.confirmClearCache(state = openState)

        assertTrue(actual = openState.isClearCacheDialogOpen)
        assertFalse(actual = closedState.isClearCacheDialogOpen)
        assertEquals(expected = setOf("liked"), actual = closedState.likedSongIds)
    }

    @Test
    fun sendLoginMailRequiresAtSymbol(): Unit {
        val controller = LoginAndDialogStateController
        val invalidState = controller.sendLoginMail(state = testState().copy(email = "not-mail"))
        val validState = controller.sendLoginMail(state = testState().copy(email = "user@example.com"))

        assertFalse(actual = invalidState.isMailSent)
        assertTrue(actual = validState.isMailSent)
    }

    @Test
    fun moreMenuCanOpenAndCloseBySongId(): Unit {
        val controller = LoginAndDialogStateController

        val openState = controller.openMore(state = testState(), songId = "song-1")
        val closedState = controller.closeMore(state = openState)

        assertEquals(expected = "song-1", actual = openState.moreSongId)
        assertNull(actual = closedState.moreSongId)
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
```

- [x] **Step 2: Run session tests and verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.session.LoginAndDialogStateControllerTest"
```

Expected: FAIL with an unresolved reference to `LoginAndDialogStateController`.

- [x] **Step 3: Add session controller**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/session/LoginAndDialogStateController.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.app.session

import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * Reducers for lightweight session UI state that does not own library or playback behavior.
 */
object LoginAndDialogStateController {
    fun openQueue(state: MusicAppUiState): MusicAppUiState {
        return state.copy(isQueueOpen = true)
    }

    fun closeQueue(state: MusicAppUiState): MusicAppUiState {
        return state.copy(isQueueOpen = false)
    }

    fun openMore(state: MusicAppUiState, songId: String): MusicAppUiState {
        return state.copy(moreSongId = songId)
    }

    fun closeMore(state: MusicAppUiState): MusicAppUiState {
        return state.copy(moreSongId = null)
    }

    fun openClearCacheDialog(state: MusicAppUiState): MusicAppUiState {
        return state.copy(isClearCacheDialogOpen = true)
    }

    fun closeClearCacheDialog(state: MusicAppUiState): MusicAppUiState {
        return state.copy(isClearCacheDialogOpen = false)
    }

    fun confirmClearCache(state: MusicAppUiState): MusicAppUiState {
        return state.copy(isClearCacheDialogOpen = false)
    }

    fun setEmail(state: MusicAppUiState, email: String): MusicAppUiState {
        return state.copy(email = email)
    }

    fun sendLoginMail(state: MusicAppUiState): MusicAppUiState {
        if (!state.email.contains("@")) {
            return state
        }
        return state.copy(isMailSent = true)
    }
}
```

- [x] **Step 4: Wire lightweight session methods**

Modify `MusicAppController`:

```kotlin
import com.yanhao.kmpmusic.feature.app.session.LoginAndDialogStateController
```

Replace method bodies:

```kotlin
fun openQueue() {
    uiState = LoginAndDialogStateController.openQueue(state = uiState)
}

fun closeQueue() {
    uiState = LoginAndDialogStateController.closeQueue(state = uiState)
}

fun openMore(song: Song) {
    uiState = LoginAndDialogStateController.openMore(state = uiState, songId = song.id)
}

fun closeMore() {
    uiState = LoginAndDialogStateController.closeMore(state = uiState)
}

fun openClearCacheDialog() {
    uiState = LoginAndDialogStateController.openClearCacheDialog(state = uiState)
}

fun closeClearCacheDialog() {
    uiState = LoginAndDialogStateController.closeClearCacheDialog(state = uiState)
}

fun confirmClearCache() {
    uiState = LoginAndDialogStateController.confirmClearCache(state = uiState)
}

fun setEmail(email: String) {
    uiState = LoginAndDialogStateController.setEmail(state = uiState, email = email)
}

fun sendLoginMail() {
    uiState = LoginAndDialogStateController.sendLoginMail(state = uiState)
}
```

Do not move permission settings methods into this controller.

- [x] **Step 5: Run session and facade tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.session.LoginAndDialogStateControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.systemBackClosesPermissionSettingsDialog" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.systemBackClosesOverlayBeforeSecondaryScreen"
```

Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/session/LoginAndDialogStateController.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/session/LoginAndDialogStateControllerTest.kt
git commit -m "重构轻量会话状态控制"
```

---

### Task 8: Slim MusicAppController Tests Into Focused Files

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`
- Modify focused test files created in Tasks 1-7 only when moving exact tests into better homes.

- [ ] **Step 1: Keep facade acceptance tests in `MusicAppControllerTest`**

Keep these tests in `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`:

```kotlin
queueSongsSurviveAfterPlaybackContextIsNoLongerInSongs
scanPermissionPermanentlyDeniedRequiresUserConfirmationBeforeSettings
playSongAddsRecentPlayback
playSongOutsideHomePreviewAddsRecentPlayback
recentPlaybackKeepsAllPlayedSongs
clearRecentPlaybackHistoryRemovesVisibleAndStoredHistory
playerScreenAndBottomPlayerReadSamePlaybackState
playerScreenAndBottomPlayerReadSamePlaybackVolume
setVolumeCoercesSharedUiStateAndEngineVolume
playSongUpdatesPlaybackAndQueue
playSongUsesProvidedQueueSongs
playSongWithoutProvidedQueueKeepsCurrentQueueWhenSongExists
cyclePlaybackModeUpdatesUiState
playSongRecordsPlaybackHistory
moveTrackDoesNotUseSongsAsImplicitQueue
moveTrackChangesCurrentSong
removeCurrentSongKeepsEngineQueueInSync
restorePlaybackSnapshotAllowsResume
restorePlaybackSnapshotRestoresAfterLibraryLoads
restorePlaybackSnapshotDoesNotAutoScanWhenLibraryIsEmpty
restorePlaybackSnapshotRestoresSavedSongOutsidePreviewWithoutFullLibraryLoad
favoriteSongsRemainAvailableBeforeFullLibraryLoads
toggleFavoriteSyncsSongList
toggleCurrentSongFavoriteUsesSharedControllerEntry
localLibrarySearchReturnsNonFavoriteLocalSongs
favoritesSearchOnlyReturnsFavoriteSongs
searchScopeLimitsResultTypes
searchReadsScannedSnapshot
homeSearchOpensLocalLibrarySearchContext
favoritesSearchOpensFavoritesSearchContext
titlebarSearchOnlyShowsOnHomeAndFavoritesRootPages
nonBlankSearchQueryCommitsToHistoryWhenLeavingSearch
searchQueryWithoutResultsCommitsToHistoryWhenLeavingSearch
searchLoadsFullLibraryInsteadOfHomePreviewOnly
repeatedSearchQueryChangesReuseLoadedLocalSongs
searchScreenHidesTitlebarSearch
rootNavigationClearsSecondaryScreen
desktopRailRootNavigationClearsSecondaryScreen
openLocalMusicUsesSecondaryChrome
openLocalMusicCanStartAtSourcesSection
openPlayerUsesFullscreenSecondaryScreen
rootScrollStateKeyStaysStableAfterSecondaryReturn
secondaryScrollStateKeyChangesForEachEntry
systemBackReturnsFromSecondaryScreen
systemBackClosesPermissionSettingsDialog
systemBackClosesOverlayBeforeSecondaryScreen
```

- [ ] **Step 2: Delete projector-only facade tests after Task 1 coverage passes**

Delete these tests from `MusicAppControllerTest` because `MusicLibraryProjectorTest` and library synchronizer tests now cover their pure projection rules:

```kotlin
coldStartUsesHomePreviewWithoutFullLocalSongs
localMusicPageLoadsFullSongsOnDemand
knownPreviewSongsCanOpenDetailsByLoadingFullLibraryOnDemand
libraryStatsComeFromScannedSnapshot
coldStartWithPersistedSongsShowsDoneStateWithoutFullLibraryLoad
```

Keep `favoriteSongsRemainAvailableBeforeFullLibraryLoads` in the facade file because it verifies lazy library loading, favorites, and detail availability together.

- [ ] **Step 3: Delete search-history unit tests after Task 3 coverage passes**

Delete these tests from `MusicAppControllerTest` because `MusicAppSearchControllerTest` now covers context isolation, clearing behavior, history restore, deduplication, and per-context clearing:

```kotlin
searchHistoryIsIsolatedByContext
clearingSearchQueryCommitsPreviousQueryToHistory
searchHistoryRestoresFromRepositoryAcrossControllerInstances
searchHistoryDeduplicatesAndMovesLatestFirst
clearSearchHistoryOnlyClearsCurrentContext
```

Keep `nonBlankSearchQueryCommitsToHistoryWhenLeavingSearch` and `searchQueryWithoutResultsCommitsToHistoryWhenLeavingSearch` in the facade file because they verify navigation lifecycle commits through `MusicAppController`.

- [ ] **Step 4: Delete reducer-only navigation/session tests after Tasks 2 and 7 coverage pass**

Delete these tests from `MusicAppControllerTest` because `MusicAppNavigationControllerTest` or `LoginAndDialogStateControllerTest` now cover the reducer-only behavior:

```kotlin
secondaryScreenKeepsPreviousRootTab
navigationStateProvidesChromeMode
initialStateHasNoPlaybackBeforeUserAction
idleStatusWithCurrentQueueKeepsPlaybackSessionActive
emptyIdleStatusHasNoActivePlaybackSession
```

Keep all system-back tests in the facade file because they coordinate dialogs, more menu, queue, and secondary pages.

- [ ] **Step 5: Run targeted split-test set**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest" --tests "com.yanhao.kmpmusic.feature.app.library.MusicLibraryProjectorTest" --tests "com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest" --tests "com.yanhao.kmpmusic.feature.app.search.MusicAppSearchControllerTest" --tests "com.yanhao.kmpmusic.feature.app.library.MusicAppLibraryStateSynchronizerTest" --tests "com.yanhao.kmpmusic.feature.app.favorites.MusicAppFavoriteStateSynchronizerTest" --tests "com.yanhao.kmpmusic.feature.app.playback.MusicAppPlaybackUiStateSynchronizerTest" --tests "com.yanhao.kmpmusic.feature.app.playback.MusicAppPlaybackRestoreOrchestratorTest" --tests "com.yanhao.kmpmusic.feature.app.session.LoginAndDialogStateControllerTest"
```

Expected: PASS.

- [ ] **Step 6: Confirm file sizes moved in the right direction**

Run:

```bash
wc -l composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
```

Expected: both files have fewer lines than the starting point of 918 and 1527, and `MusicAppController.kt` no longer contains private `buildAlbums`, `buildArtists`, search-history helpers, or favorite state projection loops.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicLibraryProjectorTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/MusicAppNavigationControllerTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/search/MusicAppSearchControllerTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/MusicAppLibraryStateSynchronizerTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/favorites/MusicAppFavoriteStateSynchronizerTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackUiStateSynchronizerTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackRestoreOrchestratorTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/session/LoginAndDialogStateControllerTest.kt
git commit -m "拆分应用控制器测试职责"
```

---

### Task 9: Full Verification And Architecture Guardrails

**Files:**
- Modify: `docs/superpowers/specs/2026-06-30-codebase-architecture-optimization-design.md` only if implementation reveals a documented boundary that must be sharpened.

- [ ] **Step 1: Run shared logic tests**

Run:

```bash
./gradlew :composeApp:desktopTest
```

Expected: PASS.

- [ ] **Step 2: Run Android common compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [ ] **Step 3: Verify no collaborator owns Compose mutable state**

Run:

```bash
rg -n "mutableStateOf|var uiState|MutableState" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/navigation composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/search composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/favorites composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/session
```

Expected: no matches.

- [ ] **Step 4: Verify UI still calls only the facade**

Run:

```bash
rg -n "NavigationStateController|SearchSessionController|LibraryStateSynchronizer|FavoriteStateSynchronizer|PlaybackUiStateSynchronizer|PlaybackRestoreOrchestrator|LoginAndDialogStateController" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt
```

Expected: no matches.

- [ ] **Step 5: Verify controller still exposes expected public methods**

Run:

```bash
rg -n "fun (navigateToSecondary|navigateToRoot|navigateBack|handleSystemBack|scanLocalMusic|restorePlaybackSnapshot|openLocalMusic|openSearch|playSong|openSong|togglePlayback|toggleFavorite|search|setThemeMode)" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt
```

Expected: matches for all listed public methods.

- [ ] **Step 6: Inspect git status**

Run:

```bash
git status --short --branch
```

Expected: only intentional source, test, and documentation changes are present.

- [ ] **Step 7: Commit verification fixes if any source changes were needed**

If Step 1 or Step 2 required source/test fixes, commit them:

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic docs/superpowers/specs/2026-06-30-codebase-architecture-optimization-design.md
git commit -m "验证应用控制器架构拆分"
```

If no source/test fixes were needed, do not create an empty commit.

## Self-Review

**Spec coverage:**  
The plan covers first-stage App state trunk only. Task 1 covers projection duplication. Task 2 covers navigation reducers. Task 3 covers search query, active query, and history isolation. Task 4 covers library sync, full-library loading, recent visible list, favorite entity completion, and permission gate state. Task 5 covers favorites across visible collections. Task 6 covers playback UI projection and snapshot restore orchestration. Task 7 covers lightweight session UI state while keeping permission settings outside that session controller. Task 8 covers test splitting. Task 9 covers desktop tests, Android compile, facade-only UI calls, and single Compose state ownership.

**Placeholder scan:**  
This plan contains no placeholder markers, no empty implementation step, and no task that asks the implementer to write unspecified tests. Each code-changing step includes concrete Kotlin or shell content.

**Type consistency:**  
The plan consistently uses `MusicAppUiState`, `NavigationState`, `SecondaryScreen`, `SearchContext`, `SearchScope`, `PlaybackState`, `QueueState`, `Song`, `MusicLibraryProjector`, `NavigationStateController`, `SearchSessionController`, `LibraryStateSynchronizer`, `FavoriteStateSynchronizer`, `PlaybackUiStateSynchronizer`, `PlaybackRestoreOrchestrator`, and `LoginAndDialogStateController` with stable names across tasks.
