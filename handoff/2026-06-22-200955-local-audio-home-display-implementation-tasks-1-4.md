# Handoff: 本地音频首页展示实现 Task 1-4

## Session Metadata
- Created: 2026-06-22 20:09:55
- Project: /Users/yanhao/Desktop/demo/kmp-music
- Branch: codex/local-audio-domain-boundary
- Session duration: about 2 hours

### Recent Commits (for context)
  - 3be686b 接入本地音频扫描快照仓库
  - c0909b2 实现扫描结果曲库合并
  - e647bd1 扩展歌曲真实来源字段
  - be61b18 定义本地音频扫描领域模型
  - 1423e9c 创建本地音频方案交接文档

## Handoff Chain

- **Continues from**: [2026-06-22-180512-local-audio-home-display-design.md](./2026-06-22-180512-local-audio-home-display-design.md)
  - Previous title: 本地音频首页展示与数据来源设计
- **Supersedes**: None

Read the previous handoff for the product/design context. This handoff captures the implementation work completed after that design handoff.

## Current State Summary

Implementation plan `docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md` has been executed through Task 4 on branch `codex/local-audio-domain-boundary`. The code now has a common-domain local audio scan boundary, real-shaped `Song` source fields, a scan-result merge use case, an in-memory snapshot repository, and an explicit fake scanner with 8 local-audio-shaped songs. The homepage UI still renders seed/mock data; scanned snapshot state is not yet wired into `MusicAppController` UI state or `HomeScreen`. The next implementation task is Task 5: separate playback history semantics from scanned songs.

## Codebase Understanding

## Architecture Overview

The KMP app still follows `core / domain / data / feature` boundaries. Task 1-4 added the local-audio data pipeline foundations but intentionally did not add Android/iOS/Desktop real scanner implementations. The current path is:

`LocalMusicScanner` -> `ScanLocalMusicUseCaseImpl` -> `MusicLibraryRepository.applyScanResult()` -> `MergeLocalMusicScanResultUseCaseImpl` -> `LibrarySnapshot`.

`SeedMusicLibraryRepository` remains the source for the existing homepage UI. `InMemoryMusicLibraryRepository` is the new fake/scan snapshot repository for the local-audio pipeline. `MusicAppController` was only compile-adapted so the new `ScanLocalMusicUseCaseImpl` has fake dependencies; the UI scan button still uses the old `ScanStatus` dialog behavior through a compatibility method.

## Critical Files

| File | Purpose | Relevance |
|------|---------|-----------|
| docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md | Implementation plan and execution checklist | Task 1-4 checkboxes are complete; Task 5 is the next source of truth. This file is currently untracked in git. |
| docs/superpowers/specs/2026-06-22-local-audio-home-display-design.md | Product/design spec | Explains confirmed UX: keep `最近播放`, add homepage `本地歌曲` max 6 preview, full list in secondary local music page. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModels.kt | Platform-neutral scan models and `LibrarySnapshot` | Defines `LocalMusicSourceKind`, `MusicFileMetadata`, scan state/result/problem/source summary, `LibraryStats`, `LibrarySnapshot.Empty`. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/Song.kt | Unified song model | Now includes `durationMs`, `sourceId`, `sourceKind`, `localUri`, `mimeType`, `sizeBytes`, `modifiedAt`, and `isPlayable`. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/LocalMusicScanner.kt | Common scanner boundary | Platform implementations must satisfy this interface later; no platform APIs in commonMain. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/MusicLibraryRepository.kt | Library repository boundary | Now exposes `getSnapshot()` and `applyScanResult()` plus compatibility list getters. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCase.kt | Scan result merge policy | Handles sourceKey ids, playable filtering, metadata fallbacks, sorting, album/artist aggregation, scan summary. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/ScanLocalMusicUseCase.kt | Scan coordinator | New suspend scan entry plus temporary `ScanStatus` compatibility method for existing UI. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeLocalMusicScanner.kt | Common fake scanner | Supplies 8 fake scanned tracks with `fake://local-audio/...` URIs. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryMusicLibraryRepository.kt | In-memory scanned snapshot repository | Stores and returns merged `LibrarySnapshot`; used by tests and controller compile adapter. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/SeedMusicLibraryRepository.kt | Existing seed/mock music repository | Still powers current UI; now implements `getSnapshot()` / `applyScanResult()` for interface compatibility. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt | App state controller | Compile-adapted to construct `ScanLocalMusicUseCaseImpl(FakeLocalMusicScanner, InMemoryMusicLibraryRepository)`, but scan results are not yet synced to UI state. |
| composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCaseTest.kt | Domain scan/merge tests | Covers sorting, failed-entry filtering, metadata fallback, and scan use case storing snapshot in repository. |

