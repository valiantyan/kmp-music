# 项目记忆状态

## 当前目标

苹果平台统一播放迁移实现批次推进到 Ticket 13：桌面默认播放链路已切换到 macOS AVFoundation engine，并完成实现、验证、票据更新和交付前审查。

## 当前进度

- 09 Apple 播放契约和 fake bridge 行为防线已完成，状态为 `ready-for-human`。
- 10 iOS 沙盒导入来源闭环已完成，状态为 `ready-for-human`。
- 11 iOS AVFoundation 播放会话 P0 已完成，状态为 `ready-for-human`。
- 12 macOS AVFoundation bridge 最小真实播放已完成，状态为 `ready-for-human`。
- 13 桌面默认 AVFoundation engine 已完成，状态为 `ready-for-human`。
- 11 新增 iOS 进程级 `IosPlaybackSession` / `IosPlaybackSessionRuntime`，由会话持有 `MusicAppController`、`IosAvFoundationAudioPlayerEngine` 和 `IosAvAudioSessionController`，`IosEntry` 只复用会话并请求一次播放快照恢复。
- 11 新增 iOS AVFoundation 播放链路：`IosPlaybackBridge`、`IosAvFoundationPlaybackBridge`、`IosAvFoundationAudioPlayerEngine`、`IosAudioSessionController` 和 `IosPlaybackHostConfiguration`。
- 11 真实 bridge 使用单个 `AVPlayer`，不引入 `AVQueuePlayer`；队列、播放模式、自然结束推进和失败策略仍由 common `PlaybackCoordinator` 拥有。
- 11 播放前配置并激活 `AVAudioSessionCategoryPlayback`；宿主 Info.plist 需要 `UIBackgroundModes = audio`，当前仓库没有真实 iOS 宿主工程，真机后台播放仍是人工验收项。
- 12 新增 macOS 进程内 JNI / Objective-C++ AVFoundation bridge，JVM 侧通过 `MacosAvFoundationPlaybackBridge` 接入 09 号 `ApplePlaybackBridge` 契约。
- 12 新增 Gradle `compileMacosAvFoundationBridge` 编译 dylib，`desktopTest` 显式加载该 dylib；新增 `macosAvFoundationBridgeSmoke` 生成本机 M4A/AAC 样本并验证真实 AVFoundation 事件回流。
- 13 已将 `DesktopAudioRuntimeFactory` 默认装配切到 `DesktopAppleAudioPlayerEngine + MacosAvFoundationPlaybackBridge`，bridge 不可用时映射为统一 `EngineUnavailable` 事件，不回退 vlcj。
- 13 新增 `macosAvFoundationDefaultRuntimeSmoke`，通过默认桌面运行时、`MusicAppController` 和真实 AVFoundation bridge 验证 current media、playing、progress、seek、pause/resume、next、previous 和 stop。
- 13 未删除 vlcj / LibVLC 依赖、打包任务、旧 engine 或旧测试资产；这些仍属于 15 号下线票范围，不作为 13 的 runtime fallback。

## 下一步

- 当前可继续推进 `14-apple-format-matrix-error-copy.md` 或 `15-decommission-vlcj-libvlc-production-path.md`；14 依赖 11/13，15 依赖 13，前置均已满足。
- 后续阻塞链保持不变：16 依赖 14/15；17 依赖 11/14/15/16。
- 若继续 iOS 方向，真机人工验收需由宿主工程确认 `UIBackgroundModes = audio`、后台继续播放、锁屏后继续播放和回前台状态同步。

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
- 本轮已尝试运行 `python3 .codex/hooks/memory_hook.py doctor --root /Users/yanhao/Desktop/demo/kmp-music`，结果失败；原因是 `.codex/hooks/memory_hook.py` 不存在。

## 相关文件

- `.scratch/apple-platform-playback-wayfinder/PRD.md`
- `.scratch/apple-platform-playback-wayfinder/issues/09-apple-playback-contract-fake-bridge.md`
- `.scratch/apple-platform-playback-wayfinder/issues/10-ios-sandbox-import-source-lifecycle.md`
- `.scratch/apple-platform-playback-wayfinder/issues/11-ios-avfoundation-playback-session-p0.md`
- `.scratch/apple-platform-playback-wayfinder/issues/12-macos-avfoundation-bridge-smoke.md`
- `.scratch/apple-platform-playback-wayfinder/issues/13-desktop-default-avfoundation-engine.md`
- `composeApp/build.gradle.kts`
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
- `composeApp/src/desktopMain/native/macos-avfoundation/KmpMacosAvFoundationBridge.mm`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/DesktopAudioRuntimeFactoryTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopAppleAudioPlayerEngineTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/MacosAvFoundationPlaybackBridgeTest.kt`
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
