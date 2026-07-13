package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.HomeContentSection
import com.yanhao.kmpmusic.feature.components.MobilePrimaryToolbar

/**
 * 手机首页，歌曲、专辑和歌手内容页签分别按 Figma 节点渲染。
 */
@Composable
fun HomeScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    libraryStats: LibraryStats,
    scanState: LocalMusicScanState,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    selectedSection: HomeContentSection,
    currentSongId: String?,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onSection: (HomeContentSection) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = MusicDimens.MobileToolbarHeight + MusicDimens.MobileToolbarBodySpacing,
                bottom = contentPadding.calculateBottomPadding(),
            ),
        ) {
            item(key = "home-filter-chips") {
                HomeFilterChips(
                    selectedSection = selectedSection,
                    onSection = onSection,
                )
            }
            when (selectedSection) {
                HomeContentSection.Songs -> homeSongItems(
                    songs = songs,
                    libraryStats = libraryStats,
                    scanState = scanState,
                    discoveryPlatform = discoveryPlatform,
                    currentSongId = currentSongId,
                    onScan = onScan,
                    onSongPlay = onSongPlay,
                    onMore = onMore,
                    onLike = onLike,
                )
                HomeContentSection.Albums -> homeAlbumItems(
                    albums = albums,
                    songs = songs,
                    currentSongId = currentSongId,
                    scanState = scanState,
                    discoveryPlatform = discoveryPlatform,
                    onScan = onScan,
                    onAlbumOpen = onAlbumOpen,
                )
                HomeContentSection.Artists -> homeArtistItems(
                    artists = artists,
                    scanState = scanState,
                    discoveryPlatform = discoveryPlatform,
                    onScan = onScan,
                    onArtistOpen = onArtistOpen,
                )
            }
        }
        MobilePrimaryToolbar(
            title = selectedSection.title(),
            onSearch = onSearch,
            modifier = Modifier.align(alignment = Alignment.TopCenter),
        )
    }
}

// 歌曲页签保留既有 Figma 歌曲列表节奏。
private fun LazyListScope.homeSongItems(
    songs: List<Song>,
    libraryStats: LibraryStats,
    scanState: LocalMusicScanState,
    discoveryPlatform: LocalMusicDiscoveryPlatform,
    currentSongId: String?,
    onScan: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    item(key = "home-filter-title-gap") {
        Spacer(modifier = Modifier.height(16.dp))
    }
    item(key = "home-title-row") {
        HomeSongSectionHeader(
            songCountText = formatHomeSongCount(
                songs = songs,
                libraryStats = libraryStats,
            ),
        )
    }
    item(key = "home-song-list-gap") {
        Spacer(modifier = Modifier.height(16.dp))
    }
    if (songs.isEmpty()) {
        item(key = "home-empty-songs") {
            HomeEmptySongsCard(
                scanState = scanState,
                discoveryPlatform = discoveryPlatform,
                onScan = onScan,
            )
        }
        return
    }
    items(
        items = songs,
        key = { song: Song -> song.id },
        contentType = { "home-song" },
    ) { song: Song ->
        HomeSongRow(
            song = song,
            isCurrentSong = song.id == currentSongId,
            queueSongs = songs,
            onSongPlay = onSongPlay,
            onMore = onMore,
            onLike = onLike,
        )
        Spacer(modifier = Modifier.height(height = favoritesSongRowGap))
    }
}

// 专辑页签复刻 Figma 双列网格，与本地音乐专辑分段共享同一份专辑数据。
private fun LazyListScope.homeAlbumItems(
    albums: List<Album>,
    songs: List<Song>,
    currentSongId: String?,
    scanState: LocalMusicScanState,
    discoveryPlatform: LocalMusicDiscoveryPlatform,
    onScan: () -> Unit,
    onAlbumOpen: (Album) -> Unit,
) {
    item(key = "home-album-grid-top-gap") {
        Spacer(modifier = Modifier.height(24.dp))
    }
    homeAlbumGridItems(
        albums = albums,
        songs = songs,
        currentSongId = currentSongId,
        scanState = scanState,
        discoveryPlatform = discoveryPlatform,
        onScan = onScan,
        onAlbumOpen = onAlbumOpen,
    )
    item(key = "home-album-grid-bottom-gap") {
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// 歌手页签复刻 Figma 圆形头像列表，与本地音乐歌手分段共享同一份歌手数据。
private fun LazyListScope.homeArtistItems(
    artists: List<Artist>,
    scanState: LocalMusicScanState,
    discoveryPlatform: LocalMusicDiscoveryPlatform,
    onScan: () -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    item(key = "home-artist-list-top-gap") {
        Spacer(modifier = Modifier.height(24.dp))
    }
    homeArtistListItems(
        artists = artists,
        scanState = scanState,
        discoveryPlatform = discoveryPlatform,
        onScan = onScan,
        onArtistOpen = onArtistOpen,
    )
    item(key = "home-artist-list-bottom-gap") {
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// 首页歌曲统计优先使用曲库统计，扫描前或测试数据缺失时退回当前列表数量。
private fun formatHomeSongCount(
    songs: List<Song>,
    libraryStats: LibraryStats,
): String {
    val songCount: Int = if (libraryStats.songCount > 0) {
        libraryStats.songCount
    } else {
        songs.size
    }
    return "$songCount 首歌曲"
}

// 顶部栏标题随首页内容页签切换，保持 Figma 页面语义一致。
private fun HomeContentSection.title(): String {
    return when (this) {
        HomeContentSection.Songs -> "歌曲"
        HomeContentSection.Albums -> "专辑"
        HomeContentSection.Artists -> "歌手"
    }
}
