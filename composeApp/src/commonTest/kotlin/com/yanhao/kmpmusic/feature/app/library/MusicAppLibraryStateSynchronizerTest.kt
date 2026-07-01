package com.yanhao.kmpmusic.feature.app.library

import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MusicAppLibraryStateSynchronizerTest {
    @Test
    fun controllerColdStartUsesHomePreviewWithoutFullLocalSongs(): Unit {
        val repository = FakeMusicLibraryRepository(
            homeSongs = (1..8).map { index: Int -> testSong(id = "home-$index", title = "Home $index") },
            allSongs = (1..8).map { index: Int -> testSong(id = "all-$index", title = "All $index") },
            stats = LibraryStats(songCount = 8),
        )

        val controller = createController(repository = repository)

        assertEquals(expected = 1, actual = repository.homePreviewReads)
        assertEquals(expected = 0, actual = repository.allSongsReadCount)
        assertEquals(expected = 6, actual = controller.uiState.homeLocalSongPreview.size)
        assertTrue(actual = controller.uiState.localSongs.isEmpty())
    }

    @Test
    fun controllerColdStartWithPersistedSongsBuildsDoneStateWithoutFullLibraryLoad(): Unit {
        val repository = FakeMusicLibraryRepository(
            homeSongs = (1..8).map { index: Int -> testSong(id = "home-$index", title = "Home $index") },
            allSongs = (1..8).map { index: Int -> testSong(id = "all-$index", title = "All $index") },
            stats = LibraryStats(songCount = 8),
        )

        val controller = createController(repository = repository)

        assertIs<LocalMusicScanState.Done>(value = controller.uiState.scanState)
        assertEquals(expected = 0, actual = repository.allSongsReadCount)
        assertEquals(expected = 6, actual = controller.uiState.homeLocalSongPreview.size)
        assertTrue(actual = controller.uiState.localSongs.isEmpty())
    }

    @Test
    fun controllerOpenLocalMusicLoadsFullSongsOnDemand(): Unit {
        val repository = FakeMusicLibraryRepository(
            homeSongs = (1..8).map { index: Int -> testSong(id = "home-$index", title = "Home $index") },
            allSongs = (1..8).map { index: Int -> testSong(id = "all-$index", title = "All $index") },
            stats = LibraryStats(songCount = 8),
        )
        val controller = createController(repository = repository)

        controller.openLocalMusic(section = LocalMusicSection.Songs)

        assertEquals(expected = 1, actual = repository.allSongsReadCount)
        assertEquals(expected = 8, actual = controller.uiState.localSongs.size)
    }

    @Test
    fun controllerPreviewSongsCanOpenDetailsAfterOnDemandLibraryLoad(): Unit {
        val songs: List<Song> = (1..8).map { index: Int ->
            testSong(
                id = "song-$index",
                title = "Song $index",
                album = "Album",
                artist = "Artist",
            )
        }
        val repository = FakeMusicLibraryRepository(
            homeSongs = songs,
            allSongs = songs,
            stats = LibraryStats(songCount = songs.size),
        )
        val controller = createController(repository = repository)
        val previewSong: Song = controller.uiState.homeLocalSongPreview.first()

        controller.openAlbumFromSong(song = previewSong)
        assertEquals(expected = "album:album", actual = controller.uiState.selectedAlbum?.id)
        controller.openArtistFromSong(song = previewSong)
        assertEquals(expected = "artist:artist", actual = controller.uiState.selectedArtist?.id)
        assertEquals(expected = 1, actual = repository.allSongsReadCount)
    }

    @Test
    fun buildInitialScanStateReflectsPersistedLibraryWithoutLoadingSongs(): Unit {
        val synchronizer: LibraryStateSynchronizer = createSynchronizer(stats = LibraryStats(songCount = 5))

        val scanState: LocalMusicScanState = synchronizer.buildInitialScanState(
            stats = LibraryStats(songCount = 5),
        )

        assertIs<LocalMusicScanState.Done>(value = scanState)
        assertEquals(expected = 5, actual = scanState.summary.addedCount)
    }

    @Test
    fun permissionPermanentlyDeniedRequiresConfirmationBeforeScanningAgain(): Unit {
        val synchronizer: LibraryStateSynchronizer = createSynchronizer()
        val state: MusicAppUiState = testState().copy(
            scanState = LocalMusicScanState.Error(
                error = LocalMusicScanError(
                    type = LocalMusicScanErrorType.PermissionPermanentlyDenied,
                    message = "permission denied",
                ),
            ),
        )

        assertTrue(
            actual = synchronizer.shouldConfirmPermissionSettingsBeforeScan(state = state),
        )
    }

    @Test
    fun syncLibrarySnapshotRefreshesFullLibraryWhenLocalSongsAlreadyLoaded(): Unit {
        val repository: FakeMusicLibraryRepository = FakeMusicLibraryRepository(
            homeSongs = listOf(testSong(id = "home", title = "Home")),
            allSongs = listOf(
                testSong(id = "liked", title = "Liked", isLiked = false),
                testSong(id = "local", title = "Local"),
            ),
            stats = LibraryStats(songCount = 2),
        )
        val synchronizer: LibraryStateSynchronizer = createSynchronizer(
            repository = repository,
            likedIds = setOf("liked"),
        )
        val state: MusicAppUiState = testState().copy(
            localSongs = listOf(testSong(id = "old", title = "Old")),
        )

        val nextState: MusicAppUiState = synchronizer.syncLibrarySnapshot(
            state = state,
            snapshot = LibrarySnapshot(
                songs = repository.getAllAvailableSongs(),
                albums = MusicLibraryProjector.buildAlbums(songs = repository.getAllAvailableSongs()),
                artists = MusicLibraryProjector.buildArtists(songs = repository.getAllAvailableSongs()),
                stats = LibraryStats(songCount = 2),
                sources = emptyList(),
                scanState = LocalMusicScanState.Done(
                    summary = LocalMusicLastScanSummary(
                        addedCount = 2,
                        updatedCount = 0,
                        removedCount = 0,
                        problemCount = 0,
                        completedAt = 0L,
                    ),
                ),
                lastScanSummary = null,
                problems = emptyList(),
            ),
        )

        assertEquals(
            expected = listOf("liked", "local"),
            actual = nextState.localSongs.map { song: Song -> song.id },
        )
        assertEquals(
            expected = listOf("liked"),
            actual = nextState.favoriteSongs.map { song: Song -> song.id },
        )
        assertTrue(actual = nextState.localSongs.first().isLiked)
    }

    @Test
    fun syncLibrarySnapshotUsesRepositoryStatsAndPreviewAsSourceOfTruth(): Unit {
        val repository: FakeMusicLibraryRepository = FakeMusicLibraryRepository(
            homeSongs = listOf(testSong(id = "preview", title = "Preview")),
            allSongs = listOf(testSong(id = "full", title = "Full")),
            stats = LibraryStats(songCount = 8, albumCount = 4, artistCount = 4),
        )
        val synchronizer: LibraryStateSynchronizer = createSynchronizer(
            repository = repository,
            stats = repository.getLibraryStats(),
        )

        val nextState: MusicAppUiState = synchronizer.syncLibrarySnapshot(
            state = testState(),
            snapshot = LibrarySnapshot(
                songs = emptyList(),
                albums = emptyList(),
                artists = emptyList(),
                stats = LibraryStats(songCount = 1, albumCount = 1, artistCount = 1),
                sources = emptyList(),
                scanState = LocalMusicScanState.Done(
                    summary = LocalMusicLastScanSummary(
                        addedCount = 1,
                        updatedCount = 0,
                        removedCount = 0,
                        problemCount = 0,
                        completedAt = 0L,
                    ),
                ),
                lastScanSummary = null,
                problems = emptyList(),
            ),
        )

        assertEquals(expected = listOf("preview"), actual = nextState.homeLocalSongPreview.map { song: Song -> song.id })
        assertEquals(
            expected = LibraryStats(songCount = 8, albumCount = 4, artistCount = 4),
            actual = nextState.libraryStats,
        )
    }

    @Test
    fun loadLocalMusicLibraryBuildsRecentSongsFromPlaybackHistory(): Unit {
        val playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository()
        playbackRepository.savePlaybackHistory(
            history = PlaybackHistory(songIds = listOf("song-2")),
        )
        val repository: FakeMusicLibraryRepository = FakeMusicLibraryRepository(
            allSongs = listOf(
                testSong(id = "song-1", title = "One"),
                testSong(id = "song-2", title = "Two"),
            ),
        )
        val synchronizer: LibraryStateSynchronizer = createSynchronizer(
            repository = repository,
            playbackRepository = playbackRepository,
        )

        val nextState: MusicAppUiState = synchronizer.loadLocalMusicLibrary(state = testState())

        assertEquals(
            expected = listOf("song-1", "song-2"),
            actual = nextState.localSongs.map { song: Song -> song.id },
        )
        assertEquals(
            expected = listOf("song-2"),
            actual = nextState.recentSongs.map { song: Song -> song.id },
        )
    }

    @Test
    fun loadLocalMusicLibraryDoesNothingWhenSongsAlreadyLoaded(): Unit {
        val repository: FakeMusicLibraryRepository = FakeMusicLibraryRepository(
            allSongs = listOf(testSong(id = "repo", title = "Repo")),
        )
        val synchronizer: LibraryStateSynchronizer = createSynchronizer(
            repository = repository,
            stats = repository.getLibraryStats(),
        )
        val state: MusicAppUiState = testState().copy(
            localSongs = listOf(testSong(id = "existing", title = "Existing")),
        )

        val nextState: MusicAppUiState = synchronizer.loadLocalMusicLibrary(state = state)

        assertEquals(
            expected = listOf("existing"),
            actual = nextState.localSongs.map { song: Song -> song.id },
        )
        assertFalse(actual = repository.allSongsRead)
    }

    private fun createSynchronizer(
        repository: FakeMusicLibraryRepository = FakeMusicLibraryRepository(),
        playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository(),
        stats: LibraryStats = LibraryStats(),
        likedIds: Set<String> = emptySet(),
    ): LibraryStateSynchronizer {
        repository.stats = stats
        return LibraryStateSynchronizer(
            musicLibraryRepository = repository,
            favoritesRepository = InMemoryFavoritesRepository(initialLikedSongIds = likedIds),
            playbackRepository = playbackRepository,
        )
    }

    private fun createController(repository: FakeMusicLibraryRepository): MusicAppController {
        return MusicAppController(
            musicLibraryRepository = repository,
            controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
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
        title: String,
        isLiked: Boolean = false,
        album: String = "Album",
        artist: String = "Artist",
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = "03:00",
            coverArt = CoverArt.HeroLocalMusic,
            isLiked = isLiked,
            lastPlayed = "",
            quality = "Lossless",
            lyric = "",
            trackNumber = 1,
            durationMs = 180_000L,
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            localUri = "content://$id",
        )
    }
}

