# Codebase Architecture Optimization Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thin `PlaybackCoordinator` into a common-layer playback facade by extracting queue navigation, shuffle policy, failure recovery, snapshot writing, and recent-history recording into focused collaborators without changing playback behavior.

**Architecture:** Keep `PlaybackCoordinator` as the only public playback coordinator used by `MusicAppController` and platform hosts. New collaborators live beside it in `domain/playback`, own one rule family each, and either return pure state transitions or perform one bounded repository/store responsibility. Engine event orchestration, repository write order, `Song` to `PlayableMedia` mapping, and `AudioPlayerEngine` commands stay in `PlaybackCoordinator`.

**Tech Stack:** Kotlin Multiplatform 2.0.21, Compose Multiplatform common domain layer, Kotlin test, kotlinx.coroutines test, existing `PlaybackRepository`, `PlaybackSnapshotStore`, and `AudioPlayerEngine`.

---

## Scope Check

This plan implements `docs/superpowers/specs/2026-07-01-codebase-architecture-optimization-phase2-design.md` using the recommended方案 C: extract stable playback rules first and keep event orchestration in `PlaybackCoordinator`.

This plan does not change:

- `AudioPlayerEngine` method signatures or event types.
- `PlaybackRepository` method signatures.
- Android Media3, MediaSession, notification, or media-button wiring.
- Desktop vlcj adapter event filtering or playback intent tracking.
- Product semantics for `PlaybackState.isPlaying`, `LoopAll`, shuffle, failure thresholds, or teardown snapshots.
- UI state synchronization in `feature/app`.

## File Structure

Create these production files:

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicy.kt`  
  Pure shuffle invariants: initial remaining set, next target selection, history/remaining transitions.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackQueueNavigator.kt`  
  Pure queue transition facade: next, previous, exact index, engine current-media transition, playback-mode reset, remove-song queue state.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackFailurePolicy.kt`  
  Stateful runtime failure counters and recovery decisions.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriter.kt`  
  Snapshot IO collaborator: async writes, progress throttling, pending-write draining, sync teardown write.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorder.kt`  
  Repository collaborator for recent playback history ordering, de-dupe, and limit.

Modify this production file:

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt`  
  Instantiate collaborators, delegate extracted rule families, preserve public methods and constructor compatibility, remove stale private state/functions, and clean invalid Elvis warnings in teardown paths.

Create these focused tests:

- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicyTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackQueueNavigatorTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackFailurePolicyTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriterTest.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorderTest.kt`

Modify this existing test file:

- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt`  
  Keep facade-level acceptance tests. Remove or narrow tests that become pure strategy duplication after focused tests cover them.

## Commit Cadence

Use one commit per task. Each task must leave targeted tests passing before committing. The final task runs the full phase verification:

```bash
./gradlew :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid
```

---

### Task 1: Extract ShuffleQueuePolicy

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicy.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicyTest.kt`

- [x] **Step 1: Write the failing shuffle policy tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicyTest.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.QueueState
import kotlin.test.Test
import kotlin.test.assertEquals

class ShuffleQueuePolicyTest {
    @Test
    fun initialRemainingExcludesCurrentIndexOnlyInShuffleMode(): Unit {
        val policy = ShuffleQueuePolicy(randomIndex = { candidates: List<Int> -> candidates.first() })

        assertEquals(
            expected = listOf(0, 2, 3),
            actual = policy.buildInitialRemaining(
                playbackMode = PlaybackMode.Shuffle,
                queueSize = 4,
                currentIndex = 1,
            ),
        )
        assertEquals(
            expected = emptyList(),
            actual = policy.buildInitialRemaining(
                playbackMode = PlaybackMode.LoopAll,
                queueSize = 4,
                currentIndex = 1,
            ),
        )
    }

    @Test
    fun nextTargetUsesInjectedRandomCandidateFromRemaining(): Unit {
        val policy = ShuffleQueuePolicy(randomIndex = { candidates: List<Int> -> candidates.last() })
        val queueState = QueueState(
            songIds = listOf("a", "b", "c", "d"),
            currentIndex = 0,
            playbackMode = PlaybackMode.Shuffle,
            shuffleRemaining = listOf(1, 2, 3),
        )

        assertEquals(expected = 3, actual = policy.nextTargetIndex(queueState = queueState))
    }

    @Test
    fun forwardTransitionPushesHistoryAndRemovesTargetFromRemaining(): Unit {
        val policy = ShuffleQueuePolicy(randomIndex = { candidates: List<Int> -> candidates.first() })
        val queueState = QueueState(
            songIds = listOf("a", "b", "c", "d"),
            currentIndex = 0,
            playbackMode = PlaybackMode.Shuffle,
            shuffleHistory = emptyList(),
            shuffleRemaining = listOf(1, 2, 3),
        )

        val nextState = policy.applyTransition(
            queueState = queueState,
            targetIndex = 2,
            isMovingBackward = false,
        )

        assertEquals(expected = 2, actual = nextState.currentIndex)
        assertEquals(expected = listOf(0), actual = nextState.shuffleHistory)
        assertEquals(expected = listOf(1, 3), actual = nextState.shuffleRemaining)
    }

    @Test
    fun backwardTransitionPopsHistoryAndReturnsLeavingIndexToRemaining(): Unit {
        val policy = ShuffleQueuePolicy(randomIndex = { candidates: List<Int> -> candidates.first() })
        val queueState = QueueState(
            songIds = listOf("a", "b", "c", "d"),
            currentIndex = 2,
            playbackMode = PlaybackMode.Shuffle,
            shuffleHistory = listOf(0),
            shuffleRemaining = listOf(1, 3),
        )

        val nextState = policy.applyTransition(
            queueState = queueState,
            targetIndex = 0,
            isMovingBackward = true,
        )

        assertEquals(expected = 0, actual = nextState.currentIndex)
        assertEquals(expected = emptyList(), actual = nextState.shuffleHistory)
        assertEquals(expected = listOf(1, 3, 2), actual = nextState.shuffleRemaining)
    }

    @Test
    fun emptyRemainingStartsNextRoundWithoutRepeatingCurrentIndex(): Unit {
        val policy = ShuffleQueuePolicy(randomIndex = { candidates: List<Int> -> candidates.first() })
        val queueState = QueueState(
            songIds = listOf("a", "b", "c"),
            currentIndex = 1,
            playbackMode = PlaybackMode.Shuffle,
            shuffleRemaining = emptyList(),
        )

        assertEquals(expected = 0, actual = policy.nextTargetIndex(queueState = queueState))

        val nextState = policy.applyTransition(
            queueState = queueState,
            targetIndex = 0,
            isMovingBackward = false,
        )

        assertEquals(expected = 0, actual = nextState.currentIndex)
        assertEquals(expected = listOf(1), actual = nextState.shuffleHistory)
        assertEquals(expected = listOf(1, 2), actual = nextState.shuffleRemaining)
    }
}
```

