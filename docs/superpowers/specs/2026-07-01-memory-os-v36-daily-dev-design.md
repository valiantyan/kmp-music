# Memory OS v3.6 daily-dev mode 设计

## 背景

当前项目已经接入 Codex Memory OS v3.5。v3.5 的定位偏保守：工具输出低可信、`wiki.md` 不自动写入、候选记忆需要较多人工确认。这保证了长期记忆不容易被工具输出或 prompt injection 污染，但日常开发时人工维护成本偏高。

v3.6 的目标不是推翻 v3.5，而是在保留安全边界的前提下增加 `daily-dev` 默认模式，让短期工作状态和已验证经验更省心地沉淀。

## 目标

- 当前项目默认启用 `daily-dev` 模式。
- `working` 自动写入 active，用于当前任务、交接、下一步和短期上下文。
- `learning` 半自动写入：明确的失败原因、修复经验、可复用排查路径可以在达到阈值后 active。
- `wiki` 仍然人工确认，不允许自动写入稳定项目事实。
- `preferences` 仍然必须来自用户明确表达，不从单次行为或工具输出推断。
- 工具输出仍不可直升 `wiki` 或 `preferences`。
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
working      自动 active
learning     半自动 active，要求明确经验信号和足够信任
wiki         review only
preferences  用户明确授权后 active
```

这保留了 v3.5 的核心安全原则：长期事实和用户偏好不能由工具输出或模型推断直接污染。

## 代码触点

### `.agent-memory/config.json`

把项目配置升级为 `3.6.0`，并设置 `mode: "daily-dev"`。`promotion` 增加或明确这些开关：

- `working_auto_write: true`
- `learning_auto_candidate: true`
- `learning_auto_write_min`
- `wiki_auto_write: false`
- `tool_to_canonical_allowed: false`
- `preferences_require_explicit_user_signal: true`

### `memory_core/config.py`

默认配置升级到 v3.6，保留 conservative 安全边界。配置读取继续兼容缺省字段，旧配置不会因为缺少 `mode` 或新增开关而崩溃。

### `memory_core/classifier.py`

分类逻辑继续只负责判断候选层级和原因。需要让 learning 信号更贴近日常开发：

- bug / failed / failure / mistake
- fixed / fix / root cause / resolved
- 踩坑 / 教训 / 失败 / 错误 / 修复 / 根因 / 解决

偏好仍要求用户来源和显式偏好信号。wiki 信号仍默认进入 review 路径。

### `memory_core/validator.py`

验证逻辑承接模式策略：

- `working` 在 daily-dev 中达到 `trust_min` 后直接 active。
- `learning` 在达到 `learning_auto_write_min` 或 `learning_min` 后 active。
- `wiki` 在 `wiki_auto_write = false` 时始终 review。
- `preferences` 只有显式用户偏好且达到阈值才 active。
- `tool` 来源默认仍只能成为 working/learning 候选或 active 经验，不能 canonical。

### `memory_core/compiler.py`

编译上下文标题升级为 v3.6，并保持“Memory is advisory”警告。输出结构不改变，避免影响 hook 和启动注入。

### `memory_core/audit.py`

审计报告升级为 v3.6，新增 daily-dev 检查：

- 配置版本和模式正确。
- working prompt 可自动 active。
- 明确用户偏好仍进入 preferences。
- 工具注入不能进入 wiki。
- wiki 自动写入仍关闭。
- learning 经验可在阈值满足时 active。

## 运行时与 git 策略

继续提交 Memory OS 代码、配置和 Markdown 模板；继续忽略运行时 JSONL、state 和 archive。v3.6 daily-dev 会更频繁更新 Markdown auto section，因此提交前需要显式确认是否要把 `.agent-memory/*.md` 的自动物化内容作为项目记忆提交。

本次实现不主动提交自动生成的个人运行时记忆。

## 验收标准

- `./install-codex-memory-os.sh` 通过。
- Red Team audit 全部通过。
- `python3 -m py_compile memory_core/*.py .codex/hooks/*.py` 通过。
- `memory_core.compiler.build_context()` 输出 v3.6 标题和 advisory 警告。
- 工具注入样例不会进入 `wiki` active。
- 用户明确偏好仍能进入 `preferences` active。
- 普通当前任务 prompt 能进入 `working` active。
- 明确修复经验能进入 `learning` active。

## 设计自检

- 没有扩大到全自动长期记忆。
- 没有改变 Memory OS 的存储格式。
- 没有把工具输出提升为 wiki 或 preferences。
- daily-dev 只降低日常工作状态和经验沉淀的人工成本。
