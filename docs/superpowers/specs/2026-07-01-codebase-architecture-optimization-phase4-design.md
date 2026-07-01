# 代码架构优化第四阶段设计

## 背景

前三阶段已经把共享状态主干、播放协调器内部规则和 UI 大文件拆分方向定清楚。当前剩余的结构压力集中在平台适配层：

- Android 侧 `AndroidPlaybackSession.kt` 同时承载进程级会话、依赖装配、Activity 可替换依赖代理和冷启动恢复幂等。
- Android `playback` 包里的 `PlaybackMediaCommands.kt` 聚合了 Media3 custom command 定义、按钮构建、按钮状态 Bundle 编解码和命令执行。
- macOS/Desktop 侧 `DesktopPlaybackSession.kt` 已经先行拆出 `createDesktopPlaybackController` 和 `DesktopPlaybackSessionRuntime`，但仍把 controller factory、runtime、libVLC runtime 选择和 audio engine 构建放在同一文件。
- macOS/Desktop 侧 `DesktopVlcjAudioPlayerEngine.kt` 同时承载 command loop、engine state、adapter event reducer、progress polling、setQueue ack 管理、release 收口和 media source mapping。
- iOS 当前只有共享 UI 入口和文件夹扫描能力，还没有真实播放 runtime、持久化会话或后台播放能力。

第四阶段目标是治理平台适配层边界，而不是新增平台能力。平台代码仍然只把系统能力翻译成 common 接口、`PlaybackEngineEvent` 或 `MusicAppController` public methods。

## 第一性原则

本阶段以平台边界清楚为第一成功标准。平台实现可以有各自的生命周期模型，但不能把平台类型或平台规则泄漏进 shared 层。

本阶段遵守这些原则：

- `MusicAppController` 仍是 shared UI 状态入口。
- `PlaybackCoordinator` 仍是播放业务规则入口。
- 平台 playback adapter 只通过 `AudioPlayerEngine`、`PlaybackEngineEvent`、`PlayableMedia` 和 controller public methods 与 common 层通信。
- Android Media3 custom command 只翻译系统媒体命令，不拥有队列、播放模式、收藏或最近播放规则。
- macOS/Desktop vlcj engine 只串行化底层播放器命令和事件，不绕过 `PlaybackCoordinator` 写 repository 或 UI state。
- iOS 后续能力参考 Android 和 macOS/Desktop 的边界原则，但不在本阶段提前抽象统一 `PlatformSession`。
- 拆分按真实变化原因，不按机械行数；允许强相关的小类型留在同一文件。

## 非目标

- 不新增 iOS 真实播放能力。
- 不新增 Android、macOS/Desktop 或 iOS 的产品功能。
- 不改变队列、播放模式、收藏、最近播放、快照恢复或错误恢复规则。
- 不把 Android、Media3、vlcj、AVFoundation、UIKit 类型引入 `commonMain`。
- 不抽统一 `PlatformSession` common 抽象。
- 不重做通知 UI、系统媒体按钮顺序或桌面 vlcj 行为。
- 不修改 UI 视觉或第三阶段 UI 结构。
- 不给 `AudioPlayerEngine` common 契约新增平台生命周期方法。

## 总体架构边界

第四阶段采用按平台能力边界拆分，而不是按文件大小或跨端抽象拆分。

平台入口只负责拿到平台资源并装配 runtime：

```text
Android Activity / Service / Desktop Window / iOS Host
  -> AndroidPlaybackSession / DesktopPlaybackSession facade / iOS host
  -> Platform runtime / dependency factory
  -> MusicAppController
  -> PlaybackCoordinator / repositories / use cases
  -> MusicAppUiState
```

平台播放适配只负责命令和事件翻译：

```text
MusicAppController / PlaybackCoordinator
  -> AudioPlayerEngine
  -> platform command loop / system media API / native player adapter
  -> PlaybackEngineEvent
  -> PlaybackCoordinator
```

执行完成后，平台入口文件不应再同时承担三类职责：依赖装配、生命周期状态机、底层播放命令翻译。

## Android 平台适配设计

Android 侧目标是把进程级会话装配和 Media3 播放适配拆开。Activity、Service、MediaSession 和通知按钮只做平台翻译，不拥有业务规则。

Android custom command 必须区分两条路径：

- 用户动作命令：收藏和播放模式命令从系统通知或外部媒体控制器进入，最终调用 `MusicAppController` public methods。
- 按钮状态刷新命令：App 内 `PlaybackServiceConnector` 把 shared 状态编码成 `MediaButtonState`，通过 Media3 custom command 发给 `MusicPlaybackService`，只更新 session button preferences 或清理通知，不调用 controller action。

