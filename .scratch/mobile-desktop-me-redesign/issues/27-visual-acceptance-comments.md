Status: ready-for-human

# 做视觉验收并更新 issue Comments

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

在所有实现切片完成后，对移动端“我的”页、移动端最近播放页、Desktop/macOS “我的”页和 Desktop/macOS 最近播放页做最终视觉与行为验收，并把实现摘要、验证命令、结果、对抗式审查和剩余风险写入相关 issue 的 Comments。

## 验收标准

- [x] 移动端“我的”页对照 Figma 节点 `919:439` 检查头像区、统计区、快速功能、最近播放和设置菜单。
- [x] 移动端最近播放页检查完整列表、空态、播放、高亮和更多菜单。
- [x] Desktop/macOS “我的”页检查同语义桌面适配，而不是手机稿等比例拉伸。
- [x] Desktop/macOS 最近播放页检查完整列表、空态、播放、高亮和更多菜单。
- [x] 相关 issue 的 Comments 包含实现摘要、验证命令与结果、对抗式审查、剩余风险或未完成项。

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

## Comments

### 实现摘要

- 使用 Figma Desktop MCP 拉取并截图节点 `919:439`，确认移动端基准结构是头像资料区、三项统计、快速功能、最近播放三条和三行设置菜单。
- 对生产源码做最终验收时发现一个阻塞视觉结构缺陷：移动端和 Desktop/macOS “我的”页虽然已经加入新结构，但仍在新结构后保留旧的“我的收藏”“常听歌手”“本地文件夹”“最近播放的专辑”等旧个人中心区块。该残留会让移动端不再贴合 Figma 节点 `919:439`，也会让桌面端超出 PRD 的同语义结构。
- 已做最小修正：移动端 `MeScreen` 只保留头像资料区、统计区、快速功能、最近播放摘要和设置菜单；Desktop/macOS `DesktopMeRootScreen` 只保留个人资料头、统计区、扫描入口、最近播放摘要和静态设置菜单；路由同步删除这些页面不再需要的旧收藏、歌手、文件夹和最近专辑回调。
- 移动端最近播放页仍通过 `SecondaryScreen.RecentPlayed` 渲染完整 `state.recentSongs`，空态、播放、高亮和更多菜单由 `RecentPlayedScreen`、`RecentPlayedSongRowDisplayModel` 与 `controller.playRecentSong` / `controller.openMore` 保持统一。
- Desktop/macOS 最近播放页仍使用 workspace 表格展示完整 `state.recentSongs`，空态、播放、高亮和更多菜单由 `DesktopRecentPlayedScreen`、`DesktopRecentPlayedPageDisplayModel`、`DesktopRecentPlayedSongTable` 与 `controller.playRecentSong` / `controller.openMore` 保持统一。
- 未修改登录页、登录路由、底部 Tab、全局迷你播放器、后端、账号、持久化表、原型目录或 `.agent-loop/*`。

### 验证命令与结果

- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，`BUILD SUCCESSFUL in 13s`。这是修正路由和共享 UI 签名后的首次 Android 编译验证。
- `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`：通过，`BUILD SUCCESSFUL in 9s`。覆盖 Android Kotlin 编译、Desktop/common 测试和最近播放相关展示模型、controller 队列、过滤与导航测试。
- `git diff --check`：通过，无空白错误。
- `rg -n "我的收藏|常听歌手|本地文件夹|最近播放的专辑|登录音乐账号|立即登录" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/MeScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens/DesktopMeScreen.kt`：无匹配，确认目标“我的”页生产入口不再残留旧登录或旧个人中心区块文案。
- `rg -n "^(Status:|## Comments|### |[-] \\[[ x]\\])" .scratch/mobile-desktop-me-redesign/issues`：确认前序 issue 01-26 均为 `ready-for-human`，并包含 Comments、验证、Code review、对抗式审查和剩余风险记录；issue 27 本文件也已补齐这些记录。
- Figma Desktop MCP `get_design_context` 与 `get_screenshot` 针对节点 `919:439`：通过，截图内容显示头像、用户名“高保真听众”、副标题“音乐是我的灵魂”、统计 `1240 / 12 / 365`、快速功能“扫描音乐”、最近播放三条、三行设置菜单。

