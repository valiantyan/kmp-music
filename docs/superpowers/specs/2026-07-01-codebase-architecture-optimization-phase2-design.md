# 代码架构优化第二阶段设计

## 背景

第一阶段已经把 App 状态主干从 `MusicAppController` 中拆出，`MusicAppController` 继续作为 UI 和平台宿主的 facade，导航、搜索、曲库、收藏、播放 UI 同步和轻量 session 状态已经有各自协作者。

第二阶段聚焦 `PlaybackCoordinator.kt` 和 `PlaybackCoordinatorTest.kt`。当前 `PlaybackCoordinator` 约 750 行，仍然同时承担：

- 播放队列生成、下标移动和精确跳转。
- 移除队列歌曲时重建队列、选择新 current、同步 engine queue。
- 随机播放历史和剩余集合维护。
- 引擎事件折返到 `PlaybackRepository`。
- 自然结束、失败重试、失败跳过和单曲循环失败阈值。
- 异步播放快照写入、退出前同步收口和 pending 写入等待。
- 最近播放历史维护。
- `Song` 到 `PlayableMedia` 的引擎输入映射。

这些职责都属于 common 播放语义，不能下沉到 Android Media3、Desktop vlcj 或 UI 层。但它们不需要全部堆在一个类里。第二阶段目标是让 `PlaybackCoordinator` 变成更薄的 common 播放 facade，把内部规则拆成可单独测试的策略和写入器。

## 当前基线

第二阶段必须以当前分支最新播放修复为基线，不能回退这些行为：

- `PlaybackState.isPlaying` 只表示真实 `Playing`，不能重新混用成 UI 暂停按钮语义。
- `Loading`、`Buffering` 和 `Playing` 的 UI 控制显示由 `PlaybackUiSemantics.shouldShowPauseControl` 这类语义判断承接。
- Android 媒体按钮链路不能依赖错误的 `isPlaying` 推断。
- `PersistentPlaybackRepository` 已经区分当前进程运行态和冷启动恢复态，不能让读取状态时反复把运行态压回暂停。
- Desktop vlcj engine 已经记录播放控制意图并过滤切媒体期间的杂散播放/暂停回调，第二阶段不能破坏这条防线。
- `PlaybackCoordinator` 当前有两个 Kotlin warning，来自 `currentPlaybackState.currentSongId ?: queueState.currentSongId` 这类无效 Elvis 表达式。第二阶段应顺手清理，但不把 warning 清理伪装成架构拆分成果。

当前第一阶段验收命令已经通过：

```bash
./gradlew :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid
```

## 执行状态

| 任务 | 状态 | 说明 |
| --- | --- | --- |
| Task 1: Extract `ShuffleQueuePolicy` | 已完成 | commits `39dd3d1..cd6937d`，task review clean after report follow-up。 |
| Task 2: Extract `PlaybackQueueNavigator` | 未开始 | 后续任务处理。 |
| Task 3: Extract `PlaybackFailurePolicy` | 未开始 | 后续任务处理。 |
| Task 4: Extract `PlaybackSnapshotWriter` | 未开始 | 后续任务处理。 |
| Task 5: Extract `PlaybackHistoryRecorder` | 未开始 | 后续任务处理。 |
| Event reducer extraction | 不在本阶段 | 本阶段继续保留事件编排在 `PlaybackCoordinator`。 |

## 第一性原则

第二阶段继续遵守播放主干：

```text
MusicAppController
  -> PlaybackCoordinator
  -> AudioPlayerEngine
  -> platform player adapter
```

`PlaybackCoordinator` 仍然是 common 层播放业务语义唯一协调器。拆分只发生在它的内部协作者之间，不改变外部调用者对播放能力的入口。

判断拆分是否值得，使用三个标准：

1. 这个规则是否能脱离平台引擎单独测试。
2. 这个规则是否有独立变化原因。
3. 抽出后是否减少 `PlaybackCoordinator` 需要同时理解的状态变量。

不为行数制造无意义中转层。纯转发、只包装一个函数、没有独立不变量的类不进入第二阶段。

## 方案取舍

### 方案 A：只按私有函数重排

把 `PlaybackCoordinator` 内部函数重新排序、重命名，并补少量注释。

优点是风险最低，不改变文件结构。缺点是根本问题没有解决：随机、失败、快照和历史仍然共享同一个类的私有状态，测试仍主要通过大协调器入口覆盖。

