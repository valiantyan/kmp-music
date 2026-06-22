# Handoff: 本地音频首页展示与数据来源设计

## Session Metadata
- Created: 2026-06-22 18:05:12
- Project: /Users/yanhao/Desktop/demo/kmp-music
- Branch: main
- Session duration: about 2 hours

### Recent Commits (for context)
  - d0fd955 补充本地歌曲方案复盘问题
  - 3baf44c 补充本地歌曲数据来源设计
  - 9fe590f 设计本地歌曲首页展示方案
  - 1d1b161 补充本地音频发现PRD评审
  - 1cd325f 补充本地音频发现PRD

## Handoff Chain

- **Continues from**: None (fresh start)
- **Supersedes**: None

This is the first handoff for the local audio homepage display and data-source design task.

## Current State Summary

The session focused on deciding how scanned, playable local audio should appear in the existing KMP Music app. The user confirmed the B方案: keep `最近播放`, then add a new `本地歌曲` preview section in the homepage red-box position between `最近播放` and `本地专辑`, showing up to 6 scanned playable songs plus `查看全部`. The design was then expanded from pure UI/UX into a combined UI/UX and data-source specification based on `docs/LOCAL_AUDIO_DISCOVERY_PRD.md`, including Android/iOS/Desktop source boundaries, scan state mapping, stable ids, library merge rules, and downstream consumers. No production Kotlin implementation has been started yet.

## Codebase Understanding

## Architecture Overview

The app is a Kotlin Multiplatform / Compose Multiplatform music app centered on `:composeApp`. Current UI state is coordinated by `MusicAppController`, with top-level tabs `首页 / 收藏 / 我的`, secondary screens, and global bottom chrome / mini-player behavior. Current music content still comes from `SeedMusicLibraryRepository`, which feeds `GetHomeMusicUseCase`, search, favorites, and playback state. Existing `ScanLocalMusicUseCase` is only a simulated state toggle and is not a real scanner. The design now requires scanned platform audio to flow through a platform-neutral scanner/use-case/repository boundary into a shared `LibrarySnapshot`, then into homepage, favorites, search, profile stats, and playback queue.

## Critical Files

| File | Purpose | Relevance |
|------|---------|-----------|
| docs/superpowers/specs/2026-06-22-local-audio-home-display-design.md | Main design spec for homepage local songs display plus data-source rules | This is the source of truth for the confirmed UX and future implementation plan. |
| docs/LOCAL_AUDIO_DISCOVERY_PRD.md | Product and architecture PRD for local audio discovery | Defines Android MediaStore, iOS import, Desktop folder-scan boundaries and error model. |
| docs/PLAYBACK_PRD.md | PRD for real playback capability | Important once scanned songs need to feed `localUri` / `PlayableMedia` into real playback. |
| docs/PRD.md | Overall KMP Music PRD | Establishes homepage, favorites, profile, search, playback, and local-first MVP scope. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt | Current homepage UI | Needs future `本地歌曲` section between `最近播放` and `本地专辑`. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt | Current app state coordinator | Future implementation should coordinate scan/use-case/repository state here, not in Composables. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt | App UI state, navigation, secondary screens | Future implementation likely adds `SecondaryScreen.LocalMusic`, scan/library state, and local music section state. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/MusicLibraryRepository.kt | Current music library repository interface | Needs evolution from seed reads to real/fake scanned library snapshot. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/ScanLocalMusicUseCase.kt | Current simulated scan use case | Must evolve into a real scan coordinator around platform-neutral scanner and merge policy. |
| composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/SeedMusicLibraryRepository.kt | Current seed/mock music content | Must not be used to masquerade as real scanned data; may only inform fake scanner/test fixtures. |

## Key Patterns Discovered

