Status: ready-for-agent

# 确认或补齐最近播放过滤与队列行为测试

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

确认最近播放歌曲列表过滤、摘要点击播放队列、最近播放页点击播放队列和当前播放高亮行为已经有测试覆盖；发现缺口时补齐。测试应断言外部可感知行为，不绑定私有 Composable 拆分或局部变量。

## 验收标准

- [ ] 确认或补齐最近播放列表过滤不可解析、已移除或不可播放歌曲的测试。
- [ ] 确认或补齐移动端摘要点击歌曲时使用完整最近播放队列的测试。
- [ ] 确认或补齐移动端最近播放页点击歌曲时使用完整最近播放队列的测试。
- [ ] 确认或补齐 Desktop/macOS 最近播放点击歌曲时使用完整最近播放队列的测试。
- [ ] 确认或补齐当前播放歌曲在最近播放列表中的高亮或播放中反馈测试。

## 依赖

- 07-recent-played-song-list-filtering.md
- 14-mobile-summary-play-uses-full-recent-queue.md
- 15-mobile-recent-page-play-uses-full-queue.md
- 24-desktop-recent-played-actions-feedback.md
