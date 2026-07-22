package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.feature.app.LocalPlaylistCardDisplayModel
import com.yanhao.kmpmusic.feature.desktop.DesktopSearchTokens
import com.yanhao.kmpmusic.feature.desktop.components.DesktopBackTitleToolbar

/** Desktop 搜索的四个单类型结果 Tab，歌单不扩张共享搜索范围。 */
internal enum class DesktopSearchResultTab(
    val label: String,
    val searchScope: SearchScope?,
) {
    Songs(label = "歌曲", searchScope = SearchScope.Songs),
    Albums(label = "专辑", searchScope = SearchScope.Albums),
    Artists(label = "歌手", searchScope = SearchScope.Artists),
    Playlists(label = "歌单", searchScope = SearchScope.All),
}

/**
 * 搜索页按 Figma `1113:1481` 承载默认态、单类型结果与本地历史，不暴露来源上下文文案。
 */
@Composable
fun DesktopSearchScreen(
    query: String,
    activeQuery: String,
    scope: SearchScope,
    result: SearchResult,
    playlists: List<LocalPlaylistCardDisplayModel>,
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
    onLike: (String) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
    onPlaylistOpen: (String) -> Unit,
) {
    var selectedTab: DesktopSearchResultTab by rememberSaveable {
        mutableStateOf(value = initialDesktopSearchResultTab(scope = scope))
    }
    val hasResolvedResults: Boolean =
        shouldShowDesktopSearchResults(
            query = query,
            activeQuery = activeQuery,
        )
    val matchingPlaylists: List<LocalPlaylistCardDisplayModel> =
        filterDesktopSearchPlaylists(
            query = activeQuery,
            playlists = playlists,
        )
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(color = DesktopSearchTokens.Background),
    ) {
        DesktopBackTitleToolbar(
            title = "搜索",
            onBack = onBack,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(all = DesktopSearchTokens.ContentPadding),
        ) {
            DesktopSearchInput(
                value = query,
                onValueChange = onQuery,
                onSubmit = onCommitSearch,
                modifier =
                    Modifier
                        .widthIn(max = DesktopSearchTokens.InputMaxWidth)
                        .fillMaxWidth()
                        .align(alignment = Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(height = 48.dp))
            if (query.isBlank()) {
                DesktopSearchHistorySection(
                    history = history,
                    onHistoryClick = onHistoryClick,
                    onHistoryRemove = onHistoryRemove,
                    onHistoryClear = onHistoryClear,
                )
                Spacer(modifier = Modifier.height(height = 32.dp))
            }
            DesktopSearchResultTabs(
                selectedTab = selectedTab,
                onTabSelected = { tab: DesktopSearchResultTab ->
                    selectedTab = tab
                    tab.searchScope?.let(onScope)
                },
            )
            Spacer(modifier = Modifier.height(height = 24.dp))
            when {
                query.isBlank() -> {
                    DesktopSearchExplorationState(modifier = Modifier.weight(weight = 1f))
                }

                !hasResolvedResults -> {
                    DesktopSearchPendingState(
                        query = query,
                        modifier = Modifier.weight(weight = 1f),
                    )
                }

                else -> {
                    DesktopSearchResultsSection(
                        tab = selectedTab,
                        query = activeQuery,
                        result = result,
                        playlists = matchingPlaylists,
                        currentSongId = currentSongId,
                        currentPlaybackStatus = currentPlaybackStatus,
                        onSongPlay = onSongPlay,
                        onCurrentSongToggle = onCurrentSongToggle,
                        onMore = onMore,
                        onLike = onLike,
                        onAlbumOpen = onAlbumOpen,
                        onArtistOpen = onArtistOpen,
                        onPlaylistOpen = onPlaylistOpen,
                        modifier = Modifier.weight(weight = 1f),
                    )
                }
            }
        }
    }
}

/** [SearchScope.All] 只在 Desktop 歌单 Tab 使用，以保留完整本地投影供页面自行按名称过滤。 */
internal fun initialDesktopSearchResultTab(scope: SearchScope): DesktopSearchResultTab =
    when (scope) {
        SearchScope.All -> DesktopSearchResultTab.Playlists
        SearchScope.Songs -> DesktopSearchResultTab.Songs
        SearchScope.Albums -> DesktopSearchResultTab.Albums
        SearchScope.Artists -> DesktopSearchResultTab.Artists
    }

