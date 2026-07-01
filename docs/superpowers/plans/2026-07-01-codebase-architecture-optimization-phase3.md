# Codebase Architecture Optimization Phase 3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the mobile and desktop Compose UI large files into focused layout, route, surface, component, and player files without changing visual design, navigation behavior, playback behavior, or controller APIs.

**Architecture:** Keep `MusicApp(controller: MusicAppController)` and `DesktopMusicApp(controller: MusicAppController)` as the only public app entry points. Move UI responsibilities out of entry files into focused commonMain packages while keeping all user actions routed through `MusicAppController`. Rename ambiguous mobile fixed-bar state names before moving UI code so subsequent files use clear vocabulary.

**Tech Stack:** Kotlin Multiplatform 2.0.21, Compose Multiplatform 1.7.3, Material3 Compose, Kotlin test, Gradle `:composeApp`.

---

## Source Spec

Implement this plan from:

- `docs/superpowers/specs/2026-07-01-codebase-architecture-optimization-phase3-design.md`

The implementation must not touch `prototypes/kmp-music-hi-fi`.

## File Structure

### Existing Files To Modify

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`
  - Rename mobile fixed-bar state model types and properties.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`
  - Keep only public mobile entry, theme, system back handler, scan callback, and `MobileAppLayout` call.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`
  - Keep only public desktop entry, theme, scan callback, library load effect, and `DesktopAppLayout` call.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt`
  - Drain page implementations into desktop `screens` files, then remove or leave empty only until the same task completes.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`
  - Drain reusable components into desktop `components` files, then remove or leave empty only until the same task completes.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicPlayer.kt`
  - Drain bottom-player UI into `feature/desktop/player`.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopPlayerDetailScreen.kt`
  - Keep public `DesktopPlayerDetailScreen` in `feature/desktop/player` package path and split its internals.
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/MusicAppNavigationControllerTest.kt`
  - Update expected mobile fixed-bar type names.
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`
  - Update expected mobile fixed-bar type names.

### New Mobile/App Files

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/layout/MobileAppLayout.kt`
  - Mobile app max-width surface, density scaling, background, and top-level composition.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/layout/MobileContentLayout.kt`
  - Content bottom padding, `SaveableStateProvider`, and route mounting.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/routes/MobileRootScreenRoute.kt`
  - `RootTab` to mobile root screen dispatch.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/routes/MobileSecondaryScreenRoute.kt`
  - `SecondaryScreen` to mobile secondary screen dispatch.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playerbar/MobileFixedPlayerBar.kt`
  - Former `BottomChrome`; fixed mini-player and bottom-tab placement animation.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playerbar/MobileMiniPlayer.kt`
  - Former `MiniPlayer`, progress fraction, and mini control button.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playerbar/MobileBottomNavigation.kt`
  - Bottom tab bar, tab item, and root-tab label.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/surfaces/AppDialogs.kt`
  - Permission-settings and clear-cache dialogs.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/surfaces/AppPanels.kt`
  - Queue and more-action bottom sheets plus `BottomSheetAction`.

### New Desktop Files

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/layout/DesktopAppLayout.kt`
  - Window layout, full-screen player interception, title bar, rail, sidebar, workspace, bottom player, dialogs, and panels.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/layout/DesktopWorkspaceLayout.kt`
  - Workspace background, responsive page padding, `SaveableStateProvider`, and route mounting.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/layout/DesktopTitleBar.kt`
  - Move `DesktopTitleBar` from components.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation/DesktopNavigationRail.kt`
  - Move `DesktopRailDestination`, `DesktopRail`, and rail item.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation/DesktopRootScreenRoute.kt`
  - `RootTab` to desktop root screen dispatch.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation/DesktopSecondaryScreenRoute.kt`
  - Desktop secondary route dispatch. This file must not render `SecondaryScreen.Player`; it may keep an exhaustive no-op branch.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens/DesktopHomeScreen.kt`
  - `DesktopLocalMusicRootScreen`, home helper rows, `buildRecentAlbums`, `buildFrequentArtists`, `rootPlayAllLabel`, `playOrToggleRootCollection`.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens/DesktopFavoritesScreen.kt`
  - `DesktopFavoritesRootScreen`.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens/DesktopMeScreen.kt`
  - `DesktopMeRootScreen`.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens/DesktopSearchScreen.kt`
  - Desktop search page, scope tabs, history, chips, results, and tiny text button.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens/DesktopDetailScreens.kt`
  - Desktop album detail, artist detail, and empty state.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens/DesktopSettingsAndLoginScreens.kt`
  - Desktop settings and login pages.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens/DesktopLocalMusicScreen.kt`
  - Desktop local music page, local sections, local section labels/subtitles, source date formatter.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/components/DesktopButtons.kt`
  - `DesktopPrimaryButton`, `DesktopSecondaryButton`, `DesktopMoreButton`, `DesktopSortButton`, `DesktopTinyTextButton`.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/components/DesktopForms.kt`
  - `DesktopTextInput`, `DesktopSegmentedControl`.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/components/DesktopTables.kt`
  - `DesktopSongTable`, table header/row, trailing value, modified date formatter.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/components/DesktopCards.kt`
  - `DesktopStatCard`, `DesktopProfilePanel`, `DesktopContentRow`.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/components/DesktopSections.kt`
  - `DesktopPageHeader`, `DesktopToolbar`, `DesktopSectionHeader`, `DesktopAlbumGrid`, `DesktopAlbumCard`, `DesktopArtistStrip`, `DesktopSectionEmptyMessage`.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/player/DesktopBottomPlayer.kt`
  - Public bottom player entry.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/player/DesktopBottomPlayerTrack.kt`
  - Bottom player track info, actions, and open-player button.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/player/DesktopBottomPlayerControls.kt`
  - Bottom player transport controls and mode icon mapping.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/player/DesktopPlayerDetailScreen.kt`
  - Public detail player entry and page-level empty-state dispatch.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/player/DesktopPlayerDetailLayout.kt`
  - Detail page top bar, palette, content layout, metadata, volume, badges, and round icon button.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/player/DesktopPlayerDetailControls.kt`
  - Detail player progress and control row.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/player/DesktopPlayerDetailQueue.kt`
  - Detail player queue preview, queue row, and row builder.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/player/DesktopPlayerProgress.kt`
  - Shared `DesktopThinSlider`, time formatting, and player time text.
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/player/DesktopPlayerShared.kt`
  - Shared player-only `PlaybackModeIcon` / `DesktopPlayerModeIcon` model if both bottom and detail player need one common model.

## Task 1: Rename Mobile Fixed-Bar State Model

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/MusicAppNavigationControllerTest.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`

