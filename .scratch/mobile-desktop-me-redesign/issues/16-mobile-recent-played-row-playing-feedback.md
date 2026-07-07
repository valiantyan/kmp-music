Status: ready-for-agent

# 移动端最近播放行接入红色高亮和播放中标识

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

让移动端最近播放摘要和最近播放页中的歌曲行接收当前播放歌曲信息。当前播放歌曲标题应显示红色，并保留现有播放中辅助标识，保持与其它歌曲列表一致的全局播放反馈。

## 验收标准

- [ ] 当前播放歌曲在移动端最近播放摘要中标题变红。
- [ ] 当前播放歌曲在移动端最近播放页中标题变红。
- [ ] 当前播放歌曲在最近播放行中保留播放中辅助标识。
- [ ] 非当前播放歌曲不错误显示红色或播放中标识。
- [ ] 不破坏首页、收藏、专辑详情或歌手详情已有播放高亮规则。
- [ ] 通过可测试边界验证当前播放歌曲标识会传入最近播放摘要和最近播放页；如果没有现成测试边界，在 Comments 记录人工验证方式。

## 依赖

- 11-mobile-me-recent-played-summary-real-top3.md
- 13-mobile-recent-played-page-full-list.md
