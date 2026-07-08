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
3. 启动播放观察、冷启动恢复等生命周期动作。
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
| `PlaybackActionController` | 管理播放入口、队列动作、进度跳转、音量、播放模式和退出前快照补写 | `PlaybackCoordinator`、`PlaybackRepository`、`PlaybackSnapshotStore` |
| `ContentNavigationController` | 管理打开首页分段、本地音乐、扫描页、最近播放、专辑详情和歌手详情 | `NavigationStateController`、`LibraryStateSynchronizer`、`SearchSessionController` |
| `SearchResultController` | 管理搜索数据源选择、结果派生和结果动作前搜索历史提交 | `MusicLibraryRepository`、`SearchSessionController` |
| `PreferenceStateController` | 管理主题模式和本地音乐发现偏好的保存与状态同步 | `UserPreferencesRepository` |
| `SystemBackController` | 管理系统返回时关闭弹窗、面板、队列和二级页面的优先级 | `NavigationStateController`、`LoginAndDialogStateController` |

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

`MusicAppController` 继续保留少数跨模块时序规则：

1. 播放状态同步后发布 `playbackUiObserver`。
2. 曲库扫描完成后，如果存在待恢复请求，再触发恢复。
3. 首次加载完整曲库后，如果存在待恢复请求，再触发恢复。
4. 初始化时按既有顺序装配仓库、用例、协调器和同步器。

## 行为保持要求

以下行为必须等价保持：

1. 扫描中再次触发扫描时取消当前扫描，并发布 `Cancelled` 状态。
2. 平台报告用户取消时进入取消态，而不是成功态或错误态。
3. 权限永久拒绝后再次扫描前先弹确认，再打开系统设置。
4. `playSong` 未传入队列且歌曲已在当前队列中时，复用当前队列。
5. 最近播放入口播放歌曲时，使用完整最近播放列表作为队列。
6. 搜索历史只在防抖结果生效、显式提交或点击搜索结果时记录。
7. 打开专辑或歌手详情前提交当前搜索词，并加载完整曲库。
8. 冷启动恢复在歌曲不足时挂起，曲库加载或扫描后再恢复。
9. 系统返回优先关闭权限弹窗、缓存弹窗、more 面板、队列，再返回二级页面。
10. 根 Tab 切换继续清空二级页面，二级页面返回栈语义保持不变。

## 迁移顺序

### 第一批：低副作用模块

先拆 `SystemBackController`、`PreferenceStateController` 和 `SearchResultController`。

这三块主要是同步状态归约、简单仓库写入或派生结果，能快速降低 `MusicAppController` 体量，同时风险较低。

### 第二批：内容导航工作流

再拆 `ContentNavigationController`。

该模块会跨搜索、曲库和导航，包含打开本地音乐、扫描页、最近播放、首页分段、专辑详情和歌手详情等入口。迁移时要保留搜索词提交和按需加载完整曲库的时序。

### 第三批：高副作用模块

最后拆 `PlaybackActionController` 和 `LocalMusicScanController`。

这两个模块涉及协程、播放协调器、快照持久化、扫描取消和待恢复请求，最容易引入未知缺陷。只有前两批拆分稳定后再迁移。

## 测试策略

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
4. 待恢复请求和曲库加载顺序。
5. 系统返回关闭浮层优先级的端到端行为。
6. 搜索结果动作提交历史的端到端行为。

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

本次重构仍可能引入未知缺陷，尤其是状态流、协程时序、扫描取消、播放队列、搜索历史和恢复播放时序。风险控制方式如下：

1. 每次只拆一个工作流，不同时大改扫描和播放。
2. `MusicAppControllerTest` 先作为回归网保留，不急着删除。
3. 新模块补聚焦测试，证明模块自己的单一职责行为。
4. 扫描、播放、待恢复请求等高风险逻辑最后拆。
5. 若某个拆分需要改变行为，默认暂停并单独确认。
6. 交付前做对抗式审查，列出最可能翻车点和验证证据。

## 验收标准

1. `MusicAppController` 的外部公开接口基本兼容，UI 和平台调用方无需大规模修改。
2. 每个新模块能用一句话说明职责，且不混合多个用户工作流。
3. `MusicAppController` 明显变薄，目标约为三百到四百五十行。
4. 新模块都有对应聚焦测试。
5. 关键集成测试继续保留在 `MusicAppControllerTest`。
6. 通过 `:composeApp:desktopTest` 和 `:composeApp:compileDebugKotlinAndroid`。
7. 最终交付说明包含对抗式审查结论、验证命令和剩余风险。

## 后续计划

本文档通过评审后，下一步进入实现计划阶段。实现计划应按迁移顺序拆成可独立验证的小步骤，并在每一步明确要迁移的控制器方法、要新增或下沉的测试，以及对应验证命令。
