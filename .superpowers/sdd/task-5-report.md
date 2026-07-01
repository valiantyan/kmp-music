# Task 5 Report: Extract PlaybackHistoryRecorder

## What I implemented

- Added `PlaybackHistoryRecorder` at `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorder.kt`.
- Kept the current playback history rules in one collaborator: prepend the new song, remove older duplicates, and cap history at 50 items.
- Added focused tests in `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorderTest.kt` covering:
  - new song is placed at the front
  - duplicate song moves to the front and keeps a single entry
  - history keeps at most fifty songs
- Did not wire `PlaybackHistoryRecorder` into `PlaybackCoordinator` yet, per task scope.

## RED test evidence

Command:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackHistoryRecorderTest"
```

Relevant output:

```text
> Task :composeApp:compileTestKotlinDesktop
e: file:///Users/yanhao/Desktop/demo/kmp-music/composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorderTest.kt:13:24 Unresolved reference 'PlaybackHistoryRecorder'.
e: file:///Users/yanhao/Desktop/demo/kmp-music/composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorderTest.kt:24:24 Unresolved reference 'PlaybackHistoryRecorder'.
e: file:///Users/yanhao/Desktop/demo/kmp-music/composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorderTest.kt:37:24 Unresolved reference 'PlaybackHistoryRecorder'.

> Task :composeApp:compileTestKotlinDesktop FAILED
```

Result:

- RED confirmed. The expected unresolved reference failure appeared before the implementation was added.

## GREEN test evidence

Command:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackHistoryRecorderTest"
```

Relevant output:

```text
> Task :composeApp:desktopTest

BUILD SUCCESSFUL in 16s
```

Result:

- GREEN confirmed. The focused playback-history-recorder test suite passes.

## Files changed

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorder.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorderTest.kt`
- `.superpowers/sdd/task-5-report.md`

## Commit

- 提交主题：`refactor: 抽出最近播放历史记录器`

## Self-review findings

- The collaborator matches the existing playback history rule currently embedded in `PlaybackCoordinator.recordHistory(...)`.
- Visibility is `internal`, consistent with nearby playback collaborators.
- The tests verify ordering, de-duplication, and the 50-item cap against `InMemoryPlaybackRepository`, which keeps the scope focused and avoids wiring changes.
- No unrelated files were modified.

## Concerns

- None for Task 5.
