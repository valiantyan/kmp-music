package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicScanCoverage
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.FavoriteSongDao
import com.yanhao.kmpmusic.domain.persistence.FavoriteSongEntity
import com.yanhao.kmpmusic.domain.persistence.LocalSongDao
import com.yanhao.kmpmusic.domain.persistence.LocalSongEntity
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentMusicLibraryRepositoryTest {
    @Test
    fun homePreviewReadsOnlySixSongsSortedByModifiedAt(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val favoriteSongDao: FakeFavoriteSongDao = FakeFavoriteSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = favoriteSongDao,
                )
            localSongDao.upsertSongs(
                songs =
                    (1..8).map { index: Int ->
                        entity(
                            id = "androidMediaStore:$index",
                            title = "Song $index",
                            modifiedAt = index.toLong(),
                        )
                    },
            )

            val preview = repository.getHomePreview(limit = 6)

            assertEquals(expected = 6, actual = preview.size)
            assertEquals(expected = "Song 8", actual = preview.first().title)
            assertEquals(expected = "Song 3", actual = preview.last().title)
        }

    @Test
    fun androidCompleteMediaStoreCoverageMarksMissingAndroidSongUnavailableAfterPositiveOnlyKeepsIt(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = FakeFavoriteSongDao(),
                )
            localSongDao.upsertSongs(
                songs =
                    listOf(
                        entity(
                            id = "androidMediaStore:old",
                            sourceId = "old",
                            title = "Old",
                            modifiedAt = 1L,
                        ),
                        entity(
                            id = "desktopFolder:keep",
                            sourceKind = "desktopFolder",
                            sourceId = "keep",
                            title = "Desktop",
                            modifiedAt = 2L,
                        ),
                    ),
            )

            repository.applyScanResult(
                request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
                scanResult =
                    LocalMusicScanResult(
                        discovered =
                            listOf(
                                metadata(
                                    sourceId = "new",
                                    title = "New",
                                    modifiedAt = 3L,
                                ),
                            ),
                        completedAt = 98L,
                    ),
                likedSongIds = emptySet(),
            )

            assertTrue(actual = localSongDao.row("androidMediaStore:old")!!.isAvailable)
            assertTrue(actual = localSongDao.row("androidMediaStore:new")!!.isAvailable)

            repository.applyScanResult(
                request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
                scanResult =
                    LocalMusicScanResult(
                        discovered =
                            listOf(
                                metadata(
                                    sourceId = "new",
                                    title = "New",
                                    modifiedAt = 3L,
                                ),
                            ),
                        completedCoverage =
                            listOf(
                                LocalMusicScanCoverage.SourceKind(sourceKind = LocalMusicSourceKind.AndroidMediaStore),
                            ),
                        completedAt = 99L,
                    ),
                likedSongIds = emptySet(),
            )

            assertFalse(actual = localSongDao.row("androidMediaStore:old")!!.isAvailable)
            assertTrue(actual = localSongDao.row("androidMediaStore:new")!!.isAvailable)
            assertTrue(actual = localSongDao.row("desktopFolder:keep")!!.isAvailable)
        }

    @Test
    fun positiveOnlyScanAddsNewSameSourceSongWithoutRemovingExistingSong(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val favoriteSongDao: FakeFavoriteSongDao = FakeFavoriteSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = favoriteSongDao,
                )
            favoriteSongDao.saveFavorite(
                entity =
                    FavoriteSongEntity(
                        songId = "androidMediaStore:existing",
                        updatedAt = 1L,
                    ),
            )
            localSongDao.upsertSongs(
                songs =
                    listOf(
                        entity(
                            id = "androidMediaStore:existing",
                            sourceId = "existing",
                            title = "Existing",
                            modifiedAt = 1L,
                        ),
                    ),
            )

            repository.applyScanResult(
                request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
                scanResult =
                    LocalMusicScanResult(
                        discovered =
                            listOf(
                                metadata(
                                    sourceId = "new",
                                    title = "New",
                                    modifiedAt = 2L,
                                ),
                            ),
                        removedSourceKeys = emptySet(),
                        sourceSummaries = emptyList(),
                        completedAt = 10L,
                    ),
                likedSongIds = favoriteSongDao.getFavoriteSongIds().toSet(),
            )

            val availableSongs: List<Song> = repository.getAllAvailableSongs()
            val availableSongIds: Set<String> =
                availableSongs
                    .map { song: Song -> song.id }
                    .toSet()
            assertTrue(actual = localSongDao.row("androidMediaStore:existing")!!.isAvailable)
            assertTrue(actual = localSongDao.row("androidMediaStore:new")!!.isAvailable)
            assertTrue(actual = availableSongs.first { song: Song -> song.id == "androidMediaStore:existing" }.isLiked)
            assertEquals(
                expected =
                    setOf(
                        "androidMediaStore:existing",
                        "androidMediaStore:new",
                    ),
                actual = availableSongIds,
            )
        }

    @Test
    fun desktopFolderAccumulationScanKeepsFolderASongWhenScanningFolderB(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = FakeFavoriteSongDao(),
                )
            val folderAPath: String = "/Users/listener/Music/A"
            val folderBPath: String = "/Users/listener/Music/B"
            val folderASourceId: String = "$folderAPath/folder-a.mp3"
            val missingFolderBSourceId: String = "$folderBPath/folder-b-old.mp3"
            val folderBSourceId: String = "$folderBPath/folder-b-new.mp3"
            val folderASongId: String = "desktopFolder:$folderASourceId"
            val missingFolderBSongId: String = "desktopFolder:$missingFolderBSourceId"
            val folderBSongId: String = "desktopFolder:$folderBSourceId"
            localSongDao.upsertSongs(
                songs =
                    listOf(
                        entity(
                            id = folderASongId,
                            sourceKind = LocalMusicSourceKind.DesktopFolder.value,
                            sourceId = folderASourceId,
                            concreteSourceId = folderAPath,
                            title = "Folder A Song",
                            modifiedAt = 1L,
                        ),
                        entity(
                            id = missingFolderBSongId,
                            sourceKind = LocalMusicSourceKind.DesktopFolder.value,
                            sourceId = missingFolderBSourceId,
                            concreteSourceId = folderBPath,
                            title = "Folder B Missing Song",
                            modifiedAt = 2L,
                        ),
                    ),
            )

            repository.applyScanResult(
                request = LocalMusicScanRequest.Source(LocalMusicSourceKind.DesktopFolder),
                scanResult =
                    LocalMusicScanResult(
                        discovered =
                            listOf(
                                metadata(
                                    sourceKind = LocalMusicSourceKind.DesktopFolder,
                                    sourceId = folderBSourceId,
                                    concreteSourceId = folderBPath,
                                    title = "Folder B New Song",
                                    modifiedAt = 3L,
                                ),
                            ),
                        completedCoverage =
                            listOf(
                                LocalMusicScanCoverage.ConcreteSource(
                                    sourceKind = LocalMusicSourceKind.DesktopFolder,
                                    sourceId = folderBPath,
                                ),
                            ),
                        completedAt = 20L,
                    ),
                likedSongIds = emptySet(),
            )

            val availableSongIds: Set<String> =
                repository
                    .getAllAvailableSongs()
                    .map { song: Song -> song.id }
                    .toSet()
            assertTrue(actual = localSongDao.row(folderASongId)!!.isAvailable)
            assertFalse(actual = localSongDao.row(missingFolderBSongId)!!.isAvailable)
            assertTrue(actual = localSongDao.row(folderBSongId)!!.isAvailable)
            assertEquals(
                expected = setOf(folderASongId, folderBSongId),
                actual = availableSongIds,
            )
        }

    @Test
    fun desktopSourceKindCoverageDoesNotDeleteWholeDesktopLibrary(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = FakeFavoriteSongDao(),
                )
            localSongDao.upsertSongs(
                songs =
                    listOf(
                        entity(
                            id = "desktopFolder:/Users/listener/Music/A/old.mp3",
                            sourceKind = LocalMusicSourceKind.DesktopFolder.value,
                            sourceId = "/Users/listener/Music/A/old.mp3",
                            concreteSourceId = "/Users/listener/Music/A",
                            title = "Folder A Old Song",
                            modifiedAt = 1L,
                        ),
                    ),
            )

            repository.applyScanResult(
                request = LocalMusicScanRequest.Source(LocalMusicSourceKind.DesktopFolder),
                scanResult =
                    LocalMusicScanResult(
                        discovered = emptyList(),
                        completedCoverage =
                            listOf(
                                LocalMusicScanCoverage.SourceKind(sourceKind = LocalMusicSourceKind.DesktopFolder),
                            ),
                        completedAt = 30L,
                    ),
                likedSongIds = emptySet(),
            )

            assertTrue(actual = localSongDao.row("desktopFolder:/Users/listener/Music/A/old.mp3")!!.isAvailable)
            assertEquals(
                expected = setOf("desktopFolder:/Users/listener/Music/A/old.mp3"),
                actual = repository.getAllAvailableSongs().map { song: Song -> song.id }.toSet(),
            )
        }

    @Test
    fun iosImportAddsNewFileWithoutReplacingExistingImportedFile(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = FakeFavoriteSongDao(),
                )
            val existingSourceId: String = "bookmark://ios/imported/existing.m4a"
            val newSourceId: String = "bookmark://ios/imported/new.m4a"
            val existingSongId: String = "iosImportedFile:$existingSourceId"
            val newSongId: String = "iosImportedFile:$newSourceId"
            localSongDao.upsertSongs(
                songs =
                    listOf(
                        entity(
                            id = existingSongId,
                            sourceKind = LocalMusicSourceKind.IosImportedFile.value,
                            sourceId = existingSourceId,
                            concreteSourceId = existingSourceId,
                            title = "Existing iOS Import",
                            modifiedAt = 1L,
                        ),
                    ),
            )

            repository.applyScanResult(
                request = LocalMusicScanRequest.Source(LocalMusicSourceKind.IosImportedFile),
                scanResult =
                    LocalMusicScanResult(
                        discovered =
                            listOf(
                                metadata(
                                    sourceKind = LocalMusicSourceKind.IosImportedFile,
                                    sourceId = newSourceId,
                                    concreteSourceId = newSourceId,
                                    title = "New iOS Import",
                                    modifiedAt = 2L,
                                ),
                            ),
                        completedCoverage = listOf(LocalMusicScanCoverage.PositiveOnly),
                        completedAt = 40L,
                    ),
                likedSongIds = emptySet(),
            )

            val availableSongIds: Set<String> =
                repository
                    .getAllAvailableSongs()
                    .map { song: Song -> song.id }
                    .toSet()
            assertTrue(actual = localSongDao.row(existingSongId)!!.isAvailable)
            assertTrue(actual = localSongDao.row(newSongId)!!.isAvailable)
            assertEquals(
                expected = setOf(existingSongId, newSongId),
                actual = availableSongIds,
            )
        }

    @Test
    fun favoritesAreDerivedAndSurviveUnavailableSongs(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val favoriteSongDao: FakeFavoriteSongDao = FakeFavoriteSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = favoriteSongDao,
                )
            favoriteSongDao.saveFavorite(
                entity =
                    FavoriteSongEntity(
                        songId = "androidMediaStore:liked",
                        updatedAt = 1L,
                    ),
            )
            localSongDao.upsertSongs(
                songs =
                    listOf(
                        entity(
                            id = "androidMediaStore:liked",
                            sourceId = "liked",
                            title = "Liked",
                            modifiedAt = 1L,
                        ),
                    ),
            )

            assertTrue(actual = repository.getAllAvailableSongs().single().isLiked)

            repository.applyScanResult(
                request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
                scanResult =
                    LocalMusicScanResult(
                        discovered = emptyList(),
                        completedCoverage =
                            listOf(
                                LocalMusicScanCoverage.SourceKind(sourceKind = LocalMusicSourceKind.AndroidMediaStore),
                            ),
                        completedAt = 2L,
                    ),
                likedSongIds = favoriteSongDao.getFavoriteSongIds().toSet(),
            )

            assertTrue(actual = repository.getAllAvailableSongs().isEmpty())

            repository.applyScanResult(
                request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
                scanResult =
                    LocalMusicScanResult(
                        discovered =
                            listOf(
                                metadata(
                                    sourceId = "liked",
                                    title = "Liked Again",
                                    modifiedAt = 3L,
                                ),
                            ),
                        completedCoverage =
                            listOf(
                                LocalMusicScanCoverage.SourceKind(sourceKind = LocalMusicSourceKind.AndroidMediaStore),
                            ),
                        completedAt = 3L,
                    ),
                likedSongIds = favoriteSongDao.getFavoriteSongIds().toSet(),
            )

            assertTrue(actual = repository.getAllAvailableSongs().single().isLiked)
        }

    @Test
    fun applyScanResultPreservesLatestSourceSummariesInReturnedAndCurrentSnapshot(): Unit =
        runBlocking {
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = FakeLocalSongDao(),
                    favoriteSongDao = FakeFavoriteSongDao(),
                )
            val sourceSummary =
                LocalMusicSourceSummary(
                    sourceKind = LocalMusicSourceKind.AndroidMediaStore,
                    displayName = "Android 媒体库",
                    songCount = 1,
                    problemCount = 0,
                    lastScannedAt = 123L,
                )

            val snapshot =
                repository.applyScanResult(
                    request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
                    scanResult =
                        LocalMusicScanResult(
                            discovered =
                                listOf(
                                    metadata(
                                        sourceId = "summary",
                                        title = "Summary Song",
                                        modifiedAt = 123L,
                                    ),
                                ),
                            sourceSummaries = listOf(sourceSummary),
                            completedAt = 123L,
                        ),
                    likedSongIds = emptySet(),
                )

            assertEquals(expected = listOf(sourceSummary), actual = snapshot.sources)
            assertEquals(expected = listOf(sourceSummary), actual = repository.getSnapshot().sources)
            assertSame(expected = sourceSummary, actual = snapshot.sources.single())
        }

    @Test
    fun multiSourceScanCountsAddedAndUpdatedByEntitySourceOnly(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = FakeFavoriteSongDao(),
                )
            localSongDao.upsertSongs(
                songs =
                    listOf(
                        entity(
                            id = "androidMediaStore:existing",
                            sourceId = "existing",
                            title = "Existing Android",
                            modifiedAt = 1L,
                        ),
                    ),
            )

            val snapshot =
                repository.applyScanResult(
                    request = LocalMusicScanRequest.Refresh,
                    scanResult =
                        LocalMusicScanResult(
                            discovered =
                                listOf(
                                    metadata(
                                        sourceKind = LocalMusicSourceKind.AndroidMediaStore,
                                        sourceId = "existing",
                                        title = "Existing Android Updated",
                                        modifiedAt = 2L,
                                    ),
                                    metadata(
                                        sourceKind = LocalMusicSourceKind.DesktopFolder,
                                        sourceId = "fresh",
                                        title = "Fresh Desktop",
                                        modifiedAt = 3L,
                                    ),
                                ),
                            completedAt = 99L,
                        ),
                    likedSongIds = emptySet(),
                )

            val summary: LocalMusicScanState.Done = snapshot.scanState as LocalMusicScanState.Done
            assertEquals(expected = 1, actual = summary.summary.addedCount)
            assertEquals(expected = 1, actual = summary.summary.updatedCount)
            assertEquals(expected = 0, actual = summary.summary.removedCount)
        }

    @Test
    fun applyScanResultPersistsScannedCoverImageUri(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = FakeFavoriteSongDao(),
                )
            val coverImageUri: String = "file:///tmp/persisted-cover.art"

            val snapshot =
                repository.applyScanResult(
                    request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
                    scanResult =
                        LocalMusicScanResult(
                            discovered =
                                listOf(
                                    metadata(
                                        sourceId = "covered",
                                        title = "Covered",
                                        modifiedAt = 4L,
                                        coverImageUri = coverImageUri,
                                    ),
                                ),
                            completedAt = 100L,
                        ),
                    likedSongIds = emptySet(),
                )

            assertEquals(expected = coverImageUri, actual = localSongDao.row("androidMediaStore:covered")?.coverImageUri)
            assertEquals(expected = coverImageUri, actual = snapshot.songs.single().coverImageUri)
            assertEquals(expected = coverImageUri, actual = snapshot.albums.single().coverImageUri)
            assertEquals(expected = coverImageUri, actual = snapshot.artists.single().coverImageUri)
        }

    @Test
    fun existingScannedSongWithoutCoverUriUsesLocalMusicPlaceholder(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = FakeFavoriteSongDao(),
                )
            localSongDao.upsertSongs(
                songs =
                    listOf(
                        entity(
                            id = "desktopFolder:legacy-cover",
                            sourceKind = LocalMusicSourceKind.DesktopFolder.value,
                            sourceId = "/Music/legacy-cover.mp3",
                            title = "旧占位封面的歌",
                            modifiedAt = 1L,
                            coverArt = CoverArt.CoverSeaDream,
                        ),
                    ),
            )

            val snapshot = repository.getSnapshot()

            assertEquals(expected = CoverArt.HeroLocalMusic, actual = snapshot.songs.single().coverArt)
            assertEquals(expected = CoverArt.HeroLocalMusic, actual = snapshot.albums.single().coverArt)
            assertEquals(expected = CoverArt.HeroLocalMusic, actual = snapshot.artists.single().coverArt)
        }

    @Test
    fun positiveOnlyRefreshWithoutDiscoveredSongsPreservesExistingSources(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = FakeFavoriteSongDao(),
                )
            localSongDao.upsertSongs(
                songs =
                    listOf(
                        entity(
                            id = "androidMediaStore:gone",
                            sourceId = "gone",
                            title = "Gone Android",
                            modifiedAt = 1L,
                        ),
                        entity(
                            id = "desktopFolder:gone",
                            sourceKind = "desktopFolder",
                            sourceId = "gone",
                            title = "Gone Desktop",
                            modifiedAt = 2L,
                        ),
                    ),
            )

            val snapshot =
                repository.applyScanResult(
                    request = LocalMusicScanRequest.Refresh,
                    scanResult =
                        LocalMusicScanResult(
                            discovered = emptyList(),
                            sourceSummaries = emptyList(),
                            completedAt = 88L,
                        ),
                    likedSongIds = emptySet(),
                )

            val summary: LocalMusicScanState.Done = snapshot.scanState as LocalMusicScanState.Done
            assertTrue(actual = localSongDao.row("androidMediaStore:gone")!!.isAvailable)
            assertTrue(actual = localSongDao.row("desktopFolder:gone")!!.isAvailable)
            assertEquals(expected = 2, actual = repository.getAllAvailableSongs().size)
            assertEquals(expected = 0, actual = summary.summary.removedCount)
            assertEquals(expected = 0, actual = summary.summary.addedCount)
            assertEquals(expected = 0, actual = summary.summary.updatedCount)
        }

    @Test
    fun failedScanHasNoDeletionAuthorityAndKeepsUnprocessedExistingSong(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = FakeFavoriteSongDao(),
                )
            localSongDao.upsertSongs(
                songs =
                    listOf(
                        entity(
                            id = "androidMediaStore:old-safe-song",
                            sourceId = "old-safe-song",
                            title = "Old Safe Song",
                            modifiedAt = 1L,
                        ),
                    ),
            )

            val snapshot =
                repository.applyScanResult(
                    request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
                    scanResult =
                        LocalMusicScanResult(
                            discovered =
                                listOf(
                                    metadata(
                                        sourceId = "verified-new-song",
                                        title = "Verified New Song",
                                        modifiedAt = 2L,
                                    ),
                                ),
                            failed = listOf(failedProblem(sourceId = "unreadable-song")),
                            completedCoverage =
                                listOf(
                                    LocalMusicScanCoverage.SourceKind(sourceKind = LocalMusicSourceKind.AndroidMediaStore),
                                ),
                            completedAt = 99L,
                        ),
                    likedSongIds = emptySet(),
                )

            val summary: LocalMusicScanState.Done = snapshot.scanState as LocalMusicScanState.Done
            val availableSongIds: Set<String> =
                repository
                    .getAllAvailableSongs()
                    .map { song: Song -> song.id }
                    .toSet()
            assertTrue(actual = localSongDao.row("androidMediaStore:old-safe-song")!!.isAvailable)
            assertTrue(actual = localSongDao.row("androidMediaStore:verified-new-song")!!.isAvailable)
            assertEquals(
                expected =
                    setOf(
                        "androidMediaStore:old-safe-song",
                        "androidMediaStore:verified-new-song",
                    ),
                actual = availableSongIds,
            )
            assertEquals(expected = 0, actual = summary.summary.removedCount)
            assertEquals(expected = 1, actual = summary.summary.problemCount)
        }

    @Test
    fun snapshotStatsCountBlankAlbumAndArtistAsSingleUnknownGroup(): Unit =
        runBlocking {
            val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
            val repository: PersistentMusicLibraryRepository =
                PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = FakeFavoriteSongDao(),
                )
            localSongDao.upsertSongs(
                songs =
                    listOf(
                        entity(
                            id = "androidMediaStore:null-metadata",
                            title = "Null Metadata",
                            modifiedAt = 1L,
                            artist = null,
                            album = null,
                        ),
                        entity(
                            id = "androidMediaStore:empty-metadata",
                            title = "Empty Metadata",
                            modifiedAt = 2L,
                            artist = "",
                            album = "",
                        ),
                        entity(
                            id = "androidMediaStore:blank-metadata",
                            title = "Blank Metadata",
                            modifiedAt = 3L,
                            artist = "   ",
                            album = "  \t  ",
                        ),
                    ),
            )

            val snapshot = repository.getSnapshot()

            assertEquals(expected = 1, actual = snapshot.albums.size)
            assertEquals(expected = "未知专辑", actual = snapshot.albums.single().title)
            assertEquals(expected = 1, actual = snapshot.artists.size)
            assertEquals(expected = "未知歌手", actual = snapshot.artists.single().name)
            assertEquals(expected = 1, actual = snapshot.stats.albumCount)
            assertEquals(expected = 1, actual = snapshot.stats.artistCount)
        }

    /** 构造扫描结果里的歌曲元数据，保持测试关注仓库行为而非构造细节。 */
    private fun metadata(
        sourceId: String,
        title: String,
        modifiedAt: Long,
        sourceKind: LocalMusicSourceKind = LocalMusicSourceKind.AndroidMediaStore,
        concreteSourceId: String? = null,
        coverImageUri: String? = null,
    ): MusicFileMetadata =
        MusicFileMetadata(
            sourceId = sourceId,
            sourceKind = sourceKind,
            concreteSourceId = concreteSourceId,
            localUri = "content://media/$sourceId",
            fileName = "$title.mp3",
            title = title,
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L,
            mimeType = "audio/mpeg",
            sizeBytes = 1_000L,
            modifiedAt = modifiedAt,
            coverArt = CoverArt.HeroLocalMusic,
            coverImageUri = coverImageUri,
        )

    /** 构造失败扫描问题，表达本轮扫描没有删除未处理旧歌的权限。 */
    private fun failedProblem(sourceId: String): LocalMusicProblem =
        LocalMusicProblem(
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            sourceId = sourceId,
            fileName = "$sourceId.mp3",
            error =
                LocalMusicScanError(
                    type = LocalMusicScanErrorType.FileUnreadable,
                    message = "扫描失败",
                    sourceKind = LocalMusicSourceKind.AndroidMediaStore,
                    sourceId = sourceId,
                ),
        )
}

