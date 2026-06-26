# Handoff: 本地音乐滚动掉帧修复与 Compose 细节经验

## Session Metadata
- Created: 2026-06-23 14:10:08
- Project: /Users/yanhao/Desktop/demo/kmp-music
- Branch: codex/local-audio-domain-boundary
- Session duration: about 1.5 hours

### Recent Commits (for context)
  - be57b73 修复 Android 音频权限引导
  - d96ef16 完成本地音频首页最终核验
  - 99cd581 统一搜索收藏我的页曲库来源
  - 6db6e0f 新增本地音乐二级页
  - 4aa3ee0 首页展示扫描本地歌曲预览

## Handoff Chain

- **Continues from**: [2026-06-23-123144-local-music-lazy-list-performance.md](./2026-06-23-123144-local-music-lazy-list-performance.md)
  - Previous title: 本地音乐二级页卡顿根治修复
- **Supersedes**: None

## Current State Summary

This session continued from the prior Local Music performance handoff. The previous fix made entering the secondary Local Music page smooth by moving the full library into a page-owned `LazyColumn`. The user then reported that page entry was now fine but continuous list scrolling still dropped frames. Device profiling showed the remaining jank was not scanning or bitmap upload; it was visible row composition/drawing cost in the shared dense `SongRow`. The code now keeps the prior lazy-list architecture, reduces dense row drawing cost, adds lazy `contentType`, and has been verified on a real Android 12 device with 111 local songs. The user confirmed the page is now very smooth.

## Codebase Understanding

## Architecture Overview

The app is a Kotlin Multiplatform Compose app. Shared UI, state, navigation, theme, domain models, and mock/in-memory data live in `commonMain`; Android-specific media scanning is isolated in `androidMain`. `MusicAppController` owns global UI state. `MusicApp.kt` owns page routing and global chrome. `LocalMusicScreen.kt` is the full-library secondary page. `CommonComponents.kt` contains reusable item components such as `SongRow`, `AlbumCard`, and `ArtistRow`. Because dense song rows are reused by Local Music, Search, Favorites, details, and the queue sheet, list performance fixes should target shared row behavior when the root cause is row cost rather than duplicating one-off local rows.

## Critical Files

| File | Purpose | Relevance |
|------|---------|-----------|
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt` | Shared app shell, navigation routing, bottom chrome, page padding | Prior handoff modified it so `SecondaryScreen.LocalMusic` bypasses app-wide `verticalScroll` and lets `LocalMusicScreen` own lazy scrolling |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt` | Full local library secondary page | Now renders header, tabs, songs, albums, artists, and sources through one `LazyColumn` with stable keys and content types |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CommonComponents.kt` | Shared UI components including `SongRow` | This session optimized dense `SongRow` drawing cost, which fixed the remaining scroll jank without cloning local-only row UI |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/core/theme/MusicTheme.kt` | Theme colors, dimensions, scaling helpers | Relevant because every row uses shared scaled dimensions and visual tokens |
| `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data/AndroidMediaStoreScanner.kt` | Android MediaStore scanner | Previous profiling showed scanning 111 songs took about 17-34ms and was not the bottleneck |

## Key Patterns Discovered

- Large or unbounded vertical content should own a `LazyColumn`; do not put a same-direction lazy list inside an unbounded `verticalScroll`.
- Put page header, tabs, list items, and footer-style sections in one lazy DSL so items can compose and measure independently.
- Use stable keys for local library items: song ID, album IDs for row chunks, artist ID.
- Use `contentType` when a lazy list contains heterogeneous item structures such as header, tabs, song rows, album grid rows, artist rows, and source section.
- Treat `key` and `contentType` as identity/reuse tools, not as fixes for expensive item drawing.
- When page entry is smooth but scrolling janks, inspect the visible item implementation for per-row shadows, clips, gradients, nested lazy layouts, conditional state reads, and unnecessary decorative drawing.
- Optimize shared components when the problem is in shared item cost. Avoid one-off local-row copies unless the product intentionally needs a separate visual contract.

## Work Completed

## Tasks Finished

