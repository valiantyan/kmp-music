# Issue tracker：本地 Markdown

本项目的 issue 和 PRD 使用 `.scratch/` 下的 Markdown 文件记录。

GitHub Issue 可以作为外部 BUG 入口，但不能替代本地 Markdown issue。修复 GitHub BUG 时，先把 GitHub Issue 镜像成本地 issue，再按本地 issue 执行、验证和记录证据。完整流程见 `docs/agents/github-bug-flow.md`。

## 约定

- 每个功能一个目录：`.scratch/<feature-slug>/`
- PRD 文件为：`.scratch/<feature-slug>/PRD.md`
- 实现 issue 放在：`.scratch/<feature-slug>/issues/<NN>-<slug>.md`，编号从 `01` 开始
- 分诊状态写在每个 issue 文件顶部附近的 `Status:` 行
- 评论和会话历史追加到文件底部的 `## Comments` 下

## 当 skill 要求“发布到 issue tracker”

在 `.scratch/<feature-slug>/` 下创建新文件；如果目录不存在，先创建目录。

## 当 skill 要求“读取相关 ticket”

读取引用路径指向的文件。用户通常会直接传入文件路径。

## GitHub BUG 镜像

- GitHub BUG 的本地镜像默认放在 `.scratch/github-bugs/issues/`。
- GitHub BUG 的附件证据默认下载到 `.scratch/github-bugs/assets/<github-issue-number>/`，原始附件默认不提交到 git。
- 镜像文件名使用 `<github-issue-number>-<short-slug>.md`。
- 镜像 issue 必须保留 GitHub Issue 链接、编号、问题现象、复现步骤、期望行为、实际行为和验收标准。
- 如果 GitHub Issue 正文或评论包含图片、视频、日志等附件，镜像 issue 必须记录原始 URL、本地路径、文件类型、文件大小、下载结果和检查结论。
- 修复完成后，本地镜像 issue 的 `Status:` 必须更新为 `ready-for-human`，并在 `## Comments` 写入实现摘要、验证命令与结果、code review 结论、对抗式审查和剩余风险。
- GitHub Issue 只能在本地镜像 issue 通过门禁、提交可达、push 成功且已回写包含问题原因、解决方案、影响范围的评论后关闭。
