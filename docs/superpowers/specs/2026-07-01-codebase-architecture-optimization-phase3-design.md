# 代码架构优化第三阶段设计

## 背景

第一阶段已经把 App 状态主干从 `MusicAppController` 中拆出，第二阶段已经把 `PlaybackCoordinator` 内部播放规则拆成独立策略和协作者。当前剩余的主要结构压力集中在 UI 大文件：

- `MusicApp.kt` 同时承载手机端入口、外层布局、页面路由、固定底栏、迷你播放器、底部导航、弹窗和底部面板。
- `DesktopMusicApp.kt` 同时承载桌面入口、窗口布局、路由、侧栏、底部播放器和全局弹层接入。
- `DesktopMusicScreens.kt` 聚合了桌面一级页、二级页、搜索、本地音乐、设置、登录、详情页和页面辅助函数。
- `DesktopMusicComponents.kt` 聚合了桌面导航、按钮、表单、表格、卡片、分区和日期格式化。
- `DesktopMusicPlayer.kt` 与 `DesktopPlayerDetailScreen.kt` 承载桌面底部播放器和播放详情页的多个子区域。

第三阶段目标是治理 UI 文件职责边界，而不是重做 UI。所有移动端和桌面端 UI 大文件都进入本阶段，但只做结构拆分、文件迁移、命名治理和 import 调整。

## 第一性原则

第三阶段以职责清楚为第一成功标准。文件数可以增加，但每个新增文件都必须能用一句话说明职责。

本阶段遵守这些原则：

- 不改变颜色、间距、圆角、排版、动效节奏、交互流程、播放语义或导航语义。
- 保留 `MusicApp(controller: MusicAppController)` 和 `DesktopMusicApp(controller: MusicAppController)` 两个外部入口。
- 页面路由只负责根据导航状态选择页面，不写具体页面布局细节。
- 固定导航和播放器栏不进入页面文件，保持全局 UI 归属清楚。
- 弹窗和面板从 App 主入口中剥离，但仍由 `MusicAppController` 统一驱动。
- 不为了行数制造纯转发层；拆出的组件必须有清晰职责、复用边界或独立变化原因。
- 生产代码中避免继续使用有歧义的 `shell`、`chrome`、`overlays` 命名。

## 非目标

- 不重做视觉设计。
- 不修改高保真原型。
- 不改变 `MusicAppController` 的业务职责。
- 不改播放、导航、搜索、收藏、扫描或持久化状态逻辑。
- 不引入 screenshot testing 框架。
- 不为了测试方便暴露额外 public API。
- 不回改历史 spec 或 plan 里的旧术语；历史文档保留当时上下文。

## 命名治理

本阶段新增目录和生产代码命名避免 `shell`、`chrome`、`overlays` 这些容易和命令行、浏览器或抽象浮层混淆的词。

推荐替换方向：

| 现有或旧设计名 | 新命名方向 | 说明 |
| --- | --- | --- |
| App shell / Desktop shell | App 外层布局 / `layout` | 表示窗口或手机容器的整体布局。 |
| bottom chrome / global chrome | 固定底栏 / `playerbar` | 表示迷你播放器、底部 Tab 和底部播放器组合。 |
| `BottomChrome` | `MobileFixedPlayerBar` | 手机端固定底栏组合。 |
| `BottomChromePlacement` | `MobileFixedBarPlacement` | 手机端固定底栏位置策略。 |
| `AppChromeMode` | `MobileFixedBarMode` | 页面对应的固定底栏和内容避让模式。 |
| `chromeMode` | `fixedBarMode` | 导航状态派生出的固定底栏模式。 |
| `AppOverlays` | `AppDialogs` + `AppPanels` | 拆成跨端弹窗和面板入口；不再使用 overlays 命名。 |

命名治理只限 UI 结构相关命名。状态模型重命名必须与对应测试同步，且不改变任何行为。

## 总体目录

App 侧 UI 建议新增：

- `feature/app/layout`
- `feature/app/dialogs`
- `feature/app/panels`
- `feature/app/surfaces`

手机端专属 UI 建议新增：

- `feature/app/routes`
- `feature/app/playerbar`

桌面端建议新增：