这两条路径共享 command catalog 和 codec，但不能共享同一个 handler 语义。按钮状态刷新不是业务动作，也不能被误接到 `PlaybackMediaButtonActions`。

### `AndroidPlaybackSession.kt`

继续保留 public object 和现有入口：

- `bootstrap(context)`
- `controller`
- `attachLocalMusicScanner(scanner)`
- `attachPermissionSettingsOpener(opener)`
- `attachPlaybackContext(context)`
- `ensurePlaybackSnapshotRestoreRequested()`
- `clearUiBindings()`

它作为门面，不再直接组装完整 repository 依赖图，也不直接持有 Activity 可替换依赖代理的实现细节。

### `AndroidPlaybackControllerFactory.kt`

承接 Android `MusicAppController` 依赖图构建：

- 创建 `PlaybackDatabase`。
- 创建 `PersistentFavoritesRepository`。
- 创建 `PersistentPlaybackRepository`。
- 创建 `RoomPlaybackSnapshotStore`。
- 创建 `PersistentMusicLibraryRepository`。
- 创建 `PersistentSearchHistoryRepository`。
- 消费外部传入的 `PlaybackServiceConnector`。
- 消费外部传入的 `LocalMusicScanner` 代理和 `PermissionSettingsOpener` 代理。
- 注入 `nowMillis`。

该 factory 只负责 wiring，不创建或持有进程生命周期对象。它不拥有 `CoroutineScope`、不创建 `PlaybackServiceConnector`、不保存 controller holder，也不处理 restore 幂等。后续如果 Android 持久化或 repository 装配调整，只改该 factory。

### `AndroidUiBindingRegistry.kt`

承接 Activity 可替换依赖代理：

- `MutableLocalMusicScanner`
- `MutablePermissionSettingsOpener`
- `MissingAndroidLocalMusicScanner`

它表达的是“当前 Activity 生命周期内可用的平台依赖代理”。Activity 重建时替换 scanner 和权限设置入口；Activity 销毁时清空代理，避免进程级 controller 持有失效 Activity。

### `AndroidPlaybackSessionRuntime.kt`

承接进程级 runtime 状态：

- `playbackScope`
- `PlaybackServiceConnector`
- `AndroidPlaybackRuntime`
- controller holder
- `bootstrap(context)` 幂等
- `attachContext(context)`
- 冷启动快照恢复只请求一次

`AndroidPlaybackSessionRuntime` 是 Android 进程生命周期的状态拥有者：

- runtime 创建并持有 `playbackScope`、`PlaybackServiceConnector`、`AndroidPlaybackRuntime` 和 `AndroidUiBindingRegistry`。
- runtime 调用 `AndroidPlaybackControllerFactory` 创建 controller。
- factory 只消费 runtime 提供的 adapter 和 scope，不反向持有 runtime。
- `AndroidPlaybackSession` 委托 runtime 完成 bootstrap、restore、UI binding 和 controller 访问，不直接管理状态字段。

### Media3 command 拆分

`PlaybackMediaCommands.kt` 拆为更小的职责文件。

`AndroidPlaybackMediaButtons` 不作为第四阶段完成态保留。执行拆分时，同一迁移任务内把现有调用点迁移到 catalog、codec、button factory、用户动作 command handler 或按钮状态 updater，并删除旧 object。只有遇到明确的 Media3 API 兼容限制时，才允许短期保留转发门面，并在 implementation plan 中写明删除窗口。

#### `PlaybackMediaCommandActions.kt`

放 `PlaybackMediaButtonActions` 和 dispatcher。

它只保存 controller-backed actions，通知和 MediaSession 都从这里取当前命令实现。

#### `PlaybackMediaCommandCatalog.kt`

放 Media3 custom command 定义：

- custom action 字符串。
- `SessionCommand` 实例。
- `availableSessionCommands()`。
- `updateButtonsCommand()`。
- custom command 类型判断。

它是 Media3 command 定义的唯一来源，避免 action 字符串散落。

#### `MediaButtonStateCodec.kt`

放 `MediaButtonState` 到 `Bundle` 的编码和解析。

解析失败返回 `null`，不执行任何 controller action。该类可以做轻量单元测试，覆盖非法播放模式、非法播放状态和缺失参数。

#### `AndroidPlaybackMediaButtonFactory.kt`

