package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType
import com.yanhao.kmpmusic.feature.desktop.components.DesktopAlbumCard
import com.yanhao.kmpmusic.feature.desktop.components.DesktopArtistStrip
import com.yanhao.kmpmusic.feature.desktop.components.DesktopAutoHideLazyScrollbar
import com.yanhao.kmpmusic.feature.desktop.components.DesktopContentRow
import com.yanhao.kmpmusic.feature.desktop.components.DesktopContentRowFolderIcon
import com.yanhao.kmpmusic.feature.desktop.components.DesktopContentRowSyncIcon
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPageHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPageTitleToolbar
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPrimaryButton
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionEmptyMessage
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSegmentedControl
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSongTable
import com.yanhao.kmpmusic.feature.screen.LocalMusicDiscoveryPlatform
import com.yanhao.kmpmusic.feature.screen.cancelledScanResultDetail
import com.yanhao.kmpmusic.feature.screen.cancelledScanResultTitle
import com.yanhao.kmpmusic.feature.screen.formatLocalMusicScanDate
import com.yanhao.kmpmusic.feature.screen.localMusicScanActionLabel
import com.yanhao.kmpmusic.feature.screen.localMusicSourceKindLabel

private const val ARTIST_STRIP_COUNT = 4
private const val DESKTOP_LOCAL_ALBUM_WIDE_COLUMNS = 4
private const val DESKTOP_LOCAL_ALBUM_NARROW_COLUMNS = 2

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
    scanState: LocalMusicScanState,
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
    if (initialSection == LocalMusicSection.Artists) {
        DesktopLocalArtistPage(
            artists = artists,
            onArtistOpen = onArtistOpen,
        )
        return
    }
    if (section == LocalMusicSection.Albums) {
        DesktopLocalAlbumPage(
            albums = albums,
            onAlbumOpen = onAlbumOpen,
        )
        return
    }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "本地音乐",
            eyebrow =
                section.desktopLocalMusicSubtitle(
                    songCount = songs.size,
                    albumCount = albums.size,
                    artistCount = artists.size,
                    sourceCount = sources.size,
                    problemCount = problems.size,
                ),
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
            DesktopPrimaryButton(
                text =
                    localMusicScanActionLabel(
                        scanState = scanState,
                        platform = LocalMusicDiscoveryPlatform.Desktop,
                    ),
                onClick = onScan,
            )
        }
        DesktopLocalMusicSegmentedControl(
            section = section,
            onSection = { selectedSection: LocalMusicSection -> section = selectedSection },
        )
        Spacer(modifier = Modifier.height(18.dp))
        when (section) {
            LocalMusicSection.Songs -> {
                DesktopSongTable(
                    songs = songs,
                    currentSongId = currentSongId,
                    currentPlaybackStatus = currentPlaybackStatus,
                    showFavoriteColumn = false,
                    trailingDateLabel = "添加时间",
                    onSongPlay = onSongPlay,
                    onCurrentSongToggle = {},
                    onMore = onMore,
                )
            }

            LocalMusicSection.Albums -> {
                DesktopLocalAlbumSection(
                    albums = albums,
                    onAlbumOpen = onAlbumOpen,
                )
            }

            LocalMusicSection.Artists -> {
                DesktopLocalArtistSection(
                    artists = artists,
                    onArtistOpen = onArtistOpen,
                )
            }

            LocalMusicSection.Sources -> {
                DesktopLocalSourcesSection(
                    sources = sources,
                    problems = problems,
                    scanState = scanState,
                )
            }
        }
    }
}

/**
 * 桌面专辑分段使用固定标题和独立滚动网格，避免回退到本地音乐管理页头部。
 */
@Composable
private fun DesktopLocalAlbumPage(
    albums: List<Album>,
    onAlbumOpen: (Album) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DesktopPageTitleToolbar(title = "专辑")
        DesktopLocalAlbumSection(
            albums = albums,
            onAlbumOpen = onAlbumOpen,
            modifier = Modifier.weight(weight = 1f),
        )
    }
}

/**
 * 本地音乐分段控件保持四个入口一致，专辑页只是替换外层标题区域。
 */
