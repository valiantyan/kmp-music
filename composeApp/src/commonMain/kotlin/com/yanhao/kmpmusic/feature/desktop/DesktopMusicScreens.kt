package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.FavoriteSection

private const val HOME_ALBUM_PREVIEW_COUNT = 4
private const val FAVORITE_ALBUM_PREVIEW_COUNT = 4
private const val ARTIST_STRIP_COUNT = 4

@Composable
// 本地音乐首页只展示播放历史反推的最近专辑，避免把全库误标成最近播放。
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
    onSongOpen: (Song, List<Song>) -> Unit,
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
            onSongOpen = onSongOpen,
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

@Composable
fun DesktopFavoritesRootScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    section: FavoriteSection,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSection: (FavoriteSection) -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    val likedSongs: List<Song> = songs.filter { song: Song -> song.isLiked }
    val playAllLabel: String = rootPlayAllLabel(
        songs = likedSongs,
        currentSongId = currentSongId,
        currentPlaybackStatus = currentPlaybackStatus,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "收藏",
            eyebrow = "喜欢的音乐都在这里",
        ) {
            DesktopSegmentedControl(
                labels = listOf("歌曲", "专辑", "歌手"),
                selectedIndex = section.ordinal,
                onSelect = { index: Int -> onSection(FavoriteSection.entries[index]) },
            )
        }
        DesktopThreeStatRow(
            firstTitle = "收藏歌曲",
            firstValue = likedSongs.size.toString(),
            secondTitle = "收藏专辑",
            secondValue = albums.size.toString(),
            thirdTitle = "收藏歌手",
            thirdValue = artists.size.toString(),
        )
        Spacer(modifier = Modifier.height(22.dp))
        when (section) {
            FavoriteSection.Songs -> {
                DesktopToolbar(
                    playAllLabel = playAllLabel,
                    sortLabel = "排序：最近收藏",
                    onPlayAll = {
                        playOrToggleRootCollection(
                            songs = likedSongs,
                            currentSongId = currentSongId,
                            currentPlaybackStatus = currentPlaybackStatus,
                            onSongPlay = onSongPlay,
                            onCurrentSongToggle = onCurrentSongToggle,
                        )
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
                DesktopSongTable(
                    songs = likedSongs,
                    currentSongId = currentSongId,
                    currentPlaybackStatus = currentPlaybackStatus,
                    showFavoriteColumn = true,
                    trailingDateLabel = "收藏时间",
                    onSongOpen = onSongOpen,
                    onSongPlay = onSongPlay,
                    onCurrentSongToggle = onCurrentSongToggle,
                    onMore = onMore,
                    onLike = onLike,
                )
                if (albums.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    DesktopSectionHeader(
                        title = "收藏的专辑",
                        actionLabel = "查看全部",
                        onAction = { onSection(FavoriteSection.Albums) },
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DesktopAlbumGrid(
                        albums = albums.take(FAVORITE_ALBUM_PREVIEW_COUNT),
                        onAlbumOpen = onAlbumOpen,
                    )
                }
            }
            FavoriteSection.Albums -> {
                DesktopSectionHeader(
                    title = "收藏的专辑",
                )
                Spacer(modifier = Modifier.height(14.dp))
                DesktopAlbumGrid(
                    albums = albums,
                    onAlbumOpen = onAlbumOpen,
                )
            }
            FavoriteSection.Artists -> {
                DesktopSectionHeader(
                    title = "收藏的歌手",
                )
                Spacer(modifier = Modifier.height(14.dp))
                DesktopArtistStrip(
                    artists = artists.take(ARTIST_STRIP_COUNT),
                    onArtistOpen = onArtistOpen,
                )
                if (artists.size > ARTIST_STRIP_COUNT) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        artists.drop(ARTIST_STRIP_COUNT).forEach { artist: Artist ->
                            DesktopContentRow(
                                icon = Icons.Rounded.Person,
                                title = artist.name,
                                subtitle = "${artist.songCount} 首歌曲 · ${artist.tag}",
                                actionLabel = "打开",
                                onClick = { onArtistOpen(artist) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
// 我的页汇总账号、收藏与最近播放概览，最近播放必须来自真实播放历史。
fun DesktopMeRootScreen(
    albums: List<Album>,
    recentSongs: List<Song>,
    artists: List<Artist>,
    libraryStats: LibraryStats,
    favoriteCount: Int,
    onLogin: () -> Unit,
    onFavorites: () -> Unit,
    onFolders: () -> Unit,
    onSettings: () -> Unit,
    onBrowseAlbums: () -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    val recentAlbums: List<Album> = buildRecentAlbums(
        recentSongs = recentSongs,
        albums = albums,
    )
    val frequentArtists: List<Artist> = buildFrequentArtists(
        recentSongs = recentSongs,
        artists = artists,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "我的",
            eyebrow = "本地资料与同步状态",
        )
        DesktopProfilePanel(
            title = "登录音乐账号",
            description = "使用 Supabase 同步收藏、播放记录和多端资料，让你的音乐在所有设备上保持一致。",
            buttonText = "立即登录",
            onClick = onLogin,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DesktopStatCard(
                icon = "●",
                title = "本地专辑",
                value = libraryStats.albumCount.toString(),
                modifier = Modifier.weight(1f),
            )
            DesktopStatCard(
                icon = "♟",
                title = "歌手",
                value = libraryStats.artistCount.toString(),
                modifier = Modifier.weight(1f),
            )
            DesktopStatCard(
                icon = "♥",
                title = "收藏",
                value = favoriteCount.toString(),
                modifier = Modifier.weight(1f),
            )
            DesktopStatCard(
                icon = "♫",
                title = "最近播放",
                value = recentSongs.size.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DesktopContentRow(
                icon = DesktopContentRowFavoritesIcon,
                title = "我的收藏",
                subtitle = "查看收藏的歌曲、专辑和歌手",
                actionLabel = "查看全部",
                onClick = onFavorites,
            )
            DesktopContentRow(
                icon = Icons.Rounded.Person,
                title = "常听歌手",
                subtitle = "你常听的歌手",
                actionLabel = null,
                onClick = null,
                extraContent = {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (frequentArtists.isNotEmpty()) {
                        DesktopArtistStrip(
                            artists = frequentArtists.take(ARTIST_STRIP_COUNT),
                            onArtistOpen = onArtistOpen,
                        )
                    } else {
                        DesktopSectionEmptyMessage(
                            message = "播放后会在这里显示你最近常听的歌手。",
                        )
                    }
                },
            )
            DesktopContentRow(
                icon = DesktopContentRowFolderIcon,
                title = "本地文件夹",
                subtitle = "管理你的本地音乐文件与目录",
                actionLabel = "管理",
                onClick = onFolders,
            )
            DesktopContentRow(
                icon = DesktopContentRowSyncIcon,
                title = "同步与备份",
                subtitle = "同步状态、备份与恢复选项",
                actionLabel = "设置",
                onClick = onSettings,
            )
        }
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
                message = "还没有最近播放的专辑，先播放一些音乐吧。",
            )
        }
    }
}

@Composable
fun DesktopEmptyStateScreen(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = title,
            eyebrow = subtitle,
        )
    }
}

@Composable
private fun DesktopThreeStatRow(
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

private fun rootPlayAllLabel(
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

private fun playOrToggleRootCollection(
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
private fun buildRecentAlbums(
    recentSongs: List<Song>,
    albums: List<Album>,
): List<Album> {
    val albumsByTitle: Map<String, List<Album>> = albums.groupBy { album ->
        normalizeDesktopLookupKey(album.title)
    }
    return recentSongs.mapNotNull { song: Song ->
        val normalizedAlbumTitle: String = normalizeDesktopLookupKey(song.album)
        val normalizedArtistName: String = normalizeDesktopLookupKey(song.artist)
        val titleMatches: List<Album> = albumsByTitle[normalizedAlbumTitle].orEmpty()
        titleMatches.firstOrNull { album ->
            normalizeDesktopLookupKey(album.artist) == normalizedArtistName
        } ?: titleMatches.singleOrNull()
    }
        .distinctBy { album -> album.id }
        .take(HOME_ALBUM_PREVIEW_COUNT)
}

private fun buildFrequentArtists(
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
        val coverArt: com.yanhao.kmpmusic.domain.model.CoverArt,
    )

    val artistsByNormalizedName: Map<String, Artist> = artists.associateBy { artist ->
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
                )
            } else {
                existing.copy(recentCount = existing.recentCount + 1)
            }
            acc
        }
    return recentArtistStats.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, RecentArtistAccumulator>> { entry ->
                entry.value.recentCount
            }.thenBy { entry ->
                entry.value.firstRecentIndex
            },
        )
        .map { entry ->
            val recentArtist: RecentArtistAccumulator = entry.value
            artistsByNormalizedName[entry.key]?.copy(songCount = recentArtist.recentCount)
                ?: Artist(
                    id = "artist:${entry.key}",
                    name = recentArtist.name,
                    songCount = recentArtist.recentCount,
                    coverArt = recentArtist.coverArt,
                    tag = "最近播放",
                )
        }
}

private fun normalizeDesktopLookupKey(value: String): String {
    return value.trim().lowercase()
}

/**
 * 最近播放区为空时的轻提示，避免用全库内容冒充最近播放。
 */
@Composable
private fun DesktopSectionEmptyMessage(
    message: String,
) {
    androidx.compose.material3.Text(
        text = message,
        color = DesktopMusicColors.MutedStrong,
        fontSize = DesktopMusicType.Eyebrow,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
