# 播放抽象层审查与轻量优化设计

## 背景

KMP Music 的播放架构前提是：`commonMain` 定义平台无关播放抽象和播放业务语义，Android、iOS、macOS、Windows 各自用原生或平台适合的音频能力实现抽象。这个前提不能改变。

当前 Android 已经接入 Media3，macOS Desktop 已经接入 vlcj / LibVLC。当前不确定点不是“应该选哪个播放器库”，而是现有 Android 与 macOS 代码是否已经严格遵循了 `commonMain` 抽象、平台层实现的方向，还是各自形成了平行播放链路。

本设计聚焦审查和轻量优化已实现的 Android 与 macOS 播放链路。iOS、Windows 和网络音频只保留接口演进路线，不进入本轮真实实现。

## 第一性原理

KMP 的根本价值不是把所有平台强行写成同一套底层代码，而是最大化复用平台无关逻辑，同时保留平台原生能力。音乐播放器里，队列、播放模式、状态流转、错误归一化、快照恢复和 UI 派生状态属于产品语义；解码、媒体会话、后台播放、文件授权和原生库加载属于平台事实。

因此正确的分层是：

```text
MusicAppController
  -> PlaybackCoordinator
  -> AudioPlayerEngine
  -> platform player adapter
  -> Media3 / vlcj / AVPlayer / desktop runtime
```

为什么这样做：播放体验的一致性来自 common 层业务语义，而不是来自底层播放器库一致。Android 的 Media3、iOS 的 AVPlayer、Desktop 的 vlcj 都有各自平台优势。common 层只应定义命令、状态、事件和队列语义，不能污染平台类型。

## 已确认结论

- 当前项目已经基本完成“common 抽象 + 平台实现”的播放主干。
- `AudioPlayerEngine` 是 `commonMain` 的正式播放抽象。
- `PlaybackCoordinator` 是 `commonMain` 的播放业务协调器，负责队列、播放模式、状态回写、失败恢复和快照恢复。
- Android 的 `PlaybackServiceConnector : AudioPlayerEngine` 是 Media3 播放链路进入 common 的平台 adapter。
- macOS Desktop 的 `DesktopVlcjAudioPlayerEngine : AudioPlayerEngine` 是 vlcj 播放链路进入 common 的平台 adapter。
- 本轮不推翻现有链路，只做契约收紧、边界澄清和播放来源语义补强。
- 本轮继续手动依赖注入，不引入 Koin 或其他第三方 DI 框架。

## 范围

### 本轮包含

- 审查并确认 Android 与 macOS 是否遵循播放抽象边界。
- 正式定义 `AudioPlayerEngine`、`PlaybackCoordinator`、平台 engine 和平台播放器的职责。
- 轻量新增 `AudioSource.Local` 概念，明确 phase 1 只承诺本地可播放来源，并记录未来网络播放来源需要另开设计。
- 明确 `LocalMusicScanner` 是扫描上游，不属于播放器层。
- 明确 Android 与 macOS 的必要优化点。
- 为 iOS 和 Windows 记录未来接入点，但不实现真实播放。
- 为未来网络音频记录 source 与事件模型演进方向，但不实现网络播放。

### 本轮不包含

- 不把 `AudioPlayerEngine` 大改成全新的 `AudioPlayer.prepare(source)`。
- 不重写 Android Media3 主链路。
- 不重写 macOS vlcj 主链路。
- 不实现 iOS AVPlayer 播放。
- 不实现 Windows vlcj 或 WinRT 播放。
- 不实现网络音频播放、缓存、鉴权刷新、重试和缓冲进度。
- 不实现音频焦点、电话/闹钟中断、输出设备切换、AirPlay 或蓝牙设备切换抽象。
- 不引入 Koin。

## 模块职责

### MusicAppController

`MusicAppController` 是 common UI 操作入口，接收点歌、播放、暂停、seek、切歌、切换播放模式等操作。

它不得直接依赖 Media3、vlcj、AVPlayer、WinRT 或平台 service。

为什么这样做：Controller 属于 shared UI 状态入口。如果 Controller 直接接触平台播放器，iOS 和 Windows 接入时会复制平台判断，common 层会失去统一播放语义。

