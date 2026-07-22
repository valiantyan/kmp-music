package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.LocalPlaylistCardDisplayModel
import com.yanhao.kmpmusic.feature.app.LocalPlaylistDetailDisplayModel
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType
import com.yanhao.kmpmusic.feature.desktop.DesktopPlaylistTokens
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPageHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPrimaryButton
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionEmptyMessage
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSongTable
import com.yanhao.kmpmusic.feature.screen.buildLocalPlaylistCountSummary

/**
 * 桌面本地自建歌单列表页，复用 workspace 二级页面语义展示已有歌单。
 */
@Composable
internal fun DesktopLocalPlaylistListScreen(
    playlists: List<LocalPlaylistCardDisplayModel>,
    onManage: () -> Unit,
    onCreate: () -> Unit,
    onPlaylistOpen: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(DesktopPlaylistTokens.Background),
    ) {
        DesktopPlaylistTopBar(
            title = "我的歌单",
        )
        Column(
            modifier =
                Modifier
                    .weight(weight = 1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(all = DesktopPlaylistTokens.ContentPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildLocalPlaylistCountSummary(playlists = playlists),
                    color = DesktopPlaylistTokens.SupportingText,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopPlaylistPrimaryAction(
                        text = "管理",
                        onClick = onManage,
                        showAddIcon = false,
                    )
                    DesktopPlaylistPrimaryAction(
                        text = "新建歌单",
                        onClick = onCreate,
                        showAddIcon = true,
                    )
                }
            }
            Spacer(modifier = Modifier.height(DesktopPlaylistTokens.GridGap))
            DesktopLocalPlaylistGrid(
                playlists = playlists,
                onCreate = onCreate,
                onPlaylistOpen = onPlaylistOpen,
            )
        }
    }
}

/**
 * 桌面本地自建歌单管理页，使用列表选择和悬浮删除操作承接 Figma 批量删除稿。
 */
