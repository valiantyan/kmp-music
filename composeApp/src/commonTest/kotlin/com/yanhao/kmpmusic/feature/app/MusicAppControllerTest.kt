package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
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
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.repository.FavoritesRepository
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.repository.SearchHistoryRepository
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
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
        assertTrue(controller.uiState.localSongs.isEmpty())
        assertEquals(expected = 6, actual = controller.uiState.homeLocalSongPreview.size)
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
     * 搜索输入每次变化都会重新派生结果，但不能因此反复读取持久层完整曲库。
     */
    @Test
    fun repeatedSearchQueryChangesReuseLoadedLocalSongs(): Unit = runTest {
        val repository = SeededMusicLibraryRepository(seedCount = 8)
        val controller = createController(
            musicLibraryRepository = repository,
            controllerScope = backgroundScope,
        )

        controller.openSearch(context = SearchContext.LocalLibrary)
        controller.setSearchScope(scope = SearchScope.Songs)
        listOf("a", "as", "asf", "asfa", "asfasfasdffsadfasdf").forEach { query: String ->
            controller.setSearchQuery(query = query)
            controller.search()
        }

        assertEquals(expected = 1, actual = repository.fullLibraryReads)
        assertEquals(expected = 8, actual = controller.search().songs.size)
        advanceTimeBy(delayTimeMillis = 301L)
        advanceUntilIdle()
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
     * 非空搜索词在离开搜索页时应自动写入历史，避免各平台 UI 自己补提交逻辑。
     */
    @Test
    fun nonBlankSearchQueryCommitsToHistoryWhenLeavingSearch(): Unit = runBlocking {
        val controller = createController()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)

        controller.openSearch(context = SearchContext.LocalLibrary)
        controller.setSearchQuery(query = "One Summer")

        controller.navigateBack()
        controller.openSearch(context = SearchContext.LocalLibrary)

        assertEquals(
            expected = listOf("One Summer"),
            actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
        )
    }

    /**
     * 搜索行为记录不依赖结果命中，用户搜过的无结果关键词也应能回到历史里。
     */
    @Test
    fun searchQueryWithoutResultsCommitsToHistoryWhenLeavingSearch(): Unit = runBlocking {
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

}

private fun createController(
    musicLibraryRepository: MusicLibraryRepository = InMemoryMusicLibraryRepository(),
    localMusicScanner: LocalMusicScanner = FakeControllerLocalMusicScanner,
    playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository(),
    audioPlayerEngine: AudioPlayerEngine = FakeAudioPlayerEngine(),
    playbackSnapshotStore: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore(),
    favoritesRepository: FavoritesRepository? = null,
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
        return com.yanhao.kmpmusic.data.FakeLocalMusicScanner().scan(request = request)
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
