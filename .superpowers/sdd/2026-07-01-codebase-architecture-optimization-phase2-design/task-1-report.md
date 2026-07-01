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

### Initial test filter attempt

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

说明：这只是一次初始的 `--tests` 过滤尝试，说明过滤条件没有命中目标测试，不构成有效的 RED/GREEN 证据。

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

- 过滤尝试：第一次 `--tests` 命令返回 `No tests found`，仅说明过滤未命中，不作为 TDD 的 RED。
- RED：随后通过临时破坏 `ShuffleQueuePolicy.buildInitialRemaining()` 的行为，`ShuffleQueuePolicyTest` 出现真实失败。
- GREEN：恢复正确实现后，focused policy/coordinator 回归、`:composeApp:desktopTest` 和 `:composeApp:compileDebugKotlinAndroid` 全部通过。

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

## Follow-up for review findings

### Fix 1: 收紧 `ShuffleQueuePolicy` 可见性并移除 public 构造暴露

- 将 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicy.kt` 的类可见性收紧为 `internal`。
- 将 `buildInitialRemaining`、`nextIndex`、`migrateQueueState` 明确标记为 `internal`，避免继续以默认 `public` 暴露模块边界。
- 从 `PlaybackCoordinator` 的 public 构造参数中移除 `shuffleQueuePolicy` 协作者，改为协调器内部私有实例：
  - 保留原有 `randomIndex` 注入点；
  - 不再把 `ShuffleQueuePolicy` 类型暴露到 `PlaybackCoordinator` 的 public API 中；
  - 测试和生产集成都继续走同一条 `randomIndex -> ShuffleQueuePolicy` 路径。

### Fix 2: 补充诚实的失败/通过证据

原报告中的 “RED” 证据无效：`No tests found` 只能说明 `--tests` 过滤条件不匹配，不能证明行为测试先失败。因此这里补一段 follow-up 证据，并明确说明它不是“补写历史”，而是对新增测试做一次可审计的行为验证。

做法：

1. 保持最终正确实现不变之前，先临时引入一个可逆突变：把 `ShuffleQueuePolicy.buildInitialRemaining()` 改成返回全部 index，故意破坏“首轮随机候选不包含当前歌曲”这一核心不变量。
2. 运行 focused `ShuffleQueuePolicyTest`，记录真实失败输出。
3. 立即恢复正确实现。
4. 重新运行 focused policy/coordinator shuffle 回归、`desktopTest` 和 Android 编译，记录 GREEN。

### Follow-up failure evidence

临时突变内容：

```kotlin
internal fun buildInitialRemaining(queueSize: Int, currentIndex: Int): List<Int> {
    return (0 until queueSize).toList()
}
```

命令：

```bash
./gradlew :composeApp:desktopTest --tests '*ShuffleQueuePolicyTest'
```

结果：

```text
> Task :composeApp:desktopTest FAILED

ShuffleQueuePolicyTest[desktop] > buildInitialRemainingExcludesCurrentIndex[desktop] FAILED
    java.lang.AssertionError at ShuffleQueuePolicyTest.kt:23

5 tests completed, 1 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':composeApp:desktopTest'.
> There were failing tests.

BUILD FAILED in 9s
```

说明：这次失败来自故意破坏 `buildInitialRemaining` 的行为，能够真实证明新增纯策略测试会在不变量被打破时报警；随后已恢复正确代码，最终工作树不保留该突变。

### Follow-up GREEN after restoring correct code

命令（focused policy + coordinator shuffle 回归）：

```bash
./gradlew :composeApp:desktopTest \
  --tests '*ShuffleQueuePolicyTest' \
  --tests 'com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest.shufflePreviousUsesHistory' \
  --tests 'com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest.externalShuffleTransitionUpdatesHistoryAndRemaining' \
  --tests 'com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest.shuffleNextAfterPreviousDoesNotReplayCurrentSong'
```

结果：

```text
> Task :composeApp:desktopTest
BUILD SUCCESSFUL in 18s
18 actionable tasks: 4 executed, 14 up-to-date
```

命令（full desktop tests）：

```bash
./gradlew :composeApp:desktopTest
```

结果：

```text
> Task :composeApp:desktopTest
BUILD SUCCESSFUL in 14s
18 actionable tasks: 7 executed, 11 up-to-date
```

命令（Android compile）：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

结果：

```text
> Task :composeApp:compileDebugKotlinAndroid
BUILD SUCCESSFUL in 28s
25 actionable tasks: 3 executed, 22 up-to-date
```
