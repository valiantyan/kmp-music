# Task 6 Report: Wire Collaborators Into PlaybackCoordinator

## Status

DONE

## Baseline

- Step 1 command:
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest"`
- Result:
  - PASS before changes

## Changes Made

### `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt`

- Added internal collaborator fields for:
  - `ShuffleQueuePolicy`
  - `PlaybackQueueNavigator`
  - `PlaybackFailurePolicy`
  - `PlaybackSnapshotWriter`
  - `PlaybackHistoryRecorder`
- Removed duplicated coordinator-owned runtime state for:
  - pending snapshot write tracking
  - failure counters
  - progress snapshot throttling timestamp
- Replaced direct shuffle/history logic in:
  - `playSong`
  - `restoreSnapshot`
- Delegated queue navigation behavior in:
  - `moveNext`
  - `movePrevious`
  - `cyclePlaybackMode`
  - `handleCurrentMediaChanged`
  - `handleEnded`
  - `handleFailure`
  - `moveToIndex`
- Added `moveToNavigationResult(...)` so exact-index, next/previous, ended, and failure recovery all share the same repository/engine transition path.
- Delegated queue-removal transition to `PlaybackQueueNavigator.removeSong(...)` while preserving existing engine `setQueue` and play/pause behavior.
- Delegated snapshot behavior to `PlaybackSnapshotWriter`:
  - event-triggered persistence
  - async save
  - sync teardown save
  - pending-write await
- Delegated playback history writes to `PlaybackHistoryRecorder`.
- Delegated successful-playback failure reset to `PlaybackFailurePolicy.reset()`.
- Removed obsolete coordinator-local helper functions that duplicated collaborator behavior.

### `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt`

- Added regression test:
  - `nonLoopSingleSongFailureStaysErrorWithoutRetryingSameSong`
- Added regression test:
  - `removeCurrentSongKeepsRepositoryAndEngineQueueInSync`

## Compatibility Adaptation

- The current `ShuffleQueuePolicy.buildInitialRemaining(...)` does not accept `playbackMode`.
- I adapted Task 6 to the existing API exactly as requested by calling the current collaborator method only where the coordinator already needs shuffle-specific initialization.
- No collaborator APIs were rewritten.

## Verification

### Focused command

- `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.ShuffleQueuePolicyTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackQueueNavigatorTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackFailurePolicyTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackSnapshotWriterTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackHistoryRecorderTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest"`

### Result

- PASS

## Issues Encountered

- First focused test run failed because `PlaybackCoordinator.kt` still needs `PlaybackSnapshot` for `restoreSnapshot(...)`, and I had removed that import while deleting obsolete snapshot-writing code.
- Fixed by restoring the required import.
- No additional source compatibility changes were needed outside the two owned Kotlin files.

## Self-Review

- Verified only the owned coordinator and coordinator test files were changed for code behavior.
- Confirmed collaborator wiring preserves existing queue, shuffle, snapshot, and failure-recovery behavior through the focused test suite.
- Confirmed the new regression coverage protects the two main Task 6 risks:
  - single-song non-loop failure should not retry the same song
  - removing the current song should keep repository state and engine queue aligned

## Commit

- Planned commit message from brief:
  - `refactor: 播放协调器接入内部协作者`

## Review Fix Follow-up

- 修复了 reviewer 指出的 `shuffleRemaining` 回归：
  - `PlaybackCoordinator.playSong(...)`
  - `PlaybackCoordinator.restoreSnapshot(...)`
- 处理方式保持在协调器内部，没有改动协作者 API：
  - 新增本地 helper，只在 `PlaybackMode.Shuffle` 下调用 `ShuffleQueuePolicy.buildInitialRemaining(...)`
  - `LoopAll` / `LoopOne` 明确保持 `shuffleRemaining = emptyList()`
- 补强了非随机模式回归断言：
  - `playSongUsesWholeCurrentListAsQueue`
  - `restoreSnapshotPrimesEngineForResume`
  - 两处都验证非随机模式不会预填 `shuffleRemaining`
- 补强了 `removeCurrentSongKeepsRepositoryAndEngineQueueInSync` 的证据链：
  - 先移除当前歌曲并停在暂停态
  - 再调用 `moveNext()` 与 `advanceUntilIdle()`
  - 断言 repository 当前歌曲推进到精简后队列中的下一首 `songs[2]`
  - 该断言依赖 fake engine 发出的后续 `CurrentMediaChanged` / 状态事件，因此能够证明引擎内部队列也已同步替换，而不是只验证 repository 本地状态

## Review Fix Verification

- Focused command rerun after review fixes:
  - `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.ShuffleQueuePolicyTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackQueueNavigatorTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackFailurePolicyTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackSnapshotWriterTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackHistoryRecorderTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest"`
- Result:
  - PASS
