package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Apple 播放迁移交接门禁，确保最终 issue 保留可复查证据。
 */
class ApplePlaybackGateEvidenceHandoffTest {
    /** 验证 17 号交接 issue 已进入人工复核状态且验收项全部勾选。 */
    @Test
    fun marksGateHandoffIssueReadyForHuman() {
        val text: String = readIssueText()
        assertContainsAll(
            text = text,
            requiredSnippets =
                listOf(
                    "Status: ready-for-human",
                    "Labels: ready-for-human",
                ),
        )
        assertTrue(
            actual = !text.contains(other = "- [ ]"),
            message = "$GATE_HANDOFF_ISSUE_PATH 仍有未勾选验收项",
        )
    }

    /** 验证交接记录包含自动化、真实播放、打包、格式和文档门禁证据。 */
    @Test
    fun recordsHardGateEvidenceWithResults() {
        val text: String = readIssueText()
        assertContainsAll(
            text = text,
            requiredSnippets =
                listOf(
                    "./gradlew :composeApp:tasks --all",
                    "linkDebugFrameworkIosSimulatorArm64",
                    "./gradlew :composeApp:desktopTest :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:macosAvFoundationBridgeSmoke :composeApp:macosAvFoundationDefaultRuntimeSmoke :composeApp:compileDebugKotlinAndroid",
                    "macOS 本机真实播放 smoke",
                    "prepared",
                    "playing",
                    "progress",
                    "ended",
                    "failed(type=MissingFile)",
                    "打包产物 bridge 加载检查",
                    "打包产物 bridge 加载检查未通过",
                    "不代表整批可进入人工验收",
                    "无 vlcj / LibVLC 生产引用证明",
                    "Apple 格式支持矩阵",
                    "错误文案测试",
                    "文档门禁",
                ),
        )
    }

    /** 验证交接没有把人工验收和剩余风险伪装成自动化通过。 */
    @Test
    fun separatesManualAcceptanceAndRemainingRisks() {
        val text: String = readIssueText()
        assertContainsAll(
            text = text,
            requiredSnippets =
                listOf(
                    "人工验收待办",
                    "iOS 真机播放",
                    "后台继续播放",
                    "锁屏后音频继续",
                    "回前台状态同步",
                    "必要听感检查",
                    "剩余风险或未完成项",
                    "签名",
                    "公证",
                    "干净机安装",
                    "Mac App Store sandbox",
                    "Now Playing",
                    "远程命令",
                    "媒体键",
                    "非 macOS Desktop 真实播放",
                ),
        )
    }

    /** 验证交接 issue 包含实现、验证、审查和风险的完整 Comments。 */
    @Test
    fun recordsReviewAndHandoffSummary() {
        val text: String = readIssueText()
        assertContainsAll(
            text = text,
            requiredSnippets =
                listOf(
                    "实现摘要",
                    "验证命令与结果",
                    "对抗式审查结论",
                    "code review 结论",
                    "剩余风险或未完成项",
                ),
        )
    }

    /** 读取 17 号本地 issue，避免把交接门禁藏在源码实现里。 */
    private fun readIssueText(): String {
        val projectRoot: Path = resolveProjectRoot()
        return Files.readString(projectRoot.resolve(GATE_HANDOFF_ISSUE_PATH))
    }

    /** 兼容 Gradle 和 IDE 工作目录，确保门禁可稳定定位仓库根。 */
    private fun resolveProjectRoot(): Path {
        val workingDirectory: Path = Path.of(System.getProperty("user.dir"))
        if (Files.exists(workingDirectory.resolve("gradle/libs.versions.toml"))) {
            return workingDirectory
        }
        return workingDirectory.parent
    }

    /** 验证交接文本包含所有关键证据片段。 */
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
            message = "$GATE_HANDOFF_ISSUE_PATH 缺少：${missingSnippets.joinToString(separator = "；")}",
        )
    }

    private companion object {
        // 17 号交接 issue 是本票唯一需要固化的审计产物。
        private const val GATE_HANDOFF_ISSUE_PATH =
            ".scratch/apple-platform-playback-wayfinder/issues/17-apple-playback-gate-evidence-handoff.md"
    }
}
