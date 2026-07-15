# 项目记忆状态

## 当前目标

修复 `./gradlew :composeApp:packageDmg` 和 `./gradlew :composeApp:packageReleaseDmg` 生成的 macOS DMG 安装包无法播放音频的问题：确保打包产物内置 macOS AVFoundation bridge dylib，且运行时能从 `.app` 自身 resources 加载该 bridge。

## 当前进度

- 本轮已确认旧 `desktopRun --dry-run` 任务图不包含 `compileMacosAvFoundationBridge`，而现有真实播放 smoke 依赖显式 `kmp.music.macos.avfoundation.bridge.path`，因此 `desktopRun` 会进入缺少 bridge 的播放路径。
- 已在 `composeApp/build.gradle.kts` 通过 `tasks.withType<JavaExec>().configureEach` 惰性命中后期注册的 `desktopRun`，为它增加 `compileMacosAvFoundationBridge` 依赖，并设置 `kmp.music.macos.avfoundation.bridge.path` 指向 `composeApp/build/macos-avfoundation-bridge/native/libkmp_music_macos_avfoundation_bridge.dylib`。
- 已新增 `MacosAvFoundationDesktopRunGateTest`，门禁 `desktopRun` 必须保留 bridge 编译依赖和 JVM 属性注入，防止开发运行路径再次绕过 AVFoundation bridge。
- 本轮已确认旧 `packageDmg` 产物中 `composeApp/build/compose/binaries/main/app/KMP Music.app/Contents/app/resources/macos-avfoundation/libkmp_music_macos_avfoundation_bridge.dylib` 不存在，导致 DMG 安装包运行时无法加载 AVFoundation bridge。
- 已新增 `stageMacosAvFoundationBridgeIntoPackageApp`，让 `packageDmg` 先执行 `compileMacosAvFoundationBridge` 和 `createDistributable`，再把 dylib 放入 `KMP Music.app/Contents/app/resources/macos-avfoundation/`。
- 已新增 `stageMacosAvFoundationBridgeIntoReleasePackageApp`，让 `packageReleaseDmg` 先执行 `compileMacosAvFoundationBridge` 和 `createReleaseDistributable`，再把 dylib 放入 `composeApp/build/compose/binaries/main-release/app/KMP Music.app/Contents/app/resources/macos-avfoundation/`。
- 已扩展 `SystemMacosAvFoundationNativeLibraryLoader`：优先使用显式 `kmp.music.macos.avfoundation.bridge.path`，其次从 Compose Desktop 注入的 `compose.application.resources.dir` 查找 packaged app resources 内的 dylib，最后才回退 `System.loadLibrary`。
- 已新增 `MacosAvFoundationNativeLibraryTest`、`MacosAvFoundationPackageDmgGateTest`、`macosAvFoundationPackagedBridgeSmoke` 和 `macosAvFoundationReleasePackagedBridgeSmoke`，分别覆盖 resources 路径解析、main / main-release DMG 打包配置和 packaged resources dylib 真实播放加载。
- 09 Apple 播放契约和 fake bridge 行为防线已完成，状态为 `ready-for-human`。
- 10 iOS 沙盒导入来源闭环已完成，状态为 `ready-for-human`。
- 11 iOS AVFoundation 播放会话 P0 已完成，状态为 `ready-for-human`。
- 12 macOS AVFoundation bridge 最小真实播放已完成，状态为 `ready-for-human`。
- 13 桌面默认 AVFoundation engine 已完成，状态为 `ready-for-human`。
- 14 Apple 格式矩阵和播放错误自救文案已完成，状态为 `ready-for-human`。
- 15 下线 vlcj / LibVLC 生产链路已完成，状态为 `ready-for-human`。
- 16 更新 ADR 和旧播放路线文档已完成，状态为 `ready-for-human`。
- 17 汇总硬门禁证据和人工验收交接已完成记录，状态为 `ready-for-human`；该状态只表示交接票可人工复核，不表示整批硬门禁全部通过。
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
- 17 新增 `ApplePlaybackGateEvidenceHandoffTest`，把 17 号本地 issue 作为交接审计产物门禁，检查状态、验收项、自动化证据、真实播放 smoke、打包结果、人工验收待办、剩余风险和 code review 记录。
- 17 已记录 `packageDmg` 成功生成 `composeApp/build/compose/binaries/main/dmg/KMP Music-1.0.0.dmg`，但 `find composeApp/build/compose/binaries/main/app -name 'libkmp_music_macos_avfoundation_bridge.dylib' -print` 无输出；当前 `.app` 未内置 macOS AVFoundation bridge dylib。

