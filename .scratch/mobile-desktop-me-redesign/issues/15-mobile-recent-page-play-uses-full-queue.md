Status: ready-for-agent

# 移动端最近播放页点击歌曲使用完整队列

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

让移动端“最近播放页”中的歌曲可点击播放。点击任意歌曲时，播放队列必须使用完整最近播放歌曲列表，并以被点击歌曲作为播放起点。

## 验收标准

- [ ] 点击最近播放页任意歌曲会播放该歌曲。
- [ ] 播放队列使用完整最近播放歌曲列表。
- [ ] 播放起点是被点击的歌曲。
- [ ] 队列不包含不可解析、已移除或不可播放歌曲。
- [ ] 不新增播放日志管理、清空、编辑或排序能力。
- [ ] 更新播放行为测试，断言最近播放页点击传入的是完整最近播放队列。

## 依赖

- 07-recent-played-song-list-filtering.md
- 13-mobile-recent-played-page-full-list.md
