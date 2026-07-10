package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.AddSongToLocalPlaylistResult
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.CreateLocalPlaylistWithSongResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.LocalPlaylist
import com.yanhao.kmpmusic.domain.model.LocalPlaylistCreateResult
import com.yanhao.kmpmusic.domain.model.LocalPlaylistSong
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.LocalPlaylistDao
import com.yanhao.kmpmusic.domain.persistence.LocalPlaylistEntity
import com.yanhao.kmpmusic.domain.persistence.LocalPlaylistSongDao
import com.yanhao.kmpmusic.domain.persistence.LocalPlaylistSongEntity
import com.yanhao.kmpmusic.domain.repository.LocalPlaylistRepository
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 验证本地自建歌单持久化仓库的最小闭环行为。
 */
class PersistentLocalPlaylistRepositoryTest {
    /**
     * 创建歌单会裁剪首尾空格并保留大小写与中间空格。
     */
    @Test
    fun createPlaylistTrimsOnlyOuterSpacesAndPersistsTimestamps(): Unit = runTest {
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture(nowValues = listOf(10L))
        val repository: LocalPlaylistRepository = fixture.createRepository()

        val result: LocalPlaylistCreateResult = repository.createPlaylist(name = "  My  List  ")

        val success: LocalPlaylistCreateResult.Success = assertIs<LocalPlaylistCreateResult.Success>(value = result)
        assertEquals(expected = "My  List", actual = success.playlist.name)
        assertEquals(expected = 10L, actual = success.playlist.createdAt)
        assertEquals(expected = 10L, actual = success.playlist.updatedAt)
    }

    /**
     * 创建判重按裁剪后的完全字符一致处理，不合并大小写和中间空格差异。
     */
    @Test
    fun createPlaylistRejectsBlankAndExactDuplicateOnly(): Unit = runTest {
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture()
        val repository: LocalPlaylistRepository = fixture.createRepository()

        assertIs<LocalPlaylistCreateResult.BlankName>(value = repository.createPlaylist(name = "   "))
        assertIs<LocalPlaylistCreateResult.Success>(value = repository.createPlaylist(name = "MyList"))
        assertIs<LocalPlaylistCreateResult.DuplicateName>(value = repository.createPlaylist(name = "  MyList  "))
        assertIs<LocalPlaylistCreateResult.Success>(value = repository.createPlaylist(name = "mylist"))
        assertIs<LocalPlaylistCreateResult.Success>(value = repository.createPlaylist(name = "My List"))
        assertIs<LocalPlaylistCreateResult.Success>(value = repository.createPlaylist(name = "my list"))
        assertEquals(expected = 4, actual = repository.getPlaylists().size)
    }

    /**
     * 默认歌单名从 1 开始递增，并复用创建判重的完全一致规则。
     */
    @Test
    fun nextDefaultPlaylistNameUsesFirstAvailablePositiveIndex(): Unit = runTest {
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture()
        val repository: LocalPlaylistRepository = fixture.createRepository()
        repository.createPlaylist(name = "默认歌单 1")
        repository.createPlaylist(name = "默认歌单 3")

        assertEquals(expected = "默认歌单 2", actual = repository.getNextDefaultPlaylistName())

        repository.createPlaylist(name = "默认歌单 2")

        assertEquals(expected = "默认歌单 4", actual = repository.getNextDefaultPlaylistName())
    }

    /**
     * 搜索忽略查询首尾空格和大小写，但不改变创建判重。
     */
    @Test
    fun searchPlaylistsIsCaseInsensitiveAndKeepsCreateRulesSeparate(): Unit = runTest {
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture()
        val repository: LocalPlaylistRepository = fixture.createRepository()
        repository.createPlaylist(name = "Road Trip")
        repository.createPlaylist(name = "road trip")
        repository.createPlaylist(name = "Work Focus")

        val searchResult: List<LocalPlaylist> = repository.searchPlaylists(query = "  ROAD  ")

        assertEquals(
            expected = listOf("road trip", "Road Trip"),
            actual = searchResult.map { playlist: LocalPlaylist -> playlist.name },
        )
        assertIs<LocalPlaylistCreateResult.DuplicateName>(value = repository.createPlaylist(name = "Road Trip"))
    }

