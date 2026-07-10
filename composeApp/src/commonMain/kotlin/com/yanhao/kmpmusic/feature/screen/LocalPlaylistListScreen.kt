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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.feature.app.LocalPlaylistCardDisplayModel
import com.yanhao.kmpmusic.feature.components.CoverArtImage

/**
 * 移动端本地自建歌单列表页，按 Figma 二级页工具栏和双列网格渲染。
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
            .background(color = localPlaylistListBackgroundColor),
        contentPadding = PaddingValues(
            bottom = contentPadding.calculateBottomPadding() + 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(space = 24.dp),
    ) {
        item(key = "local-playlist-list-header", contentType = "local-playlist-header") {
            LocalPlaylistListHeader(
                playlistCountSummary = buildLocalPlaylistCountSummary(playlists = playlists),
                onBack = onBack,
                onManage = onManage,
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
 * 顶部区域严格对应 Figma 的 Top App Bar 与 Subheading 两段结构。
 */
@Composable
private fun LocalPlaylistListHeader(
    playlistCountSummary: String,
    onBack: () -> Unit,
    onManage: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LocalPlaylistTopAppBar(onBack = onBack)
        Spacer(modifier = Modifier.height(height = 16.dp))
        LocalPlaylistSubheading(
            playlistCountSummary = playlistCountSummary,
            onManage = onManage,
        )
    }
}

// 标准二级页 toolbar：40dp 返回按钮、16dp 图标、24sp 标题。
@Composable
private fun LocalPlaylistTopAppBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(size = 40.dp),
            shape = CircleShape,
            color = localPlaylistToolbarButtonColor,
            onClick = onBack,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = localPlaylistBackIcon,
                    contentDescription = "返回",
                    tint = localPlaylistAccentColor,
                    modifier = Modifier.size(size = 16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(width = 16.dp))
        Text(
            text = "我的歌单",
            color = localPlaylistAccentColor,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 副标题行使用 Figma 原始图标比例，避免 material icon 与设计稿形状偏差。
@Composable
private fun LocalPlaylistSubheading(
    playlistCountSummary: String,
    onManage: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 24.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = playlistCountSummary,
            color = localPlaylistSubheadingColor.copy(alpha = 0.70f),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(
            modifier = Modifier.size(size = 40.dp),
            color = Color.Transparent,
            onClick = onManage,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = localPlaylistManageIcon,
                    contentDescription = "管理歌单",
                    tint = localPlaylistAccentColor,
                    modifier = Modifier
                        .width(width = 16.667.dp)
                        .height(height = 12.563.dp),
                )
            }
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
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
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

private val localPlaylistListBackgroundColor: Color = Color(0xFFF8FAFB)
private val localPlaylistToolbarButtonColor: Color = Color(0xFFECEEEF)
private val localPlaylistAccentColor: Color = Color(0xFF006A62)
private val localPlaylistSubheadingColor: Color = Color(0xFF3D4947)

private val localPlaylistBackIcon: ImageVector = ImageVector.Builder(
    name = "LocalPlaylistBackIcon",
    defaultWidth = 16.dp,
    defaultHeight = 16.dp,
    viewportWidth = 16f,
    viewportHeight = 16f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(x = 3.825f, y = 9f)
        lineTo(x = 9.425f, y = 14.6f)
        lineTo(x = 8f, y = 16f)
        lineTo(x = 0f, y = 8f)
        lineTo(x = 8f, y = 0f)
        lineTo(x = 9.425f, y = 1.4f)
        lineTo(x = 3.825f, y = 7f)
        lineTo(x = 16f, y = 7f)
        verticalLineTo(y = 9f)
        lineTo(x = 3.825f, y = 9f)
        close()
    }
}.build()

private val localPlaylistManageIcon: ImageVector = ImageVector.Builder(
    name = "LocalPlaylistManageIcon",
    defaultWidth = 16.667.dp,
    defaultHeight = 12.563.dp,
    viewportWidth = 16.6667f,
    viewportHeight = 12.5625f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(x = 2.95833f, y = 12.5625f)
        lineTo(x = 0f, y = 9.60417f)
        lineTo(x = 1.16667f, y = 8.4375f)
        lineTo(x = 2.9375f, y = 10.2083f)
        lineTo(x = 6.47917f, y = 6.66667f)
        lineTo(x = 7.64583f, y = 7.85417f)
        lineTo(x = 2.95833f, y = 12.5625f)
        close()
        moveTo(x = 2.95833f, y = 5.89583f)
        lineTo(x = 0f, y = 2.9375f)
        lineTo(x = 1.16667f, y = 1.77083f)
        lineTo(x = 2.9375f, y = 3.54167f)
        lineTo(x = 6.47917f, y = 0f)
        lineTo(x = 7.64583f, y = 1.1875f)
        lineTo(x = 2.95833f, y = 5.89583f)
        close()
        moveTo(x = 9.16667f, y = 10.8958f)
        verticalLineTo(y = 9.22917f)
        horizontalLineTo(x = 16.6667f)
        verticalLineTo(y = 10.8958f)
        horizontalLineTo(x = 9.16667f)
        close()
        moveTo(x = 9.16667f, y = 4.22917f)
        verticalLineTo(y = 2.5625f)
        horizontalLineTo(x = 16.6667f)
        verticalLineTo(y = 4.22917f)
        horizontalLineTo(x = 9.16667f)
        close()
    }
}.build()
