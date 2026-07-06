# KMP Music OpenWiki 快速开始

KMP Music 是一个本地优先的跨平台音乐播放器，使用 Kotlin Multiplatform 和 Compose Multiplatform 构建。它面向 Android、iOS 和 Desktop，共享 UI、状态和领域代码，并通过平台适配层处理本地音频扫描、播放引擎、权限、存储和打包。

产品方向刻意保持轻量：扫描或导入本地音乐、浏览歌曲/专辑/歌手、播放音乐、管理队列、收藏内容、搜索、调整设置，并把登录和云同步作为未来能力。当前源码不只是 mock UI：已经包含 Room 持久化、Android Media3 播放、Desktop vlcj/LibVLC 播放、Android/Desktop 持久化 controller graph，以及共享的播放、曲库、搜索和收藏测试。iOS 目前有 Compose 入口和文件夹扫描器，但在已检查源码中没有看到真实 iOS 播放引擎或持久化 DB 接线。

## 从这里开始

把本页当作地图，然后阅读与你任务匹配的章节：

- [架构：App shell、controller、导航和 UI 表面](architecture/app-architecture.md)
- [领域：音乐曲库、扫描、播放、收藏和搜索](domain/music-library-and-playback.md)
- [数据、持久化和平台集成](data-persistence/data-and-platform-integrations.md)
- [产品工作流和页面地图](workflows/product-workflows.md)
- [测试与验证指南](testing/verification-guide.md)

## 事实来源说明

仓库中的主要文档仍然有用，尤其是 `README.md`、`CONTEXT.md`、`docs/PRD.md`、`docs/LOCAL_AUDIO_DISCOVERY_PRD.md` 和 `docs/PLAYBACK_PRD.md`。把 PRD 视为产品意图；在宣称某项能力已经完成前，先用当前源码验证。

重要版本注意事项：`README.md` 和 `AGENTS.md` 提到较旧的 Kotlin/Compose 版本，而当前版本目录是 `gradle/libs.versions.toml`，其中 Kotlin 为 `2.4.0`、Compose Multiplatform 为 `1.11.1`。构建事实优先参考 Gradle 文件和版本目录。

初始化运行时提供的近期 git 历史显示，活跃工作集中在移动端专辑/歌手详情页、移除详情页 demo 歌曲数据、收藏页 Figma 重设计、本地曲库分组规则、播放器 chrome/手势修复、Android 导航栏/播放器颜色行为、持久化播放/搜索/历史、Desktop 播放重构，以及 Android 性能 harness。这些主题与当前源码热点相符：`feature/screen`、`feature/app`、`domain/playback`、`domain/persistence`、Android 播放和 Desktop 播放。

## 仓库一览

```text
.
├── composeApp/                    # 主 Kotlin Multiplatform 模块
│   ├── build.gradle.kts            # Target、依赖、Room/KSP、Desktop 打包任务
│   ├── schemas/                    # 导出的 Room schema JSON
│   └── src/
│       ├── commonMain/             # 共享 UI、App controller、domain、data repo、Desktop 共享 UI
│       ├── commonTest/             # 共享 domain/data/controller/screen 测试
│       ├── androidMain/            # Activity、MediaStore 扫描、Media3 service/session/playback
│       ├── androidUnitTest/         # Android 播放命令测试
│       ├── desktopMain/            # Desktop 入口、扫描器、vlcj/LibVLC 播放、打包辅助
│       ├── desktopTest/             # Desktop DB/session/metadata/playback 测试
│       └── iosMain/                 # iOS Compose 入口和文件夹扫描器
├── docs/                           # 产品需求、播放/本地音频文档、agent 文档/规格
├── .agents/skills/                 # 本地 agent skills 和协作工作流
├── prototypes/                     # 高保真视觉参考，不是生产 App 代码
├── CONTEXT.md                      # 规范产品/领域词汇
├── README.md                       # 面向人的概览和命令
└── AGENTS.md                       # Agent 指令和项目规则
```

## 构建和运行基础

仓库当前只有一个 Gradle 模块：`settings.gradle.kts` 中声明的 `:composeApp`。根构建文件只应用共享插件；`composeApp/build.gradle.kts` 配置 Android、iOS arm64/simulator arm64 静态 framework target，以及 Desktop JVM。

| 范围 | 源码支持的事实 |
| --- | --- |
| 主模块 | `settings.gradle.kts` 中的 `:composeApp` |
| 包名/application ID | `composeApp/build.gradle.kts` 中的 `com.yanhao.kmpmusic` |
| Android SDK | compileSdk 36、minSdk 24、targetSdk 36 |
| JVM target | Android compile options 和 Kotlin Android target 均为 17 |
| KMP targets | Android、iOS arm64/simulator arm64 static framework、Desktop JVM |
| 依赖版本 | `gradle/libs.versions.toml` |
| Room schema 导出 | 通过 `room3 { schemaDirectory(...) }` 导出到 `composeApp/schemas` |

