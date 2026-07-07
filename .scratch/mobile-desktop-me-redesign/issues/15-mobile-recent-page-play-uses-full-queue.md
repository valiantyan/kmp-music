Status: ready-for-human

# 移动端最近播放页点击歌曲使用完整队列

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

让移动端“最近播放页”中的歌曲可点击播放。点击任意歌曲时，播放队列必须使用完整最近播放歌曲列表，并以被点击歌曲作为播放起点。

## 验收标准

- [x] 点击最近播放页任意歌曲会播放该歌曲。
- [x] 播放队列使用完整最近播放歌曲列表。
- [x] 播放起点是被点击的歌曲。
- [x] 队列不包含不可解析、已移除或不可播放歌曲。
- [x] 不新增播放日志管理、清空、编辑或排序能力。
- [x] 更新播放行为测试，断言最近播放页点击传入的是完整最近播放队列。

## 依赖

- 07-recent-played-song-list-filtering.md
- 13-mobile-recent-played-page-full-list.md

## Comments

### 实现摘要

- `RecentPlayedScreen` 新增歌曲点击回调，完整页歌曲行可点击播放，但不接入当前播放高亮、播放中辅助标识或更多菜单。
- 移动端 `MobileSecondaryScreenRoute` 将最近播放页点击接到 `MusicAppController.playRecentSong`，复用 issue 14 已建立的最近播放专用 controller 入口。
- 播放队列继续由 controller 从 `uiState.recentSongs` 统一选择，页面层不扫描全库、不拼 demo、不自行解析播放历史，也不使用摘要 Top3 局部列表。
- 新增 `playRecentPageSongUsesFullRecentQueueWithClickedStart` 共享测试，覆盖最近播放页打开后点击第 4 首时，队列仍为完整最近播放列表，当前播放下标为 3，并过滤已移除歌曲。
- 未修改 Desktop 最近播放、底部 Tab、全局迷你播放器、播放日志管理、清空、编辑、排序、持久化或 `prototypes/kmp-music-hi-fi`。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playRecentPageSongUsesFullRecentQueueWithClickedStart`：通过，`BUILD SUCCESSFUL in 7s`。
- `./gradlew :composeApp:desktopTest`：通过，`BUILD SUCCESSFUL in 7s`。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，`BUILD SUCCESSFUL in 5s`。
- `git diff --check`：通过，无 whitespace error。
- 验证输出仍包含既有 Gradle deprecated property 警告；首次聚焦测试编译阶段仍提示 `MusicAppControllerTest.kt` 两处既有 `No cast needed` 警告。

### Code review 结论

- 规格符合性：最近播放页歌曲行现在可点击，点击入口统一调用 `playRecentSong`；新增测试断言点击第 4 首后 `queueSongIds` 等于完整最近播放队列，`currentSongId` 和 `QueueState.currentIndex` 都指向被点击歌曲。
- 过滤安全：队列来源是 issue 07 统一过滤后的 `uiState.recentSongs`；测试把 `removed-song` 放入底层播放历史，断言它不会进入最近播放页队列。
- 范围控制：diff 只触及移动端最近播放页、移动端二级路由和 controller 播放行为测试；没有修改其它页面的 `playSong(song, queueSongs)` 路径，也没有抢做 issue 16、17、22-24。
- 代码标准：UI 层只传递点击意图，队列决策保留在 controller；新增注释为中文，Kotlin 命名和参数格式与周边代码一致。

### 对抗式审查

- 风险 1：页面只传可见局部列表。检查结果：最近播放页只传 `Song`，完整队列由 `controller.playRecentSong` 从 `uiState.recentSongs` 读取；测试覆盖完整 5 首队列。
- 风险 2：播放起点偏移。检查结果：测试点击第 4 首并断言 `currentIndex == 3`，同时断言当前播放歌曲 ID 等于被点歌曲。
- 风险 3：绕过统一过滤导致陈旧历史进入队列。检查结果：页面不读取历史；测试中的 `removed-song` 被过滤，issue 07 已覆盖不可解析和不可播放歌曲过滤规则。
- 风险 4：污染其它页面播放行为。检查结果：首页、收藏、搜索、本地音乐、专辑详情和歌手详情的 `onSongPlay(song, queueSongs)` 接线未改。
- 风险 5：抢做高亮、更多菜单或管理能力。检查结果：最近播放页仅新增点击播放，没有新增红色高亮、播放中辅助标识、更多菜单、清空、编辑、排序或持久化能力。

### 剩余风险或未完成项

- 未做真机、模拟器或截图视觉验证；本切片是点击播放和队列行为，已用共享测试与 Android Kotlin 编译覆盖主要风险。
- 当前播放红色高亮、播放中辅助反馈、更多菜单和 Desktop 最近播放操作仍留给后续 issue。
