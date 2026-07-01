package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.desktop.DesktopAlbumGrid
import com.yanhao.kmpmusic.feature.desktop.DesktopArtistStrip
import com.yanhao.kmpmusic.feature.desktop.DesktopContentRow
import com.yanhao.kmpmusic.feature.desktop.DesktopContentRowFolderIcon
import com.yanhao.kmpmusic.feature.desktop.DesktopContentRowSyncIcon
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType
import com.yanhao.kmpmusic.feature.desktop.DesktopPageHeader
import com.yanhao.kmpmusic.feature.desktop.DesktopPrimaryButton
import com.yanhao.kmpmusic.feature.desktop.DesktopSectionHeader
import com.yanhao.kmpmusic.feature.desktop.DesktopSegmentedControl
import com.yanhao.kmpmusic.feature.desktop.DesktopSongTable
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionEmptyMessage

private const val ARTIST_STRIP_COUNT = 4

/**
 * 本地音乐二级页在桌面端保留分段语义，避免不同入口都退化成来源管理页。
 */
@Composable
internal fun DesktopLocalMusicScreen(
    initialSection: LocalMusicSection,
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    sources: List<LocalMusicSourceSummary>,
    problems: List<LocalMusicProblem>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    var section: LocalMusicSection by remember(initialSection) {
        mutableStateOf(value = initialSection)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "本地音乐",
            eyebrow = section.desktopLocalMusicSubtitle(
                songCount = songs.size,
                albumCount = albums.size,
                artistCount = artists.size,
                sourceCount = sources.size,
                problemCount = problems.size,
            ),
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
            DesktopPrimaryButton(text = "重新扫描", onClick = onScan)
        }
        DesktopSegmentedControl(
            labels = LocalMusicSection.entries.map { sectionEntry: LocalMusicSection ->
                sectionEntry.desktopLabel()
            },
            selectedIndex = LocalMusicSection.entries.indexOf(section),
            onSelect = { index: Int -> section = LocalMusicSection.entries[index] },
        )
        Spacer(modifier = Modifier.height(18.dp))
        when (section) {
            LocalMusicSection.Songs -> DesktopSongTable(
                songs = songs,
                currentSongId = currentSongId,
                currentPlaybackStatus = currentPlaybackStatus,
                showFavoriteColumn = false,
                trailingDateLabel = "添加时间",
                onSongPlay = onSongPlay,
                onCurrentSongToggle = {},
                onMore = onMore,
            )
            LocalMusicSection.Albums -> DesktopLocalAlbumSection(
                albums = albums,
                onAlbumOpen = onAlbumOpen,
            )
            LocalMusicSection.Artists -> DesktopLocalArtistSection(
                artists = artists,
                onArtistOpen = onArtistOpen,
            )
            LocalMusicSection.Sources -> DesktopLocalSourcesSection(
                sources = sources,
                problems = problems,
            )
        }
    }
}

/**
 * 专辑分段复用现有桌面网格，保持与首页预览一致的阅读节奏。
 */
@Composable
private fun DesktopLocalAlbumSection(
    albums: List<Album>,
    onAlbumOpen: (Album) -> Unit,
) {
    if (albums.isEmpty()) {
        DesktopSectionEmptyMessage(message = "扫描后会按专辑自动聚合。")
        return
    }
    DesktopAlbumGrid(
        albums = albums,
        onAlbumOpen = onAlbumOpen,
    )
}

/**
 * 歌手分段按固定列数分组，避免桌面宽屏下条目宽度忽大忽小。
 */
@Composable
private fun DesktopLocalArtistSection(
    artists: List<Artist>,
    onArtistOpen: (Artist) -> Unit,
) {
    if (artists.isEmpty()) {
        DesktopSectionEmptyMessage(message = "扫描后会按歌手自动聚合。")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        artists.chunked(size = ARTIST_STRIP_COUNT).forEach { artistGroup: List<Artist> ->
            DesktopArtistStrip(
                artists = artistGroup,
                onArtistOpen = onArtistOpen,
            )
        }
    }
}

/**
 * 来源分段展示来源摘要和问题明细，让桌面端能直接查看扫描健康度。
 */