private class FakeMusicLibraryRepository(
    private val homeSongs: List<Song> = emptyList(),
    private val allSongs: List<Song> = emptyList(),
    var stats: LibraryStats = LibraryStats(songCount = allSongs.size),
) : MusicLibraryRepository {
    // 记录是否命中过完整曲库读取，确保按需加载约束没有退化。
    var allSongsRead: Boolean = false
        private set
    var allSongsReadCount: Int = 0
        private set
    var homePreviewReads: Int = 0
        private set
    var songsByIdsReads: Int = 0
        private set

    /** 提供与真实仓库一致的快照结构，便于同步器测试直接消费。 */
    override fun getSnapshot(): LibrarySnapshot {
        return LibrarySnapshot(
            songs = allSongs,
            albums = MusicLibraryProjector.buildAlbums(songs = allSongs),
            artists = MusicLibraryProjector.buildArtists(songs = allSongs),
            stats = stats,
            sources = emptyList(),
            scanState = LocalMusicScanState.Idle,
            lastScanSummary = null,
            problems = emptyList(),
        )
    }

    /** 首页预览只暴露受限数量，模拟真实冷启动策略。 */
    override fun getHomePreview(limit: Int): List<Song> {
        homePreviewReads += 1
        return homeSongs.take(n = limit)
    }

    /** 只有显式请求完整曲库时才标记读取，用于验证按需加载。 */
    override fun getAllAvailableSongs(): List<Song> {
        allSongsRead = true
        allSongsReadCount += 1
        return allSongs
    }

    /** 收藏和恢复快照按 id 回查歌曲实体时复用同一批测试数据。 */
    override fun getAvailableSongsByIds(songIds: List<String>): List<Song> {
        songsByIdsReads += 1
        return allSongs.filter { song: Song -> songIds.contains(element = song.id) }
    }

    /** 暴露预置统计信息，覆盖持久层已有曲库的冷启动场景。 */
    override fun getLibraryStats(): LibraryStats {
        return stats
    }

    /** 扫描结果在本任务测试里只需要回填来源与问题列表。 */
    override fun applyScanResult(
        request: com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest,
        scanResult: com.yanhao.kmpmusic.domain.model.LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot {
        return LibrarySnapshot(
            songs = allSongs,
            albums = MusicLibraryProjector.buildAlbums(songs = allSongs),
            artists = MusicLibraryProjector.buildArtists(songs = allSongs),
            stats = stats,
            sources = scanResult.sourceSummaries,
            scanState = LocalMusicScanState.Idle,
            lastScanSummary = null,
            problems = scanResult.failed,
        )
    }
}
