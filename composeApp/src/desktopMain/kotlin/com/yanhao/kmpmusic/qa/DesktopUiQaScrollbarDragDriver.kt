package com.yanhao.kmpmusic.qa

import androidx.compose.ui.awt.ComposeWindow
import java.awt.Point
import java.awt.Robot
import java.awt.event.InputEvent

/**
 * 在截图与拖拽前激活 QA 窗口，避免 macOS 将第一次鼠标按下仅用于窗口聚焦。
 */
internal fun activateDesktopUiQaWindow(
    window: ComposeWindow,
    robot: Robot,
) {
    val contentLocation: Point = window.contentPane.locationOnScreen
    robot.mouseMove(
        contentLocation.x + window.contentPane.width / 2,
        contentLocation.y + DesktopUiQaCaptureSpec.WINDOW_ACTIVATION_Y_OFFSET_PX,
    )
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
}

/**
 * 向隐藏状态的滚动条命中区发送连续小步拖动，覆盖真实鼠标难以拖动的回归场景。
 */
internal fun dragDesktopUiQaScrollbar(
    window: ComposeWindow,
    robot: Robot,
    scenario: DesktopUiQaScenario,
) {
    val contentLocation: Point = window.contentPane.locationOnScreen
    val edgeInsetPx: Int = resolveDesktopUiQaScrollbarEdgeInset(scenario = scenario)
    val screenX: Int = contentLocation.x + window.contentPane.width - edgeInsetPx
    val startScreenY: Int = contentLocation.y + window.contentPane.height * 2 / 5
    robot.mouseMove(screenX, startScreenY)
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
    repeat(times = DesktopUiQaCaptureSpec.SCROLLBAR_DRAG_STEP_COUNT) { stepIndex: Int ->
        robot.mouseMove(
            screenX,
            startScreenY + (stepIndex + 1) * DesktopUiQaCaptureSpec.SCROLLBAR_DRAG_STEP_PX,
        )
    }
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
}

// 页面内边距不同，使用各自已知滚动条中心，避免测试驱动器拖到空白区域。
private fun resolveDesktopUiQaScrollbarEdgeInset(scenario: DesktopUiQaScenario): Int =
    when (scenario) {
        DesktopUiQaScenario.Albums -> DesktopUiQaCaptureSpec.ALBUMS_SCROLLBAR_DRAG_EDGE_INSET_PX

        DesktopUiQaScenario.Home,
        DesktopUiQaScenario.RecentPlayed,
        DesktopUiQaScenario.Artists,
        DesktopUiQaScenario.ArtistDetailCompact,
        DesktopUiQaScenario.ArtistDetailWide,
        DesktopUiQaScenario.Favorites,
        DesktopUiQaScenario.AlbumDetail,
        DesktopUiQaScenario.AlbumDetailPlaying,
        -> DesktopUiQaCaptureSpec.HOME_ARTISTS_SCROLLBAR_DRAG_EDGE_INSET_PX

        DesktopUiQaScenario.HomePlaying,
        DesktopUiQaScenario.Me,
        DesktopUiQaScenario.ArtistDetail,
        DesktopUiQaScenario.ArtistDetailPlaying,
        DesktopUiQaScenario.ArtistDetailNoCover,
        DesktopUiQaScenario.ArtistDetailInteraction,
        DesktopUiQaScenario.Playlists,
        DesktopUiQaScenario.PlaylistManagement,
        DesktopUiQaScenario.Search,
        DesktopUiQaScenario.SearchPlaying,
        DesktopUiQaScenario.SearchAlbums,
        DesktopUiQaScenario.SearchArtists,
        DesktopUiQaScenario.SearchPlaylists,
        DesktopUiQaScenario.SearchEmpty,
        -> error("当前场景不支持滚动条拖动")
    }
