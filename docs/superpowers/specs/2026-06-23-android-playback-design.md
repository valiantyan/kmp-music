# Android 真实播放与媒体通知设计

## 背景

当前项目已经完成本地歌曲数据来源：Android 通过 MediaStore 生成 `content://` 音频 URI，iOS 与 Desktop 也已有平台 scanner 雏形。现有播放链路仍停留在内存状态：`PlaybackState` 只有 `currentSongId` 和 `isPlaying`，点击歌曲只是更新 UI 状态，没有真实音频播放、进度、后台播放、媒体通知和进程恢复。

本设计聚焦 Android 第一版真实播放。目标是在不污染 `commonMain` 的前提下，让 Android 本地歌曲可真实播放，并接入后台播放和自定义媒体通知。iOS 与 Desktop 暂不进入真实播放实现，但共享模型和持久化边界需要为后续平台预留。

## 已确认方案

采用方案 2：`Media3 ExoPlayer + MediaSessionService + MediaSession + 自定义前台媒体通知`。

- ExoPlayer 负责真实播放。
- MediaSessionService 和 MediaSession 负责后台播放、系统媒体键和 Android 标准媒体协议。
- 自定义前台通知负责固定展示产品要求的控制按钮。
- Quick Settings 和锁屏媒体控件由系统根据 MediaSession 自然生成，本轮不定制、不验收。
- 持久化采用 Room3 `3.0.0-rc01`，为 Android、iOS、Compose Desktop JVM 的共享数据存储预留路线。

## 范围与验收

Android 本轮 P0 范围：

- 点击本地音乐、首页、搜索、收藏、专辑、歌手等列表中的歌曲时，当前列表成为播放队列，点击项成为 `currentIndex`。
- ExoPlayer 真实播放 `AndroidMediaStoreScanner` 生成的 `content://` 音频。
- 播放页显示真实播放状态和真实进度，并支持手动 seek。
- 迷你播放器显示当前歌曲和真实播放/暂停状态，可以展示只读进度，但不要求手动 seek。
- 支持上一首、播放/暂停、下一首、收藏/取消收藏、播放模式切换。
- 播放模式只包含 `列表循环 / 单曲循环 / 随机播放`，默认 `列表循环`。
- App 切后台后继续播放。
- Android 前台媒体通知折叠态显示三键：上一首、播放/暂停、下一首。
- Android 前台媒体通知展开态固定显示五键：收藏/未收藏、上一首、播放/暂停、下一首、播放模式。
- 播放模式通知按钮图标随当前模式变化，点击后切换到下一个模式。
- 冷启动恢复当前歌曲、队列、`currentIndex`、播放模式、进度和收藏状态，恢复后显示为暂停，不自动播放。
- 文件缺失、权限失效、格式不支持等错误要提示并自动跳下一首。
- 单曲循环同一首连续失败 3 次后停止；列表循环或随机播放连续 3 首失败后停止。
- Android 真机优先验证后台播放和通知，模拟器仅作辅助。

不进入本轮：

- iOS 真实播放、Now Playing、锁屏或控制中心控制。
- Desktop 真实播放。
- Android Quick Settings 和锁屏媒体控件的视觉、按钮数量、展示位置。
- 固定控制系统派生媒体面板。App 只通过 MediaSession 提供标准播放信息。

## 共享架构边界

共享层新增或升级四类边界。

### AudioPlayerEngine

`AudioPlayerEngine` 是平台无关播放接口，只表达播放意图和事件：

- `setQueue(items, startIndex, startPositionMs)`
- `play()`
- `pause()`
- `seekTo(positionMs)`
- `skipToIndex(index)`
- `stop()`
- `events: Flow<PlaybackEngineEvent>`

common 层只认识 `PlayableMedia`、播放命令和播放事件，不认识 ExoPlayer、MediaSession、Notification、Android Service 或平台 URI 权限 API。

### PlaybackCoordinator

`PlaybackCoordinator` 是 common 层播放协调器，负责把 UI、通知和系统媒体控制转成统一业务命令。

职责：

- 根据用户点击的歌曲和当前列表生成队列。
- 维护 `currentIndex`、播放模式、随机历史、失败计数和最近播放历史。
- 把命令转发给 `AudioPlayerEngine`。
- 订阅 engine event，把真实播放状态写回运行时 repository 和持久化层。
- 处理播放错误和自动跳过规则。

UI 可以在用户点击后进入 `Loading` 或显示目标歌曲，但 `Playing / Paused / Ended / Error / Progress` 必须由 engine event 校正。

