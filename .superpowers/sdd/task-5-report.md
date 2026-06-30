# Task 5 Report: Extract FavoriteStateSynchronizer

## What I implemented

- Added `FavoriteStateSynchronizer` at `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/favorites/FavoriteStateSynchronizer.kt`.
- Moved favorite toggle projection out of `MusicAppController.toggleFavorite` so the controller now delegates favorite state synchronization and remains the public facade plus sole Compose `mutableStateOf` owner.
- Wired `FavoriteStateSynchronizer` in `MusicAppController` using existing `LibraryStateSynchronizer.buildFavoriteSongs` and `buildRecentSongs` rules.
- Added focused tests in `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/favorites/MusicAppFavoriteStateSynchronizerTest.kt` covering:
  - sync of `homeLocalSongPreview`
  - sync of `localSongs`
  - sync of `queueSongsSnapshot`
  - sync of `favoriteSongs`
  - sync of `recentSongs`
  - sync of derived `currentSong`
  - preservation of favorite album/artist projection through `MusicLibraryProjector`
- Updated Task 5 step checkboxes in `docs/superpowers/plans/2026-06-30-codebase-architecture-optimization-phase1.md`.

## RED test evidence

Command:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.favorites.MusicAppFavoriteStateSynchronizerTest"
```

Relevant output:

```text
> Task :composeApp:compileTestKotlinDesktop FAILED
e: .../MusicAppFavoriteStateSynchronizerTest.kt:21:28 Unresolved reference 'FavoriteStateSynchronizer'.

FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':composeApp:compileTestKotlinDesktop'.
```

Result:

- RED confirmed. The expected missing `FavoriteStateSynchronizer` failure occurred before implementation.

## GREEN test evidence

Command:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.favorites.MusicAppFavoriteStateSynchronizerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.toggleFavoriteSyncsSongList" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.toggleCurrentSongFavoriteUsesSharedControllerEntry"
```

Relevant output:

```text
> Task :composeApp:desktopTest

BUILD SUCCESSFUL in 8s
18 actionable tasks: 8 executed, 10 up-to-date
```

Notes:

- The build emitted pre-existing warnings outside Task 5 scope, including two Elvis-operator warnings in `PlaybackCoordinator.kt` and existing test warnings in other files.
- Targeted Task 5 tests passed.

## Files changed

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/favorites/FavoriteStateSynchronizer.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/favorites/MusicAppFavoriteStateSynchronizerTest.kt`
- `docs/superpowers/plans/2026-06-30-codebase-architecture-optimization-phase1.md`
- `.superpowers/sdd/task-5-report.md`

## Commit

- 提交主题：`重构收藏状态同步`

## Self-review findings

- The extraction follows the existing phase-1 collaborator pattern used by `LibraryStateSynchronizer` and keeps `MusicAppController` as the only mutable UI state owner.
- Favorite toggling still flows through `ToggleFavoriteUseCase`, so repository truth remains centralized.
- Favorite list reconstruction still reuses `LibraryStateSynchronizer.buildFavoriteSongs`, avoiding duplicated entity resolution logic.
- Recent playback reconstruction still reuses the shared playback-history rule via `LibraryStateSynchronizer.buildRecentSongs`.
- `currentSong` stays correct because it is derived from synchronized collections on `MusicAppUiState`, so no extra mutable state was introduced.
- Test fixtures were adapted to the current `Song` and `CoverArt` model in this checkout while preserving the brief's intended assertions.

## Concerns

- No functional concerns for Task 5.
- The brief sample test fixture did not match the current repository exactly because `Song` now requires additional fields and `CoverArt.Generated` does not exist in this checkout. I adapted the fixture to the live model without changing the intended behavior under test.
