# Handoff: Android Playback Planning

## Session Metadata
- Created: 2026-06-23 17:41:48
- Project: /Users/yanhao/Desktop/demo/kmp-music
- Branch: main
- Session duration: ~2.5 hours

### Recent Commits (for context)
  - 85cda58 完善 Android 播放设计评审
  - ebe6bb8 新增 Android 真实播放设计
  - 66d42eb 更新本地音乐能力说明
  - 44c7ac7 优化本地音乐页列表性能并沉淀诊断技能
  - be57b73 修复 Android 音频权限引导

## Handoff Chain

- **Continues from**: [2026-06-23-141008-local-music-scroll-performance-compose-details.md](./2026-06-23-141008-local-music-scroll-performance-compose-details.md)
  - Previous title: 本地音乐滚动掉帧修复与 Compose 细节经验
- **Supersedes**: None

> The previous handoff is useful for recent Compose/list performance context, but this session's active continuation point is the Android playback design and implementation plan listed below.

## Current State Summary

The session completed requirements clarification for Android real playback, wrote and committed the approved design spec, then wrote an implementation plan. The agreed scope is Android first: shared playback boundary + Media3 ExoPlayer + MediaSessionService + custom foreground media notification + Room3 `3.0.0-rc01` persistence. The plan is saved but not committed yet. The next user-facing decision is execution approach: subagent-driven development is recommended, inline execution is also available.

## Codebase Understanding

## Architecture Overview

KMP Music is a Kotlin Multiplatform Compose app in `:composeApp`. `commonMain` owns domain models, repositories, use cases, shared UI, navigation, and the current memory playback state. Platform source sets own platform scanners and should own real playback implementations. The current Android scanner already emits playable MediaStore `content://` URIs through `Song.localUri`; playback must consume that data without making UI create platform players. Existing playback is still memory-only (`currentSongId` plus `isPlaying`), so the implementation plan starts by introducing shared playback status/mode/error models, a common `AudioPlayerEngine`, a `PlaybackCoordinator`, Room3 persistence, then Android Media3 service/notification.

## Critical Files

| File | Purpose | Relevance |
|------|---------|-----------|
| `docs/PLAYBACK_PRD.md` | Product and architecture PRD for real playback | Source of platform scope, true playback state, engine boundary, Android Media3 requirement |
| `docs/superpowers/specs/2026-06-23-android-playback-design.md` | Approved Android playback design spec | Must be read before executing the implementation plan |
| `docs/superpowers/plans/2026-06-23-android-playback-implementation.md` | Newly written implementation plan | Main next-step artifact; currently untracked |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt` | Current simple playback/search/theme models | Will be upgraded with playback status, mode, errors, queue index, snapshot |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/PlaybackUseCases.kt` | Current memory-only play/toggle/move use cases | Plan replaces or delegates this behavior to `PlaybackCoordinator` |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt` | Shared UI state controller | Must become the single UI bridge to coordinator, persistence, and playback events |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt` | Shared Compose app wiring and bottom chrome | Needs queue-list callbacks and player screen props |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/PlayerScreen.kt` | Current playback page UI | Needs real progress, seek, mode, and error UI |
| `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data/AndroidMediaStoreScanner.kt` | Android scanner producing local songs | Provides `content://` URI inputs for playback |
| `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/MusicAppViewModel.kt` | Android ViewModel holding shared controller | Will host Android playback bridge / notification action dispatcher |
| `composeApp/src/androidMain/AndroidManifest.xml` | Android permissions and components | Needs media playback service and notification permissions |
| `gradle/libs.versions.toml` and `composeApp/build.gradle.kts` | Dependency and plugin configuration | Plan adds Room3, KSP, SQLite, Media3, AndroidX core, coroutine test |

## Key Patterns Discovered

- Follow `core / domain / data / feature` boundaries; platform APIs must stay out of `commonMain`.
- UI pages send intents through `MusicAppController`; Composables should not instantiate repositories, scanners, or platform players.
- Current scanned songs already include `localUri`, `sourceKind`, `durationMs`, `mimeType`, etc.; playback should consume these instead of rescanning or constructing paths.
- Global mini-player is bottom chrome in `MusicApp.kt`; do not duplicate mini-player inside screens.
- Tests are primarily `commonTest` with `kotlin.test` and `desktopTest`.
- Project instructions require root-cause design, not quick patches; if implementation scope grows significantly, ask the user.

