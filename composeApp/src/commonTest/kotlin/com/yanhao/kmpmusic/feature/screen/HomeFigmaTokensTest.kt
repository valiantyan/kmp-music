package com.yanhao.kmpmusic.feature.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 首页 Figma token 测试，锁住歌曲行和内容页签的关键渲染规则。
 */
class HomeFigmaTokensTest {
    /**
     * 普通首页歌曲行要跟收藏页一样使用白色卡片、阴影和收藏页文字层级。
     */
    @Test
    fun normalRowStyleUsesFavoritesCardSurface() {
        val style: HomeSongRowStyle =
            resolveHomeSongRowStyle(
                isCurrentSong = false,
            )
        assertEquals(expected = Color.White, actual = style.containerColor)
        assertNull(actual = style.border)
        assertEquals(expected = 2.dp, actual = style.shadowElevation)
        assertEquals(expected = favoritesTextColor, actual = style.textColor)
        assertEquals(expected = favoritesMetaColor, actual = style.metaColor)
        assertFalse(actual = style.showsCoverPlaybackBadge)
    }

    /**
     * 当前歌曲行不再使用旧首页浅绿色背景，而是跟收藏页一致用红字和封面标识表达播放态。
     */
    @Test
    fun currentRowStyleUsesFavoritesPlaybackMarker() {
        val style: HomeSongRowStyle =
            resolveHomeSongRowStyle(
                isCurrentSong = true,
            )
        assertEquals(expected = Color.White, actual = style.containerColor)
        assertNull(actual = style.border)
        assertEquals(expected = 2.dp, actual = style.shadowElevation)
        assertEquals(expected = MusicColors.PlayingRed, actual = style.textColor)
        assertEquals(expected = MusicColors.PlayingRed, actual = style.metaColor)
        assertTrue(actual = style.showsCoverPlaybackBadge)
    }

    /**
     * 专辑页签网格必须锁住 Figma 节点 `883:514` 的双列节奏和当前专辑标识。
     */
    @Test
    fun albumGridTokensMatchFigmaNode() {
        assertEquals(expected = Color(0xFFE1E3E4), actual = homeAlbumCoverBackgroundColor)
        assertEquals(expected = Color(0x1A006A62), actual = homeActiveAlbumOverlayColor)
        assertEquals(expected = 24.dp, actual = homeAlbumGridGap)
        assertEquals(expected = 24.dp, actual = homeAlbumCoverRadius)
        assertEquals(expected = 4.dp, actual = homeAlbumActiveBorderHeight)
    }

    /**
     * 歌手页签列表必须锁住 Figma 节点 `886:592` 的圆形头像和列表节奏。
     */
    @Test
    fun artistListTokensMatchFigmaNode() {
        assertEquals(expected = 84.dp, actual = homeArtistRowHeight)
        assertEquals(expected = 10.dp, actual = homeArtistRowVerticalPadding)
        assertEquals(expected = 0.dp, actual = homeArtistListGap)
        assertEquals(expected = 20.sp, actual = homeArtistNameFontSize)
        assertEquals(expected = 28.sp, actual = homeArtistNameLineHeight)
        assertEquals(expected = 64.dp, actual = homeArtistAvatarOuterSize)
        assertEquals(expected = 2.dp, actual = homeArtistAvatarBorderWidth)
        assertEquals(expected = 2.dp, actual = homeArtistAvatarInset)
        assertEquals(expected = Color(0x3326A69A), actual = homeArtistAvatarBorderColor)
        assertEquals(expected = Color(0xFFB8C7C4), actual = homeArtistChevronColor)
    }
}