结论：不推荐。它更像整理房间，不是结构优化。

### 方案 B：抽出全部细粒度策略

按第一阶段设计中的五个名字完整抽出：`PlaybackQueueNavigator`、`ShuffleQueuePolicy`、`PlaybackSnapshotWriter`、`PlaybackFailurePolicy`、`PlaybackHistoryRecorder`，并把事件 reducer 也抽成独立类。

优点是边界清晰，单元测试粒度最好。缺点是如果一次性把事件 reducer 也抽走，`PlaybackCoordinator` 可能变成过早的编排壳，短期内会增加状态传递复杂度。

结论：方向正确，但需要控制一次拆分的深度。

### 方案 C：先抽稳定规则，保留事件编排

先抽出纯队列/随机策略、失败策略、快照写入器和历史记录器；`PlaybackCoordinator` 保留事件订阅、public method、repository 写入顺序和 engine 命令编排。

优点是能移走最复杂、最可测试的规则，同时保留播放主干的局部性。引擎事件的写 repository 顺序仍在协调器里，降低行为回归风险。

结论：推荐采用。第二阶段以方案 C 为目标，后续如果事件 reducer 仍然拥挤，再另开阶段处理。

## 范围

### 本阶段包含

- 抽出 `PlaybackQueueNavigator`，集中处理下一首、上一首、精确下标、移除队列歌曲和引擎媒体切换对应的队列状态迁移。（状态：未开始）
- 抽出 `ShuffleQueuePolicy`，集中维护随机模式的历史栈、剩余集合和首轮待播集合。（状态：已完成）
- 抽出 `PlaybackFailurePolicy`，集中处理单曲循环失败阈值、连续失败跳过和成功播放后的计数清空。（状态：未开始）
- 抽出 `PlaybackSnapshotWriter`，集中管理异步快照写入、pending 写入集合、退出前等待和同步落盘。（状态：未开始）
- 抽出 `PlaybackHistoryRecorder`，集中维护最近播放历史去重和上限。（状态：未开始）
- 保留 `PlaybackCoordinator` 公开方法和构造参数兼容，除非后续执行计划确认可以用默认参数平滑迁移。
- 拆分 `PlaybackCoordinatorTest` 中的纯规则测试，让协调器测试保留跨职责验收。
- 清理当前 `PlaybackCoordinator.kt` 中的无效 Elvis warning。

### 本阶段不包含

- 不改变 `AudioPlayerEngine` 契约。
- 不改变 `PlaybackRepository` 契约。
- 不重写 Android Media3、MediaSession、通知或媒体按钮链路。
- 不重写 Desktop vlcj engine、adapter event reducer 或 LibVLC runtime resolver。
- 不实现 iOS、Windows 或网络音频播放。
- 不把 `PlaybackCoordinator` 的角色下沉到 UI、Android service 或 Desktop engine。
- 不改变播放模式产品语义：`LoopAll` 当前仍代表顺序播放并在队尾回到队首。
- 不把播放 UI 状态同步重新塞回 `PlaybackCoordinator`。

## 目标结构

