package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.feature.app.library.MusicLibraryProjector

/**
 * 桌面播放器默认音量(68%)，沿用现有控件初始值并交给全局状态托管。
 */
private const val DEFAULT_PLAYBACK_VOLUME = 0.68f

/**
 * 根导航 Tab。
 */
enum class RootTab {
    Home,
    Favorites,
    Me,
}

/**
 * 手机版首页内容页签。
 */
enum class HomeContentSection {
    Songs,
    Albums,
    Artists,
}

/**
 * 收藏页面分段类型。
 */
enum class FavoriteSection {
    Songs,
    Albums,
    Artists,
}

/**
 * 本地音乐二级页分段。
 */
enum class LocalMusicSection {
    Songs,
    Albums,
    Artists,
    Sources,
}

/**
 * 单曲更多面板来源，用来限制歌单详情页不要再次分发“添加到歌单”。
 */
enum class SongMoreSourceContext {
    General,
    LocalPlaylistDetail,
}

/**
 * 添加到歌单流程的临时弹窗状态。
 *
 * @property songId 本次要加入歌单的歌曲标识。
 * @property isCreateDialogOpen 是否已经进入新建歌单弹窗。
 * @property newPlaylistName 新建歌单输入框当前名称。
 * @property newPlaylistNameError 新建名称校验失败时展示的错误文案。
 */
data class AddToPlaylistFlowState(
    val songId: String,
    val isCreateDialogOpen: Boolean = false,
    val newPlaylistName: String = "",
    val newPlaylistNameError: String? = null,
)

/**
 * 手机端固定底栏的整体位置策略。
 */
enum class MobileFixedBarPlacement {
    TopLevel,
    MiniPlayerOnly,
    Hidden,
}

/**
 * 页面内容底部避让策略。
 */
enum class ContentBottomSpace {
    TopLevel,
    SecondaryWithMiniPlayer,
    Fullscreen,
}

/**
 * 手机端页面固定底栏策略，统一管理底部 Tab、迷你播放器和页面留白。
 */
enum class MobileFixedBarMode(
    val showsBottomNavigation: Boolean,
    val fixedBarPlacement: MobileFixedBarPlacement,
    val contentBottomSpace: ContentBottomSpace,
    val coversUnderlyingChrome: Boolean,
) {
    TopLevel(
        showsBottomNavigation = true,
        fixedBarPlacement = MobileFixedBarPlacement.TopLevel,
        contentBottomSpace = ContentBottomSpace.TopLevel,
        coversUnderlyingChrome = false,
    ),
    SecondaryWithMiniPlayer(
        showsBottomNavigation = false,
        fixedBarPlacement = MobileFixedBarPlacement.MiniPlayerOnly,
        contentBottomSpace = ContentBottomSpace.SecondaryWithMiniPlayer,
        coversUnderlyingChrome = false,
    ),
    SecondaryWithoutChrome(
        showsBottomNavigation = false,
        fixedBarPlacement = MobileFixedBarPlacement.Hidden,
        contentBottomSpace = ContentBottomSpace.Fullscreen,
        coversUnderlyingChrome = true,
    ),
    Player(
        showsBottomNavigation = false,
        fixedBarPlacement = MobileFixedBarPlacement.Hidden,
        contentBottomSpace = ContentBottomSpace.Fullscreen,
        coversUnderlyingChrome = true,
    ),
}

/**
 * 二级页面路由。
 */
sealed interface SecondaryScreen {
    data class Search(val context: SearchContext = SearchContext.LocalLibrary) : SecondaryScreen
    data object Player : SecondaryScreen
    data object AlbumDetail : SecondaryScreen
    data object ArtistDetail : SecondaryScreen
    data object Settings : SecondaryScreen
    data object About : SecondaryScreen
    data object Login : SecondaryScreen
    data object AudioScan : SecondaryScreen
    data object RecentPlayed : SecondaryScreen
    data class LocalMusic(val initialSection: LocalMusicSection = LocalMusicSection.Songs) : SecondaryScreen
}

