# KMP Music

KMP Music 是一个本地音乐优先的 Kotlin Multiplatform 音乐播放器。项目目标是先把本地音频发现、曲库浏览、播放、队列、收藏、最近播放、本地自建歌单和设置做成跨端可复用闭环，再逐步补齐平台系统播放体验、账号登录和云同步。

当前仓库已经从原型阶段进入真实 KMP App 迭代：共享领域模型、播放协调、数据仓库、移动端 UI 和桌面端 UI 位于 `commonMain`；Android、iOS、Desktop/macOS 通过各自 source set 接入扫描、播放和平台能力，Android/Desktop 额外接入数据库构造。

## 当前代码状态

| 范围 | 当前已落地 | 明确边界 |
| --- | --- | --- |
| `commonMain` | 领域模型、Repository 接口、UseCase、播放协调器、队列/随机/失败策略、播放快照、搜索历史、用户偏好、本地歌单、移动端页面、桌面端页面和共享设计令牌。 | 不直接引入 Android、iOS、Desktop 专属 API。 |
| Android | `MainActivity`、进程级 `AndroidPlaybackSession`、MediaStore 扫描、音频/通知权限入口、Media3 播放服务、MediaSession、通知与媒体按钮、Room3 持久化。 | Android 本地音乐来源语义是系统媒体库，不引入文件夹来源。 |
| iOS | `MainViewController()`、进程级 `IosPlaybackSession`、文件夹选择与沙盒导入扫描、AVFoundation App 内播放、宿主后台音频配置证据。 | 当前 iOS 入口未接入持久化依赖图；Now Playing、远程命令、控制中心/耳机按钮和冷启动恢复不宣称已完成。 |
| Desktop/macOS | `DesktopMusicApp`、左侧导航、桌面播放器、文件夹扫描、音频元数据/封面读取、Room3 持久化、macOS AVFoundation JNI bridge、DMG 打包时 bridge staging。 | 真实 Desktop 播放只面向 macOS；不宣称 Windows/Linux 已支持真实播放。 |

## 产品范围

MVP 聚焦本地音乐闭环：

- 本地曲库扫描、刷新、来源摘要和问题摘要。
- 首页、收藏、我的、搜索、播放页、专辑详情、歌手详情、最近播放、本地音乐、本地自建歌单、设置和登录占位。
- 播放/暂停、上一首/下一首、seek、播放模式、队列、播放失败提示、播放快照恢复。
- 收藏歌曲、最近播放、搜索历史、主题模式、本地发现偏好和本地歌单。
- 游客模式完整可用；登录入口仅为后续云同步预留。

暂不做复杂社区、直播、会员支付、广告、在线歌曲下载和发布级云同步。

## 信息架构

移动端一级导航固定为三项：

```text
首页 / 收藏 / 我的
```

二级页面包括搜索、播放、专辑、歌手、设置、关于、登录、本地扫描、最近播放、本地音乐、本地歌单、本地歌单管理和歌单详情。移动端一级页面显示底部 Tab 与全局迷你播放器，二级页面隐藏底部 Tab。Desktop 使用左侧导航和底部播放器，但沿用同一套核心状态与内容层级。

## 技术栈

版本事实以 `gradle/libs.versions.toml` 和 `composeApp/build.gradle.kts` 为准。

| 类别 | 版本 / 说明 |
| --- | --- |
| Kotlin Multiplatform | 2.4.0 |
| Compose Multiplatform | 1.11.1 |
| Android Gradle Plugin | 8.13.2 |
| Coroutines | 1.11.0 |
| Ktor | 3.5.1 |
| Coil | 3.5.0 |
| AndroidX Media3 | 1.10.1 |
| Room3 | 3.0.0-rc01 |
| SQLite | 2.6.2 |
| jaudiotagger | 2.0.3 |
| Android SDK | minSdk 24, targetSdk 36, compileSdk 36 |
| JVM target | 17 |
| 主模块 | `:composeApp` |
| 包名 / applicationId | `com.yanhao.kmpmusic` |

## 快速开始

### 环境要求

- JDK 17。
- Android Studio 或可用 Android SDK。
- iOS framework 构建需要 Xcode/Kotlin Native 环境。
- macOS Desktop 真实播放和 smoke 需要 macOS 主机、`clang++`、AVFoundation framework。

### 常用命令

