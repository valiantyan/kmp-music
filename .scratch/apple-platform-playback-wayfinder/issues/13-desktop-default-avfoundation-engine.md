# 13 — 切换桌面默认播放链路到 AVFoundation engine

**What to build:** 让 macOS Desktop App 默认使用 AVFoundation engine 播放本地曲库。用户从现有桌面入口点击歌曲后，播放、暂停、seek、切歌、自然结束和窗口最小化继续播放都走新的 Apple 原生链路。

**Blocked by:** 12 — 打通 macOS AVFoundation bridge 最小真实播放。

Status: ready-for-human

Labels: ready-for-agent

- [x] 桌面运行时默认装配 AVFoundation engine，而不是旧 vlcj engine。
- [x] 从本地曲库点击歌曲后可以通过 AVFoundation 完成播放、暂停、seek、上一首、下一首和停止。
- [x] 窗口最小化后音乐继续播放，回到窗口后当前歌曲、状态和进度保持一致。
- [x] 旧媒体回调不会污染当前曲目，快速切歌和 seek 的结果稳定。
- [x] 播放模式仍由 common 播放协调器决定，底层 AVFoundation 不成为第二个队列真相源。
- [x] bridge 初始化失败映射为统一播放错误，不允许静默回退到 vlcj。
- [x] 桌面测试和本机 smoke 证明新默认链路可用；如仍有旧链路残留，只能作为待删除对象，不能作为 runtime fallback。

## 对抗式审查

- 最可能翻车点一：默认 factory 仍在失败时回退 vlcj，表面切换成功但实际保留双引擎。修正要求：bridge 不可用时只能映射为统一错误，不能 runtime fallback。
- 最可能翻车点二：只验证直接播放，不验证从本地曲库入口到播放状态回流的完整用户路径。修正要求：验收必须覆盖用户从现有桌面入口点击歌曲后的播放、状态和进度。
- 最可能翻车点三：把 macOS 可用误写成 Desktop 全平台可用。修正要求：本票只承诺 macOS，Windows / Linux 真实播放留给后续设计或风险说明。

## Comments

- 由 Apple 平台统一播放迁移 PRD 拆分而来。
- 2026-07-15 实现摘要：`DesktopAudioRuntimeFactory` 默认装配已从 `DesktopVlcjAudioPlayerEngine` 切换为 `DesktopAppleAudioPlayerEngine + MacosAvFoundationPlaybackBridge`，不再解析 `MacosLibVlcRuntime`，也不在 bridge 不可用时回退 vlcj。新增 `macosAvFoundationDefaultRuntimeSmoke`，通过默认桌面运行时、`MusicAppController` 和真实 AVFoundation bridge 播放本机 M4A 样本，验证本地曲库播放入口到 controller 状态回流的生产装配链路。新增 `DesktopAudioRuntimeFactoryTest` 覆盖默认 engine 类型、本地曲库播放/暂停/seek/stop、common 下一首/上一首经默认运行时下发到 Apple bridge、旧 generation 进度不污染当前曲目、bridge 初始化失败映射为统一 `EngineUnavailable` 且不回退 vlcj；补充 `DesktopAppleAudioPlayerEngineTest.stopSendsBridgeCommandAndEmitsIdle` 覆盖 stop 行为。
- 2026-07-15 TDD 证据：先新增 `DesktopAudioRuntimeFactoryTest` 后首次运行 `./gradlew :composeApp:desktopTest` 因 `DesktopAudioRuntimeFactory.create` 缺少 `bridgeFactory` 和 `dispatcher` 参数编译失败；随后最小修改工厂装配 AVFoundation engine 后转绿。后续 code review 指出默认真实 smoke 证据不足，新增并扩展 `macosAvFoundationDefaultRuntimeSmoke` 覆盖 `current-media`、`playing`、`progress`、`seek`、`paused`、`resume`、`next`、`previous` 和 `stop`。
- 2026-07-15 验证命令与结果：`./gradlew :composeApp:desktopTest :composeApp:macosAvFoundationBridgeSmoke :composeApp:macosAvFoundationDefaultRuntimeSmoke :composeApp:compileDebugKotlinAndroid` 通过。bridge smoke 输出包含 `prepared`、`playing`、多次 `progress`、`ended` 和 `failed(type=MissingFile)`；默认 runtime smoke 输出包含 `current-media`、`playing`、`progress`、`seek`、`paused`、`resume`、`next`、`previous` 和 `stop`，样本路径为 `composeApp/build/macos-avfoundation-bridge/smoke/macos-avfoundation-smoke.m4a`。`git diff --check` 通过。
- 2026-07-15 对抗式审查结论：一，默认 factory 不能失败回退 vlcj，已移除默认装配中的 LibVLC 解析和 vlcj engine 创建，初始化失败经 `MacosAvFoundationPlaybackBridge` 事件映射为 `EngineUnavailable`。二，不能只验证直接 bridge，已新增默认 runtime smoke 走 `DesktopAudioRuntimeFactory -> MusicAppController -> DesktopAppleAudioPlayerEngine -> AVFoundation bridge`。三，本票只承诺 macOS，新增文案和测试均使用 macOS AVFoundation，不把 Windows / Linux Desktop 写成可用。四，旧回调污染风险由 `DesktopAppleAudioPlayerEngineTest.seekSkipRaceIgnoresStaleGenerationCallbacks` 和本票新增 previous/next 默认运行时测试覆盖。五，窗口最小化没有自动化 GUI 驱动，代码证据是 `DesktopPlaybackSession` 为进程级会话，`DesktopMain` 只在 `onCloseRequest` 调用 `DesktopPlaybackSession.close()`，最小化窗口不会释放 engine；真实 GUI 最小化仍建议人工验收确认。
- 2026-07-15 code review 结论：Standards 初审发现显式类型和测试职责过宽问题，已补类型并拆分/收窄测试；最终 Standards 复查通过。Spec 初审指出默认真实 smoke 只覆盖 play/progress，已扩展为播放、seek、暂停、恢复、上一首、下一首和停止；复查中关于 `libs.vlcj` 与 LibVLC 打包任务残留的意见属于 15 号下线票范围，本票按用户要求不实现 15，但已确认这些残留不再作为 13 号默认 runtime fallback。
- 2026-07-15 剩余风险或未完成项：旧 vlcj / LibVLC 依赖、打包任务、旧 engine 和旧测试资产仍作为 15 号 ticket 的待删除对象存在；本票只保证默认桌面播放链路不再使用它们。窗口最小化继续播放未做自动化 GUI smoke，仅有进程级会话生命周期代码证据；打包产物 bridge 内置检查、格式矩阵和无 vlcj 生产引用证明仍属于 14-17 后续票范围。
