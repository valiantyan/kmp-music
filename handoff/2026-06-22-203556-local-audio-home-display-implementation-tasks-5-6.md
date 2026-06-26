# Handoff: 本地音频首页展示实现 Task 5-6

## Session Metadata
- Created: 2026-06-22 20:35:56
- Project: /Users/yanhao/Desktop/demo/kmp-music
- Branch: codex/local-audio-domain-boundary
- Session duration: about 1.5 hours

### Recent Commits (for context)
  - da6777a 让控制器消费本地曲库快照
  - 3be686b 接入本地音频扫描快照仓库
  - c0909b2 实现扫描结果曲库合并
  - e647bd1 扩展歌曲真实来源字段
  - be61b18 定义本地音频扫描领域模型

## Handoff Chain

- **Continues from**: [2026-06-22-200955-local-audio-home-display-implementation-tasks-1-4.md](./2026-06-22-200955-local-audio-home-display-implementation-tasks-1-4.md)
  - Previous title: 本地音频首页展示实现 Task 1-4
- **Supersedes**: None

Read the previous handoff first for Task 1-4 domain/data context. This handoff captures the Task 5-6 implementation session after that point.

## Current State Summary

Tasks 5 and 6 from `docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md` are implemented, verified, and committed together in `da6777a 让控制器消费本地曲库快照`. The app controller now initializes from a `LibrarySnapshot`, starts with an empty in-memory library by default, scans fake local audio through `ScanLocalMusicUseCase`, syncs snapshot-derived UI state, and keeps true recent playback separate from scanned songs. The homepage UI still has not been updated to render `本地歌曲`; that begins at Task 7.

## Codebase Understanding

## Architecture Overview

The shared local-audio slice now flows through: `LocalMusicScanner` -> `ScanLocalMusicUseCaseImpl` -> `MusicLibraryRepository.applyScanResult()` -> `LibrarySnapshot` -> `MusicAppController.uiState`. The controller is the only bridge between domain snapshot state and Compose UI state. Playback history is stored in `PlaybackRepository` separately from the library snapshot, so scanned songs do not automatically become `最近播放`.

`MusicAppController` now defaults to `InMemoryMusicLibraryRepository()` and `FakeLocalMusicScanner()`. This means a new controller starts with an empty library until `scanLocalMusic()` is called. Tests that interact with songs now explicitly scan first and then select songs from `uiState.songs`.

## Critical Files

| File | Purpose | Relevance |
|------|---------|-----------|
| docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md | Active task checklist | Task 1-6 checkboxes are locally updated; Task 7 is next. This directory remains untracked. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt | Playback state, queue, history models | `PlaybackState.currentSongId` is nullable and `PlaybackHistory` exists. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/PlaybackRepository.kt | Playback repository boundary | Added `getPlaybackHistory()` and `savePlaybackHistory()`. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryPlaybackRepository.kt | In-memory playback state | Starts with no current song, no queue, and empty playback history. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/PlaybackUseCases.kt | Playback use cases | `PlaySongUseCaseImpl` records history; `MoveQueueUseCase` no longer accepts full-library fallback. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt | App UI state and navigation models | Added `LocalMusicSection`, `SecondaryScreen.LocalMusic`, nullable `currentSong`, nullable detail targets, and snapshot-derived fields. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt | Main app state controller | Now consumes `LibrarySnapshot`, exposes `scanLocalMusic()` and `openLocalMusic()`, and derives `recentSongs` from playback history. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt | App shell and route rendering | Handles nullable current song and missing library items; routes `LocalMusic` to a temporary fallback until Task 8. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/DetailScreens.kt | Album/artist detail screens | Added `MissingLibraryItemScreen`; `currentSongId` is nullable. |
| composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt | Controller behavior tests | Covers empty initial state, scan preview count, recent playback separation, local music route, queue rules, search, and navigation. |

## Key Patterns Discovered

- Keep domain/data/feature boundaries intact. UI never calls scanner or repository directly; controller calls use cases.
- Use `LibrarySnapshot` as the single source for songs, albums, artists, stats, source summaries, scan state, and problems.
- Use `PlaybackHistory` as the only source for `recentSongs`; never infer recent playback from `snapshot.songs`.
- Controller tests should call `controller.scanLocalMusic(LocalMusicScanRequest.Refresh)` before expecting songs in `uiState`.
- `ScanStatus` still exists as a temporary compatibility path for the old scan dialog. New scan state is `LocalMusicScanState`; later tasks should phase the old dialog path out carefully.
- Plan checkboxes are part of the local workflow and are intentionally untracked so far.

