# iOS Kotlin/Native 接入 AVFoundation 的播放边界调研

调研时间：2026-07-14。

## 结论摘要

iOS 端可以在 `iosMain` 直接通过 Kotlin/Native 调用 AVFoundation、AVFAudio、MediaPlayer、Foundation 等 Objective-C 系统框架；AVFoundation / AVPlayer 系列不需要额外 Swift 桥接层才能从 Kotlin 使用。需要桥接层的主要场景不是“能不能调用”，而是规避 Kotlin/Native 对 Objective-C 符号的强链接风险、封装复杂 KVO / 观察器生命周期，或把纯 Swift API 先导出成 Objective-C API。

当前 `AudioPlayerEngine` 契约下，iOS 首版更适合用单个 `AVPlayer` 加 Kotlin 侧队列状态实现，而不是把业务队列交给 `AVQueuePlayer` 自动推进。原因是本项目的下一首、单曲循环、随机、失败跳过和自然结束处理已经由 `PlaybackCoordinator` 统一负责；`AVQueuePlayer` 会管理并消费自己的队列，容易和 common 层队列语义重叠。`AVQueuePlayer` 更适合作为后续“无缝播放 / 预加载下一首”优化，而不是首版适配器的默认形态。

当前 iOS 文件夹扫描器生成 `file://` 形式的 `localUri`，但只在扫描期间调用 `startAccessingSecurityScopedResource()`，扫描结束后马上释放授权。若播放文件仍在 App 沙盒外，后续 `AVPlayerItem` 很可能拿不到稳定访问权。实现播放前必须先决定 iOS 导入文件的生命周期：复制进 App 私有目录，或让播放器持有可用的 security-scoped 访问窗口；不能只把 `localUri` 字符串交给引擎就结束。

如果后续接入 `IosMediaLibrary`，必须单独处理 `MPMediaLibrary` 授权、`NSAppleMusicUsageDescription` 隐私说明、`MPMediaItem.assetURL` 可空、云端条目和受保护条目。只有拿到可交给 AVFoundation 的 URL 的条目，才能进入 `PlayableMedia` 队列。

## 关键事实与引用

### Kotlin/Native 接入边界

