package com.yanhao.kmpmusic.qa

import androidx.compose.ui.awt.ComposeWindow
import kotlinx.coroutines.delay
import java.awt.Point
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage

/**
 * 从本次 QA 窗口自动采集 claim 对应的三帧，并检查截图非空和状态确实变化。
 */
internal class DesktopUiQaCaptureRunner(
    private val config: DesktopUiQaConfig,
) {
    // 帧写入与像素校验交给单独模块，运行器只保留场景时序。
    private val frameVerifier: DesktopUiQaFrameVerifier = DesktopUiQaFrameVerifier(config = config)

    /** 根据场景执行滚动条或播放动画取证。 */
    suspend fun capture(window: ComposeWindow) {
        frameVerifier.prepareOutputDirectory()
        val location: Point = window.locationOnScreen
        val bounds: Rectangle =
            Rectangle(
                location.x,
                location.y,
                window.width,
                window.height,
            )
        check(
            bounds.width == DesktopUiQaCaptureSpec.WINDOW_WIDTH &&
                bounds.height == DesktopUiQaCaptureSpec.WINDOW_HEIGHT,
        ) {
            "Desktop UI QA 窗口尺寸错误: ${bounds.width}x${bounds.height}"
        }
        val robot: Robot = Robot(window.graphicsConfiguration.device)
        robot.autoDelay = DesktopUiQaCaptureSpec.ROBOT_ACTION_DELAY_MILLIS
        activateDesktopUiQaWindow(window = window, robot = robot)
        when (config.scenario.captureMode) {
            DesktopUiQaCaptureMode.Scrollbar -> {
                captureScrollbarFrames(
                    window = window,
                    robot = robot,
                    bounds = bounds,
                )
            }

            DesktopUiQaCaptureMode.PlaybackAnimation -> {
                capturePlaybackAnimationFrames(robot = robot, bounds = bounds)
            }
        }
    }

    // 滚动条场景覆盖初始隐藏、滚动后显示和停止五秒后隐藏。
    private suspend fun captureScrollbarFrames(
        window: ComposeWindow,
        robot: Robot,
        bounds: Rectangle,
    ) {
        val initialFrame: BufferedImage =
            frameVerifier.captureFrame(
                robot = robot,
                bounds = bounds,
                fileName = DesktopUiQaCaptureSpec.INITIAL_FRAME_FILE_NAME,
            )
        dragDesktopUiQaScrollbar(
            window = window,
            robot = robot,
            scenario = config.scenario,
        )
        delay(timeMillis = DesktopUiQaCaptureSpec.SCROLLBAR_ACTIVE_CAPTURE_DELAY_MILLIS)
        val activeFrame: BufferedImage =
            frameVerifier.captureFrame(
                robot = robot,
                bounds = bounds,
                fileName = DesktopUiQaCaptureSpec.ACTIVE_FRAME_FILE_NAME,
            )
        delay(timeMillis = DesktopUiQaCaptureSpec.SCROLLBAR_SETTLED_CAPTURE_DELAY_MILLIS)
        val settledFrame: BufferedImage =
            frameVerifier.captureFrame(
                robot = robot,
                bounds = bounds,
                fileName = DesktopUiQaCaptureSpec.SETTLED_FRAME_FILE_NAME,
            )
        frameVerifier.verifyFrameChange(
            firstFrame = initialFrame,
            secondFrame = activeFrame,
            minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_SCROLL_CHANGED_PIXELS,
            maximumChangedPixels = null,
            claim = "滚动后列表画面应发生变化",
        )
        frameVerifier.verifyRegionChange(
            firstFrame = activeFrame,
            secondFrame = settledFrame,
            region = DesktopUiQaCaptureSpec.appContentRegion(),
            minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_SCROLLBAR_CHANGED_PIXELS,
            maximumChangedPixels = DesktopUiQaCaptureSpec.MAXIMUM_SCROLLBAR_CHANGED_PIXELS,
            claim = "停止滚动五秒后滚动条应消失",
        )
        frameVerifier.verifyRegionChange(
            firstFrame = activeFrame,
            secondFrame = settledFrame,
            region = DesktopUiQaCaptureSpec.scrollbarChangeRegion(),
            minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_SCROLLBAR_CHANGED_PIXELS,
            maximumChangedPixels = null,
            claim = "滚动条区域应在五秒边界前后发生变化",
        )
        frameVerifier.verifyStableWindowShell(firstFrame = initialFrame, secondFrame = activeFrame)
        frameVerifier.verifyStableWindowShell(firstFrame = activeFrame, secondFrame = settledFrame)
    }

    // 播放场景在不同动画周期取三帧，证明等高器不是静态图标。
    private suspend fun capturePlaybackAnimationFrames(
        robot: Robot,
        bounds: Rectangle,
    ) {
        val initialFrame: BufferedImage =
            frameVerifier.captureFrame(
                robot = robot,
                bounds = bounds,
                fileName = DesktopUiQaCaptureSpec.INITIAL_FRAME_FILE_NAME,
            )
        delay(timeMillis = DesktopUiQaCaptureSpec.FIRST_ANIMATION_FRAME_DELAY_MILLIS)
        val activeFrame: BufferedImage =
            frameVerifier.captureFrame(
                robot = robot,
                bounds = bounds,
                fileName = DesktopUiQaCaptureSpec.ACTIVE_FRAME_FILE_NAME,
            )
        delay(timeMillis = DesktopUiQaCaptureSpec.SECOND_ANIMATION_FRAME_DELAY_MILLIS)
        val settledFrame: BufferedImage =
            frameVerifier.captureFrame(
                robot = robot,
                bounds = bounds,
                fileName = DesktopUiQaCaptureSpec.SETTLED_FRAME_FILE_NAME,
            )
        frameVerifier.verifyRegionChange(
            firstFrame = initialFrame,
            secondFrame = activeFrame,
            region = DesktopUiQaCaptureSpec.playbackAnimationRegion(),
            minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_ANIMATION_CHANGED_PIXELS,
            maximumChangedPixels = null,
            claim = "播放动画前两帧应发生变化",
        )
        frameVerifier.verifyRegionChange(
            firstFrame = activeFrame,
            secondFrame = settledFrame,
            region = DesktopUiQaCaptureSpec.playbackAnimationRegion(),
            minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_ANIMATION_CHANGED_PIXELS,
            maximumChangedPixels = null,
            claim = "播放动画后两帧应发生变化",
        )
        frameVerifier.verifyStableWindowShell(firstFrame = initialFrame, secondFrame = activeFrame)
        frameVerifier.verifyStableWindowShell(firstFrame = activeFrame, secondFrame = settledFrame)
    }
}
