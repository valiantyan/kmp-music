package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.domain.model.SearchScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [MusicAppController] 的核心交互测试，覆盖原型迁移后的关键状态规则。
 */
class MusicAppControllerTest {
    /**
     * 二级页面应隐藏主导航，并保留返回前的根 Tab。
     */
    @Test
    fun secondaryScreenKeepsPreviousRootTab(): Unit {
        val controller = MusicAppController()
        controller.navigateToRoot(tab = RootTab.Favorites)
        controller.navigateToSecondary(screen = SecondaryScreen.Search)
        assertFalse(controller.uiState.navigationState.isTopLevel)
        assertEquals(RootTab.Favorites, controller.uiState.navigationState.previousRootTab)
        controller.navigateBack()
        assertTrue(controller.uiState.navigationState.isTopLevel)
        assertEquals(RootTab.Favorites, controller.uiState.navigationState.rootTab)
    }

    /**
     * 播放歌曲后当前歌曲、播放状态和队列应同步。
     */
    @Test
    fun playSongUpdatesPlaybackAndQueue(): Unit {
        val controller = MusicAppController()
        val targetSong = controller.uiState.songs.first { song -> song.id == "best" }
        controller.playSong(song = targetSong)
        assertEquals("best", controller.uiState.currentSongId)
        assertTrue(controller.uiState.isPlaying)
        assertTrue(controller.uiState.queueSongIds.contains("best"))
    }

    /**
     * 队列切歌应循环移动并保持播放状态。
     */
    @Test
    fun moveTrackChangesCurrentSong(): Unit {
        val controller = MusicAppController()
        val originalSongId = controller.uiState.currentSongId
        controller.moveTrack(direction = 1)
        assertNotEquals(originalSongId, controller.uiState.currentSongId)
        assertTrue(controller.uiState.isPlaying)
    }

    /**
     * 收藏状态应同时同步到集合和歌曲列表。
     */
    @Test
    fun toggleFavoriteSyncsSongList(): Unit {
        val controller = MusicAppController()
        controller.toggleFavorite(songId = "summer-waltz")
        assertTrue(controller.uiState.likedSongIds.contains("summer-waltz"))
        assertTrue(controller.uiState.songs.first { song -> song.id == "summer-waltz" }.isLiked)
    }

    /**
     * 搜索范围为歌曲时不应返回专辑和歌手。
     */
    @Test
    fun searchScopeLimitsResultTypes(): Unit {
        val controller = MusicAppController()
        controller.setSearchQuery(query = "旅行团")
        controller.setSearchScope(scope = SearchScope.Songs)
        val result = controller.search()
        assertTrue(result.songs.isNotEmpty())
        assertTrue(result.albums.isEmpty())
        assertTrue(result.artists.isEmpty())
    }

    /**
     * 一级 Tab 切换后不应存在二级页面。
     */
    @Test
    fun rootNavigationClearsSecondaryScreen(): Unit {
        val controller = MusicAppController()
        controller.navigateToSecondary(screen = SecondaryScreen.Player)
        controller.navigateToRoot(tab = RootTab.Me)
        assertNull(controller.uiState.navigationState.secondaryScreen)
        assertEquals(RootTab.Me, controller.uiState.navigationState.rootTab)
    }
}