来自 `README.md` 和 `AGENTS.md` 的常用命令：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug
./gradlew :composeApp:desktopTest
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
./gradlew :composeApp:run
./gradlew :composeApp:tasks
```

在 Apple Silicon macOS 上做 Desktop 播放前，先准备打包的 LibVLC runtime：

```bash
./gradlew :composeApp:prepareMacosArm64LibVlc
```

详情见 `composeApp/src/desktopMain/packaging/macos-libvlc/README.md` 和 [数据、持久化和平台集成](data-persistence/data-and-platform-integrations.md)。

## 产品形态

规范词汇在 `CONTEXT.md`；代码评审和后续文档应使用这些术语。关键规则：

- 移动端根 Tab 只有 `首页 / 收藏 / 我的`。
- 搜索、播放器、专辑详情、歌手详情、设置、关于、登录和本地音乐都是二级页面。
- 迷你播放器是由 App layout/playerbar 代码拥有的全局 chrome，不是页面内容。
- 普通移动端二级页面隐藏底部 Tab 但保留迷你播放器；`About` 同时隐藏底部 Tab 和迷你播放器；`Player` 是带独立控制的全屏 overlay。
- 专辑和歌手详情列表来自完整曲库，不是短 demo/推荐子集。
- 访客模式应保持可用；登录/云同步仍是未来能力。

## 当前能力快照

已在检查过的源码中实现或体现的能力：

- 共享 Compose UI 入口和 App 级 `MusicAppController` 位于 `commonMain`。
- 移动端首页、收藏、我的、搜索、播放器、本地音乐、专辑详情、歌手详情、设置、关于和登录占位页面。
- Desktop shell，包含 rail/sidebar/workspace、底部播放器和全屏播放器详情。
- 本地扫描模型和扫描器：Android MediaStore、Desktop 文件夹扫描、iOS 文件夹扫描，以及 fake scanner/demo catalog。
- Room 持久化本地歌曲、收藏歌曲 ID、搜索历史、播放快照、队列和播放历史。
- Android Media3 service/session/notification 播放集成，以及 Desktop vlcj/LibVLC 播放集成。
- 共享 controller 逻辑覆盖导航、根/二级 chrome 策略、播放/队列、搜索、收藏、本地曲库加载/扫描、主题、对话框和面板。
- 覆盖 controller/navigation、播放协调、持久化、扫描合并、本地文件规则、调色板/主题、页面行为、Android media commands、Desktop 数据库/session/playback 支撑的测试。

已知未完成或面向未来的区域：

- 在已检查源码中未看到 iOS 真实播放引擎与 Android/Desktop 对齐；`IosEntry.kt` 将 `IosFolderMusicScanner` 接入默认共享 controller。
- 在已检查入口源码中未看到 iOS 持久化接线。
- 登录/magic-link UI 是占位；未发现真实 auth/cloud-sync 后端集成。
- 部分设置项和播放/设备入口是未来能力占位。
- `PlaybackDatabase` 的职责比名称更广：它存储的不只是播放。

## 修改代码时从哪里开始

| 修改范围 | 先看 | 再看 |
| --- | --- | --- |
| App 状态、导航、播放/收藏/搜索交互 | `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt` | `MusicAppModels.kt`、`feature/app/*` 下的子 controller、`MusicAppControllerTest.kt` |
| 移动端 layout/chrome/手势行为 | `feature/app/layout/MobileAppLayout.kt`、`MobileContentLayout.kt` | `MusicAppModels.kt`、`MobilePlayerOverlayGesture.kt`、playerbar 组件 |
| 根页面和二级移动端页面 | `feature/screen/` | `feature/app/routes/`、共享组件、附近的 `*FigmaTokens.kt` 文件 |
| Desktop UI | `feature/desktop/DesktopMusicApp.kt` | `feature/desktop/layout/DesktopAppLayout.kt`、desktop screens/player 组件 |
| 本地音乐扫描和曲库合并 | `domain/repository/LocalMusicScanner.kt`、`domain/usecase/ScanLocalMusicUseCase.kt` | 平台扫描器、`PersistentMusicLibraryRepository.kt`、本地扫描测试 |
| 播放和队列行为 | `domain/playback/PlaybackCoordinator.kt`、`AudioPlayerEngine.kt` | `PlaybackRepository.kt`、`PlaybackSnapshotStore.kt`、Android/Desktop 播放适配器、播放测试 |
| 持久化 schema | `domain/persistence/PlaybackDatabase.kt` | `PlaybackDatabaseMigrations.kt`、持久化 repository、数据库 factory、schemas |
| 产品规则和词汇 | `CONTEXT.md`、`docs/PRD.md` | 相关 OpenWiki 领域/工作流页面 |

## 给未来 agent 的改动建议

1. 发明路由标签、页面名或工作流词汇前，先读 `CONTEXT.md`。
2. 不要把平台 API 放进 `commonMain` 的 UI/domain。先新增或调整领域边界，再在平台 source set 中实现。
3. 优先通过共享 controller、state、theme、全局 chrome 修复问题，而不是在单个页面打补丁。
4. 不要把 `prototypes/kmp-music-hi-fi` 当作生产代码；除非用户明确要求原型工作，它只是视觉参考。
5. 修改 controller、导航、播放、队列、收藏或搜索时，更新或新增共享测试。
6. 修改 UI layout 或平台 source set 时，尽量运行匹配的编译/测试命令，并说明任何未验证的平台行为。
