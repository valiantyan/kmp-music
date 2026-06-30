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
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanProgress
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
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
import com.yanhao.kmpmusic.feature.app.library.MusicLibraryProjector
import com.yanhao.kmpmusic.feature.app.navigation.NavigationStateController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // 搜索输入节流任务，避免连续输入时每个字符都触发结果计算。
    private var searchQueryDebounceJob: Job? = null

    /**
     * Compose 可观察 UI 状态。
     */
    var uiState: MusicAppUiState by mutableStateOf(createInitialState())
        private set

    init {
        playbackCoordinator.start(scope = controllerScope) {
            syncPlaybackState(playbackState = playbackRepository.getPlaybackState())
        }
        favoritesRepository = injectedFavoritesRepository ?: InMemoryFavoritesRepository(
            initialLikedSongIds = uiState.likedSongIds,
        )
        toggleFavoriteUseCase = ToggleFavoriteUseCaseImpl(
            favoritesRepository = favoritesRepository,
        )
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
        val savedQueueSongIds: List<String> = playbackSnapshotStore.getSavedQueueSongIds()
        if (savedQueueSongIds.isEmpty()) {
            isPlaybackRestorePending = false
            return
        }
        val availableSongs: List<Song> = resolveAvailableSongsByIds(
            songIds = savedQueueSongIds,
            preferredSongs = preferredKnownSongs(),
        )
        if (availableSongs.isEmpty()) {
            isPlaybackRestorePending = true
            return
        }
        uiState = uiState.copy(queueSongsSnapshot = availableSongs)
        isPlaybackRestorePending = false
        playbackCoordinator.restoreSnapshot(
            availableSongs = availableSongs,
        )
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
        syncActiveSearchQueryImmediately(query = "")
        uiState = uiState.copy(
            searchContext = context,
            searchQuery = "",
            activeSearchQuery = "",
            searchScope = SearchScope.All,
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
        val likedSongIds: Set<String> = toggleFavoriteUseCase(songId = songId)
        fun Song.withFavorite(): Song = copy(isLiked = likedSongIds.contains(id))
        val homePreview = uiState.homeLocalSongPreview.map { song -> song.withFavorite() }
        val localSongs = uiState.localSongs.map { song -> song.withFavorite() }
        val queueSnapshot = uiState.queueSongsSnapshot.map { song -> song.withFavorite() }
        uiState = uiState.copy(
            likedSongIds = likedSongIds,
            homeLocalSongPreview = homePreview,
            localSongs = localSongs,
            favoriteSongs = buildFavoriteSongs(
                likedSongIds = likedSongIds,
                preferredSongs = homePreview + localSongs + queueSnapshot + uiState.favoriteSongs,
            ),
            queueSongsSnapshot = queueSnapshot,
            recentSongs = buildRecentSongs(songs = localSongs.ifEmpty { homePreview }),
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
        val previousQuery: String = uiState.searchQuery
        if (shouldCommitSearchQueryBeforeClearing(previousQuery = previousQuery, nextQuery = query)) {
            commitSearchQueryToHistory(
                query = previousQuery,
                context = uiState.searchContext,
            )
            syncActiveSearchQueryImmediately(query = "")
            uiState = uiState.copy(searchQuery = query)
            return
        }
        uiState = uiState.copy(searchQuery = query)
        scheduleActiveSearchQuerySync(query = query)
    }

    /** 更新搜索范围。 */
    fun setSearchScope(scope: SearchScope) {
        uiState = uiState.copy(searchScope = scope)
    }

    /** 将当前搜索词写入当前上下文历史。 */
    fun commitSearchQueryToHistory() {
        syncActiveSearchQueryImmediately(query = uiState.searchQuery)
        commitSearchQueryToHistory(
            query = uiState.searchQuery,
            context = uiState.searchContext,
        )
    }

    // 清空搜索框会切到历史空态，必须在旧 query 被覆盖前把它记入最近搜索。
    private fun shouldCommitSearchQueryBeforeClearing(previousQuery: String, nextQuery: String): Boolean {
        return uiState.navigationState.secondaryScreen is SecondaryScreen.Search &&
            previousQuery.trim().isNotBlank() &&
            nextQuery.isBlank()
    }

    // 支持在覆盖 UI 输入前提交指定 query，避免清空输入时丢失旧搜索词。
    private fun commitSearchQueryToHistory(query: String, context: SearchContext) {
        val normalizedQuery: String = query.trim()
        if (normalizedQuery.isBlank()) {
            return
        }
        updateSearchHistory(
            context = context,
            history = moveQueryToHistoryTop(
                query = normalizedQuery,
                currentHistory = uiState.searchHistoryFor(context = context),
            ),
        )
    }

    /** 点击历史词时回填搜索框并刷新该词位置。 */
    fun selectSearchHistory(query: String) {
        syncActiveSearchQueryImmediately(query = query)
        uiState = uiState.copy(searchQuery = query)
        commitSearchQueryToHistory()
    }

    /** 删除当前上下文中的单条搜索历史。 */
    fun removeSearchHistoryItem(context: SearchContext, query: String) {
        updateSearchHistory(
            context = context,
            history = uiState.searchHistoryFor(context = context).filterNot { item -> item == query },
        )
    }

    /** 清空指定上下文的搜索历史。 */
    fun clearSearchHistory(context: SearchContext = uiState.searchContext) {
        updateSearchHistory(context = context, history = emptyList())
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

    // 输入节流结束后才更新真正参与搜索的 query，降低连续输入时的过滤和重组频率。
    private fun scheduleActiveSearchQuerySync(query: String) {
        searchQueryDebounceJob?.cancel()
        if (query.isBlank()) {
            syncActiveSearchQueryImmediately(query = query)
            return
        }
        searchQueryDebounceJob = controllerScope.launch {
            delay(timeMillis = searchQueryDebounceMillis)
            uiState = uiState.copy(activeSearchQuery = query)
        }
    }

    // 显式提交、清空和历史点击必须立刻同步，不能等待节流窗口。
    private fun syncActiveSearchQueryImmediately(query: String) {
        searchQueryDebounceJob?.cancel()
        searchQueryDebounceJob = null
        uiState = uiState.copy(activeSearchQuery = query)
    }

    /** 将命中的历史词提到最前，并限制保留数量。 */
    private fun moveQueryToHistoryTop(query: String, currentHistory: List<String>): List<String> {
        return (listOf(query) + currentHistory.filterNot { item -> item == query })
            .take(n = 10)
    }

    /** 按搜索上下文写回对应历史，避免不同入口共用同一份运行时状态。 */
    private fun updateSearchHistory(context: SearchContext, history: List<String>) {
        searchHistoryRepository.saveSearchHistory(
            context = context,
            history = history,
        )
        uiState = when (context) {
            SearchContext.LocalLibrary -> uiState.copy(localLibrarySearchHistory = history)
            SearchContext.Favorites -> uiState.copy(favoritesSearchHistory = history)
        }
    }

    // 离开搜索页前集中提交非空搜索词，避免各平台 UI 分别维护历史写入规则。
    private fun commitActiveSearchQueryToHistoryIfNeeded() {
        if (uiState.navigationState.secondaryScreen !is SecondaryScreen.Search) {
            return
        }
        commitSearchQueryToHistory()
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
    private fun createInitialState(): MusicAppUiState {
        val homePreview: List<Song> = musicLibraryRepository.getHomePreview(limit = 6)
        val stats: LibraryStats = musicLibraryRepository.getLibraryStats()
        val playbackState: PlaybackState = playbackRepository.getPlaybackState()
        val queueState: QueueState = playbackRepository.getQueueState()
        val initialLikedSongIds: Set<String> = injectedFavoritesRepository?.getLikedSongIds()
            ?: homePreview.filter { song -> song.isLiked }.map { song -> song.id }.toSet()
        val previewWithLikes = homePreview.map { song ->
            song.copy(isLiked = initialLikedSongIds.contains(song.id) || song.isLiked)
        }
        val initialScanState: LocalMusicScanState = buildInitialScanState(stats = stats)
        return MusicAppUiState(
            homeLocalSongPreview = previewWithLikes,
            localSongs = emptyList(),
            localAlbums = emptyList(),
            localArtists = emptyList(),
            favoriteSongs = buildFavoriteSongs(
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
            recentSongs = buildRecentSongs(songs = previewWithLikes),
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
        val scanState: LocalMusicScanState = uiState.scanState
        return scanState is LocalMusicScanState.Error &&
            scanState.error.type == LocalMusicScanErrorType.PermissionPermanentlyDenied
    }

    // 持久层已有歌曲时，冷启动首页应表达“已有曲库，可重新扫描”，但不为此读取全量歌曲。
    private fun buildInitialScanState(stats: LibraryStats): LocalMusicScanState {
        if (stats.songCount <= 0) {
            return LocalMusicScanState.Idle
        }
        return LocalMusicScanState.Done(
            summary = LocalMusicLastScanSummary(
                addedCount = stats.songCount,
                updatedCount = 0,
                removedCount = 0,
                problemCount = 0,
                completedAt = 0L,
            ),
        )
    }

    // 同步播放仓库和 UI 状态，避免多个入口各自写状态。
    private fun syncPlaybackState(playbackState: PlaybackState) {
        val queueState: QueueState = playbackRepository.getQueueState()
        uiState = uiState.copy(
            currentSongId = playbackState.currentSongId,
            playbackStatus = playbackState.status,
            playbackPositionMs = playbackState.positionMs,
            playbackDurationMs = playbackState.durationMs,
            playbackMode = queueState.playbackMode,
            playbackError = playbackState.error,
            queueSongIds = queueState.songIds,
            recentSongs = buildRecentSongs(songs = knownSongsForRecentPlayback()),
        )
        publishPlaybackUiState()
    }

    // 曲库快照是首页、搜索、收藏和我的页的唯一列表来源。
    private fun syncLibrarySnapshot(snapshot: LibrarySnapshot) {
        val likedSongIds: Set<String> = favoritesRepository.getLikedSongIds()
        val previewWithLikes: List<Song> = musicLibraryRepository.getHomePreview(limit = 6).map { song ->
            song.copy(isLiked = likedSongIds.contains(song.id) || song.isLiked)
        }
        val shouldRefreshFullLibrary: Boolean = uiState.localSongs.isNotEmpty() ||
            uiState.navigationState.secondaryScreen is SecondaryScreen.LocalMusic
        val fullSongsWithLikes: List<Song> = if (shouldRefreshFullLibrary) {
            musicLibraryRepository.getAllAvailableSongs().map { song ->
                song.copy(isLiked = likedSongIds.contains(song.id) || song.isLiked)
            }
        } else {
            uiState.localSongs
        }
        uiState = uiState.copy(
            homeLocalSongPreview = previewWithLikes,
            localSongs = fullSongsWithLikes,
            localAlbums = if (shouldRefreshFullLibrary) {
                MusicLibraryProjector.buildAlbums(songs = fullSongsWithLikes)
            } else {
                uiState.localAlbums
            },
            localArtists = if (shouldRefreshFullLibrary) {
                MusicLibraryProjector.buildArtists(songs = fullSongsWithLikes)
            } else {
                uiState.localArtists
            },
            libraryStats = musicLibraryRepository.getLibraryStats(),
            localMusicSources = snapshot.sources,
            localMusicProblems = snapshot.problems,
            scanState = snapshot.scanState,
            likedSongIds = likedSongIds + previewWithLikes.filter { song -> song.isLiked }.map { song -> song.id },
            recentSongs = buildRecentSongs(
                songs = knownSongsForRecentPlayback(extraSongs = fullSongsWithLikes + previewWithLikes),
            ),
            favoriteSongs = buildFavoriteSongs(
                likedSongIds = likedSongIds,
                preferredSongs = previewWithLikes + fullSongsWithLikes + uiState.queueSongsSnapshot + uiState.favoriteSongs,
            ),
        )
        restorePlaybackSnapshotIfPending()
    }

    // 最近播放只读取播放历史，不从扫描结果自动生成。
    private fun buildRecentSongs(songs: List<Song>): List<Song> {
        val songsById: Map<String, Song> = songs.distinctBy { song -> song.id }.associateBy { song -> song.id }
        return playbackRepository.getPlaybackHistory().songIds
            .mapNotNull { songId: String -> songsById[songId] }
    }

    // 最近播放要覆盖当前队列和完整曲库，避免播放非首页预览歌曲后无法反查实体。
    private fun knownSongsForRecentPlayback(extraSongs: List<Song> = emptyList()): List<Song> {
        return extraSongs +
            uiState.queueSongsSnapshot +
            uiState.localSongs +
            uiState.homeLocalSongPreview +
            uiState.favoriteSongs
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
        if (uiState.localSongs.isNotEmpty()) {
            return
        }
        val likedSongIds = favoritesRepository.getLikedSongIds()
        val songsWithLikes = musicLibraryRepository.getAllAvailableSongs().map { song ->
            song.copy(isLiked = likedSongIds.contains(song.id) || song.isLiked)
        }
        uiState = uiState.copy(
            localSongs = songsWithLikes,
            localAlbums = MusicLibraryProjector.buildAlbums(songs = songsWithLikes),
            localArtists = MusicLibraryProjector.buildArtists(songs = songsWithLikes),
            favoriteSongs = buildFavoriteSongs(
                likedSongIds = likedSongIds,
                preferredSongs = uiState.homeLocalSongPreview + songsWithLikes + uiState.queueSongsSnapshot + uiState.favoriteSongs,
            ),
            likedSongIds = likedSongIds + songsWithLikes.filter { song -> song.isLiked }.map { song -> song.id },
            recentSongs = buildRecentSongs(songs = knownSongsForRecentPlayback(extraSongs = songsWithLikes)),
        )
        restorePlaybackSnapshotIfPending()
    }

    // 收藏和冷启动恢复都需要按 id 补齐歌曲实体，优先复用当前已知对象，缺口再向仓库按需查询。
    private fun resolveAvailableSongsByIds(songIds: List<String>, preferredSongs: List<Song>): List<Song> {
        if (songIds.isEmpty()) {
            return emptyList()
        }
        val preferredById: Map<String, Song> = preferredSongs.associateBy { song -> song.id }
        val fetchedSongs: List<Song> = musicLibraryRepository.getAvailableSongsByIds(songIds = songIds)
        val fetchedSongIds: Set<String> = fetchedSongs.map { song -> song.id }.toSet()
        return (fetchedSongs.map { song -> preferredById[song.id] ?: song } +
            preferredSongs.filter { song -> songIds.contains(song.id) && !fetchedSongIds.contains(song.id) })
            .distinctBy { song -> song.id }
    }

    // 当前状态里已经拿到的歌曲优先参与收藏/恢复，避免重复构造不同实例。
    private fun preferredKnownSongs(): List<Song> {
        return (uiState.queueSongsSnapshot + uiState.localSongs + uiState.homeLocalSongPreview + uiState.favoriteSongs)
            .distinctBy { song -> song.id }
    }

    // 收藏页的实体来源独立于 localSongs 是否已加载，保证首页 preview 拆分后语义仍然稳定。
    private fun buildFavoriteSongs(likedSongIds: Set<String>, preferredSongs: List<Song>): List<Song> {
        return resolveAvailableSongsByIds(
            songIds = likedSongIds.toList(),
            preferredSongs = preferredSongs,
        ).map { song ->
            song.copy(isLiked = true)
        }
    }
}
