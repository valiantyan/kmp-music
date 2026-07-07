package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.ArtistRow
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.components.PlayingGlyph
import com.yanhao.kmpmusic.feature.components.SectionTitle
import kmpmusic.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * 我的页收藏摘要最多展示 3 张，完整内容通过“查看”进入，避免窄屏被数据数量挤坏。
 */
private const val FAVORITE_ALBUM_PREVIEW_COUNT = 3

/**
 * “我的”页最近播放摘要最多展示最新 3 条，点击和播放反馈复用完整最近播放语义。
 */
private const val RECENT_PLAYED_SUMMARY_PREVIEW_COUNT = 3

/**
 * “我的”页最近播放摘要入口文案，后接右箭头图标并进入完整最近播放页。
 */
private const val RECENT_PLAYED_SUMMARY_ACTION_LABEL = "查看全部"

/**
 * 我的页，提供本地资料、收藏资产和常听歌手摘要。
 */
@Composable
fun MeScreen(
    albums: List<Album>,
    artists: List<Artist>,
    recentSongs: List<Song>,
    currentSongId: String?,
    libraryStats: LibraryStats,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
    onScanMusic: () -> Unit,
    onRecentPlayedViewAll: () -> Unit,
    onRecentSongPlay: (Song) -> Unit,
    onRecentSongMore: (Song) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ProfileSummary()
        MetricRow(
            libraryStats = libraryStats,
        )
        QuickActionsSection(onScanMusic = onScanMusic)
        RecentPlayedSummarySection(
            recentSongs = recentSongs,
            currentSongId = currentSongId,
            onViewAll = onRecentPlayedViewAll,
            onSongPlay = onRecentSongPlay,
            onSongMore = onRecentSongMore,
        )
        Surface(shape = RoundedCornerShape(20.dp), color = MusicColors.Paper, tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionTitle(title = "我的收藏", actionLabel = "查看", onAction = { albums.firstOrNull()?.let(onAlbumOpen) })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    albums.take(FAVORITE_ALBUM_PREVIEW_COUNT).forEach { album ->
                        Column(
                            modifier = Modifier.weight(weight = 1f).clickable { onAlbumOpen(album) },
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            CoverArtImage(
                                coverArt = album.coverArt,
                                coverImageUri = album.coverImageUri,
                                contentDescription = "${album.title} 封面",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(ratio = 1f)
                                    .clip(RoundedCornerShape(11.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Text(
                                text = album.title,
                                color = MusicColors.Muted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = MusicColors.Paper, tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(18.dp)) {
                SectionTitle(title = "常听歌手", actionLabel = "更多", onAction = { artists.firstOrNull()?.let(onArtistOpen) })
                artists.take(3).forEach { artist -> ArtistRow(artist = artist, onOpen = onArtistOpen) }
            }
        }
        StaticSettingsMenuSection()
    }
}

/**
 * 最近播放摘要展示模型，只接收外部已过滤的最近播放歌曲列表。
 *
 * @property title 区块标题。
 * @property actionLabel 查看完整最近播放页入口文案。
 * @property emptyMessage 空态提示。
 * @property songRows 可见最近播放歌曲行，已附带当前播放标识和更多入口状态。
 * @property isActionEnabled 查看全部入口是否可点击。
 */
internal data class RecentPlayedSummaryDisplayModel(
    val title: String,
    val actionLabel: String,
    val emptyMessage: String,
    val songRows: List<RecentPlayedSongRowDisplayModel>,
    val isActionEnabled: Boolean,
) {
    /**
     * 兼容既有测试和调用方的歌曲列表视图，真实渲染状态以 [songRows] 为准。
     */
    val songs: List<Song>
        get() = songRows.map { row: RecentPlayedSongRowDisplayModel -> row.song }
}

/**
 * 构造“我的”页最近播放摘要，调用方负责传入统一过滤后的最近播放歌曲列表。
 */
internal fun buildRecentPlayedSummaryDisplayModel(
    recentSongs: List<Song>,
    currentSongId: String? = null,
): RecentPlayedSummaryDisplayModel {
    val visibleSongs: List<Song> = recentSongs.take(n = RECENT_PLAYED_SUMMARY_PREVIEW_COUNT)
    return RecentPlayedSummaryDisplayModel(
        title = "最近播放",
        actionLabel = RECENT_PLAYED_SUMMARY_ACTION_LABEL,
        emptyMessage = "播放歌曲后，最近听过的音乐会出现在这里。",
        songRows = buildRecentPlayedSongRowDisplayModels(
            songs = visibleSongs,
            currentSongId = currentSongId,
        ),
        isActionEnabled = true,
    )
}

/**
 * 最近播放摘要只渲染可见 Top3，并把查看全部限制为普通二级页导航。
 */
@Composable
private fun RecentPlayedSummarySection(
    recentSongs: List<Song>,
    currentSongId: String?,
    onViewAll: () -> Unit,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    val displayModel: RecentPlayedSummaryDisplayModel = buildRecentPlayedSummaryDisplayModel(
        recentSongs = recentSongs,
        currentSongId = currentSongId,
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MusicColors.Paper,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RecentPlayedSummaryHeader(
                displayModel = displayModel,
                onViewAll = onViewAll,
            )
            if (displayModel.songs.isEmpty()) {
                RecentPlayedSummaryEmptyState(message = displayModel.emptyMessage)
            } else {
                RecentPlayedSummarySongList(
                    songRows = displayModel.songRows,
                    onSongPlay = onSongPlay,
                    onSongMore = onSongMore,
                )
            }
        }
    }
}

/**
 * 标题行只提供查看全部入口，避免和歌曲行更多菜单产生视觉混淆。
 */
@Composable
private fun RecentPlayedSummaryHeader(
    displayModel: RecentPlayedSummaryDisplayModel,
    onViewAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayModel.title,
            color = MusicColors.Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(
                    enabled = displayModel.isActionEnabled,
                    onClick = onViewAll,
                )
                .padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayModel.actionLabel,
                color = MusicColors.Muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MusicColors.Muted,
            )
        }
    }
}