- [x] Read the prior handoff and confirmed the previous root cause and uncommitted lazy-list changes.
- [x] Reproduced the remaining issue on connected Android device `GUKF4DWOYLFU49QW`.
- [x] Used `adb shell dumpsys gfxinfo com.yanhao.kmpmusic` before code changes to classify the jank.
- [x] Found the remaining cost was mixed UI/draw work: sample had 471 frames, 43 janky frames, 9.13% jank, P95 24ms, P99 29ms, `Slow UI thread: 27`, `Slow issue draw commands: 38`, and `Slow bitmap uploads: 0`.
- [x] Optimized shared dense `SongRow` by removing dense cover shadow while preserving non-dense/home preview shadow.
- [x] Optimized playback indicator drawing by reserving fixed glyph space but only drawing the equalizer glyph for the current song.
- [x] Added `contentType` to Local Music lazy song, album-row, and artist items.
- [x] Verified Android compile, desktop shared tests, debug install, and real-device scroll performance.
- [x] Discussed how to preserve the learning as a narrow Compose scroll performance diagnostic skill/checklist rather than a generic “remove shadows” recipe.
- [x] Researched official Android/Compose documentation for finer-grained performance details: lazy structure, content type, heavy composables, state reads, drawing phase, stability, strong skipping, and release-mode measurement caveats.

## Files Modified

