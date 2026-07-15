# macOS 接入 AVFoundation 的运行时路线调研

调研时间：2026-07-14。

## 结论摘要

当前 macOS 版是 Compose Desktop JVM 应用，真实播放由 `DesktopVlcjAudioPlayerEngine` 承接，Gradle 仍声明 `vlcj` 依赖并在 macOS 打包链路下载、提取、内置和验收 LibVLC。若目标是“macOS 和 iOS 统一使用 Apple 原生播放方案”，最小迁移路线不是立刻重写桌面 App 形态，而是在保留 Compose Desktop JVM 壳的前提下新增一个 AVFoundation bridge，让新的 `DesktopAvFoundationAudioPlayerEngine` 继续实现现有 `AudioPlayerEngine`。

可行路线分两大类：

- 保留 JVM 桌面壳：通过 JNI / Objective-C 或 Swift wrapper、JNA 直连 Objective-C runtime，或进程外 helper / XPC 调用 AVFoundation。优点是最大限度复用当前桌面 UI、Room、扫描器、控制器和打包入口；缺点是 macOS 与 iOS 很难共享同一份 Kotlin 播放实现，只能共享 Apple 底层框架、`AudioPlayerEngine` 契约、队列语义和事件映射。
- 新增或切换 Kotlin/Native macOS target：可以像 iOS 一样从 Kotlin/Native 直接调用 AVFoundation，更接近“同一份 Apple Kotlin adapter”。但当前官方 Compose Desktop 打包链路和项目代码都围绕 JVM desktop；切换到 Kotlin/Native macOS 会牵动 UI 宿主、Room / KSP、文件扫描、桌面分发和签名公证，接近重写桌面运行时形态。

建议当前决策选择“保留 Compose Desktop JVM 壳 + 进程内 Objective-C / Swift native library bridge + 新 `AudioPlayerEngine` 实现”作为第一迁移路线。它能先移除 vlcj / LibVLC 和第三方运行时打包风险，同时保留现有桌面产品形态。只有当后续明确要让 macOS 也迁移为 Kotlin/Native App 或 Swift/AppKit 宿主时，再把 Kotlin/Native macOS target 作为二阶段路线评估。

## 可行路线对比

| 路线 | 运行时边界 | 当前桌面复用 | 与 iOS 统一程度 | 主要代价 | 建议 |
| --- | --- | --- | --- | --- | --- |
| JVM + JNI + Objective-C / Swift 动态库 | JVM 调用项目自建 `.dylib` / `.framework`，native 侧持有 `AVPlayer` | 高 | 中：共享 AVFoundation、契约和行为，难共享 Kotlin 实现 | 需要 Xcode / Clang 构建、JNI 回调、主线程调度、签名嵌套代码 | 推荐首选 |
| JVM + JNA 直连 Objective-C runtime | JVM 通过 JNA 调 `libobjc`、`objc_msgSend` 和系统 framework | 高 | 中低 | `CMTime`、block、KVO、ARC、消息签名和线程要求复杂，出错面大 | 只建议做 spike，不建议生产首版 |
| JVM + 进程外 Swift / XPC helper | JVM 主进程通过 IPC 调 native helper，helper 持有 `AVPlayer` | 高 | 中 | IPC 协议、生命周期、沙盒 entitlements、helper 打包和签名复杂 | 适合隔离 native 崩溃或沙盒权限，不适合作为最小首版 |
| Kotlin/Native macOS target | `macosArm64` / `macosX64` 直接调用 AVFoundation | 低到中 | 高：可能共享 Apple Kotlin adapter | 需要确认 macOS UI 宿主、Compose 能力、Room / KSP、打包和持久化链路 | 二阶段评估 |
| Kotlin/Native 播放动态库 + JVM UI | Kotlin/Native 编出 native 库，JVM 通过 C ABI / JNI 调用 | 中 | 中高 | 双 Kotlin runtime、C ABI、回调和内存边界复杂 | 暂不推荐首版 |

### 路线一：保留 Compose Desktop JVM，通过 JNI 调 Objective-C / Swift wrapper

