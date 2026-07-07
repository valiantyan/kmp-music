# 长跑 Agent Harness 契约

## 当前状态

- 状态: active
- 批次名称: mobile-desktop-me-redesign
- 模式: 顺序批次薄分发器
- 建立时间: 2026-07-07 17:55 CST
- 工作区: `/Users/yanhao/Desktop/demo/kmp-music`
- PRD: `/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md`
- Issues 目录: `/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/issues`
- 入口规则: `AGENTS.md`
- Harness 规则: `/Users/yanhao/Downloads/qinglilaji /.agents/skills/long-running-loop/SKILL.md`

## 目标

按顺序完成 `mobile-desktop-me-redesign` 批次的 27 个 issue。每个 issue 必须由独立任务会话完成实现、最小可靠验证、code review、对抗式审查、issue 文件证据更新和对应 Git checkpoint。分发会话只负责派发、等待、记录最小恢复状态和执行轻量门禁。

## 队列

1. `01-figma-static-avatar-resource.md`
2. `02-mobile-me-remove-old-title-login-card.md`
3. `03-mobile-me-avatar-outline-edit-badge.md`
4. `04-mobile-me-stats-song-playlist-duration.md`
5. `05-mobile-me-scan-music-entry.md`
6. `06-mobile-me-static-settings-menu.md`
7. `07-recent-played-song-list-filtering.md`
8. `08-recent-played-secondary-route.md`
9. `09-mobile-recent-played-page-empty-skeleton.md`
10. `10-mobile-me-recent-played-summary-skeleton.md`
11. `11-mobile-me-recent-played-summary-real-top3.md`
12. `12-mobile-me-view-all-recent-played-navigation.md`
13. `13-mobile-recent-played-page-full-list.md`
14. `14-mobile-summary-play-uses-full-recent-queue.md`
15. `15-mobile-recent-page-play-uses-full-queue.md`
16. `16-mobile-recent-played-row-playing-feedback.md`
17. `17-mobile-recent-played-row-more-menu.md`
18. `18-desktop-me-new-profile-structure.md`
19. `19-desktop-me-stats-song-playlist-duration.md`
20. `20-desktop-me-scan-music-entry.md`
21. `21-desktop-me-static-settings-menu.md`
22. `22-desktop-me-recent-played-summary.md`
23. `23-desktop-recent-played-page-list-empty.md`
24. `24-desktop-recent-played-actions-feedback.md`
25. `25-navigation-chrome-regression-tests.md`
26. `26-recent-played-filter-queue-tests.md`
27. `27-visual-acceptance-comments.md`

## 分发规则

- 每次只派发当前 issue，不并发派发后续依赖 issue。
- 分发会话不得直接实现业务代码，不运行大验证命令，不替任务会话 stage 或 commit。
- 任务会话负责实现、验证、code review、对抗式审查、更新 issue 文件为 `ready-for-human`，并创建当前 issue 的 Git checkpoint。
- `已派发`、`等待实现`、`等待线程返回` 等状态只能写入 `.agent-loop` 作为恢复记录，不能作为任务完成证据，也不能单独提交为 issue checkpoint。
- metadata checkpoint 只能跟随已通过门禁的 issue checkpoint，用于固化提交哈希、评分、日志、最终归档或最终状态。

## 每个 issue 的门禁

任务会话返回 commit hash 后，分发会话执行轻量门禁：

1. `git cat-file -e <hash>^{commit}` 确认提交存在。
2. `git merge-base --is-ancestor <hash> HEAD` 确认提交可从当前 `HEAD` 追溯。
3. 如果已有上一 issue checkpoint，确认上一 checkpoint 是当前 hash 的祖先，且当前 hash 不是上一 checkpoint 本身。
4. 使用 `git show <hash>:<issue-file>` 读取提交内 issue 文件。
5. 确认提交内 issue 文件 `Status: ready-for-human`。
6. 确认验收项已勾选；如有未勾选项，必须说明原因、影响和剩余风险。
7. 确认 `Comments` 包含实现摘要、验证命令与结果、code review、对抗式审查、剩余风险或未完成项。
8. 确认当前工作区没有该 issue 的未提交遗留改动；只允许分发会话自己的 `.agent-loop` 运行时状态 diff 留在工作区。

任一门禁失败时，停在当前 issue，记录阻塞原因，不派发下一项。

## 验证底线

- 每个 issue 运行与改动范围匹配的最小可靠验证。
- 涉及共享逻辑、导航、播放队列或过滤规则时，优先补充或运行测试。
- 最终至少运行 `./gradlew :composeApp:compileDebugKotlinAndroid` 和 `./gradlew :composeApp:desktopTest`。
- 如果某项验证无法运行，必须在对应 issue `Comments` 和 `.agent-loop/scorecard.md` 记录原因、影响和剩余风险。

## 非目标

- 不修改 `prototypes/kmp-music-hi-fi`。
- 不新增后端、账号、歌单、听歌时长统计、最近播放管理页或持久化表。
- 不删除现有登录页或登录路由。
- 不修改底部 Tab 和全局迷你播放器行为。
- 不把手机 Figma 稿硬套到 Desktop/macOS。
- 不用专辑封面、歌手封面或随机图片冒充 Figma 头像。

## 完成条件

- 01 到 27 全部 `ready-for-human`。
- 每个 issue 均有当前分支可达的 Git checkpoint。
- 每个 issue 的提交内 issue 文件固化验证、审查、对抗式审查和剩余风险证据。
- 最终审查确认 PRD 验收标准未遗漏。
- 最终验证通过，或缺口明确记录。
- 队列全部完成后，将本批次 `.agent-loop` 状态复制到 `.agent-loop/archive/mobile-desktop-me-redesign-<日期>/`。
- 归档目录不复制 `AGENTS.md`，只在归档说明中记录入口规则路径或摘要。
- 活跃 `.agent-loop/contract.md`、`log.md`、`progress.md`、`restart-policy.md`、`scorecard.md` 全部恢复默认 `idle` 内容。

## 契约变更规则

只有用户改变目标、接受新的验收标准，或要求修改运行框架契约时，才修改本文件的完成条件和门禁规则。
