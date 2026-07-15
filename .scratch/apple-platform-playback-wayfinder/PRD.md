Status: ready-for-agent
Labels: ready-for-agent
Source: apple-platform-playback-wayfinder

# 苹果平台统一播放迁移实现规格

## 问题陈述

KMP Music 当前的播放主干已经通过 common 层统一队列、播放模式、状态回流和错误处理，但苹果平台播放能力仍不一致：iOS 尚未落到稳定的 AVFoundation 播放会话，macOS Desktop 仍依赖 vlcj / LibVLC。这个状态会带来三个用户可感知问题：macOS 需要额外处理第三方运行时和打包内置风险，iOS 文件来源和后台播放生命周期不稳定，苹果平台的可播放格式、错误提示和系统能力边界也不清晰。

用户需要的是一条能落地的苹果平台统一播放迁移路线：macOS 不再使用 vlcj / LibVLC，iOS 与 macOS 都使用 Apple 原生 AVFoundation / AVPlayer 系列能力，并且不破坏现有 Android 播放链路、common 队列语义和本地音乐优先的产品闭环。

## 解决方案

首轮实现以“替换真实播放后端并守住现有播放语义”为目标。iOS 使用 Kotlin/Native 直接接入 AVFoundation，macOS 保留当前 Compose Desktop JVM 应用壳，通过进程内 Objective-C / Swift native bridge 调用 AVFoundation。两个平台不强求共享同一份播放器实现代码，但必须共享同一个 common 播放契约、同一套事件语义、同一套队列规则和同一组用户可感知播放行为。

macOS 实现分支交付人工验收前必须移除所有活跃 vlcj / LibVLC 生产链路，不保留 vlcj fallback、双引擎 runtime gate 或“切回 VLC”作为完成方案。iOS 首轮必须具备稳定的 App 内播放、后台继续播放基础能力和 AVAudioSession 生命周期收口。格式能力、错误文案、文档路线和人工验收边界要同步更新，避免用户看到“扫描成功但播放失败且无法自救”的体验。

## 用户故事

