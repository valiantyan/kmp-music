# Local Audio Home Display Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first production KMP path that turns scanned local-audio metadata into a shared library snapshot, then renders a homepage `本地歌曲` preview between `最近播放` and `本地专辑`.

**Architecture:** Keep platform audio discovery behind a common `LocalMusicScanner` interface, merge scanner results into a single `LibrarySnapshot`, and make `MusicAppController` the only bridge from domain state to Compose UI. This plan covers the common data contract, fake scanner, in-memory merge flow, homepage display, secondary local music page, and shared tests; Android MediaStore, iOS Files import, Desktop folder scanning, and real playback URI handling each need separate platform implementation plans after this milestone.

**Tech Stack:** Kotlin Multiplatform 2.0.21, Compose Multiplatform 1.7.3, kotlinx.coroutines core, kotlin.test, existing `:composeApp` shared architecture.

---

## Scope Check

The spec spans four subsystems: shared library data flow, homepage UI, three platform scanners, and real playback integration. This plan intentionally implements the shared data flow plus UI consumption first, using an explicit fake scanner that emits real-shaped metadata. That gives a working and testable slice without putting Android/iOS/Desktop APIs into `commonMain` or hiding mock seed data as if it were a real platform scan.

Create separate follow-up plans after this one lands:

- `2026-06-22-android-mediastore-scanner.md`: Android permissions + `MediaStore.Audio` scanner.
- `2026-06-22-ios-document-audio-importer.md`: iOS Files / Document Picker import into sandbox.
- `2026-06-22-desktop-folder-audio-scanner.md`: Desktop folder chooser + recursive scanner.
- `2026-06-22-local-uri-playback-integration.md`: playback service consuming scanner-generated `localUri`.

## File Structure

- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/Song.kt`: extend the display song model with stable source fields while keeping existing UI code compiling.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModels.kt`: platform-neutral scan request/state/result/source/problem/snapshot models.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/LocalMusicScanner.kt`: common scanner contract; platform implementations will live in platform source sets later.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/MusicLibraryRepository.kt`: expose `LibrarySnapshot` and a scan-result merge entry point while preserving existing `getSongs/getAlbums/getArtists` call sites.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCase.kt`: merge scanner results by stable `sourceKey`, filter failed entries, apply metadata fallbacks, and aggregate albums/artists/stats.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/ScanLocalMusicUseCase.kt`: replace the old `ScanStatus` toggle with a scanner + repository use case.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeLocalMusicScanner.kt`: explicit fake scanner for common tests and UI development.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryMusicLibraryRepository.kt`: in-memory snapshot repository used by the app until platform repositories arrive.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt`: allow no current song before the user actually plays a scanned item and add playback history.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/PlaybackRepository.kt`: expose playback history.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryPlaybackRepository.kt`: store current playback, queue, and true playback history separately.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/PlaybackUseCases.kt`: record playback history only when a user plays a song.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`: add local music section navigation, snapshot-derived UI state, nullable current song, and scan state.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`: wire scanner/use cases/repository, expose `scanLocalMusic`, derive `recentSongs` from playback history, and navigate to `SecondaryScreen.LocalMusic`.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`: launch scan from UI events via `rememberCoroutineScope`, route the new secondary page, and keep bottom tab visible when no mini-player exists.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/DetailScreens.kt`: add a safe missing-item secondary screen for empty or changed libraries.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt`: render real stats, true recent playback, `本地歌曲` max-6 preview, and real album aggregation.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt`: secondary page with `歌曲 / 专辑 / 歌手 / 来源` sections.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/MeScreen.kt`: use real library and favorite counts.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/UtilityScreens.kt`: update settings library rows to use scan/source state; remove or stop routing to `LocalFolderScreen`.
- Modify `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`: add controller-level behavior tests.
- Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModelsTest.kt`: source key and model tests.
- Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCaseTest.kt`: merge/filter/fallback/aggregation tests.

## Task 1: Local Music Domain Boundary

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModels.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/LocalMusicScanner.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModelsTest.kt`

- [x] **Step 1: Write the failing model test**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModelsTest.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 本地音频扫描模型测试，保护平台来源标识和稳定 key 规则。
 */
class LocalMusicModelsTest {
    /**
     * sourceKey 必须由来源类型和来源 id 组成，重新扫描后才能保留收藏和队列引用。
     */
    @Test
    fun metadataBuildsStableSourceKey(): Unit {
        val metadata = MusicFileMetadata(
            sourceId = "42",
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            localUri = "content://media/external/audio/media/42",
            fileName = "river.flac",
            title = "海边的梦",
            artist = "旅行团乐队",
            album = "似水流年",
            durationMs = 225_000L,
            mimeType = "audio/flac",
            sizeBytes = 24_000_000L,
            modifiedAt = 1_719_360_000_000L,
            coverArt = CoverArt.CoverSeaDream,
        )

        assertEquals(
            expected = "androidMediaStore:42",
            actual = metadata.sourceKey,
        )
    }
}
```

- [x] **Step 2: Run the test to verify it fails**

Run: `./gradlew :composeApp:desktopTest`

Expected: FAIL with unresolved references for `MusicFileMetadata` and `LocalMusicSourceKind`.

- [x] **Step 3: Add the scan and snapshot models**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModels.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.model

/**
 * 本地音乐来源类型，value 与产品文档中的 sourceKind 保持一致。
 */
enum class LocalMusicSourceKind(
    val value: String,
    val displayName: String,
) {
    AndroidMediaStore(value = "androidMediaStore", displayName = "Android 媒体库"),
    IosImportedFile(value = "iosImportedFile", displayName = "iOS 导入文件"),
    IosMediaLibrary(value = "iosMediaLibrary", displayName = "iOS 音乐资料库"),
    DesktopFolder(value = "desktopFolder", displayName = "桌面文件夹"),
    FakeScanner(value = "fakeScanner", displayName = "演示扫描"),
}

/**
 * 本地音乐扫描请求，表达平台无关的扫描意图。
 */
sealed interface LocalMusicScanRequest {
    data object InitialScan : LocalMusicScanRequest
    data object Refresh : LocalMusicScanRequest
    data class Source(val sourceKind: LocalMusicSourceKind) : LocalMusicScanRequest
}

/**
 * 本地音乐扫描进度，供首页卡片和弹层展示状态摘要。
 */
data class LocalMusicScanProgress(
    val processedCount: Int = 0,
    val discoveredCount: Int = 0,
    val currentSourceName: String = "",
)

/**
 * 平台无关扫描错误类型，避免 UI 依赖平台异常。
 */
enum class LocalMusicScanErrorType {
    PermissionDenied,
    UserCancelled,
    FolderUnavailable,
    FileMissing,
    FileUnreadable,
    UnsupportedFormat,
    MetadataUnavailable,
    SecurityScopeExpired,
    Unknown,
}

/**
 * 扫描错误详情，用于错误页和来源分段说明失败原因。
 */
data class LocalMusicScanError(
    val type: LocalMusicScanErrorType,
    val message: String,
    val sourceKind: LocalMusicSourceKind? = null,
    val sourceId: String? = null,
)

/**
 * 平台 scanner 输出的音频元数据，UI 不直接构造这个模型。
 */
data class MusicFileMetadata(
    val sourceId: String,
    val sourceKind: LocalMusicSourceKind,
    val localUri: String,
    val fileName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val mimeType: String?,
    val sizeBytes: Long?,
    val modifiedAt: Long?,
    val coverArt: CoverArt,
) {
    /**
     * 跨平台稳定来源 key，重新扫描后用它合并元数据和用户状态。
     */
    val sourceKey: String = "${sourceKind.value}:$sourceId"
}

/**
 * 扫描问题条目，不进入可播放歌曲列表。
 */
data class LocalMusicProblem(
    val sourceKind: LocalMusicSourceKind,
    val sourceId: String,
    val fileName: String,
    val error: LocalMusicScanError,
)

/**
 * 单个来源的扫描摘要，用于二级来源页。
 */
data class LocalMusicSourceSummary(
    val sourceKind: LocalMusicSourceKind,
    val displayName: String,
    val songCount: Int,
    val problemCount: Int,
    val lastScannedAt: Long?,
)

/**
 * 最近一次扫描摘要，用于首页卡片展示结果。
 */
data class LocalMusicLastScanSummary(
    val addedCount: Int,
    val updatedCount: Int,
    val removedCount: Int,
    val problemCount: Int,
    val completedAt: Long,
)

/**
 * Scanner 返回结果，discovered 同时承载新增和更新的可播放候选条目。
 */
data class LocalMusicScanResult(
    val discovered: List<MusicFileMetadata>,
    val removedSourceKeys: Set<String> = emptySet(),
    val failed: List<LocalMusicProblem> = emptyList(),
    val sourceSummaries: List<LocalMusicSourceSummary> = emptyList(),
    val completedAt: Long = 0L,
)

/**
 * 本地扫描状态，页面只根据这个平台无关状态渲染。
 */
sealed interface LocalMusicScanState {
    data object Idle : LocalMusicScanState
    data object WaitingForPermission : LocalMusicScanState
    data class Importing(val progress: LocalMusicScanProgress) : LocalMusicScanState
    data class Scanning(val progress: LocalMusicScanProgress) : LocalMusicScanState
    data class Done(val summary: LocalMusicLastScanSummary) : LocalMusicScanState
    data class Error(
        val error: LocalMusicScanError,
        val summary: LocalMusicLastScanSummary? = null,
    ) : LocalMusicScanState
}

/**
 * 曲库统计值，首页卡片和我的页共用。
 */
data class LibraryStats(
    val songCount: Int = 0,
    val albumCount: Int = 0,
    val artistCount: Int = 0,
)

/**
 * UI 读取曲库的唯一快照，避免页面各自维护列表。
 */
data class LibrarySnapshot(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val stats: LibraryStats,
    val sources: List<LocalMusicSourceSummary>,
    val scanState: LocalMusicScanState,
    val lastScanSummary: LocalMusicLastScanSummary?,
    val problems: List<LocalMusicProblem>,
) {
    companion object {
        /**
         * 无来源时的空曲库快照，保证页面可以安全渲染入口状态。
         */
        val Empty: LibrarySnapshot = LibrarySnapshot(
            songs = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
            stats = LibraryStats(),
            sources = emptyList(),
            scanState = LocalMusicScanState.Idle,
            lastScanSummary = null,
            problems = emptyList(),
        )
    }
}
```

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/LocalMusicScanner.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult

/**
 * 平台无关本地音乐扫描接口，真实平台实现只能放在对应 source set。
 */
interface LocalMusicScanner {
    /**
     * 执行一次本地音乐扫描，返回可合并到曲库快照的扫描结果。
     */
    suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult
}
```

