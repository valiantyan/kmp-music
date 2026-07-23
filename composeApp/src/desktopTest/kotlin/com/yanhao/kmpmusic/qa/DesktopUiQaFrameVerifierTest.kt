package com.yanhao.kmpmusic.qa

import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Desktop UI QA 证据身份回归测试，覆盖真实取证中出现过的遮挡与焦点噪音。
 */
class DesktopUiQaFrameVerifierTest {
    /** 固定侧栏内出现外部悬浮层时必须拒绝证据。 */
    @Test
    fun overlayInsideStableShellFails() {
        val initialFrame: BufferedImage = createFrame()
        val overlaidFrame: BufferedImage = copyFrame(source = initialFrame)
        overlaidFrame.createGraphics().use { graphics ->
            graphics.color = Color.WHITE
            graphics.fillRect(0, 300, 48, 48)
        }

        assertFailsWith<IllegalStateException> {
            createVerifier().verifyStableWindowShell(
                firstFrame = initialFrame,
                secondFrame = overlaidFrame,
            )
        }
    }

    /** macOS 原生标题栏失焦变色不应污染应用证据判断。 */
    @Test
    fun nativeTitleBarFocusChangeIsIgnored() {
        val focusedFrame: BufferedImage = createFrame()
        val unfocusedFrame: BufferedImage = copyFrame(source = focusedFrame)
        unfocusedFrame.createGraphics().use { graphics ->
            graphics.color = Color.GRAY
            graphics.fillRect(0, 0, DesktopUiQaCaptureSpec.WINDOW_WIDTH, DesktopUiQaCaptureSpec.STABLE_CHROME_TOP)
        }

        createVerifier().verifyStableWindowShell(
            firstFrame = focusedFrame,
            secondFrame = unfocusedFrame,
        )
    }

    /** 目标区域外的变化不能冒充首页播放均衡器动画。 */
    @Test
    fun unrelatedAnimationOutsideTargetRegionFails() {
        val initialFrame: BufferedImage = createFrame()
        val unrelatedAnimationFrame: BufferedImage = copyFrame(source = initialFrame)
        unrelatedAnimationFrame.createGraphics().use { graphics ->
            graphics.color = Color.RED
            graphics.fillRect(600, 400, 40, 40)
        }

        assertFailsWith<IllegalStateException> {
            createVerifier().verifyRegionChange(
                firstFrame = initialFrame,
                secondFrame = unrelatedAnimationFrame,
                region = DesktopUiQaCaptureSpec.playbackAnimationRegion(scenario = DesktopUiQaScenario.HomePlaying),
                minimumChangedPixels = DesktopUiQaCaptureSpec.MINIMUM_ANIMATION_CHANGED_PIXELS,
                maximumChangedPixels = null,
                claim = "播放动画应发生变化",
            )
        }
    }

    private fun createVerifier(): DesktopUiQaFrameVerifier =
        DesktopUiQaFrameVerifier(
            config =
                DesktopUiQaConfig(
                    scenario = DesktopUiQaScenario.Artists,
                    outputDirectory = Path.of("build", "desktop-ui-qa-test"),
                ),
        )

    private fun createFrame(): BufferedImage =
        BufferedImage(
            DesktopUiQaCaptureSpec.WINDOW_WIDTH,
            DesktopUiQaCaptureSpec.WINDOW_HEIGHT,
            BufferedImage.TYPE_INT_ARGB,
        ).also { image: BufferedImage ->
            image.createGraphics().use { graphics ->
                graphics.color = Color.BLACK
                graphics.fillRect(0, 0, image.width, image.height)
            }
        }

    private fun copyFrame(source: BufferedImage): BufferedImage =
        BufferedImage(
            source.width,
            source.height,
            source.type,
        ).also { copy: BufferedImage ->
            copy.createGraphics().use { graphics ->
                graphics.drawImage(source, 0, 0, null)
            }
        }
}

private inline fun <R> Graphics2D.use(block: (Graphics2D) -> R): R =
    try {
        block(this)
    } finally {
        dispose()
    }
