package com.yanhao.kmpmusic.feature.app.navigation

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.AppChromeMode
import com.yanhao.kmpmusic.feature.app.BottomChromePlacement
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.NavigationState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
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

    /**
     * 切换一级页时应彻底退出二级路由，并把目标一级页作为新的返回基线。
     */
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

    /**
     * 从二级页返回时应恢复上次一级页，同时保留 entry id 作为滚动 key 的稳定来源。
     */
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

    /**
     * 页面 chrome 策略应由导航状态纯派生，避免 facade 层重复维护同一规则。
     */
    @Test
    fun navigationStateProvidesChromeMode(): Unit {
        val topLevelState: NavigationState = NavigationState()
        assertEquals(expected = AppChromeMode.TopLevel, actual = topLevelState.chromeMode)
        assertTrue(actual = topLevelState.chromeMode.showsBottomNavigation)
        assertEquals(
            expected = BottomChromePlacement.TopLevel,
            actual = topLevelState.chromeMode.bottomChromePlacement,
        )

        val secondaryState: NavigationState = NavigationState(
            secondaryScreen = SecondaryScreen.AlbumDetail,
        )
        assertEquals(expected = AppChromeMode.SecondaryWithMiniPlayer, actual = secondaryState.chromeMode)
        assertFalse(actual = secondaryState.chromeMode.showsBottomNavigation)
        assertEquals(
            expected = BottomChromePlacement.MiniPlayerOnly,
            actual = secondaryState.chromeMode.bottomChromePlacement,
        )

        val fullscreenPlayerState: NavigationState = NavigationState(
            secondaryScreen = SecondaryScreen.Player,
        )
        assertEquals(expected = AppChromeMode.SecondaryFullscreen, actual = fullscreenPlayerState.chromeMode)
        assertFalse(actual = fullscreenPlayerState.chromeMode.showsBottomNavigation)
        assertEquals(
            expected = BottomChromePlacement.Hidden,
            actual = fullscreenPlayerState.chromeMode.bottomChromePlacement,
        )

        val fullscreenSettingsState: NavigationState = NavigationState(
            secondaryScreen = SecondaryScreen.Settings,
        )
        assertEquals(expected = AppChromeMode.SecondaryFullscreen, actual = fullscreenSettingsState.chromeMode)
        assertEquals(
            expected = BottomChromePlacement.Hidden,
            actual = fullscreenSettingsState.chromeMode.bottomChromePlacement,
        )
    }

    /** 构造只包含导航测试所需最小字段的 [MusicAppUiState]。 */
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
}
