# 代码架构优化调整设计

## 背景

KMP Music 当前已经具备本地曲库、播放、收藏、搜索、导航、Android 真实播放和 macOS Desktop 播放链路。随着功能持续叠加，部分文件开始承担过多职责，典型信号包括：

- `MusicAppController.kt` 接近 1000 行，集中处理导航、扫描、播放、收藏、搜索、设置、弹窗和派生列表。
- `MusicAppControllerTest.kt` 超过 1500 行，说明大量规则只能通过同一个大集成测试文件覆盖。
- `PlaybackCoordinator.kt`、`DesktopVlcjAudioPlayerEngine.kt`、`MusicApp.kt`、桌面 UI 文件也已经形成大文件。
- 本机 Kotlin 规范要求保持单一职责，并把 200 行作为可读性参考，但项目现状更需要先治理职责边界，而不是机械压缩行数。

本设计聚焦项目级架构优化调整。目标不是重写功能，也不是一次性把所有文件切到固定行数，而是在不破坏现有行为的前提下，把职责、依赖和测试边界逐步拆清楚。

## 第一性原则

这次优化以职责清晰为硬目标，以行数为风险信号。一个文件是否需要拆分，优先看四个问题：

1. 文件是否有多个变化原因。
2. 公开 API 是否过多，调用者是否难以判断该找哪个方法。
3. 测试是否只能通过巨大集成测试覆盖。
4. 读者是否必须上下滚动很多次才能理解一个行为。

行数不是唯一标准。超过 300 行的文件需要审视，超过 600 行的文件基本必须进入拆分候选；但如果一个 120 行文件混合了平台 I/O、业务规则和 UI 状态写入，也应调整。反过来，如果一个较长文件只是稳定的声明集合或资源映射，并且职责单一，可以暂缓拆分。

## 总体目标

- 保持当前功能逻辑不变，不破坏导航、播放、队列、收藏、搜索、扫描、最近播放和底部 chrome。
- 保留 `MusicAppController` 作为 UI 和平台宿主的外部入口，先把它变薄，而不是让调用方大面积震荡。
- 保持播放主干不变：`MusicAppController -> PlaybackCoordinator -> AudioPlayerEngine -> 平台实现`。
- 新增协作者承接内部职责，不让 UI 直接依赖 repository、platform engine 或平台实现。
- 拆分测试承载点，让纯逻辑和小协作者能被单独测试。
- 不引入第三方 DI，不改变现有 KMP source set 边界。

## 非目标

- 不为了达到 200 行而做无意义转发层。
- 不重写播放器架构。
- 不改 `AudioPlayerEngine` 契约。
- 不把真实媒体扫描或播放逻辑塞进 UI 层。
- 不把移动端、桌面端或 iOS 平台 API 引入 `commonMain`。
- 不修改高保真原型来解决生产 App 的结构问题。
- 不删除失败测试来制造通过结果。

## 分阶段路线

### 第一阶段：App 状态主干

第一阶段处理 `MusicAppController.kt` 和 `MusicAppControllerTest.kt`。

`MusicAppController` 继续作为外部 facade，保留现有公开方法，例如：

- `navigateToSecondary`
- `navigateToRoot`
- `navigateBack`
- `handleSystemBack`
- `scanLocalMusic`
- `restorePlaybackSnapshot`
- `openLocalMusic`
- `openSearch`
- `playSong`
- `openSong`
- `togglePlayback`
- `toggleFavorite`
- `search`
- `setThemeMode`

内部新增协作者承接职责：

