package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * macOS 桌面开发运行链路门禁，防止 [desktopRun] 绕过 AVFoundation bridge。
 */
class MacosAvFoundationDesktopRunGateTest {
    /** 验证 [desktopRun] 会准备并注入真实播放所需的 native bridge。 */
    @Test
    fun configuresDesktopRunWithMacosAvFoundationBridge(): Unit {
        val text: String = readProjectFile(relativePath = BUILD_FILE_PATH)
        assertContainsAll(
            text = text,
            requiredSnippets = listOf(
                "tasks.withType<JavaExec>().configureEach",
                "name == \"desktopRun\"",
                "dependsOn(\"compileMacosAvFoundationBridge\")",
                "systemProperty(",
                "\"kmp.music.macos.avfoundation.bridge.path\"",
                "macosAvFoundationBridgeLibrary.get().asFile.absolutePath",
            ),
        )
    }

    /** 兼容 Gradle 和 IDE 工作目录，确保门禁能从仓库根读取构建脚本。 */
    private fun resolveProjectRoot(): Path {
        val workingDirectory: Path = Path.of(System.getProperty("user.dir"))
        if (Files.exists(workingDirectory.resolve("gradle/libs.versions.toml"))) {
            return workingDirectory
        }
        return workingDirectory.parent
    }

    /** 读取仓库内文件，缺失时让测试直接报告固定路径。 */
    private fun readProjectFile(relativePath: String): String {
        val projectRoot: Path = resolveProjectRoot()
        return Files.readString(projectRoot.resolve(relativePath))
    }

    /** 验证构建脚本保留开发运行路径的关键 bridge 配置。 */
    private fun assertContainsAll(
        text: String,
        requiredSnippets: List<String>,
    ): Unit {
        val missingSnippets: List<String> = requiredSnippets.filterNot { snippet: String ->
            text.contains(other = snippet)
        }
        assertTrue(
            actual = missingSnippets.isEmpty(),
            message = "$BUILD_FILE_PATH 缺少：${missingSnippets.joinToString(separator = "；")}",
        )
    }

    private companion object {
        // 桌面运行配置位于 composeApp 构建脚本中。
        private const val BUILD_FILE_PATH = "composeApp/build.gradle.kts"
    }
}
