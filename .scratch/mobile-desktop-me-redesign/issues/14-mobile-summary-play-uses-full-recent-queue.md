Status: ready-for-agent

# 移动端摘要点击歌曲使用完整最近播放队列

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

让移动端“我的”页最近播放摘要中的歌曲可点击播放。即使摘要只显示 3 条，点击任意摘要歌曲时也必须把完整最近播放歌曲列表作为播放队列，并以被点击歌曲作为播放起点。

## 验收标准

- [ ] 点击摘要区任意歌曲会播放该歌曲。
- [ ] 播放队列使用完整最近播放歌曲列表，不只是可见 3 条。
- [ ] 播放起点是被点击的歌曲。
- [ ] 队列不包含不可解析、已移除或不可播放歌曲。
- [ ] 不改变其它页面歌曲列表的播放行为。
- [ ] 更新播放行为测试，断言摘要点击传入的是完整最近播放队列。

## 依赖

- 07-recent-played-song-list-filtering.md
- 11-mobile-me-recent-played-summary-real-top3.md
