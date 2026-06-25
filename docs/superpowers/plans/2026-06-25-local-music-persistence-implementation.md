# Local Music Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist manually scanned local songs in Room, load only 6 songs for the home preview on cold start, load the full local library on demand, and preserve favorite and playback queue semantics.

**Architecture:** Add a `local_song` table to the existing common Room database and make `PersistentMusicLibraryRepository` the Android music-library source. Split UI state so home preview, full local library, and playback queue snapshots do not depend on one overloaded `songs` list.

**Tech Stack:** Kotlin Multiplatform 2.0.21, Compose Multiplatform 1.7.3, Room 3, AndroidX SQLite bundled driver, kotlinx.coroutines test, kotlin.test.

---

## File Structure

- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackDatabaseMigrations.kt`: version 1 to 2 Room migration for `local_song`.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackDatabase.kt`: add `LocalSongEntity`, `LocalSongDao`, register DAO, bump database version to 2.
- Modify `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data/AndroidPlaybackDatabase.kt`: register `MIGRATION_1_2` with the Room builder.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/MusicLibraryRepository.kt`: add preview/full-library read APIs and scan request aware `applyScanResult`.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryMusicLibraryRepository.kt`: keep tests and previews working with the expanded interface.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/PersistentMusicLibraryRepository.kt`: Room-backed music library repository.
- Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/data/PersistentMusicLibraryRepositoryTest.kt`: fake DAO tests for preview, upsert, availability, and favorites.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/ScanLocalMusicUseCase.kt`: pass `LocalMusicScanRequest` into repository writes.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/SearchMusicUseCase.kt`: search full available library on demand.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`: add home preview, local library, and queue snapshot state.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`: cold-start preview loading, explicit full-library loading, no auto-scan restore, queue snapshot support.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`: wire home, favorites, search, detail, and local music screens to the split state.
- Modify `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSession.kt`: inject `PersistentMusicLibraryRepository`.
- Modify `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`: update old assumptions and add regressions for cold start, full load, and queue context.
- Update generated Room schema `composeApp/schemas/com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase/2.json` by running Android compile after schema changes.

## Task 1: Room Schema and Migration

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackDatabase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackDatabaseMigrations.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data/AndroidPlaybackDatabase.kt`
- Generated: `composeApp/schemas/com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase/2.json`

- [x] **Step 1: Add the `local_song` entity and DAO**

Add this import:

```kotlin
import androidx.room3.Index
```

Insert this entity after `FavoriteSongEntity`:

```kotlin
/**
 * 持久化本地歌曲扫描元数据，收藏状态由 favorite_song 独立保存。
 */
@Entity(
    tableName = "local_song",
    indices = [
        Index(value = ["sourceKind", "isAvailable"]),
        Index(value = ["isAvailable", "modifiedAt"]),
    ],
)
data class LocalSongEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val sourceKind: String,
    val localUri: String,
    val fileName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val mimeType: String?,
    val sizeBytes: Long?,
    val modifiedAt: Long?,
    val coverArt: String,
    val lastScannedAt: Long,
    val isAvailable: Boolean,
)
```

Insert this DAO after `FavoriteSongDao`:

```kotlin
/**
 * 本地歌曲读写接口。
 */
@Dao
interface LocalSongDao {
    /** 读取首页最多展示的可用歌曲。 */
    @Query(
        """
        SELECT * FROM local_song
        WHERE isAvailable = 1
        ORDER BY COALESCE(modifiedAt, -1) DESC, LOWER(COALESCE(title, fileName)) ASC
        LIMIT :limit
        """,
    )
    suspend fun getHomePreview(limit: Int): List<LocalSongEntity>

    /** 读取全部可用歌曲，供本地二级页、搜索和详情使用。 */
    @Query(
        """
        SELECT * FROM local_song
        WHERE isAvailable = 1
        ORDER BY COALESCE(modifiedAt, -1) DESC, LOWER(COALESCE(title, fileName)) ASC
        """,
    )
    suspend fun getAllAvailableSongs(): List<LocalSongEntity>

    /** 按来源读取可用歌曲 id，用于扫描后标记消失歌曲。 */
    @Query("SELECT id FROM local_song WHERE sourceKind = :sourceKind AND isAvailable = 1")
    suspend fun getAvailableSongIdsBySource(sourceKind: String): List<String>

    /** 覆盖写入扫描确认存在的歌曲元数据。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<LocalSongEntity>)

    /** 标记指定来源下本次未扫描到的歌曲不可用。 */
    @Query("UPDATE local_song SET isAvailable = 0 WHERE sourceKind = :sourceKind AND id IN (:songIds)")
    suspend fun markUnavailable(sourceKind: String, songIds: List<String>)

    /** 统计当前可用歌曲数。 */
    @Query("SELECT COUNT(*) FROM local_song WHERE isAvailable = 1")
    suspend fun countAvailableSongs(): Int

    /** 统计当前可用专辑数。 */
    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT DISTINCT LOWER(TRIM(COALESCE(album, '未知专辑'))) AS albumKey
            FROM local_song
            WHERE isAvailable = 1
        )
        """,
    )
    suspend fun countAvailableAlbums(): Int

    /** 统计当前可用歌手数。 */
    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT DISTINCT LOWER(TRIM(COALESCE(artist, '未知歌手'))) AS artistKey
            FROM local_song
            WHERE isAvailable = 1
        )
        """,
    )
    suspend fun countAvailableArtists(): Int
}
```

Update `@Database`:

```kotlin
@Database(
    entities = [
        PlaybackSnapshotEntity::class,
        PlaybackQueueItemEntity::class,
        FavoriteSongEntity::class,
        LocalSongEntity::class,
    ],
    version = 2,
)
```

Add the DAO accessor:

```kotlin
/** 暴露本地歌曲 DAO。 */
abstract fun localSongDao(): LocalSongDao
```

- [x] **Step 2: Add the migration**