- [ ] **Step 1: Update navigation tests to the new names**

Change imports in `MusicAppNavigationControllerTest.kt`:

```kotlin
import com.yanhao.kmpmusic.feature.app.MobileFixedBarMode
import com.yanhao.kmpmusic.feature.app.MobileFixedBarPlacement
```

In `navigationStateProvidesChromeMode`, replace the body with:

```kotlin
val topLevelState: NavigationState = NavigationState()
assertEquals(expected = MobileFixedBarMode.TopLevel, actual = topLevelState.fixedBarMode)
assertTrue(actual = topLevelState.fixedBarMode.showsBottomNavigation)
assertEquals(
    expected = MobileFixedBarPlacement.TopLevel,
    actual = topLevelState.fixedBarMode.fixedBarPlacement,
)

val secondaryState: NavigationState = NavigationState(
    secondaryScreen = SecondaryScreen.AlbumDetail,
)
assertEquals(expected = MobileFixedBarMode.SecondaryWithMiniPlayer, actual = secondaryState.fixedBarMode)
assertFalse(actual = secondaryState.fixedBarMode.showsBottomNavigation)
assertEquals(
    expected = MobileFixedBarPlacement.MiniPlayerOnly,
    actual = secondaryState.fixedBarMode.fixedBarPlacement,
)

val fullscreenPlayerState: NavigationState = NavigationState(
    secondaryScreen = SecondaryScreen.Player,
)
assertEquals(expected = MobileFixedBarMode.SecondaryFullscreen, actual = fullscreenPlayerState.fixedBarMode)
assertFalse(actual = fullscreenPlayerState.fixedBarMode.showsBottomNavigation)
assertEquals(
    expected = MobileFixedBarPlacement.Hidden,
    actual = fullscreenPlayerState.fixedBarMode.fixedBarPlacement,
)

val fullscreenSettingsState: NavigationState = NavigationState(
    secondaryScreen = SecondaryScreen.Settings,
)
assertEquals(expected = MobileFixedBarMode.SecondaryFullscreen, actual = fullscreenSettingsState.fixedBarMode)
assertEquals(
    expected = MobileFixedBarPlacement.Hidden,
    actual = fullscreenSettingsState.fixedBarMode.fixedBarPlacement,
)
```

Change `MusicAppControllerTest.kt` expectations:

```kotlin
assertEquals(
    expected = MobileFixedBarMode.SecondaryWithMiniPlayer,
    actual = controller.uiState.navigationState.fixedBarMode,
)
```

and:

```kotlin
assertEquals(
    expected = MobileFixedBarMode.SecondaryFullscreen,
    actual = controller.uiState.navigationState.fixedBarMode,
)
```

- [ ] **Step 2: Run the focused tests and verify they fail**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest
```

Expected: FAIL with unresolved references to `MobileFixedBarMode`, `MobileFixedBarPlacement`, or `fixedBarMode`.

- [ ] **Step 3: Rename the model types and properties**

In `MusicAppModels.kt`, rename the declarations and properties to:

```kotlin
/**
 * 手机端固定底栏的整体位置策略。
 */
enum class MobileFixedBarPlacement {
    TopLevel,
    MiniPlayerOnly,
    Hidden,
}

/**
 * 手机端页面固定底栏策略，统一管理底部 Tab、迷你播放器和页面留白。
 */
enum class MobileFixedBarMode(
    val showsBottomNavigation: Boolean,
    val fixedBarPlacement: MobileFixedBarPlacement,
    val contentBottomSpace: ContentBottomSpace,
) {
    TopLevel(
        showsBottomNavigation = true,
        fixedBarPlacement = MobileFixedBarPlacement.TopLevel,
        contentBottomSpace = ContentBottomSpace.TopLevel,
    ),
    SecondaryWithMiniPlayer(
        showsBottomNavigation = false,
        fixedBarPlacement = MobileFixedBarPlacement.MiniPlayerOnly,
        contentBottomSpace = ContentBottomSpace.SecondaryWithMiniPlayer,
    ),
    SecondaryFullscreen(
        showsBottomNavigation = false,
        fixedBarPlacement = MobileFixedBarPlacement.Hidden,
        contentBottomSpace = ContentBottomSpace.Fullscreen,
    ),
}
```

In `NavigationState`, rename the derived property and comment to:

```kotlin
/**
 * 当前页面对应的手机端固定底栏策略。
 *
 * 这里是二级页面到底部固定栏表现的唯一配置入口：新增页面时优先在这里归类，
 * 不要在页面 Composable 或固定底栏周围散写显示/隐藏判断。
 */
