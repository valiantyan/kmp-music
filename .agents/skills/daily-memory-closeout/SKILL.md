---
name: daily-memory-closeout
description: 仅当用户明确要求执行每日 memory 收口时使用，例如“每日 memory 收口”、“使用 daily-memory-closeout”或“run daily memory closeout”。这是当前项目的手动工作流，用于按 memory/WRITE_POLICY.md 清理 memory/brain.md，并把已完成或不再需要占用工作记忆的内容归档到 memory/journal/YYYY-MM-DD.md。
---

# 每日 Memory 收口

## 手动触发

仅在用户明确要求时使用本 skill，例如：

- `每日 memory 收口`
- `使用 daily-memory-closeout`
- `run daily memory closeout`
- 用户粘贴每日 memory 收口提示词并要求执行

普通 memory 讨论、查看、解释或提问时，不自动执行本 skill。没有明确执行指令时，只回答问题，不修改文件。

## 职责边界

- 本 skill 只负责手动收口流程，不维护第二套 memory 规则。
- 写入准入、去重、隐私和 L2 判断以 `memory/WRITE_POLICY.md` 为准。
- Journal 格式与归档要求以 `memory/journal/README.md` 为准。
- Memory 路径和加载范围以 `memory/MEMORY.md` 为准。

## 执行流程

1. 先读取 `memory/MEMORY.md`，确认 memory 结构索引。
2. 读取 `memory/WRITE_POLICY.md`，用它判断归档位置和 L2 写入准入。
3. 读取 `memory/brain.md` 和 `memory/projects/android-harness-engineering/context.md`。
4. 读取 `memory/MIND.md`，确认长期认知边界，避免把临时进展写入核心认知。
5. 读取 `memory/journal/README.md`，确认当天 journal 的归档格式。
6. 检查当天日志 `memory/journal/YYYY-MM-DD.md` 是否存在；不存在则创建，已存在则追加新的主题段落，不覆盖已有内容。
7. 从 `memory/brain.md` 提取不再需要占用工作记忆的内容，按主题归档到当天 journal。
8. 清理 `memory/brain.md`，只保留当前进行中、等待事项、阻塞事项和下一步。
9. 除非满足 `memory/WRITE_POLICY.md` 的 L2 准入，否则不写 `memory/projects/`、`memory/topics/`、`memory/user/` 或 `memory/MIND.md`。
10. 写入后检查 `memory/brain.md` 是否保持在 80 行以内。
11. 最后汇报修改了哪些文件、归档了什么、哪些内容仍留在 `brain.md`、是否写入 L2。

## 质量门禁

- 不复制用户原话；先压缩成事实、状态、阻塞或下一步。
- 不把流水、重复和噪音写入 journal。
- 不把不满足准入的信息写入 L2。
- 本项目生成的 Markdown 描述内容使用中文。
