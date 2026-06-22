# KMP Music 播放能力 PRD

## 1. 文档信息

| 项目 | 内容 |
| --- | --- |
| 产品名称 | KMP Music |
| 文档名称 | 播放能力 PRD |
| 目标平台 | Android、iOS、Desktop |
| 技术方向 | Kotlin Multiplatform + Compose Multiplatform |
| 文档版本 | v0.1 |
| 创建日期 | 2026-06-22 |
| 当前阶段 | 播放能力 MVP 定义 |

## 2. 背景与目标

KMP Music 是本地音乐优先的跨平台播放器。当前项目已有共享播放状态、播放队列、迷你播放器和播放页 UI，但真实音频播放仍处于内存状态阶段。播放能力下一阶段要从“UI 状态模拟”演进为“真实平台播放”，同时保持 `commonMain` 的业务状态、队列、收藏、导航和 UI 不被平台播放器 API 污染。

本 PRD 面向人类开发者和 LLM Agent，明确播放能力的产品范围、平台边界、实现约束和验收标准。

## 3. 总体原则

1. 移动端优先保证真实播放、后台播放和用户可感知的系统播放入口。
2. Desktop 先保证 App 内播放，不把系统级媒体能力列入 MVP。
3. `commonMain` 只承载平台无关的播放命令、播放状态、队列状态和错误模型。
4. Android、iOS、Desktop 的真实播放能力必须放在对应平台 source set 或平台适配层。
5. 不为了局部功能把 Media3、AVFoundation、Desktop 播放库直接塞进 UI 层。
6. 真实播放器事件是播放状态的最终来源；UI 可以即时反馈用户操作，但必须被平台 engine event 校正。
7. 播放失败、权限失效、文件缺失和格式不支持都必须进入统一错误模型，而不是静默失败。

## 4. 范围定义

### 4.1 Android P0

Android 首阶段必须支持：

| 能力 | 优先级 | 说明 |
| --- | --- | --- |
| 本地音频真实播放 | P0 | 使用 Media3 ExoPlayer 播放本地音乐文件。 |
| 播放、暂停、上一首、下一首 | P0 | App 内迷你播放器和播放页控制真实播放器。 |
| 进度更新 | P0 | 播放页和迷你播放器可读取真实播放进度。 |
| Seek | P0 | 播放页进度条可跳转到指定位置。 |
| 后台播放 | P0 | App 切后台后继续播放当前歌曲。 |
| 媒体通知栏显示 | P0 | 通知显示歌曲名、歌手、封面和基础播放控制。 |
| 播放队列同步 | P0 | App 内队列状态和真实播放器队列保持一致。 |
| 播放失败反馈 | P0 | 文件缺失、格式不支持、权限失效时有可感知错误状态。 |

Android 推荐技术路线：

- `androidx.media3:media3-exoplayer`
- `androidx.media3:media3-session`
- `MediaSessionService`
- Android foreground service `mediaPlayback`

### 4.2 iOS P0

iOS 首阶段必须支持：

| 能力 | 优先级 | 说明 |
| --- | --- | --- |
| 本地音频真实播放 | P0 | 使用 AVFoundation `AVPlayer` 或等价 Apple 原生播放能力播放本地音乐文件。 |
| 播放、暂停、上一首、下一首 | P0 | App 内迷你播放器和播放页控制真实播放器。 |
| 进度更新 | P0 | 播放页和迷你播放器可读取真实播放进度。 |
| Seek | P0 | 播放页进度条可跳转到指定位置。 |
| 后台播放 | P0 | App 切后台后继续播放当前歌曲。 |
| 播放队列同步 | P0 | App 内队列状态和真实播放器队列保持一致。 |
| 播放失败反馈 | P0 | 文件缺失、格式不支持、权限失效时有可感知错误状态。 |

iOS 推荐技术路线：

- AVFoundation `AVPlayer`
- `AVAudioSession` playback category
- iOS Background Modes: Audio

iOS 注意事项：

- iOS 没有 Android 风格的媒体通知栏。
- `MPNowPlayingInfoCenter` 属于系统 Now Playing 信息展示，可能出现在控制中心或锁屏。
- 当前阶段不把 `MPNowPlayingInfoCenter` 列为 P0，除非后续明确要做 iOS 系统正在播放信息展示。
- 当前阶段不做蓝牙耳机、车机、锁屏专项体验验收。若平台基础能力自然生效，可以保留，但不作为本阶段目标。

### 4.3 Desktop P0

Desktop 首阶段必须支持：