- `feature/desktop/layout`
- `feature/desktop/navigation`
- `feature/desktop/screens`
- `feature/desktop/components`
- `feature/desktop/player`

## 手机端结构

### `feature/app/MusicApp.kt`

继续作为公开入口，只保留：

- `MusicApp(controller)`。
- 读取 `controller.uiState`。
- 创建扫描 callback。
- 套用 `KmpMusicTheme`。
- 挂载 `PlatformBackHandler`。
- 调用手机端外层布局。

它不再直接承载页面路由、固定底栏、迷你播放器、底部导航、弹窗或面板细节。

### `feature/app/layout/MobileAppLayout.kt`

承接手机端外层布局：

- 最大宽度约束。
- 全局背景。
- 视觉缩放。
- `LocalMusicScale` 和固定 `fontScale` 的 `LocalDensity`。
- 顶层内容容器。
- 组合内容区、固定底栏、弹窗和面板。

### `feature/app/layout/MobileContentLayout.kt`

承接内容区域：

- 根据固定底栏模式计算页面 bottom padding。
- 持有 `SaveableStateProvider`。
- 调用一级页或二级页路由。

底部避让语义继续来自导航状态派生，不在页面里散写显示或隐藏判断。

### `feature/app/routes/MobileRootScreenRoute.kt`

只处理 `RootTab.Home`、`RootTab.Favorites`、`RootTab.Me` 到手机端一级页的分发。

它可以接收 `MusicAppUiState`、必要 callback 和 `PaddingValues`，但不持有状态真相，不直接访问 repository 或平台能力。

### `feature/app/routes/MobileSecondaryScreenRoute.kt`

只处理 `SecondaryScreen` 到手机端二级页的分发：

- 搜索页。
- 播放详情页。
- 专辑详情页。
- 歌手详情页。
- 设置页。
- 登录页。
- 本地音乐页。
- 缺失资源页。

搜索提交历史、打开专辑/歌手、播放歌曲等 callback 仍通过 `MusicAppController`。

### `feature/app/playerbar/MobileFixedPlayerBar.kt`

承接原 `BottomChrome` 的职责：

- 组合迷你播放器和底部 Tab。
- 保留固定底栏位置动画。
- 保留一级页、二级页、沉浸页三种位置策略。
- 保留无当前播放歌曲时只显示底部 Tab 的策略。

### `feature/app/playerbar/MobileMiniPlayer.kt`

承接迷你播放器：

- 当前歌曲封面和文案。
- 播放/暂停、上一首、队列入口。
- 迷你进度条。
- 封面调色。
- 进度比例计算。

它只消费 `MusicAppUiState` 或展开后的展示参数，不重新判断播放业务语义。是否显示暂停按钮继续使用 `shouldShowPauseControl`。

### `feature/app/playerbar/MobileBottomNavigation.kt`

承接底部 Tab 和 Tab item：

- 首页。
- 收藏。
- 我的。

它只负责展示和 root tab callback，不处理二级页面规则。

### `feature/app/surfaces/AppDialogs.kt`

承接跨端可复用的全局对话框：

- 权限设置确认。
- 清缓存确认。
- 后续同类 AlertDialog。

当前桌面端复用手机端的全局弹窗和底部面板入口，因此弹窗与面板的跨端入口不使用 `Mobile` 前缀。只有确实含手机端布局细节的内部组件，才放到 `feature/app/dialogs` 或 `feature/app/panels` 并使用 `Mobile` 前缀。

### `feature/app/surfaces/AppPanels.kt`

承接跨端可复用的全局面板：

- 队列面板。
- 更多操作面板。
- `BottomSheetAction`。

`AppOverlays(state, controller)` 不作为第三阶段后的生产入口保留。执行时可以在单个迁移任务内临时保留兼容门面，但同一任务结束前必须把手机端和桌面端调用点切到 `AppDialogs` / `AppPanels`，并删除 `AppOverlays` 旧入口。

### 手机端播放详情页

`PlayerScreen.kt` 当前只有约 160 行，职责相对单一。第三阶段不强行拆碎它。

本阶段只把它明确归类为手机端播放详情页，并在必要时移动真正可复用的小函数，例如时间格式化或控制按钮。若没有实际复用收益，保持 `PlayerScreen.kt` 单文件更符合“不制造无意义中转层”的原则。

