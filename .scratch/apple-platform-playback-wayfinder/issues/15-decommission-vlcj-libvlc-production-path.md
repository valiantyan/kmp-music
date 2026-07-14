# 15 — 下线 vlcj / LibVLC 生产链路

**What to build:** 在 AVFoundation 成为 macOS 默认真实播放链路后，删除活跃 vlcj / LibVLC 生产依赖、运行时打包和旧实现细节。交付后桌面真实播放只承诺 macOS AVFoundation，不再保留 VLC fallback 或双引擎 runtime gate。

**Blocked by:** 13 — 切换桌面默认播放链路到 AVFoundation engine。

Status: ready-for-human

Labels: ready-for-agent

- [x] 生产依赖、运行时路径、打包任务和开发运行配置不再依赖 vlcj 或 LibVLC。
- [x] 旧 vlcj engine、runtime resolver、adapter 和 LibVLC 细节测试被删除或替换为 AVFoundation 行为测试。
- [x] 旧 LibVLC 下载、提取、验收脚本和运行时资源从生产树移除；历史参考交给 git 历史。
- [x] 不保留 vlcj fallback、双引擎 runtime gate 或“切回 VLC”完成方案。
- [x] 只删除 vlcj / LibVLC 细节测试，必须保留或迁移命令串行、generation、释放和失败归一化等行为防线。
- [x] 无 vlcj / LibVLC 生产引用证明可复查，覆盖生产代码、依赖、打包任务、运行时参数和用户提示。
- [x] 非 macOS Desktop 真实播放不被误标为本轮已支持；分发范围或风险说明同步更新。

## 对抗式审查

- 最可能翻车点一：删除旧实现时连行为测试一起删掉，导致新 engine 缺少回归防线。修正要求：只删除库细节，保留或迁移平台无关行为场景。
- 最可能翻车点二：仍残留运行时参数、打包任务或用户提示，后续以为 VLC 仍是支持路径。修正要求：无引用证明必须覆盖依赖、任务、运行参数、生产代码和用户文案。
- 最可能翻车点三：删除唯一跨平台桌面播放器后仍产出 Windows / Linux 可播放暗示。修正要求：分发范围和风险说明必须同步收窄或标明未支持。

## Comments

- 由 Apple 平台统一播放迁移 PRD 拆分而来。
- 2026-07-15 实现摘要：删除 `libs.vlcj` 依赖、旧 macOS LibVLC 下载/提取/准备/打包/验收任务、开发运行时参数注入和 `composeApp/src/desktopMain/packaging/macos-libvlc/` 生产脚本资源。删除旧 `DesktopVlcjAudioPlayerEngine`、`MacosLibVlcRuntime`、`VlcjMediaPlayerAdapter`、`DesktopMediaPlayerAdapter` 旧适配边界及其 LibVLC / vlcj 细节测试。桌面 native distribution 收窄为 macOS `Dmg`，不再生成 Windows / Linux 桌面真实播放分发暗示。新增 `VlcjDecommissionGateTest`，扫描生产代码、Gradle 依赖、打包脚本、运行参数、Markdown / shell / Kotlin / native 文本文件，证明生产树不再出现旧播放路径关键词。
- 2026-07-15 TDD 证据：先新增 `VlcjDecommissionGateTest.productionTreeHasNoVlcjRuntimeReferences`，首次运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.VlcjDecommissionGateTest` 失败，暴露 `libs.vlcj`、LibVLC 打包任务、运行参数、旧 engine/runtime/adapter 和脚本资源残留；删除旧生产路径后同一测试转绿。Spec code review 指出门禁漏扫 `.mm` native 文件，已补充 `c/cc/cpp/h/hpp/m/mm` 扫描扩展名并再次验证通过。
- 2026-07-15 验证命令与结果：`./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.VlcjDecommissionGateTest` 通过。`./gradlew :composeApp:desktopTest :composeApp:macosAvFoundationBridgeSmoke :composeApp:macosAvFoundationDefaultRuntimeSmoke :composeApp:compileDebugKotlinAndroid` 通过；bridge smoke 输出包含 `prepared`、`playing`、多次 `progress`、`ended`、`failed(type=MissingFile)` 和格式矩阵，默认 runtime smoke 输出包含 `current-media`、`playing`、`progress`、`seek`、`paused`、`resume`、`next`、`previous`、`stop`。`git diff --check` 通过。无旧生产引用证明命令：`rg -n "vlcj|LibVLC|libvlc|VLC_PLUGIN_PATH|kmp\\.music\\.libvlc|macos-libvlc|TargetFormat\\.(Msi|Deb)|downloadMacosArm64LibVlc|extractMacosArm64LibVlc|prepareMacosArm64LibVlc|stageMacosArm64LibVlc" composeApp/build.gradle.kts gradle/libs.versions.toml composeApp/src/commonMain composeApp/src/desktopMain` 无命中。
- 2026-07-15 对抗式审查结论：一，删除旧实现时不能丢行为防线；已确认 `DesktopAppleAudioPlayerEngineTest`、`DesktopAudioRuntimeFactoryTest` 和 `MacosAvFoundationPlaybackBridgeTest` 保留命令串行、generation 过滤、seek/skip 竞态、release 后回调丢弃、ack 失败/超时、初始化失败和 stop/ended 行为覆盖。二，不能残留旧运行参数、打包任务或用户提示；已通过新增门禁测试和生产树 `rg` 证明依赖、任务、运行参数、脚本、Kotlin/native 生产文件无旧关键词。三，不能继续暗示 Windows / Linux Desktop 真实播放；已将 Compose Desktop native distributions 收窄为 macOS `Dmg`。四，不得保留 vlcj fallback 或双引擎 gate；默认桌面运行时仍只装配 `DesktopAppleAudioPlayerEngine + MacosAvFoundationPlaybackBridge`，bridge 不可用映射为统一失败。五，本票只执行 15 号下线范围，未修改 01-08 决策票，也未实现 16/17 文档和交接门禁。
- 2026-07-15 code review 结论：Standards 审查无发现，认为 staged diff 未违反 `AGENTS.md` 或 Kotlin 规范。Spec 审查初次发现无引用门禁漏扫 native `.mm` 生产文件，已修复并重跑精准测试与完整验证；复查自审未发现剩余 Spec 缺口。
- 2026-07-15 剩余风险或未完成项：签名、公证、发布级 Gatekeeper 和实际打包产物 bridge 加载检查仍属于后续 17 号 gate / 人工验收范围；16 号仍负责新增 ADR、旧 vlcj 文档过时标记和播放抽象审计修正。本票未删除历史 docs、PRD、issue 或研究记录中的旧路线描述，因为它们不是生产链路且由后续文档票处理。
