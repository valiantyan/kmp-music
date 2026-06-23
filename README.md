# KMP Music

KMP Music 是一个基于 Kotlin Multiplatform 和 Compose Multiplatform 的跨平台音乐 App。项目目标是先打通本地音乐优先的播放闭环，再逐步完善平台播放能力、账号登录与云同步。

当前仓库处于 MVP 构建阶段：共享 UI、导航状态、播放队列、收藏、搜索、主题、本地音乐扫描领域模型和曲库快照合并已经放在 `commonMain` 中复用；Android MediaStore 扫描、Desktop 文件夹扫描和 iOS 导入文件扫描由各平台 source set 注入。真实音频播放、持久化和云同步仍是后续阶段的能力。

## 产品定位

- 本地优先：首版围绕本地曲库、离线播放、收藏和最近播放展开。
- 轻量清晰：不做社区、直播、会员、电台、短视频等大型音乐平台能力。
- 跨端复用：Android、iOS 和 Desktop 保持一致的信息架构与核心状态。
- 游客可用：MVP 默认游客模式完整可用，登录入口为后续云同步预留。

主导航保持三项：

```text
首页 / 收藏 / 我的
```

搜索、播放页、专辑页、歌手页、设置、登录、本地音乐和本地来源等属于二级页面。移动端一级页面显示底部 Tab 和全局迷你播放器，二级页面隐藏底部 Tab。

## 技术栈

| 类别 | 版本 / 说明 |
| --- | --- |
| Kotlin Multiplatform | 2.0.21 |
| Compose Multiplatform | 1.7.3 |
| Android Gradle Plugin | 8.13.2 |
| Coroutines | 1.9.0 |
| Android SDK | minSdk 24, targetSdk 36, compileSdk 36 |
| JVM target | 17 |
| 主模块 | `:composeApp` |
| 包名 / applicationId | `com.yanhao.kmpmusic` |

## 快速开始

### 环境要求

- JDK 17
- Android Studio 或可用的 Android SDK
- Gradle Wrapper 使用仓库内的 `./gradlew`

### 常用命令

```bash
# Android Kotlin 编译
./gradlew :composeApp:compileDebugKotlinAndroid

# 生成 Android debug APK
./gradlew :composeApp:assembleDebug

# 安装到已连接 Android 设备
./gradlew :composeApp:installDebug

# 运行共享逻辑测试
./gradlew :composeApp:desktopTest

# 快速验证共享逻辑和 Android 编译
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest

# 运行 Desktop App
./gradlew :composeApp:desktopRun

# 查看 composeApp 可用任务
./gradlew :composeApp:tasks
```

Desktop 入口在 `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopMain.kt`。如果任务名不确定，先执行 `./gradlew :composeApp:tasks` 查看。

iOS 侧当前提供 `MainViewController()` 供 SwiftUI/UIKit 宿主调用，入口位于 `composeApp/src/iosMain/kotlin/com/yanhao/kmpmusic/IosEntry.kt`。

## 项目结构

```text
.
├── composeApp
│   ├── build.gradle.kts
│   └── src
│       ├── commonMain
│       │   ├── composeResources/drawable
│       │   └── kotlin/com/yanhao/kmpmusic
│       │       ├── core/theme
│       │       ├── data
│       │       ├── domain
│       │       └── feature
│       ├── commonTest
│       ├── androidMain
│       ├── iosMain
│       └── desktopMain
├── docs
│   └── PRD.md
├── .agents
│   └── skills
├── gradle
│   └── libs.versions.toml
├── prototypes
│   └── kmp-music-hi-fi
└── AGENTS.md
```

重点目录：

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain`：领域模型、Repository 接口和 UseCase。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data`：内存曲库实现、演示 scanner 和本地音频文件规则。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/core/theme`：主题、颜色、尺寸和封面调色逻辑。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app`：全局 App 状态、导航、chrome 和控制器。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components`：复用 UI 组件。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen`：首页、收藏、我的、搜索、播放、详情、设置、本地音乐等页面。
- `composeApp/src/androidMain`、`iosMain`、`desktopMain`：平台入口、权限入口和本地音乐扫描适配。
- `composeApp/src/commonTest`：共享逻辑测试，主要覆盖控制器、导航、播放状态和主题算法。
- `.agents/skills`：项目内手动技能和协作工作流；列表性能诊断技能需要显式调用。
- `prototypes/kmp-music-hi-fi`：高保真视觉参考，不是生产入口。

## 文档查找速查

遇到问题时，优先先看已有文档和源码边界，再决定是否改代码：

