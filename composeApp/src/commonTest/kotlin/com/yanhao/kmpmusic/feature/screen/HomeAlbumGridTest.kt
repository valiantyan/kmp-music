package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.CoverArt
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 首页专辑网格测试，保证当前播放专辑高亮和详情页共用同一归属口径。
 */
class HomeAlbumGridTest {
    /**
     * 当前歌曲专辑名有大小写或空白差异时，首页专辑卡片仍应高亮。
     */
    @Test
    fun homeAlbumActiveUsesNormalizedAlbumTitle() {
        val album: Album = testAlbum(title = " River Year ")
        assertTrue(
            actual =
                isHomeAlbumActive(
                    album = album,
                    currentAlbumTitle = "river year",
                ),
        )
    }

    /**
     * 没有当前歌曲专辑时，不应误亮任何专辑卡片。
     */
    @Test
    fun homeAlbumActiveReturnsFalseWithoutCurrentAlbum() {
        val album: Album = testAlbum(title = "River Year")
        assertFalse(
            actual =
                isHomeAlbumActive(
                    album = album,
                    currentAlbumTitle = null,
                ),
        )
    }
}

// 构造首页专辑网格测试使用的专辑。
private fun testAlbum(title: String): Album =
    Album(
        id = "album:test",
        title = title,
        artist = "Trip",
        songCount = 1,
        coverArt = CoverArt.HeroLocalMusic,
        mood = "本地音乐",
        year = "本地",
    )
