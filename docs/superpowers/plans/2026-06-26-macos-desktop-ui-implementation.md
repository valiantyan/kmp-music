# macOS Desktop UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the macOS Desktop mobile-sized UI with a Desktop-only Compose shell and pages that 1:1 follow `prototypes/kmp-music-desktop-uiux/index.html`, while Android/iOS mobile UI remains unchanged.

**Architecture:** Add a Desktop-specific UI package that is only called by `desktopMain`. It reuses `MusicAppController`, `MusicAppUiState`, domain models, repositories, playback state, navigation, scan, search, favorite, queue, and overlay behavior; it does not use WebView or prototype runtime assets. Keep existing `MusicApp` as the mobile/shared app surface.

**Tech Stack:** Kotlin Multiplatform 2.0.21, Compose Multiplatform 1.7.3, Material3 icons/components, existing `MusicAppController`, existing `composeResources` cover art.

---

## Scope Check

This plan covers one project: macOS Desktop UI 1:1 reproduction from the HTML prototype. It includes Desktop shell, Desktop root pages, Desktop secondary pages, Desktop player chrome, Desktop overlay access, and controller-level tests for navigation/state rules. It does not implement new playback, scanner, auth, cloud sync, or Android/iOS UI changes.

## File Structure

- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicTokens.kt`
  - Desktop-only colors, dimensions, responsive helpers, and text helpers that mirror the HTML prototype.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`
  - Reusable Desktop UI building blocks: title bar, rail item, buttons, segmented control, stats card, table rows, album grid, setting rows, slider visuals.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicPlayer.kt`
  - Bottom player chrome and queue/player entry points, backed by existing playback state and controller callbacks.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt`
  - Desktop root and secondary pages. Root pages map HTML sections; secondary pages reuse the same Desktop visual language.
- Create `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`
  - Desktop shell, routing, scan coroutine, overlay integration, and controller wiring.
- Modify `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopMain.kt`
  - Change default window size to Desktop dimensions and call `DesktopMusicApp`.
- Modify `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`
  - Add controller tests for Desktop rail behavior and playback state consistency that can be verified without UI screenshot tooling.
- No changes to `composeApp/src/androidMain`, `composeApp/src/iosMain`, existing mobile `MusicApp`, or `prototypes/kmp-music-desktop-uiux`.

---

### Task 1: Desktop Tokens And Window Entry

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicTokens.kt`
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`
- Modify: `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopMain.kt`

- [x] **Step 1: Create Desktop token file**

Create `DesktopMusicTokens.kt` with the following initial content:

```kotlin
package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Desktop UI colors copied from prototypes/kmp-music-desktop-uiux/index.html.
 */
object DesktopMusicColors {
    val Accent: Color = Color(0xFF17B59E)
    val AccentDeep: Color = Color(0xFF0FA890)
    val AccentSoft: Color = Color(0xFFE8F8F5)
    val Ink: Color = Color(0xFF07090C)
    val Muted: Color = Color(0xFF7F8A99)
    val MutedStrong: Color = Color(0xFF64707E)
    val Line: Color = Color(0xFFE7ECF0)
    val Paper: Color = Color(0xFFFBFCFD)
    val Soft: Color = Color(0xFFF4F7F8)
    val PlayerRed: Color = Color(0xFFEF3F42)
    val WindowBackground: Color = Color(0xFFEEF2F5)
}

/**
 * Desktop UI dimensions copied from the HTML prototype.
 */
object DesktopMusicDimens {
    val MinWindowWidth: Dp = 1120.dp
    val MinWindowHeight: Dp = 760.dp
    val DefaultWindowWidth: Dp = 1240.dp
    val DefaultWindowHeight: Dp = 820.dp
    val TitleBarHeight: Dp = 42.dp
    val RailWidth: Dp = 88.dp
    val PlayerHeight: Dp = 96.dp
    val PagePaddingTop: Dp = 34.dp
    val PagePaddingBottom: Dp = 30.dp
    val PagePaddingMinHorizontal: Dp = 34.dp
    val PagePaddingMaxHorizontal: Dp = 68.dp
    val RailItemSize: Dp = 64.dp
    val BrandSize: Dp = 40.dp
    val PrimaryButtonHeight: Dp = 40.dp
    val StatCardMinHeight: Dp = 76.dp
    val TableHeaderHeight: Dp = 40.dp
    val TableRowHeight: Dp = 48.dp
    val TableCoverSize: Dp = 34.dp
    val AlbumMinWidth: Dp = 120.dp
    val SettingNavWidth: Dp = 210.dp
    val PlayerTrackColumnWidth: Dp = 310.dp
    val PlayerActionsColumnWidth: Dp = 330.dp
}

object DesktopMusicType {
    val AppTitle: TextUnit = 13.sp
    val PageTitle: TextUnit = 36.sp
    val Eyebrow: TextUnit = 14.sp
    val Body: TextUnit = 13.sp
    val StatTitle: TextUnit = 15.sp
    val RailLabel: TextUnit = 12.sp
}

@Composable
fun desktopPageHorizontalPadding(width: Dp): Dp {
    val dynamicPadding = width * 0.04f
    return dynamicPadding.coerceIn(
        minimumValue = DesktopMusicDimens.PagePaddingMinHorizontal,
        maximumValue = DesktopMusicDimens.PagePaddingMaxHorizontal,
    )
}
```

- [x] **Step 2: Create a compile-safe Desktop shell stub**

Create `DesktopMusicApp.kt` with this minimal shell. It intentionally renders only the main grid and a smoke text so the window can switch away from the mobile surface before full UI components are added.

```kotlin
package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.core.theme.KmpMusicTheme
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * Desktop-only app surface. Mobile Android/iOS continue to call MusicApp.
 */
@Composable
fun DesktopMusicApp(
    controller: MusicAppController,
) {
    KmpMusicTheme(themeMode = controller.uiState.themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DesktopMusicColors.WindowBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "KMP Music Desktop",
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
```

- [x] **Step 3: Route DesktopMain to the Desktop shell**

