package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.components.CoverArtImage

enum class DesktopRailDestination {
    Home,
    Favorites,
    Me,
    Settings,
}

@Composable
fun DesktopTitleBar(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.TitleBarHeight)
            .background(Color(0xB8F7F9FB))
            .border(width = 1.dp, color = Color(0xB8C7CFD6)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .width(DesktopMusicDimens.RailWidth)
                .padding(start = 18.dp),
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "KMP Music",
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.AppTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Surface(
            modifier = Modifier
                .width(520.dp)
                .height(30.dp)
                .padding(end = 18.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.84f),
            border = BorderStroke(width = 1.dp, color = Color(0xFFD7DDE3)),
            onClick = onSearch,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color(0xFF8A95A3),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "搜索歌曲、专辑、歌手",
                    color = Color(0xFF8A95A3),
                    fontSize = DesktopMusicType.Body,
                )
            }
        }
    }
}

@Composable
fun DesktopRail(
    activeDestination: DesktopRailDestination,
    onRootTab: (RootTab) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(DesktopMusicDimens.RailWidth)
            .fillMaxHeight()
            .background(Color(0xB3F8FBFC))
            .border(width = 1.dp, color = DesktopMusicColors.Line)
            .padding(top = 20.dp, start = 12.dp, end = 12.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(DesktopMusicDimens.BrandSize)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1DC6AD),
                            Color(0xFF0CA58F),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "♪",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(modifier = Modifier.height(30.dp))
        DesktopRailItem(
            destination = DesktopRailDestination.Home,
            activeDestination = activeDestination,
            icon = Icons.Rounded.Home,
            label = "首页",
        ) {
            onRootTab(RootTab.Home)
        }
        DesktopRailItem(
            destination = DesktopRailDestination.Favorites,
            activeDestination = activeDestination,
            icon = Icons.Rounded.Favorite,
            label = "收藏",
        ) {
            onRootTab(RootTab.Favorites)
        }
        DesktopRailItem(
            destination = DesktopRailDestination.Me,
            activeDestination = activeDestination,
            icon = Icons.Rounded.Person,
            label = "我的",
        ) {
            onRootTab(RootTab.Me)
        }
        DesktopRailItem(
            destination = DesktopRailDestination.Settings,
            activeDestination = activeDestination,
            icon = Icons.Rounded.Settings,
            label = "设置",
            onClick = onSettings,
        )
    }
}

@Composable
private fun DesktopRailItem(
    destination: DesktopRailDestination,
    activeDestination: DesktopRailDestination,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val isActive: Boolean = destination == activeDestination
    Surface(
        modifier = Modifier
            .size(DesktopMusicDimens.RailItemSize)
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) DesktopMusicColors.Accent.copy(alpha = 0.10f) else Color.Transparent,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) DesktopMusicColors.Accent else DesktopMusicColors.MutedStrong,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                color = if (isActive) DesktopMusicColors.Accent else DesktopMusicColors.MutedStrong,
                fontSize = DesktopMusicType.RailLabel,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun DesktopPageHeader(
    title: String,
    eyebrow: String,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.PageTitle,
                lineHeight = DesktopMusicType.PageTitle,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = eyebrow,
                color = DesktopMusicColors.Muted,
                fontSize = DesktopMusicType.Eyebrow,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 7.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions()
        }
    }
}

@Composable
fun DesktopPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(DesktopMusicDimens.PrimaryButtonHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1AC0A8),
                            DesktopMusicColors.AccentDeep,
                        ),
                    ),
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = DesktopMusicType.Eyebrow,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * 桌面输入框统一复用浅色玻璃样式，避免搜索和登录各自拼接不一致的表单外观。
 */
@Composable
fun DesktopTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val interactionSource: MutableInteractionSource = MutableInteractionSource()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = DesktopMusicColors.Ink,
            fontSize = DesktopMusicType.Eyebrow,
            fontWeight = FontWeight.SemiBold,
        ),
        interactionSource = interactionSource,
        decorationBox = { innerTextField: @Composable () -> Unit ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.84f),
                border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = DesktopMusicColors.Muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                color = DesktopMusicColors.Muted,
                                fontSize = DesktopMusicType.Eyebrow,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        innerTextField()
                    }
                }
            }
        },
    )
}