这条路线保留当前 `jvm("desktop")`、`DesktopMainKt`、`DesktopPlaybackSession`、桌面 UI 和 Room 链路。新增的 native wrapper 用 Objective-C 或 Swift 实现 `AVPlayer`、`AVPlayerItem`、KVO / Notification、进度 observer 和 security-scoped resource 生命周期，对 JVM 暴露一组稳定 C / JNI 函数。Kotlin 侧新增 `DesktopAvFoundationAudioPlayerEngine : AudioPlayerEngine`，复用当前 `DesktopVlcjAudioPlayerEngine` 已经验证过的串行命令、generation token、过期事件丢弃、进度轮询和释放时序思想。

关键边界：

- JNI 边界只传递平台无关数据：`songId`、`fileUrl`、`startPositionMs`、`volume`、命令和事件；不要把 Objective-C 对象泄漏到 Kotlin。
- native 侧统一持有 `AVPlayer` 和 observer token；切歌、stop、release 必须成对移除 KVO / Notification / time observer。
- AVPlayer 的 `play` / `pause` / `rate` 在旧系统上有主线程要求；bridge 需要把命令调度到合适的 Apple 队列。
- JVM 侧仍以 `AudioPlayerEngine.events` 作为唯一事实回流入口，避免 UI 或 controller 直接理解 AVFoundation。

优点：

- 当前桌面 App 形态改动最小。
- 能删除 vlcj 依赖、LibVLC 下载提取任务、LibVLC source record 和 LibVLC 缺失 fallback。
- 自建 native 库比内置完整 LibVLC 小得多，签名和公证面更小。

缺点：

- iOS `iosMain` 不能直接复用 JVM bridge 实现；只能共享接口、测试契约和部分设计。
- 需要新增 native 构建与打包门禁。
- JNI callback 和 native lifecycle 会引入一层调试成本。

### 路线二：保留 Compose Desktop JVM，通过 JNA 直连 Objective-C runtime

JNA 可以让 Java 程序访问 native shared libraries；理论上可以通过 `libobjc`、`objc_msgSend`、selector 和 class lookup 调用 AVFoundation。Apple SDK 头文件也公开了 Objective-C runtime 和 message send 入口。

这条路线不推荐作为生产首版。原因不是不可行，而是 AVFoundation 播放需要处理 `CMTime`、block callback、KVO、NSNotification、ARC retain 生命周期、线程 / queue，以及不同返回类型的 `objc_msgSend` ABI。直接在 JVM / JNA 层拼 Objective-C 消息，会把本应由 Objective-C / Swift 编译器检查的错误推到运行时。

适合用途：

- 一次性 spike：验证系统 AVFoundation 是否可从当前 JVM app 进程中加载和播放一个 `file://` URL。
- 不适合承接完整播放器生命周期。

### 路线三：保留 Compose Desktop JVM，通过进程外 helper 或 XPC

这条路线把 AVFoundation 放到独立 macOS native helper 中，主 JVM App 只通过 IPC 发送播放命令和接收事件。helper 可以是普通子进程，也可以是 XPC service。Apple 的 App Sandbox 设计文档明确把 XPC service 作为拆分权限和进程边界的机制之一。

优点：

- native 崩溃不会直接带掉 JVM UI 进程。
- helper 可以有独立 entitlements，适合未来沙盒或权限隔离。
- 播放核心可以用 Swift / Objective-C 写得更自然。

缺点：

- 要设计 IPC 协议、重连、异常退出、事件乱序、helper 生命周期和日志。
- 播放延迟、seek、进度事件和 shutdown 都多一个进程边界。
- 打包结构、签名、公证和 sandbox 配置比进程内 bridge 更复杂。

这条路线适合后续“沙盒权限模型很重”或“native 播放需要隔离”的阶段，不适合作为从 vlcj 迁移的第一步。

### 路线四：新增或切换 Kotlin/Native macOS target

Kotlin/Native 官方支持 Apple targets，包括 `macosArm64` 和 `macosX64`；Kotlin/Native 可以直接使用 Objective-C frameworks，系统 framework 默认可用。也就是说，若项目新增 `macosArm64()`，macOS 侧可以像 iOS 研究中描述的 `iosMain` 一样直接从 Kotlin 调用 `platform.AVFoundation`。

这条路线最接近“macOS 与 iOS 共享同一份 Apple 播放 adapter”。可以设计 `appleMain` / `darwinMain` 之类的共享层，放置 `AVPlayer` adapter 的队列映射、事件 reducer、错误归一化和 security-scoped access 抽象，再由 `iosMain`、`macosMain` 补平台入口差异。

