# 产品工作流和页面地图

本页把用户可见流程连接到源码文件。修改跨页面、controller 状态、导航、播放、收藏或本地曲库数据的行为时，请参考本页。

## 信息架构

移动端根 Tab 固定为：

```text
首页 / 收藏 / 我的
```

其他所有页面都是二级页面。这个规则记录在 `CONTEXT.md`、`README.md` 和 `docs/PRD.md` 中，并由 `feature/app/MusicAppModels.kt` 中的 `RootTab`/`SecondaryScreen` 实现。

Desktop 保留同一产品结构，但使用 desktop shell：导航 rail、可选曲库 sidebar、workspace 和持久底部播放器。

## 路由和 chrome 工作流

### 根 Tab 切换

源码路径：

- `feature/app/navigation/NavigationStateController.kt`
- `feature/app/MusicAppController.kt`
- `feature/app/routes/MobileRootScreenRoute.kt`
- `feature/desktop/navigation/DesktopRootScreenRoute.kt`

根 Tab 变化会重置二级路由状态并关闭临时面板。在移动端，根 Tab 渲染底部导航；存在歌曲时显示全局迷你播放器。

### 二级导航

二级页面通过 `MusicAppController.navigateToSecondary` 和导航 reducer 入栈。reducer 保留 root origin，并把现有二级页面堆叠起来，因此 settings → about 或 detail → player 这类流程可以正确返回。

移动端 chrome 由 `MobileFixedBarMode` 计算：

- 普通二级页面隐藏底部 Tab，并保留迷你播放器。
- `About` 覆盖底层 chrome，且没有迷你播放器。
- `Player` 是带独立控制和下拉关闭手势的全屏 overlay。

近期修复集中在底部 chrome 闪烁和播放器关闭手势行为。避免添加临时的逐页面 chrome 动画逻辑。

## 首页工作流

主要来源：

- `feature/screen/HomeScreen.kt`
- `HomeTopAppBar.kt`、`HomeFilterChips.kt`、`HomeSongRow.kt`、`HomeAlbumGrid.kt`、`HomeArtistList.kt`、`HomeAlbumEmptyState.kt`
- `feature/app/routes/MobileRootScreenRoute.kt`
- `feature/desktop/screens/DesktopHomeScreen.kt`

首页是本地曲库浏览和继续播放的主入口。它包含歌曲、专辑和歌手内容区。专辑/歌手与本地音乐页来自同一本地曲库来源；它们不是单独导航 Tab。

用户动作：

- 搜索图标打开 `SearchContext.LocalLibrary`。
- 点击歌曲调用 controller 播放，并把当前列表作为 queue context。
- 点击专辑打开专辑详情。
- 点击歌手打开歌手详情。
- 空状态/本地扫描入口调用 `scanLocalMusic` 或打开本地音乐。

修改风险：

- 在根首页加载所有本地歌曲可能昂贵；尽量保留 preview 与 full-load 的区分。
- 当前播放歌曲样式是全局规则，应在各列表保持一致。

## 收藏工作流

主要来源：

- `feature/screen/FavoritesScreen.kt`
- `FavoritesActionHeader.kt`、`FavoritesFigmaTokens.kt`、`FavoritesSectionItems.kt`、`FavoritesSongRow.kt`
- `feature/app/favorites/FavoriteStateSynchronizer.kt`
- `feature/desktop/screens/DesktopFavoritesScreen.kt`

收藏是根 Tab，会把喜欢的歌曲投影成歌曲、专辑和歌手区块。近期 git 历史显示移动端默认收藏歌曲视图进行了 Figma 驱动的重设计。`docs/superpowers/specs/2026-07-04-mobile-favorites-figma-redesign.md` 说明应保留全局迷你播放器、底部 Tab、播放队列、收藏切换、more menu 和 favorites-context search。

用户动作：

- 搜索打开 `SearchContext.Favorites`。
- 全部播放从收藏歌曲开始队列。
- 点击歌曲在当前收藏列表 context 中播放。
- 心形按钮切换收藏状态，并必须同步到所有投影。
- More 打开全局单曲 more 面板。

修改风险：

- 收藏中的专辑/歌手由喜欢的歌曲派生。除非产品模型变化，不要新增独立持久化 favorite-album 表。
- 保留移动端默认歌曲区块的 Figma spacing/tokens。

## 我的、设置、登录和关于

主要来源：

- `feature/screen/MeScreen.kt`
- `feature/screen/UtilityScreens.kt`
- `feature/desktop/screens/DesktopMeScreen.kt`
- `feature/desktop/screens/DesktopSettingsAndLoginScreens.kt`

`我的` 是个人/音乐资产入口的根 Tab。它暴露本地音乐、统计、登录和设置相关流程。登录目前展示 UI/magic-link 文案；在已检查源码中未发现真实 auth/cloud-sync 集成。

设置包含主题和本地扫描/source 操作，以及偏占位的未来能力行。About 是无迷你播放器的二级页面，不应意外触发播放器 overlay 动画语义。

## 本地音乐工作流

主要来源：

- `feature/screen/LocalMusicScreen.kt`
- `LocalMusicSourceSection.kt`
- `domain/usecase/ScanLocalMusicUseCase.kt`
- 平台扫描器文件
- `feature/desktop/screens/DesktopLocalMusicScreen.kt`

