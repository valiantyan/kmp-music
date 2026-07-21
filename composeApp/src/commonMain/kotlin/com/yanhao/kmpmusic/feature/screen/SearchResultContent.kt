package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song

// 渲染外层 LazyColumn 提供的一行搜索结果，避免结果区一次性组合所有内容。
@Composable
internal fun SearchResultLazyRowContent(
    row: SearchResultLazyRow,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    currentAlbumTitle: String?,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    when (row) {
        is SearchResultLazyRow.HomeSongItem -> {
            SearchSongResultRow(
                row = row,
                currentSongId = currentSongId,
                currentPlaybackStatus = currentPlaybackStatus,
                onSongPlay = onSongPlay,
                onCurrentSongToggle = onCurrentSongToggle,
                onMore = onMore,
                onLike = onLike,
            )
        }

        is SearchResultLazyRow.HomeAlbumRow -> {
            HomeAlbumRow(
                rowAlbums = row.albums,
                currentAlbumTitle = currentAlbumTitle,
                onAlbumOpen = onAlbumOpen,
            )
        }

        is SearchResultLazyRow.HomeArtistItem -> {
            HomeArtistRow(
                artist = row.artist,
                onArtistOpen = onArtistOpen,
            )
        }

        is SearchResultLazyRow.Message -> {
            SearchNoResultState(message = row.text)
        }
    }
}

// 搜索结果行统一补齐页边距和首行间距，让 LazyColumn 拆项后仍保持原视觉节奏。
@Composable
internal fun SearchResultRowFrame(
    index: Int,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = if (index == 0) 24.dp else 14.dp),
    ) {
        content()
    }
}

// 歌曲结果复用首页歌曲行，搜索只注入当前播放 toggle 语义和搜索队列。
@Composable
private fun SearchSongResultRow(
    row: SearchResultLazyRow.HomeSongItem,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    HomeSongRow(
        song = row.song,
        isCurrentSong = row.song.id == currentSongId,
        queueSongs = row.queueSongs,
        currentPlaybackStatus = currentPlaybackStatus,
        onSongPlay = onSongPlay,
        onCurrentSongToggle = onCurrentSongToggle,
        onMore = onMore,
        onLike = onLike,
    )
}
