# Handoff: 本地音乐二级页卡顿根治修复

## Session Metadata
- Created: 2026-06-23 12:31:44
- Project: /Users/yanhao/Desktop/demo/kmp-music
- Branch: codex/local-audio-domain-boundary
- Session duration: about 2 hours

### Recent Commits (for context)
  - be57b73 修复 Android 音频权限引导
  - d96ef16 完成本地音频首页最终核验
  - 99cd581 统一搜索收藏我的页曲库来源
  - 6db6e0f 新增本地音乐二级页
  - 4aa3ee0 首页展示扫描本地歌曲预览

## Handoff Chain

- **Continues from**: [2026-06-22-205316-local-audio-home-display-implementation-tasks-7-8.md](./2026-06-22-205316-local-audio-home-display-implementation-tasks-7-8.md)
  - Previous title: 本地音频首页展示实现 Task 7-8
- **Supersedes**: None

## Current State Summary

This session investigated and fixed the reported performance bug: after scanning local music, entering the secondary Local Music page was very slow. Root-cause investigation and true-device profiling showed MediaStore scanning was fast, while the Local Music page was doing too much main-thread Compose work by rendering the entire scanned library inside a non-lazy `Column` under the app-wide `verticalScroll`. The fix is implemented but not committed: Local Music now owns a `LazyColumn`, and `MusicApp` skips the global `verticalScroll` only for this page so the full library remains available without composing every row at entry.

## Codebase Understanding

## Architecture Overview

The app keeps shared state in `MusicAppController`, renders common Compose UI from `commonMain`, and injects Android-specific scanning through `AndroidMediaStoreScanner` in `androidMain`. Scanning flows from Android MediaStore to `LocalMusicScanResult`, then through `ScanLocalMusicUseCase` and `InMemoryMusicLibraryRepository` into a `LibrarySnapshot`. UI pages consume `MusicAppUiState`; Home intentionally shows a bounded `localSongPreview`, while Local Music is the full-library secondary page. The global chrome and bottom padding are managed in `MusicApp.kt`, so changing scroll ownership there is the correct place to avoid nested or unbounded vertical scrolling.

## Critical Files

| File | Purpose | Relevance |
|------|---------|-----------|
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt` | Shared Compose app shell, page routing, bottom chrome, and page padding | Modified to let `SecondaryScreen.LocalMusic` bypass app-wide `verticalScroll` and receive `PaddingValues` directly |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt` | Full local library secondary screen with songs/albums/artists/sources sections | Modified from eager `Column.forEach` rendering to keyed `LazyColumn` items |
| `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data/AndroidMediaStoreScanner.kt` | Android MediaStore scanner | Profiling proved this was not the bottleneck: 111 songs scanned in 17-34ms |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CommonComponents.kt` | Shared `SongRow`, `AlbumCard`, and `ArtistRow` components | Important because every eager row included images, shadows, text, and buttons; lazy rendering avoids composing all rows at once |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt` | Home page with local library card and bounded song preview | Working comparison: Home only displays a small preview, so it does not expose the full-list performance issue |

## Key Patterns Discovered

- Use shared tokens and chrome policy from `MusicApp.kt`; do not duplicate bottom padding or mini-player avoidance inside individual screens.
- Large or unbounded vertical content should own a lazy scroll container instead of being nested under app-wide `verticalScroll`.
- Keep full data in state; do not hide performance problems by truncating `state.songs`.
- Preserve stable keys for full-library items: song ID, artist ID, and album row IDs.
- Existing tests primarily cover controller/navigation/domain behavior; this performance fix is best verified through Android compile plus true-device `adb` profiling.

## Work Completed

## Tasks Finished

- [x] Reproduced and measured the bug on a connected Android 12 device.
- [x] Identified root cause with `systematic-debugging`: MediaStore scan was fast, Local Music page entry was slow due to eager full-list composition.
- [x] Refactored `MusicApp.kt` so Local Music is the only page bypassing app-wide `verticalScroll`.
- [x] Refactored `LocalMusicScreen.kt` to use one `LazyColumn` containing header, tabs, and the active section.
- [x] Added keyed lazy items for songs, album rows, and artists.
- [x] Verified Android compile, desktop shared tests, debug install, and true-device performance improvement.

## Files Modified

| File | Changes | Rationale |
|------|---------|-----------|
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt` | Created `pagePadding`; routes `SecondaryScreen.LocalMusic` directly to `LocalMusicScreen` with `Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()` and `contentPadding`; all other pages keep the existing `Column.verticalScroll` path | Prevents `LazyColumn` from being measured inside an unbounded parent while preserving existing chrome/padding behavior for other pages |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt` | Replaced eager `Column` sections with a page-level `LazyColumn`; converted `SongSection`, `AlbumSection`, and `ArtistSection` into `LazyListScope` item builders; added stable item keys | Ensures entering the page only composes visible rows, removing the main-thread spike without truncating the library |

## Decisions Made

