# Handoff: Android Playback Plan Cross-Review

## Session Metadata
- Created: 2026-06-23 18:25:33
- Project: /Users/yanhao/Desktop/demo/kmp-music
- Branch: main
- Session duration: ~30 minutes

## Recent Commits (for context)
  - 85cda58 完善 Android 播放设计评审
  - ebe6bb8 新增 Android 真实播放设计
  - 66d42eb 更新本地音乐能力说明
  - 44c7ac7 优化本地音乐页列表性能并沉淀诊断技能
  - be57b73 修复 Android 音频权限引导

## Handoff Chain

- **Continues from**: [2026-06-23-174148-android-playback-planning.md](./2026-06-23-174148-android-playback-planning.md)
- **Supersedes**: None

> This handoff captures the follow-up cross-review of the Android playback implementation plan. The previous handoff remains the source of broader planning context and product decisions.

## Current State Summary

The user asked to read the prior handoff, the approved Android playback design spec, and the implementation plan, then perform a three-round cross-review of the implementation document. The review is complete. No production code was changed. The key outcome is that `docs/superpowers/plans/2026-06-23-android-playback-implementation.md` should not be executed as-is: it is detailed, but several P0 links are missing between Android Media3 service, shared `PlaybackCoordinator`, notification refresh, Room snapshot writes, and MediaSession/system commands.

## Codebase Understanding

## Architecture Overview

KMP Music is still in the pre-real-playback state. `commonMain` owns domain models, repositories, use cases, shared UI, navigation, and current memory-only playback state. Android scanner output already contains playable `content://` URIs on `Song.localUri`. The design spec requires Android real playback to flow through a platform-neutral `AudioPlayerEngine`, a common `PlaybackCoordinator`, Room3 persistence, and Android `MediaSessionService`/Media3. The current implementation plan introduces those pieces, but some of the proposed task snippets leave them disconnected.

## Critical Files

| File | Purpose | Relevance |
|------|---------|-----------|
| `handoff/2026-06-23-174148-android-playback-planning.md` | Previous planning handoff | Contains approved scope, decisions, and gotchas for Android playback |
| `docs/superpowers/specs/2026-06-23-android-playback-design.md` | Approved design spec | Source of requirements used for review |
| `docs/superpowers/plans/2026-06-23-android-playback-implementation.md` | Implementation plan under review | Needs revision before execution |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt` | Current memory-only playback model | Still only has `currentSongId` and `isPlaying`; plan upgrades it |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/PlaybackUseCases.kt` | Current memory-only playback use cases | Still inserts a played song at queue head; must be replaced/delegated fully |
| `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt` | UI state controller | Must become the single bridge to coordinator and persistence; current plan uses unsafe ad-hoc `MainScope` |
| `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/MusicAppViewModel.kt` | Android lifetime holder for controller | Current plan does not inject the real service engine into controller |
| `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/MainActivity.kt` | Android Activity entry | Current plan starts foreground service too early without immediate foreground notification |

## Key Patterns Discovered

- Follow the project rule: root-cause fixes only. Do not patch a single UI callback or notification button while leaving separate playback truths.
- Keep platform APIs out of `commonMain`; Android-specific ExoPlayer, service, notification, and permission work stays in `androidMain`.
- UI, notification actions, and MediaSession/system commands must all enter the same coordinator semantics.
- Current list-backed queue semantics are product-critical: the rendered list becomes queue and clicked item becomes `currentIndex`.
- Build verification alone is not enough for this work. Real playback, background service, notification layouts, and process death restore require Android device verification or an explicit risk note.

## Work Completed

## Tasks Finished

- [x] Read the previous handoff completely.
- [x] Read the approved Android playback design spec.
- [x] Read the implementation plan task structure and high-risk code snippets.
- [x] Cross-checked the implementation plan against current source files and the design spec.
- [x] Reported a three-round review with P0/P1 issues and line references.
- [x] Created this continuation handoff.

## Files Modified

| File | Changes | Rationale |
|------|---------|-----------|
| `handoff/2026-06-23-182533-android-playback-plan-review.md` | New handoff document | Preserve review findings and next steps before plan revision/execution |

## Decisions Made

| Decision | Options Considered | Rationale |
|----------|-------------------|-----------|
| Do not execute the current implementation plan as-is | Execute now, revise during implementation, revise plan first | The plan has multiple P0 wiring gaps that could produce a compiling but non-functional Android playback stack |
| Treat Android engine injection and event subscription as P0 blockers | Leave as implementation detail, or make explicit before execution | The design requires engine events as playback truth; current snippets keep the controller on fake/default engine |
| Keep review focused on plan correctness, not code edits | Patch plan immediately, start coding, or hand off findings | User requested cross-review; no implementation or plan rewrite was explicitly requested in that turn |

## Pending Work

## Immediate Next Steps