    /**
     * 搜索中的 SQL 通配符必须按字面字符匹配，避免输入 % 或 _ 时命中全部歌单。
     */
    @Test
    fun searchPlaylistsTreatsSqlWildcardsAsLiteralCharacters(): Unit = runTest {
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture()
        val repository: LocalPlaylistRepository = fixture.createRepository()
        repository.createPlaylist(name = "100% Energy")
        repository.createPlaylist(name = "100 Percent Energy")
        repository.createPlaylist(name = "mix_down")
        repository.createPlaylist(name = "mixdown")

        assertEquals(
            expected = listOf("100% Energy"),
            actual = repository.searchPlaylists(query = "%").map { playlist: LocalPlaylist -> playlist.name },
        )
        assertEquals(
            expected = listOf("mix_down"),
            actual = repository.searchPlaylists(query = "_").map { playlist: LocalPlaylist -> playlist.name },
        )
    }

    /**
     * 新增歌曲关系写入加入时间和稳定顺序，并在真实新增时更新目标歌单更新时间。
     */
    @Test
    fun addSongToPlaylistPersistsStableOrderAndUpdatesPlaylistTimestamp(): Unit = runTest {
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture(
            nowValues = listOf(10L, 20L, 30L),
            availableSongs = listOf(song(id = "song-1"), song(id = "song-2")),
        )
        val repository: LocalPlaylistRepository = fixture.createRepository()
        val playlist: LocalPlaylist = assertIs<LocalPlaylistCreateResult.Success>(
            value = repository.createPlaylist(name = "通勤"),
        ).playlist

        assertIs<AddSongToLocalPlaylistResult.Added>(
            value = repository.addSongToPlaylist(
                playlistId = playlist.id,
                songId = "song-1",
            ),
        )
        assertIs<AddSongToLocalPlaylistResult.Added>(
            value = repository.addSongToPlaylist(
                playlistId = playlist.id,
                songId = "song-2",
            ),
        )

        val detail = repository.getPlaylistDetail(playlistId = playlist.id)!!
        assertEquals(expected = listOf("song-1", "song-2"), actual = detail.relations.map { relation -> relation.songId })
        assertEquals(expected = listOf(0, 1), actual = detail.relations.map { relation -> relation.sortOrder })
        assertEquals(expected = listOf(20L, 30L), actual = detail.relations.map { relation -> relation.addedAt })
        assertEquals(expected = 30L, actual = repository.getPlaylists().first().updatedAt)
    }

    /**
     * 重复添加同一首歌按成功处理，但不新增关系、不改顺序、不刷新更新时间。
     */
    @Test
    fun addSongToPlaylistIsIdempotentForExistingSongRelation(): Unit = runTest {
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture(
            nowValues = listOf(10L, 20L, 30L),
            availableSongs = listOf(song(id = "song-1")),
        )
        val repository: LocalPlaylistRepository = fixture.createRepository()
        val playlist: LocalPlaylist = assertIs<LocalPlaylistCreateResult.Success>(
            value = repository.createPlaylist(name = "夜跑"),
        ).playlist

        repository.addSongToPlaylist(
            playlistId = playlist.id,
            songId = "song-1",
        )
        val duplicateResult: AddSongToLocalPlaylistResult = repository.addSongToPlaylist(
            playlistId = playlist.id,
            songId = "song-1",
        )

        val duplicate: AddSongToLocalPlaylistResult.AlreadyExists =
            assertIs<AddSongToLocalPlaylistResult.AlreadyExists>(value = duplicateResult)
        assertEquals(expected = 0, actual = duplicate.relation.sortOrder)
        assertEquals(expected = 1, actual = repository.getPlaylistDetail(playlistId = playlist.id)!!.relations.size)
        assertEquals(expected = 20L, actual = repository.getPlaylists().first().updatedAt)
    }