| 协作者 | 建议目录 | 职责 |
| --- | --- | --- |
| `NavigationStateController` | `feature/app/navigation` | 一级/二级导航、返回键消费顺序、进入页面时关闭队列和更多菜单。 |
| `SearchSessionController` | `feature/app/search` | 搜索输入、active query 节流、搜索历史隔离、离开搜索页前提交历史。 |
| `LibraryStateSynchronizer` | `feature/app/library` | 扫描结果同步、完整曲库按需加载、最近播放可见列表、收藏实体补齐、扫描权限门控状态。 |
| `MusicLibraryProjector` | `feature/app/library` | 纯逻辑，把歌曲聚合成专辑、歌手、收藏专辑、收藏歌手。 |
| `FavoriteStateSynchronizer` | `feature/app/favorites` | 切换收藏后同步首页预览、全量曲库、队列快照、收藏列表和播放 UI。 |
| `PlaybackUiStateSynchronizer` | `feature/app/playback` | 把 `PlaybackState` 和 `QueueState` 同步到 `MusicAppUiState`，不接管播放业务语义。 |
| `PlaybackRestoreOrchestrator` | `feature/app/playback` | 编排持久化播放快照恢复、缺失歌曲实体解析、曲库未就绪时的挂起恢复。 |
| `LoginAndDialogStateController` | `feature/app/session` | 邮箱、登录邮件状态、清缓存弹窗等非核心业务状态；不负责扫描权限弹窗。 |

第一阶段完成后，`MusicAppController` 应主要负责：

- 依赖组装。
- 持有唯一 `MusicAppUiState`。
- 维护 Compose 可观察状态。
- 暴露外部调用方法。
- 调用协作者完成具体状态迁移。

### 第二阶段：播放协调器内部复杂度

第二阶段处理 `PlaybackCoordinator.kt` 和 `PlaybackCoordinatorTest.kt`。

保持 `PlaybackCoordinator` 的公共角色不变：它仍是 common 层播放业务语义唯一协调器。内部可抽出：

- `PlaybackQueueNavigator`：计算上一首、下一首和目标下标。
- `ShuffleQueuePolicy`：维护随机播放历史和剩余集合。
- `PlaybackSnapshotWriter`：统一异步快照写入、退出前同步收口和 pending 写入等待。
- `PlaybackFailurePolicy`：处理单曲循环失败阈值和连续失败跳过。
- `PlaybackHistoryRecorder`：维护最近播放历史。

这一步不能让 UI、Android service 或 Desktop engine 绕过 `PlaybackCoordinator` 写 common 播放状态。

### 第三阶段：共享和桌面 UI 大文件

第三阶段处理 UI 结构，不改变视觉和交互。

共享移动端 UI：

- `MusicApp.kt` 拆出 App shell、root screen router、secondary screen router、bottom chrome、mini player、bottom sheet action。
- 保持一级页面和二级页面的 bottom chrome 规则不变。

桌面 UI：

- `DesktopMusicScreens.kt` 拆成 root screens、secondary screens、search screens、detail screens、local music sections。
- `DesktopMusicComponents.kt` 拆成 navigation、forms、buttons、tables、cards、sections。
- `DesktopMusicPlayer.kt` 和 `DesktopPlayerDetailScreen.kt` 后续按播放器 shell、控制区、封面区、队列区拆分。

UI 阶段只做结构迁移，不改颜色、间距、圆角、排版和交互语义。

### 第四阶段：平台适配层

第四阶段处理平台层较大的适配文件。

Desktop 播放：

- `DesktopVlcjAudioPlayerEngine.kt` 拆出 command queue、adapter event reducer、progress polling、ack 管理、media source mapping。
- 保持 vlcj 事件只通过 `PlaybackEngineEvent` 回流 common。

Android 播放：

- `PlaybackMediaCommands.kt` 可拆成 custom command 定义、按钮构建、按钮状态解析、dispatcher。
- Notification、MediaSession callback、MediaController bridge 继续只做平台适配，不拥有业务队列规则。

平台阶段不能把 Android、Desktop 或 iOS 类型泄漏到 `commonMain`。

## 第一阶段数据流

第一阶段外部数据流保持不变：

```text
UI / Android session / Desktop session
  -> MusicAppController public methods
  -> internal collaborators
  -> repositories / use cases / PlaybackCoordinator
  -> MusicAppUiState
  -> Compose UI
```

`MusicAppController` 仍然唯一持有 `uiState`。协作者不能各自持有一份 UI 状态真相。

协作者允许采用三种形态：

1. 纯函数或纯服务：输入歌曲、历史、状态片段，输出派生结果。
2. 状态 reducer：输入当前 `MusicAppUiState` 和事件，返回新的 `MusicAppUiState`。
3. delegate：持有 repository、use case 或 coroutine scope，执行特定职责并返回状态变更结果。