1. 作为一名 macOS 本地音乐用户，我想直接播放本机音乐文件，所以不需要安装或依赖 VLC 相关组件。
2. 作为一名 macOS 本地音乐用户，我想从桌面 App 内播放、暂停、上一首、下一首和拖动进度，所以日常听歌操作不因引擎迁移退化。
3. 作为一名 macOS 本地音乐用户，我想最小化窗口后音乐继续播放，所以可以边做其他事情边听歌。
4. 作为一名 macOS 本地音乐用户，我想文件被移动、删除或外置盘断开时看到明确提示，所以知道应该恢复文件或重新扫描。
5. 作为一名 macOS 本地音乐用户，我想遇到不支持的格式时看到明确提示，所以不会误以为 App 坏了。
6. 作为一名 macOS 本地音乐用户，我想 App 打包安装后仍能加载播放组件，所以不只是在开发运行里可用。
7. 作为一名 iOS 本地音乐用户，我想导入的音乐在 App 内稳定播放，所以不会因为外部文件授权释放而播放失败。
8. 作为一名 iOS 本地音乐用户，我想切到后台或锁屏后音乐继续播放，所以可以按移动音乐 App 的基本预期使用。
9. 作为一名 iOS 本地音乐用户，我想回到前台后看到正确的当前歌曲、进度和播放状态，所以不会丢失播放上下文。
10. 作为一名 iOS 本地音乐用户，我想系统中断或输出设备变化后 App 有基础处理，所以播放状态不会无提示地错乱。
11. 作为一名 iOS 本地音乐用户，我想无法访问导入文件时看到授权或重新导入提示，所以知道如何恢复播放。
12. 作为一名收藏歌曲的用户，我想收藏页、播放页和迷你播放器使用同一份当前播放事实，所以跨页面状态保持一致。
13. 作为一名按队列听歌的用户，我想顺序、随机和单曲循环仍由 App 统一处理，所以切换平台后播放模式含义不变。
14. 作为一名拖动进度的用户，我想 seek 后旧进度回调不会把进度拉回去，所以操作反馈稳定。
15. 作为一名快速切歌的用户，我想上一首和下一首不会被旧媒体回调污染，所以当前歌曲始终准确。
16. 作为一名听完整首歌的用户，我想自然结束后按当前播放模式推进，所以平台播放器不会绕过 App 队列规则。
17. 作为一名遇到播放失败的用户，我想 App 尝试按既有失败策略处理下一首，所以播放队列不被单个坏文件卡死。
18. 作为一名使用 Android 的既有用户，我想苹果平台迁移不破坏 Android Media3 播放，所以原有真实播放链路继续可编译可回归。
19. 作为一名本地曲库用户，我想扫描入口只展示真实可播放或可解释的问题状态，所以不会把 AVFoundation 不支持的格式当成普通歌曲静默失败。
20. 作为一名导入音乐的用户，我想 `MP3`、`M4A/AAC`、`WAV`、`FLAC`、`AIFF/ALAC` 等常见格式有明确支持结论，所以知道哪些文件适合导入。
21. 作为一名拥有 `OGG`、`OPUS` 或 `AMR` 文件的用户，我想看到明确的支持、不可支持或待验证结论，所以不会依赖旧 LibVLC 的格式假设。
22. 作为一名开发者，我想播放业务规则继续集中在 common 播放协调器，所以平台实现只负责真实播放和事件回流。
23. 作为一名开发者，我想 macOS native bridge 有 fake 边界可测试，所以能稳定验证乱序回调、释放时序和初始化失败。
24. 作为一名开发者，我想 iOS 播放会话脱离 Compose UI composition 生命周期，所以后台播放不会依赖偶然存活的界面对象。
25. 作为一名开发者，我想删除 vlcj 后文档同步标记旧路线过时，所以后续实现者不会继续按旧 VLC 方案扩展。
26. 作为一名维护者，我想实现分支交接时看到自动化命令、真实播放 smoke、格式矩阵和人工验收待办，所以可以判断是否进入人工验收。
27. 作为一名维护者，我想签名、公证和发布级 Gatekeeper 验收被记录为后续发布风险，而不是实现线程伪造通过，所以验收证据可信。
28. 作为一名产品维护者，我想首轮只承诺核心播放和必要生命周期能力，所以不会把 Now Playing、远程命令、媒体键等后续能力混进本次迁移。

## 实现决策

