# App 架构

KMP Music 是一个单模块 Kotlin Multiplatform App。核心架构是在 `commonMain` 中共享状态、领域和 UI，平台 source set 提供入口、扫描器、数据库 builder 和播放引擎。

## 分层

仓库遵循 `README.md` 描述并由源码布局体现的 `core / domain / data / feature` 拆分：

```text
feature -> domain <- data
core    -> theme and UI foundation
platform source sets -> entrypoints and platform adapters
```

关键目录：

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/core/theme`：主题、尺寸、颜色、封面调色逻辑。
- `.../domain/model`、`domain/repository`、`domain/usecase`、`domain/playback`、`domain/persistence`：平台无关模型、契约、use case、播放协调、Room entity/DAO。
- `.../data`：common 内存和持久化 repository 实现。
- `.../feature/app`：App 级状态、controller、导航、移动端 layout/chrome、playerbar、对话框/面板、搜索/收藏/曲库同步器。
- `.../feature/screen`：移动端/根页面 Composable。
- `.../feature/desktop`：用 common code 组合的 Desktop 专用共享 UI。
- `androidMain`、`desktopMain`、`iosMain`：平台入口和适配器。

## 入口

### 共享移动端 App 入口

`App.kt` 在没有注入 controller 时创建 `MusicAppController`，并渲染 `MusicApp`：

- Source：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/App.kt`
- `MusicApp.kt` 应用 `KmpMusicTheme`，安装 `PlatformBackHandler`，在首页有持久化统计但没有本地歌曲时自动加载完整曲库，并渲染 `MobileAppLayout`。

### Android

`MainActivity.kt` 是 Android 入口。它负责：

- 启用 edge-to-edge 并管理状态栏/导航栏外观。
- 创建 `AndroidAudioPermissionRequester`。
- 获取进程级 `MusicAppViewModel`。
- 接入 `AndroidMediaStoreScanner` 和权限设置打开器。
- 为播放通知请求 Android 13+ notification permission。
- 处理打开播放器的播放通知 intent。
- 暴露仅 debuggable 可用的专辑详情和收藏列表性能 harness intent。

真实 Android 运行时的 controller 构造位于 `AndroidPlaybackControllerFactory.kt`，其中接入 Room-backed repositories、`RoomPlaybackSnapshotStore`、`PersistentMusicLibraryRepository`、持久化收藏/搜索 repository，以及 `PlaybackServiceConnector`。

### Desktop

`DesktopMain.kt` 创建 Compose Desktop `Window`，通过 `DesktopPlaybackSession` 恢复播放快照，应用 macOS 透明标题栏属性，并渲染 `DesktopMusicApp`。

Desktop controller 构造位于 `DesktopPlaybackControllerFactory.kt`；它注入：

- `DesktopFolderMusicScanner`
- Room-backed 本地曲库/收藏/搜索/播放 repositories
- `RoomPlaybackSnapshotStore`
- 来自 desktop runtime/session 层的 Desktop audio engine

`DesktopAppLayout.kt` 定义 Desktop shell：标题栏、导航 rail、home root 上可选的曲库 sidebar、workspace route 区域、持久底部播放器、对话框/面板，以及 `SecondaryScreen.Player` 激活时的全屏 desktop player。

### iOS

`IosEntry.kt` 通过 `ComposeUIViewController` 暴露 `MainViewController()`。它使用 `IosFolderMusicScanner` 创建共享 `MusicAppController` 并渲染 `App`。在已检查源码中未看到 iOS 真实播放引擎；如果后续添加，应同步更新文档。

## Controller 和状态所有权

`MusicAppController.kt` 是中心状态拥有者。它暴露 Compose 可观察的 `uiState: MusicAppUiState`，并协调：

- 本地曲库 preview/loading/scanning。
- 收藏及由收藏派生的列表。
- 搜索 query、active debounced query、history、scope/context。
- 播放、队列、播放模式、seek、音量、最近播放历史。
- 导航、对话框、队列面板、单曲 more 面板。
- 主题模式以及 session/login/dialog 状态。

controller 有意作为小型协作者之上的 facade：

- `LibraryStateSynchronizer`：扫描/加载和共享列表投影。
- `FavoriteStateSynchronizer`：收藏切换和收藏列表投影。
- `PlaybackUiStateSynchronizer`：把 repository 播放/队列状态投影为 UI state。
- `PlaybackRestoreOrchestrator`：在能够解析足够歌曲后处理冷启动播放恢复。
- `SearchSessionController`：debounced search 和搜索历史 reducer。
- `NavigationStateController`：根/二级导航的纯 reducer。