/**
 * 最近播放摘要只让可见歌曲触发播放，队列选择交给最近播放专用 controller 入口。
 */
@Composable
private fun RecentPlayedSummarySongList(
    songRows: List<RecentPlayedSongRowDisplayModel>,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        songRows.forEach { row: RecentPlayedSongRowDisplayModel ->
            RecentPlayedSummarySongRow(
                row = row,
                onSongPlay = onSongPlay,
                onSongMore = onSongMore,
            )
        }
    }
}

/**
 * 最近播放摘要歌曲行展示封面、标题和来源信息，行尾更多入口复用全局单曲面板。
 */
@Composable
private fun RecentPlayedSummarySongRow(
    row: RecentPlayedSongRowDisplayModel,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    val song: Song = row.song
    val titleColor: Color = if (row.isCurrentSong) MusicColors.PlayingRed else MusicColors.Ink
    val metaColor: Color = if (row.isCurrentSong) MusicColors.PlayingRed else MusicColors.Muted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable { onSongPlay(song) },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArtImage(
            coverArt = song.coverArt,
            coverImageUri = song.coverImageUri,
            contentDescription = "${song.title} 封面",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = song.title,
                color = titleColor,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.playingIndicatorLabel?.let { label: String ->
                    PlayingGlyph(color = MusicColors.PlayingRed)
                    Text(
                        text = label,
                        color = MusicColors.PlayingRed,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                }
                Text(
                    text = "${song.artist} · ${song.album}",
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    color = metaColor,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        RecentPlayedSummarySongActions(
            row = row,
            metaColor = metaColor,
            onSongMore = onSongMore,
        )
    }
}

/**
 * 行尾只放歌曲时长和三点更多按钮，“查看全部”入口不复用这个操作区。
 */
@Composable
private fun RecentPlayedSummarySongActions(
    row: RecentPlayedSongRowDisplayModel,
    metaColor: Color,
    onSongMore: (Song) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.song.duration,
            color = metaColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
        )
        if (row.hasMoreAction) {
            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { onSongMore(row.song) },
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "${row.song.title} 更多操作",
                    modifier = Modifier.size(20.dp),
                    tint = MusicColors.Muted,
                )
            }
        }
    }
}