@Composable
internal fun DesktopLocalPlaylistManagementScreen(
    playlists: List<LocalPlaylistCardDisplayModel>,
    selectedPlaylistIds: Set<String>,
    canDelete: Boolean,
    onBack: () -> Unit,
    onPlaylistToggle: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(DesktopPlaylistTokens.Background),
    ) {
        DesktopPlaylistTopBar(
            title = "管理歌单",
            onBack = onBack,
        )
        if (playlists.isEmpty()) {
            Text(
                text = "暂无歌单",
                color = DesktopPlaylistTokens.MutedText,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier =
                    Modifier.padding(
                        start = DesktopPlaylistTokens.ManagementHorizontalPadding,
                        top = DesktopPlaylistTokens.ContentPadding,
                    ),
            )
            return@Column
        }
        Box(
            modifier =
                Modifier
                    .weight(weight = 1f)
                    .fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = DesktopPlaylistTokens.ManagementHorizontalPadding,
                            top = 16.dp,
                            end = DesktopPlaylistTokens.ManagementHorizontalPadding,
                            bottom = 96.dp,
                        ),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier =
                            Modifier
                                .widthIn(max = DesktopPlaylistTokens.ManagementContentMaxWidth)
                                .fillMaxWidth()
                                .align(alignment = Alignment.TopCenter),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        playlists.forEach { playlist: LocalPlaylistCardDisplayModel ->
                            DesktopLocalPlaylistManagementRow(
                                playlist = playlist,
                                isSelected = playlist.id in selectedPlaylistIds,
                                onPlaylistToggle = onPlaylistToggle,
                            )
                        }
                    }
                }
            }
            if (canDelete) {
                DesktopLocalPlaylistDeleteAction(
                    onClick = onDelete,
                    modifier =
                        Modifier
                            .align(alignment = Alignment.BottomCenter)
                            .padding(bottom = DesktopPlaylistTokens.ManagementDeleteBottomPadding),
                )
            }
        }
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
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = detail?.name ?: "歌单不可用",
            eyebrow =
                detail?.let { model: LocalPlaylistDetailDisplayModel ->
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

/** Figma 内容区顶栏只承载页面标题，管理页按需增加返回操作。 */
@Composable
private fun DesktopPlaylistTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(DesktopPlaylistTokens.HeaderHeight)
                .padding(horizontal = DesktopPlaylistTokens.ContentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        onBack?.let { action: () -> Unit ->
            Box(
                modifier =
                    Modifier
                        .size(size = 32.dp)
                        .clickable(onClick = action),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = DesktopPlaylistTokens.Title,
                    modifier = Modifier.size(size = 20.dp),
                )
            }
            Spacer(modifier = Modifier.size(size = 8.dp))
        }
        Text(
            text = title,
            color = DesktopPlaylistTokens.Title,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 歌单页两个操作共用 Figma 的深青色胶囊样式，仅新建操作展示创建语义加号。 */
@Composable
private fun DesktopPlaylistPrimaryAction(
    text: String,
    onClick: () -> Unit,
    showAddIcon: Boolean,
) {
    Surface(
        modifier = Modifier.height(height = 40.dp),
        shape = CircleShape,
        color = DesktopPlaylistTokens.Accent,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showAddIcon) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size = 16.dp),
                )
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** 网格始终附带一个创建卡片，并以固定卡片宽度防止宽屏时内容被横向拉伸。 */
@Composable
private fun DesktopLocalPlaylistGrid(
    playlists: List<LocalPlaylistCardDisplayModel>,
    onCreate: () -> Unit,
    onPlaylistOpen: (String) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns: Int =
            ((maxWidth + DesktopPlaylistTokens.GridGap) / (DesktopPlaylistTokens.CardCoverSize + DesktopPlaylistTokens.GridGap))
                .toInt()
                .coerceIn(minimumValue = 2, maximumValue = 4)
        val gridItems: List<LocalPlaylistCardDisplayModel?> = playlists + listOf(null)
        Column(verticalArrangement = Arrangement.spacedBy(DesktopPlaylistTokens.GridGap)) {
            gridItems.chunked(size = columns).forEach { row: List<LocalPlaylistCardDisplayModel?> ->
                Row(horizontalArrangement = Arrangement.spacedBy(DesktopPlaylistTokens.GridGap)) {
                    row.forEach { playlist: LocalPlaylistCardDisplayModel? ->
                        if (playlist == null) {
                            DesktopCreatePlaylistCard(onClick = onCreate)
                        } else {
                            DesktopLocalPlaylistCard(
                                playlist = playlist,
                                onPlaylistOpen = onPlaylistOpen,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 卡片点击进入详情，封面和歌曲数量始终来自当前仓库投影。 */
@Composable
private fun DesktopLocalPlaylistCard(
    playlist: LocalPlaylistCardDisplayModel,
    onPlaylistOpen: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .width(width = DesktopPlaylistTokens.CardCoverSize)
                .clickable { onPlaylistOpen(playlist.id) },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CoverArtImage(
            coverArt = playlist.coverArt,
            coverImageUri = playlist.coverImageUri,
            contentDescription = "${playlist.name} 歌单封面",
            modifier =
                Modifier
                    .size(size = DesktopPlaylistTokens.CardCoverSize)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(DesktopPlaylistTokens.CardCornerRadius),
                    ).clip(shape = RoundedCornerShape(DesktopPlaylistTokens.CardCornerRadius)),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = playlist.name,
            color = DesktopPlaylistTokens.Title,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${playlist.availableSongCount} 首歌曲",
            color = DesktopPlaylistTokens.MutedText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 虚线创建卡片与顶部按钮走同一个创建动作，使空态无需额外分支。 */
@Composable
private fun DesktopCreatePlaylistCard(onClick: () -> Unit) {
    Column(modifier = Modifier.width(width = DesktopPlaylistTokens.CardCoverSize)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height = DesktopPlaylistTokens.CardCoverSize)
                    .clip(shape = RoundedCornerShape(DesktopPlaylistTokens.CardCornerRadius))
                    .playlistCreateBorder()
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(size = 34.dp)
                            .border(
                                width = 2.dp,
                                color = DesktopPlaylistTokens.SelectionOutline,
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "创建新列表",
                        tint = DesktopPlaylistTokens.SelectionOutline,
                        modifier = Modifier.size(size = 22.dp),
                    )
                }
                Text(
                    text = "创建新列表",
                    color = DesktopPlaylistTokens.MutedText.copy(alpha = 0.84f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** 管理列表整行切换选择，扩大命中区但不改变多选状态机。 */
@Composable
private fun DesktopLocalPlaylistManagementRow(
    playlist: LocalPlaylistCardDisplayModel,
    isSelected: Boolean,
    onPlaylistToggle: (String) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height = DesktopPlaylistTokens.ManagementRowHeight)
                .clickable { onPlaylistToggle(playlist.id) },
        shape = RoundedCornerShape(DesktopPlaylistTokens.ManagementRowCornerRadius),
        color = Color.White.copy(alpha = 0.4f),
        border = BorderStroke(width = 1.dp, color = DesktopPlaylistTokens.ManagementBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesktopLocalPlaylistSelectionMark(isSelected = isSelected)
            CoverArtImage(
                coverArt = playlist.coverArt,
                coverImageUri = playlist.coverImageUri,
                contentDescription = "${playlist.name} 歌单封面",
                modifier = Modifier.size(size = 64.dp).clip(shape = RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = playlist.name,
                    color = DesktopPlaylistTokens.Title,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${playlist.availableSongCount} 首歌曲",
                    color = DesktopPlaylistTokens.SupportingText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 选择状态仅改变圆形控件，列表行保持同一背景以降低批量扫描噪音。 */
@Composable
private fun DesktopLocalPlaylistSelectionMark(isSelected: Boolean) {
    Box(
        modifier =
            Modifier
                .size(size = 24.dp)
                .background(
                    color = if (isSelected) DesktopPlaylistTokens.Accent else Color.Transparent,
                    shape = CircleShape,
                ).border(
                    width = 2.dp,
                    color = if (isSelected) DesktopPlaylistTokens.Accent else DesktopPlaylistTokens.SelectionOutline,
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

/** 有选择时才显示悬浮删除操作，避免空选择的危险按钮干扰列表扫描。 */
@Composable
private fun DesktopLocalPlaylistDeleteAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(height = 48.dp),
        shape = CircleShape,
        color = DesktopPlaylistTokens.DangerContainer,
        border = BorderStroke(width = 1.dp, color = DesktopPlaylistTokens.Danger.copy(alpha = 0.2f)),
        shadowElevation = 6.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null,
                tint = DesktopPlaylistTokens.Danger,
                modifier = Modifier.size(size = 16.dp),
            )
            Text(
                text = "删除歌单",
                color = DesktopPlaylistTokens.Danger,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** 虚线边框保持为绘制修饰符，避免为单个创建卡片引入额外组件层级。 */
private fun Modifier.playlistCreateBorder(): Modifier =
    drawBehind {
        val strokeWidth: Float = 2.dp.toPx()
        val cornerRadius: Float = DesktopPlaylistTokens.CardCornerRadius.toPx()
        drawRoundRect(
            color = DesktopPlaylistTokens.CreateBorder,
            cornerRadius = CornerRadius(x = cornerRadius, y = cornerRadius),
            style =
                Stroke(
                    width = strokeWidth,
                    pathEffect =
                        PathEffect.dashPathEffect(
                            intervals = floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                            phase = 0f,
                        ),
                ),
        )
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
                modifier =
                    Modifier
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
                    text = "按最新添加播放",
                    color = DesktopMusicColors.Muted,
                    fontSize = DesktopMusicType.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
