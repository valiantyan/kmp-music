package com.yanhao.kmpmusic.qa

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Desktop UI QA 命令行契约测试，避免场景名与脚本路由漂移。
 */
class DesktopUiQaScenarioTest {
    /** 歌手详情与既有公开场景名必须稳定解析到对应页面和取证模式。 */
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
        assertEquals(expected = DesktopUiQaCaptureMode.Static, actual = DesktopUiQaScenario.parse(argument = "me").captureMode)
        assertEquals(
            expected = DesktopUiQaCaptureMode.Scrollbar,
            actual = DesktopUiQaScenario.parse(argument = "recent-played").captureMode,
        )
        assertEquals(expected = DesktopUiQaCaptureMode.Scrollbar, actual = DesktopUiQaScenario.parse(argument = "album-detail").captureMode)
        assertEquals(
            expected = DesktopUiQaCaptureMode.PlaybackAnimation,
            actual = DesktopUiQaScenario.parse(argument = "album-detail-playing").captureMode,
        )
        assertEquals(
            expected = DesktopUiQaCaptureMode.Scrollbar,
            actual = DesktopUiQaScenario.parse(argument = "artist-detail-compact").captureMode,
        )
        assertEquals(expected = DesktopUiQaCaptureMode.Static, actual = DesktopUiQaScenario.parse(argument = "artist-detail").captureMode)
        assertEquals(
            expected = DesktopUiQaCaptureMode.Scrollbar,
            actual = DesktopUiQaScenario.parse(argument = "artist-detail-wide").captureMode,
        )
        assertEquals(
            expected = DesktopUiQaCaptureMode.PlaybackAnimation,
            actual = DesktopUiQaScenario.parse(argument = "artist-detail-playing").captureMode,
        )
        assertEquals(
            expected = DesktopUiQaCaptureMode.Static,
            actual = DesktopUiQaScenario.parse(argument = "artist-detail-no-cover").captureMode,
        )
        assertEquals(
            expected = DesktopUiQaCaptureMode.Interaction,
            actual = DesktopUiQaScenario.parse(argument = "artist-detail-interaction").captureMode,
        )
        assertEquals(expected = DesktopUiQaCaptureMode.Static, actual = DesktopUiQaScenario.parse(argument = "playlists").captureMode)
        assertEquals(
            expected = DesktopUiQaCaptureMode.Static,
            actual = DesktopUiQaScenario.parse(argument = "playlist-management").captureMode,
        )
        assertEquals(expected = DesktopUiQaCaptureMode.Static, actual = DesktopUiQaScenario.parse(argument = "search").captureMode)
        assertEquals(expected = DesktopUiQaCaptureMode.Static, actual = DesktopUiQaScenario.parse(argument = "search-playing").captureMode)
        assertEquals(expected = DesktopUiQaCaptureMode.Static, actual = DesktopUiQaScenario.parse(argument = "search-albums").captureMode)
        assertEquals(expected = DesktopUiQaCaptureMode.Static, actual = DesktopUiQaScenario.parse(argument = "search-artists").captureMode)
        assertEquals(expected = DesktopUiQaCaptureMode.Static, actual = DesktopUiQaScenario.parse(argument = "search-playlists").captureMode)
        assertEquals(expected = DesktopUiQaCaptureMode.Static, actual = DesktopUiQaScenario.parse(argument = "search-empty").captureMode)
    }

    /** 收藏页与我的页使用用户确认的 1280x1024，既有场景继续保持原始尺寸。 */
    @Test
    fun favoritesUsesFigmaWindowSizeWithoutChangingExistingScenarios() {
        assertEquals(expected = 1280, actual = DesktopUiQaScenario.Favorites.windowWidth)
        assertEquals(expected = 1024, actual = DesktopUiQaScenario.Favorites.windowHeight)
        assertEquals(expected = 1280, actual = DesktopUiQaScenario.Me.windowWidth)
        assertEquals(expected = 1024, actual = DesktopUiQaScenario.Me.windowHeight)
        assertEquals(expected = DesktopUiQaCaptureSpec.WINDOW_WIDTH, actual = DesktopUiQaScenario.Home.windowWidth)
        assertEquals(expected = DesktopUiQaCaptureSpec.WINDOW_HEIGHT, actual = DesktopUiQaScenario.Home.windowHeight)
        assertEquals(expected = 1240, actual = DesktopUiQaScenario.AlbumDetail.windowWidth)
        assertEquals(expected = 824, actual = DesktopUiQaScenario.AlbumDetail.windowHeight)
        assertEquals(expected = 1120, actual = DesktopUiQaScenario.ArtistDetailCompact.windowWidth)
        assertEquals(expected = 760, actual = DesktopUiQaScenario.ArtistDetailCompact.windowHeight)
        assertEquals(expected = 1240, actual = DesktopUiQaScenario.ArtistDetail.windowWidth)
        assertEquals(expected = 800, actual = DesktopUiQaScenario.ArtistDetail.windowHeight)
        assertEquals(expected = 1440, actual = DesktopUiQaScenario.ArtistDetailWide.windowWidth)
        assertEquals(expected = 900, actual = DesktopUiQaScenario.ArtistDetailWide.windowHeight)
        assertEquals(
            expected = 1_200,
            actual = DesktopUiQaCaptureSpec.maximumStableShellChangedPixels(scenario = DesktopUiQaScenario.AlbumDetail),
        )
        assertEquals(
            expected = DesktopUiQaCaptureSpec.MAXIMUM_STABLE_SHELL_CHANGED_PIXELS,
            actual = DesktopUiQaCaptureSpec.maximumStableShellChangedPixels(scenario = DesktopUiQaScenario.Home),
        )
        assertEquals(expected = DesktopUiQaCaptureSpec.WINDOW_WIDTH, actual = DesktopUiQaScenario.Playlists.windowWidth)
        assertEquals(expected = DesktopUiQaCaptureSpec.WINDOW_HEIGHT, actual = DesktopUiQaScenario.PlaylistManagement.windowHeight)
        assertEquals(expected = 1240, actual = DesktopUiQaScenario.Search.windowWidth)
        assertEquals(expected = 824, actual = DesktopUiQaScenario.Search.windowHeight)
        assertEquals(expected = 1240, actual = DesktopUiQaScenario.SearchEmpty.windowWidth)
        assertEquals(expected = 824, actual = DesktopUiQaScenario.SearchEmpty.windowHeight)
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
