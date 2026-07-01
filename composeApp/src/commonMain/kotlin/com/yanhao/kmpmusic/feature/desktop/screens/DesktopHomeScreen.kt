package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.components.DesktopAlbumGrid
import com.yanhao.kmpmusic.feature.desktop.components.DesktopMoreButton
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPageHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopScanIcon
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSecondaryButton
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionEmptyMessage
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSongTable
import com.yanhao.kmpmusic.feature.desktop.components.DesktopStatCard
import com.yanhao.kmpmusic.feature.desktop.components.DesktopToolbar

private const val HOME_ALBUM_PREVIEW_COUNT = 4

/**
 * 本地音乐首页只展示播放历史反推的最近专辑，避免把全库误标成最近播放。
 */
@Composable
fun DesktopLocalMusicRootScreen(
    songs: List<Song>,
    albums: List<Album>,
    recentSongs: List<Song>,
    libraryStats: LibraryStats,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onScan: () -> Unit,
    onBrowseLibrary: () -> Unit,
    onBrowseAlbums: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
) {
    val playAllLabel: String = rootPlayAllLabel(
        songs = songs,
        currentSongId = currentSongId,
        currentPlaybackStatus = currentPlaybackStatus,
    )
    val recentAlbums: List<Album> = buildRecentAlbums(
        recentSongs = recentSongs,
        albums = albums,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "本地音乐",
            eyebrow = "已扫描 ${libraryStats.songCount} 首歌曲，${libraryStats.albumCount} 张专辑，${libraryStats.artistCount} 位歌手",
        ) {
            DesktopSecondaryButton(
                text = "重新扫描",
                icon = DesktopScanIcon,
                onClick = onScan,
            )
            DesktopMoreButton(onClick = onBrowseLibrary)
        }
        DesktopThreeStatRow(
            firstTitle = "歌曲",
            firstValue = libraryStats.songCount.toString(),
            secondTitle = "专辑",
            secondValue = libraryStats.albumCount.toString(),
            thirdTitle = "歌手",
            thirdValue = libraryStats.artistCount.toString(),
        )
        Spacer(modifier = Modifier.height(22.dp))
        DesktopToolbar(
            playAllLabel = playAllLabel,
            sortLabel = "排序：最近添加",
            onPlayAll = {
                playOrToggleRootCollection(
                    songs = songs,
                    currentSongId = currentSongId,
                    currentPlaybackStatus = currentPlaybackStatus,
                    onSongPlay = onSongPlay,
                    onCurrentSongToggle = onCurrentSongToggle,
                )
            },
        )
        Spacer(modifier = Modifier.height(14.dp))
        DesktopSongTable(
            songs = songs,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongPlay = onSongPlay,
            onCurrentSongToggle = onCurrentSongToggle,
            onMore = onMore,
        )
        Spacer(modifier = Modifier.height(24.dp))
        DesktopSectionHeader(
            title = "最近播放的专辑",
            actionLabel = "查看全部",
            onAction = onBrowseAlbums,
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (recentAlbums.isNotEmpty()) {
            DesktopAlbumGrid(
                albums = recentAlbums,
                onAlbumOpen = onAlbumOpen,
            )
        } else {
            DesktopSectionEmptyMessage(
                message = "播放后会在这里显示最近听过的专辑。",
            )
        }
    }
}

/**
 * 三列统计行在桌面首页与收藏页保持相同权重，避免页面切换时卡片宽度跳动。
 */