- [x] **Step 2: Run the shuffle policy test and verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.ShuffleQueuePolicyTest"
```

Expected: FAIL with unresolved reference `ShuffleQueuePolicy`.

Actual: Completed with follow-up failure evidence recorded in
`.superpowers/sdd/2026-07-01-codebase-architecture-optimization-phase2-design/task-1-report.md`.
The initial exact-class `--tests` filter did not match tests, so the accepted RED evidence is the
temporary mutation of `ShuffleQueuePolicy.buildInitialRemaining()` that made `ShuffleQueuePolicyTest`
fail before restoring the correct implementation.

- [x] **Step 3: Add `ShuffleQueuePolicy`**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicy.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.QueueState

/**
 * Maintains shuffle queue invariants independent of repository and engine state.
 */
class ShuffleQueuePolicy(
    private val randomIndex: (List<Int>) -> Int,
) {
    fun buildInitialRemaining(
        playbackMode: PlaybackMode,
        queueSize: Int,
        currentIndex: Int,
    ): List<Int> {
        if (playbackMode != PlaybackMode.Shuffle) {
            return emptyList()
        }
        return (0 until queueSize).filterNot { index: Int -> index == currentIndex }
    }

    fun nextTargetIndex(queueState: QueueState): Int {
        if (queueState.songIds.isEmpty()) {
            return -1
        }
        val candidates: List<Int> = queueState.shuffleRemaining.ifEmpty {
            queueState.songIds.indices.filterNot { index: Int -> index == queueState.currentIndex }
        }
        return candidates.firstOrNull()?.let {
            randomIndex(candidates)
        } ?: queueState.currentIndex.coerceAtLeast(minimumValue = 0)
    }

    fun applyTransition(
        queueState: QueueState,
        targetIndex: Int,
        isMovingBackward: Boolean,
    ): QueueState {
        if (targetIndex !in queueState.songIds.indices) {
            return queueState
        }
        if (isMovingBackward && queueState.shuffleHistory.isNotEmpty()) {
            val rebuiltRemaining: List<Int> = (queueState.shuffleRemaining + queueState.currentIndex)
                .distinct()
                .filterNot { index: Int -> index == targetIndex }
            return queueState.copy(
                currentIndex = targetIndex,
                shuffleHistory = queueState.shuffleHistory.dropLast(n = 1),
                shuffleRemaining = rebuiltRemaining,
            )
        }
        val history: List<Int> = queueState.currentIndex.takeIf { index: Int -> index >= 0 }?.let { index: Int ->
            queueState.shuffleHistory + index
        } ?: queueState.shuffleHistory
        val remaining: List<Int> = queueState.shuffleRemaining.filterNot { index: Int ->
            index == targetIndex
        }.ifEmpty {
            queueState.songIds.indices.filterNot { index: Int -> index == targetIndex }
        }
        return queueState.copy(
            currentIndex = targetIndex,
            shuffleHistory = history,
            shuffleRemaining = remaining,
        )
    }
}
```

- [x] **Step 4: Run the shuffle policy test and verify it passes**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.ShuffleQueuePolicyTest"
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicy.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/ShuffleQueuePolicyTest.kt
git commit -m "refactor: 抽出随机播放队列策略"
```

---

### Task 2: Extract PlaybackQueueNavigator

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackQueueNavigator.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackQueueNavigatorTest.kt`

- [x] **Step 1: Write the failing queue navigator tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackQueueNavigatorTest.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.QueueState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackQueueNavigatorTest {
    private val navigator = PlaybackQueueNavigator(
        shufflePolicy = ShuffleQueuePolicy(randomIndex = { candidates: List<Int> -> candidates.first() }),
    )

    @Test
    fun sequenceNextLoopsFromLastToFirst(): Unit {
        val result = navigator.next(
            queueState = QueueState(
                songIds = listOf("a", "b", "c"),
                currentIndex = 2,
                playbackMode = PlaybackMode.LoopAll,
            ),
        )

        assertEquals(expected = 0, actual = result?.targetIndex)
        assertEquals(expected = 0, actual = result?.queueState?.currentIndex)
    }

    @Test
    fun sequencePreviousLoopsFromFirstToLast(): Unit {
        val result = navigator.previous(
            queueState = QueueState(
                songIds = listOf("a", "b", "c"),
                currentIndex = 0,
                playbackMode = PlaybackMode.LoopAll,
            ),
        )

        assertEquals(expected = 2, actual = result?.targetIndex)
        assertEquals(expected = 2, actual = result?.queueState?.currentIndex)
    }

    @Test
    fun loopOneNextKeepsCurrentWhenRequested(): Unit {
        val result = navigator.next(
            queueState = QueueState(
                songIds = listOf("a", "b", "c"),
                currentIndex = 1,
                playbackMode = PlaybackMode.LoopOne,
            ),
            keepLoopOneCurrent = true,
        )

        assertEquals(expected = 1, actual = result?.targetIndex)
        assertEquals(expected = 1, actual = result?.queueState?.currentIndex)
    }

    @Test
    fun exactIndexRejectsOutOfRangeIndex(): Unit {
        val result = navigator.exactIndex(
            queueState = QueueState(songIds = listOf("a", "b"), currentIndex = 0),
            targetIndex = 4,
        )

        assertNull(actual = result)
    }

    @Test
    fun exactIndexForCurrentShuffleItemDoesNotPolluteHistory(): Unit {
        val result = navigator.exactIndex(
            queueState = QueueState(
                songIds = listOf("a", "b", "c"),
                currentIndex = 1,
                playbackMode = PlaybackMode.Shuffle,
                shuffleHistory = listOf(0),
                shuffleRemaining = listOf(2),
            ),
            targetIndex = 1,
        )

        assertEquals(expected = 1, actual = result?.targetIndex)
        assertEquals(expected = listOf(0), actual = result?.queueState?.shuffleHistory)
        assertEquals(expected = listOf(2), actual = result?.queueState?.shuffleRemaining)
    }

    @Test
    fun engineTransitionUpdatesShuffleHistoryAndRemaining(): Unit {
        val result = navigator.engineTransition(
            queueState = QueueState(
                songIds = listOf("a", "b", "c", "d"),
                currentIndex = 0,
                playbackMode = PlaybackMode.Shuffle,
                shuffleRemaining = listOf(1, 2, 3),
            ),
            targetIndex = 2,
        )

        assertEquals(expected = 2, actual = result?.queueState?.currentIndex)
        assertEquals(expected = listOf(0), actual = result?.queueState?.shuffleHistory)
        assertEquals(expected = listOf(1, 3), actual = result?.queueState?.shuffleRemaining)
    }

    @Test
    fun modeChangeResetsShuffleHistoryAndBuildsRemainingForNewMode(): Unit {
        val result = navigator.changePlaybackMode(
            queueState = QueueState(
                songIds = listOf("a", "b", "c"),
                currentIndex = 1,
                playbackMode = PlaybackMode.LoopOne,
                shuffleHistory = listOf(0),
                shuffleRemaining = listOf(2),
            ),
            playbackMode = PlaybackMode.Shuffle,
        )

        assertEquals(expected = PlaybackMode.Shuffle, actual = result.playbackMode)
        assertEquals(expected = emptyList(), actual = result.shuffleHistory)
        assertEquals(expected = listOf(0, 2), actual = result.shuffleRemaining)
    }

    @Test
    fun removeCurrentSongSelectsFirstResolvedSongAndResetsShuffleState(): Unit {
        val result = navigator.removeSong(
            queueState = QueueState(
                songIds = listOf("a", "b", "c", "d"),
                currentIndex = 2,
                playbackMode = PlaybackMode.Shuffle,
                shuffleHistory = listOf(0, 1),
                shuffleRemaining = listOf(3),
            ),
            removedSongId = "c",
            currentSongId = "c",
            nextSongIds = listOf("a", "b", "d"),
        )

        assertEquals(expected = 0, actual = result?.targetIndex)
        assertEquals(expected = listOf("a", "b", "d"), actual = result?.queueState?.songIds)
        assertEquals(expected = 0, actual = result?.queueState?.currentIndex)
        assertEquals(expected = emptyList(), actual = result?.queueState?.shuffleHistory)
        assertEquals(expected = listOf(1, 2), actual = result?.queueState?.shuffleRemaining)
    }

    @Test
    fun removeNonCurrentSongKeepsCurrentSongWhenItStillExists(): Unit {
        val result = navigator.removeSong(
            queueState = QueueState(
                songIds = listOf("a", "b", "c"),
                currentIndex = 1,
                playbackMode = PlaybackMode.LoopAll,
            ),
            removedSongId = "c",
            currentSongId = "b",
            nextSongIds = listOf("a", "b"),
        )

        assertEquals(expected = 1, actual = result?.targetIndex)
        assertEquals(expected = listOf("a", "b"), actual = result?.queueState?.songIds)
        assertEquals(expected = 1, actual = result?.queueState?.currentIndex)
    }

    @Test
    fun recoverableNextTargetRequiresDifferentSongOutsideLoopOne(): Unit {
        assertEquals(
            expected = false,
            actual = navigator.hasDifferentNextTarget(
                queueState = QueueState(
                    songIds = listOf("a"),
                    currentIndex = 0,
                    playbackMode = PlaybackMode.LoopAll,
                ),
            ),
        )
        assertEquals(
            expected = true,
            actual = navigator.hasDifferentNextTarget(
                queueState = QueueState(
                    songIds = listOf("a", "b"),
                    currentIndex = 0,
                    playbackMode = PlaybackMode.LoopAll,
                ),
            ),
        )
    }
}
```

- [x] **Step 2: Run the queue navigator test and verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackQueueNavigatorTest"
```

