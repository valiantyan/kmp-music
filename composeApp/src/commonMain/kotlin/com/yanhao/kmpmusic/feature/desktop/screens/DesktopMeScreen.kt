package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.Song
import kmpmusic.composeapp.generated.resources.Res
import kmpmusic.composeapp.generated.resources.desktop_me_scan_icon
import org.jetbrains.compose.resources.painterResource

/**
 * 桌面“我的”页暂不实现真实听歌时长统计，数值只作为静态展示。
 */
private const val STATIC_LISTENING_HOURS = 365

/**
 * 我的页汇总个人资料、静态统计、扫描入口、最近播放摘要和静态设置菜单。
 */
@Composable
fun DesktopMeRootScreen(
    recentSongs: List<Song>,
    libraryStats: LibraryStats,
    localPlaylistCount: Int,
    currentSongId: String?,
    isPlaying: Boolean,
    onScanMusic: () -> Unit,
    onHomeSongsOpen: () -> Unit,
    onLocalPlaylistsOpen: () -> Unit,
    onRecentPlayedViewAll: () -> Unit,
    onRecentSongPlay: (Song) -> Unit,
    onRecentSongMore: (Song) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(DesktopMeFigmaTokens.Background)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            DesktopMeProfileHeader()
            DesktopMeStatsRow(
                libraryStats = libraryStats,
                localPlaylistCount = localPlaylistCount,
                onScanMusic = onScanMusic,
                onHomeSongsOpen = onHomeSongsOpen,
                onLocalPlaylistsOpen = onLocalPlaylistsOpen,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                DesktopMeStaticSettingsMenu(modifier = Modifier.weight(29f))
                DesktopMeRecentPlayedSummary(
                    recentSongs = recentSongs,
                    currentSongId = currentSongId,
                    isPlaying = isPlaying,
                    onViewAll = onRecentPlayedViewAll,
                    onSongPlay = onRecentSongPlay,
                    onSongMore = onRecentSongMore,
                    modifier = Modifier.weight(61f),
                )
            }
        }
    }
}

/**
 * 桌面“我的”页统计展示模型，隔离真实统计和可点击入口的边界。
 *
 * @property icon 统计卡片图标。
 * @property title 统计卡片标题。
 * @property value 统计卡片展示值。
 * @property action 统计卡片的可选动作；为空时只做展示。
 */
internal data class DesktopMeStatDisplayModel(
    val icon: ImageVector,
    val heading: String,
    val value: String,
    val title: String,
    val unit: String? = null,
    val action: DesktopMeStatAction? = null,
)

/**
 * 桌面“我的”页统计卡动作，只复用既有一级页或歌单入口。
 */
internal enum class DesktopMeStatAction {
    OpenHomeSongs,
    OpenLocalPlaylists,
}

/**
 * 桌面“我的”页快速功能动作，避免入口文案和点击行为在后续扩展时混淆。
 */
internal enum class DesktopMeQuickAction {
    ScanMusic,
}

/**
 * 桌面“我的”页快速功能展示模型，只描述入口语义，不新增扫描流程。
 *
 * @property action 点击后要复用的既有桌面动作。
 * @property title 入口标题。
 * @property subtitle 入口说明。
 */
internal data class DesktopMeQuickActionDisplayModel(
    val action: DesktopMeQuickAction,
    val title: String,
    val subtitle: String,
)

/**
 * 构造桌面“我的”页三项统计；音乐和歌单来自真实状态，听歌时长保持静态展示。
 */
internal fun buildDesktopMeStatDisplayModels(
    libraryStats: LibraryStats,
    localPlaylistCount: Int,
): List<DesktopMeStatDisplayModel> =
    listOf(
        DesktopMeStatDisplayModel(
            icon = Icons.Rounded.LibraryMusic,
            heading = "SONGS",
            value = libraryStats.songCount.toString(),
            title = "音乐",
            action = DesktopMeStatAction.OpenHomeSongs,
        ),
        DesktopMeStatDisplayModel(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            heading = "PLAYLISTS",
            value = localPlaylistCount.toString(),
            title = "创建歌单",
            action = DesktopMeStatAction.OpenLocalPlaylists,
        ),
        DesktopMeStatDisplayModel(
            icon = Icons.Rounded.Timer,
            heading = "DURATION",
            value = STATIC_LISTENING_HOURS.toString(),
            title = "累计收听",
            unit = "h",
        ),
    )

