Status: ready-for-agent

# 做视觉验收并更新 issue Comments

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

在所有实现切片完成后，对移动端“我的”页、移动端最近播放页、Desktop/macOS “我的”页和 Desktop/macOS 最近播放页做最终视觉与行为验收，并把实现摘要、验证命令、结果、对抗式审查和剩余风险写入相关 issue 的 Comments。

## 验收标准

- [ ] 移动端“我的”页对照 Figma 节点 `919:439` 检查头像区、统计区、快速功能、最近播放和设置菜单。
- [ ] 移动端最近播放页检查完整列表、空态、播放、高亮和更多菜单。
- [ ] Desktop/macOS “我的”页检查同语义桌面适配，而不是手机稿等比例拉伸。
- [ ] Desktop/macOS 最近播放页检查完整列表、空态、播放、高亮和更多菜单。
- [ ] 相关 issue 的 Comments 包含实现摘要、验证命令与结果、对抗式审查、剩余风险或未完成项。

## 依赖

- 01-figma-static-avatar-resource.md
- 02-mobile-me-remove-old-title-login-card.md
- 03-mobile-me-avatar-outline-edit-badge.md
- 04-mobile-me-stats-song-playlist-duration.md
- 05-mobile-me-scan-music-entry.md
- 06-mobile-me-static-settings-menu.md
- 07-recent-played-song-list-filtering.md
- 08-recent-played-secondary-route.md
- 09-mobile-recent-played-page-empty-skeleton.md
- 10-mobile-me-recent-played-summary-skeleton.md
- 11-mobile-me-recent-played-summary-real-top3.md
- 12-mobile-me-view-all-recent-played-navigation.md
- 13-mobile-recent-played-page-full-list.md
- 14-mobile-summary-play-uses-full-recent-queue.md
- 15-mobile-recent-page-play-uses-full-queue.md
- 16-mobile-recent-played-row-playing-feedback.md
- 17-mobile-recent-played-row-more-menu.md
- 18-desktop-me-new-profile-structure.md
- 19-desktop-me-stats-song-playlist-duration.md
- 20-desktop-me-scan-music-entry.md
- 21-desktop-me-static-settings-menu.md
- 22-desktop-me-recent-played-summary.md
- 23-desktop-recent-played-page-list-empty.md
- 24-desktop-recent-played-actions-feedback.md
- 25-navigation-chrome-regression-tests.md
- 26-recent-played-filter-queue-tests.md
