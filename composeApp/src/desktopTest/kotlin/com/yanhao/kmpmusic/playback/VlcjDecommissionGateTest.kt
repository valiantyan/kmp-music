package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 生产树下线旧桌面第三方播放链路的门禁测试。
 */
class VlcjDecommissionGateTest {
    /** 验证生产树已经下线旧桌面第三方播放路径，只保留 Apple 原生链路。 */
    @Test
    fun productionTreeHasNoVlcjRuntimeReferences(): Unit {
        val projectRoot: Path = resolveProjectRoot()
        val scannedFiles: List<Path> = productionFiles(projectRoot = projectRoot)
        val violations: List<String> = scannedFiles.flatMap { path: Path ->
            forbiddenTokens.mapNotNull { token: String ->
                val text: String = Files.readString(path)
                if (text.contains(other = token, ignoreCase = true)) {
                    "${projectRoot.relativize(path).pathString}: $token"
                } else {
                    null
                }
            }
        }
        assertTrue(
            actual = violations.isEmpty(),
            message = violations.joinToString(separator = "\n"),
        )
    }

    /** 兼容 Gradle 和 IDE 两种测试工作目录，避免门禁依赖启动位置。 */
    private fun resolveProjectRoot(): Path {
        val workingDirectory: Path = Path.of(System.getProperty("user.dir"))
        if (Files.exists(workingDirectory.resolve("gradle/libs.versions.toml"))) {
            return workingDirectory
        }
        return workingDirectory.parent
    }

    /** 收集会形成生产代码、依赖、打包或运行参数的文件。 */
    private fun productionFiles(projectRoot: Path): List<Path> {
        val roots: List<Path> = listOf(
            projectRoot.resolve("composeApp/build.gradle.kts"),
            projectRoot.resolve("gradle/libs.versions.toml"),
            projectRoot.resolve("composeApp/src/commonMain"),
            projectRoot.resolve("composeApp/src/desktopMain"),
        )
        return roots.flatMap { root: Path ->
            if (root.isRegularFile()) {
                listOf(root)
            } else {
                walkProductionRoot(root = root)
            }
        }
    }

    /** 只扫描文本型生产文件，避免把构建产物或平台二进制纳入误报。 */
    private fun walkProductionRoot(root: Path): List<Path> {
        return Files.walk(root).use { paths ->
            paths.filter { path: Path -> path.isRegularFile() }
                .filter { path: Path -> path.extension in scannedExtensions || path.name in scannedFileNames }
                .toList()
        }
    }

    private companion object {
        // 下线证明覆盖依赖、生产源码、运行参数、打包脚本和用户文案可能出现的旧关键词。
        private val forbiddenTokens: List<String> = listOf(
            "vlcj",
            "LibVLC",
            "VLC_PLUGIN_PATH",
            "kmp.music.libvlc",
            "macos-libvlc",
            "TargetFormat.Msi",
            "TargetFormat.Deb",
            "downloadMacosArm64LibVlc",
            "extractMacosArm64LibVlc",
            "prepareMacosArm64LibVlc",
            "stageMacosArm64LibVlc",
        )

        // 生产树内需要参与引用证明的源码、脚本和文档格式。
        private val scannedExtensions: Set<String> = setOf(
            "c",
            "cc",
            "cpp",
            "h",
            "hpp",
            "kt",
            "kts",
            "m",
            "mm",
            "toml",
            "md",
            "sh",
        )

        // 版本目录或配置目录里的无扩展文本文件也要纳入门禁。
        private val scannedFileNames: Set<String> = setOf(
            "SOURCE_RECORD",
        )
    }
}
