package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
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
    fun homePreviewReadsOnlySixSongsSortedByModifiedAt(): Unit = runBlocking {
        val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
        val favoriteSongDao: FakeFavoriteSongDao = FakeFavoriteSongDao()
        val repository: PersistentMusicLibraryRepository = PersistentMusicLibraryRepository(
            localSongDao = localSongDao,
            favoriteSongDao = favoriteSongDao,
        )
        localSongDao.upsertSongs(
            songs = (1..8).map { index: Int ->
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
    fun scanUpsertsExistingSongsAndMarksMissingSameSourceUnavailable(): Unit = runBlocking {
        val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
        val repository: PersistentMusicLibraryRepository = PersistentMusicLibraryRepository(
            localSongDao = localSongDao,
            favoriteSongDao = FakeFavoriteSongDao(),
        )
        localSongDao.upsertSongs(
            songs = listOf(
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
            scanResult = LocalMusicScanResult(
                discovered = listOf(
                    metadata(
                        sourceId = "new",
                        title = "New",
                        modifiedAt = 3L,
                    ),
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
    fun favoritesAreDerivedAndSurviveUnavailableSongs(): Unit = runBlocking {
        val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
        val favoriteSongDao: FakeFavoriteSongDao = FakeFavoriteSongDao()
        val repository: PersistentMusicLibraryRepository = PersistentMusicLibraryRepository(
            localSongDao = localSongDao,
            favoriteSongDao = favoriteSongDao,
        )
        favoriteSongDao.saveFavorite(
            entity = FavoriteSongEntity(
                songId = "androidMediaStore:liked",
                updatedAt = 1L,
            ),
        )
        localSongDao.upsertSongs(
            songs = listOf(
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
            scanResult = LocalMusicScanResult(
                discovered = emptyList(),
                completedAt = 2L,
            ),
            likedSongIds = favoriteSongDao.getFavoriteSongIds().toSet(),
        )

        assertTrue(actual = repository.getAllAvailableSongs().isEmpty())

        repository.applyScanResult(
            request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
            scanResult = LocalMusicScanResult(
                discovered = listOf(
                    metadata(
                        sourceId = "liked",
                        title = "Liked Again",
                        modifiedAt = 3L,
                    ),
                ),
                completedAt = 3L,
            ),
            likedSongIds = favoriteSongDao.getFavoriteSongIds().toSet(),
        )

        assertTrue(actual = repository.getAllAvailableSongs().single().isLiked)
    }

    @Test
    fun applyScanResultPreservesLatestSourceSummariesInReturnedAndCurrentSnapshot(): Unit = runBlocking {
        val repository: PersistentMusicLibraryRepository = PersistentMusicLibraryRepository(
            localSongDao = FakeLocalSongDao(),
            favoriteSongDao = FakeFavoriteSongDao(),
        )
        val sourceSummary = LocalMusicSourceSummary(
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            displayName = "Android 媒体库",
            songCount = 1,
            problemCount = 0,
            lastScannedAt = 123L,
        )

        val snapshot = repository.applyScanResult(
            request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
            scanResult = LocalMusicScanResult(
                discovered = listOf(
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
    fun multiSourceScanCountsAddedAndUpdatedByEntitySourceOnly(): Unit = runBlocking {
        val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
        val repository: PersistentMusicLibraryRepository = PersistentMusicLibraryRepository(
            localSongDao = localSongDao,
            favoriteSongDao = FakeFavoriteSongDao(),
        )
        localSongDao.upsertSongs(
            songs = listOf(
                entity(
                    id = "androidMediaStore:existing",
                    sourceId = "existing",
                    title = "Existing Android",
                    modifiedAt = 1L,
                ),
            ),
        )

        val snapshot = repository.applyScanResult(
            request = LocalMusicScanRequest.Refresh,
            scanResult = LocalMusicScanResult(
                discovered = listOf(
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
    fun refreshWithoutDiscoveredSongsMarksAllExistingSourcesUnavailable(): Unit = runBlocking {
        val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
        val repository: PersistentMusicLibraryRepository = PersistentMusicLibraryRepository(
            localSongDao = localSongDao,
            favoriteSongDao = FakeFavoriteSongDao(),
        )
        localSongDao.upsertSongs(
            songs = listOf(
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

        val snapshot = repository.applyScanResult(
            request = LocalMusicScanRequest.Refresh,
            scanResult = LocalMusicScanResult(
                discovered = emptyList(),
                sourceSummaries = emptyList(),
                completedAt = 88L,
            ),
            likedSongIds = emptySet(),
        )

        val summary: LocalMusicScanState.Done = snapshot.scanState as LocalMusicScanState.Done
        assertFalse(actual = localSongDao.row("androidMediaStore:gone")!!.isAvailable)
        assertFalse(actual = localSongDao.row("desktopFolder:gone")!!.isAvailable)
        assertTrue(actual = repository.getAllAvailableSongs().isEmpty())
        assertEquals(expected = 2, actual = summary.summary.removedCount)
        assertEquals(expected = 0, actual = summary.summary.addedCount)
        assertEquals(expected = 0, actual = summary.summary.updatedCount)
    }

    @Test
    fun snapshotStatsCountBlankAlbumAndArtistAsSingleUnknownGroup(): Unit = runBlocking {
        val localSongDao: FakeLocalSongDao = FakeLocalSongDao()
        val repository: PersistentMusicLibraryRepository = PersistentMusicLibraryRepository(
            localSongDao = localSongDao,
            favoriteSongDao = FakeFavoriteSongDao(),
        )
        localSongDao.upsertSongs(
            songs = listOf(
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
    ): MusicFileMetadata {
        return MusicFileMetadata(
            sourceId = sourceId,
            sourceKind = sourceKind,
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
        )
    }
}

private class FakeLocalSongDao : LocalSongDao {
    // 维持插入顺序，便于断言更新后的行状态。
    private val rows: LinkedHashMap<String, LocalSongEntity> = linkedMapOf()

    /** 模拟首页预览查询，排序规则必须与生产 SQL 一致。 */
    override suspend fun getHomePreview(limit: Int): List<LocalSongEntity> {
        return sortedAvailable().take(limit)
    }

    /** 模拟读取全部可用歌曲，供仓库构建完整快照。 */
    override suspend fun getAllAvailableSongs(): List<LocalSongEntity> {
        return sortedAvailable()
    }

    /** 模拟按歌曲 id 读取可用歌曲，供收藏和恢复按需补齐实体。 */
    override suspend fun getAvailableSongsByIds(songIds: List<String>): List<LocalSongEntity> {
        val requestedIds: Set<String> = songIds.toSet()
        return sortedAvailable().filter { entity: LocalSongEntity ->
            requestedIds.contains(entity.id)
        }
    }

    /** 按来源读取可用歌曲 id，用于验证缺失歌曲下线逻辑。 */
    override suspend fun getAvailableSongIdsBySource(sourceKind: String): List<String> {
        return rows.values
            .filter { entity: LocalSongEntity -> entity.sourceKind == sourceKind && entity.isAvailable }
            .map { entity: LocalSongEntity -> entity.id }
    }

    /** 返回当前仍可用的来源类型集合，供全量扫描空结果时判定覆盖范围。 */
    override suspend fun getAvailableSourceKinds(): List<String> {
        return rows.values
            .filter { entity: LocalSongEntity -> entity.isAvailable }
            .map { entity: LocalSongEntity -> entity.sourceKind }
            .distinct()
    }

    /** 覆盖写入歌曲行，保持与 Room 的 replace 语义一致。 */
    override suspend fun upsertSongs(songs: List<LocalSongEntity>) {
        songs.forEach { entity: LocalSongEntity ->
            rows[entity.id] = entity
        }
    }

    /** 只把同来源且本轮缺失的歌曲标记为不可用。 */
    override suspend fun markUnavailable(sourceKind: String, songIds: List<String>) {
        songIds.forEach { songId: String ->
            val entity: LocalSongEntity? = rows[songId]
            if (entity != null && entity.sourceKind == sourceKind) {
                rows[songId] = entity.copy(isAvailable = false)
            }
        }
    }

    /** 返回当前可用歌曲数量。 */
    override suspend fun countAvailableSongs(): Int = rows.values.count { entity: LocalSongEntity ->
        entity.isAvailable
    }

    /** 返回当前可用专辑数量，规则对齐生产 SQL 的 trim + lowercase + 兜底值。 */
    override suspend fun countAvailableAlbums(): Int {
        return rows.values
            .filter { entity: LocalSongEntity -> entity.isAvailable }
            .map { entity: LocalSongEntity -> normalizeAlbumKey(album = entity.album) }
            .toSet()
            .size
    }

    /** 返回当前可用歌手数量，规则对齐生产 SQL 的 trim + lowercase + 兜底值。 */
    override suspend fun countAvailableArtists(): Int {
        return rows.values
            .filter { entity: LocalSongEntity -> entity.isAvailable }
            .map { entity: LocalSongEntity -> normalizeArtistKey(artist = entity.artist) }
            .toSet()
            .size
    }

    /** 供断言直接读取某一行的最新状态。 */
    fun row(id: String): LocalSongEntity? = rows[id]

    /** 用与生产查询完全一致的排序规则返回可用歌曲。 */
    private fun sortedAvailable(): List<LocalSongEntity> {
        return rows.values
            .filter { entity: LocalSongEntity -> entity.isAvailable }
            .sortedWith(
                compareByDescending<LocalSongEntity> { entity: LocalSongEntity ->
                    entity.modifiedAt ?: Long.MIN_VALUE
                }.thenBy { entity: LocalSongEntity ->
                    (entity.title ?: entity.fileName).lowercase()
                },
            )
    }

    /** 统一模拟 SQL 中专辑统计的空白兜底规则，避免测试口径漂移。 */
    private fun normalizeAlbumKey(album: String?): String {
        return album?.trim()?.takeIf { value: String -> value.isNotEmpty() }?.lowercase() ?: "未知专辑"
    }

    /** 统一模拟 SQL 中歌手统计的空白兜底规则，避免测试口径漂移。 */
    private fun normalizeArtistKey(artist: String?): String {
        return artist?.trim()?.takeIf { value: String -> value.isNotEmpty() }?.lowercase() ?: "未知歌手"
    }
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
    title: String,
    modifiedAt: Long,
    artist: String? = "Artist",
    album: String? = "Album",
): LocalSongEntity {
    return LocalSongEntity(
        id = id,
        sourceId = sourceId,
        sourceKind = sourceKind,
        localUri = "content://media/$sourceId",
        fileName = "$title.mp3",
        title = title,
        artist = artist,
        album = album,
        durationMs = 180_000L,
        mimeType = "audio/mpeg",
        sizeBytes = 1_000L,
        modifiedAt = modifiedAt,
        coverArt = CoverArt.HeroLocalMusic.name,
        lastScannedAt = modifiedAt,
        isAvailable = true,
    )
}
