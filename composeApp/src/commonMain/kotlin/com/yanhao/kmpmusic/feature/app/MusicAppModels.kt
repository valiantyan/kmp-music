package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.ThemeMode

/**
 * 根导航 Tab。
 */
enum class RootTab {
    Home,
    Favorites,
    Me,
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
 * 底部全局 chrome 的整体位置策略。
 */
enum class BottomChromePlacement {
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
 * 页面全局 chrome 策略，统一管理底部 Tab、迷你播放器和页面留白。
 */
enum class AppChromeMode(
    val showsBottomNavigation: Boolean,
    val bottomChromePlacement: BottomChromePlacement,
    val contentBottomSpace: ContentBottomSpace,
) {
    TopLevel(
        showsBottomNavigation = true,
        bottomChromePlacement = BottomChromePlacement.TopLevel,
        contentBottomSpace = ContentBottomSpace.TopLevel,
    ),
    SecondaryWithMiniPlayer(
        showsBottomNavigation = false,
        bottomChromePlacement = BottomChromePlacement.MiniPlayerOnly,
        contentBottomSpace = ContentBottomSpace.SecondaryWithMiniPlayer,
    ),
    SecondaryFullscreen(
        showsBottomNavigation = false,
        bottomChromePlacement = BottomChromePlacement.Hidden,
        contentBottomSpace = ContentBottomSpace.Fullscreen,
    ),
}

/**
 * 二级页面路由。
 */
sealed interface SecondaryScreen {
    data object Search : SecondaryScreen
    data object Player : SecondaryScreen
    data object AlbumDetail : SecondaryScreen
    data object ArtistDetail : SecondaryScreen
    data object Settings : SecondaryScreen
    data object Login : SecondaryScreen
    data class LocalMusic(val initialSection: LocalMusicSection = LocalMusicSection.Songs) : SecondaryScreen
}

/**
 * App 当前展示页面。
 */
data class NavigationState(
    val rootTab: RootTab = RootTab.Home,
    val secondaryScreen: SecondaryScreen? = null,
    val previousRootTab: RootTab = RootTab.Home,
    val secondaryEntryId: Int = 0,
) {
    /**
     * 是否处于一级页面。
     */
    val isTopLevel: Boolean = secondaryScreen == null

    /**
     * 当前页面对应的全局 chrome 策略。
     *
     * 这里是二级页面到底部 chrome 表现的唯一配置入口：新增页面时优先在这里归类，
     * 不要在页面 Composable 或 [BottomChrome] 周围散写显示/隐藏判断。
     */
    val chromeMode: AppChromeMode = when (secondaryScreen) {
        // 一级页面：同时显示 mini player 和底部 Tab。
        null -> AppChromeMode.TopLevel
        // 沉浸式二级页面：mini player 和底部 Tab 都隐藏。
        SecondaryScreen.Player,
        SecondaryScreen.Settings,
        -> AppChromeMode.SecondaryFullscreen
        // 普通二级页面：只显示 mini player，底部 Tab 隐藏。
        SecondaryScreen.Search,
        SecondaryScreen.AlbumDetail,
        SecondaryScreen.ArtistDetail,
        SecondaryScreen.Login,
        is SecondaryScreen.LocalMusic,
        -> AppChromeMode.SecondaryWithMiniPlayer
    }

    /**
     * 当前页面滚动状态隔离 key，一级页按 Tab 保留，二级页每次进入都从顶部重新开始。
     */
    val scrollStateKey: String = when (secondaryScreen) {
        null -> "root:${rootTab.name}"
        else -> "secondary:${secondaryScreen.routeName()}:$secondaryEntryId"
    }
}

/**
 * 二级页面稳定路由名，用于保存页面级 UI 状态，避免依赖平台反射能力。
 */
private fun SecondaryScreen.routeName(): String {
    return when (this) {
        SecondaryScreen.Search -> "Search"
        SecondaryScreen.Player -> "Player"
        SecondaryScreen.AlbumDetail -> "AlbumDetail"
        SecondaryScreen.ArtistDetail -> "ArtistDetail"
        SecondaryScreen.Settings -> "Settings"
        SecondaryScreen.Login -> "Login"
        is SecondaryScreen.LocalMusic -> "LocalMusic:${initialSection.name}"
    }
}

/**
 * 全局 UI 状态。
 */
data class MusicAppUiState(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val likedSongIds: Set<String>,
    val currentSongId: String?,
    val isPlaying: Boolean,
    val queueSongIds: List<String>,
    val libraryStats: LibraryStats = LibraryStats(),
    val localMusicSources: List<LocalMusicSourceSummary> = emptyList(),
    val localMusicProblems: List<LocalMusicProblem> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
    val localSongPreview: List<Song> = emptyList(),
    val scanState: LocalMusicScanState = LocalMusicScanState.Idle,
    val navigationState: NavigationState = NavigationState(),
    val favoriteSection: FavoriteSection = FavoriteSection.Songs,
    val selectedAlbumId: String = "river-year",
    val selectedArtistId: String = "trip",
    val searchQuery: String = "",
    val searchScope: SearchScope = SearchScope.All,
    val themeMode: ThemeMode = ThemeMode.Light,
    val isQueueOpen: Boolean = false,
    val moreSongId: String? = null,
    val isClearCacheDialogOpen: Boolean = false,
    val email: String = "",
    val isMailSent: Boolean = false,
) {
    /**
     * 当前播放歌曲，没有真实播放时不显示迷你播放器。
     */
    val currentSong: Song? = currentSongId?.let { songId ->
        songs.firstOrNull { song -> song.id == songId }
    }

    /**
     * 当前播放队列歌曲。
     */
    val queueSongs: List<Song> = queueSongIds.mapNotNull { songId ->
        songs.firstOrNull { song -> song.id == songId }
    }

    /**
     * 系统返回键是否应由 App 内部消费。
     */
    val canHandleSystemBack: Boolean =
        isClearCacheDialogOpen ||
            moreSongId != null ||
            isQueueOpen ||
            !navigationState.isTopLevel

    /**
     * 当前专辑详情对象，曲库为空或专辑缺失时为 null。
     */
    val selectedAlbum: Album? = albums.firstOrNull { album -> album.id == selectedAlbumId }

    /**
     * 当前歌手详情对象，曲库为空或歌手缺失时为 null。
     */
    val selectedArtist: Artist? = artists.firstOrNull { artist -> artist.id == selectedArtistId }
}