private class FakeLocalSongDao : LocalSongDao {
    // 维持插入顺序，便于断言更新后的行状态。
    private val rows: LinkedHashMap<String, LocalSongEntity> = linkedMapOf()

    /** 模拟首页预览查询，排序规则必须与生产 SQL 一致。 */
    override suspend fun getHomePreview(limit: Int): List<LocalSongEntity> = sortedAvailable().take(limit)

    /** 模拟读取全部可用歌曲，供仓库构建完整快照。 */
    override suspend fun getAllAvailableSongs(): List<LocalSongEntity> = sortedAvailable()

    /** 模拟按歌曲 id 读取可用歌曲，供收藏和恢复按需补齐实体。 */
    override suspend fun getAvailableSongsByIds(songIds: List<String>): List<LocalSongEntity> {
        val requestedIds: Set<String> = songIds.toSet()
        return sortedAvailable().filter { entity: LocalSongEntity ->
            requestedIds.contains(entity.id)
        }
    }

    /** 按来源读取可用歌曲 id，用于验证缺失歌曲下线逻辑。 */
    override suspend fun getAvailableSongIdsBySource(sourceKind: String): List<String> =
        rows.values
            .filter { entity: LocalSongEntity -> entity.sourceKind == sourceKind && entity.isAvailable }
            .map { entity: LocalSongEntity -> entity.id }

