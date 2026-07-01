package com.yanhao.kmpmusic.feature.app.library

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 纯投影助手，集中维护歌曲到专辑、歌手和详情列表的派生规则。
 */
object MusicLibraryProjector {
    /**
     * 统一专辑分组规则，保证不同入口看到一致的专辑聚合结果。
     */
    fun buildAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { song: Song -> song.album.trim().lowercase() }
            .entries
            .map { entry: Map.Entry<String, List<Song>> ->
                val normalizedAlbum: String = entry.key
                val albumSongs: List<Song> = entry.value
                val firstSong: Song = albumSongs.first()
                Album(
                    id = "album:$normalizedAlbum",
                    title = firstSong.album,
                    artist = firstSong.artist,
                    songCount = albumSongs.size,
                    coverArt = firstSong.coverArt,
                    coverImageUri = firstSong.coverImageUri,
                    mood = "本地音乐",
                    year = "本地",
                )
            }
            .sortedBy { album: Album -> album.id }
    }

    /**
     * 统一歌手分组规则，避免首页、收藏和详情页各自维护一份实现。
     */
    fun buildArtists(songs: List<Song>): List<Artist> {
        return songs.groupBy { song: Song -> song.artist.trim().lowercase() }
            .entries
            .map { entry: Map.Entry<String, List<Song>> ->
                val normalizedArtist: String = entry.key
                val artistSongs: List<Song> = entry.value
                val firstSong: Song = artistSongs.first()
                Artist(
                    id = "artist:$normalizedArtist",
                    name = firstSong.artist,
                    songCount = artistSongs.size,
                    coverArt = firstSong.coverArt,
                    coverImageUri = firstSong.coverImageUri,
                    tag = "本地音乐",
                )
            }
            .sortedBy { artist: Artist -> artist.id }
    }

    /**
     * 详情页需要优先保留当前队列里的实体，再按曲库、首页预览、收藏顺序补齐缺口。
     */
    fun buildDetailSongs(
        queueSongsSnapshot: List<Song>,
        localSongs: List<Song>,
        homeLocalSongPreview: List<Song>,
        favoriteSongs: List<Song>,
    ): List<Song> {
        return (queueSongsSnapshot + localSongs + homeLocalSongPreview + favoriteSongs)
            .distinctBy { song: Song -> song.id }
    }
}
