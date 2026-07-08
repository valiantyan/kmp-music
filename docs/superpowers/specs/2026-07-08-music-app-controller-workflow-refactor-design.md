# MusicAppController 按用户工作流拆分设计

## 背景

`MusicAppController` 当前承担 App 级状态持有、依赖装配、导航、扫描、播放、收藏、搜索、偏好设置、弹窗面板和系统返回处理等职责。类体量已经接近千行，虽然已有 `NavigationStateController`、`SearchSessionController`、`LibraryStateSynchronizer`、`FavoriteStateSynchronizer`、`PlaybackUiStateSynchronizer` 和 `PlaybackRestoreOrchestrator` 等协作者，但主控制器仍直接承载多条用户工作流的关键逻辑。

本次设计的根本目标不是为了减少行数而拆文件，而是减少单个类的变化原因，让每个模块只负责一类用户工作流，并保持现有运行行为稳定。

## 已确认选择

选择 **保留 `MusicAppController` 作为唯一公开门面，并按用户工作流拆分内部协作者**。

不选择让 UI 和平台直接依赖多个模块，因为那会把复杂度扩散到调用方。不选择一次性改成全新的状态存储或用例体系，因为当前项目已经形成了小型协作者加门面的局部模式，继续深化现有模式更稳。

## 设计目标

1. `MusicAppController` 继续作为 UI、Android、Desktop 和 iOS 的唯一公开入口。
2. 外部公开接口基本保持兼容，避免生产 UI 和平台入口跟随大改。
3. 内部按用户工作流拆分，每个新模块只有一个主要变化原因。
4. 拆分以等价迁移为原则，不借重构机会改变导航、播放、搜索、收藏或扫描的用户可见行为。
5. 高风险逻辑最后拆，优先拆纯同步、低副作用模块。
6. 新模块有聚焦测试，`MusicAppControllerTest` 保留关键跨工作流集成测试。

## 非目标

1. 不重写 `PlaybackCoordinator`、仓库、Room 持久化或平台播放实现。
2. 不引入新的状态管理框架。
3. 不改变移动端和桌面端的现有页面调用方式。
4. 不修改原型目录来解决生产 App 问题。
5. 不做顺手优化，除非发现原逻辑存在明确缺陷并单独确认。

## 架构方案

`MusicAppController` 保留四类职责：

1. 持有 Compose 可观察的 `uiState`。
2. 装配仓库、用例、协调器和内部协作者。
3. 启动播放观察、冷启动加载上次播放数据等生命周期动作。
4. 通过公开方法把外部事件委派给对应工作流模块。

目标结构如下：

```text
MusicAppController
├── LocalMusicScanController
├── PlaybackActionController
├── ContentNavigationController
├── SearchResultController
├── PreferenceStateController
└── SystemBackController
```

已有协作者继续保留，新模块会复用它们，而不是替换它们：

```text
NavigationStateController
SearchSessionController
LibraryStateSynchronizer
FavoriteStateSynchronizer
PlaybackUiStateSynchronizer
PlaybackRestoreOrchestrator
LoginAndDialogStateController
```

## 模块职责

| 模块 | 单一职责 | 主要依赖 |
| --- | --- | --- |
| `LocalMusicScanController` | 管理本地扫描启动、运行中取消、权限错误、用户取消和扫描状态结果 | `ScanLocalMusicUseCase`、`LibraryStateSynchronizer`、`PermissionSettingsOpener`、`CoroutineScope` |
| `PlaybackActionController` | 管理会改变播放事实的播放动作、队列动作、进度跳转、音量、播放模式和退出前快照补写 | `PlaybackCoordinator`、`PlaybackRepository`、`PlaybackSnapshotStore` |
| `ContentNavigationController` | 管理打开首页分段、本地音乐、扫描页、最近播放、专辑详情和歌手详情 | `NavigationStateController`、`LibraryStateSynchronizer` |
| `SearchResultController` | 管理搜索数据源选择和结果派生 | `MusicLibraryRepository` |
| `PreferenceStateController` | 管理主题模式和本地音乐发现偏好的保存与状态同步 | `UserPreferencesRepository` |
| `SystemBackController` | 管理系统返回时关闭弹窗、面板、队列和二级页面的优先级 | `NavigationStateController`、`LoginAndDialogStateController` |

