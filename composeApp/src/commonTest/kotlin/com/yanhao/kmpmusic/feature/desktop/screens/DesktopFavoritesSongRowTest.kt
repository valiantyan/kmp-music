package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 收藏页歌曲行图标映射测试，避免已收藏歌曲回退成空心图标。
 */
class DesktopFavoritesSongRowTest {
    /** 已收藏歌曲必须使用实心图标，直接覆盖收藏页的默认数据状态。 */
    @Test
    fun likedSongUsesFilledFavoriteIcon() {
        assertEquals(
            expected = Icons.Rounded.Favorite,
            actual = resolveDesktopFavoritesLikeIcon(isLiked = true),
        )
    }

    /** 非收藏状态仍保留空心图标，确保映射语义完整。 */
    @Test
    fun unlikedSongUsesOutlinedFavoriteIcon() {
        assertEquals(
            expected = Icons.Rounded.FavoriteBorder,
            actual = resolveDesktopFavoritesLikeIcon(isLiked = false),
        )
    }
}
