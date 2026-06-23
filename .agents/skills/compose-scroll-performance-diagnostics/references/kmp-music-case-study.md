# KMP Music Local Music Case Study

Use this reference when diagnosing a Compose list problem that resembles the KMP Music Local Music page: a full-library secondary page with shared row components, global chrome, and Android-visible dropped frames.

## Stage 1: Slow Page Entry

Symptom: after scanning local music, entering the Local Music secondary page was slow.

Evidence:

- Android MediaStore scanning was fast for the measured library: about 17-34 ms for 111 songs.
- Page entry produced skipped frames and Davey frames.
- UIAutomator showed the old page hierarchy contained the full song list rather than only visible rows.

Root cause:

- `LocalMusicScreen` rendered the full library eagerly in a `Column`.
- The app shell wrapped secondary pages in a global `Column.verticalScroll`, so the Local Music page did not own a finite lazy scroll container.

Fix shape:

- Keep global chrome and bottom padding policy in `MusicApp.kt`.
- Special-case `SecondaryScreen.LocalMusic` so it bypasses the app-wide `verticalScroll`.
- Make `LocalMusicScreen` own one `LazyColumn` containing header, tabs, songs, album rows, artists, and sources.
- Use stable keys for song IDs, album-row IDs, and artist IDs.

Why this is root-cause work:

- It preserves the full-library product contract instead of truncating the list.
- It fixes scroll ownership at the page/app-shell boundary instead of hiding cost in one section.

## Stage 2: Continuous Scroll Jank

Symptom: after page entry became smooth, continuous scrolling still dropped frames.

Evidence before the row fix:

- 471 frames.
- 43 janky frames.
- 9.13% jank.
- P95 24 ms.
- P99 29 ms.
- `Slow UI thread: 27`.
- `Slow issue draw commands: 38`.
- `Slow bitmap uploads: 0`.

Classification:

- Bitmap uploads were not the measured bottleneck.
- The remaining cost was mixed UI/draw work in visible rows.
- The shared dense `SongRow` was the right target because it was reused by Local Music, Search, Favorites, details, and queue UI.

Fix shape:

- Remove cover shadow only for dense song rows.
- Keep non-dense/home preview row shadows, because those rows are few and visually richer.
- Reserve fixed equalizer glyph space for layout stability.
- Draw `PlayingGlyph` only for the current song instead of drawing it for every non-current row.
- Add `contentType` for Local Music song rows, album grid rows, and artist rows.

Verification after the fix:

- `./gradlew :composeApp:compileDebugKotlinAndroid` passed.
- `./gradlew :composeApp:desktopTest` passed.
- `./gradlew :composeApp:installDebug` passed on Android 12 device `PFGM00`.
- First post-fix scroll sample: 542 frames, 27 janky frames, 4.98% jank, P95 23 ms, P99 36 ms, `Slow issue draw commands: 19`.
- Warm-cache repeat sample: 564 frames, 12 janky frames, 2.13% jank, P95 18 ms, P99 22 ms, `Slow UI thread: 6`, `Slow issue draw commands: 11`, `Slow bitmap uploads: 0`.

## Lessons To Reuse

- A list performance bug can have multiple stages. Fixing page entry does not prove continuous scrolling is cheap.
- Stable keys help identity and movement. They do not reduce expensive drawing.
- `contentType` helps lazy composition reuse for mixed structures. It is not a replacement for reducing row cost.
- Row-level optimizations should usually happen in shared dense/list modes when the shared component is the measured hot path.
- A reusable skill should preserve the diagnostic method, not a fixed instruction like "remove list shadows."

## Official Docs Worth Checking

- Lazy lists and lazy grids: https://developer.android.com/develop/ui/compose/lists
- Compose performance best practices: https://developer.android.com/develop/ui/compose/performance/bestpractices
- Compose phases and performance: https://developer.android.com/develop/ui/compose/performance/phases
- Practical performance problem solving in Jetpack Compose: https://developer.android.com/codelabs/jetpack-compose-performance
- Diagnose stability issues: https://developer.android.com/develop/ui/compose/performance/stability/diagnose
- Fix stability issues: https://developer.android.com/develop/ui/compose/performance/stability/fix
- Strong skipping mode: https://developer.android.com/develop/ui/compose/performance/stability/strongskipping
