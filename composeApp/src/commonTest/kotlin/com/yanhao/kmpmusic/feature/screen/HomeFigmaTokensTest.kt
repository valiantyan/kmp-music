package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse

/**
 * 首页 Figma token 测试，锁住歌曲行选中态的最终渲染色。
 */
class HomeFigmaTokensTest {
    /**
     * 当前歌曲行背景必须使用确认后的不透明最终色。
     */
    @Test
    fun activeRowColorUsesConfirmedSolidColor(): Unit {
        assertEquals(expected = "#DDFCF7", actual = homeActiveRowColor.toHexColor())
    }

    /**
     * 普通歌曲行不能有描边或阴影，否则白底上会出现错误的 item 边框。
     */
    @Test
    fun normalRowStyleDoesNotUseBorder(): Unit {
        val style: HomeSongRowStyle = resolveHomeSongRowStyle(
            isCurrentSong = false,
            currentPlaybackStatus = PlaybackStatus.Playing,
        )
        assertEquals(expected = Color.White, actual = style.containerColor)
        assertNull(actual = style.border)
        assertEquals(expected = 0.dp, actual = style.shadowElevation)
        assertEquals(expected = homeAccentColor, actual = style.textColor)
    }

    /**
     * 当前歌曲行才使用 Figma active 背景和 1dp 描边，不能再叠加阴影。
     */
    @Test
    fun activeRowStyleUsesFigmaBorderWithoutShadow(): Unit {
        val style: HomeSongRowStyle = resolveHomeSongRowStyle(
            isCurrentSong = true,
            currentPlaybackStatus = PlaybackStatus.Paused,
        )
        val border = assertNotNull(actual = style.border)
        assertEquals(expected = homeActiveRowColor, actual = style.containerColor)
        assertEquals(expected = BorderStroke(width = 1.dp, color = homeActiveBorderColor), actual = border)
        assertEquals(expected = 0.dp, actual = style.shadowElevation)
        assertEquals(expected = homeAccentColor, actual = style.textColor)
    }

    /**
     * 只有真正播放中的当前歌曲才把歌曲名和歌手切到播放红色。
     */
    @Test
    fun activePlayingRowUsesRedTextWithoutCoverBadge(): Unit {
        val style: HomeSongRowStyle = resolveHomeSongRowStyle(
            isCurrentSong = true,
            currentPlaybackStatus = PlaybackStatus.Playing,
        )
        assertEquals(expected = MusicColors.PlayingRed, actual = style.textColor)
        assertFalse(actual = style.showsCoverPlaybackBadge)
    }

    /**
     * 专辑页签网格必须锁住 Figma 节点 `883:514` 的双列节奏和当前专辑标识。
     */
    @Test
    fun albumGridTokensMatchFigmaNode(): Unit {
        assertEquals(expected = Color(0xFFE1E3E4), actual = homeAlbumCoverBackgroundColor)
        assertEquals(expected = Color(0x1A006A62), actual = homeActiveAlbumOverlayColor)
        assertEquals(expected = 24.dp, actual = homeAlbumGridGap)
        assertEquals(expected = 24.dp, actual = homeAlbumCoverRadius)
        assertEquals(expected = 4.dp, actual = homeAlbumActiveBorderHeight)
    }
}

// 测试只比较最终显示色，避免把透明度 token 和视觉验收色混在一起。
private fun Color.toHexColor(): String {
    val redValue: Int = (red * 255).toInt()
    val greenValue: Int = (green * 255).toInt()
    val blueValue: Int = (blue * 255).toInt()
    return "#%02X%02X%02X".format(redValue, greenValue, blueValue)
}
