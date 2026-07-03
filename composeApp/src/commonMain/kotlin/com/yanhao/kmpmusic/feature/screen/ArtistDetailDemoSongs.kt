package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 歌手详情页热门歌曲测试数据数量。
 */
internal const val ARTIST_DETAIL_DEMO_SONG_COUNT = 30

/**
 * 只为歌手详情页滚动和播放态测试追加 demo 歌曲，不污染全局本地曲库。
 */
internal fun appendArtistDetailDemoSongs(
    artist: Artist,
    artistSongs: List<Song>,
    demoSongCount: Int,
): List<Song> {
    if (demoSongCount <= 0) {
        return artistSongs
    }
    return artistSongs + buildArtistDetailDemoSongs(
        artist = artist,
        artistSongs = artistSongs,
        demoSongCount = demoSongCount,
    )
}

// 按当前歌手和已有歌曲生成可区分的测试行。
private fun buildArtistDetailDemoSongs(
    artist: Artist,
    artistSongs: List<Song>,
    demoSongCount: Int,
): List<Song> {
    return (1..demoSongCount).map { index: Int ->
        val baseSong: Song? = artistSongs.getOrNull(index = (index - 1) % artistSongs.size.coerceAtLeast(minimumValue = 1))
        createArtistDetailDemoSong(
            artist = artist,
            baseSong = baseSong,
            index = index,
        )
    }
}

// 复用真实歌曲的封面和 URI，让测试行在点击播放时尽量沿用真实播放链路。
private fun createArtistDetailDemoSong(
    artist: Artist,
    baseSong: Song?,
    index: Int,
): Song {
    val paddedIndex: String = index.toString().padStart(length = 2, padChar = '0')
    val demoId: String = "artist-detail-demo:${artist.id}:$paddedIndex"
    return Song(
        id = demoId,
        title = "热门歌曲 Demo $paddedIndex",
        artist = artist.name,
        album = baseSong?.album ?: "歌手详情测试",
        duration = baseSong?.duration ?: "3:00",
        coverArt = baseSong?.coverArt ?: artist.coverArt,
        coverImageUri = baseSong?.coverImageUri ?: artist.coverImageUri,
        isLiked = false,
        lastPlayed = "测试数据",
        quality = baseSong?.quality ?: "Demo",
        lyric = baseSong?.lyric ?: "歌手详情测试歌曲",
        trackNumber = (baseSong?.trackNumber ?: 0) + index,
        durationMs = baseSong?.durationMs ?: 180_000L,
        sourceId = demoId,
        sourceKind = LocalMusicSourceKind.FakeScanner,
        localUri = baseSong?.localUri ?: "fake://artist-detail-demo/${artist.id}/$paddedIndex",
        mimeType = baseSong?.mimeType,
        sizeBytes = baseSong?.sizeBytes,
        modifiedAt = baseSong?.modifiedAt?.minus(other = index.toLong()),
    )
}
