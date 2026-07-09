# 分诊标签

| mattpocock/skills 中的标签 | 本项目 tracker 标签 | 含义 |
| --- | --- | --- |
| `needs-triage` | `needs-triage` | 需要维护者评估该 issue |
| `needs-info` | `needs-info` | 等待报告者补充信息 |
| `ready-for-agent` | `ready-for-agent` | 信息完整，可以交给 AFK agent 处理 |
| `ready-for-human` | `ready-for-human` | 需要人工实现、复核或验收 |
| `wontfix` | `wontfix` | 确认不处理 |

当 skill 提到某个分诊角色时，使用上表对应的标签字符串。

## GitHub BUG 标签

GitHub BUG 流程在上表基础上额外使用这些标签：

| 标签 | 含义 |
| --- | --- |
| `bug` | 真实或疑似缺陷 |
| `agent-working` | Agent 已领取并正在修复 |
| `blocked` | 修复被外部条件阻塞 |
| `fixed` | 修复已提交、push，并已回写修复日志 |

GitHub Issue 仍要同步使用 `needs-triage`、`needs-info`、`ready-for-agent`、`ready-for-human`、`wontfix`，含义与本地 tracker 一致。