建议新增文件都放在 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/`，因为这些规则属于 common 播放语义，不属于 feature UI。

| 模块 | 形态 | 状态 | 职责 |
| --- | --- | --- | --- |
| `PlaybackCoordinator` | facade / orchestrator | 进行中 | 保留 public API、订阅 engine event、维护 repository 写入顺序、调用 engine 命令、把 `Song` 映射为 engine 队列。 |
| `ShuffleQueuePolicy` | 纯策略 | 已完成 | 构建随机初始剩余集合、前进/后退时更新 `shuffleHistory` 和 `shuffleRemaining`。 |
| `PlaybackQueueNavigator` | 纯策略 | 未开始 | 基于 `QueueState`、目标操作和必要的 song id 列表计算目标下标与下一份 `QueueState`。 |
| `PlaybackFailurePolicy` | 有状态策略 | 未开始 | 记录失败计数，判断是否重试当前歌曲、跳到下一首或停留错误态。 |
| `PlaybackSnapshotWriter` | IO 协作者 | 未开始 | 读取 repository 当前状态，写入 `PlaybackSnapshotStore`，管理 pending async writes 和 teardown await。 |
| `PlaybackHistoryRecorder` | repository 协作者 | 未开始 | 维护最近播放历史，新歌曲置顶、去重、最多 50 条。 |

不建议单独抽 `SongPlayableMediaMapper`。`Song.toPlayableMedia()` 当前只是字段映射，且只在协调器给 engine 组队列时使用。可以先保留为 `PlaybackCoordinator` 私有扩展；如果未来网络音频或多来源 metadata 让映射复杂化，再抽成独立 mapper。

## 关键边界

### 队列导航

`PlaybackQueueNavigator` 不接触 repository，不调用 engine，不记录历史。它接收当前 `QueueState`、目标动作、必要的队列 song id 列表和随机选择器，返回下一份队列状态和目标下标。它不接收完整 `Song`，也不负责 `Song` 到 `PlayableMedia` 的映射。

它应覆盖这些动作：

- 普通下一首。
- 普通上一首。
- 随机下一首。
- 随机上一首。
- 外部平台控制器通过 `CurrentMediaChanged(index)` 切换到精确下标。
- 切换播放模式后重置随机历史和剩余集合。
- 移除队列歌曲后重建 song id 队列、计算新的 currentIndex、重置随机历史和 remaining。

初始点歌队列生成是一个例外：它需要完整 `Song` 列表、目标 `Song`、当前播放模式和后续 engine 队列映射。第二阶段不把这部分塞进 navigator。`PlaybackCoordinator` 继续决定点歌时的 `matchingQueueSongs` 和 `startIndex`，只委托 `ShuffleQueuePolicy` 构建初始随机 remaining。

移除队列歌曲时，`PlaybackCoordinator` 仍负责把 `availableSongs` 解析成可交给 engine 的 `Song` 列表，并处理解析后队列为空时的 stop 分支。`PlaybackQueueNavigator` 只负责基于已经解析出的剩余 song id 列表计算下一份 `QueueState`，避免纯策略层需要理解完整 `Song` 或 engine 队列。

`PlaybackCoordinator` 继续负责把 navigator 结果写入 repository、记录播放历史、保存快照和调用 engine。

### 随机播放

状态：已完成。`ShuffleQueuePolicy` 已抽出为 common 层内部策略，当前通过 `PlaybackCoordinator` 私有协作者使用，不暴露给 UI 或平台层直接调用。

`ShuffleQueuePolicy` 是 `PlaybackQueueNavigator` 的内部策略，不应被 UI 或平台层直接调用。它的核心不变量：

- 随机模式首轮待播集合不包含当前下标。
- 随机前进会把旧 currentIndex 推入 history。
- 随机后退优先弹出 history，并把离开的 currentIndex 放回 remaining。
- remaining 为空时开启下一轮，但不立即重复当前下标。
- 队列为空时不产生有效目标下标。

当前 `randomIndex: (List<Int>) -> Int` 注入点应保留，保证测试可以固定随机结果。

### 失败恢复

`PlaybackFailurePolicy` 可以持有失败计数，因为这些计数本来就是协调器的私有运行时状态。它不写 repository，不调用 engine，只返回决策：

- `RetryCurrent`：单曲循环同一首失败次数小于 3，重试当前下标并保留错误。
- `SkipToNext`：非单曲循环连续失败次数小于 3，跳到下一首并保留错误。
- `StayError`：达到阈值或没有可用下一首，停留错误态。
- `Reset`：成功进入 `Playing` 后清空计数。

第二阶段保持当前产品语义：失败计数只在 engine 回报 `PlaybackStatus.Playing` 时 reset。用户点新歌、恢复快照、seek、暂停、移除队列和普通切歌都不主动清空失败计数。原因是这些动作还不代表底层媒体已经成功播放；提前清空会掩盖连续坏文件或平台解码失败。若后续产品希望“用户主动点新歌即开启新失败窗口”，需要另开行为设计并补测试。

`PlaybackCoordinator` 仍负责先把错误写入 repository，再根据决策调用队列导航和 engine 命令。这样 UI 能看到最近错误，直到下一首真正恢复播放。

### 快照写入

`PlaybackSnapshotWriter` 承接当前协调器里的 pending 写入集合和写入作用域。它依赖：

- `PlaybackRepository`
- `PlaybackSnapshotStore`
- `CoroutineScope`
- `nowMillis`

它负责：

- `saveAsync()`：异步写入当前 repository 状态，并纳入 pending 集合。
- `saveNowAndAwait()`：同步写入当前 repository 状态，用于进程退出前最后收口。
- `awaitPendingWrites()`：等待所有已发出的异步写入完成。
- `saveForEvent(event)`：对 `ProgressChanged` 应用节流，其他关键事件立即写。

快照 writer 不应在第二阶段擅自做语义去重。当前流程可能在一次 engine event 中出现两类写入：事件处理内部因切歌、失败或结束触发的立即写入，以及事件处理后 `saveForEvent(event)` 的兜底写入。实现可以通过 focused tests 证明多写不会产生竞态，但不能为了减少写入次数改变这些关键落盘时机。只有纯 `ProgressChanged` 可以继续按 `snapshotThrottleMs` 节流。

`PlaybackCoordinator` 的 `persistSnapshotForServiceTeardown` 和 `persistSnapshotForProcessTeardown` 仍保留 public API，但内部委托 writer。进程退出同步写入失败必须继续向宿主抛出，不能吞掉。

### 最近播放历史

状态：未开始。此部分由后续 `PlaybackHistoryRecorder` 任务处理。

`PlaybackHistoryRecorder` 负责读取并保存 `PlaybackHistory`：

- 新播放歌曲排在最前。
- 重复歌曲只保留最新位置。
- 最多保留 50 条。

它不读取队列，不判断播放状态，不写快照。`PlaybackCoordinator` 在用户点歌和成功切换目标下标时调用它。

### 引擎事件

状态：按设计保留在 `PlaybackCoordinator`，不作为 Task 1 改动。

第二阶段暂不把 `handleEngineEvent` 整体抽成 reducer。原因是事件处理不仅是纯状态迁移，还涉及快照写入、失败策略、队列导航、状态回调和 engine 命令。过早抽成 event reducer 会让依赖从一个大类变成一个大参数对象。

本阶段只把事件处理中调用的队列、失败和快照规则下沉。`PlaybackCoordinator` 仍保留：

- `start`
- `handleEngineEvent`
- `handleCurrentMediaChanged`
- `updateProgress`
- `updateStatus`
- `handleEnded`
- `handleFailure`

如果阶段二完成后这些函数仍然臃肿，再用后续阶段处理事件 reducer，而不是在本阶段扩大范围。

## 数据流

用户点歌：

```text
MusicAppController.playSong
  -> PlaybackCoordinator.playSong
  -> PlaybackCoordinator 生成 matchingQueueSongs / startIndex
  -> ShuffleQueuePolicy.buildInitialRemaining
  -> PlaybackRepository.saveQueueState / savePlaybackState
  -> PlaybackHistoryRecorder.record
  -> PlaybackSnapshotWriter.saveAsync
  -> AudioPlayerEngine.setPlaybackMode / setQueue / play
