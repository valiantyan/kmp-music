# 12 — 打通 macOS AVFoundation bridge 最小真实播放

**What to build:** 让当前 Compose Desktop JVM 应用能加载进程内 Apple native bridge，并用 AVFoundation 播放一个本地音频文件。这个 ticket 交付的是最小真实播放通路，不切换默认桌面播放器。

**Blocked by:** 09 — 建立 Apple 播放契约和 fake bridge 行为防线。

Status: ready-for-human

Labels: ready-for-agent

- [x] JVM 能加载 Apple native bridge，并能报告 bridge 初始化成功或失败。
- [x] bridge 能用 AVFoundation 播放本地 MP3 或 M4A 文件。
- [x] bridge 能回传准备完成、播放中、进度、自然结束和失败事件。
- [x] 释放播放器后 native 回调不会污染已释放的 engine，也不会挂起命令 ack。
- [x] bridge 的线程调度、generation 和资源释放行为符合 09 号契约。
- [x] 本 ticket 不切换桌面默认 engine，不删除旧 vlcj 链路，也不把 spike 代码伪装成完成迁移。
- [x] 本机真实播放 smoke 有可复查证据；若环境不能播放，记录阻塞原因和已完成的替代验证。

## 对抗式审查

- 最可能翻车点一：只证明 native 库能加载，没有证明 AVFoundation 事件能回流到 engine。修正要求：smoke 必须覆盖准备、播放中、进度、结束和失败事件。
- 最可能翻车点二：bridge smoke 顺手切默认播放器，导致旧链路和新链路混在一起难以回滚。修正要求：本票只交付最小真实播放通路，默认切换留给 13。
- 最可能翻车点三：释放时序没测，native 回调在 JVM 侧对象释放后继续写状态。修正要求：释放后回调必须被丢弃或安全收口，并有测试或 smoke 证据。

## Comments

- 由 Apple 平台统一播放迁移 PRD 拆分而来。
- 2026-07-14 实现摘要：新增 macOS AVFoundation 进程内 JNI / Objective-C++ native bridge，JVM 侧通过 `MacosAvFoundationPlaybackBridge`、native library loader、JNI session 和 callback adapter 接入 09 号 `ApplePlaybackBridge` 契约；新增 Gradle `compileMacosAvFoundationBridge` 任务编译 dylib，并让 `desktopTest` 显式加载该 dylib。新增 `macosAvFoundationBridgeSmoke`，在本机生成 M4A/AAC 样本后通过 AVFoundation 播放，验证 prepared、playing、progress、ended 和缺文件 failed 事件回流。本票未修改 `DesktopAudioRuntimeFactory`，未切换默认桌面 engine，未删除 vlcj / LibVLC 链路。
- 2026-07-14 TDD 证据：先新增 `MacosAvFoundationPlaybackBridgeTest`，首次运行 `./gradlew :composeApp:desktopTest` 因缺少 `MacosAvFoundationPlaybackBridge`、native loader、session factory、callback 和状态常量等类型编译失败；补齐最小实现和 native bridge 后转绿。
- 2026-07-14 验证命令与结果：`./gradlew :composeApp:desktopTest` 通过，覆盖 fake native callback、初始化成功 / 失败、真实 dylib 加载、事件映射和 release 后延迟回调丢弃；`./gradlew :composeApp:macosAvFoundationBridgeSmoke` 通过，关键输出包含 `prepared(generation=1)`、`playing(generation=1)`、多次 `progress(generation=1)`、`ended(generation=1)`、`failed(generation=2,type=MissingFile)`，样本路径为 `composeApp/build/macos-avfoundation-bridge/smoke/macos-avfoundation-smoke.m4a`；`./gradlew :composeApp:compileDebugKotlinAndroid` 通过；`git diff --check` 通过。
- 2026-07-14 对抗式审查结论：一，只证明 native 库加载不够，已用真实 M4A smoke 验证 AVFoundation 事件完整回流，并额外覆盖缺文件 failed。二，本票不能顺手切默认播放器，已确认 `DesktopAudioRuntimeFactory` 仍使用 `DesktopVlcjAudioPlayerEngine`，vlcj / LibVLC 依赖和打包任务未删除。三，release 时序最容易污染已释放 engine，已用单元测试验证 release 后 fake native failure 被丢弃，native 侧 release 会移除 observer、time observer、current item 并停止后续回调。四，ack 不能被主线程调度卡死，已把 native 操作收敛到 bridge 私有串行队列，不依赖 JVM/JUnit 泵 GCD main queue。五，错误类型不能靠 enum 顺序，已改为 JNI 显式错误码映射。
- 2026-07-14 code review 结论：Standards 审查先发现两个问题：native 命令调度依赖 main queue、native 错误按 enum ordinal 映射；均已修复并重跑 `desktopTest` 与真实 smoke 通过。Spec 审查先指出 Ticket 尚未更新、01-08 决策票在工作树中未跟踪；Ticket 已按本条更新为 `ready-for-human`，01-08 决策票是本轮开始前已存在的批次输入文件，本票未修改且提交时不纳入本票 commit。复查当前实现满足 12 号 checklist，未越界实现 13-17，也未改 01-08。
- 2026-07-14 剩余风险或未完成项：本票只交付最小真实 bridge 和本机 smoke，不承诺打包产物内置 bridge、桌面默认 engine 切换、vlcj / LibVLC 下线、格式矩阵或发布签名公证；这些仍属于 13-17 后续票范围。当前 smoke 在 macOS 本机通过，非 macOS host 的 bridge 编译与 smoke 任务会跳过，不能作为其他桌面平台真实播放承诺。
