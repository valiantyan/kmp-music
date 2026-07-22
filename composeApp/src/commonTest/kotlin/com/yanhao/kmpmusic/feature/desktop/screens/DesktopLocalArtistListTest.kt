package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 桌面歌手页 Figma `1085:709` 显示规格测试，避免回退到旧四列卡片布局。
 */
class DesktopLocalArtistListTest {
    /**
     * 歌手页容器、行高和文字尺寸必须匹配本轮 Figma 节点。
     */
    @Test
    fun visualSpecMatchesFigmaNode() {
        val visualSpec: DesktopLocalArtistListVisualSpec = resolveDesktopLocalArtistListVisualSpec()
        assertEquals(expected = Color(0xFFF9F9FF), actual = visualSpec.pageBackgroundColor)
        assertEquals(expected = 24.dp, actual = visualSpec.pageHorizontalPadding)
        assertEquals(expected = 16.dp, actual = visualSpec.pageTopPadding)
        assertEquals(expected = 32.sp, actual = visualSpec.titleFontSize)
        assertEquals(expected = 40.sp, actual = visualSpec.titleLineHeight)
        assertEquals(expected = 32.dp, actual = visualSpec.titleBottomSpacing)
        assertEquals(expected = Color(0x66F0F3FF), actual = visualSpec.listColor)
        assertEquals(expected = Color(0x1ABBCAC4), actual = visualSpec.listBorderColor)
        assertEquals(expected = 12.dp, actual = visualSpec.listRadius)
        assertEquals(expected = 1.dp, actual = visualSpec.listBorderPadding)
        assertEquals(expected = 97.dp, actual = visualSpec.regularRowHeight)
        assertEquals(expected = 96.dp, actual = visualSpec.lastRowHeight)
        assertEquals(expected = 64.dp, actual = visualSpec.avatarOuterSize)
        assertEquals(expected = 20.sp, actual = visualSpec.artistNameFontSize)
        assertEquals(expected = 28.sp, actual = visualSpec.artistNameLineHeight)
        assertEquals(expected = 13.sp, actual = visualSpec.artistSubtitleFontSize)
        assertEquals(expected = 18.sp, actual = visualSpec.artistSubtitleLineHeight)
        assertEquals(expected = 7.4.dp, actual = visualSpec.chevronWidth)
        assertEquals(expected = 12.dp, actual = visualSpec.chevronHeight)
    }

    /**
     * 副标题必须包含歌曲和专辑聚合数，不能退回旧的单歌曲数文案。
     */
    @Test
    fun artistSubtitleIncludesSongAndAlbumCounts() {
        val artist: Artist = testArtist(songCount = 3, albumCount = 2)
        assertEquals(
            expected = "3 首歌曲 · 2 张专辑",
            actual = formatDesktopLocalArtistSubtitle(artist = artist),
        )
    }

    /**
     * 歌手列表只有内容超出当前列表视口时才显示滚动条。
     */
    @Test
    fun artistScrollbarOnlyShowsForScrollableList() {
        assertTrue(
            actual =
                shouldShowDesktopLocalArtistScrollbar(
                    totalItemsCount = 40,
                    visibleItemsCount = 6,
                    canScrollForward = true,
                    canScrollBackward = false,
                ),
        )
        assertFalse(
            actual =
                shouldShowDesktopLocalArtistScrollbar(
                    totalItemsCount = 6,
                    visibleItemsCount = 6,
                    canScrollForward = false,
                    canScrollBackward = false,
                ),
        )
    }

    /**
     * 歌手列表滚动条滚动中显示，停止滚动满 5 秒后隐藏。
     */
    @Test
    fun artistScrollbarHidesAfterFiveSecondIdleDelay() {
        assertTrue(
            actual =
                shouldRenderDesktopLocalArtistScrollbar(
                    hasScrollableContent = true,
                    isScrollInProgress = true,
                    idleDurationMillis = DESKTOP_LOCAL_ARTIST_SCROLLBAR_HIDE_DELAY_MILLIS,
                ),
        )
        assertTrue(
            actual =
                shouldRenderDesktopLocalArtistScrollbar(
                    hasScrollableContent = true,
                    isScrollInProgress = false,
                    idleDurationMillis = DESKTOP_LOCAL_ARTIST_SCROLLBAR_HIDE_DELAY_MILLIS - 1L,
                ),
        )
        assertFalse(
            actual =
                shouldRenderDesktopLocalArtistScrollbar(
                    hasScrollableContent = true,
                    isScrollInProgress = false,
                    idleDurationMillis = DESKTOP_LOCAL_ARTIST_SCROLLBAR_HIDE_DELAY_MILLIS,
                ),
        )
    }

    /**
     * 只有默认本地封面才使用 Figma 占位头像，扫描封面或资源封面继续显示真实图。
     */
    @Test
    fun placeholderArtworkOnlyAppliesToDefaultLocalCover() {
        assertTrue(actual = shouldUseDesktopLocalArtistPlaceholderArtwork(artist = testArtist()))
        assertFalse(
            actual =
                shouldUseDesktopLocalArtistPlaceholderArtwork(
                    artist = testArtist(coverImageUri = "file:///cover.png"),
                ),
        )
        assertFalse(
            actual =
                shouldUseDesktopLocalArtistPlaceholderArtwork(
                    artist = testArtist(coverArt = CoverArt.AlbumRiverYear),
                ),
        )
    }

    // 构造最小歌手模型，避免测试依赖扫描或 demo catalog。
    private fun testArtist(
        songCount: Int = 1,
        albumCount: Int = 1,
        coverArt: CoverArt = CoverArt.HeroLocalMusic,
        coverImageUri: String? = null,
    ): Artist =
        Artist(
            id = "artist:camila",
            name = "Camila",
            songCount = songCount,
            albumCount = albumCount,
            coverArt = coverArt,
            coverImageUri = coverImageUri,
            tag = "本地音乐",
        )
}