## Work Completed

## Tasks Finished

- [x] Read `brainstorming` skill and followed its gated design workflow.
- [x] Read `docs/PLAYBACK_PRD.md`, `docs/PRD.md`, current playback models, controller, scanner, build files, tests, and recent commits.
- [x] Clarified Android-first scope with the user.
- [x] Clarified notification controls, playback modes, queue semantics, random behavior, error skip limits, persistence, and Room3 choice.
- [x] Wrote and committed the design spec.
- [x] Added "举一反三问题速查" and "三轮交叉 Review 结果" to the design spec and committed the revision.
- [x] Invoked `writing-plans` skill and wrote the implementation plan.
- [x] Started `session-handoff` create workflow and generated this handoff.

## Files Modified

| File | Changes | Rationale |
|------|---------|-----------|
| `docs/superpowers/specs/2026-06-23-android-playback-design.md` | Added Android playback design, then review/FAQ sections | Captures approved design decisions and implementation boundaries |
| `docs/superpowers/plans/2026-06-23-android-playback-implementation.md` | Added full task-by-task implementation plan | Gives future executor an ordered TDD path from shared models to Android notification verification |
| `handoff/2026-06-23-174148-android-playback-planning.md` | This handoff | Saves context for resuming implementation planning/execution |

## Decisions Made

| Decision | Options Considered | Rationale |
|----------|-------------------|-----------|
| Android first, not all platforms | Android-only, shared-only, all platforms | User selected Android first with complete P0; iOS/Desktop remain future work |
| Include后台播放 and media notification in Android P0 | App-only first, service later, full P0 | User selected full Android P0 |
| App/notification controls | Core 3-button only, app-only extras, fixed five buttons | User requires favorite, previous, play/pause, next, mode; collapsed notification shows three, expanded shows five |
| Use MediaSessionService plus custom notification | Pure Media3 notification, hybrid, fully custom service | Hybrid preserves system playback protocol while custom notification satisfies fixed five-button expanded layout |
| Quick Settings / lock screen not customized | Try to control system panels, ignore MediaSession, provide standard session | Android system owns these surfaces; app provides MediaSession info but only custom foreground notification is验收 |
| Playback modes | Include sequence mode, or only loop/shuffle modes | User removed sequence mode; final modes are LoopAll, LoopOne, Shuffle; default LoopAll |
| Queue semantics | Insert song at head, single-song queue, current list queue | User selected current full list queue; clicked song becomes `currentIndex`; previous songs remain in queue |
| Shuffle semantics | Fully random every tap, no-repeat round with history | User accepted no-repeat round and previous using shuffle history |
| Failure limits | Stop on first error, skip forever, bounded skip | User specified LoopOne same song 3 failures stop; LoopAll/Shuffle 3 consecutive failed songs stop |
| Cold start restore | No restore, restore paused, restore and auto-play | User selected restore current song/queue/index/mode/progress/favorites as paused, no auto-play |
| Persistence library | DataStore/SharedPreferences, SQLDelight, Room3 | User selected Room3 `3.0.0-rc01`; plan uses Room3 and Android builder first |

## Pending Work

## Immediate Next Steps

1. Ask the user to choose execution mode for the plan if they have not already: Subagent-Driven recommended, Inline Execution available.
2. If user chooses Subagent-Driven, load `subagent-driven-development` skill and execute `docs/superpowers/plans/2026-06-23-android-playback-implementation.md` task by task with reviews.
3. If user chooses Inline Execution, load `executing-plans` skill and execute the same plan with checkpoints.
4. Before implementation, check `git status --short --branch`; note `docs/superpowers/plans/` and handoff files are untracked.

## Blockers/Open Questions

- [ ] Execution approach is not chosen yet because user asked for handoff immediately after plan generation.
- [ ] The implementation plan is saved but not committed. Ask whether to commit the plan/handoff before implementation if desired.
- [ ] Room3/KSP/Media3 versions were selected from current docs and plan assumptions; actual Gradle resolution must be verified during Task 3.

## Deferred Items

- iOS real playback and Now Playing / remote controls are deferred by scope.
- Desktop real playback and backend selection are deferred by scope.
- Quick Settings and lock-screen media panel customization are explicitly deferred/not owned by app layer.
- Android true-device acceptance is deferred until after implementation.

## Context for Resuming Agent

## Important Context