不允许的形态：

- 协作者直接暴露 Compose `mutableStateOf`。
- 协作者直接被页面 Composable 调用来绕过 `MusicAppController`。
- 多个协作者各自保存当前歌曲、队列、搜索词或收藏集合。

## 关键边界

### 导航

导航协作者负责 root tab、secondary screen、返回键消费、进入页面时关闭临时浮层。它不读取 repository，不处理播放队列，不提交搜索历史。离开搜索页前提交历史仍由 `MusicAppController` 协调搜索协作者完成。

### 搜索

搜索协作者负责 query、active query、节流、历史提交、历史删除和上下文隔离。搜索结果仍由 domain `buildSearchResult` 或后续 UseCase 生成。搜索数据源由 `MusicAppController` 或曲库协作者提供，避免搜索协作者直接决定“全量曲库是否要加载”。

### 曲库

曲库协作者负责从 `MusicLibraryRepository`、扫描快照和当前已知歌曲中形成 UI 所需列表。专辑和歌手聚合统一交给 `MusicLibraryProjector`，避免 controller 和 `MusicAppUiState` 重复实现。

扫描权限门控属于曲库扫描流程，不属于普通弹窗 session。永久拒绝权限后的确认框、用户确认后进入 `WaitingForPermission`、调用 `PermissionSettingsOpener` 这组行为必须由 `MusicAppController` 编排曲库协作者和权限打开适配器完成，不能下沉到登录/清缓存协作者里。

`MusicLibraryProjector` 第一阶段优先作为纯投影器复用在 controller、协作者和 `MusicAppUiState` 派生 getter 中；不要求立即把 `detailAlbums`、`detailArtists`、`favoriteAlbums`、`favoriteArtists` 全部物化为可变 UI state。只有当物化能减少重复查询或明确降低重组成本时，才在后续阶段调整状态形态。

### 收藏

收藏协作者负责在切换收藏后同步所有列表里的 `isLiked` 状态，并重建收藏页歌曲列表。它还必须保持最近播放可见列表、当前播放歌曲和队列快照中的收藏态一致。它不执行播放命令，也不修改导航。

### 播放 UI 同步

播放 UI 同步协作者只把 `PlaybackRepository` / `QueueState` 的状态投影到 `MusicAppUiState`。播放模式、自然结束、失败跳过、随机历史仍由 `PlaybackCoordinator` 负责。快照真正恢复到播放仓库和队列的动作仍调用 `PlaybackCoordinator.restoreSnapshot`，但恢复前的可用歌曲解析、空曲库挂起和扫描后续接属于 controller 或 `PlaybackRestoreOrchestrator` 的编排职责。

持久化播放快照恢复不是单纯的播放 UI 同步。恢复流程需要同时读取 `PlaybackSnapshotStore`、按当前已知歌曲和 `MusicLibraryRepository` 补齐队列实体、在曲库为空时挂起恢复、扫描成功或完整曲库加载后续上恢复。第一阶段可以保留在 `MusicAppController` 中编排，也可以抽出 `PlaybackRestoreOrchestrator`；但不能让 `LibraryStateSynchronizer` 和 `PlaybackUiStateSynchronizer` 各自承担半段恢复逻辑。

### 弹窗和登录

清缓存弹窗、邮箱输入和模拟发送登录邮件可以放进轻量 session 协作者。该协作者不应接触曲库、播放和搜索历史。权限设置弹窗因为会改变扫描状态并调用平台权限设置入口，应留在曲库扫描门控或 `MusicAppController` 编排层。

## 测试策略

第一阶段先运行现有基线：

```bash
./gradlew :composeApp:desktopTest
```

如果改动触及 common UI 或 Android 编译路径，再运行：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

测试拆分方向：

