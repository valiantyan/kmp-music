package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType
import com.yanhao.kmpmusic.feature.desktop.components.DesktopAlbumGrid
import com.yanhao.kmpmusic.feature.desktop.components.DesktopArtistStrip
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPageHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSecondaryButton
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionEmptyMessage
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSegmentedControl
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSongTable
import com.yanhao.kmpmusic.feature.desktop.components.DesktopTextInput
import com.yanhao.kmpmusic.feature.desktop.components.DesktopTinyTextButton

/**
 * 搜索页根据入口上下文展示独立的历史、范围和派生结果，避免首页与收藏搜索串味。
 */
@Composable
fun DesktopSearchScreen(
    context: SearchContext,
    query: String,
    activeQuery: String,
    scope: SearchScope,
    result: SearchResult,
    history: List<String>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onQuery: (String) -> Unit,
    onScope: (SearchScope) -> Unit,
    onBack: () -> Unit,
    onCommitSearch: () -> Unit,
    onHistoryClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onHistoryClear: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    val backLabel: String = when (context) {
        SearchContext.LocalLibrary -> "← 本地音乐"
        SearchContext.Favorites -> "← 收藏"
    }
    val subtitle: String = when (context) {
        SearchContext.LocalLibrary -> "在本地音乐中搜索歌曲、专辑、歌手"
        SearchContext.Favorites -> "在收藏中搜索歌曲、专辑、歌手"
    }
    val isEmptyQuery: Boolean = query.isBlank()
    val shouldShowResults: Boolean = shouldShowDesktopSearchResults(
        query = query,
        activeQuery = activeQuery,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "搜索",
            eyebrow = subtitle,
        ) {
            DesktopSecondaryButton(
                text = backLabel,
                onClick = onBack,
            )
        }
        DesktopTextInput(
            value = query,
            onValueChange = onQuery,
            placeholder = "搜索歌曲、专辑、歌手",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Rounded.Search,
            onSubmit = onCommitSearch,
        )
        Spacer(modifier = Modifier.height(18.dp))
        DesktopSearchScopeTabs(
            selectedScope = scope,
            onScope = onScope,
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (isEmptyQuery) {
            DesktopSearchHistorySection(
                history = history,
                onHistoryClick = onHistoryClick,
                onHistoryRemove = onHistoryRemove,
                onHistoryClear = onHistoryClear,
            )
            Spacer(modifier = Modifier.height(44.dp))
            DesktopSectionEmptyMessage(message = "输入关键词后显示匹配歌曲、专辑和歌手")
            return
        }
        if (!shouldShowResults) {
            DesktopSectionEmptyMessage(message = "正在准备“${query.trim()}”的搜索结果")
            return
        }
        DesktopSearchResultsSection(
            query = query,
            scope = scope,
            result = result,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = onCurrentSongToggle,
            onMore = onMore,
            onAlbumOpen = onAlbumOpen,
            onArtistOpen = onArtistOpen,
        )
    }
}

// 桌面端同样要等待 activeQuery 跟输入同步，避免防抖期间误报空结果。
internal fun shouldShowDesktopSearchResults(
    query: String,
    activeQuery: String,
): Boolean {
    val normalizedQuery: String = query.trim()
    val normalizedActiveQuery: String = activeQuery.trim()
    return normalizedQuery.isNotEmpty() && normalizedQuery == normalizedActiveQuery
}

/**
 * 搜索范围切换必须显式绑定 [SearchScope]，避免 UI 文案顺序与业务枚举脱节。
 */
@Composable
private fun DesktopSearchScopeTabs(
    selectedScope: SearchScope,
    onScope: (SearchScope) -> Unit,
) {
    DesktopSegmentedControl(
        labels = listOf("全部", "歌曲", "专辑", "歌手"),
        selectedIndex = when (selectedScope) {
            SearchScope.All -> 0
            SearchScope.Songs -> 1
            SearchScope.Albums -> 2
            SearchScope.Artists -> 3
        },
        onSelect = { index: Int ->
            onScope(
                when (index) {
                    0 -> SearchScope.All
                    1 -> SearchScope.Songs
                    2 -> SearchScope.Albums
                    else -> SearchScope.Artists
                },
            )
        },
    )
}

/**
 * 历史词条既要支持整词回填，也要允许单独删除，避免用户只能整体清空。
 */
@Composable
private fun DesktopSearchHistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onHistoryClear: () -> Unit,
) {
    if (history.isEmpty()) {
        DesktopSectionEmptyMessage(message = "暂无最近搜索")
        return
    }
    DesktopSectionHeader(
        title = "最近搜索",
        actionLabel = "清空",
        onAction = onHistoryClear,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        history.forEach { item: String ->
            DesktopSearchHistoryChip(
                text = item,
                onClick = { onHistoryClick(item) },
                onRemove = { onHistoryRemove(item) },
            )
        }
    }
}

/**
 * 历史词条既要支持整词回填，也要允许单独删除，避免用户只能整体清空。
 */
@Composable
private fun DesktopSearchHistoryChip(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.78f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                color = DesktopMusicColors.MutedStrong,
                fontSize = DesktopMusicType.Body,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            DesktopTinyTextButton(
                text = "×",
                onClick = onRemove,
            )
        }
    }
}

/**
 * 搜索结果区按范围保留歌曲、专辑、歌手三种结构，避免所有命中都被压扁成歌曲表格。
 */
@Composable
private fun DesktopSearchResultsSection(
    query: String,
    scope: SearchScope,
    result: SearchResult,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    val showSongs: Boolean = scope == SearchScope.All || scope == SearchScope.Songs
    val showAlbums: Boolean = scope == SearchScope.All || scope == SearchScope.Albums
    val showArtists: Boolean = scope == SearchScope.All || scope == SearchScope.Artists
    val scopedSongCount: Int = if (showSongs) result.songs.size else 0
    val scopedAlbumCount: Int = if (showAlbums) result.albums.size else 0
    val scopedArtistCount: Int = if (showArtists) result.artists.size else 0
    val hasResults: Boolean = scopedSongCount > 0 || scopedAlbumCount > 0 || scopedArtistCount > 0
    if (!hasResults) {
        DesktopSectionEmptyMessage(message = "没有找到“$query”相关内容，请尝试搜索歌曲名、专辑名或歌手名。")
        return
    }
    Text(
        text = when (scope) {
            SearchScope.All -> "找到 ${result.songs.size} 首歌曲、${result.albums.size} 张专辑、${result.artists.size} 位歌手"
            SearchScope.Songs -> "找到 ${result.songs.size} 首歌曲"
            SearchScope.Albums -> "找到 ${result.albums.size} 张专辑"
            SearchScope.Artists -> "找到 ${result.artists.size} 位歌手"
        },
        color = DesktopMusicColors.Muted,
        fontSize = DesktopMusicType.Eyebrow,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(18.dp))
    if (showSongs) {
        DesktopSongTable(
            songs = result.songs,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongPlay = onSongPlay,
            onCurrentSongToggle = onCurrentSongToggle,
            onMore = onMore,
        )
    }
    if (showAlbums && result.albums.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        DesktopSectionHeader(title = "匹配专辑")
        Spacer(modifier = Modifier.height(14.dp))
        DesktopAlbumGrid(
            albums = result.albums,
            onAlbumOpen = onAlbumOpen,
        )
    }
    if (showArtists && result.artists.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        DesktopSectionHeader(title = "匹配歌手")
        Spacer(modifier = Modifier.height(14.dp))
        DesktopArtistStrip(
            artists = result.artists,
            onArtistOpen = onArtistOpen,
        )
    }
}
