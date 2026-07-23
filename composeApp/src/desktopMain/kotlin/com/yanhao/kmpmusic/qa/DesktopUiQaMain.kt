package com.yanhao.kmpmusic.qa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yanhao.kmpmusic.applyMacosNativeTitleBar
import com.yanhao.kmpmusic.clearMacosNativeTitleBar
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicApp
import com.yanhao.kmpmusic.isMacosHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.awt.AWTException
import java.io.IOException
import kotlin.system.exitProcess

/**
 * Desktop UI QA 独立入口，使用真实 Desktop 壳、内存 fake 数据和自动取证关闭完整反馈环。
 */
object DesktopUiQaMain {
    /** 解析场景后启动固定尺寸窗口，运行完成后由取证流程自动退出。 */
    @JvmStatic
    fun main(args: Array<String>) {
        val config: DesktopUiQaConfig = DesktopUiQaConfig.parse(args = args)
        application {
            val windowState: WindowState =
                rememberWindowState(
                    width = config.scenario.windowWidth.dp,
                    height = config.scenario.windowHeight.dp,
                )
            Window(
                onCloseRequest = ::exitApplication,
                title = "KMP Music UI QA: ${config.scenario.argument}",
                state = windowState,
                resizable = false,
            ) {
                DesktopUiQaContent(
                    config = config,
                    window = window,
                    onComplete = ::exitApplication,
                    titleBarDragArea = {
                        if (isMacosHost()) {
                            WindowDraggableArea(modifier = Modifier.fillMaxSize())
                        }
                    },
                )
            }
        }
    }
}

// QA 内容只负责组装内存控制器、真实 App 壳和一次性取证副作用。
@Composable
private fun DesktopUiQaContent(
    config: DesktopUiQaConfig,
    window: ComposeWindow,
    onComplete: () -> Unit,
    titleBarDragArea: @Composable () -> Unit,
) {
    if (isMacosHost()) {
        DisposableEffect(window) {
            window.rootPane.applyMacosNativeTitleBar()
            onDispose {
                window.rootPane.clearMacosNativeTitleBar()
            }
        }
    }
    val controllerScope: CoroutineScope = rememberCoroutineScope()
    val localMusicScanner: LocalMusicScanner =
        remember(config.scenario) {
            createDesktopUiQaScanner(scenario = config.scenario)
        }
    val controller: MusicAppController =
        remember(controllerScope, localMusicScanner) {
            MusicAppController(
                localMusicScanner = localMusicScanner,
                controllerScope = controllerScope,
            )
        }
    DesktopMusicApp(
        controller = controller,
        showTitleBarBrand = isMacosHost(),
        titleBarDragArea = titleBarDragArea,
    )
    LaunchedEffect(config) {
        runDesktopUiQaWorkflow(
            config = config,
            controller = controller,
            window = window,
            onComplete = onComplete,
        )
    }
}

// 运行失败必须退出非零，避免脚本把缺图或相同帧误报为成功。
private suspend fun runDesktopUiQaWorkflow(
    config: DesktopUiQaConfig,
    controller: MusicAppController,
    window: ComposeWindow,
    onComplete: () -> Unit,
) {
    try {
        println("[desktop-ui-qa] preparing=${config.scenario.argument}")
        prepareDesktopUiQaScenario(
            controller = controller,
            scenario = config.scenario,
        )
        window.isAlwaysOnTop = true
        window.toFront()
        window.requestFocus()
        delay(timeMillis = INITIAL_RENDER_DELAY_MILLIS)
        DesktopUiQaCaptureRunner(config = config).capture(window = window)
        println("[desktop-ui-qa] completed=${config.scenario.argument} output=${config.outputDirectory}")
        onComplete()
    } catch (error: AWTException) {
        failDesktopUiQa(error = error)
    } catch (error: IOException) {
        failDesktopUiQa(error = error)
    } catch (error: IllegalStateException) {
        failDesktopUiQa(error = error)
    } catch (error: RuntimeException) {
        failDesktopUiQa(error = error)
    }
}

// 错误输出包含完整上下文并终止进程，Gradle 和调用脚本都能得到失败状态。
private fun failDesktopUiQa(error: Throwable): Nothing {
    System.err.println("[desktop-ui-qa] failed=${error.message}")
    error.printStackTrace(System.err)
    exitProcess(status = 1)
}

/** 数据和路由完成后的首帧渲染等待时间（1000ms）。 */
private const val INITIAL_RENDER_DELAY_MILLIS: Long = 1_000L