@Composable
private fun DesktopLocalSourcesSection(
    sources: List<LocalMusicSourceSummary>,
    problems: List<LocalMusicProblem>,
) {
    DesktopSectionHeader(title = "来源摘要")
    Spacer(modifier = Modifier.height(14.dp))
    if (sources.isEmpty()) {
        DesktopSectionEmptyMessage(message = "还没有来源记录，执行扫描后会显示本地文件夹摘要。")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            sources.forEach { source: LocalMusicSourceSummary ->
                DesktopContentRow(
                    icon = DesktopContentRowFolderIcon,
                    title = source.displayName,
                    subtitle = "${source.sourceKind.displayName} · ${source.songCount} 首歌曲 · ${source.problemCount} 个问题",
                    extraContent = {
                        Text(
                            text = source.lastScannedAt?.let(::formatDesktopSourceScanDate) ?: "尚未记录扫描时间",
                            color = DesktopMusicColors.Muted,
                            fontSize = DesktopMusicType.Body,
                        )
                    },
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(22.dp))
    DesktopSectionHeader(title = "扫描问题")
    Spacer(modifier = Modifier.height(14.dp))
    if (problems.isEmpty()) {
        DesktopSectionEmptyMessage(message = "当前没有扫描问题。")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            problems.forEach { problem: LocalMusicProblem ->
                DesktopContentRow(
                    icon = DesktopContentRowSyncIcon,
                    title = problem.fileName,
                    subtitle = "${problem.sourceKind.displayName} · ${problem.error.type.name}",
                    extraContent = {
                        Text(
                            text = problem.error.message,
                            color = DesktopMusicColors.Muted,
                            fontSize = DesktopMusicType.Body,
                        )
                    },
                )
            }
        }
    }
}

/** 本地音乐分段中文名与桌面分段控件保持一致。 */
private fun LocalMusicSection.desktopLabel(): String {
    return when (this) {
        LocalMusicSection.Songs -> "歌曲"
        LocalMusicSection.Albums -> "专辑"
        LocalMusicSection.Artists -> "歌手"
        LocalMusicSection.Sources -> "来源"
    }
}

/** 桌面本地音乐页根据当前分段生成副标题，避免不同入口共享同一误导文案。 */
private fun LocalMusicSection.desktopLocalMusicSubtitle(
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    sourceCount: Int,
    problemCount: Int,
): String {
    return when (this) {
        LocalMusicSection.Songs -> "已收录 $songCount 首可播放歌曲"
        LocalMusicSection.Albums -> "已聚合 $albumCount 张专辑"
        LocalMusicSection.Artists -> "已识别 $artistCount 位歌手"
        LocalMusicSection.Sources -> "来源 $sourceCount 个，问题 $problemCount 个"
    }
}

/** 来源摘要里的扫描时间只需要稳定日期文本，不依赖组件文件内的私有实现。 */
private fun formatDesktopSourceScanDate(timestampMillis: Long): String {
    val epochDay: Long = timestampMillis.floorDiv(86_400_000L)
    val shiftedDay: Long = epochDay + 719_468L
    val eraOffset: Long = if (shiftedDay >= 0L) shiftedDay else shiftedDay - 146_096L
    val eraIndex: Long = eraOffset / 146_097L
    val dayOfEra: Long = shiftedDay - eraIndex * 146_097L
    val yearOfEra: Long = (
        dayOfEra - dayOfEra / 1_460L + dayOfEra / 36_524L - dayOfEra / 146_096L
        ) / 365L
    val yearBase: Long = yearOfEra + eraIndex * 400L
    val dayOfYear: Long = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
    val monthPrime: Long = (5L * dayOfYear + 2L) / 153L
    val day: Int = (dayOfYear - (153L * monthPrime + 2L) / 5L + 1L).toInt()
    val month: Int = (monthPrime + if (monthPrime < 10L) 3L else -9L).toInt()
    val year: Int = (yearBase + if (month <= 2) 1L else 0L).toInt()
    return "${year.toString().padStart(length = 4, padChar = '0')}-${month.toString().padStart(length = 2, padChar = '0')}-${day.toString().padStart(length = 2, padChar = '0')}"
}
