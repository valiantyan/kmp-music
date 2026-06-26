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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.FavoriteSection

/**
 * 桌面首页直接消费共享曲库与播放状态，避免再派生一份桌面专用首页模型。
 */
@Composable
fun DesktopLocalMusicRootScreen(
    songs: List<Song>,
    albums: List<Album>,
    libraryStats: LibraryStats,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onScan: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "本地音乐",
            eyebrow = "已扫描 ${libraryStats.songCount} 首歌曲，${libraryStats.albumCount} 张专辑，${libraryStats.artistCount} 位歌手",
        ) {
            DesktopPrimaryButton(
                text = "↻ 重新扫描",
                onClick = onScan,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DesktopStatCard(
                icon = "♫",
                title = "歌曲",
                value = libraryStats.songCount.toString(),
                modifier = Modifier.weight(1f),
            )
            DesktopStatCard(
                icon = "●",
                title = "专辑",
                value = libraryStats.albumCount.toString(),
                modifier = Modifier.weight(1f),
            )
            DesktopStatCard(
                icon = "♟",
                title = "歌手",
                value = libraryStats.artistCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DesktopPrimaryButton(
                text = "▶ 播放全部",
                onClick = {
                    songs.firstOrNull()?.let { song: Song -> onSongPlay(song, songs) }
                },
            )
        }
        DesktopSongTable(
            songs = songs,
            currentSongId = currentSongId,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = onCurrentSongToggle,
            onMore = onMore,
        )
    }
}

/**
 * 收藏页继续复用共享收藏分段和收藏列表，桌面端只负责排版。
 */
@Composable
fun DesktopFavoritesRootScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    section: FavoriteSection,
    currentSongId: String?,
    onSection: (FavoriteSection) -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    val likedSongs: List<Song> = songs.filter { song: Song -> song.isLiked }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DesktopStatCard(
                icon = "♫",
                title = "收藏歌曲",
                value = likedSongs.size.toString(),
                modifier = Modifier.weight(1f),
            )
            DesktopStatCard(
                icon = "●",
                title = "收藏专辑",
                value = albums.size.toString(),
                modifier = Modifier.weight(1f),
            )
            DesktopStatCard(
                icon = "♟",
                title = "收藏歌手",
                value = artists.size.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        DesktopSongTable(
            songs = likedSongs,
            currentSongId = currentSongId,
            showFavoriteColumn = true,
            trailingDateLabel = "收藏时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = onCurrentSongToggle,
            onMore = onMore,
            onLike = onLike,
        )
    }
}

/**
 * 我的页先承接共享统计和导航动作，详细设置内容留给后续二级页任务接入。
 */
@Composable
fun DesktopMeRootScreen(
    albums: List<Album>,
    artists: List<Artist>,
    libraryStats: LibraryStats,
    favoriteCount: Int,
    onLogin: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "我的",
            eyebrow = "本地资料与同步状态",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DesktopStatCard(
                icon = "♫",
                title = "本地专辑",
                value = libraryStats.albumCount.toString(),
                modifier = Modifier.weight(1f),
            )
            DesktopStatCard(
                icon = "●",
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
                icon = "◷",
                title = "最近播放",
                value = "最近",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        DesktopPrimaryButton(
            text = "✓ 立即登录",
            onClick = onLogin,
        )
        Spacer(modifier = Modifier.height(18.dp))
        DesktopPrimaryButton(
            text = "设置",
            onClick = onSettings,
        )
        if (albums.isNotEmpty() || artists.isNotEmpty()) {
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

/**
 * 在二级页面尚未接入前保留统一占位，避免桌面工作区退回临时文案。
 */
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