Expected: FAIL with unresolved reference `PlaybackQueueNavigator`.

- [x] **Step 3: Add `PlaybackQueueNavigator`**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackQueueNavigator.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.QueueState

/**
 * Calculates queue state transitions without touching repositories or playback engines.
 */
class PlaybackQueueNavigator(
    private val shufflePolicy: ShuffleQueuePolicy,
) {
    fun next(
        queueState: QueueState,
        keepLoopOneCurrent: Boolean = false,
    ): QueueNavigationResult? {
        if (queueState.songIds.isEmpty()) {
            return null
        }
        val targetIndex: Int = when {
            keepLoopOneCurrent && queueState.playbackMode == PlaybackMode.LoopOne -> queueState.currentIndex
            queueState.playbackMode == PlaybackMode.Shuffle -> shufflePolicy.nextTargetIndex(queueState = queueState)
            else -> (queueState.currentIndex + 1 + queueState.songIds.size) % queueState.songIds.size
        }
        return exactIndex(
            queueState = queueState,
            targetIndex = targetIndex,
            isMovingBackward = false,
        )
    }

    fun previous(queueState: QueueState): QueueNavigationResult? {
        if (queueState.songIds.isEmpty()) {
            return null
        }
        val targetIndex: Int = if (
            queueState.playbackMode == PlaybackMode.Shuffle &&
            queueState.shuffleHistory.isNotEmpty()
        ) {
            queueState.shuffleHistory.last()
        } else {
            (queueState.currentIndex - 1 + queueState.songIds.size) % queueState.songIds.size
        }
        return exactIndex(
            queueState = queueState,
            targetIndex = targetIndex,
            isMovingBackward = true,
        )
    }

    fun exactIndex(
        queueState: QueueState,
        targetIndex: Int,
        isMovingBackward: Boolean = false,
    ): QueueNavigationResult? {
        if (targetIndex !in queueState.songIds.indices) {
            return null
        }
        if (targetIndex == queueState.currentIndex) {
            return QueueNavigationResult(
                queueState = queueState.copy(currentIndex = targetIndex),
                targetIndex = targetIndex,
            )
        }
        val nextQueueState: QueueState = if (queueState.playbackMode == PlaybackMode.Shuffle) {
            shufflePolicy.applyTransition(
                queueState = queueState,
                targetIndex = targetIndex,
                isMovingBackward = isMovingBackward,
            )
        } else {
            queueState.copy(currentIndex = targetIndex)
        }
        return QueueNavigationResult(
            queueState = nextQueueState,
            targetIndex = targetIndex,
        )
    }

    fun engineTransition(
        queueState: QueueState,
        targetIndex: Int,
    ): QueueNavigationResult? {
        if (targetIndex !in queueState.songIds.indices) {
            return null
        }
        if (queueState.playbackMode != PlaybackMode.Shuffle || targetIndex == queueState.currentIndex) {
            return QueueNavigationResult(
                queueState = queueState.copy(currentIndex = targetIndex),
                targetIndex = targetIndex,
            )
        }
        return exactIndex(
            queueState = queueState,
            targetIndex = targetIndex,
            isMovingBackward = false,
        )
    }

    fun changePlaybackMode(
        queueState: QueueState,
        playbackMode: PlaybackMode,
    ): QueueState {
        return queueState.copy(
            playbackMode = playbackMode,
            shuffleHistory = emptyList(),
            shuffleRemaining = shufflePolicy.buildInitialRemaining(
                playbackMode = playbackMode,
                queueSize = queueState.songIds.size,
                currentIndex = queueState.currentIndex,
            ),
        )
    }

    fun removeSong(
        queueState: QueueState,
        removedSongId: String,
        currentSongId: String?,
        nextSongIds: List<String>,
    ): QueueNavigationResult? {
        if (removedSongId !in queueState.songIds || nextSongIds.isEmpty()) {
            return null
        }
        val nextCurrentSongId: String = if (currentSongId == removedSongId) {
            nextSongIds.first()
        } else {
            currentSongId?.takeIf { songId: String -> songId in nextSongIds } ?: nextSongIds.first()
        }
        val nextCurrentIndex: Int = nextSongIds.indexOf(nextCurrentSongId).coerceAtLeast(minimumValue = 0)
        val nextQueueState: QueueState = queueState.copy(
            songIds = nextSongIds,
            currentIndex = nextCurrentIndex,
            shuffleHistory = emptyList(),
            shuffleRemaining = shufflePolicy.buildInitialRemaining(
                playbackMode = queueState.playbackMode,
                queueSize = nextSongIds.size,
                currentIndex = nextCurrentIndex,
            ),
        )
        return QueueNavigationResult(
            queueState = nextQueueState,
            targetIndex = nextCurrentIndex,
        )
    }

    fun hasDifferentNextTarget(queueState: QueueState): Boolean {
        if (queueState.songIds.size <= 1) {
            return false
        }
        if (queueState.playbackMode == PlaybackMode.LoopOne) {
            return false
        }
        return queueState.songIds.indices.any { index: Int -> index != queueState.currentIndex }
    }
}