```

自然结束：

```text
AudioPlayerEngine.events: Ended
  -> PlaybackCoordinator.handleEnded
  -> PlaybackQueueNavigator.next
  -> PlaybackRepository.saveQueueState / savePlaybackState
  -> PlaybackHistoryRecorder.record
  -> PlaybackSnapshotWriter.saveAsync
  -> AudioPlayerEngine.skipToIndex / play
```

失败恢复：

```text
AudioPlayerEngine.events: Failed
  -> PlaybackCoordinator.handleFailure
  -> PlaybackRepository.savePlaybackState(Error)
  -> PlaybackFailurePolicy.onFailure
  -> PlaybackQueueNavigator.next or current
  -> PlaybackCoordinator.moveToIndex(clearError = false)
```

移除队列歌曲：

```text
MusicAppController.removeFromQueue
  -> PlaybackCoordinator.removeFromQueue
  -> PlaybackQueueNavigator.removeSong
  -> PlaybackRepository.saveQueueState / savePlaybackState
  -> PlaybackSnapshotWriter.saveAsync
  -> AudioPlayerEngine.setPlaybackMode / setQueue
  -> AudioPlayerEngine.play or pause
```

快照写入：

```text
Repository current state
  -> PlaybackSnapshotWriter
  -> PlaybackSnapshotStore
