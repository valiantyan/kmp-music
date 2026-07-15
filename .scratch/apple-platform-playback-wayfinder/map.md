Status: closed
Labels: wayfinder:map
Assignee: unassigned

# 苹果平台统一播放迁移地图

## Destination

形成一份可交付的苹果平台播放迁移规格：macOS 不再使用 vlcj / LibVLC，macOS 与 iOS 统一走 Apple 原生 AVFoundation / AVPlayer 系列播放能力，并明确平台运行时形态、抽象边界、迁移顺序、验证门禁和旧实现下线范围。

## Notes

- 本地图只做路线决策，不直接修改播放器实现。
- 已确认的用户决策：macOS 与 iOS 都属于苹果生态范围，后续播放实现应统一使用 Apple 原生播放方案，而不是继续使用 vlcj。
- 当前代码事实：播放主干是 `MusicAppController -> PlaybackCoordinator -> AudioPlayerEngine -> 平台实现`；Android 已使用 Media3，macOS Desktop 目前通过 `DesktopVlcjAudioPlayerEngine`、vlcj 依赖和 LibVLC 打包任务实现真实播放。
- 相关旧设计文档需要在决策时显式处理冲突：`docs/superpowers/specs/2026-06-24-macos-vlcj-playback-design.md` 和 `docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md`。
- 后续若进入 Kotlin 修改，必须按本项目 Kotlin、架构和测试规则执行；本轮 wayfinder 不创建实现提交。
- 本地 Markdown tracker 没有原生 label、assignee 和 blocking，本文档使用 issue 文件头的 `Labels`、`Assignee`、`Blocked by` 作为约定。

## Decisions so far

- [调研 iOS Kotlin/Native 接入 AVFoundation 的边界](issues/01-research-ios-avfoundation-kotlin-native.md) — `iosMain` 可直接调用 AVFoundation；首版更适合单 `AVPlayer` 加 Kotlin 侧队列状态，文件授权生命周期是后续权限决策的重点。
- [调研 macOS 版接入 AVFoundation 的运行时路线](issues/02-research-macos-avfoundation-runtime.md) — 首版建议保留 Compose Desktop JVM 壳，通过进程内 Objective-C / Swift native bridge 调用 AVFoundation；Kotlin/Native macOS target 留作二阶段评估。
- [确定 macOS 运行时形态](issues/03-decide-macos-runtime-shape.md) — 首版保留 Compose Desktop JVM 壳，经进程内 Objective-C / Swift bridge 使用 AVFoundation；`expect/actual` 负责 KMP 装配，Kotlin/Native macOS 路线留作二阶段评估。
- [确定苹果平台播放 adapter 边界](issues/04-decide-apple-playback-adapter-boundary.md) — `AudioPlayerEngine` 继续作为唯一 common 播放契约；iOS / macOS 首版共享行为语义和测试契约，不强求共享实现代码；macOS bridge 是 `desktopMain` 内部可测试边界；vlcj 测试只迁移行为防线。
- [确定本地音频来源和权限生命周期](issues/05-decide-apple-audio-source-permission-lifecycle.md) — iOS P0 复制进 App 沙盒后播放；首版继续使用 `localUri` / `AudioSource.Local(uri)`，不新增 common 授权载体；iOS 媒体资料库作为 P1 候选；macOS 首版使用非 sandbox 普通文件 URL；adapter 不负责恢复权限；错误映射到现有 `PlaybackErrorType`。
- [确定苹果系统播放能力范围](issues/06-decide-apple-system-playback-scope.md) — 首轮只追核心播放语义和必要生命周期能力：iOS 包含 App 内播放控制、后台继续播放、`AVAudioSession` playback category、audio background mode 和中断 / 输出路线变化基础处理；macOS 包含 App 内播放控制和窗口最小化继续播放；Now Playing、锁屏 / 控制中心按钮、耳机线控、远程命令、macOS 媒体键和关闭窗口后继续播放均为后续范围。
- [确定 vlcj 下线迁移顺序](issues/07-decide-vlcj-decommission-sequence.md) — Agent 禁止合入主分支；实现分支交付人工验收前必须达到无活跃 vlcj / LibVLC 生产引用的目标态；不保留 vlcj fallback 或双引擎 runtime gate；先完成 AVFoundation bridge 最小真实播放和 fake bridge 行为测试，再切默认桌面 engine，随后尽早删除 vlcj 依赖、旧 engine / runtime / adapter、LibVLC 打包脚本和旧细节测试；旧 vlcj 文档保留并标记 superseded；Windows / Linux Desktop 暂不支持，桌面分发可收窄到 macOS DMG。
- [确定验证和文档门禁](issues/08-decide-validation-and-doc-gates.md) — 门禁拆成实现分支硬门禁和人工验收 / 发布风险说明；硬门禁包含 common 播放契约测试、Apple fake bridge 行为测试、Android 编译、iOS framework 编译、macOS 编译 / 测试、本机真实播放 smoke、打包 bridge 加载检查、无 vlcj / LibVLC 生产引用、格式支持矩阵、关键错误文案测试、新 ADR、旧 vlcj 文档 superseded 和冲突文档修正；iOS 真机后台 / 锁屏播放属于人工验收；签名 / 公证不作为本 ticket 验收项。

## Ready for implementation issue split

- wayfinder 决策已收束，可以进入实现 issue 拆分。
- 后续实现应按已确认门禁拆分并验收：先建立 AVFoundation bridge 最小真实播放和 fake bridge 行为测试，再切默认桌面 engine，随后删除 vlcj / LibVLC 生产链路，最后补齐格式矩阵、错误文案、文档和人工验收交接。
- 本地图不再新增 grilling ticket；如实现中出现新的产品取舍或大范围架构冲突，再另开独立决策。

## Out of scope

- Android Media3 播放链路重写不属于本地图范围。
- 网络音频、缓存、歌词、均衡器、变速、淡入淡出不属于本地图范围。
- Linux / Windows Desktop 播放实现不属于本地图范围。
- UI 视觉重设计、导航重构和曲库扫描模型重做不属于本地图范围。
- 本轮直接实现 AVFoundation 播放不属于 charting 范围；实现应在地图决策完成后另行执行。