## 桌面端结构

### `feature/desktop/DesktopMusicApp.kt`

继续作为桌面公开入口，只保留：

- `DesktopMusicApp(controller)`。
- 读取 `controller.uiState`。
- 创建扫描 callback。
- 冷启动本地曲库加载检查。
- 套用 `KmpMusicTheme`。
- 调用桌面外层布局。

### `feature/desktop/layout/DesktopAppLayout.kt`

承接桌面窗口整体布局：

- 标题栏。
- 左侧主导航。
- 首页资料库侧栏。
- 主工作区。
- 桌面底部播放器。
- 弹窗和面板挂载点。
- 播放详情页全屏分支。

播放详情页仍然是沉浸式全屏：显示时不渲染常规桌面布局和底部播放器。

`SecondaryScreen.Player` 只允许在 `DesktopAppLayout` 最高层拦截。`DesktopSecondaryScreenRoute` 不再处理播放详情页，避免桌面播放详情同时存在全屏入口和普通二级路由入口。

### `feature/desktop/layout/DesktopWorkspaceLayout.kt`

承接主工作区容器：

- 工作区背景。
- 响应式横向 padding。
- `SaveableStateProvider` 容器。
- 一级页和二级页路由挂载点。

### `feature/desktop/layout/DesktopTitleBar.kt`

承接桌面标题栏。标题栏搜索入口仍由导航状态推导是否显示，并保持当前 root tab 到 search context 的映射。

### `feature/desktop/navigation/DesktopNavigationRail.kt`

承接左侧主导航：

- `DesktopRailDestination`。
- rail item。
- root tab 和设置入口映射。
- root/settings 选中态。

### `feature/desktop/navigation/DesktopRootScreenRoute.kt`

负责 `RootTab.Home`、`RootTab.Favorites`、`RootTab.Me` 到桌面一级页的分发。

它不写具体页面布局，只负责把 state 和 controller callback 转交给对应页面。

### `feature/desktop/navigation/DesktopSecondaryScreenRoute.kt`

负责桌面二级页分发：

- 搜索。
- 专辑详情。
- 歌手详情。
- 设置。
- 登录。
- 本地音乐。
- 空状态。

### `feature/desktop/screens/DesktopHomeScreen.kt`

承接桌面本地音乐首页：

- 最近专辑。
- 最近播放歌曲。
- 曲库统计。
- 首页播放全部入口。
- 首页专用辅助函数 `buildRecentAlbums` 和 `rootPlayAllLabel`。

### `feature/desktop/screens/DesktopFavoritesScreen.kt`

承接桌面收藏页：

- 收藏歌曲。
- 收藏专辑。
- 收藏歌手。
- 收藏分段切换。

### `feature/desktop/screens/DesktopMeScreen.kt`

承接桌面“我的”页：

- profile panel。
- 收藏、文件夹、设置入口。
- 最近歌手或最近内容。
- 专辑入口。

### `feature/desktop/screens/DesktopSearchScreen.kt`

承接桌面搜索页：

- 搜索输入。
- scope tabs。
- 搜索历史。
- 搜索结果。
- 空结果提示。

搜索提交历史、点击历史、删除历史、清空历史和结果项播放继续由 controller callback 驱动。

### `feature/desktop/screens/DesktopDetailScreens.kt`

承接桌面专辑详情、歌手详情和通用空状态。

详情页仍使用现有本地歌曲、专辑、歌手数据，不引入新的数据源。

### `feature/desktop/screens/DesktopSettingsAndLoginScreens.kt`

承接桌面设置页和登录页。它们都是轻量二级页，放在一个文件比各自独立文件更克制。

### `feature/desktop/screens/DesktopLocalMusicScreen.kt`

承接桌面本地音乐二级页：

- 歌曲分区。
- 专辑分区。
- 歌手分区。
- 来源分区。
- 来源日期格式化。
- 本地音乐分区标题和副标题。

### `feature/desktop/components/DesktopButtons.kt`

承接桌面按钮：

- primary button。
- secondary button。
- more button。
- sort button。
- tiny text button。

### `feature/desktop/components/DesktopForms.kt`

承接桌面表单和选择组件：

