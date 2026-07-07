# 长跑 Agent 进度

## 当前状态

- 状态: idle
- 阶段: 无活跃批次
- 最后更新: 2026-07-07
- 当前目标: 等待下一次用户明确指定的长跑任务或顺序批次。
- 当前切片: 无
- 连续失败切片数: 0

## 下一步

当前没有需要继续派发、验证或归档的旧队列。

下一次启动长跑 Agent Harness 时，先根据用户目标建立新的契约、队列、门禁和终止条件，再开始执行。

## 恢复说明

1. 读取 `.agent-loop/contract.md`、本文件、`.agent-loop/log.md`、`.agent-loop/scorecard.md` 和 `.agent-loop/restart-policy.md`。
2. 如果本文件仍为 `idle`，不要继续任何旧批次。
3. 如需审计已完成批次，读取 `.agent-loop/archive/` 下对应归档目录。

## 重启提案

当前无。