- 播放主干继续保持 `MusicAppController -> PlaybackCoordinator -> AudioPlayerEngine -> 平台实现` 的方向。`AudioPlayerEngine` 是 common 层唯一播放契约，不新增面向苹果平台的 common 专用 adapter。
- `PlaybackCoordinator` 继续拥有队列、播放模式、自然结束推进、失败跳过、随机和单曲循环等业务规则。平台播放器只能把播放事实归一化为播放事件回流 common 层。
- iOS 首版使用 Kotlin/Native 直接调用 AVFoundation / AVPlayer 系列能力，不需要为了普通 Objective-C API 额外加 Swift bridge。只有在规避强链接风险、封装复杂观察器生命周期或暴露纯 Swift API 时，才引入 wrapper。
- iOS 播放 adapter 使用单个 `AVPlayer` 加 Kotlin / common 队列状态，不让 `AVQueuePlayer` 接管业务队列。自然结束只上报结束事件，由 common 层决定下一首。
- iOS 必须新增平台级播放会话，持有 controller、engine、AVAudioSession 配置和生命周期收口，避免真实播放器跟随 Compose UI composition 被释放。
- iOS P0 本地文件来源采用“用户选择音频后复制进 App 沙盒，再扫描和播放沙盒内文件”的模型。security-scoped access 只用于导入或复制窗口，不进入播放 adapter 的长期职责。
- iOS 导入链路必须让进入播放队列的 `localUri` 指向 App 沙盒内可长期访问的文件 URL。若实现阶段仍发现外部 Files URL 进入队列，应视为阻塞问题，而不是让播放 adapter 临时持有外部授权。
- iOS 系统音乐资料库作为 P1 候选，不阻塞本次 P0。只有授权通过、资源有可播放 URL、不是云端待下载、不是受保护资源且 AVFoundation 判定可播放时，才允许进入播放队列。
- iOS 后台播放必须留下宿主配置证据：播放开始前配置并激活 `AVAudioSession` 的 playback category，宿主工程或 Info.plist 启用 audio background mode。若宿主工程不在当前仓库，交接 issue 必须记录外部配置位置、验证方式和剩余风险。
- macOS 首版保留 Compose Desktop JVM 应用壳，通过进程内 Objective-C / Swift native bridge 调用 AVFoundation。Kotlin/Native macOS target 或更深的 macOS Native 迁移留作二阶段评估。
- macOS bridge 是 desktop 平台层内部可测试边界，不进入 common。桌面 AVFoundation engine 负责队列、代际、命令串行化和事件 reducer；bridge 负责 native 调用、观察器、回调和原生资源释放。
- macOS bridge 的 JVM 边界只传递平台无关数据，例如歌曲标识、本地文件 URL、起始进度、音量、命令和事件，不把 Objective-C 对象泄漏到 Kotlin 层。
- macOS bridge 必须有明确的命令和事件契约，至少覆盖准备、播放、暂停、seek、停止、音量、释放、准备完成、播放中、缓冲、进度、结束、失败和初始化失败。契约必须说明 generation、回调线程、释放后回调、命令 ack 和 native 资源所有权，避免 JNI / native 回调成为隐式协议。
- macOS 首版继续非 sandbox 普通文件 URL 路线。文件夹删除、移动、外置盘断开或普通文件权限不足时，播放失败映射为统一错误。macOS security-scoped bookmark、App Sandbox entitlements 和 Mac App Store 权限模型后续单独设计。
- 首轮两个苹果平台一致的是用户可感知播放语义和播放契约，不要求一致接入系统媒体入口。iOS 包含后台继续播放基础能力；macOS 只承诺窗口最小化后继续播放。
- iOS P0 包含 App 内播放控制、后台继续播放、AVAudioSession playback category、audio background mode、中断和输出路线变化基础处理。
- iOS P0 不包含 Now Playing 专项体验、锁屏或控制中心按钮、耳机线控、远程命令、AirPlay 专项验收、用户上滑杀 App 后恢复播放或冷启动续播。
- macOS P0 不包含关闭窗口后继续播放、菜单栏常驻、系统媒体键、控制中心、Now Playing、后台播放专项验收或远程命令。
- 首版不扩展播放错误枚举。文件不存在或移动映射为缺失文件；权限不足或授权失效映射为权限拒绝；格式不支持、DRM、受保护资源或云端无本地 URL 映射为不支持格式；native bridge 缺失或初始化失败映射为引擎不可用；其他 AVFoundation 错误映射为未知错误。
- 播放 adapter 不负责恢复权限、弹权限 UI、启动文件选择器、恢复 bookmark、重新扫描或改写曲库数据库。scanner / importer 负责权限与来源生成，repository 负责持久化和可用状态，coordinator 负责失败后的统一策略。
- macOS 实现分支内先完成 AVFoundation bridge 最小真实播放和 fake bridge 行为测试，再把桌面默认 engine 切到 AVFoundation，随后尽早删除 vlcj / LibVLC 生产链路。
- 交付人工验收前不得保留 vlcj fallback、双引擎 runtime gate 或活跃 vlcj / LibVLC 生产引用。验收失败时应继续修复 AVFoundation 链路，而不是把切回 vlcj 作为完成方案。
- 删除范围包括 vlcj 依赖声明、LibVLC 下载和提取任务、运行时路径注入、LibVLC 打包和验证脚本、旧 vlcj engine、旧 runtime resolver、旧 adapter、旧 LibVLC 细节测试和旧运行时资源目录。历史参考交给 git 历史。
- 旧中性命名协作者不能继续保留 vlcj 语义。新的桌面播放链路应建立 AVFoundation bridge 边界，可以迁移 command loop、generation、ack、progress ticker 和 reducer 等行为结构，但不能保留 VLC 插件路径或 callback 形状。
- 错误文案必须彻底移除面向 VLC / LibVLC 的用户提示。新的引擎不可用提示应指向 Apple 播放组件或 native bridge 不可用。
- 格式能力必须按 Apple 平台真实能力重新确认，不从 LibVLC 支持范围推断。扫描入口、可播放判断、错误提示和文档都必须反映新的支持矩阵。
- 文档必须新增架构决策记录，说明 macOS 从 vlcj / LibVLC 改为 Apple AVFoundation，iOS / macOS 统一到 Apple 原生播放方案。旧 vlcj 设计文档保留但标记过时，旧播放抽象审计中仍把 Desktop 等同于 vlcj 的表述必须修正。
- Windows / Linux Desktop 首轮不支持真实播放。删除 vlcj 后，桌面真实播放只承诺 macOS AVFoundation；非 macOS 桌面分发若继续存在，必须明确不属于本次验收产物。
- Agent 不负责合入主分支。实现应在独立分支完成，交付人工验收后由用户决定是否合入。

