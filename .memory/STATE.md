# 项目记忆状态

## 当前目标

苹果平台统一播放迁移实现批次推进到 Ticket 16：Apple 播放 ADR 和旧路线文档更新已完成实现、验证、票据更新和交付前审查。

## 当前进度

- 09 Apple 播放契约和 fake bridge 行为防线已完成，状态为 `ready-for-human`。
- 10 iOS 沙盒导入来源闭环已完成，状态为 `ready-for-human`。
- 11 iOS AVFoundation 播放会话 P0 已完成，状态为 `ready-for-human`。
- 12 macOS AVFoundation bridge 最小真实播放已完成，状态为 `ready-for-human`。
- 13 桌面默认 AVFoundation engine 已完成，状态为 `ready-for-human`。
- 14 Apple 格式矩阵和播放错误自救文案已完成，状态为 `ready-for-human`。
- 15 下线 vlcj / LibVLC 生产链路已完成，状态为 `ready-for-human`。
- 16 更新 ADR 和旧播放路线文档已完成，状态为 `ready-for-human`。
- 11 新增 iOS 进程级 `IosPlaybackSession` / `IosPlaybackSessionRuntime`，由会话持有 `MusicAppController`、`IosAvFoundationAudioPlayerEngine` 和 `IosAvAudioSessionController`，`IosEntry` 只复用会话并请求一次播放快照恢复。
- 11 新增 iOS AVFoundation 播放链路：`IosPlaybackBridge`、`IosAvFoundationPlaybackBridge`、`IosAvFoundationAudioPlayerEngine`、`IosAudioSessionController` 和 `IosPlaybackHostConfiguration`。
- 11 真实 bridge 使用单个 `AVPlayer`，不引入 `AVQueuePlayer`；队列、播放模式、自然结束推进和失败策略仍由 common `PlaybackCoordinator` 拥有。
- 11 播放前配置并激活 `AVAudioSessionCategoryPlayback`；宿主 Info.plist 需要 `UIBackgroundModes = audio`，当前仓库没有真实 iOS 宿主工程，真机后台播放仍是人工验收项。
- 12 新增 macOS 进程内 JNI / Objective-C++ AVFoundation bridge，JVM 侧通过 `MacosAvFoundationPlaybackBridge` 接入 09 号 `ApplePlaybackBridge` 契约。
- 12 新增 Gradle `compileMacosAvFoundationBridge` 编译 dylib，`desktopTest` 显式加载该 dylib；新增 `macosAvFoundationBridgeSmoke` 生成本机 M4A/AAC 样本并验证真实 AVFoundation 事件回流。
- 13 已将 `DesktopAudioRuntimeFactory` 默认装配切到 `DesktopAppleAudioPlayerEngine + MacosAvFoundationPlaybackBridge`，bridge 不可用时映射为统一 `EngineUnavailable` 事件，不回退 vlcj。
- 13 新增 `macosAvFoundationDefaultRuntimeSmoke`，通过默认桌面运行时、`MusicAppController` 和真实 AVFoundation bridge 验证 current media、playing、progress、seek、pause/resume、next、previous 和 stop。
- 14 新增 `AppleAudioFormatSupportMatrix` 和 `docs/APPLE_PLATFORM_FORMAT_SUPPORT_MATRIX.md`。矩阵覆盖 MP3、M4A/AAC、WAV、FLAC、AIFF/ALAC、OGG/OPUS 和 AMR；OGG/OPUS/AMR 为待验证，不进入桌面或 iOS 扫描入口。
- 14 扩展 `macosAvFoundationBridgeSmoke`，真实 M4A/AAC 播放 smoke 继续验证 prepared、playing、progress、ended 和 MissingFile；格式矩阵样本用 AVFoundation 可播放性检查输出 MP3、M4A/AAC、WAV、FLAC、AIFF/ALAC 支持证据。
- 14 更新共享错误文案，覆盖缺文件、权限拒绝、不支持格式或受保护资源、Apple 播放组件不可用和未知错误；全部错误类型的用户文案都不出现旧运行时提示。
- 14 iOS 真实格式样本播放未形成可靠自动化证据；矩阵和文档明确 iOS 导入扫描按 Apple P0 allowlist 放行，但真机真实样本播放仍需 17 号 gate 或人工验收记录。
- 15 已删除 `libs.vlcj` 依赖、旧 macOS LibVLC 下载/提取/准备/打包/验收任务、开发运行时参数注入和 `composeApp/src/desktopMain/packaging/macos-libvlc/` 生产脚本资源。
- 15 已删除旧 `DesktopVlcjAudioPlayerEngine`、`MacosLibVlcRuntime`、`VlcjMediaPlayerAdapter`、`DesktopMediaPlayerAdapter` 旧适配边界及其 LibVLC / vlcj 细节测试。
- 15 桌面 native distribution 已收窄为 macOS `Dmg`，不再生成 Windows / Linux 桌面真实播放分发暗示。
- 15 新增 `VlcjDecommissionGateTest`，扫描生产代码、Gradle 依赖、打包脚本、运行参数、Markdown / shell / Kotlin / native 文本文件，证明生产树不再出现旧播放路径关键词。
- 16 新增 `docs/adr/0005-apple-platform-avfoundation-playback.md`，固化 macOS 从 `vlcj / LibVLC` 改为 Apple `AVFoundation`，iOS 与 macOS 统一到 Apple 原生播放方案。
- 16 旧 `docs/superpowers/specs/2026-06-24-macos-vlcj-playback-design.md` 顶部已标记 `Superseded`（已被取代），并说明不能作为当前实现依据。
- 16 已修正 `docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md` 中 Desktop 等同 vlcj、未来复用 Desktop vlcj engine、iOS 仍只是未来 adapter 等过时表述。
- 16 README 只修正当前播放能力概述：Android 与 macOS 已有真实播放链路，iOS 是 App 内 AVFoundation 播放会话基础适配；iOS 真机后台 / 锁屏、Now Playing、远程命令、发布级验收和 Windows / Linux Desktop 真实播放仍在后续阶段。
- 16 新增 `ApplePlaybackDocumentationGateTest`，防止 ADR 缺失、旧 vlcj 文档未标过时、播放抽象审计继续绑定 vlcj、README 再次笼统宣称真实播放全未完成。