### PlaybackCoordinator

`PlaybackCoordinator` 是 common 播放业务协调器，负责：

- 根据用户点击歌曲和当前列表生成播放队列。
- 维护 `currentIndex`、播放模式、随机历史和失败计数。
- 调用 `AudioPlayerEngine` 执行真实播放命令。
- 订阅 `PlaybackEngineEvent`，把真实播放事实回写到 `PlaybackRepository`。
- 处理自然结束、失败跳过、单曲循环失败阈值和播放快照。

为什么这样做：这些都是产品语义，而不是平台能力。放在 common 层才能保证 Android、macOS、未来 iOS 和 Windows 的行为一致。

### AudioPlayerEngine

`AudioPlayerEngine` 是 common 层正式播放抽象，表达平台无关播放命令与事件：

```kotlin
interface AudioPlayerEngine {
    val events: Flow<PlaybackEngineEvent>

    suspend fun setQueue(
        items: List<PlayableMedia>,
        startIndex: Int,
        startPositionMs: Long = 0L,
    )

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun skipToIndex(index: Int)
    fun setPlaybackMode(playbackMode: PlaybackMode)
    fun setVolume(volume: Float)
    fun stop()
}
```

为什么保留这个接口：当前 App 已经围绕队列、MediaSession metadata、播放模式、快照恢复和系统控制建立了 `setQueue(List<PlayableMedia>)` 主链路。把它强行改成单曲 `prepare(AudioSource)` 会牵动通知、队列、快照和事件回流，收益不抵风险。

### 平台 engine

平台 engine 只负责把 common 命令转成平台播放器调用，并把平台播放器事实转回 `PlaybackEngineEvent`。

- Android：`PlaybackServiceConnector` 连接 Media3 `MediaController` 和 `MusicPlaybackService`。
- macOS/Desktop：`DesktopVlcjAudioPlayerEngine` 包装 `DesktopMediaPlayerAdapter` 和 vlcj。
- 未来 iOS：`IosAvAudioPlayerEngine` 包装 AVPlayer / AVQueuePlayer。
- 未来 Windows：JVM Desktop 路线优先复用 `DesktopVlcjAudioPlayerEngine`，只替换 LibVLC runtime resolver。

平台 engine 不拥有业务队列规则，不直接写 UI state，不把平台类型暴露给 common。

## 播放数据流

```text
用户点击歌曲
  -> MusicAppController.playSong(...)
  -> PlaybackCoordinator.playSong(...)
  -> 保存 common 队列与 Loading 状态
  -> AudioPlayerEngine.setQueue(...)
  -> AudioPlayerEngine.play()
  -> 平台播放器真实播放
  -> PlaybackEngineEvent.StatusChanged / ProgressChanged / Failed / Ended
  -> PlaybackCoordinator 回写 PlaybackRepository
  -> MusicAppUiState 更新
  -> UI 刷新
```

状态真相必须从平台播放器事件回流 common。UI 可以在用户操作后显示目标歌曲或 Loading，但 `Playing`、`Paused`、`Ended`、`Error` 和真实进度必须由 engine event 校正。

为什么这样做：如果 UI、平台 service 和 common repository 都能直接写播放状态，就会出现多个状态真相源。播放架构里最需要锁住的是事件回流方向，而不是某一个平台 API 调用。

## Android 现状评估

当前 Android 符合播放抽象主原则：

```text
PlaybackCoordinator
  -> AudioPlayerEngine
  -> PlaybackServiceConnector
  -> MediaController
  -> MusicPlaybackService
  -> ExoPlayer / MediaSession
```

关键判断：

- `PlaybackServiceConnector` 实现 `AudioPlayerEngine`，是 Android 播放 adapter。
- `MusicPlaybackService`、`ExoPlayer`、`MediaSession` 都留在 `androidMain`。
- `MediaControllerEventBridge` 把 Media3 状态、进度、错误转换成 `PlaybackEngineEvent`。
- common 层不依赖 Media3 类型。

Android 优化点：

