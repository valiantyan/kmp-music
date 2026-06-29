package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 合并扫描结果的请求对象，集中传入旧快照和用户状态。
 */
data class MergeLocalMusicScanResultRequest(
    val previousSnapshot: LibrarySnapshot,
    val scanResult: LocalMusicScanResult,
    val likedSongIds: Set<String>,
)

/**
 * 将平台扫描结果合并为 UI 唯一曲库快照。
 */
interface MergeLocalMusicScanResultUseCase {
    /**
     * 按 sourceKey 合并歌曲、过滤失败项并重新聚合专辑歌手。
     */
    operator fun invoke(request: MergeLocalMusicScanResultRequest): LibrarySnapshot
}

/**
 * 扫描合并实现，保持稳定 id 并把排序规则收敛到 domain 层。
 */
class MergeLocalMusicScanResultUseCaseImpl : MergeLocalMusicScanResultUseCase {
    /** 合并一次扫描结果，返回页面可直接消费的曲库快照。 */
    override operator fun invoke(request: MergeLocalMusicScanResultRequest): LibrarySnapshot {
        val removedSourceKeys: Set<String> = request.scanResult.removedSourceKeys
        val previousSongsById: Map<String, Song> = request.previousSnapshot.songs.associateBy { song -> song.id }
        val discoveredSongs: List<Song> = request.scanResult.discovered
            .filter { metadata -> metadata.localUri.isNotBlank() }
            .filterNot { metadata -> removedSourceKeys.contains(element = metadata.sourceKey) }
            .mapIndexed { index, metadata ->
                metadata.toSong(
                    index = index,
                    previousSong = previousSongsById[metadata.sourceKey],
                    likedSongIds = request.likedSongIds,
                )
            }
            .sortedWith(
                comparator = compareByDescending<Song> { song -> song.modifiedAt ?: Long.MIN_VALUE }
                    .thenBy { song -> song.title.lowercase() },
            )
        val albums: List<Album> = buildAlbums(songs = discoveredSongs)
        val artists: List<Artist> = buildArtists(songs = discoveredSongs)
        val summary: LocalMusicLastScanSummary = LocalMusicLastScanSummary(
            addedCount = discoveredSongs.count { song -> !previousSongsById.containsKey(key = song.id) },
            updatedCount = discoveredSongs.count { song -> previousSongsById.containsKey(key = song.id) },
            removedCount = removedSourceKeys.size,
            problemCount = request.scanResult.failed.size,
            completedAt = request.scanResult.completedAt,
        )
        return LibrarySnapshot(
            songs = discoveredSongs,
            albums = albums,
            artists = artists,
            stats = LibraryStats(
                songCount = discoveredSongs.size,
                albumCount = albums.size,
                artistCount = artists.size,
            ),
            sources = request.scanResult.sourceSummaries,
            scanState = LocalMusicScanState.Done(summary = summary),
            lastScanSummary = summary,
            problems = request.scanResult.failed,
        )
    }

    // 将 scanner 元数据映射为 UI 歌曲模型，同时应用元数据兜底。
    private fun MusicFileMetadata.toSong(
        index: Int,
        previousSong: Song?,
        likedSongIds: Set<String>,
    ): Song {
        val safeTitle: String = title?.takeIf { value -> value.isNotBlank() } ?: fileName.substringBeforeLast(
            delimiter = ".",
            missingDelimiterValue = fileName,
        )
        val safeArtist: String = artist?.takeIf { value -> value.isNotBlank() } ?: "未知歌手"
        val safeAlbum: String = album?.takeIf { value -> value.isNotBlank() } ?: "未知专辑"
        val songId: String = sourceKey
        return Song(
            id = songId,
            title = safeTitle,
            artist = safeArtist,
            album = safeAlbum,
            duration = formatDuration(durationMs = durationMs),
            coverArt = normalizedCoverArt(),
            coverImageUri = coverImageUri,
            isLiked = likedSongIds.contains(element = songId) || previousSong?.isLiked == true,
            lastPlayed = previousSong?.lastPlayed ?: "未播放",
            quality = formatQuality(mimeType = mimeType),
            lyric = previousSong?.lyric ?: "来自${sourceKind.displayName}的本地音频。",
            trackNumber = previousSong?.trackNumber ?: index + 1,
            durationMs = durationMs,
            sourceId = sourceId,
            sourceKind = sourceKind,
            localUri = localUri,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            modifiedAt = modifiedAt,
        )
    }

    // 扫描歌曲没有真实封面时使用明确占位图，不复用看起来像专辑封面的原型资源。
    private fun MusicFileMetadata.normalizedCoverArt(): CoverArt {
        if (coverImageUri.isNullOrBlank()) {
            return CoverArt.HeroLocalMusic
        }
        return coverArt
    }

    // 从歌曲按专辑名称聚合首页和详情页共用的专辑模型。
    private fun buildAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { song -> normalizeKey(value = song.album) }
            .values
            .map { albumSongs ->
                val firstSong: Song = albumSongs.first()
                Album(
                    id = "album:${normalizeKey(value = firstSong.album)}",
                    title = firstSong.album,
                    artist = firstSong.artist,
                    songCount = albumSongs.size,
                    coverArt = firstSong.coverArt,
                    coverImageUri = firstSong.coverImageUri,
                    mood = "本地音乐",
                    year = "本地",
                )
            }
            .sortedBy { album -> album.title.lowercase() }
    }

    // 从歌曲按歌手名称聚合收藏页、搜索页和详情页共用的歌手模型。
    private fun buildArtists(songs: List<Song>): List<Artist> {
        return songs.groupBy { song -> normalizeKey(value = song.artist) }
            .values
            .map { artistSongs ->
                val firstSong: Song = artistSongs.first()
                Artist(
                    id = "artist:${normalizeKey(value = firstSong.artist)}",
                    name = firstSong.artist,
                    songCount = artistSongs.size,
                    coverArt = firstSong.coverArt,
                    coverImageUri = firstSong.coverImageUri,
                    tag = "本地音乐",
                )
            }
            .sortedBy { artist -> artist.name.lowercase() }
    }

    // 统一聚合 key，避免大小写和空白造成重复专辑或歌手。
    private fun normalizeKey(value: String): String {
        return value.trim().lowercase()
    }

    // 将毫秒时长转换为 UI 已有的 m:ss 文案。
    private fun formatDuration(durationMs: Long?): String {
        if (durationMs == null || durationMs <= 0L) {
            return "--:--"
        }
        val totalSeconds: Long = durationMs / 1_000L
        val minutes: Long = totalSeconds / 60L
        val seconds: Long = totalSeconds % 60L
        return "$minutes:${seconds.toString().padStart(length = 2, padChar = '0')}"
    }

    // 从 MIME 类型生成轻量音质标签，缺失时不猜测文件编码。
    private fun formatQuality(mimeType: String?): String {
        val suffix: String = mimeType?.substringAfterLast(delimiter = "/")?.uppercase().orEmpty()
        if (suffix.isBlank()) {
            return "本地音频"
        }
        return "本地 $suffix"
    }
}
