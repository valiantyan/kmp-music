package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository

/**
 * 搜索结果聚合模型。
 */
data class SearchResult(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
)

/**
 * 搜索本地音乐接口。
 */
interface SearchMusicUseCase {
    /**
     * 根据关键词和范围搜索本地音乐。
     */
    operator fun invoke(query: String, scope: SearchScope): SearchResult
}

/**
 * 搜索本地音乐实现。
 */
class SearchMusicUseCaseImpl(
    private val musicLibraryRepository: MusicLibraryRepository,
) : SearchMusicUseCase {
    /** 执行本地内存搜索，并始终基于完整可用曲库聚合搜索结果。 */
    override operator fun invoke(query: String, scope: SearchScope): SearchResult {
        val allSongs: List<Song> = musicLibraryRepository.getAllAvailableSongs()
        return buildSearchResult(
            query = query,
            scope = scope,
            allSongs = allSongs,
        )
    }
}

/** 基于完整歌曲列表生成搜索结果，供控制器复用已加载曲库时避免重复读取仓库。 */
internal fun buildSearchResult(
    query: String,
    scope: SearchScope,
    allSongs: List<Song>,
): SearchResult {
    val normalizedQuery: String = query.trim().lowercase()
    val allAlbums: List<Album> = buildAlbums(songs = allSongs)
    val allArtists: List<Artist> = buildArtists(songs = allSongs)
    val songs: List<Song> = if (scope == SearchScope.All || scope == SearchScope.Songs) {
        allSongs.filter { song ->
            matchesSong(song = song, normalizedQuery = normalizedQuery)
        }
    } else {
        emptyList()
    }
    val albums: List<Album> = if (scope == SearchScope.All || scope == SearchScope.Albums) {
        allAlbums.filter { album ->
            matchesAlbum(album = album, normalizedQuery = normalizedQuery)
        }
    } else {
        emptyList()
    }
    val artists: List<Artist> = if (scope == SearchScope.All || scope == SearchScope.Artists) {
        allArtists.filter { artist ->
            matchesArtist(artist = artist, normalizedQuery = normalizedQuery)
        }
    } else {
        emptyList()
    }
    return SearchResult(
        songs = songs,
        albums = albums,
        artists = artists,
    )
}

// 搜索需要和控制器详情页使用同一套聚合规则，避免同歌库下结果口径不一致。
private fun buildAlbums(songs: List<Song>): List<Album> {
    return songs.groupBy { song -> song.album.trim().lowercase() }.values.map { albumSongs ->
        val firstSong: Song = albumSongs.first()
        Album(
            id = "album:${firstSong.album.trim().lowercase()}",
            title = firstSong.album,
            artist = firstSong.artist,
            songCount = albumSongs.size,
            coverArt = firstSong.coverArt,
            coverImageUri = firstSong.coverImageUri,
            mood = "本地音乐",
            year = "本地",
        )
    }.sortedBy { album -> album.title.lowercase() }
}

// 歌手搜索同样从完整曲库重建聚合，避免依赖仓库里的缓存列表时机。
private fun buildArtists(songs: List<Song>): List<Artist> {
    return songs.groupBy { song -> song.artist.trim().lowercase() }.values.map { artistSongs ->
        val firstSong: Song = artistSongs.first()
        Artist(
            id = "artist:${firstSong.artist.trim().lowercase()}",
            name = firstSong.artist,
            songCount = artistSongs.size,
            coverArt = firstSong.coverArt,
            coverImageUri = firstSong.coverImageUri,
            tag = "本地音乐",
        )
    }.sortedBy { artist -> artist.name.lowercase() }
}

// 空查询代表浏览全部本地内容。
private fun matchesSong(song: Song, normalizedQuery: String): Boolean {
    if (normalizedQuery.isBlank()) return true
    return "${song.title} ${song.artist} ${song.album}".lowercase().contains(normalizedQuery)
}

// 专辑搜索覆盖标题和歌手名。
private fun matchesAlbum(album: Album, normalizedQuery: String): Boolean {
    if (normalizedQuery.isBlank()) return true
    return "${album.title} ${album.artist}".lowercase().contains(normalizedQuery)
}

// 歌手搜索覆盖姓名和标签。
private fun matchesArtist(artist: Artist, normalizedQuery: String): Boolean {
    if (normalizedQuery.isBlank()) return true
    return "${artist.name} ${artist.tag}".lowercase().contains(normalizedQuery)
}
