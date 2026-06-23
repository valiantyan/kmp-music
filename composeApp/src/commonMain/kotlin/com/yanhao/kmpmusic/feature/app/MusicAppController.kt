package com.yanhao.kmpmusic.feature.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yanhao.kmpmusic.data.FakeLocalMusicScanner
import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.data.InMemoryUserPreferencesRepository
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicScanProgress
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.domain.repository.FavoritesRepository
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import com.yanhao.kmpmusic.domain.usecase.MoveQueueUseCase
import com.yanhao.kmpmusic.domain.usecase.MoveQueueUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.PlaySongUseCase
import com.yanhao.kmpmusic.domain.usecase.PlaySongUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCase
import com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.SearchMusicUseCase
import com.yanhao.kmpmusic.domain.usecase.SearchMusicUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCase
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.TogglePlaybackUseCase
import com.yanhao.kmpmusic.domain.usecase.TogglePlaybackUseCaseImpl

/**
 * App 状态控制器，替代原型中的 React `useState` 集群。
 */
class MusicAppController(
    private val musicLibraryRepository: MusicLibraryRepository = InMemoryMusicLibraryRepository(),
    private val localMusicScanner: LocalMusicScanner = FakeLocalMusicScanner(),
    private val playbackRepository: PlaybackRepository = InMemoryPlaybackRepository(),
    private val userPreferencesRepository: UserPreferencesRepository = InMemoryUserPreferencesRepository(),
    private val permissionSettingsOpener: PermissionSettingsOpener = PermissionSettingsOpener {},
) {
    // 搜索用例。
    private val searchMusicUseCase: SearchMusicUseCase = SearchMusicUseCaseImpl(
        musicLibraryRepository = musicLibraryRepository,
    )

    // 收藏仓库需要依赖初始歌曲，所以在控制器中初始化。
    private val favoritesRepository: FavoritesRepository

    // 切换收藏用例。
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase

    // 播放指定歌曲用例。
    private val playSongUseCase: PlaySongUseCase = PlaySongUseCaseImpl(
        playbackRepository = playbackRepository,
    )

    // 切换播放状态用例。
    private val togglePlaybackUseCase: TogglePlaybackUseCase = TogglePlaybackUseCaseImpl(
        playbackRepository = playbackRepository,
    )

    // 队列切歌用例。
    private val moveQueueUseCase: MoveQueueUseCase = MoveQueueUseCaseImpl(
        playbackRepository = playbackRepository,
    )

    // 本地扫描用例。
    private val scanLocalMusicUseCase: ScanLocalMusicUseCase = ScanLocalMusicUseCaseImpl(
        localMusicScanner = localMusicScanner,
        musicLibraryRepository = musicLibraryRepository,
    )

    /**
     * Compose 可观察 UI 状态。
     */
    var uiState: MusicAppUiState by mutableStateOf(createInitialState())
        private set

    init {
        favoritesRepository = InMemoryFavoritesRepository(
            initialLikedSongIds = uiState.likedSongIds,
        )
        toggleFavoriteUseCase = ToggleFavoriteUseCaseImpl(
            favoritesRepository = favoritesRepository,
        )
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
        navigateToSecondary(screen = SecondaryScreen.LocalMusic(initialSection = section))
    }

    /** 播放歌曲但留在当前页面。 */
    fun playSong(song: Song) {
        val playbackState: PlaybackState = playSongUseCase(song = song)
        syncPlaybackState(playbackState = playbackState)
    }

    /** 打开播放页并播放歌曲。 */
    fun openSong(song: Song) {
        playSong(song = song)
        navigateToSecondary(screen = SecondaryScreen.Player)
    }

    /** 切换播放暂停。 */
    fun togglePlayback() {
        val playbackState: PlaybackState = togglePlaybackUseCase()
        syncPlaybackState(playbackState = playbackState)
    }

    /** 切换上一首或下一首。 */
    fun moveTrack(direction: Int) {
        val playbackState: PlaybackState = moveQueueUseCase(direction = direction)
        syncPlaybackState(playbackState = playbackState)
    }

    /** 切换收藏并同步歌曲状态。 */
    fun toggleFavorite(songId: String) {
        val likedSongIds: Set<String> = toggleFavoriteUseCase(songId = songId)
        val songsWithLikes: List<Song> = uiState.songs.map { song ->
            song.copy(isLiked = likedSongIds.contains(song.id))
        }
        uiState = uiState.copy(
            likedSongIds = likedSongIds,
            songs = songsWithLikes,
            recentSongs = buildRecentSongs(songs = songsWithLikes),
            localSongPreview = songsWithLikes.take(n = 6),
        )
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
        uiState.albums.firstOrNull { album -> album.title == song.album }?.let { album ->
            uiState = uiState.copy(moreSongId = null)
            openAlbum(album = album)
        }
    }

    /** 从歌曲打开歌手详情。 */
    fun openArtistFromSong(song: Song) {
        uiState.artists.firstOrNull { artist -> artist.name == song.artist }?.let { artist ->
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
        if (uiState.queueSongIds.size <= 1) {
            return
        }
        val nextQueueIds: List<String> = uiState.queueSongIds.filterNot { id -> id == songId }
        playbackRepository.saveQueueState(state = QueueState(songIds = nextQueueIds))
        val nextCurrentSongId: String? = if (songId == uiState.currentSongId) {
            nextQueueIds.first()
        } else {
            uiState.currentSongId
        }
        playbackRepository.savePlaybackState(
            state = PlaybackState(
                currentSongId = nextCurrentSongId,
                isPlaying = true,
            ),
        )
        uiState = uiState.copy(
            queueSongIds = nextQueueIds,
            currentSongId = nextCurrentSongId,
            isPlaying = true,
        )
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
        val snapshot: LibrarySnapshot = musicLibraryRepository.getSnapshot()
        val playbackState: PlaybackState = playbackRepository.getPlaybackState()
        val queueState: QueueState = playbackRepository.getQueueState()
        val likedSongIds: Set<String> = snapshot.songs.filter { song ->
            song.isLiked
        }.map { song ->
            song.id
        }.toSet()
        return MusicAppUiState(
            songs = snapshot.songs,
            albums = snapshot.albums,
            artists = snapshot.artists,
            likedSongIds = likedSongIds,
            currentSongId = playbackState.currentSongId,
            isPlaying = playbackState.isPlaying,
            queueSongIds = queueState.songIds,
            libraryStats = snapshot.stats,
            localMusicSources = snapshot.sources,
            localMusicProblems = snapshot.problems,
            recentSongs = buildRecentSongs(songs = snapshot.songs),
            localSongPreview = snapshot.songs.take(n = 6),
            scanState = snapshot.scanState,
            themeMode = userPreferencesRepository.getThemeMode(),
        )
    }

    // 永久拒绝后首页按钮表示“打开权限设置”，再次点击时先弹确认框。
    private fun shouldConfirmPermissionSettingsBeforeScan(): Boolean {
        val scanState: LocalMusicScanState = uiState.scanState
        return scanState is LocalMusicScanState.Error &&
            scanState.error.type == LocalMusicScanErrorType.PermissionPermanentlyDenied
    }

    // 同步播放仓库和 UI 状态，避免多个入口各自写状态。
    private fun syncPlaybackState(playbackState: PlaybackState) {
        uiState = uiState.copy(
            currentSongId = playbackState.currentSongId,
            isPlaying = playbackState.isPlaying,
            queueSongIds = playbackRepository.getQueueState().songIds,
            recentSongs = buildRecentSongs(songs = uiState.songs),
        )
    }

    // 曲库快照是首页、搜索、收藏和我的页的唯一列表来源。
    private fun syncLibrarySnapshot(snapshot: LibrarySnapshot) {
        val likedSongIds: Set<String> = favoritesRepository.getLikedSongIds()
        val songsWithLikes: List<Song> = snapshot.songs.map { song ->
            song.copy(isLiked = likedSongIds.contains(song.id) || song.isLiked)
        }
        uiState = uiState.copy(
            songs = songsWithLikes,
            albums = snapshot.albums,
            artists = snapshot.artists,
            libraryStats = snapshot.stats,
            localMusicSources = snapshot.sources,
            localMusicProblems = snapshot.problems,
            scanState = snapshot.scanState,
            likedSongIds = likedSongIds + songsWithLikes.filter { song -> song.isLiked }.map { song -> song.id },
            recentSongs = buildRecentSongs(songs = songsWithLikes),
            localSongPreview = songsWithLikes.take(n = 6),
        )
    }

    // 最近播放只读取播放历史，不从扫描结果自动生成。
    private fun buildRecentSongs(songs: List<Song>): List<Song> {
        return playbackRepository.getPlaybackHistory().songIds
            .mapNotNull { songId -> songs.firstOrNull { song -> song.id == songId } }
            .take(n = 2)
    }
}
