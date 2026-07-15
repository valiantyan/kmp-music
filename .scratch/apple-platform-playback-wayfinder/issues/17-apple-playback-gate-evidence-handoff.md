# 17 — 汇总硬门禁证据和人工验收交接

**What to build:** 把 Apple 播放迁移的自动化、真实播放、打包、格式、文档和人工验收证据汇总到实现交接中，让维护者可以判断是否进入人工验收和后续合入。

**Blocked by:** 11 — 实现 iOS AVFoundation 播放会话 P0；14 — 固化 Apple 格式矩阵和播放错误自救文案；15 — 下线 vlcj / LibVLC 生产链路；16 — 更新 ADR 和旧播放路线文档。

Status: ready-for-human

Labels: ready-for-human

- [x] 记录桌面测试、Android 编译和实际 iOS framework 编译任务的命令与结果；iOS 任务名来自任务列表查证。
- [x] 记录 macOS 本机真实播放 smoke 和打包产物 bridge 加载检查结果。
- [x] 记录无 vlcj / LibVLC 生产引用证明，覆盖生产代码、依赖、打包任务、运行时参数和用户提示。
- [x] 记录 Apple 格式支持矩阵、错误文案测试和文档门禁完成情况。
- [x] 明确人工验收待办：iOS 真机播放、后台继续播放、锁屏后音频继续、回前台状态同步和必要听感检查。
- [x] 明确剩余风险：签名、公证、干净机安装、Mac App Store sandbox、Now Playing、远程命令、媒体键和非 macOS Desktop 真实播放。
- [x] issue 交接包含实现摘要、验证命令与结果、对抗式审查、code review 结论、剩余风险或未完成项。
- [x] 不把未执行、失败、环境缺失或人工待验的项目写成通过；每条证据必须能对应到命令、截图、日志摘要或明确的人工待办。

## 对抗式审查

- 最可能翻车点一：把人工验收项目写成自动化已完成，尤其是 iOS 真机后台和锁屏播放。修正要求：人工验收和自动化证据必须分开记录。
- 最可能翻车点二：只贴命令名，不贴结果、失败原因或环境限制。修正要求：每条门禁都要有结果摘要，不能用“应当通过”替代证据。
- 最可能翻车点三：交接票顺手补实现或修改源码，破坏证据汇总边界。修正要求：本票只汇总门禁和风险，发现缺口应回到对应实现票修复。

## Comments

- 由 Apple 平台统一播放迁移 PRD 拆分而来。

- 2026-07-15 实现摘要：
  - 本票没有修改播放器生产逻辑，也没有实现 09-16 的剩余功能；只完成 Apple 播放迁移最终交接证据汇总。
  - 已读取并核验前置票：11、14、15、16 均为 `ready-for-human`，验收项已勾选，Comments 中包含实现摘要、验证命令、对抗式审查、code review 结论和剩余风险。
  - 新增 `ApplePlaybackGateEvidenceHandoffTest`，把 17 号本地 issue 作为审计产物门禁，检查 `ready-for-human` 状态、全部验收项勾选、硬门禁证据、人工验收待办、剩余风险、对抗式审查和 code review 记录。
  - 交接结论：自动化与本机 smoke 证据可复查；打包产物 bridge 加载检查未通过，不能进入“打包加载已验证”的结论。本票 `ready-for-human` 只表示交接证据已经完整记录并可人工复核，不代表整批可进入人工验收。维护者若要求全部硬门禁通过，应先回到对应实现票补齐打包 bridge 内置或加载策略，再进入人工验收。