    /** 按具体来源读取可用歌曲 id，用于验证目录覆盖只影响当前目录。 */
    override suspend fun getAvailableSongIdsByConcreteSource(
        sourceKind: String,
        concreteSourceId: String,
    ): List<String> =
        rows.values
            .filter { entity: LocalSongEntity ->
                entity.sourceKind == sourceKind &&
                    entity.concreteSourceId == concreteSourceId &&
                    entity.isAvailable
            }.map { entity: LocalSongEntity -> entity.id }

    /** 返回当前仍可用的来源类型集合，供全量扫描空结果时判定覆盖范围。 */
    override suspend fun getAvailableSourceKinds(): List<String> =
        rows.values
            .filter { entity: LocalSongEntity -> entity.isAvailable }
            .map { entity: LocalSongEntity -> entity.sourceKind }
            .distinct()

    /** 覆盖写入歌曲行，保持与 Room 的 replace 语义一致。 */
    override suspend fun upsertSongs(songs: List<LocalSongEntity>) {
        songs.forEach { entity: LocalSongEntity ->
            rows[entity.id] = entity
        }
    }

    /** 只把同来源且本轮缺失的歌曲标记为不可用。 */
    override suspend fun markUnavailable(
        sourceKind: String,
        songIds: List<String>,
    ) {
        songIds.forEach { songId: String ->
            val entity: LocalSongEntity? = rows[songId]
            if (entity != null && entity.sourceKind == sourceKind) {
                rows[songId] = entity.copy(isAvailable = false)
            }
        }
    }