| 测试文件 | 覆盖内容 |
| --- | --- |
| `MusicAppControllerTest` | 保留 facade 级跨职责集成规则。 |
| `MusicAppNavigationControllerTest` | root/secondary 导航、返回键和浮层关闭顺序。 |
| `MusicAppSearchControllerTest` | 搜索 query、active query、历史隔离、提交和清空。 |
| `MusicAppLibraryStateSynchronizerTest` | 扫描快照、按需加载、最近播放可见列表、缺失歌曲解析。 |
| `MusicAppFavoriteStateSynchronizerTest` | 切换收藏后跨列表同步。 |
| `MusicAppPlaybackUiStateSynchronizerTest` | 播放状态、队列 id 和 UI 快照同步。 |
| `MusicAppPlaybackRestoreOrchestratorTest` | 快照歌曲实体解析、空曲库挂起、扫描或完整曲库加载后续接恢复。 |
| `MusicLibraryProjectorTest` | 歌曲到专辑、歌手、收藏专辑、收藏歌手的纯聚合规则。 |

`MusicAppControllerTest` 不再继续承载所有细节，只保留跨职责验收：

- 扫描成功后能续上挂起的播放快照恢复。
- 恢复播放快照在曲库为空时不会自动触发扫描，冷启动有持久曲库时也不会提前加载全量歌曲。
- 播放歌曲后最近播放可见列表同步。
- 收藏状态在首页、全量列表、收藏页、最近播放、队列快照和当前播放 UI 同步。
- 离开搜索页时提交非空搜索词。
- 搜索页自身隐藏顶部搜索入口，不出现两个搜索输入源。
- 永久拒绝权限后再次扫描先弹确认框，不重复触发平台扫描；确认后进入权限设置等待态。
- 队列上下文在歌曲不再来自当前可见列表时仍然保留，移除当前歌曲后 engine 队列和 UI 队列同步。
- 清缓存只关闭弹窗，不删除收藏、播放历史或本地曲库事实。
- 系统返回键按弹窗、更多菜单、队列、二级页面顺序消费。

## 回归防护

- 每一小步迁移前后都运行相关测试。
- 公开方法先保持兼容，不在第一阶段删除或重命名 UI 正在调用的方法。
- 纯逻辑先抽取，再移动状态写入，再拆 facade 测试。
- 与播放相关的改动必须同时关注 `PlaybackCoordinatorTest`。
- 任何无法用现有测试锁住的关键行为，先补测试再迁移。
- 如果某个根治方向会扩大到多阶段之外，先停下来确认，不做临时补丁。

## 验收标准

项目级验收：

- 大文件治理有明确阶段顺序和边界。
- 新增类、文件夹和测试文件都能用一句话说明职责。
- 没有为了缩短文件制造无意义中转层。
- `core / domain / data / feature` 分层仍然成立。
- 平台 API 不进入 `commonMain`。

第一阶段验收：

- `MusicAppController.kt` 明显变薄，主要负责组装和委托。
- `MusicAppControllerTest.kt` 不再是所有规则的唯一承载点。
- 曲库聚合逻辑不再在 controller 和 `MusicAppUiState` 中重复。
- 搜索、收藏、导航、播放 UI 同步各有清晰归属。
- `:composeApp:desktopTest` 通过。
- `:composeApp:compileDebugKotlinAndroid` 通过。

## 风险与取舍

| 风险 | 处理 |
| --- | --- |
| 拆分后状态真相分散 | `MusicAppController` 继续唯一持有 `MusicAppUiState`，协作者只返回状态变更或派生结果。 |
| 公开调用点大面积震荡 | 第一阶段保持 `MusicAppController` facade 方法兼容。 |
| 只搬代码不改善边界 | 每个新增类必须有独立职责、明确依赖和对应测试。 |
| 播放行为回归 | 不改变 `PlaybackCoordinator` 角色，播放语义仍由 common 播放协调器统一管理。 |
| UI 视觉回归 | UI 拆分阶段只做结构迁移，视觉改动另开设计。 |
| 规划过大导致迟迟不能落地 | 第一阶段只处理 App 状态主干，后续阶段独立计划和验证。 |

## 最终判断

当前项目需要的是项目级职责治理，而不是机械行数治理。推荐按主干优先分阶段推进：先把 `MusicAppController` 和测试拆清楚，稳定外部 facade；再拆播放协调器内部策略；之后处理共享 UI、桌面 UI 和平台适配层大文件。

这样可以根治“职责堆积导致阅读困难”的核心问题，同时把行为回归风险控制在可测试、可回滚、可分阶段验证的范围内。