## 下一步

- 本轮代码层面已完成并通过验证；如需要给别人测试 release 包，重新执行 `./gradlew :composeApp:packageReleaseDmg` 后分发 `composeApp/build/compose/binaries/main-release/dmg/KMP Music-1.0.0.dmg`。
- 若需要人工听感确认，可安装或打开新 DMG 里的 App 后播放本地音频；当前自动化已验证 packaged resources 内 dylib 可被真实 AVFoundation smoke 加载并播放。
- 若继续 iOS 方向，真机人工验收需由宿主工程确认 `UIBackgroundModes = audio`、后台继续播放、锁屏后继续播放、回前台状态同步，以及 MP3、M4A/AAC、WAV、FLAC、AIFF/ALAC 样本真实播放。

## 阻塞

- `.codex/hooks/memory_hook.py` 当前不存在，无法运行 memory doctor。
- `desktopRun` 开发运行路径已修复，不再是本轮阻塞。
- `packageDmg` 和 `packageReleaseDmg` 打包产物 bridge 内置 / 加载策略已在本轮修复；此前 17 号交接记录中的该阻塞已经过期。

## 验证状态

- 本轮红灯证据：新增 `MacosAvFoundationDesktopRunGateTest` 后首次运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.MacosAvFoundationDesktopRunGateTest` 失败，暴露 `composeApp/build.gradle.kts` 缺少 `desktopRun` bridge 配置。
- 本轮已运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.MacosAvFoundationDesktopRunGateTest`，结果通过。
- 本轮已运行 `./gradlew :composeApp:desktopRun --dry-run`，结果通过，任务图包含 `:composeApp:compileMacosAvFoundationBridge` 后再到 `:composeApp:desktopRun`。
- 本轮已运行 `./gradlew :composeApp:desktopTest :composeApp:macosAvFoundationBridgeSmoke :composeApp:macosAvFoundationDefaultRuntimeSmoke :composeApp:compileDebugKotlinAndroid`，结果通过；bridge smoke 输出 `prepared`、`playing`、多次 `progress`、`ended`、格式矩阵和 `failed(type=MissingFile)`；默认 runtime smoke 输出 `current-media`、`playing`、`progress`、`seek`、`paused`、`resume`、`next`、`previous` 和 `stop`。
- 本轮 DMG 红灯证据：运行 `test -f 'composeApp/build/compose/binaries/main/app/KMP Music.app/Contents/app/resources/macos-avfoundation/libkmp_music_macos_avfoundation_bridge.dylib'` 返回 exit code 1，确认旧 package app resources 未内置 bridge。
- 本轮已运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.MacosAvFoundationNativeLibraryTest --tests com.yanhao.kmpmusic.playback.MacosAvFoundationPackageDmgGateTest --tests com.yanhao.kmpmusic.playback.MacosAvFoundationDesktopRunGateTest`，结果通过。
- 本轮已运行 `./gradlew :composeApp:packageDmg --dry-run`，结果通过，任务图包含 `:composeApp:stageMacosAvFoundationBridgeIntoPackageApp` 后再到 `:composeApp:packageDmg`。
- 本轮已运行 `./gradlew :composeApp:packageDmg`，结果通过，生成 `composeApp/build/compose/binaries/main/dmg/KMP Music-1.0.0.dmg`。
- 本轮已运行 `test -f 'composeApp/build/compose/binaries/main/app/KMP Music.app/Contents/app/resources/macos-avfoundation/libkmp_music_macos_avfoundation_bridge.dylib'`，结果通过；`find composeApp/build/compose/binaries/main/app -name 'libkmp_music_macos_avfoundation_bridge.dylib' -print` 输出 packaged app resources 内 dylib 路径。
- 本轮已运行 `./gradlew :composeApp:macosAvFoundationPackagedBridgeSmoke`，结果通过；该 smoke 未设置显式 bridge path，通过 `compose.application.resources.dir` 从 packaged app resources 加载 dylib，输出 `prepared`、`playing`、多次 `progress`、`ended`、格式矩阵和 `failed(type=MissingFile)`。
- 本轮 release DMG 红灯证据：`find 'composeApp/build/compose/binaries/main-release/app' -name 'libkmp_music_macos_avfoundation_bridge.dylib' -print` 无输出；`./gradlew :composeApp:packageReleaseDmg --dry-run` 旧任务图未包含 `stageMacosAvFoundationBridgeIntoReleasePackageApp`。
- 本轮已运行 `./gradlew :composeApp:packageReleaseDmg --dry-run`，结果通过，任务图包含 `:composeApp:stageMacosAvFoundationBridgeIntoReleasePackageApp` 后再到 `:composeApp:packageReleaseDmg`。
- 本轮已运行 `./gradlew :composeApp:packageReleaseDmg`，结果通过，生成 `composeApp/build/compose/binaries/main-release/dmg/KMP Music-1.0.0.dmg`。
- 本轮已运行 `test -f 'composeApp/build/compose/binaries/main-release/app/KMP Music.app/Contents/app/resources/macos-avfoundation/libkmp_music_macos_avfoundation_bridge.dylib'`，结果通过；`find 'composeApp/build/compose/binaries/main-release/app' -name 'libkmp_music_macos_avfoundation_bridge.dylib' -print` 输出 release packaged app resources 内 dylib 路径。
- 本轮已运行 `./gradlew :composeApp:macosAvFoundationReleasePackagedBridgeSmoke`，结果通过；该 smoke 未设置显式 bridge path，通过 release packaged app resources 加载 dylib，输出 `prepared`、`playing`、多次 `progress`、`ended`、格式矩阵和 `failed(type=MissingFile)`。
- 本轮已运行 `./gradlew :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid`，结果通过。
- 本轮已运行 `git diff --check`，结果通过。
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
- 17 TDD 红灯证据：先新增 `ApplePlaybackGateEvidenceHandoffTest` 后运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.ApplePlaybackGateEvidenceHandoffTest`，3 个测试失败，暴露 17 号 issue 仍是 `ready-for-agent`、验收项未勾选、缺少硬门禁证据和 code review / 剩余风险交接记录；更新交接后同一测试通过。
- 17 已运行 `./gradlew :composeApp:tasks --all`，结果通过，并确认 iOS framework 编译任务名为 `linkDebugFrameworkIosSimulatorArm64`。
- 17 已运行 `./gradlew :composeApp:packageDmg`，结果通过，生成 `composeApp/build/compose/binaries/main/dmg/KMP Music-1.0.0.dmg`。
- 17 已运行 `find composeApp/build/compose/binaries/main/app -name 'libkmp_music_macos_avfoundation_bridge.dylib' -print`，结果无输出；打包产物 bridge 加载检查未通过。
- 17 已运行 `rg -n "vlcj|LibVLC|libvlc|VLC_PLUGIN_PATH|kmp\\.music\\.libvlc|macos-libvlc|TargetFormat\\.(Msi|Deb)|downloadMacosArm64LibVlc|extractMacosArm64LibVlc|prepareMacosArm64LibVlc|stageMacosArm64LibVlc" composeApp/build.gradle.kts gradle/libs.versions.toml composeApp/src/commonMain composeApp/src/desktopMain`，结果无命中。
- 17 已运行 `./gradlew :composeApp:desktopTest :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:macosAvFoundationBridgeSmoke :composeApp:macosAvFoundationDefaultRuntimeSmoke :composeApp:compileDebugKotlinAndroid`，结果通过，46 个 task，7 executed，39 up-to-date；bridge smoke 输出 `prepared`、`playing`、多次 `progress`、`ended`、格式矩阵和 `failed(type=MissingFile)`；默认 runtime smoke 输出 `current-media`、`playing`、`progress`、`seek`、`paused`、`resume`、`next`、`previous` 和 `stop`。
- 17 已运行 `git diff --check`，结果通过。
- 17 交付前已执行 Standards + Spec 两维 code review；Standards 初审发现 code review 结论占位文本已修复，Spec 初审确认打包 bridge 内置失败必须作为整批人工验收阻塞记录，不能把 17 当作整批通过证据。
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
- `.scratch/apple-platform-playback-wayfinder/issues/17-apple-playback-gate-evidence-handoff.md`
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
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/MacosAvFoundationDesktopRunGateTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/MacosAvFoundationNativeLibraryTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/MacosAvFoundationPackageDmgGateTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/VlcjDecommissionGateTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/ApplePlaybackDocumentationGateTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/ApplePlaybackGateEvidenceHandoffTest.kt`
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