| 能力 | 优先级 | 说明 |
| --- | --- | --- |
| App 内真实播放 | P0 | 在 Compose Desktop App 内播放本地音乐文件。 |
| 播放、暂停、上一首、下一首 | P0 | App 内迷你播放器和播放页控制真实播放器。 |
| 进度更新 | P0 | 播放页和迷你播放器可读取真实播放进度。 |
| Seek | P0 | 播放页进度条可跳转到指定位置。 |
| 播放队列同步 | P0 | App 内队列状态和真实播放器队列保持一致。 |
| 播放失败反馈 | P0 | 文件缺失、格式不支持、权限失效时有可感知错误状态。 |

Desktop 暂不要求：

- 后台播放专项能力。
- 系统媒体键。
- 系统托盘或 macOS 菜单栏迷你播放器。
- Windows System Media Transport Controls。
- macOS Now Playing 集成。
- 桌面歌词。

Desktop 后续需要调研的方向：

| 方向 | 说明 | 风险 |
| --- | --- | --- |
| JavaFX Media | API 简单，适合 MVP spike。 | 格式支持和系统集成能力有限。 |
| vlcj / LibVLC | 本地格式支持更完整，跨 Windows、macOS、Linux。 | 需要处理 LibVLC 分发、安装包体积和授权边界。 |
| GStreamer | 插件和 pipeline 能力强。 | 学习、打包、插件分发复杂度较高。 |
| 原生桥接 | macOS 用 AVFoundation，Windows 用 Media Foundation。 | 当前 Desktop 是 JVM Compose Desktop，桥接成本高，暂不作为 MVP 默认路线。 |

## 5. 非目标范围

以下能力不属于本阶段：

1. 在线流媒体播放。
2. DRM、会员、版权保护。
3. 歌词同步。
4. 跨设备播放同步。
5. 蓝牙耳机专项适配。
6. 车机或 Android Auto / CarPlay。
7. iOS 锁屏专项展示。
8. Desktop 系统媒体键和托盘能力。
9. 音频均衡器、变速、淡入淡出、高级音效。
10. 将 WebView 播放器作为真实 App 播放方案。

系统可能自动展示的能力不算作本阶段验收项。例如 Android 媒体通知可能被系统投射到锁屏，iOS 后续若启用 Now Playing 信息也可能出现在控制中心或锁屏；本阶段不对这些系统展示位置做专项 UI 承诺。

## 6. 用户体验要求

### 6.1 App 内播放体验

1. 用户点击歌曲后，当前歌曲应立即切换，播放状态应在迷你播放器、播放页和歌曲列表中同步。
2. 播放按钮状态必须反映真实播放器状态，而不是只切换内存布尔值。
3. 当前播放歌曲在所有列表中继续使用全局红色文本和播放中辅助标识。
4. 播放失败时应展示明确错误提示，并允许用户重试、跳过或返回列表。
5. 切换歌曲时，封面、标题、歌手、专辑、时长、进度应同步更新。

### 6.2 后台播放体验

1. Android 和 iOS 切后台后，当前歌曲继续播放。
2. 回到 App 后，UI 状态必须与真实播放器状态一致。
3. App 进程未被系统杀死时，播放队列和当前歌曲不应丢失。
4. 播放结束后，应按当前队列策略进入下一首或停止。

### 6.3 Android 媒体通知体验

1. 通知显示当前歌曲标题、歌手和封面。
2. 通知提供播放/暂停基础控制。
3. 通知状态和 App 内状态保持一致。
4. 停止播放或队列清空时，通知应按 Android 媒体播放惯例清理或降级。

### 6.4 平台降级体验

1. 如果移动端后台播放因系统策略、权限、音频焦点或音频会话失败而不可用，App 必须回到可理解的错误状态，并提示用户重试或回到前台播放。
2. 如果系统媒体展示能力不可用，真实播放和 App 内控制仍应保持可用；系统展示能力不应成为 App 内播放的硬依赖。
3. 如果平台发生电话、闹钟、其他 App 抢占音频等中断，当前阶段只要求同步为 paused、interrupted 或 error 等状态，不要求复杂自动恢复策略。

## 7. 共享架构要求

### 7.1 分层边界

播放能力应保持如下边界：

```text
commonMain
├── domain model
├── playback command
├── playback state
├── queue state
├── platform-neutral error
└── AudioPlayerEngine interface

androidMain
└── Media3AudioPlayerEngine

iosMain
└── AvFoundationAudioPlayerEngine

desktopMain
└── DesktopAudioPlayerEngine
```

### 7.2 `commonMain` 可以包含

