# Harness 维护指南

本文只记录让后续 AI 更可靠协作的环境约束。它不是新项目百科；若与 `README.md`、`CONTEXT.md`、`docs/PRD.md`、ADR、Gradle 或源码冲突，以那些权威来源为准，并修正本文或删除失效规则。

## 组件与失效标准

| 组件 | 解决的 AI 失败问题 | 可证伪标准 |
| --- | --- | --- |
| `AGENTS.md` | 进项目后不知道先读哪里、改哪里、跑什么。 | 后续 AI 在动手前仍读错入口、改错层、或遗漏必要验证，说明路由需要收窄或改写。 |
| `docs/agents/project-map.md` | 找不到真实源码集、测试目录和主模块。 | 后续 AI 发明不存在的模块、包名、源码集或 Gradle 任务，说明地图过期。 |
| `docs/agents/kmp-architecture.md` | 不理解 `core / domain / data / feature / platform source sets` 边界。 | 平台 API 泄漏进 `commonMain`，或业务逻辑被塞进页面级 Composable，说明规则需要升级为测试、接口或更明确示例。 |
| `docs/agents/ui-state.md` | UI 测试通过后直接推断 1:1 视觉、状态切换或动画正确。 | 后续 AI 没有建立需求状态矩阵、没有确认截图来自本次构建、缺少显式要求的静态或动态证据却仍交付，说明 UI 验收门禁失效。 |
| `docs/agents/testing.md` | 不知道改完该跑什么，或只跑代理测试却没有证明用户行为。 | 后续 AI 做了常规代码改动却没有主动运行 `./scripts/verify-local.sh`、没有跑匹配命令、没有说明环境限制，或用户验收路径未覆盖，说明验证矩阵需要补任务级入口。 |
| `scripts/verify-local.sh` | 缺少一个可执行的默认本地验收入口。 | 脚本在干净工作区不可运行、运行不存在任务，或默认与 focused 模式不能覆盖共享逻辑、Android 编译、Android JVM 回归和 Android lint 风险，说明脚本必须修正。 |
| `scripts/desktop-ui-qa.sh` 和 `:composeApp:desktopUiQa` | Desktop UI 取证依赖本机数据库、侧栏坐标、窗口名猜测和手工截图。 | 场景不能直接进入目标页、窗口不是 `1240×824`、三帧缺失或相同、外部窗口遮挡仍通过、进程不自动退出，说明 QA 入口或 artifact identity 校验失效。 |
| `.scratch/<feature-slug>/` 和 `.scratch/github-bugs/issues/` | 经验只留在对话里，缺陷修复缺少审计证据。 | 修复完成后没有复现信息、根因、验证结果、对抗式审查或剩余风险记录，说明对应工作流需要补门禁。 |
| 子代理交付模式 | 用户要求由子代理承担工作时，主代理自行探索、分析、调用工具、实现、验证或审查，造成角色承诺与实际轨迹不一致。 | 主代理越过协调接口从事实际工作、交付子代理体系没有承担被委派的工作，或审查发现未回流复核，说明委派契约失效。 |
| `scripts/agent_delivery.py` | 子代理实际修改了文件但遗漏改动，或完成旧任务后继续接受新目标。 | 交付结论缺少完整 Manifest、同一 agent 绑定多个 task、COMPLETED 后仍能返工/接管/review/render，或 reviewer 跨 task 复用，说明机器门禁失效。 |

## 子代理交付契约

### 触发与主代理接口

按任务事实人工路由，不用 NLP 分类器；用户明确指令优先。不确定时采用 `STANDARD`，一旦出现任一 `STRICT` 条件立即升级。

| 档位 | 可人工判断的范围 | 必要交付 |
| --- | --- | --- |
| `LIGHT` | 简单问答、状态查询、单文件只读检查，且无写入、无需广泛探索。 | 主代理直接完成；不使用 `agent_delivery`，不派发交付 agent 或 reviewer。 |
| `STANDARD` | 多文件分析、小文档、普通代码修复。 | 一个全新交付 agent 端到端完成，运行 focused 验证；常规代码改动再运行 `./scripts/verify-local.sh`。完整 v4 和 reviewer 不是默认门禁。 |
| `STRICT` | 破坏性清理、commit、push、外部写入、迁移、安全、资金、数据丢失风险、显式 1:1/UI 证据、复杂跨层改动，或用户明确要求完整 v4/reviewer。 | 一个全新 agent 创建 v4 snapshot，使用独立 reviewer，并以 `complete` 完成成功交付。 |