/**
 * 构造桌面“我的”页快速功能入口；扫描音乐必须复用桌面文件夹扫描动作。
 */
internal fun buildDesktopMeQuickActionDisplayModels(): List<DesktopMeQuickActionDisplayModel> =
    listOf(
        DesktopMeQuickActionDisplayModel(
            action = DesktopMeQuickAction.ScanMusic,
            title = "扫描本地音乐",
            subtitle = "更新媒体库资源",
        ),
    )

/**
 * 三项统计使用桌面端横向等权布局，保持宽屏 workspace 的自然间距。
 */
@Composable
private fun DesktopMeStatsRow(
    libraryStats: LibraryStats,
    localPlaylistCount: Int,
    onScanMusic: () -> Unit,
    onHomeSongsOpen: () -> Unit,
    onLocalPlaylistsOpen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        buildDesktopMeStatDisplayModels(
            libraryStats = libraryStats,
            localPlaylistCount = localPlaylistCount,
        ).forEach { item: DesktopMeStatDisplayModel ->
            DesktopMeStatCard(
                item = item,
                modifier = Modifier.weight(1f),
                onClick =
                    when (item.action) {
                        DesktopMeStatAction.OpenHomeSongs -> onHomeSongsOpen
                        DesktopMeStatAction.OpenLocalPlaylists -> onLocalPlaylistsOpen
                        null -> null
                    },
            )
        }
        DesktopMeScanCard(
            item = buildDesktopMeQuickActionDisplayModels().single(),
            onClick = onScanMusic,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Figma 统计卡只让已有音乐和歌单入口保持可用，听歌时长不伪造数据页面。
 */
@Composable
private fun DesktopMeStatCard(
    item: DesktopMeStatDisplayModel,
    modifier: Modifier,
    onClick: (() -> Unit)?,
) {
    val cardModifier: Modifier =
        if (onClick == null) {
            modifier
        } else {
            modifier.clickable(onClick = onClick)
        }
    Surface(
        modifier = cardModifier.height(164.dp),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = DesktopMeFigmaTokens.Accent,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = item.heading,
                    modifier = Modifier.weight(1f),
                    color = DesktopMeFigmaTokens.Muted.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = item.value,
                    color = DesktopMeFigmaTokens.Ink,
                    fontFamily = FontFamily.Serif,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 40.sp,
                )
                item.unit?.let { unit: String ->
                    Text(
                        text = unit,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                        color = DesktopMeFigmaTokens.Ink,
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                    )
                }
            }
            Text(
                text = item.title,
                color = DesktopMeFigmaTokens.Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Figma 扫描卡沿用桌面文件夹扫描回调，不在页面内直接启动扫描。 */
@Composable
private fun DesktopMeScanCard(
    item: DesktopMeQuickActionDisplayModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(164.dp),
        shape = DesktopMeFigmaTokens.ScanCardShape,
        color = DesktopMeFigmaTokens.ScanCardBackground,
        border = DesktopMeFigmaTokens.ScanCardBorder,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = DesktopMeFigmaTokens.IconCircleShape,
                color = DesktopMeFigmaTokens.ScanIconBackground,
            ) {
                Image(
                    painter = painterResource(resource = Res.drawable.desktop_me_scan_icon),
                    contentDescription = null,
                    modifier = Modifier.size(width = 25.dp, height = 27.5.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                text = item.title,
                modifier = Modifier.padding(top = 16.dp),
                color = DesktopMeFigmaTokens.Accent,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = item.subtitle,
                modifier = Modifier.padding(top = 4.dp),
                color = DesktopMeFigmaTokens.Accent.copy(alpha = 0.6f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}
