package com.yanhao.kmpmusic.qa

import java.awt.Rectangle
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * 校验 macOS 原生 traffic lights 与 Compose 品牌文字在同一视觉中心轴上。
 */
internal object DesktopUiQaMacosTitleBarVerifier {
    /**
     * 从本次窗口截图中定位红色 traffic light 与绿色品牌的像素边界，并比较垂直中心。
     */
    fun verifyBrandAlignment(frame: BufferedImage) {
        val trafficLightBounds: PixelBounds =
            findPixelBounds(
                frame = frame,
                region = TRAFFIC_LIGHT_SEARCH_REGION,
                predicate = ::isRedTrafficLightPixel,
                label = "红色 traffic light",
            )
        val brandBounds: PixelBounds =
            findPixelBounds(
                frame = frame,
                region = BRAND_SEARCH_REGION,
                predicate = ::isBrandPixel,
                label = "标题栏品牌",
            )
        val trafficLightCenterY: Double = trafficLightBounds.centerY
        val brandCenterY: Double = brandBounds.centerY
        val centerDeltaY: Double = abs(trafficLightCenterY - brandCenterY)
        println(
            "[desktop-ui-qa] claim=macOS 标题栏品牌与 traffic lights 垂直对齐 " +
                "trafficLightCenterY=$trafficLightCenterY brandCenterY=$brandCenterY deltaY=$centerDeltaY",
        )
        check(centerDeltaY <= CENTER_ALIGNMENT_TOLERANCE_PX) {
            "macOS 标题栏品牌未与 traffic lights 垂直对齐: " +
                "trafficLightCenterY=$trafficLightCenterY, brandCenterY=$brandCenterY, deltaY=$centerDeltaY"
        }
    }

    /**
     * 在限定标题栏区域中查找目标颜色像素，避免页面内容误参与标题栏测量。
     */
    private fun findPixelBounds(
        frame: BufferedImage,
        region: Rectangle,
        predicate: (Int) -> Boolean,
        label: String,
    ): PixelBounds {
        var left: Int = region.x + region.width
        var top: Int = region.y + region.height
        var right: Int = -1
        var bottom: Int = -1
        for (x: Int in region.x until region.x + region.width) {
            for (y: Int in region.y until region.y + region.height) {
                if (predicate(frame.getRGB(x, y))) {
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x)
                    bottom = maxOf(bottom, y)
                }
            }
        }
        check(right >= left && bottom >= top) { "未在标题栏截图中定位到$label" }
        return PixelBounds(left = left, top = top, right = right, bottom = bottom)
    }

    /** 仅匹配系统绘制的红色关闭按钮，排除相邻的黄色最小化按钮。 */
    private fun isRedTrafficLightPixel(color: Int): Boolean = red(color) >= 200 && green(color) in 30..150 && blue(color) <= 150

    /** 仅匹配品牌的深绿色抗锯齿像素，搜索区域避开其他页面元素。 */
    private fun isBrandPixel(color: Int): Boolean =
        red(color) <= 180 && green(color) in 80..200 && blue(color) in 60..190 &&
            green(color) - red(color) >= 25

    /** 提取 ARGB 中的红色通道。 */
    private fun red(color: Int): Int = color ushr RED_SHIFT and COLOR_CHANNEL_MASK

    /** 提取 ARGB 中的绿色通道。 */
    private fun green(color: Int): Int = color ushr GREEN_SHIFT and COLOR_CHANNEL_MASK

    /** 提取 ARGB 中的蓝色通道。 */
    private fun blue(color: Int): Int = color and COLOR_CHANNEL_MASK

    /**
     * 像素边界，用于在不依赖字体度量的情况下计算截图中元素的视觉中心。
     *
     * @property left 左侧包含像素的坐标。
     * @property top 顶部包含像素的坐标。
     * @property right 右侧包含像素的坐标。
     * @property bottom 底部包含像素的坐标。
     */
    private data class PixelBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        /** 垂直视觉中心。 */
        val centerY: Double = (top + bottom) / 2.0
    }

    /** traffic light 在固定 QA 截图左上角的搜索区域。 */
    private val TRAFFIC_LIGHT_SEARCH_REGION: Rectangle = Rectangle(0, 0, 30, 30)

    /** 品牌文字在固定 QA 截图左上角的搜索区域。 */
    private val BRAND_SEARCH_REGION: Rectangle = Rectangle(80, 0, 110, 30)

    /** 两类元素允许的最大垂直中心轴误差（1px）。 */
    private const val CENTER_ALIGNMENT_TOLERANCE_PX: Double = 1.0

    /** ARGB 单个颜色通道的位掩码。 */
    private const val COLOR_CHANNEL_MASK: Int = 0xFF

    /** ARGB 红色通道的右移位数。 */
    private const val RED_SHIFT: Int = 16

    /** ARGB 绿色通道的右移位数。 */
    private const val GREEN_SHIFT: Int = 8
}
