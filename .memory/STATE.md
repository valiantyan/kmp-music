# 项目记忆状态

## 当前目标

苹果平台统一播放迁移 wayfinder 已压缩成实现规格，并已拆分为 09-17 实现 issue。09 Apple 播放契约和 fake bridge 行为防线已实现并验证通过，下一步可从 10 或依赖 09 的 12 继续推进，但 11 仍需等待 10 完成。

## 当前进度

- 已创建实现规格：`.scratch/apple-platform-playback-wayfinder/PRD.md`。
- 已对实现规格执行对抗式审查，并补入 5 个最可能翻车点及对应修正要求。
- 已按 `to-tickets` 拆分并发布 9 个实现 issue：09 Apple 播放契约和 fake bridge、10 iOS 沙盒导入、11 iOS AVFoundation 播放会话、12 macOS bridge smoke、13 桌面默认 AVFoundation、14 格式矩阵和错误文案、15 下线 vlcj / LibVLC、16 ADR 和旧文档、17 门禁证据交接。
- 已对 09-17 实现 issue 全部启用对抗式审查：每张票新增 `## 对抗式审查`，并补强最容易假绿或越界的验收标准。
- 规格来源是 `.scratch/apple-platform-playback-wayfinder/` 下已关闭的 8 个决策 ticket 和两份研究输出。
- 规格状态为 `ready-for-agent`，可交给实现 Agent 使用。
- 已确认首版路线：iOS 用 Kotlin/Native 直接接入 AVFoundation；macOS 保留 Compose Desktop JVM 壳，通过进程内 Objective-C / Swift native bridge 调用 AVFoundation。
- 已确认 common 边界：`AudioPlayerEngine` 仍是唯一播放契约，`PlaybackCoordinator` 继续拥有队列、播放模式、自然结束和失败推进语义。
- 已确认 macOS 交付目标：实现分支交付人工验收前不保留活跃 vlcj / LibVLC 生产引用，不保留 vlcj fallback 或双引擎 runtime gate。
- 对抗式审查补强点包括：iOS 沙盒内 `localUri` 硬边界、iOS audio session / background mode 配置证据、macOS bridge 显式命令事件契约、最低 Gradle 命令线、fake 测试之外的真实播放和打包加载门禁。
- 09-17 issue 对抗式审查补强点包括：禁止偷改 common 播放契约、iOS 导入半成品不得入库、iOS 后台播放不得伪装成锁屏控制完成、macOS bridge smoke 不切默认 engine、默认切换不得 fallback 到 vlcj、格式矩阵必须有证据来源、删除 vlcj 时保留行为防线、文档不得提前宣称未验收能力、交接证据不得把人工待验写成通过。
- 2026-07-14 已完成 09：新增 desktop 平台内部 Apple bridge 契约、`DesktopAppleAudioPlayerEngine`、fake bridge 和行为测试；未修改 common `AudioPlayerEngine` 或 `PlaybackCoordinator`。
- 09 Ticket 已更新为 `ready-for-human`，所有验收清单已勾选，Comments 已记录实现摘要、验证命令、对抗式审查、code review 和剩余风险。

## 下一步

- 当前 frontier：`10-ios-sandbox-import-source-lifecycle.md` 仍无阻塞；`12-macos-avfoundation-bridge-smoke.md` 的 09 前置已满足，可在需要时认领；11 仍依赖 10 和已完成的 09。
- 后续按阻塞边推进：11 依赖 09/10；12 依赖 09；13 依赖 12；14 依赖 11/13；15 依赖 13；16 依赖 14/15；17 依赖 11/14/15/16。

## 阻塞

- `.codex/hooks/memory_hook.py` 当前不存在，无法运行 memory doctor。

## 验证状态

- 已读取 `to-spec` 技能、issue tracker 规则、分诊标签规则、wayfinder map、8 个决策 ticket、两份研究输出、项目 PRD 摘要、播放契约和现有测试先例。
- 09 实现已运行 `./gradlew :composeApp:desktopTest`，结果通过。
- 09 实现已运行 `./gradlew :composeApp:compileDebugKotlinAndroid`，结果通过。
- 已运行 `git diff --cached --check`，结果通过。
- TDD 红灯证据：新增 `DesktopAppleAudioPlayerEngineTest` 后，首次 `./gradlew :composeApp:desktopTest` 因缺少 `FakeApplePlaybackBridge`、`DesktopAppleAudioPlayerEngine` 等新类型编译失败；实现后转绿。
- 已确认新增规格和项目记忆文件存在且当前为未跟踪文件。
- 已确认 09-17 实现 issue 文件存在，均包含 `Status: ready-for-agent`、`Labels: ready-for-agent` 和阻塞边。
- 已确认 09-17 实现 issue 均包含 `## 对抗式审查`。
- 已检查新增规格、09-17 实现 issue 和项目记忆没有行尾空白。
- 已确认 `.codex/hooks/memory_hook.py` 不存在，无法运行 memory doctor。

## 相关文件

- `.scratch/apple-platform-playback-wayfinder/PRD.md`
- `.scratch/apple-platform-playback-wayfinder/map.md`
- `.scratch/apple-platform-playback-wayfinder/issues/01-research-ios-avfoundation-kotlin-native.md`
- `.scratch/apple-platform-playback-wayfinder/issues/02-research-macos-avfoundation-runtime.md`
- `.scratch/apple-platform-playback-wayfinder/issues/03-decide-macos-runtime-shape.md`
- `.scratch/apple-platform-playback-wayfinder/issues/04-decide-apple-playback-adapter-boundary.md`
- `.scratch/apple-platform-playback-wayfinder/issues/05-decide-apple-audio-source-permission-lifecycle.md`
- `.scratch/apple-platform-playback-wayfinder/issues/06-decide-apple-system-playback-scope.md`
- `.scratch/apple-platform-playback-wayfinder/issues/07-decide-vlcj-decommission-sequence.md`
- `.scratch/apple-platform-playback-wayfinder/issues/08-decide-validation-and-doc-gates.md`
- `.scratch/apple-platform-playback-wayfinder/issues/09-apple-playback-contract-fake-bridge.md`
- `.scratch/apple-platform-playback-wayfinder/issues/10-ios-sandbox-import-source-lifecycle.md`
- `.scratch/apple-platform-playback-wayfinder/issues/11-ios-avfoundation-playback-session-p0.md`
- `.scratch/apple-platform-playback-wayfinder/issues/12-macos-avfoundation-bridge-smoke.md`
- `.scratch/apple-platform-playback-wayfinder/issues/13-desktop-default-avfoundation-engine.md`
- `.scratch/apple-platform-playback-wayfinder/issues/14-apple-format-matrix-error-copy.md`
- `.scratch/apple-platform-playback-wayfinder/issues/15-decommission-vlcj-libvlc-production-path.md`
- `.scratch/apple-platform-playback-wayfinder/issues/16-apple-playback-adr-docs.md`
- `.scratch/apple-platform-playback-wayfinder/issues/17-apple-playback-gate-evidence-handoff.md`
- `.scratch/apple-platform-playback-wayfinder/research/ios-avfoundation-kotlin-native.md`
- `.scratch/apple-platform-playback-wayfinder/research/macos-avfoundation-runtime.md`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/ApplePlaybackBridge.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/ApplePlaybackBridgeEventReducer.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopAppleAudioPlayerEngine.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopApplePlaybackCommand.kt`
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopApplePlaybackCommandLoop.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopAppleAudioPlayerEngineTest.kt`
- `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/FakeApplePlaybackBridge.kt`