### PlaybackRepository

`PlaybackRepository` 继续作为运行时播放快照仓库，供 UI 读取当前状态。它不再被视为真实播放器。

运行时状态包括：

- 当前歌曲。
- 播放状态。
- 播放进度。
- 队列。
- 播放模式。
- 最近错误。

### Room3 持久化层

Room3 负责进程死亡后的恢复，不替代运行时 repository。

最小表：

| 表 | 作用 |
| --- | --- |
| `playback_snapshot` | 保存当前歌曲、`currentIndex`、播放模式、进度、时长、更新时间和恢复状态。 |
| `playback_queue_item` | 保存队列 songId 和 position。 |
| `favorite_song` | 保存收藏歌曲 songId 和更新时间。 |

Room3 entity、DAO 和 database 声明优先放在 common；平台 database builder 分平台实现。本轮只实现和验证 Android builder。iOS 与 Desktop builder 在后续平台播放阶段启用。这里的 Desktop 指当前项目的 Compose Desktop JVM 目标，覆盖 macOS 和 Windows 桌面发布形态。

## Android 架构

Android 层新增：

- `Media3AudioPlayerEngine`：common `AudioPlayerEngine` 的 Android 实现，包装 ExoPlayer。
- `MusicPlaybackService : MediaSessionService`：持有 ExoPlayer 与 MediaSession，支撑后台播放和系统媒体协议。
- `AndroidPlaybackNotificationController`：构建前台媒体通知，负责折叠态三键和展开态五键。
- `PlaybackActionReceiver` 或 service action handler：接收通知按钮，转发成统一播放命令。
- `MusicAppViewModel` 播放 bridge：像当前 scanner 注入一样，为 Activity 重建后的 UI 提供稳定控制入口。

数据流：

```text
UI / Notification Action / System Media Command
  -> MusicAppController
  -> PlaybackCoordinator
  -> AudioPlayerEngine
  -> ExoPlayer / MediaSessionService
  -> PlaybackEngineEvent
  -> PlaybackRepository + Room3 snapshot
  -> MusicAppUiState
  -> UI / Notification refresh
```

通知动作不能直接改 ExoPlayer、收藏仓库或 UI 状态。它们必须走同一套 coordinator/use case，保证 App 内、通知和后续系统控制行为一致。

## 状态模型

### PlaybackStatus

```kotlin
enum class PlaybackStatus {
    Idle,
    Loading,
    Playing,
    Paused,
    Buffering,
    Ended,
    Error,
}
```

`MusicAppUiState.isPlaying` 可以继续作为 UI 便捷字段存在，但必须由 `PlaybackStatus.Playing` 派生。

### PlaybackMode

```kotlin
enum class PlaybackMode {
    LoopAll,
    LoopOne,
    Shuffle,
}
```

默认值是 `LoopAll`。按钮切换顺序为：

```text
LoopAll -> LoopOne -> Shuffle -> LoopAll
```

### PlaybackErrorType

```kotlin
enum class PlaybackErrorType {
    MissingFile,
    UnsupportedFormat,
    PermissionDenied,
    EngineUnavailable,
    Unknown,
}
```

### PlayableMedia

`PlayableMedia` 从 `Song` 派生，由 coordinator 传给 engine：

- `songId`
- `title`
- `artist`
- `album`
- `durationMs`
- `localUri`
- `coverArt`
- `mimeType`

`localUri` 必须来自 scanner 或 importer，UI 不拼接播放地址。

### PlaybackState

`PlaybackState` 升级为：

- `currentSongId`
- `status`
- `positionMs`
- `durationMs`
- `error`

### QueueState

`QueueState` 升级为：

- `songIds`
- `currentIndex`
- `playbackMode`
- `shuffleHistory`
- `shuffleRemaining`

## 队列与播放模式

### 队列生成

用户从任意歌曲列表点击歌曲时，调用形态改为：

```kotlin
playSong(song: Song, queueSongs: List<Song>)
```

规则：

- `queueSongs` 是当前列表完整歌曲集合。
- 点击第 20 首时，第 1 到第 19 首仍在队列中。
- `currentIndex` 指向点击歌曲。
- 下一首、上一首、列表循环和随机播放都在这份完整队列内工作。
- 如果入口无法自然提供列表，则退化为单曲队列。常规列表入口必须传完整列表。

常规列表入口包括：

