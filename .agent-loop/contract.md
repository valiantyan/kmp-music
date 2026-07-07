# 长跑 Agent 契约

## 当前状态

- 状态: idle
- 批次名称: 无
- 当前目标: 无
- 当前 issue: 无
- 当前 checkpoint: 无

## 默认规则

- 等待用户提供新的顺序批次、PRD 或 issue 队列。
- 新批次开始前，必须重新读取 `AGENTS.md`、本目录状态文件、PRD 和相关 issue。
- 未建立新契约前，不派发任务会话，不创建任务 checkpoint，不运行批次验证。

