# Task 3 Report: Split Android Playback Session Runtime

## What I implemented

- 新增 `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidUiBindingRegistry.kt`
  - 把 `MutableLocalMusicScanner`、`MutablePermissionSettingsOpener` 和 `MissingAndroidLocalMusicScanner` 从 `AndroidPlaybackSession.kt` 拆出
  - 保留原有缺省扫描异常：`LocalMusicScanErrorType.Unknown` + `Android 本地音乐扫描器尚未初始化`
- 新增 `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackControllerFactory.kt`
  - 把 Android Room database、持久化 repository、`RoomPlaybackSnapshotStore` 和 `MusicAppController` 的依赖图集中到工厂函数
  - 保持 runtime attach 不在工厂中发生
- 新增 `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSessionRuntime.kt`
  - 收口进程级 `CoroutineScope`、`PlaybackServiceConnector`、`AndroidPlaybackRuntime`
  - 保留 bootstrap 时先 `attachContext(context.applicationContext)` 再 early return 的时序
  - 保留同步初始化 controller、attach runtime、restore-once 状态与 UI 绑定接线
- 精简 `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSession.kt`
  - 现在仅保留 public facade API，并把所有调用委托给 `AndroidPlaybackSessionRuntime`
  - 保持原有 public 方法名和 `AndroidPlaybackSession 尚未 bootstrap` 错误消息不变
- 更新 `docs/superpowers/plans/2026-07-01-codebase-architecture-optimization-phase4.md`
  - 仅勾选 Task 3 Step 1-7

## What I tested and test results

- Thin-session 检查：
  - 命令：`rg -n "PersistentFavoritesRepository|PersistentPlaybackRepository|PersistentMusicLibraryRepository|MutableLocalMusicScanner|MutablePermissionSettingsOpener|MissingAndroidLocalMusicScanner|SupervisorJob|PlaybackServiceConnector\\(" composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSession.kt`
  - 结果：无输出，确认 `AndroidPlaybackSession.kt` 已变薄
- Android 编译与单测：
  - 命令：`./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest`
  - 结果：`BUILD SUCCESSFUL`

## Files changed

- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSession.kt`
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackControllerFactory.kt`
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidUiBindingRegistry.kt`
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPlaybackSessionRuntime.kt`
- `docs/superpowers/plans/2026-07-01-codebase-architecture-optimization-phase4.md`
- `.superpowers/sdd/task-3-report.md`

## Self-review findings

- facade 边界符合 brief：`AndroidPlaybackSession` 不再保留 controller 构图、UI 绑定代理、scope 或 runtime 初始化细节
- 运行时时序保持一致：`bootstrap` 仍先 attach application context，再判断是否已有 controller
- controller 构图保持一致：数据库、repository、`RoomPlaybackSnapshotStore`、`MusicAppController` 参数与原实现一致
- restore 触发规则保持一致：仍只在首次 `attachLocalMusicScanner` 后请求一次恢复，避免 UI 重建干扰后台播放
- `AndroidPlaybackRuntime.attachController(...)` 仍只在 controller 成功创建后调用，没有提前 attach 半初始化对象

## Any issues or concerns

- 无功能性阻塞
- Gradle 仍有与本任务无关的既有 deprecated property warning，但不影响本次改动验证结果
