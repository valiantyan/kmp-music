# MusicAppController 按用户工作流拆分实施计划

> **给 agent 执行者：** 必须使用 `subagent-driven-development`（推荐）或 `executing-plans` 按任务逐项执行。本计划使用复选框（`- [ ]`）跟踪步骤。

**目标：** 在保持 `MusicAppController` 唯一公开门面兼容的前提下，把内部实现按用户工作流拆成可测试协作者，并补齐扫描、播放、搜索、导航和加载上次播放数据的回归证据。

**架构：** `MusicAppController` 继续持有 Compose 可观察 `uiState`、装配依赖和发布公开方法；新协作者只承接清晰工作流，并通过返回新状态或提交同步归约函数改变状态。低副作用同步模块先拆，内容导航随后拆，扫描、播放动作和加载上次播放数据这些高副作用链路最后拆。

**技术栈：** Kotlin Multiplatform `2.4.0`、Compose Multiplatform `1.11.1`、AGP `8.13.2`、Kotlin coroutines `1.11.0`、Room3 `3.0.0-rc01`、Media3 `1.10.1`、vlcj `4.12.1`、`kotlin.test`、Gradle。

## 全局约束

- Android `compileSdk = 36`、`minSdk = 24`、`targetSdk = 36`，Android 和 Desktop JVM target 都保持 `17`。
- 主模块仍是 `:composeApp`，包名和 `applicationId` 仍是 `com.yanhao.kmpmusic`。
- 只修改生产 KMP App 和 OpenWiki，不修改 `prototypes/kmp-music-hi-fi` 来解决生产问题。
- 所有新增或修改的 Markdown 描述内容使用中文。
- `MusicAppController` 继续作为 UI、Android、Desktop 和 iOS 的唯一公开入口。
- 外部公开方法签名默认保持兼容；若必须改变公开签名，先暂停并单独确认。
- 新模块放在 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app` 的工作流分包下，不下沉到 `domain` 或 `data`。
- `commonMain` 不引入 Android、iOS 或 Desktop 专属 API。
- 拆分以等价迁移为原则，不借重构机会改变导航、播放、搜索、收藏、扫描或加载上次播放数据的用户可见行为。
- 修改 controller、导航、播放、队列、收藏或搜索时，更新或新增共享测试。
- 每次 `git commit` 前运行 `git status --short --branch`，确认没有构建产物、`.scratch/`、IDE 状态、日志、Node 依赖、原型 dist、APK/DMG 或本地缓存进入提交。
- 提交前至少运行 `./gradlew :composeApp:desktopTest` 和 `./gradlew :composeApp:compileDebugKotlinAndroid`；最终优先运行组合命令 `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`。

---

## 范围检查

本 spec 聚焦同一个子系统：`MusicAppController` 内部按用户工作流拆分。它涉及导航、搜索、播放、扫描和加载上次播放数据，但这些都是同一个门面控制器的职责切片，不需要拆成多个独立产品计划。

本计划不包含：

- 重写 `PlaybackCoordinator`、仓库、Room 持久化或平台播放实现。
- 引入新的状态管理框架。
- 改变移动端、桌面端或平台入口的调用方式。
- 改动原型目录。
- 删除现有 `MusicAppControllerTest` 回归网。

## 文件结构

- 修改 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`：保留公开门面，新增统一状态写入口，逐步把公开方法委派给工作流协作者。
- 创建 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/system/SystemBackController.kt`：系统返回优先级 reducer。
- 创建 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/preferences/PreferenceStateController.kt`：播放倍速和本地音频发现偏好保存与状态同步。
- 创建 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchResultController.kt`：搜索结果数据源选择和结果派生。
- 创建 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/navigation/ContentNavigationController.kt`：首页分段、本地音乐、扫描页、最近播放、专辑详情和歌手详情导航。
- 创建 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackActionController.kt`：播放动作、队列动作、进度、音量、播放模式和退出快照委派。
- 创建 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library/LocalMusicScanController.kt`：本地扫描会话、取消、权限确认和旧事件丢弃。
- 修改 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackRestoreOrchestrator.kt`：把裸布尔待加载状态改成带身份描述符的加载上次播放数据请求。
- 修改 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt`：新增保存快照身份描述符。
- 修改 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStore.kt`：新增读取保存快照身份的接口和内存、Room 实现。
- 创建 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/system/SystemBackControllerTest.kt`。
- 创建 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/preferences/PreferenceStateControllerTest.kt`。
- 创建 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchResultControllerTest.kt`。
- 创建 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/ContentNavigationControllerTest.kt`。
- 创建 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackActionControllerTest.kt`。
- 创建 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/LocalMusicScanControllerTest.kt`。
- 修改 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`：保留跨工作流回归测试，并补齐异步交错、搜索结果动作、扫描取消、加载上次播放数据和队列不变量证据。
- 修改 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackRestoreOrchestratorTest.kt`：覆盖保存快照身份、完整队列解析、重复加载保护和过期结果丢弃。
- 修改 `openwiki/architecture/app-architecture.md`：更新 App controller 内部工作流协作者地图。
- 修改 `openwiki/testing/verification-guide.md`：更新 controller 拆分后的高价值测试入口。

## 任务一：建立统一状态写入口和异步交错回归网

**文件：**

- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- 修改：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

**接口：**

- 消费：现有 `SearchSessionController` 的 `publishStateUpdate: ((MusicAppUiState) -> MusicAppUiState) -> Unit`。
- 产出：`MusicAppController` 私有函数 `reduceUiState(reducer: (MusicAppUiState) -> MusicAppUiState): Unit`，所有异步归约都经由它串行写回当前 `uiState`。

- [x] **步骤 1：写异步交错失败测试**

在 `MusicAppControllerTest` 的搜索防抖测试附近加入下面测试：

```kotlin
    /**
     * 防抖搜索醒来时必须基于最新 uiState 归约，不能覆盖期间到达的播放状态。
     */
    @Test
    fun debouncedSearchUpdatePreservesPlaybackStateChangedBeforeDebounce(): Unit = runTest {
        val controller = createController(controllerScope = backgroundScope)
        val queueSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 3)
        val firstSong: Song = queueSongs[0]
        val secondSong: Song = queueSongs[1]

        controller.openSearch(context = SearchContext.LocalLibrary)
        controller.setSearchQuery(query = "river")
        controller.playSong(song = firstSong, queueSongs = queueSongs)
        advanceUntilIdle()
        controller.playSong(song = secondSong, queueSongs = queueSongs)
        advanceUntilIdle()

        advanceTimeBy(delayTimeMillis = 301L)
        advanceUntilIdle()

        assertEquals(expected = "river", actual = controller.uiState.activeSearchQuery)
        assertEquals(expected = secondSong.id, actual = controller.uiState.currentSongId)
        assertEquals(
            expected = queueSongs.map { song: Song -> song.id },
            actual = controller.uiState.queueSongIds,
        )
        assertEquals(
            expected = queueSongs.map { song: Song -> song.id },
            actual = controller.uiState.queueSongs.map { song: Song -> song.id },
        )
    }
```

- [x] **步骤 2：运行测试确认红灯或现有行为未被锁住**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.debouncedSearchUpdatePreservesPlaybackStateChangedBeforeDebounce"
```

预期：如果当前实现已经使用最新状态归约，测试可能直接通过；若失败，失败应表现为 `currentSongId` 或 `queueSongIds` 被防抖搜索写回覆盖。无论红灯还是直接绿灯，都保留该测试作为后续拆分保护。

- [x] **步骤 3：在门面中新增统一状态写入口**

在 `MusicAppController` 的 `publishPlaybackUiState()` 后加入：

```kotlin
    /**
     * 所有异步协作者只能提交同步归约函数，由门面用最新 uiState 串行写回。
     */
    private fun reduceUiState(reducer: (MusicAppUiState) -> MusicAppUiState) {
        uiState = reducer(uiState)
    }
```

把 `SearchSessionController` 构造里的 `publishStateUpdate` 改为：

```kotlin
        publishStateUpdate = ::reduceUiState,
```

把播放和曲库同步入口改成下面形状：

```kotlin
    private fun syncPlaybackState(playbackState: PlaybackState) {
        reduceUiState { currentState: MusicAppUiState ->
            playbackUiStateSynchronizer.syncPlaybackState(
                state = currentState,
                playbackState = playbackState,
            )
        }
        publishPlaybackUiState()
    }

    private fun syncLibrarySnapshot(snapshot: LibrarySnapshot) {
        reduceUiState { currentState: MusicAppUiState ->
            libraryStateSynchronizer.syncLibrarySnapshot(
                state = currentState,
                snapshot = snapshot,
            )
        }
        restorePlaybackSnapshotIfPending()
    }
```

- [x] **步骤 4：运行任务一测试**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.debouncedSearchUpdatePreservesPlaybackStateChangedBeforeDebounce"
```

预期：通过。

- [x] **步骤 5：提交任务一**

运行：

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "重构 MusicAppController 状态写入口"
```

## 任务二：拆出系统返回工作流

**文件：**

- 创建：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/system/SystemBackController.kt`
- 创建：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/system/SystemBackControllerTest.kt`
- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`

**接口：**

- 消费：`MusicAppUiState`、`NavigationStateController.navigateBack(state: MusicAppUiState): MusicAppUiState`。
- 产出：`SystemBackController.handleSystemBack(state: MusicAppUiState): SystemBackController.Result`。

- [x] **步骤 1：写系统返回聚焦测试**

创建 `SystemBackControllerTest.kt`：

```kotlin
package com.yanhao.kmpmusic.feature.app.system

import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.app.navigation.NavigationStateController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SystemBackControllerTest {
    /**
     * 系统返回优先关闭权限弹窗，且不继续消费到二级页面。
     */
    @Test
    fun systemBackClosesPermissionDialogFirst(): Unit {
        val state: MusicAppUiState = NavigationStateController.navigateToSecondary(
            state = baseState().copy(
                isPermissionSettingsDialogOpen = true,
                isClearCacheDialogOpen = true,
                isQueueOpen = true,
                moreSongId = "song-1",
            ),
            screen = SecondaryScreen.Settings,
        )

        val result: SystemBackController.Result = SystemBackController.handleSystemBack(state = state)

        assertTrue(actual = result.wasHandled)
        assertFalse(actual = result.state.isPermissionSettingsDialogOpen)
        assertTrue(actual = result.state.isClearCacheDialogOpen)
        assertTrue(actual = result.state.isQueueOpen)
        assertTrue(actual = result.state.navigationState.secondaryScreen is SecondaryScreen.Settings)
    }

    /**
     * 没有弹窗和面板时，系统返回才回退二级页面。
     */
    @Test
    fun systemBackReturnsFromSecondaryWhenNoOverlayExists(): Unit {
        val state: MusicAppUiState = NavigationStateController.navigateToSecondary(
            state = baseState(),
            screen = SecondaryScreen.Settings,
        )

        val result: SystemBackController.Result = SystemBackController.handleSystemBack(state = state)

        assertTrue(actual = result.wasHandled)
        assertNull(actual = result.state.navigationState.secondaryScreen)
    }

    /**
     * 清缓存弹窗、单曲更多和队列都存在时，返回键每次只关闭当前最高优先级对象。
     */
    @Test
    fun systemBackClosesCacheDialogMorePanelAndQueueInOrder(): Unit {
        val state: MusicAppUiState = NavigationStateController.navigateToSecondary(
            state = baseState().copy(
                isClearCacheDialogOpen = true,
                moreSongId = "song-1",
                isQueueOpen = true,
            ),
            screen = SecondaryScreen.Settings,
        )

        val afterCacheDialog: SystemBackController.Result = SystemBackController.handleSystemBack(state = state)
        val afterMorePanel: SystemBackController.Result = SystemBackController.handleSystemBack(state = afterCacheDialog.state)
        val afterQueue: SystemBackController.Result = SystemBackController.handleSystemBack(state = afterMorePanel.state)

        assertTrue(actual = afterCacheDialog.wasHandled)
        assertFalse(actual = afterCacheDialog.state.isClearCacheDialogOpen)
        assertEquals(expected = "song-1", actual = afterCacheDialog.state.moreSongId)
        assertTrue(actual = afterMorePanel.wasHandled)
        assertNull(actual = afterMorePanel.state.moreSongId)
        assertTrue(actual = afterMorePanel.state.isQueueOpen)
        assertTrue(actual = afterQueue.wasHandled)
        assertFalse(actual = afterQueue.state.isQueueOpen)
        assertTrue(actual = afterQueue.state.navigationState.secondaryScreen is SecondaryScreen.Settings)
    }

    /**
     * 顶层页面没有可关闭对象时，系统返回不消费事件。
     */
    @Test
    fun systemBackDoesNotHandleTopLevelIdleState(): Unit {
        val result: SystemBackController.Result = SystemBackController.handleSystemBack(state = baseState())

        assertFalse(actual = result.wasHandled)
    }
}

