Status: closed
Labels: wayfinder:grilling
Parent map: .scratch/apple-platform-playback-wayfinder/map.md
Assignee: codex
Blocked by: 无
Blocks: 确定验证和文档门禁

# 确定 vlcj 下线迁移顺序

## Question

macOS 从 vlcj / LibVLC 迁移到 Apple 原生播放方案时，应该按什么顺序下线依赖、平台 engine、打包脚本、验证脚本、测试和旧设计文档，才能控制回归风险？

这个决策需要明确：

- 是否先引入新 Apple engine 并通过 feature gate 或平台注入切换，再删除 `DesktopVlcjAudioPlayerEngine`。
- `gradle/libs.versions.toml` 的 `vlcj` 依赖、`composeApp/build.gradle.kts` 的 LibVLC 下载提取打包任务、`src/desktopMain/packaging/macos-libvlc` 资源应在哪个阶段删除。
- 旧 macOS vlcj 设计文档应标记为 superseded、移入历史，还是保留并新增 ADR 解释路线改变。
- 每个阶段需要哪些回滚点和最小验证，避免一次性删除后 macOS 播放不可诊断。

## Comments

- 2026-07-14：当前会话已认领此 ticket，按 grilling 顺序确认 macOS vlcj / LibVLC 下线迁移顺序。
- 2026-07-14 resolution：确认 07 号 ticket 只约束 macOS 删除 vlcj / LibVLC 的迁移顺序；iOS AVFoundation 支持仍属于 Apple 首轮目标，但不在本 ticket 展开，后续由验证和文档门禁覆盖。

  Agent 禁止合入主分支。AVFoundation 实现应在独立实现分支中完成，用户人工验收通过后由用户手动合入。实现分支交付人工验收前必须达到无活跃 vlcj / LibVLC 生产引用的目标态；不保留 runtime feature gate、vlcj fallback 或双引擎运行时选择。macOS 目标方案已经确定为 AVFoundation，若实现分支验收不过，应继续修复 AVFoundation bridge、事件映射、打包加载或测试缺陷，不能把“切回 vlcj”作为完成方案。

  迁移顺序采用实现分支内的阶段推进，但不要求每个中间提交都可发布：第一，先完成 AVFoundation bridge 最小真实播放 spike 和 fake bridge 行为测试，至少验证 JVM 能加载 native bridge、能播放一个本地 `file://` MP3 或 M4A、能回传 prepared / playing / progress / ended / failed 等核心事件，并用 fake bridge 覆盖 `AudioPlayerEngine` 行为契约。第二，将 Desktop factory / runtime 装配切到 AVFoundation engine。第三，在 AVFoundation 默认链路的核心验收通过后尽早删除 vlcj / LibVLC 代码、依赖、脚本和细节测试，让无 vlcj 目标态尽早进入编译、测试和打包循环。第四，修复删除后暴露的编译、测试、打包、错误文案和文档引用问题。第五，按后续验证门禁做完整验收。

  删除范围包括：`gradle/libs.versions.toml` 的 `vlcj` version 和 library、`composeApp/build.gradle.kts` 的 `libs.vlcj` 依赖、LibVLC 下载 / 提取 / prepare / stage / verify 任务、desktop run 注入的 `kmp.music.libvlc.runtime.dir`、`DesktopVlcjAudioPlayerEngine`、`MacosLibVlcRuntime`、`VlcjMediaPlayerAdapter`、旧的 LibVLC runtime resolver / packaging 细节测试，以及 `composeApp/src/desktopMain/packaging/macos-libvlc/` 下的 README、SOURCE_RECORD、download / extract / verify 脚本。旧脚本和资源目录从生产树直接删除，不保留为历史参考；历史参考交给 git 历史。

  测试资产只迁移行为防线，不迁移 vlcj 细节。应改写或新增到 AVFoundation fake bridge / engine 测试的行为包括：命令串行、generation 过滤、`setQueue` 准备成功或失败收口、`skipToIndex` 后旧回调不污染当前曲目、release 后不发事件且不挂起 ack、进度事件只归因当前媒体、bridge 初始化失败映射为 `EngineUnavailable`。应删除的细节包括：LibVLC 路径解析、`VLC_PLUGIN_PATH`、vlcj callback snapshot、vlcj media location、LibVLC app bundle 验证脚本行为。

  旧的中性命名协作者不能把 vlcj 语义伪装成通用边界继续保留。`DesktopMediaPlayerAdapter` 当前仍携带 `pluginPath`、vlcj 事件语义和旧 fake 形状；新链路应建立自己的 `DesktopAvFoundationPlayerBridge` 或等价 fake bridge 边界。可以迁移 command loop、generation、ack、progress ticker、reducer 等行为结构，但不能为了少改文件名把 vlcj 语义藏进新实现。

  错误文案保留 `EngineUnavailable` 类型，但删除或改写所有面向 LibVLC 缺失的用户文案和测试。新的不可用原因应指向 AVFoundation native bridge 初始化失败、bridge 动态库缺失或加载失败、系统 AVFoundation 播放器不可用等；不再出现“安装 VLC”“LibVLC missing”“VLC_PLUGIN_PATH”这类当前产品提示，除非位于已标记 superseded 的历史文档。

  文档处理采用保留并标记过时：`docs/superpowers/specs/2026-06-24-macos-vlcj-playback-design.md` 不删除、不移动，但顶部应标记 `Superseded`，说明该设计已被 Apple AVFoundation 迁移路线取代，vlcj / LibVLC 不再是 macOS 目标实现。`docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md` 中 “Desktop = vlcj” 和 “未来 Windows 优先复用 Desktop vlcj engine” 的表述必须改成历史状态或待重新设计，不能继续作为有效路线。是否新增 ADR 留给 08 号验证和文档门禁决定。

  Windows / Linux Desktop 暂不支持。删除 vlcj 后，桌面真实播放只承诺 macOS AVFoundation；桌面分发可收窄到 macOS DMG，或至少由 08 号门禁明确 MSI / Deb 不构成首轮验收产物，避免删除唯一跨平台桌面播放器后仍暗示 Windows / Linux 可真实播放。

  对抗式审查结论：第一，主分支合入不是 Agent 职责，若把“合入前”写成 Agent 门槛会越权，正确边界是实现分支提交人工验收前达到目标态。第二，保留 vlcj fallback 会制造长期双引擎复杂度，并和已确认的 AVFoundation 目标路线冲突。第三，过晚删除 vlcj 会让测试和打包可能继续隐性依赖旧链路，导致假绿；应在 AVFoundation 最小链路跑通并成为默认后尽早删除。第四，直接删除所有旧测试会丢失已验证的播放行为防线，因此只删除 LibVLC / vlcj 细节，迁移平台无关行为场景。第五，继续产出 Windows / Linux 桌面包会误导用户，以为删除 vlcj 后非 macOS 桌面仍支持真实播放。
