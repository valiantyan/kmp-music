Status: ready-for-agent

# 确认或补齐导航与 chrome 回归测试

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

确认“最近播放页”二级页面语义、“我的”页进入最近播放页、返回行为，以及底部 Tab 和全局迷你播放器不被破坏已经有回归测试覆盖；发现缺口时补齐。测试应关注用户可感知的导航和 chrome 行为。

## 验收标准

- [ ] 确认或补齐移动端从“我的”进入“最近播放页”的测试。
- [ ] 确认或补齐从“最近播放页”返回“我的”页的测试。
- [ ] 确认或补齐“最近播放页”是普通二级页面语义的测试。
- [ ] 确认或补齐底部 Tab 行为不因本需求改变的测试。
- [ ] 确认或补齐全局迷你播放器策略不因本需求改变的测试。

## 依赖

- 08-recent-played-secondary-route.md
- 12-mobile-me-view-all-recent-played-navigation.md
- 13-mobile-recent-played-page-full-list.md
- 23-desktop-recent-played-page-list-empty.md