private fun baseState(): MusicAppUiState {
    return MusicAppUiState(
        likedSongIds = emptySet(),
        currentSongId = null,
        playbackStatus = PlaybackStatus.Idle,
        queueSongIds = emptyList(),
    )
}
```

- [x] **步骤 2：运行测试确认红灯**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.system.SystemBackControllerTest"
```

预期：失败，错误包含 `Unresolved reference: SystemBackController`。

- [x] **步骤 3：实现 `SystemBackController`**

创建 `SystemBackController.kt`：

```kotlin
package com.yanhao.kmpmusic.feature.app.system

import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.navigation.NavigationStateController

/**
 * 系统返回键 reducer，集中维护弹窗、面板和二级页面的关闭优先级。
 */
object SystemBackController {
    /**
     * 系统返回处理结果。
     *
     * @property state 处理后的 UI 状态。
     * @property wasHandled 本次返回事件是否已被 App 消费。
     */
    data class Result(
        val state: MusicAppUiState,
        val wasHandled: Boolean,
    )

    /**
     * 按权限弹窗、清缓存弹窗、单曲更多面板、队列、二级页面的顺序处理返回。
     */
    fun handleSystemBack(state: MusicAppUiState): Result {
        if (state.isPermissionSettingsDialogOpen) {
            return Result(
                state = state.copy(isPermissionSettingsDialogOpen = false),
                wasHandled = true,
            )
        }
        if (state.isClearCacheDialogOpen) {
            return Result(
                state = state.copy(isClearCacheDialogOpen = false),
                wasHandled = true,
            )
        }
        if (state.moreSongId != null) {
            return Result(
                state = state.copy(moreSongId = null),
                wasHandled = true,
            )
        }
        if (state.isQueueOpen) {
            return Result(
                state = state.copy(isQueueOpen = false),
                wasHandled = true,
            )
        }
        if (!state.navigationState.isTopLevel) {
            return Result(
                state = NavigationStateController.navigateBack(state = state),
                wasHandled = true,
            )
        }
        return Result(
            state = state,
            wasHandled = false,
        )
    }
}
```

- [x] **步骤 4：让门面委派系统返回**

在 `MusicAppController.kt` 增加 import：

```kotlin
import com.yanhao.kmpmusic.feature.app.system.SystemBackController
```

把 `handleSystemBack()` 替换为：

```kotlin
    /**
     * 处理 Android 系统返回键，优先关闭临时浮层，最后才退出二级页面。
     */
    fun handleSystemBack(): Boolean {
        val result: SystemBackController.Result = SystemBackController.handleSystemBack(state = uiState)
        uiState = result.state
        return result.wasHandled
    }
```

- [x] **步骤 5：运行任务二测试和门面回归**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.system.SystemBackControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.systemBackClosesPermissionSettingsDialog" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.systemBackReturnsFromSecondaryScreen" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.systemBackClosesOverlayBeforeSecondaryScreen"
```

预期：全部通过。

- [x] **步骤 6：提交任务二**

运行：

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/system/SystemBackController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/system/SystemBackControllerTest.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt
git commit -m "拆分系统返回工作流"
```

## 任务三：拆出偏好设置工作流

**文件：**

- 创建：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/preferences/PreferenceStateController.kt`
- 创建：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/preferences/PreferenceStateControllerTest.kt`
- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`

**接口：**

- 消费：`UserPreferencesRepository`、`ThemeMode`、`LocalMusicDiscoveryPreferences`、`MusicAppUiState`。
- 产出：`PreferenceStateController.setThemeMode`、`setLocalMusicAutoScanOnLaunchEnabled`、`setLocalMusicShortAudioIgnored`、`setLocalMusicSystemFoldersExcluded`。

- [x] **步骤 1：写偏好聚焦测试**

创建 `PreferenceStateControllerTest.kt`：

```kotlin
package com.yanhao.kmpmusic.feature.app.preferences

import com.yanhao.kmpmusic.data.InMemoryUserPreferencesRepository
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreferenceStateControllerTest {
    /**
     * 本地发现偏好切换必须保留其他偏好字段。
     */
    @Test
    fun localMusicDiscoveryPreferenceUpdatesOnlyRequestedField(): Unit {
        val repository = InMemoryUserPreferencesRepository()
        val controller = PreferenceStateController(userPreferencesRepository = repository)
        val initialState: MusicAppUiState = baseState().copy(
            localMusicDiscoveryPreferences = LocalMusicDiscoveryPreferences(
                isAutoScanOnLaunchEnabled = false,
                shouldIgnoreShortAudio = true,
                shouldExcludeSystemFolders = true,
            ),
        )

        val state: MusicAppUiState = controller.setLocalMusicShortAudioIgnored(
            state = initialState,
            isIgnored = false,
        )

        assertFalse(actual = state.localMusicDiscoveryPreferences.shouldIgnoreShortAudio)
        assertFalse(actual = repository.getLocalMusicDiscoveryPreferences().shouldIgnoreShortAudio)
        assertFalse(actual = state.localMusicDiscoveryPreferences.isAutoScanOnLaunchEnabled)
        assertTrue(actual = state.localMusicDiscoveryPreferences.shouldExcludeSystemFolders)
    }
}

private fun baseState(): MusicAppUiState {
    return MusicAppUiState(
        likedSongIds = emptySet(),
        currentSongId = null,
        playbackStatus = PlaybackStatus.Idle,
        queueSongIds = emptyList(),
    )
}
```

- [x] **步骤 2：运行测试确认红灯**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.preferences.PreferenceStateControllerTest"
```

预期：失败，错误包含 `Unresolved reference: PreferenceStateController`。

- [x] **步骤 3：实现 `PreferenceStateController`**

创建 `PreferenceStateController.kt`：

```kotlin
package com.yanhao.kmpmusic.feature.app.preferences

import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 用户偏好 reducer，统一保存播放倍速和本地音频发现偏好。
 */
class PreferenceStateController(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    /** 设置启动时自动扫描偏好。 */
    fun setLocalMusicAutoScanOnLaunchEnabled(
        state: MusicAppUiState,
        isEnabled: Boolean,
    ): MusicAppUiState {
        return updateLocalMusicDiscoveryPreferences(state = state) { preferences: LocalMusicDiscoveryPreferences ->
            preferences.copy(isAutoScanOnLaunchEnabled = isEnabled)
        }
    }

    /** 设置短音频过滤偏好。 */
    fun setLocalMusicShortAudioIgnored(
        state: MusicAppUiState,
        isIgnored: Boolean,
    ): MusicAppUiState {
        return updateLocalMusicDiscoveryPreferences(state = state) { preferences: LocalMusicDiscoveryPreferences ->
            preferences.copy(shouldIgnoreShortAudio = isIgnored)
        }
    }

    /** 设置系统文件夹排除偏好。 */
    fun setLocalMusicSystemFoldersExcluded(
        state: MusicAppUiState,
        isExcluded: Boolean,
    ): MusicAppUiState {
        return updateLocalMusicDiscoveryPreferences(state = state) { preferences: LocalMusicDiscoveryPreferences ->
            preferences.copy(shouldExcludeSystemFolders = isExcluded)
        }
    }

    private fun updateLocalMusicDiscoveryPreferences(
        state: MusicAppUiState,
        transform: (LocalMusicDiscoveryPreferences) -> LocalMusicDiscoveryPreferences,
    ): MusicAppUiState {
        val preferences: LocalMusicDiscoveryPreferences = transform(state.localMusicDiscoveryPreferences)
        userPreferencesRepository.saveLocalMusicDiscoveryPreferences(preferences = preferences)
        return state.copy(localMusicDiscoveryPreferences = preferences)
    }
}
```

- [x] **步骤 4：让门面委派偏好设置**

在 `MusicAppController.kt` 增加属性：

```kotlin
    private val preferenceStateController: PreferenceStateController = PreferenceStateController(
        userPreferencesRepository = userPreferencesRepository,
    )
```

增加 import：

```kotlin
import com.yanhao.kmpmusic.feature.app.preferences.PreferenceStateController
```

把四个偏好公开方法替换为：

```kotlin
    /** 设置主题模式。 */
    fun setThemeMode(themeMode: ThemeMode) {
        uiState = uiState.copy(themeMode = themeMode)
    }

    /** 设置启动时自动扫描偏好。 */
    fun setLocalMusicAutoScanOnLaunchEnabled(isEnabled: Boolean) {
        uiState = preferenceStateController.setLocalMusicAutoScanOnLaunchEnabled(
            state = uiState,
            isEnabled = isEnabled,
        )
    }

    /** 设置短音频过滤偏好。 */
    fun setLocalMusicShortAudioIgnored(isIgnored: Boolean) {
        uiState = preferenceStateController.setLocalMusicShortAudioIgnored(
            state = uiState,
            isIgnored = isIgnored,
        )
    }

    /** 设置系统文件夹排除偏好。 */
    fun setLocalMusicSystemFoldersExcluded(isExcluded: Boolean) {
        uiState = preferenceStateController.setLocalMusicSystemFoldersExcluded(
            state = uiState,
            isExcluded = isExcluded,
        )
    }
```

删除 `MusicAppController` 中的私有 `updateLocalMusicDiscoveryPreferences`。

- [x] **步骤 5：运行任务三测试和门面回归**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.preferences.PreferenceStateControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.localMusicDiscoveryPreferencesPersistAndFlowIntoScanner"
```

预期：全部通过。

- [x] **步骤 6：提交任务三**

运行：

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/preferences/PreferenceStateController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/preferences/PreferenceStateControllerTest.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt
git commit -m "拆分偏好设置工作流"
```

## 任务四：拆出搜索结果派生工作流

**文件：**

- 创建：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchResultController.kt`
- 创建：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchResultControllerTest.kt`
- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`

**接口：**

- 消费：`MusicLibraryRepository`、`MusicAppUiState`、`SearchContext`、`SearchScope`。
- 产出：`SearchResultController.search(state: MusicAppUiState): SearchResult`。

- [x] **步骤 1：写搜索结果聚焦测试**

创建 `SearchResultControllerTest.kt`：

```kotlin
package com.yanhao.kmpmusic.feature.app.search

import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchResultControllerTest {
    /**
     * 防抖词未追上输入词时必须返回空结果，不能把空 active query 派生成全量曲库。
     */
    @Test
    fun pendingQueryReturnsEmptyResult(): Unit {
        val controller = SearchResultController(musicLibraryRepository = InMemoryMusicLibraryRepository())
        val result: SearchResult = controller.search(
            state = baseState().copy(
                searchQuery = "river",
                activeSearchQuery = "",
                searchScope = SearchScope.All,
            ),
        )

        assertTrue(actual = result.songs.isEmpty())
        assertTrue(actual = result.albums.isEmpty())
        assertTrue(actual = result.artists.isEmpty())
    }

    /**
     * 收藏搜索只能读取收藏投影，不应回退到完整曲库。
     */
    @Test
    fun favoritesSearchUsesFavoriteProjectionOnly(): Unit {
        val favoriteSong: Song = testSong(
            id = "external-favorite",
            title = "Only In Favorites",
        ).copy(isLiked = true)
        val controller = SearchResultController(musicLibraryRepository = InMemoryMusicLibraryRepository())

        val result: SearchResult = controller.search(
            state = baseState().copy(
                favoriteSongs = listOf(favoriteSong),
                searchContext = SearchContext.Favorites,
                searchQuery = favoriteSong.title,
                activeSearchQuery = favoriteSong.title,
                searchScope = SearchScope.Songs,
            ),
        )

        assertEquals(expected = listOf(favoriteSong.id), actual = result.songs.map { song: Song -> song.id })
    }
}

private fun testSong(id: String, title: String): Song {
    return Song(
        id = id,
        title = title,
        artist = "收藏歌手",
        album = "收藏专辑",
        duration = "03:00",
        coverArt = CoverArt.HeroLocalMusic,
        isLiked = false,
        lastPlayed = "",
        quality = "Lossless",
        lyric = "",
        trackNumber = 1,
        durationMs = 180_000L,
    )
}

private fun baseState(): MusicAppUiState {
    return MusicAppUiState(
        likedSongIds = emptySet(),
        currentSongId = null,
        playbackStatus = PlaybackStatus.Idle,
        queueSongIds = emptyList(),
    )
}
```

- [x] **步骤 2：运行测试确认红灯**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.search.SearchResultControllerTest"
```