但它会冲击当前桌面形态：

- 当前 macOS UI 是 Compose Desktop JVM，入口为 `DesktopMainKt`，`compose.desktop` 负责 JVM 桌面分发。
- 当前 Gradle 没有 `macosArm64()` / `macosX64()` target，也没有 `macosMain` source set、`kspMacosArm64` 或 macOS native Room 配置。
- 当前桌面扫描器依赖 JVM `java.nio.file`、Swing `JFileChooser` 和 `Dispatchers.IO`；Kotlin/Native macOS 需要用 AppKit / Foundation 重新承接目录选择、文件遍历和权限。
- 当前桌面打包是 Compose Desktop 的 DMG / MSI / Deb 分发；Kotlin/Native macOS 需要另行决定 Swift/AppKit 宿主、Xcode project、bundle、签名、公证和是否继续复用 Compose UI。

因此，这条路线是战略上最统一、工程上最重的方案。除非目标明确变成“重塑 macOS App 运行时”，否则不应把它作为 vlcj 下线的首个落地方案。

### 路线五：Kotlin/Native 播放动态库，JVM UI 调用

这条路线尝试让 Apple 播放核心用 Kotlin/Native 编译为 macOS native library，JVM UI 通过 JNI / JNA / C ABI 调用它。它看起来能共享 Kotlin 播放代码，但实际会引入 JVM Kotlin runtime 和 Kotlin/Native runtime 的边界，事件回调、对象生命周期、线程和错误传递都需要 C ABI 化。

除非后续证明 Objective-C / Swift wrapper 无法满足共享目标，否则不建议作为第一迁移路线。

## 关键事实与引用

### 当前仓库事实

