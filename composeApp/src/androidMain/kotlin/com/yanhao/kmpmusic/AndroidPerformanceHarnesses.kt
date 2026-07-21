package com.yanhao.kmpmusic

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.core.theme.KmpMusicTheme
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.feature.app.FavoriteSection
import com.yanhao.kmpmusic.feature.screen.AlbumDetailScreen
import com.yanhao.kmpmusic.feature.screen.FavoritesScreen

/**
 * Android debug 专辑详情性能监控页，供 [MainActivity] 通过显式 intent 挂载。
 */
@Composable
internal fun AndroidAlbumDetailPerformanceHarness(onBack: () -> Unit) {
    val album: Album = remember { createAlbumDetailPerformanceAlbum() }
    val songs: List<Song> = remember { createAlbumDetailPerformanceSongs(album = album) }
    KmpMusicTheme(themeMode = ThemeMode.Light) {
        AlbumDetailScreen(
            album = album,
            songs = songs,
            currentSongId = null,
            currentPlaybackStatus = PlaybackStatus.Paused,
            onBack = onBack,
            onSongPlay = { _: Song, _: List<Song> -> },
            onCurrentSongToggle = {},
            onMore = {},
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
        )
    }
}

/**
 * Android debug 收藏页性能监控页，提供 500 条收藏和删除压力场景。
 */
@Composable
internal fun AndroidFavoritesPerformanceHarness() {
    var favoriteSongs: List<Song> by remember { mutableStateOf(value = createFavoritesPerformanceSongs()) }
    KmpMusicTheme(themeMode = ThemeMode.Light) {
        FavoritesScreen(
            songs = favoriteSongs,
            currentSongId = null,
            section = FavoriteSection.Songs,
            onSection = {},
            onSongPlay = { _: Song, _: List<Song> -> },
            onMore = {},
            onLike = { songId: String ->
                favoriteSongs = favoriteSongs.filterNot { song: Song -> song.id == songId }
            },
            onSearch = {},
            onAlbumOpen = { _: Album -> },
            onArtistOpen = { _: Artist -> },
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
        )
    }
}

// 构造滑动性能监控使用的固定专辑。
private fun createAlbumDetailPerformanceAlbum(): Album =
    Album(
        id = "album:performance-river-year",
        title = "River Year Performance",
        artist = "Trip",
        songCount = ALBUM_DETAIL_PERFORMANCE_SONG_COUNT,
        coverArt = CoverArt.AlbumRiverYear,
        mood = "性能监控",
        year = "Debug",
    )

// 构造滑动性能监控使用的固定专辑歌曲，避免真实详情页组件携带 demo 追加逻辑。
private fun createAlbumDetailPerformanceSongs(album: Album): List<Song> =
    (1..ALBUM_DETAIL_PERFORMANCE_SONG_COUNT).map { index: Int ->
        createAlbumDetailPerformanceSong(
            album = album,
            index = index,
        )
    }

// 构造滑动性能监控使用的单首专辑歌曲。
private fun createAlbumDetailPerformanceSong(
    album: Album,
    index: Int,
): Song {
    val sourceId: String = index.toString().padStart(length = 3, padChar = '0')
    return Song(
        id = "album-performance:$sourceId",
        title = "Performance Track $sourceId",
        artist = album.artist,
        album = album.title,
        duration = "3:00",
        coverArt = album.coverArt,
        isLiked = false,
        lastPlayed = "测试数据",
        quality = "Debug",
        lyric = "专辑详情滑动性能监控歌曲",
        trackNumber = index,
        durationMs = 180_000L,
        sourceId = sourceId,
        sourceKind = LocalMusicSourceKind.FakeScanner,
        localUri = "fake://album-detail-performance/$sourceId",
    )
}

// 构造 500 条收藏页性能监控歌曲，全部保持已收藏状态。
private fun createFavoritesPerformanceSongs(): List<Song> =
    (1..FAVORITES_PERFORMANCE_SONG_COUNT).map { index: Int ->
        val sourceId: String = index.toString().padStart(length = 3, padChar = '0')
        Song(
            id = "favorites-performance:$sourceId",
            title = "收藏压力测试 $sourceId",
            artist = "Demo Artist ${((index - 1) % 20) + 1}",
            album = "收藏压力专辑 ${((index - 1) % 40) + 1}",
            duration = "3:${(index % 60).toString().padStart(length = 2, padChar = '0')}",
            coverArt = CoverArt.HeroLocalMusic,
            isLiked = true,
            lastPlayed = "测试数据",
            quality = "Debug",
            lyric = "收藏页滑动性能监控歌曲",
            trackNumber = index,
            durationMs = 180_000L + ((index % 120) * 1_000L),
            sourceId = sourceId,
            sourceKind = LocalMusicSourceKind.FakeScanner,
            localUri = "fake://favorites-performance/$sourceId",
        )
    }

/**
 * 收藏页压力测试歌曲数量。
 */
private const val FAVORITES_PERFORMANCE_SONG_COUNT: Int = 500

/**
 * 专辑详情性能监控固定歌曲数量。
 */
private const val ALBUM_DETAIL_PERFORMANCE_SONG_COUNT: Int = 500
