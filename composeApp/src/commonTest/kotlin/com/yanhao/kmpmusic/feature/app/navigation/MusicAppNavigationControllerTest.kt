package com.yanhao.kmpmusic.feature.app.navigation

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MobileFixedBarMode
import com.yanhao.kmpmusic.feature.app.MobileFixedBarPlacement
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.NavigationState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.app.SecondaryStackEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [NavigationStateController] 的纯 reducer 测试，确保 facade 委托后行为保持稳定。
 */
class MusicAppNavigationControllerTest {
    /**
     * 进入二级页时应记住来源一级 Tab，并清理不能跨路由保留的临时浮层状态。
     */
    @Test
    fun navigateToSecondaryStoresPreviousRootAndClosesTransientOverlays() {
        val state =
            testState().copy(
                navigationState = NavigationState(rootTab = RootTab.Favorites, previousRootTab = RootTab.Favorites),
                isQueueOpen = true,
                moreSongId = "song-1",
            )

        val nextState =
            NavigationStateController.navigateToSecondary(
                state = state,
                screen = SecondaryScreen.Search(context = SearchContext.Favorites),
            )

        assertEquals(expected = RootTab.Favorites, actual = nextState.navigationState.previousRootTab)
        assertEquals(expected = SecondaryScreen.Search(context = SearchContext.Favorites), actual = nextState.navigationState.secondaryScreen)
        assertEquals(expected = 1, actual = nextState.navigationState.secondaryEntryId)
        assertEquals(expected = emptyList(), actual = nextState.navigationState.secondaryBackStack)
        assertFalse(actual = nextState.isQueueOpen)
        assertNull(actual = nextState.moreSongId)
    }

