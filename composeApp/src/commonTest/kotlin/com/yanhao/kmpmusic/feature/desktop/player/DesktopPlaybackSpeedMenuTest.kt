package com.yanhao.kmpmusic.feature.desktop.player

import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 桌面播放倍速菜单测试，锁住底栏和播放页共享按钮的单行展示能力。
 */
class DesktopPlaybackSpeedMenuTest {
    /** 触发按钮要足够容纳最长倍率和单位，避免截图中的换行。 */
    @Test
    fun triggerWidthKeepsPlaybackSpeedLabelOnSingleLine() {
        assertEquals(expected = 96.dp, actual = DesktopPlaybackSpeedMenuDesignSpec.triggerWidth)
        assertEquals(expected = 91.dp, actual = DesktopPlaybackSpeedMenuDesignSpec.minimumSingleLineWidth)
        assertTrue(
            actual =
                DesktopPlaybackSpeedMenuDesignSpec.triggerWidth >=
                    DesktopPlaybackSpeedMenuDesignSpec.minimumSingleLineWidth,
        )
    }

    /** 倍速按钮必须显示倍率单位，不能只露出裸数值。 */
    @Test
    fun triggerLabelIncludesSpeedUnit() {
        assertEquals(
            expected = "0.75x",
            actual = formatDesktopPlaybackSpeedTriggerLabel(playbackSpeed = PlaybackSpeed.ThreeQuarter),
        )
        assertEquals(
            expected = "1.25x",
            actual = formatDesktopPlaybackSpeedTriggerLabel(playbackSpeed = PlaybackSpeed.OneQuarter),
        )
    }
}