Create `PlaybackDatabaseMigrations.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.persistence

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

/**
 * 播放数据库迁移集合，禁止使用破坏性迁移以保留播放快照和收藏数据。
 */
object PlaybackDatabaseMigrations {
    /** 从播放/收藏数据库升级到包含本地歌曲表。 */
    val MIGRATION_1_2: Migration = object : Migration(startVersion = 1, endVersion = 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSql(
                """
                CREATE TABLE IF NOT EXISTS local_song (
                    id TEXT NOT NULL PRIMARY KEY,
                    sourceId TEXT NOT NULL,
                    sourceKind TEXT NOT NULL,
                    localUri TEXT NOT NULL,
                    fileName TEXT NOT NULL,
                    title TEXT,
                    artist TEXT,
                    album TEXT,
                    durationMs INTEGER,
                    mimeType TEXT,
                    sizeBytes INTEGER,
                    modifiedAt INTEGER,
                    coverArt TEXT NOT NULL,
                    lastScannedAt INTEGER NOT NULL,
                    isAvailable INTEGER NOT NULL
                )
                """,
            )
            connection.execSql(
                "CREATE INDEX IF NOT EXISTS index_local_song_sourceKind_isAvailable ON local_song(sourceKind, isAvailable)",
            )
            connection.execSql(
                "CREATE INDEX IF NOT EXISTS index_local_song_isAvailable_modifiedAt ON local_song(isAvailable, modifiedAt)",
            )
        }
    }
}

private fun SQLiteConnection.execSql(sql: String) {
    prepare(sql.trimIndent()).use { statement ->
        statement.step()
    }
}
```

- [x] **Step 3: Register the migration**

Modify `AndroidPlaybackDatabase.kt`:

```kotlin
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabaseMigrations
```

Update `createPlaybackDatabase`:

```kotlin
fun createPlaybackDatabase(builder: RoomDatabase.Builder<PlaybackDatabase>): PlaybackDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .addMigrations(PlaybackDatabaseMigrations.MIGRATION_1_2)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
```

- [x] **Step 4: Run compile to generate schema 2**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: `BUILD SUCCESSFUL` and `composeApp/schemas/com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase/2.json` exists. If Room reports a schema mismatch, align the migration SQL and entity fields before continuing.

- [x] **Step 5: Commit schema work**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackDatabase.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackDatabaseMigrations.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data/AndroidPlaybackDatabase.kt composeApp/schemas/com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase/2.json
git commit -m "添加本地歌曲数据库表"
```

## Task 2: Repository Interface and In-Memory Compatibility

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/MusicLibraryRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryMusicLibraryRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/ScanLocalMusicUseCase.kt`
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCaseTest.kt`

- [x] **Step 1: Expand the repository contract**

Replace `MusicLibraryRepository` with:

```kotlin
package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 音乐库读取与扫描合并接口，支持首页轻量预览和二级页按需全量加载。
 */
interface MusicLibraryRepository {
    /** 获取当前曲库快照，旧调用方兼容入口。 */
    fun getSnapshot(): LibrarySnapshot

    /** 冷启动首页最多读取 6 条本地歌曲预览。 */
    fun getHomePreview(limit: Int = 6): List<Song>

    /** 读取全部可用本地歌曲，供本地二级页、搜索和详情使用。 */
    fun getAllAvailableSongs(): List<Song>

    /** 读取当前可用曲库统计。 */
    fun getLibraryStats(): LibraryStats

