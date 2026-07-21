package com.yanhao.kmpmusic.feature.desktop.navigation

import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.NavigationState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.app.SecondaryStackEntry
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 桌面左栏高亮解析测试，确保新版六入口导航不会被旧根 Tab 来源抢走。
 */
class DesktopNavigationRailDestinationTest {
    @Test
    fun topLevelRootTabsMapToPrimaryDestinations() {
        assertEquals(
            expected = DesktopRailDestination.Music,
            actual = resolveDesktopRailDestination(navigationState = NavigationState(rootTab = RootTab.Home)),
        )
        assertEquals(
            expected = DesktopRailDestination.Favorites,
            actual = resolveDesktopRailDestination(navigationState = NavigationState(rootTab = RootTab.Favorites)),
        )
        assertEquals(
            expected = DesktopRailDestination.Me,
            actual = resolveDesktopRailDestination(navigationState = NavigationState(rootTab = RootTab.Me)),
        )
    }

    @Test
    fun localMusicSectionsOverrideCurrentRootTab() {
        val navigationState: NavigationState =
            NavigationState(
                rootTab = RootTab.Favorites,
                secondaryScreen = SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Albums),
            )

        assertEquals(
            expected = DesktopRailDestination.Albums,
            actual = resolveDesktopRailDestination(navigationState = navigationState),
        )
    }

    @Test
    fun playlistScreensOverrideCurrentRootTab() {
        val navigationState: NavigationState =
            NavigationState(
                rootTab = RootTab.Favorites,
                secondaryScreen = SecondaryScreen.LocalPlaylists,
            )

        assertEquals(
            expected = DesktopRailDestination.Playlists,
            actual = resolveDesktopRailDestination(navigationState = navigationState),
        )
    }

    @Test
    fun searchKeepsFavoriteContext() {
        val navigationState: NavigationState =
            NavigationState(
                rootTab = RootTab.Home,
                secondaryScreen = SecondaryScreen.Search(context = SearchContext.Favorites),
            )

        assertEquals(
            expected = DesktopRailDestination.Favorites,
            actual = resolveDesktopRailDestination(navigationState = navigationState),
        )
    }

    @Test
    fun searchKeepsLocalMusicSourceSection() {
        val navigationState: NavigationState =
            NavigationState(
                rootTab = RootTab.Home,
                secondaryScreen = SecondaryScreen.Search(context = SearchContext.LocalLibrary),
                secondaryBackStack =
                    listOf(
                        SecondaryStackEntry(
                            screen = SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Artists),
                            entryId = 1,
                        ),
                    ),
            )

        assertEquals(
            expected = DesktopRailDestination.Artists,
            actual = resolveDesktopRailDestination(navigationState = navigationState),
        )
    }

    @Test
    fun detailKeepsLocalMusicSourceBeforeRootFallback() {
        val navigationState: NavigationState =
            NavigationState(
                rootTab = RootTab.Home,
                secondaryScreen = SecondaryScreen.AlbumDetail,
                secondaryBackStack =
                    listOf(
                        SecondaryStackEntry(
                            screen = SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Albums),
                            entryId = 1,
                        ),
                    ),
            )

        assertEquals(
            expected = DesktopRailDestination.Albums,
            actual = resolveDesktopRailDestination(navigationState = navigationState),
        )
    }

    @Test
    fun detailOpenedFromFavoritesKeepsFavorites() {
        val navigationState: NavigationState =
            NavigationState(
                rootTab = RootTab.Favorites,
                secondaryScreen = SecondaryScreen.AlbumDetail,
            )

        assertEquals(
            expected = DesktopRailDestination.Favorites,
            actual = resolveDesktopRailDestination(navigationState = navigationState),
        )
    }
}