1. Revise `docs/superpowers/plans/2026-06-23-android-playback-implementation.md` before execution.
2. Add explicit Android playback bridge steps: start/bind service safely, obtain the real `Media3AudioPlayerEngine`, inject it into `MusicAppController`, and call `PlaybackCoordinator.start(scope)`.
3. Add explicit notification refresh and foreground-service lifecycle steps: show/update custom notification on playback status, current song, favorite, and mode changes; avoid `startForegroundService` without timely `startForeground`.
4. Add explicit MediaSession callback routing so system play/pause/previous/next commands go through coordinator semantics, not directly through ExoPlayer.
5. Add explicit Room snapshot write strategy for engine events, auto-skip, pause/seek/stop/service stop, and 5-second progress throttling.

## Blockers/Open Questions

- [ ] The implementation plan has not been revised after review.
- [ ] Execution mode is still not chosen. Previous handoff suggested subagent-driven development or inline execution, but this review adds a plan-revision step first.
- [ ] Room3/KSP plugin names and generated constructor behavior still need Gradle verification during implementation.
- [ ] Android device availability is unknown; if unavailable, final implementation must include explicit residual verification risk.

## Deferred Items

- Android playback implementation is deferred until the plan is corrected.
- iOS and Desktop real playback remain out of scope for this Android-first slice.
- Quick Settings and lock-screen customization remain out of scope; only natural MediaSession surfaces may appear.

## Context for Resuming Agent

## Important Context

The critical review finding is that the implementation plan is not ready to run. Its largest flaw is disconnection: it creates a service/engine, controller/coordinator, notification builder, and Room store, but does not reliably wire them into one runtime graph. In particular, `MusicAppViewModel` still constructs `MusicAppController` with default fake engine in the plan; `PlaybackCoordinator.start(scope)` is never clearly called; the notification is buildable but not refreshed by state changes; and MediaSession commands may be handled by ExoPlayer directly instead of coordinator rules. Fix the plan first, then implement.

Review issues reported to the user:

- P0: Android true playback engine is not injected into `MusicAppController`.
- P0: Custom notification has no real refresh/display path tied to playback/favorite/mode changes.
- P0: `PlaybackCoordinator` engine event collection is defined but not started.
- P0: MediaSession/system controls can bypass coordinator semantics.
- P0: `startForegroundService` is proposed without a guaranteed immediate `startForeground`.
- P1: Room snapshot write timing is incomplete compared with the spec.
- P1: favorites persistence test only exercises in-memory repository.
- P1: error classification maps permission errors incorrectly and failure counters are not clearly reset after success.

The final user-facing review answer was concise and did not modify the plan. If the next user asks to proceed, first ask whether to revise the plan document or just implement with these corrections in mind. Given project instructions, revising the plan is safer.

## Assumptions Made

- The user invoked `session-handoff` to save the current review state, not to resume another handoff.
- The active branch remains `main`, ahead of origin by 2 commits.
- The implementation plan and several handoff files are untracked; they should not be staged or committed unless the user asks.
- No production code was changed during review.

## Potential Gotchas

- The scaffold script did not auto-link the previous handoff when passed `handoff/2026-06-23-174148-android-playback-planning.md`; this file manually records the correct chain.
- The current `PlaybackUseCases.kt` still performs old memory-only behavior. Do not leave it active beside coordinator-based playback.
- Avoid creating new uncontrolled `MainScope()` instances in `MusicAppController`; inject a scope or use a lifecycle-aware owner.
- A compile-only implementation may still fail product acceptance because notification, background service, and process death restore are runtime behaviors.
- Android notification permission is separate from audio read permission.
- RemoteViews custom notification can satisfy fixed app notification layout, but Quick Settings/lock-screen UI cannot be controlled.

## Environment State

## Tools/Services Used

- Skill used: `session-handoff`.
- Commands used: `sed`, `rg`, `nl`, `wc`, `git status`, scaffold script, and handoff validation script.
- Web checks: Android foreground service / Media3 / Room references were consulted during review only for current platform facts.

## Active Processes

- No dev server, Gradle task, emulator, adb session, or background process was started.

## Environment Variables

- No environment variables were inspected or required.

## Related Resources

- Previous handoff: `handoff/2026-06-23-174148-android-playback-planning.md`
- Design spec: `docs/superpowers/specs/2026-06-23-android-playback-design.md`
- Implementation plan needing revision: `docs/superpowers/plans/2026-06-23-android-playback-implementation.md`
- Product playback PRD: `docs/PLAYBACK_PRD.md`
- Current controller: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- Current playback use cases: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/PlaybackUseCases.kt`
- Android ViewModel: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/MusicAppViewModel.kt`
- Android Activity: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/MainActivity.kt`

---

**Security Reminder**: Before finalizing, run `validate_handoff.py` to check for accidental secret exposure.