| Decision | Options Considered | Rationale |
|----------|-------------------|-----------|
| Use `LazyColumn` in `LocalMusicScreen` | Keep eager `Column`; truncate to first N songs; implement `LazyColumn` | Truncation would be a symptom patch and would break the full-library contract; eager rendering caused the measured slow frame |
| Special-case Local Music scroll ownership in `MusicApp` | Replace app-wide scrolling for every page; allow nested lazy list; special-case Local Music | Broad replacement risks unrelated pages; nested lazy scrolling would be wrong; a targeted route-level branch fixes the known large page |
| Keep domain/data untouched | Change scanner paging; change repository snapshots; change UI only | Scanner was measured at 17-34ms for 111 songs, so changing data flow would not address the confirmed bottleneck |
| Use keyed lazy rows | No keys; key by index; key by stable IDs | Stable IDs reduce recomposition churn when playback/current-song state changes or sections switch |

## Pending Work

## Immediate Next Steps

1. Review the uncommitted diff in `MusicApp.kt` and `LocalMusicScreen.kt` for style and product fit.
2. If acceptable, stage and commit with a concise Chinese commit message, for example: `优化本地音乐页列表性能`.
3. Optionally rerun true-device verification after commit if more local songs are available, especially libraries much larger than 111 songs.

## Blockers/Open Questions

- [ ] No functional blocker. Open question: whether the user wants this committed now or combined with any follow-up visual/performance cleanup.

## Deferred Items

- Full macrobenchmark or automated Compose performance test was deferred because this project currently has shared logic tests but no Android performance test harness.
- Applying the same scroll-ownership pattern to Favorites/Search/Detail pages was deferred because the reported bug and measured bottleneck were specifically Local Music page entry.
- Further scroll-smoothness tuning for continuous list scrolling was deferred; entry jank was the targeted bug and is substantially improved.

## Context for Resuming Agent

## Important Context

Before the fix, true-device logs showed MediaStore was not slow: Android scanner found 111 songs in about 34ms on the first run and about 17ms after the fix. The old Local Music page entry produced `Skipped 108 frames`, with `Davey! duration=1571ms` and another `1213ms` frame. After the lazy-list fix, a focused ADB tap from Home into Local Music produced no `Skipped frames` or `Davey` logcat output. `gfxinfo` for that entry segment showed 29 rendered frames, 1 janky frame, P50 16ms, P90 18ms, P95 19ms, P99 121ms, and `Number Slow UI thread: 1`. UIAutomator confirmed the current page is `本地音乐` with `111 首可播放歌曲`, and the hierarchy only contains visible rows instead of all 111 rows, which supports that lazy composition is active.

The current worktree is intentionally dirty with two modified files plus this handoff. Pre-existing untracked files were present before creating this handoff: `docs/superpowers/plans/` and three older files under `handoff/2026-06-22-*.md`. Do not delete or revert them unless the user asks.

## Assumptions Made

- The root cause is local page eager composition, based on both source inspection and true-device profiling.
- It is acceptable for Local Music to own its scroll container because it is the only page currently intended to show the full scanned library.
- Existing page chrome and bottom padding should remain centralized in `MusicApp.kt`.
- The user prefers root-cause fixes over local patches, per `AGENTS.md`.

## Potential Gotchas

- Do not place `LazyColumn` back inside `verticalScroll`; Compose will either complain about infinite constraints or lose the performance benefit.
- `gfxinfo reset` prints current stats while also resetting; collect a second `dumpsys gfxinfo` after the isolated action for meaningful data.
- ADB may need to run outside sandbox if the daemon socket is blocked; the command prefix `adb` was approved during this session.
- `LocalMusicScreen.kt` is now 199 lines, intentionally kept just under the project Kotlin guidance of 200 lines.
- `git diff --stat` looks large because `AppContent` was rewrapped into a branch for Local Music versus other pages, but behavior outside Local Music should be unchanged.

## Environment State

## Tools/Services Used

- `systematic-debugging` skill: root-cause investigation before code changes.
- `brainstorming` skill: quick design calibration for root-cause fix shape.
- Kotlin skills: `kotlin-basics`, `kotlin-documentation`, `kotlin-functions`, `kotlin-naming`.
- Android device via ADB: `GUKF4DWOYLFU49QW`, reported during install as `PFGM00 - 12`.
- Gradle verification commands:
  - `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`
  - `./gradlew :composeApp:installDebug`
- ADB verification commands used:
  - `adb logcat -c`
  - `adb logcat -d -v time Choreographer:I OpenGLRenderer:I '*:S'`
  - `adb shell dumpsys gfxinfo com.yanhao.kmpmusic reset`
  - `adb shell dumpsys gfxinfo com.yanhao.kmpmusic`
  - `adb shell uiautomator dump /sdcard/window.xml`
  - `adb exec-out cat /sdcard/window.xml`

## Active Processes

- No long-running dev server is required.
- No intentional background `adb logcat` process was left running; the live logcat session was stopped with Ctrl-C.

## Environment Variables

- No relevant environment variables were used or required.

## Related Resources

- `AGENTS.md`: project rules, including root-cause fixes and KMP architecture boundaries.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt`
- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data/AndroidMediaStoreScanner.kt`
- Previous handoff: `handoff/2026-06-22-205316-local-audio-home-display-implementation-tasks-7-8.md`

---

**Security Reminder**: Validated with `validate_handoff.py`; no secrets were intentionally included.