预期：失败，错误包含 `Unresolved reference: SearchResultController`。

- [x] **步骤 3：补强门面搜索历史回归测试**

把 `MusicAppControllerTest.nonBlankSearchQueryDoesNotCommitToHistoryWhenLeavingSearchBeforeDebounce` 改成 `runTest`，并在离开搜索页后推进防抖时间：

```kotlin
    /**
     * 非空搜索词在防抖结果生效前离开搜索页，防抖任务醒来后也不应写入历史。
     */
    @Test
    fun nonBlankSearchQueryDoesNotCommitToHistoryWhenLeavingSearchBeforeDebounce(): Unit = runTest {
        val controller = createController(controllerScope = backgroundScope)
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)

        controller.openSearch(context = SearchContext.LocalLibrary)
        controller.setSearchQuery(query = "One Summer")
        controller.navigateBack()
        advanceTimeBy(delayTimeMillis = 301L)
        advanceUntilIdle()
        controller.openSearch(context = SearchContext.LocalLibrary)

        assertEquals(
            expected = emptyList(),
            actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
        )
    }
```

把 `searchResultActionsCommitCurrentQueryToHistory` 改成先等待防抖并从真实搜索结果取目标：

```kotlin
    /**
     * 点击真实可见搜索结果时，播放、打开专辑和打开歌手都应先提交当前搜索词。
     */
    @Test
    fun searchResultActionsCommitCurrentQueryToHistory(): Unit = runTest {
        val controller = createController(controllerScope = backgroundScope)
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)

        controller.openSearch(context = SearchContext.LocalLibrary)
        controller.setSearchQuery(query = "One Summer")
        advanceTimeBy(delayTimeMillis = 301L)
        advanceUntilIdle()
        val result: SearchResult = controller.search()
        val targetSong: Song = result.songs.first()
        val targetAlbum: Album = result.albums.first()
        val targetArtist: Artist = result.artists.first()

        controller.playSong(song = targetSong, queueSongs = result.songs)
        controller.openSearch(context = SearchContext.LocalLibrary)
        controller.setSearchQuery(query = "One Summer")
        advanceTimeBy(delayTimeMillis = 301L)
        advanceUntilIdle()
        controller.openAlbum(album = targetAlbum)
        controller.openSearch(context = SearchContext.LocalLibrary)
        controller.setSearchQuery(query = "One Summer")
        advanceTimeBy(delayTimeMillis = 301L)
        advanceUntilIdle()
        controller.openArtist(artist = targetArtist)

        assertEquals(
            expected = listOf("One Summer"),
            actual = controller.uiState.searchHistoryFor(context = SearchContext.LocalLibrary),
        )
    }
```

- [x] **步骤 4：实现 `SearchResultController`**

创建 `SearchResultController.kt`：

```kotlin
package com.yanhao.kmpmusic.feature.app.search

import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.domain.usecase.buildSearchResult
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 搜索结果派生器，按搜索上下文选择数据源并避免 pending query 泄漏全量曲库。
 */
class SearchResultController(
    private val musicLibraryRepository: MusicLibraryRepository,
) {
    /** 按当前输入态、active query 和搜索范围派生结果。 */
    fun search(state: MusicAppUiState): SearchResult {
        if (!shouldResolveCurrentSearchResult(state = state)) {
            return emptySearchResult()
        }
        return buildSearchResult(
            query = state.activeSearchQuery,
            scope = state.searchScope,
            allSongs = searchSourceSongs(state = state),
        )
    }

    private fun shouldResolveCurrentSearchResult(state: MusicAppUiState): Boolean {
        val normalizedQuery: String = state.searchQuery.trim()
        val normalizedActiveQuery: String = state.activeSearchQuery.trim()
        return normalizedQuery.isNotEmpty() && normalizedQuery == normalizedActiveQuery
    }

    private fun emptySearchResult(): SearchResult {
        return SearchResult(
            songs = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
        )
    }

    private fun searchSourceSongs(state: MusicAppUiState): List<Song> {
        return when (state.searchContext) {
            SearchContext.LocalLibrary -> {
                if (state.localSongs.isNotEmpty()) {
                    state.localSongs
                } else {
                    musicLibraryRepository.getAllAvailableSongs()
                }
            }
            SearchContext.Favorites -> state.favoriteSongs
        }
    }
}
```

- [x] **步骤 5：让门面委派搜索结果**

在 `MusicAppController.kt` 新增属性：

```kotlin
    private val searchResultController: SearchResultController = SearchResultController(
        musicLibraryRepository = musicLibraryRepository,
    )
```

增加 import：

```kotlin
import com.yanhao.kmpmusic.feature.app.search.SearchResultController
```

把 `search()` 替换为：

```kotlin
    /** 执行搜索，供 UI 渲染派生结果。 */
    fun search(): SearchResult {
        return searchResultController.search(state = uiState)
    }
```

删除 `shouldResolveCurrentSearchResult()`、`emptySearchResult()` 和 `searchSourceSongs()`。

- [x] **步骤 6：运行任务四测试和门面回归**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.search.SearchResultControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.pendingSearchQueryDoesNotReturnFullLibraryBeforeDebounce" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchResultActionsCommitCurrentQueryToHistory" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.nonSearchResultActionsDoNotCommitSearchHistory"
```

预期：全部通过。

- [x] **步骤 7：提交任务四**

运行：

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchResultController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/search/SearchResultControllerTest.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt
git commit -m "拆分搜索结果派生工作流"
```

## 任务五：拆出内容导航工作流

**文件：**

- 创建：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/navigation/ContentNavigationController.kt`
- 创建：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/ContentNavigationControllerTest.kt`
- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`

**接口：**

- 消费：`LibraryStateSynchronizer`、`NavigationStateController`、`Album`、`Artist`、`Song`。
- 产出：`ContentNavigationController.Result(state: MusicAppUiState, loadedFullLibrary: Boolean)` 和内容导航方法。

- [x] **步骤 1：写内容导航聚焦测试**

创建 `ContentNavigationControllerTest.kt`：

```kotlin
package com.yanhao.kmpmusic.feature.app.navigation

import com.yanhao.kmpmusic.data.InMemoryFavoritesRepository
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.feature.app.HomeContentSection
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.app.library.LibraryStateSynchronizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentNavigationControllerTest {
    /**
     * 打开本地音乐要按需加载完整曲库，并进入指定二级分段。
     */
    @Test
    fun openLocalMusicLoadsLibraryAndNavigatesToRequestedSection(): Unit {
        val controller = controller()

        val result: ContentNavigationController.Result = controller.openLocalMusic(
            state = baseState(),
            section = LocalMusicSection.Sources,
        )

        assertTrue(actual = result.loadedFullLibrary)
        assertTrue(actual = result.state.localSongs.isNotEmpty())
        assertEquals(
            expected = SecondaryScreen.LocalMusic(initialSection = LocalMusicSection.Sources),
            actual = result.state.navigationState.secondaryScreen,
        )
    }

    /**
     * 我的页歌曲统计入口回到首页歌曲分段，并保持一级页语义。
     */
    @Test
    fun openHomeSongsReturnsToHomeSongsTopLevel(): Unit {
        val secondaryState: MusicAppUiState = NavigationStateController.navigateToSecondary(
            state = baseState().copy(homeContentSection = HomeContentSection.Albums),
            screen = SecondaryScreen.RecentPlayed,
        )

        val result: ContentNavigationController.Result = controller().openHomeSongs(state = secondaryState)

        assertFalse(actual = result.loadedFullLibrary)
        assertEquals(expected = RootTab.Home, actual = result.state.navigationState.rootTab)
        assertEquals(expected = null, actual = result.state.navigationState.secondaryScreen)
        assertEquals(expected = HomeContentSection.Songs, actual = result.state.homeContentSection)
    }

    /**
     * 扫描页和最近播放页是纯导航入口，不应为了打开页面加载完整曲库。
     */
    @Test
    fun openAudioScanAndRecentPlayedNavigateWithoutLoadingLibrary(): Unit {
        val controller = controller()

        val scanResult: ContentNavigationController.Result = controller.openAudioScan(state = baseState())
        val recentResult: ContentNavigationController.Result = controller.openRecentPlayed(state = baseState())

        assertFalse(actual = scanResult.loadedFullLibrary)
        assertEquals(expected = SecondaryScreen.AudioScan, actual = scanResult.state.navigationState.secondaryScreen)
        assertFalse(actual = recentResult.loadedFullLibrary)
        assertEquals(expected = SecondaryScreen.RecentPlayed, actual = recentResult.state.navigationState.secondaryScreen)
    }

    /**
     * 专辑和歌手详情入口都必须先加载完整曲库，再写入选中身份并进入二级页。
     */
    @Test
    fun openAlbumAndArtistLoadLibraryAndSelectIdentity(): Unit {
        val controller = controller()
        val loadedState: MusicAppUiState = controller.loadLocalMusicLibrary(state = baseState()).state
        val album = loadedState.localAlbums.first()
        val artist = loadedState.localArtists.first()

        val albumResult: ContentNavigationController.Result = controller.openAlbum(
            state = baseState(),
            album = album,
        )
        val artistResult: ContentNavigationController.Result = controller.openArtist(
            state = baseState(),
            artist = artist,
        )

        assertTrue(actual = albumResult.loadedFullLibrary)
        assertEquals(expected = album.id, actual = albumResult.state.selectedAlbumId)
        assertEquals(expected = SecondaryScreen.AlbumDetail, actual = albumResult.state.navigationState.secondaryScreen)
        assertTrue(actual = artistResult.loadedFullLibrary)
        assertEquals(expected = artist.id, actual = artistResult.state.selectedArtistId)
        assertEquals(expected = SecondaryScreen.ArtistDetail, actual = artistResult.state.navigationState.secondaryScreen)
    }
}

private fun controller(): ContentNavigationController {
    return ContentNavigationController(
        libraryStateSynchronizer = LibraryStateSynchronizer(
            musicLibraryRepository = InMemoryMusicLibraryRepository(),
            favoritesRepository = InMemoryFavoritesRepository(),
            playbackRepository = InMemoryPlaybackRepository(),
        ),
    )
}

private fun baseState(): MusicAppUiState {
    return MusicAppUiState(
        likedSongIds = emptySet(),
        currentSongId = null,
        playbackStatus = PlaybackStatus.Idle,
        queueSongIds = emptyList(),
    )
}
```

- [x] **步骤 2：运行测试确认红灯**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.ContentNavigationControllerTest"
```

预期：失败，错误包含 `Unresolved reference: ContentNavigationController`。

- [x] **步骤 3：实现 `ContentNavigationController`**

创建 `ContentNavigationController.kt`：

