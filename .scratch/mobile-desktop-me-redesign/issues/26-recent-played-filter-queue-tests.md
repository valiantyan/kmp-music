Status: ready-for-human

# 确认或补齐最近播放过滤与队列行为测试

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

确认最近播放歌曲列表过滤、摘要点击播放队列、最近播放页点击播放队列和当前播放高亮行为已经有测试覆盖；发现缺口时补齐。测试应断言外部可感知行为，不绑定私有 Composable 拆分或局部变量。

## 验收标准

- [x] 确认或补齐最近播放列表过滤不可解析、已移除或不可播放歌曲的测试。
- [x] 确认或补齐移动端摘要点击歌曲时使用完整最近播放队列的测试。
- [x] 确认或补齐移动端最近播放页点击歌曲时使用完整最近播放队列的测试。
- [x] 确认或补齐 Desktop/macOS 最近播放点击歌曲时使用完整最近播放队列的测试。
- [x] 确认或补齐当前播放歌曲在最近播放列表中的高亮或播放中反馈测试。

## 依赖

- 07-recent-played-song-list-filtering.md
- 14-mobile-summary-play-uses-full-recent-queue.md
- 15-mobile-recent-page-play-uses-full-queue.md
- 24-desktop-recent-played-actions-feedback.md

## Comments

### 实现摘要

- 确认现有 `MusicAppLibraryStateSynchronizerTest.buildRecentSongsFiltersStaleAndUnplayableHistoryItems` 已覆盖最近播放历史中的不可解析、已移除和不可播放歌曲过滤，并断言过滤结果使用当前曲库实体。
- 确认现有 `MusicAppControllerTest` 已覆盖移动端摘要、移动端最近播放页和 Desktop/macOS 最近播放点击均通过完整 `recentSongs` 队列播放；本次补强桌面回归测试，在桌面历史中加入不存在的陈旧歌曲，断言播放队列仍只包含过滤后的完整最近播放列表。
- 为移动端最近播放行展示模型补充 `playingIndicatorLabel`，让摘要页和完整页的“播放中”辅助反馈成为可直接断言的 display model 行为；移动端 UI 继续渲染既有红色标题、播放图标和“播放中”文案，没有新增产品能力。
- 补齐移动端摘要和移动端最近播放页当前播放反馈断言；桌面摘要和桌面完整页已有 `playingIndicatorLabel` 断言继续覆盖。
- 未修改登录页、登录路由、底部 Tab、全局迷你播放器、后端、账号、持久化表、最近播放管理能力、`prototypes/kmp-music-hi-fi` 或 `.agent-loop/*`。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.library.MusicAppLibraryStateSynchronizerTest.buildRecentSongsFiltersStaleAndUnplayableHistoryItems --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playRecentSummarySongUsesFullRecentQueueWithClickedStart --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playRecentPageSongUsesFullRecentQueueWithClickedStart --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.playDesktopRecentSongUsesFullRecentQueueWithClickedStart --tests com.yanhao.kmpmusic.feature.screen.MeScreenTest.recentPlayedSummaryDisplayModelMarksOnlyCurrentVisibleSong --tests com.yanhao.kmpmusic.feature.screen.RecentPlayedScreenTest.recentPlayedPageDisplayModelMarksOnlyCurrentSong --tests com.yanhao.kmpmusic.feature.desktop.screens.DesktopMeScreenTest.desktopMeRecentPlayedSummaryMarksOnlyCurrentVisibleSong --tests com.yanhao.kmpmusic.feature.desktop.screens.DesktopRecentPlayedScreenTest.desktopRecentPlayedPageMarksOnlyCurrentSong`：通过，`BUILD SUCCESSFUL in 10s`。
- `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`：通过，`BUILD SUCCESSFUL in 7s`。
- `git diff --check`：通过，无空白错误。
- 验证输出仍包含既有 Gradle deprecated property 提示；聚焦测试首次编译时仍提示 `MusicAppControllerTest.kt` 两处既有 `No cast needed` 警告，本切片未修改相关行。

### Code review 结论

- 过滤测试符合规格：共享同步器测试同时放入不存在的 `stale` 历史 ID、当前曲库不可播放的 `unplayable` 歌曲和重复历史 ID，断言输出只剩当前可解析可播放歌曲。
- 队列测试符合规格：移动端摘要、移动端完整页和桌面点击测试都断言 `queueSongIds` 等于完整过滤后的最近播放列表，并断言 `currentSongId` 与底层 `QueueState.currentIndex` 指向被点击歌曲。
- 当前播放反馈测试符合规格：移动端摘要和完整页现在直接断言 `playingIndicatorLabel == "播放中"` 只出现在当前歌曲行；桌面摘要和桌面完整页已有同类断言。
- 范围控制通过：改动只触及最近播放行 display model、两处移动端渲染读取、相关 commonTest 和当前 issue 文件；没有引入后端、持久化、管理页、私有 Composable 结构断言或跨层新依赖。

### 对抗式审查

- 风险 1：过滤只测不可解析，漏掉不可播放。复核结果：`buildRecentSongsFiltersStaleAndUnplayableHistoryItems` 里同时包含不存在的 `stale` 和 `localUri` 为空的 `unplayable`，并断言二者均不进入结果。
- 风险 2：摘要点击误用 Top3 队列。复核结果：`playRecentSummarySongUsesFullRecentQueueWithClickedStart` 点击第 3 首时断言队列仍为 5 首完整最近播放列表。
- 风险 3：完整页点击误用页面局部队列。复核结果：`playRecentPageSongUsesFullRecentQueueWithClickedStart` 打开最近播放页后点击第 4 首，断言队列仍为完整最近播放列表。
- 风险 4：桌面点击只测 happy path。复核结果：本次给桌面队列测试加入不存在历史项，断言桌面点击仍消费过滤后的完整最近播放列表。
- 风险 5：当前播放反馈只测私有 UI 分支。复核结果：测试断言 display model 输出的 `isCurrentSong` 和 `playingIndicatorLabel`，不绑定私有 Composable 拆分或局部变量。

### 剩余风险或未完成项

- 无未完成验收项。
- 本切片未启动真机、模拟器或 Desktop App 做截图和手工点击；此次目标是测试回归补强，已通过 targeted `desktopTest`、完整 `desktopTest`、Android Kotlin 编译和 `git diff --check` 覆盖主要行为风险。
