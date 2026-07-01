# Task 1 Report - Extract ShuffleQueuePolicy

## What you implemented

- 新增 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicy.kt`
  - 抽出纯 shuffle 策略，集中维护：
    - 首轮 `shuffleRemaining` 构建
    - shuffle 下一首候选选择
    - shuffle 前进/后退时的 `shuffleHistory` / `shuffleRemaining` 迁移
  - 保留 `randomIndex: (List<Int>) -> Int` 注入点，便于 deterministic 测试。
- 更新 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt`
  - 通过默认协作者 `shuffleQueuePolicy` 委托 shuffle 相关逻辑。
  - `playSong`、`restoreSnapshot`、`cyclePlaybackMode`、`removeFromQueue` 改为委托初始 shuffle remaining 构建。
  - `buildQueueStateForEngineTransition`、`moveToIndex` 改为委托 shuffle 状态迁移。
  - `nextIndex` 在 shuffle 模式下改为委托策略选择候选。
  - 顺手清理了 `persistSnapshotForServiceTeardown` / `persistSnapshotForProcessTeardown` 的两个无效 Elvis warning，没有改行为。
- 新增 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicyTest.kt`
  - 覆盖 task brief 要求的 5 条纯策略不变量。

## What you tested and command outputs

### Focused RED

命令：

```bash
./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.domain.playback.ShuffleQueuePolicyTest
```

结果：

```text
> Task :composeApp:desktopTest FAILED
Execution failed for task ':composeApp:desktopTest'.
> No tests found for given includes: [com.yanhao.kmpmusic.domain.playback.ShuffleQueuePolicyTest](--tests filter)
BUILD FAILED
```

### Focused GREEN

命令：

```bash
./gradlew :composeApp:desktopTest \
  --tests com.yanhao.kmpmusic.domain.playback.ShuffleQueuePolicyTest \
  --tests com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest.shufflePreviousUsesHistory \
  --tests com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest.externalShuffleTransitionUpdatesHistoryAndRemaining \
  --tests com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest.shuffleNextAfterPreviousDoesNotReplayCurrentSong
```

结果：

```text
> Task :composeApp:desktopTest
BUILD SUCCESSFUL in 10s
18 actionable tasks: 8 executed, 10 up-to-date
```

### Full desktop tests

命令：

```bash
./gradlew :composeApp:desktopTest
```

结果：

```text
> Task :composeApp:desktopTest
BUILD SUCCESSFUL in 25s
18 actionable tasks: 7 executed, 11 up-to-date
```

### Android compile

命令：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

结果：

```text
> Task :composeApp:compileDebugKotlinAndroid
BUILD SUCCESSFUL in 15s
25 actionable tasks: 3 executed, 22 up-to-date
```

## TDD Evidence

- RED：`ShuffleQueuePolicyTest` 在实现前不存在，focused test 失败并报 `No tests found`。
- GREEN：实现策略与测试后，focused test 通过；随后 `:composeApp:desktopTest` 和 `:composeApp:compileDebugKotlinAndroid` 全部通过。

## Files changed

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicy.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicyTest.kt`

## Self-review findings

- `ShuffleQueuePolicy` 只依赖 `QueueState` 和 index 集合，没有引入 repository、engine、UI 或平台 API，符合纯 common playback policy 边界。
- `PlaybackCoordinator` 仍保留 facade/orchestrator 角色，没有改 public API，也没有引入其它 phase2 协作者。
- 关键 shuffle 回归语义仍由现有 coordinator 测试覆盖：
  - `shufflePreviousUsesHistory`
  - `externalShuffleTransitionUpdatesHistoryAndRemaining`
  - `shuffleNextAfterPreviousDoesNotReplayCurrentSong`
- 纯策略测试已把历史栈、remaining、空队列和新一轮不重复当前歌曲这几条核心不变量单独钉住。

## Issues/concerns

- 本次只抽取了 shuffle policy；`PlaybackQueueNavigator`、`PlaybackFailurePolicy`、`PlaybackSnapshotWriter`、`PlaybackHistoryRecorder` 仍留在后续 phase2 任务中。
- Gradle 仍会输出项目现有的 deprecated property 警告（`kotlin.mpp.androidGradlePluginCompatibility.nowarn`、`kotlin.mpp.androidSourceSetLayoutVersion`），与本任务无关，未在本次处理。