val fixedBarMode: MobileFixedBarMode = when (secondaryScreen) {
    null -> MobileFixedBarMode.TopLevel
    SecondaryScreen.Player,
    SecondaryScreen.Settings,
    -> MobileFixedBarMode.SecondaryFullscreen
    is SecondaryScreen.Search,
    SecondaryScreen.AlbumDetail,
    SecondaryScreen.ArtistDetail,
    SecondaryScreen.Login,
    is SecondaryScreen.LocalMusic,
    -> MobileFixedBarMode.SecondaryWithMiniPlayer
}
```

In `MusicApp.kt`, replace uses:

```kotlin
val fixedBarMode: MobileFixedBarMode = state.navigationState.fixedBarMode
```

and:

```kotlin
fixedBarMode = fixedBarMode
```

and:

```kotlin
placement = fixedBarMode.fixedBarPlacement
```

Update `AppContent` parameter:

```kotlin
fixedBarMode: MobileFixedBarMode,
```

and bottom padding:

```kotlin
val bottomPadding: Dp = getContentBottomPadding(contentBottomSpace = fixedBarMode.contentBottomSpace)
```

Update `BottomChrome` parameter type for this task only:

```kotlin
placement: MobileFixedBarPlacement,
```

and its `when` branches to `MobileFixedBarPlacement.TopLevel`, `MiniPlayerOnly`, and `Hidden`.

- [ ] **Step 4: Search for old names**

Run:

```bash
rg "AppChromeMode|BottomChromePlacement|chromeMode|bottomChromePlacement" composeApp/src/commonMain composeApp/src/commonTest
```

Expected: no output.

- [ ] **Step 5: Run focused tests and Android compile**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: both commands PASS.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppModels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/MusicAppNavigationControllerTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "refactor: 重命名手机端固定底栏状态"
```

## Task 2: Split Cross-App Dialogs And Panels

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/surfaces/AppDialogs.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/surfaces/AppPanels.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`

- [ ] **Step 1: Create `AppDialogs.kt`**

Move the two `AlertDialog` branches from `AppOverlays` into:

```kotlin
package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 跨端复用的全局确认对话框。
 */
@Composable
fun AppDialogs(
    state: MusicAppUiState,
    controller: MusicAppController,
) {
    if (state.isClearCacheDialogOpen) {
        AlertDialog(
            onDismissRequest = controller::closeClearCacheDialog,
            confirmButton = {
                Button(onClick = controller::confirmClearCache) {
                    Text(text = "清理")
                }
            },
            dismissButton = {
                Button(onClick = controller::closeClearCacheDialog) {
                    Text(text = "取消")
                }
            },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MusicColors.Danger) },
            title = { Text(text = "清理 428 MB 缓存？") },
            text = { Text(text = "只会删除封面缓存和临时文件，本地歌曲不会受到影响。") },
        )
    }
    if (state.isPermissionSettingsDialogOpen) {
        AlertDialog(
            onDismissRequest = controller::closePermissionSettingsDialog,
            confirmButton = {
                Button(onClick = controller::confirmPermissionSettings) {
                    Text(text = "去设置")
                }
            },
            dismissButton = {
                Button(onClick = controller::closePermissionSettingsDialog) {
                    Text(text = "取消")
                }
            },
            icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = MusicColors.Accent) },
            title = { Text(text = "开启音频权限") },
            text = { Text(text = "需要在系统设置中开启音频权限，才能扫描本机歌曲。") },
        )
    }
}
```

- [ ] **Step 2: Create `AppPanels.kt`**

Move the queue sheet, more sheet, and `BottomSheetAction` into:

```kotlin
package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.components.SongRow

/**
 * 跨端复用的全局底部面板。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppPanels(
    state: MusicAppUiState,
    controller: MusicAppController,
) {
    if (state.isQueueOpen) {
        ModalBottomSheet(onDismissRequest = controller::closeQueue) {
            val queueSongs: List<Song> = state.queueSongs
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(all = 21.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "queue-title") {
                    Text(text = "播放队列", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                }
                items(
                    items = queueSongs,
                    key = { song: Song -> song.id },
                    contentType = { "queue-song" },
                ) { song: Song ->
                    SongRow(
                        song = song,
                        isCurrentSong = song.id == state.currentSongId,
                        currentPlaybackStatus = state.playbackStatus,
                        onOpen = { selectedSong: Song ->
                            controller.playSong(
                                song = selectedSong,
                                queueSongs = queueSongs,
                            )
                        },
                        onPlay = { selectedSong: Song ->
                            controller.playSong(
                                song = selectedSong,
                                queueSongs = queueSongs,
                            )
                        },
                        onCurrentSongToggle = controller::togglePlayback,
                        onMore = controller::openMore,
                        dense = true,
                    )
                }
            }
        }
    }
    state.moreSongId?.let { songId ->
        val song: Song? = state.currentSong?.takeIf { item -> item.id == songId }
            ?: state.queueSongs.firstOrNull { item -> item.id == songId }
            ?: state.localSongs.firstOrNull { item -> item.id == songId }
            ?: state.homeLocalSongPreview.firstOrNull { item -> item.id == songId }
            ?: state.favoriteSongs.firstOrNull { item -> item.id == songId }
        if (song != null) {
            ModalBottomSheet(onDismissRequest = controller::closeMore) {
                Column(modifier = Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(
                        text = song.title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BottomSheetAction("加入收藏", Icons.Rounded.Favorite) { controller.toggleFavorite(song.id) }
                    BottomSheetAction("查看专辑", Icons.Rounded.LibraryMusic) { controller.openAlbumFromSong(song) }
                    BottomSheetAction("查看歌手", Icons.Rounded.Person) { controller.openArtistFromSong(song) }
                }
            }
        }
    }
}

/**
 * 更多操作面板中的单行动作。
 */