- [x] **Step 4: Run the model test**

Run: `./gradlew :composeApp:desktopTest`

Expected: PASS for `LocalMusicModelsTest`; existing controller tests still pass because no app wiring changed.

- [x] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/LocalMusicScanner.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModelsTest.kt
git commit -m "定义本地音频扫描领域模型"
```

Task 1 executed on `codex/local-audio-domain-boundary` in commit `be61b18`; verified with `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`.

## Task 2: Real-Shaped Song Fields

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/Song.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModelsTest.kt`

- [x] **Step 1: Write the failing song field test**

Append to `LocalMusicModelsTest`:

```kotlin
    /**
     * Song 必须携带 scanner 生成的 localUri，UI 和播放链路不能自行拼接。
     */
    @Test
    fun songExposesScannerGeneratedLocalUri(): Unit {
        val song = Song(
            id = "androidMediaStore:42",
            title = "海边的梦",
            artist = "旅行团乐队",
            album = "似水流年",
            duration = "3:45",
            coverArt = CoverArt.CoverSeaDream,
            isLiked = false,
            lastPlayed = "未播放",
            quality = "本地 FLAC",
            lyric = "海风吹过窗边，像一段刚醒来的旋律。",
            trackNumber = 1,
            durationMs = 225_000L,
            sourceId = "42",
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            localUri = "content://media/external/audio/media/42",
            mimeType = "audio/flac",
            sizeBytes = 24_000_000L,
            modifiedAt = 1_719_360_000_000L,
        )

        assertEquals(
            expected = true,
            actual = song.isPlayable,
        )
        assertEquals(
            expected = "content://media/external/audio/media/42",
            actual = song.localUri,
        )
    }
```

- [x] **Step 2: Run the test to verify it fails**

Run: `./gradlew :composeApp:desktopTest`

Expected: FAIL with constructor errors because `Song` does not yet accept scanner fields.

- [x] **Step 3: Extend `Song` without breaking existing call sites**

Replace `Song.kt` with:

```kotlin
package com.yanhao.kmpmusic.domain.model

/**
 * 本地音乐歌曲模型，作为播放、搜索、收藏和队列的统一数据源。
 *
 * @property id App 内稳定歌曲标识，真实扫描歌曲由 sourceKey 派生。
 * @property title 歌曲标题。
 * @property artist 歌手名称。
 * @property album 所属专辑名称。
 * @property duration 展示用时长。
 * @property coverArt 原型或扫描封面资源标识。
 * @property isLiked 当前收藏状态。
 * @property lastPlayed 最近播放文案。
 * @property quality 本地音质标签。
 * @property lyric 播放页展示的短句。
 * @property trackNumber 专辑内曲序。
 * @property durationMs 真实音频毫秒时长，缺失时为 null。
 * @property sourceId 平台来源侧稳定标识。
 * @property sourceKind 平台来源类型。
 * @property localUri 平台 scanner 生成的本地媒体 URI 或路径。
 * @property mimeType 音频 MIME 类型。
 * @property sizeBytes 文件大小。
 * @property modifiedAt 文件或媒体条目的修改时间戳。
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: String,
    val coverArt: CoverArt,
    val isLiked: Boolean,
    val lastPlayed: String,
    val quality: String,
    val lyric: String,
    val trackNumber: Int,
    val durationMs: Long? = null,
    val sourceId: String = id,
    val sourceKind: LocalMusicSourceKind = LocalMusicSourceKind.FakeScanner,
    val localUri: String = "",
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val modifiedAt: Long? = null,
) {
    /**
     * 只有 scanner 或 importer 生成了 localUri，歌曲才进入可播放列表。
     */
    val isPlayable: Boolean
        get() = localUri.isNotBlank()
}
```

- [x] **Step 4: Run the model tests**

Run: `./gradlew :composeApp:desktopTest`

Expected: PASS. Existing seed calls still compile because new fields have defaults.

- [x] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/Song.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/model/LocalMusicModelsTest.kt
git commit -m "扩展歌曲真实来源字段"
```

Task 2 executed on `codex/local-audio-domain-boundary` in commit `e647bd1`; verified with `./gradlew :composeApp:desktopTest` and `./gradlew :composeApp:compileDebugKotlinAndroid`.

## Task 3: Scan Result Merge and Aggregation

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCase.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCaseTest.kt`

- [x] **Step 1: Write failing merge tests**

Create `MergeLocalMusicScanResultUseCaseTest.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 扫描结果合并测试，保护首页和全量曲库的共同数据来源。
 */
class MergeLocalMusicScanResultUseCaseTest {
    private val useCase: MergeLocalMusicScanResultUseCase = MergeLocalMusicScanResultUseCaseImpl()

    /**
     * 可播放歌曲应按 modifiedAt 降序进入快照，并保留收藏状态。
     */
    @Test
    fun mergeCreatesPlayableSnapshotSortedByModifiedTime(): Unit {
        val result = LocalMusicScanResult(
            discovered = listOf(
                metadata(sourceId = "1", title = "旧歌", modifiedAt = 10L),
                metadata(sourceId = "2", title = "新歌", modifiedAt = 20L),
            ),
            completedAt = 30L,
        )

        val snapshot: LibrarySnapshot = useCase(
            request = MergeLocalMusicScanResultRequest(
                previousSnapshot = LibrarySnapshot.Empty,
                scanResult = result,
                likedSongIds = setOf("fakeScanner:2"),
            ),
        )

        assertEquals(expected = listOf("新歌", "旧歌"), actual = snapshot.songs.map { song -> song.title })
        assertTrue(snapshot.songs.first().isLiked)
        assertEquals(expected = 2, actual = snapshot.stats.songCount)
        assertEquals(expected = 1, actual = snapshot.stats.albumCount)
        assertEquals(expected = 1, actual = snapshot.stats.artistCount)
    }

    /**
     * 失败条目不能进入首页本地歌曲列表，只能出现在问题列表。
     */
    @Test
    fun mergeExcludesFailedEntriesFromSongs(): Unit {
        val failed = LocalMusicProblem(
            sourceKind = LocalMusicSourceKind.FakeScanner,
            sourceId = "bad",
            fileName = "broken.wav",
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.UnsupportedFormat,
                message = "格式不支持",
                sourceKind = LocalMusicSourceKind.FakeScanner,
                sourceId = "bad",
            ),
        )
        val snapshot: LibrarySnapshot = useCase(
            request = MergeLocalMusicScanResultRequest(
                previousSnapshot = LibrarySnapshot.Empty,
                scanResult = LocalMusicScanResult(
                    discovered = listOf(metadata(sourceId = "ok", title = "可播放", modifiedAt = 20L)),
                    failed = listOf(failed),
                    completedAt = 40L,
                ),
                likedSongIds = emptySet(),
            ),
        )

        assertEquals(expected = listOf("可播放"), actual = snapshot.songs.map { song -> song.title })
        assertEquals(expected = 1, actual = snapshot.problems.size)
        assertFalse(snapshot.songs.any { song -> song.sourceId == "bad" })
    }

    /**
     * 缺失元数据必须有稳定兜底，避免 UI 出现空标题或空专辑。
     */
    @Test
    fun mergeUsesMetadataFallbacks(): Unit {
        val snapshot: LibrarySnapshot = useCase(
            request = MergeLocalMusicScanResultRequest(
                previousSnapshot = LibrarySnapshot.Empty,
                scanResult = LocalMusicScanResult(
                    discovered = listOf(
                        metadata(
                            sourceId = "3",
                            fileName = "untitled-track.mp3",
                            title = null,
                            artist = null,
                            album = null,
                            durationMs = null,
                        ),
                    ),
                    completedAt = 50L,
                ),
                likedSongIds = emptySet(),
            ),
        )

        val song = snapshot.songs.single()
        assertEquals(expected = "untitled-track", actual = song.title)
        assertEquals(expected = "未知歌手", actual = song.artist)
        assertEquals(expected = "未知专辑", actual = song.album)
        assertEquals(expected = "--:--", actual = song.duration)
    }

    private fun metadata(
        sourceId: String,
        title: String? = "海边的梦",
        artist: String? = "旅行团乐队",
        album: String? = "似水流年",
        durationMs: Long? = 225_000L,
        fileName: String = "$sourceId.flac",
        modifiedAt: Long?,
    ): MusicFileMetadata {
        return MusicFileMetadata(
            sourceId = sourceId,
            sourceKind = LocalMusicSourceKind.FakeScanner,
            localUri = "fake://local-audio/$sourceId",
            fileName = fileName,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            mimeType = "audio/flac",
            sizeBytes = 24_000_000L,
            modifiedAt = modifiedAt,
            coverArt = CoverArt.CoverSeaDream,
        )
    }
}
```

- [x] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:desktopTest`

Expected: FAIL with unresolved references for `MergeLocalMusicScanResultUseCase`.

- [x] **Step 3: Implement the merge use case**

Create `MergeLocalMusicScanResultUseCase.kt`:

```kotlin
package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 合并扫描结果的请求对象，集中传入旧快照和用户状态。
 */
data class MergeLocalMusicScanResultRequest(
    val previousSnapshot: LibrarySnapshot,
    val scanResult: LocalMusicScanResult,
    val likedSongIds: Set<String>,
)

/**
 * 将平台扫描结果合并为 UI 唯一曲库快照。
 */
interface MergeLocalMusicScanResultUseCase {
    /**
     * 按 sourceKey 合并歌曲、过滤失败项并重新聚合专辑歌手。
     */
    operator fun invoke(request: MergeLocalMusicScanResultRequest): LibrarySnapshot
}

/**
 * 扫描合并实现，保持稳定 id 并把排序规则收敛到 domain 层。
 */
