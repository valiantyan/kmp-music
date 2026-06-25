package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
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

    /** 构造扫描结果里的歌曲元数据，保持测试关注仓库行为而非构造细节。 */
    private fun metadata(sourceId: String, title: String, modifiedAt: Long): MusicFileMetadata {
        return MusicFileMetadata(
            sourceId = sourceId,
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
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

    /** 按来源读取可用歌曲 id，用于验证缺失歌曲下线逻辑。 */
    override suspend fun getAvailableSongIdsBySource(sourceKind: String): List<String> {
        return rows.values
            .filter { entity: LocalSongEntity -> entity.sourceKind == sourceKind && entity.isAvailable }
            .map { entity: LocalSongEntity -> entity.id }
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
            .map { entity: LocalSongEntity -> (entity.album ?: "未知专辑").trim().lowercase() }
            .toSet()
            .size
    }

    /** 返回当前可用歌手数量，规则对齐生产 SQL 的 trim + lowercase + 兜底值。 */
    override suspend fun countAvailableArtists(): Int {
        return rows.values
            .filter { entity: LocalSongEntity -> entity.isAvailable }
            .map { entity: LocalSongEntity -> (entity.artist ?: "未知歌手").trim().lowercase() }
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
): LocalSongEntity {
    return LocalSongEntity(
        id = id,
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
        coverArt = CoverArt.HeroLocalMusic.name,
        lastScannedAt = modifiedAt,
        isAvailable = true,
    )
}
