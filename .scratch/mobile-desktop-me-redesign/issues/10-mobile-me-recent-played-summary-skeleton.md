Status: ready-for-human

# 移动端“我的”页最近播放摘要区骨架

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

在移动端“我的”页补充“最近播放”摘要区域骨架，包含标题、查看全部入口位置和空态承载。这个切片只搭建摘要区结构，不接入真实歌曲行、播放队列或更多菜单。

## 验收标准

- [x] 移动端“我的”页显示“最近播放”区域。
- [x] 摘要区预留“查看全部”入口位置。
- [x] 最近播放为空时显示轻量空态文案。
- [x] 摘要区不会留白或挤压全局迷你播放器。
- [x] “查看全部”本切片可以暂不接入完整跳转，后续切片完成跳转行为。

## 依赖

- 02-mobile-me-remove-old-title-login-card.md
- 08-recent-played-secondary-route.md

## Comments

### 实现摘要

- 在移动端 `MeScreen` 的“快速功能”和“我的收藏”之间新增 `RecentPlayedSummarySection`，展示“最近播放”标题、“查看全部 ›”入口占位和轻量空态文案。
- 新增 `RecentPlayedSummaryDisplayModel` 与 `buildRecentPlayedSummaryDisplayModel()`，把当前切片的展示语义固定为静态空态骨架，后续切片可在同一位置接入真实 Top3、跳转和播放行为。
- 空态容器使用稳定最小高度，避免最近播放为空时摘要区塌陷成留白；未修改底部 Tab、全局迷你播放器、Desktop/macOS “我的”页或 `prototypes/kmp-music-hi-fi`。
- 新增 `MeScreenTest` 覆盖摘要骨架标题、查看全部占位、空态文案，以及当前切片不启用跳转、播放队列或更多菜单的边界。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.screen.MeScreenTest`：通过。仅出现既有 Gradle deprecated property 警告和既有测试文件中的 `No cast needed` 警告。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过。仅出现既有 Gradle deprecated property 警告。
- 静态检查：`rg -n "RecentPlayedSummary|openRecentPlayed|recentSongs|playSong|openMore|SecondaryScreen|DesktopMeScreen|prototypes|MobileMiniPlayer|MobileBottomNavigation" ...`。结果显示新增摘要只在 `MeScreen.kt` 中出现；`MeScreen.kt` 没有新增 `openRecentPlayed`、`recentSongs`、`playSong`、`openMore` 或 `SecondaryScreen` 调用。

### Code review 结论

- Standards：改动保持在移动端页面和对应 commonTest 内，Kotlin 命名、中文注释和现有 Compose 组件风格一致；未把平台 API、Desktop 结构或原型目录纳入本切片。
- Spec：五条验收标准均已满足；“查看全部”是静态位置占位，没有绑定完整跳转；摘要区没有真实歌曲行、真实 Top3、播放队列或更多菜单。

### 对抗式审查

- 风险 1：提前接入真实最近播放列表。检查结果：`MeScreen` 未新增 `recentSongs` 入参，展示模型无歌曲列表输入。
- 风险 2：提前接入“查看全部”跳转。检查结果：标题右侧仅渲染文本，没有 `clickable`、`onClick`、`openRecentPlayed` 或 `SecondaryScreen.RecentPlayed` 调用。
- 风险 3：提前接入播放队列或更多菜单。检查结果：`MeScreen` 新增区域没有 `playSong`、`queueSongs` 或 `openMore`。
- 风险 4：摘要区空态塌陷或挤压全局迷你播放器。处理结果：空态容器设置最小高度，页面仍作为一级内容交给既有外层滚动和 chrome 避让处理，没有修改全局迷你播放器策略。
- 风险 5：误改 Desktop/macOS 或原型目录。检查结果：当前任务 diff 仅包含 `MeScreen.kt`、`MeScreenTest.kt` 和本 issue 文件；`.agent-loop/*` 是分发会话遗留未提交状态，未纳入本切片。

### 剩余风险或未完成项

- 未做真机或模拟器截图核对，因此仍有轻微视觉间距风险；本切片已用编译和展示模型测试覆盖结构边界。
- 最近播放真实 Top3、歌曲行、点击播放、更多菜单和“查看全部”跳转仍按 PRD 留给后续 issue，不属于当前切片。