    /**
     * 新建并加入当前歌曲必须走一个仓库级事务边界，供真实 Room 回滚失败写入。
     */
    @Test
    fun createPlaylistWithSongRunsInsideWriteTransaction(): Unit = runTest {
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture(
            availableSongs = listOf(song(id = "song-1")),
        )
        val repository: LocalPlaylistRepository = fixture.createRepository()

        val result: CreateLocalPlaylistWithSongResult = repository.createPlaylistWithSong(
            name = "雨天",
            songId = "song-1",
        )

        assertIs<CreateLocalPlaylistWithSongResult.Success>(value = result)
        assertEquals(expected = 1, actual = fixture.transactionCount)
        assertEquals(expected = listOf("song-1"), actual = repository.getPlaylistDetail(playlistId = repository.getPlaylists().first().id)!!.availableSongs.map { song -> song.id })
    }

    /**
     * 不可用歌曲不能进入新建并加入流程，且不会留下空歌单。
     */
    @Test
    fun createPlaylistWithUnavailableSongLeavesNoEmptyPlaylist(): Unit = runTest {
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture(availableSongs = emptyList())
        val repository: LocalPlaylistRepository = fixture.createRepository()

        val result: CreateLocalPlaylistWithSongResult = repository.createPlaylistWithSong(
            name = "失效歌曲",
            songId = "missing-song",
        )

        assertIs<CreateLocalPlaylistWithSongResult.SongUnavailable>(value = result)
        assertEquals(expected = emptyList(), actual = repository.getPlaylists())
    }

    /**
     * 关系写入失败时事务应回滚歌单插入，避免留下只创建但未加入歌曲的空歌单。
     */
    @Test
    fun createPlaylistWithSongRollsBackPlaylistWhenRelationInsertFails(): Unit = runTest {
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture(
            availableSongs = listOf(song(id = "song-1")),
            shouldFailRelationInsert = true,
        )
        val repository: LocalPlaylistRepository = fixture.createRepository()

        kotlin.test.assertFailsWith<IllegalStateException> {
            repository.createPlaylistWithSong(
                name = "失败回滚",
                songId = "song-1",
            )
        }

        assertEquals(expected = emptyList(), actual = repository.getPlaylists())
        assertNull(actual = repository.getPlaylistDetail(playlistId = "playlist-失败回滚-1"))
    }

    /**
     * 歌单详情只返回当前可用歌曲，同时保留已经保存的不可用歌曲关系。
     */
    @Test
    fun playlistDetailFiltersUnavailableSongsButKeepsRelations(): Unit = runTest {
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture(
            availableSongs = listOf(song(id = "song-1"), song(id = "song-2")),
        )
        val repository: LocalPlaylistRepository = fixture.createRepository()
        val playlist: LocalPlaylist = assertIs<CreateLocalPlaylistWithSongResult.Success>(
            value = repository.createPlaylistWithSong(
                name = "恢复测试",
                songId = "song-1",
            ),
        ).playlist
        repository.addSongToPlaylist(
            playlistId = playlist.id,
            songId = "song-2",
        )
        fixture.musicLibraryRepository.availableSongs = listOf(song(id = "song-2"))

        val detail = repository.getPlaylistDetail(playlistId = playlist.id)!!

        assertEquals(expected = listOf("song-1", "song-2"), actual = detail.relations.map { relation -> relation.songId })
        assertEquals(expected = listOf("song-2"), actual = detail.availableSongs.map { song -> song.id })
    }