```kotlin
package com.yanhao.kmpmusic.feature.app.navigation

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.hasSameAlbumTitle
import com.yanhao.kmpmusic.domain.model.hasSameArtistName
import com.yanhao.kmpmusic.feature.app.HomeContentSection
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.app.library.LibraryStateSynchronizer

/**
 * 内容导航工作流，负责会读取曲库并打开内容二级页的入口。
 */
class ContentNavigationController(
    private val libraryStateSynchronizer: LibraryStateSynchronizer,
) {
    /** 内容导航结果，门面据此决定是否续上加载上次播放数据。 */
    data class Result(
        val state: MusicAppUiState,
        val loadedFullLibrary: Boolean = false,
    )

    /** 切换首页内容页签，专辑和歌手页签按需加载完整曲库。 */
    fun setHomeContentSection(
        state: MusicAppUiState,
        section: HomeContentSection,
    ): Result {
        val loaded: Result = if (section == HomeContentSection.Albums || section == HomeContentSection.Artists) {
            loadLocalMusicLibrary(state = state)
        } else {
            Result(state = state)
        }
        return loaded.copy(state = loaded.state.copy(homeContentSection = section))
    }

    /** 我的页歌曲统计回到首页歌曲分段。 */
    fun openHomeSongs(state: MusicAppUiState): Result {
        val rootState: MusicAppUiState = NavigationStateController.navigateToRoot(
            state = state,
            tab = RootTab.Home,
        )
        return Result(state = rootState.copy(homeContentSection = HomeContentSection.Songs))
    }

    /** 打开本地音乐二级页并指定初始分段。 */
    fun openLocalMusic(
        state: MusicAppUiState,
        section: LocalMusicSection,
    ): Result {
        val loaded: Result = loadLocalMusicLibrary(state = state)
        return loaded.copy(
            state = NavigationStateController.navigateToSecondary(
                state = loaded.state,
                screen = SecondaryScreen.LocalMusic(initialSection = section),
            ),
        )
    }

    /** 打开独立扫描页。 */
    fun openAudioScan(state: MusicAppUiState): Result {
        return Result(
            state = NavigationStateController.navigateToSecondary(
                state = state,
                screen = SecondaryScreen.AudioScan,
            ),
        )
    }

    /** 打开最近播放二级页。 */
    fun openRecentPlayed(state: MusicAppUiState): Result {
        return Result(
            state = NavigationStateController.navigateToSecondary(
                state = state,
                screen = SecondaryScreen.RecentPlayed,
            ),
        )
    }

    /** 打开专辑详情前加载完整曲库。 */
    fun openAlbum(
        state: MusicAppUiState,
        album: Album,
    ): Result {
        val loaded: Result = loadLocalMusicLibrary(state = state)
        return loaded.copy(
            state = NavigationStateController.navigateToSecondary(
                state = loaded.state.copy(selectedAlbumId = album.id),
                screen = SecondaryScreen.AlbumDetail,
            ),
        )
    }

    /** 打开歌手详情前加载完整曲库。 */
    fun openArtist(
        state: MusicAppUiState,
        artist: Artist,
    ): Result {
        val loaded: Result = loadLocalMusicLibrary(state = state)
        return loaded.copy(
            state = NavigationStateController.navigateToSecondary(
                state = loaded.state.copy(selectedArtistId = artist.id),
                screen = SecondaryScreen.ArtistDetail,
            ),
        )
    }

    /** 从歌曲元数据匹配并打开专辑详情。 */
    fun openAlbumFromSong(
        state: MusicAppUiState,
        song: Song,
    ): Result {
        val loaded: Result = loadLocalMusicLibrary(state = state)
        val matchedAlbum: Album = loaded.state.detailAlbums.firstOrNull { album: Album ->
            hasSameAlbumTitle(
                firstTitle = album.title,
                secondTitle = song.album,
            )
        } ?: return loaded
        return openAlbum(
            state = loaded.state.copy(moreSongId = null),
            album = matchedAlbum,
        ).copy(loadedFullLibrary = loaded.loadedFullLibrary)
    }

    /** 从歌曲元数据匹配并打开歌手详情。 */
    fun openArtistFromSong(
        state: MusicAppUiState,
        song: Song,
    ): Result {
        val loaded: Result = loadLocalMusicLibrary(state = state)
        val matchedArtist: Artist = loaded.state.detailArtists.firstOrNull { artist: Artist ->
            hasSameArtistName(
                firstName = artist.name,
                secondName = song.artist,
            )
        } ?: return loaded
        return openArtist(
            state = loaded.state.copy(moreSongId = null),
            artist = matchedArtist,
        ).copy(loadedFullLibrary = loaded.loadedFullLibrary)
    }

    /** 按需读取完整本地曲库。 */
    fun loadLocalMusicLibrary(state: MusicAppUiState): Result {
        val previousLocalSongsLoaded: Boolean = state.localSongs.isNotEmpty()
        val nextState: MusicAppUiState = libraryStateSynchronizer.loadLocalMusicLibrary(state = state)
        return Result(
            state = nextState,
            loadedFullLibrary = !previousLocalSongsLoaded && nextState.localSongs.isNotEmpty(),
        )
    }
}
```

- [x] **步骤 4：让门面委派内容导航**

在 `MusicAppController.kt` 增加属性：

```kotlin
    private val contentNavigationController: ContentNavigationController
```

在 `libraryStateSynchronizer` 初始化后赋值：

```kotlin
        contentNavigationController = ContentNavigationController(
            libraryStateSynchronizer = libraryStateSynchronizer,
        )
```

增加 import：

```kotlin
import com.yanhao.kmpmusic.feature.app.navigation.ContentNavigationController
```

加入结果应用函数：

```kotlin
    private fun applyContentNavigationResult(result: ContentNavigationController.Result) {
        uiState = result.state
        if (result.loadedFullLibrary) {
            restorePlaybackSnapshotIfPending()
        }
    }
```

把 `setHomeContentSection`、`openHomeSongs`、`openLocalMusic`、`openAudioScan`、`openRecentPlayed`、`openAlbum`、`openArtist`、`openAlbumFromSong`、`openArtistFromSong` 和 `loadLocalMusicLibrary` 改成委派。搜索结果动作前的历史提交仍留在门面：

```kotlin
    fun openAlbum(album: Album) {
        commitSearchQueryForResultActionIfNeeded()
        applyContentNavigationResult(
            result = contentNavigationController.openAlbum(
                state = uiState,
                album = album,
            ),
        )
    }

    fun openArtist(artist: Artist) {
        commitSearchQueryForResultActionIfNeeded()
        applyContentNavigationResult(
            result = contentNavigationController.openArtist(
                state = uiState,
                artist = artist,
            ),
        )
    }

    fun loadLocalMusicLibrary() {
        applyContentNavigationResult(
            result = contentNavigationController.loadLocalMusicLibrary(state = uiState),
        )
    }
```

- [x] **步骤 5：运行任务五测试和导航回归**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.navigation.ContentNavigationControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.openLocalMusicUsesSecondaryFixedBarMode" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.openAudioScanUsesDedicatedScanRoute" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.meViewAllRecentPlayedOpensRecentPageAndReturnsToMe" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.openArtistFromSongUsesNormalizedArtistName" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.searchResultActionsCommitCurrentQueryToHistory" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.nonSearchResultActionsDoNotCommitSearchHistory"
```

预期：全部通过。

- [x] **步骤 6：提交任务五**

运行：

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/navigation/ContentNavigationController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/navigation/ContentNavigationControllerTest.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt
git commit -m "拆分内容导航工作流"
```

## 任务六：拆出播放动作工作流并保留队列不变量

**文件：**

- 创建：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackActionController.kt`
- 创建：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackActionControllerTest.kt`
- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- 修改：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

**接口：**

- 消费：`PlaybackCoordinator`、`PlaybackRepository`、`PlaybackSnapshotStore`、`CoroutineScope`、`nowMillis`、`MusicAppUiState.queueSongs`。
- 产出：`PlaybackActionController.preparePlaySong`、`preparePlayRecentSong`、`startPlayback`、`togglePlayback`、`play`、`pause`、`moveTrack`、`skipToQueueIndex`、`seekTo`、`cyclePlaybackMode`、`setVolume`、`removeFromQueue`、`persistPlaybackSnapshotForServiceTeardown`、`persistPlaybackSnapshotForProcessTeardown`、`clearRecentPlaybackHistory`。

队列不变量说明：`queueSongIds` 来自 `PlaybackRepository.getQueueState()`，`queueSongsSnapshot` 是 UI 能稳定解析队列实体的快照，`queueSongs` 是 `MusicAppUiState` 按 `queueSongIds` 从 `queueSongsSnapshot`、完整曲库、首页预览和收藏投影中派生出的实体列表。`playSong` 必须先把 `queueSongsSnapshot` 写回门面状态，再启动 `PlaybackCoordinator.playSong` 副作用，避免 coordinator 的同步状态被旧 `state.copy(...)` 覆盖。

- [x] **步骤 1：写播放动作聚焦测试**

创建 `PlaybackActionControllerTest.kt`：

```kotlin
package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.data.FakeAudioPlayerEngine
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.data.InMemoryPlaybackRepository
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.InMemoryPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.playback.PlaybackCoordinator
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackActionControllerTest {
    /**
     * 未传入队列且目标歌曲在当前队列时，应复用当前队列实体。
     */
    @Test
    fun playSongReusesCurrentQueueWhenTargetExists(): Unit = runTest {
        val songs: List<Song> = InMemoryMusicLibraryRepository().getHomePreview(limit = 4)
        val fixture = playbackFixture()
        val state = baseState().copy(
            queueSongsSnapshot = songs,
            queueSongIds = songs.map { song: Song -> song.id },
        )

        val action: PlaybackActionController.PreparedPlaySong = fixture.controller.preparePlaySong(
            state = state,
            song = songs[2],
            queueSongs = emptyList(),
        )

        assertEquals(expected = songs.map { song: Song -> song.id }, actual = action.state.queueSongsSnapshot.map { song: Song -> song.id })
        assertEquals(expected = songs[2].id, actual = action.song.id)
    }

    /**
     * 音量归一化后要同时写 UI 状态和播放引擎。
     */
    @Test
    fun setVolumeCoercesStateAndEngine(): Unit {
        val fixture = playbackFixture()

        val nextState: MusicAppUiState = fixture.controller.setVolume(
            state = baseState(),
            volume = 2f,
        )

        assertEquals(expected = 1f, actual = nextState.playbackVolume)
        assertEquals(expected = 1f, actual = fixture.audioPlayerEngine.volume)
    }
}

private data class PlaybackFixture(
    val controller: PlaybackActionController,
    val audioPlayerEngine: FakeAudioPlayerEngine,
)

private fun playbackFixture(): PlaybackFixture {
    val playbackRepository = InMemoryPlaybackRepository()
    val audioPlayerEngine = FakeAudioPlayerEngine()
    val playbackSnapshotStore = InMemoryPlaybackSnapshotStore()
    val playbackCoordinator = PlaybackCoordinator(
        playbackRepository = playbackRepository,
        audioPlayerEngine = audioPlayerEngine,
        playbackSnapshotStore = playbackSnapshotStore,
    )
    return PlaybackFixture(
        controller = PlaybackActionController(
            playbackCoordinator = playbackCoordinator,
            playbackRepository = playbackRepository,
            playbackSnapshotStore = playbackSnapshotStore,
            controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            nowMillis = { 1_719_360_000_000L },
        ),
        audioPlayerEngine = audioPlayerEngine,
    )
}

private fun baseState(): MusicAppUiState {
    return MusicAppUiState(
        likedSongIds = emptySet(),
        currentSongId = null,
        playbackStatus = PlaybackStatus.Idle,
        queueSongIds = emptyList(),
    )
}
```

- [x] **步骤 2：给门面补队列不变量回归测试**

在 `MusicAppControllerTest` 的播放队列测试附近加入：

```kotlin
    /**
     * 改变当前播放事实的公开入口后，UI 队列、仓库队列和当前歌曲实体必须保持一致。
     */
    @Test
    fun playbackActionsKeepQueueIdsAndRepositoryQueueConsistent(): Unit = runTest {
        val playbackRepository = InMemoryPlaybackRepository()
        val controller = createController(
            playbackRepository = playbackRepository,
            controllerScope = backgroundScope,
        )
        val queueSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 3)
        val initialQueueIds: List<String> = queueSongs.map { song: Song -> song.id }

        controller.playSong(song = queueSongs[0], queueSongs = queueSongs)
        advanceUntilIdle()
        assertPlaybackQueueInvariant(
            controller = controller,
            playbackRepository = playbackRepository,
            expectedQueueSongIds = initialQueueIds,
        )

        controller.skipToQueueIndex(index = 1)
        advanceUntilIdle()
        assertPlaybackQueueInvariant(
            controller = controller,
            playbackRepository = playbackRepository,
            expectedQueueSongIds = initialQueueIds,
        )

        controller.removeFromQueue(songId = queueSongs[0].id)
        advanceUntilIdle()
        assertPlaybackQueueInvariant(
            controller = controller,
            playbackRepository = playbackRepository,
            expectedQueueSongIds = initialQueueIds.drop(n = 1),
        )
    }

private fun assertPlaybackQueueInvariant(
    controller: MusicAppController,
    playbackRepository: InMemoryPlaybackRepository,
    expectedQueueSongIds: List<String>,
) {
    assertEquals(expected = expectedQueueSongIds, actual = playbackRepository.getQueueState().songIds)
    assertEquals(expected = expectedQueueSongIds, actual = controller.uiState.queueSongIds)
    assertEquals(expected = expectedQueueSongIds, actual = controller.uiState.queueSongs.map { song: Song -> song.id })
    assertEquals(
        expected = controller.uiState.currentSongId,
        actual = controller.uiState.currentSong?.id,
    )
}
```

- [x] **步骤 3：运行测试确认红灯**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.playback.PlaybackActionControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playbackActionsKeepQueueIdsAndRepositoryQueueConsistent"
```

预期：聚焦测试失败，错误包含 `Unresolved reference: PlaybackActionController`；门面队列不变量测试可以先通过，保留作为迁移回归。

- [x] **步骤 4：实现 `PlaybackActionController`**

创建 `PlaybackActionController.kt`：

```kotlin
package com.yanhao.kmpmusic.feature.app.playback

