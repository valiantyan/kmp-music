package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.SearchResult

/**
 * Figma 搜索结果 tab，其中歌单只保留视觉占位，等待后续领域模型支持。
 */
internal enum class SearchResultTab(
    val label: String,
    val scope: SearchScope?,
) {
    Songs(label = "歌曲", scope = SearchScope.Songs),
    Albums(label = "专辑", scope = SearchScope.Albums),
    Artists(label = "歌手", scope = SearchScope.Artists),
    Playlists(label = "歌单", scope = null),
}

/**
 * 搜索结果外层 [androidx.compose.foundation.lazy.LazyColumn] 的行模型。
 */
internal sealed interface SearchResultLazyRow {
    /** 歌曲结果逐行懒加载，避免一次性组合整页歌曲。 */
    data class HomeSongItem(
        val song: Song,
        val queueSongs: List<Song>,
    ) : SearchResultLazyRow

    /** 专辑结果保持两列一行，让网格也能按行懒加载。 */
    data class HomeAlbumRow(
        val albums: List<Album>,
    ) : SearchResultLazyRow

    /** 歌手结果逐行懒加载，复用全局歌手行组件。 */
    data class HomeArtistItem(
        val artist: Artist,
    ) : SearchResultLazyRow

    /** 空结果或暂不支持能力使用单独消息行，避免 UI 层重复判断。 */
    data class Message(
        val text: String,
    ) : SearchResultLazyRow
}

/** 只有输入词与已生效防抖词一致时才展示结果，避免 pending 输入渲染旧结果或全量结果。 */
internal fun shouldShowSearchResultContent(
    query: String,
    activeQuery: String,
): Boolean {
    val normalizedQuery: String = normalizeVisibleSearchQuery(value = query)
    val normalizedActiveQuery: String = normalizeVisibleSearchQuery(value = activeQuery)
    return normalizedQuery.isNotEmpty() && normalizedQuery == normalizedActiveQuery
}

/** 用户已输入但防抖词尚未追上时，显示 pending 文案而不是初始空态。 */
internal fun shouldShowSearchPendingState(
    query: String,
    activeQuery: String,
): Boolean {
    val normalizedQuery: String = normalizeVisibleSearchQuery(value = query)
    val normalizedActiveQuery: String = normalizeVisibleSearchQuery(value = activeQuery)
    return normalizedQuery.isNotEmpty() && normalizedQuery != normalizedActiveQuery
}

/** 只有存在真实搜索历史时才显示历史区，输入搜索时也保留历史入口。 */
internal fun shouldShowSearchHistorySection(
    history: List<String>,
): Boolean {
    return history.isNotEmpty()
}

/** 将旧的 [SearchScope.All] 兼容到新版 Figma 的默认“歌曲”tab。 */
internal fun visibleSearchResultTab(scope: SearchScope): SearchResultTab {
    return when (scope) {
        SearchScope.All,
        SearchScope.Songs,
        -> SearchResultTab.Songs
        SearchScope.Albums -> SearchResultTab.Albums
        SearchScope.Artists -> SearchResultTab.Artists
    }
}

/** 搜索历史 chips 只来自真实历史，不再用设计稿示例兜底。 */
internal fun visibleSearchHistoryChips(history: List<String>): List<String> {
    return history
}

/** 将搜索结果拆成可懒加载的稳定行，避免结果项过多时首帧组合卡顿。 */
internal fun buildSearchResultLazyRows(
    selectedTab: SearchResultTab,
    result: SearchResult,
): List<SearchResultLazyRow> {
    return when (selectedTab) {
        SearchResultTab.Songs -> buildSongResultRows(songs = result.songs)
        SearchResultTab.Albums -> buildAlbumResultRows(albums = result.albums)
        SearchResultTab.Artists -> buildArtistResultRows(artists = result.artists)
        SearchResultTab.Playlists -> listOf(SearchResultLazyRow.Message(text = "当前版本暂不支持歌单搜索"))
    }
}

/** 只有结果区真正可见时才构建行模型，避免空词或 pending 防抖时处理全量曲库。 */
internal fun buildVisibleSearchResultLazyRows(
    query: String,
    activeQuery: String,
    selectedTab: SearchResultTab,
    result: SearchResult,
): List<SearchResultLazyRow> {
    if (!shouldShowSearchResultContent(query = query, activeQuery = activeQuery)) {
        return emptyList()
    }
    return buildSearchResultLazyRows(
        selectedTab = selectedTab,
        result = result,
    )
}

// 每个搜索结果行使用数据身份做 key，减少输入和滚动时的节点重建。
internal fun searchResultLazyRowKey(row: SearchResultLazyRow): String {
    return when (row) {
        is SearchResultLazyRow.HomeSongItem -> "search-home-song-${row.song.id}"
        is SearchResultLazyRow.HomeAlbumRow -> row.albums.joinToString(
            prefix = "search-home-album-row-",
            separator = "|",
        ) { album: Album -> album.id }
        is SearchResultLazyRow.HomeArtistItem -> "search-home-artist-${row.artist.id}"
        is SearchResultLazyRow.Message -> "search-message-${row.text}"
    }
}

// contentType 按行形态分组，帮助 LazyColumn 复用相同类型的 Compose 节点。
internal fun searchResultLazyRowContentType(row: SearchResultLazyRow): String {
    return when (row) {
        is SearchResultLazyRow.HomeSongItem -> "search-home-song"
        is SearchResultLazyRow.HomeAlbumRow -> "search-home-album-row"
        is SearchResultLazyRow.HomeArtistItem -> "search-home-artist"
        is SearchResultLazyRow.Message -> "search-message"
    }
}

// 搜索可见性只关心用户可见词，首尾空格不应触发新旧结果闪烁。
private fun normalizeVisibleSearchQuery(value: String): String {
    return value.trim()
}

// 歌曲结果需要携带完整队列上下文，点击任意可见行都能从搜索结果队列播放。
private fun buildSongResultRows(songs: List<Song>): List<SearchResultLazyRow> {
    if (songs.isEmpty()) {
        return listOf(SearchResultLazyRow.Message(text = "没有找到歌曲"))
    }
    return songs.map { song: Song ->
        SearchResultLazyRow.HomeSongItem(
            song = song,
            queueSongs = songs,
        )
    }
}

// 专辑结果按两列分组，避免 UI 层重复 chunk 规则。
private fun buildAlbumResultRows(albums: List<Album>): List<SearchResultLazyRow> {
    if (albums.isEmpty()) {
        return listOf(SearchResultLazyRow.Message(text = "没有找到专辑"))
    }
    return albums.chunked(size = 2).map { rowAlbums: List<Album> ->
        SearchResultLazyRow.HomeAlbumRow(albums = rowAlbums)
    }
}

// 歌手结果保持一行一个歌手，便于 LazyColumn 复用节点。
private fun buildArtistResultRows(artists: List<Artist>): List<SearchResultLazyRow> {
    if (artists.isEmpty()) {
        return listOf(SearchResultLazyRow.Message(text = "没有找到歌手"))
    }
    return artists.map { artist: Artist -> SearchResultLazyRow.HomeArtistItem(artist = artist) }
}
