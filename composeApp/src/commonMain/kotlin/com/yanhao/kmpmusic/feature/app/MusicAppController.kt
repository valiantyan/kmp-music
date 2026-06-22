package com.yanhao.kmpmusic.feature.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yanhao.kmpmusic.data.FakeLocalMusicScanner
import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.data.InMemoryUserPreferencesRepository
import com.yanhao.kmpmusic.data.SeedMusicLibraryRepository
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.domain.repository.FavoritesRepository
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import com.yanhao.kmpmusic.domain.usecase.GetHomeMusicUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.MoveQueueUseCase
import com.yanhao.kmpmusic.domain.usecase.MoveQueueUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.PlaySongUseCase
import com.yanhao.kmpmusic.domain.usecase.PlaySongUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCase
import com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCaseImpl
import com.yanhao.kmpmusic.domain.usecase.ScanStatus
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
    musicLibraryRepository: MusicLibraryRepository = SeedMusicLibraryRepository(),
    private val playbackRepository: PlaybackRepository = InMemoryPlaybackRepository(),
    private val userPreferencesRepository: UserPreferencesRepository = InMemoryUserPreferencesRepository(),
) {
    // 首页数据获取用例。
    private val getHomeMusicUseCase = GetHomeMusicUseCaseImpl(
        musicLibraryRepository = musicLibraryRepository,
    )

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
        localMusicScanner = FakeLocalMusicScanner(),
        musicLibraryRepository = InMemoryMusicLibraryRepository(),
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
        if (uiState.isClearCacheDialogOpen) {
            closeClearCacheDialog()
            return true
        }
        if (uiState.moreSongId != null) {
            closeMore()
            return true
        }
        if (uiState.scanStatus != ScanStatus.Idle) {
            closeScan()
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
        val playbackState: PlaybackState = moveQueueUseCase(
            direction = direction,
            fallbackSongs = uiState.songs,
        )
        syncPlaybackState(playbackState = playbackState)
    }

    /** 切换收藏并同步歌曲状态。 */
    fun toggleFavorite(songId: String) {
        val likedSongIds: Set<String> = toggleFavoriteUseCase(songId = songId)
        uiState = uiState.copy(
            likedSongIds = likedSongIds,
            songs = uiState.songs.map { song ->
                song.copy(isLiked = likedSongIds.contains(song.id))
            },
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
        val nextCurrentSongId: String = if (songId == uiState.currentSongId) {
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

    /** 推进扫描弹层状态。 */
    fun advanceScan() {
        val nextStatus: ScanStatus = scanLocalMusicUseCase(currentStatus = uiState.scanStatus)
        uiState = uiState.copy(
            scanStatus = nextStatus,
        )
    }

    /** 打开扫描弹层。 */
    fun openScan() {
        uiState = uiState.copy(scanStatus = ScanStatus.Scanning)
    }

    /** 关闭扫描弹层。 */
    fun closeScan() {
        uiState = uiState.copy(scanStatus = ScanStatus.Idle)
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
        val homeMusic = getHomeMusicUseCase()
        val playbackState: PlaybackState = playbackRepository.getPlaybackState()
        val queueState: QueueState = playbackRepository.getQueueState()
        val likedSongIds: Set<String> = homeMusic.songs.filter { song ->
            song.isLiked
        }.map { song ->
            song.id
        }.toSet()
        return MusicAppUiState(
            songs = homeMusic.songs,
            albums = homeMusic.albums,
            artists = homeMusic.artists,
            likedSongIds = likedSongIds,
            currentSongId = playbackState.currentSongId,
            isPlaying = playbackState.isPlaying,
            queueSongIds = queueState.songIds,
            themeMode = userPreferencesRepository.getThemeMode(),
        )
    }

    // 同步播放仓库和 UI 状态，避免多个入口各自写状态。
    private fun syncPlaybackState(playbackState: PlaybackState) {
        uiState = uiState.copy(
            currentSongId = playbackState.currentSongId,
            isPlaying = playbackState.isPlaying,
            queueSongIds = playbackRepository.getQueueState().songIds,
        )
    }
}
