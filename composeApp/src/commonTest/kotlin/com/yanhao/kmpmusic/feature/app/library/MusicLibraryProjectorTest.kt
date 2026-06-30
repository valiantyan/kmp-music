package com.yanhao.kmpmusic.feature.app.library

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals

class MusicLibraryProjectorTest {
    @Test
    fun albumsUseStableLowercaseTrimmedIdsAndPreserveFirstSongArtwork(): Unit {
        val songs: List<Song> = listOf(
            testSong(id = "1", title = "First", album = " River Year ", artist = "Trip", coverImageUri = "file://first.png"),
            testSong(id = "2", title = "Second", album = "river year", artist = "Trip", coverImageUri = "file://second.png"),
            testSong(id = "3", title = "Third", album = "Summer", artist = "Aki", coverImageUri = "file://third.png"),
        )

        val albums = MusicLibraryProjector.buildAlbums(songs = songs)

        assertEquals(expected = listOf("album:river year", "album:summer"), actual = albums.map { album -> album.id })
        assertEquals(expected = listOf(2, 1), actual = albums.map { album -> album.songCount })
        assertEquals(expected = "file://first.png", actual = albums.first().coverImageUri)
        assertEquals(expected = "本地音乐", actual = albums.first().mood)
        assertEquals(expected = "本地", actual = albums.first().year)
    }

    @Test
    fun artistsUseStableLowercaseTrimmedIdsAndPreserveFirstSongArtwork(): Unit {
        val songs: List<Song> = listOf(
            testSong(id = "1", title = "First", album = "One", artist = " Trip ", coverImageUri = "file://first.png"),
            testSong(id = "2", title = "Second", album = "Two", artist = "trip", coverImageUri = "file://second.png"),
            testSong(id = "3", title = "Third", album = "Three", artist = "Aki", coverImageUri = "file://third.png"),
        )

        val artists = MusicLibraryProjector.buildArtists(songs = songs)

        assertEquals(expected = listOf("artist:aki", "artist:trip"), actual = artists.map { artist -> artist.id })
        assertEquals(expected = listOf(1, 2), actual = artists.map { artist -> artist.songCount })
        assertEquals(expected = "file://third.png", actual = artists.first().coverImageUri)
        assertEquals(expected = "本地音乐", actual = artists.first().tag)
    }

    @Test
    fun detailSongsDeduplicateBySongIdInQueueLocalHomeFavoriteOrder(): Unit {
        val queue = listOf(testSong(id = "queue", title = "Queue"), testSong(id = "same", title = "Queue Same"))
        val local = listOf(testSong(id = "same", title = "Local Same"), testSong(id = "local", title = "Local"))
        val home = listOf(testSong(id = "home", title = "Home"))
        val favorites = listOf(testSong(id = "favorite", title = "Favorite"))

        val detailSongs = MusicLibraryProjector.buildDetailSongs(
            queueSongsSnapshot = queue,
            localSongs = local,
            homeLocalSongPreview = home,
            favoriteSongs = favorites,
        )

        assertEquals(expected = listOf("queue", "same", "local", "home", "favorite"), actual = detailSongs.map { song -> song.id })
        assertEquals(expected = "Queue Same", actual = detailSongs[1].title)
    }

    private fun testSong(
        id: String,
        title: String,
        album: String = "Album",
        artist: String = "Artist",
        coverImageUri: String? = null,
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = "03:12",
            coverArt = CoverArt.HeroLocalMusic,
            coverImageUri = coverImageUri,
            isLiked = false,
            lastPlayed = "",
            quality = "Lossless",
            lyric = "",
            trackNumber = 1,
            durationMs = 192_000L,
        )
    }
}
