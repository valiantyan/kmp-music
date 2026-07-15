# Apple 平台播放统一采用 AVFoundation

状态：已接受

日期：2026-07-15

## 背景

KMP Music 的播放主干已经收敛为 `MusicAppController -> PlaybackCoordinator -> AudioPlayerEngine -> 平台实现`。Android 使用 Media3，common 层继续负责队列、播放模式、自然结束推进、失败策略、状态回流和快照语义。

Apple 平台迁移后，macOS 从 `vlcj / LibVLC` 改为 Apple `AVFoundation`，iOS 与 macOS 统一到 Apple 原生播放方案。旧 vlcj / LibVLC 路线只保留为历史文档，不能作为当前实现依据。

## 决策

- iOS 使用 Kotlin/Native 调用 AVFoundation / AVPlayer 系列能力；macOS 保留 Compose Desktop JVM 壳，通过进程内 native bridge 调用 AVFoundation。
- 两个平台共享同一个 common `AudioPlayerEngine` 契约和 `PlaybackCoordinator` 业务语义，不新增 Apple 专用 common adapter。
- 平台播放器只负责真实播放、进度、结束、失败和初始化事实回流；队列推进、随机、单曲循环和失败跳过仍由 common 层决定。
- macOS 生产链路不保留 vlcj fallback、双引擎 runtime gate 或切回 VLC 的完成方案。
- 非 macOS Desktop 不承诺真实播放；Windows / Linux Desktop 如需恢复桌面播放能力，必须重新设计平台播放器和分发边界。

## 平台能力范围

macOS P0 承诺 App 内播放、暂停、上一首、下一首、seek、真实进度、自然结束回流、失败回流，以及窗口最小化后继续播放。macOS P0 不承诺系统媒体键、控制中心、Now Playing、菜单栏常驻、关闭窗口后继续播放或发布级 Gatekeeper 验收。

iOS P0 承诺 App 内 AVFoundation 播放会话、播放前配置 `AVAudioSession` 的 playback category、导入后消费 App 沙盒内文件 URL、中断和输出路线变化基础处理。iOS 真机后台继续播放、锁屏后继续播放、回前台状态同步和宿主 `UIBackgroundModes = audio` 配置仍需要人工验收或后续 gate 记录。

## 桌面分发边界

当前桌面真实播放只面向 macOS AVFoundation。Compose Desktop native distribution 已收窄为 macOS DMG；Windows / Linux Desktop 不应被 README、构建配置或交接记录误写为本轮可真实播放平台。

签名、公证、Developer ID、staple 和发布级 Gatekeeper 验收是发布风险，不作为本 ADR 宣称已通过的实现能力。

## 格式矩阵

格式矩阵以 [docs/APPLE_PLATFORM_FORMAT_SUPPORT_MATRIX.md](../APPLE_PLATFORM_FORMAT_SUPPORT_MATRIX.md) 为准。P0 支持结论覆盖 `MP3`、`M4A/AAC`、`WAV`、`FLAC`、`AIFF/ALAC`，`OGG/OPUS` 和 `AMR` 保持待验证，不进入 Apple P0 扫描入口。

矩阵结论不能从旧 LibVLC 支持范围推断。macOS 自动化证据来自 AVFoundation 可播放性检查和真实播放 smoke；iOS 真实样本播放仍需真机或后续 gate 验证。

## 验证方式

实现分支的自动化验证至少包含：

- `./gradlew :composeApp:desktopTest`
- `./gradlew :composeApp:macosAvFoundationBridgeSmoke`
- `./gradlew :composeApp:macosAvFoundationDefaultRuntimeSmoke`
- `./gradlew :composeApp:compileDebugKotlinAndroid`
- iOS framework 或 iOS 测试任务，任务名必须先通过 `./gradlew :composeApp:tasks` 查证。
- 无 vlcj / LibVLC 生产引用证明，覆盖生产代码、Gradle 依赖、打包任务、运行参数和用户提示。

人工验收需要继续记录真实播放 smoke、打包产物 bridge 加载、格式矩阵证据、iOS 后台播放宿主配置、iOS 真机样本播放、签名公证风险和剩余发布风险。未实际运行的命令或未人工验收的系统能力不得写成已通过。

## 后果

旧 macOS vlcj 设计文档必须标记为 `Superseded`。旧播放抽象审计中把 Desktop 等同于 vlcj、未来 Windows 复用 Desktop vlcj engine 或 macOS 不是 AVFoundation 路线的表述，必须改为历史状态或待重新设计。

后续实现者应优先维护 Apple AVFoundation 链路和 common 播放契约；遇到新平台播放、系统媒体入口、签名公证或 App Sandbox 权限模型时，应另开设计，不复活旧 vlcj / LibVLC 生产链路。
