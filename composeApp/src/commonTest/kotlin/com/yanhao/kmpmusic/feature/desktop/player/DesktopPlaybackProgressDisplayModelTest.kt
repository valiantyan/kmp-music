package com.yanhao.kmpmusic.feature.desktop.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 桌面播放进度显示模型测试，覆盖平台时长偏短时的底栏和播放页表现。
 */
class DesktopPlaybackProgressDisplayModelTest {
    /** 正常时长应保持真实进度和总时长，不额外拉伸滑杆范围。 */
    @Test
    fun knownDurationUsesOriginalRange() {
        val model: DesktopPlaybackProgressDisplayModel =
            buildDesktopPlaybackProgressDisplayModel(
                playbackPositionMs = 30_000L,
                playbackDurationMs = 180_000L,
                isPlaying = true,
            )

        assertEquals(expected = 30_000L, actual = model.positionMs)
        assertEquals(expected = 180_000L, actual = model.durationMs)
        assertEquals(expected = 30_000f, actual = model.sliderValue)
        assertEquals(expected = 180_000f, actual = model.sliderRange.endInclusive)
    }

    /** 已知总时长偏短时，滑杆不能满格后继续播放。 */
    @Test
    fun playingAtKnownDurationKeepsSliderBelowEndWhilePlaybackContinues() {
        val model: DesktopPlaybackProgressDisplayModel =
            buildDesktopPlaybackProgressDisplayModel(
                playbackPositionMs = 189_000L,
                playbackDurationMs = 189_000L,
                isPlaying = true,
            )

        assertEquals(expected = 189_000L, actual = model.positionMs)
        assertEquals(expected = 190_000L, actual = model.durationMs)
        assertTrue(actual = model.sliderValue < model.sliderRange.endInclusive)
    }

    /** 已知总时长被真实进度越过时，播放中滑杆继续保留余量。 */
    @Test
    fun staleDurationKeepsSliderBelowEndWhilePlaybackContinues() {
        val model: DesktopPlaybackProgressDisplayModel =
            buildDesktopPlaybackProgressDisplayModel(
                playbackPositionMs = 190_000L,
                playbackDurationMs = 189_000L,
                isPlaying = true,
            )

        assertEquals(expected = 190_000L, actual = model.positionMs)
        assertEquals(expected = 191_000L, actual = model.durationMs)
        assertTrue(actual = model.sliderValue < model.sliderRange.endInclusive)
    }

    /** 暂停或结束态到达总时长时仍显示满格，避免把自然结束误判成时长偏短。 */
    @Test
    fun pausedAtKnownDurationKeepsSliderAtEnd() {
        val model: DesktopPlaybackProgressDisplayModel =
            buildDesktopPlaybackProgressDisplayModel(
                playbackPositionMs = 189_000L,
                playbackDurationMs = 189_000L,
                isPlaying = false,
            )

        assertEquals(expected = 189_000L, actual = model.positionMs)
        assertEquals(expected = 189_000L, actual = model.durationMs)
        assertEquals(expected = model.sliderRange.endInclusive, actual = model.sliderValue)
    }

    /** 未知时长仍禁用 seek，避免 UI 暗示可拖动到无效范围。 */
    @Test
    fun unknownDurationDisablesSeek() {
        val model: DesktopPlaybackProgressDisplayModel =
            buildDesktopPlaybackProgressDisplayModel(
                playbackPositionMs = 30_000L,
                playbackDurationMs = null,
                isPlaying = true,
            )

        assertEquals(expected = 30_000L, actual = model.positionMs)
        assertEquals(expected = 0L, actual = model.durationMs)
        assertEquals(expected = 0f, actual = model.sliderValue)
        assertTrue(actual = !model.isSeekEnabled)
    }
}
