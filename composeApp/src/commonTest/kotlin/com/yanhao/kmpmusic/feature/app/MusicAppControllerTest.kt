package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
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
    /**
     * App 启动后不能把 seed 曲库或扫描结果预填成真实播放状态。
     */
    @Test
    fun initialStateHasNoPlaybackBeforeUserAction(): Unit {
        val controller = createController()
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
        val controller = createController()
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
     * 扫描完成只应填充本地歌曲预览，不应把扫描结果冒充最近播放。
     */
    @Test
    fun scanDoesNotPopulateRecentPlayback(): Unit = runBlocking {
        val controller = createController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        assertEquals(expected = 8, actual = controller.uiState.songs.size)
        assertEquals(expected = 6, actual = controller.uiState.localSongPreview.size)
        assertEquals(expected = 8, actual = controller.uiState.libraryStats.songCount)
        assertTrue(controller.uiState.localMusicSources.isNotEmpty())
        assertTrue(controller.uiState.recentSongs.isEmpty())
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
        assertTrue(controller.uiState.songs.isEmpty())
        assertTrue(controller.uiState.localSongPreview.isEmpty())
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
        assertTrue(controller.uiState.songs.isEmpty())
        assertTrue(controller.uiState.localSongPreview.isEmpty())
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
        assertEquals(expected = listOf("设备里的歌"), actual = controller.uiState.songs.map { song -> song.title })
        assertTrue(controller.uiState.songs.all { song -> song.localUri.startsWith(prefix = "content://") })
        assertTrue(controller.uiState.songs.none { song -> song.sourceKind == LocalMusicSourceKind.FakeScanner })
    }

    /**
     * 用户真正播放歌曲后，最近播放才出现该歌曲。
     */
    @Test
    fun playSongAddsRecentPlayback(): Unit = runBlocking {
        val controller = createController()
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
        val controller = createController()
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
        val controller = createController()
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
     * 从列表点击歌曲时，应把当前列表完整写成播放队列，而不是偷偷回退为单曲队列。
     */
    @Test
    fun playSongUsesProvidedQueueSongs(): Unit = runTest {
        val controller = createController(controllerScope = backgroundScope)
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val queueSongs = controller.uiState.songs
        val targetSong = queueSongs[3]

        controller.playSong(song = targetSong, queueSongs = queueSongs)

        assertEquals(expected = queueSongs.map { song -> song.id }, actual = controller.uiState.queueSongIds)
        assertEquals(expected = targetSong.id, actual = controller.uiState.currentSongId)
    }

    /**
     * 队列弹层或系统入口只给歌曲时，应复用当前显式队列，避免退化成单曲队列。
     */
    @Test
    fun playSongWithoutProvidedQueueKeepsCurrentQueueWhenSongExists(): Unit = runTest {
        val controller: MusicAppController = createController(controllerScope = backgroundScope)
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val queueSongs: List<Song> = controller.uiState.songs.take(n = 4)
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
        val queueSongs: List<Song> = controller.uiState.songs.take(n = 2)
        controller.playSong(song = queueSongs[0], queueSongs = queueSongs)
        val originalSongId: String? = controller.uiState.currentSongId
        controller.moveTrack(direction = 1)
        assertNotEquals(originalSongId, controller.uiState.currentSongId)
        assertTrue(controller.uiState.isPlaying)
    }

    /**
     * 删除当前歌曲后，下一次切歌应依据剩余队列推进，而不是命中引擎里残留的旧队列。
     */
    @Test
    fun removeCurrentSongKeepsEngineQueueInSync(): Unit = runTest {
        val controller = createController(controllerScope = backgroundScope)
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val queueSongs: List<Song> = controller.uiState.songs.take(n = 3)

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
        val queueSongs: List<Song> = controller.uiState.songs.take(n = 3)
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
     * 启动时若曲库为空但存在快照，控制器应主动补发首次扫描并自动回填暂停态。
     */
    @Test
    fun restorePlaybackSnapshotHydratesLibraryBeforeRestoring(): Unit = runTest {
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
     * 冷启动恢复遇到空曲库时，只要存在快照就应主动触发首次扫描，而不是等待用户手动刷新。
     */
    @Test
    fun restorePlaybackSnapshotAutoScansLibraryWhenSnapshotExists(): Unit = runTest {
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

        assertEquals(
            expected = listOf<LocalMusicScanRequest>(LocalMusicScanRequest.InitialScan),
            actual = scanner.requests,
        )
        assertEquals(expected = "fakeScanner:004", actual = controller.uiState.currentSongId)
        assertEquals(expected = PlaybackStatus.Paused, actual = controller.uiState.playbackStatus)
        assertEquals(expected = 24_000L, actual = controller.uiState.playbackPositionMs)
    }

    /**
     * 收藏状态应同时同步到集合和歌曲列表。
     */
    @Test
    fun toggleFavoriteSyncsSongList(): Unit = runBlocking {
        val controller = createController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val targetSong: Song = controller.uiState.songs.first { song -> song.title == "Summer Waltz" }
        controller.toggleFavorite(songId = targetSong.id)
        assertTrue(controller.uiState.likedSongIds.contains(targetSong.id))
        assertTrue(controller.uiState.songs.first { song -> song.id == targetSong.id }.isLiked)
    }

    /**
     * 平台宿主应能通过共享控制器直接切换当前播放歌曲收藏，而不必窥探 [MusicAppUiState.currentSongId]。
     */
    @Test
    fun toggleCurrentSongFavoriteUsesSharedControllerEntry(): Unit = runBlocking {
        val controller = createController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        val targetSong: Song = controller.uiState.songs.first { song -> song.title == "Summer Waltz" }
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
        controller.setSearchScope(scope = SearchScope.Songs)
        assertEquals(
            expected = listOf("One Summer's Day"),
            actual = controller.search().songs.map { song -> song.title },
        )
    }

    /**
     * 我的页统计应来自同一份曲库快照。
     */
    @Test
    fun libraryStatsComeFromScannedSnapshot(): Unit = runBlocking {
        val controller = createController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        assertEquals(expected = 8, actual = controller.uiState.libraryStats.songCount)
        assertEquals(
            expected = controller.uiState.albums.size,
            actual = controller.uiState.libraryStats.albumCount,
        )
        assertEquals(
            expected = controller.uiState.artists.size,
            actual = controller.uiState.libraryStats.artistCount,
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
            expected = AppChromeMode.SecondaryFullscreen,
            actual = controller.uiState.navigationState.chromeMode,
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

    /**
     * 页面 chrome 策略应区分一级页、普通二级页和全屏二级页。
     */
    @Test
    fun navigationStateProvidesChromeMode(): Unit {
        val controller = createController()
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

private fun createController(
    localMusicScanner: LocalMusicScanner = FakeControllerLocalMusicScanner,
    playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository(),
    playbackSnapshotStore: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore(),
    permissionSettingsOpener: PermissionSettingsOpener = PermissionSettingsOpener {},
    controllerScope: CoroutineScope = testControllerScope(),
): MusicAppController {
    return MusicAppController(
        localMusicScanner = localMusicScanner,
        playbackRepository = playbackRepository,
        playbackSnapshotStore = playbackSnapshotStore,
        permissionSettingsOpener = permissionSettingsOpener,
        controllerScope = controllerScope,
    )
}

private fun testControllerScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
}

private object FakeControllerLocalMusicScanner : LocalMusicScanner {
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        return com.yanhao.kmpmusic.data.FakeLocalMusicScanner().scan(request = request)
    }
}

/**
 * 记录扫描请求，验证控制器是否在恢复链路主动补发首次扫描。
 */
private class RecordingLocalMusicScanner : LocalMusicScanner {
    // 按调用顺序记录收到的扫描意图。
    val requests: MutableList<LocalMusicScanRequest> = mutableListOf()

    /** 记录请求后直接复用 fake scanner 结果，避免测试依赖 Android 平台实现。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        requests += request
        return com.yanhao.kmpmusic.data.FakeLocalMusicScanner().scan(request = request)
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