## Key Patterns Discovered

- Follow TDD checkpoints from the plan: write failing test, run `desktopTest` to verify failure, implement, run tests, commit.
- Keep Task status in `docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md` synchronized as checkboxes are completed.
- Keep commits task-scoped and Chinese: Task 1 `be61b18`, Task 2 `e647bd1`, Task 3 `c0909b2`, Task 4 `3be686b`.
- Use `apply_patch` for manual edits. Do not use shell write tricks.
- Do not put platform APIs in `commonMain`; the scanner boundary is common, platform scanners come later.
- `SeedMusicLibraryRepository` should not pretend to be real scanned data. It remains current UI seed data until later tasks switch to snapshot-driven state.
- `MusicAppController` remains the bridge from domain state to Compose UI; Composables should not call scanners or repositories directly.

## Work Completed

## Tasks Finished

- [x] Task 1: Local Music Domain Boundary
  - Created `LocalMusicModels.kt`, `LocalMusicScanner.kt`, and model tests.
  - Commit: `be61b18 定义本地音频扫描领域模型`.
- [x] Task 2: Real-Shaped Song Fields
  - Extended `Song` with scanner/local source fields and `isPlayable`.
  - Commit: `e647bd1 扩展歌曲真实来源字段`.
- [x] Task 3: Scan Result Merge and Aggregation
  - Added `MergeLocalMusicScanResultUseCase` and tests for sorting, filtering, fallback, aggregation.
  - Commit: `c0909b2 实现扫描结果曲库合并`.
- [x] Task 4: Scanner Use Case and In-Memory Library Repository
  - Added scan use case coordinator, in-memory repository, fake scanner, repository snapshot APIs, and scanner/repository test.
  - Commit: `3be686b 接入本地音频扫描快照仓库`.

## Files Modified

| File | Changes | Rationale |
|------|---------|-----------|
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModels.kt | Added platform-neutral local scan models, scan states, errors, `LibrarySnapshot` | Creates the shared contract before any platform scanner implementation. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/LocalMusicScanner.kt | Added scanner interface | Keeps Android/iOS/Desktop scanning behind common boundary. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/Song.kt | Added real source fields and `isPlayable` | Lets scanner-generated songs carry `localUri` and source identity through playback/UI layers. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCase.kt | Added merge request/interface/impl | Centralizes sourceKey merge, metadata fallback, stats, album/artist aggregation. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/MusicLibraryRepository.kt | Added `getSnapshot()` and `applyScanResult()` | Moves UI toward consuming one `LibrarySnapshot`; keeps compatibility getters. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/ScanLocalMusicUseCase.kt | Replaced simulated scan use case with scanner/repository coordinator, kept `ScanStatus` compatibility | Enables scanner data flow without forcing UI rewrite in Task 4. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryMusicLibraryRepository.kt | Added in-memory snapshot repository | Stores merged fake/scan results for tests and future UI wiring. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeLocalMusicScanner.kt | Added explicit fake scanner with 8 tracks | Provides common-stage test/development data shaped like real scanned audio. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/SeedMusicLibraryRepository.kt | Added `getSnapshot()` / `applyScanResult()` compatibility implementation | Required after repository interface changed; seed remains current UI source. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt | Added fake scanner/in-memory repository dependencies for `ScanLocalMusicUseCaseImpl` construction | Compile-safe compatibility after scan use case constructor changed; UI behavior not switched yet. |
| composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModelsTest.kt | Added sourceKey and `Song.localUri` tests | Protects stable id and playable URI rules. |
| composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCaseTest.kt | Added merge and scan-store tests | Protects domain merge behavior and scanner/repository data flow. |
| docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md | Task 1-4 checkboxes and execution notes updated | Local execution log. This directory remains untracked. |

