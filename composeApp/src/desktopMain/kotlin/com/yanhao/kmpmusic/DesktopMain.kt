package com.yanhao.kmpmusic

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicApp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import java.awt.AWTEvent
import java.awt.Color as AwtColor
import java.awt.Component
import java.awt.Point
import java.awt.Window as AwtWindow
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseEvent
import java.awt.Toolkit
import java.awt.geom.RoundRectangle2D
import javax.swing.SwingUtilities
import kotlin.math.roundToInt

/**
 * macOS 原生窗口圆角通常在 10-12pt 区间，这里用 12px 保持接近系统窗口观感。
 */
private const val MACOS_WINDOW_CORNER_RADIUS_PX: Double = 12.0

/**
 * Desktop 入口。
 */
fun main() = application {
    val windowState = rememberWindowState(
        width = DesktopMusicDimens.DefaultWindowWidth,
        height = DesktopMusicDimens.DefaultWindowHeight,
    )
    val closeWindow: () -> Unit = {
        DesktopPlaybackSession.close()
        exitApplication()
    }
    Window(
        onCloseRequest = closeWindow,
        title = "KMP Music",
        undecorated = true,
        state = windowState,
    ) {
        val density = LocalDensity.current
        LaunchedEffect(Unit) {
            DesktopPlaybackSession.ensurePlaybackSnapshotRestoreRequested()
        }
        DisposableEffect(window) {
            var dragOffset: Point? = null
            val titleBarHeightPx: Int = with(density) {
                DesktopMusicDimens.TitleBarHeight.toPx().roundToInt()
            }
            val componentListener = object : ComponentAdapter() {
                override fun componentResized(event: ComponentEvent) {
                    window.applyRoundedShape()
                }
            }
            val eventListener: (AWTEvent) -> Unit = listener@{ event: AWTEvent ->
                val mouseEvent: MouseEvent = event as? MouseEvent ?: return@listener
                val sourceComponent: Component = mouseEvent.source as? Component ?: return@listener
                if (SwingUtilities.getWindowAncestor(sourceComponent) != window) {
                    return@listener
                }
                val windowPoint: Point = SwingUtilities.convertPoint(
                    sourceComponent,
                    mouseEvent.point,
                    window,
                )
                when (mouseEvent.id) {
                    MouseEvent.MOUSE_PRESSED -> {
                        dragOffset = if (windowPoint.y <= titleBarHeightPx) {
                            windowPoint
                        } else {
                            null
                        }
                    }
                    MouseEvent.MOUSE_DRAGGED -> {
                        val offset: Point = dragOffset ?: return@listener
                        if (windowState.placement == WindowPlacement.Fullscreen) {
                            return@listener
                        }
                        window.setLocation(
                            mouseEvent.xOnScreen - offset.x,
                            mouseEvent.yOnScreen - offset.y,
                        )
                    }
                    MouseEvent.MOUSE_RELEASED -> {
                        dragOffset = null
                    }
                }
            }
            val eventMask: Long = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
            window.background = AwtColor(0, 0, 0, 0)
            window.applyRoundedShape()
            window.addComponentListener(componentListener)
            Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, eventMask)
            onDispose {
                window.removeComponentListener(componentListener)
                Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener)
            }
        }
        DesktopMusicApp(
            controller = DesktopPlaybackSession.controller,
            onCloseWindow = closeWindow,
            onMinimizeWindow = {
                windowState.isMinimized = true
            },
            onToggleFullscreen = {
                windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) {
                    WindowPlacement.Floating
                } else {
                    WindowPlacement.Fullscreen
                }
            },
        )
    }
}

/**
 * 无装饰桌面窗口需要 native shape 裁切，单靠 Compose 背景圆角无法裁掉真实窗口四角。
 */
private fun AwtWindow.applyRoundedShape() {
    shape = RoundRectangle2D.Double(
        0.0,
        0.0,
        width.toDouble(),
        height.toDouble(),
        MACOS_WINDOW_CORNER_RADIUS_PX * 2,
        MACOS_WINDOW_CORNER_RADIUS_PX * 2,
    )
}
