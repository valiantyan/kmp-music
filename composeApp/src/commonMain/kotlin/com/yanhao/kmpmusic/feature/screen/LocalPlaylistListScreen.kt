package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.feature.app.LocalPlaylistCardDisplayModel
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.CoverArtImage

/**
 * 移动端本地自建歌单列表页，复用首页专辑卡片的双列视觉节奏。
 */
@Composable
fun LocalPlaylistListScreen(
    playlists: List<LocalPlaylistCardDisplayModel>,
    onBack: () -> Unit,
    onManage: () -> Unit,
    onPlaylistOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(color = meBackgroundColor),
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(space = 20.dp),
    ) {
        item(key = "local-playlist-list-header", contentType = "local-playlist-header") {
            AppHeader(
                title = "我的歌单",
                subtitle = buildLocalPlaylistCountSummary(playlists = playlists),
                onBack = onBack,
                actionIcon = Icons.AutoMirrored.Rounded.PlaylistAddCheck,
                actionContentDescription = "管理歌单",
                onAction = onManage,
            )
        }
        if (playlists.isEmpty()) {
            item(key = "local-playlist-list-empty", contentType = "local-playlist-empty") {
                LocalPlaylistListEmptyState()
            }
            return@LazyColumn
        }
        items(
            items = playlists.chunked(size = 2),
            key = { rowPlaylists: List<LocalPlaylistCardDisplayModel> ->
                rowPlaylists.joinToString(separator = "|") { playlist: LocalPlaylistCardDisplayModel -> playlist.id }
            },
            contentType = { "local-playlist-row" },
        ) { rowPlaylists: List<LocalPlaylistCardDisplayModel> ->
            LocalPlaylistRow(
                rowPlaylists = rowPlaylists,
                onPlaylistOpen = onPlaylistOpen,
            )
        }
    }
}

/**
 * 歌单列表页顶部摘要只描述当前可见事实，不暴露未提供交互的排序能力。
 */
internal fun buildLocalPlaylistCountSummary(playlists: List<LocalPlaylistCardDisplayModel>): String {
    return "共 ${playlists.size} 个歌单"
}

// 空态只作为防御兜底；正常入口会在 0 歌单时停留我的页并提示。
@Composable
private fun LocalPlaylistListEmptyState() {
    Text(
        text = "暂无歌单",
        color = homeMutedColor,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

// 双列行沿用首页专辑网格列距，末行单项补空列避免卡片变宽。
@Composable
private fun LocalPlaylistRow(
    rowPlaylists: List<LocalPlaylistCardDisplayModel>,
    onPlaylistOpen: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(space = homeAlbumGridGap),
    ) {
        rowPlaylists.forEach { playlist: LocalPlaylistCardDisplayModel ->
            LocalPlaylistCard(
                playlist = playlist,
                onPlaylistOpen = onPlaylistOpen,
                modifier = Modifier.weight(weight = 1f),
            )
        }
        if (rowPlaylists.size == 1) {
            Spacer(modifier = Modifier.weight(weight = 1f))
        }
    }
}

// 卡片只打开浏览详情，不提供新建或空歌单创建入口。
@Composable
private fun LocalPlaylistCard(
    playlist: LocalPlaylistCardDisplayModel,
    onPlaylistOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverShape: RoundedCornerShape = RoundedCornerShape(size = homeAlbumCoverRadius)
    Column(
        modifier = modifier.clickable { onPlaylistOpen(playlist.id) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 1f)
                .clip(shape = coverShape)
                .background(color = homeAlbumCoverBackgroundColor),
        ) {
            CoverArtImage(
                coverArt = playlist.coverArt,
                coverImageUri = playlist.coverImageUri,
                contentDescription = "${playlist.name} 歌单封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            text = playlist.name,
            color = homeAlbumTitleColor,
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
            text = "${playlist.availableSongCount} 首歌曲",
            color = homeAlbumMetaColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
