# Learning Memory

Validated lessons, recurring failure modes, and reusable corrections.

## Manual Notes

- 处理 Memory OS 运行态数据时，先回到“外挂 Agent + 自循环进化”的主题轴：raw prompt、任务派发表和工具输出是燃料，不是结论。
- 不要把 `.agent-memory/working.md` 自动区直接归档成 handoff 或项目文档；应先提炼成少量人工确认的 wiki/learning 条目，再决定是否清理运行态。
- 如果需要清理 `.agent-memory/working.md` 的自动物化内容，不能只手工删 Markdown；它由 `memory_items.jsonl` 等结构化运行库生成，应该通过明确的 Memory OS closeout/cleanup 工作流处理。
- 交付前检查 `RED_TEAM_REPORT.md`、handoff 草稿、`.superpowers/sdd/task-*.md` 和 `.agent-memory/*.md`，避免把审计副产物或运行态噪音误提交。

## Auto-Managed Items

<!-- MEMORY_OS_AUTO_BEGIN -->
- `a0e97d3888757fc1` authority=0.80 trust=0.69 source=inference: Turn summary: root cause fixed: validator allowed raw prompts into working active.
  - reasons: learning_pattern_validated
- `eea399299d68b663` authority=0.78 trust=0.74 source=inference: root cause fixed: Memory OS heal 在 memory_items 过多时会卡在全量 semantic graph；运行态整理优先用轻量 materialize_markdown，图优化应拆成增量或限额流程。
  - reasons: learning_pattern_validated
- `433fd075e504d5b2` authority=0.77 trust=0.73 source=inference: root cause fixed: handoff 不是 Memory OS 主题归档的正确出口；已验证的处理方式是围绕“外挂 Agent + 自循环进化”提炼运行态数据，把 raw prompt/tool output 只当证据，把长期结论写入人工确认的 wiki/learning。
  - reasons: learning_pattern_validated
<!-- MEMORY_OS_AUTO_END -->
