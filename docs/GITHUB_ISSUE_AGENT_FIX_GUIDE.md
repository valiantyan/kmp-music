# 让 Agent 修复 GitHub Issue 使用指南

本文面向项目维护者，说明当 GitHub 上已经有人提交 BUG 后，如何让 Agent 按项目流程修复问题、执行测试、提交代码、push、回写修复日志并关闭 GitHub Issue。

## 前置条件

开始前请确认：

- GitHub Issue 已经创建，例如 `https://github.com/valiantyan/kmp-music/issues/1`。
- Issue 至少包含问题现象、复现步骤、期望行为、实际行为和验收标准。
- 本地仓库位于项目根目录 `/Users/yanhao/Desktop/demo/kmp-music`。
- GitHub CLI 已登录，并且有读取、评论、打标签、关闭 issue 和 push 代码的权限。

可用下面命令检查 GitHub CLI 登录状态：

```bash
gh auth status
```

如果没有登录，请先执行：

```bash
gh auth login
```

如果不想让 Agent 直接操作 GitHub，也可以只让 Agent 完成修复和本地提交；随后由人工把修复日志复制到 GitHub Issue 并关闭。

## 推荐启动方式

在 Codex 中直接发送类似下面的指令：

```text
修复 GitHub Issue #1：https://github.com/valiantyan/kmp-music/issues/1

请按 AGENTS.md 和 docs/agents/github-bug-flow.md 执行：
1. 读取 GitHub Issue 和评论。
2. 如果信息足够，把它镜像到 .scratch/github-bugs/issues/。
3. 复现或定位问题。
4. 补充或更新回归测试。
5. 做最小根治修复。
6. 运行匹配的验证命令。
7. 做 code review 和对抗式审查。
8. 更新本地镜像 issue 的验收、验证、审查和剩余风险。
9. 中文提交并 push。
10. 回写 GitHub 修复日志。
11. 验证通过后关闭 GitHub Issue。
```

如果希望 Agent 先不要提交或关闭 issue，可以这样说：

```text
分析并修复 GitHub Issue #1，但先不要 commit、push 或关闭 issue。完成后告诉我改动、验证结果和建议的修复日志。
```

如果只想让 Agent 判断信息是否足够，可以这样说：

```text
检查 GitHub Issue #1 是否已经足够交给 Agent 修复。如果不足，请列出需要补充的信息，不要改代码。
```

## Agent 应该执行的流程

收到修复指令后，Agent 应按这个顺序推进：

1. 查看工作区状态，避免覆盖未提交改动。
2. 使用 `gh issue view <编号> --comments` 读取 GitHub Issue。
3. 判断信息是否足够。
4. 信息不足时，在 GitHub Issue 评论需要补充的内容，并停止修复。
5. 信息足够时，在 `.scratch/github-bugs/issues/` 创建或更新本地镜像 issue。
6. 读取 `AGENTS.md`、`docs/PRD.md` 和相关源码。
7. 优先补充或更新能暴露 BUG 的回归测试。
8. 实现最小根治修复。
9. 运行与改动范围匹配的验证命令。
10. 做 code review 和对抗式审查。
11. 更新本地镜像 issue 的 `Status:`、验收标准和 `Comments`。
12. 创建中文提交。
13. push 当前分支。
14. 在 GitHub Issue 回写修复日志。
15. 验证全部通过后关闭 GitHub Issue。

## 本地镜像 issue 规则

GitHub Issue 不能替代本地 tracker。每个进入修复流程的 GitHub BUG，都应该镜像到：

```text
.scratch/github-bugs/issues/
```

文件名建议：

```text
<github-issue-number>-<short-slug>.md
```

例如：

```text
.scratch/github-bugs/issues/1-mini-player-open-player-page.md
```

本地镜像 issue 至少应包含：

- GitHub Issue 链接和编号。
- 问题现象。
- 复现步骤。
- 期望行为。
- 实际行为。
- 验收标准。
- 修复计划。
- 实现摘要。
- 验证命令与结果。
- code review 结论。
- 对抗式审查。
- 剩余风险。

## 验证命令选择

Agent 应根据改动范围选择验证命令：

```bash
./gradlew :composeApp:desktopTest
```

适用于共享逻辑、控制器、状态、用例、repository 等改动。

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

适用于 UI、Android 编译边界、commonMain 编译风险等改动。

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest
```

适用于同时触及共享状态和 Android 编译风险的改动。

如果任务名不确定，先运行：

```bash
./gradlew :composeApp:tasks
```

## GitHub 修复日志格式

Agent 回写 GitHub Issue 时，建议使用下面格式：

```markdown
已修复。

## 根因

<问题为什么发生>

## 解决方案

<改了什么，为什么这样改>

## 验证

- `<命令>`：通过
- `<命令>`：通过

## 对抗式审查

- <风险一及复核结果>
- <风险二及复核结果>
- <风险三及复核结果>

## 提交

- <commit hash>

## 剩余风险

<没有则写“无已知剩余风险”；有则写清楚影响范围>
```

## 完成门禁

Agent 只有在以下条件全部满足时，才能关闭 GitHub Issue：

- GitHub Issue 信息已经镜像到本地 issue。
- 本地镜像 issue 的 `Status:` 已更新为 `ready-for-human`。
- 本地镜像 issue 的验收标准已全部勾选，或未完成项有明确说明。
- 本地镜像 issue 的 `Comments` 包含实现摘要、验证结果、code review、对抗式审查和剩余风险。
- 相关测试或编译验证通过。
- 修复提交已经创建。
- push 成功。
- GitHub Issue 已回写修复日志。

任一门禁失败，Agent 都不能关闭 GitHub Issue。

## 常见问题

### GitHub CLI 没登录怎么办？

如果 Agent 执行 `gh issue view <编号> --comments` 时提示未登录，需要维护者先执行：

```bash
gh auth login
```

登录完成后，再让 Agent 继续修复。

### Issue 信息不足怎么办？

Agent 应停止改代码，向 GitHub Issue 评论需要补充的信息，并标记为 `needs-info`。不要靠猜测修复。

### 可以让 Agent 只修复不关闭 Issue 吗？

可以。启动指令里明确写：

```text
先不要关闭 GitHub Issue，完成修复和验证后等待我确认。
```

### 可以让 Agent 直接修复当前这个 Issue #1 吗？

可以。直接发送：

```text
修复 GitHub Issue #1：https://github.com/valiantyan/kmp-music/issues/1
按 docs/agents/github-bug-flow.md 执行，验证通过后提交、push、回写修复日志并关闭 issue。
```
