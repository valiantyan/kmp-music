# Coil Image Loading Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Route every Compose UI cover image and cover-driven palette through Coil while preserving `coverImageUri` first and `CoverArt` fallback behavior.

**Architecture:** Keep image source decisions inside `feature/components`, not in pages. Add a small testable request model, a `CoverArtImage` composable backed by Coil, and a palette loader that uses the same Coil source order before feeding the existing palette extraction algorithms.

**Tech Stack:** Kotlin Multiplatform 2.0.21, Compose Multiplatform 1.7.3, Coil 3.5.0, kotlin-test, Android/JVM/iOS source sets.

---

## Scope Check

The spec covers one subsystem: Compose UI image loading and cover-driven palette extraction. It intentionally excludes Media3 notification artwork, scanning, playback, persistence, and domain model redesign, so this remains one implementation plan.

## Audit Addendum

### Implementation-Stage Q&A

- Question: Does fallback cover rendering also go through Coil, or is a Compose `painterResource` fallback acceptable?
  Answer: Fallback cover rendering must go through Coil. `CoverArtImage` first attempts `coverImageUri`; on failure it switches the same `AsyncImage` to `Res.getUri(fallbackResourcePath)`. A non-Coil `painterResource` fallback is not acceptable for final image rendering.
- Question: Do we need a Coil network module for this work?
  Answer: No. This plan covers local `file://` style scan outputs and Compose resource URIs. Network images are a non-goal and must not add OkHttp/Ktor network modules.
- Question: Can a page import Coil APIs directly for image display or palette extraction?
  Answer: No. Pages must not import `AsyncImage`, `ImageRequest`, `ImageLoader`, `SingletonImageLoader`, or `LocalPlatformContext`. Static illustrations use `CoverArtImage`; palette callers use `rememberMiniPlayerPalette` or `rememberPlayerPagePalette` without passing Coil context objects.
- Question: Should Media3 notification artwork use the new `CoverArtImage` or `ImageLoader` path?
  Answer: No. Media3 notification and lock-screen artwork remain metadata byte delivery to system UI. The final validation step checks that playback metadata files were not changed.
- Question: Is it acceptable for palette extraction to use a different source than image display if the colors look better?
  Answer: No. Palette extraction must use the same `buildCoverArtImageRequest` source order as `CoverArtImage`: external cover first, fallback resource second, default palette only after both fail.
- Question: What if the external cover URI fails after a song change and the previous song's fallback state is still remembered?
  Answer: `CoverArtImage` must key its active model by `coverArt` and `coverImageUri`; palette state must also be keyed by the same pair, so song changes reset image and palette loading.
- Question: Can implementation modify `Song`, scanner outputs, playback queues, or database schemas to make image loading easier?
  Answer: No. Those are outside the approved scope. The implementation uses the existing `coverImageUri + CoverArt` model and adds only UI/components request helpers.

### Boundary Coverage Matrix

- UI boundary: Tasks 2-4 replace all UI cover display callers with `CoverArtImage`; Task 7 scans for old painter APIs and direct page-level resource loading.
- Domain boundary: Task 1 creates the request model in `feature/components`, not `domain`; Task 7 checks implementation files do not include domain model edits beyond imports already present.
- Data boundary: No task modifies `composeApp/src/*/data`; Task 7 checks no data scanner or artwork extractor files changed.
- Playback boundary: No task modifies playback engines, queues, controller state, or Media3 metadata files; Task 7 checks no playback files changed.
- Notification boundary: The plan does not touch `AndroidPlaybackMediaMetadataAssets`; Task 7 verifies Media3 metadata artwork files are unchanged.
- Resource boundary: Task 1 maps only `composeResources/drawable` paths; Task 7 checks no prototype paths or platform-private drawable resources were introduced.
- Failure boundary: Task 2 switches failed external loads to Coil fallback resource; Task 5 falls back to default palette after primary and fallback palette loading fail.
- Test boundary: Task 1 covers source selection and enum mapping; Tasks 6-7 run `desktopTest` and Android compile; Task 7 requires visual verification status in the delivery message.

### Network Image Follow-Up Amendment

The original implementation scope treated network images as a non-goal so Tasks 1-7 could first unify local `file://` covers and Compose resource fallbacks. Real product usage still needs remote album/artist artwork, so Task 8 intentionally expands the scope by adding Coil network support with Ktor 3 while preserving the same component boundary:

- UI pages still do not import Coil APIs directly.
- `CoverArtImage` and `CoverPaletteLoader` continue to receive only `coverImageUri + CoverArt`.
- Network image support is enabled through Gradle dependencies, not page-level special cases.
- The temporary network validation fixture was used for manual macOS verification, then removed so no test URL remains in scanner data, UI, domain, or persistence defaults.

### Three-Pass Cross Review

Pass 1, requirements closure and root cause:

- Question: Does the plan remove the root cause where display and palette used different sources?
  Conclusion: Yes. Tasks 5-6 route palette extraction through `buildCoverArtImageRequest`, matching `CoverArtImage`.
- Question: Does every Compose UI image path use Coil, including fallback resources?
  Conclusion: Required after this audit. Task 2 now switches `AsyncImage` to fallback resource URI instead of rendering `painterResource` as the final error image.
- Document location changed: Task 2 `CoverArtImage` implementation and this audit addendum.

Pass 2, architecture boundaries and dependency leakage:

- Question: Does Coil leak into pages?
  Conclusion: No after this audit. Pages call `CoverArtImage` and palette helpers; Coil imports remain inside `feature/components`.