- 首页本地歌曲预览。
- 首页最近播放。
- 搜索结果歌曲列表。
- 收藏歌曲列表。
- 专辑详情歌曲列表。
- 歌手详情热门歌曲列表。
- 本地音乐歌曲分段。

### LoopAll

当前歌曲结束后进入下一首。最后一首结束后回到第一首。

### LoopOne

当前歌曲结束后重新播放当前歌曲。

### Shuffle

随机模式不改变队列，只改变下一首选择方式：

- 进入随机后维护 `shuffleRemaining`。
- 下一首从本轮未播放歌曲中随机抽取，避免一轮内重复。
- 一轮抽完后重新开始下一轮。
- `previous` 走 `shuffleHistory`，不会重新随机。

## Android 媒体通知

自定义前台通知只负责展示和收集按钮动作，不负责业务判断。

折叠态：

- 封面。
- 标题。
- 歌手。
- 上一首。
- 播放/暂停。
- 下一首。

展开态：

- 封面。
- 标题。
- 歌手。
- 收藏/未收藏。
- 上一首。
- 播放/暂停。
- 下一首。
- 播放模式。

按钮规则：

- 收藏按钮随当前歌曲收藏状态切换图标。
- 播放按钮随 `Playing / Paused / Loading / Buffering` 切换图标或禁用态。
- 播放模式按钮随 `LoopAll / LoopOne / Shuffle` 切换图标。
- 点击播放模式按钮后切到下一个模式。
- 通知内容随 engine event、收藏变化和模式变化刷新。

MediaSession 范围：

- 本轮必须使用 MediaSession。
- App 通过 MediaSession 提供标准 playback state、metadata 和基础命令。
- Quick Settings 和锁屏媒体控件可能自然显示系统媒体卡片，但不定制、不验收。
- 系统媒体控制发来的播放、暂停、上一首、下一首也必须转发给 coordinator。

## 错误处理

Android engine 把 ExoPlayer 错误映射为 common 错误：

| Android 情况 | common 错误 |
| --- | --- |
| 文件不存在或 `content://` 无法打开 | `MissingFile` |
| 解码失败或格式不支持 | `UnsupportedFormat` |
| 权限或安全异常 | `PermissionDenied` |
| player 或 service 不可用 | `EngineUnavailable` |
| 其他异常 | `Unknown` |

coordinator 收到错误后的流程：

1. 写入 `PlaybackState.Error`，保留失败歌曲和原因。
2. UI 和通知刷新到可理解的错误状态。
3. 按播放模式执行自动跳过。

失败限制：

- `LoopOne`：同一首连续失败 3 次后停止播放，保留错误。
- `LoopAll` / `Shuffle`：连续 3 首歌曲失败后停止播放，保留最后一次错误。
- 未达到阈值时自动进入下一首并置为 `Loading`。

自动跳过不能静默执行。播放页应展示最近错误提示；通知可通过内容文案或状态降级体现失败。

## 持久化与恢复

### 写入时机

立即写入 Room3：

- 队列变化。
- 当前歌曲变化。
- 播放模式变化。
- seek。
- pause。
- stop。
- service stop。
- 收藏变化。

节流写入：

- 播放中进度约 500ms 更新运行时状态。
- Room3 进度约 5s 写入一次。
- 暂停、seek、切歌、服务停止时立即补写最终进度。

### 冷启动恢复

恢复流程：

1. App 启动后加载曲库快照和收藏表。
2. 读取 `playback_snapshot` 和 `playback_queue_item`。
3. 用当前曲库过滤失效 songId。
4. 当前歌曲仍存在时，恢复当前歌曲、队列、模式和进度。
5. 当前歌曲不存在但队列还有歌时，选择合法 `currentIndex` 对应歌曲，进度归零。
6. 队列为空时恢复 `Idle`。
7. 恢复状态一律为 `Paused`。
8. 不自动调用 `engine.play()`。用户点击播放后才从恢复进度继续。

收藏状态从 `favorite_song` 恢复，不复制进播放快照。

## 页面与组件影响

### MusicAppController

控制器继续作为 UI 状态入口，但播放相关逻辑应委托给 coordinator：

- `playSong(song, queueSongs)`
- `togglePlayback()`
- `moveTrack(direction)`
- `seekTo(positionMs)`
- `cyclePlaybackMode()`
- `toggleFavorite(songId)`

控制器订阅播放状态和收藏状态，更新 `MusicAppUiState`。

### 播放页

播放页新增或改造：

