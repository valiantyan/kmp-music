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
import androidx.compose.material3.Icon
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
import com.yanhao.kmpmusic.feature.components.SectionTitle
import kmpmusic.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * 我的页收藏摘要最多展示 3 张，完整内容通过“查看”进入，避免窄屏被数据数量挤坏。
 */
private const val FAVORITE_ALBUM_PREVIEW_COUNT = 3

/**
 * “我的”页最近播放摘要最多展示最新 3 条，完整列表和交互由后续切片接入。
 */
private const val RECENT_PLAYED_SUMMARY_PREVIEW_COUNT = 3

/**
 * “我的”页最近播放摘要入口文案当前只保留静态视觉位置。
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
    libraryStats: LibraryStats,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
    onScanMusic: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ProfileSummary()
        MetricRow(
            libraryStats = libraryStats,
        )
        QuickActionsSection(onScanMusic = onScanMusic)
        RecentPlayedSummarySection(recentSongs = recentSongs)
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
 */
internal data class RecentPlayedSummaryDisplayModel(
    val title: String,
    val actionLabel: String,
    val emptyMessage: String,
    val songs: List<Song>,
    val isActionEnabled: Boolean,
)

/**
 * 构造“我的”页最近播放摘要，调用方负责传入统一过滤后的最近播放歌曲列表。
 */
internal fun buildRecentPlayedSummaryDisplayModel(recentSongs: List<Song>): RecentPlayedSummaryDisplayModel {
    return RecentPlayedSummaryDisplayModel(
        title = "最近播放",
        actionLabel = RECENT_PLAYED_SUMMARY_ACTION_LABEL,
        emptyMessage = "播放歌曲后，最近听过的音乐会出现在这里。",
        songs = recentSongs.take(n = RECENT_PLAYED_SUMMARY_PREVIEW_COUNT),
        isActionEnabled = false,
    )
}

/**
 * 最近播放摘要只渲染可见 Top3，不绑定跳转、队列或更多菜单。
 */
@Composable
private fun RecentPlayedSummarySection(recentSongs: List<Song>) {
    val displayModel: RecentPlayedSummaryDisplayModel = buildRecentPlayedSummaryDisplayModel(
        recentSongs = recentSongs,
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
            RecentPlayedSummaryHeader(displayModel = displayModel)
            if (displayModel.songs.isEmpty()) {
                RecentPlayedSummaryEmptyState(message = displayModel.emptyMessage)
            } else {
                RecentPlayedSummarySongList(songs = displayModel.songs)
            }
        }
    }
}

/**
 * 标题行保留后续跳转入口的视觉位置，但当前切片不扩大可点击行为。
 */
@Composable
private fun RecentPlayedSummaryHeader(displayModel: RecentPlayedSummaryDisplayModel) {
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
        Text(
            text = "${displayModel.actionLabel}  ›",
            color = MusicColors.Muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * 最近播放摘要行保持静态展示，避免在当前切片提前接入播放或更多菜单。
 */
@Composable
private fun RecentPlayedSummarySongList(songs: List<Song>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        songs.forEach { song: Song ->
            RecentPlayedSummarySongRow(song = song)
        }
    }
}

/**
 * 最近播放摘要歌曲行展示封面、标题和来源信息，不从全库或 demo 数据补内容。
 */
@Composable
private fun RecentPlayedSummarySongRow(song: Song) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
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
                color = MusicColors.Ink,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${song.artist} · ${song.album}",
                color = MusicColors.Muted,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = song.duration,
            color = MusicColors.Muted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
        )
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