- 当前 Gradle 只有 `iosArm64()`、`iosSimulatorArm64()` 和 `jvm("desktop")`，没有 Kotlin/Native macOS target；desktop 的 main class 是 `com.yanhao.kmpmusic.DesktopMainKt`。来源：`composeApp/build.gradle.kts:31-47`。
- 当前 `desktopMain` 依赖 `compose.desktop.currentOs`、`kotlinx-coroutines-swing`、`jaudiotagger` 和 `vlcj`。来源：`composeApp/build.gradle.kts:87-94`、`gradle/libs.versions.toml:16`、`gradle/libs.versions.toml:40`。
- 当前 Compose Desktop 分发配置使用 `compose.desktop.application`，目标格式包含 `Dmg`、`Msi`、`Deb`。来源：`composeApp/build.gradle.kts:149-160`。
- 当前 macOS LibVLC 打包链路包含下载、提取、`prepareMacosArm64LibVlc`、开发运行系统属性注入、打包时 stage 到 `.app/Contents/Resources/LibVLC`，以及 release app 验收任务。来源：`composeApp/build.gradle.kts:163-261`。
- 当前 `AudioPlayerEngine` 是 common 层和平台播放器之间的播放接口，平台实现负责真实解码、媒体会话、原生库加载和平台权限，播放事实必须通过 `events` 回流 common。来源：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt:9-75`。
- 当前 `PlaybackCoordinator` 已经持有队列、播放模式、失败恢复和事件折返逻辑；自然结束后由 common 层按模式推进，而不是由底层播放器直接决定业务下一首。来源：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt:471-598`。
- 当前 `DesktopVlcjAudioPlayerEngine` 已经把公开命令和底层回调收敛到串行 command channel，并用 generation 过滤旧媒体事件。新 AVFoundation engine 应复用这个结构思想，而不是让 native 回调直接改 repository 或 UI。来源：`composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt:22-97`、`composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt:212-427`。
- 当前桌面会话由 `DesktopPlaybackSession` 进程级持有 Room、真实播放器和共享 controller，并在窗口关闭前释放播放器、等待协程、补写快照、关闭数据库。来源：`composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt:10-49`、`composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSessionRuntime.kt:48-96`。
- 当前桌面扫描器用 Swing `JFileChooser` 选择目录，使用 `java.nio.file.Files.walk` 扫描，生成 `file://` localUri。来源：`composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/data/DesktopFolderMusicScanner.kt:33-63`、`composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/data/DesktopFolderMusicScanner.kt:137-183`、`composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/data/DesktopFolderMusicScanner.kt:213-240`。
- 当前 P0 本地音频识别包含 `mp3`、`m4a`、`aac`、`wav`、`flac`、`ogg`、`opus`、`aiff`、`alac`、`amr` 等扩展名。AVFoundation 迁移前必须确认这些格式的真实可播放矩阵。来源：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/LocalAudioFileRules.kt:9-25`。
- 当前本地音频发现 PRD 明确暂不要求 macOS sandbox security-scoped bookmark，若后续进 Mac App Store 再单独评估。来源：`docs/LOCAL_AUDIO_DISCOVERY_PRD.md:134`。

### Kotlin / Compose 官方事实

- Kotlin/Native 可以使用 Objective-C frameworks 和 libraries，系统 framework 默认可用；Swift library 只有导出为 Objective-C API 时才能从 Kotlin 使用。来源：[Kotlin Swift/Objective-C interop](https://kotlinlang.org/docs/native-objc-interop.html)。
- Kotlin/Native 官方支持 Apple macOS targets，包括 `macosArm64` 和 `macosX64`。来源：[Kotlin Native target support](https://kotlinlang.org/docs/native-target-support.html)。
- Kotlin Multiplatform 可以配置 native binary，例如 executable、framework、static library 和 shared library。来源：[Kotlin Native binaries](https://kotlinlang.org/docs/multiplatform-build-native-binaries.html)。
- Compose Multiplatform 的桌面原生分发文档面向 `compose.desktop` JVM 应用，提供 DMG、PKG、MSI、EXE、DEB、RPM 等桌面包能力，并包含 macOS 签名和公证配置。来源：[Compose Multiplatform native distribution](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-native-distribution.html)。

### JVM bridge 官方事实

- Oracle JNI 规范定义 Java 代码如何声明 native methods，并由 C / C++ 实现和动态库承载。来源：[Oracle JNI specification](https://docs.oracle.com/en/java/javase/17/docs/specs/jni/intro.html)。
- JNA 项目自身定位是让 Java 代码访问 native shared libraries；它可以减少手写 JNI glue，但不等于自动提供高层 AVFoundation 绑定。来源：[JNA 官方仓库](https://github.com/java-native-access/jna)。
- Apple Objective-C runtime 暴露 `objc_msgSend`、class lookup、method lookup 等 C ABI；这让 JNA 直连在技术上可行，也说明错误签名和返回类型会直接落到 ABI 风险上。来源：本机 Xcode SDK `MacOSX26.2.sdk/usr/include/objc/message.h`、`MacOSX26.2.sdk/usr/include/objc/runtime.h`。

### AVFoundation / AVPlayer 官方事实

- `AVPlayer` 的 `play`、`pause`、`rate`、`timeControlStatus`、`reasonForWaitingToPlay`、`replaceCurrentItemWithPlayerItem`、`volume` 都在 macOS 可用。`timeControlStatus` 可 KVO，能区分 paused、waiting 和 playing。来源：[AVPlayer](https://developer.apple.com/documentation/avfoundation/avplayer)，本机 Xcode SDK `AVFoundation.framework/Headers/AVPlayer.h`。
- `play`、`pause` 和 `rate` 在 macOS 13 之前有主线程 / 主队列要求。native bridge 必须决定最低系统版本，或始终把这些命令调度到主队列。来源：本机 Xcode SDK `AVFoundation.framework/Headers/AVPlayer.h`。
- `AVPlayerItem` 会发出播放结束、播放到结束前失败、播放卡顿等通知，而且通知可能在不同于注册 observer 的线程发出。来源：[AVPlayerItem](https://developer.apple.com/documentation/avfoundation/avplayeritem)，本机 Xcode SDK `AVFoundation.framework/Headers/AVPlayerItem.h`。
- `addPeriodicTimeObserver` 可周期上报进度；返回 token 必须持有，并与 `removeTimeObserver` 成对调用。传入 queue 必须是 serial queue，传 `NULL` 时使用 main queue；传 concurrent queue 会导致未定义行为。来源：[AVPlayer.addPeriodicTimeObserver](https://developer.apple.com/documentation/avfoundation/avplayer/addperiodictimeobserver%28forinterval:queue:using:%29)，本机 Xcode SDK `AVFoundation.framework/Headers/AVPlayer.h`。
- `AVQueuePlayer` 是 `AVPlayer` 子类，用于多 item 顺序播放；`advanceToNextItem` 会从队列移除当前 item，同一个 `AVPlayerItem` 不能在队列中重复出现。来源：[AVQueuePlayer](https://developer.apple.com/documentation/avfoundation/avqueueplayer)，本机 Xcode SDK `AVFoundation.framework/Headers/AVPlayer.h`。
- 对本项目首版而言，`AVQueuePlayer` 不应接管业务队列。`PlaybackCoordinator` 已经拥有 loop、shuffle、失败跳过、自然结束推进规则，AVFoundation engine 应回报 `Ended`，让 common 决定下一首。
- `AVURLAsset` 可用 URL 表示 timed audiovisual media，支持 `isPlayable`、`audiovisualMIMETypes`、`audiovisualContentTypes` 和 `isPlayableExtendedMIMEType` 等能力。格式兼容矩阵应通过这些 API 和真实样本验证，而不是从 LibVLC 支持范围推断。来源：[AVURLAsset](https://developer.apple.com/documentation/avfoundation/avurlasset)，本机 Xcode SDK `AVFoundation.framework/Headers/AVAsset.h`。

### macOS 文件权限、沙盒、签名和打包事实

- macOS sandbox 下，security-scoped bookmark 的创建和解析选项在 macOS 可用；创建时可包含 security scope，解析时可恢复 sandboxed process 的访问能力。来源：[Accessing files from the macOS App Sandbox](https://developer.apple.com/documentation/security/accessing-files-from-the-macos-app-sandbox)，本机 Xcode SDK `Foundation.framework/Headers/NSURL.h`。
- `startAccessingSecurityScopedResource()` 成功后必须与 `stopAccessingSecurityScopedResource()` 配对；调用是引用计数语义。来源：[NSURL startAccessingSecurityScopedResource](https://developer.apple.com/documentation/foundation/nsurl/startaccessingsecurityscopedresource%28%29)，本机 Xcode SDK `Foundation.framework/Headers/NSURL.h`。
- App Sandbox、user-selected file read-only / read-write entitlements 是 Apple 官方 entitlements；如果未来进入 Mac App Store 或启用 sandbox，目录选择、扫描、播放和持久访问都必须落到 entitlements 与 security-scoped bookmark 生命周期。来源：[App Sandbox entitlement](https://developer.apple.com/documentation/bundleresources/entitlements/com_apple_security_app-sandbox)、[User-selected read-only entitlement](https://developer.apple.com/documentation/bundleresources/entitlements/com_apple_security_files_user-selected_read-only)、[User-selected read-write entitlement](https://developer.apple.com/documentation/bundleresources/entitlements/com_apple_security_files_user-selected_read-write)。
- XPC 是 Apple 官方的进程间通信机制；若播放 helper 需要独立进程或独立 sandbox 权限，应把 XPC service 作为候选边界，而不是让 JVM 进程直接承担所有 native 权限。来源：[XPC](https://developer.apple.com/documentation/xpc)、[App Sandbox Design Guide](https://developer.apple.com/library/archive/documentation/Security/Conceptual/AppSandboxDesignGuide/AboutAppSandbox/AboutAppSandbox.html)。
- macOS App bundle 有固定目录结构；可执行文件、资源、framework、helper 或 XPC service 的放置位置会影响签名和运行时加载。来源：[Apple Bundle Programming Guide](https://developer.apple.com/library/archive/documentation/CoreFoundation/Conceptual/CFBundles/BundleTypes/BundleTypes.html)。
- 对外分发的 macOS 软件需要签名、公证，并在发布前完成相应验证。来源：[Notarizing macOS software before distribution](https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution)。

## 对当前 Compose Desktop JVM 形态的影响

### 若选择推荐路线：JVM + 进程内 native bridge

需要新增：

- `DesktopAvFoundationAudioPlayerEngine : AudioPlayerEngine`。
- native bridge API：`prepare(url, songId, generation, startPositionMs)`、`play`、`pause`、`seekTo`、`stop`、`setVolume`、`release`、observer 回调。
- Gradle native 构建任务：编译 Objective-C / Swift `.dylib` 或 `.framework`，复制到 `.app` bundle。
- native bridge 的签名和公证验收：确认 bundle 内 native code 已签名，`codesign --verify`、`spctl`、首次启动和隔离属性测试通过。
- desktopTest fake adapter 或 fake bridge，用于验证事件映射、generation 过滤、release 后不回调、seek / skip 竞态。

可以保留：

- `MusicAppController`。
- `PlaybackCoordinator`。
- `AudioPlayerEngine` 契约。
- `DesktopPlaybackSession` 的进程级 controller / engine / database 生命周期。
- 当前 Compose Desktop UI。
- 当前 Room 桌面数据库形态。
- 当前目录扫描基本产品入口。

需要替换或删除：

- `libs.vlcj` 版本和依赖。
- `DesktopVlcjAudioPlayerEngine`、`VlcjMediaPlayerAdapter`、`MacosLibVlcRuntime` 及对应测试。
- `src/desktopMain/packaging/macos-libvlc` 下的下载、提取、来源记录和 verify 脚本。
- `composeApp/build.gradle.kts` 中 LibVLC 下载、提取、stage、verify 和开发运行系统属性注入任务。
- 面向 `EngineUnavailable` 的 LibVLC 缺失错误文案，需要改成 AVFoundation bridge 初始化失败或 native bridge 缺失。

### 若选择 Kotlin/Native macOS target

需要新增或重做：

- `macosArm64()` / `macosX64()` Gradle target 和 `macosMain` source set。
- macOS native app 宿主或 Xcode 工程。
- macOS native UI 承载方式。当前 Compose Desktop JVM UI 不能简单原地变成 Kotlin/Native macOS target。
- Room / KSP 的 macOS native 配置，或替代持久化。
- AppKit / Foundation 目录选择与文件扫描。
- macOS native 打包、签名、公证。

可以复用：

- 一部分 `commonMain` domain、repository interface、controller 逻辑，前提是依赖仍可在 native target 编译。
- iOS AVFoundation adapter 的设计和部分 Kotlin/Native 代码。

最大风险：

- 这不再是播放器实现替换，而是桌面运行时迁移。

## AVFoundation adapter 对当前 `AudioPlayerEngine` 的适配建议

首版建议用单个 `AVPlayer` 加 Kotlin 侧队列状态，而不是 `AVQueuePlayer`。

原因：

- common 层已经拥有队列顺序、随机、单曲循环、失败跳过和自然结束推进。
- `AVQueuePlayer` 会消费自己的队列并移除当前 item，容易和 common 队列状态发生双真相。
- 单 `AVPlayer` 的 `replaceCurrentItemWithPlayerItem` 更贴合当前 `setQueue` / `skipToIndex` / `Ended` 语义。

建议映射：

| `AudioPlayerEngine` 命令 | AVFoundation 映射 |
| --- | --- |
| `setQueue` | 保存 Kotlin 队列和 current index，创建 `AVURLAsset` / `AVPlayerItem`，发出 `CurrentMediaChanged` 和 `Loading`，替换 current item。 |
| `play` | 记录 pending play；item ready 后调用 `AVPlayer.play()`。旧系统或稳妥实现统一转主队列。 |
| `pause` | 清 pending play，调用 `AVPlayer.pause()`，上报 `Paused`。 |
| `seekTo` | 使用 `AVPlayer.seek`，seek 完成或本地确认后补 `ProgressChanged`，旧 generation 回调丢弃。 |
| `skipToIndex` | 新 generation，移除旧 observer，替换 item，重置进度。 |
| `setPlaybackMode` | 首版只记录；loop / shuffle 继续由 `PlaybackCoordinator` 控制。 |
| `setVolume` | 映射到 `AVPlayer.volume`，语义是 App 内相对音量。 |
| `stop` | 暂停，移除 observer，清 pending intent，上报 `Idle`。 |

建议事件映射：

| Apple 事实 | common 事件 |
| --- | --- |
| 替换 item 并确定 songId / index | `CurrentMediaChanged` |
| item 准备中 | `StatusChanged(Loading, positionMs, durationMs)` |
| `timeControlStatus == Playing` | `StatusChanged(Playing, currentPositionMs, durationMs)` |
| `timeControlStatus == WaitingToPlayAtSpecifiedRate` 或 stalled notification | `StatusChanged(Buffering, currentPositionMs, durationMs)` |
| `pause()`、rate 归零或显式暂停 | `StatusChanged(Paused, currentPositionMs, durationMs)` |
| periodic time observer | `ProgressChanged(positionMs, durationMs)` |
| `AVPlayerItemDidPlayToEndTimeNotification` | `Ended` |
| item failed 或 failed-to-end notification | `Failed(PlaybackError(...))` |

## 对 Gradle 和打包链路的影响

### 旧 vlcj / LibVLC 下线前需要补齐的事实

- 新 AVFoundation engine 是否能覆盖当前 P0 格式列表，尤其是 `flac`、`ogg`、`opus`、`alac`、`amr`。
- 新 bridge 在开发运行、`packageDmg`、`packageReleaseDmg`、签名、公证、首次启动、无系统额外安装场景下是否都能加载。
- native bridge 是 `.dylib`、`.framework`、XPC service 还是 helper executable；不同形态决定 `Contents/Frameworks`、`Contents/Resources`、`Contents/MacOS` 或 `Contents/XPCServices` 的放置和签名顺序。
- 是否启用 App Sandbox；如果启用，当前 Swing `JFileChooser` 和 `file://` 持久路径模型必须补 security-scoped bookmark。
- 是否继续支持 Windows / Linux 桌面包。当前 `nativeDistributions` 同时配置 Dmg、Msi、Deb；如果播放能力只面向 macOS，打包任务和运行时注入不能让非 macOS 包误依赖 Apple native bridge。