class MergeLocalMusicScanResultUseCaseImpl : MergeLocalMusicScanResultUseCase {
    /** 合并一次扫描结果，返回页面可直接消费的曲库快照。 */
    override operator fun invoke(request: MergeLocalMusicScanResultRequest): LibrarySnapshot {
        val removedSourceKeys: Set<String> = request.scanResult.removedSourceKeys
        val previousSongsById: Map<String, Song> = request.previousSnapshot.songs.associateBy { song -> song.id }
        val discoveredSongs: List<Song> = request.scanResult.discovered
            .filter { metadata -> metadata.localUri.isNotBlank() }
            .filterNot { metadata -> removedSourceKeys.contains(metadata.sourceKey) }
            .mapIndexed { index, metadata ->
                metadata.toSong(
                    index = index,
                    previousSong = previousSongsById[metadata.sourceKey],
                    likedSongIds = request.likedSongIds,
                )
            }
            .sortedWith(compareByDescending<Song> { song -> song.modifiedAt ?: Long.MIN_VALUE }.thenBy { song -> song.title.lowercase() })
        val albums: List<Album> = buildAlbums(songs = discoveredSongs)
        val artists: List<Artist> = buildArtists(songs = discoveredSongs)
        val summary = LocalMusicLastScanSummary(
            addedCount = discoveredSongs.count { song -> !previousSongsById.containsKey(song.id) },
            updatedCount = discoveredSongs.count { song -> previousSongsById.containsKey(song.id) },
            removedCount = removedSourceKeys.size,
            problemCount = request.scanResult.failed.size,
            completedAt = request.scanResult.completedAt,
        )
        return LibrarySnapshot(
            songs = discoveredSongs,
            albums = albums,
            artists = artists,
            stats = LibraryStats(
                songCount = discoveredSongs.size,
                albumCount = albums.size,
                artistCount = artists.size,
            ),
            sources = request.scanResult.sourceSummaries,
            scanState = LocalMusicScanState.Done(summary = summary),
            lastScanSummary = summary,
            problems = request.scanResult.failed,
        )
    }

    // 将 scanner 元数据映射为 UI 歌曲模型，同时应用元数据兜底。
    private fun MusicFileMetadata.toSong(
        index: Int,
        previousSong: Song?,
        likedSongIds: Set<String>,
    ): Song {
        val safeTitle: String = title?.takeIf { value -> value.isNotBlank() } ?: fileName.substringBeforeLast(
            delimiter = ".",
            missingDelimiterValue = fileName,
        )
        val safeArtist: String = artist?.takeIf { value -> value.isNotBlank() } ?: "未知歌手"
        val safeAlbum: String = album?.takeIf { value -> value.isNotBlank() } ?: "未知专辑"
        val songId: String = sourceKey
        return Song(
            id = songId,
            title = safeTitle,
            artist = safeArtist,
            album = safeAlbum,
            duration = formatDuration(durationMs = durationMs),
            coverArt = coverArt,
            isLiked = likedSongIds.contains(songId) || previousSong?.isLiked == true,
            lastPlayed = previousSong?.lastPlayed ?: "未播放",
            quality = formatQuality(mimeType = mimeType),
            lyric = previousSong?.lyric ?: "来自${sourceKind.displayName}的本地音频。",
            trackNumber = previousSong?.trackNumber ?: index + 1,
            durationMs = durationMs,
            sourceId = sourceId,
            sourceKind = sourceKind,
            localUri = localUri,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            modifiedAt = modifiedAt,
        )
    }

    // 从歌曲按专辑名称聚合首页和详情页共用的专辑模型。
    private fun buildAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { song -> normalizeKey(value = song.album) }
            .values
            .map { albumSongs ->
                val firstSong: Song = albumSongs.first()
                Album(
                    id = "album:${normalizeKey(value = firstSong.album)}",
                    title = firstSong.album,
                    artist = firstSong.artist,
                    songCount = albumSongs.size,
                    coverArt = firstSong.coverArt,
                    mood = "本地音乐",
                    year = "本地",
                )
            }
            .sortedBy { album -> album.title.lowercase() }
    }

    // 从歌曲按歌手名称聚合收藏页、搜索页和详情页共用的歌手模型。
    private fun buildArtists(songs: List<Song>): List<Artist> {
        return songs.groupBy { song -> normalizeKey(value = song.artist) }
            .values
            .map { artistSongs ->
                val firstSong: Song = artistSongs.first()
                Artist(
                    id = "artist:${normalizeKey(value = firstSong.artist)}",
                    name = firstSong.artist,
                    songCount = artistSongs.size,
                    coverArt = firstSong.coverArt,
                    tag = "本地音乐",
                )
            }
            .sortedBy { artist -> artist.name.lowercase() }
    }

    // 统一聚合 key，避免大小写和空白造成重复专辑或歌手。
    private fun normalizeKey(value: String): String {
        return value.trim().lowercase()
    }

    // 将毫秒时长转换为 UI 已有的 m:ss 文案。
    private fun formatDuration(durationMs: Long?): String {
        if (durationMs == null || durationMs <= 0L) {
            return "--:--"
        }
        val totalSeconds: Long = durationMs / 1_000L
        val minutes: Long = totalSeconds / 60L
        val seconds: Long = totalSeconds % 60L
        return "$minutes:${seconds.toString().padStart(length = 2, padChar = '0')}"
    }

    // 从 MIME 类型生成轻量音质标签，缺失时不猜测文件编码。
    private fun formatQuality(mimeType: String?): String {
        val suffix: String = mimeType?.substringAfterLast(delimiter = "/")?.uppercase().orEmpty()
        if (suffix.isBlank()) {
            return "本地音频"
        }
        return "本地 $suffix"
    }
}
```

- [x] **Step 4: Run merge tests**

Run: `./gradlew :composeApp:desktopTest`

Expected: PASS for `MergeLocalMusicScanResultUseCaseTest`.

- [x] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCase.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCaseTest.kt
git commit -m "实现扫描结果曲库合并"
```

Task 3 executed on `codex/local-audio-domain-boundary` in commit `c0909b2`; verified with `./gradlew :composeApp:desktopTest` and `./gradlew :composeApp:compileDebugKotlinAndroid`.

## Task 4: Scanner Use Case and In-Memory Library Repository

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/MusicLibraryRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/ScanLocalMusicUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeLocalMusicScanner.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryMusicLibraryRepository.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCaseTest.kt`

- [x] **Step 1: Write the failing scanner/repository test**

Append to `MergeLocalMusicScanResultUseCaseTest`:

```kotlin
    /**
     * 扫描用例必须通过 scanner 和 repository 产出同一份曲库快照。
     */
    @Test
    fun scanUseCaseStoresSnapshotInRepository(): Unit = kotlinx.coroutines.runBlocking {
        val repository = com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository()
        val scanner = com.yanhao.kmpmusic.data.FakeLocalMusicScanner()
        val scanUseCase = ScanLocalMusicUseCaseImpl(
            localMusicScanner = scanner,
            musicLibraryRepository = repository,
        )

        val snapshot: LibrarySnapshot = scanUseCase(
            request = com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest.Refresh,
            likedSongIds = emptySet(),
        )

        assertEquals(expected = snapshot.songs, actual = repository.getSnapshot().songs)
        assertTrue(snapshot.songs.size >= 6)
        assertTrue(snapshot.songs.all { song -> song.localUri.startsWith(prefix = "fake://local-audio/") })
    }
```

- [x] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:desktopTest`

Expected: FAIL with unresolved references for `InMemoryMusicLibraryRepository`, `FakeLocalMusicScanner`, and the new `ScanLocalMusicUseCaseImpl` constructor.

- [x] **Step 3: Update the music library repository interface**

Replace `MusicLibraryRepository.kt` with:

```kotlin
package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 音乐库读取与扫描合并接口，UI 只读取统一的 [LibrarySnapshot]。
 */
interface MusicLibraryRepository {
    /**
     * 获取当前曲库快照。
     */
    fun getSnapshot(): LibrarySnapshot

    /**
     * 合并扫描结果并返回新的曲库快照。
     */
    fun applyScanResult(
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot

    /**
     * 获取本地歌曲，保留旧用例兼容性。
     */
    fun getSongs(): List<Song> = getSnapshot().songs

    /**
     * 获取本地专辑，保留旧用例兼容性。
     */
    fun getAlbums(): List<Album> = getSnapshot().albums

    /**
     * 获取本地歌手，保留旧用例兼容性。
     */
    fun getArtists(): List<Artist> = getSnapshot().artists
}
```

- [x] **Step 4: Replace the scan use case contract**

Replace `ScanLocalMusicUseCase.kt` with:

```kotlin
package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository

/**
 * 本地音乐扫描接口，协调平台 scanner 和曲库快照合并。
 */
interface ScanLocalMusicUseCase {
    /**
     * 执行扫描并把结果写入曲库仓库。
     */
    suspend operator fun invoke(
        request: LocalMusicScanRequest,
        likedSongIds: Set<String>,
    ): LibrarySnapshot
}

/**
 * 本地音乐扫描实现，保证 scanner 不直接驱动 Composable。
 */
class ScanLocalMusicUseCaseImpl(
    private val localMusicScanner: LocalMusicScanner,
    private val musicLibraryRepository: MusicLibraryRepository,
) : ScanLocalMusicUseCase {
    /** 扫描并合并为 UI 可读的 [LibrarySnapshot]。 */
    override suspend operator fun invoke(
        request: LocalMusicScanRequest,
        likedSongIds: Set<String>,
    ): LibrarySnapshot {
        val result: LocalMusicScanResult = localMusicScanner.scan(request = request)
        return musicLibraryRepository.applyScanResult(
            scanResult = result,
            likedSongIds = likedSongIds,
        )
    }
}
```

- [x] **Step 5: Add the in-memory repository implementation**

Create `InMemoryMusicLibraryRepository.kt`:

```kotlin
package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.usecase.MergeLocalMusicScanResultRequest
import com.yanhao.kmpmusic.domain.usecase.MergeLocalMusicScanResultUseCase
import com.yanhao.kmpmusic.domain.usecase.MergeLocalMusicScanResultUseCaseImpl

/**
 * 内存曲库仓库，承接 fake scanner 和后续平台 scanner 的扫描结果。
 */
class InMemoryMusicLibraryRepository(
    private val mergeUseCase: MergeLocalMusicScanResultUseCase = MergeLocalMusicScanResultUseCaseImpl(),
) : MusicLibraryRepository {
    // 当前曲库快照，第一阶段不落盘。
    private var snapshot: LibrarySnapshot = LibrarySnapshot.Empty

    /** 获取当前曲库快照。 */
    override fun getSnapshot(): LibrarySnapshot = snapshot

    /** 合并扫描结果并更新内存快照。 */
    override fun applyScanResult(
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot {
        snapshot = mergeUseCase(
            request = MergeLocalMusicScanResultRequest(
                previousSnapshot = snapshot,
                scanResult = scanResult,
                likedSongIds = likedSongIds,
            ),
        )
        return snapshot
    }
}
```

