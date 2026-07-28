package com.yanhao.kmpmusic.feature.screen

import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 播放页元数据和辅助操作测试，锁住截图反馈的倍速入口默认态和角标规则。
 */
class PlayerScreenMetadataTest {
    /** 默认 1.0 也是有效倍速，入口不能显示成禁用灰色。 */
    @Test
    fun defaultPlaybackSpeedActionUsesActiveVisualAndBadge() {
        val visual: PlayerPlaybackSpeedVisual =
            buildPlayerPlaybackSpeedVisual(playbackSpeed = PlaybackSpeed.resolveDefault())

        assertEquals(expected = MusicColors.AccentDeep, actual = visual.tint)
        assertEquals(expected = "1.0x", actual = visual.badgeText)
    }

    /** 非默认倍速复用同一有效状态，只通过角标区分当前倍率。 */
    @Test
    fun customPlaybackSpeedActionShowsCurrentBadge() {
        val visual: PlayerPlaybackSpeedVisual =
            buildPlayerPlaybackSpeedVisual(playbackSpeed = PlaybackSpeed.ThreeQuarter)

        assertEquals(expected = MusicColors.AccentDeep, actual = visual.tint)
        assertEquals(expected = "0.75x", actual = visual.badgeText)
    }

    /** 角标必须收敛在辅助动作槽位内，避免压住底部系统导航。 */
    @Test
    fun playbackSpeedBadgeFitsAuxiliaryActionSlot() {
        assertEquals(expected = 64.dp, actual = PlayerAuxiliaryActionDesignSpec.width)
        assertTrue(actual = PlayerAuxiliaryActionDesignSpec.badgeMinWidth < PlayerAuxiliaryActionDesignSpec.width)
        assertTrue(actual = PlayerAuxiliaryActionDesignSpec.badgeHeight < PlayerAuxiliaryActionDesignSpec.iconSlotSize)
    }
}
