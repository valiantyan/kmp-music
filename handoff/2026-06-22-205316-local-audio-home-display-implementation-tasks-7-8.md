# Handoff: 本地音频首页展示实现 Task 7-8

## Session Metadata
- Created: 2026-06-22 20:53:16
- Project: /Users/yanhao/Desktop/demo/kmp-music
- Branch: codex/local-audio-domain-boundary
- Session duration: about 1.5 hours

### Recent Commits (for context)
  - 6db6e0f 新增本地音乐二级页
  - 4aa3ee0 首页展示扫描本地歌曲预览
  - da6777a 让控制器消费本地曲库快照
  - 3be686b 接入本地音频扫描快照仓库
  - c0909b2 实现扫描结果曲库合并

## Handoff Chain

- **Continues from**: [2026-06-22-203556-local-audio-home-display-implementation-tasks-5-6.md](./2026-06-22-203556-local-audio-home-display-implementation-tasks-5-6.md)
  - Previous title: 本地音频首页展示实现 Task 5-6
- **Supersedes**: None

Read the previous Task 5-6 handoff first if you need controller/state history. This handoff captures the Task 7-8 UI wiring session after that point.

## Current State Summary

Tasks 7 and 8 from `docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md` are implemented, verified, and committed. The homepage now consumes snapshot-derived `libraryStats`, `recentSongs`, and `localSongPreview`; the old homepage mock recent builder is gone. The app also has a real secondary `本地音乐` page with `歌曲 / 专辑 / 歌手 / 来源` sections, and settings now links to the source/problem section instead of the old mock local-folder page. The next planned task is Task 9: make Search, Favorites, and Me explicitly use the same scanned `LibrarySnapshot` state, especially Me page stats.

## Codebase Understanding

## Architecture Overview

The local-audio display flow now reaches visible UI: `FakeLocalMusicScanner` -> `ScanLocalMusicUseCaseImpl` -> `MusicLibraryRepository` snapshot -> `MusicAppController.uiState` -> `HomeScreen` / `LocalMusicScreen`. Compose screens still receive data and callbacks from `MusicApp`; they do not call scanners, repositories, or use cases directly. `最近播放` remains separate from scanned songs and is rendered only from controller-derived playback history.

`SecondaryScreen.LocalMusic(initialSection)` is now a real route. It is classified as `AppChromeMode.SecondaryWithMiniPlayer`, so bottom tabs are hidden but mini-player remains available when a song is playing. The full local music page is intentionally commonMain-only and platform-neutral; Android/iOS/Desktop scanner implementations are still deferred.

## Critical Files

| File | Purpose | Relevance |
|------|---------|-----------|
| docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md | Active implementation checklist | Task 1-8 are locally checked complete; Task 9 is next. Directory remains untracked. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt | Homepage UI | Now renders real library stats, true recent playback empty state, and `本地歌曲` max-6 preview between `最近播放` and `本地专辑`. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt | Secondary local music page | New full list page with `歌曲 / 专辑 / 歌手 / 来源` tabs and empty states. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicSourceSection.kt | Source/problem section | Split from `LocalMusicScreen` to keep files focused; renders scan source summaries and failed item messages. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt | App shell and routing | Wires homepage scan button to `controller.scanLocalMusic(LocalMusicScanRequest.Refresh)` and routes `SecondaryScreen.LocalMusic` to `LocalMusicScreen`. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/UtilityScreens.kt | Settings/login utility screens | Old mock `LocalFolderScreen` was removed; settings row now opens local music source section. |
| composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt | Controller behavior tests | Added coverage for opening local music at the Sources section; existing tests continue to protect scan/recent separation. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt | State controller | Still the single bridge from scan repository snapshot to UI state; Task 9 should verify search uses this same repository/state. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/MeScreen.kt | My/profile page | Not yet updated in this session; Task 9 will pass real `LibraryStats` and favorite count. |

## Key Patterns Discovered

- Keep UI screens data-only: screen parameters receive lists/state/callbacks from `MusicApp`; screens never instantiate data-layer classes.
- Use `LibrarySnapshot`-derived `uiState.songs/albums/artists/libraryStats/localMusicSources/localMusicProblems` as the shared source for homepage and local-music page.
- Do not infer `recentSongs` from scanned songs. The homepage now shows `播放过的歌曲会出现在这里` when playback history is empty.
- Prefer splitting large UI pages by responsibility. `LocalMusicSourceSection.kt` was created because putting source/problem rendering into `LocalMusicScreen.kt` pushed the file beyond the project’s preferred small-file style.
- Plan checkboxes in `docs/superpowers/plans/` are local workflow state and remain untracked unless the user explicitly asks to commit docs.

