Status: closed
Labels: wayfinder:grilling
Parent map: .scratch/apple-platform-playback-wayfinder/map.md
Assignee: codex
Blocked by: 无
Blocks: 确定 vlcj 下线迁移顺序；确定验证和文档门禁

# 确定苹果平台播放 adapter 边界

## Question

在不污染 `commonMain`、不破坏 `PlaybackCoordinator` 业务语义的前提下，苹果平台播放实现应如何划分共享契约、平台 adapter 和平台原生播放器边界？

这个决策需要明确：

- 是否保留当前 `AudioPlayerEngine` 作为唯一 common 播放抽象，还是需要新增只面向苹果平台的内部 adapter 契约。
- iOS 与 macOS 可以共享哪些代码：队列映射、事件归一化、错误归一化、进度轮询、AVPlayer 状态 reducer，还是只能共享设计和测试契约。
- macOS bridge 如果存在，应该隐藏在平台实现内部，还是提升成可测试的独立边界。
- 旧 `DesktopVlcjAudioPlayerEngine` 的测试资产应迁移为通用平台 engine 行为测试，还是作为删除时的参考。

## Comments

- 2026-07-14：当前会话已认领此 ticket，按 grilling 顺序确认苹果平台播放 adapter 边界。
- 2026-07-14 resolution：确认 `AudioPlayerEngine` 继续作为 `commonMain` 唯一播放契约，不新增面向苹果平台的 common 层专用 adapter。`PlaybackCoordinator` 继续拥有队列、播放模式、自然结束推进、失败跳过、随机和单曲循环等业务语义；苹果平台实现只能把平台播放事实归一化为 `PlaybackEngineEvent` 回流 common。

  iOS 与 macOS 首版共享行为语义和测试契约，不强求共享同一份播放器实现代码。共享内容包括 `AudioPlayerEngine` 命令语义、`PlaybackEngineEvent` 事件语义、单 `AVPlayer` 加 Kotlin / 上层队列的适配策略、结束 / 失败 / 缓冲 / 暂停 / 进度事件映射，以及切歌代际、释放后不回调、seek / skip 竞态、失败归一化等行为测试场景。iOS 的 Kotlin/Native AVFoundation 调用、macOS 的 JVM 到 Objective-C / Swift bridge、KVO / Notification 生命周期和文件权限实现都留在各自平台层。

  macOS native bridge 必须提升为 `desktopMain` 内部可测试边界，但不得进入 common。建议形态是 `DesktopAvFoundationAudioPlayerEngine : AudioPlayerEngine` 负责队列、代际、命令串行化和事件 reducer，底层 `DesktopAvFoundationPlayerBridge` 或 `MacosAvPlayerAdapter` 负责 JNI / Objective-C / Swift 调用、observer、回调和原生资源释放；测试使用 fake bridge 驱动确定性事件，覆盖回调乱序、旧 generation、release 后回调、prepare / play / seek 竞态和 bridge 初始化失败。

  旧 `DesktopVlcjAudioPlayerEngine` 的测试资产只迁移平台 engine 行为防线：公开命令和底层回调串行化、generation 过滤、`setQueue` 准备确认或失败收口、`skipToIndex` 后旧回调不污染当前曲目、release 后不发事件且不挂起 ack、进度事件只归因当前媒体、adapter / bridge 初始化失败映射成统一 `PlaybackError`。vlcj 插件路径、LibVLC runtime resolve、vlcj media location 转换、vlcj callback snapshot 细节和 LibVLC 打包验收脚本行为不迁移。

  对抗式审查结论：第一，若把 Apple adapter 抬进 common，会污染 `commonMain` 并和现有 `PlaybackCoordinator` 形成双真相源，因此明确禁止。第二，若为了统一而强行共享实现代码，会把 iOS Kotlin/Native 与 macOS JVM bridge 的线程、观察器和生命周期差异揉在一起，首版不承担这个复杂度。第三，若 macOS bridge 没有独立测试边界，最容易出事故的 native 回调乱序和 release 时序无法稳定验证，因此必须保留 fake bridge 测试缝。第四，若完整继承 vlcj 测试细节，会把第三方库形状带入 AVFoundation 迁移，因此只继承行为防线。
