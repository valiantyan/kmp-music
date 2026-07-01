# Task 4 Report - Extract PlaybackSnapshotWriter

## Status

DONE

## Scope

- Created [PlaybackSnapshotWriter] as a standalone playback-domain collaborator.
- Added focused tests for snapshot throttling, async write tracking, teardown waiting, and error propagation.
- Did not modify [PlaybackCoordinator], per task boundary.

## Files Changed

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriter.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriterTest.kt`

## TDD Record

### RED

Command:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackSnapshotWriterTest"
```

Result:

- `:composeApp:compileTestKotlinDesktop FAILED`
- Unresolved reference `PlaybackSnapshotWriter`
- Follow-on unresolved references for `saveForEvent`, `saveAsync`, `awaitPendingWrites`, and `saveNowAndAwait`

Key evidence:

```text
e: .../PlaybackSnapshotWriterTest.kt:33:21 Unresolved reference 'PlaybackSnapshotWriter'.
e: .../PlaybackSnapshotWriterTest.kt:42:16 Unresolved reference 'saveForEvent'.
e: .../PlaybackSnapshotWriterTest.kt:117:16 Unresolved reference 'saveAsync'.
e: .../PlaybackSnapshotWriterTest.kt:121:20 Unresolved reference 'awaitPendingWrites'.
e: .../PlaybackSnapshotWriterTest.kt:144:20 Unresolved reference 'saveNowAndAwait'.
```

### GREEN

Command:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackSnapshotWriterTest"
```

Result:

- `BUILD SUCCESSFUL`
- Focused test target passed after adding [PlaybackSnapshotWriter]

## Implementation Notes

- Extracted snapshot-writing behavior into [PlaybackSnapshotWriter] with `internal` visibility to match neighboring playback collaborators.
- Preserved the current rules from [PlaybackCoordinator]:
  - first progress event writes immediately
  - progress writes are throttled by `snapshotThrottleMs`
  - non-progress engine events bypass throttling
  - async writes are tracked in a pending set
  - teardown can await all pending writes
  - synchronous teardown save propagates store failures
- Kept repository access synchronous via [PlaybackRepository], and snapshot persistence behind [PlaybackSnapshotStore].

## Focused Test Coverage Added

- `firstProgressEventPersistsSnapshot`
- `progressEventsInsideThrottleWindowAreSkipped`
- `nonProgressEventsAreNotThrottled`
- `awaitPendingWritesWaitsForAsyncSaveCompletion`
- `saveNowAndAwaitPropagatesSnapshotStoreFailure`

## Self Review

- Confirmed only the two task-owned files were modified.
- Confirmed [PlaybackCoordinator] was not changed or wired to the new collaborator.
- Confirmed the extracted collaborator keeps the existing snapshot rules intact instead of introducing a partial patch.
- Confirmed the focused desktop test command passes after the extraction.

## Commit

- `refactor: 抽出播放快照写入器`

## Concerns

- None for this task scope.