## 测试决策

- 最高测试缝合点是 common 播放行为：通过 `PlaybackCoordinator` 和 `AudioPlayerEngine` 契约验证队列、播放模式、自然结束、失败推进、快照恢复和状态回流。测试应覆盖用户可感知行为，不依赖平台实现内部细节。
- 平台测试的次级缝合点是 Apple fake engine / fake bridge。macOS 必须用 fake bridge 驱动确定性事件，覆盖命令串行、generation 过滤、准备成功、准备失败、seek / skip 竞态、旧回调丢弃、release 后不回调、不挂起 ack、进度只归因当前媒体和 bridge 初始化失败。
- iOS 首轮自动化门禁聚焦 framework 编译和 fake engine / bridge 契约测试。真机播放、后台继续播放、锁屏后音频继续、回前台状态同步属于人工验收，不伪装成自动化已完成。
- macOS 硬门禁必须包含真实本机播放 smoke。至少用 AVFoundation engine 播放一个本地 `file://` MP3 或 M4A，并验证 prepared、playing、progress、ended、failed 等核心事件能回流。
- macOS 硬门禁必须包含打包产物 bridge 加载检查，确认当前 App 或 DMG 产物包含并能加载 native bridge。签名、公证、Developer ID、staple 和 Gatekeeper 发布验收不作为本实现规格的必过门禁。
- Android 编译必须进入硬门禁，因为实现会触碰 common 播放契约、错误模型或可播放媒体模型，Android Media3 是现有真实播放链路的回归哨兵。
- 最低自动化命令包含 `./gradlew :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid`。iOS framework 编译任务必须先通过 `./gradlew :composeApp:tasks` 查证实际名称后再运行，不能猜任务名。
- iOS 自动化和交接证据必须覆盖两条容易漏掉的边界：导入后播放队列只消费 App 沙盒内文件 URL，后台播放所需的 audio session 与 background mode 配置已经落地或被明确列为人工验收风险。
- 格式支持矩阵进入硬门禁。实现分支必须用真实样本、AVFoundation 可播放性检查或真实播放 smoke 输出 Apple 平台格式支持矩阵，至少覆盖 `MP3`、`M4A/AAC`、`WAV`、`FLAC`、`AIFF/ALAC`，并明确 `OGG`、`OPUS`、`AMR` 的支持、不可支持或待验证状态。
- 错误文案测试必须覆盖引擎不可用、文件缺失、权限拒绝、不支持格式和未知错误。测试应确认用户文案不再出现 VLC、LibVLC、安装 VLC、VLC 插件路径或旧运行时路径提示。
- 无 vlcj / LibVLC 生产引用证明进入硬门禁。实现分支交付前需要证明生产代码、Gradle 依赖、打包任务和运行时路径不再依赖 vlcj、LibVLC、VLC App、VLC 插件路径或旧 LibVLC runtime 参数。
- 旧 vlcj 测试资产只迁移行为防线，不迁移库细节。路径解析、插件路径、LibVLC app bundle 验证脚本、vlcj media location 和 vlcj callback snapshot 细节应删除或替换。
- 文档门禁需要验证新增 ADR、旧 vlcj 文档过时标记、播放抽象审计修正和实现 issue 交接记录。交接记录必须包含实现摘要、验证命令与结果、格式矩阵、人工验收待办和剩余风险。
- 现有测试先例包括 common 控制器测试、播放错误文案测试、播放模型测试和桌面播放 engine 行为测试。新增测试应复用这些层级，而不是让 UI 或 repository 直接理解 AVFoundation。