## Decisions Made

| Decision | Options Considered | Rationale |
|----------|-------------------|-----------|
| Use `sourceKind.value + ":" + sourceId` as `Song.id` for scanned songs | Random ids, title/path ids, sourceKey | Stable `sourceKey` preserves favorites, queue, and history through rescans. |
| Keep `ScanStatus` compatibility in `ScanLocalMusicUseCase` | Remove it now, or keep temporary overload | Removing it would force UI/controller work before Task 5/6. Keeping it lets Task 4 compile independently. |
| Keep seed UI source separate from fake scanner | Replace seed repository immediately, or add fake scanner as separate source | User requested real data path design; fake scanner should exercise the scan pipeline, not masquerade as old seed UI. |
| Update `SeedMusicLibraryRepository` for new repository interface | Rewrite app default repository now, or add compatibility methods | Rewriting default belongs in later controller/UI tasks; compatibility keeps current app behavior stable. |
| Compile-adapt `MusicAppController` with fake scanner dependencies | Leave broken until Task 6, or add default fake/in-memory dependencies | Each task should produce buildable software. This change does not wire scanned songs into UI yet. |
| Commit code only, leave plan handoff untracked | Commit plan docs together, or keep code commits task-scoped | User has been using plan/handoff docs as local workflow artifacts; code commits remain clean and task-specific. |

## Pending Work

## Immediate Next Steps

1. Implement Task 5 from `docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md`: "Playback History Is Separate From Scanned Songs".
2. Before editing Task 5, load required skills again: `writing-plans`, likely `executing-plans`, plus Kotlin skills (`kotlin-basics`, `kotlin-architecture`, `kotlin-data-classes`, `kotlin-interface`, `kotlin-functions`, `kotlin-naming`, `kotlin-documentation`).
3. Start Task 5 by writing the failing controller tests around recent playback not being auto-filled by scanned songs and queue movement not using scanned songs as implicit fallback.
4. Keep updating plan checkboxes as each Task 5 step completes.
5. Run `./gradlew :composeApp:desktopTest` after each meaningful Task 5 change and `./gradlew :composeApp:compileDebugKotlinAndroid` before final commit.

## Blockers/Open Questions

- [ ] No active blocker for Task 5.
- [ ] Open design detail for later tasks: Task 3 currently treats each scan result as the new discovered set except explicit removed keys. True incremental scans that omit unchanged songs may need an additional test/policy later.
- [ ] Plan has a sequencing oddity: Task 5 Step 9 says "Commit after Task 6 passes"; Task 6 Step 9 also references committing Task 5 and Task 6 together if not committed. Follow the plan carefully or ask the user before changing the commit boundary.

## Deferred Items

- Real Android MediaStore scanner implementation is deferred to a later platform-specific plan.
- Real iOS Files / Document Picker import implementation is deferred to a later platform-specific plan.
- Real Desktop folder scanner implementation is deferred to a later platform-specific plan.
- Real playback of `localUri` is deferred; current work only carries the URI through models and fake data.
- Homepage `本地歌曲` UI section is deferred until Task 7.
- Secondary `本地音乐` full list is deferred until Task 8.

## Context for Resuming Agent

