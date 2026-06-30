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
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MusicAppLibraryStateSynchronizerTest {
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
        return LibraryStateSynchronizer(
            musicLibraryRepository = repository.copyWithStats(stats = stats),
            favoritesRepository = InMemoryFavoritesRepository(initialLikedSongIds = likedIds),
            playbackRepository = playbackRepository,
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

    private fun testSong(id: String, title: String, isLiked: Boolean = false): Song {
        return Song(
            id = id,
            title = title,
            artist = "Artist",
            album = "Album",
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
    private val stats: LibraryStats = LibraryStats(songCount = allSongs.size),
) : MusicLibraryRepository {
    // 记录是否命中过完整曲库读取，确保按需加载约束没有退化。
    var allSongsRead: Boolean = false
        private set

    // 为测试覆盖持久层已有歌曲数量场景，返回仅替换统计信息的新仓库副本。
    fun copyWithStats(stats: LibraryStats): FakeMusicLibraryRepository {
        return FakeMusicLibraryRepository(
            homeSongs = homeSongs,
            allSongs = allSongs,
            stats = stats,
        )
    }

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
        return homeSongs.take(n = limit)
    }

    /** 只有显式请求完整曲库时才标记读取，用于验证按需加载。 */
    override fun getAllAvailableSongs(): List<Song> {
        allSongsRead = true
        return allSongs
    }

    /** 收藏和恢复快照按 id 回查歌曲实体时复用同一批测试数据。 */
    override fun getAvailableSongsByIds(songIds: List<String>): List<Song> {
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
