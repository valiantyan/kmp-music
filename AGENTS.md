# KMP Music Agent 指南

本文件是仓库级入口地图，不是项目百科。它只回答四件事：先读什么、改哪里、跑什么验证、哪些边界不能越过。若子目录有自己的 `AGENTS.md`，进入该目录前先读；用户最新指令优先，就近 `AGENTS.md` 次之，本文件兜底。

## 先做判断

- 分析问题时先做第一性原理拆解：明确真实目标、事实依据、硬约束和最小可验证路径，避免只按惯例或表象修补。
- 动手前用 3 到 5 行梳理：任务目标、最小改动点、验证方式；需求不清或会扩大产品、架构范围时先问用户。
- 只改与任务目标直接相关的文件；不要顺手重构、批量格式化或修正无关问题。
- 修缺陷追求根因修复；如果根因修复会明显扩大范围，先说明取舍。
- 查找文件和文本优先用 `rg` 或 `rg --files`。
- 不要回滚、覆盖或整理用户已有未提交改动；遇到相关改动，先读懂再协同处理。
- 生成或更新 Markdown 时，正文描述必须使用中文；命令、路径、代码标识符和 URL 可以保留原文。

## 任务路由

| 任务类型 | 先读 | 重点边界 |
| --- | --- | --- |
| 产品范围、信息架构、MVP 取舍 | `docs/PRD.md`、`CONTEXT.md` | 不发明与领域词汇冲突的新概念。 |
| 代码定位、模块归属、技术栈版本 | `docs/agents/project-map.md`、`gradle/libs.versions.toml`、`composeApp/build.gradle.kts` | 只在需要找入口、源码集或测试位置时读；版本事实以 Gradle 为准。 |
| 领域命名、本地音频发现、扫描来源 | `CONTEXT.md`、`docs/agents/domain.md`、相关 `docs/adr/` | 以来源文件仍存在且可访问为准，不设计复制音频后的永久保活。 |
| 架构、Repository、UseCase、数据、播放、平台能力 | `docs/agents/kmp-architecture.md`、相关源码和测试 | `commonMain` 不引入平台 API；跨层依赖走接口或 `expect/actual`。 |
| 移动端导航、全局 chrome、迷你播放器、播放页、桌面页面或播放器、视觉还原 | `docs/agents/ui-state.md`、相关显示模型和测试 | 生产 App 必须是 Compose 原生 UI；显式要求的 1:1、截图和动画证据属于交付硬门禁。 |
| 本地 issue、PRD 或 GitHub 缺陷 | `docs/agents/issue-tracker.md`、`docs/agents/github-bug-flow.md`、对应 `.scratch/<feature-slug>/` | 本地 Markdown issue 是执行和审计主记录。 |
| 测试范围不确定 | `docs/agents/testing.md`、`./gradlew :composeApp:tasks` | 不猜任务名，不声称未运行的验证通过。 |
| Harness 维护、重复错误沉淀、验证入口失效 | `docs/agents/harness.md`、`docs/agents/testing.md`、`scripts/verify-local.sh` | 先找最小权威 owner；能升级成测试、脚本、lint 或类型约束的，不只写成口头规则。 |

## Agent skills

`.agents/skills` 使用 mattpocock/skills 风格的仓库级配置；本节保留发现锚点，细节按链接加载。

### Issue tracker

本项目 issue 和 PRD 使用本地 Markdown。见 `docs/agents/issue-tracker.md`；GitHub 缺陷流程见 `docs/agents/github-bug-flow.md`。

### Triage labels

本仓库使用默认五角色分诊标签。见 `docs/agents/triage-labels.md`。

### Domain docs

本仓库使用 single-context 领域文档布局。见 `docs/agents/domain.md`。

## 子代理交付模式

- 除非用户明确要求主代理亲自处理，涉及代码或文档探索、分析、工具调用、修改、测试、验证或审查的任务，均由交付子代理端到端完成。
- 主代理仅可创建交付子代理、转发用户新增指令、等待结果，并原样转发交付子代理不超过 500 个字符的结论。结论缺少“改了什么 / 验证了什么 / 剩余风险”任一项时，主代理只能退回同一交付子代理重新生成并继续等待，不得自行检查、补写或推断。除这些协调动作外，主代理不得调用任何工具：不得读取或分析仓库、检查工作树、调用终端、文件、网络或 MCP 工具、编辑文件、运行测试，或执行代码与文档审查。
- 交付子代理负责任务的全部实际工作：探索、分析、方案、工具调用、代码或文档产物、验证、证据整理和结论。首次修改前运行 `python3 scripts/agent_delivery.py snapshot` 保存基线；交付时用同一快照执行 `render`，并把其三行标准输出原样作为最终结论。“改了什么”中的链接必须指向脚本自动生成的临时 Markdown Manifest，其中包含全部文件清单和任务级 Diff。它可在内部派发专项子代理；主代理不参与中间判断或产物处理。
- 交付前，交付子代理必须派发独立只读审查子代理审查任务产物、实际 diff、验证证据和 `render` 生成的最终结论，处理发现后取得复核。用户明确要求“子代理实现”时，产品代码、测试、脚本和任务文档的实际变更必须由交付子代理体系完成。完整职责契约、命令参数与失效标准见 `docs/agents/harness.md`。

