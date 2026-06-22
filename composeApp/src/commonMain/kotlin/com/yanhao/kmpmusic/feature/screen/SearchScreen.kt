package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.feature.components.AlbumCard
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.ArtistRow
import com.yanhao.kmpmusic.feature.components.SectionTitle
import com.yanhao.kmpmusic.feature.components.SongRow

/**
 * 搜索页，保留原型的关键词、快捷标签、范围过滤和分组结果。
 */
@Composable
fun SearchScreen(
    query: String,
    scope: SearchScope,
    result: SearchResult,
    currentSongId: String?,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onScope: (SearchScope) -> Unit,
    onSongOpen: (Song) -> Unit,
    onSongPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    val total: Int = result.songs.size + result.albums.size + result.artists.size
    Column(verticalArrangement = Arrangement.spacedBy(scaledDp(18.dp))) {
        AppHeader(title = "搜索", subtitle = "在本地音乐库中查找", onBack = onBack)
        SearchField(query = query, onQuery = onQuery)
        SearchTagRow(
            tags = listOf("旅行团乐队", "人声", "钢琴", "最近播放"),
            selectedTag = query,
            onTag = onQuery,
        )
        Text(
            text = "找到 $total 个结果",
            color = MusicColors.Muted,
            fontSize = scaledSp(13.sp),
            fontWeight = FontWeight.SemiBold,
        )
        SearchScopeRow(
            selectedScope = scope,
            onScope = onScope,
        )
        if (scope == SearchScope.All || scope == SearchScope.Songs) {
            SectionTitle(title = "歌曲", meta = "${result.songs.size} 首")
            Column(verticalArrangement = Arrangement.spacedBy(scaledDp(14.dp))) {
                result.songs.take(if (scope == SearchScope.All) 8 else result.songs.size).forEach { song ->
                    SongRow(song, song.id == currentSongId, onSongOpen, onSongPlay, onMore, dense = true)
                }
                if (result.songs.isEmpty()) EmptyState("没有找到歌曲", "试试搜索歌手、专辑名，或换一个关键词。")
            }
        }
        if (scope == SearchScope.All || scope == SearchScope.Albums) {
            SectionTitle(title = "专辑", meta = "${result.albums.size} 张")
            Row(horizontalArrangement = Arrangement.spacedBy(scaledDp(14.dp))) {
                result.albums.take(2).forEach { album ->
                    AlbumCard(album = album, onOpen = onAlbumOpen, modifier = Modifier.weight(weight = 1f))
                }
            }
            if (result.albums.isEmpty()) EmptyState("没有找到专辑", "本地专辑会在扫描后自动出现在这里。")
        }
        if (scope == SearchScope.All || scope == SearchScope.Artists) {
            SectionTitle(title = "歌手", meta = "${result.artists.size} 位")
            Column {
                result.artists.forEach { artist -> ArtistRow(artist = artist, onOpen = onArtistOpen) }
                if (result.artists.isEmpty()) EmptyState("没有找到歌手", "换成歌手名、流派或专辑名再试一次。")
            }
        }
    }
}

/**
 * 搜索输入框，固定高度和内边距，避免图标、文案和筛选入口互相挤压。
 */
@Composable
private fun SearchField(
    query: String,
    onQuery: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(scaledDp(MusicDimens.SearchFieldHeight)),
        shape = RoundedCornerShape(scaledDp(MusicDimens.SearchFieldRadius)),
        color = MusicColors.Soft,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = scaledDp(MusicDimens.SearchFieldHorizontalPadding)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(scaledDp(MusicDimens.SearchFieldContentGap)),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(scaledDp(MusicDimens.SearchFieldIconSize)),
                tint = MusicColors.Muted,
            )
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(weight = 1f),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = scaledSp(17.sp),
                    lineHeight = scaledSp(22.sp),
                    fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush = SolidColor(MusicColors.Accent),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "歌曲、歌手或专辑",
                            color = MusicColors.Muted,
                            fontSize = scaledSp(17.sp),
                            lineHeight = scaledSp(22.sp),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                },
            )
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                modifier = Modifier.size(scaledDp(MusicDimens.SearchFieldIconSize)),
                tint = MusicColors.Muted,
            )
        }
    }
}

/**
 * 搜索快捷标签横向带，标签变多时保持单行并允许左右滑动。
 */
@Composable
private fun SearchTagRow(
    tags: List<String>,
    selectedTag: String,
    onTag: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(scaledDp(MusicDimens.SearchChipGap)),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(items = tags) { tag ->
            FilterChip(
                selected = selectedTag == tag,
                onClick = { onTag(tag) },
                label = {
                    Text(
                        text = tag,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

/**
 * 搜索范围横向带，与快捷标签保持同样的溢出策略。
 */
@Composable
private fun SearchScopeRow(
    selectedScope: SearchScope,
    onScope: (SearchScope) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(scaledDp(MusicDimens.SearchChipGap)),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(items = SearchScope.entries) { item ->
            FilterChip(
                selected = selectedScope == item,
                onClick = { onScope(item) },
                label = {
                    Text(
                        text = item.label(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

/**
 * 搜索范围中文名。
 */
private fun SearchScope.label(): String {
    return when (this) {
        SearchScope.All -> "全部"
        SearchScope.Songs -> "歌曲"
        SearchScope.Albums -> "专辑"
        SearchScope.Artists -> "歌手"
    }
}

/**
 * 空结果提示。
 */
@Composable
private fun EmptyState(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(scaledDp(6.dp))) {
        Text(text = title, fontSize = scaledSp(16.sp), fontWeight = FontWeight.Bold)
        Text(text = detail, color = MusicColors.Muted, fontSize = scaledSp(14.sp))
    }
}
