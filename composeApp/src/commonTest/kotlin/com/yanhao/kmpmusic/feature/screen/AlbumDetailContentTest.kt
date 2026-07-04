package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.normalizeAlbumTitle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 专辑详情页内容测试，锁住 Figma UI 背后的真实专辑队列规则。
 */
class AlbumDetailContentTest {
    /**
     * 专辑详情页应按归一化专辑名聚合歌曲，并按专辑曲序形成播放队列。
     */
    @Test
    fun albumDetailContentUsesNormalizedAlbumSongsInTrackOrder(): Unit {
        val album: Album = testAlbum(title = " River Year ")
        val firstTrack: Song = testSong(
            id = "river:01",
            title = "First Track",
            album = "river year",
            trackNumber = 1,
        )
        val secondTrack: Song = testSong(
            id = "river:02",
            title = "Second Track",
            album = "RIVER YEAR",
            trackNumber = 2,
        )
        val otherSong: Song = testSong(
            id = "other:01",
            title = "Other Track",
            album = "Other Album",
            trackNumber = 1,
        )

        val content: AlbumDetailContent = buildAlbumDetailContent(
            album = album,
            songs = listOf(secondTrack, otherSong, firstTrack),
        )
        val activeRowState: AlbumDetailSongRowState = buildAlbumDetailSongRowState(
            index = 1,
            song = secondTrack,
            isCurrentSong = true,
        )

        assertEquals(expected = listOf(firstTrack.id, secondTrack.id), actual = content.albumSongs.map { song: Song -> song.id })
        assertEquals(expected = "播放全部", actual = content.playAllText)
        assertEquals(expected = "2首", actual = content.playAllCountText)
        assertEquals(expected = "02", actual = activeRowState.indexLabel)
        assertEquals(expected = MusicColors.PlayingRed, actual = activeRowState.titleColor)
        assertEquals(expected = MusicColors.PlayingRed, actual = activeRowState.durationColor)
        assertTrue(actual = activeRowState.showsPlaybackGlyph)
    }

    /**
     * 当前专辑歌曲再次点击应切换播放状态，其他歌曲点击才进入切歌逻辑。
     */
    @Test
    fun currentAlbumSongClickTogglesCurrentPlayback(): Unit {
        assertEquals(
            expected = AlbumDetailSongClickAction.ToggleCurrentPlayback,
            actual = resolveAlbumDetailSongClickAction(
                isCurrentSong = true,
                currentPlaybackStatus = PlaybackStatus.Playing,
            ),
        )
        assertEquals(
            expected = AlbumDetailSongClickAction.ToggleCurrentPlayback,
            actual = resolveAlbumDetailSongClickAction(
                isCurrentSong = true,
                currentPlaybackStatus = PlaybackStatus.Paused,
            ),
        )
        assertEquals(
            expected = AlbumDetailSongClickAction.PlaySong,
            actual = resolveAlbumDetailSongClickAction(
                isCurrentSong = false,
                currentPlaybackStatus = PlaybackStatus.Playing,
            ),
        )
    }

    /**
     * 测试模式下专辑详情页可以追加 500 条 demo 曲目，用于滚动、掉帧和重组压力验证。
     */
    @Test
    fun albumDetailContentCanAppendFiveHundredDemoSongsForScrollStress(): Unit {
        val album: Album = testAlbum(title = "River Year")
        val realSong: Song = testSong(
            id = "river:01",
            title = "Real Track",
            album = "River Year",
            trackNumber = 1,
        )

        val content: AlbumDetailContent = buildAlbumDetailContent(
            album = album,
            songs = listOf(realSong),
            demoSongCount = ALBUM_DETAIL_DEMO_SONG_COUNT,
        )
        val expectedSongCount: Int = ALBUM_DETAIL_DEMO_SONG_COUNT + 1
        val lastRowState: AlbumDetailSongRowState = buildAlbumDetailSongRowState(
            index = expectedSongCount - 1,
            song = content.albumSongs.last(),
            isCurrentSong = false,
        )

        assertEquals(expected = 500, actual = ALBUM_DETAIL_DEMO_SONG_COUNT)
        assertEquals(expected = expectedSongCount, actual = content.albumSongs.size)
        assertEquals(expected = "${expectedSongCount}首", actual = content.playAllCountText)
        assertEquals(expected = expectedSongCount.toString(), actual = lastRowState.indexLabel)
        assertTrue(actual = content.albumSongs.drop(n = 1).all { song: Song -> song.title.startsWith(prefix = "专辑曲目 Demo ") })
        assertTrue(actual = content.albumSongs.drop(n = 1).all { song: Song -> song.album == album.title })
    }

    /**
     * 当前播放歌曲变化只应重建行状态，不应重新生成 500 条专辑队列。
     */
    @Test
    fun albumDetailContentReusesResolvedSongsWhenCurrentSongChanges(): Unit {
        val album: Album = testAlbum(title = "River Year")
        val realSong: Song = testSong(
            id = "river:01",
            title = "Real Track",
            album = "River Year",
            trackNumber = 1,
        )
        val albumSongs: List<Song> = buildAlbumDetailSongs(
            album = album,
            songs = listOf(realSong),
            demoSongCount = ALBUM_DETAIL_DEMO_SONG_COUNT,
        )

        val content: AlbumDetailContent = buildAlbumDetailContent(albumSongs = albumSongs)
        val firstRowState: AlbumDetailSongRowState = buildAlbumDetailSongRowState(
            index = 0,
            song = realSong,
            isCurrentSong = true,
        )
        val lastRowState: AlbumDetailSongRowState = buildAlbumDetailSongRowState(
            index = albumSongs.lastIndex,
            song = albumSongs.last(),
            isCurrentSong = true,
        )

        assertTrue(actual = content.albumSongs === albumSongs)
        assertEquals(expected = MusicColors.PlayingRed, actual = firstRowState.titleColor)
        assertEquals(expected = MusicColors.PlayingRed, actual = lastRowState.titleColor)
    }
}

// 构造专辑详情页内容测试使用的专辑。
private fun testAlbum(title: String): Album {
    return Album(
        id = "album:${normalizeAlbumTitle(value = title)}",
        title = title,
        artist = "Trip",
        songCount = 2,
        coverArt = CoverArt.HeroLocalMusic,
        mood = "本地音乐",
        year = "本地",
    )
}

// 构造专辑详情页内容测试使用的歌曲。
private fun testSong(
    id: String,
    title: String,
    album: String,
    trackNumber: Int,
): Song {
    return Song(
        id = id,
        title = title,
        artist = "Trip",
        album = album,
        duration = "3:00",
        coverArt = CoverArt.HeroLocalMusic,
        isLiked = false,
        lastPlayed = "未播放",
        quality = "本地 MP3",
        lyric = "本地音频",
        trackNumber = trackNumber,
        durationMs = 180_000L,
        sourceId = id,
        sourceKind = LocalMusicSourceKind.FakeScanner,
        localUri = "fake://$id",
    )
}
