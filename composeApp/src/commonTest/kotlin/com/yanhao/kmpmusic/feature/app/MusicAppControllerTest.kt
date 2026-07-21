package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.data.FakeAudioPlayerEngine
import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.data.InMemoryUserPreferencesRepository
import com.yanhao.kmpmusic.domain.model.AddSongToLocalPlaylistResult
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.CreateLocalPlaylistWithSongResult
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.LocalPlaylist
import com.yanhao.kmpmusic.domain.model.LocalPlaylistCreateResult
import com.yanhao.kmpmusic.domain.model.LocalPlaylistDeleteResult
import com.yanhao.kmpmusic.domain.model.LocalPlaylistDetail
import com.yanhao.kmpmusic.domain.model.LocalPlaylistSong
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.repository.FavoritesRepository
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.domain.repository.LocalPlaylistRepository
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.repository.SearchHistoryRepository
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.feature.screen.cancelledScanResultDetail
import com.yanhao.kmpmusic.feature.screen.cancelledScanResultTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [MusicAppController] 的核心交互测试，覆盖原型迁移后的关键状态规则。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MusicAppControllerTest {
    @Test
    fun queueSongsSurviveAfterPlaybackContextIsNoLongerInSongs(): Unit =
        runTest {
            val controller = createController(controllerScope = backgroundScope)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val queueSongs =
                controller.uiState.localSongs
                    .take(4)
                    .ifEmpty { controller.uiState.homeLocalSongPreview.take(4) }

            controller.playSong(song = queueSongs[0], queueSongs = queueSongs)

            assertEquals(expected = queueSongs.map { song -> song.id }, actual = controller.uiState.queueSongs.map { song -> song.id })
        }

    /**
     * 改变当前播放事实的公开入口后，UI 队列、仓库队列和当前歌曲实体必须保持一致。
     */
    @Test
    fun playbackActionsKeepQueueIdsAndRepositoryQueueConsistent(): Unit =
        runTest {
            val playbackRepository = InMemoryPlaybackRepository()
            val controller =
                createController(
                    playbackRepository = playbackRepository,
                    controllerScope = backgroundScope,
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val queueSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 3)
            val initialQueueIds: List<String> = queueSongs.map { song: Song -> song.id }

            controller.playSong(song = queueSongs[0], queueSongs = queueSongs)
            advanceUntilIdle()
            assertPlaybackQueueInvariant(
                controller = controller,
                playbackRepository = playbackRepository,
                expectedQueueSongIds = initialQueueIds,
            )

            controller.skipToQueueIndex(index = 1)
            advanceUntilIdle()
            assertPlaybackQueueInvariant(
                controller = controller,
                playbackRepository = playbackRepository,
                expectedQueueSongIds = initialQueueIds,
            )

            controller.removeFromQueue(songId = queueSongs[0].id)
            advanceUntilIdle()
            assertPlaybackQueueInvariant(
                controller = controller,
                playbackRepository = playbackRepository,
                expectedQueueSongIds = initialQueueIds.drop(n = 1),
            )
        }

    /**
     * 查看全部应进入本地音乐二级页，底部 Tab 隐藏但 mini-player 策略保持普通二级页。
     */
    @Test
    fun openLocalMusicUsesSecondaryFixedBarMode() {
        val controller = createController()
        controller.openLocalMusic(section = LocalMusicSection.Songs)
        assertEquals(
            expected = SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Songs),
            actual = controller.uiState.navigationState.secondaryScreen,
        )
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
    }

    /**
     * 设置页的本地来源入口应能直接打开来源分段。
     */
    @Test
    fun openLocalMusicCanStartAtSourcesSection() {
        val controller = createController()
        controller.openLocalMusic(section = LocalMusicSection.Sources)
        assertEquals(
            expected = SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Sources),
            actual = controller.uiState.navigationState.secondaryScreen,
        )
    }

    /**
     * 首页空态扫描入口应进入覆盖型扫描页，让底层一级 chrome 保持原位。
     */
    @Test
    fun openAudioScanUsesDedicatedScanRoute() {
        val controller = createController()
        controller.openAudioScan()
        assertEquals(
            expected = SecondaryScreen.AudioScan,
            actual = controller.uiState.navigationState.secondaryScreen,
        )
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithoutChrome,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
        assertEquals(
            expected = SecondaryScreen.AudioScan,
            actual = controller.uiState.navigationState.chromeOverlayScreen,
        )
        assertNull(actual = controller.uiState.navigationState.chromeUnderlaySecondaryScreen)
        assertEquals(
            expected = MobileFixedBarMode.TopLevel,
            actual = controller.uiState.navigationState.chromeUnderlayFixedBarMode,
        )
    }

    /**
     * 我的页查看全部应进入最近播放普通二级页，并保持底部 Tab 与迷你播放器策略稳定。
     */
    @Test
    fun meViewAllRecentPlayedOpensRecentPageAndReturnsToMe() {
        val controller = createController()
        controller.navigateToRoot(tab = RootTab.Me)

        controller.openRecentPlayed()

        assertEquals(expected = RootTab.Me, actual = controller.uiState.navigationState.rootTab)
        assertEquals(expected = SecondaryScreen.RecentPlayed, actual = controller.uiState.navigationState.secondaryScreen)
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
        assertFalse(actual = controller.uiState.navigationState.fixedBarMode.showsBottomNavigation)
        assertEquals(
            expected = MobileFixedBarPlacement.MiniPlayerOnly,
            actual = controller.uiState.navigationState.fixedBarMode.fixedBarPlacement,
        )
        assertEquals(
            expected = ContentBottomSpace.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.fixedBarMode.contentBottomSpace,
        )
        assertFalse(actual = controller.uiState.navigationState.fixedBarMode.coversUnderlyingChrome)
        assertNull(actual = controller.uiState.navigationState.chromeOverlayScreen)
        assertEquals(
            expected = SecondaryScreen.RecentPlayed,
            actual = controller.uiState.navigationState.chromeUnderlaySecondaryScreen,
        )
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.chromeUnderlayFixedBarMode,
        )

        controller.navigateBack()

        assertEquals(expected = RootTab.Me, actual = controller.uiState.navigationState.rootTab)
        assertNull(actual = controller.uiState.navigationState.secondaryScreen)
        assertEquals(
            expected = MobileFixedBarMode.TopLevel,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
        assertTrue(actual = controller.uiState.navigationState.fixedBarMode.showsBottomNavigation)
        assertEquals(
            expected = MobileFixedBarPlacement.TopLevel,
            actual = controller.uiState.navigationState.fixedBarMode.fixedBarPlacement,
        )
        assertEquals(
            expected = ContentBottomSpace.TopLevel,
            actual = controller.uiState.navigationState.fixedBarMode.contentBottomSpace,
        )
        assertFalse(actual = controller.uiState.navigationState.fixedBarMode.coversUnderlyingChrome)
    }

    /**
     * 我的页歌曲统计入口应回到首页歌曲分段，避免误进入本地音乐二级页或保留旧分段。
     */
    @Test
    fun meSongStatOpensHomeSongsAsTopLevelPage() {
        val controller = createController()
        controller.setHomeContentSection(section = HomeContentSection.Albums)
        controller.navigateToRoot(tab = RootTab.Me)
        controller.openRecentPlayed()

        controller.openHomeSongs()

        assertEquals(expected = RootTab.Home, actual = controller.uiState.navigationState.rootTab)
        assertNull(actual = controller.uiState.navigationState.secondaryScreen)
        assertEquals(expected = HomeContentSection.Songs, actual = controller.uiState.homeContentSection)
        assertEquals(
            expected = MobileFixedBarMode.TopLevel,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
        assertTrue(actual = controller.uiState.navigationState.fixedBarMode.showsBottomNavigation)
    }

    /**
     * 没有本地自建歌单时，我的页歌单统计只能提示空态，不能跳进空列表页。
     */
    @Test
    fun mePlaylistStatShowsEmptyMessageWithoutNavigatingWhenNoPlaylists() {
        val controller: MusicAppController = createController()
        controller.navigateToRoot(tab = RootTab.Me)

        controller.openLocalPlaylists()

        assertEquals(expected = 0, actual = controller.uiState.localPlaylistCount)
        assertNull(actual = controller.uiState.navigationState.secondaryScreen)
        assertEquals(expected = RootTab.Me, actual = controller.uiState.navigationState.rootTab)
        assertEquals(expected = "暂无歌单", actual = controller.uiState.transientMessage)
    }

    /**
     * 有本地自建歌单时，我的页歌单统计进入普通二级列表页并保持迷你播放器 chrome。
     */
    @Test
    fun mePlaylistStatOpensMobilePlaylistListAsSecondaryPage() {
        val playlist: LocalPlaylist = testPlaylist(id = "playlist-road", name = "Road", updatedAt = 20L)
        val repository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists = mutableListOf(playlist),
            )
        val controller: MusicAppController = createController(localPlaylistRepository = repository)
        controller.navigateToRoot(tab = RootTab.Me)

        controller.openLocalPlaylists()

        assertEquals(expected = 1, actual = controller.uiState.localPlaylistCount)
        assertEquals(expected = SecondaryScreen.LocalPlaylists, actual = controller.uiState.navigationState.secondaryScreen)
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
        assertFalse(actual = controller.uiState.navigationState.fixedBarMode.showsBottomNavigation)
        assertEquals(
            expected = MobileFixedBarPlacement.MiniPlayerOnly,
            actual = controller.uiState.navigationState.fixedBarMode.fixedBarPlacement,
        )
    }

    /**
     * 移动端歌单列表卡片应展示真实名称、当前可用歌曲数量和第一首真实歌曲封面。
     */
    @Test
    fun localPlaylistCardsUseAvailableSongCountAndFirstScannedCover() {
        val firstSong: Song =
            testSong(id = "song-first", title = "First", modifiedAt = 1L).copy(
                coverArt = CoverArt.AlbumRiverYear,
            )
        val secondSong: Song =
            testSong(id = "song-second", title = "Second", modifiedAt = 2L).copy(
                coverArt = CoverArt.CoverSeaDream,
                coverImageUri = "file://second-cover.jpg",
            )
        val playlist: LocalPlaylist = testPlaylist(id = "playlist-cover", name = "封面歌单", updatedAt = 20L)
        val repository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists = mutableListOf(playlist),
                playlistDetails =
                    mutableMapOf(
                        playlist.id to
                            LocalPlaylistDetail(
                                playlist = playlist,
                                relations =
                                    listOf(
                                        LocalPlaylistSong(
                                            playlistId = playlist.id,
                                            songId = firstSong.id,
                                            addedAt = 1L,
                                            sortOrder = 0,
                                        ),
                                        LocalPlaylistSong(
                                            playlistId = playlist.id,
                                            songId = secondSong.id,
                                            addedAt = 2L,
                                            sortOrder = 1,
                                        ),
                                    ),
                                availableSongs = listOf(firstSong, secondSong),
                            ),
                    ),
            )

        val controller: MusicAppController = createController(localPlaylistRepository = repository)

        val card: LocalPlaylistCardDisplayModel = controller.uiState.localPlaylists.single()
        assertEquals(expected = playlist.id, actual = card.id)
        assertEquals(expected = "封面歌单", actual = card.name)
        assertEquals(expected = 2, actual = card.availableSongCount)
        assertEquals(expected = CoverArt.CoverSeaDream, actual = card.coverArt)
        assertEquals(expected = "file://second-cover.jpg", actual = card.coverImageUri)
    }

    /**
     * 歌单内没有任何真实扫描封面时，卡片必须直接回退到默认本地音乐封面。
     */
    @Test
    fun localPlaylistCardsUseDefaultCoverWhenNoSongHasScannedCover() {
        val songWithoutCover: Song =
            testSong(id = "song-no-cover", title = "No Cover", modifiedAt = 1L).copy(
                coverArt = CoverArt.CoverSummerWaltz,
            )
        val playlist: LocalPlaylist = testPlaylist(id = "playlist-no-cover", name = "无封面歌单", updatedAt = 20L)
        val repository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists = mutableListOf(playlist),
                playlistDetails =
                    mutableMapOf(
                        playlist.id to
                            LocalPlaylistDetail(
                                playlist = playlist,
                                relations =
                                    listOf(
                                        LocalPlaylistSong(
                                            playlistId = playlist.id,
                                            songId = songWithoutCover.id,
                                            addedAt = 20L,
                                            sortOrder = 0,
                                        ),
                                    ),
                                availableSongs = listOf(songWithoutCover),
                            ),
                    ),
            )

        val controller: MusicAppController = createController(localPlaylistRepository = repository)

        val card: LocalPlaylistCardDisplayModel = controller.uiState.localPlaylists.single()
        assertEquals(expected = CoverArt.HeroLocalMusic, actual = card.coverArt)
        assertNull(actual = card.coverImageUri)
    }

    /**
     * 歌单列表排序应按更新时间倒序，时间相同时按名称升序，并为空歌单提供默认封面。
     */
    @Test
    fun localPlaylistCardsUseRepositoryOrderAndDefaultCoverForEmptyPlaylist() {
        val oldPlaylist: LocalPlaylist = testPlaylist(id = "playlist-old", name = "旧歌单", updatedAt = 5L)
        val betaPlaylist: LocalPlaylist = testPlaylist(id = "playlist-beta", name = "Beta", updatedAt = 20L)
        val alphaPlaylist: LocalPlaylist = testPlaylist(id = "playlist-alpha", name = "Alpha", updatedAt = 20L)
        val controller: MusicAppController =
            createController(
                localPlaylistRepository =
                    RecordingLocalPlaylistRepository(
                        playlists = mutableListOf(oldPlaylist, betaPlaylist, alphaPlaylist),
                    ),
            )

        assertEquals(
            expected = listOf("Alpha", "Beta", "旧歌单"),
            actual = controller.uiState.localPlaylists.map { card: LocalPlaylistCardDisplayModel -> card.name },
        )
        assertEquals(
            expected = listOf(0, 0, 0),
            actual = controller.uiState.localPlaylists.map { card: LocalPlaylistCardDisplayModel -> card.availableSongCount },
        )
        assertTrue(
            actual =
                controller.uiState.localPlaylists.all { card: LocalPlaylistCardDisplayModel ->
                    card.coverArt == CoverArt.HeroLocalMusic && card.coverImageUri == null
                },
        )
    }

    /**
     * 新增歌曲后应重新读取歌单事实，让我的页数量、列表排序、封面和歌曲数量即时刷新。
     */
    @Test
    fun addingSongRefreshesLocalPlaylistCardsImmediately() {
        val targetSong: Song =
            testSong(id = "song-refresh", title = "Refresh", modifiedAt = 1L).copy(
                coverArt = CoverArt.CoverSummerWaltz,
                coverImageUri = "file://refresh-cover.jpg",
            )
        val oldPlaylist: LocalPlaylist = testPlaylist(id = "playlist-old", name = "旧歌单", updatedAt = 10L)
        val targetPlaylist: LocalPlaylist = testPlaylist(id = "playlist-target", name = "目标歌单", updatedAt = 5L)
        val refreshedTarget: LocalPlaylist = targetPlaylist.copy(updatedAt = 30L)
        val repository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists = mutableListOf(oldPlaylist, targetPlaylist),
                addSongResults =
                    mutableListOf(
                        AddSongToLocalPlaylistResult.Added(
                            relation =
                                LocalPlaylistSong(
                                    playlistId = targetPlaylist.id,
                                    songId = targetSong.id,
                                    addedAt = 30L,
                                    sortOrder = 0,
                                ),
                        ),
                    ),
            )
        repository.onAddSong = { _: String, _: String ->
            repository.replacePlaylists(
                nextPlaylists = listOf(oldPlaylist, refreshedTarget),
            )
            repository.putPlaylistDetail(
                detail =
                    LocalPlaylistDetail(
                        playlist = refreshedTarget,
                        relations =
                            listOf(
                                LocalPlaylistSong(
                                    playlistId = targetPlaylist.id,
                                    songId = targetSong.id,
                                    addedAt = 30L,
                                    sortOrder = 0,
                                ),
                            ),
                        availableSongs = listOf(targetSong),
                    ),
            )
        }

        val controller: MusicAppController = createController(localPlaylistRepository = repository)
        controller.openAddToPlaylistFlow(song = targetSong)
        controller.selectAddToPlaylistTarget(playlistId = targetPlaylist.id)
        controller.addCurrentSongToSelectedPlaylist()

        assertEquals(
            expected = listOf("目标歌单", "旧歌单"),
            actual = controller.uiState.localPlaylists.map { card: LocalPlaylistCardDisplayModel -> card.name },
        )
        val targetCard: LocalPlaylistCardDisplayModel = controller.uiState.localPlaylists.first()
        assertEquals(expected = 1, actual = targetCard.availableSongCount)
        assertEquals(expected = CoverArt.CoverSummerWaltz, actual = targetCard.coverArt)
        assertEquals(expected = "file://refresh-cover.jpg", actual = targetCard.coverImageUri)
    }

    /**
     * 歌单卡片无论是否有可播放歌曲，都应能进入详情页，空歌单由详情页展示空态。
     */
    @Test
    fun openLocalPlaylistDetailAllowsEmptyPlaylist() {
        val playlist: LocalPlaylist = testPlaylist(id = "playlist-empty", name = "空歌单", updatedAt = 20L)
        val repository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists = mutableListOf(playlist),
                playlistDetails =
                    mutableMapOf(
                        playlist.id to
                            LocalPlaylistDetail(
                                playlist = playlist,
                                relations = emptyList(),
                                availableSongs = emptyList(),
                            ),
                    ),
            )
        val controller: MusicAppController = createController(localPlaylistRepository = repository)

        controller.openLocalPlaylistDetail(playlistId = playlist.id)

        assertEquals(
            expected = SecondaryScreen.LocalPlaylistDetail,
            actual = controller.uiState.navigationState.secondaryScreen,
        )
        assertEquals(expected = playlist.id, actual = controller.uiState.selectedLocalPlaylistDetail?.id)
        assertEquals(expected = "暂无可播放歌曲", actual = controller.uiState.selectedLocalPlaylistDetail?.emptyText)
        assertFalse(actual = controller.uiState.selectedLocalPlaylistDetail?.canPlayAll ?: true)
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
        assertFalse(actual = controller.uiState.navigationState.fixedBarMode.showsBottomNavigation)
    }

    /**
     * 歌单详情只展示当前仍可用歌曲，并按仓库最新添加顺序形成页面列表和封面。
     */
    @Test
    fun localPlaylistDetailUsesAvailableSongsInLatestAddedOrder() {
        val firstSong: Song =
            testSong(id = "song-first", title = "First", modifiedAt = 1L).copy(
                coverArt = CoverArt.AlbumRiverYear,
            )
        val secondSong: Song =
            testSong(id = "song-second", title = "Second", modifiedAt = 2L).copy(
                coverArt = CoverArt.CoverSeaDream,
                coverImageUri = "file://second.jpg",
            )
        val playlist: LocalPlaylist = testPlaylist(id = "playlist-detail", name = "详情歌单", updatedAt = 20L)
        val repository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists = mutableListOf(playlist),
                playlistDetails =
                    mutableMapOf(
                        playlist.id to
                            LocalPlaylistDetail(
                                playlist = playlist,
                                relations =
                                    listOf(
                                        LocalPlaylistSong(
                                            playlistId = playlist.id,
                                            songId = "missing-song",
                                            addedAt = 1L,
                                            sortOrder = 0,
                                        ),
                                        LocalPlaylistSong(
                                            playlistId = playlist.id,
                                            songId = firstSong.id,
                                            addedAt = 2L,
                                            sortOrder = 1,
                                        ),
                                        LocalPlaylistSong(
                                            playlistId = playlist.id,
                                            songId = secondSong.id,
                                            addedAt = 3L,
                                            sortOrder = 2,
                                        ),
                                    ),
                                availableSongs = listOf(secondSong, firstSong),
                            ),
                    ),
            )
        val controller: MusicAppController = createController(localPlaylistRepository = repository)

        controller.openLocalPlaylistDetail(playlistId = playlist.id)

        val detail: LocalPlaylistDetailDisplayModel = requireNotNull(controller.uiState.selectedLocalPlaylistDetail)
        assertEquals(expected = playlist.name, actual = detail.name)
        assertEquals(expected = 2, actual = detail.availableSongCount)
        assertEquals(expected = listOf(secondSong.id, firstSong.id), actual = detail.songs.map { song: Song -> song.id })
        assertEquals(expected = CoverArt.CoverSeaDream, actual = detail.coverArt)
        assertEquals(expected = "file://second.jpg", actual = detail.coverImageUri)
        assertTrue(actual = detail.canPlayAll)
    }

    /**
     * 添加歌曲后若目标详情页已打开，应立即重读仓库事实刷新详情歌曲列表。
     */
    @Test
    fun addingSongRefreshesOpenLocalPlaylistDetailImmediately() {
        val targetSong: Song = testSong(id = "song-new-detail", title = "New Detail", modifiedAt = 1L)
        val playlist: LocalPlaylist = testPlaylist(id = "playlist-open", name = "打开的歌单", updatedAt = 20L)
        val repository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists = mutableListOf(playlist),
                playlistDetails =
                    mutableMapOf(
                        playlist.id to
                            LocalPlaylistDetail(
                                playlist = playlist,
                                relations = emptyList(),
                                availableSongs = emptyList(),
                            ),
                    ),
            )
        repository.onAddSong = { playlistId: String, songId: String ->
            repository.putPlaylistDetail(
                detail =
                    LocalPlaylistDetail(
                        playlist = playlist.copy(updatedAt = 30L),
                        relations =
                            listOf(
                                LocalPlaylistSong(
                                    playlistId = playlistId,
                                    songId = songId,
                                    addedAt = 30L,
                                    sortOrder = 0,
                                ),
                            ),
                        availableSongs = listOf(targetSong),
                    ),
            )
        }
        val controller: MusicAppController = createController(localPlaylistRepository = repository)
        controller.openLocalPlaylistDetail(playlistId = playlist.id)
        assertTrue(
            actual =
                controller.uiState.selectedLocalPlaylistDetail
                    ?.songs
                    .orEmpty()
                    .isEmpty(),
        )

        controller.openAddToPlaylistFlow(song = targetSong)
        controller.selectAddToPlaylistTarget(playlistId = playlist.id)
        controller.addCurrentSongToSelectedPlaylist()

        assertEquals(expected = playlist.id, actual = controller.uiState.selectedLocalPlaylistDetail?.id)
        assertEquals(
            expected = listOf(targetSong.id),
            actual =
                controller.uiState.selectedLocalPlaylistDetail
                    ?.songs
                    .orEmpty()
                    .map { song: Song -> song.id },
        )
    }

    /**
     * 歌单管理页从列表进入时清空旧选择，并作为覆盖页隐藏迷你播放器 chrome。
     */
    @Test
    fun openLocalPlaylistManagementClearsSelectionAndUsesOverlayChrome() {
        val playlist: LocalPlaylist = testPlaylist(id = "playlist-manage", name = "管理歌单", updatedAt = 20L)
        val controller: MusicAppController =
            createController(
                localPlaylistRepository =
                    RecordingLocalPlaylistRepository(
                        playlists = mutableListOf(playlist),
                    ),
            )
        controller.openLocalPlaylists()
        controller.openLocalPlaylistManagement()

        assertEquals(expected = SecondaryScreen.LocalPlaylistManagement, actual = controller.uiState.navigationState.secondaryScreen)
        assertEquals(expected = emptySet(), actual = controller.uiState.selectedManagedLocalPlaylistIds)
        assertFalse(actual = controller.uiState.canDeleteManagedLocalPlaylists)
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithoutChrome,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
        assertEquals(
            expected = SecondaryScreen.LocalPlaylistManagement,
            actual = controller.uiState.navigationState.chromeOverlayScreen,
        )
    }

    /**
     * 管理页整行点击应支持单选、多选和再次点击取消。
     */
    @Test
    fun toggleManagedLocalPlaylistSelectionSupportsSingleAndMultipleSelection() {
        val firstPlaylist: LocalPlaylist = testPlaylist(id = "playlist-first", name = "第一", updatedAt = 20L)
        val secondPlaylist: LocalPlaylist = testPlaylist(id = "playlist-second", name = "第二", updatedAt = 10L)
        val controller: MusicAppController =
            createController(
                localPlaylistRepository =
                    RecordingLocalPlaylistRepository(
                        playlists = mutableListOf(firstPlaylist, secondPlaylist),
                    ),
            )
        controller.openLocalPlaylistManagement()

        controller.toggleManagedLocalPlaylistSelection(playlistId = firstPlaylist.id)
        controller.toggleManagedLocalPlaylistSelection(playlistId = secondPlaylist.id)
        assertEquals(
            expected = setOf(firstPlaylist.id, secondPlaylist.id),
            actual = controller.uiState.selectedManagedLocalPlaylistIds,
        )
        assertTrue(actual = controller.uiState.canDeleteManagedLocalPlaylists)

        controller.toggleManagedLocalPlaylistSelection(playlistId = firstPlaylist.id)

        assertEquals(expected = setOf(secondPlaylist.id), actual = controller.uiState.selectedManagedLocalPlaylistIds)
    }

    /**
     * 未选择歌单时删除确认不会打开，保证置灰按钮没有副作用。
     */
    @Test
    fun deleteManagedLocalPlaylistsDialogDoesNotOpenWithoutSelection() {
        val controller: MusicAppController = createController()

        controller.openDeleteLocalPlaylistsDialog()

        assertFalse(actual = controller.uiState.isDeleteLocalPlaylistsDialogOpen)
    }

    /**
     * 确认删除后停留管理页、刷新列表、清空选择并展示删除成功提示。
     */
    @Test
    fun confirmDeleteManagedLocalPlaylistsRefreshesManagementPageAndShowsMessage() {
        val keepPlaylist: LocalPlaylist = testPlaylist(id = "playlist-keep", name = "保留", updatedAt = 20L)
        val deletePlaylist: LocalPlaylist = testPlaylist(id = "playlist-delete", name = "删除", updatedAt = 10L)
        val repository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists = mutableListOf(keepPlaylist, deletePlaylist),
            )
        val controller: MusicAppController = createController(localPlaylistRepository = repository)
        controller.openLocalPlaylistManagement()
        controller.toggleManagedLocalPlaylistSelection(playlistId = deletePlaylist.id)

        controller.openDeleteLocalPlaylistsDialog()
        controller.confirmDeleteLocalPlaylists()

        assertEquals(expected = SecondaryScreen.LocalPlaylistManagement, actual = controller.uiState.navigationState.secondaryScreen)
        assertEquals(expected = listOf(keepPlaylist.id), actual = controller.uiState.localPlaylists.map { playlist -> playlist.id })
        assertEquals(expected = emptySet(), actual = controller.uiState.selectedManagedLocalPlaylistIds)
        assertFalse(actual = controller.uiState.isDeleteLocalPlaylistsDialogOpen)
        assertEquals(expected = "已删除 1 个歌单", actual = controller.uiState.transientMessage)
        assertEquals(expected = "已删除", actual = controller.uiState.transientMessageTitle)
        assertEquals(expected = listOf(setOf(deletePlaylist.id)), actual = repository.deletePlaylistCalls)
    }

    /**
     * 删除全部歌单后管理页保留空列表，删除按钮重新置灰。
     */
    @Test
    fun deletingAllManagedLocalPlaylistsLeavesEmptyManagementPage() {
        val playlist: LocalPlaylist = testPlaylist(id = "playlist-only", name = "唯一", updatedAt = 20L)
        val controller: MusicAppController =
            createController(
                localPlaylistRepository =
                    RecordingLocalPlaylistRepository(
                        playlists = mutableListOf(playlist),
                    ),
            )
        controller.openLocalPlaylistManagement()
        controller.toggleManagedLocalPlaylistSelection(playlistId = playlist.id)

        controller.confirmDeleteLocalPlaylists()

        assertTrue(actual = controller.uiState.localPlaylists.isEmpty())
        assertFalse(actual = controller.uiState.canDeleteManagedLocalPlaylists)
        assertEquals(expected = 0, actual = controller.uiState.localPlaylistCount)
    }

    /**
     * 删除歌单容器不能改变当前播放队列和当前歌曲。
     */
    @Test
    fun deletingLocalPlaylistDoesNotChangePlaybackQueue() {
        val song: Song = testSong(id = "song-playing", title = "Playing", modifiedAt = 1L)
        val playlist: LocalPlaylist = testPlaylist(id = "playlist-playing", name = "播放中", updatedAt = 20L)
        val playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
        val controller: MusicAppController =
            createController(
                playbackRepository = playbackRepository,
                localPlaylistRepository =
                    RecordingLocalPlaylistRepository(
                        playlists = mutableListOf(playlist),
                    ),
            )
        controller.playSong(song = song, queueSongs = listOf(song))
        controller.openLocalPlaylistManagement()
        controller.toggleManagedLocalPlaylistSelection(playlistId = playlist.id)

        controller.confirmDeleteLocalPlaylists()

        assertEquals(expected = song.id, actual = controller.uiState.currentSongId)
        assertEquals(expected = listOf(song.id), actual = controller.uiState.queueSongIds)
        assertEquals(expected = listOf(song.id), actual = playbackRepository.getQueueState().songIds)
    }

    /**
     * 扫描页统计必须使用完整曲库总数，不能误用首页预览列表的 6 条限制。
     */
    @Test
    fun audioScanCountUsesLibraryStatsWhenOnlyHomePreviewIsLoaded(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            controller.openAudioScan()
            assertTrue(actual = controller.uiState.localSongs.isEmpty())
            assertEquals(expected = 6, actual = controller.uiState.songs.size)
            assertEquals(expected = 8, actual = controller.uiState.libraryStats.songCount)
            assertEquals(expected = 8, actual = controller.uiState.audioScanPlayableSongCount)
        }

    /**
     * 来源页触发扫描完成后应留在当前路由，方便用户继续检查扫描摘要。
     */
    @Test
    fun scanCompletionKeepsCurrentLocalMusicRoute(): Unit =
        runBlocking {
            val controller = createController()
            controller.openLocalMusic(section = LocalMusicSection.Sources)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            assertEquals(
                expected = SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Sources),
                actual = controller.uiState.navigationState.secondaryScreen,
            )
        }

    /**
     * 首页专辑页签应读取本地音乐专辑分段同源的全量专辑数据，且不能跳出首页。
     */
    @Test
    fun homeAlbumSectionLoadsAllLocalAlbumsWithoutLeavingHome() {
        val repository = SeededMusicLibraryRepository(seedCount = 8)
        val controller = createController(musicLibraryRepository = repository)
        assertTrue(actual = controller.uiState.localAlbums.isEmpty())
        assertEquals(expected = 0, actual = repository.fullLibraryReads)
        controller.setHomeContentSection(section = HomeContentSection.Albums)
        assertEquals(expected = HomeContentSection.Albums, actual = controller.uiState.homeContentSection)
        assertEquals(expected = listOf("album:album"), actual = controller.uiState.localAlbums.map { album: Album -> album.id })
        assertEquals(expected = 1, actual = repository.fullLibraryReads)
        assertTrue(actual = controller.uiState.navigationState.isTopLevel)
    }

    /**
     * 首页歌手页签应读取本地音乐歌手分段同源的全量歌手数据，且不能跳出首页。
     */
    @Test
    fun homeArtistSectionLoadsAllLocalArtistsWithoutLeavingHome() {
        val repository = SeededMusicLibraryRepository(seedCount = 8)
        val controller = createController(musicLibraryRepository = repository)
        assertTrue(actual = controller.uiState.localArtists.isEmpty())
        assertEquals(expected = 0, actual = repository.fullLibraryReads)
        controller.setHomeContentSection(section = HomeContentSection.Artists)
        assertEquals(expected = HomeContentSection.Artists, actual = controller.uiState.homeContentSection)
        assertEquals(expected = listOf("artist:artist"), actual = controller.uiState.localArtists.map { artist: Artist -> artist.id })
        assertEquals(expected = listOf(1), actual = controller.uiState.localArtists.map { artist: Artist -> artist.albumCount })
        assertEquals(expected = 1, actual = repository.fullLibraryReads)
        assertTrue(actual = controller.uiState.navigationState.isTopLevel)
    }

    /**
     * 用户停留在首页专辑页签时，扫描完成必须同步专辑网格，而不是只刷新歌曲预览。
     */
    @Test
    fun scanRefreshesHomeAlbumSectionAlbums(): Unit =
        runBlocking {
            val controller = createController()
            controller.setHomeContentSection(section = HomeContentSection.Albums)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            assertEquals(expected = HomeContentSection.Albums, actual = controller.uiState.homeContentSection)
            assertTrue(actual = controller.uiState.localAlbums.isNotEmpty())
            assertEquals(expected = controller.uiState.libraryStats.albumCount, actual = controller.uiState.localAlbums.size)
        }

    /**
     * 用户停留在首页歌手页签时，扫描完成必须同步歌手列表，而不是只刷新歌曲预览。
     */
    @Test
    fun scanRefreshesHomeArtistSectionArtists(): Unit =
        runBlocking {
            val controller = createController()
            controller.setHomeContentSection(section = HomeContentSection.Artists)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            assertEquals(expected = HomeContentSection.Artists, actual = controller.uiState.homeContentSection)
            assertTrue(actual = controller.uiState.localArtists.isNotEmpty())
            assertEquals(expected = controller.uiState.libraryStats.artistCount, actual = controller.uiState.localArtists.size)
        }

    /**
     * 扫描完成只应填充本地歌曲预览，不应把扫描结果冒充最近播放。
     */
    @Test
    fun scanDoesNotPopulateRecentPlayback(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            assertTrue(controller.uiState.localSongs.isEmpty())
            assertEquals(expected = 6, actual = controller.uiState.homeLocalSongPreview.size)
            assertEquals(expected = 8, actual = controller.uiState.libraryStats.songCount)
            assertTrue(controller.uiState.localMusicSources.isNotEmpty())
            assertTrue(controller.uiState.recentSongs.isEmpty())
        }

    /**
     * partial/positive-only 扫描不能把未证明不可用的既有队列歌曲从 UI 队列里误丢。
     */
    @Test
    fun positiveOnlyScanKeepsExistingPlaybackQueueSongs(): Unit =
        runBlocking {
            val repository = PositiveOnlyRefreshMusicLibraryRepository()
            val controller =
                createController(
                    musicLibraryRepository = repository,
                    localMusicScanner = PositiveOnlyRefreshScanner(),
                )
            val queueSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 2)
            controller.playSong(song = queueSongs.first(), queueSongs = queueSongs)

            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)

            val expectedQueueSongIds: List<String> = queueSongs.map { song: Song -> song.id }
            assertEquals(expected = expectedQueueSongIds, actual = controller.uiState.queueSongIds)
            assertEquals(expected = expectedQueueSongIds, actual = controller.uiState.queueSongsSnapshot.map { song: Song -> song.id })
            assertEquals(expected = expectedQueueSongIds, actual = controller.uiState.queueSongs.map { song: Song -> song.id })
        }

    /**
     * 平台 scanner 返回权限错误时，控制器应进入错误态且不能回填演示歌曲。
     */
    @Test
    fun scanPermissionDeniedKeepsLibraryEmpty(): Unit =
        runBlocking {
            val controller =
                createController(
                    localMusicScanner = PermissionDeniedScanner(),
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val scanState = controller.uiState.scanState
            assertTrue(actual = scanState is LocalMusicScanState.Error)
            assertEquals(
                expected = LocalMusicScanErrorType.PermissionDenied,
                actual = (scanState as LocalMusicScanState.Error).error.type,
            )
            assertTrue(controller.uiState.localSongs.isEmpty())
            assertTrue(controller.uiState.homeLocalSongPreview.isEmpty())
        }

    /**
     * 权限永久拒绝时应保留明确错误类型，供 Android 入口转系统设置。
     */
    @Test
    fun scanPermissionPermanentlyDeniedKeepsLibraryEmpty(): Unit =
        runBlocking {
            val controller =
                createController(
                    localMusicScanner = PermissionPermanentlyDeniedScanner(),
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val scanState = controller.uiState.scanState
            assertTrue(actual = scanState is LocalMusicScanState.Error)
            assertEquals(
                expected = LocalMusicScanErrorType.PermissionPermanentlyDenied,
                actual = (scanState as LocalMusicScanState.Error).error.type,
            )
            assertTrue(controller.uiState.localSongs.isEmpty())
            assertTrue(controller.uiState.homeLocalSongPreview.isEmpty())
        }

    /**
     * 权限永久拒绝后再次点击扫描，应先显示确认框，确认后才打开系统设置。
     */
    @Test
    fun scanPermissionPermanentlyDeniedRequiresUserConfirmationBeforeSettings(): Unit =
        runBlocking {
            val scanner = CountingPermissionPermanentlyDeniedScanner()
            val opener = RecordingPermissionSettingsOpener()
            val controller =
                createController(
                    localMusicScanner = scanner,
                    permissionSettingsOpener = opener,
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            assertEquals(expected = 1, actual = scanner.scanCount)
            assertFalse(controller.uiState.isPermissionSettingsDialogOpen)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            assertEquals(expected = 1, actual = scanner.scanCount)
            assertTrue(controller.uiState.isPermissionSettingsDialogOpen)
            assertEquals(expected = 0, actual = opener.openCount)
            controller.confirmPermissionSettings()
            assertFalse(controller.uiState.isPermissionSettingsDialogOpen)
            assertEquals(expected = 1, actual = opener.openCount)
            assertEquals(expected = LocalMusicScanState.WaitingForPermission, actual = controller.uiState.scanState)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            assertEquals(expected = 2, actual = scanner.scanCount)
        }

    /**
     * 用户取消扫描必须进入独立结果态，而不是复用成功完成或失败错误。
     */
    @Test
    fun cancelledScanStateIsDistinctFromDoneAndError(): Unit =
        runBlocking {
            val controller: MusicAppController =
                createController(
                    localMusicScanner = UserCancelledScanner(),
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val scanState: LocalMusicScanState = controller.uiState.scanState
            assertTrue(actual = scanState is LocalMusicScanState.Cancelled)
            assertFalse(actual = isDoneScanState(scanState = scanState))
            assertFalse(actual = isErrorScanState(scanState = scanState))
        }

    /**
     * 取消结果需要能被 UI 映射为“已取消”，避免和普通失败文案混在一起。
     */
    @Test
    fun cancelledScanStateMapsToCancelledCopy(): Unit =
        runBlocking {
            val controller: MusicAppController =
                createController(
                    localMusicScanner = UserCancelledScanner(),
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val cancelledState: LocalMusicScanState.Cancelled = controller.uiState.scanState as LocalMusicScanState.Cancelled
            assertEquals(
                expected = "已取消",
                actual = cancelledScanResultTitle(scanState = cancelledState),
            )
            assertTrue(
                actual = cancelledScanResultDetail(scanState = cancelledState).contains(other = "当前曲库已保留"),
            )
        }

    /**
     * 取消扫描也要记录结果时间，便于扫描页展示最近一次明确操作结果。
     */
    @Test
    fun cancelledScanStateKeepsResultTime(): Unit =
        runBlocking {
            val controller: MusicAppController =
                createController(
                    localMusicScanner = UserCancelledScanner(),
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val cancelledState: LocalMusicScanState.Cancelled = controller.uiState.scanState as LocalMusicScanState.Cancelled
            assertTrue(
                actual = cancelledState.summary.completedAt > 0L,
            )
        }

    /**
     * 扫描中再次触发入口应走取消意图，不能启动第二个并发扫描任务。
     */
    @Test
    fun scanEntryDuringRunningScanDoesNotStartSecondScan(): Unit =
        runTest {
            val scanner: BlockingLocalMusicScanner = BlockingLocalMusicScanner()
            val controller: MusicAppController =
                createController(
                    localMusicScanner = scanner,
                    controllerScope = backgroundScope,
                )
            val scanJob: Job =
                launch {
                    controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
                }
            scanner.awaitFirstScanStarted()
            assertEquals(
                expected = "取消扫描",
                actual = renderCancelEntryLabelOrNull(scanState = controller.uiState.scanState),
            )
            val secondScanJob: Job =
                launch {
                    controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
                }
            runCurrent()
            assertEquals(expected = 1, actual = scanner.scanCount)
            assertTrue(actual = controller.uiState.scanState is LocalMusicScanState.Cancelled)
            scanner.complete()
            scanJob.join()
            secondScanJob.join()
        }

    /**
     * 扫描承载协程被系统主题切换等外部重组取消时，UI 不能继续停留在“取消扫描”。
     */
    @Test
    fun scanStateSettlesWhenRunningScanCoroutineIsCancelledExternally(): Unit =
        runTest {
            val scanner: BlockingLocalMusicScanner = BlockingLocalMusicScanner()
            val controller: MusicAppController =
                createController(
                    localMusicScanner = scanner,
                    controllerScope = backgroundScope,
                )
            val scanJob: Job =
                launch {
                    controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
                }
            scanner.awaitFirstScanStarted()
            scanJob.cancel(
                cause = CancellationException("系统主题切换取消了当前组合协程"),
            )
            scanJob.join()
            assertFalse(actual = controller.uiState.scanState is LocalMusicScanState.Scanning)
            assertNull(actual = renderCancelEntryLabelOrNull(scanState = controller.uiState.scanState))
        }

    /**
     * 用户取消扫描后，旧扫描结果晚到不能覆盖取消态或队列状态。
     */
    @Test
    fun lateScanResultAfterCancellationDoesNotOverwriteCancelledStateOrQueue(): Unit =
        runTest {
            val scanner = LateSuccessAfterCancellationScanner()
            val controller =
                createController(
                    localMusicScanner = scanner,
                    controllerScope = backgroundScope,
                )
            val queueSongs: List<Song> =
                listOf(
                    testSong(
                        id = "queue:1",
                        title = "Queue One",
                        modifiedAt = 1L,
                    ),
                    testSong(
                        id = "queue:2",
                        title = "Queue Two",
                        modifiedAt = 2L,
                    ),
                )
            controller.playSong(song = queueSongs[0], queueSongs = queueSongs)
            advanceUntilIdle()

            controller.requestLocalMusicScan(request = LocalMusicScanRequest.Refresh)
            scanner.awaitStarted()
            controller.requestLocalMusicScan(request = LocalMusicScanRequest.Refresh)
            scanner.releaseLateResult()
            advanceUntilIdle()

            assertTrue(actual = controller.uiState.scanState is LocalMusicScanState.Cancelled)
            assertEquals(expected = 0, actual = controller.uiState.libraryStats.songCount)
            assertEquals(
                expected = queueSongs.map { song: Song -> song.id },
                actual = controller.uiState.queueSongIds,
            )
        }

    /**
     * 用户看到取消态后立即再次触发扫描时，即使旧 scanner 还没退出也必须启动新会话。
     */
    @Test
    fun scanCanRestartImmediatelyAfterCancellationWhileOldScannerIsStillFinishing(): Unit =
        runTest {
            val scanner: RestartableLateSuccessScanner = RestartableLateSuccessScanner()
            val controller =
                createController(
                    localMusicScanner = scanner,
                    controllerScope = backgroundScope,
                )

            controller.requestLocalMusicScan(request = LocalMusicScanRequest.Refresh)
            scanner.awaitFirstStarted()

            controller.requestLocalMusicScan(request = LocalMusicScanRequest.Refresh)
            runCurrent()
            assertTrue(actual = controller.uiState.scanState is LocalMusicScanState.Cancelled)

            controller.requestLocalMusicScan(request = LocalMusicScanRequest.Refresh)
            runCurrent()
            scanner.awaitSecondStarted()

            assertEquals(expected = 2, actual = scanner.scanCount)
            assertTrue(actual = controller.uiState.scanState is LocalMusicScanState.Scanning)

            scanner.releaseFirstLateResult()
            runCurrent()

            assertTrue(actual = controller.uiState.scanState is LocalMusicScanState.Scanning)
            assertEquals(expected = 0, actual = controller.uiState.libraryStats.songCount)

            scanner.releaseSecondResult()
            advanceUntilIdle()
        }

    /**
     * 重新扫描期间应保留上一轮结果摘要，扫描页统计不能回退成未记录时间。
     */
    @Test
    fun runningScanKeepsPreviousLastScanSummary(): Unit =
        runTest {
            val scanner: BlockingAfterFirstScanScanner = BlockingAfterFirstScanScanner()
            val controller: MusicAppController =
                createController(
                    localMusicScanner = scanner,
                    controllerScope = backgroundScope,
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val previousSummary: LocalMusicLastScanSummary = (controller.uiState.scanState as LocalMusicScanState.Done).summary

            val scanJob: Job =
                launch {
                    controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
                }
            scanner.awaitSecondScanStarted()

            val scanningState: LocalMusicScanState.Scanning = controller.uiState.scanState as LocalMusicScanState.Scanning
            assertEquals(expected = previousSummary, actual = scanningState.previousSummary)
            scanner.completeSecondScan()
            scanJob.join()
        }

    /**
     * 本地音频发现偏好应写入仓库缓存，并随扫描请求传给平台 scanner。
     */
    @Test
    fun localMusicDiscoveryPreferencesPersistAndFlowIntoScanner(): Unit =
        runBlocking {
            val preferencesRepository: InMemoryUserPreferencesRepository = InMemoryUserPreferencesRepository()
            val scanner: PreferencesRecordingLocalMusicScanner = PreferencesRecordingLocalMusicScanner()
            val controller: MusicAppController =
                createController(
                    localMusicScanner = scanner,
                    userPreferencesRepository = preferencesRepository,
                )

            controller.setLocalMusicAutoScanOnLaunchEnabled(isEnabled = true)
            controller.setLocalMusicShortAudioIgnored(isIgnored = false)
            controller.setLocalMusicSystemFoldersExcluded(isExcluded = false)
            val restoredController: MusicAppController =
                createController(
                    localMusicScanner = scanner,
                    userPreferencesRepository = preferencesRepository,
                )
            restoredController.scanLocalMusic(request = LocalMusicScanRequest.Refresh)

            val expectedPreferences =
                LocalMusicDiscoveryPreferences(
                    isAutoScanOnLaunchEnabled = true,
                    shouldIgnoreShortAudio = false,
                    shouldExcludeSystemFolders = false,
                )
            assertEquals(expected = expectedPreferences, actual = restoredController.uiState.localMusicDiscoveryPreferences)
            assertEquals(expected = expectedPreferences, actual = scanner.preferences.last())
        }

    /**
     * 权限设置确认框应由系统返回键关闭，避免误触后只能点击按钮退出。
     */
    @Test
    fun systemBackClosesPermissionSettingsDialog() {
        val controller = createController()
        controller.openPermissionSettingsDialog()
        assertTrue(controller.uiState.canHandleSystemBack)
        assertTrue(controller.handleSystemBack())
        assertFalse(controller.uiState.isPermissionSettingsDialogOpen)
    }

    /**
     * 非歌单详情的更多面板应能进入添加到歌单流程，并关闭来源更多面板。
     */
    @Test
    fun openAddToPlaylistFlowClosesMorePanelAndKeepsTargetSong() {
        val controller: MusicAppController = createController()
        val targetSong: Song = testSong(id = "song-more-1", title = "Song More 1", modifiedAt = 1L)

        controller.openMore(song = targetSong)
        controller.openAddToPlaylistFlow(song = targetSong)

        assertNull(actual = controller.uiState.moreSongId)
        assertEquals(expected = targetSong.id, actual = controller.uiState.addToPlaylistFlow?.songId)
        assertFalse(actual = controller.uiState.addToPlaylistFlow?.isCreateDialogOpen ?: true)
    }

    /**
     * 更多面板中的收藏是一次性操作，成功切换后应自动关闭面板。
     */
    @Test
    fun toggleFavoriteFromMorePanelClosesMorePanel() {
        val controller: MusicAppController = createController()
        val targetSong: Song = testSong(id = "song-more-favorite", title = "Song More Favorite", modifiedAt = 1L)

        controller.openMore(song = targetSong)
        controller.toggleFavoriteFromMorePanel(songId = targetSong.id)

        assertNull(actual = controller.uiState.moreSongId)
        assertTrue(actual = controller.uiState.likedSongIds.contains(element = targetSong.id))
    }

    /**
     * 更多面板来源需要区分歌单详情页，避免当前切片误扩大成歌单内继续添加。
     */
    @Test
    fun localPlaylistDetailMorePanelDoesNotOpenAddToPlaylistFlow() {
        val controller: MusicAppController = createController()
        val targetSong: Song = testSong(id = "playlist-detail-song", title = "Playlist Detail Song", modifiedAt = 1L)

        controller.openMore(
            song = targetSong,
            sourceContext = SongMoreSourceContext.LocalPlaylistDetail,
        )
        controller.openAddToPlaylistFlow(song = targetSong)

        assertEquals(expected = targetSong.id, actual = controller.uiState.moreSongId)
        assertNull(actual = controller.uiState.addToPlaylistFlow)
    }

    /**
     * 新建歌单弹窗打开时必须使用仓库生成的可用默认名。
     */
    @Test
    fun createPlaylistDialogUsesNextDefaultPlaylistName() {
        val localPlaylistRepository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                defaultNames = listOf("默认歌单 1", "默认歌单 2"),
            )
        val controller: MusicAppController =
            createController(
                localPlaylistRepository = localPlaylistRepository,
            )
        val targetSong: Song = testSong(id = "default-name-song", title = "Default Name Song", modifiedAt = 1L)

        controller.openAddToPlaylistFlow(song = targetSong)
        controller.openCreatePlaylistDialog()

        assertTrue(actual = controller.uiState.addToPlaylistFlow?.isCreateDialogOpen ?: false)
        assertEquals(expected = "默认歌单 1", actual = controller.uiState.addToPlaylistFlow?.newPlaylistName)
    }

    /**
     * 名称校验失败时弹窗不能关闭，并映射为用户可见错误。
     */
    @Test
    fun createPlaylistValidationErrorsKeepDialogOpen() {
        val localPlaylistRepository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                defaultNames = listOf("默认歌单 1"),
                createResults =
                    mutableListOf(
                        CreateLocalPlaylistWithSongResult.BlankName,
                        CreateLocalPlaylistWithSongResult.DuplicateName,
                    ),
            )
        val controller: MusicAppController =
            createController(
                localPlaylistRepository = localPlaylistRepository,
            )
        val targetSong: Song = testSong(id = "validation-song", title = "Validation Song", modifiedAt = 1L)

        controller.openAddToPlaylistFlow(song = targetSong)
        controller.openCreatePlaylistDialog()
        controller.setNewPlaylistName(name = "   ")
        controller.createPlaylistWithCurrentSong()

        assertTrue(actual = controller.uiState.addToPlaylistFlow?.isCreateDialogOpen ?: false)
        assertEquals(expected = "歌单名称不能为空", actual = controller.uiState.addToPlaylistFlow?.newPlaylistNameError)

        controller.setNewPlaylistName(name = "默认歌单 1")
        controller.createPlaylistWithCurrentSong()

        assertTrue(actual = controller.uiState.addToPlaylistFlow?.isCreateDialogOpen ?: false)
        assertEquals(expected = "歌单名称已存在", actual = controller.uiState.addToPlaylistFlow?.newPlaylistNameError)
    }

    /**
     * 新建成功后必须自动加入当前歌曲、关闭两层弹窗，并保留最终歌单名的大小写和中间空格。
     */
    @Test
    fun createPlaylistWithCurrentSongClosesFlowAndShowsSuccessMessage() {
        val playlist: LocalPlaylist =
            LocalPlaylist(
                id = "playlist-1",
                name = "Road  Trip",
                createdAt = 10L,
                updatedAt = 10L,
            )
        val localPlaylistRepository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                defaultNames = listOf("默认歌单 1"),
                createResults =
                    mutableListOf(
                        CreateLocalPlaylistWithSongResult.Success(
                            playlist = playlist,
                            relation =
                                LocalPlaylistSong(
                                    playlistId = playlist.id,
                                    songId = "success-song",
                                    addedAt = 10L,
                                    sortOrder = 0,
                                ),
                        ),
                    ),
            )
        val controller: MusicAppController =
            createController(
                localPlaylistRepository = localPlaylistRepository,
            )
        val targetSong: Song = testSong(id = "success-song", title = "Success Song", modifiedAt = 1L)

        controller.openAddToPlaylistFlow(song = targetSong)
        controller.openCreatePlaylistDialog()
        controller.setNewPlaylistName(name = " Road  Trip ")
        controller.createPlaylistWithCurrentSong()

        assertNull(actual = controller.uiState.addToPlaylistFlow)
        assertEquals(expected = listOf(" Road  Trip " to targetSong.id), actual = localPlaylistRepository.createWithSongCalls)
        assertEquals(expected = "添加到 Road  Trip 歌单成功", actual = controller.uiState.transientMessage)
    }

    /**
     * 打开添加到歌单弹窗时应展示全部已有歌单，但不默认选中任何目标。
     */
    @Test
    fun addToPlaylistFlowStartsWithAllPlaylistsAndNoSelection() {
        val localPlaylistRepository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists =
                    mutableListOf(
                        testPlaylist(id = "playlist-1", name = "通勤"),
                        testPlaylist(id = "playlist-2", name = "夜跑"),
                    ),
            )
        val controller: MusicAppController =
            createController(
                localPlaylistRepository = localPlaylistRepository,
            )
        val targetSong: Song = testSong(id = "select-song", title = "Select Song", modifiedAt = 1L)

        controller.openAddToPlaylistFlow(song = targetSong)

        val flow: AddToPlaylistFlowState = requireNotNull(controller.uiState.addToPlaylistFlow)
        assertEquals(
            expected = setOf("通勤", "夜跑"),
            actual =
                flow.availablePlaylists
                    .map { playlist: LocalPlaylistCardDisplayModel -> playlist.name }
                    .toSet(),
        )
        assertNull(actual = flow.selectedPlaylistId)
        assertFalse(actual = flow.canCompleteExistingPlaylist)
    }

    /**
     * 添加到歌单弹窗应复用歌单展示封面，避免已有歌单列表退回固定占位图。
     */
    @Test
    fun addToPlaylistFlowUsesPlaylistDisplayCover() {
        val songWithoutCover: Song = testSong(id = "song-without-cover", title = "Without Cover", modifiedAt = 1L)
        val songWithCover: Song =
            testSong(id = "song-with-cover", title = "With Cover", modifiedAt = 2L).copy(
                coverArt = CoverArt.AlbumTimeForest,
                coverImageUri = "file://playlist-dialog-cover.jpg",
            )
        val playlist: LocalPlaylist = testPlaylist(id = "playlist-dialog", name = "弹窗歌单", updatedAt = 20L)
        val localPlaylistRepository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists = mutableListOf(playlist),
                playlistDetails =
                    mutableMapOf(
                        playlist.id to
                            LocalPlaylistDetail(
                                playlist = playlist,
                                relations =
                                    listOf(
                                        LocalPlaylistSong(
                                            playlistId = playlist.id,
                                            songId = songWithoutCover.id,
                                            addedAt = 1L,
                                            sortOrder = 0,
                                        ),
                                        LocalPlaylistSong(
                                            playlistId = playlist.id,
                                            songId = songWithCover.id,
                                            addedAt = 2L,
                                            sortOrder = 1,
                                        ),
                                    ),
                                availableSongs = listOf(songWithoutCover, songWithCover),
                            ),
                    ),
            )
        val controller: MusicAppController = createController(localPlaylistRepository = localPlaylistRepository)
        val targetSong: Song = testSong(id = "dialog-target-song", title = "Dialog Target", modifiedAt = 3L)

        controller.openAddToPlaylistFlow(song = targetSong)

        val playlistDisplay: LocalPlaylistCardDisplayModel =
            requireNotNull(controller.uiState.addToPlaylistFlow).availablePlaylists.single()
        assertEquals(expected = CoverArt.AlbumTimeForest, actual = playlistDisplay.coverArt)
        assertEquals(expected = "file://playlist-dialog-cover.jpg", actual = playlistDisplay.coverImageUri)
    }

    /**
     * 已有歌单一次只允许选择一个，新版添加弹窗移除搜索后不会再因筛选清空目标。
     */
    @Test
    fun addToPlaylistSelectionIsSingleAndKeepsFullPlaylistList() {
        val localPlaylistRepository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists =
                    mutableListOf(
                        testPlaylist(id = "playlist-a", name = "A List"),
                        testPlaylist(id = "playlist-b", name = "B List"),
                    ),
            )
        val controller: MusicAppController =
            createController(
                localPlaylistRepository = localPlaylistRepository,
            )
        val targetSong: Song = testSong(id = "single-select-song", title = "Single Select Song", modifiedAt = 1L)

        controller.openAddToPlaylistFlow(song = targetSong)
        controller.selectAddToPlaylistTarget(playlistId = "playlist-a")
        assertEquals(expected = "playlist-a", actual = controller.uiState.addToPlaylistFlow?.selectedPlaylistId)
        assertTrue(actual = controller.uiState.addToPlaylistFlow?.canCompleteExistingPlaylist ?: false)

        controller.selectAddToPlaylistTarget(playlistId = "playlist-b")
        assertEquals(expected = "playlist-b", actual = controller.uiState.addToPlaylistFlow?.selectedPlaylistId)
        assertEquals(
            expected = listOf("A List", "B List"),
            actual =
                controller.uiState.addToPlaylistFlow?.availablePlaylists?.map { playlist: LocalPlaylistCardDisplayModel ->
                    playlist.name
                },
        )
    }

    /**
     * 选择已有歌单后点击完成应保存当前歌曲、关闭流程，并显示目标歌单成功提示。
     */
    @Test
    fun addCurrentSongToSelectedPlaylistClosesFlowAndShowsSuccessMessage() {
        val playlist: LocalPlaylist = testPlaylist(id = "playlist-road", name = "Road  Trip")
        val localPlaylistRepository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists = mutableListOf(playlist),
                addSongResults =
                    mutableListOf(
                        AddSongToLocalPlaylistResult.Added(
                            relation =
                                LocalPlaylistSong(
                                    playlistId = playlist.id,
                                    songId = "existing-add-song",
                                    addedAt = 10L,
                                    sortOrder = 0,
                                ),
                        ),
                    ),
            )
        val controller: MusicAppController =
            createController(
                localPlaylistRepository = localPlaylistRepository,
            )
        val targetSong: Song = testSong(id = "existing-add-song", title = "Existing Add Song", modifiedAt = 1L)

        controller.openAddToPlaylistFlow(song = targetSong)
        controller.selectAddToPlaylistTarget(playlistId = playlist.id)
        controller.addCurrentSongToSelectedPlaylist()

        assertNull(actual = controller.uiState.addToPlaylistFlow)
        assertEquals(expected = listOf(playlist.id to targetSong.id), actual = localPlaylistRepository.addSongCalls)
        assertEquals(expected = "添加到 Road  Trip 歌单成功", actual = controller.uiState.transientMessage)
    }

    /**
     * 重复添加已有关系仍按成功结束，且控制器不能额外触发第二次保存。
     */
    @Test
    fun addCurrentSongToSelectedPlaylistTreatsDuplicateAsSuccess() {
        val playlist: LocalPlaylist = testPlaylist(id = "playlist-night", name = "夜跑")
        val localPlaylistRepository: RecordingLocalPlaylistRepository =
            RecordingLocalPlaylistRepository(
                playlists = mutableListOf(playlist),
                addSongResults =
                    mutableListOf(
                        AddSongToLocalPlaylistResult.AlreadyExists(
                            relation =
                                LocalPlaylistSong(
                                    playlistId = playlist.id,
                                    songId = "duplicate-song",
                                    addedAt = 10L,
                                    sortOrder = 0,
                                ),
                        ),
                    ),
            )
        val controller: MusicAppController =
            createController(
                localPlaylistRepository = localPlaylistRepository,
            )
        val targetSong: Song = testSong(id = "duplicate-song", title = "Duplicate Song", modifiedAt = 1L)

        controller.openAddToPlaylistFlow(song = targetSong)
        controller.selectAddToPlaylistTarget(playlistId = playlist.id)
        controller.addCurrentSongToSelectedPlaylist()

        assertNull(actual = controller.uiState.addToPlaylistFlow)
        assertEquals(expected = listOf(playlist.id to targetSong.id), actual = localPlaylistRepository.addSongCalls)
        assertEquals(expected = "添加到 夜跑 歌单成功", actual = controller.uiState.transientMessage)
    }

    /**
     * 控制器必须消费注入的 scanner，避免 Android 入口无意落回 common fake 数据。
     */
    @Test
    fun scanUsesInjectedScannerData(): Unit =
        runBlocking {
            val controller =
                createController(
                    localMusicScanner = SingleAndroidSongScanner(),
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            assertEquals(expected = listOf("设备里的歌"), actual = controller.uiState.homeLocalSongPreview.map { song -> song.title })
            assertTrue(controller.uiState.homeLocalSongPreview.all { song -> song.localUri.startsWith(prefix = "content://") })
            assertTrue(controller.uiState.homeLocalSongPreview.none { song -> song.sourceKind == LocalMusicSourceKind.FakeScanner })
        }

    /**
     * 用户真正播放歌曲后，最近播放才出现该歌曲。
     */
    @Test
    fun playSongAddsRecentPlayback(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val targetSong: Song = controller.uiState.homeLocalSongPreview.first()
            controller.playSong(song = targetSong)
            assertEquals(
                expected = listOf(targetSong.id),
                actual = controller.uiState.recentSongs.map { song -> song.id },
            )
        }

    /**
     * 播放不在首页预览中的歌曲时，最近播放也必须通过当前队列反查到歌曲实体。
     */
    @Test
    fun playSongOutsideHomePreviewAddsRecentPlayback(): Unit =
        runBlocking {
            val repository = SeededMusicLibraryRepository(seedCount = 8)
            val controller = createController(musicLibraryRepository = repository)
            val targetSong: Song = repository.getAllAvailableSongs().first { song -> song.id == "seed:1" }
            assertFalse(controller.uiState.homeLocalSongPreview.any { song -> song.id == targetSong.id })
            assertTrue(controller.uiState.localSongs.isEmpty())
            controller.playSong(song = targetSong)
            assertEquals(
                expected = listOf(targetSong.id),
                actual = controller.uiState.recentSongs.map { song -> song.id },
            )
        }

    /**
     * 最近播放应按历史保留全部可解析歌曲，具体页面再决定展示数量。
     */
    @Test
    fun recentPlaybackKeepsAllPlayedSongs(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val playedSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 3)
            playedSongs.forEach { song: Song ->
                controller.playSong(song = song)
            }
            assertEquals(
                expected = playedSongs.reversed().map { song -> song.id },
                actual = controller.uiState.recentSongs.map { song -> song.id },
            )
        }

    /**
     * 我的页摘要只露出 Top3，但点击摘要歌曲时必须沿用完整最近播放队列并从被点歌曲开始。
     */
    @Test
    fun playRecentSummarySongUsesFullRecentQueueWithClickedStart(): Unit =
        runTest {
            val playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
            val controller: MusicAppController =
                createController(
                    playbackRepository = playbackRepository,
                    controllerScope = backgroundScope,
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val recentSongIds: List<String> =
                controller.uiState.homeLocalSongPreview
                    .take(n = 5)
                    .map { song: Song -> song.id }
            playbackRepository.savePlaybackHistory(
                history = PlaybackHistory(songIds = listOf("missing-song") + recentSongIds),
            )
            controller.loadLocalMusicLibrary()
            assertEquals(
                expected = recentSongIds,
                actual = controller.uiState.recentSongs.map { song: Song -> song.id },
            )
            val clickedSong: Song = controller.uiState.recentSongs[2]

            controller.playRecentSong(song = clickedSong)

            assertEquals(expected = recentSongIds, actual = controller.uiState.queueSongIds)
            assertEquals(expected = clickedSong.id, actual = controller.uiState.currentSongId)
            assertEquals(expected = 2, actual = playbackRepository.getQueueState().currentIndex)
        }

    /**
     * 最近播放页点击任意歌曲时，也应复用完整过滤后的最近播放队列并以被点歌曲作为起点。
     */
    @Test
    fun playRecentPageSongUsesFullRecentQueueWithClickedStart(): Unit =
        runTest {
            val playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
            val controller: MusicAppController =
                createController(
                    playbackRepository = playbackRepository,
                    controllerScope = backgroundScope,
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val recentSongIds: List<String> =
                controller.uiState.homeLocalSongPreview
                    .take(n = 5)
                    .map { song: Song -> song.id }
            playbackRepository.savePlaybackHistory(
                history = PlaybackHistory(songIds = recentSongIds + "removed-song"),
            )
            controller.loadLocalMusicLibrary()
            controller.openRecentPlayed()
            assertEquals(expected = SecondaryScreen.RecentPlayed, actual = controller.uiState.navigationState.secondaryScreen)
            assertEquals(
                expected = recentSongIds,
                actual = controller.uiState.recentSongs.map { song: Song -> song.id },
            )
            val clickedSong: Song = controller.uiState.recentSongs[3]

            controller.playRecentSong(song = clickedSong)

            assertEquals(expected = recentSongIds, actual = controller.uiState.queueSongIds)
            assertEquals(expected = clickedSong.id, actual = controller.uiState.currentSongId)
            assertEquals(expected = 3, actual = playbackRepository.getQueueState().currentIndex)
        }

    /**
     * 歌单详情播放全部应按最新添加顺序建立整个歌单队列，并从第一首开始播放。
     */
    @Test
    fun playLocalPlaylistAllUsesAvailableSongsInLatestAddedOrder(): Unit =
        runTest {
            val playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
            val songs: List<Song> =
                listOf(
                    testSong(id = "playlist-song-1", title = "Playlist One", modifiedAt = 1L),
                    testSong(id = "playlist-song-2", title = "Playlist Two", modifiedAt = 2L),
                    testSong(id = "playlist-song-3", title = "Playlist Three", modifiedAt = 3L),
                )
            val playlist: LocalPlaylist = testPlaylist(id = "playlist-queue", name = "队列歌单", updatedAt = 20L)
            val controller: MusicAppController =
                createController(
                    playbackRepository = playbackRepository,
                    localPlaylistRepository =
                        RecordingLocalPlaylistRepository(
                            playlists = mutableListOf(playlist),
                            playlistDetails =
                                mutableMapOf(
                                    playlist.id to
                                        LocalPlaylistDetail(
                                            playlist = playlist,
                                            relations = emptyList(),
                                            availableSongs = songs,
                                        ),
                                ),
                        ),
                    controllerScope = backgroundScope,
                )
            controller.openLocalPlaylistDetail(playlistId = playlist.id)

            controller.playSelectedLocalPlaylistAll()

            assertEquals(expected = songs.map { song: Song -> song.id }, actual = controller.uiState.queueSongIds)
            assertEquals(expected = songs.first().id, actual = controller.uiState.currentSongId)
            assertEquals(expected = 0, actual = playbackRepository.getQueueState().currentIndex)
        }

    /**
     * 歌单详情点击任意歌曲时，应保留整张歌单队列并从被点歌曲开始。
     */
    @Test
    fun playLocalPlaylistSongUsesWholePlaylistQueueWithClickedStart(): Unit =
        runTest {
            val playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
            val songs: List<Song> =
                listOf(
                    testSong(id = "playlist-click-1", title = "Click One", modifiedAt = 1L),
                    testSong(id = "playlist-click-2", title = "Click Two", modifiedAt = 2L),
                    testSong(id = "playlist-click-3", title = "Click Three", modifiedAt = 3L),
                )
            val playlist: LocalPlaylist = testPlaylist(id = "playlist-click", name = "点击歌单", updatedAt = 20L)
            val controller: MusicAppController =
                createController(
                    playbackRepository = playbackRepository,
                    localPlaylistRepository =
                        RecordingLocalPlaylistRepository(
                            playlists = mutableListOf(playlist),
                            playlistDetails =
                                mutableMapOf(
                                    playlist.id to
                                        LocalPlaylistDetail(
                                            playlist = playlist,
                                            relations = emptyList(),
                                            availableSongs = songs,
                                        ),
                                ),
                        ),
                    controllerScope = backgroundScope,
                )
            controller.openLocalPlaylistDetail(playlistId = playlist.id)

            controller.playSelectedLocalPlaylistSong(song = songs[2])

            assertEquals(expected = songs.map { song: Song -> song.id }, actual = controller.uiState.queueSongIds)
            assertEquals(expected = songs[2].id, actual = controller.uiState.currentSongId)
            assertEquals(expected = 2, actual = playbackRepository.getQueueState().currentIndex)
        }

    /**
     * 桌面最近播放摘要和完整页共用 [MusicAppController.playRecentSong]，
     * 因此点击 Top3 内歌曲也必须读取完整最近播放队列。
     */
    @Test
    fun playDesktopRecentSongUsesFullRecentQueueWithClickedStart(): Unit =
        runTest {
            val playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
            val controller: MusicAppController =
                createController(
                    playbackRepository = playbackRepository,
                    controllerScope = backgroundScope,
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val recentSongIds: List<String> =
                controller.uiState.homeLocalSongPreview
                    .take(n = 5)
                    .map { song: Song -> song.id }
            playbackRepository.savePlaybackHistory(
                history =
                    PlaybackHistory(
                        songIds = listOf("removed-desktop-song") + recentSongIds + "stale-desktop-song",
                    ),
            )
            controller.loadLocalMusicLibrary()
            val clickedSummarySong: Song = controller.uiState.recentSongs[1]

            controller.playRecentSong(song = clickedSummarySong)

            assertEquals(expected = recentSongIds, actual = controller.uiState.queueSongIds)
            assertEquals(expected = clickedSummarySong.id, actual = controller.uiState.currentSongId)
            assertEquals(expected = 1, actual = playbackRepository.getQueueState().currentIndex)
        }

    /**
     * 清空最近播放必须同时清空 UI 状态和底层播放历史，避免刷新后旧记录又回来。
     */
    @Test
    fun clearRecentPlaybackHistoryRemovesVisibleAndStoredHistory(): Unit =
        runBlocking {
            val playbackRepository = InMemoryPlaybackRepository()
            val controller = createController(playbackRepository = playbackRepository)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val playedSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 2)
            playedSongs.forEach { song: Song ->
                controller.playSong(song = song)
            }
            controller.clearRecentPlaybackHistory()
            assertTrue(controller.uiState.recentSongs.isEmpty())
            assertTrue(playbackRepository.getPlaybackHistory().songIds.isEmpty())
        }

    /**
     * 桌面 rail 返回一级页面时必须同步清空二级页面，避免导航高亮与内容错位。
     */
    @Test
    fun desktopRailRootNavigationClearsSecondaryScreen() {
        val controller = createController()
        controller.navigateToSecondary(screen = SecondaryScreen.Search(context = SearchContext.LocalLibrary))
        assertFalse(controller.uiState.navigationState.isTopLevel)

        controller.navigateToRoot(tab = RootTab.Favorites)

        assertTrue(controller.uiState.navigationState.isTopLevel)
        assertEquals(expected = RootTab.Favorites, actual = controller.uiState.navigationState.rootTab)
        assertNull(controller.uiState.navigationState.secondaryScreen)
    }

    /**
     * 桌面底部播放器与播放详情页必须读取同一份播放状态，避免两个入口各自维护开关。
     */
    @Test
    fun playerScreenAndBottomPlayerReadSamePlaybackState(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val targetSong: Song = controller.uiState.homeLocalSongPreview.first()

            controller.playSong(song = targetSong)
            controller.openPlayer()

            assertEquals(expected = SecondaryScreen.Player, actual = controller.uiState.navigationState.secondaryScreen)
            assertEquals(expected = targetSong.id, actual = controller.uiState.currentSongId)
            assertTrue(controller.uiState.shouldShowPauseControl)

            controller.togglePlayback()

            assertEquals(expected = targetSong.id, actual = controller.uiState.currentSongId)
            assertFalse(controller.uiState.isPlaying)
        }

    /**
     * 桌面底部播放器和播放详情页必须读取同一份音量状态，避免页面切换后控件值分叉。
     */
    @Test
    fun playerScreenAndBottomPlayerReadSamePlaybackVolume() {
        val audioPlayerEngine = FakeAudioPlayerEngine()
        val controller = createController(audioPlayerEngine = audioPlayerEngine)

        controller.setVolume(volume = 0.24f)
        controller.openPlayer()

        assertEquals(expected = 0.24f, actual = controller.uiState.playbackVolume)
        assertEquals(expected = 0.24f, actual = audioPlayerEngine.volume)

        controller.setVolume(volume = 0.82f)
        controller.navigateBack()

        assertEquals(expected = 0.82f, actual = controller.uiState.playbackVolume)
        assertEquals(expected = 0.82f, actual = audioPlayerEngine.volume)
    }

    /**
     * 共享音量入口负责归一化，避免不同平台实现重复处理越界 UI 输入。
     */
    @Test
    fun setVolumeCoercesSharedUiStateAndEngineVolume() {
        val audioPlayerEngine = FakeAudioPlayerEngine()
        val controller = createController(audioPlayerEngine = audioPlayerEngine)

        controller.setVolume(volume = 1.4f)

        assertEquals(expected = 1f, actual = controller.uiState.playbackVolume)
        assertEquals(expected = 1f, actual = audioPlayerEngine.volume)

        controller.setVolume(volume = -0.2f)

        assertEquals(expected = 0f, actual = controller.uiState.playbackVolume)
        assertEquals(expected = 0f, actual = audioPlayerEngine.volume)
    }

    /**
     * 播放歌曲后当前歌曲、播放状态和队列应同步。
     */
    @Test
    fun playSongUpdatesPlaybackAndQueue(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val targetSong: Song =
                controller.uiState.homeLocalSongPreview.first { song ->
                    song.title == "The Best of Me"
                }
            controller.playSong(song = targetSong)
            assertEquals(targetSong.id, controller.uiState.currentSongId)
            assertTrue(controller.uiState.shouldShowPauseControl)
            assertTrue(controller.uiState.queueSongIds.contains(targetSong.id))
        }

    /**
     * 从列表点击歌曲时，应把当前列表完整写成播放队列，而不是偷偷回退为单曲队列。
     */
    @Test
    fun playSongUsesProvidedQueueSongs(): Unit =
        runTest {
            val controller = createController(controllerScope = backgroundScope)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val queueSongs = controller.uiState.homeLocalSongPreview
            val targetSong = queueSongs[3]

            controller.playSong(song = targetSong, queueSongs = queueSongs)

            assertEquals(expected = queueSongs.map { song -> song.id }, actual = controller.uiState.queueSongIds)
            assertEquals(expected = targetSong.id, actual = controller.uiState.currentSongId)
        }

    /**
     * 列表点击歌曲只应更新播放状态，播放页必须继续由迷你播放器等显式入口打开。
     */
    @Test
    fun playSongDoesNotNavigateToPlayer(): Unit =
        runTest {
            val controller = createController(controllerScope = backgroundScope)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val queueSongs: List<Song> = controller.uiState.homeLocalSongPreview
            val firstSong: Song = queueSongs[0]
            val secondSong: Song = queueSongs[1]

            controller.navigateToRoot(tab = RootTab.Favorites)
            controller.playSong(song = firstSong, queueSongs = queueSongs)

            assertEquals(expected = RootTab.Favorites, actual = controller.uiState.navigationState.rootTab)
            assertNull(actual = controller.uiState.navigationState.secondaryScreen)

            controller.openLocalMusic(section = LocalMusicSection.Songs)
            controller.playSong(song = secondSong, queueSongs = queueSongs)

            assertEquals(
                expected = SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Songs),
                actual = controller.uiState.navigationState.secondaryScreen,
            )
            assertEquals(expected = secondSong.id, actual = controller.uiState.currentSongId)
        }

    /**
     * 队列弹层或系统入口只给歌曲时，应复用当前显式队列，避免退化成单曲队列。
     */
    @Test
    fun playSongWithoutProvidedQueueKeepsCurrentQueueWhenSongExists(): Unit =
        runTest {
            val controller: MusicAppController = createController(controllerScope = backgroundScope)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val queueSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 4)
            controller.playSong(song = queueSongs[0], queueSongs = queueSongs)
            controller.playSong(song = queueSongs[2])
            assertEquals(expected = queueSongs.map { song -> song.id }, actual = controller.uiState.queueSongIds)
            assertEquals(expected = queueSongs[2].id, actual = controller.uiState.currentSongId)
        }

    /**
     * 播放模式按钮应驱动 UI 状态按顺序反映列表循环、单曲循环和随机播放。
     */
    @Test
    fun cyclePlaybackModeUpdatesUiState(): Unit =
        runTest {
            val controller = createController(controllerScope = backgroundScope)

            assertEquals(expected = PlaybackMode.LoopAll, actual = controller.uiState.playbackMode)
            controller.cyclePlaybackMode()
            assertEquals(expected = PlaybackMode.LoopOne, actual = controller.uiState.playbackMode)
            controller.cyclePlaybackMode()
            assertEquals(expected = PlaybackMode.Shuffle, actual = controller.uiState.playbackMode)
        }

    /**
     * 用户播放歌曲后才会写入真实播放历史，重复播放同一首时保持最近一次在最前。
     */
    @Test
    fun playSongRecordsPlaybackHistory(): Unit =
        runBlocking {
            val playbackRepository = InMemoryPlaybackRepository()
            val controller = createController(playbackRepository = playbackRepository)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val availableSongs: List<Song> = controller.uiState.homeLocalSongPreview
            val firstSong: Song = availableSongs.first { song -> song.title == "海边的梦" }
            val secondSong: Song = availableSongs.first { song -> song.title == "The Best of Me" }
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
    fun moveTrackDoesNotUseSongsAsImplicitQueue() {
        val controller = createController()
        controller.moveTrack(direction = 1)
        assertNull(controller.uiState.currentSongId)
        assertFalse(controller.uiState.isPlaying)
        assertTrue(controller.uiState.queueSongIds.isEmpty())
    }

    /**
     * 用户播放形成显式队列后，队列切歌才应循环移动并保持播放状态。
     */
    @Test
    fun moveTrackChangesCurrentSong(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val queueSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 2)
            controller.playSong(song = queueSongs[0], queueSongs = queueSongs)
            val originalSongId: String? = controller.uiState.currentSongId
            controller.moveTrack(direction = 1)
            assertNotEquals(originalSongId, controller.uiState.currentSongId)
            assertTrue(controller.uiState.shouldShowPauseControl)
        }

    /**
     * 删除当前歌曲后，下一次切歌应依据剩余队列推进，而不是命中引擎里残留的旧队列。
     */
    @Test
    fun removeCurrentSongKeepsEngineQueueInSync(): Unit =
        runTest {
            val controller = createController(controllerScope = backgroundScope)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val queueSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 3)

            controller.playSong(song = queueSongs[1], queueSongs = queueSongs)
            controller.removeFromQueue(songId = queueSongs[1].id)
            assertEquals(expected = queueSongs[0].id, actual = controller.uiState.currentSongId)
            assertEquals(
                expected = listOf(queueSongs[0].id, queueSongs[2].id),
                actual = controller.uiState.queueSongIds,
            )

            controller.moveTrack(direction = 1)

            assertEquals(expected = queueSongs[2].id, actual = controller.uiState.currentSongId)
        }

    /**
     * 恢复暂停快照后再次点击播放，应能直接从恢复的进度继续。
     */
    @Test
    fun restorePlaybackSnapshotAllowsResume(): Unit =
        runTest {
            val snapshotStore = InMemoryPlaybackSnapshotStore()
            val controller =
                createController(
                    playbackSnapshotStore = snapshotStore,
                    controllerScope = backgroundScope,
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val queueSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 3)
            val restoredSong: Song = queueSongs[1]
            snapshotStore.saveSnapshot(
                snapshot =
                    PlaybackSnapshot(
                        playbackState =
                            PlaybackState(
                                currentSongId = restoredSong.id,
                                status = PlaybackStatus.Playing,
                                positionMs = 42_000L,
                                durationMs = restoredSong.durationMs,
                            ),
                        queueState =
                            QueueState(
                                songIds = queueSongs.map { song -> song.id },
                                currentIndex = 1,
                                playbackMode = PlaybackMode.LoopAll,
                            ),
                    ),
            )

            controller.restorePlaybackSnapshot()

            assertEquals(expected = restoredSong.id, actual = controller.uiState.currentSongId)
            assertEquals(expected = PlaybackStatus.Paused, actual = controller.uiState.playbackStatus)
            assertEquals(expected = 42_000L, actual = controller.uiState.playbackPositionMs)

            controller.togglePlayback()
            advanceUntilIdle()

            assertEquals(expected = PlaybackStatus.Playing, actual = controller.uiState.playbackStatus)
            assertEquals(expected = restoredSong.id, actual = controller.uiState.currentSongId)
            assertEquals(expected = 42_000L, actual = controller.uiState.playbackPositionMs)
        }

    /**
     * 冷启动只加载首页 preview 时，也应能按快照队列 id 恢复 preview 外歌曲，且不把这次恢复扩大成全量曲库读取。
     */
    @Test
    fun restorePlaybackSnapshotRestoresSavedSongOutsidePreviewWithoutFullLibraryLoad(): Unit =
        runTest {
            val repository = SeededMusicLibraryRepository(seedCount = 8)
            val snapshotStore = InMemoryPlaybackSnapshotStore()
            snapshotStore.saveSnapshot(
                snapshot =
                    PlaybackSnapshot(
                        playbackState =
                            PlaybackState(
                                currentSongId = "seed:2",
                                status = PlaybackStatus.Playing,
                                positionMs = 24_000L,
                                durationMs = 180_000L,
                            ),
                        queueState =
                            QueueState(
                                songIds = listOf("seed:2", "seed:1"),
                                currentIndex = 0,
                                playbackMode = PlaybackMode.LoopAll,
                            ),
                    ),
            )
            val controller =
                createController(
                    musicLibraryRepository = repository,
                    playbackSnapshotStore = snapshotStore,
                    controllerScope = backgroundScope,
                )

            controller.restorePlaybackSnapshot()

            assertFalse(controller.uiState.homeLocalSongPreview.any { song -> song.id == "seed:2" })
            assertEquals(expected = "seed:2", actual = controller.uiState.currentSongId)
            assertEquals(expected = PlaybackStatus.Paused, actual = controller.uiState.playbackStatus)
            assertEquals(expected = 24_000L, actual = controller.uiState.playbackPositionMs)
            assertEquals(expected = listOf("seed:2", "seed:1"), actual = controller.uiState.queueSongIds)
            assertEquals(expected = "seed:2", actual = controller.uiState.currentSong?.id)
            assertEquals(expected = listOf("seed:2", "seed:1"), actual = controller.uiState.queueSongs.map { song -> song.id })
            assertEquals(expected = 0, actual = repository.fullLibraryReads)
            assertEquals(expected = 1, actual = repository.songsByIdsReads)
        }

    /**
     * 启动时若曲库暂不可用但存在快照，控制器应在后续曲库刷新后按快照恢复暂停态。
     */
    @Test
    fun restorePlaybackSnapshotRestoresAfterLibraryLoads(): Unit =
        runTest {
            val snapshotStore = InMemoryPlaybackSnapshotStore()
            val controller =
                createController(
                    playbackSnapshotStore = snapshotStore,
                    controllerScope = backgroundScope,
                )
            snapshotStore.saveSnapshot(
                snapshot =
                    PlaybackSnapshot(
                        playbackState =
                            PlaybackState(
                                currentSongId = "fakeScanner:004",
                                status = PlaybackStatus.Playing,
                                positionMs = 24_000L,
                                durationMs = 247_000L,
                            ),
                        queueState =
                            QueueState(
                                songIds = listOf("fakeScanner:004", "fakeScanner:002"),
                                currentIndex = 0,
                                playbackMode = PlaybackMode.LoopAll,
                            ),
                    ),
            )

            controller.restorePlaybackSnapshot()
            assertNull(controller.uiState.currentSongId)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            advanceUntilIdle()

            assertEquals(expected = "fakeScanner:004", actual = controller.uiState.currentSongId)
            assertEquals(expected = PlaybackStatus.Paused, actual = controller.uiState.playbackStatus)
            assertEquals(expected = 24_000L, actual = controller.uiState.playbackPositionMs)
            assertEquals(
                expected = listOf("fakeScanner:004", "fakeScanner:002"),
                actual = controller.uiState.queueSongIds.take(n = 2),
            )
        }

    /**
     * 冷启动恢复遇到空曲库时，只记录待恢复状态，不主动触发首次扫描。
     */
    @Test
    fun restorePlaybackSnapshotDoesNotAutoScanWhenLibraryIsEmpty(): Unit =
        runTest {
            val snapshotStore = InMemoryPlaybackSnapshotStore()
            val scanner = RecordingLocalMusicScanner()
            val controller =
                createController(
                    localMusicScanner = scanner,
                    playbackSnapshotStore = snapshotStore,
                    controllerScope = backgroundScope,
                )
            snapshotStore.saveSnapshot(
                snapshot =
                    PlaybackSnapshot(
                        playbackState =
                            PlaybackState(
                                currentSongId = "fakeScanner:004",
                                status = PlaybackStatus.Playing,
                                positionMs = 24_000L,
                                durationMs = 247_000L,
                            ),
                        queueState =
                            QueueState(
                                songIds = listOf("fakeScanner:004", "fakeScanner:002"),
                                currentIndex = 0,
                                playbackMode = PlaybackMode.LoopAll,
                            ),
                    ),
            )

            controller.restorePlaybackSnapshot()
            advanceUntilIdle()

            assertTrue(scanner.requests.isEmpty())
            assertNull(controller.uiState.currentSongId)
            assertFalse(controller.uiState.isPlaying)
        }

    /**
     * 用户显式播放后，旧待加载请求失效，后续扫描完成不能覆盖用户的新播放意图。
     */
    @Test
    fun explicitPlayInvalidatesPendingPlaybackSnapshotRequest(): Unit =
        runTest {
            val persistedSnapshotStore: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore()
            val audioPlayerEngine: RecordingAudioPlayerEngine = RecordingAudioPlayerEngine()
            val blockingSnapshotStore: BlockingRestoreSnapshotStore =
                BlockingRestoreSnapshotStore(
                    delegate = persistedSnapshotStore,
                )
            val controller: MusicAppController =
                createController(
                    audioPlayerEngine = audioPlayerEngine,
                    playbackSnapshotStore = blockingSnapshotStore,
                    controllerScope = backgroundScope,
                )
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            advanceUntilIdle()
            val previewSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 3)
            val restoredQueueSongs: List<Song> = listOf(previewSongs[0], previewSongs[2])
            persistedSnapshotStore.saveSnapshot(
                snapshot =
                    PlaybackSnapshot(
                        playbackState =
                            PlaybackState(
                                currentSongId = restoredQueueSongs[0].id,
                                status = PlaybackStatus.Paused,
                                positionMs = 42_000L,
                                durationMs = restoredQueueSongs[0].durationMs,
                            ),
                        queueState =
                            QueueState(
                                songIds = restoredQueueSongs.map { song: Song -> song.id },
                                currentIndex = 0,
                            ),
                        updatedAt = 1_719_360_000_000L,
                    ),
            )

            val restoreJob: Job =
                launch {
                    controller.restorePlaybackSnapshot()
                }
            blockingSnapshotStore.awaitRestoreStarted()
            val userSong: Song = previewSongs[1]
            controller.playSong(
                song = userSong,
                queueSongs = previewSongs,
            )
            blockingSnapshotStore.releaseRestore()
            restoreJob.join()
            advanceUntilIdle()

            assertEquals(expected = userSong.id, actual = controller.uiState.currentSongId)
            assertEquals(expected = userSong.id, actual = controller.uiState.currentSong?.id)
            assertEquals(
                expected = previewSongs.map { song: Song -> song.id },
                actual = controller.uiState.queueSongIds,
            )
            assertEquals(expected = 0L, actual = controller.uiState.playbackPositionMs)
            assertEquals(expected = PlaybackStatus.Playing, actual = controller.uiState.playbackStatus)
            assertEquals(
                expected = listOf(previewSongs.map { song: Song -> song.id }),
                actual = audioPlayerEngine.setQueueSongIdCalls,
            )
            assertEquals(expected = 1, actual = audioPlayerEngine.playCalls)
            assertEquals(expected = 0, actual = audioPlayerEngine.pauseCalls)
        }

    /**
     * 收藏歌曲应独立于 localSongs 是否已加载，只要喜欢列表里有 id，就应能先补齐实体，再按需进入详情。
     */
    @Test
    fun favoriteSongsRemainAvailableBeforeFullLibraryLoads() {
        val repository = SeededMusicLibraryRepository(seedCount = 8)
        val controller =
            createController(
                musicLibraryRepository = repository,
                favoritesRepository =
                    InMemoryFavoritesRepository(
                        initialLikedSongIds = setOf("seed:2"),
                    ),
            )

        assertTrue(controller.uiState.localSongs.isEmpty())
        assertFalse(controller.uiState.homeLocalSongPreview.any { song -> song.id == "seed:2" })
        assertEquals(expected = listOf("seed:2"), actual = controller.uiState.favoriteSongs.map { song -> song.id })
        assertTrue(controller.uiState.favoriteSongs.all { song -> song.isLiked })
        assertEquals(expected = listOf("album:album"), actual = controller.uiState.favoriteAlbums.map { album -> album.id })
        assertEquals(expected = listOf("artist:artist"), actual = controller.uiState.favoriteArtists.map { artist -> artist.id })
        controller.openAlbum(album = controller.uiState.favoriteAlbums.single())
        assertEquals(expected = "album:album", actual = controller.uiState.selectedAlbum?.id)
        controller.openArtist(artist = controller.uiState.favoriteArtists.single())
        assertEquals(expected = "artist:artist", actual = controller.uiState.selectedArtist?.id)
        assertEquals(expected = 2, actual = repository.songsByIdsReads)
        assertEquals(expected = 1, actual = repository.fullLibraryReads)
    }

    /**
     * 已加载完整曲库后，连续收藏/取消 500 首歌应只更新内存态，不能每次都反查仓库。
     */
    @Test
    fun favoriteStressToggleUsesLoadedLibraryWithoutRepeatedIdLookup(): Unit =
        runTest {
            val repository = SeededMusicLibraryRepository(seedCount = 500)
            val controller =
                createController(
                    musicLibraryRepository = repository,
                    favoritesRepository = InMemoryFavoritesRepository(initialLikedSongIds = emptySet()),
                    controllerScope = backgroundScope,
                )
            controller.loadLocalMusicLibrary()
            repository.songsByIdsReads = 0
            val stressSongs: List<Song> = controller.uiState.localSongs.take(n = 500)

            stressSongs.forEach { song: Song ->
                controller.toggleFavorite(songId = song.id)
            }

            assertEquals(expected = 500, actual = controller.uiState.likedSongIds.size)
            assertEquals(expected = 500, actual = controller.uiState.favoriteSongs.size)

            stressSongs.forEach { song: Song ->
                controller.toggleFavorite(songId = song.id)
            }

            assertTrue(actual = controller.uiState.likedSongIds.isEmpty())
            assertTrue(actual = controller.uiState.favoriteSongs.isEmpty())
            assertEquals(expected = 0, actual = repository.songsByIdsReads)
        }

    /**
     * common 默认 fake 演示入口扫描后应直接得到 500 条收藏歌曲，方便收藏页压力测试。
     */
    @Test
    fun defaultFakeScannerSeedsFiveHundredFavoriteSongsForStress(): Unit =
        runBlocking {
            val controller = MusicAppController(controllerScope = testControllerScope())

            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)

            assertEquals(expected = 500, actual = controller.uiState.libraryStats.songCount)
            assertEquals(expected = 500, actual = controller.uiState.likedSongIds.size)
            assertEquals(expected = 500, actual = controller.uiState.favoriteSongs.size)
        }

    /**
     * 从歌曲进入歌手详情时，应复用歌手归属规则，避免本地元数据大小写或空白差异导致入口失效。
     */
    @Test
    fun openArtistFromSongUsesNormalizedArtistName() {
        val controller =
            createController(
                musicLibraryRepository = ArtistVariantMusicLibraryRepository(),
            )
        val targetSong: Song = controller.uiState.homeLocalSongPreview.first()
        controller.openArtistFromSong(song = targetSong)
        assertEquals(expected = SecondaryScreen.ArtistDetail, actual = controller.uiState.navigationState.secondaryScreen)
        assertEquals(expected = "artist:jay chou", actual = controller.uiState.selectedArtist?.id)
    }

    /**
     * 收藏状态应同时同步到集合和歌曲列表。
     */
    @Test
    fun toggleFavoriteSyncsSongList(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val targetSong: Song = controller.uiState.homeLocalSongPreview.first { song -> song.title == "Summer Waltz" }
            controller.toggleFavorite(songId = targetSong.id)
            assertTrue(controller.uiState.likedSongIds.contains(targetSong.id))
            assertTrue(
                controller.uiState.homeLocalSongPreview
                    .first { song -> song.id == targetSong.id }
                    .isLiked,
            )
        }

    /**
     * 平台宿主应能通过共享控制器直接切换当前播放歌曲收藏，而不必窥探 [MusicAppUiState.currentSongId]。
     */
    @Test
    fun toggleCurrentSongFavoriteUsesSharedControllerEntry(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val targetSong: Song = controller.uiState.homeLocalSongPreview.first { song -> song.title == "Summer Waltz" }
            controller.playSong(song = targetSong)
            controller.toggleCurrentSongFavorite()
            assertTrue(controller.uiState.likedSongIds.contains(element = targetSong.id))
            assertTrue(controller.uiState.currentSong?.isLiked == true)
        }

    /**
     * 搜索范围为歌曲时不应返回专辑和歌手。
     */
    @Test
    fun searchScopeLimitsResultTypes(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            controller.setSearchQuery(query = "旅行团")
            controller.commitSearchQueryToHistory()
            controller.setSearchScope(scope = SearchScope.Songs)
            val result = controller.search()
            assertTrue(result.songs.isNotEmpty())
            assertTrue(result.albums.isEmpty())
            assertTrue(result.artists.isEmpty())
        }

    /**
     * 搜索必须读取扫描后的曲库快照，而不是 seed/mock 仓库。
     */
    @Test
    fun searchReadsScannedSnapshot(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            controller.setSearchQuery(query = "One Summer")
            controller.commitSearchQueryToHistory()
            controller.setSearchScope(scope = SearchScope.Songs)
            assertEquals(
                expected = listOf("One Summer's Day"),
                actual = controller.search().songs.map { song -> song.title },
            )
        }

    /**
     * 首页搜索应搜索完整本地曲库，不受收藏集合限制。
     */
    @Test
    fun localLibrarySearchReturnsNonFavoriteLocalSongs(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            controller.openSearch(context = SearchContext.LocalLibrary)
            controller.setSearchQuery(query = "One Summer")
            controller.commitSearchQueryToHistory()
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
    fun favoritesSearchOnlyReturnsFavoriteSongs(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            controller.openSearch(context = SearchContext.LocalLibrary)
            val favoriteSong: Song = controller.uiState.localSongs.first { song -> song.title == "One Summer's Day" }
            controller.toggleFavorite(songId = favoriteSong.id)

            controller.openSearch(context = SearchContext.Favorites)
            controller.setSearchQuery(query = "One Summer")
            controller.commitSearchQueryToHistory()
            controller.setSearchScope(scope = SearchScope.Songs)

            assertEquals(
                expected = listOf("One Summer's Day"),
                actual = controller.search().songs.map { song -> song.title },
            )

            controller.setSearchQuery(query = "The Best of Me")
            controller.commitSearchQueryToHistory()

            assertTrue(actual = controller.search().songs.isEmpty())
        }

    /**
     * 首页顶部搜索应进入本地曲库搜索上下文。
     */
    @Test
    fun homeSearchOpensLocalLibrarySearchContext() {
        val controller = createController()

        controller.navigateToRoot(tab = RootTab.Home)
        controller.openSearch(context = SearchContext.LocalLibrary)

        assertEquals(
            expected = SecondaryScreen.Search(context = SearchContext.LocalLibrary),
            actual = controller.uiState.navigationState.secondaryScreen,
        )
        assertEquals(expected = RootTab.Home, actual = controller.uiState.navigationState.previousRootTab)
        assertEquals(expected = SearchContext.LocalLibrary, actual = controller.uiState.searchContext)
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
        assertFalse(actual = controller.uiState.navigationState.fixedBarMode.showsBottomNavigation)
    }

    /**
     * 收藏顶部搜索应进入收藏搜索上下文。
     */
    @Test
    fun favoritesSearchOpensFavoritesSearchContext() {
        val controller = createController()

        controller.navigateToRoot(tab = RootTab.Favorites)
        controller.openSearch(context = SearchContext.Favorites)

        assertEquals(
            expected = SecondaryScreen.Search(context = SearchContext.Favorites),
            actual = controller.uiState.navigationState.secondaryScreen,
        )
        assertEquals(expected = RootTab.Favorites, actual = controller.uiState.navigationState.previousRootTab)
        assertEquals(expected = SearchContext.Favorites, actual = controller.uiState.searchContext)
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
        assertFalse(actual = controller.uiState.navigationState.fixedBarMode.showsBottomNavigation)
    }

    /**
     * 搜索入口应按需加载完整曲库，而不是只搜索首页 preview。
     */
    @Test
    fun searchLoadsFullLibraryInsteadOfHomePreviewOnly() {
        val repository = SeededMusicLibraryRepository(seedCount = 8)
        val controller = createController(musicLibraryRepository = repository)

        controller.openSearch(context = SearchContext.LocalLibrary)
        controller.setSearchQuery(query = "Seed 8")
        controller.commitSearchQueryToHistory()
        controller.setSearchScope(scope = SearchScope.Songs)

        assertEquals(
            expected = listOf("Seed 8"),
            actual = controller.search().songs.map { song -> song.title },
        )
        assertEquals(expected = 1, actual = repository.fullLibraryReads)
    }

    /**
     * 搜索输入防抖生效前不应读取完整曲库，避免把空 active query 当成全量搜索。
     */
    @Test
    fun pendingSearchQueryDoesNotReturnFullLibraryBeforeDebounce(): Unit =
        runTest {
            val repository = SeededMusicLibraryRepository(seedCount = 8)
            val controller =
                createController(
                    musicLibraryRepository = repository,
                    controllerScope = backgroundScope,
                )

            controller.openSearch(context = SearchContext.LocalLibrary)
            controller.setSearchScope(scope = SearchScope.Songs)
            val fullLibraryReadsBeforeTyping: Int = repository.fullLibraryReads
            listOf("a", "as", "asf", "asfa", "asfasfasdffsadfasdf").forEach { query: String ->
                controller.setSearchQuery(query = query)
                controller.search()
            }
            runCurrent()

            assertEquals(expected = fullLibraryReadsBeforeTyping, actual = repository.fullLibraryReads)
            assertTrue(actual = controller.search().songs.isEmpty())
            advanceTimeBy(delayTimeMillis = 301L)
            runCurrent()
            advanceUntilIdle()
            assertEquals(expected = "asfasfasdffsadfasdf", actual = controller.uiState.activeSearchQuery)
            assertTrue(actual = controller.search().songs.isEmpty())
        }

    /**
     * facade 层设置搜索词后，active query 应通过防抖发布回 [uiState]，证明提取后的 reducer 仍接在公开边界上。
     */
    @Test
    fun debouncedSearchQueryPublishesActiveQueryThroughFacade(): Unit =
        runTest {
            val controller = createController(controllerScope = backgroundScope)

            controller.openSearch(context = SearchContext.LocalLibrary)
            controller.setSearchQuery(query = "river")

            assertEquals(expected = "", actual = controller.uiState.activeSearchQuery)
            advanceTimeBy(delayTimeMillis = 301L)
            advanceUntilIdle()
            assertEquals(expected = "river", actual = controller.uiState.activeSearchQuery)
        }

    /**
     * 防抖搜索醒来时必须基于最新 [uiState] 归约，不能覆盖期间到达的播放状态。
     */
    @Test
    fun debouncedSearchUpdatePreservesPlaybackStateChangedBeforeDebounce(): Unit =
        runTest {
            val controller = createController(controllerScope = backgroundScope)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            advanceUntilIdle()
            val queueSongs: List<Song> =
                controller.uiState.localSongs
                    .take(n = 3)
                    .ifEmpty { controller.uiState.homeLocalSongPreview.take(n = 3) }
            val firstSong: Song = queueSongs[0]
            val secondSong: Song = queueSongs[1]

            controller.openSearch(context = SearchContext.LocalLibrary)
            controller.setSearchQuery(query = "river")
            controller.playSong(song = firstSong, queueSongs = queueSongs)
            advanceUntilIdle()
            controller.playSong(song = secondSong, queueSongs = queueSongs)
            advanceUntilIdle()

            advanceTimeBy(delayTimeMillis = 301L)
            advanceUntilIdle()

            assertEquals(expected = "river", actual = controller.uiState.activeSearchQuery)
            assertEquals(expected = secondSong.id, actual = controller.uiState.currentSongId)
            assertEquals(
                expected = queueSongs.map { song: Song -> song.id },
                actual = controller.uiState.queueSongIds,
            )
            assertEquals(
                expected = queueSongs.map { song: Song -> song.id },
                actual = controller.uiState.queueSongs.map { song: Song -> song.id },
            )
        }

    /**
     * 非空搜索词在防抖结果生效前离开搜索页不应写入历史，避免把未执行搜索污染为历史。
     */
    @Test
    fun nonBlankSearchQueryDoesNotCommitToHistoryWhenLeavingSearchBeforeDebounce(): Unit =
        runTest {
            val controller = createController(controllerScope = backgroundScope)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            controller.openSearch(context = SearchContext.LocalLibrary)
            controller.setSearchQuery(query = "One Summer")
            controller.navigateBack()
            advanceTimeBy(delayTimeMillis = 301L)
            advanceUntilIdle()
            controller.openSearch(context = SearchContext.LocalLibrary)
            assertEquals(
                expected = emptyList(),
                actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
            )
        }

    /**
     * 搜索行为记录不依赖结果命中，用户搜过的无结果关键词也应能回到历史里。
     */
    @Test
    fun searchQueryWithoutResultsCommitsToHistoryAfterExplicitSubmit(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)

            controller.openSearch(context = SearchContext.LocalLibrary)
            controller.setSearchQuery(query = "绝对不存在的搜索词")
            controller.commitSearchQueryToHistory()
            assertTrue(actual = controller.search().songs.isEmpty())
            assertTrue(actual = controller.search().albums.isEmpty())
            assertTrue(actual = controller.search().artists.isEmpty())

            controller.navigateBack()
            controller.openSearch(context = SearchContext.LocalLibrary)

            assertEquals(
                expected = listOf("绝对不存在的搜索词"),
                actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
            )
        }

    /**
     * 搜索结果动作应记录当前搜索词，覆盖歌曲播放、专辑打开、歌手打开以及歌曲更多菜单详情入口。
     */
    @Test
    fun searchResultActionsCommitCurrentQueryToHistory(): Unit =
        runTest {
            val controller = createController(controllerScope = backgroundScope)
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)

            controller.openSearch(context = SearchContext.LocalLibrary)

            controller.setSearchQuery(query = "Summer Waltz")
            advanceTimeBy(delayTimeMillis = 301L)
            advanceUntilIdle()
            val songResult: SearchResult = controller.search()
            val targetSong: Song = songResult.songs.first()
            controller.playSong(song = targetSong, queueSongs = songResult.songs)
            assertEquals(
                expected = listOf("Summer Waltz"),
                actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
            )

            controller.openSearch(context = SearchContext.LocalLibrary)
            controller.setSearchQuery(query = "Dream Stories")
            advanceTimeBy(delayTimeMillis = 301L)
            advanceUntilIdle()
            val albumResult: SearchResult = controller.search()
            val targetAlbum: Album = albumResult.albums.first()
            controller.openAlbum(album = targetAlbum)
            assertEquals(
                expected = listOf("Dream Stories", "Summer Waltz"),
                actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
            )

            controller.openSearch(context = SearchContext.LocalLibrary)
            controller.setSearchQuery(query = "久石让")
            advanceTimeBy(delayTimeMillis = 301L)
            advanceUntilIdle()
            val artistResult: SearchResult = controller.search()
            val targetArtist: Artist = artistResult.artists.first()
            controller.openArtist(artist = targetArtist)
            assertEquals(
                expected = listOf("久石让", "Dream Stories", "Summer Waltz"),
                actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
            )

            controller.openSearch(context = SearchContext.LocalLibrary)
            controller.setSearchQuery(query = "One Summer's Day")
            advanceTimeBy(delayTimeMillis = 301L)
            advanceUntilIdle()
            val songAlbumResult: SearchResult = controller.search()
            val albumSourceSong: Song = songAlbumResult.songs.first()
            controller.openAlbumFromSong(song = albumSourceSong)
            assertEquals(
                expected = listOf("One Summer's Day", "久石让", "Dream Stories", "Summer Waltz"),
                actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
            )

            controller.openSearch(context = SearchContext.LocalLibrary)
            controller.setSearchQuery(query = "Summer Waltz")
            advanceTimeBy(delayTimeMillis = 301L)
            advanceUntilIdle()
            val songArtistResult: SearchResult = controller.search()
            val artistSourceSong: Song = songArtistResult.songs.first()
            controller.openArtistFromSong(song = artistSourceSong)
            assertEquals(
                expected =
                    listOf(
                        "Summer Waltz",
                        "One Summer's Day",
                        "久石让",
                        "Dream Stories",
                    ),
                actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
            )
        }

    /**
     * 非搜索页里的播放和详情动作不应把残留输入词写入搜索历史。
     */
    @Test
    fun nonSearchResultActionsDoNotCommitSearchHistory(): Unit =
        runBlocking {
            val controller = createController()
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
            val targetSong: Song = controller.uiState.homeLocalSongPreview.first()
            controller.setHomeContentSection(section = HomeContentSection.Albums)
            val targetAlbum: Album = controller.uiState.localAlbums.first()
            controller.setHomeContentSection(section = HomeContentSection.Artists)
            val targetArtist: Artist = controller.uiState.localArtists.first()

            controller.setSearchQuery(query = "普通页面动作")
            controller.playSong(song = targetSong, queueSongs = listOf(targetSong))
            controller.openAlbum(album = targetAlbum)
            controller.openArtist(artist = targetArtist)

            assertEquals(
                expected = emptyList(),
                actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
            )
        }

    /**
     * 一级 Tab 切换后不应存在二级页面。
     */
    @Test
    fun rootNavigationClearsSecondaryScreen() {
        val controller = createController()
        controller.navigateToSecondary(screen = SecondaryScreen.Player)
        controller.navigateToRoot(tab = RootTab.Me)
        assertNull(controller.uiState.navigationState.secondaryScreen)
        assertEquals(RootTab.Me, controller.uiState.navigationState.rootTab)
    }

    /**
     * 当前播放页入口应复用共享导航，供迷你播放器和 Android 媒体通知正文点击保持一致。
     */
    @Test
    fun openPlayerUsesFullscreenSecondaryScreen() {
        val controller = createController()
        controller.navigateToRoot(tab = RootTab.Favorites)
        controller.openPlayer()
        assertEquals(
            expected = SecondaryScreen.Player,
            actual = controller.uiState.navigationState.secondaryScreen,
        )
        assertEquals(
            expected = RootTab.Favorites,
            actual = controller.uiState.navigationState.previousRootTab,
        )
        assertEquals(
            expected = MobileFixedBarMode.Player,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
    }

    /**
     * 设置页打开关于页时应保留设置页作为底层页面，让 mini-player 只被覆盖而不执行隐藏动画。
     */
    @Test
    fun aboutScreenCoversSettingsWithoutChangingUnderlayChrome() {
        val controller = createController()
        controller.navigateToRoot(tab = RootTab.Me)
        controller.navigateToSecondary(screen = SecondaryScreen.Settings)
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.fixedBarMode,
        )

        controller.navigateToSecondary(screen = SecondaryScreen.About)

        assertEquals(expected = SecondaryScreen.About, actual = controller.uiState.navigationState.secondaryScreen)
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithoutChrome,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
        assertEquals(
            expected = SecondaryScreen.Settings,
            actual = controller.uiState.navigationState.chromeUnderlaySecondaryScreen,
        )
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.chromeUnderlayFixedBarMode,
        )

        controller.navigateBack()

        assertEquals(expected = SecondaryScreen.Settings, actual = controller.uiState.navigationState.secondaryScreen)
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
    }

    /**
     * 设置页打开扫描页时应覆盖当前页，底层 mini-player 不重新执行贴底或隐藏动画。
     */
    @Test
    fun audioScanScreenCoversSettingsWithoutChangingUnderlayChrome() {
        val controller = createController()
        controller.navigateToRoot(tab = RootTab.Me)
        controller.navigateToSecondary(screen = SecondaryScreen.Settings)

        controller.openAudioScan()

        assertEquals(
            expected = SecondaryScreen.AudioScan,
            actual = controller.uiState.navigationState.secondaryScreen,
        )
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithoutChrome,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
        assertEquals(
            expected = SecondaryScreen.Settings,
            actual = controller.uiState.navigationState.chromeUnderlaySecondaryScreen,
        )
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.chromeUnderlayFixedBarMode,
        )

        controller.navigateBack()

        assertEquals(
            expected = SecondaryScreen.Settings,
            actual = controller.uiState.navigationState.secondaryScreen,
        )
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
    }

    /**
     * 一级页面滚动 key 应按 Tab 保持稳定，保证从二级页返回后能恢复原滚动位置。
     */
    @Test
    fun rootScrollStateKeyStaysStableAfterSecondaryReturn() {
        val controller = createController()
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
    fun secondaryScrollStateKeyChangesForEachEntry() {
        val controller = createController()
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
    fun systemBackReturnsFromSecondaryScreen(): Unit =
        runBlocking {
            val controller = createController()
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
    fun systemBackClosesOverlayBeforeSecondaryScreen() {
        val controller = createController()
        controller.navigateToSecondary(screen = SecondaryScreen.AlbumDetail)
        controller.openQueue()
        assertTrue(controller.handleSystemBack())
        assertFalse(controller.uiState.isQueueOpen)
        assertFalse(controller.uiState.navigationState.isTopLevel)
        assertTrue(controller.handleSystemBack())
        assertTrue(controller.uiState.navigationState.isTopLevel)
    }
}

private fun createController(
    musicLibraryRepository: MusicLibraryRepository = InMemoryMusicLibraryRepository(),
    localMusicScanner: LocalMusicScanner = FakeControllerLocalMusicScanner,
    playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository(),
    audioPlayerEngine: AudioPlayerEngine = FakeAudioPlayerEngine(),
    playbackSnapshotStore: PlaybackSnapshotStore = InMemoryPlaybackSnapshotStore(),
    favoritesRepository: FavoritesRepository? = null,
    localPlaylistRepository: LocalPlaylistRepository = RecordingLocalPlaylistRepository(),
    userPreferencesRepository: UserPreferencesRepository = InMemoryUserPreferencesRepository(),
    searchHistoryRepository: SearchHistoryRepository = FakeSearchHistoryRepository(),
    permissionSettingsOpener: PermissionSettingsOpener = PermissionSettingsOpener {},
    controllerScope: CoroutineScope = testControllerScope(),
    searchQueryDebounceMillis: Long = 300L,
): MusicAppController =
    MusicAppController(
        musicLibraryRepository = musicLibraryRepository,
        localMusicScanner = localMusicScanner,
        playbackRepository = playbackRepository,
        audioPlayerEngine = audioPlayerEngine,
        playbackSnapshotStore = playbackSnapshotStore,
        injectedFavoritesRepository = favoritesRepository,
        localPlaylistRepository = localPlaylistRepository,
        userPreferencesRepository = userPreferencesRepository,
        searchHistoryRepository = searchHistoryRepository,
        permissionSettingsOpener = permissionSettingsOpener,
        controllerScope = controllerScope,
        searchQueryDebounceMillis = searchQueryDebounceMillis,
    )

private class RecordingLocalPlaylistRepository(
    private val defaultNames: List<String> = listOf("默认歌单 1"),
    private val createResults: MutableList<CreateLocalPlaylistWithSongResult> = mutableListOf(),
    // 测试用已有歌单集合，模拟仓库按当前任务读取弹窗候选项。
    private val playlists: MutableList<LocalPlaylist> = mutableListOf(),
    // 测试用详情集合，模拟仓库解析歌单封面和可用歌曲数量。
    private val playlistDetails: MutableMap<String, LocalPlaylistDetail> = mutableMapOf(),
    // 预置添加结果，用来覆盖真实新增和幂等命中两种成功路径。
    private val addSongResults: MutableList<AddSongToLocalPlaylistResult> = mutableListOf(),
) : LocalPlaylistRepository {
    // 记录新建并加入调用，确认控制器传入的是当前歌曲。
    val createWithSongCalls: MutableList<Pair<String, String>> = mutableListOf()

    // 记录添加到已有歌单调用，确认控制器只保存用户选中的单个目标。
    val addSongCalls: MutableList<Pair<String, String>> = mutableListOf()

    // 记录批量删除调用，确认管理页只传用户当前选择。
    val deletePlaylistCalls: MutableList<Set<String>> = mutableListOf()

    // 添加歌曲后的测试回调，用于模拟仓库事实即时变化。
    var onAddSong: (playlistId: String, songId: String) -> Unit = { _: String, _: String -> }

    /** 当前测试不通过独立创建空歌单入口，返回最小成功值即可。 */
    override fun createPlaylist(name: String): LocalPlaylistCreateResult {
        val playlist: LocalPlaylist =
            LocalPlaylist(
                id = "created:$name",
                name = name.trim(),
                createdAt = 1L,
                updatedAt = 1L,
            )
        return LocalPlaylistCreateResult.Success(playlist = playlist)
    }

    /** 记录原子流程调用，并按测试预置结果返回。 */
    override fun createPlaylistWithSong(
        name: String,
        songId: String,
    ): CreateLocalPlaylistWithSongResult {
        createWithSongCalls += name to songId
        val nextResult: CreateLocalPlaylistWithSongResult? =
            if (createResults.isEmpty()) {
                null
            } else {
                createResults.removeAt(index = 0)
            }
        return nextResult ?: CreateLocalPlaylistWithSongResult.Success(
            playlist =
                LocalPlaylist(
                    id = "created:${name.trim()}",
                    name = name.trim(),
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            relation =
                LocalPlaylistSong(
                    playlistId = "created:${name.trim()}",
                    songId = songId,
                    addedAt = 1L,
                    sortOrder = 0,
                ),
        )
    }

    /** 添加到已有歌单时记录调用，并按预置结果模拟仓库返回。 */
    override fun addSongToPlaylist(
        playlistId: String,
        songId: String,
    ): AddSongToLocalPlaylistResult {
        addSongCalls += playlistId to songId
        val nextResult: AddSongToLocalPlaylistResult? =
            if (addSongResults.isEmpty()) {
                null
            } else {
                addSongResults.removeAt(index = 0)
            }
        val result: AddSongToLocalPlaylistResult =
            nextResult ?: AddSongToLocalPlaylistResult.Added(
                relation =
                    LocalPlaylistSong(
                        playlistId = playlistId,
                        songId = songId,
                        addedAt = 1L,
                        sortOrder = 0,
                    ),
            )
        if (result is AddSongToLocalPlaylistResult.Added) {
            onAddSong(playlistId, songId)
        }
        return result
    }

    /** 删除歌单时同时移除详情，模拟仓库只删除歌单容器数据。 */
    override fun deletePlaylists(playlistIds: Set<String>): LocalPlaylistDeleteResult {
        deletePlaylistCalls += playlistIds
        val beforeCount: Int = playlists.size
        playlists.removeAll { playlist: LocalPlaylist -> playlist.id in playlistIds }
        playlistIds.forEach { playlistId: String ->
            playlistDetails.remove(key = playlistId)
        }
        return LocalPlaylistDeleteResult(deletedCount = beforeCount - playlists.size)
    }

    /** 读取已有歌单列表，供添加到已有歌单弹窗展示全部目标。 */
    override fun getPlaylists(): List<LocalPlaylist> =
        playlists.sortedWith(
            compareByDescending<LocalPlaylist> { playlist: LocalPlaylist -> playlist.updatedAt }
                .thenBy { playlist: LocalPlaylist -> playlist.name },
        )

    /** 搜索已有歌单时只按名称做大小写不敏感包含匹配。 */
    override fun searchPlaylists(query: String): List<LocalPlaylist> {
        val normalizedQuery: String = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) {
            return getPlaylists()
        }
        return playlists.filter { playlist: LocalPlaylist ->
            playlist.name.lowercase().contains(other = normalizedQuery)
        }
    }

    /** 依次返回预置默认名，用来验证控制器确实读取仓库规则。 */
    override fun getNextDefaultPlaylistName(): String = defaultNames.first()

    /** 读取测试预置详情，供歌单列表卡片投影使用。 */
    override fun getPlaylistDetail(playlistId: String): LocalPlaylistDetail? = playlistDetails[playlistId]

    /** 替换测试仓库中的歌单列表，模拟持久层保存后的最新排序事实。 */
    fun replacePlaylists(nextPlaylists: List<LocalPlaylist>) {
        playlists.clear()
        playlists += nextPlaylists
    }

    /** 写入测试仓库中的歌单详情，模拟持久层保存后的卡片事实。 */
    fun putPlaylistDetail(detail: LocalPlaylistDetail) {
        playlistDetails[detail.playlist.id] = detail
    }
}

private fun assertPlaybackQueueInvariant(
    controller: MusicAppController,
    playbackRepository: InMemoryPlaybackRepository,
    expectedQueueSongIds: List<String>,
) {
    assertEquals(expected = expectedQueueSongIds, actual = playbackRepository.getQueueState().songIds)
    assertEquals(expected = expectedQueueSongIds, actual = controller.uiState.queueSongIds)
    assertEquals(expected = expectedQueueSongIds, actual = controller.uiState.queueSongs.map { song: Song -> song.id })
    assertEquals(
        expected = controller.uiState.currentSongId,
        actual = controller.uiState.currentSong?.id,
    )
}

// 构造本地自建歌单元信息，避免控制器测试依赖持久化实现。
private fun testPlaylist(
    id: String,
    name: String,
    updatedAt: Long = 1L,
): LocalPlaylist =
    LocalPlaylist(
        id = id,
        name = name,
        createdAt = 1L,
        updatedAt = updatedAt,
    )

private class FakeSearchHistoryRepository : SearchHistoryRepository {
    // 测试用内存表，按上下文隔离搜索词。
    private val histories: MutableMap<SearchContext, List<String>> = mutableMapOf()

    /** 读取指定上下文的历史。 */
    override fun getSearchHistory(context: SearchContext): List<String> = histories[context].orEmpty()

    /** 保存指定上下文的历史。 */
    override fun saveSearchHistory(
        context: SearchContext,
        history: List<String>,
    ) {
        histories[context] = history
    }
}

private fun testControllerScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

private object FakeControllerLocalMusicScanner : LocalMusicScanner {
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult =
        com.yanhao.kmpmusic.data
            .FakeLocalMusicScanner(demoSongCount = 8)
            .scan(request = request)

    /** 转发本地音频发现偏好，避免测试默认 scanner 丢掉过滤配置。 */
    override suspend fun scan(
        request: LocalMusicScanRequest,
        preferences: LocalMusicDiscoveryPreferences,
    ): LocalMusicScanResult =
        com.yanhao.kmpmusic.data.FakeLocalMusicScanner(demoSongCount = 8).scan(
            request = request,
            preferences = preferences,
        )
}

// 将运行中的扫描入口映射为取消动作文案，避免二次点击被误当成重新扫描。
private fun renderCancelEntryLabelOrNull(scanState: LocalMusicScanState): String? =
    when (scanState) {
        LocalMusicScanState.Idle,
        LocalMusicScanState.WaitingForPermission,
        is LocalMusicScanState.Done,
        is LocalMusicScanState.Cancelled,
        is LocalMusicScanState.Error,
        -> null

        is LocalMusicScanState.Scanning,
        is LocalMusicScanState.Importing,
        -> "取消扫描"
    }

// 通过函数边界避免测试在已确认取消态后触发 sealed smart-cast 恒假警告。
private fun isDoneScanState(scanState: LocalMusicScanState): Boolean = scanState is LocalMusicScanState.Done

// 通过函数边界保留“取消不是失败”的用户可感知契约。
private fun isErrorScanState(scanState: LocalMusicScanState): Boolean = scanState is LocalMusicScanState.Error

/**
 * 模拟平台 scanner 把用户取消作为独立可识别错误上报。
 */
private class UserCancelledScanner : LocalMusicScanner {
    /** 抛出用户取消错误，驱动控制器进入未来的取消结果态。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult =
        throw LocalMusicScanException(
            error =
                LocalMusicScanError(
                    type = LocalMusicScanErrorType.UserCancelled,
                    message = "用户取消了本地音乐扫描",
                    sourceKind = LocalMusicSourceKind.FakeScanner,
                ),
        )
}

/**
 * 挂起扫描流程，验证控制器在扫描未结束时不会重复进入 scanner。
 */
private class BlockingLocalMusicScanner : LocalMusicScanner {
    // 第一次扫描启动信号，测试用它稳定等待扫描进入挂起点。
    private val firstScanStarted: CompletableDeferred<Unit> = CompletableDeferred()

    // 扫描完成信号，由测试显式释放，避免真实时间等待。
    private val scanCanComplete: CompletableDeferred<Unit> = CompletableDeferred()

    // 记录 scanner 被调用次数，扫描中二次点击不应增加。
    var scanCount: Int = 0
        private set

    /** 挂起到测试释放，用来模拟长时间扫描任务。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        scanCount += 1
        if (scanCount == 1) {
            firstScanStarted.complete(value = Unit)
        }
        scanCanComplete.await()
        return LocalMusicScanResult(
            discovered = emptyList(),
            completedAt = 1_719_360_004_000L,
        )
    }

    /** 等待第一次扫描实际进入 scanner，避免并发断言抢跑。 */
    suspend fun awaitFirstScanStarted() {
        firstScanStarted.await()
    }

    /** 释放挂起扫描，让测试可以收尾。 */
    fun complete() {
        scanCanComplete.complete(value = Unit)
    }
}

/**
 * 第一次扫描立即完成，第二次扫描挂起，用来检查运行中状态保留上一轮摘要。
 */
private class BlockingAfterFirstScanScanner : LocalMusicScanner {
    // 第二次扫描启动信号，测试用它稳定观察运行中状态。
    private val secondScanStarted: CompletableDeferred<Unit> = CompletableDeferred()

    // 第二次扫描完成信号，由测试显式释放。
    private val secondScanCanComplete: CompletableDeferred<Unit> = CompletableDeferred()

    // 记录 scanner 被调用次数，用来区分第一次完成和第二次挂起。
    private var scanCount: Int = 0

    /** 按调用次数返回完成结果或挂起结果。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult =
        scan(
            request = request,
            preferences = LocalMusicDiscoveryPreferences(),
        )

    /** 按调用次数返回完成结果或挂起结果，并保留偏好传递路径。 */
    override suspend fun scan(
        request: LocalMusicScanRequest,
        preferences: LocalMusicDiscoveryPreferences,
    ): LocalMusicScanResult {
        scanCount += 1
        if (scanCount == 1) {
            return com.yanhao.kmpmusic.data.FakeLocalMusicScanner(demoSongCount = 8).scan(
                request = request,
                preferences = preferences,
            )
        }
        secondScanStarted.complete(value = Unit)
        secondScanCanComplete.await()
        return com.yanhao.kmpmusic.data.FakeLocalMusicScanner(demoSongCount = 8).scan(
            request = request,
            preferences = preferences,
        )
    }

    /** 等待第二次扫描进入挂起点。 */
    suspend fun awaitSecondScanStarted() {
        secondScanStarted.await()
    }

    /** 释放第二次扫描，让测试可以收尾。 */
    fun completeSecondScan() {
        secondScanCanComplete.complete(value = Unit)
    }
}

/**
 * 取消后仍会晚到返回结果，用来验证门面不会被旧扫描覆盖。
 */
private class LateSuccessAfterCancellationScanner : LocalMusicScanner {
    // 记录扫描已启动，便于测试稳定触发取消。
    private val started: CompletableDeferred<Unit> = CompletableDeferred()

    // 即使收到取消也继续等待该信号，制造旧结果晚到。
    private val release: CompletableDeferred<Unit> = CompletableDeferred()

    /**
     * 忽略协程取消并晚到返回结果，模拟不配合取消的平台扫描器。
     */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        started.complete(value = Unit)
        try {
            release.await()
        } catch (cancellationException: CancellationException) {
            withContext(NonCancellable) {
                release.await()
            }
        }
        return com.yanhao.kmpmusic.data
            .FakeLocalMusicScanner(demoSongCount = 8)
            .scan(request = request)
    }

    /** 等待扫描启动。 */
    suspend fun awaitStarted() {
        started.await()
    }

    /** 释放晚到结果。 */
    fun releaseLateResult() {
        release.complete(value = Unit)
    }
}

/**
 * 第一次扫描取消后晚到成功，第三次入口要能马上启动第二次真实扫描。
 */
private class RestartableLateSuccessScanner : LocalMusicScanner {
    // 第一次扫描已进入 scanner 的信号。
    private val firstStarted: CompletableDeferred<Unit> = CompletableDeferred()

    // 第二次扫描已进入 scanner 的信号。
    private val secondStarted: CompletableDeferred<Unit> = CompletableDeferred()

    // 第一次扫描即使取消也会等到这里才返回。
    private val firstRelease: CompletableDeferred<Unit> = CompletableDeferred()

    // 第二次扫描的正常完成信号。
    private val secondRelease: CompletableDeferred<Unit> = CompletableDeferred()

    // 记录扫描调用次数，用来确认第三次入口确实启动了新扫描。
    var scanCount: Int = 0
        private set

    /**
     * 第一次调用忽略取消并晚到返回，第二次调用代表新的可见扫描会话。
     */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        scanCount += 1
        return if (scanCount == 1) {
            firstStarted.complete(value = Unit)
            awaitFirstRelease()
            LocalMusicScanResult(
                discovered =
                    buildDiscoveredFiles(
                        count = 2,
                        prefix = "old",
                    ),
                completedAt = 1_719_360_004_000L,
            )
        } else {
            secondStarted.complete(value = Unit)
            secondRelease.await()
            LocalMusicScanResult(
                discovered =
                    buildDiscoveredFiles(
                        count = 3,
                        prefix = "new",
                    ),
                completedAt = 1_719_360_005_000L,
            )
        }
    }

    /** 等待第一次扫描启动。 */
    suspend fun awaitFirstStarted() {
        firstStarted.await()
    }

    /** 等待第二次扫描启动。 */
    suspend fun awaitSecondStarted() {
        secondStarted.await()
    }

    /** 释放第一次扫描的晚到成功。 */
    fun releaseFirstLateResult() {
        firstRelease.complete(value = Unit)
    }

    /** 释放第二次扫描的正常成功。 */
    fun releaseSecondResult() {
        secondRelease.complete(value = Unit)
    }

    /** 第一次调用收到取消后继续等待，稳定复现复审里的竞态窗口。 */
    private suspend fun awaitFirstRelease() {
        try {
            firstRelease.await()
        } catch (cancellationException: CancellationException) {
            withContext(NonCancellable) {
                firstRelease.await()
            }
        }
    }

    /** 生成可区分来源前缀的发现结果，便于断言最终曲库来自新会话。 */
    private fun buildDiscoveredFiles(
        count: Int,
        prefix: String,
    ): List<MusicFileMetadata> =
        (1..count).map { index: Int ->
            MusicFileMetadata(
                sourceId = "$prefix:$index",
                sourceKind = LocalMusicSourceKind.FakeScanner,
                localUri = "test://restart/$prefix/$index",
                fileName = "$prefix-$index.mp3",
                title = "$prefix track $index",
                artist = "artist $prefix",
                album = "album $prefix",
                durationMs = 180_000L,
                mimeType = "audio/mpeg",
                sizeBytes = 1_000L + index,
                modifiedAt = index.toLong(),
                coverArt = CoverArt.HeroLocalMusic,
            )
        }
}

/**
 * 记录 scanner 收到的本地音频发现偏好，验证 controller 到 scanner 的配置链路。
 */
private class PreferencesRecordingLocalMusicScanner : LocalMusicScanner {
    // 按扫描调用顺序记录偏好快照。
    val preferences: MutableList<LocalMusicDiscoveryPreferences> = mutableListOf()

    /** 默认扫描入口按默认偏好记录，保持接口兼容。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult =
        scan(
            request = request,
            preferences = LocalMusicDiscoveryPreferences(),
        )

    /** 记录偏好后返回 fake 扫描结果。 */
    override suspend fun scan(
        request: LocalMusicScanRequest,
        preferences: LocalMusicDiscoveryPreferences,
    ): LocalMusicScanResult {
        this.preferences += preferences
        return com.yanhao.kmpmusic.data.FakeLocalMusicScanner(demoSongCount = 8).scan(
            request = request,
            preferences = preferences,
        )
    }
}

private class SeededMusicLibraryRepository(
    seedCount: Int,
) : com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository {
    var homePreviewReads: Int = 0
    var fullLibraryReads: Int = 0
    var songsByIdsReads: Int = 0
    private val seededSongs: List<Song> =
        (1..seedCount)
            .map { index ->
                testSong(id = "seed:$index", title = "Seed $index", modifiedAt = index.toLong())
            }.sortedByDescending { song -> song.modifiedAt }

    override fun getSnapshot(): LibrarySnapshot {
        val albums =
            listOf(
                Album(
                    id = "album:album",
                    title = "Album",
                    artist = "Artist",
                    songCount = seededSongs.size,
                    coverArt = CoverArt.HeroLocalMusic,
                    mood = "本地音乐",
                    year = "本地",
                ),
            )
        val artists =
            listOf(
                Artist(
                    id = "artist:artist",
                    name = "Artist",
                    songCount = seededSongs.size,
                    albumCount = 1,
                    coverArt = CoverArt.HeroLocalMusic,
                    tag = "本地音乐",
                ),
            )
        return LibrarySnapshot(
            songs = seededSongs,
            albums = albums,
            artists = artists,
            stats = getLibraryStats(),
            sources = emptyList(),
            scanState = LocalMusicScanState.Idle,
            lastScanSummary = null,
            problems = emptyList(),
        )
    }

    override fun getHomePreview(limit: Int): List<Song> {
        homePreviewReads += 1
        return seededSongs.take(limit)
    }

    override fun getAllAvailableSongs(): List<Song> {
        fullLibraryReads += 1
        return seededSongs
    }

    override fun getAvailableSongsByIds(songIds: List<String>): List<Song> {
        songsByIdsReads += 1
        if (songIds.isEmpty()) {
            return emptyList()
        }
        val requestedIds: Set<String> = songIds.toSet()
        return seededSongs.filter { song -> requestedIds.contains(song.id) }
    }

    override fun getLibraryStats(): LibraryStats = LibraryStats(songCount = seededSongs.size, albumCount = 1, artistCount = 1)

    override fun applyScanResult(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot = getSnapshot()
}

/**
 * 扫描后只暴露正向发现的新歌，用来验证 controller 不会把旧队列随 partial scan 清掉。
 */
private class PositiveOnlyRefreshMusicLibraryRepository : MusicLibraryRepository {
    // 初始曲库歌曲，测试会在扫描前用它们建立播放队列。
    private val initialSongs: List<Song> =
        listOf(
            testSong(id = "partial:old-1", title = "Old One", modifiedAt = 1L),
            testSong(id = "partial:old-2", title = "Old Two", modifiedAt = 2L),
        )

    // 当前仓库快照，扫描后故意只包含新歌来模拟 partial scan 结果。
    private var snapshot: LibrarySnapshot =
        buildSnapshot(
            songs = initialSongs,
            scanState = LocalMusicScanState.Idle,
        )

    /** 返回当前测试快照。 */
    override fun getSnapshot(): LibrarySnapshot = snapshot

    /** 返回首页预览歌曲。 */
    override fun getHomePreview(limit: Int): List<Song> = snapshot.songs.take(n = limit)

    /** 返回全部当前可见歌曲。 */
    override fun getAllAvailableSongs(): List<Song> = snapshot.songs

    /** 按 id 返回当前快照内歌曲。 */
    override fun getAvailableSongsByIds(songIds: List<String>): List<Song> {
        val requestedIds: Set<String> = songIds.toSet()
        return snapshot.songs.filter { song: Song -> requestedIds.contains(element = song.id) }
    }

    /** 返回当前快照统计。 */
    override fun getLibraryStats(): LibraryStats = snapshot.stats

    /** 应用扫描后只保留 positive-only 新歌，放大旧队列误丢风险。 */
    override fun applyScanResult(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot {
        snapshot =
            buildSnapshot(
                songs = listOf(testSong(id = "partial:new", title = "New Partial", modifiedAt = 3L)),
                scanState =
                    LocalMusicScanState.Done(
                        summary =
                            LocalMusicLastScanSummary(
                                addedCount = 1,
                                updatedCount = 0,
                                removedCount = 0,
                                problemCount = 0,
                                completedAt = scanResult.completedAt,
                            ),
                    ),
            )
        return snapshot
    }

    /** 构造测试快照，避免每个方法重复拼装统计。 */
    private fun buildSnapshot(
        songs: List<Song>,
        scanState: LocalMusicScanState,
    ): LibrarySnapshot =
        LibrarySnapshot(
            songs = songs,
            albums = emptyList(),
            artists = emptyList(),
            stats =
                LibraryStats(
                    songCount = songs.size,
                    albumCount = 0,
                    artistCount = 0,
                ),
            sources = emptyList(),
            scanState = scanState,
            lastScanSummary = null,
            problems = emptyList(),
        )
}

/**
 * 返回一首新歌且不声明完成覆盖，表达 positive-only 扫描结果。
 */
private class PositiveOnlyRefreshScanner : LocalMusicScanner {
    /** 返回没有删除权的正向扫描结果。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult =
        LocalMusicScanResult(
            discovered =
                listOf(
                    MusicFileMetadata(
                        sourceId = "new",
                        sourceKind = LocalMusicSourceKind.FakeScanner,
                        localUri = "test://partial/new",
                        fileName = "new.mp3",
                        title = "New Partial",
                        artist = "Artist",
                        album = "Album",
                        durationMs = 180_000L,
                        mimeType = "audio/mpeg",
                        sizeBytes = 1_000L,
                        modifiedAt = 3L,
                        coverArt = CoverArt.HeroLocalMusic,
                    ),
                ),
            completedAt = 1_719_360_005_000L,
        )
}

private class ArtistVariantMusicLibraryRepository : MusicLibraryRepository {
    private val seededSongs: List<Song> =
        listOf(
            testSong(
                id = "variant:1",
                title = "Variant One",
                modifiedAt = 1L,
            ).copy(artist = " JAY   CHOU "),
            testSong(
                id = "variant:2",
                title = "Variant Two",
                modifiedAt = 2L,
            ).copy(artist = "jay chou"),
        )

    override fun getSnapshot(): LibrarySnapshot =
        LibrarySnapshot(
            songs = seededSongs,
            albums = emptyList(),
            artists =
                listOf(
                    Artist(
                        id = "artist:jay chou",
                        name = "Jay Chou",
                        songCount = seededSongs.size,
                        albumCount = 0,
                        coverArt = CoverArt.HeroLocalMusic,
                        tag = "本地音乐",
                    ),
                ),
            stats = getLibraryStats(),
            sources = emptyList(),
            scanState = LocalMusicScanState.Idle,
            lastScanSummary = null,
            problems = emptyList(),
        )

    override fun getHomePreview(limit: Int): List<Song> = seededSongs.take(n = limit)

    override fun getAllAvailableSongs(): List<Song> = seededSongs

    override fun getAvailableSongsByIds(songIds: List<String>): List<Song> {
        val requestedIds: Set<String> = songIds.toSet()
        return seededSongs.filter { song: Song -> requestedIds.contains(element = song.id) }
    }

    override fun getLibraryStats(): LibraryStats =
        LibraryStats(
            songCount = seededSongs.size,
            albumCount = 0,
            artistCount = 1,
        )

    override fun applyScanResult(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot = getSnapshot()
}

private fun testSong(
    id: String,
    title: String,
    modifiedAt: Long,
): Song =
    Song(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        duration = "3:00",
        coverArt = CoverArt.HeroLocalMusic,
        isLiked = false,
        lastPlayed = "未播放",
        quality = "本地 MP3",
        lyric = "本地音频",
        trackNumber = 1,
        durationMs = 180_000L,
        sourceId = id.substringAfter(":"),
        sourceKind = LocalMusicSourceKind.FakeScanner,
        localUri = "fake://$id",
        mimeType = "audio/mpeg",
        sizeBytes = 1_000L,
        modifiedAt = modifiedAt,
    )

/**
 * 记录扫描请求，验证控制器不会在恢复链路主动补发首次扫描。
 */
private class RecordingLocalMusicScanner : LocalMusicScanner {
    // 按调用顺序记录收到的扫描意图。
    val requests: MutableList<LocalMusicScanRequest> = mutableListOf()

    /** 记录请求后直接复用 fake scanner 结果，避免测试依赖 Android 平台实现。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        requests += request
        return com.yanhao.kmpmusic.data
            .FakeLocalMusicScanner(demoSongCount = 8)
            .scan(request = request)
    }
}

/**
 * restoreSnapshot 会故意卡住且忽略取消，用来验证旧恢复晚到时不会再触碰音频引擎。
 */
private class BlockingRestoreSnapshotStore(
    private val delegate: PlaybackSnapshotStore,
) : PlaybackSnapshotStore {
    // 记录恢复读取已经开始，避免测试靠时间猜测竞态窗口。
    private val restoreStarted: CompletableDeferred<Unit> = CompletableDeferred()

    // 显式放行 restoreSnapshot，让失效后的旧恢复继续跑到提交边界。
    private val allowRestoreToFinish: CompletableDeferred<Unit> = CompletableDeferred()

    override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
        delegate.saveSnapshot(snapshot = snapshot)
    }

    override suspend fun hasSavedSnapshot(): Boolean = delegate.hasSavedSnapshot()

    override suspend fun getSavedQueueSongIds(): List<String> = delegate.getSavedQueueSongIds()

    override suspend fun getSavedSnapshotIdentity(): com.yanhao.kmpmusic.domain.model.PlaybackSnapshotIdentity? = delegate.getSavedSnapshotIdentity()

    override suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot {
        restoreStarted.complete(value = Unit)
        withContext(context = NonCancellable) {
            allowRestoreToFinish.await()
        }
        return delegate.restoreSnapshot(availableSongIds = availableSongIds)
    }

    suspend fun awaitRestoreStarted() {
        restoreStarted.await()
    }

    fun releaseRestore() {
        allowRestoreToFinish.complete(value = Unit)
    }
}

/**
 * 记录音频引擎是否真的收到旧恢复提交，直接验证 setQueue/pause/play 的副作用边界。
 */
private class RecordingAudioPlayerEngine : AudioPlayerEngine {
    private val delegate: FakeAudioPlayerEngine = FakeAudioPlayerEngine()

    val setQueueSongIdCalls: MutableList<List<String>> = mutableListOf()
    var playCalls: Int = 0
        private set
    var pauseCalls: Int = 0
        private set

    override val events = delegate.events

    override suspend fun setQueue(
        items: List<com.yanhao.kmpmusic.domain.model.PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        setQueueSongIdCalls += items.map { media -> media.songId }
        delegate.setQueue(
            items = items,
            startIndex = startIndex,
            startPositionMs = startPositionMs,
        )
    }

    override fun play() {
        playCalls += 1
        delegate.play()
    }

    override fun pause() {
        pauseCalls += 1
        delegate.pause()
    }

    override fun seekTo(positionMs: Long) {
        delegate.seekTo(positionMs = positionMs)
    }

    override fun skipToIndex(index: Int) {
        delegate.skipToIndex(index = index)
    }

    override fun setPlaybackMode(playbackMode: PlaybackMode) {
        delegate.setPlaybackMode(playbackMode = playbackMode)
    }

    override fun setVolume(volume: Float) {
        delegate.setVolume(volume = volume)
    }

    override fun stop() {
        delegate.stop()
    }
}

/**
 * 固定权限拒绝场景，避免控制器把平台失败误当成空扫描或 fake 数据。
 */
private class PermissionDeniedScanner : LocalMusicScanner {
    /** 抛出平台无关扫描异常，模拟 Android 用户拒绝音频权限。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult =
        throw LocalMusicScanException(
            error =
                LocalMusicScanError(
                    type = LocalMusicScanErrorType.PermissionDenied,
                    message = "需要音频权限后才能扫描本机歌曲",
                    sourceKind = LocalMusicSourceKind.AndroidMediaStore,
                ),
        )
}

/**
 * 固定权限永久拒绝场景，保证 UI 可以进入系统设置引导分支。
 */
private class PermissionPermanentlyDeniedScanner : LocalMusicScanner {
    /** 抛出永久拒绝错误，模拟 Android 系统不再展示权限弹窗。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult =
        throw LocalMusicScanException(
            error =
                LocalMusicScanError(
                    type = LocalMusicScanErrorType.PermissionPermanentlyDenied,
                    message = "请到系统设置开启音频权限",
                    sourceKind = LocalMusicSourceKind.AndroidMediaStore,
                ),
        )
}

/**
 * 记录扫描次数的永久拒绝 scanner，用于证明确认弹窗不会重复触发系统权限请求。
 */
private class CountingPermissionPermanentlyDeniedScanner : LocalMusicScanner {
    // 扫描调用次数，用户再次点击“打开权限设置”时不应增加。
    var scanCount: Int = 0
        private set

    /** 抛出永久拒绝错误，并记录扫描次数。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        scanCount += 1
        throw LocalMusicScanException(
            error =
                LocalMusicScanError(
                    type = LocalMusicScanErrorType.PermissionPermanentlyDenied,
                    message = "请到系统设置开启音频权限",
                    sourceKind = LocalMusicSourceKind.AndroidMediaStore,
                ),
        )
    }
}

/**
 * 记录系统设置打开次数的假入口，避免测试依赖真实 Android Intent。
 */
private class RecordingPermissionSettingsOpener : PermissionSettingsOpener {
    // 系统设置打开次数，只有用户确认后才应增加。
    var openCount: Int = 0
        private set

    /** 记录一次设置打开动作。 */
    override fun openPermissionSettings() {
        openCount += 1
    }
}

/**
 * 只返回一首 Android MediaStore 歌曲，用来证明 controller 尊重注入数据源。
 */
private class SingleAndroidSongScanner : LocalMusicScanner {
    /** 返回真实平台形态的 content URI 元数据。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult =
        LocalMusicScanResult(
            discovered =
                listOf(
                    MusicFileMetadata(
                        sourceId = "42",
                        sourceKind = LocalMusicSourceKind.AndroidMediaStore,
                        localUri = "content://media/external/audio/media/42",
                        fileName = "device-song.mp3",
                        title = "设备里的歌",
                        artist = "本机歌手",
                        album = "本机专辑",
                        durationMs = 180_000L,
                        mimeType = "audio/mpeg",
                        sizeBytes = 7_200_000L,
                        modifiedAt = 1_719_360_000_000L,
                        coverArt = CoverArt.HeroLocalMusic,
                    ),
                ),
            completedAt = 1_719_360_001_000L,
        )
}
