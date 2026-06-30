# Playback Abstraction Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the existing playback abstraction explicit in code and documentation without rewriting the Android Media3 or Desktop vlcj playback chains.

**Architecture:** Keep `AudioPlayerEngine` as the common playback interface and `PlaybackCoordinator` as the playback business coordinator. Introduce a local-only `AudioSource` interface in `commonMain`, make platform adapters consume it, and keep network playback source variants out of phase 1 code until buffering, network errors, auth, and cache semantics are designed.

**Tech Stack:** Kotlin Multiplatform 2.0.21, Compose Multiplatform 1.7.3, Kotlin coroutines Flow, Android Media3, Desktop vlcj, kotlin.test, Gradle.

---

## Scope Check

The spec covers one implementation unit for this plan: playback abstraction audit cleanup. It mentions iOS, Windows, and network playback, but those are future integration routes, not independently testable software in this phase.

This plan deliberately excludes:

- iOS AVPlayer implementation.
- Windows vlcj runtime resolver implementation.
- Network playback implementation.
- `AudioPlayerEngine` replacement with `prepare(AudioSource)`.
- Koin or any third-party dependency injection framework.

The plan includes a small spec correction first because the current spec says to add `AudioSource.Remote` in phase 1 while also saying network playback needs a separate design. The code should only expose the contract it can honor now: local playable URIs.

## File Structure

- Modify: `docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md`
  - Tighten `AudioSource` wording so phase 1 code only adds `AudioSource.Local`.
  - Keep network source requirements as documented future constraints.

- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt`
  - Add `AudioSource`.
  - Add `PlayableMedia.audioSource` derived property.
  - Clarify KDoc that `localUri` is a scanner-provided local playable URI, not necessarily a filesystem path.

- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackModelsTest.kt`
  - Add tests that `PlayableMedia.audioSource` preserves `content://` and `file://` local URIs without scheme parsing.

- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlayableMediaMapper.kt`
  - Use `PlayableMedia.audioSource.uri` when building Media3 `MediaItem`.

- Modify: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt`
  - Use `PlayableMedia.audioSource.uri` when preparing the desktop adapter.

- Modify: `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngineTest.kt`
  - Add a test proving the desktop adapter receives the URI from the source abstraction.

- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt`
  - Expand KDoc for the common playback interface and event flow.

- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/LocalMusicScanner.kt`
  - Expand KDoc to mark scanning as upstream discovery, not playback execution.

## Task 1: Correct the Spec Scope Before Code

**Files:**
- Modify: `docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md`

- [x] **Step 1: Replace the phase 1 `AudioSource` code sample**

Replace the sample under `本轮建议轻量新增 AudioSource 概念` with:

````markdown
本轮建议轻量新增 `AudioSource` 概念，但 phase 1 代码只承诺本地可播放来源：

```kotlin
sealed interface AudioSource {
    val uri: String

    data class Local(
        override val uri: String,
    ) : AudioSource
}
```

`Remote` 不在本轮代码中落地。网络播放需要 buffering、网络错误、鉴权、缓存和 URL 过期策略；在这些运行时契约明确前，把 `Remote` 放进生产模型会让接口看起来已经支持网络播放，但实际事件和错误模型无法履约。
````

- [x] **Step 2: Replace the migration trigger paragraph**

Replace the paragraph that starts with `后续当 iOS、Windows 或网络音频接入时` and the following Kotlin sample with:

````markdown
后续只有当出现非本地来源时，例如网络音频、缓存后的 source 切换、签名 URL 刷新，才评估把主字段从 `localUri` 改成 `audioSource`：

```kotlin
data class PlayableMedia(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long?,
    val audioSource: AudioSource,
    val coverArt: CoverArt,
    val mimeType: String?,
)
```

iOS 本地文件夹导入和 Windows JVM Desktop 本地文件播放仍属于 `AudioSource.Local`，不应该单独触发主字段迁移。
````

- [x] **Step 3: Replace the network evolution opening sentence**

Replace:

```markdown
未来网络音频不是“另一个本地 path”，而是一个新的运行时契约。`AudioSource.Remote` 只表达来源类型，本轮不实现网络播放。
```

