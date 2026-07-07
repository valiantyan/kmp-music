Status: ready-for-agent

# 新增“最近播放页”二级路由语义

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

新增“最近播放页”的普通二级页面路由语义，让移动端和 Desktop/macOS 都能从“我的”页进入完整最近播放歌曲列表。这个切片只建立路由和页面入口承载，不实现完整列表播放行为。

## 验收标准

- [ ] 导航模型包含“最近播放页”二级页面语义。
- [ ] “最近播放页”被归类为普通二级页面，不是播放器 overlay。
- [ ] 移动端进入该页面时复用现有二级页面返回语义。
- [ ] Desktop/macOS 可以在 workspace 中承载该页面语义。
- [ ] 不修改底部 Tab 和全局迷你播放器的既有策略。
- [ ] 更新导航模型测试，覆盖路由名、返回栈和普通二级页分类。

## 依赖

无，可以立即开始