- Question: Does the plan expand into domain/data/playback to solve UI loading?
  Conclusion: No. The boundary matrix and Task 7 file-scope checks make that explicit.
- Document location changed: Boundary Coverage Matrix and Task 7 validation.

Pass 3, failure scenarios and executable validation:

- Question: Can external URI failure be validated mechanically?
  Conclusion: Runtime failure is hard to unit-test without a Compose/UI harness, but Task 2 implements deterministic fallback switching and Task 7 requires visual or risk reporting. Source-order tests cover the pure rule.
- Question: Can accidental Media3 notification changes be caught?
  Conclusion: Yes. Task 7 adds file-scope checks for playback metadata boundaries.
- Document location changed: Task 7 final validation.

## File Structure

- Modify `gradle/libs.versions.toml`: add Coil version and library aliases.
- Modify `composeApp/build.gradle.kts`: add Coil dependencies to `commonMain`.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtImageRequest.kt`: pure request model, resource path mapping, and source selection.
- Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtImageRequestTest.kt`: source selection and enum coverage tests.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.kt`: replace painter-centered API with Coil image and palette-facing APIs.
- Delete `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.android.kt`: remove Android hand decoding for UI covers.
- Delete `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.desktop.kt`: remove Desktop hand decoding for UI covers.
- Delete `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.ios.kt`: remove iOS fallback-only actual.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverPaletteLoader.kt`: Coil `ImageLoader` palette-loading composables.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.kt`: expect bridge from `coil3.Image` to Compose `ImageBitmap`.
- Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.android.kt`: Android bitmap conversion.
- Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.desktop.kt`: Desktop bitmap conversion.
- Create `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.ios.kt`: iOS bitmap conversion.
- Modify mobile UI files that currently call `Image(painter = coverArtPainter(...))`: replace with `CoverArtImage(...)`.
- Modify desktop UI files that currently call `Image(painter = coverArtPainter(...))`: replace with `CoverArtImage(...)`.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`: mini player image and palette use Coil source.
- Modify `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopPlayerDetailScreen.kt`: desktop player page palette uses Coil source.
- Modify `gradle/libs.versions.toml`: add Coil Ktor 3 network module and Ktor engines.
- Modify `composeApp/build.gradle.kts`: add Coil network support to `commonMain` and Ktor engines to platform source sets.

## Task 1: Add Coil Dependencies and Testable Request Model

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtImageRequest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtImageRequestTest.kt`

- [x] **Step 1: Add the failing request model test**

Create `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtImageRequestTest.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.components

import com.yanhao.kmpmusic.domain.model.CoverArt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoverArtImageRequestTest {
    @Test
    fun buildCoverArtImageRequestUsesCoverImageUriBeforeFallbackResource(): Unit {
        val request: CoverArtImageRequest = buildCoverArtImageRequest(
            coverArt = CoverArt.AlbumBestOfMe,
            coverImageUri = "file:///tmp/cover.art",
        )

        assertEquals("file:///tmp/cover.art", request.primaryModel)
        assertEquals("drawable/album_best_of_me.png", request.fallbackResourcePath)
        assertTrue(request.usesExternalCover)
    }

    @Test
    fun buildCoverArtImageRequestUsesFallbackResourceWhenCoverImageUriIsBlank(): Unit {
        val request: CoverArtImageRequest = buildCoverArtImageRequest(
            coverArt = CoverArt.CoverSeaDream,
            coverImageUri = "   ",
        )

        assertEquals("drawable/cover_sea_dream.png", request.primaryModel)
        assertEquals("drawable/cover_sea_dream.png", request.fallbackResourcePath)
        assertFalse(request.usesExternalCover)
    }

    @Test
    fun coverArtResourcePathMapsEveryCoverArtValue(): Unit {
        val paths: Set<String> = CoverArt.entries.map(::coverArtResourcePath).toSet()

        assertEquals(CoverArt.entries.size, paths.size)
        assertEquals("drawable/album_best_of_me.png", coverArtResourcePath(CoverArt.AlbumBestOfMe))
        assertEquals("drawable/album_river_year.png", coverArtResourcePath(CoverArt.AlbumRiverYear))
        assertEquals("drawable/album_time_forest.png", coverArtResourcePath(CoverArt.AlbumTimeForest))
        assertEquals("drawable/cover_sea_dream.png", coverArtResourcePath(CoverArt.CoverSeaDream))
        assertEquals("drawable/cover_summer_waltz.png", coverArtResourcePath(CoverArt.CoverSummerWaltz))
        assertEquals("drawable/hero_local_folder.png", coverArtResourcePath(CoverArt.HeroLocalMusic))
    }
}
```

- [x] **Step 2: Run the new test and verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.components.CoverArtImageRequestTest
```

Expected: FAIL with unresolved references for `CoverArtImageRequest`, `buildCoverArtImageRequest`, and `coverArtResourcePath`.

- [x] **Step 3: Add Coil aliases to the version catalog**

