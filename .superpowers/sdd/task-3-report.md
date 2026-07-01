# Task 3 Report: Extract PlaybackFailurePolicy

## What I implemented

- 新增 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackFailurePolicy.kt`
  - 抽出纯失败恢复协作者 `PlaybackFailurePolicy`
  - 提供 `onFailure(...)` 与 `reset()`
  - 新增恢复决策枚举 `PlaybackFailureDecision`
- 新增 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackFailurePolicyTest.kt`
  - 覆盖单曲循环同曲两次重试、第三次停留错误
  - 覆盖单曲循环切到新歌后失败窗口独立重置
  - 覆盖非单曲循环前两次跳过、第三次停留错误
  - 覆盖无可恢复目标直接停留错误
  - 覆盖成功恢复后的 `reset()` 清空失败窗口

## Adaptation to current source

- 按当前 `domain/playback` 协作者风格使用 `internal`
- KDoc 与注释保持中文语气，并维持纯 common-domain 边界
- 严格未修改 `PlaybackCoordinator`，只为后续 facade 接线任务准备独立协作者

## Tests run and results

- RED:
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackFailurePolicyTest"`
  - 结果：失败，`PlaybackFailurePolicy` / `PlaybackFailureDecision` unresolved reference
- GREEN:
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackFailurePolicyTest"`
  - 结果：通过，新增失败恢复策略测试全部成功

## TDD Evidence

### RED command/output summary

- 命令：
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackFailurePolicyTest"`
- 摘要：
  - `:composeApp:compileTestKotlinDesktop FAILED`
  - 关键失败：`Unresolved reference 'PlaybackFailurePolicy'`
  - 连带失败：`Unresolved reference 'PlaybackFailureDecision'`
  - 说明测试先于实现建立，RED 证据成立

### GREEN command/output summary

- 命令：
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackFailurePolicyTest"`
- 摘要：
  - `BUILD SUCCESSFUL`
  - 新增 `PlaybackFailurePolicyTest` 全部通过
  - 编译阶段仅出现与本任务无关的既有 warning，不影响结果

## Files changed

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackFailurePolicy.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackFailurePolicyTest.kt`
- `.superpowers/sdd/task-3-report.md`

## Self-review findings

- `PlaybackFailurePolicy` 只持有运行时失败计数和最近失败歌曲标识，没有引入 repository、engine 或 coroutine 依赖
- 单曲循环与非单曲循环的失败窗口拆分保存，避免两类恢复规则在调用方继续分散实现
- `reset()` 显式同时清理两套计数和 `lastFailedSongId`，保证成功恢复后不会把旧窗口带到后续歌曲
- 本任务未越界修改 `PlaybackCoordinator`，和 brief 的任务边界一致

## Concerns, if any

- 无功能性阻塞
- Gradle 仍输出与本任务无关的既有 warning：
  - Kotlin MPP deprecated property 警告
  - 其他测试文件中的既有 `No cast needed` / `Unnecessary safe call` warning