    /**
     * 按更新时间倒序读取歌单，时间相同时按名称升序兜底。
     */
    @Test
    fun getPlaylistsSortsByUpdatedAtDescendingThenNameAscending(): Unit = runTest {
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture(nowValues = listOf(10L, 10L, 20L))
        val repository: LocalPlaylistRepository = fixture.createRepository()
        repository.createPlaylist(name = "B")
        repository.createPlaylist(name = "A")
        repository.createPlaylist(name = "C")

        assertEquals(
            expected = listOf("C", "A", "B"),
            actual = repository.getPlaylists().map { playlist: LocalPlaylist -> playlist.name },
        )
    }

    /**
     * 删除歌单只移除歌单元信息和关系，不影响曲库可用歌曲事实。
     */
    @Test
    fun deletePlaylistsRemovesMetadataAndRelationsOnly(): Unit = runTest {
        val song: Song = song(id = "song-kept")
        val fixture: LocalPlaylistRepositoryFixture = LocalPlaylistRepositoryFixture(
            availableSongs = listOf(song),
        )
        val repository: LocalPlaylistRepository = fixture.createRepository()
        val keepPlaylist: LocalPlaylist = assertIs<CreateLocalPlaylistWithSongResult.Success>(
            value = repository.createPlaylistWithSong(
                name = "保留",
                songId = song.id,
            ),
        ).playlist
        val deletePlaylist: LocalPlaylist = assertIs<CreateLocalPlaylistWithSongResult.Success>(
            value = repository.createPlaylistWithSong(
                name = "删除",
                songId = song.id,
            ),
        ).playlist

        val result = repository.deletePlaylists(playlistIds = setOf(deletePlaylist.id, "missing"))

        assertEquals(expected = 1, actual = result.deletedCount)
        assertEquals(expected = listOf(keepPlaylist.id), actual = repository.getPlaylists().map { playlist -> playlist.id })
        assertNull(actual = repository.getPlaylistDetail(playlistId = deletePlaylist.id))
        assertEquals(expected = listOf(song.id), actual = repository.getPlaylistDetail(playlistId = keepPlaylist.id)?.relations?.map { relation -> relation.songId })
        assertEquals(expected = listOf(song.id), actual = fixture.musicLibraryRepository.availableSongs.map { availableSong -> availableSong.id })
        assertEquals(expected = 3, actual = fixture.transactionCount)
    }

    private class LocalPlaylistRepositoryFixture(
        nowValues: List<Long> = (1L..100L).toList(),
        availableSongs: List<Song> = emptyList(),
        shouldFailRelationInsert: Boolean = false,
    ) {
        // 歌单 DAO 复用同一实例，模拟持久化表。
        private val playlistDao: FakeLocalPlaylistDao = FakeLocalPlaylistDao()

        // 歌单歌曲 DAO 复用同一实例，模拟关系表。
        private val playlistSongDao: FakeLocalPlaylistSongDao = FakeLocalPlaylistSongDao(
            shouldFailRelationInsert = shouldFailRelationInsert,
        )

        // 可变曲库仓库用来验证不可用歌曲过滤。
        val musicLibraryRepository: FakeMusicLibraryRepository = FakeMusicLibraryRepository(
            availableSongs = availableSongs,
        )

        // 当前测试已经进入的写事务次数。
        var transactionCount: Int = 0

        // 测试时钟序列，避免时间排序断言依赖真实时间。
        private val nowQueue: ArrayDeque<Long> = ArrayDeque(nowValues)

        /** 创建仓库实例。 */
        fun createRepository(): PersistentLocalPlaylistRepository {
            return PersistentLocalPlaylistRepository(
                playlistDao = playlistDao,
                playlistSongDao = playlistSongDao,
                musicLibraryRepository = musicLibraryRepository,
                runInWriteTransaction = { block: suspend () -> Unit ->
                    transactionCount += 1
                    val playlistRows: LinkedHashMap<String, LocalPlaylistEntity> = playlistDao.snapshotRows()
                    val relationRows: LinkedHashMap<Pair<String, String>, LocalPlaylistSongEntity> =
                        playlistSongDao.snapshotRows()
                    try {
                        block()
                    } catch (error: Throwable) {
                        playlistDao.restoreRows(rows = playlistRows)
                        playlistSongDao.restoreRows(rows = relationRows)
                        throw error
                    }
                },
                nowMillis = {
                    if (nowQueue.isEmpty()) {
                        999L
                    } else {
                        nowQueue.removeFirst()
                    }
                },
                idFactory = { name: String, createdAt: Long -> "playlist-$name-$createdAt" },
            )
        }
    }