放系统媒体按钮构建：

- 收藏按钮。
- 上一首按钮。
- 播放/暂停按钮。
- 下一首按钮。
- 播放模式按钮。
- system slot 声明。

它只消费 `MediaButtonState` 或展开后的展示参数，不读取 `MusicAppUiState`，也不决定收藏或播放模式业务结果。

#### `AndroidPlaybackMediaCommandHandler.kt`

放用户动作 custom command dispatcher：

- 识别收藏命令。
- 识别播放模式命令。
- 调用 `PlaybackMediaButtonActions`。
- 返回 `SessionResult` code。

未 attach actions 时返回 invalid state。按钮刷新命令不在该 handler 里处理，避免把状态刷新误当成 controller action。

#### `AndroidMediaButtonStateUpdater.kt`

放按钮状态刷新 custom command 的解析与应用：

- 使用 `MediaButtonStateCodec` 解析 `Bundle`。
- 调用 `MusicPlaybackService` 提供的 session button preference 更新函数。
- 在 `PlaybackStatus.Idle` 且没有 active playback session 时清理通知。
- 返回 `SessionResult` code。

它不持有 `PlaybackMediaButtonActions`，不调用 `MusicAppController`，不改变队列、收藏或播放模式业务状态。

### Android 保留边界

这些文件可以保留原名，但职责验收要收紧：

- `MusicPlaybackService` 只负责 service 生命周期、session/player 创建、前台通知接线和 teardown 快照。
- `MediaControllerEventBridge` 只把 Media3 controller/player 事件翻译成 `AudioPlayerEngine` 调用或 engine event。
- `AndroidPlaybackMediaSessionCallback` 只处理 Media3 callback 和 custom command 分发。
- `AndroidPlaybackMediaNotificationProvider` 只处理通知展示顺序和系统兼容性。
- `PlaybackServiceConnector` 继续作为 common `AudioPlayerEngine` 的 Android 实现，不写 repository 或 UI state。

## macOS/Desktop 平台适配设计

macOS/Desktop 侧目标是拆清两层：`DesktopPlaybackSession` 负责进程生命周期和依赖装配，`DesktopVlcjAudioPlayerEngine` 负责把 common 播放命令串行翻译给 vlcj，并把 vlcj 回调规整成 `PlaybackEngineEvent`。

本阶段不改变 vlcj 行为，不新增 Windows/Linux 专门逻辑。

### `DesktopPlaybackSession.kt`

继续保留 public object 和现有入口：

- `controller`
- `ensurePlaybackSnapshotRestoreRequested()`
- `close()`

它作为门面，不再直接写完整依赖图、libVLC fallback 选择或关闭时序。

### `DesktopPlaybackControllerFactory.kt`

从现有 `createDesktopPlaybackController` 迁出独立文件，并继续承接：

- Room database。
- persistent repositories。
- snapshot store。
- favorites 初始加载。
- scanner。
- audio engine。
- controller scope。
- `nowMillis`。

后续如果 desktop persistence 调整，只动 factory。迁移时不改变函数签名和测试入口，避免把纯文件迁移变成行为重写。

### `DesktopPlaybackSessionRuntime.kt`

从现有 `DesktopPlaybackSessionRuntime` 迁出独立文件，并继续承接桌面进程生命周期状态机：

- 冷启动 restore 幂等。
- close 幂等。
- 释放 audio engine。
- 取消 session scope。
- 持久化最终播放快照。
- 关闭 database。
- 汇总关闭过程中的 suppressed failures。

它不构建具体 vlcj adapter，也不构建 repository 依赖图。迁移时保留当前 close 顺序：先 release audio engine，取消 session scope，再读取 controller 当前进度并持久化，最后关闭 database，同时汇总 suppressed failures。

### `DesktopAudioRuntimeFactory.kt`

承接桌面 audio runtime 创建：

- `MacosLibVlcRuntime.resolve()`。
- `VlcjMediaPlayerAdapter` 与 `UnavailableDesktopMediaPlayerAdapter` 选择。
- `DesktopVlcjAudioPlayerEngine` 构建。
- libVLC plugin path 传递。

这样 `DesktopPlaybackSession` 不需要知道 libVLC 路径解析和 fallback 细节。

### `DesktopVlcjAudioPlayerEngine.kt`

保留 `DesktopVlcjAudioPlayerEngine : AudioPlayerEngine` 外部类不变，但把内部职责拆成协作者。