```bash
# 查看 composeApp 可用任务
./gradlew :composeApp:tasks

# Android Kotlin 编译
./gradlew :composeApp:compileDebugKotlinAndroid

# 生成 Android debug APK
./gradlew :composeApp:assembleDebug

# 安装到已连接 Android 设备
./gradlew :composeApp:installDebug

# 共享逻辑与 Desktop 测试
./gradlew :composeApp:desktopTest

# 快速验证共享逻辑和 Android 编译
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest

# Android JVM 单测
./gradlew :composeApp:testDebugUnitTest

# 运行 Desktop App，当前 macOS bridge 属性在该任务上接入
./gradlew :composeApp:desktopRun

# 生成 iOS simulator debug framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

macOS AVFoundation 相关验证：

```bash
./gradlew :composeApp:macosAvFoundationBridgeSmoke
./gradlew :composeApp:macosAvFoundationDefaultRuntimeSmoke
./gradlew :composeApp:macosAvFoundationRestartResumeSmoke
./gradlew :composeApp:macosAvFoundationPackagedBridgeSmoke
./gradlew :composeApp:macosAvFoundationReleasePackagedBridgeSmoke
```

## 项目结构

```text
.
├── composeApp
│   ├── build.gradle.kts
│   ├── schemas
│   └── src
│       ├── commonMain
│       ├── commonTest
│       ├── androidMain
│       ├── androidUnitTest
│       ├── iosMain
│       ├── iosTest
│       ├── desktopMain
│       └── desktopTest
├── docs
│   ├── agents
│   ├── adr
│   └── PRD.md
├── gradle
│   └── libs.versions.toml
├── prototypes
│   └── kmp-music-hi-fi
└── AGENTS.md
```

重点入口：

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain`：领域模型、Repository 接口、UseCase、播放协调和持久化契约。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data`：共享数据实现、持久化 Repository、数据库工厂、本地扫描合并和演示数据。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app`：全局状态、导航、播放、收藏、搜索、扫描、偏好和会话控制器。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen`：移动端页面级 Composable 与显示模型。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop`：桌面端布局、导航、页面、表格和播放器。
- `composeApp/src/androidMain`：Android 入口、权限、MediaStore、Media3、通知、数据库和平台适配。
- `composeApp/src/iosMain`：iOS 入口、沙盒导入扫描、AVFoundation 和平台适配。
- `composeApp/src/desktopMain`：Desktop/macOS 入口、文件夹扫描、数据库、AVFoundation native bridge 和打包适配。
- `composeApp/src/*Test`：共享、Android、iOS、Desktop 测试与 macOS 播放门禁。

## 架构边界

项目保持 `core / domain / data / feature / platform source sets` 分层：

```text
feature -> domain <- data
core    -> theme and UI foundation
platform source sets -> entry, scanner, playback, database, system adapters
```

- `MusicAppController` 是共享 UI 状态门面；扫描、播放、收藏、搜索、偏好和导航工作流已拆到子控制器或同步器。
- 播放主链路是 `MusicAppController -> PlaybackCoordinator -> AudioPlayerEngine -> 平台实现`。
- Android 和 Desktop 通过 `createPersistentMusicAppController` 接入 Room3；默认 `MusicAppController` 仍保留内存仓库，供预览、测试和未接持久化的平台入口使用。
- 平台扫描只回答“发现了哪些可播放本地歌曲”；播放、队列推进和快照语义留在 common 层。
- `prototypes/kmp-music-hi-fi` 只作为视觉参考，不是生产入口。

## 测试策略

- Markdown 纯文档改动通常不需要跑 Gradle，但需要检查 `git diff`、链接路径和源码事实。
- 改共享状态、导航、播放、队列、收藏、搜索、扫描或偏好时，至少运行：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
```

- 改 Android MediaStore、Media3 service、通知按钮或权限时，至少运行 Android 编译，并补对应 JVM 或共享测试。
- 改 Apple/macOS AVFoundation 时，至少运行 `desktopTest`，按影响范围补 smoke。
- 不确定任务名时先跑 `./gradlew :composeApp:tasks`。

## 相关文档

- 产品需求：[docs/PRD.md](docs/PRD.md)
- 本地音频发现 PRD：[docs/LOCAL_AUDIO_DISCOVERY_PRD.md](docs/LOCAL_AUDIO_DISCOVERY_PRD.md)
- 播放 PRD：[docs/PLAYBACK_PRD.md](docs/PLAYBACK_PRD.md)
- 项目地图：[docs/agents/project-map.md](docs/agents/project-map.md)
- 架构边界：[docs/agents/kmp-architecture.md](docs/agents/kmp-architecture.md)
- UI 与状态规则：[docs/agents/ui-state.md](docs/agents/ui-state.md)
- 验证策略：[docs/agents/testing.md](docs/agents/testing.md)
- ADR：[docs/adr](docs/adr)
- Agent 项目指南：[AGENTS.md](AGENTS.md)
