package com.yanhao.kmpmusic.feature.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yanhao.kmpmusic.data.FakeAudioPlayerEngine
import com.yanhao.kmpmusic.data.FakeLocalMusicScanner
import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.data.InMemorySearchHistoryRepository
import com.yanhao.kmpmusic.data.InMemoryUserPreferencesRepository
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicScanProgress
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.playback.PlaybackCoordinator
import com.yanhao.kmpmusic.domain.repository.FavoritesRepository
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import com.yanhao.kmpmusic.domain.repository.SearchHistoryRepository
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCase
import com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCase
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.buildSearchResult
import com.yanhao.kmpmusic.feature.app.favorites.FavoriteStateSynchronizer
import com.yanhao.kmpmusic.feature.app.library.LibraryStateSynchronizer
import com.yanhao.kmpmusic.feature.app.library.MusicLibraryProjector
import com.yanhao.kmpmusic.feature.app.navigation.NavigationStateController
import com.yanhao.kmpmusic.feature.app.playback.PlaybackRestoreOrchestrator
import com.yanhao.kmpmusic.feature.app.playback.PlaybackUiStateSynchronizer
import com.yanhao.kmpmusic.feature.app.search.SearchSessionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

private const val DEFAULT_SEARCH_QUERY_DEBOUNCE_MILLIS = 300L

/**
 * App 状态控制器，替代原型中的 React `useState` 集群。
 */