拆分必须保留当前最重要的深模块：`DesktopPlaybackCommandLoop` 是唯一可写 runtime state、唯一可按顺序执行 adapter effect、唯一可发送 `PlaybackEngineEvent` 的 module。其他协作者只能降低局部复杂度，不能新增并发状态所有者。

当前 engine 的关键不变量必须在拆分后继续成立：

- 所有 external command、adapter event 和 progress tick 都进入同一条 command channel。
- `generation` 只在 command loop 内递增和比较。
- release 开始后不再接收新的外部命令，且所有 pending `setQueue` ack 都会完成。
- 切歌、暂停、停止、错误和 release 都通过 command loop 停止 progress ticker。
- 事件发送顺序仍由 command loop 决定，不能由 reducer、ticker 或 ack tracker 直接发送。

#### `DesktopPlaybackCommand.kt`

放 `EngineCommand` sealed 类型：

- `SetQueue`
- `Play`
- `Pause`
- `SeekTo`
- `SkipToIndex`
- `SetPlaybackMode`
- `SetVolume`
- `Stop`
- `Release`
- `AdapterEventReceived`
- `ProgressTick(generation)`

命令定义从巨型 engine 文件中移出，便于 command loop 和测试共享。

#### `DesktopPlaybackEngineState.kt`

承接 engine 可变状态：

- queue。
- current index。
- generation。
- prepared。
- pending seek。
- playback control intent。
- release flags。
- volume。

它不直接调用 vlcj，只表达状态。

#### `DesktopPlaybackCommandLoop.kt`

承接 `commandChannel` 消费、命令分发和 release 后收口。

它是唯一能串行改写 engine runtime state 的地方。外部 `DesktopVlcjAudioPlayerEngine` 只把 `AudioPlayerEngine` 方法转成 command。

command loop 的 interface 应保持深：调用方只知道“提交 command”和“releaseAndAwait”，不需要知道 generation、prepared、pending seek、ticker 或 ack tracker 的组合规则。删除 command loop 后，这些规则会重新散落到 engine、reducer、ticker 和 adapter callback 中；因此 command loop 是本次拆分最重要的 locality。

#### `DesktopMediaSourceMapper.kt`

把 `PlayableMedia` / `AudioSource` 映射成 `DesktopMediaPlayerAdapter` 能播放的路径或 URI。

不支持来源集中映射为播放错误事件，避免多个 command handler 各自处理平台路径。

#### `DesktopAdapterEventReducer.kt`

把 `DesktopMediaPlayerEvent` 和当前 generation/state 规整为状态变化与 `PlaybackEngineEvent`。

Reducer 是纯规则协作者：只接收当前 state snapshot 和 adapter event，返回 command loop 可应用的 transition 描述。它不持有 channel、adapter、ticker 或 coroutine scope，也不直接 mutate `DesktopPlaybackEngineState`。只有 `DesktopPlaybackCommandLoop` 可以应用 reducer result、改写 runtime state、调用 adapter、启动或停止 ticker，并发送 `PlaybackEngineEvent`。

它负责：

- 判断旧 generation 回调是否应被忽略。
- 判断 prepared 回调应兑现 pending seek、play intent、pause intent 还是保持等待。
- 判断播放结束事件应转成 ended transition。
- 判断错误事件应失效当前 generation 并停止 ticker。
- 判断 playing/paused 回调是否符合最近一次 control intent。

它不写 repository，不读 `MusicAppUiState`。

#### `DesktopProgressTicker.kt`

承接播放中进度轮询启动和停止。

切歌、暂停、停止、错误和 release 都通过 command loop 停止 ticker。ticker 只向 command loop 投递带 generation 的 `ProgressTick` command，不直接发送 `PlaybackEngineEvent` 或改写 state；command loop 再按当前 generation 读取 adapter position 并发送 position event，避免旧媒体进度污染新媒体。

#### `DesktopSetQueueAckTracker.kt`

承接 `setQueue` ack 注册、完成、异常收口和 release 全量收口。

它降低 release/setQueue 竞态散落风险，确保调用方不会永久挂起。

ack tracker 只能管理 ack 集合，不能读写 queue、generation、prepared、release flags 或 adapter。release 仍由 command loop 发起全量收口。

#### `DesktopPlaybackControlIntent.kt`

如果当前控制意图类型仍在 engine 文件里，迁移到单独文件或 state 旁边。

它只表达最近一次 play、pause 或 none 意图，不直接调用 adapter。

### Desktop 保留边界

