package com.yanhao.kmpmusic.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.max
import kotlin.math.min

/**
 * 迷你播放器从封面提取出的视觉配色。
 *
 * @property containerColor 播放器容器背景色。
 */
data class MiniPlayerPalette(
    val containerColor: Color,
)

/**
 * 播放详情页从封面提取出的视觉配色。
 *
 * @property backgroundColor 页面底色，必须保留足够亮度以承载深色文字。
 * @property ambientColor 页面氛围色，用于局部渐变强化当前歌曲感。
 */
data class PlayerPagePalette(
    val backgroundColor: Color,
    val ambientColor: Color,
)

/**
 * 容器背景中主题纸色的混入比例。
 */
private const val MINI_PLAYER_CONTAINER_PAPER_WEIGHT = 0.84f

/**
 * 播放页背景中主题纸色的混入比例。
 */
private const val PLAYER_PAGE_BACKGROUND_PAPER_WEIGHT = 0.62f

/**
 * 播放页氛围色中主题纸色的混入比例。
 */
private const val PLAYER_PAGE_AMBIENT_PAPER_WEIGHT = 0.34f

/**
 * 大图取样时单边最多采样的点数，限制真实本地封面读取成本。
 */
private const val MAX_SAMPLE_EDGE = 64

/**
 * 根据封面图片提取迷你播放器配色。
 *
 * 空图或资源占位图回退到当前纸色，避免资源异步加载时闪出异常颜色。
 */
fun extractMiniPlayerPalette(imageBitmap: ImageBitmap): MiniPlayerPalette {
    if (imageBitmap.width <= 1 || imageBitmap.height <= 1) {
        return MiniPlayerPalette(
            containerColor = MusicColors.Paper.copy(alpha = 0.92f),
        )
    }
    val sampledPixels: IntArray = readSampledPixels(imageBitmap = imageBitmap)
    val seedColor: Color = selectCoverSeedColor(pixels = sampledPixels)
    return MiniPlayerPalette(
        containerColor = createMiniPlayerContainerColor(seedColor = seedColor),
    )
}

/**
 * 根据封面图片提取播放详情页配色。
 *
 * 播放页比迷你播放器更沉浸，因此保留更高的封面色占比；同时混入纸色保证文字可读。
 */
fun extractPlayerPagePalette(imageBitmap: ImageBitmap): PlayerPagePalette {
    if (imageBitmap.width <= 1 || imageBitmap.height <= 1) {
        return PlayerPagePalette(
            backgroundColor = MusicColors.Paper,
            ambientColor = MusicColors.Accent.copy(alpha = 0.18f),
        )
    }
    val sampledPixels: IntArray = readSampledPixels(imageBitmap = imageBitmap)
    val seedColor: Color = selectCoverSeedColor(pixels = sampledPixels)
    return PlayerPagePalette(
        backgroundColor = createPlayerPageBackgroundColor(seedColor = seedColor),
        ambientColor = createPlayerPageAmbientColor(seedColor = seedColor),
    )
}

// 把封面种子色转成浅色模式下可读的播放器容器色。
internal fun createMiniPlayerContainerColor(seedColor: Color): Color {
    return blendColors(
        start = seedColor,
        end = MusicColors.Paper,
        endWeight = MINI_PLAYER_CONTAINER_PAPER_WEIGHT,
    )
}

// 把封面种子色转成整页底色，既明显取自封面又不压低文字对比度。
internal fun createPlayerPageBackgroundColor(seedColor: Color): Color {
    return blendColors(
        start = seedColor,
        end = MusicColors.Paper,
        endWeight = PLAYER_PAGE_BACKGROUND_PAPER_WEIGHT,
    )
}

// 页面氛围色保留更多封面原色，用于渐变光晕而不是承载正文。
internal fun createPlayerPageAmbientColor(seedColor: Color): Color {
    return blendColors(
        start = seedColor,
        end = MusicColors.Paper,
        endWeight = PLAYER_PAGE_AMBIENT_PAPER_WEIGHT,
    )
}

// 大封面只读取均匀取样点，避免每次切歌都遍历超大位图。
private fun readSampledPixels(imageBitmap: ImageBitmap): IntArray {
    val sampleWidth: Int = min(imageBitmap.width, MAX_SAMPLE_EDGE)
    val sampleHeight: Int = min(imageBitmap.height, MAX_SAMPLE_EDGE)
    val xStep: Int = max(imageBitmap.width / sampleWidth, 1)
    val yStep: Int = max(imageBitmap.height / sampleHeight, 1)
    val rowBuffer: IntArray = IntArray(imageBitmap.width)
    val sampledPixels: IntArray = IntArray(sampleWidth * sampleHeight)
    var sampleIndex: Int = 0
    for (rowIndex: Int in 0 until sampleHeight) {
        val sourceY: Int = min(rowIndex * yStep, imageBitmap.height - 1)
        imageBitmap.readPixels(
            buffer = rowBuffer,
            startY = sourceY,
            width = imageBitmap.width,
            height = 1,
        )
        for (columnIndex: Int in 0 until sampleWidth) {
            val sourceX: Int = min(columnIndex * xStep, imageBitmap.width - 1)
            sampledPixels[sampleIndex] = rowBuffer[sourceX]
            sampleIndex += 1
        }
    }
    return sampledPixels
}

// 混合两个 [Color]，用于把封面色收敛到主题表面色。
private fun blendColors(start: Color, end: Color, endWeight: Float): Color {
    val startWeight: Float = 1f - endWeight
    return Color(
        red = start.red * startWeight + end.red * endWeight,
        green = start.green * startWeight + end.green * endWeight,
        blue = start.blue * startWeight + end.blue * endWeight,
        alpha = start.alpha * startWeight + end.alpha * endWeight,
    )
}