## Work Completed

## Tasks Finished

- [x] Task 7: Homepage Local Songs Preview
  - Changed `HomeScreen` signature to accept `LibraryStats`, `LocalMusicScanState`, `recentSongs`, and `localSongPreview`.
  - Deleted `buildHomeRecentSongs`, removing the old mock recent-playback behavior.
  - Added homepage `本地歌曲` section between `最近播放` and `本地专辑`, shown only when preview data exists.
  - Made the homepage library card use real stats and scan-state-driven button label.
  - Wired homepage scan button through `rememberCoroutineScope` to `controller.scanLocalMusic(LocalMusicScanRequest.Refresh)`.
  - Commit: `4aa3ee0 首页展示扫描本地歌曲预览`.
- [x] Task 8: Secondary Local Music Page
  - Added `LocalMusicScreen` with `歌曲 / 专辑 / 歌手 / 来源` sections.
  - Added `LocalMusicSourceSection` for source summaries and scan problems.
  - Replaced the temporary `SecondaryScreen.LocalMusic` fallback with the real screen.
  - Removed old mock `LocalFolderScreen` and `FolderSummaryRow`.
  - Changed settings from `本地文件夹` mock row to `本地来源`, opening `LocalMusicSection.Sources`.
  - Added controller test for opening local music at the Sources section.
  - Commit: `6db6e0f 新增本地音乐二级页`.

## Files Modified

| File | Changes | Rationale |
|------|---------|-----------|
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt | Consumes real stats/recent/preview; renders empty recent state and `本地歌曲`; removes mock recent builder | Makes scanned songs visible on the homepage without polluting true playback history. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt | Adds coroutine scan callback; passes homepage state; routes LocalMusic to real screen; settings source row opens Sources section | Keeps scanner invocation in controller/app shell and connects navigation to new UI. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt | New secondary page with full songs, albums, artists, and tab state | Provides the `查看全部` destination requested by the product decision. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicSourceSection.kt | New source/problem rendering helpers | Keeps source/problem rendering focused and avoids a large LocalMusic screen file. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/UtilityScreens.kt | Adds `onLocalMusicSources`, updates settings copy, deletes mock local folder page | Removes stale mock local-folder UI in favor of real scanned source summary. |
| composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt | Adds `openLocalMusicCanStartAtSourcesSection` | Protects the settings/source-section route behavior. |
| docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md | Task 7-8 checkboxes and execution notes updated | Local execution log; currently untracked workflow artifact. |

## Decisions Made

| Decision | Options Considered | Rationale |
|----------|-------------------|-----------|
| Homepage scan button calls real controller scan | Keep old `openScan` dialog, or call `scanLocalMusic` | User asked how scanned playable audio appears in the app; homepage scan must populate the snapshot path, not the old simulated dialog. |
| `本地歌曲` only appears when preview list exists | Always show empty block, or hide until scan results exist | Keeps first screen clean before scan while still preserving `最近播放` empty state. |
| Full local music page is a secondary route | Put all songs on homepage, or use `SecondaryScreen.LocalMusic` | Matches approved UX: homepage max 6 preview plus full list via `查看全部`. |
| Split source/problem section into its own file | Keep all Task 8 UI in one file, or split by section responsibility | File stayed under the project’s preferred size and source/problem UI has distinct responsibility from list tabs. |
| Delete old mock `LocalFolderScreen` | Leave unused, or remove now | It was no longer routed and still encoded fake folders/imports; removing it prevents stale local-data semantics. |
| Add one route-focused test instead of UI rendering tests | Test Compose UI, or protect controller route state | Existing test setup is controller-focused; the key regression risk is wrong initial section/chrome state, which controller tests can cover cheaply. |

## Pending Work

## Immediate Next Steps

1. Continue with Task 9 in `docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md`: Search, Favorites, and Me Use the Same Snapshot.
2. Start Task 9 by adding the plan’s controller tests for `searchReadsScannedSnapshot` and `libraryStatsComeFromScannedSnapshot`, then run `./gradlew :composeApp:desktopTest`.
3. Update `MeScreen` to accept `libraryStats: LibraryStats` and `favoriteCount: Int`, then wire those from `MusicApp.RootScreen`.
4. Verify `SearchMusicUseCaseImpl` in `MusicAppController` uses the same `musicLibraryRepository` field that scanning populates.
5. Run `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest` and commit Task 9 with `统一搜索收藏我的页曲库来源`.

## Blockers/Open Questions