- 不重构主链路。
- 文档和 KDoc 中明确 Android service 是平台执行层，不是业务播放状态真相。
- 保持状态由 `PlaybackEngineEvent` 回流到 `PlaybackCoordinator`，不能让 UI 或 service 直接写 common 播放状态。
- 明确 Android 的本地播放来源可能是 MediaStore `content://`，不能把播放来源抽象成普通文件 path。

为什么这样做：Android 的真实播放必须通过 Media3 service/session 承接后台播放和系统媒体协议，但业务状态仍应归 common coordinator 管。

## macOS Desktop 现状评估

当前 macOS Desktop 也符合播放抽象主原则：

```text
PlaybackCoordinator
  -> AudioPlayerEngine
  -> DesktopVlcjAudioPlayerEngine
  -> DesktopMediaPlayerAdapter
  -> VlcjMediaPlayerAdapter
  -> LibVLC
```

关键判断：

- `DesktopVlcjAudioPlayerEngine` 实现 `AudioPlayerEngine`，是 Desktop 播放 adapter。
- vlcj、JNA、LibVLC runtime、插件路径都留在 `desktopMain`。
- `DesktopMediaPlayerAdapter` 是 Desktop 内部 adapter，不是 common 抽象。
- 自然结束后的下一首、单曲循环、随机和失败跳过仍由 `PlaybackCoordinator` 决定。

macOS/Desktop 优化点：

- 不重写 vlcj 播放引擎。
- `DesktopVlcjAudioPlayerEngine` 名字保留，继续代表 JVM Desktop 播放引擎。
- 未来 Windows 接入前，把 `MacosLibVlcRuntime` 的职责收敛为 macOS runtime resolver，或引入 `DesktopLibVlcRuntimeResolver` 抽象。
- 文档明确当前 macOS 实现是 Compose Desktop JVM 路线，不是 macOS 原生 AVFoundation 路线。

为什么这样做：当前 macOS 已经抽象在 `desktopMain`，不应为了平台名重写。真正要防的是 Windows 接入时复制出第二套 Desktop 播放架构。

## AudioSource 契约

当前 `PlayableMedia.localUri` 已经承担播放来源角色：

```kotlin
data class PlayableMedia(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long?,
    val localUri: String,
    val coverArt: CoverArt,
    val mimeType: String?,
)
```

问题是 `localUri` 语义偏窄：Android 可能是 `content://`，Desktop 可能是 `file://` 或文件路径，iOS 未来可能是 sandbox file URL，网络音频未来会是 `https://` 且可能需要 headers。

本轮建议轻量新增 `AudioSource` 概念，但 phase 1 代码只承诺本地可播放来源：

```kotlin
sealed interface AudioSource {
    val uri: String

    data class Local(
        override val uri: String,
    ) : AudioSource
}
```

`Remote` 不在本轮代码中落地。网络播放需要 buffering、网络错误、鉴权、缓存和 URL 过期策略；在这些运行时契约明确前，把 `Remote` 放进生产模型会让接口看起来已经支持网络播放，但实际事件和错误模型无法履约。

为什么不合并成一个裸 `Url`：本地资源和网络资源表面上都是 URI 字符串，但运行时契约不同。本地主要失败在权限、文件缺失、格式不支持；网络还会涉及断网、超时、HTTP 错误、鉴权、URL 过期、缓冲、range seek 和缓存。如果只用一个字符串，平台实现就要靠 `startsWith("http")` 猜语义，这是把领域事实藏进字符串格式。

### 迁移策略

本轮只做轻量迁移：新增 `AudioSource` 类型和 `PlayableMedia.audioSource` 派生属性，不替换现有 `localUri` 主字段。

```kotlin
data class PlayableMedia(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long?,
    val localUri: String,
    val coverArt: CoverArt,
    val mimeType: String?,
) {
    val audioSource: AudioSource
        get() = AudioSource.Local(uri = localUri)
}
```

后续只有当出现非本地来源时，例如网络音频、缓存后的 source 切换、签名 URL 刷新，才评估把主字段从 `localUri` 改成 `audioSource`：

```kotlin
data class PlayableMedia(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long?,
    val audioSource: AudioSource,
    val coverArt: CoverArt,
    val mimeType: String?,
)
```