- text input。
- segmented control。

### `feature/desktop/components/DesktopTables.kt`

承接桌面表格：

- song table。
- table header。
- table row。
- modified date 格式化。

### `feature/desktop/components/DesktopCards.kt`

承接桌面卡片和行项：

- stat card。
- album card。
- profile panel。
- content row。

### `feature/desktop/components/DesktopSections.kt`

承接桌面内容分区：

- section header。
- album grid。
- artist strip。
- section empty message。

辅助纯函数应随职责迁移，不单独堆到通用 utils 文件里。

组件分类以高内聚优先，不做机械拆分。若一个 grid 和 card 总是一起演进，例如 `DesktopAlbumGrid` 与 `DesktopAlbumCard`，可以放在同一个文件中。`buttons/forms/tables/cards/sections` 是默认归类，而不是强制把强相关组件拆散的规则。

## 播放器 UI 结构

### 手机端播放器

手机端播放器分两块：

- 迷你播放器和固定底栏：从 `MusicApp.kt` 拆到 `feature/app/playerbar`。
- 播放详情页：`PlayerScreen.kt` 保持为手机端播放详情页入口，不强行拆碎。

手机端播放器 UI 不接触 `PlaybackCoordinator`、repository 或 engine；所有播放控制继续通过 `MusicAppController`。

### 桌面端播放器

桌面播放器统一放到 `feature/desktop/player`。

建议拆分：

| 文件 | 职责 |
| --- | --- |
| `DesktopBottomPlayer.kt` | 桌面底部播放器入口，组合歌曲信息、控制区、进度、音量和队列入口。 |
| `DesktopBottomPlayerTrack.kt` | 底部播放器左侧歌曲信息、封面、喜欢按钮和打开详情按钮。 |
| `DesktopBottomPlayerControls.kt` | 底部播放器播放控制、模式切换、进度 seek。 |
| `DesktopPlayerDetailScreen.kt` | 播放详情页公开入口，负责页面级组合和空状态分发。 |
| `DesktopPlayerDetailLayout.kt` | 播放详情页大布局：顶部栏、封面区、信息区、队列预览区、音量区。 |
| `DesktopPlayerDetailControls.kt` | 详情页控制按钮、喜欢按钮和播放模式 icon 映射。 |
| `DesktopPlayerDetailQueue.kt` | 详情页队列预览、队列行和当前歌曲高亮。 |
| `DesktopPlayerProgress.kt` | 可复用进度条和时间格式化。 |
| `DesktopPlayerShared.kt` | 真正跨底部播放器和详情页复用的小类型或函数。 |

`DesktopPlayerShared.kt` 不能变成播放器 utils 垃圾桶。只有同时被底部播放器和详情页使用、且具备明确播放器展示语义的小类型或函数才放进去。

播放器 UI 边界：

- 播放控制 callback 仍全部来自 `MusicAppController`。
- UI 组件不直接接触 `PlaybackCoordinator`、repository 或 engine。
- `shouldShowPauseControl` 继续由 `MusicAppUiState` 提供。
- 当前歌曲红色高亮、喜欢状态、进度 seek、音量变化、播放模式切换行为不变。

## 数据流

手机端：

```text
MusicApp
  -> MobileAppLayout
  -> MobileContentLayout
  -> MobileRootScreenRoute / MobileSecondaryScreenRoute
  -> screen composables

MusicApp
  -> MobileAppLayout
  -> MobileFixedPlayerBar
  -> MobileMiniPlayer / MobileBottomNavigation

MusicApp
  -> MobileAppLayout
  -> AppDialogs / AppPanels
```

桌面端：

```text
DesktopMusicApp
  -> DesktopAppLayout
  -> DesktopTitleBar / DesktopNavigationRail / DesktopWorkspaceLayout / DesktopBottomPlayer
  -> DesktopRootScreenRoute / DesktopSecondaryScreenRoute
  -> desktop screen composables

DesktopMusicApp
  -> DesktopAppLayout
  -> DesktopPlayerDetailScreen

DesktopMusicApp
  -> DesktopAppLayout
  -> AppDialogs / AppPanels
```

所有用户动作继续回到 `MusicAppController`。UI 拆分不新增第二个状态真相。

## 测试与验收