### 推荐路线的 Gradle 变化方向

第一阶段引入新 bridge 时：

- 保留旧 vlcj 和 LibVLC 打包任务，新增 feature gate 或 runtime factory 选择，方便回退。
- 新增 native bridge 编译和 stage 任务，但不立即删除旧任务。
- 新增 `desktopTest` 覆盖 AVFoundation adapter fake bridge 行为。

第二阶段确认新路线后：

- 删除 `vlcj` 版本和依赖。
- 删除 LibVLC 下载、提取、stage、verify 任务和 `macos-libvlc` 目录。
- 删除 `MacosLibVlcRuntime` 和旧 adapter。
- 用 AVFoundation bridge 的签名和加载验收替代 `verifyMacosArm64ReleaseApp`。

第三阶段清理文档：

- 将旧 `docs/superpowers/specs/2026-06-24-macos-vlcj-playback-design.md` 标记为被 AVFoundation 路线取代，或新增 ADR 解释路线变化。
- 更新播放抽象审计文档中 “Desktop = vlcj” 的表述。

## 未知风险

- AVFoundation 对当前 P0 音频扩展名的实际支持矩阵未验证。尤其 `ogg`、`oga`、`opus` 和部分 `flac` / `amr` 文件可能与 LibVLC 行为不同；必须用真实样本跑 `AVURLAsset.isPlayable`、`isPlayableExtendedMIMEType` 和实际播放。
- 最低 macOS 版本未在当前项目中明确。若需要支持 macOS 13 之前，`AVPlayer.play` / `pause` / `rate` 的主线程要求必须进入 bridge 设计。
- 当前桌面目录授权没有 security-scoped bookmark。未 sandbox 时可继续用普通路径；一旦进入 Mac App Store 或启用 sandbox，扫描和播放都要重新设计持久访问。
- Swift / Objective-C wrapper 的语言选择未定。Swift 写 AVFoundation 更自然，但导出 C / Objective-C ABI 给 JVM 需要额外 wrapper；Objective-C 直接做 JNI 更直接，但代码可维护性可能弱于 Swift。
- Kotlin/Native macOS target 的 UI 承载方式、Room / KSP 支持、Compose macOS native 可行性和分发链路都未验证。
- 进程外 helper / XPC 是否值得引入，取决于 sandbox 和崩溃隔离需求；当前还没有证据表明首版必须承受 IPC 复杂度。
- macOS 系统媒体能力范围未定：Now Playing、媒体键、远程命令、AirPlay、耳机按钮、中断恢复是否首版包含，需要独立决策。

