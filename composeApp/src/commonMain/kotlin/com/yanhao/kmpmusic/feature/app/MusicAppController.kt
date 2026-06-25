package com.yanhao.kmpmusic.feature.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yanhao.kmpmusic.data.FakeAudioPlayerEngine
import com.yanhao.kmpmusic.data.FakeLocalMusicScanner
import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
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
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
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
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCase
import com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.SearchMusicUseCase
import com.yanhao.kmpmusic.domain.usecase.SearchMusicUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCase
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCaseImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

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
    private val permissionSettingsOpener: PermissionSettingsOpener = PermissionSettingsOpener {},
    private val controllerScope: CoroutineScope,
    private val nowMillis: () -> Long = { 0L },
) {
    // 搜索用例。
    private val searchMusicUseCase: SearchMusicUseCase = SearchMusicUseCaseImpl(
        musicLibraryRepository = musicLibraryRepository,
    )

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
        uiState = uiState.copy(
            navigationState = uiState.navigationState.copy(
                secondaryScreen = screen,
                previousRootTab = uiState.navigationState.rootTab,
                secondaryEntryId = uiState.navigationState.secondaryEntryId + 1,
            ),
            isQueueOpen = false,
            moreSongId = null,
        )
    }

    /** 切换一级 Tab。 */
    fun navigateToRoot(tab: RootTab) {
        uiState = uiState.copy(
            navigationState = NavigationState(
                rootTab = tab,
                previousRootTab = tab,
            ),
            isQueueOpen = false,
            moreSongId = null,
        )
    }

    /** 从二级页面返回上一个一级页面。 */
    fun navigateBack() {
        uiState = uiState.copy(
            navigationState = uiState.navigationState.copy(
                rootTab = uiState.navigationState.previousRootTab,
                secondaryScreen = null,
            ),
        )
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

    /** 播放歌曲但留在当前页面，未显式传列表时优先复用当前队列上下文。 */
    fun playSong(song: Song, queueSongs: List<Song> = emptyList()) {
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

    /**
     * Android 播放 service 退出前，通过协调器补写最终暂停快照，避免恢复时丢掉最后位置。
     */
    fun persistPlaybackSnapshotForServiceTeardown(positionMs: Long, durationMs: Long?) {
        playbackCoordinator.persistSnapshotForServiceTeardown(
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
        uiState = uiState.copy(selectedAlbumId = album.id)
        navigateToSecondary(screen = SecondaryScreen.AlbumDetail)
    }

    /** 打开歌手详情。 */
    fun openArtist(artist: Artist) {
        uiState = uiState.copy(selectedArtistId = artist.id)
        navigateToSecondary(screen = SecondaryScreen.ArtistDetail)
    }

    /** 从歌曲打开专辑详情。 */
    fun openAlbumFromSong(song: Song) {
        uiState.detailAlbums.firstOrNull { album -> album.title == song.album }?.let { album ->
            uiState = uiState.copy(moreSongId = null)
            openAlbum(album = album)
        }
    }

    /** 从歌曲打开歌手详情。 */
    fun openArtistFromSong(song: Song) {
        uiState.detailArtists.firstOrNull { artist -> artist.name == song.artist }?.let { artist ->
            uiState = uiState.copy(moreSongId = null)
            openArtist(artist = artist)
        }
    }

    /** 更新收藏页分段。 */
    fun setFavoriteSection(section: FavoriteSection) {
        uiState = uiState.copy(favoriteSection = section)
    }

    /** 更新搜索关键词。 */
    fun setSearchQuery(query: String) {
        uiState = uiState.copy(searchQuery = query)
    }

    /** 更新搜索范围。 */
    fun setSearchScope(scope: SearchScope) {
        uiState = uiState.copy(searchScope = scope)
    }

    /** 执行搜索，供 UI 渲染派生结果。 */
    fun search(): com.yanhao.kmpmusic.domain.usecase.SearchResult {
        return searchMusicUseCase(
            query = uiState.searchQuery,
            scope = uiState.searchScope,
        )
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
            recentSongs = buildRecentSongs(songs = uiState.localSongs.ifEmpty { uiState.homeLocalSongPreview }),
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
            localAlbums = if (shouldRefreshFullLibrary) buildAlbums(songs = fullSongsWithLikes) else uiState.localAlbums,
            localArtists = if (shouldRefreshFullLibrary) buildArtists(songs = fullSongsWithLikes) else uiState.localArtists,
            libraryStats = musicLibraryRepository.getLibraryStats(),
            localMusicSources = snapshot.sources,
            localMusicProblems = snapshot.problems,
            scanState = snapshot.scanState,
            likedSongIds = likedSongIds + previewWithLikes.filter { song -> song.isLiked }.map { song -> song.id },
            recentSongs = buildRecentSongs(songs = fullSongsWithLikes.ifEmpty { previewWithLikes }),
            favoriteSongs = buildFavoriteSongs(
                likedSongIds = likedSongIds,
                preferredSongs = previewWithLikes + fullSongsWithLikes + uiState.queueSongsSnapshot + uiState.favoriteSongs,
            ),
        )
        restorePlaybackSnapshotIfPending()
    }

    // 最近播放只读取播放历史，不从扫描结果自动生成。
    private fun buildRecentSongs(songs: List<Song>): List<Song> {
        return playbackRepository.getPlaybackHistory().songIds
            .mapNotNull { songId -> songs.firstOrNull { song -> song.id == songId } }
            .take(n = 2)
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
        val likedSongIds = favoritesRepository.getLikedSongIds()
        val songsWithLikes = musicLibraryRepository.getAllAvailableSongs().map { song ->
            song.copy(isLiked = likedSongIds.contains(song.id) || song.isLiked)
        }
        uiState = uiState.copy(
            localSongs = songsWithLikes,
            localAlbums = buildAlbums(songs = songsWithLikes),
            localArtists = buildArtists(songs = songsWithLikes),
            favoriteSongs = buildFavoriteSongs(
                likedSongIds = likedSongIds,
                preferredSongs = uiState.homeLocalSongPreview + songsWithLikes + uiState.queueSongsSnapshot + uiState.favoriteSongs,
            ),
            likedSongIds = likedSongIds + songsWithLikes.filter { song -> song.isLiked }.map { song -> song.id },
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

    /** 复用持久化仓库的专辑分组规则，确保首页、二级页和统计口径一致。 */
    private fun buildAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { song: Song -> song.album.trim().lowercase() }
            .values
            .map { albumSongs: List<Song> ->
                val firstSong: Song = albumSongs.first()
                Album(
                    id = "album:${firstSong.album.trim().lowercase()}",
                    title = firstSong.album,
                    artist = firstSong.artist,
                    songCount = albumSongs.size,
                    coverArt = firstSong.coverArt,
                    mood = "本地音乐",
                    year = "本地",
                )
            }
            .sortedBy { album: Album -> album.title.lowercase() }
    }

    /** 复用持久化仓库的歌手分组规则，避免不同入口读到不同聚合结果。 */
    private fun buildArtists(songs: List<Song>): List<Artist> {
        return songs.groupBy { song: Song -> song.artist.trim().lowercase() }
            .values
            .map { artistSongs: List<Song> ->
                val firstSong: Song = artistSongs.first()
                Artist(
                    id = "artist:${firstSong.artist.trim().lowercase()}",
                    name = firstSong.artist,
                    songCount = artistSongs.size,
                    coverArt = firstSong.coverArt,
                    tag = "本地音乐",
                )
            }
            .sortedBy { artist: Artist -> artist.name.lowercase() }
    }
}