    /**
     * 从二级页继续打开二级页时应压栈，返回才能恢复上一层 chrome 与内容。
     */
    @Test
    fun navigateToSecondaryFromSecondaryPushesPreviousScreen() {
        val state =
            testState().copy(
                navigationState =
                    NavigationState(
                        rootTab = RootTab.Me,
                        previousRootTab = RootTab.Me,
                        secondaryScreen = SecondaryScreen.Settings,
                        secondaryEntryId = 4,
                    ),
            )

        val nextState =
            NavigationStateController.navigateToSecondary(
                state = state,
                screen = SecondaryScreen.About,
            )

        assertEquals(expected = SecondaryScreen.About, actual = nextState.navigationState.secondaryScreen)
        assertEquals(expected = 5, actual = nextState.navigationState.secondaryEntryId)
        assertEquals(
            expected = listOf(SecondaryStackEntry(screen = SecondaryScreen.Settings, entryId = 4)),
            actual = nextState.navigationState.secondaryBackStack,
        )
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = nextState.navigationState.chromeUnderlayFixedBarMode,
        )
    }

    /**
     * 切换一级页时应彻底退出二级路由，并把目标一级页作为新的返回基线。
     */
    @Test
    fun navigateToRootClearsSecondaryAndUsesTargetRootAsPreviousRoot() {
        val state =
            testState().copy(
                navigationState =
                    NavigationState(
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

    /**
     * 从二级页返回时应恢复上次一级页，同时保留 entry id 作为滚动 key 的稳定来源。
     */
    @Test
    fun navigateBackReturnsToPreviousRootWithoutChangingEntryId() {
        val state =
            testState().copy(
                navigationState =
                    NavigationState(
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

    /**
     * 栈内返回应先恢复上一层二级页，而不是直接退回一级页。
     */
    @Test
    fun navigateBackFromStackRestoresPreviousSecondary() {
        val state =
            testState().copy(
                navigationState =
                    NavigationState(
                        rootTab = RootTab.Me,
                        previousRootTab = RootTab.Me,
                        secondaryScreen = SecondaryScreen.About,
                        secondaryEntryId = 5,
                        secondaryBackStack =
                            listOf(
                                SecondaryStackEntry(screen = SecondaryScreen.Settings, entryId = 4),
                            ),
                    ),
            )

        val nextState = NavigationStateController.navigateBack(state = state)

        assertEquals(expected = SecondaryScreen.Settings, actual = nextState.navigationState.secondaryScreen)
        assertEquals(expected = 4, actual = nextState.navigationState.secondaryEntryId)
        assertEquals(expected = emptyList(), actual = nextState.navigationState.secondaryBackStack)
        assertEquals(expected = RootTab.Me, actual = nextState.navigationState.rootTab)
    }

    /**
     * 最近播放页应是稳定命名的普通二级页，方便移动端和桌面工作区共用路由语义。
     */
    @Test
    fun recentPlayedUsesNamedSecondaryRouteAndBackStack() {
        val state =
            testState().copy(
                navigationState =
                    NavigationState(
                        rootTab = RootTab.Me,
                        previousRootTab = RootTab.Me,
                        secondaryScreen = SecondaryScreen.Settings,
                        secondaryEntryId = 6,
                    ),
            )

        val recentState: MusicAppUiState =
            NavigationStateController.navigateToSecondary(
                state = state,
                screen = SecondaryScreen.RecentPlayed,
            )

        assertEquals(expected = SecondaryScreen.RecentPlayed, actual = recentState.navigationState.secondaryScreen)
        assertEquals(expected = "secondary:RecentPlayed:7", actual = recentState.navigationState.scrollStateKey)
        assertEquals(
            expected = listOf(SecondaryStackEntry(screen = SecondaryScreen.Settings, entryId = 6)),
            actual = recentState.navigationState.secondaryBackStack,
        )

        val backState: MusicAppUiState = NavigationStateController.navigateBack(state = recentState)

        assertEquals(expected = SecondaryScreen.Settings, actual = backState.navigationState.secondaryScreen)
        assertEquals(expected = 6, actual = backState.navigationState.secondaryEntryId)
        assertEquals(expected = emptyList(), actual = backState.navigationState.secondaryBackStack)
    }

    /**
     * 页面 fixed-bar 策略应由导航状态纯派生，避免 facade 层重复维护同一规则。
     */
    @Test
    fun navigationStateProvidesFixedBarMode() {
        val topLevelState: NavigationState = NavigationState()
        assertEquals(expected = MobileFixedBarMode.TopLevel, actual = topLevelState.fixedBarMode)
        assertTrue(actual = topLevelState.fixedBarMode.showsBottomNavigation)
        assertEquals(
            expected = MobileFixedBarPlacement.TopLevel,
            actual = topLevelState.fixedBarMode.fixedBarPlacement,
        )

        val secondaryState: NavigationState =
            NavigationState(
                secondaryScreen = SecondaryScreen.AlbumDetail,
            )
        assertEquals(expected = MobileFixedBarMode.SecondaryWithMiniPlayer, actual = secondaryState.fixedBarMode)
        assertFalse(actual = secondaryState.fixedBarMode.showsBottomNavigation)
        assertEquals(
            expected = MobileFixedBarPlacement.MiniPlayerOnly,
            actual = secondaryState.fixedBarMode.fixedBarPlacement,
        )

        val artistDetailState: NavigationState =
            NavigationState(
                secondaryScreen = SecondaryScreen.ArtistDetail,
            )
        assertEquals(expected = MobileFixedBarMode.SecondaryWithMiniPlayer, actual = artistDetailState.fixedBarMode)
        assertFalse(actual = artistDetailState.fixedBarMode.showsBottomNavigation)
        assertEquals(
            expected = MobileFixedBarPlacement.MiniPlayerOnly,
            actual = artistDetailState.fixedBarMode.fixedBarPlacement,
        )

        val fullscreenPlayerState: NavigationState =
            NavigationState(
                secondaryScreen = SecondaryScreen.Player,
            )
        assertEquals(expected = MobileFixedBarMode.Player, actual = fullscreenPlayerState.fixedBarMode)
        assertFalse(actual = fullscreenPlayerState.fixedBarMode.showsBottomNavigation)
        assertEquals(
            expected = MobileFixedBarPlacement.Hidden,
            actual = fullscreenPlayerState.fixedBarMode.fixedBarPlacement,
        )

        val settingsState: NavigationState =
            NavigationState(
                secondaryScreen = SecondaryScreen.Settings,
            )
        assertEquals(expected = MobileFixedBarMode.SecondaryWithMiniPlayer, actual = settingsState.fixedBarMode)
        assertEquals(
            expected = MobileFixedBarPlacement.MiniPlayerOnly,
            actual = settingsState.fixedBarMode.fixedBarPlacement,
        )

        val recentPlayedState: NavigationState =
            NavigationState(
                secondaryScreen = SecondaryScreen.RecentPlayed,
            )
        assertEquals(expected = MobileFixedBarMode.SecondaryWithMiniPlayer, actual = recentPlayedState.fixedBarMode)
        assertFalse(actual = recentPlayedState.fixedBarMode.showsBottomNavigation)
        assertNull(actual = recentPlayedState.chromeOverlayScreen)
        assertEquals(
            expected = MobileFixedBarPlacement.MiniPlayerOnly,
            actual = recentPlayedState.fixedBarMode.fixedBarPlacement,
        )

        val aboutState: NavigationState =
            NavigationState(
                secondaryScreen = SecondaryScreen.About,
            )
        assertEquals(expected = MobileFixedBarMode.SecondaryWithoutChrome, actual = aboutState.fixedBarMode)
        assertEquals(
            expected = MobileFixedBarPlacement.Hidden,
            actual = aboutState.fixedBarMode.fixedBarPlacement,
        )

        val localPlaylistManagementState: NavigationState =
            NavigationState(
                secondaryScreen = SecondaryScreen.LocalPlaylistManagement,
            )
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithoutChrome,
            actual = localPlaylistManagementState.fixedBarMode,
        )
        assertFalse(actual = localPlaylistManagementState.fixedBarMode.showsBottomNavigation)
        assertEquals(
            expected = MobileFixedBarPlacement.Hidden,
            actual = localPlaylistManagementState.fixedBarMode.fixedBarPlacement,
        )
        assertEquals(
            expected = SecondaryScreen.LocalPlaylistManagement,
            actual = localPlaylistManagementState.chromeOverlayScreen,
        )
    }

    /** 构造只包含导航测试所需最小字段的 [MusicAppUiState]。 */
    private fun testState(): MusicAppUiState =
        MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
            homeLocalSongPreview =
                listOf(
                    Song(
                        id = "song-1",
                        title = "Song",
                        artist = "Artist",
                        album = "Album",
                        duration = "03:00",
                        coverArt = CoverArt.CoverSeaDream,
                        isLiked = false,
                        lastPlayed = "",
                        quality = "Lossless",
                        lyric = "",
                        trackNumber = 1,
                        durationMs = 180_000L,
                    ),
                ),
        )
}