@Composable
internal fun DesktopThreeStatRow(
    firstTitle: String,
    firstValue: String,
    secondTitle: String,
    secondValue: String,
    thirdTitle: String,
    thirdValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DesktopStatCard(
            icon = "♫",
            title = firstTitle,
            value = firstValue,
            modifier = Modifier.weight(1f),
        )
        DesktopStatCard(
            icon = "●",
            title = secondTitle,
            value = secondValue,
            modifier = Modifier.weight(1f),
        )
        DesktopStatCard(
            icon = "♟",
            title = thirdTitle,
            value = thirdValue,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 根页面播放全部按钮根据当前队列归属显示可预期的动作文案。 */
internal fun rootPlayAllLabel(
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
): String {
    val containsCurrentSong: Boolean = songs.any { song: Song -> song.id == currentSongId }
    if (!containsCurrentSong) {
        return "播放全部"
    }
    return when (currentPlaybackStatus) {
        PlaybackStatus.Playing,
        PlaybackStatus.Buffering,
        PlaybackStatus.Loading,
        -> "暂停播放"
        PlaybackStatus.Paused,
        PlaybackStatus.Ended,
        PlaybackStatus.Idle,
        PlaybackStatus.Error,
        -> "继续播放"
    }
}

/** 根页面播放全部按钮优先切换当前队列，未命中时从列表首曲开始播放。 */
internal fun playOrToggleRootCollection(
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
) {
    val containsCurrentSong: Boolean = songs.any { song: Song -> song.id == currentSongId }
    if (containsCurrentSong && currentPlaybackStatus != PlaybackStatus.Error) {
        onCurrentSongToggle()
        return
    }
    songs.firstOrNull()?.let { song: Song -> onSongPlay(song, songs) }
}

/**
 * 根据最近播放歌曲反推最近播放的专辑，按歌曲顺序保留首次出现的专辑。
 */
internal fun buildRecentAlbums(
    recentSongs: List<Song>,
    albums: List<Album>,
): List<Album> {
    val albumsByTitle: Map<String, List<Album>> = albums.groupBy { album: Album ->
        normalizeDesktopLookupKey(album.title)
    }
    return recentSongs.mapNotNull { song: Song ->
        val normalizedAlbumTitle: String = normalizeDesktopLookupKey(song.album)
        val normalizedArtistName: String = normalizeDesktopLookupKey(song.artist)
        val titleMatches: List<Album> = albumsByTitle[normalizedAlbumTitle].orEmpty()
        titleMatches.firstOrNull { album: Album ->
            normalizeDesktopLookupKey(album.artist) == normalizedArtistName
        } ?: titleMatches.singleOrNull()
    }
        .distinctBy { album: Album -> album.id }
        .take(HOME_ALBUM_PREVIEW_COUNT)
}

/**
 * 根据最近播放歌曲统计常听歌手，缺失歌手资料时保留歌曲里的封面信息。
 */
internal fun buildFrequentArtists(
    recentSongs: List<Song>,
    artists: List<Artist>,
): List<Artist> {
    if (recentSongs.isEmpty()) {
        return emptyList()
    }
    data class RecentArtistAccumulator(
        val name: String,
        val recentCount: Int,
        val firstRecentIndex: Int,
        val coverArt: CoverArt,
        val coverImageUri: String?,
    )
    val artistsByNormalizedName: Map<String, Artist> = artists.associateBy { artist: Artist ->
        normalizeDesktopLookupKey(artist.name)
    }
    val recentArtistStats: Map<String, RecentArtistAccumulator> =
        recentSongs.withIndex().fold(mutableMapOf()) { acc, indexedSong ->
            val normalizedArtistName: String = normalizeDesktopLookupKey(indexedSong.value.artist)
            val existing: RecentArtistAccumulator? = acc[normalizedArtistName]
            acc[normalizedArtistName] = if (existing == null) {
                RecentArtistAccumulator(
                    name = indexedSong.value.artist,
                    recentCount = 1,
                    firstRecentIndex = indexedSong.index,
                    coverArt = indexedSong.value.coverArt,
                    coverImageUri = indexedSong.value.coverImageUri,
                )
            } else {
                existing.copy(recentCount = existing.recentCount + 1)
            }
            acc
        }
    return recentArtistStats.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, RecentArtistAccumulator>> { entry: Map.Entry<String, RecentArtistAccumulator> ->
                entry.value.recentCount
            }.thenBy { entry: Map.Entry<String, RecentArtistAccumulator> ->
                entry.value.firstRecentIndex
            },
        )
        .map { entry: Map.Entry<String, RecentArtistAccumulator> ->
            val recentArtist: RecentArtistAccumulator = entry.value
            artistsByNormalizedName[entry.key]?.copy(songCount = recentArtist.recentCount)
                ?: Artist(
                    id = "artist:${entry.key}",
                    name = recentArtist.name,
                    songCount = recentArtist.recentCount,
                    coverArt = recentArtist.coverArt,
                    coverImageUri = recentArtist.coverImageUri,
                    tag = "最近播放",
                )
        }
}

/** 桌面端最近播放匹配使用统一规整规则，避免大小写或空格造成重复。 */
private fun normalizeDesktopLookupKey(value: String): String {
    return value.trim().lowercase()
}
