package com.yanhao.kmpmusic.core.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 量化色桶位移，16 阶 RGB 能保留封面倾向，也避免逐像素噪声。
 */
private const val COLOR_BIN_SHIFT = 4

/**
 * 量化色桶总数，16 * 16 * 16。
 */
private const val COLOR_BIN_COUNT = 4096

/**
 * 可参与显著色竞争的最小像素占比。
 */
private const val MIN_PROMINENT_POPULATION = 0.02f

/**
 * 低于该 alpha 的像素不参与背景取色。
 */
private const val MIN_VISIBLE_ALPHA = 160

// 从像素集合中选择封面种子色，优先显著彩色，必要时回退主导色。
internal fun selectCoverSeedColor(pixels: IntArray): Color {
    if (pixels.isEmpty()) {
        return MusicColors.Paper
    }
    val buckets: ColorBuckets = collectColorBuckets(pixels = pixels)
    val total: Int = buckets.totalCount
    if (total == 0) {
        return MusicColors.Paper
    }
    var dominantSwatch: ColorSwatch? = null
    var prominentSwatch: ColorSwatch? = null
    for (bucketIndex: Int in 0 until COLOR_BIN_COUNT) {
        val swatch: ColorSwatch = buckets.createSwatch(bucketIndex = bucketIndex, total = total) ?: continue
        if (dominantSwatch == null || swatch.population > dominantSwatch.population) {
            dominantSwatch = swatch
        }
        if (!swatch.isProminentCandidate()) {
            continue
        }
        if (prominentSwatch == null || swatch.score > prominentSwatch.score) {
            prominentSwatch = swatch
        }
    }
    return prominentSwatch?.color ?: dominantSwatch?.color ?: MusicColors.Paper
}

// 将像素归入 16 阶 RGB 色桶，并跳过不可见像素。
private fun collectColorBuckets(pixels: IntArray): ColorBuckets {
    val buckets: ColorBuckets = ColorBuckets()
    pixels.forEach { pixel: Int ->
        val alpha: Int = (pixel ushr 24) and 0xFF
        if (alpha < MIN_VISIBLE_ALPHA) {
            return@forEach
        }
        buckets.addPixel(pixel = pixel)
    }
    return buckets
}

// HSL 明度和饱和度用于过滤不适合当背景种子的色块。
private fun ColorSwatch.isProminentCandidate(): Boolean {
    return population >= MIN_PROMINENT_POPULATION &&
        hsl.saturation >= 0.12f &&
        hsl.lightness >= 0.22f &&
        hsl.lightness <= 0.78f
}

// 计算显著色分数，平衡占比、彩度和中间明度。
private fun calculateSwatchScore(population: Float, hsl: HslColor): Float {
    val lightnessPreference: Float = 1f - min(abs(hsl.lightness - 0.48f) / 0.42f, 1f)
    return population.toDouble().pow(0.60).toFloat() *
        (0.65f + hsl.saturation) *
        (0.45f + 0.55f * lightnessPreference)
}

// 把 RGB 转成 HSL，方便用视觉明度和饱和度过滤候选色。
private fun createHslColor(red: Float, green: Float, blue: Float): HslColor {
    val maxChannel: Float = max(red, max(green, blue))
    val minChannel: Float = min(red, min(green, blue))
    val lightness: Float = (maxChannel + minChannel) / 2f
    if (maxChannel == minChannel) {
        return HslColor(saturation = 0f, lightness = lightness)
    }
    val delta: Float = maxChannel - minChannel
    val saturation: Float = if (lightness > 0.5f) {
        delta / (2f - maxChannel - minChannel)
    } else {
        delta / (maxChannel + minChannel)
    }
    return HslColor(saturation = saturation, lightness = lightness)
}

/**
 * 色桶统计结果，集中维护数组以避免为每个像素创建对象。
 */
private class ColorBuckets {
    // 每个色桶的像素数量。
    private val counts: IntArray = IntArray(size = COLOR_BIN_COUNT)
    // 每个色桶的红色通道累加值。
    private val redSums: LongArray = LongArray(size = COLOR_BIN_COUNT)
    // 每个色桶的绿色通道累加值。
    private val greenSums: LongArray = LongArray(size = COLOR_BIN_COUNT)
    // 每个色桶的蓝色通道累加值。
    private val blueSums: LongArray = LongArray(size = COLOR_BIN_COUNT)
    // 参与统计的总像素数。
    var totalCount: Int = 0
        private set

    // 添加一个 ARGB 像素到对应量化色桶。
    fun addPixel(pixel: Int): Unit {
        val red: Int = (pixel shr 16) and 0xFF
        val green: Int = (pixel shr 8) and 0xFF
        val blue: Int = pixel and 0xFF
        val bucketIndex: Int = ((red shr COLOR_BIN_SHIFT) shl 8) or
            ((green shr COLOR_BIN_SHIFT) shl 4) or
            (blue shr COLOR_BIN_SHIFT)
        counts[bucketIndex] += 1
        redSums[bucketIndex] += red.toLong()
        greenSums[bucketIndex] += green.toLong()
        blueSums[bucketIndex] += blue.toLong()
        totalCount += 1
    }

    // 从色桶创建可评分的色块，空桶返回 null。
    fun createSwatch(bucketIndex: Int, total: Int): ColorSwatch? {
        val count: Int = counts[bucketIndex]
        if (count == 0) {
            return null
        }
        val red: Float = redSums[bucketIndex].toFloat() / count / 255f
        val green: Float = greenSums[bucketIndex].toFloat() / count / 255f
        val blue: Float = blueSums[bucketIndex].toFloat() / count / 255f
        val hsl: HslColor = createHslColor(red = red, green = green, blue = blue)
        val population: Float = count.toFloat() / total
        return ColorSwatch(
            color = Color(red = red, green = green, blue = blue),
            population = population,
            hsl = hsl,
            score = calculateSwatchScore(population = population, hsl = hsl),
        )
    }
}

/**
 * 单个量化色块及其评分数据。
 */
private data class ColorSwatch(
    val color: Color,
    val population: Float,
    val hsl: HslColor,
    val score: Float,
)

/**
 * 只保留当前算法需要的 HSL 属性。
 */
private data class HslColor(
    val saturation: Float,
    val lightness: Float,
)
