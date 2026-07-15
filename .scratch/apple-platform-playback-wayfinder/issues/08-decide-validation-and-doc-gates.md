Status: closed
Labels: wayfinder:grilling
Parent map: .scratch/apple-platform-playback-wayfinder/map.md
Assignee: codex
Blocked by: 无
Blocks: 无

# 确定验证和文档门禁

## Question

苹果平台统一播放迁移完成前，必须通过哪些自动化测试、手动验收、打包检查和文档更新，才能认为“macOS 与 iOS 使用同一 Apple 播放方案”已经可交付？

这个决策需要明确：

- common 层 `PlaybackCoordinator`、平台 engine fake 测试、iOS 模拟器或真机验证、macOS 桌面运行和打包验证各自最低门槛。
- 需要新增哪些错误文案测试，替代当前面向 LibVLC 缺失的 `EngineUnavailable` 文案。
- 是否需要实际音频样本、设备权限脚本、macOS 首次授权或沙盒场景截图作为验收证据。
- 哪些文档必须更新：旧设计文档、ADR、AGENTS 指南、README 或 issue 交接。

## Comments

- 2026-07-14：当前会话已认领此 ticket，按 grilling 顺序确认 Apple 统一播放迁移的验证和文档门禁。
- 2026-07-14 resolution：确认验证门禁拆成两层：实现分支交付人工验收前的硬门禁，以及人工验收 / 发布风险说明。实现分支硬门禁只放 Agent 或实现线程应能稳定给出证据的项目；真机、人工听感、干净机安装等环境相关事项作为人工验收证据。签名、公证、Developer ID、staple 和 Gatekeeper 发布验收不作为本 ticket 的必过验收项，只作为后续发布阶段风险说明或 release checklist 事项，不能在 08 号门禁里要求实现线程证明已经发布级通过。

  实现分支硬门禁必须包含自动化验证：common 播放契约测试继续覆盖 `PlaybackCoordinator` 的队列、播放模式、自然结束、失败推进、快照和状态回流；Apple 平台 fake bridge / fake engine 测试覆盖命令串行、generation 过滤、`setQueue` 准备成功或失败收口、`skipToIndex` 后旧回调不污染当前媒体、release 后不发事件且不挂起 ack、进度事件只归因当前媒体、bridge 初始化失败映射为 `EngineUnavailable`。最低命令线包括 `./gradlew :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid`，并追加实现后实际存在的 iOS framework 编译任务，例如 `linkDebugFrameworkIosSimulatorArm64` 或等价任务；任务名不确定时必须先跑 `./gradlew :composeApp:tasks` 查证，不能猜。

  Android 编译纳入硬门禁。虽然本迁移目标是 Apple 播放链路，但实现会触碰 `AudioPlayerEngine`、`PlaybackCoordinator`、错误模型和 `PlayableMedia` 等 common 契约；Android Media3 是当前已有真实播放链路，Android 编译是必要回归哨兵。

  iOS 硬门禁只要求模拟器 framework 编译和 fake engine / bridge 契约测试。iOS 真机播放、后台继续播放、锁屏后音频继续、回前台后状态和进度同步属于人工验收门禁，不得伪装成 Agent 自动化已完成。若实现线程能访问 iOS Simulator 并启动宿主 App，可以把模拟器 smoke 作为补充证据，但不能替代真机后台验收。

  macOS 实现分支硬门禁必须包含真实本机播放 smoke、打包产物 bridge 加载检查和无 vlcj / LibVLC 依赖证明。至少要在开发机用 AVFoundation engine 播放一个本地 `file://` MP3 或 M4A；macOS app / DMG 或当前分发产物能包含并加载 native bridge；生产代码、Gradle 依赖、打包任务和运行时路径不再依赖 vlcj、LibVLC、VLC app、`VLC_PLUGIN_PATH` 或旧 `kmp.music.libvlc.runtime.dir`。签名 / 公证不属于本 ticket 验收项。

  格式支持矩阵进入硬门禁，但不要求所有旧扫描扩展名都播放成功。实现分支必须用真实样本、`AVURLAsset` 可播放性检查或真实播放 smoke 产出 Apple 平台格式支持矩阵，至少覆盖 `MP3`、`M4A/AAC`、`WAV`、`FLAC`、`AIFF/ALAC`，并明确 `OGG`、`OPUS`、`AMR` 是支持、不可支持还是待验证。交付标准是扫描入口、可播放判断、错误文案和文档不再宣称 AVFoundation 支持未验证格式；如果 AVFoundation 相比 LibVLC 能力收窄，应通过过滤、问题标记或 `UnsupportedFormat` 诚实表达，不能让用户扫描成功后随机失败且没有解释。

  错误文案和测试必须覆盖全部关键错误类型，而不是只删除 VLC 字眼。`EngineUnavailable` 应指向 Apple 播放组件或 native bridge 不可用，不能出现 `VLC`、`LibVLC`、`安装 VLC`、`VLC_PLUGIN_PATH`；`MissingFile` 应覆盖文件删除、移动或外置盘断开，提示重新扫描或恢复文件；`PermissionDenied` 应覆盖 iOS 沙盒导入失效、macOS 普通文件权限不足，提示授权、重新导入或重新选择来源，不能误报为格式不支持；`UnsupportedFormat` 应覆盖 AVFoundation 不支持、DRM 或受保护资源，提示格式暂不支持或资源不可播放；`Unknown` 保留通用失败，不把 native bridge / JNI 底层诊断直接暴露给普通用户。

  文档门禁必须新增 ADR，记录 macOS 从 vlcj / LibVLC 改为 Apple AVFoundation，且 iOS / macOS 统一到 Apple 原生播放方案。这是长期架构决策，不是普通库替换：目标平台能力、桌面分发边界、验证方式和非 macOS 桌面承诺都发生变化。旧文档处理采用保留并标记过时，不删除历史设计。`docs/superpowers/specs/2026-06-24-macos-vlcj-playback-design.md` 顶部必须加 `Superseded` 中文说明，明确它已被 Apple AVFoundation 迁移路线取代，不能作为当前实现依据。`docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md` 必须修正 “Desktop = vlcj” 和 “未来 Windows 优先复用 Desktop vlcj engine” 的有效路线表述，改为历史状态或待重新设计。`README.md` 和 `AGENTS.md` 只在仍暗示 Desktop / vlcj / LibVLC 或错误分发范围时更新，不做无关大修。实现 issue 交接必须包含实现摘要、验证命令、人工验收待办、格式矩阵、剩余风险。

  08 号决策关闭后，本 wayfinder 收束。地图应更新为可进入实现 issue 拆分；不再新增 grilling ticket，不在本轮直接实现播放器代码。

  对抗式审查结论：第一，如果不拆硬门禁和人工验收门禁，实现线程会被迫证明无法稳定自动化的 iOS 真机和干净机事实，最终诱发假验证。第二，如果 macOS 只靠 fake bridge 测试，会漏掉 native bridge 打包和加载这类最可能翻车的路径，因此本机播放 smoke 和打包加载检查必须进硬门禁。第三，如果格式矩阵不进门禁，从 LibVLC 切到 AVFoundation 后可能静默收窄格式能力，用户会看到“扫描成功但播放失败”的随机体验。第四，如果只检查 `EngineUnavailable` 是否删 VLC 字眼，会漏掉权限、缺文件、DRM、格式不支持等真正影响用户自救的路径。第五，如果不新增 ADR 且不标记旧文档过时，后续实现者很容易按旧 vlcj 路线继续扩展，和已确认的 Apple 原生路线冲突。