    /** 只把同具体来源且本轮缺失的歌曲标记为不可用。 */
    override suspend fun markUnavailableByConcreteSource(
        sourceKind: String,
        concreteSourceId: String,
        songIds: List<String>,
    ) {
        songIds.forEach { songId: String ->
            val entity: LocalSongEntity? = rows[songId]
            if (
                entity != null &&
                entity.sourceKind == sourceKind &&
                entity.concreteSourceId == concreteSourceId
            ) {
                rows[songId] = entity.copy(isAvailable = false)
            }
        }
    }

    /** 返回当前可用歌曲数量。 */
    override suspend fun countAvailableSongs(): Int =
        rows.values.count { entity: LocalSongEntity ->
            entity.isAvailable
        }

    /** 返回当前可用专辑数量，规则对齐生产 SQL 的 trim + lowercase + 兜底值。 */
    override suspend fun countAvailableAlbums(): Int =
        rows.values
            .filter { entity: LocalSongEntity -> entity.isAvailable }
            .map { entity: LocalSongEntity -> normalizeAlbumKey(album = entity.album) }
            .toSet()
            .size

    /** 返回当前可用歌手数量，规则对齐生产 SQL 的 trim + lowercase + 兜底值。 */
    override suspend fun countAvailableArtists(): Int =
        rows.values
            .filter { entity: LocalSongEntity -> entity.isAvailable }
            .map { entity: LocalSongEntity -> normalizeArtistKey(artist = entity.artist) }
            .toSet()
            .size

