# Task 4 Report

## What I implemented

- Added `shouldShowTitlebarMusicSearch` to `MusicAppUiState` so desktop titlebar visibility is derived from shared navigation state instead of scattered UI checks.
- Added controller-level tests covering:
  - titlebar search visible only on Home/Favorites root pages
  - search screen hides titlebar search
- Updated `DesktopTitleBar` to accept `showSearch` and preserve layout width with a placeholder spacer when hidden.
- Updated desktop titlebar search routing to open:
  - `SearchContext.LocalLibrary` from `Home`
  - `SearchContext.Favorites` from `Favorites`
  - `SearchContext.LocalLibrary` fallback for other root tabs
- Updated desktop library sidebar search to stop routing to global search and remain a local filter placeholder.
- Updated sidebar copy/comment semantics from global search to local filter semantics:
  - `搜索本地库` -> `筛选本地库`

## What I tested and test results

1. Focused desktop tests for Task 4 visibility rules
   - Result: PASS after implementation
2. Android shared compile
   - Result: PASS

## TDD Evidence

### RED command/output summary

Command:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.titlebarSearchOnlyShowsOnHomeAndFavoritesRootPages" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchScreenHidesTitlebarSearch"
```

Summary:

- Build failed in `:composeApp:compileTestKotlinDesktop`
- Cause: unresolved reference `shouldShowTitlebarMusicSearch` in `MusicAppControllerTest.kt`

### GREEN command/output summary

Command:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.titlebarSearchOnlyShowsOnHomeAndFavoritesRootPages" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchScreenHidesTitlebarSearch"
```

Summary:

- `BUILD SUCCESSFUL`
- Focused tests passed

Additional verification:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Summary:

- `BUILD SUCCESSFUL`
- Android/common compile remained healthy

## Files changed

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopLibrarySidebar.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`
- `.superpowers/sdd/task-4-report.md`

## Self-review findings

- Visibility rule is now centralized in shared UI state, which avoids patching desktop-only route checks in multiple composables.
- Titlebar routing now follows active root tab explicitly, which matches Tasks 1-3 contextual search model.
- Sidebar search no longer opens the global search page, preserving Task 5 space for future local filtering behavior.
- Layout stability is preserved when titlebar search is hidden by keeping the same width with a spacer.

## Any issues or concerns

- Existing Gradle output still contains unrelated warnings about deprecated Kotlin/AGP properties and two `PlaybackCoordinator.kt` Elvis warnings; they were pre-existing and did not block this task.
