# 11 — 实现 iOS AVFoundation 播放会话 P0

**What to build:** 让 iOS 使用 AVFoundation / AVPlayer 承接真实播放，并通过平台级播放会话保持播放器、播放协调器和音频会话生命周期稳定。用户可以在 iOS 上完成 App 内播放控制，并获得基础后台播放能力的配置证据。

**Blocked by:** 09 — 建立 Apple 播放契约和 fake bridge 行为防线；10 — 打通 iOS 沙盒导入来源闭环。

Status: ready-for-human

Labels: ready-for-human

- [x] iOS 播放实现支持播放、暂停、seek、上一首、下一首、停止、音量和播放模式同步。
- [x] iOS 使用单个 AVPlayer 加上层队列状态，不让系统队列接管业务队列规则。
- [x] 自然结束、失败、缓冲、暂停、进度和当前媒体变化都通过统一播放事件回流 common 层。
- [x] iOS 播放会话脱离 Compose UI composition 生命周期，持有 controller、engine、音频会话配置和释放收口逻辑。
- [x] 播放开始前配置并激活 playback category；宿主工程或 Info.plist 的 audio background mode 配置有明确证据。
- [x] 中断和输出路线变化至少有基础处理，不让 UI 状态和真实播放事实长期错位。
- [x] 自动化验证包含 fake 行为测试和 iOS framework 编译；真机后台播放记录为人工验收项。
- [x] 不宣称完成 Now Playing、锁屏控制、控制中心按钮、远程命令、耳机线控或冷启动续播。

## 对抗式审查

- 最可能翻车点一：只实现 AVPlayer adapter，忘记平台级播放会话，播放器仍跟随 Compose UI 生命周期释放。修正要求：会话必须持有 controller、engine、音频会话配置和释放收口。
- 最可能翻车点二：把后台继续播放写成已完成，但没有宿主 background mode 或真机证据。修正要求：自动化只证明编译和契约，真机后台播放必须列为人工验收。
- 最可能翻车点三：系统中断、输出路线变化或观察器释放没有处理，导致状态卡在播放中。修正要求：至少覆盖中断开始、可恢复提示、输出断开和释放观察器路径。
- 最可能翻车点四：把 `AVQueuePlayer` 引入后形成第二套队列规则。修正要求：首版保持单 `AVPlayer`，自然结束只回报事件，由 common 决定下一首。

## Comments

- 由 Apple 平台统一播放迁移 PRD 拆分而来。

- 2026-07-14 实现摘要：
  - 新增 iOS 平台级 `IosPlaybackSession` / `IosPlaybackSessionRuntime`，由进程级会话持有 `MusicAppController`、`IosAvFoundationAudioPlayerEngine` 和 `IosAvAudioSessionController`，`IosEntry` 只复用该会话并请求一次播放快照恢复，避免播放器跟随 Compose composition 重建。
  - 新增 iOS AVFoundation 播放链路：`IosPlaybackBridge`、`IosAvFoundationPlaybackBridge`、`IosAvFoundationAudioPlayerEngine` 和 `IosAudioSessionController`。真实 bridge 使用单个 `AVPlayer`，不引入 `AVQueuePlayer`；common 队列、播放模式、自然结束推进和失败策略仍由 `PlaybackCoordinator` 拥有。
  - 引擎支持播放、暂停、seek、切歌、停止、音量、播放模式同步；通过 generation 过滤、`Mutex` 串行状态和 pending seek 处理 seek/skip 竞态及旧回调。
  - 真实 bridge 监听当前 item 结束、失败、卡顿、周期进度、`AVAudioSessionInterruptionNotification` 和 `AVAudioSessionRouteChangeNotification`，并在安装观察器时捕获 generation 快照，避免切歌后延迟通知伪装成当前媒体事件。
  - 播放前通过 `AVAudioSessionCategoryPlayback` 配置并激活 audio session；新增 `IosPlaybackHostConfiguration` 记录宿主 Info.plist 必须配置 `UIBackgroundModes` 包含 `audio`，以及本票明确不包含的 Now Playing、远程命令、控制中心按钮、耳机线控和冷启动续播。
  - 新增 iOS fake bridge / audio session / runtime 测试，覆盖单当前媒体准备、audio session 先于 play、seek/skip generation、缓冲/进度/结束/失败事件回流、中断恢复、输出断开、release 后丢弃延迟回调、会话 controller 稳定和 close 收口。