import com.yanhao.kmpmusic.domain.model.PlaybackHistory
import com.yanhao.kmpmusic.domain.model.PlaybackSnapshot
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.playback.PlaybackCoordinator
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * 播放动作工作流，集中处理会改变播放事实、队列事实或播放持久快照的入口。
 */
class PlaybackActionController(
    private val playbackCoordinator: PlaybackCoordinator,
    private val playbackRepository: PlaybackRepository,
    private val playbackSnapshotStore: PlaybackSnapshotStore,
    private val controllerScope: CoroutineScope,
    private val nowMillis: () -> Long,
) {
    /**
     * 已解析的播放动作。门面必须先写入 [state]，再调用 [startPlayback] 执行副作用。
     */
    data class PreparedPlaySong(
        val state: MusicAppUiState,
        val song: Song,
        val queueSongs: List<Song>,
    )

    /** 播放歌曲但留在当前页面，未显式传列表时优先复用当前队列上下文。 */
    fun preparePlaySong(
        state: MusicAppUiState,
        song: Song,
        queueSongs: List<Song>,
    ): PreparedPlaySong {
        val resolvedQueueSongs: List<Song> = resolvePlaybackQueueSongs(
            state = state,
            song = song,
            queueSongs = queueSongs,
        )
        return PreparedPlaySong(
            state = state.copy(queueSongsSnapshot = resolvedQueueSongs),
            song = song,
            queueSongs = resolvedQueueSongs,
        )
    }

    /** 门面写入实体队列快照后，再启动真正播放副作用。 */
    fun startPlayback(action: PreparedPlaySong) {
        controllerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            playbackCoordinator.playSong(
                song = action.song,
                queueSongs = action.queueSongs,
            )
        }
    }

    /** 最近播放入口必须复用完整最近播放列表。 */
    fun preparePlayRecentSong(
        state: MusicAppUiState,
        song: Song,
    ): PreparedPlaySong {
        return preparePlaySong(
            state = state,
            song = song,
            queueSongs = state.recentSongs,
        )
    }

    /** 切换播放暂停。 */
    fun togglePlayback() {
        playbackCoordinator.togglePlayback()
    }

    /** 显式恢复或开始播放。 */
    fun play() {
        playbackCoordinator.play()
    }

    /** 显式暂停播放。 */
    fun pause() {
        playbackCoordinator.pause()
    }

    /** 切换上一首或下一首。 */
    fun moveTrack(direction: Int) {
        if (direction < 0) {
            playbackCoordinator.movePrevious()
            return
        }
        playbackCoordinator.moveNext()
    }

    /** 按精确队列下标切歌。 */
    fun skipToQueueIndex(index: Int, positionMs: Long = 0L) {
        playbackCoordinator.skipToQueueIndex(
            index = index,
            positionMs = positionMs,
        )
    }

    /** 拖动播放进度时同时更新运行态与持久化快照。 */
    fun seekTo(positionMs: Long) {
        playbackCoordinator.seekTo(positionMs = positionMs)
        controllerScope.launch {
            playbackSnapshotStore.saveSnapshot(
                snapshot = PlaybackSnapshot(
                    playbackState = playbackRepository.getPlaybackState().copy(
                        positionMs = positionMs.coerceAtLeast(minimumValue = 0L),
                    ),
                    queueState = playbackRepository.getQueueState(),
                    updatedAt = nowMillis(),
                ),
            )
        }
    }

    /** 播放模式按钮只负责触发协调器切换。 */
    fun cyclePlaybackMode() {
        playbackCoordinator.cyclePlaybackMode()
    }

    /** 调整共享播放器音量。 */
    fun setVolume(
        state: MusicAppUiState,
        volume: Float,
    ): MusicAppUiState {
        val safeVolume: Float = volume.coerceIn(minimumValue = 0f, maximumValue = 1f)
        playbackCoordinator.setVolume(volume = safeVolume)
        return state.copy(playbackVolume = safeVolume)
    }

    /** Android 播放 service 退出前补写最终暂停快照。 */
    fun persistPlaybackSnapshotForServiceTeardown(positionMs: Long, durationMs: Long?) {
        playbackCoordinator.persistSnapshotForServiceTeardown(
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    /** Desktop 进程退出前同步固化最后进度。 */
    suspend fun persistPlaybackSnapshotForProcessTeardown(positionMs: Long, durationMs: Long?) {
        playbackCoordinator.persistSnapshotForProcessTeardown(
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    /** 从队列移除歌曲，至少保留一首。 */
    fun removeFromQueue(
        state: MusicAppUiState,
        songId: String,
    ) {
        controllerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            playbackCoordinator.removeFromQueue(
                songId = songId,
                availableSongs = state.queueSongs,
            )
        }
    }

    /** 清空真实最近播放历史，并立即同步当前页面列表。 */
    fun clearRecentPlaybackHistory(state: MusicAppUiState): MusicAppUiState {
        playbackRepository.savePlaybackHistory(history = PlaybackHistory())
        return state.copy(recentSongs = emptyList())
    }

    private fun resolvePlaybackQueueSongs(
        state: MusicAppUiState,
        song: Song,
        queueSongs: List<Song>,
    ): List<Song> {
        if (queueSongs.any { candidate: Song -> candidate.id == song.id }) {
            return queueSongs
        }
        val currentQueueSongs: List<Song> = state.queueSongs
        if (currentQueueSongs.any { candidate: Song -> candidate.id == song.id }) {
            return currentQueueSongs
        }
        return listOf(song)
    }
}
```

- [x] **步骤 5：让门面委派播放动作**

在 `MusicAppController.kt` 初始化 `playbackCoordinator` 后创建：

```kotlin
    private val playbackActionController: PlaybackActionController = PlaybackActionController(
        playbackCoordinator = playbackCoordinator,
        playbackRepository = playbackRepository,
        playbackSnapshotStore = playbackSnapshotStore,
        controllerScope = controllerScope,
        nowMillis = nowMillis,
    )
```

公开方法委派示例：

```kotlin
    fun playSong(song: Song, queueSongs: List<Song> = emptyList()) {
        commitSearchQueryForResultActionIfNeeded()
        clearPendingPlaybackSnapshotRequest()
        val action: PlaybackActionController.PreparedPlaySong = playbackActionController.preparePlaySong(
            state = uiState,
            song = song,
            queueSongs = queueSongs,
        )
        uiState = action.state
        playbackActionController.startPlayback(action = action)
    }

    fun playRecentSong(song: Song) {
        commitSearchQueryForResultActionIfNeeded()
        clearPendingPlaybackSnapshotRequest()
        val action: PlaybackActionController.PreparedPlaySong = playbackActionController.preparePlayRecentSong(
            state = uiState,
            song = song,
        )
        uiState = action.state
        playbackActionController.startPlayback(action = action)
    }

    fun setVolume(volume: Float) {
        uiState = playbackActionController.setVolume(
            state = uiState,
            volume = volume,
        )
    }

    fun removeFromQueue(songId: String) {
        clearPendingPlaybackSnapshotRequest()
        playbackActionController.removeFromQueue(
            state = uiState,
            songId = songId,
        )
    }
```

在任务六先加入兼容空实现，任务八会补齐真实待加载请求失效逻辑：

```kotlin
    private fun clearPendingPlaybackSnapshotRequest() {
        isPlaybackRestorePending = false
    }
```

- [x] **步骤 6：运行任务六测试和播放回归**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.playback.PlaybackActionControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playSongUpdatesPlaybackAndQueue" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playSongUsesProvidedQueueSongs" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playSongWithoutProvidedQueueKeepsCurrentQueueWhenSongExists" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playRecentPageSongUsesFullRecentQueueWithClickedStart" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playerScreenAndBottomPlayerReadSamePlaybackState" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playbackActionsKeepQueueIdsAndRepositoryQueueConsistent"
```

预期：全部通过。

- [x] **步骤 7：提交任务六**

运行：

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackActionController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackActionControllerTest.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "拆分播放动作工作流"
```

## 任务七：拆出本地扫描工作流并加固会话取消

**文件：**

- 创建：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library/LocalMusicScanController.kt`
- 创建：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/LocalMusicScanControllerTest.kt`
- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- 修改：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

**接口：**

- 消费：`ScanLocalMusicUseCase`、`LibraryStateSynchronizer.shouldConfirmPermissionSettingsBeforeScan`、`PermissionSettingsOpener`、`CoroutineScope`、`nowMillis`、`LocalMusicScanRequest`。
- 产出：`LocalMusicScanController.scanLocalMusic(state, request, onLibrarySnapshot)`、`requestLocalMusicScan`、`openPermissionSettingsDialog`、`closePermissionSettingsDialog`、`confirmPermissionSettings`。

- [x] **步骤 1：写会话取消和旧事件丢弃聚焦测试**

创建 `LocalMusicScanControllerTest.kt`，覆盖二次触发取消、旧成功晚到和旧错误晚到。旧事件测试必须使用会忽略取消并继续返回的 fake，不能使用普通可取消挂起点：

```kotlin
package com.yanhao.kmpmusic.feature.app.library

import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanProgress
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCase
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalMusicScanControllerTest {
    /**
     * 运行中再次触发扫描只取消当前会话，不启动第二个扫描，并发布取消态。
     */
    @Test
    fun runningScanSecondEntryCancelsCurrentSessionOnly(): Unit = runTest {
        var state: MusicAppUiState = baseState()
        val useCase = BlockingScanUseCase()
        val controller = LocalMusicScanController(
            scanLocalMusicUseCase = useCase,
            permissionSettingsOpener = PermissionSettingsOpener {},
            nowMillis = { 10L },
            resolveLikedSongIdsForScan = { currentState: MusicAppUiState -> currentState.likedSongIds },
            shouldConfirmPermissionSettingsBeforeScan = { false },
            publishStateUpdate = { reducer -> state = reducer(state) },
        )

        val job = launch {
            controller.scanLocalMusic(
                state = state,
                request = LocalMusicScanRequest.Refresh,
                onLibrarySnapshot = {},
            )
        }
        useCase.awaitStarted()

        controller.scanLocalMusic(
            state = state,
            request = LocalMusicScanRequest.Refresh,
            onLibrarySnapshot = {},
        )

        assertEquals(expected = 1, actual = useCase.callCount)
        assertTrue(actual = state.scanState is LocalMusicScanState.Cancelled)
        job.cancel()
    }

    /**
     * 取消后旧成功晚到必须被丢弃，不能同步曲库快照或覆盖取消态。
     */
    @Test
    fun lateSuccessAfterCancellationIsIgnored(): Unit = runTest {
        var state: MusicAppUiState = baseState()
        var syncedSnapshotCount = 0
        val useCase = LateSuccessAfterCancellationUseCase()
        val controller = LocalMusicScanController(
            scanLocalMusicUseCase = useCase,
            permissionSettingsOpener = PermissionSettingsOpener {},
            nowMillis = { 10L },
            resolveLikedSongIdsForScan = { currentState: MusicAppUiState -> currentState.likedSongIds },
            shouldConfirmPermissionSettingsBeforeScan = { false },
            publishStateUpdate = { reducer -> state = reducer(state) },
        )

        val job = launch {
            controller.scanLocalMusic(
                state = state,
                request = LocalMusicScanRequest.Refresh,
                onLibrarySnapshot = { syncedSnapshotCount += 1 },
            )
        }
        useCase.awaitStarted()
        controller.scanLocalMusic(
            state = state,
            request = LocalMusicScanRequest.Refresh,
            onLibrarySnapshot = { syncedSnapshotCount += 1 },
        )
        useCase.releaseLateResult()
        job.join()

        assertEquals(expected = 0, actual = syncedSnapshotCount)
        assertTrue(actual = state.scanState is LocalMusicScanState.Cancelled)
    }

    /**
     * 取消后旧错误晚到也必须被丢弃，不能把取消态改成错误态。
     */
    @Test
    fun lateErrorAfterCancellationIsIgnored(): Unit = runTest {
        var state: MusicAppUiState = baseState()
        val useCase = LateErrorAfterCancellationUseCase()
        val controller = LocalMusicScanController(
            scanLocalMusicUseCase = useCase,
            permissionSettingsOpener = PermissionSettingsOpener {},
            nowMillis = { 10L },
            resolveLikedSongIdsForScan = { currentState: MusicAppUiState -> currentState.likedSongIds },
            shouldConfirmPermissionSettingsBeforeScan = { false },
            publishStateUpdate = { reducer -> state = reducer(state) },
        )

        val job = launch {
            controller.scanLocalMusic(
                state = state,
                request = LocalMusicScanRequest.Refresh,
                onLibrarySnapshot = {},
            )
        }
        useCase.awaitStarted()
        controller.scanLocalMusic(
            state = state,
            request = LocalMusicScanRequest.Refresh,
            onLibrarySnapshot = {},
        )
        useCase.releaseLateError()
        job.join()

        assertTrue(actual = state.scanState is LocalMusicScanState.Cancelled)
    }
}

private class BlockingScanUseCase : ScanLocalMusicUseCase {
    private val started: CompletableDeferred<Unit> = CompletableDeferred()
    private val release: CompletableDeferred<Unit> = CompletableDeferred()

    var callCount: Int = 0
        private set

    override suspend fun invoke(
        request: LocalMusicScanRequest,
        likedSongIds: Set<String>,
        preferences: LocalMusicDiscoveryPreferences,
    ): LibrarySnapshot {
        callCount += 1
        started.complete(value = Unit)
        release.await()
        return LibrarySnapshot(
            songs = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
            stats = LibraryStats(),
            sources = emptyList(),
            scanState = LocalMusicScanState.Done(
                summary = LocalMusicLastScanSummary(
                    addedCount = 0,
                    updatedCount = 0,
                    removedCount = 0,
                    problemCount = 0,
                    completedAt = 10L,
                ),
            ),
            lastScanSummary = null,
            problems = emptyList(),
        )
    }

    suspend fun awaitStarted() {
        started.await()
    }
}

private class LateErrorAfterCancellationUseCase : ScanLocalMusicUseCase {
    private val started: CompletableDeferred<Unit> = CompletableDeferred()
    private val release: CompletableDeferred<Unit> = CompletableDeferred()

    override suspend fun invoke(
        request: LocalMusicScanRequest,
        likedSongIds: Set<String>,
        preferences: LocalMusicDiscoveryPreferences,
    ): LibrarySnapshot {
        started.complete(value = Unit)
        try {
            release.await()
        } catch (cancellationException: CancellationException) {
            withContext(NonCancellable) {
                release.await()
            }
        }
        throw LocalMusicScanException(
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.Unknown,
                message = "旧扫描错误晚到",
            ),
        )
    }

    suspend fun awaitStarted() {
        started.await()
    }

    fun releaseLateError() {
        release.complete(value = Unit)
    }
}

private class LateSuccessAfterCancellationUseCase : ScanLocalMusicUseCase {
    private val started: CompletableDeferred<Unit> = CompletableDeferred()
    private val release: CompletableDeferred<Unit> = CompletableDeferred()

    override suspend fun invoke(
        request: LocalMusicScanRequest,
        likedSongIds: Set<String>,
        preferences: LocalMusicDiscoveryPreferences,
    ): LibrarySnapshot {
        started.complete(value = Unit)
        try {
            release.await()
        } catch (cancellationException: CancellationException) {
            withContext(NonCancellable) {
                release.await()
            }
        }
        return LibrarySnapshot(
            songs = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
            stats = LibraryStats(songCount = 99),
            sources = emptyList(),
            scanState = LocalMusicScanState.Done(
                summary = LocalMusicLastScanSummary(
                    addedCount = 99,
                    updatedCount = 0,
                    removedCount = 0,
                    problemCount = 0,
                    completedAt = 10L,
                ),
            ),
            lastScanSummary = null,
            problems = emptyList(),
        )
    }

    suspend fun awaitStarted() {
        started.await()
    }

    fun releaseLateResult() {
        release.complete(value = Unit)
    }
}

private fun baseState(): MusicAppUiState {
    return MusicAppUiState(
        likedSongIds = emptySet(),
        currentSongId = null,
        playbackStatus = PlaybackStatus.Idle,
        queueSongIds = emptyList(),
        scanState = LocalMusicScanState.Scanning(
            progress = LocalMusicScanProgress(currentSourceName = "上一次"),
        ),
    )
}
```

- [x] **步骤 2：在门面测试补旧结果晚到回归**

在 `MusicAppControllerTest` 扫描测试附近加入。这里同样使用忽略取消的 scanner，确保测试真的覆盖旧结果晚到；同时在文件 import 区加入 `kotlinx.coroutines.NonCancellable` 和 `kotlinx.coroutines.withContext`：

```kotlin
    /**
     * 用户取消扫描后，旧扫描结果晚到不能覆盖取消态或队列状态。
     */
    @Test
    fun lateScanResultAfterCancellationDoesNotOverwriteCancelledStateOrQueue(): Unit = runTest {
        val scanner = LateSuccessAfterCancellationScanner()
        val controller = createController(
            localMusicScanner = scanner,
            controllerScope = backgroundScope,
        )
        val queueSongs: List<Song> = controller.uiState.homeLocalSongPreview.take(n = 2)
        controller.playSong(song = queueSongs[0], queueSongs = queueSongs)
        advanceUntilIdle()

        controller.requestLocalMusicScan(request = LocalMusicScanRequest.Refresh)
        scanner.awaitStarted()
        controller.requestLocalMusicScan(request = LocalMusicScanRequest.Refresh)
        scanner.releaseLateResult()
        advanceUntilIdle()

        assertTrue(actual = controller.uiState.scanState is LocalMusicScanState.Cancelled)
        assertEquals(expected = 0, actual = controller.uiState.libraryStats.songCount)
        assertEquals(
            expected = queueSongs.map { song: Song -> song.id },
            actual = controller.uiState.queueSongIds,
        )
    }

private class LateSuccessAfterCancellationScanner : LocalMusicScanner {
    private val started: CompletableDeferred<Unit> = CompletableDeferred()
    private val release: CompletableDeferred<Unit> = CompletableDeferred()

    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        started.complete(value = Unit)
        try {
            release.await()
        } catch (cancellationException: CancellationException) {
            withContext(NonCancellable) {
                release.await()
            }
        }
        return com.yanhao.kmpmusic.data.FakeLocalMusicScanner(demoSongCount = 8).scan(request = request)
    }

    suspend fun awaitStarted() {
        started.await()
    }

    fun releaseLateResult() {
        release.complete(value = Unit)
    }
}
```

- [x] **步骤 3：运行测试确认红灯**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.library.LocalMusicScanControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.lateScanResultAfterCancellationDoesNotOverwriteCancelledStateOrQueue"
```