1. 播放命令：play、pause、resume、seek、next、previous、stop。
2. 播放状态：idle、loading、playing、paused、buffering、ended、error。
3. 播放进度：positionMs、durationMs。
4. 当前媒体信息：songId、title、artist、album、coverArt、localUri。
5. 队列状态：songIds、currentIndex、repeatMode、shuffleEnabled。
6. 错误模型：missingFile、unsupportedFormat、permissionDenied、unknown。

### 7.3 `commonMain` 禁止包含

1. `ExoPlayer`、`MediaSession`、`MediaSessionService`。
2. `AVPlayer`、`AVAudioSession`、`MPNowPlayingInfoCenter`。
3. JavaFX Media、LibVLC、GStreamer。
4. Android、iOS、Desktop 专属文件权限 API。
5. 平台通知、service、系统媒体键实现。

### 7.4 推荐接口形态

```kotlin
interface AudioPlayerEngine {
    val events: Flow<PlaybackEngineEvent>

    suspend fun load(item: PlayableMedia)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun stop()
}
```

接口仅表达平台无关的播放意图。平台实现负责把这些意图转换为 Media3、AVFoundation 或 Desktop 播放库调用。

### 7.5 状态真相来源

播放链路应区分“用户意图”和“真实结果”：

| 层级 | 职责 | 是否是真相来源 |
| --- | --- | --- |
| UI | 发送播放、暂停、seek、切歌等用户意图。 | 否 |
| Controller / UseCase | 校验命令、维护队列、把命令转发给 engine。 | 部分，仅负责业务队列真相 |
| `AudioPlayerEngine` | 执行真实播放并发布进度、状态、错误和结束事件。 | 是，负责播放状态真相 |
| `PlaybackRepository` | 保存当前播放快照和队列快照，供 UI 和测试读取。 | 是，负责持久化快照，不直接代表底层播放器成功 |

实现时不能只修改 `PlaybackRepository.isPlaying` 就宣称播放成功。真实播放成功、暂停、结束、错误和进度变化必须由 `AudioPlayerEngine.events` 回流后同步到 repository 和 UI。

### 7.6 现有 `PlaybackRepository` 的演进关系

当前 `PlaybackRepository` 是内存播放状态仓库，不是真实音频 engine。后续演进应保持两个角色：

1. `PlaybackRepository`：保存平台无关的播放状态和队列状态，便于 UI、UseCase 和测试读取。
2. `AudioPlayerEngine`：封装平台真实播放器，负责播放文件、暂停、seek、停止、进度和错误事件。

优先让 UseCase 或 Controller 同时协调 repository 和 engine，不要把平台播放器依赖注入到 Composable 页面。

## 8. 数据与状态要求

当前 `Song` 模型以展示字段为主，真实播放需要补充或派生平台可播放数据。

### 8.1 歌曲数据

真实播放至少需要：

| 字段 | 说明 |
| --- | --- |
| `id` | 稳定歌曲标识。 |
| `title` | 歌曲标题。 |
| `artist` | 歌手名称。 |
| `album` | 专辑名称。 |
| `durationMs` | 毫秒时长，供进度和系统展示使用。 |
| `localUri` | 平台可解析的本地媒体 URI 或路径。 |
| `coverArt` | App 内封面资源或本地封面引用。 |

`localUri` 不应由 UI 拼接。它应由本地音乐扫描、平台媒体库 data source 或测试 fake data source 产生，并在进入播放 engine 前转换成平台可识别的媒体定位信息。

### 8.2 播放状态

真实播放状态至少需要：

| 字段 | 说明 |
| --- | --- |
| `currentSongId` | 当前歌曲标识。 |
| `status` | 当前播放状态。 |
| `positionMs` | 当前播放进度。 |
| `durationMs` | 当前歌曲时长。 |
| `error` | 可选播放错误。 |

### 8.3 队列状态

队列状态至少需要：

| 字段 | 说明 |
| --- | --- |
| `songIds` | 当前播放队列。 |
| `currentIndex` | 当前播放位置。 |
| `repeatMode` | 顺序、单曲循环、列表循环。 |
| `shuffleEnabled` | 是否随机播放。 |

## 9. LLM Agent 实现约束

LLM Agent 修改播放能力时必须遵守：

1. 修改前先读本 PRD、`docs/PRD.md`、`AGENTS.md` 和相关源码。
2. 不要在 UI Composable 中直接创建平台播放器。
3. 不要在 `commonMain` 引入平台专属 API。
4. 不要用单页面补丁绕过共享播放状态。
5. 先定义平台无关接口和状态，再接平台实现。
6. Android 实现必须放在 `androidMain` 或 Android 专属适配层。
7. iOS 实现必须放在 `iosMain` 或 Swift/Objective-C 包装层。
8. Desktop 实现必须放在 `desktopMain`。
9. 改动播放状态、队列、导航或收藏联动时，必须补充共享测试。
10. 无法验证真实设备行为时，最终说明必须标明剩余平台风险。