为什么分两步：当前 Android 通知 metadata、Desktop vlcj 转换、队列快照和现有测试都围绕 `PlayableMedia` 工作。先增加 `audioSource` 派生语义，可以立住边界而不破坏主链路。

## LocalMusicScanner 边界

`LocalMusicScanner` 是扫描上游，不属于播放器层。

```text
LocalMusicScanner
  -> Song(localUri / metadata)
  -> MusicLibraryRepository
  -> PlaybackCoordinator
  -> PlayableMedia / AudioSource
  -> AudioPlayerEngine
```

职责区分：

- `LocalMusicScanner`：发现有哪些本地歌曲，返回 `Song` 和平台来源信息。
- `PlayableMedia` / `AudioSource`：描述可交给播放器的媒体项和播放来源。
- `AudioPlayerEngine`：执行播放命令并发出播放事实事件。
- `PlaybackCoordinator`：把扫描结果、队列、用户操作和播放器事件组合成产品播放语义。

为什么这样做：扫描和播放都接触“文件/URI”，但它们不是同一层能力。扫描回答“我有哪些歌”，播放回答“给我这个来源怎么播放”。把 scanner 纳入播放器抽象会让权限、目录选择和播放状态混在一起。

## 依赖注入

本轮继续手动 DI，不引入 Koin。

当前 composition root 已经清晰：

```text
AndroidPlaybackSession.bootstrap(...)
  -> PlaybackServiceConnector
  -> Android scanner / Room / repositories
  -> MusicAppController

DesktopPlaybackSession
  -> DesktopVlcjAudioPlayerEngine
  -> Desktop scanner / Room / repositories
  -> MusicAppController

未来 IosEntry
  -> IosAvAudioPlayerEngine
  -> iOS scanner / repositories
  -> MusicAppController
```

为什么不引入第三方 DI：当前依赖图还可读，对象创建点也集中。播放抽象优化的根因是职责边界，不是对象创建框架。引入 Koin 会增加 KMP 配置、生命周期和调试成本，但不能直接提升播放边界质量。

## iOS 和 Windows 预留路线

### iOS

未来 iOS 接入点是：

```text
IosEntry
  -> IosAvAudioPlayerEngine : AudioPlayerEngine
  -> AVPlayer / AVQueuePlayer
```

设计原则：

- AVPlayer / AVQueuePlayer 只出现在 `iosMain`。
- AVAudioSession、MPNowPlayingInfoCenter、Remote Command Center 都留在 iOS 平台层。
- iOS 播放事件必须映射成 `PlaybackEngineEvent` 回流 common。
- common 不依赖 AVFoundation 类型。

本轮不实现 iOS 播放。

### Windows

当前 Desktop 假设仍是 Compose Multiplatform Desktop JVM。未来 Windows 优先复用 Desktop vlcj engine：

```text
DesktopPlaybackSession
  -> DesktopVlcjAudioPlayerEngine
  -> WindowsLibVlcRuntimeResolver
  -> vlcj / LibVLC
```

设计原则：

- `DesktopVlcjAudioPlayerEngine` 继续复用。
- Windows 差异封装在 LibVLC runtime resolver 和打包层。
- 不在 common 或 UI 层新增 Windows 专用播放链路。
- 若未来 Desktop 转 Kotlin/Native，再单独评估 WinRT `Windows.Media.Playback.MediaPlayer` adapter。

本轮不实现 Windows 播放。

## 网络音频演进

未来网络音频不是“另一个本地 path”，而是一个新的运行时契约。网络播放设计可以引入远程播放来源类型，例如带 headers 的远程 URI，但该类型不属于本轮生产代码。

网络播放实施时需要另开设计，至少补充：

- buffering 状态和缓冲进度事件。
- 网络错误分类，如 timeout、HTTP error、unauthorized、server unavailable。
- URL 过期与鉴权刷新策略。
- headers、cookie 或签名参数处理边界。
- 缓存策略。
- 断点续播与 seek 对服务端 range 支持的要求。
- 网络到本地缓存的 source 切换规则。

为什么不现在实现：网络播放的不确定性来自外部 I/O、鉴权和缓冲策略，过早把这些状态塞进当前本地播放 phase 会扩大 Android/macOS 已稳定链路的改动范围。