- 一个 `STANDARD` 或 `STRICT` 交付只能绑定一个全新 agent；已结束的 agent 不接收新目标。若 `STANDARD` 执行中风险升级，不能复用该 agent 补办 `STRICT`，必须新建 `STRICT` task、agent 和 snapshot。
- `STRICT` 仍遵守主代理只协调、交付 agent 完成实际工作的角色约束；主代理可转发同一 ACTIVE 合同内的澄清、审查修复和重新验证，并原样转发不超过 500 个字符的三行结论。`LIGHT` 不适用该限制，因为没有交付 task。
- `STRICT` 成功后结论必须是“改了什么 / 验证了什么 / 剩余风险”三行。若外层、writer、reviewer 或工具发生 503/systemError，仓库脚本不能自动感知；协调者取得宿主终止证据后必须执行收口。脚本本身不可调用时，最终只能诚实报告“生命周期未闭环”，不得伪造终态或成功。

| 场景 | 默认档位 | 升级或收口规则 |
| --- | --- | --- |
| 简单问答、只读状态 | `LIGHT` | 需要跨文件深入审计时升为 `STANDARD`。 |
| 完整只读审计 | `STANDARD` | 用户要求独立 reviewer 或外部发布证据时升为 `STRICT`。 |
| 小文档、普通代码修复 | `STANDARD` | 触及迁移、安全、数据丢失或复杂跨层时升为 `STRICT`。 |
| 复杂跨层改动、高风险 UI 或显式 1:1 证据 | `STRICT` | 保持完整 v4 与独立 reviewer。 |
| commit、push、破坏性 cleanup、外部写入 | `STRICT` | 先取得用户授权；破坏性清理必须核实目标。 |
| writer 超时 | `STRICT` | 先 `confirm-terminated`，再 `takeover` 或由确认者 `terminate`。 |
| reviewer/tool/systemError | `STRICT` | 不归因于仓库脚本；有终止证据时由确认者 `terminate` 为 `FAILED`、`CANCELLED` 或 `DEGRADED_REPORT`。 |

### 交付子代理职责

- 交付子代理拥有任务的完整结果，负责所有实际工作：读取上下文、探索代码和文档、分析、制定最小方案、调用工具、实现、生成或更新文档、运行验证、整理证据并完成交付。
- 需要并行探索、专项分析、实现或审查时，交付子代理可以继续派发子代理；这些子代理的结论、文件变更和验证结果由交付子代理整合。主代理不介入中间工作。
- 用户明确要求“子代理实现”时，交付子代理或其明确指派的实现子代理必须实际拥有产品代码、测试、脚本和任务文档的变更。只读任务则由交付子代理体系完成所需探索、分析、工具调用和结论，无须虚构实现变更。主代理不得代写后再把结果称为子代理实现。

### STRICT 一任务一代理生命周期

