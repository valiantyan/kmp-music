package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * macOS 进度越界门禁，防止 AVFoundation 估算时长偏短时继续写入不可恢复进度。
 */
class MacosAvFoundationProgressOverrunGateTest {
    /** 验证 native progress 超过 duration 后会结束当前 generation，而不是继续推进进度。 */
    @Test
    fun nativeProgressBeyondDurationFinishesCurrentGeneration() {
        val text: String = readProjectFile(relativePath = NATIVE_BRIDGE_FILE_PATH)
        assertContainsAll(
            text = text,
            requiredSnippets =
                listOf(
                    "KMP_END_POSITION_TOLERANCE_MS",
                    "KmpShouldFinishAtDuration",
                    "KmpClampPositionToDuration",
                    "self.lastPositionMs = 0",
                    "positionMs >= durationMs + KMP_END_POSITION_TOLERANCE_MS",
                    "finishGenerationAsEnded:generation",
                    "removeTimeObserver",
                ),
        )
    }

    /** 兼容 Gradle 和 IDE 工作目录，确保门禁能从仓库根读取 native 源码。 */
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

    /** 验证 native bridge 包含结束越界播放所需的关键约束。 */
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
            message = "$NATIVE_BRIDGE_FILE_PATH 缺少：${missingSnippets.joinToString(separator = "；")}",
        )
    }

    private companion object {
        // macOS AVFoundation JNI bridge 是越界 progress 的事实源。
        private const val NATIVE_BRIDGE_FILE_PATH =
            "composeApp/src/desktopMain/native/macos-avfoundation/KmpMacosAvFoundationBridge.mm"
    }
}
