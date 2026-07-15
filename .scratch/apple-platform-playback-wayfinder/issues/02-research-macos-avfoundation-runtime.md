Status: closed
Labels: wayfinder:research
Parent map: .scratch/apple-platform-playback-wayfinder/map.md
Assignee: codex
Blocked by: 无
Blocks: 确定 macOS 运行时形态；确定苹果平台播放 adapter 边界；确定本地音频来源和权限生命周期；确定苹果系统播放能力范围

# 调研 macOS 版接入 AVFoundation 的运行时路线

## Question

基于官方 Apple 文档、Kotlin Multiplatform / Compose Multiplatform 官方文档和当前项目代码，macOS 版从 Compose Desktop JVM + vlcj 路线迁移到 AVFoundation / AVPlayer 系列播放能力有哪些可行运行时路线？

调研需要回答：

- 继续保留当前 Compose Desktop JVM 壳时，调用 AVFoundation 的可行桥接方式有哪些，分别需要 JNI、JNA、Objective-C、Swift helper、进程内动态库或进程外 helper 的哪类边界。
- 新增或切换 Kotlin/Native macOS target 承接播放是否更适合“macOS 与 iOS 使用同一方案”的目标，会对现有 Compose Desktop UI、Room、扫描目录和打包链路造成什么影响。
- AVFoundation 在 macOS 本地文件播放、沙盒文件权限、安全作用域书签、应用签名和打包上的官方约束。
- 哪条路线最容易复用当前 `AudioPlayerEngine` 抽象，哪条路线会迫使重写桌面应用形态。
- 当前 `composeApp/build.gradle.kts` 中 vlcj 依赖和 LibVLC 打包任务将来下线前，新的 macOS 路线需要补齐哪些事实。

期望输出保存到 `.scratch/apple-platform-playback-wayfinder/research/macos-avfoundation-runtime.md`，所有事实必须引用第一手来源。

## Comments

- 2026-07-14 研究输出：`.scratch/apple-platform-playback-wayfinder/research/macos-avfoundation-runtime.md`。摘要：当前最稳妥路线是保留 Compose Desktop JVM 壳，通过进程内 Objective-C / Swift native bridge 调用 AVFoundation，并新增 `DesktopAvFoundationAudioPlayerEngine` 复用现有 `AudioPlayerEngine` 契约；Kotlin/Native macOS target 更接近源码级统一，但会牵动桌面 UI、Room、扫描和打包形态，建议作为二阶段评估。
- 2026-07-14：已关闭此 research ticket，并在地图 `Decisions so far` 添加事实索引。