- v4 snapshot 自动生成不可变 UUID `task_id`，并同时绑定原始合同摘要、唯一 writer、`ACTIVE` 生命周期以及 OPEN 的合同/writer 资源。仓库外单一身份注册表在同一锁内登记 task、agent、规范 snapshot 路径与权威 lifecycle；注册表存续期间同一 writer 只属于一个 task，新 snapshot 必须使用新 agent，snapshot 输出路径与 task_id 都不能复用。
- 生命周期为 `ACTIVE -> COMPLETED | FAILED | CANCELLED | DEGRADED_REPORT`。`complete` 仍是唯一成功路径，并且只有完整 reviewer approval 后才可执行；`terminate` 是唯一非成功收口接口。所有终态关闭合同和 writer，旧 agent 不得接受任何新指令。
- 每个 v4 写命令都交叉校验本地 snapshot 与注册表中的 task、合同摘要、规范路径和 lifecycle。任一终态 tombstone 优先于本地文件；完成前复制 snapshot、终态后覆盖回 ACTIVE 备份、或从非规范路径运行，都不能复活任务。
- `terminate` 记录 outcome、actor、时间，以及结构化 reason/evidence。活动 writer 只能自报 `FAILED`；协调者必须先以 `confirm-terminated` 记录 writer 终止证据，且收口身份必须等于该确认者，才能收口三种非成功 outcome。`CANCELLED` 表示用户或 owner 主动停止，`FAILED` 表示无可交付结果，`DEGRADED_REPORT` 表示只有明确局限的只读或部分报告，绝不代表 PASS、`COMPLETED` 或 reviewer approval。
- 任一终态后，`append-rework`、`confirm-terminated`、`takeover`、`review`、`render`、`approve`、`complete`、重复 `terminate` 全部机器拒绝。唯一允许的脚本操作是只读 `status --snapshot <路径>`；它报告权威 lifecycle、本地 lifecycle、terminal outcome、reason/evidence、actor 与路径是否匹配，不能恢复、改写或重新打开任务。
- 每个新的 `STRICT` 目标或合同都必须重新运行 `snapshot`，从而取得新 `task_id`，并由主代理创建新交付子代理。`STANDARD` 也必须使用全新交付 agent，但不因普通风险被强制创建 v4 task。脚本只验证 `STRICT` task、摘要、writer 和状态，不伪造自然语言意图判断。

### STRICT 结构化交付

`STRICT` 交付子代理在首次文件修改前先把用户原话保存为 UTF-8 文件，并把原始验收项保存为 JSON 数组；每项必须包含唯一 `id`、`kind`（`MUST / FORBIDDEN / UNCHANGED`）和 `text`。然后运行：

```bash
python3 scripts/agent_delivery.py snapshot \
  --request-file <用户原话文件> \
  --requirements-file <验收合同 JSON> \
  --writer-id <当前写入者 ID>
```

命令会把用户原话、原始合同摘要、唯一写入者、不可变 `task_id`、ACTIVE 生命周期、当前所有受 Git 管理及未忽略文件的内容指纹写入系统临时目录，并复制任务开始前已经脏的文件作为最小基线，同时原子登记 task/writer。默认身份注册表也位于仓库外系统临时目录；需要跨系统清理周期保留 agent/task 绑定时，必须用 `KMP_MUSIC_AGENT_DELIVERY_STATE_DIR` 指向持久目录。注册表被外部删除后，脚本无法恢复此前的身份历史。已绑定其他 task 或 reviewer 角色的 agent、新旧 task_id 碰撞、已有 snapshot 输出路径都会拒绝。干净文件从快照记录的原始 `HEAD` 还原；用户已有修改不会被自动算作本任务改动。返工必须保持同一 task 和原合同边界，在 snapshot 进程锁内追加连续版本，不能覆盖原始合同：

```bash
python3 scripts/agent_delivery.py append-rework \
  --snapshot <快照路径> \
  --writer-id <当前写入者 ID> \
  --expected-version <已读取的合同版本> \
  --instruction-file <返工原话文件>
```

超时或接管时，协调者先确认旧写入者已经终止并记录证据，再允许接管；确认者必须与旧写入者不同，旧写入者不能自证终止。`takeover` 在没有有效 `confirm-terminated` 记录时直接失败。确认前，新代理禁止写同一任务文件：

```bash
python3 scripts/agent_delivery.py confirm-terminated \
  --snapshot <快照路径> \
  --expected-writer <旧写入者 ID> \
  --confirmed-by <确认者 ID> \
  --evidence '<终止确认依据>'
python3 scripts/agent_delivery.py takeover \
  --snapshot <快照路径> \
  --expected-writer <旧写入者 ID> \
  --new-writer <新写入者 ID>
```

当前活动 writer 只有在无可交付结果时可自行收口为 `FAILED`：

```bash
python3 scripts/agent_delivery.py terminate \
  --snapshot <快照路径> \
  --writer-id <当前写入者 ID> \
  --outcome FAILED \
  --reason '<单行原因>' \
  --evidence '<单行证据>'
```

