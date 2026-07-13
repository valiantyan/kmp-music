package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.feature.app.LocalPlaylistCardDisplayModel
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.components.MobileSecondaryPage

/**
 * 移动端歌单管理页，按 Figma 批量删除稿承载单选和多选。
 */
@Composable
fun LocalPlaylistManagementScreen(
    playlists: List<LocalPlaylistCardDisplayModel>,
    selectedPlaylistIds: Set<String>,
    canDelete: Boolean,
    onBack: () -> Unit,
    onPlaylistToggle: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    MobileSecondaryPage(
        title = "管理歌单",
        onBack = onBack,
        backgroundColor = localPlaylistManagementBackgroundColor,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.weight(weight = 1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 66.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            ) {
                if (playlists.isEmpty()) {
                    item(key = "local-playlist-management-empty", contentType = "management-empty") {
                        LocalPlaylistManagementEmptyState()
                    }
                    return@LazyColumn
                }
                items(
                    items = playlists,
                    key = { playlist: LocalPlaylistCardDisplayModel -> playlist.id },
                    contentType = { "management-playlist-row" },
                ) { playlist: LocalPlaylistCardDisplayModel ->
                    LocalPlaylistManagementItem(
                        playlist = playlist,
                        isSelected = playlist.id in selectedPlaylistIds,
                        onToggle = onPlaylistToggle,
                    )
                }
            }
            LocalPlaylistManagementDeleteBar(
                canDelete = canDelete,
                onDelete = onDelete,
                modifier = Modifier.align(alignment = Alignment.BottomCenter),
            )
        }
    }
}

// 空态保留在列表区域，底部删除按钮继续置灰。
@Composable
private fun LocalPlaylistManagementEmptyState() {
    Text(
        text = "暂无歌单",
        color = homeMutedColor,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
    )
}

// 整行可点，符合管理页批量选择目标，避免只点小圆圈的窄热区。
@Composable
private fun LocalPlaylistManagementItem(
    playlist: LocalPlaylistCardDisplayModel,
    isSelected: Boolean,
    onToggle: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(height = 98.dp)
            .clickable { onToggle(playlist.id) },
        shape = RoundedCornerShape(size = 24.dp),
        color = Color.White,
        border = BorderStroke(width = 1.dp, color = localPlaylistManagementLineColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LocalPlaylistManagementSelectionMark(isSelected = isSelected)
            CoverArtImage(
                coverArt = playlist.coverArt,
                coverImageUri = playlist.coverImageUri,
                contentDescription = "${playlist.name} 歌单封面",
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(size = 64.dp)
                    .clip(shape = RoundedCornerShape(size = 12.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(weight = 1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = playlist.name,
                    color = localPlaylistManagementTitleColor,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${playlist.availableSongCount}首",
                    color = localPlaylistManagementMetaColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// 选中态只改变圆形控件，保持卡片白底以贴近 Figma 未选中稿。
@Composable
private fun LocalPlaylistManagementSelectionMark(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(size = 24.dp)
            .background(
                color = if (isSelected) localPlaylistManagementAccentColor else Color.Transparent,
                shape = CircleShape,
            )
            .border(
                width = 2.dp,
                color = if (isSelected) localPlaylistManagementAccentColor else localPlaylistManagementSelectionColor,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "已选中",
                tint = Color.White,
                modifier = Modifier.size(size = 16.dp),
            )
        }
    }
}

// 底部删除栏固定为全宽白底条，避免管理页底部出现过厚空白区域。
@Composable
private fun LocalPlaylistManagementDeleteBar(
    canDelete: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height = 66.dp)
            .clickable(enabled = canDelete, onClick = onDelete),
        color = Color.White,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(alpha = if (canDelete) 1f else 0.5f),
            horizontalArrangement = Arrangement.spacedBy(
                space = 6.dp,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "删除",
                tint = MusicColors.Danger,
                modifier = Modifier.size(width = 19.dp, height = 21.dp),
            )
            Text(
                text = "删除",
                color = MusicColors.Danger,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private val localPlaylistManagementBackgroundColor: Color = MusicColors.PageBackground
private val localPlaylistManagementAccentColor: Color = Color(0xFF006A62)
private val localPlaylistManagementLineColor: Color = Color(0xFFECEEEF)
private val localPlaylistManagementSelectionColor: Color = Color(0xFF6D7A77)
private val localPlaylistManagementTitleColor: Color = Color(0xFF191C1D)
private val localPlaylistManagementMetaColor: Color = Color(0xFF3D4947)