## Work Completed

## Tasks Finished

- [x] Task 5: Playback History Is Separate From Scanned Songs
  - Added `PlaybackHistory`.
  - Made `PlaybackState.currentSongId` nullable.
  - Removed default current song and default queue from `InMemoryPlaybackRepository`.
  - Recorded history only in `PlaySongUseCaseImpl`.
  - Removed `fallbackSongs` from queue movement, so empty queues and missing current songs preserve state.
- [x] Task 6: Controller State From LibrarySnapshot
  - Added `LocalMusicSection` and `SecondaryScreen.LocalMusic`.
  - Added snapshot-derived fields to `MusicAppUiState`.
  - Switched controller default repository to `InMemoryMusicLibraryRepository`.
  - Added `scanLocalMusic()` and `openLocalMusic()`.
  - Synced snapshot state into songs/albums/artists/stats/sources/problems/preview.
  - Added missing-item fallback UI for empty or stale detail routes.
  - Updated and expanded controller tests.

## Files Modified

| File | Changes | Rationale |
|------|---------|-----------|
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt | `currentSongId` nullable; default playback/queue state; added `PlaybackHistory` | Represents "no playback yet" and separates true history from scanned songs. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/PlaybackRepository.kt | Added playback history read/write methods | Gives use cases a shared source for true recent playback. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryPlaybackRepository.kt | Removed seed playback defaults; added history storage | Prevents app launch or scan from pretending something has been played. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/PlaybackUseCases.kt | `PlaySongUseCaseImpl` writes history; `MoveQueueUseCase` only uses explicit queue | Prevents scanned list from becoming implicit queue/recent playback. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt | Added local music route/section and snapshot-derived UI fields | Enables controller and UI to represent local scan results explicitly. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt | Switched to snapshot source, added scan/navigation methods, derives recent/preview | Connects fake scanned data into app state without platform APIs. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt | Handles nullable current song and missing detail targets; routes local music to fallback | Makes empty library safe until dedicated screens are implemented. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/DetailScreens.kt | Added `MissingLibraryItemScreen`; nullable current song ids | Avoids crashes when detail target does not exist in an empty library. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt | `currentSongId` parameter nullable | Supports no-current-song state. Task 7 still needs real homepage data rendering. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/SearchScreen.kt | `currentSongId` parameter nullable | Supports no-current-song state. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/FavoritesScreen.kt | `currentSongId` parameter nullable | Supports no-current-song state. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/UtilityScreens.kt | `currentSongId` parameter nullable | Supports no-current-song state in existing local folder fallback screen. |
| composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt | Added/updated controller tests around scan, recent playback, local route, queue, and empty startup | Protects the user-visible semantics requested in the spec. |

## Decisions Made

| Decision | Options Considered | Rationale |
|----------|-------------------|-----------|
| Commit Task 5 and Task 6 together | Separate commits, or one combined commit | The plan explicitly linked Task 5 controller tests to Task 6 fields; combined commit keeps the tested behavior atomic. |
| Default controller uses empty in-memory library | Keep `SeedMusicLibraryRepository`, or switch to `InMemoryMusicLibraryRepository` | The confirmed product path says scanned data should be the displayed source; empty before scan avoids mock data pretending to be real. |
| Keep old `ScanStatus` compatibility for now | Remove old scan dialog immediately, or keep until UI tasks | Removing it would expand Task 6 into UI overlay work. Task 7/8 can replace scan UI intentionally. |
| Route `SecondaryScreen.LocalMusic` to fallback UI temporarily | Build full local music page now, or route to fallback until Task 8 | Full page is Task 8. The route and chrome semantics are enough for Task 6. |
| Hide mini-player when no current song exists | Keep first-song fallback, or make current song nullable | First-song fallback recreated the mock/recent-playback bug. Nullable state is the truthful model. |
| Recent playback derives from `PlaybackHistory` only | Use `songs.take(2)`, `lastPlayed`, or playback history | User explicitly asked whether `最近播放` still exists; it must exist but remain true history, not scan output. |

## Pending Work

## Immediate Next Steps

1. Continue with Task 7: Homepage Local Songs Preview in `docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md`.
2. Update `HomeScreen` to accept `libraryStats`, `scanState`, `recentSongs`, and `localSongPreview`; delete `buildHomeRecentSongs`.
3. Wire `MusicApp` with `rememberCoroutineScope` and call `controller.scanLocalMusic(LocalMusicScanRequest.Refresh)` from the homepage scan action.
4. Run `./gradlew :composeApp:desktopTest` and `./gradlew :composeApp:compileDebugKotlinAndroid`.
5. Commit Task 7 with a concise Chinese message if the plan says to commit at that point.

