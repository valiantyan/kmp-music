package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.domain.usecase.ScanStatus

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
 * 二级页面路由。
 */
sealed interface SecondaryScreen {
    data object Search : SecondaryScreen
    data object Player : SecondaryScreen
    data object AlbumDetail : SecondaryScreen
    data object ArtistDetail : SecondaryScreen
    data object Settings : SecondaryScreen
    data object Login : SecondaryScreen
    data object LocalFolder : SecondaryScreen
}

/**
 * App 当前展示页面。
 */
data class NavigationState(
    val rootTab: RootTab = RootTab.Home,
    val secondaryScreen: SecondaryScreen? = null,
    val previousRootTab: RootTab = RootTab.Home,
) {
    /**
     * 是否处于一级页面。
     */
    val isTopLevel: Boolean = secondaryScreen == null
}

/**
 * 全局 UI 状态。
 */
data class MusicAppUiState(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val likedSongIds: Set<String>,
    val currentSongId: String,
    val isPlaying: Boolean,
    val queueSongIds: List<String>,
    val navigationState: NavigationState = NavigationState(),
    val favoriteSection: FavoriteSection = FavoriteSection.Songs,
    val selectedAlbumId: String = "river-year",
    val selectedArtistId: String = "trip",
    val searchQuery: String = "",
    val searchScope: SearchScope = SearchScope.All,
    val themeMode: ThemeMode = ThemeMode.Light,
    val scanStatus: ScanStatus = ScanStatus.Idle,
    val isQueueOpen: Boolean = false,
    val moreSongId: String? = null,
    val isClearCacheDialogOpen: Boolean = false,
    val email: String = "",
    val isMailSent: Boolean = false,
    val toast: String? = null,
) {
    /**
     * 当前播放歌曲，缺失时回退到第一首。
     */
    val currentSong: Song = songs.firstOrNull { song -> song.id == currentSongId } ?: songs.first()

    /**
     * 当前播放队列歌曲。
     */
    val queueSongs: List<Song> = queueSongIds.mapNotNull { songId ->
        songs.firstOrNull { song -> song.id == songId }
    }

    /**
     * 当前专辑详情对象。
     */
    val selectedAlbum: Album = albums.firstOrNull { album -> album.id == selectedAlbumId } ?: albums.first()

    /**
     * 当前歌手详情对象。
     */
    val selectedArtist: Artist = artists.firstOrNull { artist -> artist.id == selectedArtistId } ?: artists.first()
}
