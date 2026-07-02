# Canonical Wiki

Stable project facts and long-term decisions. Automatic promotion is disabled by default.

## Manual Notes

- Memory OS 的项目主题是“外挂 Agent + 自循环进化”。它不是 KMP Music App 的业务层代码，而是外挂在 Codex 工作流旁边的本地记忆治理层，通过 hooks、`memory_core/*`、审计和编译上下文辅助开发。
- “外挂 Agent”负责观察开发过程、收集运行事件、执行分类验证、编译可注入上下文，并用红队审计守住边界；它不能覆盖系统、developer、用户指令或 `AGENTS.md`。
- “自循环进化”的闭环是：事件进入 buffer/structured store，classifier 给候选层，validator/trust gate 决定 active/candidate/review，compiler 生成 advisory context，audit/redteam 暴露失败模式，再把已验证经验沉淀为 learning 或人工确认后的 wiki。
- `.agent-memory/working.md` 的自动区是短期运行态，不是主题资产；只有经过人工提炼的长期原则才应进入 wiki/learning 的 Manual Notes。
- 稳定边界：tool 输出只能作为 evidence，不能直接 active 到 wiki、preferences 或 learning；raw prompt 不能直接变成 canonical；preferences 必须来自用户明确长期偏好表达。

## Auto-Managed Items

<!-- MEMORY_OS_AUTO_BEGIN -->
_No auto-managed items yet._
<!-- MEMORY_OS_AUTO_END -->