本地音乐是二级页面，包含歌曲、专辑、歌手和来源区块。它消费 controller state 中的 `LibrarySnapshot`。sources 区块显示最近扫描结果中的 source summaries 和 problems。

扫描流程：

1. UI 用 `LocalMusicScanRequest` 调用 controller scan action。
2. `ScanLocalMusicUseCaseImpl` 调用注入的平台 scanner。
3. Scanner 返回平台无关的 `LocalMusicScanResult`。
4. `MusicLibraryRepository.applyScanResult` 合并并返回 `LibrarySnapshot`。
5. Controller 发布更新后的 UI state 和 scan state。

各 source set 的平台行为不同；见 [数据、持久化和平台集成](../data-persistence/data-and-platform-integrations.md)。

## 搜索工作流

主要来源：

- `feature/screen/SearchScreen.kt`
- `feature/app/search/SearchSessionController.kt`
- `domain/usecase/SearchMusicUseCase.kt`
- `data/PersistentSearchHistoryRepository.kt`
- desktop search screen

搜索是带 context（`LocalLibrary` 或 `Favorites`）和 scope（`All`、`Songs`、`Albums`、`Artists`）的二级页面。输入 query 会先 debounce，再成为 active search。controller 在导航边界把搜索词提交到按 context 隔离的历史。

修改风险：

- 从收藏和本地曲库进入搜索时要保持 context。
- 搜索应使用当前曲库/收藏投影，而不是过期 mock 列表。

## 专辑详情工作流

主要来源：

- `feature/screen/AlbumDetailScreen.kt`
- `AlbumDetailContent.kt`、`AlbumDetailHeader.kt`、`AlbumDetailPlayAllButton.kt`、`AlbumDetailSongRow.kt`
- `domain/model/AlbumIdentity.kt`
- `feature/app/library/MusicLibraryProjector.kt`
- `feature/desktop/screens/DesktopDetailScreens.kt`

专辑详情显示专辑封面、标题、歌手和完整专辑歌曲列表。近期历史移除了 demo 歌曲数据，因此生产详情页必须从当前队列、曲库、首页 preview 和收藏投影解析真实歌曲。

期望行为：

- 从专辑卡片或歌曲专辑入口打开。
- 全部播放使用完整专辑歌曲列表作为队列。
- 点击歌曲从专辑队列播放，并以被点歌曲作为起点。
- 如果无法解析专辑，显示 `MissingLibraryItemScreen`，而不是 fake data。

## 歌手详情工作流

主要来源：

- `feature/screen/ArtistDetailScreen.kt`
- `ArtistDetailContent.kt`、`ArtistDetailHeaderComponents.kt`、`ArtistDetailSongRow.kt`
- `ArtistDetailBackground.kt`、`ArtistDetailCollapsingChrome.kt`、`ArtistDetailPullStretch.kt`、`ArtistDetailScrollBehavior.kt`、`ArtistDetailToolbar.kt`
- `domain/model/ArtistIdentity.kt`
- `feature/desktop/screens/DesktopDetailScreens.kt`

歌手详情是沉浸式/折叠式移动端二级页面。它聚合该歌手的所有歌曲，即使 UI 文案写“热门歌曲”；`CONTEXT.md` 明确说明数据不应裁剪成少量推荐歌曲。

近期历史包括沉浸背景、状态栏可读性、滚动跳动/空白暴露、快速滚动性能、播放入口位置和底部回弹修复。把滚动/chrome 行为视为已测试的产品行为。

## 播放器和队列工作流

主要来源：

- `feature/screen/PlayerScreen.kt`
- `PlayerScreenControls.kt`、`PlayerScreenMetadata.kt`、`PlayerScreenProgress.kt`、`PlayerScreenVisuals.kt`
- `feature/app/playerbar/*`
- `feature/app/surfaces/AppPanels.kt`
- `domain/playback/PlaybackCoordinator.kt`
- Android/Desktop 播放适配器文件

播放从 controller 方法开始，这些方法把选中歌曲和 queue context 传给 `PlaybackCoordinator`。完整播放器是移动端 overlay route；迷你播放器是全局 App chrome。队列和 more 面板是全局 surface，不是页面局部 overlay。

期望行为：

- 当前歌曲、进度、播放模式和喜欢状态在迷你播放器、完整播放器、desktop 底部播放器、队列和歌曲行之间同步。
- 移动端播放器只有在 app underlay 暴露足够多时，下拉才会关闭；阈值行为有测试覆盖。
- Android 通知/系统媒体命令必须通过共享 playback/session state 派发，而不是私有 Activity state。

## 性能和视觉工作流

近期源码包含 Android debug-only harness：

- Album detail performance harness。
- Favorites 500-row performance harness。

它们由 `MainActivity.kt` 中的 debuggable explicit intents gate，并在 `AndroidPerformanceHarnesses.kt` 中实现。调查这些高风险页面的滚动卡顿或回归时可使用它们。

做视觉工作时，在修改 spacing 或 hierarchy 前，先检查附近的 `*FigmaTokens.kt` 文件和相关 `docs/superpowers/specs/*` 规格。