- Project instructions strongly prefer root-cause design over quick patches.
- Real app work should target production KMP app, not `prototypes/kmp-music-hi-fi`.
- UI should keep the existing homepage visual language: library card, recent songs, local albums, global mini player, bottom tabs.
- Top-level pages show bottom tabs; secondary pages hide bottom tabs and keep mini player unless fullscreen.
- Global current song styling should remain synchronized in all lists.
- Platform-specific filesystem/media APIs must stay out of `commonMain` and out of Composables.
- Search, favorites, playback queue, homepage, and profile stats should share one library state, not each maintain its own list.

## Work Completed

## Tasks Finished

- [x] Clarified user intent: scanned playable audio should appear directly on the homepage in a new preview section, not replace `最近播放`.
- [x] Created and committed the initial homepage display design spec.
- [x] Expanded the spec with data-source rules from `docs/LOCAL_AUDIO_DISCOVERY_PRD.md`.
- [x] Added Android / iOS / Desktop platform source mapping, `sourceKind`, `localUri` shape, and platform-specific button copy.
- [x] Added stable `sourceKey = sourceKind + ":" + sourceId` merge rules.
- [x] Added UI section-to-data-source mapping for homepage, favorites, profile, search, and playback queue.
- [x] Added empty/error/scan state display table.
- [x] Added phased implementation guidance from fake scanner through real platform scanners and playback integration.
- [x] Added "举一反三问题速查" and three-round cross review sections.
- [x] Stopped the temporary visual-companion local preview server.

## Files Modified

| File | Changes | Rationale |
|------|---------|-----------|
| .gitignore | Added `.superpowers/` | Visual companion generated local brainstorming previews that should not enter git. |
| docs/superpowers/specs/2026-06-22-local-audio-home-display-design.md | Created and expanded into full UI/UX + data-source spec with review sections | Captures confirmed design and implementation constraints before coding. |
| handoff/2026-06-22-180512-local-audio-home-display-design.md | Created this handoff | Preserve session context for a future agent. |

## Decisions Made

| Decision | Options Considered | Rationale |
|----------|-------------------|-----------|
| Add homepage `本地歌曲` preview between `最近播放` and `本地专辑` | Full list on homepage, preview on homepage plus secondary full list, or segmented full library on homepage | User confirmed this exact red-box placement; it shows scanned data immediately without turning homepage into a full library browser. |
| Keep `最近播放` | Replace it with scanned songs, mix scanned songs into recent playback, or keep separate | `最近播放` should only mean real playback history; scanned songs belong in `本地歌曲`. |
| Limit homepage local songs to 6 | Full list, 3-5 preview, 6 preview | User requested "最多显示 6 条数据 + 查看全部"; full list goes to secondary page. |
| Add `SecondaryScreen.LocalMusic` for full list | Reuse `LocalFolder`, add new `LocalMusic`, or keep full list only in search | A dedicated route better matches the data model: songs, albums, artists, sources, scan issues. |
| Use platform-specific source boundaries | Single generic "scan local music" for all platforms, or platform-specific sources | `LOCAL_AUDIO_DISCOVERY_PRD.md` requires Android MediaStore, iOS import, Desktop folder selection with distinct UX copy. |
| Use stable `sourceKey` for merge | Use display title, random ids, path only, or `sourceKind + ":" + sourceId` | Stable source identity is required to preserve favorites, recent playback, and queue after rescans. |
| Keep seed/mock out of real scanned UI | Continue seed data until scanner complete, or use fake scanner/test fixtures only | Project/PRD explicitly forbid mock seed data from masquerading as real scan results. |

## Pending Work

## Immediate Next Steps

1. Create an implementation plan from the spec before editing production Kotlin. Use `writing-plans` if following the project skill workflow.
2. Start with shared data boundaries: define `LocalMusicScanner`, scan request/state/result/error/source models, `MusicFileMetadata` or `LibraryTrack`, and fake scanner/test data.
3. Evolve `MusicLibraryRepository` so UI consumes a real/fake `LibrarySnapshot` instead of `SeedMusicLibraryRepository` directly.
4. Add homepage `本地歌曲` section in `HomeScreen`, capped at 6 rows, with `查看全部` navigation to the future local music secondary route.
5. Add shared tests around scan-result merge, homepage preview cap, recent playback not being polluted by scan results, and stable id preservation.