- TDD 红灯证据：
  - 先新增 `ApplePlaybackGateEvidenceHandoffTest` 后运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.ApplePlaybackGateEvidenceHandoffTest`，3 个测试失败，暴露 17 号 issue 仍是 `ready-for-agent`、验收项未勾选、缺少硬门禁证据和 code review / 剩余风险交接记录。
- 验证命令与结果：
  - `./gradlew :composeApp:tasks --all`：通过；实际 iOS framework 编译任务名为 `linkDebugFrameworkIosSimulatorArm64`。
  - `./gradlew :composeApp:packageDmg`：通过；生成 `composeApp/build/compose/binaries/main/dmg/KMP Music-1.0.0.dmg`。
  - `find composeApp/build/compose/binaries/main/app -name 'libkmp_music_macos_avfoundation_bridge.dylib' -print`：无输出；当前打包生成的 `.app` 目录未包含 macOS AVFoundation bridge dylib，因此打包产物 bridge 加载检查未通过，不能写成已通过。
  - `find composeApp/build/macos-avfoundation-bridge/native -maxdepth 1 -type f -print -exec ls -l {} \;`：通过；独立编译产物存在于 `composeApp/build/macos-avfoundation-bridge/native/libkmp_music_macos_avfoundation_bridge.dylib`，smoke 通过显式路径加载该产物。
  - `rg -n "vlcj|LibVLC|libvlc|VLC_PLUGIN_PATH|kmp\\.music\\.libvlc|macos-libvlc|TargetFormat\\.(Msi|Deb)|downloadMacosArm64LibVlc|extractMacosArm64LibVlc|prepareMacosArm64LibVlc|stageMacosArm64LibVlc" composeApp/build.gradle.kts gradle/libs.versions.toml composeApp/src/commonMain composeApp/src/desktopMain`：无输出，表示生产代码、依赖、打包任务、运行时参数和用户提示范围内未发现旧 vlcj / LibVLC 生产引用。
  - `./gradlew :composeApp:desktopTest :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:macosAvFoundationBridgeSmoke :composeApp:macosAvFoundationDefaultRuntimeSmoke :composeApp:compileDebugKotlinAndroid`：通过，46 个 task，7 executed，39 up-to-date；bridge smoke 输出 `prepared`、`playing`、多次 `progress`、`ended`、格式矩阵和 `failed(type=MissingFile)`；默认 runtime smoke 输出 `current-media`、`playing`、`progress`、`seek`、`paused`、`resume`、`next`、`previous`、`stop`。
  - `git diff --check`：通过。
- macOS 本机真实播放 smoke 证据：
  - `macosAvFoundationBridgeSmoke` 使用本机生成的 M4A/AAC 样本和真实 AVFoundation bridge，输出并检查 `prepared`、`playing`、`progress`、`ended` 和 `failed(type=MissingFile)`。
  - `macosAvFoundationDefaultRuntimeSmoke` 通过默认桌面运行时和 `MusicAppController` 验证 `current-media`、`playing`、`progress`、`seek`、`paused`、`resume`、`next`、`previous` 和 `stop`。
- Apple 格式支持矩阵、错误文案测试和文档门禁：
  - 格式矩阵已记录在 `docs/APPLE_PLATFORM_FORMAT_SUPPORT_MATRIX.md`：`MP3`、`M4A/AAC`、`WAV`、`FLAC`、`AIFF/ALAC` 为 Apple P0 支持；`OGG/OPUS` 与 `AMR` 为待验证，不进入扫描入口。
  - 14 号票已记录错误文案测试覆盖缺文件、权限拒绝、不支持格式、受保护资源、播放器组件不可用和未知错误，并对全部关键错误类型执行旧运行时禁词检查。
  - 16 号票已记录文档门禁：新增 Apple AVFoundation ADR、旧 macOS vlcj 设计标记 `Superseded`、播放抽象审计修正旧假设、README 不提前宣称未验收能力。
- 人工验收待办：
  - iOS 真机播放：使用真实宿主工程和本地样本确认 App 内播放控制稳定。
  - 后台继续播放：宿主工程确认 `UIBackgroundModes = audio`，并在真机上验证切后台后继续播放。
  - 锁屏后音频继续：真机锁屏后确认音频持续、无 UI 生命周期释放导致的中断。
  - 回前台状态同步：从后台或锁屏返回后确认当前歌曲、播放状态和进度与真实播放一致。
  - 必要听感检查：覆盖播放、暂停、seek、上一首、下一首、自然结束推进、坏文件失败跳过和常见格式样本。
- 对抗式审查结论：
  - 翻车点一：把人工验收项目写成自动化已完成。已把 iOS 真机播放、后台继续播放、锁屏后继续、回前台状态同步和听感检查全部放入人工验收待办，没有写成自动化通过。
  - 翻车点二：只贴命令名不贴结果。每条自动化或命令检查均写明通过、无输出、未通过或人工待验；打包产物 bridge 检查明确记录为未通过。
  - 翻车点三：交接票顺手补实现或修改源码。当前只新增交接门禁测试并更新本 issue，没有修改播放器生产链路、09-16 票或 01-08 决策票；打包缺口按规则记录为风险，不在本票扩面修复。
  - 翻车点四：把 macOS 自动化格式证据偷换成 iOS 真机播放证据。矩阵和本交接均明确 iOS 真实样本播放仍需真机或后续 gate 验证。
  - 翻车点五：把无 vlcj 搜索限缩到 Kotlin 文件导致漏扫构建或 native。复查命令覆盖 `composeApp/build.gradle.kts`、`gradle/libs.versions.toml`、`commonMain` 和 `desktopMain`，15 号门禁测试也覆盖 Kotlin、Markdown、shell 和 native 文本扩展名。
- code review 结论：
  - Standards 审查发现 1 个问题：本票已勾选 code review 结论，但 Comments 仍写“将在本轮最终 diff 完成后执行”。已修复为当前实际审查结论。Kotlin 文件显式类型、中文注释、命名和文件长度未发现违规；Markdown 描述主体为中文，英文仅用于模板字段、命令、状态值和技术名词。
  - Standards 审查指出 `ApplePlaybackGateEvidenceHandoffTest` 使用较多硬编码片段，有轻微 `Primitive Obsession` 判断性气味；作为交接证据快照测试可接受，暂不抽象，避免把一次性审计文本过度工程化。
  - Spec 审查发现 1 个阻塞事实：打包产物 bridge 检查被诚实记录为失败，`.app` 未包含 `libkmp_music_macos_avfoundation_bridge.dylib`，因此本票不能作为整批进入人工验收的通过证据。已在实现摘要和剩余风险中明确区分“交接票可复核完成”和“整批硬门禁仍未通过”。
  - Spec 审查还指出新增测试只检查 issue 文本包含关键片段，不能替代真实命令输出、打包产物或前置票真实性验证。该测试仅作为交接记录门禁；真实证据仍以上方命令、产物检查、前置票和 smoke 输出为准。
- 剩余风险或未完成项：
  - 打包产物 bridge 加载检查未通过：`packageDmg` 产物存在，但 `.app` 目录没有 `libkmp_music_macos_avfoundation_bridge.dylib`。这属于实现链路缺口，不应由 17 号交接票顺手修复。
  - 签名、公证、干净机安装、Mac App Store sandbox、Gatekeeper 发布级验收均未执行，仍是发布前风险。
  - Now Playing、远程命令、媒体键、锁屏/控制中心按钮、耳机线控、AirPlay 专项验收和冷启动续播不在本轮 P0 自动化完成范围。
  - 非 macOS Desktop 真实播放不承诺支持；Windows / Linux Desktop 若后续继续分发，需要重新设计真实播放方案，不能复活旧 vlcj / LibVLC 链路。