with:

```markdown
未来网络音频不是“另一个本地 path”，而是一个新的运行时契约。网络播放设计可以引入远程播放来源类型，例如带 headers 的远程 URI，但该类型不属于本轮生产代码。
```

- [x] **Step 4: Replace the broad scope and acceptance wording**

Replace this bullet under `本轮包含`:

```markdown
- 轻量新增 `AudioSource` 概念，区分本地播放来源和未来网络播放来源。
```

with:

```markdown
- 轻量新增 `AudioSource.Local` 概念，明确 phase 1 只承诺本地可播放来源，并记录未来网络播放来源需要另开设计。
```

Replace this acceptance bullet:

```markdown
- `AudioSource` 能表达本地与未来网络来源差异，不把二者压成裸字符串 URL。
```

with:

```markdown
- `AudioSource.Local` 能表达 scanner 已确认可访问的本地播放来源，不把 `content://`、`file://` 或平台文件 URI 误收窄成普通 filesystem path。
```

Replace this risk row:

```markdown
| 过早引入 `AudioSource.Remote` 让人误以为已支持网络播放 | 明确 `Remote` 只是未来 source 类型，网络播放需要单独设计 buffering、错误、鉴权和缓存。 |
```

with:

```markdown
| 过早引入远程播放来源让人误以为已支持网络播放 | 本轮生产模型只加入 `AudioSource.Local`，网络播放需要单独设计 buffering、错误、鉴权和缓存。 |
```

- [x] **Step 5: Run a focused spec scan**

Run:

```bash
rg -n "AudioSource\\.Remote|data class Remote|iOS、Windows 或网络音频" docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md
```

Expected: no output.

- [x] **Step 6: Commit the spec correction**

```bash
git add docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md
git commit -m "docs: 收紧播放来源设计范围"
```

## Task 2: Add Local AudioSource in Common Model

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackModelsTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt`

- [x] **Step 1: Write the failing tests**

Add the import:

```kotlin
import com.yanhao.kmpmusic.domain.model.AudioSource
```

Add these tests inside `PlaybackModelsTest`:

```kotlin
    @Test
    fun playableMediaDerivesLocalAudioSourceFromAndroidContentUri(): Unit {
        val media: PlayableMedia = PlayableMedia(
            songId = "androidMediaStore:42",
            title = "设备里的歌",
            artist = "未知歌手",
            album = "未知专辑",
            durationMs = 180_000L,
            localUri = "content://media/external/audio/media/42",
            coverArt = CoverArt.HeroLocalMusic,
            mimeType = "audio/mpeg",
        )

        assertEquals(
            expected = AudioSource.Local(uri = "content://media/external/audio/media/42"),
            actual = media.audioSource,
        )
    }

    @Test
    fun playableMediaDerivesLocalAudioSourceFromDesktopFileUri(): Unit {
        val media: PlayableMedia = PlayableMedia(
            songId = "desktop:/Users/tester/Music/song.flac",
            title = "Desktop Song",
            artist = "Artist",
            album = "Album",
            durationMs = 240_000L,
            localUri = "file:///Users/tester/Music/song.flac",
            coverArt = CoverArt.HeroLocalMusic,
            mimeType = "audio/flac",
        )

        assertEquals(
            expected = AudioSource.Local(uri = "file:///Users/tester/Music/song.flac"),
            actual = media.audioSource,
        )
    }
```

- [x] **Step 2: Run the tests to verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackModelsTest"
```

Expected: FAIL with unresolved reference errors for `AudioSource` and `audioSource`.

- [x] **Step 3: Add `AudioSource` and `PlayableMedia.audioSource`**

In `PlaybackModels.kt`, insert this model above `PlayableMedia`:

```kotlin
/**
 * 平台无关的播放来源。
 *
 * phase 1 只承诺本地可播放 URI，例如 Android MediaStore `content://`、
 * iOS sandbox `file://` 和 Desktop 文件 URI。网络来源需要单独扩展错误、
 * 缓冲、鉴权和缓存语义后再进入生产模型。
 */
