# Task 2 Report: Context-Specific Search Results

## What I implemented

- 在 `MusicAppController.search()` 中按 `uiState.searchContext` 选择搜索源：
  - `SearchContext.LocalLibrary` 优先使用已加载的 `uiState.localSongs`，否则回退到 `musicLibraryRepository.getAllAvailableSongs()`
  - `SearchContext.Favorites` 只使用 `uiState.favoriteSongs`
- 将 `SearchMusicUseCase.kt` 中的 `buildSearchResult(...)` 从 `internal` 调整为公开函数，供控制器跨包复用同一套搜索聚合规则
- 在 `MusicAppControllerTest.kt` 中新增本地曲库搜索与收藏搜索的上下文隔离测试，并保留现有 `searchScopeLimitsResultTypes` 作为回归覆盖
- 移除了控制器中已不再使用的 `searchMusicUseCase` 注入路径，避免留下双搜索实现入口

## What I tested and test results

- RED 验证命令：
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.localLibrarySearchReturnsNonFavoriteLocalSongs" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.favoritesSearchOnlyReturnsFavoriteSongs"`
  - 结果：失败，`favoritesSearchOnlyReturnsFavoriteSongs` 断言失败，说明收藏搜索仍错误命中了完整本地曲库
- GREEN 验证命令：
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.localLibrarySearchReturnsNonFavoriteLocalSongs" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.favoritesSearchOnlyReturnsFavoriteSongs" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchScopeLimitsResultTypes"`
  - 结果：全部通过，3 条目标测试通过

## TDD Evidence

### RED command/output summary

- 命令：
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.localLibrarySearchReturnsNonFavoriteLocalSongs" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.favoritesSearchOnlyReturnsFavoriteSongs"`
- 摘要：
  - `2 tests completed, 1 failed`
  - 失败用例：`MusicAppControllerTest[desktop] > favoritesSearchOnlyReturnsFavoriteSongs[desktop] FAILED`
  - 失败原因：收藏搜索仍使用完整本地曲库作为结果源，未按 `searchContext` 隔离

### GREEN command/output summary

- 命令：
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.localLibrarySearchReturnsNonFavoriteLocalSongs" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.favoritesSearchOnlyReturnsFavoriteSongs" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchScopeLimitsResultTypes"`
- 摘要：
  - `BUILD SUCCESSFUL`
  - 3 条目标测试均通过

## Files changed

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/SearchMusicUseCase.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`
- `.superpowers/sdd/task-2-report.md`

## Self-review findings

- 搜索结果聚合规则仍只保留一份，控制器与用例共用 `buildSearchResult(...)`，避免不同入口出现结果口径分叉
- `LocalLibrary` 场景保留“已加载列表优先、仓库兜底”的既有性能语义，没有强行改变扫描与完整曲库加载的边界
- `Favorites` 场景直接以 `uiState.favoriteSongs` 为唯一搜索源，满足入口隔离要求，也不会误把未收藏本地歌曲带入结果
- 已清理控制器里的废弃搜索用例引用，避免未来维护时误以为存在第二条有效搜索路径

## Any issues or concerns

- 目标测试通过，但 Gradle 输出里仍有与本任务无关的既有 warning：
  - Kotlin MPP/AGP deprecated property 警告
  - `PlaybackCoordinator.kt` 中 Elvis operator 警告
  - 其他测试文件中的轻量 warning
- 本次未修改 `docs/superpowers` 下的 plan/spec 文件，符合任务约束