默认构造依赖是内存/fake 实现，因此 common 测试和简单 preview 可以不依赖平台适配器运行。Android/Desktop factory 会注入持久化 repository 和真实播放/扫描适配器。

## 导航模型

`MusicAppModels.kt` 定义根路由和二级路由：

- 根 Tab：`Home`、`Favorites`、`Me`。
- 二级页面：`Search(context)`、`Player`、`AlbumDetail`、`ArtistDetail`、`Settings`、`About`、`Login`、`LocalMusic(initialSection)`。

`NavigationStateController.kt` 集中处理路由转换：

- `navigateToRoot` 重置二级路由，并把选中的 tab 作为新的返回基线。
- `navigateToSecondary` 保留根 origin，递增 `secondaryEntryId`，并把现有二级页面压入简单 back stack。
- `navigateBack` 优先弹出二级 stack；没有二级 stack 时返回 `previousRootTab`。

`NavigationState` 还计算滚动状态 key。根页面按 root tab 保留滚动；二级页面使用 route 加 entry id，使每个新二级入口有隔离状态。Chrome overlay 页面使用 underlay key 保持被覆盖页面稳定。

## 移动端 layout 和 chrome

移动端渲染拆分在：

- `feature/app/layout/MobileAppLayout.kt`：外层受约束 App surface、背景、underlay content、固定 player/bottom nav、overlay 页面、对话框、面板。
- `feature/app/layout/MobileContentLayout.kt`：status/navigation padding、saveable state holder、根/二级内容路由、full-bleed 页面例外。
- `feature/app/layout/MobilePlayerOverlayGesture.kt`：全屏播放器下拉关闭；近期测试断言半屏阈值行为。
- `feature/app/playerbar/*`：全局迷你播放器、固定 player bar、底部导航。

唯一的 route-to-chrome 策略入口是 `MusicAppModels.kt` 中的 `mobileFixedBarModeFor`：

| 路由类别 | Mode | 行为 |
| --- | --- | --- |
| 根 Tab | `TopLevel` | 底部 Tab 可见；有歌曲时迷你播放器显示在其上方。 |
| Search、album detail、artist detail、settings、login、local music | `SecondaryWithMiniPlayer` | 底部 Tab 隐藏；有歌曲时全局迷你播放器贴在底部。 |
| About | `SecondaryWithoutChrome` | 覆盖底层 chrome；没有底部 Tab 或迷你播放器。 |
| Player | `Player` | 全屏播放器 overlay；没有全局迷你播放器/底部 Tab。 |

不要在页面中重复实现迷你播放器。它是由 App layout/playerbar 层拥有的全局 chrome。

## Desktop UI 架构

Desktop UI 使用同一套 controller/state，但在 `feature/desktop` 下有 Desktop 专用 shell：

- `DesktopMusicApp.kt`：Desktop 的 theme/state wrapper。
- `layout/DesktopAppLayout.kt`：顶层窗口布局和全屏播放器切换。
- `navigation/DesktopNavigationRail.kt` 与 desktop route 文件：rail destinations 和 route mapping。
- `screens/*`：desktop home/favorites/search/local music/me/settings/detail screens。
- `player/*`：持久底部播放器和全屏播放器详情组件。

Desktop 保留左侧导航和底部播放器语义，而不是照搬移动端底部 Tab。

## 设计和近期演进

近期 git 历史显示 UI 架构工作集中在：

- 基于 Figma 的移动端收藏页重设计（`FavoritesScreen`、tokens、row/header 组件）。
- 专辑和歌手详情页，包括分组规则和移除 demo 歌曲数据。
- 播放器全屏 chrome 和 Android 导航栏颜色行为。
- 全屏播放器关闭手势修复。
- 专辑详情和 500 行收藏压力测试的性能 harness。

这意味着很多看似视觉层的规则其实编码在共享 state/chrome 策略和测试中，而不只是页面 Composable。

## 架构陷阱

- 不要把 Android/iOS/Desktop API 放进 `commonMain` UI/domain。
- 不要绕过 `MusicAppController` 管理播放/收藏/搜索状态。
- 不要添加 route 专用的迷你播放器或底部 Tab 副本。
- 除非用户明确要求原型工作，不要编辑 prototype 代码来修复生产 App 行为。
- 注意 `PlaybackDatabase` 的职责比名称更广；它存储本地歌曲、收藏、搜索历史、播放快照/队列和播放历史。