## 不在范围

- Android Media3 播放链路重写不在本规格范围内。
- 网络音频、缓存、歌词、均衡器、变速、淡入淡出不在本规格范围内。
- Linux / Windows Desktop 真实播放实现不在本规格范围内。
- macOS Kotlin/Native App 运行时迁移、Swift/AppKit 宿主重做、Room native 替换和桌面 UI 运行时重塑不在本规格范围内。
- iOS 系统音乐资料库 P1 能力不阻塞本次实现。
- iOS Now Playing、锁屏或控制中心按钮、耳机线控、远程命令、AirPlay 专项验收和冷启动续播不在首轮范围内。
- macOS 菜单栏常驻、关闭窗口后继续播放、系统媒体键、控制中心、Now Playing 和远程命令不在首轮范围内。
- macOS App Sandbox、security-scoped bookmark、Mac App Store 权限模型、签名、公证和发布级 Gatekeeper 验收不作为本规格硬门禁。
- UI 视觉重设计、导航重构、曲库扫描模型整体重做和登录云同步不在本规格范围内。
- 保留 vlcj fallback、双引擎 runtime gate 或继续维护 LibVLC 打包链路不在本规格范围内。

## 对抗式审查

- 最可能翻车点一：规格说 iOS 要后台播放，但实现只做 AVPlayer adapter，忘了宿主 Info.plist / Background Modes 和 AVAudioSession 生命周期。修正要求：PRD 已明确宿主配置证据和交接风险，不能只用 fake engine 测试宣称后台播放完成。
- 最可能翻车点二：iOS 仍把外部 Files URL 交给播放器，扫描结束释放 security scope 后真实设备播放失败。修正要求：PRD 已把沙盒内 `localUri` 作为进入播放队列的硬边界，外部 URL 进入队列视为阻塞问题。
- 最可能翻车点三：macOS bridge 没有显式命令 / 事件契约，JNI、Swift / Objective-C 回调和 Kotlin reducer 之间靠实现细节碰运气。修正要求：PRD 已要求 bridge 契约说明命令、事件、generation、线程、ack、释放后回调和 native 资源所有权。
- 最可能翻车点四：实现线程只跑 fake bridge 测试，漏掉 native bridge 打包加载、真实本机播放和无 vlcj 生产引用证明。修正要求：PRD 已把本机播放 smoke、打包加载检查、格式矩阵和无 vlcj / LibVLC 生产引用列为硬门禁。
- 最可能翻车点五：删除 VLC 文案后没有覆盖权限、缺文件、DRM 和格式不支持，用户仍然无法自救。修正要求：PRD 已要求错误文案测试覆盖全部关键错误类型，不能只检查 `EngineUnavailable`。

## 补充说明

- 本规格来自 wayfinder 已关闭的 8 个决策 ticket 和两份研究输出，目标是把路线决策压缩成一份可执行的实现规格。
- 本规格默认采用已确认的测试缝合点：common 行为优先，平台 fake bridge 次之，真实播放和打包加载作为 macOS 硬门禁。
- 实现拆分建议按风险递进：先做 AVFoundation bridge 最小真实播放和 fake bridge 行为测试，再切换默认桌面 engine，再删除 vlcj / LibVLC 生产链路，最后补齐格式矩阵、错误文案、ADR、旧文档过时标记和人工验收交接。
- 若实现中出现新的产品取舍或大范围架构冲突，应另开决策 ticket，不把未决问题藏进实现分支。
