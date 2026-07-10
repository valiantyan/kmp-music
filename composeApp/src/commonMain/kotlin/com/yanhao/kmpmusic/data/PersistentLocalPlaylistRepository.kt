package com.yanhao.kmpmusic.data

import androidx.room3.withWriteTransaction
import com.yanhao.kmpmusic.domain.model.AddSongToLocalPlaylistResult
import com.yanhao.kmpmusic.domain.model.CreateLocalPlaylistWithSongResult
import com.yanhao.kmpmusic.domain.model.LocalPlaylist
import com.yanhao.kmpmusic.domain.model.LocalPlaylistCreateResult
import com.yanhao.kmpmusic.domain.model.LocalPlaylistDetail
import com.yanhao.kmpmusic.domain.model.LocalPlaylistSong
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.LocalPlaylistDao
import com.yanhao.kmpmusic.domain.persistence.LocalPlaylistEntity
import com.yanhao.kmpmusic.domain.persistence.LocalPlaylistSongDao
import com.yanhao.kmpmusic.domain.persistence.LocalPlaylistSongEntity
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.domain.repository.LocalPlaylistRepository
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import kotlinx.coroutines.runBlocking

/**
 * 基于 Room 的本地自建歌单仓库，只保存歌单元信息和歌曲标识关系。
 */
