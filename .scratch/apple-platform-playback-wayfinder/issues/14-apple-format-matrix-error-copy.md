# 14 — 固化 Apple 格式矩阵和播放错误自救文案

**What to build:** 让 Apple 平台真实可播放能力、扫描入口和错误文案一致。用户遇到缺文件、权限、格式不支持、受保护资源或播放器组件不可用时，能看到可行动的提示，而不是继承旧 LibVLC 假设。

**Blocked by:** 11 — 实现 iOS AVFoundation 播放会话 P0；13 — 切换桌面默认播放链路到 AVFoundation engine。

Status: ready-for-human

Labels: ready-for-human

- [x] 产出 Apple 平台格式支持矩阵，至少覆盖 MP3、M4A/AAC、WAV、FLAC、AIFF/ALAC、OGG、OPUS 和 AMR。
- [x] 格式矩阵基于真实样本、AVFoundation 可播放性检查或真实播放 smoke，不从 LibVLC 支持范围推断。
- [x] 扫描入口、可播放判断和文档不宣称 AVFoundation 支持未经验证的格式。
- [x] 缺文件、权限拒绝、不支持格式、受保护资源、播放器组件不可用和未知错误都有清晰用户文案。
- [x] `UnsupportedFormat`、`PermissionDenied` 和 `MissingFile` 不互相误报；用户能从文案判断是换文件、重新授权还是重新扫描。
- [x] 用户文案和测试不再出现 VLC、LibVLC、安装 VLC、VLC 插件路径或旧运行时路径提示。
- [x] 错误文案测试覆盖全部关键错误类型，不能只覆盖引擎不可用。

## 对抗式审查

- 最可能翻车点一：格式矩阵只查扩展名，没有用真实样本或 AVFoundation 可播放性验证。修正要求：每个结论必须说明证据来源，未知就标待验证。
- 最可能翻车点二：为了减少改动仍让扫描入口收下 AVFoundation 明确不支持的格式，播放时才失败。修正要求：扫描入口、可播放判断、错误文案和文档必须一致。
- 最可能翻车点三：权限、缺文件、DRM 和格式不支持都落到同一句“播放失败”。修正要求：错误文案测试必须覆盖用户自救路径，不只检查没有 VLC 字样。

## Comments

- 由 Apple 平台统一播放迁移 PRD 拆分而来。

- 2026-07-15 实现摘要：
  - 新增 `AppleAudioFormatSupportMatrix`，把 Apple P0 格式结论作为扫描入口、测试、文档和 smoke 输出的同一事实源。矩阵覆盖 `MP3`、`M4A/AAC`、`WAV`、`FLAC`、`AIFF/ALAC`、`OGG/OPUS`、`AMR`。
  - 桌面和 iOS 扫描入口现在只接收矩阵中允许扫描的 Apple P0 格式；`OGG`、`OGA`、`OPUS`、`AMR`、`AWB` 保留为待验证，不进入可播放曲库。
  - 新增 `docs/APPLE_PLATFORM_FORMAT_SUPPORT_MATRIX.md`，用中文记录每类格式的扫描入口结论、macOS 自动化证据、iOS 边界和待验证格式处理方式。
  - 扩展 `macosAvFoundationBridgeSmoke`，保留 M4A/AAC 真实播放事件回流 smoke，并新增格式矩阵检查：生成 MP3、M4A/AAC、WAV、FLAC、AIFF、ALAC-in-M4A 样本后用 AVFoundation `AVURLAsset.load(.isPlayable)` 检查；`OGG/OPUS` 与 `AMR` 输出待验证。
  - 更新播放错误文案：缺文件提示恢复文件后重新扫描，权限拒绝提示重新授权或重新导入，不支持格式和受保护资源提示换用已验证格式的无保护本地文件，播放器组件不可用提示 Apple 播放组件不可用，未知错误提示稍后重试、重新扫描或更换已验证格式。
  - 新增/扩展测试覆盖矩阵完整性、待验证格式拒收、桌面和 iOS 扫描入口不复制/不发布待验证格式、全部关键错误类型的自救文案，以及所有错误类型都不出现旧运行时提示。
