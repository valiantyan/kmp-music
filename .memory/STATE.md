# 项目记忆状态

## 当前目标

苹果平台统一播放迁移实现批次推进到 Ticket 11：iOS AVFoundation 播放会话 P0 已完成实现、验证、票据更新和交付前审查。

## 当前进度

- 09 Apple 播放契约和 fake bridge 行为防线已完成，状态为 `ready-for-human`。
- 10 iOS 沙盒导入来源闭环已完成，状态为 `ready-for-human`。
- 11 iOS AVFoundation 播放会话 P0 已完成，状态为 `ready-for-human`。
- 11 新增 iOS 进程级 `IosPlaybackSession` / `IosPlaybackSessionRuntime`，由会话持有 `MusicAppController`、`IosAvFoundationAudioPlayerEngine` 和 `IosAvAudioSessionController`，`IosEntry` 只复用会话并请求一次播放快照恢复。
- 11 新增 iOS AVFoundation 播放链路：`IosPlaybackBridge`、`IosAvFoundationPlaybackBridge`、`IosAvFoundationAudioPlayerEngine`、`IosAudioSessionController` 和 `IosPlaybackHostConfiguration`。
- 11 真实 bridge 使用单个 `AVPlayer`，不引入 `AVQueuePlayer`；队列、播放模式、自然结束推进和失败策略仍由 common `PlaybackCoordinator` 拥有。
- 11 播放前配置并激活 `AVAudioSessionCategoryPlayback`；宿主 Info.plist 需要 `UIBackgroundModes = audio`，当前仓库没有真实 iOS 宿主工程，真机后台播放仍是人工验收项。
- 11 已补 iOS fake bridge、audio session 和 session runtime 测试，覆盖单当前媒体准备、播放前 audio session、seek/skip generation、缓冲/进度/结束/失败回流、中断恢复、输出断开、release 后延迟回调丢弃和会话收口。
- 11 code review 中发现的 engine 状态未串行化、native observer 使用全局 generation、runtime 一次性状态未加锁问题已修复。

## 下一步

- 当前可继续推进 `12-macos-avfoundation-bridge-smoke.md`，其前置 09 已满足。
- 后续阻塞链保持不变：13 依赖 12；14 依赖 11/13；15 依赖 13；16 依赖 14/15；17 依赖 11/14/15/16。
- 若继续 iOS 方向，真机人工验收需由宿主工程确认 `UIBackgroundModes = audio`、后台继续播放、锁屏后继续播放和回前台状态同步。

## 阻塞

- `.codex/hooks/memory_hook.py` 当前不存在，无法运行 memory doctor。

## 验证状态

- 11 已运行 `./gradlew :composeApp:tasks --all`，结果通过，并确认 iOS framework 编译任务名为 `linkDebugFrameworkIosSimulatorArm64`。
- 11 已运行 `./gradlew :composeApp:iosSimulatorArm64Test :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid`，结果通过。
- 11 已运行 `git diff --check`，结果通过。
- 11 TDD 红灯证据：新增 iOS 播放会话和 AVFoundation engine fake 测试后，首次 `./gradlew :composeApp:iosSimulatorArm64Test` 因缺少 `IosPlaybackBridge`、`IosAvFoundationAudioPlayerEngine`、`IosPlaybackSessionRuntime` 等新类型编译失败；实现后转绿。
- 11 交付前已执行 Standards + Spec 两维 code review；阻塞问题已修复，剩余风险记录在 Ticket Comments。
- 本轮尝试运行 `python3 .codex/hooks/memory_hook.py doctor --root /Users/yanhao/Desktop/demo/kmp-music`，结果失败；原因是 `.codex/hooks/memory_hook.py` 不存在。

## 相关文件

- `.scratch/apple-platform-playback-wayfinder/PRD.md`
- `.scratch/apple-platform-playback-wayfinder/issues/09-apple-playback-contract-fake-bridge.md`
- `.scratch/apple-platform-playback-wayfinder/issues/10-ios-sandbox-import-source-lifecycle.md`
- `.scratch/apple-platform-playback-wayfinder/issues/11-ios-avfoundation-playback-session-p0.md`
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