    private class FakeLocalPlaylistDao : LocalPlaylistDao {
        // 用歌单 id 模拟元信息表。
        private val rows: LinkedHashMap<String, LocalPlaylistEntity> = linkedMapOf()

        /** 写入歌单元信息。 */
        override suspend fun insertPlaylist(entity: LocalPlaylistEntity) {
            rows[entity.id] = entity
        }

        /** 按 id 读取歌单。 */
        override suspend fun getPlaylistById(playlistId: String): LocalPlaylistEntity? {
            return rows[playlistId]
        }

        /** 按名称完全匹配读取歌单。 */
        override suspend fun getPlaylistByName(name: String): LocalPlaylistEntity? {
            return rows.values.firstOrNull { entity: LocalPlaylistEntity -> entity.name == name }
        }

        /** 读取全部歌单并模拟 SQL 排序。 */
        override suspend fun getPlaylists(): List<LocalPlaylistEntity> {
            return rows.values.sortedWith(
                compareByDescending<LocalPlaylistEntity> { entity: LocalPlaylistEntity -> entity.updatedAt }
                    .thenBy { entity: LocalPlaylistEntity -> entity.name },
            )
        }

        /** 按搜索规则读取歌单。 */
        override suspend fun searchPlaylists(escapedQuery: String): List<LocalPlaylistEntity> {
            val literalQuery: String = escapedQuery
                .replace(oldValue = "\\%", newValue = "%")
                .replace(oldValue = "\\_", newValue = "_")
                .replace(oldValue = "\\\\", newValue = "\\")
            return rows.values
                .filter { entity: LocalPlaylistEntity -> entity.name.lowercase().contains(other = literalQuery) }
                .sortedWith(
                    compareByDescending<LocalPlaylistEntity> { entity: LocalPlaylistEntity -> entity.updatedAt }
                        .thenBy { entity: LocalPlaylistEntity -> entity.name },
                )
        }

        /** 更新歌单最近变更时间。 */
        override suspend fun updatePlaylistUpdatedAt(
            playlistId: String,
            updatedAt: Long,
        ) {
            rows[playlistId]?.let { entity: LocalPlaylistEntity ->
                rows[playlistId] = entity.copy(updatedAt = updatedAt)
            }
        }

        /** 删除歌单元信息并返回真实删除行数。 */
        override suspend fun deletePlaylists(playlistIds: List<String>): Int {
            var deletedCount: Int = 0
            playlistIds.forEach { playlistId: String ->
                if (rows.remove(key = playlistId) != null) {
                    deletedCount += 1
                }
            }
            return deletedCount
        }

        /** 保存当前表快照，供事务失败测试回滚。 */
        fun snapshotRows(): LinkedHashMap<String, LocalPlaylistEntity> {
            return LinkedHashMap(rows)
        }

        /** 恢复表快照，模拟 Room 事务失败后的回滚。 */
        fun restoreRows(rows: LinkedHashMap<String, LocalPlaylistEntity>) {
            this.rows.clear()
            this.rows.putAll(from = rows)
        }
    }