搜索结果动作前的历史提交不归 `PlaybackActionController` 或 `ContentNavigationController` 各自处理，而是由 `MusicAppController` 在委派播放、打开专辑和打开歌手之前统一执行。若实现时这条前置规则开始重复，可抽成很小的 `SearchActionRecorder`，但它只负责“当前路由是搜索页时提交当前搜索词”，不负责播放或导航。

`openPlayer()` 不归 `PlaybackActionController`，因为它只进入播放器页面，不改变播放事实、队列、进度、音量、播放模式或持久化快照。它继续由门面通过导航控制器进入 `SecondaryScreen.Player`。

`openSearch()` 是门面级编排，不归 `ContentNavigationController` 或 `SearchSessionController` 单独负责。门面先按上下文决定是否加载完整曲库，再重置搜索会话，最后导航到搜索页。

新增模块继续放在 `feature/app` 下按工作流分包，不下沉到 `domain` 或 `data`。建议位置如下：

```text
feature/app/system/SystemBackController.kt
feature/app/preferences/PreferenceStateController.kt
feature/app/search/SearchResultController.kt
feature/app/navigation/ContentNavigationController.kt
feature/app/playback/PlaybackActionController.kt
feature/app/library/LocalMusicScanController.kt
```

## 公开接口兼容与方法归属

拆分后 `MusicAppController` 继续暴露现有公开入口。迁移目标是把实现委派出去，而不是要求 UI、Android、Desktop 或 iOS 改成直接依赖内部模块。

| 现有公开入口 | 目标归属 | 兼容要求 |
| --- | --- | --- |
| `attachPlaybackUiObserver` | `MusicAppController` | 继续由门面保存观察者，并在播放 UI 投影更新后发布。 |
| `navigateToSecondary`、`navigateToRoot`、`navigateBack`、`setHomeContentSection`、`openHomeSongs` | `NavigationStateController` 和门面 | 公开方法签名保持；根 Tab 切换仍清空二级页面和临时面板。 |
| `handleSystemBack` | `SystemBackController` | 关闭权限弹窗、清缓存弹窗、单曲更多面板、队列、二级页面的优先级不变。 |
| `requestLocalMusicScan`、`scanLocalMusic`、`openPermissionSettingsDialog`、`closePermissionSettingsDialog`、`confirmPermissionSettings` | `LocalMusicScanController` 和门面 | 入口签名保持；门面仍负责从 UI 生命周期外启动扫描协程；扫描模块只报告曲库是否同步，不直接触发播放快照加载。 |
| `restorePlaybackSnapshot` | `PlaybackRestoreOrchestrator` 和门面 | 公开入口可短期保留兼容名称；内部语义是冷启动加载上次播放数据，不自动播放；只有启动期显式加载请求能设置待加载播放快照状态。 |
| `openLocalMusic`、`openAudioScan`、`openRecentPlayed`、`openAlbum`、`openArtist`、`openAlbumFromSong`、`openArtistFromSong` | `ContentNavigationController` | 详情页打开前仍按需加载完整曲库；从搜索结果进入详情时由门面先提交搜索历史。 |
| `openSearch` | `MusicAppController` 门面 | 保持门面级编排：按上下文加载曲库、重置搜索会话并导航到搜索页。 |
| `setSearchQuery`、`setSearchScope`、`commitSearchQueryToHistory`、`selectSearchHistory`、`removeSearchHistoryItem`、`clearSearchHistory`、`search` | `SearchSessionController` 和 `SearchResultController` | 搜索输入、防抖、历史和结果派生规则保持；搜索页外醒来的防抖任务不得写入历史。 |
| `openPlayer` | `NavigationStateController` 和门面 | 只进入播放器二级页，不归播放动作模块。 |
| `playSong`、`playRecentSong`、`togglePlayback`、`play`、`pause`、`moveTrack`、`skipToQueueIndex`、`seekTo`、`cyclePlaybackMode`、`setVolume`、`removeFromQueue`、`persistPlaybackSnapshotForServiceTeardown`、`persistPlaybackSnapshotForProcessTeardown`、`clearRecentPlaybackHistory` | `PlaybackActionController` | 队列解析、最近播放、快照补写、音量范围和系统媒体命令语义保持。 |
| `toggleFavorite`、`toggleCurrentSongFavorite`、`setFavoriteSection` | `FavoriteStateSynchronizer` 和门面 | 收藏事实仍由仓库和同步器投影，播放 UI 观察者继续随收藏变化发布。 |
| `setThemeMode`、`setLocalMusicAutoScanOnLaunchEnabled`、`setLocalMusicShortAudioIgnored`、`setLocalMusicSystemFoldersExcluded` | `PreferenceStateController` | 偏好写入仓库后同步到 UI 状态，并继续流入扫描请求。 |
| `openQueue`、`closeQueue`、`openMore`、`closeMore`、`openClearCacheDialog`、`closeClearCacheDialog`、`confirmClearCache`、`setEmail`、`sendLoginMail` | 现有面板和登录协作者 | 不作为本次新增模块的重点；迁移时只保留现有行为，不扩大功能范围。 |

