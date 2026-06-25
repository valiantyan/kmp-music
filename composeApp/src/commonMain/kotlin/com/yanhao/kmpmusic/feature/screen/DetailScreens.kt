package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.AlbumCard
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.PrimaryPill
import com.yanhao.kmpmusic.feature.components.SectionTitle
import com.yanhao.kmpmusic.feature.components.SongRow
import com.yanhao.kmpmusic.feature.components.coverArtPainter

/**
 * 专辑详情页。
 */
@Composable
fun AlbumDetailScreen(
    album: Album,
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onBack: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    val albumSongs: List<Song> = songs.filter { song -> song.album == album.title }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        AppHeader(title = "专辑", onBack = onBack)
        DetailHero(
            title = album.title,
            subtitle = "${album.artist} · ${albumSongs.size} 首",
            tag = "${album.year} · ${album.mood}",
            cover = { Image(painter = coverArtPainter(album.coverArt), contentDescription = "${album.title} 专辑封面", modifier = Modifier.size(126.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryPill(
                text = "播放全部",
                onClick = {
                    albumSongs.firstOrNull()?.let { song: Song ->
                        onSongPlay(song, albumSongs)
                    }
                },
            )
            Text(text = "离线", color = MusicColors.Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        SectionTitle(title = "曲目", meta = "${albumSongs.size} 首")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            albumSongs.forEach { song ->
                SongRow(
                    song = song,
                    isCurrentSong = song.id == currentSongId,
                    currentPlaybackStatus = currentPlaybackStatus,
                    onOpen = { selectedSong: Song -> onSongOpen(selectedSong, albumSongs) },
                    onPlay = { selectedSong: Song -> onSongPlay(selectedSong, albumSongs) },
                    onCurrentSongToggle = onCurrentSongToggle,
                    onMore = onMore,
                    onLike = onLike,
                    dense = true,
                )
            }
        }
    }
}

/**
 * 歌手详情页。
 */
@Composable
fun ArtistDetailScreen(
    artist: Artist,
    songs: List<Song>,
    albums: List<Album>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onBack: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    onAlbumOpen: (Album) -> Unit,
) {
    val artistSongs: List<Song> = songs.filter { song -> song.artist == artist.name }
    val displayedArtistSongs: List<Song> = artistSongs.take(n = 5)
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        AppHeader(title = "歌手", onBack = onBack)
        DetailHero(
            title = artist.name,
            subtitle = "${artist.songCount} 首 · 本地收藏",
            tag = artist.tag,
            cover = { Image(painter = coverArtPainter(artist.coverArt), contentDescription = "${artist.name} 图片", modifier = Modifier.size(126.dp).clip(CircleShape), contentScale = ContentScale.Crop) },
        )
        SectionTitle(title = "热门歌曲")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            displayedArtistSongs.forEach { song ->
                SongRow(
                    song = song,
                    isCurrentSong = song.id == currentSongId,
                    currentPlaybackStatus = currentPlaybackStatus,
                    onOpen = { selectedSong: Song -> onSongOpen(selectedSong, displayedArtistSongs) },
                    onPlay = { selectedSong: Song -> onSongPlay(selectedSong, displayedArtistSongs) },
                    onCurrentSongToggle = onCurrentSongToggle,
                    onMore = onMore,
                    onLike = onLike,
                    dense = true,
                )
            }
        }
        SectionTitle(title = "相关专辑")
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            albums.filter { album -> album.artist == artist.name || artist.name == "旅行团乐队" }.take(3).forEach { album ->
                AlbumCard(album = album, onOpen = onAlbumOpen, modifier = Modifier.weight(weight = 1f))
            }
        }
    }
}

/**
 * 曲库条目缺失时的二级页兜底，避免空库状态崩溃。
 */
@Composable
fun MissingLibraryItemScreen(
    title: String,
    subtitle: String = "重新扫描后再试",
    message: String = "当前曲库中找不到这个条目。",
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        AppHeader(title = title, subtitle = subtitle, onBack = onBack)
        Text(text = message, color = MusicColors.Muted)
    }
}

/**
 * 详情页头部封面与文字组合。
 */
@Composable
private fun DetailHero(
    title: String,
    subtitle: String,
    tag: String,
    cover: @Composable () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Bottom) {
        cover()
        Column {
            Text(text = tag, color = MusicColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = subtitle, color = MusicColors.Muted, fontSize = 14.sp)
        }
    }
}