- [x] **Step 6: Add the explicit fake scanner**

Create `FakeLocalMusicScanner.kt` with at least eight songs so homepage max-6 behavior can be tested:

```kotlin
package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner

/**
 * 显式 fake scanner，只用于 common 阶段验证 UI 与数据链路。
 */
class FakeLocalMusicScanner : LocalMusicScanner {
    /** 返回真实形态的扫描元数据，不复用 seed repository 冒充平台扫描。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        val songs: List<MusicFileMetadata> = buildFakeMetadata()
        return LocalMusicScanResult(
            discovered = songs,
            sourceSummaries = listOf(
                LocalMusicSourceSummary(
                    sourceKind = LocalMusicSourceKind.FakeScanner,
                    displayName = LocalMusicSourceKind.FakeScanner.displayName,
                    songCount = songs.size,
                    problemCount = 0,
                    lastScannedAt = 1_782_043_200_000L,
                ),
            ),
            completedAt = 1_782_043_200_000L,
        )
    }

    // 构造带 sourceKind/sourceId/localUri/modifiedAt 的演示扫描数据。
    private fun buildFakeMetadata(): List<MusicFileMetadata> {
        return listOf(
            metadata("001", "海边的梦", "旅行团乐队", "似水流年", 225_000L, "audio/flac", CoverArt.CoverSeaDream, 1_782_043_200_000L),
            metadata("002", "Summer Waltz", "久石让", "Dream Stories", 265_000L, "audio/aac", CoverArt.CoverSummerWaltz, 1_782_043_100_000L),
            metadata("003", "像水流年", "旅行团乐队", "似水流年", 238_000L, "audio/flac", CoverArt.AlbumRiverYear, 1_782_043_000_000L),
            metadata("004", "The Best of Me", "A-Lin", "The Best of Me", 247_000L, "audio/alac", CoverArt.AlbumBestOfMe, 1_782_042_900_000L),
            metadata("005", "时光森林", "苏打绿", "时光森林", 311_000L, "audio/mpeg", CoverArt.AlbumTimeForest, 1_782_042_800_000L),
            metadata("006", "沿岸公路", "旅行团乐队", "似水流年", 251_000L, "audio/flac", CoverArt.AlbumRiverYear, 1_782_042_700_000L),
            metadata("007", "小情歌", "苏打绿", "时光森林", 273_000L, "audio/mpeg", CoverArt.AlbumTimeForest, 1_782_042_600_000L),
            metadata("008", "One Summer's Day", "久石让", "Dream Stories", 248_000L, "audio/aac", CoverArt.CoverSummerWaltz, 1_782_042_500_000L),
        )
    }

    // 创建单首 fake 音频元数据，sourceId 与 localUri 保持一一对应。
    private fun metadata(
        sourceId: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
        mimeType: String,
        coverArt: CoverArt,
        modifiedAt: Long,
    ): MusicFileMetadata {
        return MusicFileMetadata(
            sourceId = sourceId,
            sourceKind = LocalMusicSourceKind.FakeScanner,
            localUri = "fake://local-audio/$sourceId",
            fileName = "$sourceId-${title.lowercase()}.audio",
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            mimeType = mimeType,
            sizeBytes = 24_000_000L,
            modifiedAt = modifiedAt,
            coverArt = coverArt,
        )
    }
}
```

- [x] **Step 7: Run tests**

Run: `./gradlew :composeApp:desktopTest`

Expected: PASS. If `SeedMusicLibraryRepository` fails to compile because `MusicLibraryRepository` added new abstract methods, update it in the same task by implementing `getSnapshot()` and `applyScanResult()` with its current seed lists wrapped in a `LibrarySnapshot`; do not make it the app default in later tasks.

- [x] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/MusicLibraryRepository.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/ScanLocalMusicUseCase.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/FakeLocalMusicScanner.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryMusicLibraryRepository.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/usecase/MergeLocalMusicScanResultUseCaseTest.kt
git commit -m "接入本地音频扫描快照仓库"
```

Task 4 executed on `codex/local-audio-domain-boundary` in commit `3be686b`; verified with `./gradlew :composeApp:desktopTest` and `./gradlew :composeApp:compileDebugKotlinAndroid`. `SeedMusicLibraryRepository` and `MusicAppController` were also updated for compile-safe compatibility after the repository and scan use case contracts changed.

## Task 5: Playback History Is Separate From Scanned Songs

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/PlaybackRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryPlaybackRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/PlaybackUseCases.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [x] **Step 1: Add failing controller tests for recent playback semantics**

Append to `MusicAppControllerTest` after controller wiring is adjusted in Task 6 if needed:

```kotlin
    /**
     * 扫描完成只应填充本地歌曲，不应把扫描结果冒充最近播放。
     */
    @Test
    fun scanDoesNotPopulateRecentPlayback(): Unit = kotlinx.coroutines.runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest.Refresh)

        assertEquals(expected = 8, actual = controller.uiState.songs.size)
        assertEquals(expected = 6, actual = controller.uiState.localSongPreview.size)
        assertTrue(controller.uiState.recentSongs.isEmpty())
    }

    /**
     * 用户真正播放歌曲后，最近播放才出现该歌曲。
     */
    @Test
    fun playSongAddsRecentPlayback(): Unit = kotlinx.coroutines.runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest.Refresh)
        val targetSong = controller.uiState.songs.first()
        controller.playSong(song = targetSong)

        assertEquals(expected = listOf(targetSong.id), actual = controller.uiState.recentSongs.map { song -> song.id })
    }

    /**
     * 队列为空时切歌不能用扫描曲库第一首静默替换当前播放。
     */
    @Test
    fun moveTrackDoesNotUseScannedSongsAsImplicitQueue(): Unit = kotlinx.coroutines.runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest.Refresh)

        controller.moveTrack(direction = 1)

        assertEquals(expected = null, actual = controller.uiState.currentSongId)
        assertFalse(controller.uiState.isPlaying)
        assertTrue(controller.uiState.queueSongIds.isEmpty())
    }
```

Execution note: Task 6 才会引入 `scanLocalMusic`、`localSongPreview`、`recentSongs`
这些 UI 状态字段；本轮 Task 5 先落地可独立验证的播放语义测试：
启动不预填当前播放/队列、播放动作写入 `PlaybackHistory`、空队列切歌不从曲库兜底。
扫描后最近播放为空的 controller 断言保留给 Task 6 状态接线时补齐。

- [x] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:desktopTest`

Expected: FAIL because `scanLocalMusic`, `localSongPreview`, and `recentSongs` are not wired yet. Keep these failing tests for Task 6.

- [x] **Step 3: Update playback models**

Replace the playback part of `PlaybackModels.kt` with:

```kotlin
/**
 * 当前播放状态，供迷你播放器和播放页共享。
 *
 * @property currentSongId 当前歌曲标识，没有播放过歌曲时为 null。
 * @property isPlaying 是否正在播放。
 */
data class PlaybackState(
    val currentSongId: String? = null,
    val isPlaying: Boolean = false,
)

/**
 * 播放队列状态。
 *
 * @property songIds 当前播放队列中的歌曲标识。
 */
data class QueueState(
    val songIds: List<String> = emptyList(),
)

/**
 * 真实播放历史，扫描结果不会自动进入这里。
 *
 * @property songIds 最近播放歌曲标识，新播放的歌曲排在最前。
 */
data class PlaybackHistory(
    val songIds: List<String> = emptyList(),
)
```

- [x] **Step 4: Update playback repository**

Add history methods to `PlaybackRepository.kt`:

```kotlin
    /**
     * 读取真实播放历史。
     */
    fun getPlaybackHistory(): PlaybackHistory

    /**
     * 保存真实播放历史。
     */
    fun savePlaybackHistory(history: PlaybackHistory)
```

- [x] **Step 5: Update in-memory playback storage**

In `InMemoryPlaybackRepository.kt`, replace default state and add history:

```kotlin
    // 当前播放状态，未播放前不指向任何歌曲。
    private var playbackState: PlaybackState = PlaybackState()

    // 当前队列状态，扫描结果不会自动写入队列。
    private var queueState: QueueState = QueueState()

    // 真实播放历史，只由播放动作写入。
    private var playbackHistory: PlaybackHistory = PlaybackHistory()
```

Add:

```kotlin
    /** 获取真实播放历史。 */
    override fun getPlaybackHistory(): PlaybackHistory = playbackHistory

    /** 保存真实播放历史。 */
    override fun savePlaybackHistory(history: PlaybackHistory) {
        playbackHistory = history
    }
```

- [x] **Step 6: Record history in `PlaySongUseCaseImpl`**

Inside `PlaySongUseCaseImpl.invoke`, after saving queue and playback state, add:

```kotlin
        val currentHistory: List<String> = playbackRepository.getPlaybackHistory().songIds
        val nextHistory: List<String> = listOf(song.id) + currentHistory.filterNot { songId -> songId == song.id }
        playbackRepository.savePlaybackHistory(history = PlaybackHistory(songIds = nextHistory.take(n = 50)))
```

Import `PlaybackHistory`.

- [x] **Step 7: Make queue movement nullable-safe and remove full-library fallback**

Change `MoveQueueUseCase` so it no longer accepts `fallbackSongs`:

```kotlin
interface MoveQueueUseCase {
    /**
     * 按方向移动已有队列，队列为空或当前歌曲缺失时保持原状态。
     */
    operator fun invoke(direction: Int): PlaybackState
}
```

Replace `MoveQueueUseCaseImpl.invoke` with:

```kotlin
    /** 基于显式队列循环切歌，不用全曲库静默替换缺失歌曲。 */
    override operator fun invoke(direction: Int): PlaybackState {
        val currentState: PlaybackState = playbackRepository.getPlaybackState()
        val queueIds: List<String> = playbackRepository.getQueueState().songIds
        if (queueIds.isEmpty()) {
            return currentState
        }
        val currentSongId: String = currentState.currentSongId ?: return currentState
        val currentIndex: Int = queueIds.indexOf(currentSongId)
        if (currentIndex < 0) {
            return currentState
        }
        val nextIndex: Int = (currentIndex + direction + queueIds.size) % queueIds.size
        val nextState: PlaybackState = PlaybackState(
            currentSongId = queueIds[nextIndex],
            isPlaying = true,
        )
        playbackRepository.savePlaybackState(state = nextState)
        return nextState
    }
```