## 状态写入规则

拆分后的默认数据流如下：

```text
外部事件
  ↓
MusicAppController 公开方法
  ↓
对应工作流控制器
  ↓
返回新的 MusicAppUiState 或触发明确副作用
  ↓
MusicAppController 统一写回 uiState
```

新模块默认接收当前 `MusicAppUiState` 并返回新的 `MusicAppUiState`。只有确实需要协程、取消任务、仓库写入或播放引擎副作用的模块才持有依赖。新增模块不默认接收通用 `setState` 回调，避免状态写入分散。

异步模块必须遵守更严格的写回协议：

1. 异步模块不能持有可变的 `uiState` 引用，也不能缓存旧状态后在协程晚些时候直接写回。
2. 门面应提供唯一状态写入口，例如内部 `reduceUiState` 或等价函数；拆出的异步模块只能提交同步状态归约函数或明确事件，不能直接赋值 `uiState`。
3. 状态归约必须在同一个门面写入口内串行应用；归约函数本身不挂起、不做仓库读写，也不在读旧状态和写新状态之间留下挂起窗口。
4. 状态归约函数必须先检查事件是否仍然适用，例如搜索词是否仍相同、扫描会话是否仍是当前会话、待加载播放快照请求是否仍处于有效状态。
5. 现有 `SearchSessionController` 的防抖发布就是例外样板：它通过 `publishStateUpdate` 提交归约函数，并在归约函数内检查当前搜索词是否仍匹配。后续新增异步模块必须采用同类约束，而不是新增裸 `setState`。
6. 需要同时返回状态和副作用的模块，应让门面先写入状态，再由门面或专属模块触发副作用，避免副作用读取到过期 UI 投影。
7. 实现阶段要补交错场景测试，至少覆盖搜索防抖、扫描取消和播放状态同步在相近时间到达时不会丢失彼此字段更新。

`MusicAppController` 继续保留少数跨模块时序规则：

1. 播放状态同步后发布 `playbackUiObserver`。
2. 曲库扫描完成后，如果存在待加载播放快照请求，再尝试加载上次播放数据。
3. 首次加载完整曲库后，如果存在待加载播放快照请求，再尝试加载上次播放数据。
4. 初始化时按既有顺序装配仓库、用例、协调器和同步器。

## 副作用契约