- `DesktopMediaPlayerAdapter.kt` 继续作为 vlcj 和 fake adapter 的接口缝。
- `VlcjMediaPlayerAdapter.kt` 继续封装 vlcj 原生 API，不把 vlcj 类型泄漏到 engine state。
- `MacosLibVlcRuntime.kt` 继续封装 macOS app bundle、VLC fallback 和打包路径解析。
- `DesktopVlcjAudioPlayerEngineTestHooks` 继续只供测试，不进入生产路径判断。

## iOS 边界

iOS 本阶段只做边界定义，不实现真实播放。

现有 `IosEntry` 继续使用 `MusicAppController` 和 `IosFolderMusicScanner`。本阶段不新增：

- `IosPlaybackSession`
- `IosAvAudioPlayerEngine`
- AVFoundation 后台播放
- `AVAudioSession`
- lock screen command center
- iOS 持久化数据库

后续实现 iOS 平台能力时，参考 Android 和 macOS/Desktop 已验证过的边界原则：

- 参考 Android 的宿主/会话接线：iOS 入口负责把平台 scanner、未来 player engine、权限/文件选择入口接到 shared controller。
- 参考 macOS/Desktop 的 adapter 边界：未来 `IosAvAudioPlayerEngine` 只实现 `AudioPlayerEngine`，AVPlayer 事件只转成 `PlaybackEngineEvent`。
- iOS 平台类型只留在 `iosMain`。
- 按 AVFoundation、`AVAudioSession`、`MPRemoteCommandCenter`、后台播放、security-scoped file access 的生命周期单独设计。
- 不提前抽象统一 `PlatformSession`。

如果未来发现 `IosFolderMusicScanner` 和 `IosFolderPicker` 需要拆分，应作为 iOS 平台能力专项处理，不混进本阶段 Android/macOS 结构治理。

## 数据流

Android 通知按钮数据流保持：

```text
System media button / notification custom command
  -> AndroidPlaybackMediaSessionCallback
  -> AndroidPlaybackMediaCommandHandler
  -> PlaybackMediaButtonActions
  -> MusicAppController public method
  -> PlaybackCoordinator / repositories
  -> MusicAppUiState
```

Android 按钮状态刷新数据流保持：

```text
MusicAppUiState observer
  -> PlaybackServiceConnector.refreshMediaButtonPreferences
  -> MediaButtonState
  -> MediaButtonStateCodec
  -> Media3 custom update-buttons command
  -> AndroidPlaybackMediaSessionCallback
  -> AndroidMediaButtonStateUpdater
  -> MediaButtonState
  -> Media3 session button preferences
```

Desktop 播放命令数据流保持：

```text
MusicAppController / PlaybackCoordinator
  -> DesktopVlcjAudioPlayerEngine : AudioPlayerEngine
  -> DesktopPlaybackCommandLoop
  -> DesktopMediaPlayerAdapter
  -> DesktopMediaPlayerEvent
  -> DesktopAdapterEventReducer
  -> transition result
  -> DesktopPlaybackCommandLoop applies state/effects
  -> PlaybackEngineEvent
  -> PlaybackCoordinator
```

平台会话装配数据流保持：

```text
Platform host
  -> AndroidPlaybackSession / DesktopPlaybackSession facade / iOS host
  -> Platform runtime / factory
  -> MusicAppController
```

## 测试策略

### 编译验证

