# Task 2 Report: Extract NavigationStateController

## What I implemented

- 新增 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/navigation/NavigationStateController.kt`
  - 抽出 `navigateToSecondary(...)`
  - 抽出 `navigateToRoot(...)`
  - 抽出 `navigateBack(...)`
- 新增 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/MusicAppNavigationControllerTest.kt`
  - 覆盖进入二级页时保存 `previousRootTab`、自增 `secondaryEntryId`、关闭 queue / more
  - 覆盖切换一级 Tab 时清空二级路由并重置 `previousRootTab`
  - 覆盖返回一级页时保留 `secondaryEntryId`
- 修改 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
  - 保持 `MusicAppController` 作为唯一 public facade 和 Compose mutable state owner
  - 将 `navigateToSecondary(...)`、`navigateToRoot(...)`、`navigateBack()` 改为委托给 `NavigationStateController`
  - 保持 `handleSystemBack()` 原有关闭顺序不变
- 更新 `docs/superpowers/plans/2026-06-30-codebase-architecture-optimization-phase1.md` 中 Task 2 的步骤勾选

## Tests run and results

- RED:
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest"`
  - 结果：失败，`NavigationStateController` unresolved reference；同时当前仓库里的 `Song` / `CoverArt` 模型与 brief 样例有差异，测试夹具需要补齐现有必填字段
- GREEN:
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.rootNavigationClearsSecondaryScreen" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.systemBackClosesOverlayBeforeSecondaryScreen"`
  - 结果：通过，目标 3 组测试全部成功

## TDD Evidence

### RED command/output summary

- 命令：
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest"`
- 摘要：
  - `:composeApp:compileTestKotlinDesktop FAILED`
  - 关键失败：`Unresolved reference 'NavigationStateController'`
  - 同次编译还暴露出当前 `Song` 构造函数缺少 `coverArt`、`isLiked`、`lastPlayed`、`quality`、`lyric`、`trackNumber` 等必填参数，说明需要让测试夹具适配现有模型

### GREEN command/output summary

- 命令：
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.rootNavigationClearsSecondaryScreen" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.systemBackClosesOverlayBeforeSecondaryScreen"`
- 摘要：
  - `BUILD SUCCESSFUL`
  - 新增导航 reducer 测试通过
  - facade 回归测试 `rootNavigationClearsSecondaryScreen` 与 `systemBackClosesOverlayBeforeSecondaryScreen` 通过

## Files changed

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/navigation/NavigationStateController.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/MusicAppNavigationControllerTest.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- `docs/superpowers/plans/2026-06-30-codebase-architecture-optimization-phase1.md`
- `.superpowers/sdd/task-2-report.md`

## Self-review findings

- 抽取后的 `NavigationStateController` 保持纯函数形态，只接收 `MusicAppUiState` 并返回新状态，没有引入 Compose 状态或仓库依赖
- `MusicAppController` 仍然保留搜索 history commit 与系统返回编排，职责边界和 brief 要求一致
- 导航 reducer 保留了 `secondaryEntryId` 的累加与回退保留语义，因此不会破坏现有滚动 key 稳定性
- 新测试覆盖了 queue / more 清理规则，避免以后把导航副作用悄悄散回 facade

## Concerns, if any

- 无功能性阻塞
- Gradle 仍输出与本任务无关的既有 warning：
  - Kotlin MPP/AGP deprecated property 警告
  - `PlaybackCoordinator.kt` 中 Elvis operator 警告
  - 现有测试文件中的轻量 warning