data class QueueNavigationResult(
    val queueState: QueueState,
    val targetIndex: Int,
)
```

- [x] **Step 4: Run queue and shuffle tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackQueueNavigatorTest" --tests "com.yanhao.kmpmusic.domain.playback.ShuffleQueuePolicyTest"
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackQueueNavigator.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackQueueNavigatorTest.kt
git commit -m "refactor: 抽出播放队列导航策略"
```

---

### Task 3: Extract PlaybackFailurePolicy

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackFailurePolicy.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackFailurePolicyTest.kt`

- [x] **Step 1: Write the failing failure-policy tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackFailurePolicyTest.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackFailurePolicyTest {
    @Test
    fun loopOneRetriesSameSongUntilThirdFailure(): Unit {
        val policy = PlaybackFailurePolicy()
        val error = playbackError(songId = "a")

        assertEquals(
            expected = PlaybackFailureDecision.RetryCurrent,
            actual = policy.onFailure(error = error, playbackMode = PlaybackMode.LoopOne, hasRecoverableTarget = true),
        )
        assertEquals(
            expected = PlaybackFailureDecision.RetryCurrent,
            actual = policy.onFailure(error = error, playbackMode = PlaybackMode.LoopOne, hasRecoverableTarget = true),
        )
        assertEquals(
            expected = PlaybackFailureDecision.StayError,
            actual = policy.onFailure(error = error, playbackMode = PlaybackMode.LoopOne, hasRecoverableTarget = true),
        )
    }

    @Test
    fun loopOneNewSongStartsOwnFailureWindow(): Unit {
        val policy = PlaybackFailurePolicy()

        policy.onFailure(error = playbackError(songId = "a"), playbackMode = PlaybackMode.LoopOne, hasRecoverableTarget = true)

        assertEquals(
            expected = PlaybackFailureDecision.RetryCurrent,
            actual = policy.onFailure(
                error = playbackError(songId = "b"),
                playbackMode = PlaybackMode.LoopOne,
                hasRecoverableTarget = true,
            ),
        )
    }

    @Test
    fun nonLoopOneSkipsUntilThirdConsecutiveFailure(): Unit {
        val policy = PlaybackFailurePolicy()

        assertEquals(
            expected = PlaybackFailureDecision.SkipToNext,
            actual = policy.onFailure(error = playbackError(songId = "a"), playbackMode = PlaybackMode.LoopAll, hasRecoverableTarget = true),
        )
        assertEquals(
            expected = PlaybackFailureDecision.SkipToNext,
            actual = policy.onFailure(error = playbackError(songId = "b"), playbackMode = PlaybackMode.LoopAll, hasRecoverableTarget = true),
        )
        assertEquals(
            expected = PlaybackFailureDecision.StayError,
            actual = policy.onFailure(error = playbackError(songId = "c"), playbackMode = PlaybackMode.LoopAll, hasRecoverableTarget = true),
        )
    }

    @Test
    fun nonLoopOneStaysErrorWhenNoNextSongExists(): Unit {
        val policy = PlaybackFailurePolicy()

        assertEquals(
            expected = PlaybackFailureDecision.StayError,
            actual = policy.onFailure(error = playbackError(songId = "a"), playbackMode = PlaybackMode.LoopAll, hasRecoverableTarget = false),
        )
    }

    @Test
    fun resetAfterSuccessfulPlayingClearsCounters(): Unit {
        val policy = PlaybackFailurePolicy()

        policy.onFailure(error = playbackError(songId = "a"), playbackMode = PlaybackMode.LoopAll, hasRecoverableTarget = true)
        policy.onFailure(error = playbackError(songId = "b"), playbackMode = PlaybackMode.LoopAll, hasRecoverableTarget = true)
        policy.reset()

        assertEquals(
            expected = PlaybackFailureDecision.SkipToNext,
            actual = policy.onFailure(error = playbackError(songId = "c"), playbackMode = PlaybackMode.LoopAll, hasRecoverableTarget = true),
        )
    }

    private fun playbackError(songId: String): PlaybackError {
        return PlaybackError(
            type = PlaybackErrorType.Unknown,
            songId = songId,
            message = "坏文件",
        )
    }
}
```

- [x] **Step 2: Run the failure-policy test and verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackFailurePolicyTest"
```

Expected: FAIL with unresolved reference `PlaybackFailurePolicy`.

- [x] **Step 3: Add `PlaybackFailurePolicy`**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackFailurePolicy.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackMode

/**
 * Owns runtime playback failure counters and converts failures into recovery decisions.
 */
class PlaybackFailurePolicy {
    private var loopOneFailureCount: Int = 0
    private var consecutiveFailedSongCount: Int = 0
    private var lastFailedSongId: String? = null

    fun onFailure(
        error: PlaybackError,
        playbackMode: PlaybackMode,
        hasRecoverableTarget: Boolean,
    ): PlaybackFailureDecision {
        if (playbackMode == PlaybackMode.LoopOne) {
            loopOneFailureCount = if (lastFailedSongId == error.songId) loopOneFailureCount + 1 else 1
            lastFailedSongId = error.songId
            return if (loopOneFailureCount < FAILURE_THRESHOLD) {
                PlaybackFailureDecision.RetryCurrent
            } else {
                PlaybackFailureDecision.StayError
            }
        }
        consecutiveFailedSongCount += 1
        lastFailedSongId = error.songId
        return if (consecutiveFailedSongCount < FAILURE_THRESHOLD && hasRecoverableTarget) {
            PlaybackFailureDecision.SkipToNext
        } else {
            PlaybackFailureDecision.StayError
        }
    }

    fun reset() {
        loopOneFailureCount = 0
        consecutiveFailedSongCount = 0
        lastFailedSongId = null
    }

    private companion object {
        private const val FAILURE_THRESHOLD: Int = 3
    }
}

enum class PlaybackFailureDecision {
    RetryCurrent,
    SkipToNext,
    StayError,
}
```

- [x] **Step 4: Run the failure-policy test and verify it passes**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackFailurePolicyTest"
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackFailurePolicy.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackFailurePolicyTest.kt
git commit -m "refactor: 抽出播放失败恢复策略"
```

---

### Task 4: Extract PlaybackSnapshotWriter

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriter.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriterTest.kt`