/** 只有输入词和防抖生效词一致时才显示结果，避免显示过期的匹配内容。 */
internal fun shouldShowDesktopSearchResults(
    query: String,
    activeQuery: String,
): Boolean {
    val normalizedQuery: String = query.trim()
    val normalizedActiveQuery: String = activeQuery.trim()
    return normalizedQuery.isNotEmpty() && normalizedQuery == normalizedActiveQuery
}

/** 本地歌单搜索只匹配用户可见名称，保持当前一级歌单页的排序和投影。 */
internal fun filterDesktopSearchPlaylists(
    query: String,
    playlists: List<LocalPlaylistCardDisplayModel>,
): List<LocalPlaylistCardDisplayModel> {
    val normalizedQuery: String = query.trim()
    if (normalizedQuery.isBlank()) {
        return emptyList()
    }
    return playlists.filter { playlist: LocalPlaylistCardDisplayModel ->
        playlist.name.contains(other = normalizedQuery, ignoreCase = true)
    }
}

/** 无命中提示必须明确当前类型，避免把单类型 Tab 误解为全局搜索失败。 */
internal fun desktopSearchNoResultTitle(
    tab: DesktopSearchResultTab,
    query: String,
): String = "未找到与“${query.trim()}”相关的${tab.label}"

/** 搜索输入独立还原 Figma 的固定尺寸和浅蓝输入容器，避免影响其它 Desktop 表单。 */
@Composable
private fun DesktopSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        textStyle =
            TextStyle(
                color = DesktopSearchTokens.SupportingText,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
            ),
        decorationBox = { innerTextField: @Composable () -> Unit ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(size = 12.dp),
                color = DesktopSearchTokens.InputContainer,
                border = BorderStroke(width = 1.dp, color = DesktopSearchTokens.Line),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = DesktopSearchTokens.MutedText,
                        modifier = Modifier.size(size = 18.dp),
                    )
                    Box(
                        modifier = Modifier.weight(weight = 1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = "搜索歌曲、专辑、歌手…",
                                color = DesktopSearchTokens.MutedText,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        innerTextField()
                    }
                    if (value.isNotBlank()) {
                        Surface(
                            modifier = Modifier.size(size = 24.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            onClick = { onValueChange("") },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "清空搜索词",
                                    tint = DesktopSearchTokens.MutedText,
                                    modifier = Modifier.size(size = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

/** 结果 Tab 保持 Figma 下划线样式，切换只影响当前类型，不重置关键词。 */
@Composable
private fun DesktopSearchResultTabs(
    selectedTab: DesktopSearchResultTab,
    onTabSelected: (DesktopSearchResultTab) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 32.dp)) {
            DesktopSearchResultTab.entries.forEach { tab: DesktopSearchResultTab ->
                val isSelected: Boolean = tab == selectedTab
                Column(
                    modifier =
                        Modifier
                            .widthIn(min = 48.dp, max = 48.dp)
                            .height(height = 38.dp)
                            .clickable { onTabSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = tab.label,
                        color = if (isSelected) DesktopSearchTokens.Accent else DesktopSearchTokens.SupportingText,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.weight(weight = 1f))
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(height = 2.dp)
                                .background(color = if (isSelected) DesktopSearchTokens.Accent else Color.Transparent),
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height = 1.dp)
                    .background(color = DesktopSearchTokens.FaintLine),
        )
    }
}

/** 历史标题、清空入口和 Chips 都仅使用当前上下文的真实持久化记录。 */
@Composable
private fun DesktopSearchHistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onHistoryClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "搜索历史",
                color = DesktopSearchTokens.Title,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium,
            )
            if (history.isNotEmpty()) {
                Surface(
                    modifier = Modifier.size(size = 32.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    onClick = onHistoryClear,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "清空搜索历史",
                            tint = DesktopSearchTokens.MutedText,
                            modifier = Modifier.size(size = 16.dp),
                        )
                    }
                }
            }
        }
        if (history.isNotEmpty()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(state = rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
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
    }
}

