package com.yanhao.kmpmusic.core.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

class CoverPaletteTest {
    @Test
    fun selectCoverSeedColorUsesProminentChromaticColor(): Unit {
        val pixels: IntArray = createPixels(
            packedColor(red = 107, green = 150, blue = 168) to 70,
            packedColor(red = 19, green = 23, blue = 27) to 30,
        )
        val seedColor: Color = selectCoverSeedColor(pixels = pixels)
        assertEquals(expected = "#6B96A8", actual = seedColor.toHexColor())
    }

    @Test
    fun selectCoverSeedColorFallsBackToDominantWhenTintIsTiny(): Unit {
        val pixels: IntArray = createPixels(
            packedColor(red = 70, green = 70, blue = 72) to 99,
            packedColor(red = 92, green = 117, blue = 118) to 1,
        )
        val seedColor: Color = selectCoverSeedColor(pixels = pixels)
        assertEquals(expected = "#464648", actual = seedColor.toHexColor())
    }

    @Test
    fun createMiniPlayerContainerColorKeepsCoverTintSubtle(): Unit {
        val containerColor: Color = createMiniPlayerContainerColor(
            seedColor = Color(red = 107, green = 150, blue = 168),
        )
        assertEquals(expected = "#E4ECEF", actual = containerColor.toHexColor())
    }

    @Test
    fun createPlayerPageBackgroundColorKeepsStrongerCoverTint(): Unit {
        val backgroundColor: Color = createPlayerPageBackgroundColor(
            seedColor = Color(red = 107, green = 150, blue = 168),
        )
        assertEquals(expected = "#C4D5DD", actual = backgroundColor.toHexColor())
    }
}

// 构造重复像素，便于用用户可感知的比例验证取色规则。
private fun createPixels(vararg entries: Pair<Int, Int>): IntArray {
    val pixels: MutableList<Int> = mutableListOf()
    entries.forEach { entry: Pair<Int, Int> ->
        repeat(times = entry.second) {
            pixels += entry.first
        }
    }
    return pixels.toIntArray()
}

// 生成 [ImageBitmap.readPixels] 使用的 ARGB packed int。
private fun packedColor(red: Int, green: Int, blue: Int): Int {
    return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}

// 转成十六进制颜色，避免测试暴露 Compose [Color] 的内部浮点表示。
private fun Color.toHexColor(): String {
    val red: Int = (this.red * 255f).roundToInt().coerceIn(minimumValue = 0, maximumValue = 255)
    val green: Int = (this.green * 255f).roundToInt().coerceIn(minimumValue = 0, maximumValue = 255)
    val blue: Int = (this.blue * 255f).roundToInt().coerceIn(minimumValue = 0, maximumValue = 255)
    return "#${red.toHexByte()}${green.toHexByte()}${blue.toHexByte()}"
}

// 按 UI 颜色常用格式输出两位十六进制通道值。
private fun Int.toHexByte(): String {
    return toString(radix = 16).padStart(length = 2, padChar = '0').uppercase()
}
