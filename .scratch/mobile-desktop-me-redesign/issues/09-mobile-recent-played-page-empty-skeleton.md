Status: ready-for-human

# 移动端最近播放页空态骨架

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

为移动端“最近播放页”补充最小页面骨架和空态。没有最近播放歌曲时，页面应显示清晰、轻量的空态文案，而不是崩溃、白屏或留空。

## 验收标准

- [x] 移动端“最近播放页”可以正常渲染。
- [x] 最近播放列表为空时显示轻量空态文案。
- [x] 空态说明播放歌曲后才会产生最近播放记录。
- [x] 页面保留普通二级页面返回语义。
- [x] 本切片不实现完整歌曲列表、播放队列或更多菜单。

## 依赖

- 08-recent-played-secondary-route.md

## Comments

### 实现摘要

- 新增移动端 `RecentPlayedScreen`，在普通二级页内渲染返回头部和轻量提示文案。
- `SecondaryScreen.RecentPlayed` 不再复用曲库缺失兜底页，而是接入最近播放页骨架，并继续使用 `controller.navigateBack` 保留 issue 08 的返回语义。
- 新增 `RecentPlayedScreenTest` 覆盖空列表文案，并锁住本切片非目标：非空时仍只提示后续切片接入，不实现完整歌曲列表、播放队列或更多菜单。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.screen.RecentPlayedScreenTest --tests com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest`：通过。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过。
- Gradle 输出仍包含既有 Kotlin MPP deprecated property 警告，本次改动未新增该警告。

### Code review 结论

- Standards：未发现违反 `AGENTS.md`、OpenWiki 或 Kotlin 基础规范的硬性问题；改动集中在移动端 route 和新屏幕展示模型，没有引入跨层依赖、平台 API 或重复菜单/队列逻辑。
- Spec：当前实现满足本 issue 的五条验收标准；`RecentPlayed` 仍归类为普通二级页，底部 Tab 与全局迷你播放器策略未修改。

### 对抗式审查

- 风险 1：空态只在占位屏里写死，未来难以接列表。处理：把文案隔离到 `buildRecentPlayedPageDisplayModel`，后续列表切片可保留空态分支。
- 风险 2：误把完整列表、播放队列或更多菜单提前做掉。检查结果：本次没有新增歌曲行、播放回调或 more 入口；测试也断言占位文案不宣称队列和更多菜单。
- 风险 3：破坏二级返回语义或 chrome 策略。检查结果：route 继续传 `controller.navigateBack`，并复跑 `MusicAppNavigationControllerTest` 覆盖 `RecentPlayed` 的普通二级页 fixed-bar 和返回栈规则。
- 风险 4：影响 Desktop 最近播放页范围。检查结果：只修改移动端 `MobileSecondaryScreenRoute` 和移动端 screen；Desktop route 未改。
- 风险 5：误提交分发会话 `.agent-loop` 运行态。处理：仅计划 stage 当前 issue 与本次源码/测试文件，`.agent-loop` 不纳入提交。

### 剩余风险或未完成项

- 未做真机或截图视觉验收；本切片仅通过编译和可测试展示模型确认空态语义。
- 完整歌曲列表、播放队列、更多菜单、当前播放高亮和 Desktop 最近播放页仍按后续 issue 处理。
