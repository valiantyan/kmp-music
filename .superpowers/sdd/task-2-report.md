# Task 2 Report: Extract PlaybackQueueNavigator

## What I implemented

- 新增 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackQueueNavigator.kt`
  - 抽出纯队列导航协作者 `PlaybackQueueNavigator`
  - 提供 `next(...)`、`previous(...)`、`exactIndex(...)`、`engineTransition(...)`
  - 提供 `changePlaybackMode(...)`、`removeSong(...)`、`hasDifferentNextTarget(...)`
  - 新增结果类型 `QueueNavigationResult`
- 新增 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackQueueNavigatorTest.kt`
  - 覆盖顺序前进/后退循环
  - 覆盖单曲循环自然结束保持当前
  - 覆盖越界拒绝与随机模式当前项不污染历史
  - 覆盖引擎切歌、模式切换、删歌、可恢复下一首判断

## Adaptation to current source

- 按当前真实实现适配 `ShuffleQueuePolicy`，没有沿用 brief 里的旧接口名
  - 使用 `buildInitialRemaining(...)`
  - 使用 `nextIndex(...)`
  - 使用 `migrateQueueState(...)`
- 没有修改 `PlaybackCoordinator`
- 没有改写 `ShuffleQueuePolicy`，因为当前接口已足够支撑 Task 2 行为

## Tests run and results

- RED:
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackQueueNavigatorTest"`
  - 结果：失败，`PlaybackQueueNavigator` 与 `QueueNavigationResult` unresolved reference
- GREEN:
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackQueueNavigatorTest" --tests "com.yanhao.kmpmusic.domain.playback.ShuffleQueuePolicyTest"`
  - 结果：通过，目标 2 组测试全部成功

## TDD Evidence

### RED command/output summary

- 命令：
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackQueueNavigatorTest"`
- 摘要：
  - `:composeApp:compileTestKotlinDesktop FAILED`
  - 关键失败：`Unresolved reference 'PlaybackQueueNavigator'`
  - 连带失败：`Unresolved reference 'QueueNavigationResult'`
  - 说明测试先于实现建立，RED 证据成立

### GREEN command/output summary

- 命令：
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackQueueNavigatorTest" --tests "com.yanhao.kmpmusic.domain.playback.ShuffleQueuePolicyTest"`
- 摘要：
  - `BUILD SUCCESSFUL`
  - 新增 `PlaybackQueueNavigatorTest` 全部通过
  - 现有 `ShuffleQueuePolicyTest` 继续通过，说明新协作者与当前随机策略接口兼容

## Files changed

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackQueueNavigator.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackQueueNavigatorTest.kt`
- `.superpowers/sdd/task-2-report.md`

## Self-review findings

- `PlaybackQueueNavigator` 保持纯计算边界，只接收 `QueueState` 和简单参数，不引入 repository、engine 或 coroutine 依赖
- 随机模式相关更新全部委托给 `ShuffleQueuePolicy`，避免在 navigator 内复制随机历史规则
- 模式切换与删歌后的随机状态重建遵循当前 `PlaybackCoordinator` 既有语义：仅 Shuffle 模式构建 remaining，其它模式清空
- 当前任务严格未触碰 `PlaybackCoordinator`，为后续 facade 接线任务保留清晰迁移面

## Concerns, if any

- 无功能性阻塞
- Gradle 仍输出与本任务无关的既有 warning：
  - Kotlin MPP/AGP deprecated property 警告
  - 其他测试文件中的既有轻量 warning