- [ ] **Step 1: Write the failing snapshot-writer tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriterTest.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackSnapshotWriterTest {
    @Test
    fun firstProgressEventPersistsSnapshot(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val store = RecordingPlaybackSnapshotStore()
        var currentTimeMs = 1_000L
        val writer = PlaybackSnapshotWriter(
            playbackRepository = repository,
            playbackSnapshotStore = store,
            snapshotWriteScope = backgroundScope,
            nowMillis = { currentTimeMs },
        )
        repository.saveQueueState(state = QueueState(songIds = listOf("a"), currentIndex = 0))
        repository.savePlaybackState(state = PlaybackState(currentSongId = "a", positionMs = 10L))

        writer.saveForEvent(event = PlaybackEngineEvent.ProgressChanged(positionMs = 10L, durationMs = 100L))
        advanceUntilIdle()

        assertEquals(expected = 1, actual = store.savedSnapshots.size)
        assertEquals(expected = 10L, actual = store.savedSnapshots.single().playbackState.positionMs)
    }

    @Test
    fun progressEventsInsideThrottleWindowAreSkipped(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val store = RecordingPlaybackSnapshotStore()
        var currentTimeMs = 1_000L
        val writer = PlaybackSnapshotWriter(
            playbackRepository = repository,
            playbackSnapshotStore = store,
            snapshotWriteScope = backgroundScope,
            nowMillis = { currentTimeMs },
            snapshotThrottleMs = 5_000L,
        )

        writer.saveForEvent(event = PlaybackEngineEvent.ProgressChanged(positionMs = 10L, durationMs = 100L))
        advanceUntilIdle()
        currentTimeMs = 2_000L
        writer.saveForEvent(event = PlaybackEngineEvent.ProgressChanged(positionMs = 20L, durationMs = 100L))
        advanceUntilIdle()

        assertEquals(expected = 1, actual = store.savedSnapshots.size)
    }

    @Test
    fun nonProgressEventsAreNotThrottled(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val store = RecordingPlaybackSnapshotStore()
        val writer = PlaybackSnapshotWriter(
            playbackRepository = repository,
            playbackSnapshotStore = store,
            snapshotWriteScope = backgroundScope,
            nowMillis = { 1_000L },
            snapshotThrottleMs = 5_000L,
        )

        writer.saveForEvent(event = PlaybackEngineEvent.ProgressChanged(positionMs = 10L, durationMs = 100L))
        advanceUntilIdle()
        writer.saveForEvent(
            event = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Playing,
                positionMs = 10L,
                durationMs = 100L,
            ),
        )
        advanceUntilIdle()

        assertEquals(expected = 2, actual = store.savedSnapshots.size)
    }

    @Test
    fun awaitPendingWritesWaitsForAsyncSaveCompletion(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val store = RecordingPlaybackSnapshotStore(writeDelayMs = 100L)
        val writer = PlaybackSnapshotWriter(
            playbackRepository = repository,
            playbackSnapshotStore = store,
            snapshotWriteScope = backgroundScope,
            nowMillis = { 1_000L },
        )

        writer.saveAsync()
        assertEquals(expected = 0, actual = store.savedSnapshots.size)

        val awaitJob: Deferred<Unit> = async {
            writer.awaitPendingWrites()
        }
        advanceTimeBy(delayTimeMillis = 100L)
        advanceUntilIdle()
        awaitJob.await()

        assertEquals(expected = 1, actual = store.savedSnapshots.size)
    }

    @Test
    fun saveNowAndAwaitPropagatesSnapshotStoreFailure(): Unit = runTest {
        val repository = InMemoryPlaybackRepository()
        val writer = PlaybackSnapshotWriter(
            playbackRepository = repository,
            playbackSnapshotStore = FailingPlaybackSnapshotStore(),
            snapshotWriteScope = backgroundScope,
            nowMillis = { 1_000L },
        )

        val failure = assertFailsWith<IllegalStateException> {
            writer.saveNowAndAwait()
        }

        assertEquals(expected = "snapshot write failed", actual = failure.message)
    }

    private class RecordingPlaybackSnapshotStore(
        private val writeDelayMs: Long = 0L,
    ) : PlaybackSnapshotStore {
        val savedSnapshots: MutableList<PlaybackSnapshot> = mutableListOf()

        override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
            if (writeDelayMs > 0L) {
                delay(timeMillis = writeDelayMs)
            }
            savedSnapshots += snapshot
        }

        override suspend fun hasSavedSnapshot(): Boolean {
            return savedSnapshots.isNotEmpty()
        }

        override suspend fun getSavedQueueSongIds(): List<String> {
            return savedSnapshots.lastOrNull()?.queueState?.songIds ?: emptyList()
        }

        override suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot {
            return savedSnapshots.lastOrNull() ?: PlaybackSnapshot()
        }
    }

    private class FailingPlaybackSnapshotStore : PlaybackSnapshotStore {
        override suspend fun saveSnapshot(snapshot: PlaybackSnapshot) {
            throw IllegalStateException("snapshot write failed")
        }

        override suspend fun hasSavedSnapshot(): Boolean {
            return false
        }

        override suspend fun getSavedQueueSongIds(): List<String> {
            return emptyList()
        }

        override suspend fun restoreSnapshot(availableSongIds: Set<String>): PlaybackSnapshot {
            return PlaybackSnapshot()
        }
    }
}
```

- [ ] **Step 2: Run the snapshot-writer test and verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackSnapshotWriterTest"
```

Expected: FAIL with unresolved reference `PlaybackSnapshotWriter`.

- [ ] **Step 3: Add `PlaybackSnapshotWriter`**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriter.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

/**
 * Persists playback snapshots and tracks asynchronous writes for teardown safety.
 */
class PlaybackSnapshotWriter(
    private val playbackRepository: PlaybackRepository,
    private val playbackSnapshotStore: PlaybackSnapshotStore,
    private val snapshotWriteScope: CoroutineScope,
    private val nowMillis: () -> Long,
    private val snapshotThrottleMs: Long = 5_000L,
) {
    private val pendingWritesLock: Any = Any()
    private val pendingWrites: MutableSet<Deferred<Unit>> = linkedSetOf()
    private var lastProgressSnapshotAt: Long? = null

    fun saveAsync(): Deferred<Unit> {
        val job: Deferred<Unit> = snapshotWriteScope.async(start = CoroutineStart.UNDISPATCHED) {
            saveCurrentSnapshot()
        }
        synchronized(lock = pendingWritesLock) {
            pendingWrites += job
        }
        job.invokeOnCompletion {
            synchronized(lock = pendingWritesLock) {
                pendingWrites.remove(element = job)
            }
        }
        return job
    }

    fun saveForEvent(event: PlaybackEngineEvent) {
        if (event is PlaybackEngineEvent.ProgressChanged) {
            val now: Long = nowMillis()
            val previousSnapshotAt: Long? = lastProgressSnapshotAt
            if (previousSnapshotAt != null && now - previousSnapshotAt < snapshotThrottleMs) {
                return
            }
            lastProgressSnapshotAt = now
        }
        saveAsync()
    }

    suspend fun saveNowAndAwait() {
        saveCurrentSnapshot()
    }

    suspend fun awaitPendingWrites() {
        while (true) {
            val pendingJobs: List<Deferred<Unit>> = synchronized(lock = pendingWritesLock) {
                pendingWrites.toList()
            }
            if (pendingJobs.isEmpty()) {
                return
            }
            pendingJobs.awaitAll()
        }
    }

    private suspend fun saveCurrentSnapshot() {
        playbackSnapshotStore.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = playbackRepository.getPlaybackState(),
                queueState = playbackRepository.getQueueState(),
                updatedAt = nowMillis(),
            ),
        )
    }
}
```

- [ ] **Step 4: Run the snapshot-writer test and verify it passes**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackSnapshotWriterTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriter.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriterTest.kt
git commit -m "refactor: 抽出播放快照写入器"
```

---