Modify `gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.13.2"
coil = "3.5.0"
composeMultiplatform = "1.7.3"
kotlin = "2.0.21"
kotlinxCoroutines = "1.9.0"
kotlinxSerialization = "1.8.1"
androidxCore = "1.17.0"
androidxAppCompat = "1.7.1"
ksp = "2.0.21-1.0.28"
media3 = "1.10.1"
room3 = "3.0.0-rc01"
sqlite = "2.6.2"
vlcj = "4.12.1"

[libraries]
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-compose-core = { module = "io.coil-kt.coil3:coil-compose-core", version.ref = "coil" }
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-swing = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-swing", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
kotlinx-serialization-core = { module = "org.jetbrains.kotlinx:kotlinx-serialization-core", version.ref = "kotlinxSerialization" }
androidx-core = { module = "androidx.core:core", version.ref = "androidxCore" }
androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "androidxAppCompat" }
androidx-sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
androidx-room3-runtime = { module = "androidx.room3:room3-runtime", version.ref = "room3" }
androidx-room3-compiler = { module = "androidx.room3:room3-compiler", version.ref = "room3" }
androidx-media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "media3" }
androidx-media3-session = { module = "androidx.media3:media3-session", version.ref = "media3" }
androidx-media3-ui = { module = "androidx.media3:media3-ui", version.ref = "media3" }
vlcj = { module = "uk.co.caprica:vlcj", version.ref = "vlcj" }
```

- [x] **Step 4: Add Coil dependencies to commonMain**

Modify the `commonMain.dependencies` block in `composeApp/build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation(libs.coil.compose)
    implementation(libs.coil.compose.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
}
```

- [x] **Step 5: Add the request model**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtImageRequest.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.components

import com.yanhao.kmpmusic.domain.model.CoverArt

/**
 * Coil 封面加载请求的纯数据部分，便于测试来源优先级。
 */
internal data class CoverArtImageRequest(
    val primaryModel: String,
    val fallbackResourcePath: String,
    val usesExternalCover: Boolean,
)

/**
 * 构建封面来源：扫描封面优先，应用内资源兜底。
 */
internal fun buildCoverArtImageRequest(
    coverArt: CoverArt,
    coverImageUri: String?,
): CoverArtImageRequest {
    val fallbackResourcePath: String = coverArtResourcePath(coverArt = coverArt)
    val normalizedCoverImageUri: String? = coverImageUri?.trim()?.takeIf { uri: String -> uri.isNotEmpty() }
    return CoverArtImageRequest(
        primaryModel = normalizedCoverImageUri ?: fallbackResourcePath,
        fallbackResourcePath = fallbackResourcePath,
        usesExternalCover = normalizedCoverImageUri != null,
    )
}

/**
 * Compose Multiplatform resources 中的封面路径，供 Res.getUri 加载。
 */
internal fun coverArtResourcePath(coverArt: CoverArt): String {
    return when (coverArt) {
        CoverArt.AlbumBestOfMe -> "drawable/album_best_of_me.png"
        CoverArt.AlbumRiverYear -> "drawable/album_river_year.png"
        CoverArt.AlbumTimeForest -> "drawable/album_time_forest.png"
        CoverArt.CoverSeaDream -> "drawable/cover_sea_dream.png"
        CoverArt.CoverSummerWaltz -> "drawable/cover_summer_waltz.png"
        CoverArt.HeroLocalMusic -> "drawable/hero_local_folder.png"
    }
}
```

- [x] **Step 6: Run the request model test**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.components.CoverArtImageRequestTest
```

Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtImageRequest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtImageRequestTest.kt
git commit -m "接入 Coil 封面请求模型"
```

## Task 2: Add Shared Coil CoverArtImage Component

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.kt`
- Test: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtImageRequestTest.kt`

- [x] **Step 1: Replace the painter file with a Coil-backed image component**

Replace `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.kt` with:

```kotlin
package com.yanhao.kmpmusic.feature.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.yanhao.kmpmusic.domain.model.CoverArt
import kmpmusic.composeapp.generated.resources.Res

/**
 * 将 domain 层封面标识映射到 Compose resource URI。
 */
@Composable
internal fun coverArtResourceUri(coverArt: CoverArt): String {
    return Res.getUri(coverArtResourcePath(coverArt = coverArt))
}

/**
 * Coil 封面图组件。扫描封面优先，加载失败时回退到应用内资源。
 */
@Composable
fun CoverArtImage(
    coverArt: CoverArt,
    coverImageUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val request: CoverArtImageRequest = remember(coverArt, coverImageUri) {
        buildCoverArtImageRequest(
            coverArt = coverArt,
            coverImageUri = coverImageUri,
        )
    }
    val fallbackModel: String = Res.getUri(request.fallbackResourcePath)
    val primaryModel: String = if (request.usesExternalCover) {
        request.primaryModel
    } else {
        fallbackModel
    }
    var activeModel: String by remember(coverArt, coverImageUri) {
        mutableStateOf(primaryModel)
    }
    LaunchedEffect(primaryModel) {
        activeModel = primaryModel
    }
    AsyncImage(
        model = activeModel,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onError = {
            if (activeModel != fallbackModel) {
                activeModel = fallbackModel
            }
        },
    )
}

/**
 * 只使用应用内兜底封面时的便捷重载。
 */