- TDD 红灯证据：
  - 先新增 `LocalAudioFileRulesTest.appleFormatMatrixCoversRequiredFormatsWithEvidence` 后运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.LocalAudioFileRulesTest --tests com.yanhao.kmpmusic.feature.app.PlaybackErrorMessageTest`，因缺少 `AppleAudioFormatSupport`、`AppleAudioFormatSupportMatrix` 和 `AppleAudioFormatSupportStatus` 编译失败。
  - 扩展 smoke 后首次 `./gradlew :composeApp:macosAvFoundationBridgeSmoke` 暴露 MP3 样本不能由当前 `afconvert` 编码的问题；修正为本机编码器生成 MP3 样本、AVFoundation 执行可播放性检查。
  - 后续 smoke 又暴露短 MP3 样本不稳定产生 ended/progress 的问题；修正为格式矩阵使用 AVFoundation 可播放性检查，真实播放事件 smoke 继续由 M4A/AAC 样本覆盖。
- 验证命令与结果：
  - `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.LocalAudioFileRulesTest --tests com.yanhao.kmpmusic.feature.app.PlaybackErrorMessageTest`：通过。
  - `./gradlew :composeApp:iosSimulatorArm64Test --tests com.yanhao.kmpmusic.data.IosFolderMusicScannerTest`：通过。
  - `./gradlew :composeApp:macosAvFoundationBridgeSmoke`：通过；输出包含 `prepared`、`playing`、多次 `progress`、`ended`、`failed(type=MissingFile)`，并输出 MP3、M4A/AAC、WAV、FLAC、AIFF/ALAC 支持，OGG/OPUS 与 AMR 待验证。
  - `./gradlew :composeApp:desktopTest :composeApp:iosSimulatorArm64Test :composeApp:macosAvFoundationBridgeSmoke :composeApp:compileDebugKotlinAndroid`：通过，58 个 task，10 executed，48 up-to-date。
  - `git diff --check`：通过。
- 对抗式审查结论：
  - 翻车点一：格式矩阵只查扩展名。已用真实样本加 AVFoundation 可播放性检查输出 macOS 自动化证据；无法诚实自动化证明 iOS 真实样本播放的部分已在矩阵和文档中记录为 iOS 后续 gate / 真机验证边界。
  - 翻车点二：扫描入口收下待验证格式。已通过 `LocalAudioFileRulesTest`、`DesktopFolderMusicScannerTest` 和 `IosFolderMusicScannerTest` 证明 `OGG/OPUS/AMR` 不进入可播放曲库，也不会触发 iOS 沙盒复制。
  - 翻车点三：权限、缺文件、受保护资源和格式不支持都落到同一句。已按 `PlaybackErrorType` 分支提供不同自救动作，并用 `PlaybackErrorMessageTest` 覆盖全部关键错误类型。
  - 翻车点四：用户文案仍夹带旧运行时提示。共享错误文案与测试目标中不再出现旧运行时提示；测试对全部错误类型执行禁词检查。
  - 翻车点五：把 macOS 自动化证据偷换成 iOS 格式播放证据。已在矩阵代码、文档和本 Comments 明确 iOS 真实样本播放仍需真机或后续 gate 验证。
- code review 结论：
  - Standards 初审发现 `MacosAvFoundationFormatMatrixSmoke` 中 `sampleRate`、`bytes`、`format` 缺少显式类型，已补齐；同时将格式矩阵 helper 拆出，主 bridge smoke 保持 200 行以内，新增 helper 控制在 199 行。
  - Standards 初审指出格式名字符串作为样本映射 key 有潜在重复/原始字符串风险；当前矩阵测试覆盖格式名集合，样本映射只在 smoke helper 内部使用，保留为低风险，后续若矩阵继续扩展可把样本生成改为按扩展名或对象身份索引。
  - Spec 初审指出 iOS 复用矩阵但只有 macOS 证据，以及旧运行时禁词只测引擎不可用；已修正为矩阵和文档显式记录 iOS 后续 gate 边界，并让禁词测试覆盖全部 `PlaybackErrorType`。
- 剩余风险或未完成项：
  - 当前自动化只证明 macOS AVFoundation 格式样本检查和 iOS 扫描 allowlist；iOS 真机真实样本播放、后台播放下格式兼容和受保护资源实机行为仍需 17 号 gate 或人工验收记录。
  - `OGG/OPUS/AMR` 不是永久不支持结论，只是本票未取得可靠 Apple 平台样本证据，因此暂不进入扫描入口。
  - 15 号仍负责下线旧 vlcj / LibVLC 生产链路；本票未删除旧依赖、打包任务或旧测试资产。
