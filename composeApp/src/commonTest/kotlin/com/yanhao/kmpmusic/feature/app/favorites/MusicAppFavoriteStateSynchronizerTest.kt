package com.yanhao.kmpmusic.feature.app.favorites

import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCaseImpl
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.library.MusicLibraryProjector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MusicAppFavoriteStateSynchronizerTest {
    @Test
    fun toggleFavoriteSyncsHomeLocalQueueFavoriteRecentAndCurrentSongSources(): Unit {
        val playbackRepository = InMemoryPlaybackRepository()
        playbackRepository.savePlaybackHistory(history = PlaybackHistory(songIds = listOf("song-1")))
        val favoritesRepository = InMemoryFavoritesRepository(initialLikedSongIds = emptySet())
        val synchronizer = FavoriteStateSynchronizer(
            toggleFavoriteUseCase = ToggleFavoriteUseCaseImpl(favoritesRepository = favoritesRepository),
            favoriteSongsResolver = { likedSongIds: Set<String>, preferredSongs: List<Song> ->
                preferredSongs.filter { song: Song -> likedSongIds.contains(element = song.id) }
                    .distinctBy { song -> song.id }
                    .map { song: Song -> song.copy(isLiked = true) }
            },
            recentSongsBuilder = { _: MusicAppUiState, songs: List<Song> ->
                val songsById: Map<String, Song> = songs
                    .distinctBy { song: Song -> song.id }
                    .associateBy { song: Song -> song.id }
                playbackRepository.getPlaybackHistory().songIds.mapNotNull { songId -> songsById[songId] }
            },
        )
        val state = testState().copy(
            homeLocalSongPreview = listOf(testSong(id = "song-1")),
            localSongs = listOf(testSong(id = "song-1")),
            queueSongsSnapshot = listOf(testSong(id = "song-1")),
            currentSongId = "song-1",
            queueSongIds = listOf("song-1"),
        )

        val nextState = synchronizer.toggleFavorite(
            state = state,
            songId = "song-1",
        )

        assertEquals(expected = setOf("song-1"), actual = nextState.likedSongIds)
        assertTrue(actual = nextState.homeLocalSongPreview.single().isLiked)
        assertTrue(actual = nextState.localSongs.single().isLiked)
        assertTrue(actual = nextState.queueSongsSnapshot.single().isLiked)
        assertTrue(actual = nextState.favoriteSongs.single().isLiked)
        assertTrue(actual = nextState.recentSongs.single().isLiked)
        assertTrue(actual = nextState.currentSong?.isLiked == true)
    }

    @Test
    fun favoriteAlbumsAndArtistsStillComeFromProjectorAfterToggle(): Unit {
        val favoritesRepository = InMemoryFavoritesRepository(initialLikedSongIds = emptySet())
        val synchronizer = FavoriteStateSynchronizer(
            toggleFavoriteUseCase = ToggleFavoriteUseCaseImpl(favoritesRepository = favoritesRepository),
            favoriteSongsResolver = { likedSongIds: Set<String>, preferredSongs: List<Song> ->
                preferredSongs.filter { song: Song -> likedSongIds.contains(element = song.id) }
                    .distinctBy { song -> song.id }
                    .map { song: Song -> song.copy(isLiked = true) }
            },
            recentSongsBuilder = { _: MusicAppUiState, _: List<Song> -> emptyList() },
        )
        val state = testState().copy(
            homeLocalSongPreview = listOf(
                testSong(
                    id = "song-1",
                    album = "Album",
                    artist = "Artist",
                ),
            ),
        )

        val nextState = synchronizer.toggleFavorite(
            state = state,
            songId = "song-1",
        )

        assertEquals(
            expected = listOf("album:album"),
            actual = MusicLibraryProjector.buildAlbums(nextState.favoriteSongs).map { album -> album.id },
        )
        assertEquals(
            expected = listOf("artist:artist"),
            actual = MusicLibraryProjector.buildArtists(nextState.favoriteSongs).map { artist -> artist.id },
        )
    }

    private fun testState(): MusicAppUiState {
        return MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        )
    }

    private fun testSong(
        id: String,
        album: String = "Album",
        artist: String = "Artist",
    ): Song {
        return Song(
            id = id,
            title = id,
            artist = artist,
            album = album,
            duration = "03:00",
            coverArt = CoverArt.CoverSummerWaltz,
            isLiked = false,
            lastPlayed = "",
            quality = "Lossless",
            lyric = "",
            trackNumber = 1,
            durationMs = 180_000L,
        )
    }
}
