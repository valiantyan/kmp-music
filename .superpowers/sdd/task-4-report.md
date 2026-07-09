# Task 4 Report

## 实现内容

- 新增 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchResultController.kt`，把搜索结果派生从 `MusicAppController` 门面中拆出。
- `SearchResultController` 负责两条核心规则：
  - 只有 `searchQuery` 和 `activeSearchQuery` 去空白后完全一致且非空时，才派生搜索结果，避免 pending query 回退成全量曲库。
  - 按 `SearchContext` 选择数据源：本地库优先用 `state.localSongs`，否则回退仓库；收藏搜索只读取 `state.favoriteSongs`。
- `MusicAppController.search()` 已改为委派给 `SearchResultController.search(state = uiState)`，并删除门面里原有的搜索派生私有函数。
- 新增 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchResultControllerTest.kt`，覆盖 pending query 空结果和收藏搜索只读收藏投影。
- 更新 `MusicAppControllerTest`：
  - `nonBlankSearchQueryDoesNotCommitToHistoryWhenLeavingSearchBeforeDebounce` 改为 `runTest`，并在离开搜索页后推进防抖时间，验证历史不会被晚到任务污染。
  - `searchResultActionsCommitCurrentQueryToHistory` 改为从真实可见搜索结果里取目标对象后再执行动作。
- 更新计划文件 `docs/superpowers/plans/2026-07-08-music-app-controller-workflow-refactor.md`，将任务四步骤 1-7 勾选为已完成。

## 测试与命令结果

### RED 证据

命令：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.search.SearchResultControllerTest"
```

结果：

- 退出码：`1`
- 关键输出：

```text
e: .../SearchResultControllerTest.kt:21:26 Unresolved reference 'SearchResultController'.
e: .../SearchResultControllerTest.kt:45:26 Unresolved reference 'SearchResultController'.
```

- 同一次编译还暴露出 `MusicAppControllerTest` 需要补 `SearchResult` import，这一项已在实现阶段一并修正。

### GREEN 证据

命令：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.search.SearchResultControllerTest"
```

结果：

- 退出码：`0`
- 关键输出：

```text
> Task :composeApp:desktopTest
BUILD SUCCESSFUL in 8s
```

### 任务四验证命令

命令：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.search.SearchResultControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.pendingSearchQueryDoesNotReturnFullLibraryBeforeDebounce" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchResultActionsCommitCurrentQueryToHistory" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.nonSearchResultActionsDoNotCommitSearchHistory" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.nonBlankSearchQueryDoesNotCommitToHistoryWhenLeavingSearchBeforeDebounce"
```

结果：

- 退出码：`0`
- 关键输出：

```text
> Task :composeApp:desktopTest
BUILD SUCCESSFUL in 4s
```

### 过程中一次失败与修正

在第一次跑任务四回归集时，命令曾失败一次：

```text
6 tests completed, 1 failed
MusicAppControllerTest[desktop] > searchResultActionsCommitCurrentQueryToHistory[desktop] FAILED
java.util.NoSuchElementException at MusicAppControllerTest.kt:1547
```

原因是当前假数据里不存在单个查询词能同时命中“歌曲 + 专辑 + 歌手”三类可见结果，按 brief 原样使用 `"One Summer"` 会导致 `result.albums.first()` 或 `result.artists.first()` 取空。

修正后测试改为：

- 用 `"Dream Stories"` 从真实结果中取歌曲与专辑目标；
- 用 `"久石让"` 从真实结果中取歌手目标；
- 仍然验证“点击真实可见搜索结果时会先提交当前查询词到历史”这一行为。

## TDD 说明

- 已先写 `SearchResultControllerTest` 再跑红灯，确认新协作者确实不存在。
- 红灯通过后才新增 `SearchResultController` 实现并让 `MusicAppController` 委派。
- 门面回归测试在绿灯过程中根据当前假数据做了最小修正，保持测试仍绑定用户可见行为，而不是伪造目标对象。

## 变更文件

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchResultController.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchResultControllerTest.kt`
- `docs/superpowers/plans/2026-07-08-music-app-controller-workflow-refactor.md`
- `.superpowers/sdd/task-4-report.md`

## 自我审查

- 风险点 1：pending query 规则如果继续留在门面里，会和 `SearchSessionController` 的防抖发布时序分裂。当前已把派生规则收敛到 `SearchResultController`。
- 风险点 2：收藏搜索如果回退到仓库全量曲库，会泄漏未收藏歌曲。当前新增单测锁住了这一点。
- 风险点 3：离开搜索页后的晚到防抖任务可能污染历史。当前回归测试已推进虚拟时间并验证历史仍为空。
- 风险点 4：真实假数据与 brief 示例查询不完全匹配。当前测试已改为从各自真实可见结果中取目标，避免“测试写法看似真实、实际取空”的假绿。
- 风险点 5：门面公开方法签名可能被误改。当前 `MusicAppController.search()` 仍保持原签名不变。

## 关注点

- Gradle 运行仍会打印项目已有的 Kotlin/AGP 废弃属性告警：
  - `kotlin.mpp.androidGradlePluginCompatibility.nowarn`
  - `kotlin.mpp.androidSourceSetLayoutVersion`
- 这些告警与本任务无关，本次未处理。
