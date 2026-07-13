package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.LocalPlaylistDetailDisplayModel
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.components.MobileSecondaryPage

/**
 * 移动端本地自建歌单详情页，按专辑详情页形态展示当前仍可播放歌曲。
 */
@Composable
fun LocalPlaylistDetailScreen(
    detail: LocalPlaylistDetailDisplayModel,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onSongPlay: (Song) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    MobileSecondaryPage(
        title = detail.name,
        onBack = onBack,
        backgroundColor = albumDetailBackgroundColor,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(weight = 1f),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 40.dp,
            ),
        ) {
        item(key = "local-playlist-header", contentType = "local-playlist-header") {
            LocalPlaylistDetailHeader(detail = detail)
        }
        item(key = "local-playlist-play-all", contentType = "local-playlist-play-all") {
            AlbumDetailPlayAllButton(
                text = "播放全部",
                countText = "${detail.availableSongCount}首",
                enabled = detail.canPlayAll,
                onClick = onPlayAll,
            )
        }
        item(key = "local-playlist-song-list-top-gap", contentType = "local-playlist-gap") {
            Spacer(modifier = Modifier.height(height = 32.dp))
        }
        if (detail.songs.isEmpty()) {
            item(key = "local-playlist-empty", contentType = "local-playlist-empty") {
                LocalPlaylistDetailEmptyState(text = detail.emptyText)
            }
            return@LazyColumn
        }
        itemsIndexed(
            items = detail.songs,
            key = { _: Int, song: Song -> song.id },
            contentType = { _: Int, _: Song -> "local-playlist-detail-song" },
        ) { index: Int, song: Song ->
            val rowState: AlbumDetailSongRowState = buildAlbumDetailSongRowState(
                index = index,
                song = song,
                isCurrentSong = song.id == currentSongId,
            )
            AlbumDetailSongRow(
                rowState = rowState,
                isCurrentSong = song.id == currentSongId,
                currentPlaybackStatus = currentPlaybackStatus,
                onSongPlay = onSongPlay,
                onCurrentSongToggle = onCurrentSongToggle,
                onMore = onMore,
            )
        }
        }
    }
}

// 头部与专辑详情保持同尺寸结构，但副标题使用当前可用歌曲数量。
@Composable
private fun LocalPlaylistDetailHeader(detail: LocalPlaylistDetailDisplayModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LocalPlaylistDetailCover(detail = detail)
        Text(
            text = detail.name,
            color = albumDetailTextColor,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 32.dp, end = 20.dp),
        )
        Text(
            text = "${detail.availableSongCount} 首歌曲",
            color = albumDetailMetaColor,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )
    }
}

// 封面优先使用第一首可用歌曲封面，空歌单使用默认封面。
@Composable
private fun LocalPlaylistDetailCover(detail: LocalPlaylistDetailDisplayModel) {
    Box(
        modifier = Modifier.size(size = detailHeroCoverSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .offset(y = detailHeroCoverGlowOffsetY)
                .size(width = detailHeroCoverGlowWidth, height = detailHeroCoverGlowHeight)
                .blur(radius = detailHeroCoverGlowBlurRadius)
                .background(
                    color = albumDetailActionColor.copy(alpha = 0.20f),
                    shape = detailHeroCoverShape,
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = detailHeroCoverShape)
                .border(
                    border = BorderStroke(
                        width = detailHeroCoverBorderWidth,
                        color = Color.White.copy(alpha = DETAIL_HERO_COVER_BORDER_ALPHA),
                    ),
                    shape = detailHeroCoverShape,
                )
                .padding(all = detailHeroCoverPadding),
        ) {
            CoverArtImage(
                coverArt = detail.coverArt,
                coverImageUri = detail.coverImageUri,
                contentDescription = "${detail.name} 歌单封面",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape = detailHeroCoverInnerShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

// 空态放在歌曲列表区域，播放全部按钮仍保留在上方但不可点击。
@Composable
private fun LocalPlaylistDetailEmptyState(text: String) {
    Text(
        text = text,
        color = albumDetailMetaColor,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 32.dp),
    )
}