协调者必须先成功运行 `confirm-terminated`，并使用同一 `confirmed-by` 身份，才可收口 `FAILED`、`CANCELLED` 或 `DEGRADED_REPORT`。这条路径用于已取得 writer/reviewer/tool 宿主终止证据但无法完成成功门禁的情形；它不替代或绕过 `complete`：

```bash
python3 scripts/agent_delivery.py terminate \
  --snapshot <快照路径> \
  --coordinator-id <confirm-terminated 的 confirmed-by> \
  --outcome DEGRADED_REPORT \
  --reason '<单行原因>' \
  --evidence '<单行终止证据>'
```

执行中的 v3 快照不会静默获得 task 身份；写命令会拒绝并要求显式迁移。迁移保留原合同、返工历史、writer、基线提交、文件指纹和基线目录，但生成或接收一个规范 UUID，并使旧 v1 review 报告失效；迁移后必须重新 review/render：

```bash
python3 scripts/agent_delivery.py status --snapshot <v3 快照路径>
python3 scripts/agent_delivery.py migrate-v3 \
  --snapshot <v3 快照路径> \
  --expected-writer <当前写入者 ID> \
  [--task-id <已有规范 UUID>]
```

验证完成后准备逐条规格结论 JSON。每项包含合同中的 `id`、`PASS / FAIL` 和非空 `evidence`；证据来源可为 `raw_request / original_contract / rework_instruction / task_diff / test / runtime / build`，但每个 requirement 至少要有一条 `task_diff / test / runtime` 证据，只有构建通过不能证明规格。先生成审查报告，再生成交付 Manifest：

```bash
python3 scripts/agent_delivery.py review \
  --snapshot <快照路径> \
  --writer-id <当前写入者 ID> \
  --reviewer-id <本 task 的全新 reviewer ID> \
  --verdicts-file <逐条规格结论 JSON> \
  --verification-evidence-file <验证命令与结果文件>

python3 scripts/agent_delivery.py render \
  --snapshot <快照路径> \
  --writer-id <当前写入者 ID> \
  --review-report <review 输出路径> \
  --changes '<行为或产物改动摘要>' \
  --verification '<验证命令、结果和独立审查结论>' \
  --risks '<剩余风险；没有时明确写无已知剩余风险>' \
  --receipt-output <仓库外 render receipt 路径>

python3 scripts/agent_delivery.py approve \
  --snapshot <快照路径> \
  --reviewer-id <本 task reviewer ID> \
  --review-report <review 输出路径> \
  --render-receipt <reviewer 已检查的 receipt 路径> \
  --output <仓库外 reviewer approval 路径>

python3 scripts/agent_delivery.py complete \
  --snapshot <快照路径> \
  --writer-id <当前写入者 ID> \
  --review-report <review 输出路径> \
  --render-receipt <reviewer 已检查的 receipt 路径> \
  --review-approval <reviewer approval 路径>
```

`review` 同时绑定 reviewer ID、task_id、原始合同摘要、用户原话、全部返工指令和当前任务级 diff，要求逐个 requirement 给出结论与证据。reviewer 必须与当前及历史 writer 不同；同一 task 的复核可复用，不同角色或 task 复用直接拒绝。任务 diff、合同、writer 或 reviewer 状态变化后旧报告失效；任一 requirement 为 FAIL 时禁止 render/complete。

`render` 在 ACTIVE 内生成新的仓库外 Markdown Manifest、三行候选结论和 receipt，不关闭任务。Manifest 包含 task/reviewer 身份、本轮变化、基线归因、完整文件清单、任务级 unified diff、验证证据、逐条规格审查和剩余风险；同一快照的旧 Manifest 不会被覆盖。receipt 绑定 task、合同、reviewer、当前 diff、review 报告、Manifest 内容和三行候选，任何一项变化都会使它过期。

