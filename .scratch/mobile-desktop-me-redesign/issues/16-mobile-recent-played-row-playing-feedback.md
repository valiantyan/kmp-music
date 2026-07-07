Status: ready-for-human

# 移动端最近播放行接入红色高亮和播放中标识

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

让移动端最近播放摘要和最近播放页中的歌曲行接收当前播放歌曲信息。当前播放歌曲标题应显示红色，并保留现有播放中辅助标识，保持与其它歌曲列表一致的全局播放反馈。

## 验收标准

- [x] 当前播放歌曲在移动端最近播放摘要中标题变红。
- [x] 当前播放歌曲在移动端最近播放页中标题变红。
- [x] 当前播放歌曲在最近播放行中保留播放中辅助标识。
- [x] 非当前播放歌曲不错误显示红色或播放中标识。
- [x] 不破坏首页、收藏、专辑详情或歌手详情已有播放高亮规则。
- [x] 通过可测试边界验证当前播放歌曲标识会传入最近播放摘要和最近播放页；如果没有现成测试边界，在 Comments 记录人工验证方式。

## 依赖

- 11-mobile-me-recent-played-summary-real-top3.md
- 13-mobile-recent-played-page-full-list.md

## Comments

### 实现摘要

- 新增 `RecentPlayedSongRowDisplayModel`，把歌曲行和 `isCurrentSong` 放到共享展示模型边界，供移动端最近播放摘要和移动端最近播放页共用。
- 移动端 `MobileRootScreenRoute` 在“我的”页传入 `state.currentSongId`，摘要展示模型只给可见 Top3 中命中的当前歌曲设置当前播放标识。
- 移动端 `MobileSecondaryScreenRoute` 在最近播放页传入 `state.currentSongId`，完整页展示模型对完整最近播放列表逐行标记当前播放歌曲。
- 摘要行和完整页行在当前歌曲上使用 `MusicColors.PlayingRed` 渲染标题，并显示 `PlayingGlyph` 和“播放中”辅助标识；非当前歌曲继续使用普通颜色且不显示辅助标识。
- 未接入最近播放更多菜单、清空、编辑、排序、Desktop 最近播放反馈，也未修改底部 Tab、全局迷你播放器、原型目录或其它列表的既有高亮实现。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.screen.MeScreenTest --tests com.yanhao.kmpmusic.feature.screen.RecentPlayedScreenTest`：通过，`BUILD SUCCESSFUL in 8s`。覆盖摘要和最近播放页展示模型只标记当前歌曲，非当前歌曲不误标。
- `./gradlew :composeApp:desktopTest`：通过，`BUILD SUCCESSFUL in 8s`。覆盖共享逻辑回归。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，`BUILD SUCCESSFUL in 5s`。
- `git diff --check`：通过，无 whitespace error。
- 验证输出仍包含既有 Gradle deprecated property 警告；聚焦测试编译阶段仍提示 `MusicAppControllerTest.kt` 两处既有 `No cast needed` 警告。

### Code review 结论

- Spec：六条验收标准均已满足。摘要和完整页都接收 `currentSongId`，展示模型测试覆盖只标记当前歌曲；行 UI 渲染红色标题并保留 `PlayingGlyph` 与“播放中”辅助标识。
- Standards：改动集中在移动端 route、最近播放行展示模型、`MeScreen`、`RecentPlayedScreen` 和对应 commonTest；没有引入平台 API、数据层变更、持久化变更或额外全局 chrome 策略。
- 范围：没有改首页、收藏、专辑详情、歌手详情、本地音乐、搜索的现有高亮路径；没有实现 issue 17 或 22-24 的更多菜单/Desktop 最近播放反馈。

### 对抗式审查

- 风险 1：非当前歌曲被误标红或显示播放中。检查结果：`recentPlayedSummaryDisplayModelMarksOnlyCurrentVisibleSong` 和 `recentPlayedPageDisplayModelMarksOnlyCurrentSong` 都断言只有命中 `currentSongId` 的行 `isCurrentSong=true`。
- 风险 2：摘要只显示 Top3 时把不可见当前歌曲误投到可见行。检查结果：摘要模型先 `take(3)`，再构建行状态；测试输入 4 首时只验证可见 3 首的标识。
- 风险 3：接入红色标题时丢掉播放中辅助标识。检查结果：摘要行和完整页行都在当前歌曲分支渲染 `PlayingGlyph` 和“播放中”文案。
- 风险 4：污染其它列表高亮规则。检查结果：diff 没有修改 `HomeScreen`、`FavoritesScreen`、`AlbumDetailScreen`、`ArtistDetailScreen`、`LocalMusicScreen` 或 `SearchScreen` 的高亮/播放传参。
- 风险 5：抢做后续能力。检查结果：本次没有新增最近播放 more 菜单、清空、编辑、排序、Desktop 最近播放页/摘要反馈，也没有修改底部 Tab、迷你播放器或原型目录。

### 剩余风险或未完成项

- 未做真机、模拟器或截图视觉核对；当前风险主要由展示模型测试、共享测试和 Android Kotlin 编译覆盖。
- 最近播放更多菜单和 Desktop 最近播放反馈仍留给后续 issue。