预期：聚焦测试失败，错误包含 `Unresolved reference: LocalMusicScanController`；门面测试可能先失败于旧结果覆盖，也可能通过，保留作为迁移回归。

- [x] **步骤 4：实现 `LocalMusicScanController`**

创建 `LocalMusicScanController.kt`：

```kotlin
package com.yanhao.kmpmusic.feature.app.library

import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanProgress
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCase
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext

private const val LOCAL_MUSIC_SCAN_LOG_PREFIX = "[LocalMusicScan]"

/**
 * 本地音乐扫描工作流，把一次扫描视为独立会话并丢弃旧会话晚到事件。
 */
class LocalMusicScanController(
    private val scanLocalMusicUseCase: ScanLocalMusicUseCase,
    private val permissionSettingsOpener: PermissionSettingsOpener,
    private val nowMillis: () -> Long,
    private val resolveLikedSongIdsForScan: (MusicAppUiState) -> Set<String>,
    private val shouldConfirmPermissionSettingsBeforeScan: (MusicAppUiState) -> Boolean,
    private val publishStateUpdate: ((MusicAppUiState) -> MusicAppUiState) -> Unit,
) {
    private var nextSessionId: Long = 0L
    private var runningSessionId: Long? = null
    private val cancelledSessionIds: MutableSet<Long> = mutableSetOf()
    private var currentLocalMusicScanJob: Job? = null

    /** 扫描本地音乐并在成功时把曲库快照交还给门面同步。 */
    suspend fun scanLocalMusic(
        state: MusicAppUiState,
        request: LocalMusicScanRequest,
        onLibrarySnapshot: (LibrarySnapshot) -> Unit,
    ) {
        if (runningSessionId != null) {
            cancelRunningLocalMusicScan()
            return
        }
        if (shouldConfirmPermissionSettingsBeforeScan(state)) {
            openPermissionSettingsDialog()
            return
        }
        val sessionId: Long = ++nextSessionId
        runningSessionId = sessionId
        currentLocalMusicScanJob = currentCoroutineContext()[Job]
        val previousSummary: LocalMusicLastScanSummary? = findLastScanSummary(scanState = state.scanState)
        logLocalMusicScan(message = "开始扫描: request=$request, sessionId=$sessionId")
        publishStateUpdate { currentState: MusicAppUiState ->
            currentState.copy(
                scanState = LocalMusicScanState.Scanning(
                    progress = LocalMusicScanProgress(currentSourceName = "本地音乐"),
                    previousSummary = previousSummary,
                ),
                isQueueOpen = false,
                moreSongId = null,
            )
        }
        try {
            val snapshot: LibrarySnapshot = scanLocalMusicUseCase(
                request = request,
                likedSongIds = resolveLikedSongIdsForScan(state),
                preferences = state.localMusicDiscoveryPreferences,
            )
            if (!shouldAcceptResult(sessionId = sessionId)) {
                logLocalMusicScan(message = "扫描结果已忽略: request=$request, sessionId=$sessionId")
                return
            }
            onLibrarySnapshot(snapshot)
        } catch (cancellationException: CancellationException) {
            publishCancelledLocalMusicScanIfSameSession(sessionId = sessionId)
            throw cancellationException
        } catch (scanException: LocalMusicScanException) {
            if (!shouldAcceptResult(sessionId = sessionId)) {
                return
            }
            if (scanException.error.type == LocalMusicScanErrorType.UserCancelled) {
                publishCancelledLocalMusicScan(sessionId = sessionId)
                return
            }
            publishStateUpdate { currentState: MusicAppUiState ->
                currentState.copy(
                    scanState = LocalMusicScanState.Error(
                        error = scanException.error,
                        summary = previousSummary,
                    ),
                    isQueueOpen = false,
                    moreSongId = null,
                )
            }
        } finally {
            if (runningSessionId == sessionId) {
                runningSessionId = null
                currentLocalMusicScanJob = null
            }
        }
    }

    /** 打开权限设置确认框。 */
    fun openPermissionSettingsDialog() {
        publishStateUpdate { state: MusicAppUiState ->
            state.copy(
                isPermissionSettingsDialogOpen = true,
                isQueueOpen = false,
                moreSongId = null,
            )
        }
    }

    /** 关闭权限设置确认框。 */
    fun closePermissionSettingsDialog() {
        publishStateUpdate { state: MusicAppUiState ->
            state.copy(isPermissionSettingsDialogOpen = false)
        }
    }

    /** 用户确认后再打开系统权限设置页。 */
    fun confirmPermissionSettings() {
        publishStateUpdate { state: MusicAppUiState ->
            state.copy(
                isPermissionSettingsDialogOpen = false,
                scanState = LocalMusicScanState.WaitingForPermission,
            )
        }
        permissionSettingsOpener.openPermissionSettings()
    }

    private fun cancelRunningLocalMusicScan() {
        val sessionId: Long = runningSessionId ?: return
        cancelledSessionIds += sessionId
        currentLocalMusicScanJob?.cancel(
            cause = CancellationException("用户取消了本地音乐扫描"),
        )
        publishCancelledLocalMusicScan(sessionId = sessionId)
    }

    private fun publishCancelledLocalMusicScanIfSameSession(sessionId: Long) {
        if (!isSameRunningSession(sessionId = sessionId)) {
            return
        }
        publishStateUpdate { currentState: MusicAppUiState ->
            val scanState: LocalMusicScanState = currentState.scanState
            if (scanState !is LocalMusicScanState.Scanning && scanState !is LocalMusicScanState.Importing) {
                currentState
            } else {
                buildCancelledState(state = currentState)
            }
        }
    }

    private fun publishCancelledLocalMusicScan(sessionId: Long) {
        if (!isSameRunningSession(sessionId = sessionId)) {
            return
        }
        publishStateUpdate { currentState: MusicAppUiState ->
            buildCancelledState(state = currentState)
        }
    }

    private fun buildCancelledState(state: MusicAppUiState): MusicAppUiState {
        return state.copy(
            scanState = LocalMusicScanState.Cancelled(
                summary = LocalMusicLastScanSummary(
                    addedCount = 0,
                    updatedCount = 0,
                    removedCount = 0,
                    problemCount = 0,
                    completedAt = scanResultTimeMillis(),
                ),
            ),
            isQueueOpen = false,
            moreSongId = null,
            isPermissionSettingsDialogOpen = false,
        )
    }

    private fun isSameRunningSession(sessionId: Long): Boolean {
        return runningSessionId == sessionId
    }

    private fun shouldAcceptResult(sessionId: Long): Boolean {
        return isSameRunningSession(sessionId = sessionId) && !cancelledSessionIds.contains(element = sessionId)
    }

    private fun scanResultTimeMillis(): Long {
        val currentTimeMillis: Long = nowMillis()
        if (currentTimeMillis > 0L) {
            return currentTimeMillis
        }
        return 1L
    }

    private fun findLastScanSummary(scanState: LocalMusicScanState): LocalMusicLastScanSummary? {
        return when (scanState) {
            LocalMusicScanState.Idle,
            LocalMusicScanState.WaitingForPermission,
            -> null
            is LocalMusicScanState.Importing -> scanState.previousSummary
            is LocalMusicScanState.Scanning -> scanState.previousSummary
            is LocalMusicScanState.Done -> scanState.summary
            is LocalMusicScanState.Cancelled -> scanState.summary
            is LocalMusicScanState.Error -> scanState.summary
        }
    }

    private fun logLocalMusicScan(message: String) {
        println("$LOCAL_MUSIC_SCAN_LOG_PREFIX $message")
    }
}
```