独立 reviewer 检查实际 diff、验证证据、Manifest 和候选三行；发现回流修复后，同一 reviewer 重新运行 review/render 复核。确认无发现后运行 `approve`，批准凭据绑定最终 receipt、Manifest、diff 与三行摘要；缺少或过期 approval 时禁止 `complete`。`complete` 在 snapshot 锁内重新校验全部绑定，原子关闭合同与 writer，并原样重放 reviewer 已检查的三行；并发 complete 只有一个成功。交付子代理必须把 `complete` 的标准输出原样作为 final。

### STRICT 审查与复核

- 交付前，代码、文档和任务结论的审查由本 task 全新、独立、只读的审查子代理完成；审查子代理不修改实现文件或任务文档，并核对 task_id、合同摘要、实际 diff、验证证据、Manifest 与 `render` 候选三行是否一致。
- 审查发现先回流给交付子代理或实现子代理修复，再由审查子代理复核最终 diff 和验证证据。没有完成该回流时，不得给出“无问题”结论。
- 同一 task 可让同一 reviewer 完成复核循环；新 task 必须创建新 reviewer。reviewer 检查候选后才能 complete，且完成后该 reviewer 不得用于后续任务。
- 对抗审查、Figma 对照、截图、测试和运行态验证仍按任务类型执行；其探索、工具调用、判断和结论均由交付子代理体系完成。

### 可证伪标准

- `STRICT` 会话轨迹中，主代理调用创建、转发或等待子代理以外的工具，或出现仓库探索或分析、代码或任务文档的文件变更、验证命令、审查判断，均视为本契约失败；仅检查三项标题是否齐全不算实际工作。
- `STRICT` 交付子代理只进行审查、研究或汇报，却没有完成其被委派的实际工作；含变更的任务没有由其完成实现；或产物未经独立审查和必要验证，均视为本契约失败。
- `STRICT` 交付没有在首次修改前冻结原话、三类合同和快照，返工覆盖原始合同，未确认旧 writer 终止就接管，缺少 task/reviewer/diff 绑定、受审 receipt 或 `complete`，Manifest 被覆盖或缺少基线归因、完整文件清单、任务级 Diff、验证证据，修改存在却未上报，或任一终态后继续复用 agent/reviewer，均视为本契约失败。
- 后续任务需以新会话验证：主代理只派发、退回不合格结论并原样转发；交付子代理体系完成实际工作并交回不超过 500 个字符、可核查的三行结论。

## 升级顺序

重复错误不要只追加提示语。优先按下面顺序找最早 owner：

1. 能用测试证明的行为，补到 `composeApp/src/*Test`。
2. 能自动化的本地门禁，补到 `scripts/verify-local.sh` 或 Gradle 任务。
3. 能让非法状态不可表示的规则，收进领域模型、UseCase、Repository 接口、mapper 或 `expect/actual` 边界。
4. 只有检索和流程问题，才更新 `AGENTS.md`、`docs/agents/*`、ADR 或 `.scratch/<feature-slug>/`。

## 保留或删除规则

- 保留：能改变后续 AI 行为，并且失败时能指出要改哪个 owner。
- 修改：规则真实但太宽，导致 AI 仍需猜测具体命令、目录或验收证据。
- 删除：重复了更权威的 Gradle、源码、ADR 或测试事实，或者基于 `.scratch` issue、审查记录或交付记录能看出连续两次没有被后续任务用到。

## 三个校验任务

用这些真实任务定期验证 harness 是否有效：

1. 修复一个 `MusicAppController`、播放、队列、收藏、搜索或扫描相关缺陷：AI 应读取架构和测试策略，补回归测试，先跑 focused 测试，再主动运行 `./scripts/verify-local.sh`；如果没跑脚本，必须说明环境限制或等价证据。
2. 进行一次移动端或桌面 UI 视觉改版：AI 应读取 `docs/agents/ui-state.md`，建立需求相关状态矩阵；Desktop 首页、专辑或歌手页优先运行对应 `desktop-ui-qa.sh` 场景。显式要求 1:1 或动画时，必须取得本次构建的指定尺寸截图或动态证据，不能把证据缺失降级为剩余风险。
3. 处理一个 GitHub BUG：AI 应先镜像到 `.scratch/github-bugs/issues/`，下载附件证据，记录根因、验证、对抗式审查和剩余风险，再考虑提交、push 与回写。