Modify `DesktopMain.kt` to import `DesktopMusicApp`, use Desktop window dimensions, and stop calling mobile `App`:

```kotlin
package com.yanhao.kmpmusic

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicApp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens

/**
 * Desktop 入口。
 */
fun main() = application {
    Window(
        onCloseRequest = {
            DesktopPlaybackSession.close()
            exitApplication()
        },
        title = "KMP Music",
        state = WindowState(
            width = DesktopMusicDimens.DefaultWindowWidth,
            height = DesktopMusicDimens.DefaultWindowHeight,
        ),
    ) {
        LaunchedEffect(Unit) {
            DesktopPlaybackSession.ensurePlaybackSnapshotRestoreRequested()
        }
        DesktopMusicApp(controller = DesktopPlaybackSession.controller)
    }
}
```

- [x] **Step 4: Compile Desktop Kotlin**

Run:

```bash
./gradlew :composeApp:compileKotlinDesktop
```

Expected: build succeeds. The Desktop target is declared as `jvm("desktop")` in `composeApp/build.gradle.kts`, so the compile task is `:composeApp:compileKotlinDesktop`.

- [x] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicTokens.kt \
  composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt \
  composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopMain.kt
git commit -m "添加桌面版 UI 入口与视觉 token"
```

---

### Task 2: Desktop Shell Components

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`

- [x] **Step 1: Add reusable shell components**

Create `DesktopMusicComponents.kt` with the title bar, rail, page header, buttons, and stat cards:

```kotlin
package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.feature.app.RootTab

enum class DesktopRailDestination {
    Home,
    Favorites,
    Me,
    Settings,
}

@Composable
fun DesktopTitleBar(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.TitleBarHeight)
            .background(Color(0xB8F7F9FB))
            .border(width = 1.dp, color = Color(0xB8C7CFD6)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.width(DesktopMusicDimens.RailWidth).padding(start = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TrafficLight(color = Color(0xFFFF5F57))
            TrafficLight(color = Color(0xFFFEBC2E))
            TrafficLight(color = Color(0xFF28C840))
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = "KMP Music",
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.AppTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Surface(
            modifier = Modifier
                .width(520.dp)
                .height(30.dp)
                .padding(end = 18.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.84f),
            border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = Color(0xFFD7DDE3)),
            onClick = onSearch,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF8A95A3), modifier = Modifier.size(16.dp))
                Text(
                    text = "搜索歌曲、专辑、歌手",
                    color = Color(0xFF8A95A3),
                    fontSize = DesktopMusicType.Body,
                )
            }
        }
    }
}

@Composable
private fun TrafficLight(color: Color) {
    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
}

@Composable
fun DesktopRail(
    activeDestination: DesktopRailDestination,
    onRootTab: (RootTab) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(DesktopMusicDimens.RailWidth)
            .fillMaxHeight()
            .background(Color(0xB3F8FBFC))
            .border(width = 1.dp, color = DesktopMusicColors.Line)
            .padding(top = 20.dp, start = 12.dp, end = 12.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(DesktopMusicDimens.BrandSize)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1DC6AD), Color(0xFF0CA58F)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "♪", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(30.dp))
        DesktopRailItem(DesktopRailDestination.Home, activeDestination, Icons.Rounded.Home, "首页") {
            onRootTab(RootTab.Home)
        }
        DesktopRailItem(DesktopRailDestination.Favorites, activeDestination, Icons.Rounded.Favorite, "收藏") {
            onRootTab(RootTab.Favorites)
        }
        DesktopRailItem(DesktopRailDestination.Me, activeDestination, Icons.Rounded.Person, "我的") {
            onRootTab(RootTab.Me)
        }
        DesktopRailItem(DesktopRailDestination.Settings, activeDestination, Icons.Rounded.Settings, "设置", onSettings)
    }
}

@Composable
private fun DesktopRailItem(
    destination: DesktopRailDestination,
    activeDestination: DesktopRailDestination,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val isActive = destination == activeDestination
    Surface(
        modifier = Modifier.size(DesktopMusicDimens.RailItemSize).padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) DesktopMusicColors.Accent.copy(alpha = 0.10f) else Color.Transparent,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) DesktopMusicColors.Accent else DesktopMusicColors.MutedStrong,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                color = if (isActive) DesktopMusicColors.Accent else DesktopMusicColors.MutedStrong,
                fontSize = DesktopMusicType.RailLabel,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun DesktopPageHeader(
    title: String,
    eyebrow: String,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.PageTitle,
                lineHeight = DesktopMusicType.PageTitle,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = eyebrow,
                color = DesktopMusicColors.Muted,
                fontSize = DesktopMusicType.Eyebrow,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 7.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}

@Composable
fun DesktopPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(DesktopMusicDimens.PrimaryButtonHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(Color(0xFF1AC0A8), DesktopMusicColors.AccentDeep)))
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, color = Color.White, fontSize = DesktopMusicType.Eyebrow, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DesktopStatCard(
    icon: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(DesktopMusicDimens.StatCardMinHeight),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.64f),
        border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Text(text = icon, color = Color(0xFF303845), fontSize = 24.sp)
            }
            Column {
                Text(text = title, color = DesktopMusicColors.Ink, fontSize = DesktopMusicType.StatTitle, fontWeight = FontWeight.Bold)
                Text(text = value, color = Color(0xFF5E6A78), fontSize = DesktopMusicType.Eyebrow)
            }
        }
    }
}
```

After creating the file, verify the yellow traffic light line is exactly:

```kotlin
TrafficLight(color = Color(0xFFFEBC2E))
```

- [x] **Step 2: Replace shell stub with the Desktop grid**

Modify `DesktopMusicApp.kt` to render the fixed title bar, rail, workspace, and player slot:

