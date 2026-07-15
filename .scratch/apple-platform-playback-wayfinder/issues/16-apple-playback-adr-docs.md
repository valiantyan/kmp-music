# 16 — 更新 ADR 和旧播放路线文档

**What to build:** 把 Apple AVFoundation 播放路线固化为长期架构决策，并让旧 vlcj / LibVLC 文档明确过时。后续实现者不会再把旧 Desktop = vlcj 路线当成当前目标。

**Blocked by:** 14 — 固化 Apple 格式矩阵和播放错误自救文案；15 — 下线 vlcj / LibVLC 生产链路。

Status: ready-for-human

Labels: ready-for-agent

- [x] 新增 ADR，记录 macOS 从 vlcj / LibVLC 改为 Apple AVFoundation，且 iOS / macOS 统一到 Apple 原生播放方案。
- [x] ADR 说明平台能力范围、桌面分发边界、验证方式、格式矩阵和非 macOS Desktop 承诺。
- [x] 旧 macOS vlcj 设计文档顶部标记 `Superseded`，并用中文说明不能作为当前实现依据。
- [x] 旧播放抽象审计中 Desktop = vlcj 和未来复用 Desktop vlcj engine 的表述改为历史状态或待重新设计。
- [x] README 和项目指南只在仍暗示 Desktop / vlcj / LibVLC 或错误分发范围时更新，不做无关大修。
- [x] 文档更新与当前实现状态一致，不提前宣称未通过人工验收的系统能力。
- [x] 所有新增或修改的 Markdown 描述内容使用中文书写。

## 对抗式审查

- 最可能翻车点一：只新增 ADR，不标记旧文档过时，后续实现者仍按 vlcj 方案扩展。修正要求：旧设计必须明确 `Superseded`，且说明不能作为当前实现依据。
- 最可能翻车点二：文档把 P0 写成完整系统媒体体验，误导用户以为 Now Playing、媒体键和远程命令已完成。修正要求：文档只能声明已实现和已验收的能力，后续范围必须明确。
- 最可能翻车点三：顺手大改 README 或项目指南，制造无关 diff。修正要求：只修正仍暗示 vlcj / LibVLC 或错误分发范围的内容。

## Comments

- 由 Apple 平台统一播放迁移 PRD 拆分而来。

- 2026-07-15 实现摘要：
  - 新增 `docs/adr/0005-apple-platform-avfoundation-playback.md`，固化 Apple 平台统一 AVFoundation 路线，记录 macOS 从 `vlcj / LibVLC` 改为 Apple `AVFoundation`，iOS 与 macOS 共享 common 播放契约和 Apple 原生播放方向。
  - ADR 明确平台能力范围、桌面分发边界、非 macOS Desktop 不承诺真实播放、格式矩阵依据、自动化验证方式和仍需人工验收的 iOS 后台 / 锁屏、签名公证与发布级风险。
  - 旧 `docs/superpowers/specs/2026-06-24-macos-vlcj-playback-design.md` 顶部标记 `Superseded`（已被取代），并说明不能作为当前实现依据。
  - 更新 `docs/superpowers/specs/2026-06-30-playback-abstraction-audit-design.md`，把 Desktop = vlcj、未来复用 Desktop vlcj engine、iOS 仍只是未来 adapter 等表述修正为历史状态或当前 AVFoundation / 待重新设计边界。
  - README 只修正播放能力概述：说明 Android 与 macOS 已有真实播放链路，iOS 是 App 内 AVFoundation 播放会话基础适配；iOS 真机后台 / 锁屏、Now Playing、远程命令、发布级验收和 Windows / Linux Desktop 真实播放仍在后续阶段。
  - 新增 `ApplePlaybackDocumentationGateTest`，作为文档门禁覆盖 ADR、旧 vlcj 设计过时标记、播放抽象审计历史化和 README 不再笼统宣称真实播放全未完成。
- TDD 红灯证据：
  - 先新增 `ApplePlaybackDocumentationGateTest` 后运行 `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.ApplePlaybackDocumentationGateTest`，4 个测试失败：缺少 ADR、旧 vlcj 设计未标记 `Superseded`、播放抽象审计仍保留旧 vlcj 假设、README 仍把真实播放笼统写成后续能力。
  - 补齐文档后同一精准测试通过；Standards 审查发现英文元数据和标题问题后，改为中文 `状态：已接受`、`日期：2026-07-15` 和“状态：`Superseded`（已被取代）”，并再次验证通过。
- 验证命令与结果：
  - `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.playback.ApplePlaybackDocumentationGateTest`：通过。
  - `./gradlew :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid`：通过，38 个 task，4 executed，34 up-to-date。
  - `git diff --check`：通过。
- 对抗式审查结论：
  - 翻车点一：只新增 ADR，不标记旧文档过时。已在旧 macOS vlcj 设计顶部标记 `Superseded` 并写明不能作为当前实现依据，文档门禁测试覆盖该事实。
  - 翻车点二：把 P0 写成完整系统媒体体验。ADR 与 README 均明确 iOS 后台 / 锁屏、Now Playing、远程命令、签名公证和发布级验收仍是人工验收或后续范围；README 的 iOS 表述已收窄为 App 内播放会话基础适配。
  - 翻车点三：顺手大改 README 或项目指南。未修改 `AGENTS.md`；README 只改过时播放能力概述和后续范围，没有做无关结构大修。
  - 翻车点四：旧播放抽象审计仍引导未来 Windows 复用 Desktop vlcj engine。已改为 Windows / Linux Desktop 真实播放需要重新设计，不能复活旧 vlcj engine 或 LibVLC runtime resolver。
  - 翻车点五：新增 Markdown 出现英文描述内容。Standards 审查发现后已修正 ADR 元数据和旧文档状态表达；保留的 `Superseded` 是 Ticket 明确要求的字面状态标记，并配套中文说明。
- code review 结论：
  - Standards 审查初次发现 ADR 的 `Status` / `Date` 和旧文档标题 `Superseded` 违反 Markdown 描述内容中文规则，已修复；同时指出测试重复读文件的判断性气味，已收敛为 `readProjectFile(relativePath)` helper。Shotgun Surgery 属于本票要求同时更新 ADR、旧文档、审计和 README 的固有文档门禁，保留为低风险。
  - Spec 审查初次指出 Ticket 尚未更新和 README “iOS AVFoundation P0 播放适配”有提前宣称风险；Ticket 已在本 Comments 中补齐交接记录，README 已改为 “iOS App 内 AVFoundation 播放会话基础适配”，并明确后台 / 锁屏等仍待后续验收。
- 剩余风险或未完成项：
  - 本票只完成 ADR 与旧路线文档固化，不执行 17 号人工验收交接；iOS 真机后台 / 锁屏、iOS 真实样本播放、打包产物 bridge 加载、签名、公证和 Gatekeeper 仍需 17 号或人工验收记录。
  - 历史实现计划文档中仍可能出现旧 vlcj 过程记录；本票只修正 Ticket 指定的旧 macOS vlcj 设计文档、播放抽象审计和 README 当前能力描述，不修改 01-08 wayfinder 决策票或研究归档。
