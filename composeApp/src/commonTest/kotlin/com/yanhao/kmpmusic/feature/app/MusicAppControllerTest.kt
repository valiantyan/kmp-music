package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import kotlinx.coroutines.runBlocking
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
     * App 启动后不能把 seed 曲库或扫描结果预填成真实播放状态。
     */
    @Test
    fun initialStateHasNoPlaybackBeforeUserAction(): Unit {
        val controller = MusicAppController()
        assertNull(controller.uiState.currentSongId)
        assertNull(controller.uiState.currentSong)
        assertFalse(controller.uiState.isPlaying)
        assertTrue(controller.uiState.queueSongIds.isEmpty())
        assertTrue(controller.uiState.songs.isEmpty())
        assertTrue(controller.uiState.localSongPreview.isEmpty())
        assertTrue(controller.uiState.recentSongs.isEmpty())
    }

    /**
     * 查看全部应进入本地音乐二级页，底部 Tab 隐藏但 mini-player 策略保持普通二级页。
     */
    @Test
    fun openLocalMusicUsesSecondaryChrome(): Unit {
        val controller = MusicAppController()
        controller.openLocalMusic(section = LocalMusicSection.Songs)
        assertEquals(
            expected = SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Songs),
            actual = controller.uiState.navigationState.secondaryScreen,
        )
        assertEquals(
            expected = AppChromeMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.chromeMode,
        )
    }

    /**
     * 扫描完成只应填充本地歌曲预览，不应把扫描结果冒充最近播放。
     */
    @Test
    fun scanDoesNotPopulateRecentPlayback(): Unit = runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        assertEquals(expected = 8, actual = controller.uiState.songs.size)
        assertEquals(expected = 6, actual = controller.uiState.localSongPreview.size)
        assertEquals(expected = 8, actual = controller.uiState.libraryStats.songCount)
        assertTrue(controller.uiState.localMusicSources.isNotEmpty())
        assertTrue(controller.uiState.recentSongs.isEmpty())
    }

    /**
     * 用户真正播放歌曲后，最近播放才出现该歌曲。
     */
    @Test
    fun playSongAddsRecentPlayback(): Unit = runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val targetSong: Song = controller.uiState.songs.first()
        controller.playSong(song = targetSong)
        assertEquals(
            expected = listOf(targetSong.id),
            actual = controller.uiState.recentSongs.map { song -> song.id },
        )
    }

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
    fun playSongUpdatesPlaybackAndQueue(): Unit = runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val targetSong: Song = controller.uiState.songs.first { song ->
            song.title == "The Best of Me"
        }
        controller.playSong(song = targetSong)
        assertEquals(targetSong.id, controller.uiState.currentSongId)
        assertTrue(controller.uiState.isPlaying)
        assertTrue(controller.uiState.queueSongIds.contains(targetSong.id))
    }

    /**
     * 用户播放歌曲后才会写入真实播放历史，重复播放同一首时保持最近一次在最前。
     */
    @Test
    fun playSongRecordsPlaybackHistory(): Unit = runBlocking {
        val playbackRepository = InMemoryPlaybackRepository()
        val controller = MusicAppController(playbackRepository = playbackRepository)
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val firstSong: Song = controller.uiState.songs.first { song -> song.title == "海边的梦" }
        val secondSong: Song = controller.uiState.songs.first { song -> song.title == "The Best of Me" }
        controller.playSong(song = firstSong)
        controller.playSong(song = secondSong)
        controller.playSong(song = firstSong)
        assertEquals(
            expected = listOf(firstSong.id, secondSong.id),
            actual = playbackRepository.getPlaybackHistory().songIds,
        )
    }

    /**
     * 队列为空时切歌不能用完整曲库第一首静默替换当前播放。
     */
    @Test
    fun moveTrackDoesNotUseSongsAsImplicitQueue(): Unit {
        val controller = MusicAppController()
        controller.moveTrack(direction = 1)
        assertNull(controller.uiState.currentSongId)
        assertFalse(controller.uiState.isPlaying)
        assertTrue(controller.uiState.queueSongIds.isEmpty())
    }

    /**
     * 用户播放形成显式队列后，队列切歌才应循环移动并保持播放状态。
     */
    @Test
    fun moveTrackChangesCurrentSong(): Unit = runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        controller.playSong(song = controller.uiState.songs[0])
        controller.playSong(song = controller.uiState.songs[1])
        val originalSongId: String? = controller.uiState.currentSongId
        controller.moveTrack(direction = 1)
        assertNotEquals(originalSongId, controller.uiState.currentSongId)
        assertTrue(controller.uiState.isPlaying)
    }

    /**
     * 收藏状态应同时同步到集合和歌曲列表。
     */
    @Test
    fun toggleFavoriteSyncsSongList(): Unit = runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val targetSong: Song = controller.uiState.songs.first { song -> song.title == "Summer Waltz" }
        controller.toggleFavorite(songId = targetSong.id)
        assertTrue(controller.uiState.likedSongIds.contains(targetSong.id))
        assertTrue(controller.uiState.songs.first { song -> song.id == targetSong.id }.isLiked)
    }

    /**
     * 搜索范围为歌曲时不应返回专辑和歌手。
     */
    @Test
    fun searchScopeLimitsResultTypes(): Unit = runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
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

    /**
     * 一级页面滚动 key 应按 Tab 保持稳定，保证从二级页返回后能恢复原滚动位置。
     */
    @Test
    fun rootScrollStateKeyStaysStableAfterSecondaryReturn(): Unit {
        val controller = MusicAppController()
        val rootKey: String = controller.uiState.navigationState.scrollStateKey
        controller.navigateToSecondary(screen = SecondaryScreen.AlbumDetail)
        assertNotEquals(rootKey, controller.uiState.navigationState.scrollStateKey)
        controller.navigateBack()
        assertEquals(rootKey, controller.uiState.navigationState.scrollStateKey)
    }

    /**
     * 二级页面每次进入都应使用新滚动 key，避免继承上一次或一级页滚动位置。
     */
    @Test
    fun secondaryScrollStateKeyChangesForEachEntry(): Unit {
        val controller = MusicAppController()
        controller.navigateToSecondary(screen = SecondaryScreen.AlbumDetail)
        val firstSecondaryKey: String = controller.uiState.navigationState.scrollStateKey
        controller.navigateBack()
        controller.navigateToSecondary(screen = SecondaryScreen.AlbumDetail)
        assertNotEquals(firstSecondaryKey, controller.uiState.navigationState.scrollStateKey)
    }

    /**
     * 系统返回键在二级页应回到一级页，而不是交给系统直接退出 App。
     */
    @Test
    fun systemBackReturnsFromSecondaryScreen(): Unit = runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        controller.navigateToRoot(tab = RootTab.Favorites)
        controller.navigateToSecondary(screen = SecondaryScreen.AlbumDetail)
        assertTrue(controller.uiState.canHandleSystemBack)
        assertTrue(controller.handleSystemBack())
        assertTrue(controller.uiState.navigationState.isTopLevel)
        assertEquals(RootTab.Favorites, controller.uiState.navigationState.rootTab)
        assertFalse(controller.uiState.canHandleSystemBack)
        assertFalse(controller.handleSystemBack())
    }

    /**
     * 系统返回键应优先关闭临时浮层，再处理二级页面返回。
     */
    @Test
    fun systemBackClosesOverlayBeforeSecondaryScreen(): Unit {
        val controller = MusicAppController()
        controller.navigateToSecondary(screen = SecondaryScreen.AlbumDetail)
        controller.openQueue()
        assertTrue(controller.handleSystemBack())
        assertFalse(controller.uiState.isQueueOpen)
        assertFalse(controller.uiState.navigationState.isTopLevel)
        assertTrue(controller.handleSystemBack())
        assertTrue(controller.uiState.navigationState.isTopLevel)
    }

    /**
     * 页面 chrome 策略应区分一级页、普通二级页和全屏二级页。
     */
    @Test
    fun navigationStateProvidesChromeMode(): Unit {
        val controller = MusicAppController()
        assertEquals(AppChromeMode.TopLevel, controller.uiState.navigationState.chromeMode)
        assertTrue(controller.uiState.navigationState.chromeMode.showsBottomNavigation)
        assertEquals(BottomChromePlacement.TopLevel, controller.uiState.navigationState.chromeMode.bottomChromePlacement)
        controller.navigateToSecondary(screen = SecondaryScreen.AlbumDetail)
        assertEquals(AppChromeMode.SecondaryWithMiniPlayer, controller.uiState.navigationState.chromeMode)
        assertFalse(controller.uiState.navigationState.chromeMode.showsBottomNavigation)
        assertEquals(BottomChromePlacement.MiniPlayerOnly, controller.uiState.navigationState.chromeMode.bottomChromePlacement)
        controller.navigateBack()
        controller.openLocalMusic(section = LocalMusicSection.Songs)
        assertEquals(AppChromeMode.SecondaryWithMiniPlayer, controller.uiState.navigationState.chromeMode)
        controller.navigateToSecondary(screen = SecondaryScreen.Player)
        assertEquals(AppChromeMode.SecondaryFullscreen, controller.uiState.navigationState.chromeMode)
        assertFalse(controller.uiState.navigationState.chromeMode.showsBottomNavigation)
        assertEquals(BottomChromePlacement.Hidden, controller.uiState.navigationState.chromeMode.bottomChromePlacement)
        controller.navigateBack()
        controller.navigateToSecondary(screen = SecondaryScreen.Settings)
        assertEquals(AppChromeMode.SecondaryFullscreen, controller.uiState.navigationState.chromeMode)
        assertEquals(BottomChromePlacement.Hidden, controller.uiState.navigationState.chromeMode.bottomChromePlacement)
    }
}