## 测试策略

### common 测试

继续以 fake `AudioPlayerEngine` 测 `PlaybackCoordinator`：

- 点歌时写入队列并调用 engine。
- `StatusChanged` 回写播放状态。
- `ProgressChanged` 更新进度。
- `Ended` 按播放模式推进。
- `Failed` 进入错误和失败跳过逻辑。
- `AudioSource.Local` 不把 Android `content://` 误判成文件 path。

### Android 测试

- `MediaControllerEventBridge` 的 Media3 状态映射保持正确。
- Media3 错误映射到 `PlaybackErrorType`。
- service/controller 层只发 engine event，不直接写 UI state。

### Desktop 测试

- `DesktopVlcjAudioPlayerEngine` 继续通过 fake `DesktopMediaPlayerAdapter` 覆盖命令串行。
- generation token 能丢弃过期事件。
- seek 使用 latest-wins。
- `finished` 只发 `PlaybackEngineEvent.Ended`，不自行推进下一首。
- release 后 delayed callback 不更新 common 状态。

为什么这样测试：播放架构中最重要的稳定性来自状态真相源和事件顺序。测试应覆盖事件回流和 common 状态更新，而不是只断言底层 `play()` 被调用。

## 实施建议

本设计后续实施计划应按小步推进：

1. 在 common 模型中新增 `AudioSource`，先通过 `PlayableMedia.audioSource` 派生 `AudioSource.Local(localUri)`。
2. 补充 `AudioPlayerEngine`、`PlayableMedia`、`LocalMusicScanner` 的 KDoc，明确职责边界。
3. 审查 Android 播放链路，确认没有 UI 或 service 绕过 `PlaybackCoordinator` 写 common 播放状态。
4. 审查 Desktop 播放链路，确认 vlcj 事件只经 `DesktopVlcjAudioPlayerEngine` 回流 `PlaybackEngineEvent`。
5. 记录后续 Windows runtime resolver 泛化任务，但不在本轮实现。
6. 记录后续 iOS AVPlayer adapter 任务，但不在本轮实现。

## 验收标准

- 设计文档明确当前 Android 与 macOS 已基本符合播放抽象原则。
- `AudioPlayerEngine` 被确认为 common 播放抽象，平台 engine 只作为实现。
- `PlaybackCoordinator` 被确认为播放业务语义唯一协调器。
- `LocalMusicScanner` 被明确排除在播放器层之外。
- `AudioSource.Local` 能表达 scanner 已确认可访问的本地播放来源，不把 `content://`、`file://` 或平台文件 URI 误收窄成普通 filesystem path。
- 本轮不要求 Android/macOS 主链路重写。
- 本轮不实现 iOS、Windows 或网络播放。

## 风险与取舍

| 风险 | 处理 |
| --- | --- |
| 只写文档不改代码，边界仍可能被后续实现误用 | 后续实施计划必须至少补 KDoc 和轻量 `AudioSource`，让边界出现在代码入口。 |
| 过早引入远程播放来源让人误以为已支持网络播放 | 本轮生产模型只加入 `AudioSource.Local`，网络播放需要单独设计 buffering、错误、鉴权和缓存。 |
| 大改 `AudioPlayerEngine` 造成 Android/macOS 回归 | 本轮不改主接口，只轻量补 source 语义。 |
| Windows 复用 Desktop vlcj 时 macOS runtime 命名误导 | 未来 Windows 前先泛化 runtime resolver，而不是复制 engine。 |
| DI 框架缺失导致后续依赖变复杂 | 当前继续手动 DI；等依赖图真实膨胀后再单独评估 Koin。 |

## 最终判断

Android 与 macOS 当前已经不是各自独立播放代码、缺少统一抽象的状态。项目已经有正确主干：`MusicAppController -> PlaybackCoordinator -> AudioPlayerEngine -> 平台实现`。

本轮真正需要优化的是把这个主干正式化，并补清楚播放来源、扫描上游、平台 adapter 和未来网络音频的边界。这样既能根治“是否已经遵循 KMP 播放抽象”的不确定性，又不会为了形式上的新接口破坏已经工作的 Android 和 macOS 播放链路。