```

## 测试策略

新增 focused tests：

| 测试文件 | 状态 | 覆盖内容 |
| --- | --- | --- |
| `PlaybackQueueNavigatorTest` | 未开始 | 顺序下一首/上一首、精确跳转、外部引擎下标迁移、播放模式切换后的队列状态、移除当前/非当前歌曲后的队列状态。 |
| `ShuffleQueuePolicyTest` | 已完成 | 初始 remaining、前进 history、后退恢复 remaining、下一轮不重复当前歌曲。 |
| `PlaybackFailurePolicyTest` | 未开始 | 单曲循环三次阈值、非单曲连续失败三次阈值、成功播放后计数清空。 |
| `PlaybackSnapshotWriterTest` | 未开始 | 首个进度事件写入、进度节流、关键事件不被节流、pending writes 等待、同步写入异常向外抛。 |
| `PlaybackHistoryRecorderTest` | 未开始 | 新歌曲置顶、重复去重、最多 50 条。 |

`PlaybackCoordinatorTest` 保留 facade 级跨职责验收：

状态：Task 1 相关 shuffle 回归已验证，其它跨职责验收随后续任务继续保留和补充。

- 点歌写入队列并启动 engine。
- 恢复快照预热 engine，并停在暂停态。
- toggle 在 `Loading` 状态下请求 play，而不是误 pause。
- 显式 play/pause 通过 engine event 回写 repository。
- 自然结束调用队列导航并推进 engine。
- 失败自动跳歌时错误保留到下一首真正 `Playing`。
- 成功 `Playing` 后清空失败计数，普通切歌和移除队列不提前清空失败计数。
- 移除当前歌曲后 repository 队列和 engine 队列保持同步。
- service/process teardown 写入暂停快照。

阶段二执行时每个小拆分都先写或迁移测试，再移动实现，最后运行对应 focused test 和受影响的 `PlaybackCoordinatorTest`。

## 回归防护

- 每个协作者抽出后，`PlaybackCoordinator` public API 保持兼容。
- 不改变 `AudioPlayerEngine` 和 `PlaybackRepository` 的方法签名。
- 不改变 `PlaybackState.isPlaying` 语义。
- 不删除现有协调器验收测试，只把纯规则迁移到 focused test。
- 不把移除队列歌曲留成协调器内的大块私有规则；它必须进入队列策略或明确的队列重建 helper。
- 不为减少快照写入次数改变结束、失败、切歌和退出前同步写入的落盘时机。
- 与 Android 媒体按钮、Desktop vlcj 意图过滤、持久化恢复相关的修复测试必须继续保留。
- 如果某个拆分需要平台层一起改动，先停下来重新评估范围，不在第二阶段偷偷扩大到第四阶段。

## 验收标准

第二阶段完成时应满足：

- `PlaybackCoordinator.kt` 明显变薄，主要负责 public API、repository 写入顺序、engine 命令和事件编排。
- 队列导航、随机策略、失败策略、快照写入和历史记录各有清晰归属。
- `PlaybackCoordinatorTest.kt` 不再是纯队列、纯随机、纯失败计数和纯快照写入规则的唯一承载点。
- 当前两个 Kotlin warning 被清理。
- 随机后退再前进不重复当前歌曲。
- 外部 `CurrentMediaChanged(index)` 在随机模式下同步更新 shuffle history 和 remaining。
- 失败自动跳歌保留错误，直到下一首真正进入 `Playing`。
- `removeFromQueue` 移除当前歌曲后同步 repository 队列和 engine 队列。
- process teardown 同步快照写入失败继续向宿主抛出。
- `:composeApp:desktopTest` 通过。
- `:composeApp:compileDebugKotlinAndroid` 通过。
- Android 与 Desktop 的播放修复基线不回退。

## 风险与取舍

| 风险 | 处理 |
| --- | --- |
| 抽分后播放状态写入顺序变化 | 让 `PlaybackCoordinator` 继续持有 repository 写入顺序，协作者只返回决策或执行快照写入。 |
| 随机播放规则被拆散后更难理解 | 随机相关不变量集中到 `ShuffleQueuePolicy` 和对应测试里。 |
| 快照异步写入出现竞态 | `PlaybackSnapshotWriter` 复用当前 pending writes 机制，并补 focused tests 锁住 teardown 行为。 |
| 失败策略隐藏错误展示时机 | `PlaybackCoordinator` 仍先保存 Error，再按 policy 决策跳转，保持“错误保留到恢复播放”的现有行为。 |
| 文件数量增加但深度不足 | 不抽只有字段映射或单次转发的类；每个新增类必须有独立测试和一句话职责。 |

## 最终判断

第二阶段应该推进，但要按“稳定规则先抽、事件编排后留”的方式推进。推荐先拆纯队列与随机策略，再拆失败策略，然后拆快照 writer 和历史 recorder，最后收窄 `PlaybackCoordinatorTest`。这样能根治 `PlaybackCoordinator` 职责堆积，同时把播放行为回归风险控制在 common 层测试内。