### 本地扫描事件契约

`LocalMusicScanController` 拆出后必须把一次扫描视为一个独立会话。实现可以使用会话编号、活动 `Job` 或等价机制，但必须满足下面约束：

1. 启动扫描时记录当前会话，发布 `Scanning`，关闭队列和单曲更多面板，并保留上一轮扫描摘要。
2. 扫描中再次触发扫描只取消当前会话，不启动第二个扫描器，也不弹权限确认框。
3. 用户取消或外部协程取消只发布一次 `Cancelled`，并关闭权限弹窗、队列和单曲更多面板。
4. 取消后的旧成功、旧错误或平台晚到回调必须丢弃，不得更新曲库、播放队列、待加载播放快照状态或 `scanState`。
5. 非当前会话的 `finally` 不能清掉新会话的运行中标记，避免旧协程结束覆盖新扫描。
6. 权限永久拒绝后的下一次扫描先进入权限设置确认流程，只有用户确认后才打开系统设置。
7. 平台返回 `UserCancelled` 错误时进入 `Cancelled`，不能映射成 `Done` 或普通错误。
8. 扫描成功同步曲库后只向门面报告曲库已同步，不能直接调用播放快照加载逻辑。

### 播放动作事件契约

`PlaybackActionController` 可以持有 `PlaybackCoordinator`、`PlaybackRepository` 和 `PlaybackSnapshotStore`，但播放事实仍以仓库和协调器为准，UI 状态只保存必要投影。

1. `playSong` 在调用协调器前必须先解析并写入队列实体快照，保证迷你播放器、全屏播放器和冷启动加载上次播放数据能读到同一队列。
2. 未显式传入队列且目标歌曲已在当前队列时，复用当前队列；否则退化为单曲队列。
3. 最近播放入口必须使用完整最近播放列表作为队列，不能使用摘要列表。
4. `seekTo` 既要更新运行态，也要补写持久化快照，避免重启后回到旧进度。
5. Android 播放服务退出和 Desktop 进程退出的快照固化入口保持公开，并继续委派给播放协调器。
6. `openPlayer()` 不属于播放动作模块；它只改变导航状态。

### 搜索结果动作契约

搜索结果动作是跨模块边界，必须由门面统一安排顺序：

1. 当前页面是搜索页且当前搜索词非空时，播放歌曲、打开专辑和打开歌手之前先提交搜索历史。
2. 提交搜索历史后，再委派给播放或内容导航模块。
3. 非搜索页触发的同名动作不能写入搜索历史，例如单曲更多面板中的“查看专辑”和“查看歌手”。
4. 搜索结果派生只读取当前上下文的数据源：本地曲库搜索优先用已加载本地歌曲，必要时读取仓库全量歌曲；收藏搜索只读收藏投影。
5. 防抖词未追上输入词时，搜索结果必须为空，不能把空生效搜索词派生成全量曲库。

### 待加载播放快照契约

待加载播放快照请求只能由冷启动显式加载流程创建，普通扫描或普通加载曲库不能自行发起请求。这里的语义是加载上次播放数据，不会自动开始播放；实现中可把内部动作命名为 `hydrate snapshot`，但文档描述应始终表达为“加载上次播放数据”。`restorePlaybackSnapshot` 公开入口可以短期保留兼容名称，但 KDoc 和内部模型应明确“只加载，不自动播放”。

待加载状态不能只是裸布尔，必须带有可验证的描述符，例如最初读取到的保存队列标识列表、当前歌曲标识、播放进度、快照更新时间或等价版本标记。若队列标识相同但当前项、进度或更新时间变化，也应视为新的快照身份。拆分后必须满足：

