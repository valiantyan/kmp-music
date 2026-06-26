package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.pathString

/**
 * Gradle 开发运行注入的项目内 LibVLC 根目录属性名。
 */
private const val DEVELOPMENT_RUNTIME_SYSTEM_PROPERTY = "kmp.music.libvlc.runtime.dir"

/**
 * 解析 macOS Desktop 所需的 LibVLC 运行时路径。
 *
 * 发布包必须优先使用 App Bundle 内的运行时；开发运行优先使用项目内自动解压的运行时。
 */
object MacosLibVlcRuntime {
    /**
     * 按 Bundle 优先规则解析 LibVLC 和 plugins 目录。
     *
     * @param appDirectory 当前应用的 `.app` 目录。
     * @param developmentRuntimeDirectory 项目内开发运行时根目录。
     * @param developmentVlcApp 本地开发机上的 VLC.app 目录。
     * @param allowDevelopmentFallback 是否允许使用开发机 VLC.app 回退。
     * @return 可用运行时路径；发布包缺少 Bundle 运行时时返回 `null`。
     */
    fun resolve(
        appDirectory: Path = defaultAppDirectory(),
        developmentRuntimeDirectory: Path = defaultDevelopmentRuntimeDirectory(),
        developmentVlcApp: Path = Path.of("/Applications/VLC.app"),
        allowDevelopmentFallback: Boolean = isDevelopmentRun(),
    ): MacosLibVlcRuntimePath? {
        val bundledDirectory: Path = appDirectory.resolve("Contents/Frameworks/LibVLC")
        val bundledLibrary: Path = bundledDirectory.resolve("lib")
        val bundledPlugins: Path = bundledDirectory.resolve("plugins")
        if (hasRuntimeDirectories(libraryDirectory = bundledLibrary, pluginDirectory = bundledPlugins)) {
            return MacosLibVlcRuntimePath(
                libraryDirectory = bundledLibrary.pathString,
                pluginDirectory = bundledPlugins.pathString,
                isBundled = true,
            )
        }
        if (!allowDevelopmentFallback) {
            return null
        }
        resolveProjectDevelopmentRuntime(developmentRuntimeDirectory = developmentRuntimeDirectory)?.let { runtime ->
            return runtime
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

    // 开发运行没有 `.app`，因此从 Gradle 注入路径或项目目录中寻找自动解压的 LibVLC。
    private fun defaultDevelopmentRuntimeDirectory(): Path {
        System.getProperty(DEVELOPMENT_RUNTIME_SYSTEM_PROPERTY)
            ?.takeIf { property: String -> property.isNotBlank() }
            ?.let { property: String -> return Path.of(property).toAbsolutePath() }
        return defaultProjectDirectory().resolve("composeApp/build/macos-libvlc/runtime/LibVLC")
    }

    // IDE 直接运行和 Gradle 运行的工作目录不同，向上寻找项目根目录能兼容两者。
    private fun defaultProjectDirectory(): Path {
        val userDirectory: Path = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        val codeSourceDirectory: Path = MacosLibVlcRuntime::class.java.protectionDomain
            .codeSource
            ?.location
            ?.toURI()
            ?.let { uri -> Path.of(uri).toAbsolutePath() }
            ?: userDirectory
        return sequenceOf(userDirectory, codeSourceDirectory)
            .mapNotNull(::findProjectDirectory)
            .firstOrNull()
            ?: userDirectory
    }

    // 同时识别仓库根目录和 `composeApp` 模块目录，避免 IDE 运行配置差异影响开发播放。
    private fun findProjectDirectory(startPath: Path): Path? {
        return generateSequence(startPath) { path: Path -> path.parent }
            .firstOrNull { path: Path -> path.resolve("composeApp/build.gradle.kts").let(Files::isRegularFile) }
            ?: generateSequence(startPath) { path: Path -> path.parent }
                .firstOrNull { path: Path -> path.resolve("build.gradle.kts").let(Files::isRegularFile) }
                ?.parent
    }

    // Compose 打包运行时会设置资源目录；本地 IDE/Gradle 运行缺少该属性。
    private fun isDevelopmentRun(): Boolean {
        return System.getProperty("compose.application.resources.dir").isNullOrBlank()
    }

    // 项目内运行时与发布包同构：动态库在 `lib` 子目录，插件在 `plugins` 子目录。
    private fun resolveProjectDevelopmentRuntime(developmentRuntimeDirectory: Path): MacosLibVlcRuntimePath? {
        val developmentLibrary: Path = developmentRuntimeDirectory.resolve("lib")
        val developmentPlugins: Path = developmentRuntimeDirectory.resolve("plugins")
        if (!hasRuntimeDirectories(libraryDirectory = developmentLibrary, pluginDirectory = developmentPlugins)) {
            return null
        }
        return MacosLibVlcRuntimePath(
            libraryDirectory = developmentLibrary.pathString,
            pluginDirectory = developmentPlugins.pathString,
            isBundled = false,
        )
    }

    // LibVLC 与插件目录必须同时存在，否则运行时视为不可用。
    private fun hasRuntimeDirectories(
        libraryDirectory: Path,
        pluginDirectory: Path,
    ): Boolean {
        return Files.isDirectory(libraryDirectory) &&
            Files.isDirectory(pluginDirectory) &&
            hasLibVlcLibrary(libraryDirectory = libraryDirectory)
    }

    // 仅有目录不够，必须能找到 LibVLC 主库，避免空目录误判为可播放。
    private fun hasLibVlcLibrary(libraryDirectory: Path): Boolean {
        return Files.list(libraryDirectory).use { paths ->
            paths.anyMatch { path: Path -> path.name.startsWith(prefix = "libvlc") }
        }
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
