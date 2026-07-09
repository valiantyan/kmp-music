# 任务五报告：拆出内容导航工作流

## 实现摘要

- 新增 `ContentNavigationController`，把首页内容分段、本地音乐、扫描页、最近播放、专辑详情、歌手详情以及按需加载完整曲库的逻辑从 `MusicAppController` 中拆出。
- `MusicAppController` 保持唯一公开门面，新增 `applyContentNavigationResult()` 统一接收内容导航结果，并仅在真实补齐完整曲库后触发待恢复播放快照。
- 搜索结果动作前的搜索历史提交继续保留在门面；`openSearch()` 与 `openPlayer()` 没有迁入内容导航控制器。
- 新增 `ContentNavigationControllerTest`，覆盖本地音乐入口、我的页歌曲统计入口、扫描页/最近播放入口、专辑/歌手详情入口以及歌曲详情跳转的元数据匹配行为。
- 已把 `docs/superpowers/plans/2026-07-08-music-app-controller-workflow-refactor.md` 中任务五全部步骤更新为完成状态。

## TDD RED/GREEN 证据

### RED

命令：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.ContentNavigationControllerTest"
```

结果：失败。

关键信息：

```text
Unresolved reference 'ContentNavigationController'
```

说明：先创建测试文件，再运行定向测试，确认红灯确实来自缺失的协作者类型与方法，而不是测试本身的拼写或依赖错误。

### GREEN

命令：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.ContentNavigationControllerTest"
```

结果：通过，`BUILD SUCCESSFUL`。

说明：补齐 `ContentNavigationController` 与门面委派后，新增聚焦测试全部转绿。

## 验证命令与结果

1. 内容导航聚焦测试

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.ContentNavigationControllerTest"
```

结果：通过，`BUILD SUCCESSFUL`。

2. brief 指定的任务五导航回归

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.ContentNavigationControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.openLocalMusicUsesSecondaryFixedBarMode" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.openAudioScanUsesDedicatedScanRoute" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.meViewAllRecentPlayedOpensRecentPageAndReturnsToMe" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.openArtistFromSongUsesNormalizedArtistName" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchResultActionsCommitCurrentQueryToHistory" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.nonSearchResultActionsDoNotCommitSearchHistory"
```

结果：通过，`BUILD SUCCESSFUL`。

3. Android Kotlin 编译

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

结果：通过，`BUILD SUCCESSFUL`。

## 对抗式审查

1. 风险点：会不会把搜索结果动作前的历史提交也挪进新控制器，破坏门面时序？
结论：没有。`openAlbum()` 和 `openArtist()` 仍先在 `MusicAppController` 中调用 `commitSearchQueryForResultActionIfNeeded()`，然后才委派给 `ContentNavigationController`。

2. 风险点：`openSearch()` 或 `openPlayer()` 会不会被顺手塞进内容导航控制器，越界实现后续任务？
结论：没有。两个入口仍留在 `MusicAppController`，内容导航控制器只承接 brief 指定的方法集合。

3. 风险点：首次预热完整曲库后恢复播放快照的语义会不会被改坏，尤其是空曲库场景？
结论：已修正。`ContentNavigationController.loadLocalMusicLibrary()` 只有在 `nextState.localSongs.isNotEmpty()` 时才把 `loadedFullLibrary` 置为 `true`，与原门面“真实拿到完整歌曲列表后再恢复”的行为保持一致。

4. 风险点：扫描页和最近播放页只是纯导航入口，会不会误触发全量曲库读取？
结论：已由新增测试和定向回归覆盖，两个入口都返回 `loadedFullLibrary = false`，且保持 `localSongs` 为空。

5. 风险点：从歌曲打开专辑/歌手详情时，元数据匹配和关闭 `moreSongId` 会不会在拆分后丢失？
结论：已保留，并由 `songDetailRoutesMatchMetadataAndCloseMoreMenu()` 与现有 `openArtistFromSongUsesNormalizedArtistName()` 共同覆盖。

## 文件变更清单

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/navigation/ContentNavigationController.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/ContentNavigationControllerTest.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- `docs/superpowers/plans/2026-07-08-music-app-controller-workflow-refactor.md`

## 剩余风险或无

- 无阻塞性剩余风险。
- 已知非阻塞噪音：Gradle 仍会打印 `kotlin.mpp.androidGradlePluginCompatibility.nowarn` 和 `kotlin.mpp.androidSourceSetLayoutVersion` 的废弃告警，本任务未处理这类构建配置问题。

---

## 任务五复审修复追加记录（2026-07-09）

### 修复摘要

- 将 `ContentNavigationController` 与其 `Result` 类型收紧为 `internal`，继续保持 `MusicAppController` 作为内容导航工作流的唯一公开门面。
- 在 `MusicAppController.openAlbumFromSong()` 与 `MusicAppController.openArtistFromSong()` 入口先执行 `commitSearchQueryForResultActionIfNeeded()`，再委派给内容导航控制器，补回“搜索结果页歌曲更多菜单进入专辑/歌手详情”遗漏的搜索历史提交时序。

### 新增/更新测试

- 更新 `MusicAppControllerTest.searchResultActionsCommitCurrentQueryToHistory()`：
  - 保留歌曲播放、专辑详情、歌手详情的既有断言；
  - 新增搜索结果页歌曲更多菜单进入“查看专辑 / 查看歌手”都会写入当前搜索词的回归断言；
  - 维持现有搜索历史“命中旧词时前移去重”的既有产品语义。

### 验证命令与结果

1. TDD 红灯

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchResultActionsCommitCurrentQueryToHistory"
```

结果：失败，新增断言命中遗漏的搜索历史提交边界。

2. 新增测试转绿

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchResultActionsCommitCurrentQueryToHistory"
```

结果：通过，`BUILD SUCCESSFUL`。

3. 任务五定向回归

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.ContentNavigationControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchResultActionsCommitCurrentQueryToHistory" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.nonSearchResultActionsDoNotCommitSearchHistory" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.openLocalMusicUsesSecondaryFixedBarMode" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.openAudioScanUsesDedicatedScanRoute" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.meViewAllRecentPlayedOpensRecentPageAndReturnsToMe" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.openArtistFromSongUsesNormalizedArtistName"
```

结果：通过，`BUILD SUCCESSFUL`。

4. Android Kotlin 编译

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

结果：通过，`BUILD SUCCESSFUL`。

### 文件变更

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/navigation/ContentNavigationController.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`
- `.superpowers/sdd/2026-07-08-music-app-controller-workflow-refactor-task-5-report.md`