@Composable
private fun BottomSheetAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = MusicColors.Soft, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null)
            Text(text = label, fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.MoreHoriz, contentDescription = null, tint = MusicColors.Muted)
        }
    }
}
```

- [ ] **Step 3: Replace `AppOverlays` call sites**

In `MusicApp.kt`, replace:

```kotlin
AppOverlays(state = state, controller = controller)
```

with:

```kotlin
AppDialogs(state = state, controller = controller)
AppPanels(state = state, controller = controller)
```

Add imports:

```kotlin
import com.yanhao.kmpmusic.feature.app.surfaces.AppDialogs
import com.yanhao.kmpmusic.feature.app.surfaces.AppPanels
```

In `DesktopMusicApp.kt`, replace both `AppOverlays` call sites with the same two calls and replace its import with the same two surface imports.

- [ ] **Step 4: Delete old overlay functions from `MusicApp.kt`**

Remove these declarations from `MusicApp.kt`:

```text
internal fun AppOverlays
private fun BottomSheetAction
```

- [ ] **Step 5: Verify old overlay name is gone**

Run:

```bash
rg "AppOverlays|BottomSheetAction" composeApp/src/commonMain composeApp/src/commonTest
```

Expected: no output for `AppOverlays`; one output for `BottomSheetAction` is allowed only in `feature/app/surfaces/AppPanels.kt`.

- [ ] **Step 6: Compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/surfaces/AppDialogs.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/surfaces/AppPanels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt
git commit -m "refactor: 拆分全局弹窗和面板"
```

## Task 3: Extract Mobile Layout, Routes, And Fixed Player Bar

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/layout/MobileAppLayout.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/layout/MobileContentLayout.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/routes/MobileRootScreenRoute.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/routes/MobileSecondaryScreenRoute.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playerbar/MobileFixedPlayerBar.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playerbar/MobileMiniPlayer.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playerbar/MobileBottomNavigation.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`

- [ ] **Step 1: Create `MobileAppLayout` public API**

Create this shell and then move the current `BoxWithConstraints` body from `MusicApp.kt` into the function:

```kotlin
package com.yanhao.kmpmusic.feature.app.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 手机端 App 外层布局，统一承接宽度、背景、缩放、内容、固定底栏和全局 surfaces。
 */
