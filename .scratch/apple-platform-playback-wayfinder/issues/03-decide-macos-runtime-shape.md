Status: closed
Labels: wayfinder:grilling
Parent map: .scratch/apple-platform-playback-wayfinder/map.md
Assignee: codex
Blocked by: 无
Blocks: 确定苹果平台播放 adapter 边界；确定本地音频来源和权限生命周期；确定苹果系统播放能力范围

# 确定 macOS 运行时形态

## Question

macOS 版应继续保留当前 Compose Desktop JVM 应用壳并通过受控 native bridge 调用 AVFoundation，还是新增或切换到更接近 iOS 的 Kotlin/Native macOS 路线来承接 Apple 原生播放？

这个决策需要明确：

- “macOS 与 iOS 使用同一方案”在本项目中指复用同一底层 Apple 播放框架、复用同一 Kotlin adapter 代码，还是只复用同一 `AudioPlayerEngine` 契约和行为语义。
- 当前桌面 UI、数据库、扫描目录、打包、测试和开发运行成本能接受多大迁移幅度。
- 是否允许先用 JVM 壳 + native bridge 作为过渡路线，后续再评估更深的 macOS Native 迁移。
- 哪些现有 vlcj / LibVLC 风险必须被这次路线选择一次性消除。

## Comments

- 2026-07-14：当前会话已认领此 ticket，开始短 grilling 以确认 macOS 运行时形态。
- 2026-07-14 resolution：确认首版 macOS 播放迁移采用“保留当前 Compose Desktop JVM 应用壳 + 进程内 Objective-C / Swift native bridge + AVFoundation / AVPlayer”的运行时形态。`macOS 与 iOS 使用同一方案` 在首版定义为：同用 Apple AVFoundation 播放能力，并保持同一 `AudioPlayerEngine` 行为契约、事件语义和播放业务规则；不要求首版共享同一份 Kotlin/Native 播放实现。

  `expect/actual` 只用于 KMP 平台装配：`iosMain actual` 可直接创建 iOS AVFoundation engine，`desktopMain actual` 创建桌面 AVFoundation engine；桌面 engine 内部再通过 JVM 到 Objective-C / Swift 的 native bridge 调用 `AVPlayer`。Kotlin/Native macOS target 或更深 macOS Native 迁移留作二阶段评估，不进入首版迁移。此路线需要一次性消除 vlcj / LibVLC 依赖、运行时下载提取和打包内置风险，但实际删除顺序留给后续“确定 vlcj 下线迁移顺序”ticket 决定。