@Composable
private fun DesktopLocalMusicSegmentedControl(
    section: LocalMusicSection,
    onSection: (LocalMusicSection) -> Unit,
) {
    DesktopSegmentedControl(
        labels =
            LocalMusicSection.entries.map { sectionEntry: LocalMusicSection ->
                sectionEntry.desktopLabel()
            },
        selectedIndex = LocalMusicSection.entries.indexOf(section),
        onSelect = { index: Int -> onSection(LocalMusicSection.entries[index]) },
    )
}

/**
 * 专辑分段复用现有桌面网格，保持与首页预览一致的阅读节奏。
 */
@Composable
private fun DesktopLocalAlbumSection(
    albums: List<Album>,
    onAlbumOpen: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (albums.isEmpty()) {
        Box(modifier = modifier.fillMaxSize()) {
            DesktopSectionEmptyMessage(message = "扫描后会按专辑自动聚合。")
        }
        return
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns: Int =
            if (maxWidth < 720.dp) {
                DESKTOP_LOCAL_ALBUM_NARROW_COLUMNS
            } else {
                DESKTOP_LOCAL_ALBUM_WIDE_COLUMNS
            }
        val albumRows: List<List<Album>> = albums.chunked(size = columns)
        val listState: LazyListState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(
                    items = albumRows,
                    key = { rowAlbums: List<Album> ->
                        rowAlbums.joinToString(separator = "|") { album: Album -> album.id }
                    },
                    contentType = { "desktop-local-album-row" },
                ) { rowAlbums: List<Album> ->
                    DesktopLocalAlbumRow(
                        rowAlbums = rowAlbums,
                        columns = columns,
                        onAlbumOpen = onAlbumOpen,
                    )
                }
            }
            DesktopAutoHideLazyScrollbar(
                listState = listState,
                modifier = Modifier.align(alignment = Alignment.CenterEnd),
            )
        }
    }
}

/**
 * 专辑网格行补齐空槽位，避免最后一行卡片宽度变大。
 */
@Composable
private fun DesktopLocalAlbumRow(
    rowAlbums: List<Album>,
    columns: Int,
    onAlbumOpen: (Album) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        rowAlbums.forEach { album: Album ->
            DesktopAlbumCard(
                album = album,
                onOpen = onAlbumOpen,
                modifier = Modifier.weight(weight = 1f),
            )
        }
        repeat(times = columns - rowAlbums.size) {
            Spacer(modifier = Modifier.weight(weight = 1f))
        }
    }
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
    scanState: LocalMusicScanState,
) {
    if (scanState is LocalMusicScanState.Cancelled) {
        DesktopContentRow(
            icon = DesktopContentRowSyncIcon,
            title = cancelledScanResultTitle(scanState = scanState),
            subtitle = cancelledScanResultDetail(scanState = scanState),
        )
        Spacer(modifier = Modifier.height(18.dp))
    }
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
                    subtitle =
                        "${localMusicSourceKindLabel(sourceKind = source.sourceKind, platform = LocalMusicDiscoveryPlatform.Desktop)} · " +
                            "${source.songCount} 首歌曲 · ${source.problemCount} 个问题",
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
                    subtitle =
                        "${localMusicSourceKindLabel(sourceKind = problem.sourceKind, platform = LocalMusicDiscoveryPlatform.Desktop)} · " +
                            problem.error.type.name,
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
private fun LocalMusicSection.desktopLabel(): String =
    when (this) {
        LocalMusicSection.Songs -> "歌曲"
        LocalMusicSection.Albums -> "专辑"
        LocalMusicSection.Artists -> "歌手"
        LocalMusicSection.Sources -> "来源"
    }

/** 桌面本地音乐页根据当前分段生成副标题，避免不同入口共享同一误导文案。 */
private fun LocalMusicSection.desktopLocalMusicSubtitle(
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    sourceCount: Int,
    problemCount: Int,
): String =
    when (this) {
        LocalMusicSection.Songs -> "已收录 $songCount 首可播放歌曲"
        LocalMusicSection.Albums -> "已聚合 $albumCount 张专辑"
        LocalMusicSection.Artists -> "已识别 $artistCount 位歌手"
        LocalMusicSection.Sources -> "来源 $sourceCount 个，问题 $problemCount 个"
    }

/** 来源摘要里的扫描时间只需要稳定日期文本，不依赖组件文件内的私有实现。 */
private fun formatDesktopSourceScanDate(timestampMillis: Long): String = formatLocalMusicScanDate(timestampMillis = timestampMillis)
