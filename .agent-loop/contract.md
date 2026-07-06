# 本批次长跑调度契约

## 目标

使用“长跑 Agent Harness”作为 `local-audio-discovery-source-coverage` 批次的顺序协调器，从 issue 13 自动推进到 issue 17。Harness 负责队列状态、派发、门禁检查、恢复记录和最终完成判断；每个 issue 的业务实现必须在独立实现线程或 fresh session 中完成。

本契约描述如何在 kmp-music 项目中使用 Harness。若用户只要求说明操作方法，当前会话不得启动或执行 13 到 17 的实现线程。

## 固定顺序

已完成基线：

- `12-cancelled-scan-ui-state`：已派发到 `codex://threads/019f373d-62aa-7730-b6c0-706127243eda`，issue 文件当前为 `ready-for-human`，验收标准已全勾。

待执行队列：

1. `13-failed-scan-preserves-existing-songs-test`
2. `14-failed-scan-positive-only-merge`
3. `15-scan-page-summary-copy-test`
4. `16-scan-page-platform-copy`
5. `17-final-verification-and-adversarial-review`

## 协调器职责

- 启动时读取 `AGENTS.md`、本目录 `.agent-loop/*.md`、长跑 Harness skill、PRD 和 issue 12 到 17。
- 从 `progress.md` 恢复当前 issue、阶段、已派发线程和门禁状态。
- 每次只派发队列中的当前 issue，不并发派发依赖链后续 issue。
- 当前 issue 声称完成后，重新读取该 issue 文件并执行门禁检查。
- 门禁通过后，更新 `progress.md`、追加 `log.md`、更新 `scorecard.md`，再推进下一项。
- 门禁未通过时，停在当前 issue，记录缺失证据和下一步，不推进下一项。
- issue 17 通过最终验证与对抗式审查后，才能把批次标记为完成。

## 当前 issue 统一 prompt 模板

派发每个 issue 时，使用以下模板，只替换 `Issue:` 路径：

```text
PRD:
/Users/yanhao/Desktop/demo/kmp-music/.scratch/local-audio-discovery-source-coverage/PRD.md

Issue:
<当前 issue 路径>

请只按这个 issue 交付：实现当前 issue 要求的最小改动，并运行与改动范围匹配的验证。

范围约束：
- 不要顺手实现后续 issue。
- 如果当前 issue 是红灯测试任务，验证到测试按预期失败即可，不要为了变绿去实现后续 issue。
- 优先遵守仓库 AGENTS.md、PRD 和当前 issue 的要求；如有冲突，以更高优先级指令为准。
- 修改前先阅读 PRD、当前 issue 和相关源码。

实现与验证：
- 按 `/implement` 流程实现当前 issue；如果当前环境没有该流程，按仓库 AGENTS.md 的实现、验证和提交前自检要求等价执行。
- 根据改动范围运行匹配验证；涉及共享状态或逻辑时运行对应测试，涉及 Android 编译时运行匹配的 Gradle 命令。
- 不要在没有验证的情况下声称成功；无法运行的验证要说明原因。

对抗式审查：
- 交付前请按 AGENTS.md 的“对抗式审查”要求自检。
- 列出本次改动最可能翻车的 3-5 个点，从逻辑漏洞、事实正确性、是否有更简单做法、是否越界实现后续 issue、验证是否充分几个角度逐项攻击。
- 发现问题先修复，并重新运行匹配验证。

Code Review：
- 完成实现和验证后，请按 `/code-review` 流程运行 code-review；如果当前环境没有该流程，手动执行 Standards + Spec 两轴审查。
- 对本次 diff 做 Standards + Spec 两轴审查。
- 如发现问题，先修复并重新验证，然后重新确认 review 结论。

最后请同步更新当前 issue 文件：
- 勾选已经满足的验收标准。
- 在底部追加 `## Comments`。
- Comments 里记录：实现摘要、验证命令与结果、对抗式审查结论、code-review 结论、剩余风险或未完成项。
- 如果全部验收标准已满足，将 `Status:` 从 `ready-for-agent` 改为 `ready-for-human`，表示等待人工验收。
- 如果仍未完成，保持 `ready-for-agent`，并在 Comments 说明原因。
```

## 推进门禁

协调器必须重新读取当前 issue 文件，并检查：

- `Status` 是否为 `ready-for-human`。
- 验收标准是否全部勾选。
- `Comments` 是否记录实现摘要。
- `Comments` 是否记录验证命令与结果。
- `Comments` 是否记录对抗式审查结论。
- `Comments` 是否记录 code-review 结论。
- `Comments` 是否记录剩余风险或未完成项。

只有当前 issue 全部满足门禁，才推进下一个 issue。否则停在当前 issue，并在 `progress.md` 和 `log.md` 记录阻塞原因。

## 完成条件

以下条件全部满足后，批次才算完成：

- issue 13 到 17 均为 `ready-for-human`。
- issue 13 到 17 的验收标准均已全勾。
- issue 13 到 17 的 `Comments` 均满足门禁。
- issue 17 已记录最终验证和对抗式审查。
- `progress.md`、`log.md` 和 `scorecard.md` 均记录最终状态与验证证据。