## 总边界

- KMP Music 是本地音乐优先的跨平台音乐播放器；首要闭环是本地音频发现、曲库浏览、播放、队列、收藏、最近播放、本地自建歌单和设置。
- MVP 不做复杂社区、直播、会员支付、广告、在线歌曲下载；登录和云同步只作为入口或后续能力预留，不能破坏游客模式可用性。
- 真实 KMP App 优先；原型视觉问题才读 `prototypes/kmp-music-hi-fi/AGENTS.md`，不要用 WebView 或原型产物替代生产 Compose UI。
- 保持 `core / domain / data / feature / 平台源码集` 分层；扫描、播放、通知、权限、文件系统、AVFoundation、Media3 等平台能力不能污染 `domain` 或 UI 层。
- 新增数据或平台能力时，先定义 Repository、UseCase、平台数据源契约或 `expect/actual` 边界，再写 `Impl` 实现。
- 不为一次性调用过早抽象；只有抽象能隔离平台差异、降低真实复杂度或复用既有模式时才新增。

## 常用验证

- 常规代码默认验收：`./scripts/verify-local.sh`
- 不确定任务是否存在：`./gradlew :composeApp:tasks`
- Android 编译：`./gradlew :composeApp:compileDebugKotlinAndroid`
- 共享逻辑和 Android 编译：`./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`
- 桌面端测试：`./gradlew :composeApp:desktopTest`
- Desktop UI 取证：`./scripts/desktop-ui-qa.sh <home|home-playing|albums|artists>`
- macOS AVFoundation 冒烟验证：`./gradlew :composeApp:macosAvFoundationBridgeSmoke`、`./gradlew :composeApp:macosAvFoundationDefaultRuntimeSmoke`

更多命令和测试选择细则见 `docs/agents/testing.md`；提交前至少运行与改动范围匹配的验证。常规代码改动后，AI 必须主动运行 `./scripts/verify-local.sh`；如果因环境限制、任务范围或更精确的 focused 验证没有运行该脚本，交付说明必须写清原因、已运行的等价命令和人工验收缺口。

## 交付门禁

- 提交前查看 `git status --short --branch`，避免提交 `.scratch/` 临时产物、构建产物、IDE 状态、日志、Node 依赖、原型 dist、APK/DMG、本地缓存和原始附件。
- 最终回复只说与任务目标相关的内容：改了什么、验证了什么、剩余风险是什么。
- 如果验证未运行、失败或受环境限制，明确说明，不要把推测写成已通过。
- 用户显式要求的 1:1、截图、录屏、动画、真机或指定状态验收必须取得对应证据；缺少时任务仍未完成，不能降级为“剩余风险”后交付或提交。
- 缺陷修复提交信息必须用中文写明问题原因和解决方案。
- 交付前做对抗式审查：基于当前可见上下文、实际 diff 和必要文件列出最可能翻车的风险；上下文不足时先补查或标明未覆盖，不凭记忆审查。
- 如果同类错误第二次出现，把教训沉淀到最早能阻止它的位置：优先测试、脚本、类型或接口约束，其次更新 `docs/agents/*` 或对应 `.scratch/<feature-slug>/` 记录。

## 禁止事项

- 不要把真实媒体扫描或播放逻辑直接塞进 UI 层。
- 不要在 `commonMain` 引入 Android、iOS、Desktop 专属 API。
- 不要硬编码 secrets、token、私有路径或本机账号信息。
- 不要删除失败测试来“修复”构建，除非用户明确要求移除该行为。
- 不要把 README、AGENTS 或交接记录写成与 Gradle、ADR、源码不一致的旧事实。
- 不要把 Desktop 真实播放泛化为 Windows / Linux 已支持；当前生产真实播放只面向 macOS AVFoundation。
