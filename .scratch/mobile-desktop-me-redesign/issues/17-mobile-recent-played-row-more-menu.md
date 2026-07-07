Status: ready-for-human

# 移动端最近播放行接入单曲更多菜单

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

让移动端最近播放摘要和最近播放页中的歌曲行右侧三点按钮打开现有单曲更多菜单。这个切片复用现有更多面板能力，不新增重复菜单或新的歌曲操作模型。

## 验收标准

- [x] 最近播放摘要歌曲行右侧显示三点按钮。
- [x] 最近播放页歌曲行右侧显示三点按钮。
- [x] 点击三点按钮打开现有单曲更多菜单。
- [x] “查看全部”入口自身不显示三点更多按钮。
- [x] 不新增重复更多面板或新的歌曲操作入口。

## 依赖

- 11-mobile-me-recent-played-summary-real-top3.md
- 13-mobile-recent-played-page-full-list.md

## Comments

### 实现摘要

- 移动端“我的”页最近播放摘要新增歌曲行右侧三点按钮，按钮只调用既有 `controller.openMore`，行主体点击播放逻辑仍走 `controller.playRecentSong`。
- 移动端最近播放页完整列表新增歌曲行右侧三点按钮，继续保留完整最近播放队列播放、当前播放红色高亮和“播放中”辅助标识。
- 最近播放行展示模型新增 `hasMoreAction` 视图状态，用于证明歌曲行显示更多入口；没有新增歌曲操作列表、管理模型或重复 more 面板。
- 全局 `AppPanels` 抽出 `resolveMorePanelSong`，在既有 more 面板解析链中补充 `state.recentSongs`，避免最近播放歌曲只存在于最近播放列表时无法弹出既有单曲更多菜单。
- “查看全部”标题入口仍只显示文案和右箭头，不复用歌曲行操作区；未修改 Desktop 最近播放、底部 Tab、全局迷你播放器、最近播放清空/编辑/排序/持久化或原型目录。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.screen.MeScreenTest --tests com.yanhao.kmpmusic.feature.screen.RecentPlayedScreenTest --tests com.yanhao.kmpmusic.feature.app.surfaces.AppPanelsTest`：通过，`BUILD SUCCESSFUL in 10s`。覆盖摘要/完整页行级更多入口展示模型，以及最近播放歌曲可被既有 more 面板解析。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，`BUILD SUCCESSFUL in 5s`。
- `./gradlew :composeApp:desktopTest`：通过，`BUILD SUCCESSFUL in 2s`。
- `git diff --check`：通过，无 whitespace error。
- Gradle 输出仍包含既有 deprecated property 警告；首次测试编译仍提示 `MusicAppControllerTest.kt` 两处既有 `No cast needed` 警告。

### Code review 结论

- Spec：五条验收标准均已满足。摘要和完整页歌曲行都显示三点按钮，路由均接到 `controller.openMore`；`AppPanels` 继续使用原有 `moreSongId` + `ModalBottomSheet` 面板，只补充最近播放歌曲解析来源。
- Standards：改动集中在移动端最近播放摘要、移动端最近播放页、移动端路由、全局面板解析 seam 和对应 commonTest；没有引入平台 API、重复面板、Desktop 最近播放能力或新的歌曲操作模型。
- 范围：未修改 `prototypes/kmp-music-hi-fi`，未修改 `.agent-loop/*`，未改底部 Tab、全局迷你播放器、最近播放队列播放、高亮规则或最近播放管理能力。

### 对抗式审查

- 风险 1：误建重复更多菜单。检查结果：没有新增面板 Composable 或操作模型，按钮只设置既有 `moreSongId`，弹层仍由 `AppPanels` 的原 `ModalBottomSheet` 渲染。
- 风险 2：三点按钮错误出现在“查看全部”入口。检查结果：`RecentPlayedSummaryHeader` 仍只渲染“查看全部”和右箭头；三点按钮只在 `RecentPlayedSummarySongActions` 和 `RecentPlayedPageSongActions` 内出现。
- 风险 3：按钮点击抢走歌曲行播放或破坏播放队列。检查结果：行主体仍调用 `onSongPlay(song)`，按钮独立调用 `onSongMore(row.song)`；`playRecentSong` 和最近播放队列逻辑未改。
- 风险 4：最近播放歌曲不在本地库缓存时 more 面板打不开。检查结果：新增 `resolveMorePanelSongFindsRecentPlayedSong` 覆盖 `state.recentSongs` 兜底解析。
- 风险 5：抢做 Desktop 或管理能力。检查结果：diff 没有修改 Desktop 最近播放路由/页面，没有新增清空、编辑、排序或持久化逻辑。

### 剩余风险或未完成项

- 未做真机、模拟器或截图视觉核对；当前证据来自展示模型测试、面板解析测试、完整共享测试和 Android Kotlin 编译。
- 当前 commonTest 没有 Compose UI 点击测试依赖，因此“三点按钮点击”通过路由回调接线、`controller.openMore` 既有 reducer 测试和新增 more 面板歌曲解析测试共同覆盖。
