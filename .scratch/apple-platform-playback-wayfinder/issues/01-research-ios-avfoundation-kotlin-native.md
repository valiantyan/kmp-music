Status: closed
Labels: wayfinder:research
Parent map: .scratch/apple-platform-playback-wayfinder/map.md
Assignee: codex
Blocked by: 无
Blocks: 确定苹果平台播放 adapter 边界；确定本地音频来源和权限生命周期；确定苹果系统播放能力范围

# 调研 iOS Kotlin/Native 接入 AVFoundation 的边界

## Question

基于官方 Apple 文档、Kotlin Multiplatform / Kotlin/Native 官方文档和当前项目代码，iOS 端用 AVFoundation / AVPlayer 系列实现 `AudioPlayerEngine` 需要满足哪些边界和约束？

调研需要回答：

- `iosMain` 能否直接调用 AVFoundation 相关 Objective-C / Swift API，是否需要额外 bridge。
- AVPlayer、AVQueuePlayer 或其他 AVFoundation 类型更适合当前 `setQueue(List<PlayableMedia>)`、播放、暂停、seek、切歌、音量和事件回流模型。
- 本地文件 URL、iOS 导入曲库、iOS 系统音乐资料库条目在播放 URL、权限、DRM 或云端条目上的约束。
- 进度、时长、自然结束、失败、打断和后台播放相关事件如何映射到 `PlaybackEngineEvent`。
- 需要避开的线程、生命周期和 Kotlin/Native 内存模型风险。

期望输出保存到 `.scratch/apple-platform-playback-wayfinder/research/ios-avfoundation-kotlin-native.md`，所有事实必须引用第一手来源。

## Comments

- 2026-07-14：研究输出已保存到 `.scratch/apple-platform-playback-wayfinder/research/ios-avfoundation-kotlin-native.md`。摘要：`iosMain` 可直接调用 AVFoundation / AVPlayer 系列；首版更建议单 `AVPlayer` + Kotlin 侧队列状态，不让 `AVQueuePlayer` 接管 common 队列；当前 iOS 文件夹扫描释放 security scope 后再播放存在访问生命周期风险，媒体库来源还需处理授权、`assetURL` 可空、云端和受保护条目。
- 2026-07-14：已关闭此 research ticket，并在地图 `Decisions so far` 添加事实索引。
