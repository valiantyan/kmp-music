# Task 7 报告：拆出本地扫描工作流并加固会话取消

## 实现摘要

- 新增 `LocalMusicScanController`，把本地扫描的会话编号、取消请求、权限设置确认和旧事件丢弃从 `MusicAppController` 中抽离。
- `MusicAppController` 改为委派扫描入口与权限设置入口，只保留 `uiState` 所有权和曲库快照同步门面。
- 新增 `LocalMusicScanControllerTest`，覆盖二次触发取消、旧成功晚到、旧错误晚到。
- 在 `MusicAppControllerTest` 增加门面回归，验证用户取消后旧扫描结果晚到不会覆盖取消态或队列状态。
- 更新计划文档，将任务七七个步骤全部勾选为完成。

## 红灯命令和结果

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.library.LocalMusicScanControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.lateScanResultAfterCancellationDoesNotOverwriteCancelledStateOrQueue"
```

- 结果：失败。
- 关键信息：
  - `Unresolved reference 'LocalMusicScanController'`
  - 由于生产类缺失，`publishStateUpdate` 的 lambda 类型也连带无法推断。

## 绿灯命令和结果

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.library.LocalMusicScanControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.lateScanResultAfterCancellationDoesNotOverwriteCancelledStateOrQueue"
```

- 结果：通过，`BUILD SUCCESSFUL`。

## 任务七验证命令和结果

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.library.LocalMusicScanControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.scanCompletionKeepsCurrentLocalMusicRoute" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.scanEntryDuringRunningScanDoesNotStartSecondScan" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.scanStateSettlesWhenRunningScanCoroutineIsCancelledExternally" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.cancelledScanStateIsDistinctFromDoneAndError" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.lateScanResultAfterCancellationDoesNotOverwriteCancelledStateOrQueue"
```

- 结果：通过，`BUILD SUCCESSFUL`。

## 最终验证命令和结果

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
```

- 结果：通过，`BUILD SUCCESSFUL`。

## 文件变更

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library/LocalMusicScanController.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/LocalMusicScanControllerTest.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`
- `docs/superpowers/plans/2026-07-08-music-app-controller-workflow-refactor.md`

## 自审 / 对抗式审查

1. 旧扫描成功在取消后晚到，是否会重新同步曲库并覆盖取消态？
   - 已验证：`LocalMusicScanControllerTest.lateSuccessAfterCancellationIsIgnored`
   - 已验证：`MusicAppControllerTest.lateScanResultAfterCancellationDoesNotOverwriteCancelledStateOrQueue`

2. 旧扫描错误在取消后晚到，是否会把取消态改成错误态？
   - 已验证：`LocalMusicScanControllerTest.lateErrorAfterCancellationIsIgnored`

3. 扫描中再次触发，是否会偷偷启动第二个扫描会话？
   - 已验证：`LocalMusicScanControllerTest.runningScanSecondEntryCancelsCurrentSessionOnly`
   - 已验证：`MusicAppControllerTest.scanEntryDuringRunningScanDoesNotStartSecondScan`

4. 外部协程取消扫描时，UI 是否可能一直卡在运行中？
   - 已验证：`MusicAppControllerTest.scanStateSettlesWhenRunningScanCoroutineIsCancelledExternally`

5. 抽出工作流后，权限设置确认入口是否仍保持原行为？
   - 代码路径保留为门面委派，没有改动 `WaitingForPermission` 语义。
   - 任务七回归与总测试均通过，未发现权限确认分支回退。

## 剩余风险

- 当前 Gradle 构建会继续输出项目已有的 Kotlin Android Source Set 相关弃用警告，本任务未处理这些非功能性告警。
- `MusicAppControllerTest` 里已有两处 “No cast needed” 编译警告，本任务没有顺手清理，避免扩散到任务范围之外。