Update `MusicAppController.moveTrack` accordingly:

```kotlin
    /** 切换上一首或下一首，只在显式队列内移动。 */
    fun moveTrack(direction: Int) {
        val playbackState: PlaybackState = moveQueueUseCase(direction = direction)
        syncPlaybackState(playbackState = playbackState)
    }
```

Update the existing `moveTrackChangesCurrentSong` test so it first scans and plays two songs:

```kotlin
    @Test
    fun moveTrackChangesCurrentSong(): Unit = kotlinx.coroutines.runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest.Refresh)
        controller.playSong(song = controller.uiState.songs[0])
        controller.playSong(song = controller.uiState.songs[1])
        val originalSongId: String? = controller.uiState.currentSongId

        controller.moveTrack(direction = 1)

        assertNotEquals(illegal = originalSongId, actual = controller.uiState.currentSongId)
        assertTrue(controller.uiState.isPlaying)
    }
```

- [x] **Step 8: Run tests**

Run: `./gradlew :composeApp:desktopTest`

Expected: Task 5 playback changes compile. The controller tests added in Step 1 may still fail until Task 6; leave them failing only if their failure is caused by missing controller fields, not playback model compilation errors.

- [x] **Step 9: Commit after Task 6 passes**

Commit Task 5 together with Task 6 because the new controller tests span both layers:

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/repository/PlaybackRepository.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/InMemoryPlaybackRepository.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/usecase/PlaybackUseCases.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "拆分扫描列表和真实播放历史"
```

## Task 6: Controller State From LibrarySnapshot

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/DetailScreens.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [x] **Step 1: Add failing navigation and preview tests**

Append to `MusicAppControllerTest`:

```kotlin
    /**
     * 查看全部应进入本地音乐二级页，底部 Tab 隐藏但 mini-player 策略保持普通二级页。
     */
    @Test
    fun openLocalMusicUsesSecondaryChrome(): Unit {
        val controller = MusicAppController()

        controller.openLocalMusic(section = LocalMusicSection.Songs)

        assertEquals(
            expected = SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Songs),
            actual = controller.uiState.navigationState.secondaryScreen,
        )
        assertEquals(expected = AppChromeMode.SecondaryWithMiniPlayer, actual = controller.uiState.navigationState.chromeMode)
    }
```

- [x] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:desktopTest`

Expected: FAIL with unresolved references for `LocalMusicSection`, `SecondaryScreen.LocalMusic`, and controller scan methods.

- [x] **Step 3: Update app models**

In `MusicAppModels.kt`:

Add imports:

```kotlin
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
```

Add enum:

```kotlin
/**
 * 本地音乐二级页分段。
 */
enum class LocalMusicSection {
    Songs,
    Albums,
    Artists,
    Sources,
}
```

Replace `SecondaryScreen.LocalFolder` with:

```kotlin
    data class LocalMusic(val initialSection: LocalMusicSection = LocalMusicSection.Songs) : SecondaryScreen
```

Update `chromeMode` and `routeName()` branches with `is SecondaryScreen.LocalMusic`.

Update `MusicAppUiState` fields:

```kotlin
    val currentSongId: String?,
    val libraryStats: LibraryStats = LibraryStats(),
    val localMusicSources: List<LocalMusicSourceSummary> = emptyList(),
    val localMusicProblems: List<LocalMusicProblem> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
    val localSongPreview: List<Song> = emptyList(),
    val scanState: LocalMusicScanState = LocalMusicScanState.Idle,
```

Change current song to nullable:

```kotlin
    /**
     * 当前播放歌曲，没有真实播放时不显示迷你播放器。
     */
    val currentSong: Song? = currentSongId?.let { id ->
        songs.firstOrNull { song -> song.id == id }
    }
```

Change selected album and artist to nullable so an empty, unscanned library can render safely:

```kotlin
    /**
     * 当前专辑详情对象，曲库为空或专辑缺失时为 null。
     */
    val selectedAlbum: Album? = albums.firstOrNull { album -> album.id == selectedAlbumId }

    /**
     * 当前歌手详情对象，曲库为空或歌手缺失时为 null。
     */
    val selectedArtist: Artist? = artists.firstOrNull { artist -> artist.id == selectedArtistId }
```

Change `currentSongId` parameters from `String` to `String?` in these Composables; comparisons such as `song.id == currentSongId` remain valid:

```text
composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/SearchScreen.kt
composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/FavoritesScreen.kt
composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/DetailScreens.kt
composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt
```

In `MusicApp.kt`, guard nullable detail targets:

```kotlin
                SecondaryScreen.AlbumDetail -> state.selectedAlbum?.let { album ->
                    AlbumDetailScreen(
                        album = album,
                        songs = state.songs,
                        currentSongId = state.currentSongId,
                        onBack = controller::navigateBack,
                        onSongOpen = controller::openSong,
                        onSongPlay = controller::playSong,
                        onMore = controller::openMore,
                        onLike = controller::toggleFavorite,
                    )
                } ?: MissingLibraryItemScreen(
                    title = "专辑不可用",
                    onBack = controller::navigateBack,
                )

                SecondaryScreen.ArtistDetail -> state.selectedArtist?.let { artist ->
                    ArtistDetailScreen(
                        artist = artist,
                        songs = state.songs,
                        albums = state.albums,
                        currentSongId = state.currentSongId,
                        onBack = controller::navigateBack,
                        onSongOpen = controller::openSong,
                        onSongPlay = controller::playSong,
                        onMore = controller::openMore,
                        onLike = controller::toggleFavorite,
                        onAlbumOpen = controller::openAlbum,
                    )
                } ?: MissingLibraryItemScreen(
                    title = "歌手不可用",
                    onBack = controller::navigateBack,
                )
```

Create `MissingLibraryItemScreen` in `DetailScreens.kt`:

```kotlin
/**
 * 曲库条目缺失时的二级页兜底，避免空库状态崩溃。
 */
@Composable
fun MissingLibraryItemScreen(
    title: String,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        AppHeader(title = title, subtitle = "重新扫描后再试", onBack = onBack)
        Text(text = "当前曲库中找不到这个条目。", color = MusicColors.Muted)
    }
}
```

Guard the player route as well:

```kotlin
                SecondaryScreen.Player -> state.currentSong?.let { song ->
                    PlayerScreen(
                        song = song,
                        isPlaying = state.isPlaying,
                        onBack = controller::navigateBack,
                        onToggle = controller::togglePlayback,
                        onPrev = { controller.moveTrack(direction = -1) },
                        onNext = { controller.moveTrack(direction = 1) },
                        onLike = controller::toggleFavorite,
                        onQueue = controller::openQueue,
                    )
                } ?: MissingLibraryItemScreen(
                    title = "暂无播放",
                    onBack = controller::navigateBack,
                )
```

- [x] **Step 4: Update controller dependencies**

In `MusicAppController.kt`, replace the default repository and scan use case construction:

```kotlin
import com.yanhao.kmpmusic.data.FakeLocalMusicScanner
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicScanProgress
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
```

Constructor:

```kotlin
class MusicAppController(
    private val musicLibraryRepository: MusicLibraryRepository = InMemoryMusicLibraryRepository(),
    private val localMusicScanner: LocalMusicScanner = FakeLocalMusicScanner(),
    private val playbackRepository: PlaybackRepository = InMemoryPlaybackRepository(),
    private val userPreferencesRepository: UserPreferencesRepository = InMemoryUserPreferencesRepository(),
)
```

Use case:

```kotlin
    // 本地扫描用例。
    private val scanLocalMusicUseCase: ScanLocalMusicUseCase = ScanLocalMusicUseCaseImpl(
        localMusicScanner = localMusicScanner,
        musicLibraryRepository = musicLibraryRepository,
    )
```

- [x] **Step 5: Add scan and local music navigation methods**

Add to `MusicAppController`:

```kotlin
    /** 扫描本地音乐并同步曲库快照。 */
    suspend fun scanLocalMusic(request: LocalMusicScanRequest = LocalMusicScanRequest.Refresh) {
        uiState = uiState.copy(
            scanState = LocalMusicScanState.Scanning(
                progress = LocalMusicScanProgress(currentSourceName = "本地音乐"),
            ),
            isQueueOpen = false,
            moreSongId = null,
        )
        val snapshot: LibrarySnapshot = scanLocalMusicUseCase(
            request = request,
            likedSongIds = uiState.likedSongIds,
        )
        syncLibrarySnapshot(snapshot = snapshot)
    }

    /** 打开本地音乐二级页并指定初始分段。 */
    fun openLocalMusic(section: LocalMusicSection = LocalMusicSection.Songs) {
        navigateToSecondary(screen = SecondaryScreen.LocalMusic(initialSection = section))
    }
```

Replace old `advanceScan`, `openScan`, and `closeScan` callers after Task 8. During this task, keep `closeScan()` only if overlays still reference it; make it set `scanState = LocalMusicScanState.Idle`.

- [x] **Step 6: Sync snapshot-derived state**

Add private helpers:

```kotlin
    // 曲库快照是首页、搜索、收藏和我的页的唯一列表来源。
    private fun syncLibrarySnapshot(snapshot: LibrarySnapshot) {
        val likedSongIds: Set<String> = favoritesRepository.getLikedSongIds()
        val songsWithLikes: List<Song> = snapshot.songs.map { song ->
            song.copy(isLiked = likedSongIds.contains(song.id) || song.isLiked)
        }
        uiState = uiState.copy(
            songs = songsWithLikes,
            albums = snapshot.albums,
            artists = snapshot.artists,
            libraryStats = snapshot.stats,
            localMusicSources = snapshot.sources,
            localMusicProblems = snapshot.problems,
            scanState = snapshot.scanState,
            recentSongs = buildRecentSongs(songs = songsWithLikes),
            localSongPreview = songsWithLikes.take(n = 6),
        )
    }

    // 最近播放只读取播放历史，不从扫描结果自动生成。
    private fun buildRecentSongs(songs: List<Song>): List<Song> {
        return playbackRepository.getPlaybackHistory().songIds
            .mapNotNull { songId -> songs.firstOrNull { song -> song.id == songId } }
            .take(n = 2)
    }
```

Update `playSong` and `syncPlaybackState` to refresh `recentSongs`:

```kotlin
        uiState = uiState.copy(
            currentSongId = playbackState.currentSongId,
            isPlaying = playbackState.isPlaying,
            queueSongIds = playbackRepository.getQueueState().songIds,
            recentSongs = buildRecentSongs(songs = uiState.songs),
        )
```

- [x] **Step 7: Initialize state from repository snapshot**

In `createInitialState()`:

```kotlin
        val snapshot: LibrarySnapshot = musicLibraryRepository.getSnapshot()
        val playbackState: PlaybackState = playbackRepository.getPlaybackState()
        val queueState: QueueState = playbackRepository.getQueueState()
        val likedSongIds: Set<String> = snapshot.songs.filter { song ->
            song.isLiked
        }.map { song ->
            song.id
        }.toSet()
        return MusicAppUiState(
            songs = snapshot.songs,
            albums = snapshot.albums,
            artists = snapshot.artists,
            libraryStats = snapshot.stats,
            localMusicSources = snapshot.sources,
            localMusicProblems = snapshot.problems,
            likedSongIds = likedSongIds,
            currentSongId = playbackState.currentSongId,
            isPlaying = playbackState.isPlaying,
            queueSongIds = queueState.songIds,
            recentSongs = buildRecentSongs(songs = snapshot.songs),
            localSongPreview = snapshot.songs.take(n = 6),
            scanState = snapshot.scanState,
            themeMode = userPreferencesRepository.getThemeMode(),
        )
```

- [x] **Step 8: Run controller tests**

Run: `./gradlew :composeApp:desktopTest`

Expected: PASS for the new scan/recent/navigation tests and existing controller tests, after adjusting old test song ids to scan first before selecting a target song.

- [x] **Step 9: Commit Task 5 and Task 6 together if not committed**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "让控制器消费本地曲库快照"
```

## Task 7: Homepage Local Songs Preview

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`

- [x] **Step 1: Update `HomeScreen` signature**

Change `HomeScreen` parameters:

```kotlin
fun HomeScreen(
    songs: List<Song>,
    albums: List<Album>,
    libraryStats: LibraryStats,
    scanState: LocalMusicScanState,
    recentSongs: List<Song>,
    localSongPreview: List<Song>,
    currentSongId: String?,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onLocalMusic: () -> Unit,
    onSongOpen: (Song) -> Unit,
    onSongPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
)
```

Import `LibraryStats` and `LocalMusicScanState`.

- [x] **Step 2: Remove mock recent-song builder**

Delete `buildHomeRecentSongs`. The caller now passes `recentSongs` from true playback history.

- [x] **Step 3: Render the new homepage order**

Inside `HomeScreen`, use this order:

```kotlin
        LibraryCard(
            stats = libraryStats,
            scanState = scanState,
            onScan = onScan,
            onLocalMusic = onLocalMusic,
        )
        Spacer(modifier = Modifier.height(scaledDp(24.dp)))
        SectionTitle(
            title = "最近播放",
            actionLabel = "全部",
            onAction = onSearch,
        )
        Spacer(modifier = Modifier.height(scaledDp(14.dp)))
        if (recentSongs.isEmpty()) {
            Text(
                text = "播放过的歌曲会出现在这里",
                color = MusicColors.Muted,
                fontSize = scaledSp(15.sp),
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(scaledDp(14.dp))) {
                recentSongs.forEach { song ->
                    SongRow(
                        song = song,
                        isCurrentSong = song.id == currentSongId,
                        onOpen = onSongOpen,
                        onPlay = onSongPlay,
                        onMore = onMore,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(scaledDp(18.dp)))
        if (localSongPreview.isNotEmpty()) {
            SectionTitle(
                title = "本地歌曲",
                actionLabel = "查看全部",
                onAction = onLocalMusic,
            )
            Spacer(modifier = Modifier.height(scaledDp(14.dp)))
            Column(verticalArrangement = Arrangement.spacedBy(scaledDp(14.dp))) {
                localSongPreview.forEach { song ->
                    SongRow(
                        song = song,
                        isCurrentSong = song.id == currentSongId,
                        onOpen = onSongOpen,
                        onPlay = onSongPlay,
                        onMore = onMore,
                    )
                }
            }
            Spacer(modifier = Modifier.height(scaledDp(18.dp)))
        }
```

Keep `本地专辑` below this block and continue using `albums.take(3)`.

- [x] **Step 4: Make `LibraryCard` use real stats and action label**

Change `LibraryCard` parameters:

```kotlin
private fun LibraryCard(
    stats: LibraryStats,
    scanState: LocalMusicScanState,
    onScan: () -> Unit,
    onLocalMusic: () -> Unit,
)
```

Replace hardcoded values:

```kotlin
Text(text = stats.songCount.toString())
Text(text = "${stats.albumCount} 张专辑 · ${stats.artistCount} 位歌手")
PrimaryPill(text = scanActionLabel(scanState = scanState), onClick = onScan)
```

Add helper:

```kotlin
// 首页阶段使用平台无关状态决定主按钮文案，真实平台入口在平台 scanner 计划中细分。
private fun scanActionLabel(scanState: LocalMusicScanState): String {
    return when (scanState) {
        LocalMusicScanState.Idle -> "扫描本地音乐"
        LocalMusicScanState.WaitingForPermission -> "继续授权"
        is LocalMusicScanState.Importing -> "导入中"
        is LocalMusicScanState.Scanning -> "扫描中"
        is LocalMusicScanState.Done -> "重新扫描"
        is LocalMusicScanState.Error -> "重试扫描"
    }
}
```

- [x] **Step 5: Wire HomeScreen from `MusicApp.kt`**

At the top of `MusicApp.kt`, add imports:

```kotlin
import androidx.compose.runtime.rememberCoroutineScope
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import kotlinx.coroutines.launch
```

Inside `MusicApp`, create one scan callback before `AppContent`:

```kotlin
val coroutineScope = rememberCoroutineScope()
val scanLocalMusic: () -> Unit = {
    coroutineScope.launch {
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
    }
}
```

Update `AppContent` and `RootScreen` signatures so the callback is passed explicitly:

```kotlin
private fun AppContent(
    state: MusicAppUiState,
    controller: MusicAppController,
    chromeMode: AppChromeMode,
    onScanLocalMusic: () -> Unit,
)

private fun RootScreen(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
)
```

Call `AppContent` with `onScanLocalMusic = scanLocalMusic`, and call `RootScreen` with the same callback.

In `RootScreen`, update `HomeScreen` call:

```kotlin
        RootTab.Home -> HomeScreen(
            songs = state.songs,
            albums = state.albums,
            libraryStats = state.libraryStats,
            scanState = state.scanState,
            recentSongs = state.recentSongs,
            localSongPreview = state.localSongPreview,
            currentSongId = state.currentSongId,
            onSearch = { controller.navigateToSecondary(SecondaryScreen.Search) },
            onScan = onScanLocalMusic,
            onLocalMusic = { controller.openLocalMusic(section = LocalMusicSection.Songs) },
            onSongOpen = controller::openSong,
            onSongPlay = controller::playSong,
            onMore = controller::openMore,
            onAlbumOpen = controller::openAlbum,
        )
```

- [x] **Step 6: Keep bottom navigation visible when no mini-player exists**

Change `BottomChrome` to accept a nullable song:

```kotlin
private fun BottomChrome(
    song: Song?,
    isPlaying: Boolean,
    placement: BottomChromePlacement,
    showsBottomNavigation: Boolean,
    rootTab: RootTab,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onQueue: () -> Unit,
    onRootTab: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
)
```

Inside `BottomChrome`, compute mini-player height from the nullable song:

```kotlin
val miniPlayerHeight: Dp = if (song == null) 0.dp else scaledDp(MusicDimens.MiniPlayerHeight)
val stackHeight: Dp = miniPlayerHeight + scaledDp(MusicDimens.BottomNavHeight)
```

Render `MiniPlayer` only when a song exists, and always render `BottomNavigation` at the bottom of the stack:

```kotlin
song?.let { currentSong ->
    MiniPlayer(
        song = currentSong,
        isPlaying = isPlaying,
        onOpen = onOpen,
        onToggle = onToggle,
        onPrev = onPrev,
        onQueue = onQueue,
        modifier = Modifier.align(Alignment.TopCenter),
    )
}
BottomNavigation(
    rootTab = rootTab,
    isEnabled = showsBottomNavigation,
    onRootTab = onRootTab,
    modifier = Modifier.align(Alignment.BottomCenter),
)
```

Keep `MiniPlayer` itself non-nullable; only `BottomChrome` decides whether to render it.

Execution note: 该能力已在 Task 5/6 中完成；本任务复核了 [BottomChrome] 仍接收可空歌曲且无当前播放时只渲染底部导航。

- [x] **Step 7: Run Android compile**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`

Expected: PASS. If Compose reports stale imports, remove unused `IconButton` or `FolderOpen` imports only where the compiler identifies them.

Execution note: `./gradlew :composeApp:compileDebugKotlinAndroid` PASS；额外运行 `./gradlew :composeApp:desktopTest` PASS。

- [x] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt
git commit -m "首页展示扫描本地歌曲预览"
```

Execution note: commit `4aa3ee0 首页展示扫描本地歌曲预览`.

## Task 8: Secondary Local Music Page

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicSourceSection.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/UtilityScreens.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [x] **Step 1: Add the page Composable**