### 自动化验证

每个阶段性拆分后至少运行：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

完成全部第三阶段后运行：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
```

如果执行过程中触碰 `MusicAppController`、导航状态、播放状态、搜索或收藏行为，额外运行对应 focused tests。本阶段原则上不需要修改这些状态协作者。

### 编译级结构防护

必须保持：

- `MusicApp(controller)` 签名不变。
- `DesktopMusicApp(controller)` 签名不变。
- `AppOverlays(state, controller)` 不作为完成态保留；迁移任务结束时调用点应切到 `AppDialogs` / `AppPanels`。
- `DesktopBottomPlayer`、`DesktopPlayerDetailScreen` 等外部调用的公开 composable 名称尽量不改。
- `MusicAppController` public API 不因 UI 拆分而改变。

### 手动或截图核对清单

若环境允许，第三阶段结束后核对：

- 手机端：首页、收藏、我的、搜索、播放页、队列面板、更多面板、权限确认弹窗。
- 桌面端：首页、收藏、我的、搜索页、本地音乐四分区、专辑详情、歌手详情、设置、登录。
- 播放器：桌面底部播放器、桌面播放详情页、手机迷你播放器、手机播放详情页。

重点核对：

- 一级/二级页面底部空间没有被播放器遮挡。
- 手机一级页迷你播放器与底部 Tab 不留缝。
- 手机二级页隐藏底部 Tab，迷你播放器贴底。
- 手机沉浸播放页和全屏设置页隐藏固定底栏。
- 桌面播放详情页全屏时不显示常规桌面布局和底部播放器。
- 当前播放歌曲红色高亮不丢。
- 搜索、更多、收藏、播放全部、打开详情、返回都还走原 controller callback。

## 风险与处理

| 风险 | 处理 |
| --- | --- |
| 大量移动文件导致 import 和可见性错误 | 分批迁移，每批运行 Android 编译。 |
| 固定底栏规则回归 | 先重命名并保留原测试语义，再迁移 UI 组件。 |
| 桌面播放详情页不再沉浸 | 把详情页全屏分支留在 `DesktopAppLayout` 的最高层。 |
| 桌面组件目录变成新杂物间 | 按 buttons/forms/tables/cards/sections 默认分类，强相关组件允许同文件保持高内聚。 |
| 命名治理扩大范围 | 只改 UI 结构相关命名，不回改历史文档，不改业务状态语义。 |
| 视觉回归难以自动测试 | 自动编译加手动/截图清单，最终说明剩余视觉风险。 |

## 验收标准

第三阶段完成时应满足：

- `MusicApp.kt` 只保留公开入口、主题、系统返回、扫描 callback 和顶层布局调用；不再定义页面级、固定底栏、弹窗或面板 Composable。
- `DesktopMusicApp.kt` 只保留公开入口、主题、扫描 callback、冷启动加载副作用和顶层布局调用；不再定义工作区、路由或播放器 Composable。
- `DesktopMusicScreens.kt` 的页面内容按一级页、二级页和本地音乐分区拆清。
- `DesktopMusicComponents.kt` 的组件按按钮、表单、表格、卡片和分区拆清。
- `DesktopMusicPlayer.kt` 和 `DesktopPlayerDetailScreen.kt` 的底部播放器、详情页布局、控制区、队列区职责清晰。
- 生产代码新增命名不再使用 `shell`、`chrome`、`overlays`。
- 现有 `BottomChrome`、`AppChromeMode`、`BottomChromePlacement` 等 UI 结构相关歧义命名完成替换；仅允许单个迁移任务内部短期兼容，不作为阶段完成态保留。
- 手机端和桌面端视觉与交互保持现状。
- `:composeApp:compileDebugKotlinAndroid` 通过。
- `:composeApp:desktopTest` 通过。

## 最终判断

第三阶段应该完整覆盖 UI 大文件，但按职责边界推进，而不是按行数机械拆分。推荐先处理手机端固定底栏命名和 `MusicApp.kt` 结构，再处理桌面布局和路由，随后拆桌面页面与通用组件，最后处理桌面播放器文件。这样可以根治 UI 文件职责堆积，同时把视觉和交互回归风险控制在可编译、可分批验证、可人工核对的范围内。
