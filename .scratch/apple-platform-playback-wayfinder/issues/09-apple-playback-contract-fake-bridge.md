# 09 — 建立 Apple 播放契约和 fake bridge 行为防线

**What to build:** 建立 Apple 平台播放实现的共同验收边界，让 iOS 和 macOS 后续实现都按同一套命令、事件、代际、ack、释放和初始化失败语义交付。这个 ticket 完成后，后续真实 AVFoundation 实现可以先接入 fake bridge 验证行为，再接原生播放器。

**Blocked by:** None — can start immediately.

Status: ready-for-human

Labels: ready-for-agent

- [x] 明确 Apple 播放 bridge 的命令和事件契约，覆盖准备、播放、暂停、seek、停止、音量、释放、准备完成、播放中、缓冲、进度、结束、失败和初始化失败。
- [x] 契约说明 generation、回调线程、释放后回调、命令 ack 和 native 资源所有权。
- [x] fake bridge 能驱动确定性事件，覆盖准备成功、准备失败、seek、skip、旧回调、释放后回调和初始化失败。
- [x] 平台 engine 行为测试只验证用户可感知播放事实，不依赖 AVFoundation 内部实现细节。
- [x] common 播放协调器仍是队列、播放模式、自然结束和失败推进的唯一业务真相源。
- [x] 不修改 common 播放契约；若发现必须扩展契约，应另开决策票，不能在本 ticket 中顺手扩面。
- [x] 验证命令至少包含桌面测试；若任务名变化，先查任务列表再记录实际命令。

## 对抗式审查

- 最可能翻车点一：fake bridge 只模拟理想顺序，真实 native 回调乱序后仍污染当前媒体。修正要求：测试必须覆盖旧 generation、seek / skip 竞态和释放后回调。
- 最可能翻车点二：为了方便 Apple 实现而偷改 common 契约，破坏 Android 和现有控制器测试。修正要求：本票只建立平台内部契约，不改 `AudioPlayerEngine` 对外语义。
- 最可能翻车点三：ack、线程和资源所有权没有写清，后续 JNI / Kotlin/Native 实现各自解释。修正要求：契约必须显式说明命令完成、失败、超时和释放后的行为。

## Comments

- 由 Apple 平台统一播放迁移 PRD 拆分而来。
- 2026-07-14 实现摘要：新增 desktop 平台内部 Apple bridge 契约与 `DesktopAppleAudioPlayerEngine` 行为防线，覆盖 prepare/play/pause/seek/stop/volume/release 命令、prepared/buffering/playing/paused/progress/ended/failed/initialization failed 事件、generation 过滤、命令 ack、释放后回调和 native 资源所有权说明；新增 `FakeApplePlaybackBridge` 和桌面测试，通过 fake 事件验证准备成功、准备失败、seek、skip、旧回调、释放后回调、初始化失败、ack 失败与超时、自然结束只回传 `Ended`。未修改 common `AudioPlayerEngine` 或 `PlaybackCoordinator`。
- 2026-07-14 验证命令与结果：`./gradlew :composeApp:desktopTest` 通过；`./gradlew :composeApp:compileDebugKotlinAndroid` 通过；`git diff --cached --check` 通过。过程中先写测试并确认红灯：`desktopTest` 因缺少 `FakeApplePlaybackBridge`、`DesktopAppleAudioPlayerEngine` 等新类型编译失败；实现后转绿。
- 2026-07-14 对抗式审查结论：一，已用旧 generation、seek/skip 竞态、release 后延迟 failure 覆盖乱序 native 回调污染风险。二，`git diff` 确认 common 播放契约与协调器无改动，并用 Android 编译做回归哨兵。三，bridge 契约已显式写清 generation、任意回调线程、释放后语义、命令 accepted/failed/timed out ack 和 native 资源所有权。
- 2026-07-14 code review 结论：Spec 审查最初指出缺少成功 seek、ack failed/timeout 和自然结束测试，已补齐并重跑验证通过；Standards 审查未发现阻断级问题，指出 Apple command loop 与旧 desktop loop 同构、Apple engine 与旧 VLCJ engine 生命周期平行，这些为后续 AVFoundation 默认切换和 vlcj 下线阶段的收敛风险，本票为避免修改旧生产链路暂不泛型化旧 loop。
- 2026-07-14 剩余风险或未完成项：本票只建立平台内部契约和 fake 行为防线，未接真实 AVFoundation native bridge，未切换桌面默认 engine，未删除 vlcj/LibVLC，未做真实播放 smoke 或打包加载检查；这些属于后续 12-17 ticket 范围。
