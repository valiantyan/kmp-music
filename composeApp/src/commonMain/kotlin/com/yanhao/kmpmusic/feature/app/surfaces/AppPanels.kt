package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.components.SongRow

/**
 * 跨端复用的全局底部面板。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppPanels(
    state: MusicAppUiState,
    controller: MusicAppController,
) {
    if (state.isQueueOpen) {
        ModalBottomSheet(onDismissRequest = controller::closeQueue) {
            val queueSongs: List<Song> = state.queueSongs
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(all = 21.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "queue-title") {
                    Text(text = "播放队列", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                }
                items(
                    items = queueSongs,
                    key = { song: Song -> song.id },
                    contentType = { "queue-song" },
                ) { song: Song ->
                    SongRow(
                        song = song,
                        isCurrentSong = song.id == state.currentSongId,
                        currentPlaybackStatus = state.playbackStatus,
                        onOpen = { selectedSong: Song ->
                            controller.playSong(
                                song = selectedSong,
                                queueSongs = queueSongs,
                            )
                        },
                        onPlay = { selectedSong: Song ->
                            controller.playSong(
                                song = selectedSong,
                                queueSongs = queueSongs,
                            )
                        },
                        onCurrentSongToggle = controller::togglePlayback,
                        onMore = controller::openMore,
                        dense = true,
                    )
                }
            }
        }
    }
    state.moreSongId?.let { songId ->
        val song: Song? = state.currentSong?.takeIf { item -> item.id == songId }
            ?: state.queueSongs.firstOrNull { item -> item.id == songId }
            ?: state.localSongs.firstOrNull { item -> item.id == songId }
            ?: state.homeLocalSongPreview.firstOrNull { item -> item.id == songId }
            ?: state.favoriteSongs.firstOrNull { item -> item.id == songId }
        if (song != null) {
            ModalBottomSheet(onDismissRequest = controller::closeMore) {
                Column(modifier = Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(
                        text = song.title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BottomSheetAction("加入收藏", Icons.Rounded.Favorite) { controller.toggleFavorite(song.id) }
                    BottomSheetAction("查看专辑", Icons.Rounded.LibraryMusic) { controller.openAlbumFromSong(song) }
                    BottomSheetAction("查看歌手", Icons.Rounded.Person) { controller.openArtistFromSong(song) }
                }
            }
        }
    }
}

/**
 * 更多操作面板中的单行动作。
 */
@Composable
private fun BottomSheetAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = MusicColors.Soft, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null)
            Text(text = label, fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.MoreHoriz, contentDescription = null, tint = MusicColors.Muted)
        }
    }
}