- TDD 红灯证据：
  - 先新增 iOS 播放会话和 AVFoundation engine fake 测试后运行 `./gradlew :composeApp:iosSimulatorArm64Test`，因缺少 `IosPlaybackBridge`、`IosAvFoundationAudioPlayerEngine`、`IosPlaybackSessionRuntime` 等类型编译失败。
  - 实现过程中 `seekSkipRaceIgnoresStaleGenerationProgress` 曾暴露切歌后旧 generation 进度污染风险，修正为切歌同步推进 generation、失效 prepared 状态并兑现新 generation pending seek。
- 验证命令与结果：
  - `./gradlew :composeApp:tasks --all`：通过；确认 iOS framework 编译任务名为 `linkDebugFrameworkIosSimulatorArm64`。
  - `./gradlew :composeApp:iosSimulatorArm64Test :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid`：通过，57 个 task，11 executed，46 up-to-date。
  - `git diff --check`：通过。
- 对抗式审查结论：
  - 翻车点一：只做 adapter、忘记平台级会话。已通过 `IosPlaybackSession` / `IosPlaybackSessionRuntime` 固化 controller、engine、audio session 和释放收口。
  - 翻车点二：把后台播放伪装成自动化完成。当前自动化只证明 playback category 配置代码、Info.plist 所需 key/value 证据和 framework 编译；真机后台播放、锁屏继续播放、回前台状态同步仍记录为人工验收项。
  - 翻车点三：中断、输出变化或 release 后观察器泄漏导致 UI 卡在 Playing。已覆盖中断开始、可恢复提示、输出断开、release 后延迟回调；真实 bridge 释放 item、time observer 和 audio session observer。
  - 翻车点四：引入 `AVQueuePlayer` 形成第二套队列。当前实现仅使用单个 `AVPlayer`，自然结束只上报 `Ended`，由 common 层决定下一首。
  - 翻车点五：旧 native 回调被切歌后当前 generation 吞掉。已让 bridge 观察器捕获安装时 generation 快照，并让 engine 统一按当前 generation 过滤。
- code review 结论：
  - Standards 审查发现两个 P1：engine 状态未串行化、native 观察器闭包读取全局 generation。已修复为 `Mutex` 串行 engine 状态，并在 bridge 观察器安装时捕获 generation / songId 快照。
  - Standards 审查发现的 runtime 一次性状态未加锁已同步修复为 Foundation 锁保护；“核心实现文件偏长”和“真实 native observer 无法完全用 fake 证明”保留为后续重构/真机风险，不阻塞当前 P0。
  - Spec 审查发现 Ticket 未更新、验证证据未写入、自然结束/缓冲/失败测试不够直接。已更新本 Comments、重跑完整验证，并补充 `nativePlaybackFactsFlowBackAsCommonEvents` fake 行为测试。
- 剩余风险或未完成项：
  - 当前仓库产出的是 KMP framework，没有真实 iOS 宿主 Xcode 工程或 Info.plist；`UIBackgroundModes = audio` 的宿主落地位置、真机后台播放、锁屏后继续播放和回前台状态同步必须由宿主工程人工验收。
  - 本票不包含 Now Playing、锁屏/控制中心按钮、远程命令、耳机线控、AirPlay 专项验收或冷启动续播，后续能力不得把本票当作已完成依据。
  - `IosAvFoundationAudioPlayerEngine` 与 `IosAvFoundationPlaybackBridge` 为 P0 集中实现，文件偏长；后续可在不改变契约的前提下拆出 reducer、observer registry 或错误映射。
