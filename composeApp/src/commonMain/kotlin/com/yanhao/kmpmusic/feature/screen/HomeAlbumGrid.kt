package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.hasSameAlbumTitle
import com.yanhao.kmpmusic.feature.components.CoverArtImage

/**
 * 渲染首页专辑双列网格，数据来源与本地音乐专辑分段保持一致。
 */
internal fun LazyListScope.homeAlbumGridItems(
    albums: List<Album>,
    songs: List<Song>,
    currentSongId: String?,
    scanState: LocalMusicScanState,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    onScan: () -> Unit,
    onAlbumOpen: (Album) -> Unit,
) {
    if (albums.isEmpty()) {
        item(key = "home-empty-albums") {
            HomeEmptyAlbumsCard(
                scanState = scanState,
                discoveryPlatform = discoveryPlatform,
                onScan = onScan,
            )
        }
        return
    }
    val currentAlbumTitle: String? = resolveCurrentAlbumTitle(
        songs = songs,
        currentSongId = currentSongId,
    )
    items(
        items = albums.chunked(size = 2),
        key = { rowAlbums: List<Album> -> rowAlbums.joinToString(separator = "|") { album: Album -> album.id } },
        contentType = { "home-album-row" },
    ) { rowAlbums: List<Album> ->
        HomeAlbumRow(
            rowAlbums = rowAlbums,
            currentAlbumTitle = currentAlbumTitle,
            onAlbumOpen = onAlbumOpen,
        )
        Spacer(modifier = Modifier.height(height = homeAlbumGridGap))
    }
}

// 首页专辑行保持固定两列节奏，末行单项时补空列避免卡片变宽。
@Composable
private fun HomeAlbumRow(
    rowAlbums: List<Album>,
    currentAlbumTitle: String?,
    onAlbumOpen: (Album) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(homeAlbumGridGap),
    ) {
        rowAlbums.forEach { album: Album ->
            HomeAlbumItem(
                album = album,
                isActive = isHomeAlbumActive(
                    album = album,
                    currentAlbumTitle = currentAlbumTitle,
                ),
                onAlbumOpen = onAlbumOpen,
                modifier = Modifier.weight(weight = 1f),
            )
        }
        if (rowAlbums.size == 1) {
            Spacer(modifier = Modifier.weight(weight = 1f))
        }
    }
}

// 首页专辑卡片只表达打开专辑，不在卡片上放额外播放语义。
@Composable
private fun HomeAlbumItem(
    album: Album,
    isActive: Boolean,
    onAlbumOpen: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverShape: RoundedCornerShape = RoundedCornerShape(size = homeAlbumCoverRadius)
    Column(
        modifier = modifier.clickable { onAlbumOpen(album) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 1f)
                .clip(shape = coverShape)
                .background(color = homeAlbumCoverBackgroundColor),
        ) {
            CoverArtImage(
                coverArt = album.coverArt,
                coverImageUri = album.coverImageUri,
                contentDescription = "${album.title} 专辑封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = homeActiveAlbumOverlayColor),
                )
                Box(
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(height = homeAlbumActiveBorderHeight)
                        .background(color = homeAccentColor),
                )
            }
        }
        Text(
            text = album.title,
            color = Color(0xFF191C1D),
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
        Text(
            text = album.artist,
            color = Color(0xFF3D4947),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// 当前播放专辑只由当前歌曲反查，避免引入单独的专辑播放状态。
private fun resolveCurrentAlbumTitle(
    songs: List<Song>,
    currentSongId: String?,
): String? {
    if (currentSongId == null) {
        return null
    }
    return songs.firstOrNull { song: Song -> song.id == currentSongId }?.album
}

// 首页专辑高亮复用专辑归属规则，避免大小写或空白差异导致当前播放专辑漏标。
internal fun isHomeAlbumActive(
    album: Album,
    currentAlbumTitle: String?,
): Boolean {
    if (currentAlbumTitle == null) {
        return false
    }
    return hasSameAlbumTitle(
        firstTitle = album.title,
        secondTitle = currentAlbumTitle,
    )
}
