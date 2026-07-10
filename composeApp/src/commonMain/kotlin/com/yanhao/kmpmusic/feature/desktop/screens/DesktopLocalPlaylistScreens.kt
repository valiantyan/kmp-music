package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
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
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.LocalPlaylistCardDisplayModel
import com.yanhao.kmpmusic.feature.app.LocalPlaylistDetailDisplayModel
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPageHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPrimaryButton
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionEmptyMessage
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSongTable

/**
 * 桌面本地自建歌单列表页，复用 workspace 二级页面语义展示已有歌单。
 */
@Composable
internal fun DesktopLocalPlaylistListScreen(
    playlists: List<LocalPlaylistCardDisplayModel>,
    onBack: () -> Unit,
    onPlaylistOpen: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "我的歌单",
            eyebrow = "按最近更新时间排序",
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
        }
        if (playlists.isEmpty()) {
            DesktopSectionEmptyMessage(message = "暂无歌单")
            return@Column
        }
        DesktopLocalPlaylistGrid(
            playlists = playlists,
            onPlaylistOpen = onPlaylistOpen,
        )
    }
}

/**
 * 桌面本地自建歌单详情页，使用桌面二级内容区和表格承载当前可播放歌曲。
 */
@Composable
internal fun DesktopLocalPlaylistDetailScreen(
    detail: LocalPlaylistDetailDisplayModel?,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = detail?.name ?: "歌单不可用",
            eyebrow = detail?.let { model: LocalPlaylistDetailDisplayModel ->
                "${model.availableSongCount} 首可播放歌曲"
            } ?: "没有找到歌单信息",
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
            DesktopPrimaryButton(
                text = "▶ 播放全部",
                onClick = onPlayAll,
                enabled = detail?.canPlayAll == true,
            )
        }
        if (detail == null) {
            DesktopSectionEmptyMessage(message = "暂无可播放歌曲")
            return@Column
        }
        DesktopLocalPlaylistDetailSummary(detail = detail)
        Spacer(modifier = Modifier.height(18.dp))
        if (detail.songs.isEmpty()) {
            DesktopSectionEmptyMessage(message = detail.emptyText)
            return@Column
        }
        DesktopSongTable(
            songs = detail.songs,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongPlay = onSongPlay,
            onCurrentSongToggle = onCurrentSongToggle,
            onMore = onMore,
        )
    }
}

/**
 * 宽屏列表按可用宽度决定列数，避免小窗口卡片被挤压。
 */
@Composable
private fun DesktopLocalPlaylistGrid(
    playlists: List<LocalPlaylistCardDisplayModel>,
    onPlaylistOpen: (String) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns: Int = if (maxWidth < 720.dp) 2 else 4
        val rows: List<List<LocalPlaylistCardDisplayModel>> = playlists.chunked(size = columns)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEach { rowPlaylists: List<LocalPlaylistCardDisplayModel> ->
                DesktopLocalPlaylistRow(
                    rowPlaylists = rowPlaylists,
                    columns = columns,
                    onPlaylistOpen = onPlaylistOpen,
                )
            }
        }
    }
}

/**
 * 每行补足空列，保证末行单个歌单不会横向拉伸。
 */
@Composable
private fun DesktopLocalPlaylistRow(
    rowPlaylists: List<LocalPlaylistCardDisplayModel>,
    columns: Int,
    onPlaylistOpen: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        rowPlaylists.forEach { playlist: LocalPlaylistCardDisplayModel ->
            DesktopLocalPlaylistCard(
                playlist = playlist,
                onPlaylistOpen = onPlaylistOpen,
                modifier = Modifier.weight(1f),
            )
        }
        repeat(columns - rowPlaylists.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * 卡片只负责打开详情，不提供创建空歌单入口。
 */
@Composable
private fun DesktopLocalPlaylistCard(
    playlist: LocalPlaylistCardDisplayModel,
    onPlaylistOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable { onPlaylistOpen(playlist.id) },
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoverArtImage(
                coverArt = playlist.coverArt,
                coverImageUri = playlist.coverImageUri,
                contentDescription = "${playlist.name} 歌单封面",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = playlist.name,
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.StatTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${playlist.availableSongCount} 首歌曲",
                    color = DesktopMusicColors.MutedStrong,
                    fontSize = DesktopMusicType.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 详情摘要在表格前保留封面和数量，帮助桌面用户确认当前浏览对象。
 */
@Composable
private fun DesktopLocalPlaylistDetailSummary(
    detail: LocalPlaylistDetailDisplayModel,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArtImage(
                coverArt = detail.coverArt,
                coverImageUri = detail.coverImageUri,
                contentDescription = "${detail.name} 歌单封面",
                modifier = Modifier
                    .height(96.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = detail.name,
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.SidebarTitle,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${detail.availableSongCount} 首当前可播放歌曲",
                    color = DesktopMusicColors.MutedStrong,
                    fontSize = DesktopMusicType.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "按加入顺序播放",
                    color = DesktopMusicColors.Muted,
                    fontSize = DesktopMusicType.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
