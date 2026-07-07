Status: ready-for-human

# 移动端最近播放页展示完整列表

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

让移动端“最近播放页”展示完整的最近播放歌曲列表。列表使用统一过滤规则，不限制为摘要区的 3 条，也不展示无法解析或不可播放的历史项。

## 验收标准

- [x] 最近播放页展示完整最近播放歌曲列表。
- [x] 列表使用统一过滤后的最近播放歌曲列表。
- [x] 列表不被摘要区 3 条限制影响。
- [x] 列表为空时继续显示清晰空态。
- [x] 本切片不要求完成点击播放、红色高亮或更多菜单。

## 依赖

- 07-recent-played-song-list-filtering.md
- 09-mobile-recent-played-page-empty-skeleton.md

## Comments

### 实现摘要

- 移动端 `RecentPlayedScreen` 保留普通二级页头部和返回语义，非空时改为渲染完整歌曲列表。
- 最近播放页展示模型只接收调用方传入的 `songs`，路由继续传入 `state.recentSongs`，因此复用 issue 07 的统一过滤结果，不扫描全库、不拼 demo、不解析历史项。
- 完整页列表不使用“我的”页摘要的 `take(3)` 规则；测试覆盖 5 条最近播放输入时完整保留 5 条并保持顺序。
- 空列表时继续显示“暂无最近播放”和“播放歌曲后才会产生最近播放记录。”的清晰空态。
- 本切片没有接入点击播放、红色高亮、播放队列、更多菜单、Desktop 最近播放列表、底部 Tab 或全局迷你播放器策略。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.screen.RecentPlayedScreenTest`：通过，`BUILD SUCCESSFUL in 6s`。输出包含既有 Gradle deprecated property 警告，以及既有 `MusicAppControllerTest.kt` 两处 `No cast needed` 警告。
- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.screen.MeScreenTest`：通过，`BUILD SUCCESSFUL in 1s`。用于确认摘要 Top3 截断仍隔离在“我的”页摘要模型内。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，`BUILD SUCCESSFUL in 8s`。输出包含既有 Gradle deprecated property 警告。
- `git diff --check`：通过，无 whitespace error。

### Code review 结论

- Spec：五条验收标准均已满足。最近播放页非空分支现在展示传入的完整 `songs`，测试证明 5 条输入不会被摘要区 3 条规则截断；空态文案保留。
- Standards：改动集中在移动端 `RecentPlayedScreen` 和对应 commonTest，没有引入平台 API、Desktop 行为、原型目录改动或新的数据源。新增注释为中文，函数职责保持单一。
- 范围控制：未出现 `clickable`、more 菜单、播放队列、当前播放高亮、全库读取或 demo 回退逻辑；`.agent-loop/*` 运行态未纳入本切片。

### 对抗式审查

- 风险 1：误用全库或 demo 歌曲。检查结果：页面和展示模型只消费入参 `songs`，移动端 route 已在前序切片传入 `state.recentSongs`。
- 风险 2：完整页被摘要 Top3 截断。检查结果：`RecentPlayedScreen` 没有 `take(3)`；测试覆盖 5 条输入完整输出 5 条。
- 风险 3：显示不可解析或不可播放历史项。检查结果：本页不重新解析历史，过滤仍由 issue 07 的 `LibraryStateSynchronizer.buildRecentSongs` 负责，本页只展示统一过滤结果。
- 风险 4：提前接入 issue 14 到 17 的播放、高亮或更多菜单。检查结果：新列表行没有点击修饰、播放回调、more 入口或当前播放状态入参。
- 风险 5：破坏空态、底部 Tab 或全局迷你播放器。检查结果：空态分支保留；未修改 `MobileContentLayout`、chrome 策略或 playerbar。

### 剩余风险或未完成项

- 未做真机、模拟器或截图视觉核对；本切片验证集中在完整列表数据语义、空态和 Android Kotlin 编译。
- 点击播放、完整最近播放队列、当前播放红色高亮、播放中辅助标识、更多菜单和 Desktop 最近播放列表仍留给后续 issue。