    /** 合并扫描结果并返回新的曲库快照。 */
    fun applyScanResult(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot

    /** 获取本地歌曲，保留旧用例兼容性。 */
    fun getSongs(): List<Song> = getAllAvailableSongs()

    /** 获取本地专辑，保留旧用例兼容性。 */
    fun getAlbums(): List<Album> = getSnapshot().albums

    /** 获取本地歌手，保留旧用例兼容性。 */
    fun getArtists(): List<Artist> = getSnapshot().artists
}
```

- [x] **Step 2: Update the in-memory repository**

In `InMemoryMusicLibraryRepository.kt`, keep the current `snapshot` field and add:

```kotlin
override fun getHomePreview(limit: Int): List<Song> {
    return snapshot.songs.take(n = limit)
}

override fun getAllAvailableSongs(): List<Song> {
    return snapshot.songs
}

override fun getLibraryStats(): LibraryStats {
    return snapshot.stats
}
```

Change `applyScanResult` signature:

```kotlin
override fun applyScanResult(
    request: LocalMusicScanRequest,
    scanResult: LocalMusicScanResult,
    likedSongIds: Set<String>,
): LibrarySnapshot {
    snapshot = mergeUseCase(
        request = MergeLocalMusicScanResultRequest(
            previousSnapshot = snapshot,
            scanResult = scanResult,
            likedSongIds = likedSongIds,
        ),
    )
    return snapshot
}
```

Add imports:

```kotlin
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.Song
```

- [x] **Step 3: Pass scan request through the use case**

In `ScanLocalMusicUseCaseImpl.invoke`, replace the repository call:

```kotlin
return musicLibraryRepository.applyScanResult(
    request = request,
    scanResult = result,
    likedSongIds = likedSongIds,
)
```

- [x] **Step 4: Run existing scan use case test**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.usecase.MergeLocalMusicScanResultUseCaseTest"
```

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit interface compatibility**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/MusicLibraryRepository.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryMusicLibraryRepository.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/ScanLocalMusicUseCase.kt
git commit -m "扩展曲库仓库读取接口"
```

## Task 3: PersistentMusicLibraryRepository

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/PersistentMusicLibraryRepository.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/data/PersistentMusicLibraryRepositoryTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackDatabase.kt`

- [x] **Step 1: Write repository tests with a fake DAO**

Create `PersistentMusicLibraryRepositoryTest.kt` with these test names and helpers:

```kotlin
package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.persistence.FavoriteSongDao
import com.yanhao.kmpmusic.domain.persistence.FavoriteSongEntity
import com.yanhao.kmpmusic.domain.persistence.LocalSongDao
import com.yanhao.kmpmusic.domain.persistence.LocalSongEntity
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentMusicLibraryRepositoryTest {
    @Test
    fun homePreviewReadsOnlySixSongsSortedByModifiedAt(): Unit = runBlocking {
        val localSongDao = FakeLocalSongDao()
        val favoriteSongDao = FakeFavoriteSongDao()
        val repository = PersistentMusicLibraryRepository(
            localSongDao = localSongDao,
            favoriteSongDao = favoriteSongDao,
        )
        localSongDao.upsertSongs((1..8).map { index ->
            entity(id = "androidMediaStore:$index", title = "Song $index", modifiedAt = index.toLong())
        })

        val preview = repository.getHomePreview(limit = 6)

        assertEquals(expected = 6, actual = preview.size)
        assertEquals(expected = "Song 8", actual = preview.first().title)
        assertEquals(expected = "Song 3", actual = preview.last().title)
    }

    @Test
    fun scanUpsertsExistingSongsAndMarksMissingSameSourceUnavailable(): Unit = runBlocking {
        val localSongDao = FakeLocalSongDao()
        val repository = PersistentMusicLibraryRepository(localSongDao, FakeFavoriteSongDao())
        localSongDao.upsertSongs(
            listOf(
                entity(id = "androidMediaStore:old", sourceId = "old", title = "Old", modifiedAt = 1L),
                entity(id = "desktopFolder:keep", sourceKind = "desktopFolder", sourceId = "keep", title = "Desktop", modifiedAt = 2L),
            ),
        )

        repository.applyScanResult(
            request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
            scanResult = LocalMusicScanResult(
                discovered = listOf(metadata(sourceId = "new", title = "New", modifiedAt = 3L)),
                completedAt = 99L,
            ),
            likedSongIds = emptySet(),
        )

        assertFalse(localSongDao.row("androidMediaStore:old")!!.isAvailable)
        assertTrue(localSongDao.row("androidMediaStore:new")!!.isAvailable)
        assertTrue(localSongDao.row("desktopFolder:keep")!!.isAvailable)
    }

    @Test
    fun favoritesAreDerivedAndSurviveUnavailableSongs(): Unit = runBlocking {
        val localSongDao = FakeLocalSongDao()
        val favoriteSongDao = FakeFavoriteSongDao()
        val repository = PersistentMusicLibraryRepository(localSongDao, favoriteSongDao)
        favoriteSongDao.saveFavorite(FavoriteSongEntity(songId = "androidMediaStore:liked", updatedAt = 1L))
        localSongDao.upsertSongs(listOf(entity(id = "androidMediaStore:liked", sourceId = "liked", title = "Liked", modifiedAt = 1L)))

        assertTrue(repository.getAllAvailableSongs().single().isLiked)

        repository.applyScanResult(
            request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
            scanResult = LocalMusicScanResult(discovered = emptyList(), completedAt = 2L),
            likedSongIds = favoriteSongDao.getFavoriteSongIds().toSet(),
        )

        assertTrue(repository.getAllAvailableSongs().isEmpty())

        repository.applyScanResult(
            request = LocalMusicScanRequest.Source(LocalMusicSourceKind.AndroidMediaStore),
            scanResult = LocalMusicScanResult(
                discovered = listOf(metadata(sourceId = "liked", title = "Liked Again", modifiedAt = 3L)),
                completedAt = 3L,
            ),
            likedSongIds = favoriteSongDao.getFavoriteSongIds().toSet(),
        )

        assertTrue(repository.getAllAvailableSongs().single().isLiked)
    }

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
```

Then add fake DAO implementations in the same file. The fake `LocalSongDao` must sort exactly like production queries:

```kotlin
private class FakeLocalSongDao : LocalSongDao {
    private val rows: LinkedHashMap<String, LocalSongEntity> = linkedMapOf()

    override suspend fun getHomePreview(limit: Int): List<LocalSongEntity> {
        return sortedAvailable().take(limit)
    }

    override suspend fun getAllAvailableSongs(): List<LocalSongEntity> {
        return sortedAvailable()
    }

    override suspend fun getAvailableSongIdsBySource(sourceKind: String): List<String> {
        return rows.values.filter { entity -> entity.sourceKind == sourceKind && entity.isAvailable }.map { entity -> entity.id }
    }

    override suspend fun upsertSongs(songs: List<LocalSongEntity>) {
        songs.forEach { entity -> rows[entity.id] = entity }
    }

    override suspend fun markUnavailable(sourceKind: String, songIds: List<String>) {
        songIds.forEach { songId ->
            val entity = rows[songId]
            if (entity != null && entity.sourceKind == sourceKind) {
                rows[songId] = entity.copy(isAvailable = false)
            }
        }
    }

    override suspend fun countAvailableSongs(): Int = rows.values.count { entity -> entity.isAvailable }

    override suspend fun countAvailableAlbums(): Int {
        return rows.values.filter { entity -> entity.isAvailable }
            .map { entity -> (entity.album ?: "未知专辑").trim().lowercase() }
            .toSet()
            .size
    }

    override suspend fun countAvailableArtists(): Int {
        return rows.values.filter { entity -> entity.isAvailable }
            .map { entity -> (entity.artist ?: "未知歌手").trim().lowercase() }
            .toSet()
            .size
    }

    fun row(id: String): LocalSongEntity? = rows[id]

    private fun sortedAvailable(): List<LocalSongEntity> {
        return rows.values.filter { entity -> entity.isAvailable }
            .sortedWith(
                compareByDescending<LocalSongEntity> { entity -> entity.modifiedAt ?: Long.MIN_VALUE }
                    .thenBy { entity -> (entity.title ?: entity.fileName).lowercase() },
            )
    }
}

private class FakeFavoriteSongDao : FavoriteSongDao {
    private val rows: LinkedHashMap<String, FavoriteSongEntity> = linkedMapOf()

    override suspend fun getFavoriteSongIds(): List<String> = rows.keys.toList()

    override suspend fun saveFavorite(entity: FavoriteSongEntity) {
        rows[entity.songId] = entity
    }

    override suspend fun deleteFavorite(songId: String) {
        rows.remove(songId)
    }
}
```

Add this helper:

```kotlin
private fun entity(
    id: String,
    sourceKind: String = "androidMediaStore",
    sourceId: String = id.substringAfter(":"),
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
```

- [x] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest"
```

Expected: FAIL because `PersistentMusicLibraryRepository` does not exist.

- [x] **Step 3: Implement the persistent repository**

Create `PersistentMusicLibraryRepository.kt`:

```kotlin
package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.FavoriteSongDao
import com.yanhao.kmpmusic.domain.persistence.LocalSongDao
import com.yanhao.kmpmusic.domain.persistence.LocalSongEntity
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import kotlinx.coroutines.runBlocking

/**
 * Room backed 曲库仓库，负责本地歌曲扫描事实与收藏状态派生。
 */
class PersistentMusicLibraryRepository(
    private val localSongDao: LocalSongDao,
    private val favoriteSongDao: FavoriteSongDao,
) : MusicLibraryRepository {
    private var lastScanState: LocalMusicScanState = LocalMusicScanState.Idle
    private var lastProblems = emptyList<com.yanhao.kmpmusic.domain.model.LocalMusicProblem>()

    override fun getSnapshot(): LibrarySnapshot = runBlocking {
        val songs = readAllSongs()
        buildSnapshot(songs = songs, scanState = lastScanState)
    }

    override fun getHomePreview(limit: Int): List<Song> = runBlocking {
        mapEntities(localSongDao.getHomePreview(limit = limit), favoriteSongDao.getFavoriteSongIds().toSet())
    }

    override fun getAllAvailableSongs(): List<Song> = runBlocking {
        readAllSongs()
    }

    override fun getLibraryStats(): LibraryStats = runBlocking {
        readLibraryStats()
    }

    private suspend fun readLibraryStats(): LibraryStats {
        LibraryStats(
            songCount = localSongDao.countAvailableSongs(),
            albumCount = localSongDao.countAvailableAlbums(),
            artistCount = localSongDao.countAvailableArtists(),
        )
    }

    override fun applyScanResult(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot = runBlocking {
        val coveredSources = resolveCoveredSources(request = request, scanResult = scanResult)
        val discoveredEntities = scanResult.discovered
            .filter { metadata -> metadata.localUri.isNotBlank() }
            .map { metadata -> metadata.toEntity(lastScannedAt = scanResult.completedAt) }
        val previousIdsBySource = coveredSources.associateWith { sourceKind ->
            localSongDao.getAvailableSongIdsBySource(sourceKind = sourceKind.value).toSet()
        }
        localSongDao.upsertSongs(songs = discoveredEntities)
        coveredSources.forEach { sourceKind ->
            val discoveredIds = discoveredEntities
                .filter { entity -> entity.sourceKind == sourceKind.value }
                .map { entity -> entity.id }
                .toSet()
            val missingIds = previousIdsBySource.getValue(sourceKind) - discoveredIds
            if (missingIds.isNotEmpty()) {
                localSongDao.markUnavailable(sourceKind = sourceKind.value, songIds = missingIds.toList())
            }
        }
        val summary = LocalMusicLastScanSummary(
            addedCount = discoveredEntities.count { entity ->
                coveredSources.any { sourceKind -> previousIdsBySource[sourceKind]?.contains(entity.id) == false }
            },
            updatedCount = discoveredEntities.count { entity ->
                coveredSources.any { sourceKind -> previousIdsBySource[sourceKind]?.contains(entity.id) == true }
            },
            removedCount = coveredSources.sumOf { sourceKind ->
                val discoveredIds = discoveredEntities.filter { entity -> entity.sourceKind == sourceKind.value }.map { entity -> entity.id }.toSet()
                (previousIdsBySource.getValue(sourceKind) - discoveredIds).size
            },
            problemCount = scanResult.failed.size,
            completedAt = scanResult.completedAt,
        )
        lastScanState = LocalMusicScanState.Done(summary = summary)
        lastProblems = scanResult.failed
        buildSnapshot(songs = readAllSongs(likedSongIds = likedSongIds), scanState = lastScanState)
    }

    private suspend fun readAllSongs(likedSongIds: Set<String> = favoriteSongDao.getFavoriteSongIds().toSet()): List<Song> {
        return mapEntities(localSongDao.getAllAvailableSongs(), likedSongIds)
    }

    private fun mapEntities(entities: List<LocalSongEntity>, likedSongIds: Set<String>): List<Song> {
        return entities.mapIndexed { index, entity -> entity.toSong(index = index, likedSongIds = likedSongIds) }
    }

    private suspend fun buildSnapshot(songs: List<Song>, scanState: LocalMusicScanState): LibrarySnapshot {
        val albums = buildAlbums(songs = songs)
        val artists = buildArtists(songs = songs)
        return LibrarySnapshot(
            songs = songs,
            albums = albums,
            artists = artists,
            stats = readLibraryStats(),
            sources = emptyList(),
            scanState = scanState,
            lastScanSummary = (scanState as? LocalMusicScanState.Done)?.summary,
            problems = lastProblems,
        )
    }

    private fun resolveCoveredSources(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
    ): Set<LocalMusicSourceKind> {
        val fromRequest = when (request) {
            is LocalMusicScanRequest.Source -> setOf(request.sourceKind)
            LocalMusicScanRequest.InitialScan,
            LocalMusicScanRequest.Refresh,
            -> emptySet()
        }
        val fromSummaries = scanResult.sourceSummaries.map { source -> source.sourceKind }.toSet()
        val fromDiscovered = scanResult.discovered.map { metadata -> metadata.sourceKind }.toSet()
        return fromRequest + fromSummaries + fromDiscovered
    }

    private fun MusicFileMetadata.toEntity(lastScannedAt: Long): LocalSongEntity {
        return LocalSongEntity(
            id = sourceKey,
            sourceId = sourceId,
            sourceKind = sourceKind.value,
            localUri = localUri,
            fileName = fileName,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            modifiedAt = modifiedAt,
            coverArt = coverArt.name,
            lastScannedAt = lastScannedAt,
            isAvailable = true,
        )
    }

    private fun LocalSongEntity.toSong(index: Int, likedSongIds: Set<String>): Song {
        val safeTitle = title?.takeIf { value -> value.isNotBlank() } ?: fileName.substringBeforeLast(".", fileName)
        val safeArtist = artist?.takeIf { value -> value.isNotBlank() } ?: "未知歌手"
        val safeAlbum = album?.takeIf { value -> value.isNotBlank() } ?: "未知专辑"
        return Song(
            id = id,
            title = safeTitle,
            artist = safeArtist,
            album = safeAlbum,
            duration = formatDuration(durationMs = durationMs),
            coverArt = CoverArt.entries.firstOrNull { cover -> cover.name == coverArt } ?: CoverArt.HeroLocalMusic,
            isLiked = likedSongIds.contains(id),
            lastPlayed = "未播放",
            quality = formatQuality(mimeType = mimeType),
            lyric = "来自${sourceKind}的本地音频。",
            trackNumber = index + 1,
            durationMs = durationMs,
            sourceId = sourceId,
            sourceKind = LocalMusicSourceKind.entries.firstOrNull { kind -> kind.value == sourceKind } ?: LocalMusicSourceKind.FakeScanner,
            localUri = localUri,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            modifiedAt = modifiedAt,
        )
    }

    private fun buildAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { song -> song.album.trim().lowercase() }.values.map { albumSongs ->
            val firstSong = albumSongs.first()
            Album(
                id = "album:${firstSong.album.trim().lowercase()}",
                title = firstSong.album,
                artist = firstSong.artist,
                songCount = albumSongs.size,
                coverArt = firstSong.coverArt,
                mood = "本地音乐",
                year = "本地",
            )
        }.sortedBy { album -> album.title.lowercase() }
    }

    private fun buildArtists(songs: List<Song>): List<Artist> {
        return songs.groupBy { song -> song.artist.trim().lowercase() }.values.map { artistSongs ->
            val firstSong = artistSongs.first()
            Artist(
                id = "artist:${firstSong.artist.trim().lowercase()}",
                name = firstSong.artist,
                songCount = artistSongs.size,
                coverArt = firstSong.coverArt,
                tag = "本地音乐",
            )
        }.sortedBy { artist -> artist.name.lowercase() }
    }

    private fun formatDuration(durationMs: Long?): String {
        if (durationMs == null || durationMs <= 0L) return "--:--"
        val totalSeconds = durationMs / 1_000L
        return "${totalSeconds / 60L}:${(totalSeconds % 60L).toString().padStart(2, '0')}"
    }

    private fun formatQuality(mimeType: String?): String {
        val suffix = mimeType?.substringAfterLast("/")?.uppercase().orEmpty()
        return if (suffix.isBlank()) "本地音频" else "本地 $suffix"
    }
}
```

- [x] **Step 4: Run repository tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest"
```

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit persistent repository**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/PersistentMusicLibraryRepository.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/data/PersistentMusicLibraryRepositoryTest.kt
git commit -m "实现本地歌曲持久化仓库"
```

## Task 4: Split UI State and Preserve Queue Snapshots

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [ ] **Step 1: Add failing controller tests**

Add these tests to `MusicAppControllerTest`:

```kotlin
@Test
fun coldStartUsesHomePreviewWithoutFullLocalSongs(): Unit {
    val repository = SeededMusicLibraryRepository(seedCount = 8)
    val controller = createController(musicLibraryRepository = repository)

    assertEquals(expected = 1, actual = repository.homePreviewReads)
    assertEquals(expected = 0, actual = repository.fullLibraryReads)
    assertEquals(expected = 6, actual = controller.uiState.homeLocalSongPreview.size)
    assertTrue(controller.uiState.localSongs.isEmpty())
}

@Test
fun localMusicPageLoadsFullSongsOnDemand(): Unit {
    val repository = SeededMusicLibraryRepository(seedCount = 8)
    val controller = createController(musicLibraryRepository = repository)

    controller.openLocalMusic(section = LocalMusicSection.Songs)

    assertEquals(expected = 1, actual = repository.fullLibraryReads)
    assertEquals(expected = 8, actual = controller.uiState.localSongs.size)
}

@Test
fun queueSongsSurviveAfterPlaybackContextIsNoLongerInSongs(): Unit = runTest {
    val controller = createController(controllerScope = backgroundScope)
    controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
    val queueSongs = controller.uiState.localSongs.take(4).ifEmpty { controller.uiState.homeLocalSongPreview.take(4) }

    controller.playSong(song = queueSongs[0], queueSongs = queueSongs)

    assertEquals(expected = queueSongs.map { song -> song.id }, actual = controller.uiState.queueSongs.map { song -> song.id })
}
```

Update the `createController` helper to accept a repository:

```kotlin
private fun createController(
    musicLibraryRepository: MusicLibraryRepository = InMemoryMusicLibraryRepository(),
    localMusicScanner: LocalMusicScanner = FakeControllerLocalMusicScanner,
    playbackRepository: InMemoryPlaybackRepository = InMemoryPlaybackRepository(),
    playbackSnapshotStore: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore(),
    permissionSettingsOpener: PermissionSettingsOpener = PermissionSettingsOpener {},
    controllerScope: CoroutineScope = testControllerScope(),
): MusicAppController {
    return MusicAppController(
        musicLibraryRepository = musicLibraryRepository,
        localMusicScanner = localMusicScanner,
        playbackRepository = playbackRepository,
        playbackSnapshotStore = playbackSnapshotStore,
        permissionSettingsOpener = permissionSettingsOpener,
        controllerScope = controllerScope,
    )
}
```

Add imports:

```kotlin
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
```

Add a `SeededMusicLibraryRepository` helper near other test fakes:

```kotlin
private class SeededMusicLibraryRepository(seedCount: Int) : com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository {
    var homePreviewReads: Int = 0
    var fullLibraryReads: Int = 0
    private val seededSongs: List<Song> = (1..seedCount).map { index ->
        testSong(id = "seed:$index", title = "Seed $index", modifiedAt = index.toLong())
    }.sortedByDescending { song -> song.modifiedAt }