    /** 供断言直接读取某一行的最新状态。 */
    fun row(id: String): LocalSongEntity? = rows[id]

    /** 用与生产查询完全一致的排序规则返回可用歌曲。 */
    private fun sortedAvailable(): List<LocalSongEntity> =
        rows.values
            .filter { entity: LocalSongEntity -> entity.isAvailable }
            .sortedWith(
                compareByDescending<LocalSongEntity> { entity: LocalSongEntity ->
                    entity.modifiedAt ?: Long.MIN_VALUE
                }.thenBy { entity: LocalSongEntity ->
                    (entity.title ?: entity.fileName).lowercase()
                },
            )

    /** 统一模拟 SQL 中专辑统计的空白兜底规则，避免测试口径漂移。 */
    private fun normalizeAlbumKey(album: String?): String = album?.trim()?.takeIf { value: String -> value.isNotEmpty() }?.lowercase() ?: "未知专辑"

    /** 统一模拟 SQL 中歌手统计的空白兜底规则，避免测试口径漂移。 */
    private fun normalizeArtistKey(artist: String?): String = artist?.trim()?.takeIf { value: String -> value.isNotEmpty() }?.lowercase() ?: "未知歌手"
}

private class FakeFavoriteSongDao : FavoriteSongDao {
    // 以歌曲 id 作为 key，模拟收藏表的主键覆盖行为。
    private val rows: LinkedHashMap<String, FavoriteSongEntity> = linkedMapOf()

