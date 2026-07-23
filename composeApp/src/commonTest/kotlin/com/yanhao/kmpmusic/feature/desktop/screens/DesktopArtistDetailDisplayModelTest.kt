package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Desktop 歌手详情显示模型测试，固定曲目队列、hero 候选顺序和空态语义。 */
class DesktopArtistDetailDisplayModelTest {
    /** hero 必须跳过默认图歌曲，保留有效封面的原始歌曲顺序，并与播放队列使用同一曲目集。 */
    @Test
    fun buildsArtistQueueAndOrderedHeroArtworkCandidates() {
        val artist: Artist = createArtist()
        val displayModel: DesktopArtistDetailDisplayModel =
            buildDesktopArtistDetailDisplayModel(
                artist = artist,
                songs =
                    listOf(
                        createSong(id = "default", coverArt = CoverArt.HeroLocalMusic),
                        createSong(id = "resource", coverArt = CoverArt.AlbumRiverYear),
                        createSong(id = "uri", coverArt = CoverArt.HeroLocalMusic, coverImageUri = "file:///qa/cover.jpg"),
                        createSong(id = "other", artist = "其他歌手", coverArt = CoverArt.CoverSeaDream),
                    ),
            )
        assertEquals(expected = listOf("default", "resource", "uri"), actual = displayModel.songs.map { song: Song -> song.id })
        assertEquals(expected = listOf("resource", "uri"), actual = displayModel.heroArtworkCandidates.map { song: Song -> song.id })
        assertEquals(expected = "播放全部 (3)", actual = displayModel.playAllLabel)
        assertTrue(actual = displayModel.isPlaybackEnabled)
    }

    /** 当前歌手没有歌曲时保留可识别名称和默认封面降级路径，但不允许建立空播放队列。 */
    @Test
    fun emptyArtistDisablesPlaybackAndShowsEmptyMessage() {
        val displayModel: DesktopArtistDetailDisplayModel =
            buildDesktopArtistDetailDisplayModel(
                artist = createArtist(),
                songs = listOf(createSong(id = "other", artist = "其他歌手", coverArt = CoverArt.CoverSeaDream)),
            )
        assertEquals(expected = "播放全部 (0)", actual = displayModel.playAllLabel)
        assertEquals(expected = "暂无歌曲", actual = displayModel.emptyMessage)
        assertTrue(actual = displayModel.heroArtworkCandidates.isEmpty())
        assertFalse(actual = displayModel.isPlaybackEnabled)
    }

    /** 详情身份失效时必须回退到不可用文案，避免显示上一次歌手的数据或保留可点播放按钮。 */
    @Test
    fun unavailableArtistUsesSafeFallback() {
        val displayModel: DesktopArtistDetailDisplayModel =
            buildDesktopArtistDetailDisplayModel(
                artist = null,
                songs = listOf(createSong(id = "song", coverArt = CoverArt.CoverSeaDream)),
            )
        assertEquals(expected = "歌手不可用", actual = displayModel.title)
        assertEquals(expected = "没有找到歌手信息", actual = displayModel.emptyMessage)
        assertFalse(actual = displayModel.isPlaybackEnabled)
    }

    /** 第一张 URI 封面失败必须尝试下一首；全部失败后候选为空，hero 因而显示歌曲默认图。 */
    @Test
    fun heroArtworkFailureAdvancesInOrderThenFallsBackToDefaultArtwork() {
        val candidates: List<Song> =
            listOf(
                createSong(id = "first", coverArt = CoverArt.HeroLocalMusic, coverImageUri = "file:///qa/first.jpg"),
                createSong(id = "second", coverArt = CoverArt.HeroLocalMusic, coverImageUri = "file:///qa/second.jpg"),
            )
        val nextCandidateIndex: Int? =
            nextDesktopArtistDetailHeroArtworkCandidateIndex(
                candidates = candidates,
                candidateIndex = 0,
            )
        assertEquals(expected = 1, actual = nextCandidateIndex)
        assertEquals(
            expected = "second",
            actual =
                resolveDesktopArtistDetailHeroArtworkCandidate(
                    candidates = candidates,
                    candidateIndex = nextCandidateIndex,
                )?.id,
        )
        val exhaustedCandidateIndex: Int? =
            nextDesktopArtistDetailHeroArtworkCandidateIndex(
                candidates = candidates,
                candidateIndex = nextCandidateIndex,
            )
        assertNull(actual = exhaustedCandidateIndex)
        assertNull(
            actual =
                resolveDesktopArtistDetailHeroArtworkCandidate(
                    candidates = candidates,
                    candidateIndex = exhaustedCandidateIndex,
                ),
        )
    }

    /** 构造只含歌手详情过滤和封面候选判断所需字段的稳定歌手。 */
    private fun createArtist(): Artist =
        Artist(
            id = "artist:detail",
            name = "详情歌手",
            songCount = 3,
            coverArt = CoverArt.HeroLocalMusic,
            tag = "本地音乐",
        )

    /** 构造可播放的最小歌曲，便于验证歌手过滤和按顺序封面候选规则。 */
    private fun createSong(
        id: String,
        artist: String = "详情歌手",
        coverArt: CoverArt,
        coverImageUri: String? = null,
    ): Song =
        Song(
            id = id,
            title = id,
            artist = artist,
            album = "详情专辑",
            duration = "03:20",
            coverArt = coverArt,
            coverImageUri = coverImageUri,
            isLiked = false,
            lastPlayed = "",
            quality = "Lossless",
            lyric = "",
            trackNumber = 1,
            localUri = "fake://$id",
        )
}