## 建议下一步

1. 在“确定 macOS 运行时形态”ticket 中决策：首版采用“保留 Compose Desktop JVM + 进程内 AVFoundation native bridge”，并把 Kotlin/Native macOS target 标为二阶段评估。
2. 在“确定苹果平台播放 adapter 边界”ticket 中明确：macOS 和 iOS 统一到 `AudioPlayerEngine` 行为契约、单 `AVPlayer` adapter 语义、错误和事件映射；不强求首版共享同一份 Kotlin 实现。
3. 在“确定本地音频来源和权限生命周期”ticket 中补齐 macOS sandbox 策略：当前是否继续非 sandbox 路线，若未来 sandbox，何时引入 security-scoped bookmark。
4. 做一个最小 native spike：JVM 调用 Objective-C / Swift wrapper，用 `AVPlayer` 播放一个 `file://` MP3，回传 prepared、playing、progress、ended、failed，并验证 app bundle 加载。
5. 做格式矩阵 spike：用当前 P0 扩展名样本验证 AVFoundation 可播放性，输出支持、不可支持和需要降级提示的清单。
6. 再进入 vlcj 下线 ticket：确认新 bridge 的构建、测试、签名和打包验收稳定后，分阶段删除旧依赖和 LibVLC 任务。

## 来源索引

