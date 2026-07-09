# GitHub BUG 修复流程

本文定义 GitHub Issue 与本地 `.scratch/.../issues` 的协作方式。GitHub Issue 是外部 BUG 入口、通知和关闭对象；本地 Markdown issue 仍是项目内 agent 执行、验收和审计的主记录。

## 目标

- 保留本地 issue tracker 的可审计任务记录。
- 让 GitHub Issue 能承载用户反馈、修复日志和关闭状态。
- 每个 BUG 修复都有复现信息、根因、改动摘要、验证命令、对抗式审查、剩余风险和提交哈希。
- 测试成功后再提交、push、回写 GitHub Issue 并关闭。

## 角色分工

- GitHub Issue：记录外部报告、讨论、附件、最终修复日志和关闭状态。
- 本地 issue：记录 agent 实施任务、验收标准、验证结果、code review 结论、对抗式审查和剩余风险。
- Git commit：固化代码修复和本地 issue 证据。

## 标签约定

- `bug`：真实或疑似缺陷。
- `needs-triage`：等待确认复现条件、影响范围和是否进入修复。
- `needs-info`：缺少复现、日志、版本或验收标准，暂不能交给 agent 修复。
- `ready-for-agent`：信息足够，已镜像为本地 issue，可以交给 agent 修复。
- `agent-working`：agent 已领取并正在修复。
- `blocked`：修复被外部条件阻塞。
- `ready-for-human`：修复完成，等待人工复核或发布确认。
- `fixed`：修复已提交、push，并已在 GitHub Issue 回写证据。
- `wontfix`：确认不处理。

## 从 GitHub Issue 到本地 issue

GitHub 上提供两个 BUG 入口：

- `BUG 报告`：结构化表单，适合多数用户逐项填写。
- `BUG 报告（Markdown 模板）`：传统 Markdown 模板，适合用户直接在正文里按模板填写。

两个入口都进入同一套修复流程。`问题现象`、`复现步骤`、`期望行为`、`实际行为` 和 `验收标准` 是进入修复前的核心信息；`设备/平台` 与 `截图、录屏或日志` 是推荐信息，但不是必填项。

当用户要求修复 GitHub Issue 时，agent 必须先读取 GitHub Issue：

```bash
gh issue view <编号> --comments
```

如果用户要求修复完成后自动回写评论或关闭 GitHub Issue，agent 必须在改代码前确认 GitHub 写权限：

```bash
gh auth status
```

如果 `gh` 未登录或没有写权限，先停止并要求维护者登录或授权；不要等到修复、提交、push 后才发现无法回写 GitHub。

如果信息不足，agent 不应开始改代码，应在 GitHub Issue 评论缺失项，并将 GitHub Issue 标记为 `needs-info`。

如果信息足够，agent 还必须检查 Issue 正文和评论中的附件链接。附件链接包括但不限于：

- `https://github.com/user-attachments/assets/...`
- `https://user-images.githubusercontent.com/...`
- Markdown 图片、视频、日志文件或其他可下载链接

附件必须用 `GET` 实际下载，不能用 `HEAD` 请求作为附件不可下载的最终判断；GitHub 附件常会跳转到短期 S3 签名地址，`HEAD` 可能返回 403，但 `GET` 仍然可用。

附件默认保存到：

```text
.scratch/github-bugs/assets/<github-issue-number>/
```

推荐下载命令：

```bash
curl -L --fail --output ".scratch/github-bugs/assets/<编号>/<文件名>" "<附件 URL>"
```

如果附件 URL 不包含原始文件名或扩展名，下载后必须根据 `file`、`Content-Type` 或内容检查结果重命名为可审计文件名，例如 `issue-<编号>-<用途>.<扩展名>`，避免只保存为 GitHub asset UUID。

agent 必须在本地镜像 issue 中记录附件清单：原始 URL、本地保存路径、文件类型、文件大小、下载是否成功、检查结论、是否影响根因判断。除非用户明确要求，不要把原始图片、视频、日志附件 stage 或 commit；提交本地镜像 issue 中的文字证据即可。

如果附件下载失败，agent 必须记录失败命令、错误原因和下一步需求。缺少关键附件且无法判断问题时，停止修复并把 GitHub Issue 标记为 `needs-info`，不要靠猜测继续。