| File | Changes | Rationale |
|------|---------|-----------|
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt` | Previous handoff change: Local Music page is routed outside the app-wide `Column.verticalScroll` and receives `contentPadding` directly | Keeps lazy list under finite constraints and preserves central chrome/padding policy |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt` | Previous handoff converted eager sections to one `LazyColumn`; this session added `contentType` for song, album-row, and artist lazy items | Reduces lazy list composition reuse mismatch and keeps full library rows independently composable |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CommonComponents.kt` | Dense `SongRow` cover no longer uses shadow; non-current rows reserve glyph space but do not draw `PlayingGlyph`; non-dense rows keep existing cover shadow | Device metrics pointed to visible row draw cost, so the fix belongs in the shared dense row behavior rather than scanner or data layer |
| `handoff/2026-06-23-141008-local-music-scroll-performance-compose-details.md` | New handoff document | Preserves current context, verification data, and Compose performance lessons |

## Decisions Made

| Decision | Options Considered | Rationale |
|----------|-------------------|-----------|
| Optimize dense `SongRow` instead of creating a local-only lightweight row | Local-only row, shared row optimization, data truncation | The same dense row is used by search/favorites/details/queue; fixing shared row cost prevents repeated future jank and avoids hiding full-library behavior |
| Remove dense cover shadow but keep non-dense shadow | Remove all shadows, keep all shadows, remove only dense-row shadow | Long scrolling lists need cheaper rows; home/recent preview rows are few and can keep the richer visual |
| Draw playback glyph only for current song while reserving space | Draw glyph for every row, remove glyph entirely, reserve slot and draw only current glyph | Preserves layout stability and the current-playing auxiliary indicator without spending draw work on every non-current row |
| Add `contentType` to Local Music lazy items | Keys only, content type per row kind, split into separate lazy lists | Content type improves lazy composition reuse for mixed structures while keeping one scroll owner |
| Do not create a broad “generic list optimization” skill | Generic checklist, fixed recipe, diagnostic skill | Compose performance is detail-sensitive. The reusable artifact should be a diagnostic method that classifies the bottleneck before recommending code changes |

## Pending Work

## Immediate Next Steps

1. Review the current uncommitted diff and decide whether to commit the combined prior Local Music lazy-list fix plus this dense-row scroll optimization.
2. If the user wants a reusable artifact, create a narrow project skill/checklist named something like `compose-scroll-performance-diagnostics`, focused on measurement and decision points rather than fixed prescriptions.
3. If creating that skill, include official-source-backed details: lazy container constraints, multi-element item pitfall, `contentType`, heavy composables, state read deferral, draw-vs-composition phase, stability reports, and release/real-device measurement caveats.

## Blockers/Open Questions

- [ ] Open question: whether to commit the current performance fix now or first add the project skill/checklist.
- [ ] Open question: whether the future skill should live under `.agents/skills/compose-scroll-performance-diagnostics` or remain a documented project note instead.

## Deferred Items

- Macrobenchmark or Compose compiler stability reporting was deferred because the immediate bug was fixed with direct `gfxinfo` evidence and the project does not currently have a performance test harness.
- Further row-level refinements such as replacing `Surface` buttons, reducing IconButton semantics, or adding release-mode benchmark baselines were deferred until there is a new measured bottleneck.
- Creating the reusable Compose performance skill was discussed but not implemented before this handoff.

## Context for Resuming Agent

## Important Context

The user’s latest performance bug is fixed: they explicitly said the Local Music list is now very smooth. The worktree is intentionally dirty. Do not revert existing changes. The previous handoff’s `MusicApp.kt` and `LocalMusicScreen.kt` lazy-list edits are still present and uncommitted. This session added `CommonComponents.kt` changes and small `LocalMusicScreen.kt` content type changes.

The measured root cause evolved in two stages. Stage 1 from the previous handoff: entering Local Music was slow because the page rendered all local songs eagerly in a non-lazy `Column` under app-wide `verticalScroll`. Stage 2 from this session: after entry became smooth, continuous scrolling still dropped frames because visible dense `SongRow` drawing remained too expensive. Device `gfxinfo` before this session’s fix showed `Slow issue draw commands: 38` and `Slow bitmap uploads: 0`, so the right target was row drawing cost, not scanner speed or image upload.

Verification after the fix:
- `./gradlew :composeApp:compileDebugKotlinAndroid` passed.
- `./gradlew :composeApp:desktopTest` passed.
- `./gradlew :composeApp:installDebug` passed and installed on Android device `PFGM00 - 12`.
- Real-device 111-song scroll sample after fix: first sample 542 frames, 27 janky frames, 4.98% jank, P95 23ms, P99 36ms, `Slow issue draw commands: 19`.
- Warm-cache repeat sample: 564 frames, 12 janky frames, 2.13% jank, P95 18ms, P99 22ms, `Slow UI thread: 6`, `Slow issue draw commands: 11`, `Slow bitmap uploads: 0`.

Official Compose research summary from the user discussion:
- Lazy lists should avoid unbounded same-direction nesting and should avoid putting many real elements in one lazy item.
- `contentType` helps Compose reuse compositions only between items with similar structure.
- Heavy composables can drop frames; `painterResource` can load large local images on the main thread during composition.
- Fast-changing scroll/animation state should be read as late as possible; sometimes draw-phase reads are better than composition-phase reads.
- Stability and skippability matter, but they are not universal fixes. Standard collections can make parameters unstable, strong skipping is enabled by default in Kotlin 2.0.20, and not every composable needs to be skippable.

## Assumptions Made

- The user wants root-cause fixes and useful reusable process, not quick visual patches.
- The current visual tradeoff is acceptable: dense rows in long lists can be slightly flatter than preview rows, while home/recent non-dense rows keep richer cover shadow.
- `Slow bitmap uploads: 0` means the current local cover resources are not the limiting factor for the measured scroll sample.
- The existing Android device with 111 songs is sufficient for validating this bug class, though larger libraries may reveal new bottlenecks later.

## Potential Gotchas

- Do not put `LocalMusicScreen` back inside the app-wide `verticalScroll`; that reintroduces the original entry-jank structure.
- Do not interpret stable keys as a cure for expensive drawing. Keys help identity and movement reuse; this bug needed item-cost reduction.
- Do not make a generic skill that says “remove shadows from lists.” The reusable lesson is to measure first and choose the optimization based on composition, layout, draw, bitmap upload, or stability evidence.
- If Android debug builds appear slower, remember official docs say lazy layout performance should be measured reliably in release mode with R8 for final performance conclusions.
- The scaffold script expects `python`, but this machine used `python3`.

## Environment State

## Tools/Services Used

- Skill: `session-handoff` for this document.
- Skills used earlier in the session: `systematic-debugging`, `brainstorming`, `kotlin-basics`, `kotlin-functions`, `kotlin-naming`, `kotlin-documentation`.
- Android device: `GUKF4DWOYLFU49QW`, shown by Gradle install as `PFGM00 - 12`.
- Verification commands:
  - `./gradlew :composeApp:compileDebugKotlinAndroid`
  - `./gradlew :composeApp:desktopTest`
  - `./gradlew :composeApp:installDebug`
  - `adb shell dumpsys gfxinfo com.yanhao.kmpmusic reset`
  - `adb shell dumpsys gfxinfo com.yanhao.kmpmusic`
  - `adb shell input swipe 540 1800 540 520 700`
  - `adb shell input swipe 540 520 540 1800 700`
  - `adb shell uiautomator dump /sdcard/window.xml`
  - `adb exec-out cat /sdcard/window.xml`

## Active Processes

- No known long-running dev server, logcat process, or watcher was left running.

## Environment Variables

- No relevant environment variables were used or required.

## Related Resources

- `AGENTS.md`
- `handoff/2026-06-23-123144-local-music-lazy-list-performance.md`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CommonComponents.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/core/theme/MusicTheme.kt`
- Android official docs consulted:
  - [Lazy lists and lazy grids](https://developer.android.com/develop/ui/compose/lists)
  - [Compose performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)
  - [Compose phases and performance](https://developer.android.com/develop/ui/compose/performance/phases)
  - [Practical performance problem solving in Jetpack Compose](https://developer.android.com/codelabs/jetpack-compose-performance)
  - [Diagnose stability issues](https://developer.android.com/develop/ui/compose/performance/stability/diagnose)
  - [Fix stability issues](https://developer.android.com/develop/ui/compose/performance/stability/fix)
  - [Strong skipping mode](https://developer.android.com/develop/ui/compose/performance/stability/strongskipping)

---

**Security Reminder**: Validate with `validate_handoff.py` before finalizing.