| 问题 | 先看哪里 | 判断标准 |
| --- | --- | --- |
| 这个功能是否属于 MVP？ | `docs/PRD.md` 的产品范围、非目标范围和里程碑 | PRD 已排除的能力不要在当前任务中顺手实现 |
| 某个页面应该是一层还是二层？ | `docs/PRD.md` 信息架构、`AGENTS.md` UI 与状态规则 | 一级页面只保留首页、收藏、我的 |
| 播放、队列、收藏或搜索状态不一致怎么办？ | `feature/app/MusicAppController.kt` 和 `MusicAppControllerTest.kt` | 先修共享控制器和测试，再看页面表现 |
| 迷你播放器、底部栏或内容遮挡问题从哪里改？ | `feature/app/MusicApp.kt`、`core/theme/MusicTheme.kt` | 优先修全局 chrome、inset 和 token，不在单页复制补丁 |
| 要改本地音乐扫描、来源或元数据合并怎么办？ | `domain/repository/LocalMusicScanner.kt`、`domain/usecase/ScanLocalMusicUseCase.kt`、`MergeLocalMusicScanResultUseCase.kt`、平台 scanner | 先守住 scanner/use case/repository 边界，再改平台实现 |
| 要接入真实播放怎么办？ | `domain/repository/PlaybackRepository.kt`、`domain/usecase/PlaybackUseCases.kt`、平台 source set | 先定义播放边界，再接平台实现，不把平台 API 塞进 `commonMain` UI |
| 视觉应该参考哪里？ | `prototypes/kmp-music-hi-fi` 和它自己的 `AGENTS.md` | 原型只作视觉参考，不作为生产入口 |
| 列表卡顿、掉帧或重组需要复盘怎么办？ | `$compose-scroll-performance-diagnostics` | 这是手动技能，只有显式调用时使用；先测量分类，再决定优化点 |
| 改动后该跑什么验证？ | README 的常用命令、`AGENTS.md` 测试与提交 | 触及共享状态时至少跑 `desktopTest`，UI 大改至少跑 Android 编译 |

## 架构约定

项目采用 `core / domain / data / feature` 分层：

```text
feature -> domain <- data
core   -> shared theme and UI foundation
platform source sets -> platform entry and adapters
```

- UI 层不直接依赖平台专属实现。
- 新增数据能力时，先在 `domain/repository` 或 `domain/usecase` 定义边界，再在 `data` 或平台目录提供实现。
- 平台媒体扫描、真实播放、通知、权限等能力通过接口、`expect/actual` 或平台 data source 接入。
- 全局播放、队列、收藏、搜索和导航状态集中由 `MusicAppController` 管理，页面只负责布局和事件转发。
- 设计 token 优先沉淀在 `MusicTheme.kt`，页面和组件复用共享尺寸、颜色和样式。

## 当前能力

已实现或已有骨架：

- 首页、收藏、我的、搜索、播放、专辑详情、歌手详情、设置、登录占位、本地音乐二级页。
- 本地音乐页按歌曲、专辑、歌手、来源分段展示，全量歌曲列表使用懒加载结构承载。
- 本地音乐扫描领域模型、扫描状态、来源摘要、问题摘要和曲库快照合并。
- Android MediaStore 扫描和音频权限引导；Desktop 文件夹扫描；iOS 导入文件扫描适配。
- 全局迷你播放器、底部 Tab、二级页面导航、系统返回处理。
- 播放/暂停、上一首/下一首、播放队列、队列弹层、更多操作弹层。
- 歌曲收藏状态同步、收藏页分区、搜索范围切换，并统一消费当前曲库快照。
- 主题模式切换和封面色彩提取相关逻辑。
- 演示 scanner 与内存 Repository，用于非平台环境和共享状态验证。

仍在后续阶段：

- 真实音频播放、后台播放、通知和锁屏控制。
- 本地扫描结果、收藏、播放历史、设置等持久化。
- 真实封面解析、更多平台媒体库能力和更完整的权限/导入体验。
- 账号登录、云同步和冲突合并。
- iOS/Android/Desktop 各平台的完整播放适配。

## 测试

常用验证：

```bash
./gradlew :composeApp:desktopTest
./gradlew :composeApp:compileDebugKotlinAndroid
```

涉及共享状态、导航、播放、队列、收藏或搜索时，请同步更新 `MusicAppControllerTest` 或新增 `commonTest` 测试。UI 视觉大改后至少运行 Android 编译；条件允许时用真机、模拟器或 Desktop 截图核对关键页面。

## 开发注意事项

- 修改生产 App 时优先改 `composeApp`，不要用 `prototypes/kmp-music-hi-fi` 代替真实实现。
- 修复问题要追求根因；如果根因修复会明显扩大范围，应先说明取舍再推进。
- 改动前先查 README、`AGENTS.md`、`docs/PRD.md` 和相关源码，确认已有边界是否能回答当前问题。
- 一级页面只有首页、收藏、我的；搜索、播放、专辑、歌手、设置等保持为二级页面。
- 迷你播放器是全局 chrome，不要在单个页面重复实现。
- 当前播放歌曲需要在所有列表中保持一致高亮和播放中标识。
- Compose 列表性能问题需要沉淀经验时，显式调用 `$compose-scroll-performance-diagnostics`；不要让它自动替代普通调试流程。
- 不要在 `commonMain` 引入 Android、iOS 或 Desktop 专属 API。
- 不要删除失败测试来让构建通过。
- 提交前查看 `git status --short --branch`，避免提交 `.scratch/`、构建产物、IDE 状态、日志、原型 dist、APK/DMG 或本地缓存。

## 相关文档

- 产品需求：[docs/PRD.md](docs/PRD.md)
- Agent 项目指南：[AGENTS.md](AGENTS.md)
- 高保真原型参考：`prototypes/kmp-music-hi-fi`