## Blockers/Open Questions

- [ ] No blocking product question remains for the confirmed homepage layout.
- [ ] Implementation sequencing should be confirmed: recommended first milestone is shared fake scanner and library snapshot, not direct Android-only UI wiring.
- [ ] Real platform validation will require device/emulator coverage later; this handoff only covers design/spec work.

## Deferred Items

- Real Android `MediaStore.Audio` scanner implementation is deferred until after shared scanner/repository models are planned.
- Real iOS Document Picker import implementation is deferred; the spec explicitly states iOS P0 import boundaries.
- Real Desktop folder scanner implementation is deferred; the spec explicitly states folder-selection boundaries.
- Real playback integration is deferred until scanner-generated `localUri` / `PlayableMedia` can be passed to playback layer.

## Context for Resuming Agent

## Important Context

The user is not asking for a purely visual homepage polish anymore. They want the homepage design tied to real scanned audio data from `docs/LOCAL_AUDIO_DISCOVERY_PRD.md`. The confirmed UX is: homepage still has `最近播放`, then a new `本地歌曲` preview list in the red-box location, max 6 rows, then `本地专辑`. The spec has already been expanded with platform data-source rules and reviewed through "举一反三" and three cross-review passes. Do not implement by simply inserting seed/mock songs into the new section; the next meaningful work should plan and implement the shared data model and fake scanner boundary first.

## Assumptions Made

- `modifiedAt desc` is the default ordering for homepage local song preview; if a platform lacks `modifiedAt`, scanner order is used.
- Android `sourceId` for P0 is MediaStore `_ID`.
- iOS imported-file `sourceId` for P0 is a persisted normalized sandbox-path hash after import.
- Desktop `sourceId` for P0 is normalized file-path hash.
- Missing title falls back to filename without extension; missing artist/album use `未知歌手` / `未知专辑`; missing duration displays `--:--`.
- Recent playback empty state is `播放过的歌曲会出现在这里`.
- `SeedMusicLibraryRepository` can inform fake scanner fixtures but must not pretend to be real scanned data in production UI.

## Potential Gotchas

- iOS P0 must not say `扫描本机音乐` or imply whole-device scanning. Use `导入音频` / `从 Files 选择音频`.
- Do not put Android `ContentResolver`, iOS Document Picker, or Desktop filesystem APIs in `commonMain` or Composables.
- Do not let each page build its own library list. All consumers should use one `LibrarySnapshot`.
- Do not silently replace missing queue songs with other songs; preserve stable ids and surface playback/library errors.
- Adding up to 6 rows on homepage may affect mini-player/bottom-tab spacing; visual verification will be needed after UI implementation.
- The old visual-companion server was stopped, but `.superpowers/` may exist locally and is ignored by git.
- Use `python3` for handoff scripts on this machine; `python` is not available.

## Environment State

## Tools/Services Used

- Product Design plugin skills were used for design brief / UX workflow.
- Brainstorming visual companion was used at `http://localhost:52926` during design exploration.
- Session handoff scripts were run with `python3`.

## Active Processes

- No required active dev server or preview server is running.
- The earlier visual companion process was stopped with Ctrl-C.

## Environment Variables

- No task-specific environment variables are required.
- Do not record or expose secret values in follow-up work.

## Related Resources

- docs/superpowers/specs/2026-06-22-local-audio-home-display-design.md
- docs/LOCAL_AUDIO_DISCOVERY_PRD.md
- docs/PLAYBACK_PRD.md
- docs/PRD.md
- AGENTS.md
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/MusicLibraryRepository.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/ScanLocalMusicUseCase.kt

---

**Security Reminder**: Validated with the handoff validation script before final response.
