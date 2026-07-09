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
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
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
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCase
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCaseImpl
import com.yanhao.kmpmusic.feature.app.search.SearchResultController
import com.yanhao.kmpmusic.feature.app.favorites.FavoriteStateSynchronizer
import com.yanhao.kmpmusic.feature.app.library.LibraryStateSynchronizer
import com.yanhao.kmpmusic.feature.app.library.LocalMusicScanController
import com.yanhao.kmpmusic.feature.app.library.MusicLibraryProjector
import com.yanhao.kmpmusic.feature.app.navigation.ContentNavigationController
import com.yanhao.kmpmusic.feature.app.navigation.NavigationStateController
import com.yanhao.kmpmusic.feature.app.playback.PlaybackActionController
import com.yanhao.kmpmusic.feature.app.playback.PlaybackRestoreGate
import com.yanhao.kmpmusic.feature.app.playback.PlaybackRestoreOrchestrator
import com.yanhao.kmpmusic.feature.app.playback.PlaybackUiStateSynchronizer
import com.yanhao.kmpmusic.feature.app.preferences.PreferenceStateController
import com.yanhao.kmpmusic.feature.app.search.SearchSessionController
import com.yanhao.kmpmusic.feature.app.session.LoginAndDialogStateController
import com.yanhao.kmpmusic.feature.app.system.SystemBackController
import kotlinx.coroutines.CoroutineScope

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

    // 播放恢复 gate 统一持有 pending/job/generation/mutex，facade 只保留触发点。
    private val playbackRestoreGate: PlaybackRestoreGate

    // 内容导航工作流统一处理曲库预热与内容路由，facade 只保留门面时序。
    private val contentNavigationController: ContentNavigationController

    // 偏好设置工作流统一处理主题与本地发现设置的持久化和状态同步。
    private val preferenceStateController: PreferenceStateController = PreferenceStateController(
        userPreferencesRepository = userPreferencesRepository,
    )

    // 本地扫描用例。
    private val scanLocalMusicUseCase: ScanLocalMusicUseCase = ScanLocalMusicUseCaseImpl(
        localMusicScanner = localMusicScanner,
        musicLibraryRepository = musicLibraryRepository,
    )

    // 本地扫描工作流统一收敛会话取消、权限确认和旧事件丢弃。
    private val localMusicScanController: LocalMusicScanController

    // 播放协调器统一托管运行时播放、队列和快照写入。
    private val playbackCoordinator: PlaybackCoordinator = PlaybackCoordinator(
        playbackRepository = playbackRepository,
        audioPlayerEngine = audioPlayerEngine,
        playbackSnapshotStore = playbackSnapshotStore,
        nowMillis = nowMillis,
    )

    // 播放动作工作流统一处理会改变播放事实的入口，facade 只保留公开 API 与状态发布时序。
    private val playbackActionController: PlaybackActionController = PlaybackActionController(
        playbackCoordinator = playbackCoordinator,
        playbackRepository = playbackRepository,
        playbackSnapshotStore = playbackSnapshotStore,
        controllerScope = controllerScope,
        nowMillis = nowMillis,
    )

    // 播放 UI 刷新观察者，供平台通知或其他宿主订阅共享状态。
    private var playbackUiObserver: (MusicAppUiState) -> Unit = {}

    // 搜索会话子控制器负责搜索输入态、防抖和历史 reducer。
    private val searchSessionController: SearchSessionController = SearchSessionController(
        searchHistoryRepository = searchHistoryRepository,
        controllerScope = controllerScope,
        debounceMillis = searchQueryDebounceMillis,
        publishStateUpdate = ::reduceUiState,
    )

    // 搜索结果派生单独收敛，避免 facade 继续持有上下文分流和 pending query 规则。
    private val searchResultController: SearchResultController = SearchResultController(
        musicLibraryRepository = musicLibraryRepository,
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
        localMusicScanController = LocalMusicScanController(
            scanLocalMusicUseCase = scanLocalMusicUseCase,
            permissionSettingsOpener = permissionSettingsOpener,
            controllerScope = controllerScope,
            nowMillis = nowMillis,
            resolveLikedSongIdsForScan = { _: MusicAppUiState ->
                resolveLikedSongIdsForScan()
            },
            shouldConfirmPermissionSettingsBeforeScan = { state: MusicAppUiState ->
                libraryStateSynchronizer.shouldConfirmPermissionSettingsBeforeScan(state = state)
            },
            publishStateUpdate = ::reduceUiState,
        )
        contentNavigationController = ContentNavigationController(
            libraryStateSynchronizer = libraryStateSynchronizer,
        )
        playbackUiStateSynchronizer = PlaybackUiStateSynchronizer(
            playbackRepository = playbackRepository,
            recentSongsBuilder = libraryStateSynchronizer::buildRecentSongs,
        )
        val playbackRestoreOrchestrator: PlaybackRestoreOrchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = playbackSnapshotStore,
            availableSongsResolver = libraryStateSynchronizer::resolveAvailableSongsByIds,
        )
        playbackRestoreGate = PlaybackRestoreGate(
            playbackRestoreOrchestrator = playbackRestoreOrchestrator,
            playbackCoordinator = playbackCoordinator,
            controllerScope = controllerScope,
            stateHost = object : PlaybackRestoreGate.StateHost {
                override fun getState(): MusicAppUiState {
                    return uiState
                }

                override fun getPreferredKnownSongs(): List<Song> {
                    return preferredKnownSongs()
                }

                override fun reduceState(reducer: (MusicAppUiState) -> MusicAppUiState) {
                    reduceUiState(reducer = reducer)
                }
            },
        )
        val initialStateBuilder: MusicAppInitialStateBuilder = MusicAppInitialStateBuilder(
            musicLibraryRepository = musicLibraryRepository,
            playbackRepository = playbackRepository,
            userPreferencesRepository = userPreferencesRepository,
            searchHistoryRepository = searchHistoryRepository,
            favoriteSongsBuilder = libraryStateSynchronizer::buildFavoriteSongs,
            recentSongsBuilder = libraryStateSynchronizer::buildRecentSongs,
        )
        uiState = initialStateBuilder.build(
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

    /** 所有异步协作者都经由这里提交同步归约，避免晚到结果覆盖最新 [uiState]。 */
    private fun reduceUiState(reducer: (MusicAppUiState) -> MusicAppUiState) {
        uiState = reducer(uiState)
    }

    /** 应用内容导航结果，并在首次拿到完整曲库后补跑待恢复的播放快照。 */
    private fun applyContentNavigationResult(result: ContentNavigationController.Result) {
        uiState = result.state
        if (result.loadedFullLibrary) {
            playbackRestoreGate.restorePlaybackSnapshotIfPending()
        }
    }

    /** 进入二级页面并隐藏主 Tab。 */
    fun navigateToSecondary(screen: SecondaryScreen) {
        uiState = NavigationStateController.navigateToSecondary(
            state = uiState,
            screen = screen,
        )
    }

    /** 切换一级 Tab。 */
    fun navigateToRoot(tab: RootTab) {
        uiState = NavigationStateController.navigateToRoot(
            state = uiState,
            tab = tab,
        )
    }

    /** 从二级页面返回上一个一级页面。 */
    fun navigateBack() {
        uiState = NavigationStateController.navigateBack(state = uiState)
    }

    /** 切换首页内容页签，聚合型页签按需加载完整本地曲库。 */
    fun setHomeContentSection(section: HomeContentSection) {
        applyContentNavigationResult(
            result = contentNavigationController.setHomeContentSection(
                state = uiState,
                section = section,
            ),
        )
    }

    /** 我的页歌曲统计回到首页歌曲分段，保持它作为一级页入口。 */
    fun openHomeSongs() {
        applyContentNavigationResult(
            result = contentNavigationController.openHomeSongs(state = uiState),
        )
    }

    /**
     * 处理 Android 系统返回键，优先关闭临时浮层，最后才退出二级页面。
     */
    fun handleSystemBack(): Boolean {
        val result: SystemBackController.Result = SystemBackController.handleSystemBack(state = uiState)
        uiState = result.state
        return result.wasHandled
    }

    /** 使用控制器生命周期启动扫描，避免主题切换重组取消 UI 协程后卡住扫描态。 */
    fun requestLocalMusicScan(request: LocalMusicScanRequest = LocalMusicScanRequest.Refresh) {
        localMusicScanController.requestLocalMusicScan(
            state = uiState,
            request = request,
            onLibrarySnapshot = { snapshot: LibrarySnapshot ->
                syncLibrarySnapshot(snapshot = snapshot)
            },
        )
    }

    /** 扫描本地音乐并同步曲库快照。 */
    suspend fun scanLocalMusic(request: LocalMusicScanRequest = LocalMusicScanRequest.Refresh) {
        localMusicScanController.scanLocalMusic(
            state = uiState,
            request = request,
            onLibrarySnapshot = { snapshot: LibrarySnapshot ->
                syncLibrarySnapshot(snapshot = snapshot)
            },
        )
    }

    /**
     * 按可用曲库恢复持久化播放快照，并始终以暂停态回填共享 UI。
     */
    suspend fun restorePlaybackSnapshot() {
        playbackRestoreGate.restorePlaybackSnapshot()
    }

    /** 打开权限设置确认框，由用户选择是否离开 App 进入系统设置。 */
    fun openPermissionSettingsDialog() {
        localMusicScanController.openPermissionSettingsDialog()
    }

    /** 关闭权限设置确认框，保留当前权限错误态供用户稍后重试。 */
    fun closePermissionSettingsDialog() {
        localMusicScanController.closePermissionSettingsDialog()
    }

    /** 用户确认后再打开系统权限设置页，避免永久拒绝后突然跳出 App。 */
    fun confirmPermissionSettings() {
        localMusicScanController.confirmPermissionSettings()
    }

    /** 打开本地音乐二级页并指定初始分段。 */
    fun openLocalMusic(section: LocalMusicSection = LocalMusicSection.Songs) {
        applyContentNavigationResult(
            result = contentNavigationController.openLocalMusic(
                state = uiState,
                section = section,
            ),
        )
    }

    /** 打开独立扫描页，让首页入口先展示扫描设置和统计。 */
    fun openAudioScan() {
        applyContentNavigationResult(
            result = contentNavigationController.openAudioScan(state = uiState),
        )
    }

    /** 打开最近播放普通二级页，完整列表行为由后续切片补齐。 */
    fun openRecentPlayed() {
        applyContentNavigationResult(
            result = contentNavigationController.openRecentPlayed(state = uiState),
        )
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
        commitSearchQueryForResultActionIfNeeded()
        launchPlaybackFactMutation {
            val action: PlaybackActionController.PreparedPlaySong = playbackActionController.preparePlaySong(
                state = uiState,
                song = song,
                queueSongs = queueSongs,
            )
            uiState = action.state
            playbackActionController.startPlayback(action = action)
        }
    }

    /** 最近播放入口必须复用完整过滤后列表，避免“我的”页摘要 Top3 截断播放队列。 */
    fun playRecentSong(song: Song) {
        commitSearchQueryForResultActionIfNeeded()
        launchPlaybackFactMutation {
            val action: PlaybackActionController.PreparedPlaySong = playbackActionController.preparePlayRecentSong(
                state = uiState,
                song = song,
            )
            uiState = action.state
            playbackActionController.startPlayback(action = action)
        }
    }

    /** 打开当前播放页，供迷你播放器和 Android 通知正文复用同一路由入口。 */
    fun openPlayer() {
        navigateToSecondary(screen = SecondaryScreen.Player)
    }

    /** 切换播放暂停。 */
    fun togglePlayback() {
        playbackActionController.togglePlayback()
    }

    /** 显式恢复或开始播放，供 Android 系统媒体命令调用。 */
    fun play() {
        playbackActionController.play()
    }

    /** 显式暂停播放，供 Android 系统媒体命令调用。 */
    fun pause() {
        playbackActionController.pause()
    }

    /** 切换上一首或下一首。 */
    fun moveTrack(direction: Int) {
        launchPlaybackFactMutation {
            playbackActionController.moveTrack(direction = direction)
        }
    }

    /** 按精确队列下标切歌，并带入系统命令指定的起始进度。 */
    fun skipToQueueIndex(index: Int, positionMs: Long = 0L) {
        launchPlaybackFactMutation {
            playbackActionController.skipToQueueIndex(
                index = index,
                positionMs = positionMs,
            )
        }
    }

    /** 拖动播放进度时同时更新运行态与持久化快照，避免冷启动回到旧进度。 */
    fun seekTo(positionMs: Long) {
        launchPlaybackFactMutation {
            playbackActionController.seekTo(positionMs = positionMs)
        }
    }

    /** 播放模式按钮只负责触发协调器切换，UI 统一从仓库回读。 */
    fun cyclePlaybackMode() {
        launchPlaybackFactMutation {
            playbackActionController.cyclePlaybackMode()
        }
    }

    /** 调整共享播放器音量，所有页面读取同一份状态后再由 [PlaybackCoordinator] 下发到播放引擎。 */
    fun setVolume(volume: Float) {
        uiState = playbackActionController.setVolume(
            state = uiState,
            volume = volume,
        )
    }

    /**
     * Android 播放 service 退出前，通过协调器补写最终暂停快照，避免恢复时丢掉最后位置。
     */
    fun persistPlaybackSnapshotForServiceTeardown(positionMs: Long, durationMs: Long?) {
        playbackActionController.persistPlaybackSnapshotForServiceTeardown(
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    /**
     * Desktop 进程退出前同步固化最后进度，避免宿主关闭数据库或协程作用域时丢掉尾帧。
     */
    suspend fun persistPlaybackSnapshotForProcessTeardown(positionMs: Long, durationMs: Long?) {
        playbackActionController.persistPlaybackSnapshotForProcessTeardown(
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
        commitSearchQueryForResultActionIfNeeded()
        applyContentNavigationResult(
            result = contentNavigationController.openAlbum(
                state = uiState,
                album = album,
            ),
        )
    }

    /** 打开歌手详情。 */
    fun openArtist(artist: Artist) {
        commitSearchQueryForResultActionIfNeeded()
        applyContentNavigationResult(
            result = contentNavigationController.openArtist(
                state = uiState,
                artist = artist,
            ),
        )
    }

    /** 从歌曲打开专辑详情。 */
    fun openAlbumFromSong(song: Song) {
        commitSearchQueryForResultActionIfNeeded()
        applyContentNavigationResult(
            result = contentNavigationController.openAlbumFromSong(
                state = uiState,
                song = song,
            ),
        )
    }

    /** 从歌曲打开歌手详情。 */
    fun openArtistFromSong(song: Song) {
        commitSearchQueryForResultActionIfNeeded()
        applyContentNavigationResult(
            result = contentNavigationController.openArtistFromSong(
                state = uiState,
                song = song,
            ),
        )
    }

    /** 更新收藏页分段。 */
    fun setFavoriteSection(section: FavoriteSection) {
        uiState = uiState.copy(favoriteSection = section)
    }

    /** 更新搜索关键词；防抖搜索生效或点击结果等明确动作会写入历史。 */
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
        uiState = playbackActionController.clearRecentPlaybackHistory(state = uiState)
    }

    /** 执行搜索，供 UI 渲染派生结果。 */
    fun search(): SearchResult {
        return searchResultController.search(state = uiState)
    }

    // 搜索结果动作前集中提交非空搜索词，避免各平台 UI 分别维护历史写入规则。
    private fun commitSearchQueryForResultActionIfNeeded() {
        uiState = searchSessionController.commitActiveSearchQueryToHistoryIfNeeded(state = uiState)
    }

    /** 设置主题模式。 */
    fun setThemeMode(themeMode: ThemeMode) {
        uiState = preferenceStateController.setThemeMode(
            state = uiState,
            themeMode = themeMode,
        )
    }

    /** 设置启动时自动扫描偏好。 */
    fun setLocalMusicAutoScanOnLaunchEnabled(isEnabled: Boolean) {
        uiState = preferenceStateController.setLocalMusicAutoScanOnLaunchEnabled(
            state = uiState,
            isEnabled = isEnabled,
        )
    }

    /** 设置短音频过滤偏好。 */
    fun setLocalMusicShortAudioIgnored(isIgnored: Boolean) {
        uiState = preferenceStateController.setLocalMusicShortAudioIgnored(
            state = uiState,
            isIgnored = isIgnored,
        )
    }

    /** 设置系统文件夹排除偏好。 */
    fun setLocalMusicSystemFoldersExcluded(isExcluded: Boolean) {
        uiState = preferenceStateController.setLocalMusicSystemFoldersExcluded(
            state = uiState,
            isExcluded = isExcluded,
        )
    }

    /** 打开队列弹层。 */
    fun openQueue() {
        uiState = LoginAndDialogStateController.openQueue(state = uiState)
    }

    /** 关闭队列弹层。 */
    fun closeQueue() {
        uiState = LoginAndDialogStateController.closeQueue(state = uiState)
    }

    /** 从队列移除歌曲，至少保留一首。 */
    fun removeFromQueue(songId: String) {
        launchPlaybackFactMutation {
            playbackActionController.removeFromQueue(
                state = uiState,
                songId = songId,
            )
        }
    }

    // 所有会改写当前播放事实的公开入口都走同一串行域，避免旧恢复晚到覆盖新动作。
    private fun launchPlaybackFactMutation(block: suspend () -> Unit) {
        playbackRestoreGate.launchPlaybackFactMutation(block = block)
    }

    /** 打开更多操作弹层。 */
    fun openMore(song: Song) {
        uiState = LoginAndDialogStateController.openMore(
            state = uiState,
            songId = song.id,
        )
    }

    /** 关闭更多操作弹层。 */
    fun closeMore() {
        uiState = LoginAndDialogStateController.closeMore(state = uiState)
    }

    /** 打开清理缓存确认。 */
    fun openClearCacheDialog() {
        uiState = LoginAndDialogStateController.openClearCacheDialog(state = uiState)
    }

    /** 关闭清理缓存确认。 */
    fun closeClearCacheDialog() {
        uiState = LoginAndDialogStateController.closeClearCacheDialog(state = uiState)
    }

    /** 确认清理缓存。 */
    fun confirmClearCache() {
        uiState = LoginAndDialogStateController.confirmClearCache(state = uiState)
    }

    /** 更新登录邮箱。 */
    fun setEmail(email: String) {
        uiState = LoginAndDialogStateController.setEmail(
            state = uiState,
            email = email,
        )
    }

    /** 模拟发送登录邮件。 */
    fun sendLoginMail() {
        uiState = LoginAndDialogStateController.sendLoginMail(state = uiState)
    }

    // common fake 演示环境自动填充收藏压力数据；真实平台使用注入仓库，不写入演示收藏。
    private fun resolveLikedSongIdsForScan(): Set<String> {
        if (injectedFavoritesRepository != null || uiState.likedSongIds.isNotEmpty()) {
            return uiState.likedSongIds
        }
        val fakeScanner: FakeLocalMusicScanner = localMusicScanner as? FakeLocalMusicScanner
            ?: return uiState.likedSongIds
        val demoFavoriteSongIds: Set<String> = fakeScanner.demoFavoriteSongIds()
        favoritesRepository.replaceLikedSongIds(songIds = demoFavoriteSongIds)
        return demoFavoriteSongIds
    }

    // 同步播放仓库和 UI 状态，避免多个入口各自写状态。
    private fun syncPlaybackState(playbackState: PlaybackState) {
        reduceUiState { currentState: MusicAppUiState ->
            playbackUiStateSynchronizer.syncPlaybackState(
                state = currentState,
                playbackState = playbackState,
            )
        }
        publishPlaybackUiState()
    }

    // 曲库快照是首页、搜索、收藏和我的页的唯一列表来源。
    private fun syncLibrarySnapshot(snapshot: LibrarySnapshot) {
        reduceUiState { currentState: MusicAppUiState ->
            libraryStateSynchronizer.syncLibrarySnapshot(
                state = currentState,
                snapshot = snapshot,
            )
        }
        playbackRestoreGate.restorePlaybackSnapshotIfPending()
    }

    /** 按需读取完整本地曲库，避免首页冷启动直接打满持久层。 */
    fun loadLocalMusicLibrary() {
        applyContentNavigationResult(
            result = contentNavigationController.loadLocalMusicLibrary(state = uiState),
        )
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