Create `LocalMusicScreen.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.components.AlbumCard
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.ArtistRow
import com.yanhao.kmpmusic.feature.components.SectionTitle
import com.yanhao.kmpmusic.feature.components.SongRow

/**
 * 本地音乐二级页，承载首页“查看全部”后的全量曲库浏览。
 */
@Composable
fun LocalMusicScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    sources: List<LocalMusicSourceSummary>,
    problems: List<LocalMusicProblem>,
    initialSection: LocalMusicSection,
    currentSongId: String?,
    onBack: () -> Unit,
    onSongOpen: (Song) -> Unit,
    onSongPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    var section: LocalMusicSection by remember(initialSection) {
        mutableStateOf(initialSection)
    }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        AppHeader(title = "本地音乐", subtitle = "${songs.size} 首可播放歌曲", onBack = onBack)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LocalMusicSection.entries.forEach { item ->
                FilterChip(
                    selected = section == item,
                    onClick = { section = item },
                    label = { Text(text = item.label()) },
                )
            }
        }
        when (section) {
            LocalMusicSection.Songs -> SongSection(
                songs = songs,
                currentSongId = currentSongId,
                onSongOpen = onSongOpen,
                onSongPlay = onSongPlay,
                onMore = onMore,
            )
            LocalMusicSection.Albums -> AlbumSection(albums = albums, onAlbumOpen = onAlbumOpen)
            LocalMusicSection.Artists -> ArtistSection(artists = artists, onArtistOpen = onArtistOpen)
            LocalMusicSection.Sources -> SourceSection(sources = sources, problems = problems)
        }
    }
}

// 渲染全量歌曲列表，复用全局播放中红色规则。
@Composable
private fun SongSection(
    songs: List<Song>,
    currentSongId: String?,
    onSongOpen: (Song) -> Unit,
    onSongPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        songs.forEach { song ->
            SongRow(
                song = song,
                isCurrentSong = song.id == currentSongId,
                onOpen = onSongOpen,
                onPlay = onSongPlay,
                onMore = onMore,
                dense = true,
            )
        }
    }
}

// 渲染真实曲库聚合出的专辑。
@Composable
private fun AlbumSection(
    albums: List<Album>,
    onAlbumOpen: (Album) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        albums.chunked(size = 2).forEach { rowAlbums ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                rowAlbums.forEach { album ->
                    AlbumCard(album = album, onOpen = onAlbumOpen, modifier = Modifier.weight(weight = 1f))
                }
            }
        }
    }
}

// 渲染真实曲库聚合出的歌手。
@Composable
private fun ArtistSection(
    artists: List<Artist>,
    onArtistOpen: (Artist) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        artists.forEach { artist ->
            ArtistRow(artist = artist, onOpen = onArtistOpen)
        }
    }
}

// 渲染来源和扫描问题，让部分失败不会阻塞成功歌曲展示。
@Composable
private fun SourceSection(
    sources: List<LocalMusicSourceSummary>,
    problems: List<LocalMusicProblem>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(title = "来源", meta = "${sources.size} 个")
        sources.forEach { source ->
            Text(
                text = "${source.displayName} · ${source.songCount} 首 · ${source.problemCount} 个问题",
                color = MusicColors.Muted,
                fontWeight = FontWeight.SemiBold,
            )
        }
        SectionTitle(title = "扫描问题", meta = "${problems.size} 个")
        if (problems.isEmpty()) {
            Text(text = "没有扫描问题", color = MusicColors.Muted)
        } else {
            problems.forEach { problem ->
                Text(
                    text = "${problem.fileName} · ${problem.error.message}",
                    color = MusicColors.Muted,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// 本地音乐分段中文名。
private fun LocalMusicSection.label(): String {
    return when (this) {
        LocalMusicSection.Songs -> "歌曲"
        LocalMusicSection.Albums -> "专辑"
        LocalMusicSection.Artists -> "歌手"
        LocalMusicSection.Sources -> "来源"
    }
}
```

Execution note: 为保持文件职责和长度，来源/扫描问题分段实际拆到 `LocalMusicSourceSection.kt`。

- [x] **Step 2: Route `SecondaryScreen.LocalMusic`**

In `MusicApp.kt`, import `LocalMusicScreen` and replace the old `SecondaryScreen.LocalFolder` branch:

```kotlin
                is SecondaryScreen.LocalMusic -> LocalMusicScreen(
                    songs = state.songs,
                    albums = state.albums,
                    artists = state.artists,
                    sources = state.localMusicSources,
                    problems = state.localMusicProblems,
                    initialSection = secondaryScreen.initialSection,
                    currentSongId = state.currentSongId,
                    onBack = controller::navigateBack,
                    onSongOpen = controller::openSong,
                    onSongPlay = controller::playSong,
                    onMore = controller::openMore,
                    onAlbumOpen = controller::openAlbum,
                    onArtistOpen = controller::openArtist,
                )
```

- [x] **Step 3: Replace LocalFolder references**

In `UtilityScreens.kt`, stop exposing `LocalFolderScreen` through navigation. Either leave the Composable unused for a later removal commit, or delete it if no imports depend on it. If deleted, also remove `FolderSummaryRow` if it becomes unused.

In settings callbacks, route the local source row to:

```kotlin
onLocalMusicSources: () -> Unit,
```

and display:

```kotlin
SettingsListRow("本地来源", "查看扫描来源和问题", onLocalMusicSources)
```

- [x] **Step 4: Run Android compile and tests**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`

Expected: PASS. The new local music page should be classified as `SecondaryWithMiniPlayer`.

Execution note: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest` PASS。

- [x] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/LocalMusicSourceSection.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/UtilityScreens.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "新增本地音乐二级页"
```

Execution note: commit `6db6e0f 新增本地音乐二级页`.

## Task 9: Search, Favorites, and Me Use the Same Snapshot

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/MeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [x] **Step 1: Add failing snapshot consumer tests**

Append to `MusicAppControllerTest`:

```kotlin
    /**
     * 搜索必须读取扫描后的曲库快照，而不是 seed/mock 仓库。
     */
    @Test
    fun searchReadsScannedSnapshot(): Unit = kotlinx.coroutines.runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest.Refresh)

        controller.setSearchQuery(query = "One Summer")
        controller.setSearchScope(scope = SearchScope.Songs)

        assertEquals(expected = listOf("One Summer's Day"), actual = controller.search().songs.map { song -> song.title })
    }

    /**
     * 我的页统计应来自同一份曲库快照。
     */
    @Test
    fun libraryStatsComeFromScannedSnapshot(): Unit = kotlinx.coroutines.runBlocking {
        val controller = MusicAppController()
        controller.scanLocalMusic(request = com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest.Refresh)

        assertEquals(expected = 8, actual = controller.uiState.libraryStats.songCount)
        assertEquals(expected = controller.uiState.albums.size, actual = controller.uiState.libraryStats.albumCount)
        assertEquals(expected = controller.uiState.artists.size, actual = controller.uiState.libraryStats.artistCount)
    }
```

- [x] **Step 2: Run tests to verify failures**

Run: `./gradlew :composeApp:desktopTest`

Expected: FAIL only if controller search/use case still points to a stale repository or Me state lacks stats.

- [x] **Step 3: Pass stats and favorite count to `MeScreen`**

Change `MeScreen` signature:

```kotlin
fun MeScreen(
    albums: List<Album>,
    artists: List<Artist>,
    libraryStats: LibraryStats,
    favoriteCount: Int,
    onSettings: () -> Unit,
    onLogin: () -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
)
```

Replace `MetricRow()` with:

```kotlin
MetricRow(
    libraryStats = libraryStats,
    favoriteCount = favoriteCount,
)
```

Replace `MetricRow`:

```kotlin
@Composable
private fun MetricRow(
    libraryStats: LibraryStats,
    favoriteCount: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            "本地专辑" to libraryStats.albumCount.toString(),
            "歌手" to libraryStats.artistCount.toString(),
            "收藏" to favoriteCount.toString(),
        ).forEach { item ->
            Surface(
                modifier = Modifier.weight(weight = 1f),
                shape = RoundedCornerShape(18.dp),
                color = MusicColors.AccentSoft,
            ) {
                Column(modifier = Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = item.second, modifier = Modifier.padding(horizontal = 8.dp), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = item.first, modifier = Modifier.padding(horizontal = 8.dp), color = MusicColors.Muted, fontSize = 13.sp)
                }
            }
        }
    }
}
```

- [x] **Step 4: Wire MeScreen from `MusicApp.kt`**

```kotlin
        RootTab.Me -> MeScreen(
            albums = state.albums,
            artists = state.artists,
            libraryStats = state.libraryStats,
            favoriteCount = state.likedSongIds.size,
            onSettings = { controller.navigateToSecondary(SecondaryScreen.Settings) },
            onLogin = { controller.navigateToSecondary(SecondaryScreen.Login) },
            onAlbumOpen = controller::openAlbum,
            onArtistOpen = controller::openArtist,
        )
```

- [x] **Step 5: Verify search use case uses injected repository**

If `MusicAppController` constructs `SearchMusicUseCaseImpl` before replacing the default repository, keep the existing construction but ensure it uses the same `musicLibraryRepository` field:

```kotlin
    private val searchMusicUseCase: SearchMusicUseCase = SearchMusicUseCaseImpl(
        musicLibraryRepository = musicLibraryRepository,
    )
```

- [x] **Step 6: Run tests and compile**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`

Expected: PASS. Search, favorites, profile stats, and homepage should all consume `MusicAppUiState.songs/albums/artists` derived from `LibrarySnapshot`.

- [x] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/MeScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "统一搜索收藏我的页曲库来源"
```

## Task 10: Final Verification and Documentation Notes

**Files:**
- Modify: `docs/superpowers/specs/2026-06-22-local-audio-home-display-design.md`
- Optional create: `handoff/YYYY-MM-DD-HHMMSS-local-audio-home-display-implementation.md`

- [x] **Step 1: Run full verification**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`

Expected: PASS.

- [x] **Step 2: Review mock/seed leakage**

Run: `rg "SeedMusicLibraryRepository|1,248|86 张专辑|128 位歌手|LocalFolder|ScanStatus" composeApp/src/commonMain composeApp/src/commonTest`

Expected:

- No `SeedMusicLibraryRepository()` in `MusicAppController` default constructor.
- No hardcoded homepage stats `1,248`, `86 张专辑`, or `128 位歌手`.
- No active route to `SecondaryScreen.LocalFolder`.
- No import or use of old `ScanStatus`.

- [x] **Step 3: Review platform API boundary**

Run: `rg "ContentResolver|MediaStore|UIDocumentPicker|AVAsset|Files\\.walk|JFileChooser" composeApp/src/commonMain`

Expected: no matches in `commonMain`.

- [x] **Step 4: Update the spec implementation status**

Append this section to `docs/superpowers/specs/2026-06-22-local-audio-home-display-design.md`:

```markdown
## 实施状态

- 第一阶段已覆盖：common 扫描模型、fake scanner、曲库快照合并、首页 `本地歌曲` 预览、二级 `本地音乐` 页、搜索/收藏/我的页共用快照、最近播放与扫描结果分离。
- 尚需独立计划推进：Android MediaStore scanner、iOS Files 导入、Desktop 文件夹 scanner、真实播放器读取 scanner 生成的 `localUri`。
- 当前 fake scanner 使用 `sourceKind = fakeScanner`，用于验证 UI 与数据流，不代表任何真实平台来源。
```

- [x] **Step 5: Commit**

