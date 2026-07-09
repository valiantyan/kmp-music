# 让 Agent 修复 GitHub Issue 使用指南

本文面向项目维护者，说明当 GitHub 上已经有人提交 BUG 后，如何让 Agent 按项目流程修复问题、执行测试、提交代码、push、回写修复日志并关闭 GitHub Issue。

## 前置条件

开始前请确认：

- GitHub Issue 已经创建，例如 `https://github.com/valiantyan/kmp-music/issues/1`。
- Issue 至少包含问题现象、复现步骤、期望行为、实际行为和验收标准。
- 本地仓库位于项目根目录 `/Users/yanhao/Desktop/demo/kmp-music`。
- 如果要求 Agent 自动评论或关闭 Issue，GitHub CLI 必须已登录，并且有读取、评论、打标签、关闭 issue 和 push 代码的权限。

可用下面命令检查 GitHub CLI 登录状态：

```bash
gh auth status
```

如果没有登录，请先执行：

```bash
gh auth login
```

如果不想让 Agent 直接操作 GitHub，也可以只让 Agent 完成修复和本地提交；随后由人工把修复日志复制到 GitHub Issue 并关闭。只要启动指令包含“自动回写评论”或“关闭 Issue”，Agent 就应该先检查 `gh auth status`，未登录时应先停住。

## 推荐启动方式

在 Codex 中直接发送类似下面的指令：

```text
修复 GitHub Issue #1：https://github.com/valiantyan/kmp-music/issues/1

请按 AGENTS.md 和 docs/agents/github-bug-flow.md 执行：
1. 先检查 gh auth status，确认可以回写评论和关闭 issue。
2. 读取 GitHub Issue 和评论。
3. 下载并检查 Issue 正文和评论中的所有附件，保存到 .scratch/github-bugs/assets/1/。
4. 如果信息足够，把它镜像到 .scratch/github-bugs/issues/，并记录附件证据。
5. 从第一性原理分析问题根本原因、复现或定位问题。
6. 补充或更新回归测试。
7. 做最小根治修复。
8. 运行匹配的验证命令。
9. 做 code review 和对抗式审查。
10. 更新本地镜像 issue 的验收、验证、审查和剩余风险。
11. 中文提交并 push。
12. 回写 GitHub 修复日志，评论必须包含问题原因、解决方案、影响范围。
13. 验证通过且评论成功后关闭 GitHub Issue。
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
2. 如果用户要求自动评论或关闭 Issue，先运行 `gh auth status`；未登录时停止。
3. 使用 `gh issue view <编号> --comments` 读取 GitHub Issue。
4. 提取 Issue 正文和评论中的附件链接。
5. 用 `GET` 下载所有附件，保存到 `.scratch/github-bugs/assets/<编号>/`；不能用 `HEAD` 判断附件不可下载。
6. 判断信息和附件证据是否足够。
7. 信息不足或关键附件无法下载时，在 GitHub Issue 评论需要补充的内容，并停止修复。
8. 信息足够时，在 `.scratch/github-bugs/issues/` 创建或更新本地镜像 issue，并记录附件证据。
9. 读取 `AGENTS.md`、`docs/PRD.md` 和相关源码。
10. 优先补充或更新能暴露 BUG 的回归测试。
11. 实现最小根治修复。
12. 运行与改动范围匹配的验证命令。
13. 做 code review 和对抗式审查。
14. 更新本地镜像 issue 的 `Status:`、验收标准和 `Comments`。
15. 创建中文提交。
16. push 当前分支。
17. 在 GitHub Issue 回写修复日志，评论必须包含问题原因、解决方案、影响范围。
18. 验证全部通过且评论成功后关闭 GitHub Issue。

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
- 附件证据：原始 URL、本地路径、文件类型、文件大小、下载结果、检查结论。
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

## 附件处理规则

Agent 必须处理 Issue 正文和评论中的附件。常见附件链接包括：

- `https://github.com/user-attachments/assets/...`
- `https://user-images.githubusercontent.com/...`
- Markdown 图片、视频、日志文件或其他可下载链接

附件保存位置：

```text
.scratch/github-bugs/assets/<issue-number>/
```

推荐下载命令：

```bash
curl -L --fail --output ".scratch/github-bugs/assets/<编号>/<文件名>" "<附件 URL>"
```

注意：GitHub 附件可能跳转到短期 S3 签名地址，`HEAD` 请求可能返回 403，但 `GET` 仍然可以下载。因此 Agent 不能用 `curl -I` 或 `HEAD` 结果作为附件不可下载的最终判断。

下载完成后，Agent 应记录：

- 原始 URL。
- 本地保存路径。
- 文件类型和大小。
- 附件内容检查结论。
- 是否影响根因判断。

原始图片、视频或日志附件默认不提交到 git；除非用户明确要求，只提交本地镜像 issue 中的文字证据。

## GitHub 修复日志格式

Agent 回写 GitHub Issue 时，建议使用下面格式：

```markdown
已修复。

## 根因

<问题为什么发生>

## 解决方案

<改了什么，为什么这样改>

## 影响范围

<影响哪些平台、模块、用户行为；明确不影响哪些范围；写出剩余风险>

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
- 如果 Issue 有附件，附件已下载并检查，或已记录下载失败原因和补充需求。
- 本地镜像 issue 的 `Status:` 已更新为 `ready-for-human`。
- 本地镜像 issue 的验收标准已全部勾选，或未完成项有明确说明。
- 本地镜像 issue 的 `Comments` 包含实现摘要、验证结果、code review、对抗式审查和剩余风险。
- 相关测试或编译验证通过。
- 修复提交已经创建。
- push 成功。
- GitHub Issue 已回写修复日志，且评论包含问题原因、解决方案、影响范围。

任一门禁失败，Agent 都不能关闭 GitHub Issue。

## 常见问题

### GitHub CLI 没登录怎么办？

如果 Agent 执行 `gh issue view <编号> --comments` 时提示未登录，需要维护者先执行：

```bash
gh auth login
```

登录完成后，再让 Agent 继续修复。

如果修复要求自动评论或关闭 Issue，Agent 必须在改代码前发现这个问题并停止，不能等到 push 后才发现无法回写。

### 附件下载失败怎么办？

Agent 应先确认自己使用的是 `GET` 下载，而不是 `HEAD` 检查。如果 `GET` 仍失败，应在本地镜像 issue 记录失败命令和错误原因；如果附件对根因判断关键，应停止修复并要求补充可访问附件。

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
按 docs/agents/github-bug-flow.md 执行；先检查 gh 权限，下载并检查所有附件，验证通过后提交、push，回写包含问题原因、解决方案、影响范围的修复日志并关闭 issue。
```
