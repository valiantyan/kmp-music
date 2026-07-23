package com.yanhao.kmpmusic.qa

import androidx.compose.ui.awt.ComposeWindow
import com.yanhao.kmpmusic.isMacosHost
import kotlinx.coroutines.delay
import java.awt.Point
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.InputEvent
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
        val robot: Robot = Robot(window.graphicsConfiguration.device)
        robot.autoDelay = DesktopUiQaCaptureSpec.ROBOT_ACTION_DELAY_MILLIS
        activateDesktopUiQaWindow(window = window, robot = robot)
        if (isMacosHost()) {
            verifyDesktopUiQaTitleBarCanDrag(window = window, robot = robot)
        }
        val location: Point = window.locationOnScreen
        val bounds: Rectangle =
            Rectangle(
                location.x,
                location.y,
                window.width,
                window.height,
            )
        check(
            bounds.width == config.scenario.windowWidth &&
                bounds.height == config.scenario.windowHeight,
        ) {
            "Desktop UI QA 窗口尺寸错误: ${bounds.width}x${bounds.height}"
        }
        when (config.scenario.captureMode) {
            DesktopUiQaCaptureMode.Static -> {
                captureStaticFrames(robot = robot, bounds = bounds)
            }

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

            DesktopUiQaCaptureMode.Interaction -> {
                captureArtistDetailInteractionFrames(window = window, robot = robot, bounds = bounds)
            }
        }
    }

    // 静态设计稿页面仍输出三帧，证明截图来自本次真实窗口但不伪造滚动或动画 claim。
    private suspend fun captureStaticFrames(
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
        frameVerifier.verifyStableWindowShell(firstFrame = initialFrame, secondFrame = activeFrame)
        frameVerifier.verifyStableWindowShell(firstFrame = activeFrame, secondFrame = settledFrame)
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
            region = DesktopUiQaCaptureSpec.appContentRegion(scenario = config.scenario),
            minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_SCROLLBAR_CHANGED_PIXELS,
            maximumChangedPixels = DesktopUiQaCaptureSpec.MAXIMUM_SCROLLBAR_CHANGED_PIXELS,
            claim = "停止滚动五秒后滚动条应消失",
        )
        frameVerifier.verifyRegionChange(
            firstFrame = activeFrame,
            secondFrame = settledFrame,
            region = DesktopUiQaCaptureSpec.scrollbarChangeRegion(scenario = config.scenario),
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
            region = DesktopUiQaCaptureSpec.playbackAnimationRegion(scenario = config.scenario),
            minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_ANIMATION_CHANGED_PIXELS,
            maximumChangedPixels = null,
            claim = "播放动画前两帧应发生变化",
        )
        frameVerifier.verifyRegionChange(
            firstFrame = activeFrame,
            secondFrame = settledFrame,
            region = DesktopUiQaCaptureSpec.playbackAnimationRegion(scenario = config.scenario),
            minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_ANIMATION_CHANGED_PIXELS,
            maximumChangedPixels = null,
            claim = "播放动画后两帧应发生变化",
        )
        frameVerifier.verifyStableWindowShell(firstFrame = initialFrame, secondFrame = activeFrame)
        frameVerifier.verifyStableWindowShell(firstFrame = activeFrame, secondFrame = settledFrame)
    }

    /** 依次取证返回、hero 主动作和歌曲行的悬停/按下反馈，避免仅凭代码声明交互态。 */
    private suspend fun captureArtistDetailInteractionFrames(
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
        moveMouseToArtistDetailInteractionTarget(
            robot = robot,
            window = window,
            point = DesktopUiQaCaptureSpec.artistDetailBackButtonPoint(),
        )
        val backHoveredFrame: BufferedImage =
            frameVerifier.captureFrame(
                robot = robot,
                bounds = bounds,
                fileName = DesktopUiQaCaptureSpec.ACTIVE_FRAME_FILE_NAME,
            )
        frameVerifier.verifyRegionChange(
            firstFrame = initialFrame,
            secondFrame = backHoveredFrame,
            region = DesktopUiQaCaptureSpec.artistDetailBackButtonRegion(),
            minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_INTERACTION_CHANGED_PIXELS,
            claim = "悬浮返回按钮应提高背景不透明度",
        )
        moveMouseToArtistDetailInteractionTarget(
            robot = robot,
            window = window,
            point = DesktopUiQaCaptureSpec.artistDetailPlayAllButtonPoint(),
        )
        val playAllHoveredFrame: BufferedImage =
            frameVerifier.captureFrame(
                robot = robot,
                bounds = bounds,
                fileName = DesktopUiQaCaptureSpec.SETTLED_FRAME_FILE_NAME,
            )
        frameVerifier.verifyRegionChange(
            firstFrame = backHoveredFrame,
            secondFrame = playAllHoveredFrame,
            region = DesktopUiQaCaptureSpec.artistDetailPlayAllButtonRegion(),
            minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_INTERACTION_CHANGED_PIXELS,
            claim = "悬浮播放全部按钮应提高背景亮度",
        )
        moveMouseToArtistDetailInteractionTarget(
            robot = robot,
            window = window,
            point = DesktopUiQaCaptureSpec.artistDetailFirstSongRowPoint(),
        )
        val rowHoveredFrame: BufferedImage =
            frameVerifier.captureFrame(
                robot = robot,
                bounds = bounds,
                fileName = "04-row-hover.png",
            )
        frameVerifier.verifyRegionChange(
            firstFrame = playAllHoveredFrame,
            secondFrame = rowHoveredFrame,
            region = DesktopUiQaCaptureSpec.artistDetailFirstSongRowRegion(),
            minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_INTERACTION_CHANGED_PIXELS,
            claim = "悬浮歌曲行应显示浅色背景",
        )
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
        delay(timeMillis = DesktopUiQaCaptureSpec.ROBOT_ACTION_DELAY_MILLIS.toLong())
        val rowPressedFrame: BufferedImage =
            frameVerifier.captureFrame(
                robot = robot,
                bounds = bounds,
                fileName = DesktopUiQaCaptureSpec.PRESSED_FRAME_FILE_NAME,
            )
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
        frameVerifier.verifyRegionChange(
            firstFrame = rowHoveredFrame,
            secondFrame = rowPressedFrame,
            region = DesktopUiQaCaptureSpec.artistDetailFirstSongRowRegion(),
            minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_INTERACTION_CHANGED_PIXELS,
            claim = "按下歌曲行应加深浅色背景",
        )
        frameVerifier.verifyStableWindowShell(firstFrame = initialFrame, secondFrame = backHoveredFrame)
        frameVerifier.verifyStableWindowShell(firstFrame = backHoveredFrame, secondFrame = playAllHoveredFrame)
        frameVerifier.verifyStableWindowShell(firstFrame = playAllHoveredFrame, secondFrame = rowHoveredFrame)
        frameVerifier.verifyStableWindowShell(firstFrame = rowHoveredFrame, secondFrame = rowPressedFrame)
    }

    /** 坐标以应用内容区域为基准，避免 macOS 标题栏偏移后把鼠标移动到错误控件。 */
    private suspend fun moveMouseToArtistDetailInteractionTarget(
        robot: Robot,
        window: ComposeWindow,
        point: Point,
    ) {
        val contentLocation: Point = window.contentPane.locationOnScreen
        robot.mouseMove(contentLocation.x + point.x, contentLocation.y + point.y)
        delay(timeMillis = DesktopUiQaCaptureSpec.ROBOT_ACTION_DELAY_MILLIS.toLong())
    }
}