@Composable
fun MobileAppLayout(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Use the `BoxWithConstraints` expression currently inside `KmpMusicTheme` in `MusicApp.kt` as this function body. In the same task, update that moved body so it calls `MobileContentLayout`, `MobileFixedPlayerBar`, `AppDialogs`, and `AppPanels`.

- [ ] **Step 2: Reduce `MusicApp.kt` to the public entry**

After moving layout code, `MusicApp.kt` should keep this shape:

```kotlin
@Composable
fun MusicApp(
    controller: MusicAppController,
) {
    val state: MusicAppUiState = controller.uiState
    val coroutineScope = rememberCoroutineScope()
    val scanLocalMusic: () -> Unit = {
        coroutineScope.launch {
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        }
    }
    KmpMusicTheme(themeMode = state.themeMode) {
        PlatformBackHandler(
            enabled = state.canHandleSystemBack,
            onBack = { controller.handleSystemBack() },
        )
        MobileAppLayout(
            state = state,
            controller = controller,
            onScanLocalMusic = scanLocalMusic,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

- [ ] **Step 3: Create `MobileContentLayout`**

Move current `AppContent` and `getContentBottomPadding` into `MobileContentLayout.kt`. Rename the public function:

```kotlin
@Composable
fun MobileContentLayout(
    state: MusicAppUiState,
    controller: MusicAppController,
    fixedBarMode: MobileFixedBarMode,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomPadding: Dp = getContentBottomPadding(contentBottomSpace = fixedBarMode.contentBottomSpace)
    val pagePadding: PaddingValues = PaddingValues(
        start = scaledDp(MusicDimens.PagePaddingHorizontal),
        top = scaledDp(MusicDimens.PagePaddingTop),
        end = scaledDp(MusicDimens.PagePaddingHorizontal),
        bottom = bottomPadding,
    )
    val saveableStateHolder = rememberSaveableStateHolder()
    saveableStateHolder.SaveableStateProvider(key = state.navigationState.scrollStateKey) {
        val secondaryScreen: SecondaryScreen? = state.navigationState.secondaryScreen
        if (secondaryScreen is SecondaryScreen.LocalMusic) {
            MobileSecondaryScreenRoute(
                secondaryScreen = secondaryScreen,
                state = state,
                controller = controller,
                onScanLocalMusic = onScanLocalMusic,
                modifier = modifier,
                contentPadding = pagePadding,
            )
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(pagePadding),
            ) {
                if (secondaryScreen == null) {
                    MobileRootScreenRoute(
                        state = state,
                        controller = controller,
                        onScanLocalMusic = onScanLocalMusic,
                    )
                } else {
                    MobileSecondaryScreenRoute(
                        secondaryScreen = secondaryScreen,
                        state = state,
                        controller = controller,
                        onScanLocalMusic = onScanLocalMusic,
                        contentPadding = pagePadding,
                    )
                }
            }
        }
    }
}
```

This preserves the current root-page and non-local-secondary wrapper: status bar padding, navigation bar padding, vertical scroll, and `pagePadding` must stay outside `MobileRootScreenRoute` and non-local secondary routes. `LocalMusicScreen` remains the only mobile route mounted outside the vertical-scroll `Column`, matching the current list-screen behavior.

- [ ] **Step 4: Create `MobileRootScreenRoute`**

Move current `RootScreen` from `MusicApp.kt` into `MobileRootScreenRoute.kt` and rename it:

```kotlin
@Composable
fun MobileRootScreenRoute(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
)
```

Move the full `when (state.navigationState.rootTab)` expression from the existing `RootScreen` function into `MobileRootScreenRoute`. Keep every existing `HomeScreen`, `FavoritesScreen`, and `MeScreen` argument and controller callback mapping unchanged.

Do not add status bar padding, navigation bar padding, vertical scroll, or bottom content padding inside `MobileRootScreenRoute`; `MobileContentLayout` owns that wrapper so every root tab keeps the current shared layout behavior.

- [ ] **Step 5: Create `MobileSecondaryScreenRoute`**

Move the secondary `when` branches from `AppContent` into:

```kotlin
@Composable
fun MobileSecondaryScreenRoute(
    secondaryScreen: SecondaryScreen,
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
)
```

Move the full `when (secondaryScreen)` expression from the existing `AppContent` function into `MobileSecondaryScreenRoute`. Keep the `LocalMusicScreen`, `SearchScreen`, `PlayerScreen`, `AlbumDetailScreen`, `ArtistDetailScreen`, `SettingsScreen`, `LoginScreen`, and `MissingLibraryItemScreen` branches with the same argument and controller callback mapping.

- [ ] **Step 6: Create mobile playerbar files**

Move these functions from `MusicApp.kt`:

```text
BottomChrome -> MobileFixedPlayerBar
MiniPlayer -> MobileMiniPlayer
calculateMiniPlayerProgressFraction -> calculateMiniPlayerProgressFraction
MiniControlButton -> MiniControlButton
BottomNavigation -> MobileBottomNavigation
BottomNavigationItem -> MobileBottomNavigationItem
RootTab.label -> RootTab.mobileLabel
```

The public entry in `MobileFixedPlayerBar.kt` must be:

```kotlin
@Composable
fun MobileFixedPlayerBar(
    song: Song?,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    placement: MobileFixedBarPlacement,
    showsBottomNavigation: Boolean,
    rootTab: RootTab,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onQueue: () -> Unit,
    onRootTab: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
) {
}
```

Use the former `BottomChrome` body adapted to the new function names. Rename animation labels from `"BottomChromePlacement"` and `"BottomChromeOffset"` to `"MobileFixedBarPlacement"` and `"MobileFixedBarOffset"`.

- [ ] **Step 7: Wire new mobile files**

In `MobileAppLayout`, call:

```kotlin
val fixedBarMode: MobileFixedBarMode = state.navigationState.fixedBarMode
MobileContentLayout(
    state = state,
    controller = controller,
    fixedBarMode = fixedBarMode,
    onScanLocalMusic = onScanLocalMusic,
)
MobileFixedPlayerBar(
    song = state.currentSong,
    isPlaying = state.shouldShowPauseControl,
    playbackPositionMs = state.playbackPositionMs,
    playbackDurationMs = state.playbackDurationMs,
    placement = fixedBarMode.fixedBarPlacement,
    showsBottomNavigation = fixedBarMode.showsBottomNavigation,
    rootTab = state.navigationState.rootTab,
    onOpen = controller::openPlayer,
    onToggle = controller::togglePlayback,
    onPrev = { controller.moveTrack(direction = -1) },
    onQueue = controller::openQueue,
    onRootTab = controller::navigateToRoot,
    modifier = Modifier.align(Alignment.BottomCenter),
)
AppDialogs(state = state, controller = controller)
AppPanels(state = state, controller = controller)
```

- [ ] **Step 8: Verify `MusicApp.kt` no longer owns moved UI**

Run:

```bash
rg "private fun AppContent|private fun RootScreen|BottomChrome|MiniPlayer|BottomNavigation|BottomSheetAction|AppDialogs|AppPanels" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt
```

Expected: output contains only imports or call sites for `MobileAppLayout`; it must not contain old function declarations.

- [ ] **Step 9: Compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/layout composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/routes composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playerbar
git commit -m "refactor: 拆分手机端 App 布局和固定底栏"
```

