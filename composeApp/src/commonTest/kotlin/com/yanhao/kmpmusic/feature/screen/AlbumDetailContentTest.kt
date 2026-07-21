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
    fun albumDetailContentUsesNormalizedAlbumSongsInTrackOrder() {
        val album: Album = testAlbum(title = " River Year ")
        val firstTrack: Song =
            testSong(
                id = "river:01",
                title = "First Track",
                album = "river year",
                trackNumber = 1,
            )
        val secondTrack: Song =
            testSong(
                id = "river:02",
                title = "Second Track",
                album = "RIVER YEAR",
                trackNumber = 2,
            )
        val otherSong: Song =
            testSong(
                id = "other:01",
                title = "Other Track",
                album = "Other Album",
                trackNumber = 1,
            )

        val content: AlbumDetailContent =
            buildAlbumDetailContent(
                album = album,
                songs = listOf(secondTrack, otherSong, firstTrack),
            )
        val activeRowState: AlbumDetailSongRowState =
            buildAlbumDetailSongRowState(
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
    fun currentAlbumSongClickTogglesCurrentPlayback() {
        assertEquals(
            expected = AlbumDetailSongClickAction.ToggleCurrentPlayback,
            actual =
                resolveAlbumDetailSongClickAction(
                    isCurrentSong = true,
                    currentPlaybackStatus = PlaybackStatus.Playing,
                ),
        )
        assertEquals(
            expected = AlbumDetailSongClickAction.ToggleCurrentPlayback,
            actual =
                resolveAlbumDetailSongClickAction(
                    isCurrentSong = true,
                    currentPlaybackStatus = PlaybackStatus.Paused,
                ),
        )
        assertEquals(
            expected = AlbumDetailSongClickAction.PlaySong,
            actual =
                resolveAlbumDetailSongClickAction(
                    isCurrentSong = false,
                    currentPlaybackStatus = PlaybackStatus.Playing,
                ),
        )
    }

    /**
     * 当前播放歌曲变化只应重建行状态，不应重新过滤专辑队列。
     */
    @Test
    fun albumDetailContentReusesResolvedSongsWhenCurrentSongChanges() {
        val album: Album = testAlbum(title = "River Year")
        val realSong: Song =
            testSong(
                id = "river:01",
                title = "Real Track",
                album = "River Year",
                trackNumber = 1,
            )
        val albumSongs: List<Song> =
            buildAlbumDetailSongs(
                album = album,
                songs = listOf(realSong),
            )

        val content: AlbumDetailContent = buildAlbumDetailContent(albumSongs = albumSongs)
        val firstRowState: AlbumDetailSongRowState =
            buildAlbumDetailSongRowState(
                index = 0,
                song = realSong,
                isCurrentSong = true,
            )
        val lastRowState: AlbumDetailSongRowState =
            buildAlbumDetailSongRowState(
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
private fun testAlbum(title: String): Album =
    Album(
        id = "album:${normalizeAlbumTitle(value = title)}",
        title = title,
        artist = "Trip",
        songCount = 2,
        coverArt = CoverArt.HeroLocalMusic,
        mood = "本地音乐",
        year = "本地",
    )

// 构造专辑详情页内容测试使用的歌曲。
private fun testSong(
    id: String,
    title: String,
    album: String,
    trackNumber: Int,
): Song =
    Song(
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
