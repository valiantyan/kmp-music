package com.yanhao.kmpmusic.feature.app.navigation

import com.yanhao.kmpmusic.data.FakeLocalMusicScanner
import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.HomeContentSection
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.NavigationState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.app.library.LibraryStateSynchronizer
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [ContentNavigationController] 的聚焦测试，确保内容导航拆分后仍保持既有工作流语义。
 */
class ContentNavigationControllerTest {
    /**
     * 打开本地音乐应按需加载完整曲库，并进入指定二级分段。
     */
    @Test
    fun openLocalMusicLoadsFullLibraryAndOpensRequestedSection() {
        val repository: InMemoryMusicLibraryRepository = createSeededRepository()
        val controller: ContentNavigationController = createController(repository = repository)

        val result: ContentNavigationController.Result =
            controller.openLocalMusic(
                state = baseState(repository = repository),
                section = LocalMusicSection.Artists,
            )

        assertTrue(actual = result.loadedFullLibrary)
        assertTrue(actual = result.state.localSongs.isNotEmpty())
        assertEquals(
            expected = SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Artists),
            actual = result.state.navigationState.secondaryScreen,
        )
    }

    /**
     * 我的页歌曲统计入口应回到首页歌曲分段，并保持一级页语义。
     */
    @Test
    fun openHomeSongsReturnsToHomeSongsTopLevelPage() {
        val repository: InMemoryMusicLibraryRepository = createSeededRepository()
        val controller: ContentNavigationController = createController(repository = repository)
        val initialState: MusicAppUiState =
            baseState(repository = repository).copy(
                homeContentSection = HomeContentSection.Albums,
                navigationState =
                    NavigationState(
                        rootTab = RootTab.Me,
                        previousRootTab = RootTab.Me,
                        secondaryScreen = SecondaryScreen.RecentPlayed,
                    ),
            )

        val result: ContentNavigationController.Result = controller.openHomeSongs(state = initialState)

        assertFalse(actual = result.loadedFullLibrary)
        assertEquals(expected = RootTab.Home, actual = result.state.navigationState.rootTab)
        assertNull(actual = result.state.navigationState.secondaryScreen)
        assertEquals(expected = HomeContentSection.Songs, actual = result.state.homeContentSection)
    }

    /**
     * 扫描页和最近播放页只是导航入口，不应为了进入页面读取完整曲库。
     */
    @Test
    fun scanAndRecentRoutesDoNotLoadFullLibrary() {
        val repository: InMemoryMusicLibraryRepository = createSeededRepository()
        val controller: ContentNavigationController = createController(repository = repository)
        val initialState: MusicAppUiState = baseState(repository = repository)

        val scanResult: ContentNavigationController.Result = controller.openAudioScan(state = initialState)
        val recentResult: ContentNavigationController.Result = controller.openRecentPlayed(state = initialState)

        assertFalse(actual = scanResult.loadedFullLibrary)
        assertFalse(actual = recentResult.loadedFullLibrary)
        assertTrue(actual = scanResult.state.localSongs.isEmpty())
        assertTrue(actual = recentResult.state.localSongs.isEmpty())
        assertEquals(expected = SecondaryScreen.AudioScan, actual = scanResult.state.navigationState.secondaryScreen)
        assertEquals(expected = SecondaryScreen.RecentPlayed, actual = recentResult.state.navigationState.secondaryScreen)
    }

    /**
     * 专辑和歌手详情入口必须先补齐完整曲库，再写入选中身份并进入二级页。
     */
    @Test
    fun albumAndArtistDetailRoutesLoadLibraryBeforeSelectingIdentity() {
        val repository: InMemoryMusicLibraryRepository = createSeededRepository()
        val controller: ContentNavigationController = createController(repository = repository)
        val initialState: MusicAppUiState = baseState(repository = repository)

        val albumResult: ContentNavigationController.Result =
            controller.openAlbum(
                state = initialState,
                album = initialState.detailAlbums.first(),
            )
        val artistResult: ContentNavigationController.Result =
            controller.openArtist(
                state = initialState,
                artist = initialState.detailArtists.first(),
            )

        assertTrue(actual = albumResult.loadedFullLibrary)
        assertTrue(actual = artistResult.loadedFullLibrary)
        assertTrue(actual = albumResult.state.localSongs.isNotEmpty())
        assertTrue(actual = artistResult.state.localSongs.isNotEmpty())
        assertEquals(expected = SecondaryScreen.AlbumDetail, actual = albumResult.state.navigationState.secondaryScreen)
        assertEquals(expected = SecondaryScreen.ArtistDetail, actual = artistResult.state.navigationState.secondaryScreen)
        assertEquals(expected = initialState.detailAlbums.first().id, actual = albumResult.state.selectedAlbumId)
        assertEquals(expected = initialState.detailArtists.first().id, actual = artistResult.state.selectedArtistId)
    }

    /**
     * 从歌曲进入详情时应复用元数据匹配，并关闭歌曲更多菜单残留。
     */
    @Test
    fun songDetailRoutesMatchMetadataAndCloseMoreMenu() {
        val repository: InMemoryMusicLibraryRepository = createSeededRepository()
        val controller: ContentNavigationController = createController(repository = repository)
        val targetSong: Song = baseState(repository = repository).homeLocalSongPreview.first()

        val albumResult: ContentNavigationController.Result =
            controller.openAlbumFromSong(
                state = baseState(repository = repository).copy(moreSongId = targetSong.id),
                song = targetSong,
            )
        val artistResult: ContentNavigationController.Result =
            controller.openArtistFromSong(
                state = baseState(repository = repository).copy(moreSongId = targetSong.id),
                song = targetSong,
            )

        assertTrue(actual = albumResult.loadedFullLibrary)
        assertTrue(actual = artistResult.loadedFullLibrary)
        assertNull(actual = albumResult.state.moreSongId)
        assertNull(actual = artistResult.state.moreSongId)
        assertEquals(expected = SecondaryScreen.AlbumDetail, actual = albumResult.state.navigationState.secondaryScreen)
        assertEquals(expected = SecondaryScreen.ArtistDetail, actual = artistResult.state.navigationState.secondaryScreen)
    }

    /** 构造控制器依赖，复用真实曲库同步规则避免测试和生产分叉。 */
    private fun createController(repository: InMemoryMusicLibraryRepository): ContentNavigationController {
        val initialSongs: List<Song> = repository.getHomePreview(limit = 6)
        val likedSongIds: Set<String> =
            initialSongs
                .filter { song: Song -> song.isLiked }
                .map { song: Song -> song.id }
                .toSet()
        return ContentNavigationController(
            libraryStateSynchronizer =
                LibraryStateSynchronizer(
                    musicLibraryRepository = repository,
                    favoritesRepository =
                        InMemoryFavoritesRepository(
                            initialLikedSongIds = likedSongIds,
                        ),
                    playbackRepository = InMemoryPlaybackRepository(),
                ),
        )
    }

    /** 先灌入一份 fake 扫描结果，让导航测试可以验证真实的完整曲库预热行为。 */
    private fun createSeededRepository(): InMemoryMusicLibraryRepository {
        val repository: InMemoryMusicLibraryRepository = InMemoryMusicLibraryRepository()
        val scanner: FakeLocalMusicScanner = FakeLocalMusicScanner(demoSongCount = 8)
        runBlocking {
            repository.applyScanResult(
                request = LocalMusicScanRequest.Refresh,
                scanResult = scanner.scan(request = LocalMusicScanRequest.Refresh),
                likedSongIds = scanner.demoFavoriteSongIds(),
            )
        }
        return repository
    }

    /** 仅构造内容导航测试需要的最小初始状态，保持与门面冷启动语义一致。 */
    private fun baseState(repository: InMemoryMusicLibraryRepository): MusicAppUiState {
        val previewSongs: List<Song> = repository.getHomePreview(limit = 6)
        return MusicAppUiState(
            homeLocalSongPreview = previewSongs,
            likedSongIds =
                previewSongs
                    .filter { song: Song -> song.isLiked }
                    .map { song: Song -> song.id }
                    .toSet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        )
    }
}