- 真实进度条。
- seek。
- 播放模式按钮。
- 错误提示。
- loading/buffering 状态。

### 迷你播放器

迷你播放器继续是全局 chrome：

- 显示当前歌曲。
- 显示真实播放/暂停状态。
- 可选展示只读进度。
- 不要求手动 seek。

### 列表入口

所有列表入口需要传入当前列表：

- `onSongPlay(song, queueSongs)`
- `onSongOpen(song, queueSongs)`

避免只传单曲导致队列丢失。

## 测试计划

### 共享测试

新增或扩展 `PlaybackCoordinatorTest`：

- 点击列表第 N 首时，完整列表成为队列，`currentIndex=N`。
- `LoopAll` 最后一首结束后回到第一首。
- `LoopOne` 结束后仍播放当前歌曲。
- `Shuffle` 一轮内不重复。
- `previous` 回到随机历史。
- 播放模式切换顺序正确。
- seek 更新播放页可见进度。
- engine `Playing / Paused / Ended / Error` 事件能校正 repository/UI 状态。
- 单曲循环同一首连续失败 3 次停止。
- 列表循环和随机播放连续 3 首失败后停止。

新增 `PlaybackSnapshotStoreTest`：

- Room3 写入和读取播放快照。
- 队列项按 position 恢复。
- 失效 songId 被过滤。
- 冷启动恢复状态固定为 paused。

新增或扩展收藏持久化测试：

- App 内收藏和通知动作触发同一收藏状态。
- 冷启动后当前歌曲收藏状态恢复。

### Android 验证

至少运行：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

涉及 common 逻辑时运行：

```bash
./gradlew :composeApp:desktopTest
```

如果 Room3/KSP 增加新的构建任务影响编译，按 Gradle 实际任务补充验证。

### Android 真机验收

真机优先验证：

- 扫描 Android MediaStore 后，点击歌曲可真实播放。
- 播放页显示真实进度，支持 seek。
- 迷你播放器状态跟随真实播放/暂停。
- 上一首和下一首按当前列表队列工作。
- 三种播放模式按规则工作。
- 收藏/取消收藏在播放页、列表、通知之间同步。
- App 切后台后继续播放。
- 前台媒体通知折叠态显示三键。
- 前台媒体通知展开态固定显示五键。
- 通知按钮能控制同一播放队列和收藏状态。
- 杀死进程后重新打开 App，恢复上次歌曲、队列、模式、进度和收藏状态，状态为暂停，不自动播放。
- 文件失效或格式错误时显示错误，并按规则自动跳过。

## 举一反三问题速查

| 问题 | 文档结论 | 依据 |
| --- | --- | --- |
| Android 可以只在 UI 里创建 ExoPlayer 吗？ | 不可以。平台播放器必须由 Android engine/service 封装，Composable 只发送播放意图。 | 共享架构边界、Android 架构 |
| 播放状态能否继续只用 `isPlaying` 布尔值？ | 不可以。`isPlaying` 只能作为 UI 派生字段，真实状态要升级为 `PlaybackStatus` 并由 engine event 校正。 | 状态模型 |
| 点击本地音乐第 20 首时，前 19 首会不会进入队列？ | 会。当前列表完整进入队列，`currentIndex` 指向第 20 首。 | 队列与播放模式 |
| 还需要顺序播放模式吗？ | 不需要。本轮只保留列表循环、单曲循环、随机播放。 | 状态模型、队列与播放模式 |
| 随机播放是否允许一轮内重复抽到同一首？ | 不允许。一轮内从 `shuffleRemaining` 抽取，抽完再开启新一轮。 | Shuffle |
| 上一首在随机模式下是否重新随机？ | 不重新随机。上一首走 `shuffleHistory`。 | Shuffle |
| 迷你播放器是否需要手动 seek？ | 不需要。seek 入口只要求在播放页支持；迷你播放器可展示只读进度。 | 范围与验收、页面与组件影响 |
| Android 媒体通知是否必须使用 MediaSession？ | 必须使用。MediaSession 是后台播放、系统媒体键和系统媒体协议的基础。 | 已确认方案、Android 媒体通知 |
| 自定义通知是否替代 MediaSession？ | 不替代。自定义通知只负责固定展示按钮，播放和系统协议仍通过 ExoPlayer 与 MediaSession。 | 已确认方案、Android 架构 |
| Quick Settings 或锁屏媒体控件是否由 App 定制？ | 不定制、不验收。App 只通过 MediaSession 提供标准播放信息，系统是否展示和如何展示由 Android 决定。 | 范围与验收、Android 媒体通知、不声称完成 |
| 为什么持久化不用纯内存或 Android-only DataStore？ | 因为需要进程死亡恢复，并且要为 iOS 与 Compose Desktop JVM 预留跨平台数据路线；本轮采用 Room3 `3.0.0-rc01`。 | Room3 持久化层、持久化与恢复 |
| 冷启动恢复后会自动继续播放吗？ | 不会。恢复当前歌曲、队列、模式和进度后状态固定为暂停。 | 持久化与恢复 |
| 收藏状态是否存进播放快照？ | 不存。收藏由 `favorite_song` 表恢复，播放快照只引用歌曲 id。 | Room3 持久化层、持久化与恢复 |
| 播放失败会静默跳过吗？ | 不会。先进入错误态并展示失败原因，再按规则自动跳过。 | 错误处理 |
| 单曲循环遇到坏文件会无限重试吗？ | 不会。同一首连续失败 3 次后停止并保留错误。 | 错误处理 |
| 列表循环或随机播放连续失败怎么办？ | 连续 3 首歌曲失败后停止播放，并保留最后一次错误。 | 错误处理 |
| Android 第一版完成是否代表 iOS/Desktop 也能真实播放？ | 不代表。本轮只实现 Android；iOS/Desktop 只做共享模型和持久化边界预留。 | 范围与验收、不声称完成 |
| 没有真机验证能否宣称后台播放和通知完成？ | 不能。后台播放和通知必须以 Android 真机验收为准。 | 测试计划、不声称完成 |

