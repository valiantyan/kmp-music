# 本批次进度

## 当前状态

- 批次：`local-audio-discovery-source-coverage` issue 13 到 17。
- 调度模式：长跑 Agent Harness 队列协调器。
- 当前状态：已完成 Harness 配置修复，等待用户在 kmp-music 项目中启动协调器线程。
- 当前 issue：`13-failed-scan-preserves-existing-songs-test`。
- 当前阶段：等待启动协调器并派发 issue 13。
- 已完成门禁检查：issue 12 已通过文件门禁。
- 禁止事项：当前说明/配置会话不得直接执行 13 到 17 的 Harness，也不得创建实现线程。

## 队列状态

| 顺序 | Issue | 状态 | 门禁 | 下一步 |
| ---: | --- | --- | --- | --- |
| 12 | `12-cancelled-scan-ui-state` | `ready-for-human` | 已通过 | 作为 13 的前置基线 |
| 13 | `13-failed-scan-preserves-existing-songs-test` | `ready-for-agent` | 未检查 | 启动协调器后派发 |
| 14 | `14-failed-scan-positive-only-merge` | `ready-for-agent` | 未检查 | 等 13 通过后派发 |
| 15 | `15-scan-page-summary-copy-test` | `ready-for-agent` | 未检查 | 等 14 通过后派发 |
| 16 | `16-scan-page-platform-copy` | `ready-for-agent` | 未检查 | 等 15 通过后派发 |
| 17 | `17-final-verification-and-adversarial-review` | `ready-for-agent` | 未检查 | 等 16 通过后派发 |

## 已读取上下文

- `AGENTS.md`
- `.agent-loop/contract.md`
- `.agent-loop/progress.md`
- `.agent-loop/log.md`
- `.agent-loop/scorecard.md`
- `.agent-loop/restart-policy.md`
- `/Users/yanhao/Downloads/qinglilaji /.agents/skills/long-running-loop/SKILL.md`
- `.scratch/local-audio-discovery-source-coverage/PRD.md`
- issue 12 到 17 的本地 issue 文件

## 下一步

用户需要在 kmp-music 项目中新开一个 Codex 线程，并使用最终说明中的 coordinator prompt 启动长跑 Agent Harness。协调器启动后先派发 issue 13，等 13 门禁通过后再派发 14，依次直到 issue 17。

## 恢复提示

恢复时先读取 `.agent-loop/contract.md`、本文件、`.agent-loop/log.md`、`.agent-loop/scorecard.md` 和 `.agent-loop/restart-policy.md`。如果某个 issue 声称完成，先读取该 issue 文件并执行门禁检查；不要只凭聊天状态推进。
