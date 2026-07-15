package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Apple 平台播放路线文档门禁，防止旧 vlcj 路线重新变成当前依据。
 */
class ApplePlaybackDocumentationGateTest {
    /** 验证 ADR 固化 Apple 原生播放路线和交付边界。 */
    @Test
    fun documentsAppleNativePlaybackDecisionInAdr(): Unit {
        val text: String = readProjectFile(relativePath = APPLE_PLAYBACK_ADR_PATH)
        assertContainsAll(
            relativePath = APPLE_PLAYBACK_ADR_PATH,
            text = text,
            requiredSnippets = listOf(
                "macOS 从 `vlcj / LibVLC` 改为 Apple `AVFoundation`",
                "iOS 与 macOS 统一到 Apple 原生播放方案",
                "非 macOS Desktop 不承诺真实播放",
                "验证方式",
                "格式矩阵",
                "docs/APPLE_PLATFORM_FORMAT_SUPPORT_MATRIX.md",
            ),
        )
    }

    /** 验证旧设计文件顶部明确声明不能继续作为当前实现依据。 */
    @Test
    fun marksOldVlcjDesignAsSuperseded(): Unit {
        val text: String = readProjectFile(relativePath = OLD_VLCJ_DESIGN_PATH)
        assertContainsAll(
            relativePath = OLD_VLCJ_DESIGN_PATH,
            text = text.take(n = 500),
            requiredSnippets = listOf(
                "`Superseded`",
                "不能作为当前实现依据",
                "Apple AVFoundation",
                "docs/adr/0005-apple-platform-avfoundation-playback.md",
            ),
        )
    }

    /** 验证旧播放抽象审计不再把未来 Desktop 路线绑定到 vlcj。 */
    @Test
    fun rewritesPlaybackAuditDesktopVlcjAssumptionsAsHistorical(): Unit {
        val text: String = readProjectFile(relativePath = PLAYBACK_ABSTRACTION_AUDIT_PATH)
        assertContainsAll(
            relativePath = PLAYBACK_ABSTRACTION_AUDIT_PATH,
            text = text,
            requiredSnippets = listOf(
                "历史状态",
                "Desktop 不能继续等同于 vlcj",
                "Windows / Linux Desktop 真实播放需要重新设计",
            ),
        )
        val forbiddenSnippets: List<String> = listOf(
            "未来 Windows 优先复用 Desktop vlcj engine",
            "`DesktopVlcjAudioPlayerEngine` 继续复用",
            "不重写 macOS vlcj 主链路",
        )
        assertContainsNone(
            relativePath = PLAYBACK_ABSTRACTION_AUDIT_PATH,
            text = text,
            forbiddenSnippets = forbiddenSnippets,
        )
    }

    /** 验证 README 不再把已有平台播放能力笼统写成全都未完成。 */
    @Test
    fun avoidsReadmeRealPlaybackFutureOnlyStatement(): Unit {
        val text: String = readProjectFile(relativePath = "README.md")
        assertContainsNone(
            relativePath = "README.md",
            text = text,
            forbiddenSnippets = listOf(
                "真实音频播放、持久化和云同步仍是后续阶段的能力",
                "iOS/Android/Desktop 各平台的完整播放适配",
            ),
        )
    }

    /** 兼容 Gradle 和 IDE 工作目录，确保门禁能从仓库根读取文档。 */
    private fun resolveProjectRoot(): Path {
        val workingDirectory: Path = Path.of(System.getProperty("user.dir"))
        if (Files.exists(workingDirectory.resolve("gradle/libs.versions.toml"))) {
            return workingDirectory
        }
        return workingDirectory.parent
    }

    /** 读取仓库内文档，缺失时让测试直接暴露文件路径。 */
    private fun readProjectFile(relativePath: String): String {
        val projectRoot: Path = resolveProjectRoot()
        return Files.readString(projectRoot.resolve(relativePath))
    }

    /** 验证文档包含全部关键事实，而不是只创建空文件。 */
    private fun assertContainsAll(
        relativePath: String,
        text: String,
        requiredSnippets: List<String>,
    ): Unit {
        val missingSnippets: List<String> = requiredSnippets.filterNot { snippet: String ->
            text.contains(other = snippet)
        }
        assertTrue(
            actual = missingSnippets.isEmpty(),
            message = "$relativePath 缺少：${missingSnippets.joinToString(separator = "；")}",
        )
    }

    /** 验证旧路线关键句已经被移除，避免后续继续按旧方案扩展。 */
    private fun assertContainsNone(
        relativePath: String,
        text: String,
        forbiddenSnippets: List<String>,
    ): Unit {
        val violations: List<String> = forbiddenSnippets.filter { snippet: String ->
            text.contains(other = snippet)
        }
        assertTrue(
            actual = violations.isEmpty(),
            message = "$relativePath 仍包含：${violations.joinToString(separator = "；")}",
        )
    }

    private companion object {
        // 当前 Apple 播放路线 ADR 的固定路径。
        private const val APPLE_PLAYBACK_ADR_PATH = "docs/adr/0005-apple-platform-avfoundation-playback.md"

        // 旧 macOS vlcj 设计文档路径。
        private const val OLD_VLCJ_DESIGN_PATH = "docs/superpowers/specs/2026-06-24-macos-vlcj-playback-design.md"

        // 旧播放抽象审计文档路径。
        private const val PLAYBACK_ABSTRACTION_AUDIT_PATH =
            "docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md"
    }
}