## 三轮交叉 Review 结果

### 第一轮：产品范围 Review

结论：

- 本轮范围已限定为 Android 真实播放，不扩大到 iOS 与 Desktop 真播。
- Android P0 包含 App 内播放、后台播放、前台媒体通知、进程恢复、收藏同步和播放模式。
- 媒体通知的验收对象是 App 自定义前台通知；Quick Settings 与锁屏媒体控件不是本轮验收对象。

补强项：

- 明确迷你播放器不要求手动 seek，避免把播放页交互压到全局 chrome。
- 明确折叠态三键、展开态五键，兼顾固定控制和通知空间。
- 明确冷启动恢复后不自动播放，避免用户重开 App 后突然出声。

### 第二轮：架构边界 Review

结论：

- `commonMain` 只保留平台无关播放命令、状态、队列、错误、持久化抽象和 coordinator。
- Android 平台能力由 `Media3AudioPlayerEngine`、`MediaSessionService` 和通知控制器承接。
- 播放状态真相必须来自 engine event，不能继续只写 `PlaybackRepository.isPlaying=true`。

补强项：

- 将运行时 `PlaybackRepository` 与 Room3 持久化层拆开，避免把内存状态误当进程恢复来源。
- 将收藏持久化放入独立 `favorite_song` 表，不复制到播放快照，减少双真相。
- 明确通知动作、系统媒体命令和 UI 操作都必须回到 coordinator，避免绕过队列和失败规则。

### 第三轮：实现与验证 Review

结论：

- 实现应先打通共享播放模型、coordinator、Room3 快照，再接 Android Media3 engine 和通知。
- 共享测试覆盖队列、播放模式、随机历史、失败跳过和恢复规则。
- Android 后台播放与通知必须真机验证，不能只靠编译或单元测试证明完成。

补强项：

- 将播放中进度分为运行时高频更新和 Room3 节流写入，避免频繁 IO。
- 明确错误自动跳过不是静默跳过，UI 和通知要保留失败原因。
- 保留“不声称完成”边界，防止把 Quick Settings、锁屏、iOS 或 Desktop 能力误报为已交付。

## 不声称完成

实现完成后，如果没有真机验证，不能声称后台播放和通知已经验收通过。

本轮不声称：

- Quick Settings 或锁屏媒体控件按钮固定显示。
- iOS 真实播放完成。
- Desktop 真实播放完成。
- macOS Native 或 Windows Native 数据库目标已验证。

## 参考资料

- Android Media3 background playback: https://developer.android.com/media/media3/session/background-playback
- Android media controls: https://developer.android.com/media/implement/surfaces/mobile
- Android custom notification layouts: https://developer.android.com/develop/ui/views/notifications/custom-notification
- Room KMP setup: https://developer.android.com/kotlin/multiplatform/room
- Room3 release notes: https://developer.android.com/jetpack/androidx/releases/room3