### Task 5: Extract PlaybackHistoryRecorder

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorder.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorderTest.kt`

- [ ] **Step 1: Write the failing history-recorder tests**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorderTest.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackHistoryRecorderTest {
    @Test
    fun newSongIsPlacedAtFront(): Unit {
        val repository = InMemoryPlaybackRepository()
        repository.savePlaybackHistory(history = PlaybackHistory(songIds = listOf("b", "c")))
        val recorder = PlaybackHistoryRecorder(playbackRepository = repository)

        recorder.record(songId = "a")

        assertEquals(expected = listOf("a", "b", "c"), actual = repository.getPlaybackHistory().songIds)
    }

    @Test
    fun duplicateSongMovesToFrontAndKeepsSingleEntry(): Unit {
        val repository = InMemoryPlaybackRepository()
        repository.savePlaybackHistory(history = PlaybackHistory(songIds = listOf("a", "b", "c")))
        val recorder = PlaybackHistoryRecorder(playbackRepository = repository)

        recorder.record(songId = "b")

        assertEquals(expected = listOf("b", "a", "c"), actual = repository.getPlaybackHistory().songIds)
    }

    @Test
    fun historyKeepsAtMostFiftySongs(): Unit {
        val repository = InMemoryPlaybackRepository()
        repository.savePlaybackHistory(
            history = PlaybackHistory(songIds = (1..55).map { index: Int -> "song-$index" }),
        )
        val recorder = PlaybackHistoryRecorder(playbackRepository = repository)

        recorder.record(songId = "new")

        val history = repository.getPlaybackHistory().songIds
        assertEquals(expected = 50, actual = history.size)
        assertEquals(expected = "new", actual = history.first())
        assertEquals(expected = "song-49", actual = history.last())
    }
}
```

- [ ] **Step 2: Run the history-recorder test and verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackHistoryRecorderTest"
```

Expected: FAIL with unresolved reference `PlaybackHistoryRecorder`.

- [ ] **Step 3: Add `PlaybackHistoryRecorder`**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorder.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository

/**
 * Maintains recent playback history ordering and de-duplication.
 */
class PlaybackHistoryRecorder(
    private val playbackRepository: PlaybackRepository,
) {
    fun record(songId: String) {
        val currentSongIds: List<String> = playbackRepository.getPlaybackHistory().songIds
        playbackRepository.savePlaybackHistory(
            history = PlaybackHistory(
                songIds = (listOf(songId) + currentSongIds.filterNot { currentId: String ->
                    currentId == songId
                }).take(n = MAX_HISTORY_SIZE),
            ),
        )
    }

    private companion object {
        private const val MAX_HISTORY_SIZE: Int = 50
    }
}
```

- [ ] **Step 4: Run the history-recorder test and verify it passes**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackHistoryRecorderTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorder.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackHistoryRecorderTest.kt
git commit -m "refactor: 抽出最近播放历史记录器"
```

---

### Task 6: Wire Collaborators Into PlaybackCoordinator

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt`

