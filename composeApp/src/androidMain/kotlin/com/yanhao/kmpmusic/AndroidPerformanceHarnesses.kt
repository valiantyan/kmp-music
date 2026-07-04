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
import com.yanhao.kmpmusic.feature.screen.ALBUM_DETAIL_DEMO_SONG_COUNT
import com.yanhao.kmpmusic.feature.screen.AlbumDetailScreen
import com.yanhao.kmpmusic.feature.screen.FavoritesScreen

/**
 * Android debug 专辑详情性能监控页，供 [MainActivity] 通过显式 intent 挂载。
 */
@Composable
internal fun AndroidAlbumDetailPerformanceHarness(onBack: () -> Unit) {
    val album: Album = remember { createAlbumDetailPerformanceAlbum() }
    val songs: List<Song> = remember { listOf(createAlbumDetailPerformanceSong(album = album)) }
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
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            demoSongCount = ALBUM_DETAIL_DEMO_SONG_COUNT,
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
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        )
    }
}

// 构造滑动性能监控使用的固定专辑。
private fun createAlbumDetailPerformanceAlbum(): Album {
    return Album(
        id = "album:performance-river-year",
        title = "River Year Performance",
        artist = "Trip",
        songCount = ALBUM_DETAIL_DEMO_SONG_COUNT + 1,
        coverArt = CoverArt.AlbumRiverYear,
        mood = "性能监控",
        year = "Debug",
    )
}

// 构造滑动性能监控使用的真实种子曲目，剩余曲目由专辑详情 demo 生成器追加。
private fun createAlbumDetailPerformanceSong(album: Album): Song {
    return Song(
        id = "album-performance:001",
        title = "Performance Seed Track",
        artist = album.artist,
        album = album.title,
        duration = "3:00",
        coverArt = album.coverArt,
        isLiked = false,
        lastPlayed = "测试数据",
        quality = "Debug",
        lyric = "专辑详情滑动性能监控种子歌曲",
        trackNumber = 1,
        durationMs = 180_000L,
        sourceId = "album-performance:001",
        sourceKind = LocalMusicSourceKind.FakeScanner,
        localUri = "fake://album-detail-performance/001",
    )
}

// 构造 500 条收藏页性能监控歌曲，全部保持已收藏状态。
private fun createFavoritesPerformanceSongs(): List<Song> {
    return (1..FAVORITES_PERFORMANCE_SONG_COUNT).map { index: Int ->
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
}

/**
 * 收藏页压力测试歌曲数量。
 */
private const val FAVORITES_PERFORMANCE_SONG_COUNT: Int = 500