- [x] **步骤 5：让门面委派扫描**

在 `MusicAppController.kt` 先声明延迟初始化属性：

```kotlin
    private val localMusicScanController: LocalMusicScanController
```

在 `init` 中完成 `libraryStateSynchronizer` 赋值后再初始化扫描控制器：

```kotlin
        localMusicScanController = LocalMusicScanController(
            scanLocalMusicUseCase = scanLocalMusicUseCase,
            permissionSettingsOpener = permissionSettingsOpener,
            nowMillis = nowMillis,
            resolveLikedSongIdsForScan = ::resolveLikedSongIdsForScan,
            shouldConfirmPermissionSettingsBeforeScan = { state: MusicAppUiState ->
                libraryStateSynchronizer.shouldConfirmPermissionSettingsBeforeScan(state = state)
            },
            publishStateUpdate = ::reduceUiState,
        )
```

公开方法改为：

```kotlin
    fun requestLocalMusicScan(request: LocalMusicScanRequest = LocalMusicScanRequest.Refresh) {
        controllerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            scanLocalMusic(request = request)
        }
    }

    suspend fun scanLocalMusic(request: LocalMusicScanRequest = LocalMusicScanRequest.Refresh) {
        localMusicScanController.scanLocalMusic(
            state = uiState,
            request = request,
            onLibrarySnapshot = { snapshot: LibrarySnapshot ->
                syncLibrarySnapshot(snapshot = snapshot)
            },
        )
    }

    fun openPermissionSettingsDialog() {
        localMusicScanController.openPermissionSettingsDialog()
    }

    fun closePermissionSettingsDialog() {
        localMusicScanController.closePermissionSettingsDialog()
    }

    fun confirmPermissionSettings() {
        localMusicScanController.confirmPermissionSettings()
    }
```

删除门面中 `isLocalMusicScanRunning`、`isLocalMusicScanCancellationRequested`、`currentLocalMusicScanJob`、`cancelRunningLocalMusicScan`、`publishCancelledLocalMusicScanIfRunning`、`publishCancelledLocalMusicScan`、`logLocalMusicScan`、`scanResultTimeMillis`、`findLastScanSummary` 和 `shouldConfirmPermissionSettingsBeforeScan`。

- [x] **步骤 6：运行任务七测试和扫描回归**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.library.LocalMusicScanControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.scanCompletionKeepsCurrentLocalMusicRoute" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.scanEntryDuringRunningScanDoesNotStartSecondScan" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.scanStateSettlesWhenRunningScanCoroutineIsCancelledExternally" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.cancelledScanStateIsDistinctFromDoneAndError" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.lateScanResultAfterCancellationDoesNotOverwriteCancelledStateOrQueue"
```

预期：全部通过。

- [x] **步骤 7：提交任务七**

运行：

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/library/LocalMusicScanController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/library/LocalMusicScanControllerTest.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "拆分本地扫描工作流"
```

## 任务八：把加载上次播放数据改成带身份的一次性请求

**文件：**

- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt`
- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStore.kt`
- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackRestoreOrchestrator.kt`
- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- 修改：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStoreTest.kt`
- 修改：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt`
- 修改：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriterTest.kt`
- 修改：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackRestoreOrchestratorTest.kt`
- 修改：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt`

**接口：**

- 消费：`PlaybackSnapshotStore.getSavedSnapshotIdentity()`、`PlaybackSnapshotStore.restoreSnapshot(availableSongIds)`、`PlaybackRestoreOrchestrator.createPendingRequest()`、`PlaybackRestoreOrchestrator.restore(...)`。
- 产出：`PlaybackSnapshotIdentity`、`PendingPlaybackSnapshotRequest`、一次性且可失效的加载上次播放数据请求。只有公开 `restorePlaybackSnapshot()` 能创建新请求；扫描完成和首次完整曲库加载只能续上已有请求。

完整队列约束在 `PlaybackRestoreOrchestrator` 层执行：保存队列中的歌曲实体和当前歌曲实体没有全部解析前，不调用 `PlaybackSnapshotStore.restoreSnapshot(...)`。不要为了这个约束删除 `PlaybackSnapshotStore` 底层过滤失效歌曲的能力；底层 store 测试继续证明它能安全过滤，App 编排测试证明门面不会部分加载。

- [x] **步骤 1：写待加载请求失效回归测试**

在 `MusicAppControllerTest` 的加载上次播放数据测试附近加入：

```kotlin
    /**
     * 用户显式播放后，旧待加载请求失效，后续扫描完成不能覆盖用户的新播放意图。
     */
    @Test
    fun explicitPlayInvalidatesPendingPlaybackSnapshotRequest(): Unit = runTest {
        val snapshotStore = InMemoryPlaybackSnapshotStore()
        snapshotStore.saveSnapshot(
            snapshot = com.yanhao.kmpmusic.domain.model.PlaybackSnapshot(
                playbackState = PlaybackState(
                    currentSongId = "seed:8",
                    status = PlaybackStatus.Paused,
                    positionMs = 42_000L,
                    durationMs = 180_000L,
                ),
                queueState = QueueState(
                    songIds = listOf("seed:8"),
                    currentIndex = 0,
                ),
                updatedAt = 1_719_360_000_000L,
            ),
        )
        val controller = createController(
            musicLibraryRepository = SeededMusicLibraryRepository(seedCount = 2),
            localMusicScanner = RecordingLocalMusicScanner(),
            playbackSnapshotStore = snapshotStore,
            controllerScope = backgroundScope,
        )

        controller.restorePlaybackSnapshot()
        val userSong: Song = controller.uiState.homeLocalSongPreview.first()
        controller.playSong(song = userSong, queueSongs = controller.uiState.homeLocalSongPreview)
        advanceUntilIdle()
        controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        advanceUntilIdle()

        assertEquals(expected = userSong.id, actual = controller.uiState.currentSongId)
        assertEquals(expected = userSong.id, actual = controller.uiState.currentSong?.id)
        assertEquals(
            expected = controller.uiState.homeLocalSongPreview.map { song: Song -> song.id },
            actual = controller.uiState.queueSongIds,
        )
        assertEquals(expected = 0L, actual = controller.uiState.playbackPositionMs)
        assertEquals(expected = PlaybackStatus.Playing, actual = controller.uiState.playbackStatus)
    }
```

在 `MusicAppPlaybackRestoreOrchestratorTest` 中新增身份和完整队列测试：

```kotlin
    /**
     * 同一队列但当前项、进度或更新时间不同，应视为不同保存快照身份。
     */
    @Test
    fun snapshotIdentityIncludesCurrentSongPositionAndUpdatedAt(): Unit = runTest {
        val firstStore: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore()
        firstStore.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(currentSongId = "song-1", positionMs = 1_000L),
                queueState = QueueState(songIds = listOf("song-1", "song-2"), currentIndex = 0),
                updatedAt = 10L,
            ),
        )
        val secondStore: InMemoryPlaybackSnapshotStore = InMemoryPlaybackSnapshotStore()
        secondStore.saveSnapshot(
            snapshot = PlaybackSnapshot(
                playbackState = PlaybackState(currentSongId = "song-2", positionMs = 2_000L),
                queueState = QueueState(songIds = listOf("song-1", "song-2"), currentIndex = 1),
                updatedAt = 20L,
            ),
        )

        assertTrue(actual = firstStore.getSavedSnapshotIdentity() != secondStore.getSavedSnapshotIdentity())
    }

    /**
     * 队列没有完整解析时只能保留待加载请求，不能触发播放协调器恢复。
     */
    @Test
    fun restoreWaitsUntilEntireSavedQueueAndCurrentSongAreResolvable(): Unit = runTest {
        val restoredCalls: MutableList<List<String>> = mutableListOf()
        val store: PlaybackSnapshotStore = snapshotStoreWithQueue(songIds = listOf("song-1", "song-2"))
        val orchestrator = PlaybackRestoreOrchestrator(
            playbackSnapshotStore = store,
            availableSongsResolver = { _, preferredSongs: List<Song> -> preferredSongs },
            restoreSnapshot = { songs: List<Song> ->
                restoredCalls += songs.map { song: Song -> song.id }
            },
        )
        val request: PendingPlaybackSnapshotRequest = orchestrator.createPendingRequest()
            ?: error("保存快照应创建待加载请求")

        val result: PlaybackRestoreOrchestrator.Result = orchestrator.restore(
            state = testState(),
            preferredSongs = listOf(testSong(id = "song-1")),
            pendingRequest = request,
            isRequestCurrent = { true },
        )

        assertEquals(expected = request, actual = result.pendingRequest)
        assertEquals(expected = emptyList(), actual = restoredCalls)
        assertEquals(expected = null, actual = result.queueSongsSnapshot)
    }
```

- [x] **步骤 2：新增保存快照身份模型和存储接口**

在 `PlaybackModels.kt` 的 `PlaybackSnapshot` 后加入：

```kotlin
/**
 * 保存快照身份，用来判断一次加载上次播放数据请求是否仍然对应同一份持久化快照。
 *
 * @property queueSongIds 保存队列中的歌曲标识，顺序必须参与身份判断。
 * @property currentSongId 保存时的当前歌曲标识。
 * @property currentIndex 保存时的当前队列下标。
 * @property positionMs 保存时的播放进度。
 * @property updatedAt 保存快照更新时间。
 */
data class PlaybackSnapshotIdentity(
    val queueSongIds: List<String>,
    val currentSongId: String?,
    val currentIndex: Int,
    val positionMs: Long,
    val updatedAt: Long,
)

/** 把完整快照压缩成可比较身份。 */
val PlaybackSnapshot.identity: PlaybackSnapshotIdentity
    get() = PlaybackSnapshotIdentity(
        queueSongIds = queueState.songIds,
        currentSongId = playbackState.currentSongId,
        currentIndex = queueState.currentIndex,
        positionMs = playbackState.positionMs,
        updatedAt = updatedAt,
    )
```

在 `PlaybackSnapshotStore` 接口增加：

```kotlin
    /**
     * 读取最近保存快照的身份；没有可加载队列时返回 null。
     */
    suspend fun getSavedSnapshotIdentity(): PlaybackSnapshotIdentity?
```

内存实现：

```kotlin
    override suspend fun getSavedSnapshotIdentity(): PlaybackSnapshotIdentity? {
        if (snapshot.queueState.songIds.isEmpty()) {
            return null
        }
        return snapshot.identity
    }
```

Room 实现：

```kotlin
    override suspend fun getSavedSnapshotIdentity(): PlaybackSnapshotIdentity? {
        val snapshotEntity: PlaybackSnapshotEntity = database.playbackSnapshotDao().getSnapshot()
            ?: return null
        val queueSongIds: List<String> = database.playbackQueueDao().getQueueItems().map { item: PlaybackQueueItemEntity ->
            item.songId
        }
        if (queueSongIds.isEmpty()) {
            return null
        }
        return PlaybackSnapshotIdentity(
            queueSongIds = queueSongIds,
            currentSongId = snapshotEntity.currentSongId,
            currentIndex = snapshotEntity.currentIndex,
            positionMs = snapshotEntity.positionMs,
            updatedAt = snapshotEntity.updatedAt,
        )
    }
```

同步修复测试里的自定义 `PlaybackSnapshotStore`，每个实现都按自身保存的 `PlaybackSnapshot` 返回 `snapshot.identity` 或 `null`。

必须显式修改这些测试 fake：

```text
composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt
composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriterTest.kt
```

每个 fake store 都增加同形实现；如果 fake 内部保存变量名不是 `snapshot`，使用该 fake 已有的快照字段：

```kotlin
    override suspend fun getSavedSnapshotIdentity(): PlaybackSnapshotIdentity? {
        if (snapshot.queueState.songIds.isEmpty()) {
            return null
        }
        return snapshot.identity
    }
```