- [ ] **Step 1: Run current coordinator tests as the behavior baseline**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest"
```

Expected: PASS before refactoring. If it fails, stop and fix the existing baseline before wiring collaborators.

- [ ] **Step 2: Add collaborator fields and remove duplicated runtime state**

Modify the top of `PlaybackCoordinator.kt` so imports no longer include `PlaybackHistory`, `PlaybackSnapshot`, `Deferred`, `async`, or `awaitAll`, and add collaborator fields immediately after constructor parameters:

```kotlin
class PlaybackCoordinator(
    private val playbackRepository: PlaybackRepository,
    private val audioPlayerEngine: AudioPlayerEngine,
    private val playbackSnapshotStore: PlaybackSnapshotStore = InMemoryPlaybackSnapshotStore(),
    private val snapshotWriteScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val nowMillis: () -> Long = { 0L },
    private val snapshotThrottleMs: Long = 5_000L,
    private val randomIndex: (List<Int>) -> Int = { candidates -> candidates.random() },
) {
    private val shuffleQueuePolicy: ShuffleQueuePolicy = ShuffleQueuePolicy(randomIndex = randomIndex)
    private val queueNavigator: PlaybackQueueNavigator = PlaybackQueueNavigator(shufflePolicy = shuffleQueuePolicy)
    private val failurePolicy: PlaybackFailurePolicy = PlaybackFailurePolicy()
    private val snapshotWriter: PlaybackSnapshotWriter = PlaybackSnapshotWriter(
        playbackRepository = playbackRepository,
        playbackSnapshotStore = playbackSnapshotStore,
        snapshotWriteScope = snapshotWriteScope,
        nowMillis = nowMillis,
        snapshotThrottleMs = snapshotThrottleMs,
    )
    private val historyRecorder: PlaybackHistoryRecorder = PlaybackHistoryRecorder(
        playbackRepository = playbackRepository,
    )

    private var eventJob: Job? = null
    private var onStateChanged: () -> Unit = {}
```

Delete these old fields from `PlaybackCoordinator`:

```kotlin
private val pendingSnapshotWritesLock: Any = Any()
private val pendingSnapshotWrites: MutableSet<Deferred<Unit>> = linkedSetOf()
private var loopOneFailureCount: Int = 0
private var consecutiveFailedSongCount: Int = 0
private var lastFailedSongId: String? = null
private var lastProgressSnapshotAt: Long? = null
```

- [ ] **Step 3: Replace initial shuffle and history calls**

In `playSong`, replace the `shuffleRemaining` and history lines with:

```kotlin
shuffleRemaining = shuffleQueuePolicy.buildInitialRemaining(
    playbackMode = currentPlaybackMode,
    queueSize = matchingQueueSongs.size,
    currentIndex = startIndex,
),
```

and:

```kotlin
historyRecorder.record(songId = song.id)
saveSnapshotNow()
```

In `restoreSnapshot`, replace restored shuffle remaining with:

```kotlin
shuffleRemaining = shuffleQueuePolicy.buildInitialRemaining(
    playbackMode = snapshot.queueState.playbackMode,
    queueSize = restoredQueueSongs.size,
    currentIndex = restoredIndex,
),
```

- [ ] **Step 4: Clean invalid Elvis warnings in teardown methods**

In both `persistSnapshotForServiceTeardown` and `persistSnapshotForProcessTeardown`, replace:

```kotlin
currentSongId = currentPlaybackState.currentSongId ?: queueState.currentSongId,
```

with:

```kotlin
currentSongId = currentPlaybackState.currentSongId,
```

The preceding guard already returns when `currentPlaybackState.currentSongId == null`, so this removes the useless Elvis expression without changing behavior.

- [ ] **Step 5: Delegate next, previous, mode change, and engine transition**

Replace `moveNext` body with:

```kotlin
fun moveNext() {
    val queueState: QueueState = playbackRepository.getQueueState()
    val navigationResult: QueueNavigationResult? = queueNavigator.next(queueState = queueState)
    if (navigationResult != null) {
        moveToNavigationResult(navigationResult = navigationResult)
        return
    }
    playbackRepository.savePlaybackState(
        state = playbackRepository.getPlaybackState().copy(status = PlaybackStatus.Ended),
    )
}
```

Replace `movePrevious` body with:

```kotlin
fun movePrevious() {
    val queueState: QueueState = playbackRepository.getQueueState()
    val navigationResult: QueueNavigationResult = queueNavigator.previous(queueState = queueState) ?: return
    moveToNavigationResult(navigationResult = navigationResult)
}
```

Replace `cyclePlaybackMode` queue save block with:

```kotlin
playbackRepository.saveQueueState(
    state = queueNavigator.changePlaybackMode(
        queueState = queueState,
        playbackMode = nextMode,
    ),
)
```

Replace `handleCurrentMediaChanged` queue calculation with:

```kotlin
val nextQueueState: QueueState = queueNavigator.engineTransition(
    queueState = queueState,
    targetIndex = event.index,
)?.queueState ?: queueState
```

- [ ] **Step 6: Delegate ended, failure, and exact-index movement**

Replace `handleEnded` body with:

```kotlin
private fun handleEnded() {
    val queueState: QueueState = playbackRepository.getQueueState()
    val navigationResult: QueueNavigationResult? = queueNavigator.next(
        queueState = queueState,
        keepLoopOneCurrent = true,
    )
    if (navigationResult != null) {
        moveToNavigationResult(navigationResult = navigationResult)
        return
    }
    playbackRepository.savePlaybackState(
        state = playbackRepository.getPlaybackState().copy(status = PlaybackStatus.Ended),
    )
}
```

Replace `handleFailure` body with:

```kotlin
private fun handleFailure(error: PlaybackError) {
    playbackRepository.savePlaybackState(
        state = playbackRepository.getPlaybackState().copy(
            status = PlaybackStatus.Error,
            error = error,
        ),
    )
    val queueState: QueueState = playbackRepository.getQueueState()
    when (
        failurePolicy.onFailure(
            error = error,
            playbackMode = queueState.playbackMode,
            hasRecoverableTarget = queueNavigator.hasDifferentNextTarget(queueState = queueState),
        )
    ) {
        PlaybackFailureDecision.RetryCurrent -> {
            val retryResult: QueueNavigationResult = queueNavigator.exactIndex(
                queueState = queueState,
                targetIndex = queueState.currentIndex,
            ) ?: return
            moveToNavigationResult(
                navigationResult = retryResult,
                clearError = false,
            )
        }
        PlaybackFailureDecision.SkipToNext -> {
            val nextResult: QueueNavigationResult? = queueNavigator.next(queueState = queueState)
            if (nextResult != null) {
                moveToNavigationResult(
                    navigationResult = nextResult,
                    clearError = false,
                )
            }
        }
        PlaybackFailureDecision.StayError -> Unit
    }
}
```

Replace `moveToIndex` with a small resolver plus a new result-based function:

```kotlin
private fun moveToIndex(
    targetIndex: Int,
    isMovingBackward: Boolean = false,
    positionMs: Long = 0L,
    shouldResumePlayback: Boolean = true,
    clearError: Boolean = true,
) {
    val queueState: QueueState = playbackRepository.getQueueState()
    val navigationResult: QueueNavigationResult = queueNavigator.exactIndex(
        queueState = queueState,
        targetIndex = targetIndex,
        isMovingBackward = isMovingBackward,
    ) ?: return
    moveToNavigationResult(
        navigationResult = navigationResult,
        positionMs = positionMs,
        shouldResumePlayback = shouldResumePlayback,
        clearError = clearError,
    )
}

private fun moveToNavigationResult(
    navigationResult: QueueNavigationResult,
    positionMs: Long = 0L,
    shouldResumePlayback: Boolean = true,
    clearError: Boolean = true,
) {
    val safePositionMs: Long = positionMs.coerceAtLeast(minimumValue = 0L)
    val currentPlaybackState: PlaybackState = playbackRepository.getPlaybackState()
    val songId: String = navigationResult.queueState.songIds[navigationResult.targetIndex]
    playbackRepository.saveQueueState(state = navigationResult.queueState)
    playbackRepository.savePlaybackState(
        state = currentPlaybackState.copy(
            currentSongId = songId,
            status = if (shouldResumePlayback) PlaybackStatus.Loading else PlaybackStatus.Paused,
            positionMs = safePositionMs,
            error = if (clearError) null else currentPlaybackState.error,
        ),
    )
    historyRecorder.record(songId = songId)
    saveSnapshotNow()
    onStateChanged()
    audioPlayerEngine.skipToIndex(index = navigationResult.targetIndex)
    if (safePositionMs > 0L) {
        audioPlayerEngine.seekTo(positionMs = safePositionMs)
    }
    if (shouldResumePlayback) {
        audioPlayerEngine.play()
        return
    }
    audioPlayerEngine.pause()
}
```

- [ ] **Step 7: Delegate remove-from-queue transition**

In `removeFromQueue`, after `nextQueueSongs` has been resolved and the empty branch handled, replace the current-index calculation and queue-state save with:

```kotlin
val currentSongWasRemoved: Boolean = playbackState.currentSongId == songId
val navigationResult: QueueNavigationResult = queueNavigator.removeSong(
    queueState = queueState,
    removedSongId = songId,
    currentSongId = playbackState.currentSongId,
    nextSongIds = nextQueueSongs.map { song: Song -> song.id },
) ?: return
val nextCurrentSong: Song = nextQueueSongs[navigationResult.targetIndex]
val shouldResumePlayback: Boolean = playbackState.status == PlaybackStatus.Playing ||
    playbackState.status == PlaybackStatus.Loading
playbackRepository.saveQueueState(state = navigationResult.queueState)
playbackRepository.savePlaybackState(
    state = playbackState.copy(
        currentSongId = nextCurrentSong.id,
        status = if (shouldResumePlayback) PlaybackStatus.Loading else PlaybackStatus.Paused,
        positionMs = if (currentSongWasRemoved) 0L else playbackState.positionMs,
        durationMs = nextCurrentSong.durationMs,
        error = null,
    ),
)
```

Keep the existing snapshot, callback, engine mode, `setQueue`, and play/pause logic. Change `startIndex` to use the navigation result:

```kotlin
startIndex = navigationResult.targetIndex,
```

- [ ] **Step 8: Delegate snapshot writer and failure reset**

Replace `saveSnapshotForEvent`, `saveSnapshotNow`, `saveSnapshotNowAndAwait`, and `awaitPendingSnapshotWrites` with:

```kotlin
private fun saveSnapshotForEvent(event: PlaybackEngineEvent) {
    snapshotWriter.saveForEvent(event = event)
}

private fun saveSnapshotNow() {
    snapshotWriter.saveAsync()
}

private suspend fun saveSnapshotNowAndAwait() {
    snapshotWriter.saveNowAndAwait()
}

suspend fun awaitPendingSnapshotWrites() {
    snapshotWriter.awaitPendingWrites()
}
```

Replace `resetFailureCounters()` call in `updateStatus` with:

```kotlin
failurePolicy.reset()
```

Delete the old private functions after delegation:

```kotlin
buildQueueStateForEngineTransition
nextIndex
launchSnapshotWrite
saveCurrentSnapshot
resetFailureCounters
recordHistory
buildInitialShuffleRemaining
buildShuffleQueueState
```

- [ ] **Step 9: Add coordinator regression tests for failure and queue removal**

Add these tests to `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt`:

```kotlin
/**
 * 非单曲循环的单首坏文件没有可跳过目标时，应停留错误态而不是重试同一首。
 */
@Test
fun nonLoopSingleSongFailureStaysErrorWithoutRetryingSameSong(): Unit = runTest {
    val repository = InMemoryPlaybackRepository()
    val coordinator = PlaybackCoordinator(
        playbackRepository = repository,
        audioPlayerEngine = FakeAudioPlayerEngine(),
        snapshotWriteScope = backgroundScope,
    )
    val songs = buildSongs(count = 1)
    val expectedError = PlaybackError(
        type = PlaybackErrorType.Unknown,
        songId = songs[0].id,
        message = "坏文件",
    )

    coordinator.playSong(song = songs[0], queueSongs = songs)
    coordinator.handleEngineEventForTest(PlaybackEngineEvent.Failed(error = expectedError))

    assertEquals(expected = 0, actual = repository.getQueueState().currentIndex)
    assertEquals(expected = songs[0].id, actual = repository.getPlaybackState().currentSongId)
    assertEquals(expected = PlaybackStatus.Error, actual = repository.getPlaybackState().status)
    assertEquals(expected = expectedError, actual = repository.getPlaybackState().error)
}

/**
 * 移除当前歌曲后，repository 队列和引擎队列应同步到新的当前歌曲。
 */
@Test
fun removeCurrentSongKeepsRepositoryAndEngineQueueInSync(): Unit = runTest {
    val repository = InMemoryPlaybackRepository()
    val engine = FakeAudioPlayerEngine()
    val coordinator = PlaybackCoordinator(
        playbackRepository = repository,
        audioPlayerEngine = engine,
        snapshotWriteScope = backgroundScope,
    )
    val songs = buildSongs(count = 3)

    coordinator.start(scope = backgroundScope)
    coordinator.playSong(song = songs[0], queueSongs = songs)
    advanceUntilIdle()
    coordinator.pause()
    advanceUntilIdle()

    coordinator.removeFromQueue(songId = songs[0].id, availableSongs = songs)
    advanceUntilIdle()

    assertEquals(expected = listOf(songs[1].id, songs[2].id), actual = repository.getQueueState().songIds)
    assertEquals(expected = 0, actual = repository.getQueueState().currentIndex)
    assertEquals(expected = songs[1].id, actual = repository.getPlaybackState().currentSongId)
    assertEquals(expected = PlaybackStatus.Paused, actual = repository.getPlaybackState().status)

    coordinator.play()
    advanceUntilIdle()

    assertEquals(expected = songs[1].id, actual = repository.getPlaybackState().currentSongId)
    assertEquals(expected = PlaybackStatus.Playing, actual = repository.getPlaybackState().status)
}
```

- [ ] **Step 10: Run focused and coordinator tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.ShuffleQueuePolicyTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackQueueNavigatorTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackFailurePolicyTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackSnapshotWriterTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackHistoryRecorderTest" --tests "com.yanhao.kmpmusic.domain.playback.PlaybackCoordinatorTest"
```

Expected: PASS.

- [ ] **Step 11: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt
git commit -m "refactor: 播放协调器接入内部协作者"
```

---

### Task 7: Trim Coordinator Tests And Run Phase Verification

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt`

- [ ] **Step 1: Remove pure strategy duplication from coordinator tests**

Keep facade-level tests that prove `PlaybackCoordinator` still integrates repository, history, snapshot, and engine commands. Preserve these tests in `PlaybackCoordinatorTest.kt`:

```text
playSongUsesWholeCurrentListAsQueue
playSongFallsBackToSingleSongWhenQueueDoesNotContainTarget
cyclePlaybackModeLoopsThroughThreeModes
cyclePlaybackModeSyncsPlatformEngineMode
sequenceModeEndedFromLastSongLoopsToFirstSong
loopOneKeepsCurrentSongOnEnded
externalShuffleTransitionUpdatesHistoryAndRemaining
shuffleNextAfterPreviousDoesNotReplayCurrentSong
startCollectsEngineEventsIntoRepository
playSongPersistsSnapshotBeforeStart
restoreSnapshotPrimesEngineForResume
toggleWhileLoadingRequestsPlayInsteadOfPause
firstProgressEventPersistsSnapshot
explicitPlayAndPauseCommandsUpdateRepositoryThroughEngineEvents
skipToQueueIndexUsesExactIndexAndRequestedPosition
autoSkipKeepsRecentErrorVisibleUntilPlaybackRecovers
successfulPlaybackResetsFailureCounters
nonLoopSingleSongFailureStaysErrorWithoutRetryingSameSong
removeCurrentSongKeepsRepositoryAndEngineQueueInSync
serviceTeardownPersistsFinalPausedSnapshot
processTeardownPropagatesSnapshotSaveFailure
```

Delete coordinator tests whose only assertion is now covered by focused pure tests:

```text
sequenceModeNextFromLastSongLoopsToFirstSong
shufflePreviousUsesHistory
loopOneStopsAfterThreeFailuresForSameSong
```

Do not delete facade tests that protect cross-collaborator behavior even when a focused test has a similar name.

- [ ] **Step 2: Run playback-domain tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.*"
```

Expected: PASS.

- [ ] **Step 3: Run full phase verification**

Run:

```bash
./gradlew :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid
```

Expected: PASS for both tasks.

- [ ] **Step 4: Inspect working tree**

Run:

```bash
git status --short --branch
```

Expected: only the intended playback production/test files are modified, plus this plan document if it has not already been committed.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt
git commit -m "test: 收窄播放协调器验收测试"
```

## Self-Review

Spec coverage:

- Queue navigation is covered by Task 2 and wired in Task 6.
- Shuffle history, remaining set, initial remaining, current-index no-op, previous/next, next-round behavior, and injected random are covered by Task 1 and Task 2.
- Failure thresholds, reset-on-playing only, error retention, non-loop single-song failure behavior, and auto-skip facade behavior are covered by Task 3 and Task 6.
- Snapshot pending writes, progress throttling, non-progress writes, virtual-time-safe teardown await, and failure propagation are covered by Task 4 and Task 6.
- Recent playback de-dupe and limit are covered by Task 5 and Task 6.
- `removeFromQueue` repository and engine queue synchronization is covered by Task 6.
- Existing public API compatibility and constructor compatibility are preserved in Task 6.
- Warning cleanup for invalid Elvis expressions is covered by Task 6.
- Final desktop and Android verification is covered by Task 7.

Placeholder scan:

- No unresolved placeholders or “write tests for the above” steps remain.
- Every new production file has concrete Kotlin code.
- Every new test file has concrete Kotlin code and exact Gradle commands.

Type consistency:

- `ShuffleQueuePolicy`, `PlaybackQueueNavigator`, `QueueNavigationResult`, `PlaybackFailurePolicy`, `PlaybackFailureDecision`, `PlaybackSnapshotWriter`, and `PlaybackHistoryRecorder` are defined before coordinator wiring references them.
- All snippets use existing project types: `QueueState`, `PlaybackMode`, `PlaybackState`, `PlaybackStatus`, `PlaybackError`, `PlaybackEngineEvent`, `PlaybackRepository`, and `PlaybackSnapshotStore`.
- Coordinator wiring keeps `Song.toPlayableMedia()` private to `PlaybackCoordinator`, matching the design decision not to extract a mapper in phase 2.