1. `restorePlaybackSnapshot` 没有保存队列时立即清除待加载状态。
2. 保存队列存在但歌曲实体不可解析时，只记录待加载描述符，不主动扫描。
3. 不允许部分加载队列；保存队列中的歌曲实体必须完整解析，且当前歌曲必须可解析，才能加载上次播放数据。
4. 保存队列完整解析成功后调用播放协调器加载上次播放数据，并把待加载状态清除。
5. 加载时必须恢复上次播放进度，并同步到播放协调器和音频引擎运行态；最终播放状态保持暂停，不能自动播放。
6. 待加载描述符必须绑定最初读取到的保存快照身份；后续再次加载前若持久化快照身份已变化，旧待加载请求立即失效。
7. `playSong`、`playRecentSong`、`removeFromQueue`、精确切队列、切换曲目或其他会改变当前播放事实的用户动作必须清除旧待加载请求，避免扫描完成后用旧快照覆盖用户的新播放意图。
8. 曲库扫描完成和首次加载完整曲库都可以尝试续上待加载请求，但同一时刻只能有一个加载协程在执行。
9. 进行中保护必须可测试：启动加载前先登记当前加载 `Job`、进行中布尔或等价标记；重复触发只复用或跳过当前加载，不再启动第二个协程；协程结束时只清理自己的标记。
10. 调用播放协调器加载前必须重新校验待加载描述符仍有效；若加载进行中时待加载描述符被用户动作或新快照失效，加载结果必须丢弃，不能写队列快照或触发播放协调器。
11. 加载成功或确认无可加载内容后，后续普通扫描不得再次触发加载；冷启动缺歌曲实体时也不得自动触发本地扫描。

## 行为保持要求

以下行为必须等价保持：

1. 扫描中再次触发扫描时取消当前扫描，并发布 `Cancelled` 状态。
2. 平台报告用户取消时进入取消态，而不是成功态或错误态。
3. 权限永久拒绝后再次扫描前先弹确认，再打开系统设置。
4. `playSong` 未传入队列且歌曲已在当前队列中时，复用当前队列。
5. 最近播放入口播放歌曲时，使用完整最近播放列表作为队列。
6. 搜索历史只在防抖结果生效、显式提交或点击搜索结果时记录。
7. 打开专辑或歌手详情前提交当前搜索词，并加载完整曲库。
8. 冷启动加载上次播放数据在歌曲不足时挂起，不自动扫描；曲库加载或用户主动扫描完成后再尝试加载。
9. 系统返回优先关闭权限弹窗、缓存弹窗、单曲更多面板、队列，再返回二级页面。
10. 根 Tab 切换继续清空二级页面，二级页面返回栈语义保持不变。
11. 旧扫描结果晚到时不能覆盖取消态、曲库、队列或待加载播放快照状态。
12. 待加载播放快照请求必须是一次性的，绑定保存快照身份，并且有进行中保护。
13. 用户显式播放或修改队列后，旧待加载播放快照请求必须失效。
14. 加载上次播放数据必须恢复上次进度并同步运行态，但最终保持暂停，不自动播放。
15. 保存队列不能部分加载；队列歌曲和当前歌曲完整解析后才能加载。

## 迁移顺序

### 第一批：低副作用模块

先拆 `SystemBackController`、`PreferenceStateController` 和 `SearchResultController`。

这三块主要是同步状态归约、简单仓库写入或派生结果，能快速降低 `MusicAppController` 体量，同时风险较低。

### 第二批：内容导航工作流

再拆 `ContentNavigationController`。

该模块会跨曲库和导航，包含打开本地音乐、扫描页、最近播放、首页分段、专辑详情和歌手详情等入口。搜索词提交和 `openSearch()` 仍由门面编排，迁移时要保留按需加载完整曲库的时序。

### 第三批：高副作用模块

最后拆 `PlaybackActionController`、`LocalMusicScanController`，并修正待加载播放快照的描述符与失效规则。

这些模块涉及协程、播放协调器、快照持久化、扫描取消和待加载播放快照请求，最容易引入未知缺陷。只有前两批拆分稳定后再迁移。

## 测试策略

