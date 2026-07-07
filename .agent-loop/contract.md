# 本批次长跑调度契约

## 目标

使用“长跑 Agent Harness”作为 `local-audio-discovery-source-coverage` 批次的顺序协调器；在协调器持续运行、被用户恢复或被已配置自动化唤醒时，从 issue 13 按队列推进到 issue 17。Harness 负责队列状态、派发、门禁检查、恢复记录和最终完成判断；每个 issue 的业务实现必须在独立实现线程或 fresh session 中完成。

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
- 门禁通过后，先创建 Git checkpoint 并记录提交哈希，再更新 `progress.md`、追加 `log.md`、更新 `scorecard.md`；如果这些状态文件产生新 diff，再创建一个小的调度 metadata commit，确认工作区状态满足契约后才推进下一项。
- `已派发`、`等待实现`、`等待线程返回` 等运行时状态只能作为 `.agent-loop` 恢复记录，不能单独提交为 Git checkpoint，也不能解除当前 issue 的文件、验证、审查和 Git checkpoint 门禁。
- metadata checkpoint 只能跟随已经完成门禁的 issue checkpoint，用于固化提交哈希、评分、日志或最终状态；任务完成 checkpoint 可以包含 `.agent-loop` 从等待态更新为完成态的状态 diff，但提交语义必须是当前 issue 完成并通过门禁，而不是“任务已派发”。
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
- 不要在实现线程中执行 `git commit`；提交由协调器在门禁通过后统一创建。

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

## Git checkpoint 策略

本批次要求每个 issue 通过门禁后形成 Git checkpoint，避免多个 issue 的改动堆在同一个未提交工作区。

- 实现线程只负责当前 issue 的 diff、验证、审查和 issue 文件更新，不自行提交。
- 协调器在当前 issue 文件门禁通过后，先检查 `git status --short --branch` 和当前 diff，确认改动只属于当前 issue 和必要的 `.agent-loop` 调度记录。
- 协调器创建一个当前 issue 的 checkpoint commit，并记录提交哈希。
- 如果记录提交哈希会产生新的 `.agent-loop` 变更，协调器再创建一个小的调度 metadata commit，确保派发下一项前工作区干净。
- 派发或等待状态可以暂留工作区用于恢复，但不得单独提交；只有当前 issue 已完成并通过所有适用门禁后，才允许提交任务 checkpoint 或随后的 metadata checkpoint。
- `progress.md`、`log.md` 或 `scorecard.md` 必须记录当前 issue 的 checkpoint commit hash。
- 只有当前 issue 的文件门禁、验证门禁、Git checkpoint 和工作区状态均满足契约，才能派发下一项。

当前已知 checkpoint：

- issue 13：`118b5163 test: 固化失败扫描保留旧歌红灯用例`
- issue 14：`9f63f09c fix: 修复失败扫描误删旧歌`

历史注意：

- `08d65ff7 chore: 记录 issue 14 派发状态` 只是派发态 metadata commit，不得视为 issue 14 完成 checkpoint；后续不再为派发/等待态单独创建提交。

## 推进门禁

协调器必须重新读取当前 issue 文件，并检查：

- `Status` 是否为 `ready-for-human`。
- 验收标准是否全部勾选。
- `Comments` 是否记录实现摘要。
- `Comments` 是否记录验证命令与结果。
- `Comments` 是否记录对抗式审查结论。
- `Comments` 是否记录 code-review 结论。
- `Comments` 是否记录剩余风险或未完成项。
- 当前 issue 是否已有 Git checkpoint commit hash。
- 派发下一项前工作区是否干净，或是否只剩用户明确声明不属于本批次的无关改动。

只有当前 issue 全部满足门禁，才推进下一个 issue。否则停在当前 issue，并在 `progress.md` 和 `log.md` 记录阻塞原因。

## 完成条件

以下条件全部满足后，批次才算完成：

- issue 13 到 17 均为 `ready-for-human`。
- issue 13 到 17 的验收标准均已全勾。
- issue 13 到 17 的 `Comments` 均满足门禁。
- issue 13 到 17 均有对应 Git checkpoint commit hash。
- issue 17 已记录最终验证和对抗式审查。
- `progress.md`、`log.md` 和 `scorecard.md` 均记录最终状态与验证证据。
