package com.yanhao.kmpmusic.qa

import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.max

/**
 * Desktop UI QA 帧写入与像素校验模块，集中守住证据非空、变化范围和窗口身份。
 */
internal class DesktopUiQaFrameVerifier(
    private val config: DesktopUiQaConfig,
) {
    /** 创建调用方指定的证据目录，不隐式回退到其他位置。 */
    fun prepareOutputDirectory() {
        Files.createDirectories(config.outputDirectory)
    }

    /** 截取本次进程的固定窗口边界，并拒绝空白图。 */
    fun captureFrame(
        robot: Robot,
        bounds: Rectangle,
        fileName: String,
    ): BufferedImage {
        val image: BufferedImage = robot.createScreenCapture(bounds)
        verifyFrameIsNotBlank(image = image)
        val outputPath: Path = config.outputDirectory.resolve(fileName)
        val wasWritten: Boolean = ImageIO.write(image, "png", outputPath.toFile())
        check(wasWritten && Files.size(outputPath) > 0L) { "Desktop UI QA 截图写入失败: $outputPath" }
        println("[desktop-ui-qa] captured=$outputPath size=${image.width}x${image.height}")
        return image
    }

    /** 检查 claim 对应的帧变化下限，并在需要时限制异常大范围变化。 */
    fun verifyFrameChange(
        firstFrame: BufferedImage,
        secondFrame: BufferedImage,
        minimumChangedPixels: Int,
        maximumChangedPixels: Int?,
        claim: String,
    ) {
        check(firstFrame.width == secondFrame.width && firstFrame.height == secondFrame.height) {
            "Desktop UI QA 帧尺寸不一致"
        }
        val changedPixels: Int = countChangedPixels(firstFrame = firstFrame, secondFrame = secondFrame)
        println("[desktop-ui-qa] claim=$claim changedPixels=$changedPixels")
        check(changedPixels >= minimumChangedPixels) {
            "$claim，实际变化像素 $changedPixels，最低要求 $minimumChangedPixels"
        }
        if (maximumChangedPixels != null) {
            check(changedPixels <= maximumChangedPixels) {
                "$claim，但变化像素 $changedPixels 超过上限 $maximumChangedPixels，可能存在遮挡或内容仍在移动"
            }
        }
    }

    /** 只在 claim 对应区域内统计变化，拒绝用无关页面动画冒充目标行为。 */
    fun verifyRegionChange(
        firstFrame: BufferedImage,
        secondFrame: BufferedImage,
        region: Rectangle,
        minimumChangedPixels: Int,
        maximumChangedPixels: Int? = null,
        claim: String,
    ) {
        check(firstFrame.width == secondFrame.width && firstFrame.height == secondFrame.height) {
            "Desktop UI QA 帧尺寸不一致"
        }
        check(
            region.x >= 0 &&
                region.y >= 0 &&
                region.x + region.width <= firstFrame.width &&
                region.y + region.height <= firstFrame.height,
        ) {
            "Desktop UI QA 验收区域越界: $region"
        }
        val changedPixels: Int =
            countChangedPixels(
                firstFrame = firstFrame,
                secondFrame = secondFrame,
                region = region,
            )
        println("[desktop-ui-qa] claim=$claim changedPixels=$changedPixels region=$region")
        check(changedPixels >= minimumChangedPixels) {
            "$claim，区域变化像素 $changedPixels，最低要求 $minimumChangedPixels"
        }
        if (maximumChangedPixels != null) {
            check(changedPixels <= maximumChangedPixels) {
                "$claim，但区域变化像素 $changedPixels 超过上限 $maximumChangedPixels，可能存在遮挡或内容仍在移动"
            }
        }
    }

    /** 固定 shell 应在三帧间保持稳定，外部悬浮层或窗口遮挡会破坏这些区域。 */
    fun verifyStableWindowShell(
        firstFrame: BufferedImage,
        secondFrame: BufferedImage,
    ) {
        var changedPixels: Int = 0
        for (region: Rectangle in DesktopUiQaCaptureSpec.stableShellRegions(scenario = config.scenario)) {
            for (x: Int in region.x until region.x + region.width) {
                for (y: Int in region.y until region.y + region.height) {
                    if (firstFrame.getRGB(x, y) != secondFrame.getRGB(x, y)) {
                        changedPixels += 1
                    }
                }
            }
        }
        check(changedPixels <= DesktopUiQaCaptureSpec.MAXIMUM_STABLE_SHELL_CHANGED_PIXELS) {
            "Desktop UI QA 窗口身份不稳定，固定 shell 区域变化像素=$changedPixels"
        }
    }

    // 采样颜色数量可以快速识别权限问题或错误窗口导致的纯色截图。
    private fun verifyFrameIsNotBlank(image: BufferedImage) {
        val colors: MutableSet<Int> = mutableSetOf()
        val horizontalStep: Int = max(a = 1, b = image.width / DesktopUiQaCaptureSpec.SAMPLE_GRID_SIZE)
        val verticalStep: Int = max(a = 1, b = image.height / DesktopUiQaCaptureSpec.SAMPLE_GRID_SIZE)
        for (x: Int in 0 until image.width step horizontalStep) {
            for (y: Int in 0 until image.height step verticalStep) {
                colors.add(image.getRGB(x, y))
            }
        }
        check(colors.size >= DesktopUiQaCaptureSpec.MINIMUM_SAMPLED_COLORS) {
            "Desktop UI QA 截图疑似空白，采样颜色数=${colors.size}"
        }
    }

    // 全帧差异计数为不同 claim 复用，阈值由运行器按场景提供。
    private fun countChangedPixels(
        firstFrame: BufferedImage,
        secondFrame: BufferedImage,
        region: Rectangle = Rectangle(0, 0, firstFrame.width, firstFrame.height),
    ): Int {
        var changedPixels: Int = 0
        for (x: Int in region.x until region.x + region.width) {
            for (y: Int in region.y until region.y + region.height) {
                if (firstFrame.getRGB(x, y) != secondFrame.getRGB(x, y)) {
                    changedPixels += 1
                }
            }
        }
        return changedPixels
    }
}