```kotlin
@Composable
fun DesktopMusicApp(
    controller: MusicAppController,
) {
    val state = controller.uiState
    KmpMusicTheme(themeMode = state.themeMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DesktopMusicColors.WindowBackground),
        ) {
            DesktopTitleBar(onSearch = controller::openSearch)
            Row(modifier = Modifier.weight(1f)) {
                DesktopRail(
                    activeDestination = state.desktopRailDestination(),
                    onRootTab = controller::navigateToRoot,
                    onSettings = { controller.navigateToSecondary(SecondaryScreen.Settings) },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(DesktopMusicColors.Paper),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Workspace", color = DesktopMusicColors.Muted)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DesktopMusicDimens.PlayerHeight)
                    .background(Color.White.copy(alpha = 0.86f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Desktop Player", color = DesktopMusicColors.Muted)
            }
        }
    }
}

private fun MusicAppUiState.desktopRailDestination(): DesktopRailDestination {
    return when (navigationState.secondaryScreen) {
        SecondaryScreen.Settings -> DesktopRailDestination.Settings
        else -> when (navigationState.rootTab) {
            RootTab.Home -> DesktopRailDestination.Home
            RootTab.Favorites -> DesktopRailDestination.Favorites
            RootTab.Me -> DesktopRailDestination.Me
        }
    }
}
```

Add imports required by this replacement:

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
```

- [x] **Step 3: Compile**

Run:

```bash
./gradlew :composeApp:compileKotlinDesktop
```

Expected: build succeeds and no Android/iOS files are modified.

- [x] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt \
  composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt
git commit -m "实现桌面版主框架组件"
```

---

### Task 3: Desktop Player And Controller State Tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicPlayer.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

- [ ] **Step 1: Add controller test for rail root navigation**

Append this test to `MusicAppControllerTest`:

```kotlin
@Test
fun desktopRailRootNavigationClearsSecondaryScreen(): Unit {
    val controller = createController()
    controller.navigateToSecondary(screen = SecondaryScreen.Search)
    assertFalse(controller.uiState.navigationState.isTopLevel)

    controller.navigateToRoot(tab = RootTab.Favorites)

    assertTrue(controller.uiState.navigationState.isTopLevel)
    assertEquals(expected = RootTab.Favorites, actual = controller.uiState.navigationState.rootTab)
    assertNull(controller.uiState.navigationState.secondaryScreen)
}
```

- [ ] **Step 2: Add controller test for player/detail shared state**

Append this test to `MusicAppControllerTest`:

```kotlin
@Test
fun playerScreenAndBottomPlayerReadSamePlaybackState(): Unit = runBlocking {
    val controller = createController()
    controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
    val targetSong = controller.uiState.homeLocalSongPreview.first()

    controller.playSong(song = targetSong)
    controller.openPlayer()

    assertEquals(expected = SecondaryScreen.Player, actual = controller.uiState.navigationState.secondaryScreen)
    assertEquals(expected = targetSong.id, actual = controller.uiState.currentSongId)
    assertTrue(controller.uiState.isPlaying)

    controller.togglePlayback()

    assertEquals(expected = targetSong.id, actual = controller.uiState.currentSongId)
    assertFalse(controller.uiState.isPlaying)
}
```

- [ ] **Step 3: Run the focused tests**

Run:

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest"
```

Expected: all `MusicAppControllerTest` tests pass, including the two new tests.

- [ ] **Step 4: Create Desktop player component**

Create `DesktopMusicPlayer.kt`:

```kotlin
package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.coverArtPainter

@Composable
fun DesktopBottomPlayer(
    song: Song?,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onLike: (String) -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.PlayerHeight)
            .background(Color.White.copy(alpha = 0.86f))
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopPlayerTrack(song = song, onOpen = onOpen, onLike = onLike)
        DesktopPlayerControls(
            isPlaying = isPlaying,
            playbackPositionMs = playbackPositionMs,
            playbackDurationMs = playbackDurationMs,
            onToggle = onToggle,
            onPrev = onPrev,
            onNext = onNext,
            onMode = onMode,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.width(DesktopMusicDimens.PlayerActionsColumnWidth),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "♩", fontSize = DesktopMusicType.PageTitle, color = DesktopMusicColors.Ink)
            Box(modifier = Modifier.width(118.dp).height(4.dp).clip(CircleShape).background(DesktopMusicColors.Accent))
            IconButton(onClick = onQueue) {
                Icon(Icons.Rounded.QueueMusic, contentDescription = "播放队列", tint = DesktopMusicColors.Ink)
            }
        }
    }
}

@Composable
private fun DesktopPlayerTrack(
    song: Song?,
    onOpen: () -> Unit,
    onLike: (String) -> Unit,
) {
    Row(
        modifier = Modifier.width(DesktopMusicDimens.PlayerTrackColumnWidth).fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (song != null) {
            Image(
                painter = coverArtPainter(song.coverArt),
                contentDescription = "${song.title} 封面",
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = DesktopMusicColors.Ink,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    color = DesktopMusicColors.Muted,
                    fontSize = DesktopMusicType.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { onLike(song.id) }) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (song.isLiked) "取消收藏" else "收藏",
                    tint = if (song.isLiked) DesktopMusicColors.PlayerRed else DesktopMusicColors.Ink,
                )
            }
        } else {
            Box(modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)).background(DesktopMusicColors.Soft))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "暂无播放", color = DesktopMusicColors.Ink, fontWeight = FontWeight.Bold)
                Text(text = "播放一首本地歌曲后会显示在这里", color = DesktopMusicColors.Muted, fontSize = DesktopMusicType.Body)
            }
        }
    }
}

@Composable
private fun DesktopPlayerControls(
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMode) {
            Icon(Icons.Rounded.Repeat, contentDescription = "播放模式", tint = DesktopMusicColors.Accent)
        }
        IconButton(onClick = onPrev) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首", tint = DesktopMusicColors.Ink)
        }
        Surface(modifier = Modifier.size(58.dp), shape = CircleShape, color = DesktopMusicColors.Ink, onClick = onToggle) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "下一首", tint = DesktopMusicColors.Ink)
        }
        DesktopProgressText(positionMs = playbackPositionMs, durationMs = playbackDurationMs)
    }
}