测试迁移的第一原则是先保留 `MusicAppControllerTest` 作为跨工作流回归网，再把纯模块规则下沉到新测试。每拆一个模块，至少先写或迁移一个能失败的聚焦测试，再做实现迁移。

新增聚焦测试：

```text
SystemBackControllerTest
PreferenceStateControllerTest
SearchResultControllerTest
ContentNavigationControllerTest
PlaybackActionControllerTest
LocalMusicScanControllerTest
```

`MusicAppControllerTest` 保留跨工作流集成测试，尤其是：

1. 扫描完成后路由不被改坏。
2. 播放队列和 UI 状态同步。
3. 收藏、搜索、详情跳转联动。
4. 待加载播放快照请求和曲库加载顺序。
5. 系统返回关闭浮层优先级的端到端行为。
6. 搜索结果动作提交历史的端到端行为。

### 必须保留或迁移的测试清单

| 行为区域 | 聚焦测试目标 | 门面回归测试必须保留 |
| --- | --- | --- |
| 系统返回 | `SystemBackControllerTest` 覆盖关闭权限弹窗、清缓存弹窗、单曲更多面板、队列和二级页面优先级 | `systemBackClosesPermissionSettingsDialog`、`systemBackReturnsFromSecondaryScreen`、`systemBackClosesOverlayBeforeSecondaryScreen` |
| 偏好设置 | `PreferenceStateControllerTest` 覆盖主题、本地扫描偏好持久化和状态同步 | `localMusicDiscoveryPreferencesPersistAndFlowIntoScanner` |
| 搜索输入和历史 | 保留 `MusicAppSearchControllerTest`，新增或迁移搜索结果数据源测试到 `SearchResultControllerTest` | `pendingSearchQueryDoesNotReturnFullLibraryBeforeDebounce`、`debouncedSearchQueryPublishesActiveQueryThroughFacade`、`nonBlankSearchQueryDoesNotCommitToHistoryWhenLeavingSearchBeforeDebounce` |
| 搜索结果动作 | `SearchResultControllerTest` 只覆盖结果派生；搜索历史前置动作保留门面测试或小型 `SearchActionRecorderTest` | `searchResultActionsCommitCurrentQueryToHistory`、`nonSearchResultActionsDoNotCommitSearchHistory` |
| 内容导航 | `ContentNavigationControllerTest` 覆盖本地音乐、扫描页、最近播放、首页分段、专辑详情、歌手详情 | `openLocalMusicUsesSecondaryFixedBarMode`、`openAudioScanUsesDedicatedScanRoute`、`meViewAllRecentPlayedOpensRecentPageAndReturnsToMe`、`openArtistFromSongUsesNormalizedArtistName` |
| 门面级编排 | 门面测试覆盖 `openSearch()` 的搜索会话加导航组合，以及 `openPlayer()` 的纯导航行为 | `homeSearchOpensLocalLibrarySearchContext`、`favoritesSearchOpensFavoritesSearchContext`、`openPlayerUsesFullscreenSecondaryScreen` |
| 播放动作 | `PlaybackActionControllerTest` 覆盖队列解析、最近播放队列、进度跳转、音量、退出快照；`openPlayer()` 不进入该模块 | `playSongUpdatesPlaybackAndQueue`、`playSongUsesProvidedQueueSongs`、`playSongWithoutProvidedQueueKeepsCurrentQueueWhenSongExists`、`playRecentPageSongUsesFullRecentQueueWithClickedStart`、`playerScreenAndBottomPlayerReadSamePlaybackState` |
| 异步状态写入 | 门面测试覆盖搜索防抖、扫描取消和播放状态同步交错到达时不丢字段；新异步模块测试必须验证旧事件会被归约函数拒绝；实现计划必须列出具体测试名和需要的可控假实现 | `debouncedSearchQueryPublishesActiveQueryThroughFacade` |
| 本地扫描 | `LocalMusicScanControllerTest` 覆盖权限、用户取消、协程取消、上一轮摘要保留；旧结果丢弃必须拆成旧成功、旧错误、旧结束清理、新会话不被旧会话覆盖四类测试 | `scanCompletionKeepsCurrentLocalMusicRoute`、`scanEntryDuringRunningScanDoesNotStartSecondScan`、`scanStateSettlesWhenRunningScanCoroutineIsCancelledExternally`、`cancelledScanStateIsDistinctFromDoneAndError` |
| 待加载播放快照请求 | `PlaybackRestoreOrchestratorTest` 继续覆盖快照解析；新增门面测试覆盖待加载描述符绑定保存快照身份、用户显式播放后失效、扫描完成和首次加载同时续加载只启动一次、进行中加载失效后丢弃结果、缺实体不自动扫描、完整队列解析后才加载、加载后保持暂停但进度同步；失效测试必须用可记录的假播放协调器证明没有触发过期加载副作用 | `restorePlaybackSnapshotRestoresAfterLibraryLoads`、`restorePlaybackSnapshotDoesNotAutoScanWhenLibraryIsEmpty` |
| 播放队列不变量 | `PlaybackActionControllerTest` 和门面回归测试在每个改变队列或当前项的动作后断言 `queueSongIds` 能解析为同长度 `queueSongs`，并保留当前歌曲实体可解析；覆盖 `playSong`、`skipToQueueIndex`、`removeFromQueue`、`moveTrack` 和加载上次播放数据路径 | `playSongUpdatesPlaybackAndQueue`、`removeCurrentSongKeepsEngineQueueInSync`、`moveTrackChangesCurrentSong` |