sealed interface AudioSource {
    /** 可交给平台播放器消费的 URI 字符串。 */
    val uri: String

    /**
     * 平台 scanner 已确认可访问的本地播放来源。
     *
     * @property uri scanner 提供的可播放 URI，不要求是文件系统 path。
     */
    data class Local(
        override val uri: String,
    ) : AudioSource
}
```

Replace the `PlayableMedia` KDoc and data class with:

```kotlin
/**
 * 可交给播放引擎的媒体项。
 *
 * @property songId 领域层歌曲标识。
 * @property title 媒体标题。
 * @property artist 媒体歌手名。
 * @property album 媒体专辑名。
 * @property durationMs 媒体总时长，未知时为 null。
 * @property localUri 平台 scanner 提供的本地可播放 URI，可能是 `content://`、`file://` 或平台文件 URI。
 * @property coverArt 当前媒体封面。
 * @property mimeType 平台识别的媒体类型，未知时为 null。
 */
data class PlayableMedia(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long?,
    val localUri: String,
    val coverArt: CoverArt,
    val mimeType: String?,
) {
    /**
     * 当前媒体的播放来源。phase 1 保持从 [localUri] 派生，避免破坏现有队列和快照结构。
     */
    val audioSource: AudioSource
        get() = AudioSource.Local(uri = localUri)
}
```

- [x] **Step 4: Run the focused model tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackModelsTest"
```

Expected: PASS.

- [x] **Step 5: Commit the common model change**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackModelsTest.kt
git commit -m "feat: 明确本地播放来源模型"
```

## Task 3: Make Platform Engines Consume AudioSource

**Files:**
- Modify: `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngineTest.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlayableMediaMapper.kt`
- Modify: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt`

- [x] **Step 1: Add the desktop adapter regression test**

Add this test inside `DesktopVlcjAudioPlayerEngineTest`:

```kotlin
    /** 验证桌面引擎通过播放来源抽象读取 URI，而不是在平台层重新解释 scanner 字符串。 */
    @Test
    fun setQueuePreparesAdapterWithAudioSourceUri(): Unit = runTest {
        val adapter = FakeDesktopMediaPlayerAdapter()
        val engine = testEngine(adapter = adapter)

        engine.setQueue(
            items = listOf(
                media(
                    songId = "song-content-uri",
                    uri = "content://media/external/audio/media/42",
                    durationMs = 180_000L,
                ),
            ),
            startIndex = 0,
            startPositionMs = 4_000L,
        )
        advanceUntilIdle()

        assertEquals(
            expected = "prepare:song-content-uri:content://media/external/audio/media/42:1:4000",
            actual = adapter.commands.single(),
        )
        engine.release()
        advanceUntilIdle()
    }
```

- [x] **Step 2: Run the desktop test before production refactor**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngineTest.setQueuePreparesAdapterWithAudioSourceUri"
```

Expected: PASS before the production refactor because `audioSource` is currently derived from `localUri`; keep the test as a regression guard that platform code preserves scanner-provided URI values.

- [x] **Step 3: Update Android mapper to consume `audioSource`**

In `AndroidPlayableMediaMapper.kt`, add:

```kotlin
import com.yanhao.kmpmusic.domain.model.AudioSource
```

Replace `toMediaItem` with:

```kotlin
    // 把单个 common 媒体项映射成 Media3 播放器可消费的媒体项。
    private fun PlayableMedia.toMediaItem(context: Context): MediaItem {
        return MediaItem.Builder()
            .setUri(Uri.parse(playbackUri()))
            .setMediaId(songId)
            .setMediaMetadata(toMediaMetadata(context = context))
            .build()
    }

    // phase 1 只支持本地播放来源；网络来源进入模型时必须在这里显式扩展。
    private fun PlayableMedia.playbackUri(): String {
        return when (val source: AudioSource = audioSource) {
            is AudioSource.Local -> source.uri
        }
    }
```

- [x] **Step 4: Update Desktop engine to consume `audioSource`**

In `DesktopVlcjAudioPlayerEngine.kt`, add:

```kotlin
import com.yanhao.kmpmusic.domain.model.AudioSource
```

Replace the `mediaUri = media.localUri` line inside `prepareCurrentMedia` with:

```kotlin
            mediaUri = media.playbackUri(),
