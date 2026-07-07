Status: ready-for-human

# 移动端摘要点击歌曲使用完整最近播放队列

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

让移动端“我的”页最近播放摘要中的歌曲可点击播放。即使摘要只显示 3 条，点击任意摘要歌曲时也必须把完整最近播放歌曲列表作为播放队列，并以被点击歌曲作为播放起点。

## 验收标准

- [x] 点击摘要区任意歌曲会播放该歌曲。
- [x] 播放队列使用完整最近播放歌曲列表，不只是可见 3 条。
- [x] 播放起点是被点击的歌曲。
- [x] 队列不包含不可解析、已移除或不可播放歌曲。
- [x] 不改变其它页面歌曲列表的播放行为。
- [x] 更新播放行为测试，断言摘要点击传入的是完整最近播放队列。

## 依赖

- 07-recent-played-song-list-filtering.md
- 11-mobile-me-recent-played-summary-real-top3.md

## Comments

### 实现摘要

- `MusicAppController` 新增 `playRecentSong`，固定使用当前 `uiState.recentSongs` 作为播放队列来源，避免“我的”页摘要 Top3 截断播放上下文。
- 移动端 `MobileRootScreenRoute` 在“我的”页把最近播放摘要歌曲点击接到 `controller.playRecentSong`。
- `MeScreen` 的最近播放摘要行从静态展示改为可点击播放；点击只传递歌曲本身，完整队列选择留在 controller 内完成。
- 未修改最近播放完整页、Desktop 最近播放、当前播放红色高亮、更多菜单、底部 Tab、全局迷你播放器或原型目录。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playRecentSummarySongUsesFullRecentQueueWithClickedStart`：先红后绿。红灯为缺少 `playRecentSong`；实现后通过，`BUILD SUCCESSFUL in 3s`。
- `./gradlew :composeApp:desktopTest`：通过，`BUILD SUCCESSFUL in 2s`。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，`BUILD SUCCESSFUL in 5s`。
- `git diff --check`：通过，无 whitespace error。
- 验证输出仍包含既有 Gradle deprecated property 警告；聚焦测试编译阶段仍提示 `MusicAppControllerTest.kt` 两处既有 `No cast needed` 警告。

### Code review 结论

- 规格符合性：摘要行已可点击播放；`playRecentSong` 始终把完整过滤后的 `uiState.recentSongs` 传给播放入口，测试覆盖 5 条最近播放中点击第 3 条时，队列仍为 5 条且当前队列下标为 2。
- 过滤安全：队列来源是 issue 07 已统一过滤的 `recentSongs`；本次新增测试把 `missing-song` 放入底层历史，断言它不会进入摘要播放队列。
- 范围控制：没有接入完整最近播放页点击播放、红色高亮、更多菜单或 Desktop 最近播放行为；其它页面仍继续通过各自原有 `onSongPlay(song, queueSongs)` 路径播放。
- 代码质量：新增入口很窄，只封装最近播放队列选择；UI 层不自行读取全库、不拼 demo 数据、不解析播放历史。

### 对抗式审查

- 风险 1：只把可见 Top3 传入队列。已用 `playRecentSummarySongUsesFullRecentQueueWithClickedStart` 覆盖，点击第 3 条时队列仍等于完整 5 条 `recentSongs`。
- 风险 2：播放起点错误。测试断言 `currentSongId` 是被点击歌曲，并断言底层 `QueueState.currentIndex` 为被点击歌曲在完整最近播放队列中的下标。
- 风险 3：不可解析或已移除历史项混入队列。测试将 `missing-song` 放入原始播放历史，断言最终 `recentSongs` 和播放队列都不包含它；不可播放歌曲过滤仍由 issue 07 的同步器测试覆盖。
- 风险 4：污染其它页面播放行为。Diff 只改移动端“我的”页 route、`MeScreen` 摘要行、controller 新入口和对应测试；首页、收藏、本地音乐、搜索、专辑、歌手和最近播放完整页的 `onSongPlay` 传参没有变化。
- 风险 5：抢做后续 issue。未改最近播放完整页点击、当前播放红色高亮、播放中辅助标识、更多菜单或 Desktop 最近播放摘要。

### 剩余风险或未完成项

- 未做真机或截图视觉验证；本切片是点击播放和队列行为改动，已用共享测试和 Android Kotlin 编译覆盖主要风险。
- 最近播放完整页点击播放仍留给 issue 15；当前播放高亮和更多菜单仍留给 issue 16、17。
