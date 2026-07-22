package com.yanhao.kmpmusic.qa

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Desktop UI QA 命令行契约测试，避免场景名与脚本路由漂移。
 */
class DesktopUiQaScenarioTest {
    /** 七个公开场景名必须稳定解析到对应页面和取证模式。 */
    @Test
    fun supportedScenariosParseWithExpectedCaptureModes() {
        assertEquals(expected = DesktopUiQaCaptureMode.Scrollbar, actual = DesktopUiQaScenario.parse(argument = "home").captureMode)
        assertEquals(
            expected = DesktopUiQaCaptureMode.PlaybackAnimation,
            actual = DesktopUiQaScenario.parse(argument = "home-playing").captureMode,
        )
        assertEquals(expected = DesktopUiQaCaptureMode.Scrollbar, actual = DesktopUiQaScenario.parse(argument = "albums").captureMode)
        assertEquals(expected = DesktopUiQaCaptureMode.Scrollbar, actual = DesktopUiQaScenario.parse(argument = "artists").captureMode)
        assertEquals(expected = DesktopUiQaCaptureMode.Scrollbar, actual = DesktopUiQaScenario.parse(argument = "favorites").captureMode)
        assertEquals(expected = DesktopUiQaCaptureMode.Static, actual = DesktopUiQaScenario.parse(argument = "playlists").captureMode)
        assertEquals(
            expected = DesktopUiQaCaptureMode.Static,
            actual = DesktopUiQaScenario.parse(argument = "playlist-management").captureMode,
        )
    }

    /** 收藏页使用用户确认的 1280x1024，既有场景继续保持原始尺寸。 */
    @Test
    fun favoritesUsesFigmaWindowSizeWithoutChangingExistingScenarios() {
        assertEquals(expected = 1280, actual = DesktopUiQaScenario.Favorites.windowWidth)
        assertEquals(expected = 1024, actual = DesktopUiQaScenario.Favorites.windowHeight)
        assertEquals(expected = DesktopUiQaCaptureSpec.WINDOW_WIDTH, actual = DesktopUiQaScenario.Home.windowWidth)
        assertEquals(expected = DesktopUiQaCaptureSpec.WINDOW_HEIGHT, actual = DesktopUiQaScenario.Home.windowHeight)
        assertEquals(expected = DesktopUiQaCaptureSpec.WINDOW_WIDTH, actual = DesktopUiQaScenario.Playlists.windowWidth)
        assertEquals(expected = DesktopUiQaCaptureSpec.WINDOW_HEIGHT, actual = DesktopUiQaScenario.PlaylistManagement.windowHeight)
    }

    /** 未知场景必须立即报错，不能静默退回其他页面生成错误证据。 */
    @Test
    fun unknownScenarioFails() {
        assertFailsWith<IllegalStateException> {
            DesktopUiQaScenario.parse(argument = "unknown")
        }
    }

    /** 配置同时保留场景和调用方指定的证据目录。 */
    @Test
    fun configParsesScenarioAndOutputDirectory() {
        val config: DesktopUiQaConfig =
            DesktopUiQaConfig.parse(
                args = arrayOf("artists", "/tmp/kmp-music-ui-qa"),
            )
        assertEquals(expected = DesktopUiQaScenario.Artists, actual = config.scenario)
        assertEquals(expected = Path.of("/tmp/kmp-music-ui-qa"), actual = config.outputDirectory)
    }
}