    /** 返回全部收藏歌曲 id。 */
    override suspend fun getFavoriteSongIds(): List<String> = rows.keys.toList()

    /** 保存或覆盖单首收藏记录。 */
    override suspend fun saveFavorite(entity: FavoriteSongEntity) {
        rows[entity.songId] = entity
    }

    /** 删除单首收藏记录。 */
    override suspend fun deleteFavorite(songId: String) {
        rows.remove(songId)
    }
}

/** 构造默认可用的本地歌曲实体，避免测试重复铺开持久化字段。 */
private fun entity(
    id: String,
    sourceKind: String = "androidMediaStore",
    sourceId: String = id.substringAfter(delimiter = ":"),
    concreteSourceId: String? = null,
    title: String,
    modifiedAt: Long,
    artist: String? = "Artist",
    album: String? = "Album",
    coverArt: CoverArt = CoverArt.HeroLocalMusic,
): LocalSongEntity =
    LocalSongEntity(
        id = id,
        sourceId = sourceId,
        sourceKind = sourceKind,
        concreteSourceId = concreteSourceId,
        localUri = "content://media/$sourceId",
        fileName = "$title.mp3",
        title = title,
        artist = artist,
        album = album,
        durationMs = 180_000L,
        mimeType = "audio/mpeg",
        sizeBytes = 1_000L,
        modifiedAt = modifiedAt,
        coverArt = coverArt.name,
        lastScannedAt = modifiedAt,
        isAvailable = true,
    )