## Important Context

The user is executing the implementation plan task-by-task. They expect the plan document checkboxes to be updated as work proceeds. Task 1-4 code is committed on branch `codex/local-audio-domain-boundary`, but `docs/superpowers/plans/` is still untracked and contains the local execution notes and checkbox states. Do not assume the plan status exists on another clone unless the untracked docs are present.

The current app UI still shows seed/mock data. Task 4 only built the fake scanner and snapshot path. The scanner path is testable through `ScanLocalMusicUseCaseImpl`, but `MusicAppController` does not yet sync scanned `LibrarySnapshot` into UI state. That is intentional; Task 6 is planned to move controller state to `LibrarySnapshot`.

`ScanLocalMusicUseCase` currently has two `invoke` paths:
- `suspend operator fun invoke(request, likedSongIds): LibrarySnapshot` is the new scanner/repository path.
- `operator fun invoke(currentStatus: ScanStatus): ScanStatus` is temporary compatibility for current scan dialog UI.

Do not remove the compatibility path unless you are also updating `MusicAppController`, `MusicAppModels`, and UI references in the relevant later tasks.

## Assumptions Made

- `FakeLocalMusicScanner` lives in `commonMain/data` only as a development/test scanner until platform scanners exist.
- `SeedMusicLibraryRepository.applyScanResult()` intentionally ignores scans and returns its seed snapshot; scan results belong in `InMemoryMusicLibraryRepository`.
- `MusicAppController` fake scanner dependency is only a constructor compatibility adapter, not final UI wiring.
- Full scan result semantics are acceptable for Task 4; incremental partial scan semantics can be refined later if needed.
- `docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md` remains the active task checklist.

## Potential Gotchas

- `MusicAppController` currently imports `FakeLocalMusicScanner` and `InMemoryMusicLibraryRepository`; this is not final architecture for production UI, only a temporary compile-safe bridge until Task 6.
- If Task 5 changes playback state to nullable current song, many UI assumptions may break. Follow plan steps and tests rather than making broad UI rewrites.
- `SeedMusicLibraryRepository.kt` is 188 lines after Task 4; be cautious about adding much more there because project Kotlin guidance prefers files under 200 lines.
- `FakeLocalMusicScanner.kt` has 145 lines and verbose metadata calls. Keep future fake data additions restrained.
- The plan directory is untracked. `git status` will keep showing `?? docs/superpowers/plans/` and this new handoff file until user decides whether to stage docs.
- Do not modify `prototypes/kmp-music-hi-fi` for production tasks.
- Do not run destructive git commands. The branch is ahead of previous local state with task commits.

## Environment State

## Tools/Services Used

- `session-handoff` skill was used to create this document.
- `writing-plans` was used as the task source for Tasks 1-4.
- `executing-plans` workflow was followed inline for each task.
- Kotlin skills were loaded for Kotlin edits.
- Verification commands used:
  - `./gradlew :composeApp:desktopTest`
  - `./gradlew :composeApp:compileDebugKotlinAndroid`

## Active Processes

- No dev server, emulator, watcher, or background process is required for the next step.

## Environment Variables

- No task-specific environment variables are required.
- Do not record secret values in future handoffs.

## Related Resources

- docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md
- docs/superpowers/specs/2026-06-22-local-audio-home-display-design.md
- handoff/2026-06-22-180512-local-audio-home-display-design.md
- docs/LOCAL_AUDIO_DISCOVERY_PRD.md
- docs/PRD.md
- AGENTS.md
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModels.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/Song.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/MusicLibraryRepository.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/LocalMusicScanner.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCase.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/ScanLocalMusicUseCase.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeLocalMusicScanner.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryMusicLibraryRepository.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/SeedMusicLibraryRepository.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt
- composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCaseTest.kt
- composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModelsTest.kt

---

**Security Reminder**: Validate with the handoff validation script before final response.