@Composable
private fun DesktopProgressText(
    positionMs: Long,
    durationMs: Long?,
) {
    Text(
        text = "${formatTime(positionMs)} / ${formatTime(durationMs ?: 0L)}",
        color = DesktopMusicColors.Ink,
        fontSize = DesktopMusicType.Body,
    )
}

private fun formatTime(valueMs: Long): String {
    val totalSeconds = (valueMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(length = 2, padChar = '0')}"
}
```

- [ ] **Step 5: Wire player into DesktopMusicApp**

Replace the temporary player `Box` in `DesktopMusicApp.kt` with:

```kotlin
DesktopBottomPlayer(
    song = state.currentSong,
    isPlaying = state.isPlaying,
    playbackPositionMs = state.playbackPositionMs,
    playbackDurationMs = state.playbackDurationMs,
    onOpen = controller::openPlayer,
    onToggle = controller::togglePlayback,
    onPrev = { controller.moveTrack(direction = -1) },
    onNext = { controller.moveTrack(direction = 1) },
    onMode = controller::cyclePlaybackMode,
    onLike = controller::toggleFavorite,
    onQueue = controller::openQueue,
)
```

- [ ] **Step 6: Run tests and compile**

Run:

```bash
./gradlew :composeApp:desktopTest :composeApp:compileKotlinDesktop
```

Expected: tests and Desktop compile pass.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicPlayer.kt \
  composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt \
  composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "接入桌面播放器与状态测试"
```

---