## 下一步

- 当前可继续推进 `17-apple-playback-gate-evidence-handoff.md`；17 依赖 11/14/15/16，前置已满足。
- 若继续 iOS 方向，真机人工验收需由宿主工程确认 `UIBackgroundModes = audio`、后台继续播放、锁屏后继续播放、回前台状态同步，以及 MP3、M4A/AAC、WAV、FLAC、AIFF/ALAC 样本真实播放。

## 阻塞

- `.codex/hooks/memory_hook.py` 当前不存在，无法运行 memory doctor。

## 验证状态

- 11 已运行 `./gradlew :composeApp:tasks --all`，结果通过，并确认 iOS framework 编译任务名为 `linkDebugFrameworkIosSimulatorArm64`。
- 11 已运行 `./gradlew :composeApp:iosSimulatorArm64Test :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid`，结果通过。
- 11 已运行 `git diff --check`，结果通过。
- 11 TDD 红灯证据：新增 iOS 播放会话和 AVFoundation engine fake 测试后，首次 `./gradlew :composeApp:iosSimulatorArm64Test` 因缺少 `IosPlaybackBridge`、`IosAvFoundationAudioPlayerEngine`、`IosPlaybackSessionRuntime` 等新类型编译失败；实现后转绿。
- 11 交付前已执行 Standards + Spec 两维 code review；阻塞问题已修复，剩余风险记录在 Ticket Comments。
- 12 已运行 `./gradlew :composeApp:desktopTest :composeApp:macosAvFoundationBridgeSmoke :composeApp:compileDebugKotlinAndroid`，结果通过；smoke 输出包含 `prepared`、`playing`、多次 `progress`、`ended` 和 `failed(type=MissingFile)`。
- 12 已运行 `git diff --check`，结果通过。
- 12 TDD 红灯证据：新增 macOS bridge JVM seam 测试后，首次 `./gradlew :composeApp:desktopTest` 因缺少 `MacosAvFoundationPlaybackBridge`、native loader、session factory、callback 和状态常量等类型编译失败；实现后转绿。
- 12 交付前已执行 Standards + Spec 两维 code review；Standards 发现的 main queue 调度和 enum ordinal 错误码问题已修复，Spec 发现的 Ticket 证据缺失已修复。01-08 决策票为本轮开始前已存在的未跟踪批次输入，本票未修改且不纳入 12 的提交。
- 13 已运行 `./gradlew :composeApp:desktopTest :composeApp:macosAvFoundationBridgeSmoke :composeApp:macosAvFoundationDefaultRuntimeSmoke :composeApp:compileDebugKotlinAndroid`，结果通过；bridge smoke 输出包含 `prepared`、`playing`、多次 `progress`、`ended` 和 `failed(type=MissingFile)`；默认 runtime smoke 输出包含 `current-media`、`playing`、`progress`、`seek`、`paused`、`resume`、`next`、`previous` 和 `stop`。
- 13 已运行 `git diff --check`，结果通过。
- 13 TDD 红灯证据：新增 `DesktopAudioRuntimeFactoryTest` 后，首次 `./gradlew :composeApp:desktopTest` 因 `DesktopAudioRuntimeFactory.create` 缺少 `bridgeFactory` 和 `dispatcher` 参数编译失败；切换默认装配后转绿。
- 13 交付前已执行 Standards + Spec 两维 code review；显式类型、测试职责过宽和默认真实 smoke 证据不足问题已修复。Spec 审查中关于 vlcj / LibVLC 残留的意见属于 15 号下线票范围，本票未删除这些待删除对象。
- 14 已运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.LocalAudioFileRulesTest --tests com.yanhao.kmpmusic.feature.app.PlaybackErrorMessageTest`，结果通过。
- 14 已运行 `./gradlew :composeApp:iosSimulatorArm64Test --tests com.yanhao.kmpmusic.data.IosFolderMusicScannerTest`，结果通过。
- 14 已运行 `./gradlew :composeApp:macosAvFoundationBridgeSmoke`，结果通过；输出包含 M4A/AAC 真实播放事件和格式矩阵：MP3、M4A/AAC、WAV、FLAC、AIFF/ALAC 支持，OGG/OPUS、AMR 待验证。
- 14 已运行 `./gradlew :composeApp:desktopTest :composeApp:iosSimulatorArm64Test :composeApp:macosAvFoundationBridgeSmoke :composeApp:compileDebugKotlinAndroid`，结果通过，58 个 task，10 executed，48 up-to-date。
- 14 已运行 `git diff --check`，结果通过。
- 14 TDD 红灯证据：新增格式矩阵测试后，首次 targeted desktopTest 因缺少 `AppleAudioFormatSupport`、`AppleAudioFormatSupportMatrix`、`AppleAudioFormatSupportStatus` 编译失败；smoke 扩展过程中还暴露 MP3 样本编码和短样本 ended 判据不稳定，已改为 AVFoundation 可播放性检查。
- 14 交付前已执行 Standards + Spec 两维 code review；Standards 指出的显式类型问题已修复，Spec 指出的 iOS 证据边界和禁词覆盖不足已修复为显式边界记录与全部错误类型禁词测试。
- 15 TDD 红灯证据：先新增 `VlcjDecommissionGateTest.productionTreeHasNoVlcjRuntimeReferences`，首次运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.VlcjDecommissionGateTest` 失败，暴露 `libs.vlcj`、LibVLC 打包任务、运行参数、旧 engine/runtime/adapter 和脚本资源残留；删除旧生产路径后同一测试转绿。
- 15 已运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.VlcjDecommissionGateTest`，结果通过。
- 15 已运行 `./gradlew :composeApp:desktopTest :composeApp:macosAvFoundationBridgeSmoke :composeApp:macosAvFoundationDefaultRuntimeSmoke :composeApp:compileDebugKotlinAndroid`，结果通过；bridge smoke 输出包含 `prepared`、`playing`、多次 `progress`、`ended`、`failed(type=MissingFile)` 和格式矩阵；默认 runtime smoke 输出包含 `current-media`、`playing`、`progress`、`seek`、`paused`、`resume`、`next`、`previous` 和 `stop`。
- 15 已运行 `git diff --check`，结果通过。
- 15 已运行生产树无旧引用证明 `rg -n "vlcj|LibVLC|libvlc|VLC_PLUGIN_PATH|kmp\\.music\\.libvlc|macos-libvlc|TargetFormat\\.(Msi|Deb)|downloadMacosArm64LibVlc|extractMacosArm64LibVlc|prepareMacosArm64LibVlc|stageMacosArm64LibVlc" composeApp/build.gradle.kts gradle/libs.versions.toml composeApp/src/commonMain composeApp/src/desktopMain`，结果无命中。
- 15 交付前已执行 Standards + Spec 两维 code review；Standards 无发现，Spec 初审指出无引用门禁漏扫 `.mm` native 文件，已补充 native 扩展名扫描并重跑精准测试与完整验证。
- 16 TDD 红灯证据：先新增 `ApplePlaybackDocumentationGateTest` 后运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.ApplePlaybackDocumentationGateTest`，4 个测试失败，分别暴露缺少 ADR、旧 vlcj 设计未标记 `Superseded`、播放抽象审计仍保留旧 vlcj 假设、README 仍把真实播放笼统写成后续能力。
- 16 已运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.ApplePlaybackDocumentationGateTest`，结果通过。
- 16 已运行 `./gradlew :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid`，结果通过，38 个 task，4 executed，34 up-to-date。
- 16 已运行 `git diff --check`，结果通过。
- 16 交付前已执行 Standards + Spec 两维 code review；Standards 指出的英文 Markdown 描述和测试重复读文件已修复，Spec 指出的 README iOS P0 提前宣称风险已修复为 App 内播放会话基础适配。
- 本轮已尝试运行 `python3 .codex/hooks/memory_hook.py doctor --root /Users/yanhao/Desktop/demo/kmp-music`，结果失败；原因是 `.codex/hooks/memory_hook.py` 不存在。

## 相关文件

- `.scratch/apple-platform-playback-wayfinder/PRD.md`
- `.scratch/apple-platform-playback-wayfinder/issues/09-apple-playback-contract-fake-bridge.md`
- `.scratch/apple-platform-playback-wayfinder/issues/10-ios-sandbox-import-source-lifecycle.md`
- `.scratch/apple-platform-playback-wayfinder/issues/11-ios-avfoundation-playback-session-p0.md`
- `.scratch/apple-platform-playback-wayfinder/issues/12-macos-avfoundation-bridge-smoke.md`
- `.scratch/apple-platform-playback-wayfinder/issues/13-desktop-default-avfoundation-engine.md`
- `.scratch/apple-platform-playback-wayfinder/issues/14-apple-format-matrix-error-copy.md`
- `.scratch/apple-platform-playback-wayfinder/issues/15-decommission-vlcj-libvlc-production-path.md`
- `.scratch/apple-platform-playback-wayfinder/issues/16-apple-playback-adr-docs.md`
- `docs/APPLE_PLATFORM_FORMAT_SUPPORT_MATRIX.md`
- `docs/adr/0005-apple-platform-avfoundation-playback.md`
- `README.md`
- `docs/superpowers/specs/2026-06-24-macos-vlcj-playback-design.md`
- `docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md`
- `composeApp/build.gradle.kts`
- `gradle/libs.versions.toml`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopAudioRuntimeFactory.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/MacosAvFoundationDefaultRuntimeSmoke.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopMain.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/MacosAvFoundationNativeLibrary.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/MacosAvFoundationNativeBridgeSession.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/JniMacosAvFoundationNativeBridgeSession.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/MacosAvFoundationPlaybackBridge.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/MacosAvFoundationPlaybackBridgeFactory.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/MacosAvFoundationPlaybackBridgeCallback.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/MacosAvFoundationBridgeSmoke.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/MacosAvFoundationFormatMatrixSmoke.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/LocalAudioFileRules.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/PlaybackErrorMessage.kt`
- `composeApp/src/desktopMain/native/macos-avfoundation/KmpMacosAvFoundationBridge.mm`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/DesktopAudioRuntimeFactoryTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopAppleAudioPlayerEngineTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/MacosAvFoundationPlaybackBridgeTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/VlcjDecommissionGateTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/ApplePlaybackDocumentationGateTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/FakeMacosAvFoundationNativeBridgeSession.kt`
- `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/IosEntry.kt`
- `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/IosPlaybackSession.kt`
- `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/IosPlaybackSessionRuntime.kt`
- `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/IosPlaybackHostConfiguration.kt`
- `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/playback/IosAudioSessionController.kt`
- `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/playback/IosPlaybackBridge.kt`
- `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/playback/IosAvFoundationPlaybackBridge.kt`
- `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/playback/IosAvFoundationAudioPlayerEngine.kt`
- `composeApp/src/iosTest/kotlin/com/yanhao/kmpmusic/IosPlaybackSessionRuntimeTest.kt`
- `composeApp/src/iosTest/kotlin/com/yanhao/kmpmusic/playback/FakeIosPlaybackBridge.kt`
- `composeApp/src/iosTest/kotlin/com/yanhao/kmpmusic/playback/IosAvFoundationAudioPlayerEngineTest.kt`
- `composeApp/src/iosTest/kotlin/com/yanhao/kmpmusic/playback/RecordingIosAudioSessionController.kt`