### Code review 结论

- Standards 轴通过。改动集中在生产 App 的移动端/桌面端“我的”页和根路由签名，保持 `commonMain` 共享 UI 边界，没有引入平台 API、后端、持久化、账号、登录、原型目录或全局 chrome 改动。
- Spec 轴通过。移动端“我的”页现在按 Figma 节点 `919:439` 的内容顺序收口；Desktop/macOS “我的”页保留桌面 workspace 的横向资料头、等权统计卡和桌面扫描入口，没有把手机稿等比例拉伸，也不再追加旧个人中心内容。
- 最近播放行为通过。移动端摘要、移动端完整页、桌面摘要和桌面完整页都消费统一过滤后的 `state.recentSongs`；点击歌曲统一走 `controller.playRecentSong`，因此播放队列使用完整最近播放歌曲列表；三点入口统一走 `controller.openMore`。
- 测试覆盖通过。`MusicAppControllerTest` 覆盖移动端摘要、移动端完整页和桌面最近播放点击使用完整队列；`MusicAppLibraryStateSynchronizerTest` 覆盖不可解析、已移除或不可播放历史过滤；`MeScreenTest`、`RecentPlayedScreenTest`、`DesktopMeScreenTest`、`DesktopRecentPlayedScreenTest` 覆盖 Top3、完整列表、空态、当前播放标识、更多入口和桌面 workspace 表格策略。

### 对抗式审查

- 风险一：视觉验收只看前序 issue，漏掉真实页面仍有旧内容。复核结果：本次源码复核实际发现并删除移动端和桌面端旧个人中心残留，随后用 `rg` 确认目标页面不再包含旧文案。
- 风险二：把移动端 Figma 稿硬套到桌面端。复核结果：桌面端仍使用 `DesktopProfileHeader`、`DesktopMeStatsRow`、`DesktopMeQuickActions`、`DesktopMeRecentPlayedSummary` 和 `DesktopMeStaticSettingsMenu`，保留桌面 workspace 横向/卡片布局与 `DesktopRecentPlayedLayoutPolicy.WorkspaceTable`。
- 风险三：最近播放摘要点击只使用 Top3 队列。复核结果：移动端和桌面端歌曲点击都调用 `controller.playRecentSong`，测试断言队列等于完整过滤后的最近播放列表。
- 风险四：遗漏空态、播放、高亮或更多菜单。复核结果：移动端和桌面端完整页展示模型均有空态文案、当前播放 `playingIndicatorLabel == "播放中"`、播放动作和 `hasMoreAction` 测试；源码行尾三点按钮均调用既有更多面板入口。
- 风险五：误改登录、底部 Tab、全局迷你播放器、后端或原型目录。复核结果：diff 仅触及 `MeScreen`、`MobileRootScreenRoute`、`DesktopMeScreen`、`DesktopRootScreenRoute` 和本 issue 文件；`.agent-loop/*` 保持分发会话运行态未纳入本切片修改。

### 剩余风险或未完成项

- 无未完成验收项。
- 剩余视觉风险：本次未启动 Android 真机/模拟器或 Desktop App 做运行态截图与手工点击；原因是当前任务会话以源码、Figma 节点截图、路由/展示模型复核和 Gradle 验证完成最终验收记录，未建立设备或桌面 GUI 截图流程。因此像素级间距、真实滚动状态和真实点击反馈仍建议由人工在设备或 Desktop App 上做最后目测。
- 验证输出仍包含既有 Gradle deprecated property 提示，和本切片无关；构建与测试结果均为通过。