The latest user-visible state before this handoff: implementation plan was saved and the user was asked to choose execution approach. The user then invoked `session-handoff`. Do not start coding until the user picks Subagent-Driven or Inline Execution, unless they explicitly ask to proceed. The design spec is committed in two commits (`ebe6bb8`, `85cda58`); the implementation plan is untracked under `docs/superpowers/plans/`. The plan is intentionally detailed but still a plan: executors must follow TDD and compile/test at each task, especially around Room3/KSP APIs and Media3 service APIs.

Critical playback requirements to preserve:

- Android first only.
- Use MediaSessionService; custom notification does not replace MediaSession.
- Custom foreground notification is the only notification surface being visually验收: collapsed three buttons, expanded five buttons.
- Quick Settings and lock screen may appear naturally from MediaSession but are not customized or驗收.
- Mini player does not need manual seek; player page does.
- Playback mode set is exactly LoopAll, LoopOne, Shuffle. No sequence mode.
- Queue is full current list with clicked song as currentIndex.
- Shuffle is no-repeat per round and previous uses history.
- Cold restore is paused and never auto-plays.
- Playback errors are visible, then auto-skip with three-failure stop rules.

## Assumptions Made

- `main` is the active branch and should remain the target unless user requests a feature branch.
- User wants root-cause architecture, not patchy UI-only changes.
- Android scanner output is the first real playback input source.
- Room3 `3.0.0-rc01` is the chosen persistence library even though it is a release candidate.
- Current Desktop target is Compose Desktop JVM, not macOS/Windows native targets.
- Network browsing was used only for current Android/Room/SQLDelight facts; implementation should rely on official docs if APIs differ.

## Potential Gotchas

- The plan file is untracked because user did not ask to commit it. Do not assume it is in git.
- There are several pre-existing untracked handoff files and `docs/superpowers/plans/`; do not delete or stage unrelated ones without user request.
- `PlaybackUseCases.kt` currently contains memory-only use cases. The plan says replace or delegate; avoid leaving two competing playback paths.
- `MusicAppController` currently uses synchronous methods; plan snippets introduce coroutine calls. Implementation must keep tests deterministic and avoid leaking `MainScope` if a cleaner controller scope exists or can be injected.
- Room3/KSP exact plugin names and generated constructor behavior must be verified by Gradle. If APIs differ, consult official Room KMP docs and adapt narrowly.
- Android custom RemoteViews notification can display fixed app notification buttons, but Quick Settings/lock-screen UI cannot be controlled.
- Android 13+ notification permission is separate from audio read permission.
- Media3 playback service must not be bypassed by notification actions; all actions should go through coordinator/controller semantics.
- The current `FakeAudioPlayerEngine` and coordinator plan are test-oriented; actual implementation may need minor adjustments for event ordering, but must keep engine events as playback truth.

## Environment State

## Tools/Services Used

- Skills used: `brainstorming`, `writing-plans`, `session-handoff`.
- Web/docs consulted: Android Media3 background playback, Android media controls, Android custom notifications, Room KMP, Room3 release notes, SQLDelight docs during persistence comparison.
- Commands used: `sed`, `rg`, `find`, `git status`, `git commit`, handoff scripts.

## Active Processes

- No dev server, Gradle daemon task, emulator, or adb session was started by this assistant during planning.

## Environment Variables

- No environment variables were required or inspected.

## Related Resources

- Design spec: `docs/superpowers/specs/2026-06-23-android-playback-design.md`
- Implementation plan: `docs/superpowers/plans/2026-06-23-android-playback-implementation.md`
- Playback PRD: `docs/PLAYBACK_PRD.md`
- Main PRD: `docs/PRD.md`
- Previous local audio design: `docs/superpowers/specs/2026-06-22-local-audio-home-display-design.md`
- Existing local audio implementation plan: `docs/superpowers/plans/2026-06-22-local-audio-home-display-implementation.md`
- Android Media3 background playback: https://developer.android.com/media/media3/session/background-playback
- Android media controls: https://developer.android.com/media/implement/surfaces/mobile
- Android custom notification layouts: https://developer.android.com/develop/ui/views/notifications/custom-notification
- Room KMP setup: https://developer.android.com/kotlin/multiplatform/room
- Room3 release notes: https://developer.android.com/jetpack/androidx/releases/room3

---

**Security Reminder**: Before finalizing, run `validate_handoff.py` to check for accidental secret exposure.