## 10. 分阶段里程碑

### Milestone 1: 共享播放边界

目标：

- 建立 `AudioPlayerEngine` 平台无关接口。
- 扩展播放状态、队列状态和错误模型。
- 明确 `PlaybackRepository` 保存状态快照，`AudioPlayerEngine` 执行真实播放。
- 保留内存或 fake engine，保证 Desktop test 和 Android 编译可运行。

验收：

- UI 不再只依赖 `isPlaying` 布尔值表达真实播放状态。
- 播放页、迷你播放器、列表当前播放状态仍保持一致。
- 共享测试覆盖 play、pause、next、previous、seek、error 状态流转。

### Milestone 2: Android 真实播放

目标：

- 接入 Media3 ExoPlayer。
- 接入 `MediaSessionService`。
- 实现后台播放和媒体通知栏显示。

验收：

- Android 真机或模拟器可播放本地音频。
- App 切后台后音乐继续播放。
- 媒体通知显示当前歌曲信息和基础控制。
- 通知控制和 App 内 UI 状态同步。

### Milestone 3: iOS 真实播放

目标：

- 接入 AVFoundation `AVPlayer`。
- 配置 `AVAudioSession` playback category。
- 开启 Audio background mode。

验收：

- iOS 模拟器或真机可播放本地音频。
- App 切后台后音乐继续播放。
- 回到 App 后播放状态和进度同步。
- 文件缺失或格式不支持时有错误反馈。

### Milestone 4: Desktop 播放调研与 MVP

目标：

- 对 JavaFX Media、vlcj/LibVLC、GStreamer 做最小 spike。
- 选择 Desktop MVP 播放后端。
- 实现 App 内播放、暂停、切歌、进度和错误反馈。

验收：

- macOS Desktop 和 Windows Desktop 至少明确一个可实施后端。
- Desktop App 内可播放本地音频。
- Desktop 暂不要求系统媒体键、托盘或系统 Now Playing。

## 11. 测试要求

### 11.1 共享测试

必须覆盖：

1. 播放指定歌曲后，当前歌曲和队列同步。
2. 暂停和恢复不改变当前歌曲。
3. 下一首和上一首按队列移动。
4. Seek 更新进度状态。
5. 播放失败进入 error 状态，并保留可恢复路径。
6. 切换底部 Tab 或二级页面时，播放状态不丢失。

### 11.2 Android 验证

至少验证：

1. `:composeApp:compileDebugKotlinAndroid` 通过。
2. 真机或模拟器播放本地音频。
3. 后台播放继续。
4. 媒体通知显示并可控制播放/暂停。

### 11.3 iOS 验证

至少验证：

1. iOS 编译通过。
2. 模拟器或真机播放本地音频。
3. 后台播放继续。
4. 前台恢复后状态同步。

### 11.4 Desktop 验证

至少验证：

1. `:composeApp:desktopTest` 通过。
2. Desktop App 内播放本地音频。
3. 播放、暂停、切歌、seek 可用。
4. 不支持的文件有错误反馈。

## 12. 风险与待调研问题

| 问题 | 风险 | 当前处理 |
| --- | --- | --- |
| iOS 后台播放配置 | App 生命周期、音频会话和系统权限细节不熟悉。 | 先按 AVAudioSession + Background Modes 做 P0。 |
| iOS Now Playing | 可能被误解为通知栏或锁屏专项功能。 | 当前不列入 P0，后续单独确认。 |
| Desktop 播放后端 | JavaFX Media、LibVLC、GStreamer 的格式、打包、授权差异较大。 | 先调研和 spike，不提前锁死。 |
| 本地文件 URI | Android、iOS、Desktop 的文件访问和权限模型不同。 | 通过平台数据源提供 `PlayableMedia`。 |
| 状态同步 | 平台播放器事件和 UI 状态可能不一致。 | 以 engine event 为真实状态来源，控制器统一同步。 |
| 测试环境 | 后台播放和通知行为依赖真实系统。 | 编译和共享测试之外，保留真机验证清单。 |

## 13. 举一反三问题速查

本节用于帮助人类和 LLM Agent 判断：遇到相邻问题时，是否能从本 PRD 找到处理方向。