class MusicAppController(
    private val musicLibraryRepository: MusicLibraryRepository = InMemoryMusicLibraryRepository(),
    private val localMusicScanner: LocalMusicScanner = FakeLocalMusicScanner(),
    private val playbackRepository: PlaybackRepository = InMemoryPlaybackRepository(),
    private val audioPlayerEngine: AudioPlayerEngine = FakeAudioPlayerEngine(),
    private val playbackSnapshotStore: PlaybackSnapshotStore = InMemoryPlaybackSnapshotStore(),
    private val injectedFavoritesRepository: FavoritesRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository = InMemoryUserPreferencesRepository(),
    private val searchHistoryRepository: SearchHistoryRepository = InMemorySearchHistoryRepository(),
    private val permissionSettingsOpener: PermissionSettingsOpener = PermissionSettingsOpener {},
    private val controllerScope: CoroutineScope,
    private val nowMillis: () -> Long = { 0L },
    private val searchQueryDebounceMillis: Long = DEFAULT_SEARCH_QUERY_DEBOUNCE_MILLIS,
) {
    // 收藏仓库需要依赖初始歌曲，所以在控制器中初始化。
    private val favoritesRepository: FavoritesRepository

    // 切换收藏用例。
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase

    // 曲库状态同步器承接扫描、加载和共享列表推导，facade 只保留状态所有权与时序控制。
    private val libraryStateSynchronizer: LibraryStateSynchronizer

    // 收藏状态同步器统一收藏切换后的列表投影，facade 只保留入口与状态发布。
    private val favoriteStateSynchronizer: FavoriteStateSynchronizer

    // 播放状态投影由专用同步器统一负责，facade 只保留状态所有权与观察者发布。
    private val playbackUiStateSynchronizer: PlaybackUiStateSynchronizer

    // 冷启动恢复由编排器统一管理依赖顺序，避免 facade 混杂实体解析与恢复时序。
    private val playbackRestoreOrchestrator: PlaybackRestoreOrchestrator

    // 本地扫描用例。
    private val scanLocalMusicUseCase: ScanLocalMusicUseCase = ScanLocalMusicUseCaseImpl(
        localMusicScanner = localMusicScanner,
        musicLibraryRepository = musicLibraryRepository,
    )

    // 播放协调器统一托管运行时播放、队列和快照写入。
    private val playbackCoordinator: PlaybackCoordinator = PlaybackCoordinator(
        playbackRepository = playbackRepository,
        audioPlayerEngine = audioPlayerEngine,
        playbackSnapshotStore = playbackSnapshotStore,
        nowMillis = nowMillis,
    )

    // 播放 UI 刷新观察者，供平台通知或其他宿主订阅共享状态。
    private var playbackUiObserver: (MusicAppUiState) -> Unit = {}

    // 冷启动恢复请求在曲库尚未准备好时先挂起，等扫描结果到位后再真正执行。
    private var isPlaybackRestorePending: Boolean = false

    // 搜索会话子控制器负责搜索输入态、防抖和历史 reducer。
    private val searchSessionController: SearchSessionController = SearchSessionController(
        searchHistoryRepository = searchHistoryRepository,
        controllerScope = controllerScope,
        debounceMillis = searchQueryDebounceMillis,
        publishStateUpdate = { reducer: (MusicAppUiState) -> MusicAppUiState ->
            uiState = reducer(uiState)
        },
    )

    /**
     * Compose 可观察 UI 状态。
     */
    var uiState: MusicAppUiState by mutableStateOf(
        MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        ),
    )
        private set

    init {
        val initialHomePreview: List<Song> = musicLibraryRepository.getHomePreview(limit = 6)
        val initialLikedSongIds: Set<String> = injectedFavoritesRepository?.getLikedSongIds()
            ?: initialHomePreview.filter { song: Song -> song.isLiked }.map { song: Song -> song.id }.toSet()
        favoritesRepository = injectedFavoritesRepository ?: InMemoryFavoritesRepository(
            initialLikedSongIds = initialLikedSongIds,
        )
        toggleFavoriteUseCase = ToggleFavoriteUseCaseImpl(
            favoritesRepository = favoritesRepository,
        )
        libraryStateSynchronizer = LibraryStateSynchronizer(
            musicLibraryRepository = musicLibraryRepository,
            favoritesRepository = favoritesRepository,
            playbackRepository = playbackRepository,
        )
        favoriteStateSynchronizer = FavoriteStateSynchronizer(
            toggleFavoriteUseCase = toggleFavoriteUseCase,
            favoriteSongsResolver = libraryStateSynchronizer::buildFavoriteSongs,
            recentSongsBuilder = { state: MusicAppUiState, songs: List<Song> ->
                libraryStateSynchronizer.buildRecentSongs(
                    state = state,
                    extraSongs = songs,
                )
            },
        )
        playbackUiStateSynchronizer = PlaybackUiStateSynchronizer(
            playbackRepository = playbackRepository,
            recentSongsBuilder = libraryStateSynchronizer::buildRecentSongs,
        )
        playbackRestoreOrchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = playbackSnapshotStore,
            availableSongsResolver = libraryStateSynchronizer::resolveAvailableSongsByIds,
            restoreSnapshot = playbackCoordinator::restoreSnapshot,
        )
        uiState = createInitialState(
            homePreview = initialHomePreview,
            initialLikedSongIds = initialLikedSongIds,
        )
        playbackCoordinator.start(scope = controllerScope) {
            syncPlaybackState(playbackState = playbackRepository.getPlaybackState())
        }
    }

    /**
     * 注入播放 UI 观察者，让平台宿主能在共享状态变化时刷新通知或系统控件。
     */
    fun attachPlaybackUiObserver(observer: (MusicAppUiState) -> Unit) {
        playbackUiObserver = observer
        playbackUiObserver(uiState)
    }

    /** 统一向宿主发布最新播放 UI 状态，避免控制器外部重复读取内部细节。 */
    private fun publishPlaybackUiState() {
        playbackUiObserver(uiState)
    }

    /** 进入二级页面并隐藏主 Tab。 */
    fun navigateToSecondary(screen: SecondaryScreen) {
        commitActiveSearchQueryToHistoryIfNeeded()
        uiState = NavigationStateController.navigateToSecondary(
            state = uiState,
            screen = screen,
        )
    }

    /** 切换一级 Tab。 */
    fun navigateToRoot(tab: RootTab) {
        commitActiveSearchQueryToHistoryIfNeeded()
        uiState = NavigationStateController.navigateToRoot(
            state = uiState,
            tab = tab,
        )
    }

    /** 从二级页面返回上一个一级页面。 */
    fun navigateBack() {
        commitActiveSearchQueryToHistoryIfNeeded()
        uiState = NavigationStateController.navigateBack(state = uiState)
    }

    /**
     * 处理 Android 系统返回键，优先关闭临时浮层，最后才退出二级页面。
     */
    fun handleSystemBack(): Boolean {
        if (uiState.isPermissionSettingsDialogOpen) {
            closePermissionSettingsDialog()
            return true
        }
        if (uiState.isClearCacheDialogOpen) {
            closeClearCacheDialog()
            return true
        }
        if (uiState.moreSongId != null) {
            closeMore()
            return true
        }
        if (uiState.isQueueOpen) {
            closeQueue()
            return true
        }
        if (!uiState.navigationState.isTopLevel) {
            navigateBack()
            return true
        }
        return false
    }

    /** 扫描本地音乐并同步曲库快照。 */
    suspend fun scanLocalMusic(request: LocalMusicScanRequest = LocalMusicScanRequest.Refresh) {
        if (shouldConfirmPermissionSettingsBeforeScan()) {
            openPermissionSettingsDialog()
            return
        }
        uiState = uiState.copy(
            scanState = LocalMusicScanState.Scanning(
                progress = LocalMusicScanProgress(currentSourceName = "本地音乐"),
            ),
            isQueueOpen = false,
            moreSongId = null,
        )
        try {
            val snapshot: LibrarySnapshot = scanLocalMusicUseCase(
                request = request,
                likedSongIds = uiState.likedSongIds,
            )
            syncLibrarySnapshot(snapshot = snapshot)
        } catch (scanException: LocalMusicScanException) {
            uiState = uiState.copy(
                scanState = LocalMusicScanState.Error(error = scanException.error),
                isQueueOpen = false,
                moreSongId = null,
            )
        }
    }

    /**
     * 按可用曲库恢复持久化播放快照，并始终以暂停态回填共享 UI。
     */
    suspend fun restorePlaybackSnapshot() {
        val result: PlaybackRestoreOrchestrator.Result = playbackRestoreOrchestrator.restore(
            state = uiState,
            preferredSongs = preferredKnownSongs(),
        )
        uiState = uiState.copy(queueSongsSnapshot = result.state.queueSongsSnapshot)
        isPlaybackRestorePending = result.isPending
    }

    /** 打开权限设置确认框，由用户选择是否离开 App 进入系统设置。 */
    fun openPermissionSettingsDialog() {
        uiState = uiState.copy(
            isPermissionSettingsDialogOpen = true,
            isQueueOpen = false,
            moreSongId = null,
        )
    }

    /** 关闭权限设置确认框，保留当前权限错误态供用户稍后重试。 */
    fun closePermissionSettingsDialog() {
        uiState = uiState.copy(isPermissionSettingsDialogOpen = false)
    }

    /** 用户确认后再打开系统权限设置页，避免永久拒绝后突然跳出 App。 */
    fun confirmPermissionSettings() {
        uiState = uiState.copy(
            isPermissionSettingsDialogOpen = false,
            scanState = LocalMusicScanState.WaitingForPermission,
        )
        permissionSettingsOpener.openPermissionSettings()
    }

    /** 打开本地音乐二级页并指定初始分段。 */
    fun openLocalMusic(section: LocalMusicSection = LocalMusicSection.Songs) {
        loadLocalMusicLibrary()
        navigateToSecondary(screen = SecondaryScreen.LocalMusic(initialSection = section))
    }

    /** 搜索页应按入口上下文拿到对应数据集合，避免搜索结果跨页面串联。 */
    fun openSearch(context: SearchContext = SearchContext.LocalLibrary) {
        if (context == SearchContext.LocalLibrary) {
            loadLocalMusicLibrary()
        }
        uiState = searchSessionController.openSearch(
            state = uiState,
            context = context,
        )
        navigateToSecondary(screen = SecondaryScreen.Search(context = context))
    }

    /** 播放歌曲但留在当前页面，未显式传列表时优先复用当前队列上下文。 */
    fun playSong(song: Song, queueSongs: List<Song> = emptyList()) {
        commitActiveSearchQueryToHistoryIfNeeded()
        val resolvedQueueSongs: List<Song> = resolvePlaybackQueueSongs(
            song = song,
            queueSongs = queueSongs,
        )
        uiState = uiState.copy(queueSongsSnapshot = resolvedQueueSongs)
        controllerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            playbackCoordinator.playSong(song = song, queueSongs = resolvedQueueSongs)
        }
    }

    /** 打开播放页并播放歌曲。 */
    fun openSong(song: Song, queueSongs: List<Song> = emptyList()) {
        playSong(song = song, queueSongs = queueSongs)
        openPlayer()
    }

    // 队列弹层等入口只有歌曲本身时，复用当前显式队列，避免变成单曲队列。
    private fun resolvePlaybackQueueSongs(song: Song, queueSongs: List<Song>): List<Song> {
        if (queueSongs.any { candidate -> candidate.id == song.id }) {
            return queueSongs
        }
        val currentQueueSongs: List<Song> = uiState.queueSongs
        if (currentQueueSongs.any { candidate -> candidate.id == song.id }) {
            return currentQueueSongs
        }
        return listOf(song)
    }

    /** 打开当前播放页，供迷你播放器和 Android 通知正文复用同一路由入口。 */
    fun openPlayer() {
        navigateToSecondary(screen = SecondaryScreen.Player)
    }

    /** 切换播放暂停。 */
    fun togglePlayback() {
        playbackCoordinator.togglePlayback()
    }

    /** 显式恢复或开始播放，供 Android 系统媒体命令调用。 */
    fun play() {
        playbackCoordinator.play()
    }

    /** 显式暂停播放，供 Android 系统媒体命令调用。 */
    fun pause() {
        playbackCoordinator.pause()
    }

    /** 切换上一首或下一首。 */
    fun moveTrack(direction: Int) {
        if (direction < 0) {
            playbackCoordinator.movePrevious()
            return
        }
        playbackCoordinator.moveNext()
    }

    /** 按精确队列下标切歌，并带入系统命令指定的起始进度。 */
    fun skipToQueueIndex(index: Int, positionMs: Long = 0L) {
        playbackCoordinator.skipToQueueIndex(
            index = index,
            positionMs = positionMs,
        )
    }

    /** 拖动播放进度时同时更新运行态与持久化快照，避免冷启动回到旧进度。 */
    fun seekTo(positionMs: Long) {
        playbackCoordinator.seekTo(positionMs = positionMs)
        controllerScope.launch {
            playbackSnapshotStore.saveSnapshot(
                snapshot = com.yanhao.kmpmusic.domain.model.PlaybackSnapshot(
                    playbackState = playbackRepository.getPlaybackState().copy(
                        positionMs = positionMs.coerceAtLeast(minimumValue = 0L),
                    ),
                    queueState = playbackRepository.getQueueState(),
                    updatedAt = nowMillis(),
                ),
            )
        }
    }

    /** 播放模式按钮只负责触发协调器切换，UI 统一从仓库回读。 */
    fun cyclePlaybackMode() {
        playbackCoordinator.cyclePlaybackMode()
    }

    /** 调整共享播放器音量，所有页面读取同一份状态后再由 [PlaybackCoordinator] 下发到播放引擎。 */
    fun setVolume(volume: Float) {
        val safeVolume: Float = volume.coerceIn(minimumValue = 0f, maximumValue = 1f)
        uiState = uiState.copy(playbackVolume = safeVolume)
        playbackCoordinator.setVolume(volume = safeVolume)
    }

    /**
     * Android 播放 service 退出前，通过协调器补写最终暂停快照，避免恢复时丢掉最后位置。
     */
    fun persistPlaybackSnapshotForServiceTeardown(positionMs: Long, durationMs: Long?) {
        playbackCoordinator.persistSnapshotForServiceTeardown(
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    /**
     * Desktop 进程退出前同步固化最后进度，避免宿主关闭数据库或协程作用域时丢掉尾帧。
     */
    suspend fun persistPlaybackSnapshotForProcessTeardown(positionMs: Long, durationMs: Long?) {
        playbackCoordinator.persistSnapshotForProcessTeardown(
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    /** 切换收藏并同步歌曲状态。 */
    fun toggleFavorite(songId: String) {
        uiState = favoriteStateSynchronizer.toggleFavorite(
            state = uiState,
            songId = songId,
        )
        publishPlaybackUiState()
    }

    /** 切换当前播放歌曲收藏，避免平台宿主直接探查 [uiState] 细节。 */
    fun toggleCurrentSongFavorite() {
        val currentSongId: String = uiState.currentSongId ?: return
        toggleFavorite(songId = currentSongId)
    }

    /** 打开专辑详情。 */
    fun openAlbum(album: Album) {
        commitActiveSearchQueryToHistoryIfNeeded()
        loadLocalMusicLibrary()
        uiState = uiState.copy(selectedAlbumId = album.id)
        navigateToSecondary(screen = SecondaryScreen.AlbumDetail)
    }

    /** 打开歌手详情。 */
    fun openArtist(artist: Artist) {
        commitActiveSearchQueryToHistoryIfNeeded()
        loadLocalMusicLibrary()
        uiState = uiState.copy(selectedArtistId = artist.id)
        navigateToSecondary(screen = SecondaryScreen.ArtistDetail)
    }

    /** 从歌曲打开专辑详情。 */
    fun openAlbumFromSong(song: Song) {
        loadLocalMusicLibrary()
        uiState.detailAlbums.firstOrNull { album -> album.title == song.album }?.let { album ->
            uiState = uiState.copy(moreSongId = null)
            openAlbum(album = album)
        }
    }

    /** 从歌曲打开歌手详情。 */
    fun openArtistFromSong(song: Song) {
        loadLocalMusicLibrary()
        uiState.detailArtists.firstOrNull { artist -> artist.name == song.artist }?.let { artist ->
            uiState = uiState.copy(moreSongId = null)
            openArtist(artist = artist)
        }
    }

    /** 更新收藏页分段。 */
    fun setFavoriteSection(section: FavoriteSection) {
        uiState = uiState.copy(favoriteSection = section)
    }

    /** 更新搜索关键词，清空输入前先保留用户刚刚搜索过的非空词。 */
    fun setSearchQuery(query: String) {
        uiState = searchSessionController.setSearchQuery(
            state = uiState,
            query = query,
        )
    }

    /** 更新搜索范围。 */
    fun setSearchScope(scope: SearchScope) {
        uiState = searchSessionController.setSearchScope(
            state = uiState,
            scope = scope,
        )
    }

    /** 将当前搜索词写入当前上下文历史。 */
    fun commitSearchQueryToHistory() {
        uiState = searchSessionController.commitSearchQueryToHistory(state = uiState)
    }

    /** 点击历史词时回填搜索框并刷新该词位置。 */
    fun selectSearchHistory(query: String) {
        uiState = searchSessionController.selectSearchHistory(
            state = uiState,
            query = query,
        )
    }

    /** 删除当前上下文中的单条搜索历史。 */
    fun removeSearchHistoryItem(context: SearchContext, query: String) {
        uiState = searchSessionController.removeSearchHistoryItem(
            state = uiState,
            context = context,
            query = query,
        )
    }

    /** 清空指定上下文的搜索历史。 */
    fun clearSearchHistory(context: SearchContext = uiState.searchContext) {
        uiState = searchSessionController.clearSearchHistory(
            state = uiState,
            context = context,
        )
    }

    /** 清空真实最近播放历史，并立即同步当前页面列表。 */
    fun clearRecentPlaybackHistory() {
        playbackRepository.savePlaybackHistory(history = PlaybackHistory())
        uiState = uiState.copy(recentSongs = emptyList())
    }

    /** 执行搜索，供 UI 渲染派生结果。 */
    fun search(): com.yanhao.kmpmusic.domain.usecase.SearchResult {
        return buildSearchResult(
            query = uiState.activeSearchQuery,
            scope = uiState.searchScope,
            allSongs = searchSourceSongs(),
        )
    }

    // 离开搜索页前集中提交非空搜索词，避免各平台 UI 分别维护历史写入规则。
    private fun commitActiveSearchQueryToHistoryIfNeeded() {
        uiState = searchSessionController.commitActiveSearchQueryToHistoryIfNeeded(state = uiState)
    }

    // 按搜索上下文选择共享数据源，保证 UI 只消费派生结果。
    private fun searchSourceSongs(): List<Song> {
        return when (uiState.searchContext) {
            SearchContext.LocalLibrary -> {
                if (uiState.localSongs.isNotEmpty()) {
                    uiState.localSongs
                } else {
                    musicLibraryRepository.getAllAvailableSongs()
                }
            }
            SearchContext.Favorites -> uiState.favoriteSongs
        }
    }

    /** 设置主题模式。 */
    fun setThemeMode(themeMode: ThemeMode) {
        userPreferencesRepository.saveThemeMode(themeMode = themeMode)
        uiState = uiState.copy(themeMode = themeMode)
    }

    /** 打开队列弹层。 */
    fun openQueue() {
        uiState = uiState.copy(isQueueOpen = true)
    }

    /** 关闭队列弹层。 */
    fun closeQueue() {
        uiState = uiState.copy(isQueueOpen = false)
    }

    /** 从队列移除歌曲，至少保留一首。 */
    fun removeFromQueue(songId: String) {
        controllerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            playbackCoordinator.removeFromQueue(
                songId = songId,
                availableSongs = uiState.queueSongs,
            )
        }
    }

    /** 打开更多操作弹层。 */
    fun openMore(song: Song) {
        uiState = uiState.copy(moreSongId = song.id)
    }

    /** 关闭更多操作弹层。 */
    fun closeMore() {
        uiState = uiState.copy(moreSongId = null)
    }

    /** 打开清理缓存确认。 */
    fun openClearCacheDialog() {
        uiState = uiState.copy(isClearCacheDialogOpen = true)
    }

    /** 关闭清理缓存确认。 */
    fun closeClearCacheDialog() {
        uiState = uiState.copy(isClearCacheDialogOpen = false)
    }

    /** 确认清理缓存。 */
    fun confirmClearCache() {
        uiState = uiState.copy(isClearCacheDialogOpen = false)
    }

    /** 更新登录邮箱。 */
    fun setEmail(email: String) {
        uiState = uiState.copy(email = email)
    }

    /** 模拟发送登录邮件。 */
    fun sendLoginMail() {
        if (!uiState.email.contains("@")) {
            return
        }
        uiState = uiState.copy(isMailSent = true)
    }

    // 创建初始状态，保证仓库初始化顺序集中。
    private fun createInitialState(
        homePreview: List<Song>,
        initialLikedSongIds: Set<String>,
    ): MusicAppUiState {
        val stats: LibraryStats = musicLibraryRepository.getLibraryStats()
        val playbackState: PlaybackState = playbackRepository.getPlaybackState()
        val queueState: QueueState = playbackRepository.getQueueState()
        val previewWithLikes = homePreview.map { song ->
            song.copy(isLiked = initialLikedSongIds.contains(song.id) || song.isLiked)
        }
        val initialScanState: LocalMusicScanState = buildInitialScanState(stats = stats)
        return MusicAppUiState(
            homeLocalSongPreview = previewWithLikes,
            localSongs = emptyList(),
            localAlbums = emptyList(),
            localArtists = emptyList(),
            favoriteSongs = libraryStateSynchronizer.buildFavoriteSongs(
                likedSongIds = initialLikedSongIds,
                preferredSongs = previewWithLikes,
            ),
            queueSongsSnapshot = emptyList(),
            likedSongIds = initialLikedSongIds,
            currentSongId = playbackState.currentSongId,
            playbackStatus = playbackState.status,
            playbackPositionMs = playbackState.positionMs,
            playbackDurationMs = playbackState.durationMs,
            playbackMode = queueState.playbackMode,
            playbackError = playbackState.error,
            queueSongIds = queueState.songIds,
            libraryStats = stats,
            scanState = initialScanState,
            recentSongs = libraryStateSynchronizer.buildRecentSongs(
                state = MusicAppUiState(
                    homeLocalSongPreview = previewWithLikes,
                    localSongs = emptyList(),
                    localAlbums = emptyList(),
                    localArtists = emptyList(),
                    favoriteSongs = emptyList(),
                    queueSongsSnapshot = emptyList(),
                    likedSongIds = initialLikedSongIds,
                    currentSongId = playbackState.currentSongId,
                    playbackStatus = playbackState.status,
                    playbackPositionMs = playbackState.positionMs,
                    playbackDurationMs = playbackState.durationMs,
                    playbackMode = queueState.playbackMode,
                    playbackError = playbackState.error,
                    queueSongIds = queueState.songIds,
                    libraryStats = stats,
                    scanState = initialScanState,
                ),
                extraSongs = previewWithLikes,
            ),
            themeMode = userPreferencesRepository.getThemeMode(),
            localLibrarySearchHistory = searchHistoryRepository.getSearchHistory(
                context = SearchContext.LocalLibrary,
            ),
            favoritesSearchHistory = searchHistoryRepository.getSearchHistory(
                context = SearchContext.Favorites,
            ),
        )
    }

    // 永久拒绝后首页按钮表示“打开权限设置”，再次点击时先弹确认框。
    private fun shouldConfirmPermissionSettingsBeforeScan(): Boolean {
        return libraryStateSynchronizer.shouldConfirmPermissionSettingsBeforeScan(state = uiState)
    }

    // 持久层已有歌曲时，冷启动首页应表达“已有曲库，可重新扫描”，但不为此读取全量歌曲。
    private fun buildInitialScanState(stats: LibraryStats): LocalMusicScanState {
        return LibraryStateSynchronizer.buildInitialScanStateFromStats(stats = stats)
    }

    // 同步播放仓库和 UI 状态，避免多个入口各自写状态。
    private fun syncPlaybackState(playbackState: PlaybackState) {
        uiState = playbackUiStateSynchronizer.syncPlaybackState(
            state = uiState,
            playbackState = playbackState,
        )
        publishPlaybackUiState()
    }

    // 曲库快照是首页、搜索、收藏和我的页的唯一列表来源。
    private fun syncLibrarySnapshot(snapshot: LibrarySnapshot) {
        uiState = libraryStateSynchronizer.syncLibrarySnapshot(
            state = uiState,
            snapshot = snapshot,
        )
        restorePlaybackSnapshotIfPending()
    }

    // 只有启动期显式请求过恢复时，扫描成功后才续上真正的快照恢复，避免平时扫描打断当前播放。
    private fun restorePlaybackSnapshotIfPending() {
        if (!isPlaybackRestorePending) {
            return
        }
        controllerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            restorePlaybackSnapshot()
        }
    }

    /** 按需读取完整本地曲库，避免首页冷启动直接打满持久层。 */
    fun loadLocalMusicLibrary() {
        val previousLocalSongsLoaded: Boolean = uiState.localSongs.isNotEmpty()
        uiState = libraryStateSynchronizer.loadLocalMusicLibrary(state = uiState)
        if (!previousLocalSongsLoaded && uiState.localSongs.isNotEmpty()) {
            restorePlaybackSnapshotIfPending()
        }
    }

    // 当前状态里已经拿到的歌曲优先参与收藏/恢复，避免重复构造不同实例。
    private fun preferredKnownSongs(): List<Song> {
        return MusicLibraryProjector.buildDetailSongs(
            queueSongsSnapshot = uiState.queueSongsSnapshot,
            localSongs = uiState.localSongs,
            homeLocalSongPreview = uiState.homeLocalSongPreview,
            favoriteSongs = uiState.favoriteSongs,
        )
    }
}