- [x] **步骤 3：改造 `PlaybackRestoreOrchestrator`**

把 `Result` 改成只返回要合入最新 UI 状态的队列实体快照，避免把挂起前的旧 `MusicAppUiState` 整包写回：

```kotlin
    data class Result(
        val queueSongsSnapshot: List<Song>?,
        val pendingRequest: PendingPlaybackSnapshotRequest?,
        val didHydrateSnapshot: Boolean,
    )
```

在文件中新增：

```kotlin
/**
 * 待加载上次播放数据请求，绑定首次读取到的保存快照身份。
 */
data class PendingPlaybackSnapshotRequest(
    val identity: PlaybackSnapshotIdentity,
)
```

新增显式创建请求入口，并把 `restore` 签名改为只消费已有请求：

```kotlin
    suspend fun createPendingRequest(): PendingPlaybackSnapshotRequest? {
        val savedIdentity: PlaybackSnapshotIdentity = playbackSnapshotStore.getSavedSnapshotIdentity()
            ?: return null
        return PendingPlaybackSnapshotRequest(identity = savedIdentity)
    }

    suspend fun restore(
        state: MusicAppUiState,
        preferredSongs: List<Song>,
        pendingRequest: PendingPlaybackSnapshotRequest?,
        isRequestCurrent: (PendingPlaybackSnapshotRequest) -> Boolean,
    ): Result
```

核心逻辑按下面代码实现：

```kotlin
        val request: PendingPlaybackSnapshotRequest = pendingRequest
            ?: return Result(
                queueSongsSnapshot = null,
                pendingRequest = null,
                didHydrateSnapshot = false,
            )
        val savedIdentity: PlaybackSnapshotIdentity = playbackSnapshotStore.getSavedSnapshotIdentity()
            ?: return Result(
                queueSongsSnapshot = null,
                pendingRequest = null,
                didHydrateSnapshot = false,
            )
        if (savedIdentity != request.identity) {
            return Result(
                queueSongsSnapshot = null,
                pendingRequest = null,
                didHydrateSnapshot = false,
            )
        }
        val availableSongs: List<Song> = availableSongsResolver(
            request.identity.queueSongIds,
            preferredSongs,
        )
        val availableSongIds: Set<String> = availableSongs.map { song: Song -> song.id }.toSet()
        val hasCompleteQueue: Boolean = request.identity.queueSongIds.all { songId: String ->
            availableSongIds.contains(element = songId)
        }
        val hasCurrentSong: Boolean = request.identity.currentSongId?.let { songId: String ->
            availableSongIds.contains(element = songId)
        } ?: false
        if (!hasCompleteQueue || !hasCurrentSong) {
            return Result(
                queueSongsSnapshot = null,
                pendingRequest = request,
                didHydrateSnapshot = false,
            )
        }
        if (!isRequestCurrent(request) || playbackSnapshotStore.getSavedSnapshotIdentity() != request.identity) {
            return Result(
                queueSongsSnapshot = null,
                pendingRequest = null,
                didHydrateSnapshot = false,
            )
        }
        restoreSnapshot(availableSongs)
        return Result(
            queueSongsSnapshot = availableSongs,
            pendingRequest = null,
            didHydrateSnapshot = true,
        )
```

- [x] **步骤 4：让门面持有描述符和进行中保护**

在 `MusicAppController.kt` 把：

```kotlin
    private var isPlaybackRestorePending: Boolean = false
```

替换为：

```kotlin
    private var pendingPlaybackSnapshotRequest: PendingPlaybackSnapshotRequest? = null
    private var playbackSnapshotHydrationJob: Job? = null
    private var playbackSnapshotHydrationGeneration: Long = 0L
```

增加 import：

```kotlin
import com.yanhao.kmpmusic.feature.app.playback.PendingPlaybackSnapshotRequest
```

把 `restorePlaybackSnapshot()` 改为：

```kotlin
    /**
     * 按可用曲库加载上次播放数据；只加载队列和进度，最终保持暂停，不自动播放。
     */
    suspend fun restorePlaybackSnapshot() {
        if (playbackSnapshotHydrationJob?.isActive == true) {
            return
        }
        val request: PendingPlaybackSnapshotRequest = pendingPlaybackSnapshotRequest
            ?: playbackRestoreOrchestrator.createPendingRequest()
            ?: run {
                pendingPlaybackSnapshotRequest = null
                return
            }
        pendingPlaybackSnapshotRequest = request
        hydratePendingPlaybackSnapshot(request = request)
    }

    private suspend fun hydratePendingPlaybackSnapshot(request: PendingPlaybackSnapshotRequest) {
        val generationAtStart: Long = playbackSnapshotHydrationGeneration
        val result: PlaybackRestoreOrchestrator.Result = playbackRestoreOrchestrator.restore(
            state = uiState,
            preferredSongs = preferredKnownSongs(),
            pendingRequest = request,
            isRequestCurrent = { currentRequest: PendingPlaybackSnapshotRequest ->
                pendingPlaybackSnapshotRequest == currentRequest &&
                    playbackSnapshotHydrationGeneration == generationAtStart
            },
        )
        if (playbackSnapshotHydrationGeneration != generationAtStart) {
            return
        }
        result.queueSongsSnapshot?.let { queueSongsSnapshot: List<Song> ->
            reduceUiState { currentState: MusicAppUiState ->
                currentState.copy(queueSongsSnapshot = queueSongsSnapshot)
            }
        }
        pendingPlaybackSnapshotRequest = result.pendingRequest
    }
```

把续加载入口改为：

```kotlin
    private fun restorePlaybackSnapshotIfPending() {
        if (pendingPlaybackSnapshotRequest == null) {
            return
        }
        if (playbackSnapshotHydrationJob?.isActive == true) {
            return
        }
        playbackSnapshotHydrationJob = controllerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val request: PendingPlaybackSnapshotRequest = pendingPlaybackSnapshotRequest
                    ?: return@launch
                hydratePendingPlaybackSnapshot(request = request)
            } finally {
                if (playbackSnapshotHydrationJob == currentCoroutineContext()[Job]) {
                    playbackSnapshotHydrationJob = null
                }
            }
        }
    }

    private fun clearPendingPlaybackSnapshotRequest() {
        pendingPlaybackSnapshotRequest = null
        playbackSnapshotHydrationGeneration += 1
        playbackSnapshotHydrationJob?.cancel()
        playbackSnapshotHydrationJob = null
    }
```

在 `playSong`、`playRecentSong`、`removeFromQueue`、`moveTrack`、`skipToQueueIndex`、`seekTo` 和 `cyclePlaybackMode` 开头调用 `clearPendingPlaybackSnapshotRequest()`。`togglePlayback`、`play` 和 `pause` 只改变播放/暂停状态，不改变当前歌曲、队列或进度，不清理待加载请求；如果执行中发现它们会写持久播放事实，必须暂停并补测试后再决定。

- [x] **步骤 5：运行待加载播放快照测试**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.playback.MusicAppPlaybackRestoreOrchestratorTest" --tests "com.yanhao.kmpmusic.domain.persistence.PlaybackSnapshotStoreTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.restorePlaybackSnapshotRestoresAfterLibraryLoads" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.restorePlaybackSnapshotDoesNotAutoScanWhenLibraryIsEmpty" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.explicitPlayInvalidatesPendingPlaybackSnapshotRequest"
```

预期：全部通过；`explicitPlayInvalidatesPendingPlaybackSnapshotRequest` 证明旧请求不会在扫描完成后覆盖用户播放意图。

- [x] **步骤 6：提交任务八**

运行：

```bash
git add composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/model/PlaybackModels.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStore.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/playback/PlaybackRestoreOrchestrator.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/persistence/PlaybackSnapshotStoreTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinatorTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackSnapshotWriterTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/playback/MusicAppPlaybackRestoreOrchestratorTest.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppControllerTest.kt
git commit -m "加固加载上次播放数据请求身份"
```

## 任务九：更新 OpenWiki、最终验证和对抗式审查证据

**文件：**

- 修改：`openwiki/architecture/app-architecture.md`
- 修改：`openwiki/testing/verification-guide.md`
- 检查：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- 检查：全部新增工作流控制器和测试文件。

**接口：**

- 消费：前八个任务的实现和测试结果。
- 产出：更新后的架构入口说明、测试入口说明、最终验证记录、公开方法签名变化说明和剩余风险说明。

- [ ] **步骤 1：更新 OpenWiki 架构说明**

把 `openwiki/architecture/app-architecture.md` 中 `Controller 和状态所有权` 小节的协作者列表更新为包含：

```markdown
- `SystemBackController`：系统返回时关闭权限弹窗、清缓存弹窗、单曲更多面板、队列和二级页面。
- `PreferenceStateController`：播放倍速和本地音频发现偏好保存与 UI 状态同步。
- `SearchResultController`：按搜索上下文派生歌曲、专辑和歌手结果。
- `ContentNavigationController`：首页分段、本地音乐、扫描页、最近播放、专辑详情和歌手详情导航。
- `PlaybackActionController`：播放、队列、进度、音量、播放模式和退出快照动作。
- `LocalMusicScanController`：本地扫描会话、取消、权限错误和旧事件丢弃。
```

- [ ] **步骤 2：更新 OpenWiki 测试指南**

把 `openwiki/testing/verification-guide.md` 中 App controller 高价值测试文件补充为：

```markdown
- `feature/app/system/SystemBackControllerTest.kt`：系统返回优先级。
- `feature/app/preferences/PreferenceStateControllerTest.kt`：播放倍速和本地音乐发现偏好。
- `feature/app/search/SearchResultControllerTest.kt`：搜索结果数据源和 pending query 空结果。
- `feature/app/navigation/ContentNavigationControllerTest.kt`：内容导航、按需加载完整曲库和详情入口。
- `feature/app/playback/PlaybackActionControllerTest.kt`：播放动作、队列复用、音量和队列不变量。
- `feature/app/library/LocalMusicScanControllerTest.kt`：扫描会话取消、旧事件丢弃和权限确认。
```

- [ ] **步骤 3：运行完整共享测试**

运行：

```bash
./gradlew :composeApp:desktopTest
```

预期：通过。

- [ ] **步骤 4：运行 Android 编译**

运行：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

预期：通过。

- [ ] **步骤 5：运行最终组合验证**

运行：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
```

预期：通过。

- [ ] **步骤 6：做交付前对抗式审查**

逐项确认下面 5 个最可能翻车点都有证据：

```markdown
1. 扫描取消：旧成功、旧错误和旧 finally 不会覆盖新会话或取消态。
2. 搜索结果动作：只有搜索页播放、打开专辑、打开歌手前会提交当前搜索词。
3. 播放队列：`queueSongIds` 在播放、切队列、移除队列和加载上次播放数据后都能解析为同长度 `queueSongs`。
4. 加载上次播放数据：请求带保存快照身份，用户显式播放或队列动作后旧请求失效，加载成功后保持暂停并恢复进度。
5. 公开门面：UI、Android、Desktop 和 iOS 调用方仍只依赖 `MusicAppController` 公开方法。
```

- [ ] **步骤 7：检查公开方法签名和工作区状态**

运行：

```bash
rg -n "^    (fun|suspend fun) " composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt
git status --short --branch
```

预期：`MusicAppController` 仍保留原有公开入口；如果有公开方法删除、改名或参数变化，交付说明必须列出调用方同步范围。`git status --short --branch` 只显示本轮源码、测试和 OpenWiki 文件，不包含构建产物、`.scratch/`、IDE 状态、日志、Node 依赖、原型 dist、APK/DMG 或本地缓存。

- [ ] **步骤 8：提交任务九**

运行：

```bash
git add openwiki/architecture/app-architecture.md openwiki/testing/verification-guide.md
git commit -m "更新控制器拆分文档"
```

## 自检

- spec 覆盖：计划覆盖唯一公开门面、低副作用优先、内容导航、播放动作、本地扫描、搜索结果动作、待加载播放快照身份、异步写回、队列不变量、OpenWiki 更新和最终验证。
- 占位扫描：本文没有留下空白占位、泛泛而谈的错误处理步骤、无代码的测试步骤或跨任务省略引用。
- 类型一致性：计划中的新增公开内部接口为 `SystemBackController.Result`、`PreferenceStateController`、`SearchResultController.search`、`ContentNavigationController.Result`、`PlaybackActionController`、`LocalMusicScanController.scanLocalMusic`、`PendingPlaybackSnapshotRequest` 和 `PlaybackSnapshotIdentity`，后续任务引用名称保持一致。
