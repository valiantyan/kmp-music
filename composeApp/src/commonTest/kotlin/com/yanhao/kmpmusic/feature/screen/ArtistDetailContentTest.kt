package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 歌手详情页内容测试，锁住与 Figma 文案不同的真实数据规则。
 */
class ArtistDetailContentTest {
    /**
     * 播放入口下应展示当前歌手名下全部歌曲，并忽略轻微空白和英文大小写差异。
     */
    @Test
    fun artistDetailContentUsesAllNormalizedArtistSongs(): Unit {
        val artist = testArtist(name = "Jay Chou")
        val artistSongs: List<Song> = (1..7).map { index: Int ->
            testSong(
                id = "artist:$index",
                title = "Artist Song $index",
                artist = if (index % 2 == 0) " jay   chou " else "JAY CHOU",
            )
        }
        val otherSong: Song = testSong(
            id = "other:1",
            title = "Other Song",
            artist = "Other Artist",
        )

        val content: ArtistDetailContent = buildArtistDetailContent(
            artist = artist,
            songs = artistSongs + otherSong,
            currentSongId = "artist:2",
            currentPlaybackStatus = PlaybackStatus.Playing,
        )

        assertEquals(expected = artistSongs.map { song: Song -> song.id }, actual = content.artistSongs.map { song: Song -> song.id })
        assertEquals(expected = "播放全部", actual = content.playAllText)
        assertEquals(expected = "7 首歌曲", actual = content.playAllCountText)
        assertEquals(expected = "02", actual = content.songRows[1].indexLabel)
        assertEquals(expected = MusicColors.PlayingRed, actual = content.songRows[1].titleColor)
        assertEquals(expected = MusicColors.PlayingRed, actual = content.songRows[1].metaColor)
        assertFalse(actual = content.songRows[1].showsPlaybackAnimation)
    }

    /**
     * 当前歌曲再次点击应切换播放状态，其他歌曲点击才进入切歌逻辑。
     */
    @Test
    fun currentArtistSongClickTogglesCurrentPlayback(): Unit {
        assertEquals(
            expected = ArtistDetailSongClickAction.ToggleCurrentPlayback,
            actual = resolveArtistDetailSongClickAction(
                isCurrentSong = true,
                currentPlaybackStatus = PlaybackStatus.Playing,
            ),
        )
        assertEquals(
            expected = ArtistDetailSongClickAction.ToggleCurrentPlayback,
            actual = resolveArtistDetailSongClickAction(
                isCurrentSong = true,
                currentPlaybackStatus = PlaybackStatus.Paused,
            ),
        )
        assertEquals(
            expected = ArtistDetailSongClickAction.PlaySong,
            actual = resolveArtistDetailSongClickAction(
                isCurrentSong = false,
                currentPlaybackStatus = PlaybackStatus.Playing,
            ),
        )
    }

    /**
     * 测试模式下歌手详情页可以追加 30 条 demo 热门歌曲，且不改变默认真实数据规则。
     */
    @Test
    fun artistDetailContentCanAppendThirtyDemoSongs(): Unit {
        val artist = testArtist(name = "Camila")
        val realSong: Song = testSong(
            id = "camila:mientes",
            title = "Mientes",
            artist = "Camila",
        )

        val content: ArtistDetailContent = buildArtistDetailContent(
            artist = artist,
            songs = listOf(realSong),
            currentSongId = null,
            currentPlaybackStatus = PlaybackStatus.Paused,
            demoSongCount = 30,
        )

        assertEquals(expected = 31, actual = content.artistSongs.size)
        assertEquals(expected = "播放全部", actual = content.playAllText)
        assertEquals(expected = "31 首歌曲", actual = content.playAllCountText)
        assertEquals(expected = "31", actual = content.songRows.last().indexLabel)
        assertTrue(actual = content.artistSongs.drop(n = 1).all { song: Song -> song.title.startsWith(prefix = "热门歌曲 Demo ") })
        assertTrue(actual = content.artistSongs.drop(n = 1).all { song: Song -> song.artist == artist.name })
    }
}

// 构造歌手详情页内容测试使用的歌手。
private fun testArtist(name: String): Artist {
    return Artist(
        id = "artist:${name.lowercase()}",
        name = name,
        songCount = 7,
        albumCount = 0,
        coverArt = CoverArt.HeroLocalMusic,
        tag = "本地音乐",
    )
}

// 构造歌手详情页内容测试使用的歌曲。
private fun testSong(
    id: String,
    title: String,
    artist: String,
): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        duration = "3:00",
        coverArt = CoverArt.HeroLocalMusic,
        isLiked = false,
        lastPlayed = "未播放",
        quality = "本地 MP3",
        lyric = "本地音频",
        trackNumber = 1,
        durationMs = 180_000L,
        sourceId = id,
        sourceKind = LocalMusicSourceKind.FakeScanner,
        localUri = "fake://$id",
    )
}