第四阶段涉及 `androidMain`、`desktopMain` 和 common 接口调用边界。完成后至少运行：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
```

因为 Android Media3 command 拆分必须新增 Android local unit tests，最终验证还必须运行：

```bash
./gradlew :composeApp:testDebugUnitTest
```

如果拆分过程中改到 common playback/domain 协议，额外运行相关 common tests。

### Android 测试

Android Media3 command 拆分必须补轻量 local unit tests：

- `MediaButtonStateCodec`：有效 Bundle 可解析。
- `MediaButtonStateCodec`：非法播放模式或非法播放状态返回 `null`。
- `PlaybackMediaCommandCatalog`：custom action 判断准确。
- `AndroidPlaybackMediaCommandHandler`：未 attach actions 时返回 invalid state。
- `AndroidPlaybackMediaCommandHandler`：收藏命令只调用 favorite action。
- `AndroidPlaybackMediaCommandHandler`：播放模式命令只调用 mode action。
- `AndroidMediaButtonStateUpdater`：非法 Bundle 返回 bad value，且不更新 session preferences。
- `AndroidMediaButtonStateUpdater`：idle 且无 active session 时触发 clear notification。

这些测试应放在 `composeApp/src/androidUnitTest`，因为它们依赖 Android `Bundle` 和 Media3 `SessionCommand` / `SessionResult`，不能放进 `commonTest`。如果当前 Gradle source set 尚未创建 `androidUnitTest` 目录，implementation plan 需要把创建目录和确认 `:composeApp:testDebugUnitTest` 可运行作为 Android command 拆分任务的一部分。

若 Android instrumented test 成本过高，本阶段不强制引入新测试框架。

### Desktop 测试

保留并补强现有 `desktopTest`：

- `DesktopVlcjAudioPlayerEngineTest`
- `FakeDesktopMediaPlayerAdapterTest`
- `UnavailableDesktopMediaPlayerAdapterTest`
- `VlcjMediaPlayerAdapterSupportTest`
- `MacosLibVlcRuntimeTest`
- `DesktopPlaybackSessionTest`

如果 command loop、reducer、ack tracker 拆成可测类，补测试覆盖：

- release/setQueue 竞态不会永久挂起。
- 旧 generation 事件被过滤。
- seek 仍是 latest-wins。
- progress ticker 在暂停、停止、错误、release 时停止。
- unavailable adapter 仍回流播放错误而不是崩溃。

### 手动验收

Android：

- App 内播放、暂停、上一首、下一首、seek、音量逻辑不变。
- 通知按钮收藏、播放模式、播放/暂停、上一首、下一首不变。
- 通知正文点击仍打开播放页。
- Activity 旋转或重建后本地扫描和权限设置入口仍可用。
- Service 冷启动或后台命令仍复用同一个 controller。

macOS/Desktop：

- 正常播放、暂停、上一首、下一首、seek、音量不变。
- app 退出时不挂起，最终快照仍持久化。
- libVLC 不可用时仍走 unavailable adapter，不崩溃。
- 播放失败、播放结束、进度刷新仍回流到 UI。
- 关闭窗口后原生播放器和数据库释放。

跨层：

- `commonMain` 不出现 Android、Media3、vlcj、AVFoundation 或 UIKit 类型。
- 平台代码不直接写 repository、`MusicAppUiState` 或 queue 业务规则。
- `MusicAppController` public API 不因本阶段变更而扩大。
- `AudioPlayerEngine` common 契约不新增平台生命周期方法。
- 拆分后的平台入口文件不再同时承担依赖装配、生命周期状态机和底层命令翻译三类职责。

## 验收标准

本阶段完成后应满足：

- `AndroidPlaybackSession.kt` 只保留门面和委托，不直接包含 controller 完整依赖图和 Activity binding 代理实现。
- `PlaybackMediaCommands.kt` 不再作为聚合文件保留；命令定义、按钮构建、Bundle codec 和 command handler 有独立职责文件。
- `AndroidPlaybackMediaButtons` 不作为完成态保留；调用点直接依赖新的 catalog、codec、button factory、用户动作 command handler 或按钮状态 updater。
- `DesktopPlaybackSession.kt` 只保留门面和委托，不直接包含 controller 完整依赖图、libVLC fallback 选择和关闭状态机。
- `DesktopVlcjAudioPlayerEngine.kt` 仍是 `AudioPlayerEngine` 实现入口，但 command、state、event reducer、progress ticker、ack tracker、media source mapper 拆出独立职责。
- `DesktopPlaybackCommandLoop` 是唯一 runtime state 写入者、adapter effect 执行者和 engine event 发送者。
- `DesktopAdapterEventReducer` 不直接 mutate runtime state，`DesktopProgressTicker` 不直接发送 engine event，`DesktopSetQueueAckTracker` 不读写播放状态；状态改写和进度事件发送仍收敛在 command loop。
- Android、Desktop、iOS 平台类型仍留在各自 source set。
- 现有 Android 和 macOS/Desktop 播放行为、快照恢复、错误回流和资源释放行为不变。

## 迁移顺序建议

推荐实施顺序：

1. 先拆 Android Media3 command 文件，因为它主要是纯平台 command/codec/button factory，行为面清晰。
2. 再拆 Android session 装配，把依赖 factory、UI binding registry、runtime 状态分离。
3. 再拆 Desktop session 装配，把 controller factory、session runtime、audio runtime factory 分离。
4. 最后拆 `DesktopVlcjAudioPlayerEngine`，先抽 command/state/ack，再抽 reducer/ticker/mapper，保持每一步都有 desktop tests 兜底。

每一步都应保持公开入口不变，并在阶段性拆分后运行与改动范围匹配的编译或测试。