/**
 * 二级页面栈条目，保存页面和它自己的滚动隔离 id。
 */
data class SecondaryStackEntry(
    val screen: SecondaryScreen,
    val entryId: Int,
)

/**
 * App 当前展示页面。
 */
data class NavigationState(
    val rootTab: RootTab = RootTab.Home,
    val secondaryScreen: SecondaryScreen? = null,
    val previousRootTab: RootTab = RootTab.Home,
    val secondaryEntryId: Int = 0,
    val secondaryBackStack: List<SecondaryStackEntry> = emptyList(),
) {
    /**
     * 是否处于一级页面。
     */
    val isTopLevel: Boolean = secondaryScreen == null

    /**
     * 当前页面对应的手机端固定底栏策略。
     *
     * 这里是二级页面到底部固定栏表现的唯一配置入口：新增页面时优先在这里归类，
     * 不要在页面 Composable 或固定底栏周围散写显示/隐藏判断。
     */
    val fixedBarMode: MobileFixedBarMode = mobileFixedBarModeFor(screen = secondaryScreen)

    /**
     * 当前需要压在底层 chrome 上方的页面；为空时说明当前页自身承载 chrome。
     */
    val chromeOverlayScreen: SecondaryScreen? = secondaryScreen.takeIf {
        fixedBarMode.coversUnderlyingChrome
    }

    /**
     * 覆盖页下方真正承载固定底栏的二级页；没有二级页时由一级页承载。
     */
    val chromeUnderlaySecondaryScreen: SecondaryScreen? = if (chromeOverlayScreen == null) {
        secondaryScreen
    } else {
        secondaryBackStack.lastOrNull()?.screen
    }

    /**
     * 覆盖页下方页面的滚动隔离 id，保证返回时不会丢失上一层状态。
     */
    val chromeUnderlayEntryId: Int = if (chromeOverlayScreen == null) {
        secondaryEntryId
    } else {
        secondaryBackStack.lastOrNull()?.entryId ?: 0
    }

    /**
     * 固定底栏按底层页面计算，避免无 chrome 覆盖页触发底栏下滑隐藏动画。
     */
    val chromeUnderlayFixedBarMode: MobileFixedBarMode = mobileFixedBarModeFor(
        screen = chromeUnderlaySecondaryScreen,
    )

    /**
     * 当前页面滚动状态隔离 key，一级页按 Tab 保留，二级页每次进入都从顶部重新开始。
     */
    val scrollStateKey: String = buildScrollStateKey(
        rootTab = rootTab,
        secondaryScreen = secondaryScreen,
        entryId = secondaryEntryId,
    )

    /**
     * 底层页面滚动状态 key，供覆盖页打开时继续渲染上一层页面。
     */
    val chromeUnderlayScrollStateKey: String = buildScrollStateKey(
        rootTab = rootTab,
        secondaryScreen = chromeUnderlaySecondaryScreen,
        entryId = chromeUnderlayEntryId,
    )
}

/**
 * 页面到手机端底部 chrome 的唯一归类入口。
 */
private fun mobileFixedBarModeFor(screen: SecondaryScreen?): MobileFixedBarMode {
    return when (screen) {
        null -> MobileFixedBarMode.TopLevel
        SecondaryScreen.Player -> MobileFixedBarMode.Player
        SecondaryScreen.About,
        SecondaryScreen.AudioScan,
        -> MobileFixedBarMode.SecondaryWithoutChrome
        is SecondaryScreen.Search,
        SecondaryScreen.AlbumDetail,
        SecondaryScreen.ArtistDetail,
        SecondaryScreen.Settings,
        SecondaryScreen.Login,
        SecondaryScreen.RecentPlayed,
        is SecondaryScreen.LocalMusic,
        -> MobileFixedBarMode.SecondaryWithMiniPlayer
    }
}

/**
 * 构造页面滚动状态 key，一级页按 Tab，二级页按进入次数隔离。
 */
