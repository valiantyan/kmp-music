# Task 6 Report: Extract Playback UI Sync And Restore Orchestration

## What I implemented

- Added `PlaybackUiStateSynchronizer` to project `PlaybackState` plus repository `QueueState` into `MusicAppUiState`.
- Added `PlaybackRestoreOrchestrator` to coordinate saved queue ids, available song resolution, pending restore state, and the call into playback restoration.
- Wired `MusicAppController` to delegate playback UI sync and snapshot restore orchestration while keeping the facade as the only Compose `mutableStateOf` owner.
- Replaced controller-local preferred song collection with `MusicLibraryProjector.buildDetailSongs`.
- Removed controller-local `buildRecentSongs`, `knownSongsForRecentPlayback`, `resolveAvailableSongsByIds`, and `buildFavoriteSongs` after their call sites moved to collaborators.
- Added focused playback UI and restore orchestrator tests.
- Updated Task 6 checkboxes in `docs/superpowers/plans/2026-06-30-codebase-architecture-optimization-phase1.md`.

## RED test evidence

Command:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.playback.MusicAppPlaybackUiStateSynchronizerTest" --tests "com.yanhao.kmpmusic.feature.app.playback.MusicAppPlaybackRestoreOrchestratorTest"
```

Relevant failure before production classes existed:

```text
Unresolved reference 'PlaybackUiStateSynchronizer'
Unresolved reference 'PlaybackRestoreOrchestrator'
```

Result: RED confirmed with the expected missing collaborator references.

## GREEN test evidence

Command:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.playback.MusicAppPlaybackUiStateSynchronizerTest" --tests "com.yanhao.kmpmusic.feature.app.playback.MusicAppPlaybackRestoreOrchestratorTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.restorePlaybackSnapshotAllowsResume" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.restorePlaybackSnapshotDoesNotAutoScanWhenLibraryIsEmpty" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.restorePlaybackSnapshotRestoresSavedSongOutsidePreviewWithoutFullLibraryLoad"
```

Relevant output:

```text
BUILD SUCCESSFUL in 6s
```

Notes:

- The command emitted existing Gradle MPP deprecation warnings.
- The command emitted the existing `PlaybackCoordinator.kt` Elvis-operator warnings.
- A first GREEN attempt exposed that writing `uiState = result.state` after `playbackCoordinator.restoreSnapshot` would overwrite the coordinator callback's restored playback fields. The final implementation merges only `queueSongsSnapshot` from the orchestrator result so pause status, current song, and restored position remain intact.

## Files changed

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackUiStateSynchronizer.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackRestoreOrchestrator.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackUiStateSynchronizerTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackRestoreOrchestratorTest.kt`
- `docs/superpowers/plans/2026-06-30-codebase-architecture-optimization-phase1.md`
- `.superpowers/sdd/task-6-report.md`

## Self-review findings

- `PlaybackUiStateSynchronizer` and `PlaybackRestoreOrchestrator` do not own Compose mutable state; they return immutable results.
- `MusicAppController` remains the public facade and still publishes playback UI state after sync.
- Snapshot restore still does not auto-scan when saved songs are unavailable; it marks restore as pending.
- Snapshot restore still allows resume from restored paused position.
- Controller helper deletion follows the task boundary: favorite and available-song resolution now live behind existing collaborators.

## Concerns

- The plan's example assigns `uiState = result.state` in `restorePlaybackSnapshot`; preserving existing behavior requires merging only the orchestrator-owned queue snapshot because `restoreSnapshot` synchronously publishes playback fields through the controller callback before the method returns.