最小验证命令：

```bash
./gradlew :composeApp:desktopTest
./gradlew :composeApp:compileDebugKotlinAndroid
```

最终验证优先使用组合命令：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
```

## 风险控制

本次重构仍可能引入未知缺陷，尤其是状态流、协程时序、扫描取消、播放队列、搜索历史和播放快照加载时序。风险控制方式如下：

1. 每次只拆一个工作流，不同时大改扫描和播放。
2. `MusicAppControllerTest` 先作为回归网保留，不急着删除。
3. 新模块补聚焦测试，证明模块自己的单一职责行为。
4. 扫描、播放、待加载播放快照请求等高风险逻辑最后拆。
5. 若某个拆分需要改变行为，默认暂停并单独确认。
6. 交付前做对抗式审查，列出最可能翻车点和验证证据。
7. 实现拆分后同步更新 OpenWiki 中的架构和测试入口，避免文档仍指向旧职责边界。
8. 实现验收时用公开方法签名对比、职责归属清单和测试清单逐项核对，避免只用行数判断拆分是否成功。

## 验收标准

1. `MusicAppController` 的外部公开接口基本兼容，UI 和平台调用方无需大规模修改。
2. 每个新模块能用一句话说明职责，且不混合多个用户工作流。
3. `MusicAppController` 明显变薄；三百到四百五十行只是参考目标，不作为牺牲可读性或行为安全的硬指标。
4. 新模块都有对应聚焦测试。
5. 关键集成测试继续保留在 `MusicAppControllerTest`。
6. 通过 `:composeApp:desktopTest` 和 `:composeApp:compileDebugKotlinAndroid`。
7. 最终交付说明包含对抗式审查结论、验证命令和剩余风险。
8. 若实现改变 OpenWiki 所描述的主控制器、测试入口或工作流归属，必须同步更新对应 OpenWiki 文档。
9. 最终交付要附公开方法签名变化说明；若公开方法有删除、改名或参数变化，必须说明调用方同步范围。
10. 最终交付要附待加载播放快照、扫描取消、异步写入和播放队列不变量的测试证据。

## 后续计划

本文档通过评审后，下一步进入实现计划阶段。实现计划应按迁移顺序拆成可独立验证的小步骤，并在每一步明确要迁移的控制器方法、要新增或下沉的测试，以及对应验证命令。
