package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.pathString

/**
 * 解析 macOS Desktop 所需的 LibVLC 运行时路径。
 *
 * 发布包必须优先使用 App Bundle 内的运行时；开发回退只用于本地验证。
 */
object MacosLibVlcRuntime {
    /**
     * 按 Bundle 优先规则解析 LibVLC 和 plugins 目录。
     *
     * @param appDirectory 当前应用的 `.app` 目录。
     * @param developmentVlcApp 本地开发机上的 VLC.app 目录。
     * @param allowDevelopmentFallback 是否允许使用开发机 VLC.app 回退。
     * @return 可用运行时路径；发布包缺少 Bundle 运行时时返回 `null`。
     */
    fun resolve(
        appDirectory: Path = defaultAppDirectory(),
        developmentVlcApp: Path = Path.of("/Applications/VLC.app"),
        allowDevelopmentFallback: Boolean = isDevelopmentRun(),
    ): MacosLibVlcRuntimePath? {
        val bundledDirectory: Path = appDirectory.resolve("Contents/Frameworks/LibVLC")
        val bundledPlugins: Path = bundledDirectory.resolve("plugins")
        if (hasRuntimeDirectories(libraryDirectory = bundledDirectory, pluginDirectory = bundledPlugins)) {
            return MacosLibVlcRuntimePath(
                libraryDirectory = bundledDirectory.pathString,
                pluginDirectory = bundledPlugins.pathString,
                isBundled = true,
            )
        }
        if (!allowDevelopmentFallback) {
            return null
        }
        val developmentLibrary: Path = developmentVlcApp.resolve("Contents/MacOS/lib")
        val developmentPlugins: Path = developmentVlcApp.resolve("Contents/MacOS/plugins")
        if (!hasRuntimeDirectories(libraryDirectory = developmentLibrary, pluginDirectory = developmentPlugins)) {
            return null
        }
        return MacosLibVlcRuntimePath(
            libraryDirectory = developmentLibrary.pathString,
            pluginDirectory = developmentPlugins.pathString,
            isBundled = false,
        )
    }

    // 从当前代码位置向上寻找 `.app`，让打包后的应用无需硬编码绝对路径。
    private fun defaultAppDirectory(): Path {
        val codeSourcePath: Path = MacosLibVlcRuntime::class.java.protectionDomain
            .codeSource
            ?.location
            ?.toURI()
            ?.let { uri -> Path.of(uri) }
            ?: Path.of(".").toAbsolutePath()
        return generateSequence(codeSourcePath) { path: Path -> path.parent }
            .firstOrNull { path: Path -> path.fileName?.toString()?.endsWith(".app") == true }
            ?: codeSourcePath.toAbsolutePath()
    }

    // Compose 打包运行时会设置资源目录；本地 IDE/Gradle 运行缺少该属性。
    private fun isDevelopmentRun(): Boolean {
        return System.getProperty("compose.application.resources.dir").isNullOrBlank()
    }

    // LibVLC 与插件目录必须同时存在，否则运行时视为不可用。
    private fun hasRuntimeDirectories(
        libraryDirectory: Path,
        pluginDirectory: Path,
    ): Boolean {
        return Files.isDirectory(libraryDirectory) && Files.isDirectory(pluginDirectory)
    }
}

/**
 * 已解析的 LibVLC 运行时目录。
 *
 * @property libraryDirectory LibVLC 动态库所在目录。
 * @property pluginDirectory LibVLC 插件目录。
 * @property isBundled 是否来自 App Bundle 内置运行时。
 */
data class MacosLibVlcRuntimePath(
    val libraryDirectory: String,
    val pluginDirectory: String,
    val isBundled: Boolean,
)
