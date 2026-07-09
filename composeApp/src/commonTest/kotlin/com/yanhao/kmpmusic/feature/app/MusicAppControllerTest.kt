package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.data.InMemoryUserPreferencesRepository
import com.yanhao.kmpmusic.data.FakeAudioPlayerEngine
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.repository.FavoritesRepository
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.repository.SearchHistoryRepository
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.feature.screen.cancelledScanResultDetail
import com.yanhao.kmpmusic.feature.screen.cancelledScanResultTitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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
    fun queueSongsSurviveAfterPlaybackContextIsNoLongerInSongs(): Unit = runTest {
        val controller = createController(controllerScope = backgroundScope)
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val queueSongs = controller.uiState.localSongs.take(4).ifEmpty { controller.uiState.homeLocalSongPreview.take(4) }

        controller.playSong(song = queueSongs[0], queueSongs = queueSongs)

        assertEquals(expected = queueSongs.map { song -> song.id }, actual = controller.uiState.queueSongs.map { song -> song.id })
    }

    /**
     * 查看全部应进入本地音乐二级页，底部 Tab 隐藏但 mini-player 策略保持普通二级页。
     */
    @Test
    fun openLocalMusicUsesSecondaryFixedBarMode(): Unit {
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
    fun openLocalMusicCanStartAtSourcesSection(): Unit {
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
    fun openAudioScanUsesDedicatedScanRoute(): Unit {
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
    fun meViewAllRecentPlayedOpensRecentPageAndReturnsToMe(): Unit {
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
    fun meSongStatOpensHomeSongsAsTopLevelPage(): Unit {
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
     * 扫描页统计必须使用完整曲库总数，不能误用首页预览列表的 6 条限制。
     */
    @Test
    fun audioScanCountUsesLibraryStatsWhenOnlyHomePreviewIsLoaded(): Unit = runBlocking {
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
    fun scanCompletionKeepsCurrentLocalMusicRoute(): Unit = runBlocking {
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
    fun homeAlbumSectionLoadsAllLocalAlbumsWithoutLeavingHome(): Unit {
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
    fun homeArtistSectionLoadsAllLocalArtistsWithoutLeavingHome(): Unit {
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
    fun scanRefreshesHomeAlbumSectionAlbums(): Unit = runBlocking {
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
    fun scanRefreshesHomeArtistSectionArtists(): Unit = runBlocking {
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
    fun scanDoesNotPopulateRecentPlayback(): Unit = runBlocking {
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
    fun positiveOnlyScanKeepsExistingPlaybackQueueSongs(): Unit = runBlocking {
        val repository = PositiveOnlyRefreshMusicLibraryRepository()
        val controller = createController(
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
    fun scanPermissionDeniedKeepsLibraryEmpty(): Unit = runBlocking {
        val controller = createController(
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
    fun scanPermissionPermanentlyDeniedKeepsLibraryEmpty(): Unit = runBlocking {
        val controller = createController(
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
    fun scanPermissionPermanentlyDeniedRequiresUserConfirmationBeforeSettings(): Unit = runBlocking {
        val scanner = CountingPermissionPermanentlyDeniedScanner()
        val opener = RecordingPermissionSettingsOpener()
        val controller = createController(
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
    fun cancelledScanStateIsDistinctFromDoneAndError(): Unit = runBlocking {
        val controller: MusicAppController = createController(
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
    fun cancelledScanStateMapsToCancelledCopy(): Unit = runBlocking {
        val controller: MusicAppController = createController(
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
    fun cancelledScanStateKeepsResultTime(): Unit = runBlocking {
        val controller: MusicAppController = createController(
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
    fun scanEntryDuringRunningScanDoesNotStartSecondScan(): Unit = runTest {
        val scanner: BlockingLocalMusicScanner = BlockingLocalMusicScanner()
        val controller: MusicAppController = createController(
            localMusicScanner = scanner,
            controllerScope = backgroundScope,
        )
        val scanJob: Job = launch {
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        }
        scanner.awaitFirstScanStarted()
        assertEquals(
            expected = "取消扫描",
            actual = renderCancelEntryLabelOrNull(scanState = controller.uiState.scanState),
        )
        val secondScanJob: Job = launch {
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
    fun scanStateSettlesWhenRunningScanCoroutineIsCancelledExternally(): Unit = runTest {
        val scanner: BlockingLocalMusicScanner = BlockingLocalMusicScanner()
        val controller: MusicAppController = createController(
            localMusicScanner = scanner,
            controllerScope = backgroundScope,
        )
        val scanJob: Job = launch {
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
     * 重新扫描期间应保留上一轮结果摘要，扫描页统计不能回退成未记录时间。
     */
    @Test
    fun runningScanKeepsPreviousLastScanSummary(): Unit = runTest {
        val scanner: BlockingAfterFirstScanScanner = BlockingAfterFirstScanScanner()
        val controller: MusicAppController = createController(
            localMusicScanner = scanner,
            controllerScope = backgroundScope,
        )
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val previousSummary: LocalMusicLastScanSummary = (controller.uiState.scanState as LocalMusicScanState.Done).summary

        val scanJob: Job = launch {
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
    fun localMusicDiscoveryPreferencesPersistAndFlowIntoScanner(): Unit = runBlocking {
        val preferencesRepository: InMemoryUserPreferencesRepository = InMemoryUserPreferencesRepository()
        val scanner: PreferencesRecordingLocalMusicScanner = PreferencesRecordingLocalMusicScanner()
        val controller: MusicAppController = createController(
            localMusicScanner = scanner,
            userPreferencesRepository = preferencesRepository,
        )

        controller.setLocalMusicAutoScanOnLaunchEnabled(isEnabled = true)
        controller.setLocalMusicShortAudioIgnored(isIgnored = false)
        controller.setLocalMusicSystemFoldersExcluded(isExcluded = false)
        val restoredController: MusicAppController = createController(
            localMusicScanner = scanner,
            userPreferencesRepository = preferencesRepository,
        )
        restoredController.scanLocalMusic(request = LocalMusicScanRequest.Refresh)

        val expectedPreferences = LocalMusicDiscoveryPreferences(
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
    fun systemBackClosesPermissionSettingsDialog(): Unit {
        val controller = createController()
        controller.openPermissionSettingsDialog()
        assertTrue(controller.uiState.canHandleSystemBack)
        assertTrue(controller.handleSystemBack())
        assertFalse(controller.uiState.isPermissionSettingsDialogOpen)
    }

    /**
     * 控制器必须消费注入的 scanner，避免 Android 入口无意落回 common fake 数据。
     */
    @Test
    fun scanUsesInjectedScannerData(): Unit = runBlocking {
        val controller = createController(
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
    fun playSongAddsRecentPlayback(): Unit = runBlocking {
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
    fun playSongOutsideHomePreviewAddsRecentPlayback(): Unit = runBlocking {
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
    fun recentPlaybackKeepsAllPlayedSongs(): Unit = runBlocking {
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
    fun playRecentSummarySongUsesFullRecentQueueWithClickedStart(): Unit = runTest {
        val playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
        val controller: MusicAppController = createController(
            playbackRepository = playbackRepository,
            controllerScope = backgroundScope,
        )
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val recentSongIds: List<String> = controller.uiState.homeLocalSongPreview
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
    fun playRecentPageSongUsesFullRecentQueueWithClickedStart(): Unit = runTest {
        val playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
        val controller: MusicAppController = createController(
            playbackRepository = playbackRepository,
            controllerScope = backgroundScope,
        )
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val recentSongIds: List<String> = controller.uiState.homeLocalSongPreview
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
     * 桌面最近播放摘要和完整页共用 [MusicAppController.playRecentSong]，
     * 因此点击 Top3 内歌曲也必须读取完整最近播放队列。
     */
    @Test
    fun playDesktopRecentSongUsesFullRecentQueueWithClickedStart(): Unit = runTest {
        val playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
        val controller: MusicAppController = createController(
            playbackRepository = playbackRepository,
            controllerScope = backgroundScope,
        )
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val recentSongIds: List<String> = controller.uiState.homeLocalSongPreview
            .take(n = 5)
            .map { song: Song -> song.id }
        playbackRepository.savePlaybackHistory(
            history = PlaybackHistory(
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
    fun clearRecentPlaybackHistoryRemovesVisibleAndStoredHistory(): Unit = runBlocking {
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
    fun desktopRailRootNavigationClearsSecondaryScreen(): Unit {
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
    fun playerScreenAndBottomPlayerReadSamePlaybackState(): Unit = runBlocking {
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
    fun playerScreenAndBottomPlayerReadSamePlaybackVolume(): Unit {
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
    fun setVolumeCoercesSharedUiStateAndEngineVolume(): Unit {
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
    fun playSongUpdatesPlaybackAndQueue(): Unit = runBlocking {
        val controller = createController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val targetSong: Song = controller.uiState.homeLocalSongPreview.first { song ->
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
    fun playSongUsesProvidedQueueSongs(): Unit = runTest {
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
    fun playSongDoesNotNavigateToPlayer(): Unit = runTest {
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
    fun playSongWithoutProvidedQueueKeepsCurrentQueueWhenSongExists(): Unit = runTest {
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
    fun cyclePlaybackModeUpdatesUiState(): Unit = runTest {
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
    fun playSongRecordsPlaybackHistory(): Unit = runBlocking {
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
    fun moveTrackDoesNotUseSongsAsImplicitQueue(): Unit {
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
    fun moveTrackChangesCurrentSong(): Unit = runBlocking {
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
    fun removeCurrentSongKeepsEngineQueueInSync(): Unit = runTest {
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
    fun restorePlaybackSnapshotAllowsResume(): Unit = runTest {
        val snapshotStore = InMemoryPlaybackSnapshotStore()
        val controller = createController(
            playbackSnapshotStore = snapshotStore,
            controllerScope = backgroundScope,
        )
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val queueSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 3)
        val restoredSong: Song = queueSongs[1]
        snapshotStore.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = restoredSong.id,
                    status = PlaybackStatus.Playing,
                    positionMs = 42_000L,
                    durationMs = restoredSong.durationMs,
                ),
                queueState = QueueState(
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
    fun restorePlaybackSnapshotRestoresSavedSongOutsidePreviewWithoutFullLibraryLoad(): Unit = runTest {
        val repository = SeededMusicLibraryRepository(seedCount = 8)
        val snapshotStore = InMemoryPlaybackSnapshotStore()
        snapshotStore.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = "seed:2",
                    status = PlaybackStatus.Playing,
                    positionMs = 24_000L,
                    durationMs = 180_000L,
                ),
                queueState = QueueState(
                    songIds = listOf("seed:2", "seed:1"),
                    currentIndex = 0,
                    playbackMode = PlaybackMode.LoopAll,
                ),
            ),
        )
        val controller = createController(
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
    fun restorePlaybackSnapshotRestoresAfterLibraryLoads(): Unit = runTest {
        val snapshotStore = InMemoryPlaybackSnapshotStore()
        val controller = createController(
            playbackSnapshotStore = snapshotStore,
            controllerScope = backgroundScope,
        )
        snapshotStore.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = "fakeScanner:004",
                    status = PlaybackStatus.Playing,
                    positionMs = 24_000L,
                    durationMs = 247_000L,
                ),
                queueState = QueueState(
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
    fun restorePlaybackSnapshotDoesNotAutoScanWhenLibraryIsEmpty(): Unit = runTest {
        val snapshotStore = InMemoryPlaybackSnapshotStore()
        val scanner = RecordingLocalMusicScanner()
        val controller = createController(
            localMusicScanner = scanner,
            playbackSnapshotStore = snapshotStore,
            controllerScope = backgroundScope,
        )
        snapshotStore.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = "fakeScanner:004",
                    status = PlaybackStatus.Playing,
                    positionMs = 24_000L,
                    durationMs = 247_000L,
                ),
                queueState = QueueState(
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
     * 收藏歌曲应独立于 localSongs 是否已加载，只要喜欢列表里有 id，就应能先补齐实体，再按需进入详情。
     */
    @Test
    fun favoriteSongsRemainAvailableBeforeFullLibraryLoads(): Unit {
        val repository = SeededMusicLibraryRepository(seedCount = 8)
        val controller = createController(
            musicLibraryRepository = repository,
            favoritesRepository = InMemoryFavoritesRepository(
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
    fun favoriteStressToggleUsesLoadedLibraryWithoutRepeatedIdLookup(): Unit = runTest {
        val repository = SeededMusicLibraryRepository(seedCount = 500)
        val controller = createController(
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
    fun defaultFakeScannerSeedsFiveHundredFavoriteSongsForStress(): Unit = runBlocking {
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
    fun openArtistFromSongUsesNormalizedArtistName(): Unit {
        val controller = createController(
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
    fun toggleFavoriteSyncsSongList(): Unit = runBlocking {
        val controller = createController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val targetSong: Song = controller.uiState.homeLocalSongPreview.first { song -> song.title == "Summer Waltz" }
        controller.toggleFavorite(songId = targetSong.id)
        assertTrue(controller.uiState.likedSongIds.contains(targetSong.id))
        assertTrue(controller.uiState.homeLocalSongPreview.first { song -> song.id == targetSong.id }.isLiked)
    }

    /**
     * 平台宿主应能通过共享控制器直接切换当前播放歌曲收藏，而不必窥探 [MusicAppUiState.currentSongId]。
     */
    @Test
    fun toggleCurrentSongFavoriteUsesSharedControllerEntry(): Unit = runBlocking {
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
    fun searchScopeLimitsResultTypes(): Unit = runBlocking {
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
    fun searchReadsScannedSnapshot(): Unit = runBlocking {
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
    fun localLibrarySearchReturnsNonFavoriteLocalSongs(): Unit = runBlocking {
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
    fun favoritesSearchOnlyReturnsFavoriteSongs(): Unit = runBlocking {
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
        assertEquals(
            expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
            actual = controller.uiState.navigationState.fixedBarMode,
        )
        assertFalse(actual = controller.uiState.navigationState.fixedBarMode.showsBottomNavigation)
    }

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

    /**
     * 搜索入口应按需加载完整曲库，而不是只搜索首页 preview。
     */
    @Test
    fun searchLoadsFullLibraryInsteadOfHomePreviewOnly(): Unit {
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
    fun pendingSearchQueryDoesNotReturnFullLibraryBeforeDebounce(): Unit = runTest {
        val repository = SeededMusicLibraryRepository(seedCount = 8)
        val controller = createController(
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
    fun debouncedSearchQueryPublishesActiveQueryThroughFacade(): Unit = runTest {
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
    fun debouncedSearchUpdatePreservesPlaybackStateChangedBeforeDebounce(): Unit = runTest {
        val controller = createController(controllerScope = backgroundScope)
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        advanceUntilIdle()
        val queueSongs: List<Song> = controller.uiState.localSongs.take(n = 3)
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
    fun nonBlankSearchQueryDoesNotCommitToHistoryWhenLeavingSearchBeforeDebounce(): Unit = runTest {
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
    fun searchQueryWithoutResultsCommitsToHistoryAfterExplicitSubmit(): Unit = runBlocking {
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
    fun searchResultActionsCommitCurrentQueryToHistory(): Unit = runTest {
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
            expected = listOf(
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
    fun nonSearchResultActionsDoNotCommitSearchHistory(): Unit = runBlocking {
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
    fun rootNavigationClearsSecondaryScreen(): Unit {
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
    fun openPlayerUsesFullscreenSecondaryScreen(): Unit {
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
    fun aboutScreenCoversSettingsWithoutChangingUnderlayChrome(): Unit {
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
    fun audioScanScreenCoversSettingsWithoutChangingUnderlayChrome(): Unit {
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
    fun rootScrollStateKeyStaysStableAfterSecondaryReturn(): Unit {
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
    fun secondaryScrollStateKeyChangesForEachEntry(): Unit {
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
    fun systemBackReturnsFromSecondaryScreen(): Unit = runBlocking {
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
    fun systemBackClosesOverlayBeforeSecondaryScreen(): Unit {
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
    playbackSnapshotStore: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore(),
    favoritesRepository: FavoritesRepository? = null,
    userPreferencesRepository: UserPreferencesRepository = InMemoryUserPreferencesRepository(),
    searchHistoryRepository: SearchHistoryRepository = FakeSearchHistoryRepository(),
    permissionSettingsOpener: PermissionSettingsOpener = PermissionSettingsOpener {},
    controllerScope: CoroutineScope = testControllerScope(),
    searchQueryDebounceMillis: Long = 300L,
): MusicAppController {
    return MusicAppController(
        musicLibraryRepository = musicLibraryRepository,
        localMusicScanner = localMusicScanner,
        playbackRepository = playbackRepository,
        audioPlayerEngine = audioPlayerEngine,
        playbackSnapshotStore = playbackSnapshotStore,
        injectedFavoritesRepository = favoritesRepository,
        userPreferencesRepository = userPreferencesRepository,
        searchHistoryRepository = searchHistoryRepository,
        permissionSettingsOpener = permissionSettingsOpener,
        controllerScope = controllerScope,
        searchQueryDebounceMillis = searchQueryDebounceMillis,
    )
}

private class FakeSearchHistoryRepository : SearchHistoryRepository {
    // 测试用内存表，按上下文隔离搜索词。
    private val histories: MutableMap<SearchContext, List<String>> = mutableMapOf()

    /** 读取指定上下文的历史。 */
    override fun getSearchHistory(context: SearchContext): List<String> {
        return histories[context].orEmpty()
    }

    /** 保存指定上下文的历史。 */
    override fun saveSearchHistory(context: SearchContext, history: List<String>) {
        histories[context] = history
    }
}

private fun testControllerScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
}

private object FakeControllerLocalMusicScanner : LocalMusicScanner {
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        return com.yanhao.kmpmusic.data.FakeLocalMusicScanner(demoSongCount = 8).scan(request = request)
    }

    /** 转发本地音频发现偏好，避免测试默认 scanner 丢掉过滤配置。 */
    override suspend fun scan(
        request: LocalMusicScanRequest,
        preferences: LocalMusicDiscoveryPreferences,
    ): LocalMusicScanResult {
        return com.yanhao.kmpmusic.data.FakeLocalMusicScanner(demoSongCount = 8).scan(
            request = request,
            preferences = preferences,
        )
    }
}

// 将运行中的扫描入口映射为取消动作文案，避免二次点击被误当成重新扫描。
private fun renderCancelEntryLabelOrNull(scanState: LocalMusicScanState): String? {
    return when (scanState) {
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
}

// 通过函数边界避免测试在已确认取消态后触发 sealed smart-cast 恒假警告。
private fun isDoneScanState(scanState: LocalMusicScanState): Boolean {
    return scanState is LocalMusicScanState.Done
}

// 通过函数边界保留“取消不是失败”的用户可感知契约。
private fun isErrorScanState(scanState: LocalMusicScanState): Boolean {
    return scanState is LocalMusicScanState.Error
}

/**
 * 模拟平台 scanner 把用户取消作为独立可识别错误上报。
 */
private class UserCancelledScanner : LocalMusicScanner {
    /** 抛出用户取消错误，驱动控制器进入未来的取消结果态。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        throw LocalMusicScanException(
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.UserCancelled,
                message = "用户取消了本地音乐扫描",
                sourceKind = LocalMusicSourceKind.FakeScanner,
            ),
        )
    }
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
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        return scan(
            request = request,
            preferences = LocalMusicDiscoveryPreferences(),
        )
    }

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
 * 记录 scanner 收到的本地音频发现偏好，验证 controller 到 scanner 的配置链路。
 */
private class PreferencesRecordingLocalMusicScanner : LocalMusicScanner {
    // 按扫描调用顺序记录偏好快照。
    val preferences: MutableList<LocalMusicDiscoveryPreferences> = mutableListOf()

    /** 默认扫描入口按默认偏好记录，保持接口兼容。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        return scan(
            request = request,
            preferences = LocalMusicDiscoveryPreferences(),
        )
    }

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

private class SeededMusicLibraryRepository(seedCount: Int) : com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository {
    var homePreviewReads: Int = 0
    var fullLibraryReads: Int = 0
    var songsByIdsReads: Int = 0
    private val seededSongs: List<Song> = (1..seedCount).map { index ->
        testSong(id = "seed:$index", title = "Seed $index", modifiedAt = index.toLong())
    }.sortedByDescending { song -> song.modifiedAt }

    override fun getSnapshot(): LibrarySnapshot {
        val albums = listOf(
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
        val artists = listOf(
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

    override fun getLibraryStats(): LibraryStats {
        return LibraryStats(songCount = seededSongs.size, albumCount = 1, artistCount = 1)
    }

    override fun applyScanResult(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot {
        return getSnapshot()
    }
}

/**
 * 扫描后只暴露正向发现的新歌，用来验证 controller 不会把旧队列随 partial scan 清掉。
 */
private class PositiveOnlyRefreshMusicLibraryRepository : MusicLibraryRepository {
    // 初始曲库歌曲，测试会在扫描前用它们建立播放队列。
    private val initialSongs: List<Song> = listOf(
        testSong(id = "partial:old-1", title = "Old One", modifiedAt = 1L),
        testSong(id = "partial:old-2", title = "Old Two", modifiedAt = 2L),
    )

    // 当前仓库快照，扫描后故意只包含新歌来模拟 partial scan 结果。
    private var snapshot: LibrarySnapshot = buildSnapshot(
        songs = initialSongs,
        scanState = LocalMusicScanState.Idle,
    )

    /** 返回当前测试快照。 */
    override fun getSnapshot(): LibrarySnapshot {
        return snapshot
    }

    /** 返回首页预览歌曲。 */
    override fun getHomePreview(limit: Int): List<Song> {
        return snapshot.songs.take(n = limit)
    }

    /** 返回全部当前可见歌曲。 */
    override fun getAllAvailableSongs(): List<Song> {
        return snapshot.songs
    }

    /** 按 id 返回当前快照内歌曲。 */
    override fun getAvailableSongsByIds(songIds: List<String>): List<Song> {
        val requestedIds: Set<String> = songIds.toSet()
        return snapshot.songs.filter { song: Song -> requestedIds.contains(element = song.id) }
    }

    /** 返回当前快照统计。 */
    override fun getLibraryStats(): LibraryStats {
        return snapshot.stats
    }

    /** 应用扫描后只保留 positive-only 新歌，放大旧队列误丢风险。 */
    override fun applyScanResult(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot {
        snapshot = buildSnapshot(
            songs = listOf(testSong(id = "partial:new", title = "New Partial", modifiedAt = 3L)),
            scanState = LocalMusicScanState.Done(
                summary = LocalMusicLastScanSummary(
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
    ): LibrarySnapshot {
        return LibrarySnapshot(
            songs = songs,
            albums = emptyList(),
            artists = emptyList(),
            stats = LibraryStats(
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
}

/**
 * 返回一首新歌且不声明完成覆盖，表达 positive-only 扫描结果。
 */
private class PositiveOnlyRefreshScanner : LocalMusicScanner {
    /** 返回没有删除权的正向扫描结果。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        return LocalMusicScanResult(
            discovered = listOf(
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
}

private class ArtistVariantMusicLibraryRepository : MusicLibraryRepository {
    private val seededSongs: List<Song> = listOf(
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

    override fun getSnapshot(): LibrarySnapshot {
        return LibrarySnapshot(
            songs = seededSongs,
            albums = emptyList(),
            artists = listOf(
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
    }

    override fun getHomePreview(limit: Int): List<Song> {
        return seededSongs.take(n = limit)
    }

    override fun getAllAvailableSongs(): List<Song> {
        return seededSongs
    }

    override fun getAvailableSongsByIds(songIds: List<String>): List<Song> {
        val requestedIds: Set<String> = songIds.toSet()
        return seededSongs.filter { song: Song -> requestedIds.contains(element = song.id) }
    }

    override fun getLibraryStats(): LibraryStats {
        return LibraryStats(
            songCount = seededSongs.size,
            albumCount = 0,
            artistCount = 1,
        )
    }

    override fun applyScanResult(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot {
        return getSnapshot()
    }
}

private fun testSong(id: String, title: String, modifiedAt: Long): Song {
    return Song(
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
}

/**
 * 记录扫描请求，验证控制器不会在恢复链路主动补发首次扫描。
 */
private class RecordingLocalMusicScanner : LocalMusicScanner {
    // 按调用顺序记录收到的扫描意图。
    val requests: MutableList<LocalMusicScanRequest> = mutableListOf()

    /** 记录请求后直接复用 fake scanner 结果，避免测试依赖 Android 平台实现。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        requests += request
        return com.yanhao.kmpmusic.data.FakeLocalMusicScanner(demoSongCount = 8).scan(request = request)
    }
}

/**
 * 固定权限拒绝场景，避免控制器把平台失败误当成空扫描或 fake 数据。
 */
private class PermissionDeniedScanner : LocalMusicScanner {
    /** 抛出平台无关扫描异常，模拟 Android 用户拒绝音频权限。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        throw LocalMusicScanException(
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.PermissionDenied,
                message = "需要音频权限后才能扫描本机歌曲",
                sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            ),
        )
    }
}

/**
 * 固定权限永久拒绝场景，保证 UI 可以进入系统设置引导分支。
 */
private class PermissionPermanentlyDeniedScanner : LocalMusicScanner {
    /** 抛出永久拒绝错误，模拟 Android 系统不再展示权限弹窗。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        throw LocalMusicScanException(
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.PermissionPermanentlyDenied,
                message = "请到系统设置开启音频权限",
                sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            ),
        )
    }
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
            error = LocalMusicScanError(
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
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        return LocalMusicScanResult(
            discovered = listOf(
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
}