private fun buildScrollStateKey(
    rootTab: RootTab,
    secondaryScreen: SecondaryScreen?,
    entryId: Int,
): String {
    return when (secondaryScreen) {
        null -> "root:${rootTab.name}"
        else -> "secondary:${secondaryScreen.routeName()}:$entryId"
    }
}

/**
 * 二级页面稳定路由名，用于保存页面级 UI 状态，避免依赖平台反射能力。
 */
private fun SecondaryScreen.routeName(): String {
    return when (this) {
        is SecondaryScreen.Search -> "Search:${context.name}"
        SecondaryScreen.Player -> "Player"
        SecondaryScreen.AlbumDetail -> "AlbumDetail"
        SecondaryScreen.ArtistDetail -> "ArtistDetail"
        SecondaryScreen.Settings -> "Settings"
        SecondaryScreen.About -> "About"
        SecondaryScreen.Login -> "Login"
        SecondaryScreen.AudioScan -> "AudioScan"
        SecondaryScreen.RecentPlayed -> "RecentPlayed"
        is SecondaryScreen.LocalMusic -> "LocalMusic:${initialSection.name}"
    }
}

/**
 * 全局 UI 状态。
 */
data class MusicAppUiState(
    val homeLocalSongPreview: List<Song> = emptyList(),
    val localSongs: List<Song> = emptyList(),
    val localAlbums: List<Album> = emptyList(),
    val localArtists: List<Artist> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val queueSongsSnapshot: List<Song> = emptyList(),
    val likedSongIds: Set<String>,
    val currentSongId: String?,
    val playbackStatus: PlaybackStatus,
    val playbackPositionMs: Long = 0L,
    val playbackDurationMs: Long? = null,
    val playbackMode: PlaybackMode = PlaybackMode.LoopAll,
    val playbackVolume: Float = DEFAULT_PLAYBACK_VOLUME,
    val playbackError: PlaybackError? = null,
    val queueSongIds: List<String>,
    val libraryStats: LibraryStats = LibraryStats(),
    val localMusicSources: List<LocalMusicSourceSummary> = emptyList(),
    val localMusicProblems: List<LocalMusicProblem> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
    val scanState: LocalMusicScanState = LocalMusicScanState.Idle,
    val navigationState: NavigationState = NavigationState(),
    val homeContentSection: HomeContentSection = HomeContentSection.Songs,
    val favoriteSection: FavoriteSection = FavoriteSection.Songs,
    val selectedAlbumId: String = "river-year",
    val selectedArtistId: String = "trip",
    val searchQuery: String = "",
    val activeSearchQuery: String = "",
    val searchScope: SearchScope = SearchScope.All,
    val searchContext: SearchContext = SearchContext.LocalLibrary,
    val localLibrarySearchHistory: List<String> = emptyList(),
    val favoritesSearchHistory: List<String> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.Light,
    val localMusicDiscoveryPreferences: LocalMusicDiscoveryPreferences = LocalMusicDiscoveryPreferences(),
    val isQueueOpen: Boolean = false,
    val moreSongId: String? = null,
    val moreSongSourceContext: SongMoreSourceContext = SongMoreSourceContext.General,
    val addToPlaylistFlow: AddToPlaylistFlowState? = null,
    val transientMessage: String? = null,
    val isPermissionSettingsDialogOpen: Boolean = false,
    val isClearCacheDialogOpen: Boolean = false,
    val email: String = "",
    val isMailSent: Boolean = false,
) {
    /**
     * Desktop 顶部音乐搜索只在内容型一级页出现。
     */
    val shouldShowTitlebarMusicSearch: Boolean
        get() = navigationState.secondaryScreen == null &&
            (navigationState.rootTab == RootTab.Home || navigationState.rootTab == RootTab.Favorites)

    val songs: List<Song>
        get() = localSongs.ifEmpty { homeLocalSongPreview }

    /**
     * 独立扫描页展示的可播放曲目总数，应与曲库统计保持同一口径。
     */
    val audioScanPlayableSongCount: Int
        get() = maxOf(a = libraryStats.songCount, b = localSongs.size)

    val albums: List<Album>
        get() = localAlbums

    val artists: List<Artist>
        get() = localArtists

    val localSongPreview: List<Song>
        get() = homeLocalSongPreview

    val detailSongs: List<Song>
        get() = MusicLibraryProjector.buildDetailSongs(
            queueSongsSnapshot = queueSongsSnapshot,
            localSongs = localSongs,
            homeLocalSongPreview = homeLocalSongPreview,
            favoriteSongs = favoriteSongs,
        )

    /**
     * 当前搜索上下文对应的历史记录。
     */
    fun searchHistoryFor(context: SearchContext = searchContext): List<String> {
        return when (context) {
            SearchContext.LocalLibrary -> localLibrarySearchHistory
            SearchContext.Favorites -> favoritesSearchHistory
        }
    }

    val detailAlbums: List<Album>
        get() = (localAlbums + MusicLibraryProjector.buildAlbums(songs = detailSongs))
            .distinctBy { album -> album.id }

    val detailArtists: List<Artist>
        get() = (localArtists + MusicLibraryProjector.buildArtists(songs = detailSongs))
            .distinctBy { artist -> artist.id }

    /**
     * 当前是否处于真实播放态，只用于业务判断，不能用来决定按钮图标。
     */
    val isPlaying: Boolean
        get() = playbackStatus == PlaybackStatus.Playing

    /**
     * UI 是否应显示暂停入口，启动和缓冲阶段也保持暂停按钮避免误导用户。
     */
    val shouldShowPauseControl: Boolean
        get() = playbackStatus.shouldShowPauseControl

    /**
     * 当前仍有播放上下文时，平台层不能把暂态 [PlaybackStatus.Idle] 当成清空队列信号。
     */
    val hasActivePlaybackSession: Boolean
        get() = currentSongId != null || queueSongIds.isNotEmpty()

    /**
     * 当前播放歌曲，没有真实播放时不显示迷你播放器。
     */
    val currentSong: Song? = currentSongId?.let { songId ->
        queueSongsSnapshot.firstOrNull { song -> song.id == songId }
            ?: localSongs.firstOrNull { song -> song.id == songId }
            ?: homeLocalSongPreview.firstOrNull { song -> song.id == songId }
            ?: favoriteSongs.firstOrNull { song -> song.id == songId }
    }

    /**
     * 当前播放队列歌曲。
     */
    val queueSongs: List<Song> = queueSongIds.mapNotNull { songId ->
        queueSongsSnapshot.firstOrNull { song -> song.id == songId }
            ?: localSongs.firstOrNull { song -> song.id == songId }
            ?: homeLocalSongPreview.firstOrNull { song -> song.id == songId }
            ?: favoriteSongs.firstOrNull { song -> song.id == songId }
    }

    /**
     * 收藏专辑列表，直接由收藏歌曲投影，避免依赖全量本地曲库是否已加载。
     */
    val favoriteAlbums: List<Album>
        get() = MusicLibraryProjector.buildAlbums(songs = favoriteSongs)

    /**
     * 收藏歌手列表，直接由收藏歌曲投影，避免依赖全量本地曲库是否已加载。
     */
    val favoriteArtists: List<Artist>
        get() = MusicLibraryProjector.buildArtists(songs = favoriteSongs)

    /**
     * 系统返回键是否应由 App 内部消费。
     */
    val canHandleSystemBack: Boolean =
        isPermissionSettingsDialogOpen ||
            isClearCacheDialogOpen ||
            transientMessage != null ||
            addToPlaylistFlow != null ||
            moreSongId != null ||
            isQueueOpen ||
            !navigationState.isTopLevel

    /**
     * 当前专辑详情对象，曲库为空或专辑缺失时为 null。
     */
    val selectedAlbum: Album? = detailAlbums.firstOrNull { album -> album.id == selectedAlbumId }

    /**
     * 当前歌手详情对象，曲库为空或歌手缺失时为 null。
     */
    val selectedArtist: Artist? = detailArtists.firstOrNull { artist -> artist.id == selectedArtistId }
}