## Task 4: Extract Desktop Layout And Routes

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/layout/DesktopAppLayout.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/layout/DesktopWorkspaceLayout.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/layout/DesktopTitleBar.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation/DesktopNavigationRail.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation/DesktopRootScreenRoute.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation/DesktopSecondaryScreenRoute.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`

- [ ] **Step 1: Create `DesktopAppLayout`**

Move the themed `Box` body from `DesktopMusicApp.kt` into:

```kotlin
@Composable
fun DesktopAppLayout(
    state: MusicAppUiState,
    controller: MusicAppController,
    saveableStateHolder: SaveableStateHolder,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Use the `Box` expression currently inside `KmpMusicTheme` in `DesktopMusicApp.kt` as this function body. In the same task, update that moved body so it calls `DesktopWorkspaceLayout`, `DesktopBottomPlayer`, `AppDialogs`, and `AppPanels`.

- [ ] **Step 2: Keep `SecondaryScreen.Player` only in `DesktopAppLayout`**

In `DesktopAppLayout`, keep the full-screen branch:

```kotlin
if (state.navigationState.secondaryScreen == SecondaryScreen.Player) {
    saveableStateHolder.SaveableStateProvider(key = state.navigationState.scrollStateKey) {
        DesktopPlayerDetailScreen(
            song = state.currentSong,
            queueSongs = state.queueSongs,
            isPlaying = state.shouldShowPauseControl,
            playbackPositionMs = state.playbackPositionMs,
            playbackDurationMs = state.playbackDurationMs,
            playbackMode = state.playbackMode,
            volume = state.playbackVolume,
            onBack = controller::navigateBack,
            onToggle = controller::togglePlayback,
            onPrev = { controller.moveTrack(direction = -1) },
            onNext = { controller.moveTrack(direction = 1) },
            onMode = controller::cyclePlaybackMode,
            onLike = controller::toggleFavorite,
            onSeek = controller::seekTo,
            onVolumeChange = controller::setVolume,
            modifier = Modifier.fillMaxSize(),
        )
    }
    AppDialogs(state = state, controller = controller)
    AppPanels(state = state, controller = controller)
    return@Box
}
```

- [ ] **Step 3: Reduce `DesktopMusicApp.kt` to the public entry**

`DesktopMusicApp.kt` should keep this shape:

```kotlin
@Composable
fun DesktopMusicApp(
    controller: MusicAppController,
) {
    val state: MusicAppUiState = controller.uiState
    val coroutineScope = rememberCoroutineScope()
    val scanLocalMusic: () -> Unit = {
        coroutineScope.launch {
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        }
    }
    val saveableStateHolder: SaveableStateHolder = rememberSaveableStateHolder()
    LaunchedEffect(
        state.navigationState.rootTab,
        state.navigationState.secondaryScreen,
        state.libraryStats.songCount,
        state.localSongs.size,
    ) {
        if (state.navigationState.rootTab == RootTab.Home &&
            state.navigationState.secondaryScreen == null &&
            state.libraryStats.songCount > 0 &&
            state.localSongs.isEmpty()
        ) {
            controller.loadLocalMusicLibrary()
        }
    }
    KmpMusicTheme(themeMode = state.themeMode) {
        DesktopAppLayout(
            state = state,
            controller = controller,
            saveableStateHolder = saveableStateHolder,
            onScanLocalMusic = scanLocalMusic,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

- [ ] **Step 4: Create `DesktopWorkspaceLayout`**

Move current `DesktopWorkspace` into:

```kotlin
@Composable
fun DesktopWorkspaceLayout(
    state: MusicAppUiState,
    controller: MusicAppController,
    saveableStateHolder: SaveableStateHolder,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Use the current `DesktopWorkspace` body as this function body. Move the current non-player `SaveableStateProvider` wrapper into `DesktopWorkspaceLayout`:

```kotlin
saveableStateHolder.SaveableStateProvider(key = state.navigationState.scrollStateKey) {
    if (state.navigationState.secondaryScreen == null) {
        DesktopRootScreenRoute(
            state = state,
            controller = controller,
            onScanLocalMusic = onScanLocalMusic,
        )
    } else {
        DesktopSecondaryScreenRoute(
            state = state,
            controller = controller,
            onScanLocalMusic = onScanLocalMusic,
        )
    }
}
```

`DesktopAppLayout` must pass the same `saveableStateHolder` it receives into `DesktopWorkspaceLayout`. This preserves the current desktop scroll/state isolation for non-player pages; the full-screen player branch keeps its own top-level `SaveableStateProvider`.

- [ ] **Step 5: Create desktop route files**

Create `DesktopRootScreenRoute`:

```kotlin
@Composable
fun DesktopRootScreenRoute(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
)
```

Create `DesktopSecondaryScreenRoute`:

```kotlin
@Composable
fun DesktopSecondaryScreenRoute(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
)
```

Move the existing root and secondary route branches with their argument lists and controller callback mappings. The `SecondaryScreen.Player -> Unit` branch is intentional because `DesktopAppLayout` owns the player full-screen route.

- [ ] **Step 6: Move title bar and rail**

Move from `DesktopMusicComponents.kt`:

```text
DesktopTitleBar -> layout/DesktopTitleBar.kt
DesktopRailDestination -> navigation/DesktopNavigationRail.kt
DesktopRail -> navigation/DesktopNavigationRail.kt
DesktopRailItem -> navigation/DesktopNavigationRail.kt
```

Move from `DesktopMusicApp.kt` to `DesktopNavigationRail.kt`:

```text
MusicAppUiState.desktopRailDestination
```

Move from `DesktopMusicApp.kt` to `DesktopAppLayout.kt`:

```text
MusicAppUiState.shouldShowLibrarySidebar
```

- [ ] **Step 7: Verify player double-route is gone**

Run:

```bash
rg -n "SecondaryScreen.Player" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop
```

Expected: `SecondaryScreen.Player` appears in `layout/DesktopAppLayout.kt` and in `navigation/DesktopSecondaryScreenRoute.kt` only as the no-op branch. It must not create `DesktopPlayerDetailScreen` inside `DesktopSecondaryScreenRoute.kt`.

- [ ] **Step 8: Compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/layout composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation
git commit -m "refactor: 拆分桌面布局和路由"
```

## Task 5: Split Desktop Screens

**Files:**
- Create files under `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens/`
- Create or update: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/components/DesktopButtons.kt`
- Create or update: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/components/DesktopSections.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation/DesktopRootScreenRoute.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation/DesktopSecondaryScreenRoute.kt`

- [ ] **Step 1: Move home screen**

Move these declarations to `screens/DesktopHomeScreen.kt`:

```text
DesktopLocalMusicRootScreen
DesktopThreeStatRow
rootPlayAllLabel
playOrToggleRootCollection
buildRecentAlbums
buildFrequentArtists
normalizeDesktopLookupKey
```

Keep `DesktopLocalMusicRootScreen` public.

- [ ] **Step 2: Move favorites and me screens**

Move:

```text
DesktopFavoritesRootScreen -> screens/DesktopFavoritesScreen.kt
DesktopMeRootScreen -> screens/DesktopMeScreen.kt
```

Keep both public because route files call them.

- [ ] **Step 3: Move search screen**

Move these declarations to `screens/DesktopSearchScreen.kt`:

```text
DesktopSearchScreen
DesktopSearchScopeTabs
DesktopSearchHistorySection
DesktopSearchHistoryChip
DesktopSearchResultsSection
```

Keep `DesktopSearchScreen` public. Move `DesktopTinyTextButton` directly to `components/DesktopButtons.kt` in this task because it is shared UI, not part of the search screen's page implementation. If `components/DesktopButtons.kt` does not exist yet, create it now with only `DesktopTinyTextButton`; Task 6 will add the remaining button declarations to the same file.

- [ ] **Step 4: Move detail, settings, login, local music, and empty screens**

Move:

```text
DesktopAlbumDetailScreen -> screens/DesktopDetailScreens.kt
DesktopArtistDetailScreen -> screens/DesktopDetailScreens.kt
DesktopEmptyStateScreen -> screens/DesktopDetailScreens.kt
DesktopSettingsScreen -> screens/DesktopSettingsAndLoginScreens.kt
DesktopLoginScreen -> screens/DesktopSettingsAndLoginScreens.kt
DesktopLocalMusicScreen -> screens/DesktopLocalMusicScreen.kt
DesktopLocalAlbumSection -> screens/DesktopLocalMusicScreen.kt
DesktopLocalArtistSection -> screens/DesktopLocalMusicScreen.kt
DesktopLocalSourcesSection -> screens/DesktopLocalMusicScreen.kt
LocalMusicSection.desktopLabel -> screens/DesktopLocalMusicScreen.kt
LocalMusicSection.desktopLocalMusicSubtitle -> screens/DesktopLocalMusicScreen.kt
formatDesktopSourceScanDate -> screens/DesktopLocalMusicScreen.kt
```

Move `DesktopSectionEmptyMessage` directly to `components/DesktopSections.kt` in this task because it is used by search, home, favorites, me, and local sections. If `components/DesktopSections.kt` does not exist yet, create it now with only `DesktopSectionEmptyMessage`; Task 6 will add the remaining section declarations to the same file.

- [ ] **Step 5: Remove old router**

Delete the old `DesktopSecondaryScreen` function from `DesktopMusicScreens.kt`. Route files must call the new screen files directly.

- [ ] **Step 6: Verify `DesktopMusicScreens.kt` is drained**

Run:

```bash
rg -n "fun Desktop|private fun|@Composable" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt
```

Expected: no output. If the file is empty except package/imports, delete `DesktopMusicScreens.kt`.

- [ ] **Step 7: Compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/components/DesktopButtons.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/components/DesktopSections.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation
git commit -m "refactor: 拆分桌面页面"
```

## Task 6: Split Desktop Components

**Files:**
- Create files under `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/components/`
- Modify or delete: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`
- Modify imports in desktop layout, navigation, and screens files created in Tasks 4 and 5.

- [ ] **Step 1: Move buttons and forms**

Move to `components/DesktopButtons.kt`:

```text
DesktopPrimaryButton
DesktopSecondaryButton
DesktopMoreButton
DesktopSortButton
```

`DesktopTinyTextButton` should already be in `components/DesktopButtons.kt` from Task 5; keep it there and add the remaining button declarations beside it.

Move to `components/DesktopForms.kt`:

```text
DesktopTextInput
DesktopSegmentedControl
```

- [ ] **Step 2: Move tables**

Move to `components/DesktopTables.kt`:

```text
DesktopSongTable
DesktopSongTableHeader
DesktopSongTableRow
desktopSongTableTrailingValue
formatDesktopModifiedDate
floorDivByDay
civilDateFromEpochDay
CivilDate
```

Keep `DesktopSongTable` public. Keep helper functions private unless another file calls them.

- [ ] **Step 3: Move cards and rows**

Move to `components/DesktopCards.kt`:

```text
DesktopStatCard
DesktopProfilePanel
DesktopContentRow
```

- [ ] **Step 4: Move sections**

Move to `components/DesktopSections.kt`:

```text
DesktopPageHeader
DesktopToolbar
DesktopSectionHeader
DesktopAlbumGrid
DesktopAlbumCard
DesktopArtistStrip
```

`DesktopSectionEmptyMessage` should already be in `components/DesktopSections.kt` from Task 5; keep it there and add the remaining section declarations beside it. Keep `DesktopAlbumGrid` and `DesktopAlbumCard` in the same file to preserve high cohesion.

- [ ] **Step 5: Verify old component file is drained**

Run:

```bash
rg -n "fun Desktop|private fun|data class CivilDate|@Composable" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt
```

Expected: no output. If the file is empty except package/imports, delete `DesktopMusicComponents.kt`.

- [ ] **Step 6: Compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/components composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/layout composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens
git commit -m "refactor: 拆分桌面复用组件"
```

## Task 7: Split Desktop Player UI

**Files:**
- Create files under `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/player/`
- Modify or delete: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicPlayer.kt`
- Modify or move: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopPlayerDetailScreen.kt`
- Modify imports in `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/layout/DesktopAppLayout.kt`

- [ ] **Step 1: Move bottom player entry and track**

Move to `player/DesktopBottomPlayer.kt`:

```text
DesktopBottomPlayer
```

Move to `player/DesktopBottomPlayerTrack.kt`:

```text
DesktopPlayerTrack
DesktopPlayerTrackActions
DesktopOpenPlayerButton
```

Keep `DesktopBottomPlayer` public.

- [ ] **Step 2: Move bottom player controls and progress**

Move to `player/DesktopBottomPlayerControls.kt`:

```text
DesktopPlayerControls
PlaybackMode.toPlaybackModeIcon
PlaybackModeIcon
```

Move to `player/DesktopPlayerProgress.kt`:

```text
DesktopThinSlider
formatTime
```

If detail player can share the same formatter after Task 7 Step 4, rename `formatTime` to `formatDesktopPlayerTime` and use it from both bottom and detail player.

- [ ] **Step 3: Move detail player screen file path**

Move `DesktopPlayerDetailScreen.kt` to:

```text
composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/player/DesktopPlayerDetailScreen.kt
```

Change its package to:

```kotlin
package com.yanhao.kmpmusic.feature.desktop.player
```

Keep public function signature unchanged:

```kotlin
@Composable
fun DesktopPlayerDetailScreen(
    song: Song?,
    queueSongs: List<Song>,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    playbackMode: PlaybackMode,
    volume: Float,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onLike: (String) -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
)
```

- [ ] **Step 4: Split detail player internals**

Move from `player/DesktopPlayerDetailScreen.kt` into focused files:

```text
rememberDesktopPlayerPagePalette -> DesktopPlayerDetailLayout.kt
DesktopPlayerTopBar -> DesktopPlayerDetailLayout.kt
DesktopPlayerContent -> DesktopPlayerDetailLayout.kt
DesktopPlayerMetadata -> DesktopPlayerDetailLayout.kt
DesktopPlayerVolume -> DesktopPlayerDetailLayout.kt
DesktopPlayerEmptyState -> DesktopPlayerDetailLayout.kt
DesktopRoundIconButton -> DesktopPlayerDetailLayout.kt
DesktopPlayerBadge -> DesktopPlayerDetailLayout.kt
DesktopPlayerProgress -> DesktopPlayerDetailControls.kt
DesktopPlayerControlRow -> DesktopPlayerDetailControls.kt
PlaybackMode.toDesktopPlayerModeIcon -> DesktopPlayerDetailControls.kt
DesktopPlayerQueuePreview -> DesktopPlayerDetailQueue.kt
DesktopPlayerQueueRow -> DesktopPlayerDetailQueue.kt
buildPlayerQueueRows -> DesktopPlayerDetailQueue.kt
formatDesktopPlayerTime -> DesktopPlayerProgress.kt
DesktopPlayerTimeText -> DesktopPlayerProgress.kt
```

Keep `DesktopPlayerShared.kt` only if both bottom player and detail player use the same icon model after this split.

- [ ] **Step 5: Remove old player files**

Run:

```bash
rg -n "fun Desktop|private fun|internal fun|@Composable" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicPlayer.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopPlayerDetailScreen.kt
```

Expected: no output because both old files are deleted or drained. If files are empty except package/imports, delete them.

- [ ] **Step 6: Compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicPlayer.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopPlayerDetailScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/player composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/layout/DesktopAppLayout.kt
git commit -m "refactor: 拆分桌面播放器 UI"
```

## Task 8: Final Structure Sweep And Verification

**Files:**
- Modify imports or delete empty files discovered by the verification commands.
- No production behavior changes are allowed in this task.

- [ ] **Step 1: Verify forbidden old names**

Run:

```bash
rg "BottomChrome|AppChromeMode|BottomChromePlacement|chromeMode|bottomChromePlacement|AppOverlays|feature/app/navigation/Mobile" composeApp/src/commonMain composeApp/src/commonTest
```

Expected: no output.

- [ ] **Step 2: Verify entry files are thin**

Run:

```bash
rg -n "private fun|internal fun|@Composable" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt
```

Expected:

```text
composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt:<line>:@Composable
composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt:<line>:@Composable
```

Only the public entry composables should appear.

- [ ] **Step 3: Verify no old desktop aggregate files still own UI**

Run this check only for files that still exist. If a file was deleted intentionally, verify deletion with `rg --files` first and skip that path:

```bash
for file in \
  composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt \
  composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt \
  composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicPlayer.kt \
  composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopPlayerDetailScreen.kt
do
  if test -e "$file"; then
    rg -n "fun Desktop|private fun|internal fun|@Composable" "$file"
  fi
done
```

Expected: no output.

- [ ] **Step 4: Run final automated verification**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
```

Expected: PASS.

- [ ] **Step 5: Manual visual checklist**

Run the app on the available platform and check these screens:

```text
Mobile: Home, Favorites, Me, Search, Player, Queue panel, More panel, Permission dialog.
Desktop: Home, Favorites, Me, Search, Local Music Songs, Local Music Albums, Local Music Artists, Local Music Sources, Album detail, Artist detail, Settings, Login, Bottom player, Full-screen player.
```

Expected:

```text
Mobile top-level pages show mini player plus bottom tabs with no gap.
Mobile secondary pages hide bottom tabs and keep mini player at the bottom.
Mobile player and settings hide the fixed bottom bar.
Desktop full-screen player hides normal workspace and bottom player.
Current playing song remains highlighted.
Search, more menu, favorite, play all, open detail, and back still call controller behavior correctly.
```

- [ ] **Step 6: Commit final cleanup**

```bash
git add composeApp/src/commonMain composeApp/src/commonTest
git commit -m "refactor: 完成第三阶段 UI 结构治理"
```

## Self-Review

Spec coverage:

- Mobile `MusicApp.kt`, fixed bar, mini player, bottom navigation, routes, dialogs, and panels are covered by Tasks 1-3.
- Desktop entry, layout, title bar, rail, workspace, root route, and secondary route are covered by Task 4.
- Desktop screens are covered by Task 5.
- Desktop reusable components are covered by Task 6.
- Desktop bottom player and full-screen player are covered by Task 7.
- Naming governance and old entry cleanup are covered by Tasks 1, 2, and 8.
- Compile, desktop tests, and manual visual checks are covered by Task 8.

Type consistency:

- Mobile state names are consistently `MobileFixedBarMode`, `MobileFixedBarPlacement`, `fixedBarMode`, and `fixedBarPlacement`.
- Cross-app surfaces are consistently `AppDialogs` and `AppPanels`; `AppOverlays` is not a completion-state name.
- Desktop player full-screen route remains owned by `DesktopAppLayout`; `DesktopSecondaryScreenRoute` has only a no-op player branch.
- Public app entry signatures remain `MusicApp(controller: MusicAppController)` and `DesktopMusicApp(controller: MusicAppController)`.
