Status: ready-for-human

# 新增“最近播放页”二级路由语义

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

新增“最近播放页”的普通二级页面路由语义，让移动端和 Desktop/macOS 都能从“我的”页进入完整最近播放歌曲列表。这个切片只建立路由和页面入口承载，不实现完整列表播放行为。

## 验收标准

- [x] 导航模型包含“最近播放页”二级页面语义。
- [x] “最近播放页”被归类为普通二级页面，不是播放器 overlay。
- [x] 移动端进入该页面时复用现有二级页面返回语义。
- [x] Desktop/macOS 可以在 workspace 中承载该页面语义。
- [x] 不修改底部 Tab 和全局迷你播放器的既有策略。
- [x] 更新导航模型测试，覆盖路由名、返回栈和普通二级页分类。

## 依赖

无，可以立即开始

## Comments

### 实现摘要

- 在共享导航模型新增 `SecondaryScreen.RecentPlayed`，稳定路由名为 `RecentPlayed`。
- 将“最近播放页”归类为 `MobileFixedBarMode.SecondaryWithMiniPlayer`，因此移动端隐藏底部 Tab、保留全局迷你播放器，并且 `chromeOverlayScreen` 为 `null`，不会走播放器 overlay。
- 在 `MusicAppController` 增加 `openRecentPlayed()` 薄入口，后续“查看全部”切片可直接复用。
- 移动端 `MobileSecondaryScreenRoute` 和 Desktop/macOS `DesktopSecondaryScreenRoute` 增加最小占位承载，保证两端 route 可编译、可渲染；完整空态、列表、播放队列和更多菜单留给后续 issue。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest`：通过。
- `./gradlew :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid`：通过。
- `git diff --check`：通过，无空白错误。

### Code Review 结论

- Standards：未发现违反本仓库导航/chrome 归类规则的问题；新增路由集中放在 `SecondaryScreen`、`mobileFixedBarModeFor` 和 route 分发处，没有在底部 Tab 或迷你播放器周围散写判断。
- Spec：验收项均已覆盖；本切片只建立路由语义和最小承载，没有实现 issue 09 及后续的空态、完整列表、播放队列或更多菜单行为。

### 对抗式审查

- 风险 1：把最近播放页误归为播放器 overlay。已通过 `fixedBarMode == SecondaryWithMiniPlayer` 和 `chromeOverlayScreen == null` 测试覆盖。
- 风险 2：把最近播放页误做成 root tab。实现只新增 `SecondaryScreen.RecentPlayed`，未修改 `RootTab` 或底部 Tab。
- 风险 3：破坏全局 chrome。未改 `MobileFixedBarMode.TopLevel`、`Player`、`SecondaryWithoutChrome` 或全局迷你播放器组件；验证覆盖 Android 编译和 desktopTest。
- 风险 4：提前实现后续列表行为。当前移动端和桌面端仅使用占位承载，没有接入歌曲列表、队列、播放或更多菜单。

### 剩余风险或未完成项

- 未进行视觉截图验证，因为本切片只提供路由占位，不交付最终最近播放页 UI。
- “我的”页“查看全部”入口、移动端空态、完整列表、播放队列和 Desktop 完整列表仍由后续 issue 实现。
