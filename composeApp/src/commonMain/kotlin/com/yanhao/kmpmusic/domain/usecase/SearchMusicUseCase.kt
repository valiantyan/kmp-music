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
    /** 执行本地内存搜索，保证歌曲、专辑、歌手过滤规则集中。 */
    override operator fun invoke(query: String, scope: SearchScope): SearchResult {
        val normalizedQuery: String = query.trim().lowercase()
        val songs: List<Song> = if (scope == SearchScope.All || scope == SearchScope.Songs) {
            musicLibraryRepository.getSongs().filter { song ->
                matchesSong(song = song, normalizedQuery = normalizedQuery)
            }
        } else {
            emptyList()
        }
        val albums: List<Album> = if (scope == SearchScope.All || scope == SearchScope.Albums) {
            musicLibraryRepository.getAlbums().filter { album ->
                matchesAlbum(album = album, normalizedQuery = normalizedQuery)
            }
        } else {
            emptyList()
        }
        val artists: List<Artist> = if (scope == SearchScope.All || scope == SearchScope.Artists) {
            musicLibraryRepository.getArtists().filter { artist ->
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
}
