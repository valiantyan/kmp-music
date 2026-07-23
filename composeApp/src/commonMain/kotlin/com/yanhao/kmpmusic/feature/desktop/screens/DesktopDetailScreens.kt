package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPageHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPrimaryButton
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSongTable

/**
 * 歌手详情页复用桌面表格与统计文案，减少重复布局。
 */
@Composable
internal fun DesktopArtistDetailScreen(
    artist: Artist?,
    songs: List<Song>,
    albums: List<Album>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onBack: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
) {
    val artistSongs: List<Song> =
        artist
            ?.let { selectedArtist: Artist ->
                songs.filter { song: Song -> song.artist == selectedArtist.name }
            }.orEmpty()
    val artistAlbumCount: Int =
        artist?.let { selectedArtist: Artist ->
            albums.count { album: Album -> album.artist == selectedArtist.name }
        } ?: 0
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = artist?.name ?: "歌手不可用",
            eyebrow = "歌曲 ${artistSongs.size} 首，专辑 $artistAlbumCount 张",
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
        }
        DesktopSongTable(
            songs = artistSongs,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongPlay = onSongPlay,
            onCurrentSongToggle = {},
            onMore = onMore,
        )
    }
}

/**
 * 空路由占位只渲染页面标题，避免二级路由为空时复用任意业务页面。
 */
@Composable
fun DesktopEmptyStateScreen(
    title: String,
    subtitle: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = title,
            eyebrow = subtitle,
        )
    }
}