### Task 4: Desktop Root Pages

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`

- [ ] **Step 1: Add table, album, segmented, and settings components**

Extend `DesktopMusicComponents.kt` with these components:

```kotlin
@Composable
fun DesktopSegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .border(width = 1.dp, color = Color(0xFFD4DDE3), shape = RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            Surface(
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (index == selectedIndex) DesktopMusicColors.AccentSoft else Color.Transparent,
                onClick = { onSelect(index) },
            ) {
                Box(modifier = Modifier.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        color = if (index == selectedIndex) DesktopMusicColors.AccentDeep else Color(0xFF303A46),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopSongTable(
    songs: List<Song>,
    currentSongId: String?,
    showFavoriteColumn: Boolean,
    trailingDateLabel: String,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: ((String) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DesktopSongTableHeader(showFavoriteColumn = showFavoriteColumn, trailingDateLabel = trailingDateLabel)
        songs.forEachIndexed { index, song ->
            DesktopSongTableRow(
                index = index,
                song = song,
                songs = songs,
                isCurrentSong = song.id == currentSongId,
                showFavoriteColumn = showFavoriteColumn,
                trailingDateLabel = trailingDateLabel,
                onSongOpen = onSongOpen,
                onSongPlay = onSongPlay,
                onCurrentSongToggle = onCurrentSongToggle,
                onMore = onMore,
                onLike = onLike,
            )
        }
    }
}
```

Add required imports:

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.coverArtPainter
```

Then add private header/row helpers with the exact table rules:

```kotlin
@Composable
private fun DesktopSongTableHeader(
    showFavoriteColumn: Boolean,
    trailingDateLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(DesktopMusicDimens.TableHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFavoriteColumn) Text("", modifier = Modifier.width(36.dp))
        Text("#", modifier = Modifier.width(42.dp), color = Color(0xFF7D8795), fontSize = DesktopMusicType.Body, fontWeight = FontWeight.SemiBold)
        Text("标题", modifier = Modifier.weight(2.4f), color = Color(0xFF7D8795), fontSize = DesktopMusicType.Body, fontWeight = FontWeight.SemiBold)
        Text("歌手", modifier = Modifier.weight(1.2f), color = Color(0xFF7D8795), fontSize = DesktopMusicType.Body, fontWeight = FontWeight.SemiBold)
        Text("专辑", modifier = Modifier.weight(1.2f), color = Color(0xFF7D8795), fontSize = DesktopMusicType.Body, fontWeight = FontWeight.SemiBold)
        Text("时长", modifier = Modifier.width(72.dp), color = Color(0xFF7D8795), fontSize = DesktopMusicType.Body, fontWeight = FontWeight.SemiBold)
        Text(trailingDateLabel, modifier = Modifier.width(98.dp), color = Color(0xFF7D8795), fontSize = DesktopMusicType.Body, fontWeight = FontWeight.SemiBold)
        Text("", modifier = Modifier.width(40.dp))
    }
}

@Composable
private fun DesktopSongTableRow(
    index: Int,
    song: Song,
    songs: List<Song>,
    isCurrentSong: Boolean,
    showFavoriteColumn: Boolean,
    trailingDateLabel: String,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: ((String) -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.TableRowHeight)
            .background(if (isCurrentSong) DesktopMusicColors.Accent.copy(alpha = 0.10f) else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFavoriteColumn) {
            Text(
                text = if (song.isLiked) "♥" else "♡",
                modifier = Modifier.width(36.dp).clickable { onLike?.invoke(song.id) },
                color = if (song.isLiked) DesktopMusicColors.PlayerRed else DesktopMusicColors.Muted,
            )
        }
        Text(text = (index + 1).toString(), modifier = Modifier.width(42.dp), color = DesktopMusicColors.Muted, fontSize = DesktopMusicType.Body)
        Row(
            modifier = Modifier.weight(2.4f).clickable { onSongOpen(song, songs) },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = coverArtPainter(song.coverArt),
                contentDescription = "${song.title} 封面",
                modifier = Modifier.size(DesktopMusicDimens.TableCoverSize).clip(RoundedCornerShape(7.dp)),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = song.title,
                color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopMusicColors.Ink,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(song.artist, modifier = Modifier.weight(1.2f), color = DesktopMusicColors.Ink, fontSize = DesktopMusicType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.album, modifier = Modifier.weight(1.2f), color = DesktopMusicColors.Ink, fontSize = DesktopMusicType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.duration, modifier = Modifier.width(72.dp), color = DesktopMusicColors.Muted, fontSize = DesktopMusicType.Body)
        Text(if (trailingDateLabel == "收藏时间") "最近收藏" else "最近添加", modifier = Modifier.width(98.dp), color = DesktopMusicColors.Muted, fontSize = DesktopMusicType.Body)
        Surface(modifier = Modifier.size(30.dp), shape = RoundedCornerShape(9.dp), color = Color.Transparent, onClick = { onMore(song) }) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "•••", color = Color(0xFF475364), fontWeight = FontWeight.Bold)
            }
        }
    }
}
```

- [ ] **Step 2: Create Desktop root screens**

Create `DesktopMusicScreens.kt` with root screens:

```kotlin
package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.FavoriteSection

@Composable
fun DesktopLocalMusicRootScreen(
    songs: List<Song>,
    albums: List<Album>,
    libraryStats: LibraryStats,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onScan: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DesktopPageHeader(
            title = "本地音乐",
            eyebrow = "已扫描 ${libraryStats.songCount} 首歌曲，${libraryStats.albumCount} 张专辑，${libraryStats.artistCount} 位歌手",
        ) {
            DesktopPrimaryButton(text = "↻ 重新扫描", onClick = onScan)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            DesktopStatCard(icon = "♫", title = "歌曲", value = libraryStats.songCount.toString(), modifier = Modifier.weight(1f))
            DesktopStatCard(icon = "●", title = "专辑", value = libraryStats.albumCount.toString(), modifier = Modifier.weight(1f))
            DesktopStatCard(icon = "♟", title = "歌手", value = libraryStats.artistCount.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(22.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DesktopPrimaryButton(text = "▶ 播放全部", onClick = { songs.firstOrNull()?.let { onSongPlay(it, songs) } })
        }
        DesktopSongTable(
            songs = songs,
            currentSongId = currentSongId,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = onCurrentSongToggle,
            onMore = onMore,
        )
    }
}
```

Add `DesktopFavoritesRootScreen`, `DesktopMeRootScreen`, and `DesktopEmptyStateScreen` in the same file:

```kotlin
@Composable
fun DesktopFavoritesRootScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    section: FavoriteSection,
    currentSongId: String?,
    onSection: (FavoriteSection) -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    val likedSongs = songs.filter { it.isLiked }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DesktopPageHeader(title = "收藏", eyebrow = "喜欢的音乐都在这里") {
            DesktopSegmentedControl(
                labels = listOf("歌曲", "专辑", "歌手"),
                selectedIndex = section.ordinal,
                onSelect = { onSection(FavoriteSection.entries[it]) },
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            DesktopStatCard(icon = "♫", title = "收藏歌曲", value = likedSongs.size.toString(), modifier = Modifier.weight(1f))
            DesktopStatCard(icon = "●", title = "收藏专辑", value = albums.size.toString(), modifier = Modifier.weight(1f))
            DesktopStatCard(icon = "♟", title = "收藏歌手", value = artists.size.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(22.dp))
        DesktopSongTable(
            songs = likedSongs,
            currentSongId = currentSongId,
            showFavoriteColumn = true,
            trailingDateLabel = "收藏时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = onCurrentSongToggle,
            onMore = onMore,
            onLike = onLike,
        )
    }
}

@Composable
fun DesktopMeRootScreen(
    albums: List<Album>,
    artists: List<Artist>,
    libraryStats: LibraryStats,
    favoriteCount: Int,
    onLogin: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DesktopPageHeader(title = "我的", eyebrow = "本地资料与同步状态")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            DesktopStatCard(icon = "♫", title = "本地专辑", value = libraryStats.albumCount.toString(), modifier = Modifier.weight(1f))
            DesktopStatCard(icon = "●", title = "歌手", value = libraryStats.artistCount.toString(), modifier = Modifier.weight(1f))
            DesktopStatCard(icon = "♥", title = "收藏", value = favoriteCount.toString(), modifier = Modifier.weight(1f))
            DesktopStatCard(icon = "◷", title = "最近播放", value = "最近", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(18.dp))
        DesktopPrimaryButton(text = "✓ 立即登录", onClick = onLogin)
        Spacer(modifier = Modifier.height(18.dp))
        DesktopPrimaryButton(text = "设置", onClick = onSettings)
    }
}

@Composable
fun DesktopEmptyStateScreen(
    title: String,
    subtitle: String,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DesktopPageHeader(title = title, eyebrow = subtitle)
    }
}
```

- [ ] **Step 3: Wire root screens into DesktopMusicApp**

In `DesktopMusicApp.kt`, replace the temporary workspace `Text` with a `DesktopWorkspace` composable call:

```kotlin
DesktopWorkspace(
    state = state,
    controller = controller,
    onScanLocalMusic = scanLocalMusic,
)
```

Add `scanLocalMusic` before the theme:

```kotlin
val coroutineScope = rememberCoroutineScope()
val scanLocalMusic: () -> Unit = {
    coroutineScope.launch {
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
    }
}
```

Add imports:

```kotlin
import androidx.compose.runtime.rememberCoroutineScope
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import kotlinx.coroutines.launch
```

Create `DesktopWorkspace` in `DesktopMusicApp.kt`:

```kotlin
@Composable
private fun DesktopWorkspace(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val horizontalPadding = desktopPageHorizontalPadding(maxWidth)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = horizontalPadding,
                    top = DesktopMusicDimens.PagePaddingTop,
                    end = horizontalPadding,
                    bottom = DesktopMusicDimens.PagePaddingBottom,
                ),
        ) {
            if (state.navigationState.secondaryScreen == null) {
                when (state.navigationState.rootTab) {
                    RootTab.Home -> DesktopLocalMusicRootScreen(
                        songs = state.songs,
                        albums = state.albums,
                        libraryStats = state.libraryStats,
                        currentSongId = state.currentSongId,
                        currentPlaybackStatus = state.playbackStatus,
                        onScan = onScanLocalMusic,
                        onSongOpen = { song, queueSongs -> controller.openSong(song = song, queueSongs = queueSongs) },
                        onSongPlay = { song, queueSongs -> controller.playSong(song = song, queueSongs = queueSongs) },
                        onCurrentSongToggle = controller::togglePlayback,
                        onMore = controller::openMore,
                        onAlbumOpen = controller::openAlbum,
                    )
                    RootTab.Favorites -> DesktopFavoritesRootScreen(
                        songs = state.favoriteSongs,
                        albums = state.favoriteAlbums,
                        artists = state.favoriteArtists,
                        section = state.favoriteSection,
                        currentSongId = state.currentSongId,
                        onSection = controller::setFavoriteSection,
                        onSongOpen = { song, queueSongs -> controller.openSong(song = song, queueSongs = queueSongs) },
                        onSongPlay = { song, queueSongs -> controller.playSong(song = song, queueSongs = queueSongs) },
                        onCurrentSongToggle = controller::togglePlayback,
                        onMore = controller::openMore,
                        onLike = controller::toggleFavorite,
                    )
                    RootTab.Me -> DesktopMeRootScreen(
                        albums = state.albums,
                        artists = state.artists,
                        libraryStats = state.libraryStats,
                        favoriteCount = state.likedSongIds.size,
                        onLogin = { controller.navigateToSecondary(SecondaryScreen.Login) },
                        onSettings = { controller.navigateToSecondary(SecondaryScreen.Settings) },
                    )
                }
            } else {
                DesktopEmptyStateScreen(title = "二级页面", subtitle = "下一任务接入桌面二级页")
            }
        }
    }
}
```

- [ ] **Step 4: Compile**

Run:

```bash
./gradlew :composeApp:compileKotlinDesktop
```

Expected: Desktop compile passes and root pages render through Desktop shell.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt \
  composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt \
  composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt
git commit -m "复刻桌面版首页收藏我的页面"
```

---

### Task 5: Desktop Secondary Pages And Overlays

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`

- [ ] **Step 1: Expose mobile overlay composable safely**

In `MusicApp.kt`, keep the existing `@Composable` annotation and function body unchanged. Replace this function declaration line:

```kotlin
private fun AppOverlays(
```

with:

```kotlin
internal fun AppOverlays(
```

Do not change the parameters or function body.

- [ ] **Step 2: Add Desktop secondary route function**

In `DesktopMusicScreens.kt`, add:

```kotlin
@Composable
fun DesktopSecondaryScreen(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
) {
    when (state.navigationState.secondaryScreen) {
        SecondaryScreen.Search -> DesktopSearchScreen(
            query = state.searchQuery,
            resultSongs = controller.search().songs,
            currentSongId = state.currentSongId,
            onQuery = controller::setSearchQuery,
            onBack = controller::navigateBack,
            onSongOpen = { song, queueSongs -> controller.openSong(song = song, queueSongs = queueSongs) },
            onSongPlay = { song, queueSongs -> controller.playSong(song = song, queueSongs = queueSongs) },
            onMore = controller::openMore,
        )
        SecondaryScreen.Player -> DesktopPlayerDetailScreen(
            song = state.currentSong,
            isPlaying = state.isPlaying,
            onBack = controller::navigateBack,
            onToggle = controller::togglePlayback,
            onPrev = { controller.moveTrack(direction = -1) },
            onNext = { controller.moveTrack(direction = 1) },
            onQueue = controller::openQueue,
        )
        SecondaryScreen.AlbumDetail -> DesktopAlbumDetailScreen(
            album = state.selectedAlbum,
            songs = state.localSongs,
            currentSongId = state.currentSongId,
            onBack = controller::navigateBack,
            onSongOpen = { song, queueSongs -> controller.openSong(song = song, queueSongs = queueSongs) },
            onSongPlay = { song, queueSongs -> controller.playSong(song = song, queueSongs = queueSongs) },
            onMore = controller::openMore,
        )
        SecondaryScreen.ArtistDetail -> DesktopArtistDetailScreen(
            artist = state.selectedArtist,
            songs = state.localSongs,
            albums = state.localAlbums,
            currentSongId = state.currentSongId,
            onBack = controller::navigateBack,
            onSongOpen = { song, queueSongs -> controller.openSong(song = song, queueSongs = queueSongs) },
            onSongPlay = { song, queueSongs -> controller.playSong(song = song, queueSongs = queueSongs) },
            onMore = controller::openMore,
        )
        SecondaryScreen.Settings -> DesktopSettingsScreen(
            themeMode = state.themeMode,
            onThemeMode = controller::setThemeMode,
            onBack = controller::navigateBack,
            onScan = onScanLocalMusic,
            onLocalMusicSources = { controller.openLocalMusic(section = LocalMusicSection.Sources) },
            onClearCache = controller::openClearCacheDialog,
        )
        SecondaryScreen.Login -> DesktopLoginScreen(
            email = state.email,
            isMailSent = state.isMailSent,
            onEmail = controller::setEmail,
            onSend = controller::sendLoginMail,
            onBack = controller::navigateBack,
        )
        is SecondaryScreen.LocalMusic -> DesktopLocalSourcesScreen(
            songs = state.localSongs,
            sources = state.localMusicSources,
            problems = state.localMusicProblems,
            currentSongId = state.currentSongId,
            onBack = controller::navigateBack,
            onScan = onScanLocalMusic,
            onSongOpen = { song, queueSongs -> controller.openSong(song = song, queueSongs = queueSongs) },
            onSongPlay = { song, queueSongs -> controller.playSong(song = song, queueSongs = queueSongs) },
            onMore = controller::openMore,
        )
        null -> DesktopEmptyStateScreen(title = "本地音乐", subtitle = "桌面首页")
    }
}
```

Add imports for `MusicAppUiState`, `MusicAppController`, `SecondaryScreen`, `LocalMusicSection`, `ThemeMode`, `LocalMusicSourceSummary`, and `LocalMusicProblem`.

- [ ] **Step 3: Implement concise Desktop secondary screens**

Add concrete functions in `DesktopMusicScreens.kt`. Use Desktop tables/cards and avoid mobile components:

```kotlin
@Composable
private fun DesktopSearchScreen(
    query: String,
    resultSongs: List<Song>,
    currentSongId: String?,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DesktopPageHeader(title = "搜索", eyebrow = "搜索歌曲、专辑、歌手") {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
        }
        DesktopSongTable(
            songs = resultSongs,
            currentSongId = currentSongId,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = {},
            onMore = onMore,
        )
    }
}
```

Add `DesktopPlayerDetailScreen`, `DesktopAlbumDetailScreen`, `DesktopArtistDetailScreen`, `DesktopSettingsScreen`, `DesktopLoginScreen`, `DesktopLocalSourcesScreen`, and `DesktopEmptyStateScreen` in the same file:

```kotlin
@Composable
private fun DesktopPlayerDetailScreen(
    song: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onQueue: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DesktopPageHeader(title = song?.title ?: "暂无播放", eyebrow = song?.artist ?: "播放一首本地歌曲后会显示详情") {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
            DesktopPrimaryButton(text = if (isPlaying) "暂停" else "播放", onClick = onToggle)
            DesktopPrimaryButton(text = "队列", onClick = onQueue)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            DesktopPrimaryButton(text = "上一首", onClick = onPrev)
            DesktopPrimaryButton(text = "下一首", onClick = onNext)
        }
    }
}

@Composable
private fun DesktopAlbumDetailScreen(
    album: Album?,
    songs: List<Song>,
    currentSongId: String?,
    onBack: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
) {
    val albumSongs = album?.let { selectedAlbum -> songs.filter { it.album == selectedAlbum.title } }.orEmpty()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DesktopPageHeader(title = album?.title ?: "专辑不可用", eyebrow = album?.artist ?: "没有找到专辑信息") {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
            DesktopPrimaryButton(text = "▶ 播放全部", onClick = { albumSongs.firstOrNull()?.let { onSongPlay(it, albumSongs) } })
        }
        DesktopSongTable(
            songs = albumSongs,
            currentSongId = currentSongId,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = {},
            onMore = onMore,
        )
    }
}

@Composable
private fun DesktopArtistDetailScreen(
    artist: Artist?,
    songs: List<Song>,
    albums: List<Album>,
    currentSongId: String?,
    onBack: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
) {
    val artistSongs = artist?.let { selectedArtist -> songs.filter { it.artist == selectedArtist.name } }.orEmpty()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DesktopPageHeader(title = artist?.name ?: "歌手不可用", eyebrow = "歌曲 ${artistSongs.size} 首，专辑 ${albums.size} 张") {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
        }
        DesktopSongTable(
            songs = artistSongs,
            currentSongId = currentSongId,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = {},
            onMore = onMore,
        )
    }
}

@Composable
private fun DesktopSettingsScreen(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onLocalMusicSources: () -> Unit,
    onClearCache: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DesktopPageHeader(title = "设置", eyebrow = "播放、扫描与显示偏好") {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
        }
        DesktopSegmentedControl(
            labels = ThemeMode.entries.map { it.name },
            selectedIndex = ThemeMode.entries.indexOf(themeMode),
            onSelect = { onThemeMode(ThemeMode.entries[it]) },
        )
        Spacer(modifier = Modifier.height(18.dp))
        DesktopPrimaryButton(text = "管理本地文件夹", onClick = onLocalMusicSources)
        Spacer(modifier = Modifier.height(12.dp))
        DesktopPrimaryButton(text = "重新扫描", onClick = onScan)
        Spacer(modifier = Modifier.height(12.dp))
        DesktopPrimaryButton(text = "清理缓存", onClick = onClearCache)
    }
}

@Composable
private fun DesktopLoginScreen(
    email: String,
    isMailSent: Boolean,
    onEmail: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DesktopPageHeader(title = "登录音乐账号", eyebrow = if (isMailSent) "登录邮件已发送" else "使用邮箱接收魔法链接") {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
            DesktopPrimaryButton(text = "发送登录邮件", onClick = onSend)
        }
    }
}

@Composable
private fun DesktopLocalSourcesScreen(
    songs: List<Song>,
    sources: List<LocalMusicSourceSummary>,
    problems: List<LocalMusicProblem>,
    currentSongId: String?,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DesktopPageHeader(title = "本地文件夹", eyebrow = "来源 ${sources.size} 个，问题 ${problems.size} 个") {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
            DesktopPrimaryButton(text = "重新扫描", onClick = onScan)
        }
        DesktopSongTable(
            songs = songs,
            currentSongId = currentSongId,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = {},
            onMore = onMore,
        )
    }
}

@Composable
fun DesktopEmptyStateScreen(
    title: String,
    subtitle: String,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DesktopPageHeader(title = title, eyebrow = subtitle)
    }
}
```

- [ ] **Step 4: Wire secondary pages and overlays**

In `DesktopMusicApp.kt`, replace the secondary empty-state branch with:

```kotlin
DesktopSecondaryScreen(
    state = state,
    controller = controller,
    onScanLocalMusic = onScanLocalMusic,
)
```

In `DesktopMusicApp.kt`, wrap the existing top-level `Column` inside a `Box(modifier = Modifier.fillMaxSize())`. Keep the current `Column` body exactly as implemented by previous tasks, then add the overlay call immediately after that `Column` inside the same `Box`:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // Existing Desktop app Column remains here.
    AppOverlays(state = state, controller = controller)
}
```

Add import:

```kotlin
import com.yanhao.kmpmusic.feature.app.AppOverlays
```

- [ ] **Step 5: Run tests and compile**

Run:

```bash
./gradlew :composeApp:desktopTest :composeApp:compileKotlinDesktop
```

Expected: tests and Desktop compile pass.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt \
  composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt \
  composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt
git commit -m "补齐桌面版二级页面与浮层"
```

---

### Task 6: Visual Verification And Final Polish

**Files:**
- Inspect and modify after visual verification: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicTokens.kt`
- Inspect and modify after visual verification: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicComponents.kt`
- Inspect and modify after visual verification: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicPlayer.kt`
- Inspect and modify after visual verification: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicScreens.kt`
- Inspect and modify after visual verification: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/DesktopMusicApp.kt`
- Inspect only unless overlay access fails: `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt`

- [ ] **Step 1: Run full Desktop verification**

Run:

```bash
./gradlew :composeApp:desktopTest :composeApp:compileKotlinDesktop
```

Expected: tests and Desktop compile pass.

- [ ] **Step 2: Launch Desktop app for visual check**

Run the Desktop application task configured in `composeApp/build.gradle.kts`:

```bash
./gradlew :composeApp:run
```

Expected: the macOS window opens at Desktop dimensions and no longer shows the 430dp mobile shell.

- [ ] **Step 3: Check three viewport sizes against the HTML**

Manually resize the Desktop app to:

- `1120 x 760`
- `1240 x 800`
- `1440 x 900`

For each size, verify:

- Title bar height is visually close to the HTML `42px` row.
- Rail is visually close to `88px`.
- Bottom player is visually close to `96px`.
- Homepage table row density matches the HTML.
- Favorites segmented control uses the same compact height and active color.
- Settings page uses a `210px` left category column.
- At smaller width, album grid reduces density and text does not overlap.

- [ ] **Step 4: Run Android compile to guard mobile breakage**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: Android compile passes. This is required because Desktop UI files are in `commonMain` but must not break Android/iOS compilation even though they are not called by mobile entry points.

- [ ] **Step 5: Final git status check**

Run:

```bash
git status --short --branch
```

Expected: only intentional Desktop UI files are modified. `prototypes/kmp-music-desktop-uiux/` may remain untracked if it was already untracked before implementation; do not add it unless the user explicitly asks to track the prototype.

- [ ] **Step 6: Commit final polish**

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop \
  composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicApp.kt \
  composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopMain.kt \
  composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "完成 macOS 桌面版 UI 复刻验收"
```

---

## Self-Review

### Spec Coverage

- Desktop-only shell and window sizing: Task 1 and Task 2.
- HTML visual tokens and density: Task 1, Task 2, Task 4, Task 6.
- Bottom player 1:1 structure and shared playback state: Task 3.
- Root pages for 本地音乐、收藏、我的、设置: Task 4.
- Secondary pages for 搜索、播放详情、专辑、歌手、本地来源、登录: Task 5.
- Overlay reuse and no copied business behavior: Task 5.
- Mobile UI unchanged: File structure section plus Task 6 Android compile.
- Prototype data/assets not copied: File structure and Task 5 constraints.
- Visual verification at three sizes: Task 6.

### Placeholder Scan

This plan contains no unresolved marker or unspecified task. Desktop compile uses `:composeApp:compileKotlinDesktop`; Desktop launch uses `:composeApp:run`.

### Type Consistency

- `DesktopMusicApp` consistently accepts `MusicAppController`.
- Desktop route selection consistently uses `MusicAppUiState.navigationState`.
- Rail root navigation consistently calls `navigateToRoot`.
- Settings navigation consistently calls `navigateToSecondary(SecondaryScreen.Settings)`.
- Song table callbacks consistently use `(Song, List<Song>)`.

## Three-Round Cross Audit

### Round 1: Executability Questions

- Question: Can an executor identify the exact files to create or modify before touching code?
  Answer: Yes. The File Structure section lists every created/modified file, and each task repeats the exact file set.
- Question: Can an executor run verification after each meaningful slice?
  Answer: Yes. Tasks 1, 2, and 4 run `:composeApp:compileKotlinDesktop`; Tasks 3 and 5 run `:composeApp:desktopTest :composeApp:compileKotlinDesktop`; Task 6 adds Desktop launch and Android compile.
- Question: Are there code snippets that obviously fail before implementation starts?
  Answer: Audited and corrected. The plan now uses valid `Color(0xFFFEBC2E)`, imports `ContentScale`, avoids the nonexistent RowScope extension import, uses `Icons.Rounded.QueueMusic`, and replaces the `AppOverlays` visibility line without using a fake function body.

### Round 2: Spec Coverage Questions

- Question: Where does the plan ensure Desktop is no longer the `430.dp x 930.dp` mobile shell?
  Answer: Task 1 changes `DesktopMain.kt` to `DesktopMusicDimens.DefaultWindowWidth` and `DefaultWindowHeight`, and Task 2 renders the Desktop grid.
- Question: Where does the plan preserve Android/iOS mobile UI?
  Answer: File Structure forbids Android/iOS and mobile `MusicApp` changes except `AppOverlays` visibility, and Task 6 requires `:composeApp:compileDebugKotlinAndroid`.
- Question: Where does the plan cover HTML root pages and secondary pages?
  Answer: Task 4 covers 本地音乐、收藏、我的 root pages; Task 5 covers 搜索、播放详情、专辑、歌手、设置、登录、本地来源.
- Question: Where does the plan prevent WebView/prototype asset shortcuts?
  Answer: Architecture states no WebView or prototype runtime assets; Task 5 secondary-screen instructions explicitly avoid `prototypes/kmp-music-desktop-uiux/assets/*`.

### Round 3: Verification And Risk Questions

- Question: How will executor know the plan did not silently copy fake HTML sample data?
  Answer: Root and secondary screen snippets read `MusicAppUiState`, `controller.search()`, `state.localSongs`, `state.favoriteSongs`, source summaries, and problems. They do not load HTML `songs`, `albums`, JS, or prototype `assets`.
- Question: How does the plan guard current playback state consistency?
  Answer: Task 3 adds `playerScreenAndBottomPlayerReadSamePlaybackState`, then wires `DesktopBottomPlayer` and player detail to the same `MusicAppUiState`.
- Question: How does the plan verify visual fidelity instead of accepting a compile-only result?
  Answer: Task 6 launches `:composeApp:run` and requires manual checks at `1120 x 760`, `1240 x 800`, and `1440 x 900`, including title bar, rail, bottom player, table density, segmented control, settings column, and text overlap.
- Question: What remains intentionally lower fidelity in the implementation plan?
  Answer: The plan implements a safe, functional Desktop skeleton first. Final pixel polish is explicitly reserved for Task 6 after visual launch, where the executor adjusts the Desktop files against the HTML reference and commits the result.