class PersistentLocalPlaylistRepository(
    private val playlistDao: LocalPlaylistDao,
    private val playlistSongDao: LocalPlaylistSongDao,
    private val musicLibraryRepository: MusicLibraryRepository,
    private val runInWriteTransaction: suspend (suspend () -> Unit) -> Unit = { block -> block() },
    private val nowMillis: () -> Long = { currentTimeMillis() },
    private val idFactory: (name: String, createdAt: Long) -> String = { name: String, createdAt: Long ->
        "localPlaylist:$createdAt:${name.hashCode().toUInt().toString(radix = 16)}"
    },
) : LocalPlaylistRepository {
    /** 创建歌单元信息，并按产品规则校验名称。 */
    override fun createPlaylist(name: String): LocalPlaylistCreateResult = runBlocking {
        val trimmedName: String = name.trim()
        val validationResult: LocalPlaylistCreateResult? = validateCreateName(trimmedName = trimmedName)
        if (validationResult != null) {
            return@runBlocking validationResult
        }
        val createdAt: Long = nowMillis()
        val entity: LocalPlaylistEntity = LocalPlaylistEntity(
            id = idFactory(trimmedName, createdAt),
            name = trimmedName,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
        playlistDao.insertPlaylist(entity = entity)
        LocalPlaylistCreateResult.Success(playlist = entity.toDomain())
    }

    /** 新建歌单并加入当前歌曲，避免后续 UI 流程留下空歌单。 */
    override fun createPlaylistWithSong(
        name: String,
        songId: String,
    ): CreateLocalPlaylistWithSongResult = runBlocking {
        if (!hasAvailableSong(songId = songId)) {
            return@runBlocking CreateLocalPlaylistWithSongResult.SongUnavailable
        }
        val trimmedName: String = name.trim()
        val validationResult: CreateLocalPlaylistWithSongResult? = validateCreateNameForAtomicFlow(
            trimmedName = trimmedName,
        )
        if (validationResult != null) {
            return@runBlocking validationResult
        }
        var savedPlaylist: LocalPlaylist? = null
        var savedRelation: LocalPlaylistSong? = null
        runInWriteTransaction {
            val createdAt: Long = nowMillis()
            val playlistEntity: LocalPlaylistEntity = LocalPlaylistEntity(
                id = idFactory(trimmedName, createdAt),
                name = trimmedName,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
            val relationEntity: LocalPlaylistSongEntity = LocalPlaylistSongEntity(
                playlistId = playlistEntity.id,
                songId = songId,
                addedAt = createdAt,
                sortOrder = 0,
            )
            playlistDao.insertPlaylist(entity = playlistEntity)
            playlistSongDao.insertRelation(entity = relationEntity)
            savedPlaylist = playlistEntity.toDomain()
            savedRelation = relationEntity.toDomain()
        }
        CreateLocalPlaylistWithSongResult.Success(
            playlist = requireNotNull(savedPlaylist),
            relation = requireNotNull(savedRelation),
        )
    }

    /** 保存歌曲关系；重复添加返回成功但不刷新歌单更新时间。 */
    override fun addSongToPlaylist(
        playlistId: String,
        songId: String,
    ): AddSongToLocalPlaylistResult = runBlocking {
        val playlist: LocalPlaylistEntity = playlistDao.getPlaylistById(playlistId = playlistId)
            ?: return@runBlocking AddSongToLocalPlaylistResult.PlaylistNotFound
        val existingRelation: LocalPlaylistSongEntity? = playlistSongDao.getRelation(
            playlistId = playlistId,
            songId = songId,
        )
        if (existingRelation != null) {
            return@runBlocking AddSongToLocalPlaylistResult.AlreadyExists(
                relation = existingRelation.toDomain(),
            )
        }
        if (!hasAvailableSong(songId = songId)) {
            return@runBlocking AddSongToLocalPlaylistResult.SongUnavailable
        }
        val addedAt: Long = nowMillis()
        val relation: LocalPlaylistSongEntity = LocalPlaylistSongEntity(
            playlistId = playlist.id,
            songId = songId,
            addedAt = addedAt,
            sortOrder = playlistSongDao.getNextSortOrder(playlistId = playlistId) ?: 0,
        )
        runInWriteTransaction {
            playlistSongDao.insertRelation(entity = relation)
            playlistDao.updatePlaylistUpdatedAt(
                playlistId = playlistId,
                updatedAt = addedAt,
            )
        }
        AddSongToLocalPlaylistResult.Added(relation = relation.toDomain())
    }

    /** 读取歌单列表，排序由 DAO 固化。 */
    override fun getPlaylists(): List<LocalPlaylist> = runBlocking {
        playlistDao.getPlaylists().map { entity: LocalPlaylistEntity -> entity.toDomain() }
    }

    /** 搜索歌单名称；空查询回退到全部歌单。 */
    override fun searchPlaylists(query: String): List<LocalPlaylist> = runBlocking {
        val normalizedQuery: String = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) {
            return@runBlocking playlistDao.getPlaylists().map { entity: LocalPlaylistEntity -> entity.toDomain() }
        }
        playlistDao.searchPlaylists(escapedQuery = normalizedQuery.toEscapedLikeQuery())
            .map { entity: LocalPlaylistEntity -> entity.toDomain() }
    }

    /** 生成第一个未被完全一致占用的默认歌单名称。 */
    override fun getNextDefaultPlaylistName(): String = runBlocking {
        var index: Int = 1
        while (true) {
            val candidateName: String = "默认歌单 $index"
            if (playlistDao.getPlaylistByName(name = candidateName) == null) {
                return@runBlocking candidateName
            }
            index += 1
        }
        error(message = "unreachable")
    }

    /** 读取详情时保留全部关系，但当前可播放歌曲只来自曲库可用集合。 */
    override fun getPlaylistDetail(playlistId: String): LocalPlaylistDetail? = runBlocking {
        val playlist: LocalPlaylistEntity = playlistDao.getPlaylistById(playlistId = playlistId)
            ?: return@runBlocking null
        val relationEntities: List<LocalPlaylistSongEntity> = playlistSongDao.getRelations(playlistId = playlistId)
        val availableSongs: List<Song> = resolveAvailableSongsInRelationOrder(relationEntities = relationEntities)
        LocalPlaylistDetail(
            playlist = playlist.toDomain(),
            relations = relationEntities.map { entity: LocalPlaylistSongEntity -> entity.toDomain() },
            availableSongs = availableSongs,
        )
    }

    // 校验创建名称，完全一致判重必须保留大小写和中间空格差异。
    private suspend fun validateCreateName(trimmedName: String): LocalPlaylistCreateResult? {
        if (trimmedName.isBlank()) {
            return LocalPlaylistCreateResult.BlankName
        }
        if (playlistDao.getPlaylistByName(name = trimmedName) != null) {
            return LocalPlaylistCreateResult.DuplicateName
        }
        return null
    }

    // 为原子流程复用同一名称规则，但返回该流程自己的结果类型。
    private suspend fun validateCreateNameForAtomicFlow(
        trimmedName: String,
    ): CreateLocalPlaylistWithSongResult? {
        return when (validateCreateName(trimmedName = trimmedName)) {
            LocalPlaylistCreateResult.BlankName -> CreateLocalPlaylistWithSongResult.BlankName
            LocalPlaylistCreateResult.DuplicateName -> CreateLocalPlaylistWithSongResult.DuplicateName
            null -> null
            is LocalPlaylistCreateResult.Success -> null
        }
    }

    // 歌单关系只接受当前曲库仍可解析出的歌曲。
    private fun hasAvailableSong(songId: String): Boolean {
        return musicLibraryRepository.getAvailableSongsByIds(songIds = listOf(songId)).isNotEmpty()
    }

    // 曲库按自己的排序返回歌曲，这里恢复为歌单关系稳定顺序。
    private fun resolveAvailableSongsInRelationOrder(
        relationEntities: List<LocalPlaylistSongEntity>,
    ): List<Song> {
        val songIds: List<String> = relationEntities.map { entity: LocalPlaylistSongEntity -> entity.songId }
        val songsById: Map<String, Song> = musicLibraryRepository.getAvailableSongsByIds(songIds = songIds)
            .associateBy { song: Song -> song.id }
        return songIds.mapNotNull { songId: String -> songsById[songId] }
    }

    // SQLite LIKE 中的通配符必须按用户输入字面量处理。
    private fun String.toEscapedLikeQuery(): String {
        return replace(oldValue = "\\", newValue = "\\\\")
            .replace(oldValue = "%", newValue = "\\%")
            .replace(oldValue = "_", newValue = "\\_")
    }

    companion object {
        /**
         * 从 [PlaybackDatabase] 创建歌单仓库，确保新建并加入、添加歌曲走同一 Room 写事务。
         */
        fun create(
            playbackDatabase: PlaybackDatabase,
            musicLibraryRepository: MusicLibraryRepository,
            nowMillis: () -> Long = { currentTimeMillis() },
        ): PersistentLocalPlaylistRepository {
            return PersistentLocalPlaylistRepository(
                playlistDao = playbackDatabase.localPlaylistDao(),
                playlistSongDao = playbackDatabase.localPlaylistSongDao(),
                musicLibraryRepository = musicLibraryRepository,
                runInWriteTransaction = { block: suspend () -> Unit ->
                    playbackDatabase.withWriteTransaction {
                        block()
                    }
                },
                nowMillis = nowMillis,
            )
        }
    }
}