@Composable
fun DesktopSecondaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(DesktopMusicDimens.PrimaryButtonHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.84f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DesktopMusicColors.Ink,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = text,
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.Eyebrow,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun DesktopMoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(DesktopMusicDimens.PrimaryButtonHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.84f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = "更多",
                tint = DesktopMusicColors.Ink,
            )
        }
    }
}

@Composable
fun DesktopStatCard(
    icon: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(DesktopMusicDimens.StatCardMinHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.64f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = icon,
                    color = Color(0xFF303845),
                    fontSize = 24.sp,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.StatTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    color = Color(0xFF5E6A78),
                    fontSize = DesktopMusicType.Eyebrow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun DesktopToolbar(
    playAllLabel: String,
    sortLabel: String,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopPrimaryButton(
            text = playAllLabel,
            onClick = onPlayAll,
        )
        DesktopSortButton(label = sortLabel)
    }
}

@Composable
fun DesktopSortButton(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(DesktopMusicDimens.PrimaryButtonHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.84f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.Eyebrow,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = DesktopMusicColors.Muted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * 桌面分段控件统一使用浅色胶囊样式，避免各页自行实现导致交互不一致。
 */
@Composable
fun DesktopSegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .border(width = 1.dp, color = Color(0xFFD4DDE3), shape = RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index: Int, label: String ->
            Surface(
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (index == selectedIndex) DesktopMusicColors.AccentSoft else Color.Transparent,
                onClick = { onSelect(index) },
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = if (index == selectedIndex) DesktopMusicColors.AccentDeep else Color(0xFF303A46),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * 桌面歌曲表格复用统一表头和行高，保证首页与收藏页切换时视觉稳定。
 */
@Composable
fun DesktopSongTable(
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    showFavoriteColumn: Boolean,
    trailingDateLabel: String,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: ((String) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DesktopSongTableHeader(
            showFavoriteColumn = showFavoriteColumn,
            trailingDateLabel = trailingDateLabel,
        )
        songs.forEachIndexed { index: Int, song: Song ->
            DesktopSongTableRow(
                index = index,
                song = song,
                songs = songs,
                isCurrentSong = song.id == currentSongId,
                currentPlaybackStatus = currentPlaybackStatus,
                showFavoriteColumn = showFavoriteColumn,
                trailingDateLabel = trailingDateLabel,
                onSongPlay = onSongPlay,
                onCurrentSongToggle = onCurrentSongToggle,
                onMore = onMore,
                onLike = onLike,
            )
        }
    }
}

/**
 * 表头列宽与内容列权重需要固定，避免不同数据集造成表格整体抖动。
 */
@Composable
private fun DesktopSongTableHeader(
    showFavoriteColumn: Boolean,
    trailingDateLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.TableHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFavoriteColumn) {
            Text(text = "", modifier = Modifier.width(36.dp))
        }
        Text(
            text = "#",
            modifier = Modifier.width(42.dp),
            color = Color(0xFF7D8795),
            fontSize = DesktopMusicType.TableHeader,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "标题",
            modifier = Modifier.weight(2.4f),
            color = Color(0xFF7D8795),
            fontSize = DesktopMusicType.TableHeader,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "歌手",
            modifier = Modifier.weight(1.2f),
            color = Color(0xFF7D8795),
            fontSize = DesktopMusicType.TableHeader,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "专辑",
            modifier = Modifier.weight(1.2f),
            color = Color(0xFF7D8795),
            fontSize = DesktopMusicType.TableHeader,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "时长",
            modifier = Modifier.width(72.dp),
            color = Color(0xFF7D8795),
            fontSize = DesktopMusicType.TableHeader,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = trailingDateLabel,
            modifier = Modifier.width(98.dp),
            color = Color(0xFF7D8795),
            fontSize = DesktopMusicType.TableHeader,
            fontWeight = FontWeight.SemiBold,
        )
        Text(text = "", modifier = Modifier.width(40.dp))
    }
}

/**
 * 行内只消费控制器传入的歌曲和动作，避免桌面表格偷偷持有额外播放状态。
 */
@Composable
private fun DesktopSongTableRow(
    index: Int,
    song: Song,
    songs: List<Song>,
    isCurrentSong: Boolean,
    currentPlaybackStatus: PlaybackStatus,
    showFavoriteColumn: Boolean,
    trailingDateLabel: String,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: ((String) -> Unit)?,
) {
    val isCurrentSongPlaying: Boolean =
        isCurrentSong && currentPlaybackStatus == PlaybackStatus.Playing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.TableRowHeight)
            .background(if (isCurrentSong) DesktopMusicColors.Accent.copy(alpha = 0.10f) else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFavoriteColumn) {
            Text(
                text = if (song.isLiked) "♥" else "♡",
                modifier = Modifier
                    .width(36.dp)
                    .clickable { onLike?.invoke(song.id) },
                color = if (song.isLiked) DesktopMusicColors.PlayerRed else DesktopMusicColors.Muted,
            )
        }
        Surface(
            modifier = Modifier.width(42.dp),
            color = Color.Transparent,
            onClick = {
                if (isCurrentSong) {
                    onCurrentSongToggle()
                } else {
                    onSongPlay(song, songs)
                }
            },
        ) {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isCurrentSongPlaying) "Ⅱ" else if (isCurrentSong) "▶" else (index + 1).toString(),
                    color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopMusicColors.Muted,
                    fontSize = DesktopMusicType.Body,
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(2.4f)
                .clickable { onSongPlay(song, songs) },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArtImage(
                coverArt = song.coverArt,
                coverImageUri = song.coverImageUri,
                contentDescription = "${song.title} 封面",
                modifier = Modifier
                    .size(DesktopMusicDimens.TableCoverSize)
                    .clip(RoundedCornerShape(7.dp)),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = song.title,
                color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.Body,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = song.artist,
            modifier = Modifier.weight(1.2f),
            color = DesktopMusicColors.Ink,
            fontSize = DesktopMusicType.TableTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.album,
            modifier = Modifier.weight(1.2f),
            color = DesktopMusicColors.Ink,
            fontSize = DesktopMusicType.TableTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.duration,
            modifier = Modifier.width(72.dp),
            color = DesktopMusicColors.Muted,
            fontSize = DesktopMusicType.Body,
        )
        Text(
            text = desktopSongTableTrailingValue(
                trailingDateLabel = trailingDateLabel,
                song = song,
            ),
            modifier = Modifier.width(98.dp),
            color = DesktopMusicColors.Muted,
            fontSize = DesktopMusicType.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(
            modifier = Modifier.size(30.dp),
            shape = RoundedCornerShape(9.dp),
            color = Color.Transparent,
            onClick = { onMore(song) },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "•••",
                    color = Color(0xFF475364),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun desktopSongTableTrailingValue(
    trailingDateLabel: String,
    song: Song,
): String {
    return when (trailingDateLabel) {
        "收藏时间" -> "已收藏"
        "添加时间" -> song.modifiedAt?.let(::formatDesktopModifiedDate) ?: "最近添加"
        else -> "最近"
    }
}

private fun formatDesktopModifiedDate(timestampMillis: Long): String {
    val epochDay: Long = floorDivByDay(timestampMillis)
    val civilDate: CivilDate = civilDateFromEpochDay(epochDay)
    val month: String = civilDate.month.toString().padStart(length = 2, padChar = '0')
    val day: String = civilDate.day.toString().padStart(length = 2, padChar = '0')
    return "${civilDate.year}-$month-$day"
}

private fun floorDivByDay(timestampMillis: Long): Long {
    val dayMillis: Long = 86_400_000L
    return if (timestampMillis >= 0L) {
        timestampMillis / dayMillis
    } else {
        ((timestampMillis + 1L) / dayMillis) - 1L
    }
}

private fun civilDateFromEpochDay(epochDay: Long): CivilDate {
    val shiftedDay: Long = epochDay + 719_468L
    val era: Long = if (shiftedDay >= 0L) shiftedDay else shiftedDay - 146_096L
    val eraIndex: Long = era / 146_097L
    val dayOfEra: Long = shiftedDay - eraIndex * 146_097L
    val yearOfEra: Long = (
        dayOfEra - dayOfEra / 1_460L + dayOfEra / 36_524L - dayOfEra / 146_096L
        ) / 365L
    val year: Long = yearOfEra + eraIndex * 400L
    val dayOfYear: Long = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
    val monthPrime: Long = (5L * dayOfYear + 2L) / 153L
    val day: Int = (dayOfYear - (153L * monthPrime + 2L) / 5L + 1L).toInt()
    val month: Int = (monthPrime + if (monthPrime < 10L) 3L else -9L).toInt()
    val resolvedYear: Int = (year + if (month <= 2) 1L else 0L).toInt()
    return CivilDate(
        year = resolvedYear,
        month = month,
        day = day,
    )
}

private data class CivilDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

@Composable
fun DesktopSectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = DesktopMusicColors.Ink,
            fontSize = DesktopMusicType.StatTitle,
            fontWeight = FontWeight.ExtraBold,
        )
        if (actionLabel != null && onAction != null) {
            Row(
                modifier = Modifier.clickable(onClick = onAction),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = actionLabel,
                    color = DesktopMusicColors.MutedStrong,
                    fontSize = DesktopMusicType.Eyebrow,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = DesktopMusicColors.MutedStrong,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun DesktopAlbumGrid(
    albums: List<Album>,
    onAlbumOpen: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns: Int = if (maxWidth < 720.dp) 2 else 4
        val rows: List<List<Album>> = albums.chunked(size = columns)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEach { rowAlbums: List<Album> ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowAlbums.forEach { album: Album ->
                        DesktopAlbumCard(
                            album = album,
                            onOpen = onAlbumOpen,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowAlbums.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopAlbumCard(
    album: Album,
    onOpen: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable { onOpen(album) },
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoverArtImage(
                coverArt = album.coverArt,
                coverImageUri = album.coverImageUri,
                contentDescription = "${album.title} 封面",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = album.title,
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.StatTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = album.artist,
                    color = DesktopMusicColors.MutedStrong,
                    fontSize = DesktopMusicType.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${album.songCount} 首 · ${album.year}",
                    color = DesktopMusicColors.Muted,
                    fontSize = DesktopMusicType.Body,
                )
            }
        }
    }
}

@Composable
fun DesktopProfilePanel(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverArt: CoverArt = CoverArt.AlbumTimeForest,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArtImage(
                coverArt = coverArt,
                contentDescription = "账号头像视觉",
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = title,
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.PageTitle,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = description,
                    color = DesktopMusicColors.MutedStrong,
                    fontSize = DesktopMusicType.Eyebrow,
                )
                DesktopPrimaryButton(text = buttonText, onClick = onClick)
            }
        }
    }
}

@Composable
fun DesktopContentRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    extraContent: @Composable (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DesktopMusicColors.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DesktopMusicColors.AccentDeep,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.StatTitle,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    color = DesktopMusicColors.MutedStrong,
                    fontSize = DesktopMusicType.Body,
                )
                extraContent?.invoke()
            }
            if (actionLabel != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = actionLabel,
                        color = DesktopMusicColors.MutedStrong,
                        fontSize = DesktopMusicType.Eyebrow,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = DesktopMusicColors.MutedStrong,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
    if (onClick != null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.72f),
            border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
            onClick = onClick,
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.72f),
            border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
        ) {
            content()
        }
    }
}

@Composable
fun DesktopArtistStrip(
    artists: List<Artist>,
    onArtistOpen: (Artist) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        artists.forEach { artist: Artist ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onArtistOpen(artist) },
                shape = RoundedCornerShape(14.dp),
                color = DesktopMusicColors.Soft,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CoverArtImage(
                        coverArt = artist.coverArt,
                        coverImageUri = artist.coverImageUri,
                        contentDescription = "${artist.name} 图片",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    Text(
                        text = artist.name,
                        color = DesktopMusicColors.Ink,
                        fontSize = DesktopMusicType.Body,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${artist.songCount} 首",
                        color = DesktopMusicColors.Muted,
                        fontSize = DesktopMusicType.Body,
                    )
                }
            }
        }
    }
}

val DesktopContentRowFavoritesIcon: ImageVector
    get() = Icons.Rounded.Favorite

val DesktopContentRowFolderIcon: ImageVector
    get() = Icons.Rounded.Folder

val DesktopContentRowSyncIcon: ImageVector
    get() = Icons.Rounded.Sync

val DesktopScanIcon: ImageVector
    get() = Icons.Rounded.Refresh