```bash
git add docs/superpowers/specs/2026-06-22-local-audio-home-display-design.md
git commit -m "记录本地音频首页实现状态"
```

## 举一反三问题速查

本节用于验证：执行者遇到相邻实现问题时，能否从本计划直接找到处理方案。

| 问题 | 是否能找到方案 | 文档答案 | 依据 |
| --- | --- | --- | --- |
| 扫描出来的歌曲在哪里显示？ | 能 | 首页新增 `本地歌曲`，位于 `最近播放` 和 `本地专辑` 之间，最多 6 条；全量进入二级 `本地音乐` 页。 | Task 7、Task 8 |
| 首页 `最近播放` 是否还存在？ | 能 | 存在，且只来自真实播放历史，不被扫描结果自动填充。 | Task 5、Task 7 |
| 扫描完成但从未播放过，最近播放显示什么？ | 能 | 显示空态 `播放过的歌曲会出现在这里`。 | Task 7 |
| 首页本地歌曲为什么最多 6 条？ | 能 | `localSongPreview = songsWithLikes.take(n = 6)`，UI 只渲染预览，全量通过 `查看全部`。 | Task 6、Task 7 |
| `查看全部` 进入哪个页面？ | 能 | 进入 `SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Songs)`。 | Task 6、Task 8 |
| 二级 `本地音乐` 页有哪些分段？ | 能 | `歌曲 / 专辑 / 歌手 / 来源`。 | Task 6、Task 8 |
| 未扫描时首页会不会显示 mock 歌曲？ | 能 | 不会。默认仓库是空 `LibrarySnapshot`，本计划只通过 fake scanner 显式扫描填充数据。 | Task 4、Task 6、Task 10 |
| fake scanner 是否等于真实平台扫描？ | 能 | 不是。`FakeLocalMusicScanner` 仅用于 common 阶段验证 UI 与数据流，后续平台 scanner 独立计划实现。 | Scope Check、Task 4、Task 10 |
| seed/mock 数据是否还能冒充真实扫描结果？ | 能 | 不能。Task 10 用 `rg` 验证 `MusicAppController` 不再默认使用 `SeedMusicLibraryRepository()`。 | Task 10 |
| `localUri` 由谁生成？ | 能 | 由 scanner 生成；common 阶段 fake scanner 生成 `fake://local-audio/...`，平台阶段由 Android/iOS/Desktop scanner 生成。 | Task 1、Task 2、Task 4 |
| 稳定歌曲 id 如何保证？ | 能 | `sourceKey = sourceKind.value + ":" + sourceId`，`Song.id` 使用该 key。 | Task 1、Task 3 |
| 重新扫描后收藏如何保留？ | 能 | 合并时传入 `likedSongIds`，`toSong()` 用稳定 id 恢复 `isLiked`。 | Task 3、Task 6 |
| 缺少标题、歌手、专辑、时长怎么办？ | 能 | 标题回退文件名，歌手 `未知歌手`，专辑 `未知专辑`，时长 `--:--`。 | Task 3 |
| 不可读或不支持格式是否进入首页列表？ | 能 | 不进入 `songs`，只进入 `LocalMusicProblem` 和来源分段。 | Task 3、Task 8 |
| 部分失败是否阻塞成功歌曲展示？ | 能 | 不阻塞。成功歌曲进入快照，失败项进入 `problems`。 | Task 3、Task 8 |
| 搜索是否还搜 seed/mock？ | 能 | 不搜。搜索用例依赖同一个 `musicLibraryRepository`，扫描后读取快照。 | Task 9 |
| 收藏页是否显示全部扫描歌曲？ | 能 | 不显示。收藏页仍按 `isLiked` 过滤，扫描结果不会自动收藏。 | Task 5、Task 9 |
| 我的页统计从哪里来？ | 能 | 来自 `LibraryStats` 和 `likedSongIds.size`，不再硬编码。 | Task 9 |
| 队列为空时点击下一首会不会播放扫描列表第一首？ | 能 | 不会。`MoveQueueUseCase` 移除全曲库 fallback，空队列保持当前状态。 | Task 5 |
| 当前歌曲从曲库消失后，队列会不会静默替换成别的歌？ | 能 | 不会。当前歌曲不在队列时 `MoveQueueUseCase` 返回原状态。 | Task 5 |
| 空曲库是否会因为 `albums.first()` 或 `songs.first()` 崩溃？ | 能 | 不会。`currentSong`、`selectedAlbum`、`selectedArtist` 改为 nullable，并用 `MissingLibraryItemScreen` 兜底。 | Task 6、Task 7 |
| `commonMain` 是否可能误引平台 API？ | 能 | Task 10 用 `rg` 检查 `ContentResolver/MediaStore/UIDocumentPicker/AVAsset/Files.walk/JFileChooser` 不出现在 `commonMain`。 | Task 10 |
| Android MediaStore scanner 在本计划里实现吗？ | 能 | 不实现。本计划只做 common 数据链路和 UI，Android scanner 是独立后续计划。 | Scope Check |
| iOS Files 导入在本计划里实现吗？ | 能 | 不实现。iOS 导入涉及 Document Picker 和沙盒文件复制，需独立平台计划。 | Scope Check |
| Desktop 文件夹递归扫描在本计划里实现吗？ | 能 | 不实现。Desktop 文件选择和递归扫描需独立平台计划。 | Scope Check |
| 真实播放 `localUri` 在本计划里接入吗？ | 能 | 不接入真实播放器。本计划只保证 `Song.localUri` 随 scanner 数据进入模型。 | Scope Check、Task 2 |

## 三轮交叉 Review 结果

### 第一轮：产品与 UI/UX Review

结论：

- 首页信息架构与已确认方案一致：`本地音乐库卡片 -> 最近播放 -> 本地歌曲 -> 本地专辑`。
- `最近播放` 和 `本地歌曲` 的语义隔离已落实到测试：扫描只填充本地歌曲，不填充最近播放。
- 二级 `本地音乐` 页承载全量浏览，一级页只展示 6 条预览，符合当前底部 Tab 信息架构。
- 未扫描、无播放历史、空曲库、缺失详情条目都有兜底路径，不依赖 seed 数据维持页面不崩溃。

本轮发现并已补强：

- 原计划缺少“空曲库下 detail/player 入口不崩溃”的明确处理，已补 `currentSong/selectedAlbum/selectedArtist` nullable 和 `MissingLibraryItemScreen`。
- 原计划提到没有 mini-player 时底部 Tab 要存在，但缺少实现步骤，已补 `BottomChrome(song: Song?)` 的渲染策略。

剩余实现注意：

- 首页新增 6 条歌曲后，需要在真机或桌面预览检查与全局 mini-player、底部 Tab 的遮挡关系。
- `本地歌曲` 空态首版选择“不显示区块”，如果后续产品希望显示空态卡片，应另开 UI 微调任务。

### 第二轮：数据来源与架构 Review

结论：

- 计划先定义 `LocalMusicScanner`、`LocalMusicScanResult`、`MusicFileMetadata`、`LibrarySnapshot`，再让 UI 读取快照，符合 common/platform 边界。
- `sourceKey` 规则、`localUri` 来源、metadata fallback、失败项过滤都在 domain/usecase 层集中处理，Composable 不拼接平台路径。
- fake scanner 作为显式开发数据源存在，不复用 `SeedMusicLibraryRepository` 冒充真实扫描。
- 搜索、收藏、我的页统计、首页 preview 都从同一个 `MusicAppUiState.songs/albums/artists` 派生，避免多套列表漂移。

本轮发现并已补强：

- 原计划对播放队列缺失项的约束不够硬，可能沿用旧 `fallbackSongs = uiState.songs` 造成“下一首”自动播放扫描列表第一首。已补 Task 5：移除全曲库 fallback，空队列或当前歌曲缺失时保持原状态。

剩余实现注意：

- `InMemoryMusicLibraryRepository` 第一阶段不落盘，重启后丢失扫描快照是预期；平台 scanner 和持久化缓存应在后续计划中处理。
- `sourceKey` P0 不做内容指纹去重，未来如做去重，不能破坏本计划稳定 id 规则。

### 第三轮：实现、测试与风险 Review

结论：

- 任务按 TDD 顺序拆分：模型测试、合并测试、controller 行为测试、UI 编译验证、最终 `rg` 边界检查。
- 每个阶段都有可执行命令，最终验证覆盖 `:composeApp:compileDebugKotlinAndroid` 和 `:composeApp:desktopTest`。
- 计划中把 Android/iOS/Desktop 真实 scanner 和真实播放集成拆为后续计划，避免平台权限和 UI 改造混在一个不可审查的大改里。

本轮发现并已补强：

- 原计划没有把“队列为空不能隐式使用扫描歌曲”写成测试，已补 `moveTrackDoesNotUseScannedSongsAsImplicitQueue()`。
- 原计划中 nullable 播放状态会影响多个页面签名，已补 Search/Favorites/Detail/Home 的 `currentSongId: String?` 变更清单。

剩余执行风险：

- `MusicApp.kt` 的 `rememberCoroutineScope` 扫描回调要避免在 Composable body 直接调用 suspend 方法。
- 删除或停用 `LocalFolderScreen` 时要同步清理 import 和路由分支，不要留下死路由。
- 文档是计划，不代表实现已完成；执行后再按 Task 10 更新设计规格的“实施状态”。

## Self-Review

**Spec coverage:** This plan implements the confirmed homepage layout, true recent playback separation, max-6 local songs preview, full secondary local music list, shared `LibrarySnapshot`, stable `sourceKey`, metadata fallbacks, failed-entry filtering, source summaries, search/favorites/profile snapshot consumption, queue-no-silent-replacement behavior, empty-library safety, and common/platform API boundary. Real Android/iOS/Desktop scanner implementations and real playback of `localUri` are intentionally split into follow-up platform plans because each has separate permissions, file access, and verification constraints.

**Placeholder scan:** The plan avoids undefined placeholders and gives concrete models, tests, implementation snippets, commands, expected results, review questions, and commit messages for every task.

**Type consistency:** The plan consistently uses `LocalMusicSourceKind`, `MusicFileMetadata`, `LocalMusicScanResult`, `LibrarySnapshot`, `LocalMusicScanState`, `LocalMusicSection`, `SecondaryScreen.LocalMusic`, `ScanLocalMusicUseCase`, and `MergeLocalMusicScanResultUseCase` across tests, models, repository, controller, and UI.