/** 单条删除始终预留稳定位置，仅在悬停或聚焦时展示，避免默认 Chip 视觉拥挤。 */
@Composable
private fun DesktopSearchHistoryChip(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isHovered: Boolean by interactionSource.collectIsHoveredAsState()
    val isFocused: Boolean by interactionSource.collectIsFocusedAsState()
    Surface(
        modifier =
            Modifier
                .hoverable(interactionSource = interactionSource)
                .focusable(interactionSource = interactionSource),
        shape = CircleShape,
        color = DesktopSearchTokens.HistoryChip,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                color = DesktopSearchTokens.SupportingText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(modifier = Modifier.size(size = 20.dp)) {
                if (isHovered || isFocused) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = Color.Transparent,
                        onClick = onRemove,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "删除搜索历史 $text",
                                tint = DesktopSearchTokens.SupportingText,
                                modifier = Modifier.size(size = 14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 空关键词时展示 Figma 的探索引导；结果为空时复用同一视觉骨架。 */
@Composable
private fun DesktopSearchExplorationState(modifier: Modifier = Modifier) {
    DesktopSearchStateMessage(
        title = "输入关键词开始探索",
        subtitle = "美妙的音乐世界正在等待着你",
        modifier = modifier,
    )
}

/** 防抖尚未完成时不把旧结果或无命中提示伪装成当前查询的状态。 */
@Composable
private fun DesktopSearchPendingState(
    query: String,
    modifier: Modifier = Modifier,
) {
    DesktopSearchStateMessage(
        title = "正在准备“${query.trim()}”的搜索结果",
        subtitle = null,
        modifier = modifier,
    )
}

/** 搜索状态信息共用图标、居中位置和文案层级，避免多种空态视觉漂移。 */
@Composable
private fun DesktopSearchStateMessage(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            Surface(
                modifier = Modifier.size(size = 128.dp),
                shape = CircleShape,
                color = DesktopSearchTokens.HistoryChip.copy(alpha = 0.3f),
                border = BorderStroke(width = 1.dp, color = DesktopSearchTokens.FaintLine),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = DesktopSearchTokens.Accent.copy(alpha = 0.28f),
                        modifier = Modifier.size(size = 45.dp),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                Text(
                    text = title,
                    color = DesktopSearchTokens.SupportingText.copy(alpha = 0.8f),
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let { value: String ->
                    Text(
                        text = value,
                        color = DesktopSearchTokens.MutedText,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** 当前 Tab 只承载一种结果列表，且始终复用对应一级页面的内容组件。 */
@Composable
private fun DesktopSearchResultsSection(
    tab: DesktopSearchResultTab,
    query: String,
    result: SearchResult,
    playlists: List<LocalPlaylistCardDisplayModel>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
    onPlaylistOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (tab) {
        DesktopSearchResultTab.Songs -> {
            if (result.songs.isEmpty()) {
                DesktopSearchNoResultState(tab = tab, query = query, modifier = modifier)
                return
            }
            DesktopHomeSongList(
                songs = result.songs,
                currentSongId = currentSongId,
                isPlaying = currentPlaybackStatus == PlaybackStatus.Playing,
                onSongPlay = onSongPlay,
                onCurrentSongToggle = onCurrentSongToggle,
                onMore = onMore,
                onLike = onLike,
                modifier = modifier,
            )
        }

        DesktopSearchResultTab.Albums -> {
            if (result.albums.isEmpty()) {
                DesktopSearchNoResultState(tab = tab, query = query, modifier = modifier)
                return
            }
            DesktopLocalAlbumSection(
                albums = result.albums,
                onAlbumOpen = onAlbumOpen,
                modifier = modifier,
            )
        }

        DesktopSearchResultTab.Artists -> {
            if (result.artists.isEmpty()) {
                DesktopSearchNoResultState(tab = tab, query = query, modifier = modifier)
                return
            }
            DesktopLocalArtistResultsList(
                artists = result.artists,
                onArtistOpen = onArtistOpen,
                modifier = modifier,
            )
        }

        DesktopSearchResultTab.Playlists -> {
            if (playlists.isEmpty()) {
                DesktopSearchNoResultState(tab = tab, query = query, modifier = modifier)
                return
            }
            DesktopSearchPlaylistResults(
                playlists = playlists,
                onPlaylistOpen = onPlaylistOpen,
                modifier = modifier,
            )
        }
    }
}

/** 歌单结果复用一级页网格，但不插入管理、新建或创建卡片。 */
@Composable
private fun DesktopSearchPlaylistResults(
    playlists: List<LocalPlaylistCardDisplayModel>,
    onPlaylistOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState()),
    ) {
        DesktopLocalPlaylistGrid(
            playlists = playlists,
            onPlaylistOpen = onPlaylistOpen,
        )
    }
}

/** 当前分类没有匹配项时，明确说明关键词和结果类型。 */
@Composable
private fun DesktopSearchNoResultState(
    tab: DesktopSearchResultTab,
    query: String,
    modifier: Modifier = Modifier,
) {
    DesktopSearchStateMessage(
        title = desktopSearchNoResultTitle(tab = tab, query = query),
        subtitle = null,
        modifier = modifier,
    )
}