    private class FakeLocalPlaylistSongDao(
        private val shouldFailRelationInsert: Boolean,
    ) : LocalPlaylistSongDao {
        // 用 playlistId/songId 组合模拟关系表唯一约束。
        private val rows: LinkedHashMap<Pair<String, String>, LocalPlaylistSongEntity> = linkedMapOf()

        /** 按歌单和歌曲读取关系。 */
        override suspend fun getRelation(
            playlistId: String,
            songId: String,
        ): LocalPlaylistSongEntity? {
            return rows[playlistId to songId]
        }

        /** 读取歌单全部关系。 */
        override suspend fun getRelations(playlistId: String): List<LocalPlaylistSongEntity> {
            return rows.values
                .filter { entity: LocalPlaylistSongEntity -> entity.playlistId == playlistId }
                .sortedBy { entity: LocalPlaylistSongEntity -> entity.sortOrder }
        }

        /** 读取下一个稳定顺序。 */
        override suspend fun getNextSortOrder(playlistId: String): Int? {
            return rows.values
                .filter { entity: LocalPlaylistSongEntity -> entity.playlistId == playlistId }
                .maxOfOrNull { entity: LocalPlaylistSongEntity -> entity.sortOrder + 1 }
        }

        /** 保存歌曲关系。 */
        override suspend fun insertRelation(entity: LocalPlaylistSongEntity) {
            if (shouldFailRelationInsert) {
                error(message = "模拟关系写入失败")
            }
            rows[entity.playlistId to entity.songId] = entity
        }

        /** 删除多个歌单下的关系。 */
        override suspend fun deleteRelationsForPlaylists(playlistIds: List<String>) {
            rows.keys.removeAll { key: Pair<String, String> -> key.first in playlistIds }
        }

        /** 保存当前关系表快照，供事务失败测试回滚。 */
        fun snapshotRows(): LinkedHashMap<Pair<String, String>, LocalPlaylistSongEntity> {
            return LinkedHashMap(rows)
        }

        /** 恢复关系表快照，模拟 Room 事务失败后的回滚。 */
        fun restoreRows(rows: LinkedHashMap<Pair<String, String>, LocalPlaylistSongEntity>) {
            this.rows.clear()
            this.rows.putAll(from = rows)
        }
    }

    private class FakeMusicLibraryRepository(
        var availableSongs: List<Song>,
    ) : MusicLibraryRepository {
        /** 返回空快照，本测试只关心按 id 读取歌曲。 */
        override fun getSnapshot(): com.yanhao.kmpmusic.domain.model.LibrarySnapshot {
            return com.yanhao.kmpmusic.domain.model.LibrarySnapshot.Empty
        }

        /** 返回首页预览，本测试不使用。 */
        override fun getHomePreview(limit: Int): List<Song> {
            return availableSongs.take(n = limit)
        }

        /** 返回当前可用歌曲。 */
        override fun getAllAvailableSongs(): List<Song> {
            return availableSongs
        }

        /** 只返回当前仍可用的请求歌曲。 */
        override fun getAvailableSongsByIds(songIds: List<String>): List<Song> {
            val requestedIds: Set<String> = songIds.toSet()
            return availableSongs.filter { song: Song -> requestedIds.contains(element = song.id) }
        }

        /** 返回空统计，本测试不使用。 */
        override fun getLibraryStats(): com.yanhao.kmpmusic.domain.model.LibraryStats {
            return com.yanhao.kmpmusic.domain.model.LibraryStats()
        }

        /** 本测试不覆盖扫描合并。 */
        override fun applyScanResult(
            request: com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest,
            scanResult: com.yanhao.kmpmusic.domain.model.LocalMusicScanResult,
            likedSongIds: Set<String>,
        ): com.yanhao.kmpmusic.domain.model.LibrarySnapshot {
            return com.yanhao.kmpmusic.domain.model.LibrarySnapshot.Empty
        }
    }

    private companion object {
        /** 创建可用歌曲样本。 */
        private fun song(id: String): Song {
            return Song(
                id = id,
                title = id,
                artist = "Artist",
                album = "Album",
                duration = "03:00",
                coverArt = CoverArt.HeroLocalMusic,
                isLiked = false,
                lastPlayed = "",
                quality = "本地",
                lyric = "",
                trackNumber = 1,
                sourceKind = LocalMusicSourceKind.FakeScanner,
                localUri = "fake://$id",
            )
        }
    }
}
