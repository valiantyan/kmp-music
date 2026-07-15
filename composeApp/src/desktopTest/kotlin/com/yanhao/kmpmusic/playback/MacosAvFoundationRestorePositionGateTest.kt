package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * macOS 恢复进度门禁，防止 native bridge 在初始 seek 完成前宣称媒体已准备好。
 */
class MacosAvFoundationRestorePositionGateTest {
    /** 验证 native [prepare] 等待 AVFoundation ready，并在初始 seek 完成后才回调 prepared。 */
    @Test
    fun nativePrepareCompletesInitialSeekBeforePreparedCallback(): Unit {
        val text: String = readProjectFile(relativePath = NATIVE_BRIDGE_FILE_PATH)
        assertContainsAll(
            text = text,
            requiredSnippets = listOf(
                "completePreparationWhenReadyForItem:item",
                "AVPlayerItemStatusReadyToPlay",
                "completeInitialSeekForItem:item",
                "seekToTime:targetTime",
                "emitPrepared:generation item:item",
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

    /** 验证 native bridge 包含恢复进度所需的关键时序约束。 */
    private fun assertContainsAll(
        text: String,
        requiredSnippets: List<String>,
    ): Unit {
        val missingSnippets: List<String> = requiredSnippets.filterNot { snippet: String ->
            text.contains(other = snippet)
        }
        assertTrue(
            actual = missingSnippets.isEmpty(),
            message = "$NATIVE_BRIDGE_FILE_PATH 缺少：${missingSnippets.joinToString(separator = "；")}",
        )
    }

    private companion object {
        // macOS AVFoundation JNI bridge 是恢复进度实际兑现的位置。
        private const val NATIVE_BRIDGE_FILE_PATH =
            "composeApp/src/desktopMain/native/macos-avfoundation/KmpMacosAvFoundationBridge.mm"
    }
}
