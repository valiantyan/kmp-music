Status: ready-for-agent

# 移动端摘要区只显示最近 3 条真实歌曲

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

把移动端“我的”页最近播放摘要区接入过滤后的最近播放歌曲列表，并且最多只显示最新 3 条。摘要区不使用 demo 数据，也不展示不可解析或不可播放的历史歌曲。

## 验收标准

- [ ] 最近播放摘要最多显示 3 条歌曲。
- [ ] 摘要歌曲来自统一过滤后的最近播放歌曲列表。
- [ ] 摘要不显示 demo 歌曲、不显示全库歌曲，也不显示不可播放历史项。
- [ ] 最近播放为空时继续显示空态文案。
- [ ] 本切片不要求完成点击播放和更多菜单行为。

## 依赖

- 07-recent-played-song-list-filtering.md
- 10-mobile-me-recent-played-summary-skeleton.md