- [ ] No active blocker for Task 9.
- [ ] The old settings scan path still calls `controller.openScan`, which uses the legacy `ScanStatus` overlay. Homepage scan is now real. Decide in Task 10 or a follow-up whether settings should also call `scanLocalMusic`.
- [ ] Full visual QA on an Android device/emulator has not been done in this session; builds and shared tests pass.

## Deferred Items

- Task 9: Me page stats and search/favorites snapshot verification.
- Task 10: final verification, mock/seed leakage search, platform API boundary check, and spec status update.
- Real Android MediaStore scanner, iOS import, Desktop folder scanner, and real playback from `localUri` remain separate future plans.
- The current `FakeLocalMusicScanner` is still the only scanner feeding the UI.

## Context for Resuming Agent

## Important Context

The code is committed through Task 8 on branch `codex/local-audio-domain-boundary`. A new app still starts with no songs and no current playback. Pressing the homepage scan button now calls `controller.scanLocalMusic(LocalMusicScanRequest.Refresh)`, after which fake scanned songs populate `uiState.songs`, `localSongPreview`, albums, artists, source summaries, and the local music page.

The user’s core requirement is preserved: scanned audio is visible and playable through lists, but `最近播放` remains true playback history. Scanning alone must not fill `recentSongs`. The homepage order is now `本地音乐库卡片 -> 最近播放 -> 本地歌曲 -> 本地专辑`. The full local music route is `SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Songs)` from homepage and `LocalMusicSection.Sources` from settings.

The current git working tree has no uncommitted code changes after the Task 8 commit. It still shows untracked workflow docs: `docs/superpowers/plans/`, `handoff/2026-06-22-200955-local-audio-home-display-implementation-tasks-1-4.md`, `handoff/2026-06-22-203556-local-audio-home-display-implementation-tasks-5-6.md`, and this handoff file. Do not accidentally include them in code commits unless the user asks.

## Assumptions Made

- The fake scanner is acceptable for this implementation phase because platform scanners are explicitly deferred.
- `LocalMusicScreen` can be built in `commonMain` using existing reusable components and platform-neutral models.
- Settings “本地来源” should open the source/problem section rather than a mock file-folder list.
- UI rendering tests are not currently part of this repo’s common test pattern; controller tests are the primary regression harness for navigation/state semantics.
- Hiding the local-song homepage section before scan is acceptable; the recent-playback empty state remains visible.

## Potential Gotchas

- `SettingsScreen` now requires `onLocalMusicSources`; any future previews/call sites must pass it.
- `LocalMusicSection.label()` now exists privately in `LocalMusicScreen.kt`; do not rely on the old `MusicApp.kt` fallback helper because it was removed.
- `LocalMusicSourceSection.kt` exposes `internal fun SourceSection(...)` for the sibling screen file. Keep it package-internal unless it becomes shared elsewhere.
- `ScanStatus` legacy code still exists and is used by settings scan overlay. Task 10’s leakage search may flag it; treat that as an intentional remaining cleanup, not a surprise.
- `MeScreen` still has old hardcoded metrics until Task 9 updates it.
- If tests expect songs on startup, they must call `controller.scanLocalMusic(LocalMusicScanRequest.Refresh)` first.
- `python` command was unavailable in this environment; session-handoff scripts were run with `python3`.

## Environment State

## Tools/Services Used

- `session-handoff`: used to create this document.
- `writing-plans`: user-requested workflow source for Tasks 7 and 8.
- `executing-plans`: followed inline for Task 7 and Task 8 execution.
- `systematic-debugging`: used after a compile failure in Task 8.
- Kotlin skills loaded for Kotlin edits: `kotlin-basics`, `kotlin-architecture`, `kotlin-functions`, `kotlin-naming`, `kotlin-documentation`.
- Verification commands run successfully:
  - `./gradlew :composeApp:compileDebugKotlinAndroid`
  - `./gradlew :composeApp:desktopTest`
  - `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`
  - `git diff --check`

## Active Processes

- No dev server, emulator, watcher, or background Gradle task is required to continue.

## Environment Variables

- No task-specific environment variables are required.
- Do not record secret values in future handoffs.

## Related Resources

- docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md
- docs/superpowers/specs/2026-06-22-local-audio-home-display-design.md
- docs/LOCAL_AUDIO_DISCOVERY_PRD.md
- docs/PRD.md
- AGENTS.md
- handoff/2026-06-22-203556-local-audio-home-display-implementation-tasks-5-6.md
- handoff/2026-06-22-200955-local-audio-home-display-implementation-tasks-1-4.md
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicSourceSection.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/UtilityScreens.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/MeScreen.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt
- composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeLocalMusicScanner.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryMusicLibraryRepository.kt

---

**Security Reminder**: This handoff contains no credentials or secret values. Validate before final response.