## Blockers/Open Questions

- [ ] No active blocker for Task 7.
- [ ] Product nuance for later: the old scan dialog still uses `ScanStatus` and mock copy. Decide in Task 7/8 whether the scan button should immediately run the fake scanner or continue showing a dialog.
- [ ] LocalMusic full-page behavior is still deferred. `SecondaryScreen.LocalMusic` currently routes to fallback text, not a real list screen.

## Deferred Items

- Homepage `本地歌曲` visual section is Task 7.
- Secondary `本地音乐` full list with `歌曲 / 专辑 / 歌手 / 来源` is Task 8.
- Favorites and Me page real count refinements are later tasks in the same plan.
- Android MediaStore scanner, iOS Files import, Desktop folder scanner, and real `localUri` playback are separate follow-up plans.

## Context for Resuming Agent

## Important Context

The code is cleanly committed through Task 6 on branch `codex/local-audio-domain-boundary`. The current `MusicAppController()` starts with an empty library and no current song. To see fake scanned audio in state, call:

```kotlin
controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
```

After scanning, `uiState.songs.size == 8`, `uiState.localSongPreview.size == 6`, and `uiState.recentSongs` remains empty until `controller.playSong(song)` is called. This is intentional and central to the user's requirement.

The current homepage still ignores `uiState.recentSongs` and `uiState.localSongPreview`; `HomeScreen` still constructs mock recent songs internally. Task 7 must remove that by changing the `HomeScreen` signature and making `MusicApp.RootScreen` pass controller-derived state.

The plan file `docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md` is untracked but locally updated through Task 6. Do not assume those checkbox updates are committed elsewhere.

## Assumptions Made

- The common fake scanner is acceptable until platform scanner plans are implemented.
- Scanning is a full refresh into `InMemoryMusicLibraryRepository`; incremental partial scan semantics are still out of scope.
- `最近播放` should show no rows until a user plays a song, even if scan results exist.
- `LocalMusicSection` route is enough for Task 6; real LocalMusic screen comes later.
- `ScanStatus` can remain temporarily as compatibility, but new data-driven state should use `LocalMusicScanState`.

## Potential Gotchas

- Tests that expect songs must scan first because the default repository is now empty.
- `MusicApp.kt` currently maps `SecondaryScreen.LocalMusic` to `MissingLibraryItemScreen`; this is temporary and should be replaced in Task 8.
- `HomeScreen` still hardcodes stats and uses `buildHomeRecentSongs`; Task 7 must replace both or the UI will still appear mock-driven.
- `BottomChrome` already accepts `Song?` and hides mini-player when no current song exists. Task 7's step about nullable bottom chrome is already mostly complete; verify rather than duplicating.
- `SeedMusicLibraryRepository` still exists for compatibility and tests, but production controller default no longer uses it.
- Plan and handoff docs are untracked; avoid accidentally committing old handoff files unless the user asks.

## Environment State

## Tools/Services Used

- `writing-plans`: used as the implementation plan source.
- `executing-plans`: followed inline for Task 5 and Task 6 execution.
- Kotlin skills: basics, architecture, data-classes, interface, functions, naming, documentation.
- `session-handoff`: used to create this handoff.
- Verification commands run successfully before this handoff:
  - `./gradlew :composeApp:desktopTest`
  - `./gradlew :composeApp:compileDebugKotlinAndroid`
  - `git diff --check`

## Active Processes

- No dev server, emulator, Gradle daemon interaction, or watcher is required to continue.

## Environment Variables

- No task-specific environment variables are required.
- Do not record secret values in future handoffs.

## Related Resources

- docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md
- docs/superpowers/specs/2026-06-22-local-audio-home-display-design.md
- handoff/2026-06-22-200955-local-audio-home-display-implementation-tasks-1-4.md
- handoff/2026-06-22-180512-local-audio-home-display-design.md
- docs/LOCAL_AUDIO_DISCOVERY_PRD.md
- docs/PRD.md
- AGENTS.md
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/DetailScreens.kt
- composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/PlaybackRepository.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/PlaybackUseCases.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryPlaybackRepository.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryMusicLibraryRepository.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeLocalMusicScanner.kt

---

**Security Reminder**: This handoff contains no credentials or secret values. Validate before final response.
