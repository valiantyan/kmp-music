package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * macOS DMG 打包门禁，防止 `.app` 漏带 AVFoundation bridge。
 */
class MacosAvFoundationPackageDmgGateTest {
    /** 验证 [packageDmg] 会先把 native bridge 放入 app resources。 */
    @Test
    fun packageDmgStagesMacosAvFoundationBridgeIntoAppResources() {
        val text: String = readProjectFile(relativePath = BUILD_FILE_PATH)
        assertContainsAll(
            text = text,
            requiredSnippets =
                listOf(
                    "stageMacosAvFoundationBridgeIntoPackageApp",
                    "dependsOn(\"compileMacosAvFoundationBridge\", \"createDistributable\")",
                    "Contents/app/resources/\$macosAvFoundationBridgeBundleDirectory",
                    "task.name == \"packageDmg\"",
                    "dependsOn(stageMacosAvFoundationBridgeIntoPackageApp)",
                    "inputs.file(macosAvFoundationBundledBridgeLibrary)",
                    "stageMacosAvFoundationBridgeIntoReleasePackageApp",
                    "dependsOn(\"compileMacosAvFoundationBridge\", \"createReleaseDistributable\")",
                    "task.name == \"packageReleaseDmg\"",
                    "dependsOn(stageMacosAvFoundationBridgeIntoReleasePackageApp)",
                    "inputs.file(macosAvFoundationReleaseBundledBridgeLibrary)",
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

    /** 验证构建脚本包含 DMG bridge 打包所需的所有关键配置。 */
    private fun assertContainsAll(
        text: String,
        requiredSnippets: List<String>,
    ) {
        val missingSnippets: List<String> =
            requiredSnippets.filterNot { snippet: String ->
                text.contains(other = snippet)
            }
        assertTrue(
            actual = missingSnippets.isEmpty(),
            message = "$BUILD_FILE_PATH 缺少：${missingSnippets.joinToString(separator = "；")}",
        )
    }

    private companion object {
        // 桌面打包配置位于 composeApp 构建脚本中。
        private const val BUILD_FILE_PATH = "composeApp/build.gradle.kts"
    }
}
