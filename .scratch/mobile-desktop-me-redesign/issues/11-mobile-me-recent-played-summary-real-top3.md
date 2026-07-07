Status: ready-for-human

# 移动端摘要区只显示最近 3 条真实歌曲

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

把移动端“我的”页最近播放摘要区接入过滤后的最近播放歌曲列表，并且最多只显示最新 3 条。摘要区不使用 demo 数据，也不展示不可解析或不可播放的历史歌曲。

## 验收标准

- [x] 最近播放摘要最多显示 3 条歌曲。
- [x] 摘要歌曲来自统一过滤后的最近播放歌曲列表。
- [x] 摘要不显示 demo 歌曲、不显示全库歌曲，也不显示不可播放历史项。
- [x] 最近播放为空时继续显示空态文案。
- [x] 本切片不要求完成点击播放和更多菜单行为。

## 依赖

- 07-recent-played-song-list-filtering.md
- 10-mobile-me-recent-played-summary-skeleton.md

## Comments

### 实现摘要

- 移动端 `MobileRootScreenRoute` 在“我的”页分支把 `state.recentSongs` 传入 `MeScreen`，复用 issue 07 已建立的统一过滤后最近播放歌曲列表。
- `MeScreen` 的最近播放摘要展示模型改为接收外部最近播放歌曲列表，并在模型层使用 `take(3)` 只保留最新 3 条可见歌曲。
- 摘要区非空时渲染静态歌曲行，展示封面、标题、歌手、专辑和时长；空列表时继续显示 issue 10 的空态文案。
- 本切片没有接入点击播放、更多菜单、查看全部跳转、完整最近播放页列表、Desktop 最近播放摘要、底部 Tab、全局迷你播放器或原型目录。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.screen.MeScreenTest`：通过，`BUILD SUCCESSFUL in 20s`。存在既有 Gradle deprecated property 警告，以及既有 `MusicAppControllerTest.kt` 两处 `No cast needed` 警告。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，`BUILD SUCCESSFUL in 8s`。存在既有 Gradle deprecated property 警告。
- `./gradlew :composeApp:desktopTest`：通过，`BUILD SUCCESSFUL in 3s`。存在既有 Gradle deprecated property 警告。
- `git diff --check`：通过，无 whitespace error。

### Code review 结论

- Spec：五条验收标准均已满足。摘要只从 `state.recentSongs` 取数据，展示模型只截断到 3 条；没有全库读取、demo catalog 回退或历史项二次解析逻辑。
- Standards：改动保持在移动端路由、移动端 `MeScreen` 和对应 commonTest 内；Kotlin 注释为中文，新增函数职责单一，未引入平台 API 或额外抽象。
- 范围：没有抢做点击播放、更多菜单、查看全部跳转、完整最近播放页、Desktop 最近播放摘要、底部 Tab 或全局迷你播放器策略。

### 对抗式审查

- 风险 1：误用全库或 demo 歌曲。检查结果：`MeScreen` 只接收 `recentSongs` 入参，路由只传 `state.recentSongs`，展示模型没有 repository、全库、demo 数据源或 fallback。
- 风险 2：摘要超过 3 条。检查结果：`buildRecentPlayedSummaryDisplayModel()` 使用 `recentSongs.take(3)`，新增测试覆盖 5 条输入只显示前 3 条。
- 风险 3：显示不可解析或不可播放历史项。检查结果：过滤职责仍在 issue 07 的 `LibraryStateSynchronizer.buildRecentSongs`，本切片只消费过滤结果，不重新拼接历史 ID。
- 风险 4：提前接入播放、更多菜单或跳转。检查结果：新增摘要行无 `clickable`、`onSongPlay`、`onMore`、`openRecentPlayed` 调用；`isActionEnabled` 仍为 `false`。
- 风险 5：破坏空态。检查结果：空列表分支继续渲染原空态文案，并有测试断言 `songs` 为空和空态文案仍存在。

### 剩余风险或未完成项

- 未做真机、模拟器或截图视觉核对；当前验证覆盖数据接入、Top3 截断、空态和 Android Kotlin 编译。
- 点击播放、更多菜单、查看全部跳转、完整最近播放页列表、当前播放高亮和 Desktop 最近播放摘要仍留给后续 issue。