```

Add this helper near `prepareCurrentMedia`:

```kotlin
    // phase 1 只支持本地播放来源；网络来源进入模型时必须在桌面适配层显式处理。
    private fun PlayableMedia.playbackUri(): String {
        return when (val source: AudioSource = audioSource) {
            is AudioSource.Local -> source.uri
        }
    }
```

- [x] **Step 5: Run desktop and Android compilation checks**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngineTest.setQueuePreparesAdapterWithAudioSourceUri"
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: both PASS. The Android compile is the verification that the Media3 mapper handles the sealed `AudioSource` exhaustively.

- [x] **Step 6: Commit the platform consumption change**

```bash
git add composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlayableMediaMapper.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngineTest.kt
git commit -m "refactor: 让平台播放实现消费播放来源"
```

## Task 4: Clarify Common Playback and Scanner KDoc

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/LocalMusicScanner.kt`

- [x] **Step 1: Update `AudioPlayerEngine` KDoc**

Replace the interface KDoc with:

```kotlin
/**
 * common 层和平台播放器之间的播放接口。
 *
 * 调用方只能通过队列、播放模式、音量和播放命令表达产品语义；真实解码、
 * 媒体会话、原生库加载和平台权限都属于平台 adapter 的实现细节。
 * 播放事实必须通过 [events] 回流 common 层，避免 UI、平台层和 repository
 * 同时成为播放状态真相源。
 */
interface AudioPlayerEngine {
```

Replace the `events` KDoc with:

```kotlin
    /**
     * 平台播放器主动上报的真实播放事件。
     *
     * [PlaybackCoordinator] 订阅该事件流后统一更新队列、进度、错误和快照。
     */
    val events: Flow<PlaybackEngineEvent>
```

Replace the `setQueue` KDoc with:

```kotlin
    /**
     * 用新的媒体队列替换当前引擎状态。
     *
     * [items] 已经由 common 层整理好 metadata 和 [com.yanhao.kmpmusic.domain.model.AudioSource]；
     * 平台实现只负责把媒体项映射为 Media3、vlcj、AVPlayer 或其他平台播放器可消费的对象。
     *
     * @param items 新的可播放媒体列表。
     * @param startIndex 首次激活的队列下标。
     * @param startPositionMs 首次开始的进度。
     */
```

- [x] **Step 2: Update `LocalMusicScanner` KDoc**

Replace the file contents with:

```kotlin
package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult

/**
 * 平台无关本地音乐扫描接口，真实平台实现只能放在对应 source set。
 *
 * Scanner 只回答“当前平台发现了哪些本地歌曲以及它们的可播放 URI”。
 * 它不负责执行播放命令、不维护播放状态，也不处理队列推进；这些职责属于
 * common 播放协调器和平台播放 adapter。
 */
interface LocalMusicScanner {
    /**
     * 执行一次本地音乐扫描，返回可合并到曲库快照的扫描结果。
     */
    suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult
}
```

- [x] **Step 3: Run source-set compilation**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
```

Expected: PASS.

- [x] **Step 4: Commit the documentation boundary change**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/LocalMusicScanner.kt
git commit -m "docs: 明确播放抽象与扫描边界"
```

## Task 5: Adversarial Cross-Audit Questions