| 问题 | 文档答案 | 依据 |
| --- | --- | --- |
| Android 可以直接在 `PlayerScreen` 创建 `ExoPlayer` 吗？ | 不可以，平台播放器不能进 UI Composable。 | 7.1、7.3、9 |
| iOS 是否需要做 Android 风格媒体通知栏？ | 不需要，iOS 没有 Android 风格媒体通知栏。 | 4.2 |
| iOS 后台播放是不是 P0？ | 是，必须支持切后台继续播放。 | 4.2、6.2、10 |
| iOS Now Playing 是不是 P0？ | 不是，后续明确需要系统正在播放展示时再做。 | 4.2、12 |
| Desktop 是否需要后台播放或系统媒体键？ | 不需要，MVP 只要求 App 内播放。 | 4.3、5、10 |
| 如果 UI 显示正在播放但底层播放器失败怎么办？ | 以 `AudioPlayerEngine.events` 的错误事件校正状态，并同步到 repository 和 UI。 | 3、7.5、12 |
| `Song` 没有真实文件路径时能直接播放吗？ | 不能，必须由扫描或平台 data source 提供 `localUri` / `PlayableMedia`。 | 8.1、12 |
| 文件缺失或格式不支持时怎么办？ | 进入统一错误模型，并给用户重试、跳过或返回路径。 | 4、6、8、11 |
| Android 媒体通知不可见时播放能力是否失败？ | 不一定。App 内真实播放仍应可用，但需记录系统策略或权限风险。 | 6.4、11、12 |
| 以后要加蓝牙耳机或锁屏专项体验，应放在哪里？ | 作为新阶段平台系统集成能力，不进入当前 P0。 | 5、12 |

## 14. 三轮交叉 Review 结果

### 14.1 第一轮：产品范围 Review

结论：

- Android P0 范围清晰：真实播放、后台播放、媒体通知。
- iOS P0 范围已明确为真实播放和后台播放，不把 Android 通知栏概念套到 iOS。
- Desktop P0 只做 App 内播放，系统级能力进入调研或后续阶段。

补强项：

- 增加了平台降级体验，避免系统展示能力失败时被误判为播放能力失败。
- 增加了举一反三问题速查，帮助后续 Agent 判断相邻需求是否属于当前范围。

### 14.2 第二轮：架构边界 Review

结论：

- 文档已明确 `commonMain` 只能放平台无关命令、状态、队列和错误模型。
- Android、iOS、Desktop 的真实播放器都必须留在平台 source set。
- 需要更清楚地区分现有 `PlaybackRepository` 和未来真实 `AudioPlayerEngine`。

补强项：

- 增加了状态真相来源表，规定真实播放状态以 engine event 为准。
- 增加了 `PlaybackRepository` 演进关系，避免把状态仓库误当真实播放器。
- 增加了 `localUri` 来源约束，避免 UI 拼接平台文件路径。

### 14.3 第三轮：实现与验证 Review

结论：

- 里程碑顺序合理：先共享边界，再 Android，再 iOS，再 Desktop 调研。
- 测试要求覆盖共享状态、Android 通知、iOS 后台、Desktop App 内播放。
- 后台播放、通知和 iOS 音频会话依赖真实系统，不能只靠单元测试证明完成。

补强项：

- 在 Milestone 1 明确 `PlaybackRepository` 保存状态快照、`AudioPlayerEngine` 执行真实播放。
- 增加了平台中断和错误降级要求。
- 保留真机/模拟器验证清单，防止 Agent 在未验证时声称平台能力完成。

## 15. 参考资料

- [Android Developers: Jetpack Media3](https://developer.android.com/media/media3?hl=zh-cn)
- [Android Developers: Media3 background playback](https://developer.android.com/media/media3/session/background-playback?hl=zh-cn)
- [Apple: AVFoundation](https://developer.apple.com/av-foundation/)
- [Apple: Configuring Audio Settings for iOS and tvOS](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/MediaPlaybackGuide/Contents/Resources/en.lproj/ConfiguringAudioSettings/ConfiguringAudioSettings.html)
- [Apple: Refining the User Experience](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/MediaPlaybackGuide/Contents/Resources/en.lproj/RefiningTheUserExperience/RefiningTheUserExperience.html)
- [Kotlin: Swift/Objective-C interop](https://kotlinlang.org/docs/native-objc-interop.html)
- [Microsoft: Media Foundation](https://learn.microsoft.com/en-us/windows/win32/medfound/microsoft-media-foundation-sdk)
- [Microsoft: System Media Transport Controls](https://learn.microsoft.com/en-us/windows/apps/develop/media-playback/system-media-transport-controls)
- [GStreamer documentation](https://gstreamer.freedesktop.org/documentation/application-development/introduction/gstreamer.html)
- [vlcj project](https://capricasoftware.co.uk/projects/vlcj)