- Kotlin/Native 官方文档说明，Objective-C 框架和库可以从 Kotlin 使用；系统框架默认导入。Swift 库只有在 API 通过 `@objc` 导出到 Objective-C 时才能从 Kotlin 使用，纯 Swift 模块目前不支持。来源：[Kotlin Swift/Objective-C interop](https://kotlinlang.org/docs/native-objc-interop.html)。
- Kotlin/Native 平台库默认可用，编译器会自动检测并链接被访问的平台库；Apple 平台包含用于 Objective-C 互操作的 `objc` 库。来源：[Kotlin Platform libraries](https://kotlinlang.org/docs/native-platform-libs.html)。
- Kotlin/Native 对 Kotlin 源码中直接使用的 Objective-C class 会形成强链接；如果符号在某个设备或系统版本不可用，App 启动时就可能崩溃。新系统 API 或不确定可用性的类，应通过 Swift / Objective-C wrapper 做可用性检查。来源：[Kotlin Swift/Objective-C interop - Strong linking](https://kotlinlang.org/docs/native-objc-interop.html#strong-linking)。
- 当前项目已经有 Kotlin/Native 直接调用 UIKit / Foundation 的 iOS 代码，例如 `IosFolderPicker` 继承 `NSObject` 并实现 `UIDocumentPickerDelegateProtocol`，`IosFolderMusicScanner` 使用 `NSURL`、`NSFileManager` 和 security-scoped resource。来源：`composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/data/IosFolderPicker.kt`、`composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/data/IosFolderMusicScanner.kt`。
- 当前 Gradle 配置已有 `iosArm64()` 和 `iosSimulatorArm64()`，并把共享代码编译为 static framework；`iosMain` 当前只依赖 `ktor-client-darwin`，没有播放器实现依赖。来源：`composeApp/build.gradle.kts`。

### AVPlayer 与 AVQueuePlayer

- Apple SDK 中 `AVPlayer` 可用 URL 或 `AVPlayerItem` 播放单个视听资源；`replaceCurrentItemWithPlayerItem` 可替换当前 item。来源：[AVPlayer](https://developer.apple.com/documentation/avfoundation/avplayer)，以及本机 Xcode SDK `AVFoundation.framework/Headers/AVPlayer.h`。
- `AVQueuePlayer` 是 `AVPlayer` 子类，提供多 item 队列管理；`advanceToNextItem()` 会结束当前 item 并从队列移除当前 item，`removeAllItems()` 会停止播放。Apple 头文件还提示，入队 item 会具备加载资格，可能带来 I/O 和处理开销，且同一个 `AVPlayerItem` 不能重复入队。来源：[AVQueuePlayer](https://developer.apple.com/documentation/avfoundation/avqueueplayer)，以及本机 Xcode SDK `AVPlayer.h`。
- 对本项目而言，`AVQueuePlayer` 的自动队列消费不应成为首版默认实现。`PlaybackCoordinator` 已经在 common 层处理 `Ended` 后的 loop / shuffle / skip 规则；iOS engine 应只回报 `PlaybackEngineEvent.Ended`，让 common 层决定下一步。来源：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt`。

### 本地 URL、文件授权和媒体库

- 当前 `PlayableMedia.audioSource` 只承诺本地可播放 URI，`AudioSource.Local` 保存 scanner 提供的 URI；网络来源尚未进入生产模型。来源：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt`。
- 当前 iOS scanner 使用 `UIDocumentPickerViewController` 选择文件夹，扫描时调用 `folderUrl.startAccessingSecurityScopedResource()`，并在 `finally` 中调用 `stopAccessingSecurityScopedResource()`；它把每个文件转成 `NSURL.fileURLWithPath(...).absoluteString` 写入 `localUri`。来源：`IosFolderPicker.kt`、`IosFolderMusicScanner.kt`。
- Apple `NSURL` 头文件说明，成功调用 `startAccessingSecurityScopedResource()` 后，必须在不再需要资源时用 `stopAccessingSecurityScopedResource()` 配对释放；调用是引用计数语义。来源：[NSURL](https://developer.apple.com/documentation/foundation/nsurl)，以及本机 Xcode SDK `Foundation.framework/Headers/NSURL.h`。
- `MPMediaLibrary.requestAuthorization(_:)` 会展示系统授权 UI，让用户决定 App 是否可查看媒体库内容。来源：[MPMediaLibrary.requestAuthorization(_:)](https://developer.apple.com/documentation/mediaplayer/mpmedialibrary/requestauthorization(_:))。
- `MPMediaItem.assetURL` 类型是 `URL?`，文档说明它可用于创建 `AVAsset` 或其他基于 URL 的 AVFoundation 对象。来源：[MPMediaItem.assetURL](https://developer.apple.com/documentation/mediaplayer/mpmediaitem/asseturl)。
- Apple SDK 中 `MPMediaItem` 还暴露 `cloudItem` 和 `protectedAsset` 只读属性。后续媒体库扫描应把这些字段纳入可播放性过滤或错误映射，不能假设所有系统音乐资料库条目都有可播放 URL。来源：本机 Xcode SDK `MediaPlayer.framework/Headers/MPMediaItem.h`。

### 状态、进度、结束和失败事件

- `AVPlayer.timeControlStatus` 可 KVO，表达暂停、等待播放条件或播放中；等待状态可映射到 `PlaybackStatus.Buffering`，播放中映射到 `PlaybackStatus.Playing`，暂停映射到 `PlaybackStatus.Paused`。来源：[AVPlayer](https://developer.apple.com/documentation/avfoundation/avplayer)，本机 Xcode SDK `AVPlayer.h`。
- `AVPlayerItem.status` 可 KVO；`AVPlayerItemStatusFailed` 表示 item 已不能继续播放，需要新建实例，并可从 `error` 读取失败原因。`duration` 可观察，加载前可能是 indefinite，直播等场景也可能保持 indefinite；映射到 common 层时应允许 `durationMs = null`。来源：[AVPlayerItem](https://developer.apple.com/documentation/avfoundation/avplayeritem)，本机 Xcode SDK `AVPlayerItem.h`。
- `AVPlayerItemDidPlayToEndTimeNotification` 表示当前 item 播到结束；`AVPlayerItemFailedToPlayToEndTimeNotification` 表示播到结束前失败；`AVPlayerItemPlaybackStalledNotification` 表示媒体未及时到达导致播放无法继续。来源：[AVPlayerItem](https://developer.apple.com/documentation/avfoundation/avplayeritem)，本机 Xcode SDK `AVPlayerItem.h`。
- `addPeriodicTimeObserver` 可用于周期性上报进度；返回的观察器必须保留，并与 `removeTimeObserver` 成对调用。Apple 头文件明确要求传入串行队列，传 `NULL` 时使用主队列；并发队列会导致未定义行为。来源：[AVPlayer.addPeriodicTimeObserver](https://developer.apple.com/documentation/avfoundation/avplayer/addperiodictimeobserver(forinterval:queue:using:))，本机 Xcode SDK `AVPlayer.h`。
- `AVPlayer.volume` 是 0.0 到 1.0 的相对音量；Apple 在 iOS 上提示不要用它实现面向用户的系统媒体音量滑杆，用户音量滑杆应使用 `MPVolumeView`。本项目 `AudioPlayerEngine.setVolume(Float)` 是 App 内归一化音量，首版可以映射到 `AVPlayer.volume`，但不能把它包装成系统音量控制。来源：[AVPlayer.volume](https://developer.apple.com/documentation/avfoundation/avplayer/volume)，本机 Xcode SDK `AVPlayer.h`。

### 音频会话、中断和后台播放

- Apple 媒体播放配置文档说明，播放类 App 通常需要配置 `AVAudioSession` category；`playback` category 表示媒体播放是核心功能，且配合 Audio / AirPlay / Picture in Picture 后台模式才能后台播放。来源：[Configuring your app for media playback](https://developer.apple.com/documentation/avfoundation/media_playback/configuring_your_app_for_media_playback)。
- Apple 文档建议在设置 category 后再激活 audio session，并建议推迟到真正开始播放时激活，避免过早打断其他后台音频。来源：[Configuring your app for media playback](https://developer.apple.com/documentation/avfoundation/media_playback/configuring_your_app_for_media_playback)。
- `UIBackgroundModes` 是声明 App 需要后台运行服务的 Info.plist key；Apple 文档建议通过 Xcode Background Modes capability 添加。来源：[UIBackgroundModes](https://developer.apple.com/documentation/bundleresources/information_property_list/uibackgroundmodes)。
- `AVAudioSessionInterruptionNotification`、`AVAudioSessionRouteChangeNotification`、`AVAudioSessionInterruptionTypeKey` 和 `AVAudioSessionInterruptionOptionShouldResume` 在 Apple SDK 中可用。iOS adapter 至少要监听中断和 route change，并把中断开始、恢复建议、输出设备断开等事实归一化为暂停、缓冲或错误策略。来源：[AVAudioSession](https://developer.apple.com/documentation/avfaudio/avaudiosession)，本机 Xcode SDK `AVFAudio.framework/Headers/AVAudioSessionTypes.h`。

### Kotlin/Native 生命周期和线程

- Kotlin/Native 现代内存管理器使用共享堆，对象可从任意线程访问；GC 周期性进行追踪回收。来源：[Kotlin/Native memory management](https://kotlinlang.org/docs/native-memory-manager.html)。
- Kotlin 与 Objective-C 的内存管理模型不同：Kotlin 使用追踪式 GC，Objective-C 使用 ARC。跨边界对象通常无需额外工作，但 Objective-C 对象可能比预期活得更久；引用循环中只要包含 Objective-C 对象，整张对象图就不能单靠 Kotlin 侧打破。来源：[Kotlin Integration with Swift/Objective-C ARC](https://kotlinlang.org/docs/native-arc-integration.html)。
- 对本项目实现的直接含义是：`AVPlayer`、`AVPlayerItem`、NSNotification 观察器、KVO 观察器、周期进度观察器令牌、delegate / block 包装对象都要由 iOS 引擎明确持有，并在 item 切换、stop、release 或 controller 销毁时移除，不能依赖 GC “最终会收”。

## 对当前 AudioPlayerEngine 的影响

### 推荐适配器形态

首版建议新增 `IosAvAudioPlayerEngine : AudioPlayerEngine`，内部持有：

- `List<PlayableMedia>` 队列和 `currentIndex`。
- 一个 `AVPlayer`。
- 当前 `AVPlayerItem` 与当前代际标记。
- 进度观察器令牌。
- item 结束、失败、卡顿通知观察器。
- KVO 或等价观察器，用于 `AVPlayerItem.status`、`AVPlayerItem.duration`、`AVPlayer.timeControlStatus`。
- 一个串行命令队列或等价协程执行单元，复用桌面引擎的“公开命令和 native 回调线性化”思路。

`setQueue(items, startIndex, startPositionMs)` 不应把所有 item 交给 `AVQueuePlayer` 自动播放，而应：

1. 保存完整队列。
2. 校验空队列和 index。
3. 根据当前 `PlayableMedia.audioSource` 创建 `NSURL`、`AVURLAsset` 或 `AVPlayerItem`。
4. 发出 `CurrentMediaChanged` 和 `StatusChanged(Loading)`。
5. 替换 `AVPlayer.currentItem`。
6. 在 item 就绪后应用 `startPositionMs`，再根据待执行的播放 / 暂停意图决定是否调用 `play()`。

### 命令映射

| `AudioPlayerEngine` 命令 | iOS 首版映射 |
| --- | --- |
| `setQueue` | 保存 Kotlin 队列，替换当前 `AVPlayerItem`，设置代际标记，安装观察器，进入 `Loading`。 |
| `play` | 设置待播放意图；在 item 就绪后调用 `AVPlayer.play()`；必要时激活 `AVAudioSession`。 |
| `pause` | 清除待播放意图，调用 `AVPlayer.pause()`，上报 `Paused`。 |
| `seekTo` | 使用 `AVPlayer.seek(to:completionHandler:)` 或带 tolerance 的 seek；seek 完成前要防旧代际回写。 |
| `skipToIndex` | 校验 index，切换 current item，重置进度观察和旧代际，仍让 common 层拥有队列规则。 |
| `setPlaybackMode` | 首版只记录，不交给 `AVPlayer`；loop / shuffle 继续由 `PlaybackCoordinator` 处理。 |
| `setVolume` | 映射到 `AVPlayer.volume`，语义是 App 内相对音量，不是系统音量。 |
| `stop` | 暂停、清空待执行意图、移除观察器，发出 `StatusChanged(Idle, 0, null)`。 |

### 事件映射

| Apple 事实 | common 事件 |
| --- | --- |
| item 已替换并确定 songId / index | `CurrentMediaChanged(songId, index, durationMs)` |
| item 准备中 | `StatusChanged(Loading, positionMs, durationMs)` |
| `timeControlStatus == Playing` | `StatusChanged(Playing, currentPositionMs, durationMs)` |
| `timeControlStatus == WaitingToPlayAtSpecifiedRate` 或 `PlaybackStalled` | `StatusChanged(Buffering, currentPositionMs, durationMs)` |
| `pause()` 或中断开始 | `StatusChanged(Paused, currentPositionMs, durationMs)`，是否自动恢复留给后续系统播放能力决策 |
| 周期进度观察器 | `ProgressChanged(positionMs, durationMs)` |
| `AVPlayerItemDidPlayToEndTimeNotification` | `Ended` |
| item / player status failed，或播放结束前失败通知 | `Failed(PlaybackError(...))` |

### 生命周期边界

当前 `MainViewController()` 在 Compose 内容中用 `rememberCoroutineScope()` 创建 `MusicAppController`，没有 iOS 进程级播放会话。若后续目标包含后台播放、锁屏后继续播放、远程命令或稳定恢复，iOS 应新增类似 `AndroidPlaybackSession` / `DesktopPlaybackSession` 的平台会话，持有长生命周期 controller、engine、audio session 配置和退出收口逻辑。否则播放引擎可能随 UI composition 或 view controller 生命周期被释放。

## 未知风险

- `IosFolderMusicScanner` 当前扫描结束即释放 security scope。真实设备上，扫描后再播放 App 沙盒外文件是否稳定可访问，需要用用户选择的外部目录做实机验证。
- `MPMediaItem.cloudItem` 和 `protectedAsset` 的产品语义尚未决定：是完全过滤、显示不可播放问题，还是尝试走系统播放器能力。这属于后续“本地音频来源和权限生命周期”决策。
- iOS target 的最低部署版本和宿主 Xcode project / Info.plist 未在当前 repo 中找到；强链接风险、后台能力、`NSAppleMusicUsageDescription`、`UIBackgroundModes` 需要等 iOS 宿主工程确认后落门禁。
- Kotlin/Native 下 KVO、NSNotification block、`CMTime` 转换和 dispatch queue 的具体 API 写法需要小型编译试验验证；本研究只确定边界，不保证每个符号名已经是最终 Kotlin 名称。
- 是否接入 `MPNowPlayingInfoCenter`、`MPRemoteCommandCenter`、AirPlay 和后台播放控制，不应混进首个 AVPlayer 适配器；这些属于系统播放能力范围决策。

## 建议下一步

1. 在“确定苹果平台播放 adapter 边界”ticket 中决策：首版采用单 `AVPlayer` + Kotlin 队列状态，并明确不让 `AVQueuePlayer` 接管 common 队列规则。
2. 在“确定本地音频来源和权限生命周期”ticket 中优先解决 iOS 导入文件：复制进 App 私有目录，还是由播放会话持有 security-scoped access。
3. 在“确定苹果系统播放能力范围”ticket 中单独决定 `AVAudioSession`、后台播放、Now Playing、远程命令和 AirPlay 的首版范围。
4. 后续实现前先做最小编译试验：`AVPlayerItem` 创建、time observer 安装/移除、item 结束通知、失败通知和 `CMTime` 毫秒转换。
5. 若进入实现，补 `IosPlaybackSession` 生命周期设计，避免真实播放器被 Compose UI 生命周期误回收。

## 来源索引

- Kotlin 官方：<https://kotlinlang.org/docs/native-objc-interop.html>
- Kotlin 官方：<https://kotlinlang.org/docs/native-platform-libs.html>
- Kotlin 官方：<https://kotlinlang.org/docs/native-memory-manager.html>
- Kotlin 官方：<https://kotlinlang.org/docs/native-arc-integration.html>
- Apple 官方：<https://developer.apple.com/documentation/avfoundation/avplayer>
- Apple 官方：<https://developer.apple.com/documentation/avfoundation/avqueueplayer>
- Apple 官方：<https://developer.apple.com/documentation/avfoundation/avplayeritem>
- Apple 官方：<https://developer.apple.com/documentation/avfoundation/media_playback/configuring_your_app_for_media_playback>
- Apple 官方：<https://developer.apple.com/documentation/bundleresources/information_property_list/uibackgroundmodes>
- Apple 官方：<https://developer.apple.com/documentation/mediaplayer/mpmedialibrary/requestauthorization(_:)>
- Apple 官方：<https://developer.apple.com/documentation/mediaplayer/mpmediaitem/asseturl>
- Apple SDK：`/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS26.2.sdk/System/Library/Frameworks/AVFoundation.framework/Headers/AVPlayer.h`
- Apple SDK：`/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS26.2.sdk/System/Library/Frameworks/AVFoundation.framework/Headers/AVPlayerItem.h`
- Apple SDK：`/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS26.2.sdk/System/Library/Frameworks/AVFAudio.framework/Headers/AVAudioSessionTypes.h`
- Apple SDK：`/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS26.2.sdk/System/Library/Frameworks/MediaPlayer.framework/Headers/MPMediaItem.h`
- Apple SDK：`/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS26.2.sdk/System/Library/Frameworks/MediaPlayer.framework/Headers/MPMediaLibrary.h`
- Apple SDK：`/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS26.2.sdk/System/Library/Frameworks/Foundation.framework/Headers/NSURL.h`
- 当前仓库：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt`
- 当前仓库：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt`
- 当前仓库：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt`
- 当前仓库：`composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/IosEntry.kt`
- 当前仓库：`composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/data/IosFolderMusicScanner.kt`
- 当前仓库：`composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/data/IosFolderPicker.kt`