@Composable
fun CoverArtImage(
    coverArt: CoverArt,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    CoverArtImage(
        coverArt = coverArt,
        coverImageUri = null,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
```

- [x] **Step 2: Verify `CoverArtPainter.kt` no longer exposes painter helpers**

Run:

```bash
rg "painterResource|DrawableResource|coverArtResource\\(|fallbackCoverArtPainter|coverArtPainter\\(" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.kt
```

Expected: no matches. Fallback rendering is handled by switching `AsyncImage` from the external model to `Res.getUri(request.fallbackResourcePath)`, so the final fallback image is still loaded by Coil.

- [x] **Step 3: Compile common sources and verify old painter callers now fail**

Run:

```bash
./gradlew :composeApp:compileKotlinDesktop
```

Expected: FAIL with unresolved reference errors for `coverArtPainter` in mobile and desktop UI files. This confirms the old UI path has been cut.

- [x] **Step 4: Stop before committing**

Stop after the compile failure from Step 3. The repository will compile again after Tasks 3 and 4 migrate all callers, then those changes are committed together.

## Task 3: Migrate Mobile Shared UI Callers to CoverArtImage

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/PlayerScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/MeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/DetailScreens.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CommonComponents.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`

- [x] **Step 1: Replace PlayerScreen cover**

In `PlayerScreen.kt`, replace the `Image` import with no image import if unused, replace the `coverArtPainter` import with:

```kotlin
import com.yanhao.kmpmusic.feature.components.CoverArtImage
```

Replace the cover block with:

```kotlin
CoverArtImage(
    coverArt = song.coverArt,
    coverImageUri = song.coverImageUri,
    contentDescription = "${song.title} 封面",
    modifier = Modifier.size(328.dp).clip(RoundedCornerShape(30.dp)).align(Alignment.CenterHorizontally),
    contentScale = ContentScale.Crop,
)
```

- [x] **Step 2: Replace CommonComponents covers**

In `CommonComponents.kt`, replace:

```kotlin
import com.yanhao.kmpmusic.feature.components.coverArtPainter
```

with:

```kotlin
import com.yanhao.kmpmusic.feature.components.CoverArtImage
```

Replace the `SongRow` cover `Image(...)` with:

```kotlin
CoverArtImage(
    coverArt = song.coverArt,
    coverImageUri = song.coverImageUri,
    contentDescription = "${song.title} 封面",
    modifier = Modifier
        .size(coverSize)
        .then(
            if (coverShadowElevation > 0.dp) {
                Modifier.shadow(
                    elevation = coverShadowElevation,
                    shape = coverShape,
                    clip = false,
                )
            } else {
                Modifier
            },
        )
        .clip(coverShape),
    contentScale = ContentScale.Crop,
)
```

Replace the `AlbumCard` cover with:

```kotlin
CoverArtImage(
    coverArt = album.coverArt,
    coverImageUri = album.coverImageUri,
    contentDescription = "${album.title} 专辑封面",
    modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(ratio = 1f)
        .shadow(elevation = scaledDp(12.dp), shape = RoundedCornerShape(scaledDp(MusicDimens.AlbumRadius)), clip = false)
        .clip(RoundedCornerShape(scaledDp(MusicDimens.AlbumRadius))),
    contentScale = ContentScale.Crop,
)
```

Replace the `ArtistRow` image with:

```kotlin
CoverArtImage(
    coverArt = artist.coverArt,
    coverImageUri = artist.coverImageUri,
    contentDescription = "${artist.name} 图片",
    modifier = Modifier.size(scaledDp(58.dp)).clip(CircleShape),
    contentScale = ContentScale.Crop,
)
```

- [x] **Step 3: Replace HomeScreen and MeScreen static covers**

In `HomeScreen.kt`, replace the `coverArtPainter` import with:

```kotlin
import com.yanhao.kmpmusic.feature.components.CoverArtImage
```

Replace the hero local-folder image with:

```kotlin
CoverArtImage(
    coverArt = CoverArt.HeroLocalMusic,
    contentDescription = "本地音乐库文件夹插画",
    modifier = Modifier
        .size(scaledDp(MusicDimens.HeroFolderSize))
        .clip(RoundedCornerShape(scaledDp(28.dp))),
    contentScale = ContentScale.Crop,
)
```

In `MeScreen.kt`, replace the `coverArtPainter` import with:

```kotlin
import com.yanhao.kmpmusic.feature.components.CoverArtImage
```

Replace the account avatar image with:

```kotlin
CoverArtImage(
    coverArt = CoverArt.AlbumTimeForest,
    contentDescription = "账号头像视觉",
    modifier = Modifier.size(70.dp).clip(CircleShape),
    contentScale = ContentScale.Crop,
)
```

Replace the album preview image with:

```kotlin
CoverArtImage(
    coverArt = album.coverArt,
    coverImageUri = album.coverImageUri,
    contentDescription = "${album.title} 封面",
    modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(ratio = 1f)
        .clip(RoundedCornerShape(11.dp)),
    contentScale = ContentScale.Crop,
)
```

- [x] **Step 4: Replace DetailScreens one-line cover lambdas**

In `DetailScreens.kt`, replace the `coverArtPainter` import with:

```kotlin
import com.yanhao.kmpmusic.feature.components.CoverArtImage
```

Replace the album detail cover lambda with:

```kotlin
cover = {
    CoverArtImage(
        coverArt = album.coverArt,
        coverImageUri = album.coverImageUri,
        contentDescription = "${album.title} 专辑封面",
        modifier = Modifier.size(126.dp).clip(RoundedCornerShape(18.dp)),
        contentScale = ContentScale.Crop,
    )
}
```

Replace the artist detail cover lambda with:

```kotlin
cover = {
    CoverArtImage(
        coverArt = artist.coverArt,
        coverImageUri = artist.coverImageUri,
        contentDescription = "${artist.name} 图片",
        modifier = Modifier.size(126.dp).clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}
```

- [x] **Step 5: Replace MiniPlayer image but leave palette for Task 5**

In `MusicApp.kt`, replace the mini player `androidx.compose.foundation.Image(bitmap = coverImage, ...)` call with:

```kotlin
CoverArtImage(
    coverArt = song.coverArt,
    coverImageUri = song.coverImageUri,
    contentDescription = "${song.title} 封面",
    modifier = Modifier.size(scaledDp(45.dp)).clip(RoundedCornerShape(scaledDp(8.dp))),
    contentScale = ContentScale.Crop,
)
```

Add:

```kotlin
import com.yanhao.kmpmusic.feature.components.CoverArtImage
```

Do not remove `coverImage` and `extractMiniPlayerPalette` yet; Task 5 replaces palette logic after the palette loader exists.

- [x] **Step 6: Verify remaining mobile old calls**

Run:

```bash
rg "coverArtPainter\\(" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app
```

Expected: no matches in `feature/screen`, `feature/components/CommonComponents.kt`, or `feature/app/MusicApp.kt`. Matches in desktop files are handled in Task 4.

## Task 4: Migrate Desktop Callers and Remove Platform Painter Actuals

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicPlayer.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopLibrarySidebar.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopPlayerDetailScreen.kt`
- Delete: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.android.kt`
- Delete: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.desktop.kt`
- Delete: `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.ios.kt`

- [x] **Step 1: Replace desktop imports**

In each desktop file listed above, replace:

```kotlin
import com.yanhao.kmpmusic.feature.components.coverArtPainter
```

with:

```kotlin
import com.yanhao.kmpmusic.feature.components.CoverArtImage
```

- [x] **Step 2: Replace desktop song cover calls**

In `DesktopMusicComponents.kt`, replace the table row song cover with:

```kotlin
CoverArtImage(
    coverArt = song.coverArt,
    coverImageUri = song.coverImageUri,
    contentDescription = "${song.title} 封面",
    modifier = Modifier
        .size(DesktopMusicDimens.TableCoverSize)
        .clip(RoundedCornerShape(7.dp)),
    contentScale = ContentScale.Crop,
)
```

In `DesktopPlayerDetailScreen.kt`, replace the large player cover with:

```kotlin
CoverArtImage(
    coverArt = song.coverArt,
    coverImageUri = song.coverImageUri,
    contentDescription = "${song.title} 封面",
    modifier = Modifier
        .size(coverSize)
        .clip(RoundedCornerShape(34.dp)),
    contentScale = ContentScale.Crop,
)
```

For remaining song-cover matches in `DesktopMusicPlayer.kt`, `DesktopLibrarySidebar.kt`, and `DesktopMusicComponents.kt`, replace only the image source expression. Keep the exact `modifier`, `contentDescription`, and `contentScale` from each replaced `Image` call. The resulting call must have this shape:

```kotlin
CoverArtImage(
    coverArt = song.coverArt,
    coverImageUri = song.coverImageUri,
    contentDescription = "${song.title} 封面",
    modifier = Modifier
        .size(DesktopMusicDimens.PlayerCoverSize)
        .clip(RoundedCornerShape(12.dp)),
    contentScale = ContentScale.Crop,
)
```

If a file uses a different size token than `DesktopMusicDimens.PlayerCoverSize`, keep that file's existing size token in the `modifier`.

- [x] **Step 3: Replace album and artist cover calls**

For desktop album covers:

```kotlin
CoverArtImage(
    coverArt = album.coverArt,
    coverImageUri = album.coverImageUri,
    contentDescription = "${album.title} 封面",
    modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(12.dp)),
    contentScale = ContentScale.Crop,
)
```

For desktop artist covers:

```kotlin
CoverArtImage(
    coverArt = artist.coverArt,
    coverImageUri = artist.coverImageUri,
    contentDescription = "${artist.name} 图片",
    modifier = Modifier
        .size(DesktopMusicDimens.ArtistAvatarSize)
        .clip(CircleShape),
    contentScale = ContentScale.Crop,
)
```

If `DesktopMusicDimens.ArtistAvatarSize` is not the token used by the replaced call, keep the existing avatar size from that call while preserving the `CoverArtImage` parameters above.

For static profile art in `DesktopProfilePanel`:

```kotlin
CoverArtImage(
    coverArt = coverArt,
    contentDescription = "账号头像视觉",
    modifier = Modifier
        .size(88.dp)
        .clip(CircleShape),
    contentScale = ContentScale.Crop,
)
```

- [x] **Step 4: Delete platform painter actual files**

Run:

```bash
git rm composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.android.kt
git rm composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.desktop.kt
git rm composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.ios.kt
```

- [x] **Step 5: Verify old painter API is gone**

Run:

```bash
rg "coverArtPainter|rememberPlatformCoverArtPainter|decodeAndroidCoverImage|decodeDesktopCoverImage" composeApp/src
```

Expected: no matches.

- [x] **Step 6: Compile desktop**

Run:

```bash
./gradlew :composeApp:compileKotlinDesktop
```

Expected: PASS.

- [x] **Step 7: Commit Tasks 2 through 4**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverArtPainter.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/PlayerScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/MeScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/DetailScreens.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CommonComponents.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicPlayer.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopLibrarySidebar.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopPlayerDetailScreen.kt
git add -u composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/feature/components composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/feature/components composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/feature/components
git commit -m "使用 Coil 统一封面显示"
```

## Task 5: Add Coil-Backed Palette Loader

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.kt`
- Create: `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.android.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.desktop.kt`
- Create: `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.ios.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverPaletteLoader.kt`

- [x] **Step 1: Add common expect bridge**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.components

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image

/**
 * 将 Coil 跨平台 Image 转为 Compose ImageBitmap，供现有取色算法读取像素。
 */
internal expect fun coilImageToImageBitmap(image: Image): ImageBitmap?
```

- [x] **Step 2: Add Android actual bridge**

Create `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.android.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Image
import coil3.toBitmap

internal actual fun coilImageToImageBitmap(image: Image): ImageBitmap? {
    return runCatching {
        image.toBitmap().asImageBitmap()
    }.getOrNull()
}
```

- [x] **Step 3: Add Desktop actual bridge**

Create `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.desktop.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import coil3.Image
import coil3.toBitmap
import org.jetbrains.skia.Image as SkiaImage

internal actual fun coilImageToImageBitmap(image: Image): ImageBitmap? {
    return runCatching {
        SkiaImage.makeFromBitmap(image.toBitmap()).toComposeImageBitmap()
    }.getOrNull()
}
```

- [x] **Step 4: Add iOS actual bridge**

Create `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.ios.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import coil3.Image
import coil3.toBitmap
import org.jetbrains.skia.Image as SkiaImage

internal actual fun coilImageToImageBitmap(image: Image): ImageBitmap? {
    return runCatching {
        SkiaImage.makeFromBitmap(image.toBitmap()).toComposeImageBitmap()
    }.getOrNull()
}
```

- [x] **Step 5: Add the palette loader**

Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverPaletteLoader.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.yanhao.kmpmusic.core.theme.MiniPlayerPalette
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.PlayerPagePalette
import com.yanhao.kmpmusic.core.theme.extractMiniPlayerPalette
import com.yanhao.kmpmusic.core.theme.extractPlayerPagePalette
import com.yanhao.kmpmusic.domain.model.CoverArt
import kmpmusic.composeapp.generated.resources.Res

/**
 * 迷你播放器 palette 默认值，图片加载或取色失败时使用。
 */
fun defaultMiniPlayerPalette(): MiniPlayerPalette {
    return MiniPlayerPalette(
        containerColor = MusicColors.Paper.copy(alpha = 0.92f),
    )
}

/**
 * 桌面播放页 palette 默认值，图片加载或取色失败时使用。
 */
fun defaultPlayerPagePalette(): PlayerPagePalette {
    return PlayerPagePalette(
        backgroundColor = MusicColors.Paper,
        ambientColor = MusicColors.Accent.copy(alpha = 0.18f),
    )
}

/**
 * 使用与封面显示相同的 Coil 来源顺序提取迷你播放器配色。
 */
@Composable
fun rememberMiniPlayerPalette(
    coverArt: CoverArt,
    coverImageUri: String?,
): MiniPlayerPalette {
    return rememberCoverPalette(
        coverArt = coverArt,
        coverImageUri = coverImageUri,
        defaultPalette = defaultMiniPlayerPalette(),
        extractPalette = ::extractMiniPlayerPalette,
    )
}

/**
 * 使用与封面显示相同的 Coil 来源顺序提取桌面播放页配色。
 */
@Composable
fun rememberPlayerPagePalette(
    coverArt: CoverArt,
    coverImageUri: String?,
): PlayerPagePalette {
    return rememberCoverPalette(
        coverArt = coverArt,
        coverImageUri = coverImageUri,
        defaultPalette = defaultPlayerPagePalette(),
        extractPalette = ::extractPlayerPagePalette,
    )
}

@Composable
private fun <T> rememberCoverPalette(
    coverArt: CoverArt,
    coverImageUri: String?,
    defaultPalette: T,
    extractPalette: (ImageBitmap) -> T,
): T {
    val platformContext: PlatformContext = LocalPlatformContext.current
    val request: CoverArtImageRequest = remember(coverArt, coverImageUri) {
        buildCoverArtImageRequest(
            coverArt = coverArt,
            coverImageUri = coverImageUri,
        )
    }
    val fallbackModel: String = Res.getUri(request.fallbackResourcePath)
    val primaryModel: String = if (request.usesExternalCover) request.primaryModel else fallbackModel
    var palette: T by remember(coverArt, coverImageUri) {
        mutableStateOf(defaultPalette)
    }
    LaunchedEffect(primaryModel, fallbackModel, platformContext) {
        palette = loadCoverPalette(
            primaryModel = primaryModel,
            fallbackModel = fallbackModel,
            platformContext = platformContext,
            defaultPalette = defaultPalette,
            extractPalette = extractPalette,
        )
    }
    return palette
}

private suspend fun <T> loadCoverPalette(
    primaryModel: String,
    fallbackModel: String,
    platformContext: PlatformContext,
    defaultPalette: T,
    extractPalette: (ImageBitmap) -> T,
): T {
    return loadPaletteFromModel(
        model = primaryModel,
        platformContext = platformContext,
        extractPalette = extractPalette,
    ) ?: loadPaletteFromModel(
        model = fallbackModel,
        platformContext = platformContext,
        extractPalette = extractPalette,
    ) ?: defaultPalette
}

private suspend fun <T> loadPaletteFromModel(
    model: String,
    platformContext: PlatformContext,
    extractPalette: (ImageBitmap) -> T,
): T? {
    val request: ImageRequest = ImageRequest.Builder(platformContext)
        .data(model)
        .build()
    val result = SingletonImageLoader.get(platformContext).execute(request)
    if (result !is SuccessResult) {
        return null
    }
    val imageBitmap: ImageBitmap = coilImageToImageBitmap(image = result.image) ?: return null
    return extractPalette(imageBitmap)
}
```

- [x] **Step 6: Compile desktop**

Run:

```bash
./gradlew :composeApp:compileKotlinDesktop
```

Expected: PASS.

- [x] **Step 7: Compile Android**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [x] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.kt composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.android.kt composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.desktop.kt composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/feature/components/CoilImageBitmap.ios.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverPaletteLoader.kt
git commit -m "通过 Coil 加载封面取色"
```

## Task 6: Migrate Mini Player and Desktop Player Palette

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopPlayerDetailScreen.kt`

- [x] **Step 1: Update mini player palette imports**

In `MusicApp.kt`, remove:

```kotlin
import androidx.compose.ui.graphics.ImageBitmap
import com.yanhao.kmpmusic.core.theme.extractMiniPlayerPalette
import com.yanhao.kmpmusic.feature.components.coverArtResource
import org.jetbrains.compose.resources.imageResource
```

Add:

```kotlin
import com.yanhao.kmpmusic.feature.components.rememberMiniPlayerPalette
```

- [x] **Step 2: Replace mini player palette code**

In `MiniPlayer`, replace:

```kotlin
val coverImage: ImageBitmap = imageResource(resource = coverArtResource(coverArt = song.coverArt))
val miniPlayerPalette: MiniPlayerPalette = remember(song.coverArt, coverImage) {
    extractMiniPlayerPalette(imageBitmap = coverImage)
}
```

with:

```kotlin
val miniPlayerPalette: MiniPlayerPalette = rememberMiniPlayerPalette(
    coverArt = song.coverArt,
    coverImageUri = song.coverImageUri,
)
```

- [x] **Step 3: Update desktop player palette imports**

In `DesktopPlayerDetailScreen.kt`, remove:

```kotlin
import androidx.compose.ui.graphics.ImageBitmap
import com.yanhao.kmpmusic.core.theme.extractPlayerPagePalette
import com.yanhao.kmpmusic.feature.components.coverArtResource
import org.jetbrains.compose.resources.imageResource
```

Add:

```kotlin
import com.yanhao.kmpmusic.feature.components.defaultPlayerPagePalette
import com.yanhao.kmpmusic.feature.components.rememberPlayerPagePalette
```

- [x] **Step 4: Replace desktop player palette function**

Replace the existing private `rememberPlayerPagePalette(song: Song?)` function with:

```kotlin
@Composable
private fun rememberDesktopPlayerPagePalette(song: Song?): PlayerPagePalette {
    if (song == null) {
        return defaultPlayerPagePalette()
    }
    return rememberPlayerPagePalette(
        coverArt = song.coverArt,
        coverImageUri = song.coverImageUri,
    )
}
```

Update the caller near the top of `DesktopPlayerDetailScreen`:

```kotlin
val palette: PlayerPagePalette = rememberDesktopPlayerPagePalette(song = song)
```

- [x] **Step 5: Verify no old resource palette path remains**

Run:

```bash
rg "imageResource\\(|extractMiniPlayerPalette|extractPlayerPagePalette|coverArtResource\\(" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop
```

Expected: no matches in `feature/app` or `feature/desktop`. Matches in `core/theme/CoverPalette.kt` are valid because that file defines extraction algorithms.

- [x] **Step 6: Run shared tests**

Run:

```bash
./gradlew :composeApp:desktopTest
```

Expected: PASS.

- [x] **Step 7: Run Android compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [x] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopPlayerDetailScreen.kt
git commit -m "统一播放器封面取色来源"
```

## Task 7: Final Validation and Visual Risk Notes

**Files:**
- Modify: none unless validation exposes a compile or test issue.

- [x] **Step 1: Run the final old-API scan**

Run:

```bash
rg "coverArtPainter|rememberPlatformCoverArtPainter|decodeAndroidCoverImage|decodeDesktopCoverImage" composeApp/src
```

Expected: no matches.

Run:

```bash
rg "imageResource\\(|coverArtResource\\(" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen
```

Expected: no matches. `AndroidPlaybackMediaMetadataAssets.kt` may keep its resource-to-asset mapping because it feeds Media3 metadata, not Compose UI.

Run:

```bash
rg "coil3" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen
```

Expected: no matches. Coil imports belong in `feature/components` for this implementation.

- [x] **Step 2: Verify architecture boundaries were not crossed**

Run:

```bash
git diff --name-only origin/main -- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/data composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/data composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/data composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/playback
```

Expected: no output. This confirms the implementation did not modify domain models, scanners, artwork extractors, playback engines, Media3 session, or notification metadata files.

Run:

```bash
git diff --name-only origin/main -- prototypes composeApp/src/androidMain/res composeApp/src/commonMain/composeResources
```

Expected: no output. This confirms the implementation did not change prototype assets, platform-private resources, or bundled fallback artwork. The only resource mapping change should be Kotlin code in `feature/components`.

- [x] **Step 3: Run the required verification commands**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
```

Expected: PASS.

- [x] **Step 4: Check git status**

Run:

```bash
git status --short --branch
```

Expected: clean working tree on the current branch, with local commits ahead of `origin/main`.

- [x] **Step 5: Record visual verification status in the delivery message**

If no device or desktop screenshot was taken, the final delivery message must include:

```text
未做真机/桌面截图核对；剩余视觉风险是真实本地封面驱动的 mini player 和 macOS 播放页背景色需要人工确认。
```

If screenshots were taken, the final delivery message must name the platform and page checked:

```text
已核对 Android mini player 和 macOS 播放页，真实本地封面下背景色跟随封面变化，封面缺失时回退默认封面。
```

## Task 8: Add Coil Network Image Support

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`

**Validation note:** Network image loading was manually verified on macOS with a temporary scanner fixture. The fixture and test URL are removed before commit; Task 8 keeps only the Coil/Ktor dependency support.

- [x] **Step 1: Add Coil network and Ktor aliases**

Modify `gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.13.2"
coil = "3.5.0"
composeMultiplatform = "1.7.3"
kotlin = "2.0.21"
ktor = "3.3.3"
kotlinxCoroutines = "1.9.0"
kotlinxSerialization = "1.8.1"
androidxCore = "1.17.0"
androidxAppCompat = "1.7.1"
ksp = "2.0.21-1.0.28"
media3 = "1.10.1"
room3 = "3.0.0-rc01"
sqlite = "2.6.2"
vlcj = "4.12.1"

[libraries]
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-compose-core = { module = "io.coil-kt.coil3:coil-compose-core", version.ref = "coil" }
coil-network-ktor3 = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }
ktor-client-android = { module = "io.ktor:ktor-client-android", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-java = { module = "io.ktor:ktor-client-java", version.ref = "ktor" }
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-swing = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-swing", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
kotlinx-serialization-core = { module = "org.jetbrains.kotlinx:kotlinx-serialization-core", version.ref = "kotlinxSerialization" }
androidx-core = { module = "androidx.core:core", version.ref = "androidxCore" }
androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "androidxAppCompat" }
androidx-sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
androidx-room3-runtime = { module = "androidx.room3:room3-runtime", version.ref = "room3" }
androidx-room3-compiler = { module = "androidx.room3:room3-compiler", version.ref = "room3" }
androidx-media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "media3" }
androidx-media3-session = { module = "androidx.media3:media3-session", version.ref = "media3" }
androidx-media3-ui = { module = "androidx.media3:media3-ui", version.ref = "media3" }
vlcj = { module = "uk.co.caprica:vlcj", version.ref = "vlcj" }
```

- [x] **Step 2: Add network dependencies to source sets**

Modify `composeApp/build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation(libs.coil.compose)
    implementation(libs.coil.compose.core)
    implementation(libs.coil.network.ktor3)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
}
androidMain.dependencies {
    implementation(compose.preview)
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.ktor.client.android)
}
desktopMain.dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.ktor.client.java)
    implementation(libs.vlcj)
}
iosX64Main.dependencies {
    implementation(libs.ktor.client.darwin)
}
iosArm64Main.dependencies {
    implementation(libs.ktor.client.darwin)
}
iosSimulatorArm64Main.dependencies {
    implementation(libs.ktor.client.darwin)
}
```

- [x] **Step 3: Manually verify network loading, then remove temporary scanner fixtures**

macOS runtime verification confirmed that scanned rows can display a network cover through `coverImageUri -> CoverArtImage -> Coil`. After verification, remove all temporary scanner data and restore scanner output to production behavior:

- `FakeLocalMusicScanner` does not emit a hard-coded network cover URL.
- `DesktopFolderMusicScanner` keeps embedded artwork extraction output and does not override scan results with a test URL.

- [x] **Step 4: Verify the temporary URL is fully removed**

Run:

```bash
rg "<temporary network image marker>" composeApp/src
```

Expected: no matches.

- [x] **Step 5: Verify UI still does not import Coil directly**

Run:

```bash
rg "coil3" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen
```

Expected: no matches. Coil imports must remain inside `feature/components`.

- [x] **Step 6: Compile desktop and Android**

Run:

```bash
./gradlew :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [x] **Step 7: Record manual network-image verification status**

If no runtime desktop or Android check was performed, the final delivery message must include:

```text
未做运行时网络图片截图核对；剩余风险是测试 URL 在当前网络环境下能否稳定访问，以及 Coil/Ktor 在 Android、Desktop、iOS 的运行时网络加载表现需要人工确认。
```

If runtime verification was performed, the final delivery message must name the platform and page checked:

```text
已核对 macOS 本地音乐列表，扫描歌曲通过临时网络封面 URL 验证 Coil/Ktor 可正常加载并显示；验证夹具已删除，mini player 和播放页取色仍需按真实数据继续确认。
```

- [x] **Step 8: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts docs/superpowers/specs/2026-06-29-coil-image-loading-design.md docs/superpowers/plans/2026-06-29-coil-image-loading-implementation.md
git commit -m "支持 Coil 网络封面加载"
```

## Self-Review

- Spec coverage: Tasks 1-4 cover Coil dependency, shared request model, all Compose UI cover display, Coil-loaded fallback resources, and removal of platform hand decoding. Tasks 5-6 cover Coil-backed palette loading and migration of Android mini player and macOS desktop player palette. Task 7 covers old API scans, domain/data/playback/resource boundary checks, verification commands, and Media3 boundary preservation. Task 8 covers network image loading with Coil Ktor 3, runtime verification, and removal of temporary scanner fixtures.
- Placeholder scan: The plan contains no deferred implementation labels, no unspecified test requests, and no undefined feature tasks.
- Type consistency: `CoverArtImageRequest`, `buildCoverArtImageRequest`, `coverArtResourcePath`, `CoverArtImage`, `rememberMiniPlayerPalette`, `rememberPlayerPagePalette`, `defaultPlayerPagePalette`, and `coilImageToImageBitmap` are defined before later tasks reference them.
