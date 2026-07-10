package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.AddSongToLocalPlaylistResult
import com.yanhao.kmpmusic.domain.model.CreateLocalPlaylistWithSongResult
import com.yanhao.kmpmusic.domain.model.LocalPlaylist
import com.yanhao.kmpmusic.domain.model.LocalPlaylistCreateResult
import com.yanhao.kmpmusic.domain.model.LocalPlaylistDeleteResult
import com.yanhao.kmpmusic.domain.model.LocalPlaylistDetail
import com.yanhao.kmpmusic.domain.model.LocalPlaylistSong
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.LocalPlaylistRepository
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository

/**
 * 默认演示环境使用的内存歌单仓库，真实平台入口会注入持久化实现。
 */
class InMemoryLocalPlaylistRepositoryImpl(
    private val musicLibraryRepository: MusicLibraryRepository,
    private val nowMillis: () -> Long = { currentTimeMillis() },
) : LocalPlaylistRepository {
    // 内存歌单表，按仓库契约模拟本地数据库元信息。
    private val playlists: MutableList<LocalPlaylist> = mutableListOf()

    // 内存关系表，保留稳定顺序和加入时间。
    private val relations: MutableList<LocalPlaylistSong> = mutableListOf()

    /** 创建歌单元信息，供默认演示入口和测试以外的非持久化宿主使用。 */
    override fun createPlaylist(name: String): LocalPlaylistCreateResult {
        val trimmedName: String = name.trim()
        val validationResult: LocalPlaylistCreateResult? = validateCreateName(trimmedName = trimmedName)
        if (validationResult != null) {
            return validationResult
        }
        val createdAt: Long = nowMillis()
        val playlist: LocalPlaylist = LocalPlaylist(
            id = "localPlaylist:$createdAt:${trimmedName.hashCode().toUInt().toString(radix = 16)}",
            name = trimmedName,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
        playlists += playlist
        return LocalPlaylistCreateResult.Success(playlist = playlist)
    }

    /** 新建歌单并加入歌曲必须保持单一状态变更，避免演示入口留下空歌单。 */
    override fun createPlaylistWithSong(
        name: String,
        songId: String,
    ): CreateLocalPlaylistWithSongResult {
        if (!hasAvailableSong(songId = songId)) {
            return CreateLocalPlaylistWithSongResult.SongUnavailable
        }
        val createResult: LocalPlaylistCreateResult = createPlaylist(name = name)
        return when (createResult) {
            LocalPlaylistCreateResult.BlankName -> CreateLocalPlaylistWithSongResult.BlankName
            LocalPlaylistCreateResult.DuplicateName -> CreateLocalPlaylistWithSongResult.DuplicateName
            is LocalPlaylistCreateResult.Success -> addFirstRelation(
                playlist = createResult.playlist,
                songId = songId,
            )
        }
    }

    /** 保存歌曲关系；重复添加按成功态处理但不刷新排序和更新时间。 */
    override fun addSongToPlaylist(
        playlistId: String,
        songId: String,
    ): AddSongToLocalPlaylistResult {
        val playlist: LocalPlaylist = playlists.firstOrNull { item: LocalPlaylist -> item.id == playlistId }
            ?: return AddSongToLocalPlaylistResult.PlaylistNotFound
        val existingRelation: LocalPlaylistSong? = relations.firstOrNull { relation: LocalPlaylistSong ->
            relation.playlistId == playlistId && relation.songId == songId
        }
        if (existingRelation != null) {
            return AddSongToLocalPlaylistResult.AlreadyExists(relation = existingRelation)
        }
        if (!hasAvailableSong(songId = songId)) {
            return AddSongToLocalPlaylistResult.SongUnavailable
        }
        val addedAt: Long = nowMillis()
        val relation: LocalPlaylistSong = LocalPlaylistSong(
            playlistId = playlistId,
            songId = songId,
            addedAt = addedAt,
            sortOrder = relations.count { item: LocalPlaylistSong -> item.playlistId == playlistId },
        )
        relations += relation
        val playlistIndex: Int = playlists.indexOfFirst { item: LocalPlaylist -> item.id == playlist.id }
        if (playlistIndex >= 0) {
            playlists[playlistIndex] = playlist.copy(updatedAt = addedAt)
        }
        return AddSongToLocalPlaylistResult.Added(relation = relation)
    }

    /** 删除内存歌单和关系，保持演示入口与持久化仓库同一删除边界。 */
    override fun deletePlaylists(playlistIds: Set<String>): LocalPlaylistDeleteResult {
        val normalizedIds: Set<String> = playlistIds.filter { playlistId: String -> playlistId.isNotBlank() }.toSet()
        val beforeCount: Int = playlists.size
        playlists.removeAll { playlist: LocalPlaylist -> playlist.id in normalizedIds }
        relations.removeAll { relation: LocalPlaylistSong -> relation.playlistId in normalizedIds }
        return LocalPlaylistDeleteResult(deletedCount = beforeCount - playlists.size)
    }

    /** 读取全部歌单，排序规则对齐持久化仓库。 */
    override fun getPlaylists(): List<LocalPlaylist> {
        return playlists.sortedWith(
            compareByDescending<LocalPlaylist> { playlist: LocalPlaylist -> playlist.updatedAt }
                .thenBy { playlist: LocalPlaylist -> playlist.name },
        )
    }

    /** 搜索只影响查询，不改变创建判重规则。 */
    override fun searchPlaylists(query: String): List<LocalPlaylist> {
        val normalizedQuery: String = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) {
            return getPlaylists()
        }
        return getPlaylists().filter { playlist: LocalPlaylist ->
            playlist.name.lowercase().contains(other = normalizedQuery)
        }
    }

    /** 生成第一个未被完全一致占用的默认歌单名称。 */
    override fun getNextDefaultPlaylistName(): String {
        var index: Int = 1
        while (true) {
            val candidateName: String = "默认歌单 $index"
            if (playlists.none { playlist: LocalPlaylist -> playlist.name == candidateName }) {
                return candidateName
            }
            index += 1
        }
    }

    /** 读取歌单详情并过滤当前曲库不可用歌曲。 */
    override fun getPlaylistDetail(playlistId: String): LocalPlaylistDetail? {
        val playlist: LocalPlaylist = playlists.firstOrNull { item: LocalPlaylist -> item.id == playlistId }
            ?: return null
        val playlistRelations: List<LocalPlaylistSong> = relations
            .filter { relation: LocalPlaylistSong -> relation.playlistId == playlistId }
            .sortedBy { relation: LocalPlaylistSong -> relation.sortOrder }
        val songsById: Map<String, Song> = musicLibraryRepository.getAvailableSongsByIds(
            songIds = playlistRelations.map { relation: LocalPlaylistSong -> relation.songId },
        ).associateBy { song -> song.id }
        return LocalPlaylistDetail(
            playlist = playlist,
            relations = playlistRelations,
            availableSongs = playlistRelations.mapNotNull { relation: LocalPlaylistSong -> songsById[relation.songId] },
        )
    }

    // 复用持久化仓库的名称规则，避免 common 默认入口行为分叉。
    private fun validateCreateName(trimmedName: String): LocalPlaylistCreateResult? {
        if (trimmedName.isBlank()) {
            return LocalPlaylistCreateResult.BlankName
        }
        if (playlists.any { playlist: LocalPlaylist -> playlist.name == trimmedName }) {
            return LocalPlaylistCreateResult.DuplicateName
        }
        return null
    }

    // 原子创建成功后写入第一条关系。
    private fun addFirstRelation(
        playlist: LocalPlaylist,
        songId: String,
    ): CreateLocalPlaylistWithSongResult {
        val relation: LocalPlaylistSong = LocalPlaylistSong(
            playlistId = playlist.id,
            songId = songId,
            addedAt = playlist.createdAt,
            sortOrder = 0,
        )
        relations += relation
        return CreateLocalPlaylistWithSongResult.Success(
            playlist = playlist,
            relation = relation,
        )
    }

    // 只允许当前应用曲库能解析出的歌曲进入歌单关系。
    private fun hasAvailableSong(songId: String): Boolean {
        return musicLibraryRepository.getAvailableSongsByIds(songIds = listOf(songId)).isNotEmpty()
    }
}
