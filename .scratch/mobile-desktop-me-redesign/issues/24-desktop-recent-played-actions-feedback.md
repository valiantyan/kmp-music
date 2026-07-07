Status: ready-for-human

# Desktop 最近播放列表接入播放、更多菜单和高亮

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

让 Desktop/macOS 最近播放摘要和最近播放页中的歌曲行支持点击播放、单曲更多菜单、当前播放红色高亮和播放中辅助标识。播放队列必须使用完整最近播放歌曲列表。

## 验收标准

- [x] 点击 Desktop/macOS 最近播放摘要歌曲可以播放该歌曲。
- [x] 点击 Desktop/macOS 最近播放页歌曲可以播放该歌曲。
- [x] 播放队列使用完整最近播放歌曲列表。
- [x] 当前播放歌曲在 Desktop/macOS 最近播放列表中标题变红，并保留播放中辅助标识。
- [x] 歌曲行三点按钮打开现有单曲更多菜单。
- [x] 补充或复用测试验证 Desktop/macOS 最近播放点击使用完整队列；如果没有现成测试边界，在 Comments 记录人工验证方式。

## 依赖

- 23-desktop-recent-played-page-list-empty.md

## Comments

### 实现摘要

- Desktop/macOS “我的”页最近播放摘要行新增点击播放、三点更多按钮、当前播放红色标题和“播放中”辅助标识。
- Desktop/macOS 最近播放完整页表格行新增点击播放、三点更多按钮、当前播放行红色标题、红色播放指示和“播放中”辅助标识。
- 桌面根路由和桌面二级路由都复用既有 `controller.playRecentSong`，因此摘要 Top3 和完整页点击都会由 controller 从完整 `state.recentSongs` 读取播放队列。
- 三点按钮复用既有 `controller.openMore` 和全局单曲更多面板解析链；未新增桌面专属菜单、管理页或歌曲操作模型。
- 未修改登录页、登录路由、底部 Tab、全局迷你播放器、原型目录、后端、账号、持久化表或最近播放管理能力。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.desktop.screens.DesktopMeScreenTest --tests com.yanhao.kmpmusic.feature.desktop.screens.DesktopRecentPlayedScreenTest --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playDesktopRecentSongUsesFullRecentQueueWithClickedStart`：通过；新增桌面展示模型测试覆盖摘要/完整页播放入口、更多入口、当前播放标识和桌面点击使用完整最近播放队列。
- `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`：通过；确认 `commonMain` Android 编译和完整 Desktop/common 测试无回归。
- `git diff --check`：通过，没有补丁空白问题。
- 验证输出仍包含既有 Gradle deprecated property 提示；聚焦测试首次编译时仍提示 `MusicAppControllerTest.kt` 两处既有 `No cast needed` 警告，本切片未修改相关位置。

### Code review 结论

- Spec 轴通过：六项验收标准均已满足；桌面摘要和完整页歌曲行均可播放、可打开既有更多菜单，并能根据 `currentSongId` 显示红色标题与“播放中”辅助标识。
- 队列轴通过：桌面 UI 只传被点击歌曲，队列选择统一留在 `MusicAppController.playRecentSong`；新增测试断言点击 Top3 内歌曲时，播放队列仍等于完整最近播放歌曲列表。
- Standards 轴通过：改动集中在 Desktop 最近播放摘要、完整页、路由和对应测试；没有引入平台 API、重复菜单、后端、账号、持久化、清空、编辑、筛选、排序或审计历史能力。
- 回归轴通过：未修改登录页、登录路由、底部 Tab、全局迷你播放器、移动端页面或 `prototypes/kmp-music-hi-fi`；`.agent-loop/*` 仍为分发会话运行状态，未纳入本切片改动。

### 对抗式审查

- 风险一：摘要点击错误使用 Top3 队列。复核结果：`DesktopRootScreenRoute` 传入 `controller.playRecentSong`，controller 从完整 `uiState.recentSongs` 取队列；新增 `playDesktopRecentSongUsesFullRecentQueueWithClickedStart` 覆盖点击第 2 首时队列仍为完整 5 首。
- 风险二：完整页点击错误使用页面局部列表或重新拼队列。复核结果：完整页也只调用 `controller.playRecentSong`；展示层不传 `List<Song>`，不读取历史、全库或 demo 数据。
- 风险三：当前播放高亮只改摘要或只改完整页。复核结果：`DesktopMeRecentPlayedSummaryDisplayModel` 和 `DesktopRecentPlayedPageDisplayModel` 都接收 `currentSongId`，测试覆盖只标记命中歌曲。
- 风险四：三点按钮新建重复菜单。复核结果：按钮只调用 `controller.openMore`，更多面板仍由既有 `AppPanels` 渲染和解析。
- 风险五：破坏 issue 23 的完整列表和空态。复核结果：完整页仍使用 `state.recentSongs` 全量入参、保留空态和 `hasManagementActions=false`；新增动作没有引入清空、编辑、筛选、排序或审计入口。

### 剩余风险或未完成项

- 无未完成验收项。
- 剩余视觉风险：本切片未启动真实 Desktop App 做截图或手工点击核对；已通过桌面展示模型测试、controller 队列测试、完整 `desktopTest`、Android Kotlin 编译和静态 diff 复核覆盖主要行为风险。