**Files:**
- Verify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt`
- Verify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt`
- Verify: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/AndroidPlayableMediaMapper.kt`
- Verify: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt`
- Verify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/LocalMusicScanner.kt`
- Verify: `docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md`

- [x] **Step 1: Ask whether common playback has a real interface and adapters**

Run:

```bash
rg -n "interface AudioPlayerEngine|: AudioPlayerEngine" composeApp/src/commonMain composeApp/src/androidMain composeApp/src/desktopMain
```

Expected output includes:

```text
composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt:...
composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback/PlaybackServiceConnector.kt:...
composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt:...
```

If Android or Desktop implementation is missing from the output, stop and fix the adapter registration before continuing.

- [x] **Step 2: Ask whether production code accidentally added remote playback**

Run:

```bash
rg -n "AudioSource\\.Remote|data class Remote|Network|Http|headers" composeApp/src/commonMain composeApp/src/androidMain composeApp/src/desktopMain
```

Expected: no output.

This proves phase 1 did not smuggle network playback into the production model.

- [x] **Step 3: Ask whether common code leaks platform playback types**

Run:

```bash
rg -n "Media3|ExoPlayer|MediaController|vlcj|LibVLC|AVPlayer|AVFoundation|WinRT|Windows\\.Media" composeApp/src/commonMain
```

Expected: no output.

If this returns matches in common playback or model code, the abstraction seam has been violated.

- [x] **Step 4: Ask whether platform adapters consume the source abstraction**

Run:

```bash
rg -n "playbackUri\\(\\)|audioSource|media\\.localUri|Uri\\.parse\\(localUri\\)" composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback
```

Expected:

- `AndroidPlayableMediaMapper.kt` contains `playbackUri()` and `audioSource`.
- `DesktopVlcjAudioPlayerEngine.kt` contains `playbackUri()` and `audioSource`.
- No production playback adapter line contains `Uri.parse(localUri)` or `media.localUri`.

- [x] **Step 5: Ask whether scanner remains upstream of playback**

Run:

```bash
rg -n "AudioPlayerEngine|PlaybackEngineEvent|PlaybackCoordinator|play\\(|pause\\(|seekTo\\(" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/LocalMusicScanner.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeLocalMusicScanner.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/data
```

Expected: no output.

Scanner implementations may produce `localUri`, metadata, and scan errors, but they must not call playback commands or depend on playback events.

- [x] **Step 6: Ask whether the spec and plan agree after correction**

Run:

```bash
rg -n "AudioSource\\.Remote|data class Remote|iOS、Windows 或网络音频|区分本地播放来源和未来网络播放来源" docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md
```

Expected: no output.

Then run:

```bash
rg -n "phase 1 代码只承诺本地可播放来源" docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md
rg -n "本轮生产模型只加入 `AudioSource.Local`" docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md
rg -n "网络播放需要单独设计" docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md
```

Expected: each command prints at least one match, proving the spec now matches this plan's local-only phase 1 scope.

- [x] **Step 7: Commit audit-only plan/spec refinements if any were needed**

If Task 5 required additional doc-only corrections, commit them:

```bash
git add docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md docs/superpowers/plans/2026-06-30-playback-abstraction-audit-implementation.md
git commit -m "docs: 补充播放抽象交叉审查"
```

If no files changed during Task 5, skip this commit.

## Task 6: Final Verification and Worktree Hygiene

**Files:**
- Verify: all files modified by Tasks 1-5.

- [x] **Step 1: Run whitespace validation**

Run:

```bash
git diff --check
```

Expected: no output.

- [x] **Step 2: Run focused playback tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.playback.PlaybackModelsTest"
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngineTest"
```

Expected: both PASS.

- [x] **Step 3: Run required project verification**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
```

Expected: PASS.

- [x] **Step 4: Inspect status before final handoff**

Run:

```bash
git status --short --branch
```

Expected: only the known pre-existing untracked file may remain:

```text
?? docs/superpowers/specs/2026-06-29-desktop-contextual-search-design.md
```

If the task commits were created, the branch should also be ahead of `origin/main`.

## Self-Review

**Spec coverage:** Task 1 corrects the phase 1 source contract, Task 2 adds the local source model, Task 3 makes Android and Desktop consume it, Task 4 documents the `AudioPlayerEngine` and `LocalMusicScanner` responsibilities, Task 5 asks adversarial implementation questions, and Task 6 verifies Android compile plus Desktop/common tests.

**Placeholder scan:** The plan contains no placeholder markers, vague validation steps, unnamed files, or unspecified tests.

**Type consistency:** `AudioSource.Local(uri)` is defined in Task 2 and consumed in Task 3. `PlayableMedia.audioSource` is derived from `localUri`, so existing constructors and queue snapshots do not change.