/**
 * 空态使用稳定最小高度，避免没有歌曲时摘要区塌陷成留白或影响全局播放器避让。
 */
@Composable
private fun RecentPlayedSummaryEmptyState(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp),
        shape = RoundedCornerShape(16.dp),
        color = MusicColors.AccentSoft,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = message,
                color = MusicColors.Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * 静态设置菜单只补齐个人中心视觉，不接入任何旧设置、来源管理或关于路由。
 */
@Composable
private fun StaticSettingsMenuSection() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MusicColors.Paper,
        tonalElevation = 1.dp,
    ) {
        Column {
            listOf(
                "存储管理",
                "主题与外观",
                "关于",
            ).forEachIndexed { index: Int, title: String ->
                StaticSettingsMenuRow(title = title)
                if (index < 2) {
                    StaticSettingsMenuDivider()
                }
            }
        }
    }
}

/**
 * 单行仅展示右箭头提示，不声明点击回调，避免暴露未完成的设置功能。
 */
@Composable
private fun StaticSettingsMenuRow(
    title: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(weight = 1f),
            color = MusicColors.Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MusicColors.Muted,
        )
    }
}

/**
 * 分割线保持菜单组视觉层次，同时不扩大点击热区。
 */
@Composable
private fun StaticSettingsMenuDivider() {
    Surface(
        modifier = Modifier
            .padding(start = 18.dp)
            .fillMaxWidth()
            .height(1.dp),
        color = MusicColors.AccentSoft,
    ) {}
}

/**
 * 快速功能区只承载入口导航，扫描动作继续由独立扫描页处理。
 */
@Composable
private fun QuickActionsSection(
    onScanMusic: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "快速功能",
            color = MusicColors.Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MusicColors.Paper,
            tonalElevation = 1.dp,
            onClick = onScanMusic,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MusicColors.AccentSoft,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = homeAccentColor,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(weight = 1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "扫描音乐",
                        color = MusicColors.Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "发现设备里的本地音频",
                        color = MusicColors.Muted,
                        fontSize = 13.sp,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MusicColors.Muted,
                )
            }
        }
    }
}

/**
 * 我的页基础资料区，按 Figma 展示固定头像、静态编辑徽标和用户文案。
 */
@Composable
private fun ProfileSummary() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProfileAvatar()
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "高保真听众", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "音乐是我的灵魂", color = MusicColors.Muted, fontSize = 14.sp)
        }
    }
}

// 头像编辑能力尚未实现，徽标只表达 Figma 静态视觉，不暴露交互入口。
@Composable
@OptIn(ExperimentalResourceApi::class)
private fun ProfileAvatar() {
    Box(
        modifier = Modifier
            .size(78.dp)
            .border(
                width = 2.dp,
                color = homeAccentColor,
                shape = CircleShape,
            )
            .padding(3.dp),
    ) {
        AsyncImage(
            model = Res.getUri("drawable/me_profile_avatar.jpg"),
            contentDescription = "个人头像",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp),
            shape = CircleShape,
            color = homeAccentColor,
            shadowElevation = 2.dp,
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                modifier = Modifier.padding(5.dp),
                tint = Color.White,
            )
        }
    }
}

/**
 * 我的页统计指标。
 */
@Composable
private fun MetricRow(
    libraryStats: LibraryStats,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            "歌曲" to libraryStats.songCount.toString(),
            "歌单" to "12",
            "听歌时长" to "365",
        ).forEach { item ->
            Surface(
                modifier = Modifier.weight(weight = 1f),
                shape = RoundedCornerShape(18.dp),
                color = MusicColors.AccentSoft,
            ) {
                Column(modifier = Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = item.second, modifier = Modifier.padding(horizontal = 8.dp), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = item.first, modifier = Modifier.padding(horizontal = 8.dp), color = MusicColors.Muted, fontSize = 13.sp)
                }
            }
        }
    }
}
