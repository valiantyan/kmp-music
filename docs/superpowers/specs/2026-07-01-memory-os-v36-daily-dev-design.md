# Memory OS v3.6 daily-dev mode 设计

## 背景

当前项目已经接入 Codex Memory OS v3.5。v3.5 的定位偏保守：工具输出低可信、`wiki.md` 不自动写入、候选记忆需要较多人工确认。这保证了长期记忆不容易被工具输出或 prompt injection 污染，但日常开发时人工维护成本偏高。

v3.6 的目标不是推翻 v3.5，而是在保留安全边界的前提下增加 `daily-dev` 默认模式，让短期工作状态和已验证经验更省心地沉淀。

## 目标

- 当前项目默认启用 `daily-dev` 模式。
- `working` 对明确任务状态自动写入 active，用于交接、下一步、阻塞点和短期上下文；普通原始 prompt 不直接 active。
- `learning` 半自动写入：明确的失败原因、修复经验、可复用排查路径可以在达到阈值后 active。
- `wiki` 仍然人工确认，不允许自动写入稳定项目事实。
- `preferences` 仍然必须来自用户明确表达，不从单次行为或工具输出推断。
- 工具输出仍不可直升 `learning`、`wiki` 或 `preferences` active；只能作为候选或支撑证据。
- 审计覆盖 v3.6 的 daily-dev 行为，避免只有配置名变化、实际行为未变化。

## 非目标

- 不让所有记忆全自动进入长期层。
- 不把工具输出当作 canonical 事实。
- 不自动修改 `AGENTS.md`、系统指令或 hook policy。
- 不改变运行时 JSONL 不提交到 git 的策略。
- 不引入外部 LLM verifier 或网络依赖。
- 不重写 Memory OS 存储格式。

## 策略设计

v3.6 增加明确的模式字段：

```json
{
  "version": "3.6.0",
  "mode": "daily-dev"
}
```

`daily-dev` 模式的层级策略是：

```text
working      有资格条件的自动 active，要求明确任务状态信号
learning     半自动 active，要求明确经验信号、足够信任且来源不是 tool
wiki         review only
preferences  用户明确授权后 active
```

这保留了 v3.5 的核心安全原则：长期事实和用户偏好不能由工具输出或模型推断直接污染。

`daily-dev` 的自动写入资格比“只看 layer”更严格：

- `working` active 需要明确命中任务状态信号，例如 handoff、next step、blocked、in progress、未完成、下一步、待确认、交接。普通用户请求、闲聊、一次性问题和只有命令意图的 prompt 只能进入 buffer 或 candidate。
- `learning` active 需要明确命中经验信号，例如 root cause、fixed、resolved、avoid、lesson learned、修复、根因、解决、踩坑、教训，并且来源必须是 user 或 inference。CLI/manual add 事件继续按 user source 记录，不新增 `MemoryEvent.source` 类型。tool 只能作为 evidence，不能直接生成 active learning。
- `wiki` 在 daily-dev 中仍然只进入 review，不自动 active。
- `preferences` 必须来自用户显式长期偏好表达，例如“请记住”“以后默认”“我偏好”“希望以后”“always”。礼貌用语“请”本身不是偏好信号。

## 代码触点

### `.agent-memory/config.json`

把项目配置升级为 `3.6.0`，并设置 `mode: "daily-dev"`。`promotion` 增加或明确这些开关：

- `working_auto_write: true`
- `learning_auto_candidate: true`
- `learning_auto_write_min: 0.68`
- `wiki_auto_write: false`
- `tool_to_canonical_allowed: false`
- `tool_to_learning_active_allowed: false`
- `preferences_require_explicit_user_signal: true`

### `memory_core/config.py`

默认配置升级到 v3.6，保留 conservative 安全边界。配置读取继续兼容缺省字段，旧配置不会因为缺少 `mode` 或新增开关而崩溃。

### `memory_core/classifier.py`

分类逻辑继续只负责判断候选层级和原因，不单独决定 active。需要让 learning 信号更贴近日常开发：

- bug / failed / failure / mistake
- fixed / fix / root cause / resolved
- 踩坑 / 教训 / 失败 / 错误 / 修复 / 根因 / 解决

偏好仍要求用户来源和显式长期偏好信号，不能因为中文“请”或一次性请求触发。wiki 信号仍默认进入 review 路径。

### `memory_core/validator.py`

验证逻辑承接模式策略：

- `working` 在 daily-dev 中必须同时达到 `trust_min` 并带有明确任务状态原因，才 active；普通原始 prompt 即使达到 `trust_min` 也只能 candidate。
- `learning` 使用 `max(learning_min, learning_auto_write_min)` 作为 active 阈值，且来源不能是 tool。
- `wiki` 在 `wiki_auto_write = false` 时始终 review。
- `preferences` 只有显式用户偏好且达到阈值才 active。
- `tool` 来源默认仍只能成为 working/learning 候选或 evidence，不能 active 到 learning、wiki 或 preferences。

### `memory_core/compiler.py`

编译上下文标题从配置 `version` 生成，并保持“Memory is advisory”警告。输出结构不改变，避免影响 hook 和启动注入。

### `memory_core/audit.py`

审计报告升级为 v3.6，新增 daily-dev 检查：

- 配置版本和模式正确。
- 明确 handoff/next-step/blocker 类 working 状态可自动 active。
- 普通原始用户请求不会直接 working active。
- 明确用户偏好仍进入 preferences。
- 工具注入不能进入 wiki。
- 工具输出不能直接 active 到 learning。
- wiki 自动写入仍关闭。
- learning 经验可在阈值满足时 active。

## 运行时与 git 策略

继续提交 Memory OS 代码、配置和 Markdown 模板；继续忽略运行时 JSONL、state 和 archive。v3.6 daily-dev 会更频繁更新 Markdown auto section，因此提交前需要显式确认是否要把 `.agent-memory/*.md` 的自动物化内容作为项目记忆提交。

本次实现不主动提交自动生成的个人运行时记忆。

实现和审计命令默认不得污染真实 `.agent-memory/*.md` auto section。需要运行端到端物化测试时，应在临时仓库副本中完成；如果真实 auto section 已因 hook 运行发生变化，提交前只允许在用户明确要求提交项目记忆时 stage 这些 Markdown 变化。

## 验收标准

- `./install-codex-memory-os.sh` 通过。
- Red Team audit 全部通过。
- `python3 -m py_compile memory_core/*.py .codex/hooks/*.py` 通过。
- `memory_core.compiler.build_context()` 输出 v3.6 标题和 advisory 警告。
- 工具注入样例不会进入 `wiki` active。
- 工具输出样例不会直接进入 `learning` active。
- 用户明确偏好仍能进入 `preferences` active。
- 普通当前任务 prompt 不会直接进入 `working` active。
- 明确交接、下一步或阻塞点 prompt 能进入 `working` active。
- 明确修复经验能进入 `learning` active。
- `PREFERENCE_RE` 不再因单独的中文“请”触发偏好分类。
- audit/install 不会把测试数据物化进真实 `.agent-memory/*.md`。

## 设计自检

- 没有扩大到全自动长期记忆。
- 没有改变 Memory OS 的存储格式。
- 没有把工具输出提升为 learning、wiki 或 preferences active。
- daily-dev 只降低日常工作状态和经验沉淀的人工成本。
