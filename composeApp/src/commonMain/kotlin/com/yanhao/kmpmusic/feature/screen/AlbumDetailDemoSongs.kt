package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 专辑详情页滚动压力测试 demo 曲目数量。
 */
internal const val ALBUM_DETAIL_DEMO_SONG_COUNT = 500

/**
 * 只为专辑详情页滚动和重组测试追加 demo 曲目，不污染全局本地曲库。
 */
internal fun appendAlbumDetailDemoSongs(
    album: Album,
    albumSongs: List<Song>,
    demoSongCount: Int,
): List<Song> {
    if (demoSongCount <= 0) {
        return albumSongs
    }
    return albumSongs + buildAlbumDetailDemoSongs(
        album = album,
        albumSongs = albumSongs,
        demoSongCount = demoSongCount,
    )
}

// 按当前专辑和已有歌曲生成可区分的测试行。
private fun buildAlbumDetailDemoSongs(
    album: Album,
    albumSongs: List<Song>,
    demoSongCount: Int,
): List<Song> {
    val firstDemoTrackNumber: Int = resolveFirstAlbumDetailDemoTrackNumber(albumSongs = albumSongs)
    return (1..demoSongCount).map { index: Int ->
        val baseSong: Song? = albumSongs.getOrNull(index = (index - 1) % albumSongs.size.coerceAtLeast(minimumValue = 1))
        createAlbumDetailDemoSong(
            album = album,
            baseSong = baseSong,
            index = index,
            trackNumber = firstDemoTrackNumber + index - 1,
        )
    }
}

// demo 曲序接在真实曲目后面，避免插入到专辑真实第一首之前。
private fun resolveFirstAlbumDetailDemoTrackNumber(albumSongs: List<Song>): Int {
    val maxRealTrackNumber: Int = albumSongs.maxOfOrNull { song: Song ->
        song.trackNumber.takeIf { trackNumber: Int -> trackNumber > 0 } ?: 0
    } ?: 0
    return maxRealTrackNumber + 1
}

// 复用真实歌曲的封面和 URI，让测试行点击播放时尽量沿用真实播放链路。
private fun createAlbumDetailDemoSong(
    album: Album,
    baseSong: Song?,
    index: Int,
    trackNumber: Int,
): Song {
    val paddedIndex: String = index.toString().padStart(length = 3, padChar = '0')
    val demoId: String = "album-detail-demo:${album.id}:$paddedIndex"
    return Song(
        id = demoId,
        title = "专辑曲目 Demo $paddedIndex",
        artist = album.artist,
        album = album.title,
        duration = baseSong?.duration ?: "3:00",
        coverArt = baseSong?.coverArt ?: album.coverArt,
        coverImageUri = baseSong?.coverImageUri ?: album.coverImageUri,
        isLiked = false,
        lastPlayed = "测试数据",
        quality = baseSong?.quality ?: "Demo",
        lyric = baseSong?.lyric ?: "专辑详情测试歌曲",
        trackNumber = trackNumber,
        durationMs = baseSong?.durationMs ?: 180_000L,
        sourceId = demoId,
        sourceKind = LocalMusicSourceKind.FakeScanner,
        localUri = baseSong?.localUri ?: "fake://album-detail-demo/${album.id}/$paddedIndex",
        mimeType = baseSong?.mimeType,
        sizeBytes = baseSong?.sizeBytes,
        modifiedAt = baseSong?.modifiedAt?.minus(other = index.toLong()),
    )
}