完成信息和附件检查后，agent 在 `.scratch/github-bugs/issues/` 下创建或更新镜像 issue。文件名使用：

```text
<github-issue-number>-<short-slug>.md
```

本地镜像 issue 模板：

```markdown
Status: ready-for-agent

# <GitHub Issue 标题>

## GitHub Issue

- 链接：<GitHub Issue URL>
- 编号：#<编号>
- 标签：<关键标签>

## 问题现象

<从 GitHub Issue 摘要问题，不粘贴无关聊天全文>

## 复现步骤

<可执行复现步骤>

## 期望行为

<修复后的目标行为>

## 实际行为

<当前错误行为>

## 附件证据

- 原始 URL：<附件 URL>
- 本地路径：<下载后的本地路径>
- 类型与大小：<文件类型和大小>
- 检查结论：<看到了什么，是否影响根因判断>
- 下载失败：<失败时记录命令、错误和下一步>

## 验收标准

- [ ] <可验证标准一>
- [ ] <可验证标准二>
- [ ] 补充或更新与风险匹配的回归测试，或者说明无法补测的原因。

## 修复计划

<最小、可验证、符合架构边界的修复计划>

## Comments
```

## 修复执行顺序

1. 查看当前工作区状态，避免覆盖用户未提交改动。
2. 如果需要自动回写评论或关闭 Issue，先确认 `gh auth status` 通过。
3. 从 GitHub Issue 和本地镜像 issue 确认复现信息、验收标准和影响范围。
4. 下载并检查 Issue 正文和评论中的所有附件，在本地镜像 issue 记录附件证据；关键附件缺失时停止。
5. 根据 `AGENTS.md` 读取 `docs/PRD.md` 和相关源码。
6. 优先补充或更新能暴露 BUG 的回归测试；如果无法先写测试，在本地 issue 写明原因。
7. 做最小根治修复，保持 `core / domain / data / feature` 分层。
8. 运行与改动范围匹配的验证命令。
9. 做对抗式审查，列出最可能翻车的 3 到 5 个点并复核。
10. 更新本地 issue：验收标准打勾，并在 `Comments` 写入实现摘要、验证命令与结果、code review 结论、对抗式审查、剩余风险。
11. 用中文提交代码；BUG 修复提交信息必须写明问题原因和解决方案。
12. push 当前分支。
13. 在 GitHub Issue 回写修复日志；评论必须包含问题原因、解决方案、影响范围。
14. 如果验证成功、验收标准满足且 GitHub 评论回写成功，关闭 GitHub Issue。

## 推荐验证命令

- Android 编译：`./gradlew :composeApp:compileDebugKotlinAndroid`
- 桌面端测试：`./gradlew :composeApp:desktopTest`
- 共享逻辑和 Android 编译：`./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`
- 不确定任务是否存在时：`./gradlew :composeApp:tasks`

涉及 `MusicAppController`、导航、播放状态、队列、收藏或搜索时，必须更新或新增共享测试，并优先运行对应测试和 `:composeApp:desktopTest`。

## GitHub 修复日志模板

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

## 常用 GitHub CLI 命令

```bash
gh auth status
gh issue view <编号> --comments
gh issue edit <编号> --add-label agent-working --remove-label ready-for-agent
gh issue comment <编号> --body-file <修复日志文件>
gh issue close <编号> --comment "修复已提交并验证通过，详见上方修复日志。"
```

如果 GitHub CLI 未登录或没有权限，agent 应停止在 GitHub 写操作前，并把需要用户执行的命令列出来。

## 完成门禁

关闭 GitHub Issue 前必须满足：

- 如果 Issue 有附件，附件已经用 `GET` 下载或明确记录下载失败原因；本地镜像 issue 已记录附件证据。
- 本地镜像 issue 的 `Status:` 已更新为 `ready-for-human`。
- 本地镜像 issue 的验收标准全部勾选，或未勾选项有明确剩余风险说明。
- 本地镜像 issue 的 `Comments` 包含实现摘要、验证命令与结果、code review 结论、对抗式审查、剩余风险。
- 修复提交在当前分支可达。
- push 成功。
- GitHub Issue 已回写修复日志，且评论包含问题原因、解决方案、影响范围。

任一门禁失败，都不能关闭 GitHub Issue。