    override fun getSnapshot(): LibrarySnapshot {
        val albums = listOf(
            Album(
                id = "album:album",
                title = "Album",
                artist = "Artist",
                songCount = seededSongs.size,
                coverArt = CoverArt.HeroLocalMusic,
                mood = "本地音乐",
                year = "本地",
            ),
        )
        val artists = listOf(
            Artist(
                id = "artist:artist",
                name = "Artist",
                songCount = seededSongs.size,
                coverArt = CoverArt.HeroLocalMusic,
                tag = "本地音乐",
            ),
        )
        return LibrarySnapshot(
            songs = seededSongs,
            albums = albums,
            artists = artists,
            stats = getLibraryStats(),
            sources = emptyList(),
            scanState = LocalMusicScanState.Idle,
            lastScanSummary = null,
            problems = emptyList(),
        )
    }

    override fun getHomePreview(limit: Int): List<Song> {
        homePreviewReads += 1
        return seededSongs.take(limit)
    }

    override fun getAllAvailableSongs(): List<Song> {
        fullLibraryReads += 1
        return seededSongs
    }

    override fun getLibraryStats(): LibraryStats {
        return LibraryStats(songCount = seededSongs.size, albumCount = 1, artistCount = 1)
    }

    override fun applyScanResult(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot {
        return getSnapshot()
    }
}
```

Add `testSong`:

```kotlin
private fun testSong(id: String, title: String, modifiedAt: Long): Song {
    return Song(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        duration = "3:00",
        coverArt = CoverArt.HeroLocalMusic,
        isLiked = false,
        lastPlayed = "未播放",
        quality = "本地 MP3",
        lyric = "本地音频",
        trackNumber = 1,
        durationMs = 180_000L,
        sourceId = id.substringAfter(":"),
        sourceKind = LocalMusicSourceKind.FakeScanner,
        localUri = "fake://$id",
        mimeType = "audio/mpeg",
        sizeBytes = 1_000L,
        modifiedAt = modifiedAt,
    )
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest"
```

Expected: FAIL because `homeLocalSongPreview` and `localSongs` do not exist.

- [ ] **Step 3: Split `MusicAppUiState`**

In `MusicAppModels.kt`, replace `songs`, `albums`, `artists`, and `localSongPreview` fields with this group:

```kotlin
val homeLocalSongPreview: List<Song> = emptyList(),
val localSongs: List<Song> = emptyList(),
val localAlbums: List<Album> = emptyList(),
val localArtists: List<Artist> = emptyList(),
val favoriteSongs: List<Song> = emptyList(),
val queueSongsSnapshot: List<Song> = emptyList(),
```

Keep compatibility properties inside the data class body:

```kotlin
val songs: List<Song>
    get() = localSongs.ifEmpty { homeLocalSongPreview }

val albums: List<Album>
    get() = localAlbums

val artists: List<Artist>
    get() = localArtists

val localSongPreview: List<Song>
    get() = homeLocalSongPreview
```

Replace `currentSong` and `queueSongs`:

```kotlin
val currentSong: Song? = currentSongId?.let { songId ->
    queueSongsSnapshot.firstOrNull { song -> song.id == songId }
        ?: localSongs.firstOrNull { song -> song.id == songId }
        ?: homeLocalSongPreview.firstOrNull { song -> song.id == songId }
        ?: favoriteSongs.firstOrNull { song -> song.id == songId }
}

val queueSongs: List<Song> = queueSongIds.mapNotNull { songId ->
    queueSongsSnapshot.firstOrNull { song -> song.id == songId }
        ?: localSongs.firstOrNull { song -> song.id == songId }
        ?: homeLocalSongPreview.firstOrNull { song -> song.id == songId }
        ?: favoriteSongs.firstOrNull { song -> song.id == songId }
}
```

- [ ] **Step 4: Update controller initialization and local loading**

In `createInitialState`, read preview only:

```kotlin
val homePreview: List<Song> = musicLibraryRepository.getHomePreview(limit = 6)
val stats: LibraryStats = musicLibraryRepository.getLibraryStats()
val initialLikedSongIds: Set<String> = injectedFavoritesRepository?.getLikedSongIds()
    ?: homePreview.filter { song -> song.isLiked }.map { song -> song.id }.toSet()
val previewWithLikes = homePreview.map { song ->
    song.copy(isLiked = initialLikedSongIds.contains(song.id) || song.isLiked)
}
```

Return state with:

```kotlin
homeLocalSongPreview = previewWithLikes,
localSongs = emptyList(),
localAlbums = emptyList(),
localArtists = emptyList(),
favoriteSongs = previewWithLikes.filter { song -> song.isLiked },
queueSongsSnapshot = emptyList(),
libraryStats = stats,
recentSongs = buildRecentSongs(songs = previewWithLikes),
```

Add this method:

```kotlin
fun loadLocalMusicLibrary() {
    val likedSongIds = favoritesRepository.getLikedSongIds()
    val songsWithLikes = musicLibraryRepository.getAllAvailableSongs().map { song ->
        song.copy(isLiked = likedSongIds.contains(song.id) || song.isLiked)
    }
    uiState = uiState.copy(
        localSongs = songsWithLikes,
        localAlbums = buildAlbums(songs = songsWithLikes),
        localArtists = buildArtists(songs = songsWithLikes),
        favoriteSongs = songsWithLikes.filter { song -> song.isLiked },
        likedSongIds = likedSongIds + songsWithLikes.filter { song -> song.isLiked }.map { song -> song.id },
    )
}
```

Update `openLocalMusic`:

```kotlin
fun openLocalMusic(section: LocalMusicSection = LocalMusicSection.Songs) {
    loadLocalMusicLibrary()
    navigateToSecondary(screen = SecondaryScreen.LocalMusic(initialSection = section))
}
```

Add private `buildAlbums` and `buildArtists` to controller using the same aggregation code from `PersistentMusicLibraryRepository`.

Update `syncLibrarySnapshot` so scan success refreshes the home preview and, when the full local library is already loaded, refreshes it too:

```kotlin
private fun syncLibrarySnapshot(snapshot: LibrarySnapshot) {
    val likedSongIds: Set<String> = favoritesRepository.getLikedSongIds()
    val previewWithLikes: List<Song> = musicLibraryRepository.getHomePreview(limit = 6).map { song ->
        song.copy(isLiked = likedSongIds.contains(song.id) || song.isLiked)
    }
    val shouldRefreshFullLibrary: Boolean = uiState.localSongs.isNotEmpty() ||
        uiState.navigationState.secondaryScreen is SecondaryScreen.LocalMusic
    val fullSongsWithLikes: List<Song> = if (shouldRefreshFullLibrary) {
        musicLibraryRepository.getAllAvailableSongs().map { song ->
            song.copy(isLiked = likedSongIds.contains(song.id) || song.isLiked)
        }
    } else {
        uiState.localSongs
    }
    uiState = uiState.copy(
        homeLocalSongPreview = previewWithLikes,
        localSongs = fullSongsWithLikes,
        localAlbums = if (shouldRefreshFullLibrary) buildAlbums(songs = fullSongsWithLikes) else uiState.localAlbums,
        localArtists = if (shouldRefreshFullLibrary) buildArtists(songs = fullSongsWithLikes) else uiState.localArtists,
        libraryStats = musicLibraryRepository.getLibraryStats(),
        localMusicSources = snapshot.sources,
        localMusicProblems = snapshot.problems,
        scanState = snapshot.scanState,
        likedSongIds = likedSongIds + previewWithLikes.filter { song -> song.isLiked }.map { song -> song.id },
        recentSongs = buildRecentSongs(songs = fullSongsWithLikes.ifEmpty { previewWithLikes }),
        favoriteSongs = fullSongsWithLikes.filter { song -> song.isLiked },
    )
    restorePlaybackSnapshotIfPending(availableSongs = fullSongsWithLikes.ifEmpty { previewWithLikes })
}
```

Update `toggleFavorite` to update every loaded song list and the queue snapshot:

```kotlin
fun toggleFavorite(songId: String) {
    val likedSongIds: Set<String> = toggleFavoriteUseCase(songId = songId)
    fun Song.withFavorite(): Song = copy(isLiked = likedSongIds.contains(id))
    val homePreview = uiState.homeLocalSongPreview.map { song -> song.withFavorite() }
    val localSongs = uiState.localSongs.map { song -> song.withFavorite() }
    val queueSnapshot = uiState.queueSongsSnapshot.map { song -> song.withFavorite() }
    uiState = uiState.copy(
        likedSongIds = likedSongIds,
        homeLocalSongPreview = homePreview,
        localSongs = localSongs,
        favoriteSongs = localSongs.filter { song -> song.isLiked },
        queueSongsSnapshot = queueSnapshot,
        recentSongs = buildRecentSongs(songs = localSongs.ifEmpty { homePreview }),
    )
    publishPlaybackUiState()
}
```

- [ ] **Step 5: Preserve queue snapshot when playing**

In `playSong`, after resolving queue songs and before launching playback:

```kotlin
uiState = uiState.copy(queueSongsSnapshot = resolvedQueueSongs)
```

In `removeFromQueue`, pass `uiState.queueSongs`:

```kotlin
availableSongs = uiState.queueSongs,
```

In `restorePlaybackSnapshot`, remove the call to `requestInitialLibraryRefreshForRestoreIfNeeded()` and use available loaded songs only:

```kotlin
if (uiState.queueSongsSnapshot.isEmpty() && uiState.localSongs.isEmpty() && uiState.homeLocalSongPreview.isEmpty()) {
    isPlaybackRestorePending = playbackSnapshotStore.hasSavedSnapshot()
    return
}
```

Then call:

```kotlin
playbackCoordinator.restoreSnapshot(
    availableSongs = uiState.localSongs.ifEmpty { uiState.homeLocalSongPreview },
)
```

Delete `requestInitialLibraryRefreshForRestoreIfNeeded`.

- [ ] **Step 6: Run controller tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest"
```

Expected: `BUILD SUCCESSFUL` after updating old assertions from `uiState.songs` to either `localSongs` or `homeLocalSongPreview`.

- [ ] **Step 7: Commit state split**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "拆分首页预览与本地曲库状态"
```

## Task 5: UI Wiring and On-Demand Search/Details

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/SearchMusicUseCase.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [ ] **Step 1: Update search use case to use full available songs**

Replace `SearchMusicUseCaseImpl.invoke` with:

```kotlin
override operator fun invoke(query: String, scope: SearchScope): SearchResult {
    val normalizedQuery: String = query.trim().lowercase()
    val allSongs: List<Song> = musicLibraryRepository.getAllAvailableSongs()
    val allAlbums: List<Album> = buildAlbums(songs = allSongs)
    val allArtists: List<Artist> = buildArtists(songs = allSongs)
    val songs: List<Song> = if (scope == SearchScope.All || scope == SearchScope.Songs) {
        allSongs.filter { song -> matchesSong(song = song, normalizedQuery = normalizedQuery) }
    } else {
        emptyList()
    }
    val albums: List<Album> = if (scope == SearchScope.All || scope == SearchScope.Albums) {
        allAlbums.filter { album -> matchesAlbum(album = album, normalizedQuery = normalizedQuery) }
    } else {
        emptyList()
    }
    val artists: List<Artist> = if (scope == SearchScope.All || scope == SearchScope.Artists) {
        allArtists.filter { artist -> matchesArtist(artist = artist, normalizedQuery = normalizedQuery) }
    } else {
        emptyList()
    }
    return SearchResult(songs = songs, albums = albums, artists = artists)
}
```

Add private aggregation methods copied from controller:

```kotlin
private fun buildAlbums(songs: List<Song>): List<Album> {
    return songs.groupBy { song -> song.album.trim().lowercase() }.values.map { albumSongs ->
        val firstSong = albumSongs.first()
        Album(
            id = "album:${firstSong.album.trim().lowercase()}",
            title = firstSong.album,
            artist = firstSong.artist,
            songCount = albumSongs.size,
            coverArt = firstSong.coverArt,
            mood = "本地音乐",
            year = "本地",
        )
    }.sortedBy { album -> album.title.lowercase() }
}

private fun buildArtists(songs: List<Song>): List<Artist> {
    return songs.groupBy { song -> song.artist.trim().lowercase() }.values.map { artistSongs ->
        val firstSong = artistSongs.first()
        Artist(
            id = "artist:${firstSong.artist.trim().lowercase()}",
            name = firstSong.artist,
            songCount = artistSongs.size,
            coverArt = firstSong.coverArt,
            tag = "本地音乐",
        )
    }.sortedBy { artist -> artist.name.lowercase() }
}
```

- [ ] **Step 2: Add controller helpers for opening search and details**

Add:

```kotlin
fun openSearch() {
    loadLocalMusicLibrary()
    navigateToSecondary(screen = SecondaryScreen.Search)
}
```

Update `openAlbum`, `openArtist`, `openAlbumFromSong`, and `openArtistFromSong` to call `loadLocalMusicLibrary()` before resolving ids.

- [ ] **Step 3: Wire UI to split state**

In `MusicApp.kt`:

- Change home to pass `songs = state.homeLocalSongPreview` and `localSongPreview = state.homeLocalSongPreview`.
- Change home search callback to `onSearch = controller::openSearch`.
- Change local music screen to pass `songs = state.localSongs`, `albums = state.localAlbums`, `artists = state.localArtists`.
- Change album detail to pass `songs = state.localSongs`.
- Change artist detail to pass `songs = state.localSongs`, `albums = state.localAlbums`.
- Change favorites screen to pass `songs = state.favoriteSongs`, `albums = state.localAlbums`, `artists = state.localArtists`.
- Change more sheet lookup to use `state.currentSong ?: state.queueSongs.firstOrNull { it.id == songId } ?: state.localSongs.firstOrNull { it.id == songId } ?: state.homeLocalSongPreview.firstOrNull { it.id == songId }`.

The home block should look like:

```kotlin
RootTab.Home -> HomeScreen(
    songs = state.homeLocalSongPreview,
    albums = state.localAlbums,
    libraryStats = state.libraryStats,
    scanState = state.scanState,
    recentSongs = state.recentSongs,
    localSongPreview = state.homeLocalSongPreview,
    currentSongId = state.currentSongId,
    onSearch = controller::openSearch,
    onScan = onScanLocalMusic,
    onLocalMusic = { controller.openLocalMusic(section = LocalMusicSection.Songs) },
    onSongOpen = { song: Song, queueSongs: List<Song> -> controller.openSong(song = song, queueSongs = queueSongs) },
    onSongPlay = { song: Song, queueSongs: List<Song> -> controller.playSong(song = song, queueSongs = queueSongs) },
    onMore = controller::openMore,
    onAlbumOpen = controller::openAlbum,
)
```

- [ ] **Step 4: Add search regression test**

Add to controller tests:

```kotlin
@Test
fun searchLoadsFullLibraryInsteadOfHomePreviewOnly(): Unit {
    val repository = SeededMusicLibraryRepository(seedCount = 8)
    val controller = createController(musicLibraryRepository = repository)

    controller.openSearch()
    controller.setSearchQuery(query = "Seed 8")
    controller.setSearchScope(scope = SearchScope.Songs)

    assertEquals(expected = listOf("Seed 8"), actual = controller.search().songs.map { song -> song.title })
    assertEquals(expected = 1, actual = repository.fullLibraryReads)
}
```

- [ ] **Step 5: Run UI/controller tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest"
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit UI wiring**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/SearchMusicUseCase.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "按需加载本地曲库页面数据"
```

## Task 6: Android Injection and No Auto-Scan Restore

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSession.kt`
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [ ] **Step 1: Replace auto-scan restore test**

In `MusicAppControllerTest`, replace `restorePlaybackSnapshotAutoScansLibraryWhenSnapshotExists` expectations with:

```kotlin
@Test
fun restorePlaybackSnapshotDoesNotAutoScanWhenLibraryIsEmpty(): Unit = runTest {
    val snapshotStore = InMemoryPlaybackSnapshotStore()
    val scanner = RecordingLocalMusicScanner()
    val controller = createController(
        localMusicScanner = scanner,
        playbackSnapshotStore = snapshotStore,
        controllerScope = backgroundScope,
    )
    snapshotStore.saveSnapshot(
        snapshot = PlaybackSnapshot(
            playbackState = PlaybackState(
                currentSongId = "fakeScanner:004",
                status = PlaybackStatus.Playing,
                positionMs = 24_000L,
                durationMs = 247_000L,
            ),
            queueState = QueueState(
                songIds = listOf("fakeScanner:004", "fakeScanner:002"),
                currentIndex = 0,
                playbackMode = PlaybackMode.LoopAll,
            ),
        ),
    )

    controller.restorePlaybackSnapshot()
    advanceUntilIdle()

    assertTrue(scanner.requests.isEmpty())
    assertNull(controller.uiState.currentSongId)
    assertFalse(controller.uiState.isPlaying)
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.restorePlaybackSnapshotDoesNotAutoScanWhenLibraryIsEmpty"
```

Expected: FAIL until controller auto-scan code is removed in Task 4 or this task.

- [ ] **Step 3: Inject persistent repository on Android**

In `AndroidPlaybackSession.kt`, add import:

```kotlin
import com.yanhao.kmpmusic.data.PersistentMusicLibraryRepository
```

Inside `bootstrap`, after `favoriteSongDao`:

```kotlin
val localSongDao = playbackDatabase.localSongDao()
```

When constructing `MusicAppController`, add:

```kotlin
musicLibraryRepository = PersistentMusicLibraryRepository(
    localSongDao = localSongDao,
    favoriteSongDao = favoriteSongDao,
),
```

- [ ] **Step 4: Run Android compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit Android injection**

```bash
git add composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSession.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "接入 Android 本地曲库持久化"
```

## Task 7: Full Regression and Cleanup

**Files:**
- Review: all files changed in Tasks 1-6
- Generated: `composeApp/schemas/com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase/2.json`

- [ ] **Step 1: Run shared tests**

Run:

```bash
./gradlew :composeApp:desktopTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run Android compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Check for forbidden auto-scan path**

Run:

```bash
rg -n "InitialScan|requestInitialLibraryRefreshForRestoreIfNeeded|scanLocalMusic\\(request = LocalMusicScanRequest.InitialScan" composeApp/src
```

Expected: no controller restore path calls `InitialScan`. It is acceptable for the `LocalMusicScanRequest.InitialScan` model declaration to remain if future platforms still need the request type.

- [ ] **Step 4: Check git status**

Run:

```bash
git status --short --branch
```

Expected: only intentional files from this implementation are modified or staged; unrelated existing docs and handoff files remain untouched.

- [ ] **Step 5: Confirm no extra commit is required**

Run:

```bash
git log --oneline -6
```

Expected: recent commits correspond to Tasks 1-6. Do not create an empty commit for verification-only work.

## Self-Review

- Spec coverage: covered Room table and migration in Task 1, repository API and persistent implementation in Tasks 2-3, cold-start no auto scan and on-demand loading in Tasks 4-6, queue snapshot regression in Task 4, Android injection in Task 6, full verification in Task 7.
- Red-flag scan: no open blanks, no empty edge-case instructions, and every code-changing step includes concrete code or exact replacement text.
- Type consistency: repository methods use `getHomePreview`, `getAllAvailableSongs`, `getLibraryStats`, and `applyScanResult(request, scanResult, likedSongIds)` consistently across interface, in-memory implementation, persistent implementation, and scan use case.