- Apple 官方：<https://developer.apple.com/documentation/avfoundation/avplayer>
- Apple 官方：<https://developer.apple.com/documentation/avfoundation/avplayeritem>
- Apple 官方：<https://developer.apple.com/documentation/avfoundation/avqueueplayer>
- Apple 官方：<https://developer.apple.com/documentation/avfoundation/avurlasset>
- Apple 官方：<https://developer.apple.com/documentation/security/accessing-files-from-the-macos-app-sandbox>
- Apple 官方：<https://developer.apple.com/documentation/foundation/nsurl/startaccessingsecurityscopedresource%28%29>
- Apple 官方：<https://developer.apple.com/documentation/bundleresources/entitlements/com_apple_security_app-sandbox>
- Apple 官方：<https://developer.apple.com/documentation/bundleresources/entitlements/com_apple_security_files_user-selected_read-only>
- Apple 官方：<https://developer.apple.com/documentation/bundleresources/entitlements/com_apple_security_files_user-selected_read-write>
- Apple 官方：<https://developer.apple.com/documentation/xpc>
- Apple 官方：<https://developer.apple.com/library/archive/documentation/Security/Conceptual/AppSandboxDesignGuide/AboutAppSandbox/AboutAppSandbox.html>
- Apple 官方：<https://developer.apple.com/library/archive/documentation/CoreFoundation/Conceptual/CFBundles/BundleTypes/BundleTypes.html>
- Apple 官方：<https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution>
- Kotlin 官方：<https://kotlinlang.org/docs/native-objc-interop.html>
- Kotlin 官方：<https://kotlinlang.org/docs/native-target-support.html>
- Kotlin 官方：<https://kotlinlang.org/docs/multiplatform-build-native-binaries.html>
- JetBrains 官方：<https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-native-distribution.html>
- Oracle 官方：<https://docs.oracle.com/en/java/javase/17/docs/specs/jni/intro.html>
- JNA 官方：<https://github.com/java-native-access/jna>
- Apple SDK：`/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX26.2.sdk/System/Library/Frameworks/AVFoundation.framework/Headers/AVPlayer.h`
- Apple SDK：`/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX26.2.sdk/System/Library/Frameworks/AVFoundation.framework/Headers/AVPlayerItem.h`
- Apple SDK：`/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX26.2.sdk/System/Library/Frameworks/AVFoundation.framework/Headers/AVAsset.h`
- Apple SDK：`/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX26.2.sdk/System/Library/Frameworks/Foundation.framework/Headers/NSURL.h`
- Apple SDK：`/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX26.2.sdk/usr/include/objc/message.h`
- Apple SDK：`/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX26.2.sdk/usr/include/objc/runtime.h`
- 当前仓库：`composeApp/build.gradle.kts`
- 当前仓库：`gradle/libs.versions.toml`
- 当前仓库：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/AudioPlayerEngine.kt`
- 当前仓库：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain/playback/PlaybackCoordinator.kt`
- 当前仓库：`composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSession.kt`
- 当前仓库：`composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSessionRuntime.kt`
- 当前仓库：`composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback/DesktopVlcjAudioPlayerEngine.kt`
- 当前仓库：`composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/data/DesktopFolderMusicScanner.kt`
- 当前仓库：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data/LocalAudioFileRules.kt`
- 当前仓库：`docs/LOCAL_AUDIO_DISCOVERY_PRD.md`
