package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
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
    // 最近一次扫描状态，供未重新扫描时恢复快照展示。
    private var lastScanState: LocalMusicScanState = LocalMusicScanState.Idle

    // 最近一次扫描产生的问题列表，避免重新读取快照时丢失错误上下文。
    private var lastProblems: List<LocalMusicProblem> = emptyList()

    /** 同步返回当前曲库快照，兼容旧调用路径。 */
    override fun getSnapshot(): LibrarySnapshot = runBlocking {
        val songs: List<Song> = readAllSongs()
        buildSnapshot(
            songs = songs,
            scanState = lastScanState,
        )
    }

    /** 返回首页预览歌曲，只读取持久层要求的条数。 */
    override fun getHomePreview(limit: Int): List<Song> = runBlocking {
        mapEntities(
            entities = localSongDao.getHomePreview(limit = limit),
            likedSongIds = favoriteSongDao.getFavoriteSongIds().toSet(),
        )
    }

    /** 返回全部可用歌曲，供本地页、搜索和详情读取。 */
    override fun getAllAvailableSongs(): List<Song> = runBlocking {
        readAllSongs()
    }

    /** 返回当前可用曲库统计值。 */
    override fun getLibraryStats(): LibraryStats = runBlocking {
        readLibraryStats()
    }

    /** 合并扫描结果并把缺失歌曲在同来源内标记为不可用。 */
    override fun applyScanResult(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot = runBlocking {
        val coveredSources: Set<LocalMusicSourceKind> = resolveCoveredSources(
            request = request,
            scanResult = scanResult,
        )
        val discoveredEntities: List<LocalSongEntity> = scanResult.discovered
            .filter { metadata: MusicFileMetadata -> metadata.localUri.isNotBlank() }
            .map { metadata: MusicFileMetadata -> metadata.toEntity(lastScannedAt = scanResult.completedAt) }
        val previousIdsBySource: Map<LocalMusicSourceKind, Set<String>> = coveredSources.associateWith { sourceKind: LocalMusicSourceKind ->
            localSongDao.getAvailableSongIdsBySource(sourceKind = sourceKind.value).toSet()
        }
        localSongDao.upsertSongs(songs = discoveredEntities)
        coveredSources.forEach { sourceKind: LocalMusicSourceKind ->
            val discoveredIds: Set<String> = discoveredEntities
                .filter { entity: LocalSongEntity -> entity.sourceKind == sourceKind.value }
                .map { entity: LocalSongEntity -> entity.id }
                .toSet()
            val missingIds: Set<String> = previousIdsBySource.getValue(sourceKind) - discoveredIds
            if (missingIds.isNotEmpty()) {
                localSongDao.markUnavailable(
                    sourceKind = sourceKind.value,
                    songIds = missingIds.toList(),
                )
            }
        }
        val summary: LocalMusicLastScanSummary = LocalMusicLastScanSummary(
            addedCount = discoveredEntities.count { entity: LocalSongEntity ->
                coveredSources.any { sourceKind: LocalMusicSourceKind ->
                    previousIdsBySource[sourceKind]?.contains(element = entity.id) == false
                }
            },
            updatedCount = discoveredEntities.count { entity: LocalSongEntity ->
                coveredSources.any { sourceKind: LocalMusicSourceKind ->
                    previousIdsBySource[sourceKind]?.contains(element = entity.id) == true
                }
            },
            removedCount = coveredSources.sumOf { sourceKind: LocalMusicSourceKind ->
                val discoveredIds: Set<String> = discoveredEntities
                    .filter { entity: LocalSongEntity -> entity.sourceKind == sourceKind.value }
                    .map { entity: LocalSongEntity -> entity.id }
                    .toSet()
                (previousIdsBySource.getValue(sourceKind) - discoveredIds).size
            },
            problemCount = scanResult.failed.size,
            completedAt = scanResult.completedAt,
        )
        lastScanState = LocalMusicScanState.Done(summary = summary)
        lastProblems = scanResult.failed
        buildSnapshot(
            songs = readAllSongs(likedSongIds = likedSongIds),
            scanState = lastScanState,
        )
    }

    /** 统一读取统计值，避免调用方重复推导。 */
    private suspend fun readLibraryStats(): LibraryStats {
        return LibraryStats(
            songCount = localSongDao.countAvailableSongs(),
            albumCount = localSongDao.countAvailableAlbums(),
            artistCount = localSongDao.countAvailableArtists(),
        )
    }

    /** 读取全部可用歌曲并叠加当前收藏状态。 */
    private suspend fun readAllSongs(
        likedSongIds: Set<String>? = null,
    ): List<Song> {
        val resolvedLikedSongIds: Set<String> = likedSongIds ?: favoriteSongDao.getFavoriteSongIds().toSet()
        return mapEntities(
            entities = localSongDao.getAllAvailableSongs(),
            likedSongIds = resolvedLikedSongIds,
        )
    }

    /** 把持久化实体映射为 UI 统一消费的 [Song] 模型。 */
    private fun mapEntities(
        entities: List<LocalSongEntity>,
        likedSongIds: Set<String>,
    ): List<Song> {
        return entities.mapIndexed { index: Int, entity: LocalSongEntity ->
            entity.toSong(
                index = index,
                likedSongIds = likedSongIds,
            )
        }
    }

    /** 构建包含统计、专辑、歌手和扫描态的完整曲库快照。 */
    private suspend fun buildSnapshot(
        songs: List<Song>,
        scanState: LocalMusicScanState,
    ): LibrarySnapshot {
        val albums: List<Album> = buildAlbums(songs = songs)
        val artists: List<Artist> = buildArtists(songs = songs)
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

    /** 根据请求和结果推导本轮覆盖的来源集合，避免跨来源误下线。 */
    private fun resolveCoveredSources(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
    ): Set<LocalMusicSourceKind> {
        val fromRequest: Set<LocalMusicSourceKind> = when (request) {
            is LocalMusicScanRequest.Source -> setOf(element = request.sourceKind)
            LocalMusicScanRequest.InitialScan,
            LocalMusicScanRequest.Refresh,
            -> emptySet()
        }
        val fromSummaries: Set<LocalMusicSourceKind> = scanResult.sourceSummaries
            .map { source -> source.sourceKind }
            .toSet()
        val fromDiscovered: Set<LocalMusicSourceKind> = scanResult.discovered
            .map { metadata: MusicFileMetadata -> metadata.sourceKind }
            .toSet()
        return fromRequest + fromSummaries + fromDiscovered
    }

    /** 把扫描元数据转换为可覆盖写入数据库的本地歌曲实体。 */
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

    /** 把本地歌曲实体映射成 UI 展示和播放共用的 [Song]。 */
    private fun LocalSongEntity.toSong(
        index: Int,
        likedSongIds: Set<String>,
    ): Song {
        val safeTitle: String = title?.takeIf { value: String -> value.isNotBlank() }
            ?: fileName.substringBeforeLast(
                delimiter = ".",
                missingDelimiterValue = fileName,
            )
        val safeArtist: String = artist?.takeIf { value: String -> value.isNotBlank() } ?: "未知歌手"
        val safeAlbum: String = album?.takeIf { value: String -> value.isNotBlank() } ?: "未知专辑"
        return Song(
            id = id,
            title = safeTitle,
            artist = safeArtist,
            album = safeAlbum,
            duration = formatDuration(durationMs = durationMs),
            coverArt = CoverArt.entries.firstOrNull { cover: CoverArt -> cover.name == coverArt }
                ?: CoverArt.HeroLocalMusic,
            isLiked = likedSongIds.contains(element = id),
            lastPlayed = "未播放",
            quality = formatQuality(mimeType = mimeType),
            lyric = "来自${sourceKind}的本地音频。",
            trackNumber = index + 1,
            durationMs = durationMs,
            sourceId = sourceId,
            sourceKind = LocalMusicSourceKind.entries.firstOrNull { kind: LocalMusicSourceKind ->
                kind.value == sourceKind
            } ?: LocalMusicSourceKind.FakeScanner,
            localUri = localUri,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            modifiedAt = modifiedAt,
        )
    }

    /** 按专辑聚合歌曲，保持首页和详情页读取一致的分组规则。 */
    private fun buildAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { song: Song -> song.album.trim().lowercase() }
            .values
            .map { albumSongs: List<Song> ->
                val firstSong: Song = albumSongs.first()
                Album(
                    id = "album:${firstSong.album.trim().lowercase()}",
                    title = firstSong.album,
                    artist = firstSong.artist,
                    songCount = albumSongs.size,
                    coverArt = firstSong.coverArt,
                    mood = "本地音乐",
                    year = "本地",
                )
            }
            .sortedBy { album: Album -> album.title.lowercase() }
    }

    /** 按歌手聚合歌曲，保证统计和二级页读到同一来源事实。 */
    private fun buildArtists(songs: List<Song>): List<Artist> {
        return songs.groupBy { song: Song -> song.artist.trim().lowercase() }
            .values
            .map { artistSongs: List<Song> ->
                val firstSong: Song = artistSongs.first()
                Artist(
                    id = "artist:${firstSong.artist.trim().lowercase()}",
                    name = firstSong.artist,
                    songCount = artistSongs.size,
                    coverArt = firstSong.coverArt,
                    tag = "本地音乐",
                )
            }
            .sortedBy { artist: Artist -> artist.name.lowercase() }
    }

    /** 生成 UI 需要的分秒时长文案。 */
    private fun formatDuration(durationMs: Long?): String {
        if (durationMs == null || durationMs <= 0L) {
            return "--:--"
        }
        val totalSeconds: Long = durationMs / 1_000L
        return "${totalSeconds / 60L}:${(totalSeconds % 60L).toString().padStart(length = 2, padChar = '0')}"
    }

    /** 从 MIME 类型派生简短音质标签，缺失时回退为通用本地音频。 */
    private fun formatQuality(mimeType: String?): String {
        val suffix: String = mimeType?.substringAfterLast(delimiter = "/")?.uppercase().orEmpty()
        return if (suffix.isBlank()) {
            "本地音频"
        } else {
            "本地 $suffix"
        }
    }
}
