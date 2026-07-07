Status: ready-for-human

# 确认或补齐导航与 chrome 回归测试

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

确认“最近播放页”二级页面语义、“我的”页进入最近播放页、返回行为，以及底部 Tab 和全局迷你播放器不被破坏已经有回归测试覆盖；发现缺口时补齐。测试应关注用户可感知的导航和 chrome 行为。

## 验收标准

- [x] 确认或补齐移动端从“我的”进入“最近播放页”的测试。
- [x] 确认或补齐从“最近播放页”返回“我的”页的测试。
- [x] 确认或补齐“最近播放页”是普通二级页面语义的测试。
- [x] 确认或补齐底部 Tab 行为不因本需求改变的测试。
- [x] 确认或补齐全局迷你播放器策略不因本需求改变的测试。

## 依赖

- 08-recent-played-secondary-route.md
- 12-mobile-me-view-all-recent-played-navigation.md
- 13-mobile-recent-played-page-full-list.md
- 23-desktop-recent-played-page-list-empty.md

## Comments

### 实现摘要

- 复核 issue 08、12、13、23 与当前实现后，确认已有 controller/navigation/screen 测试覆盖最近播放二级路由、移动端“我的”页查看全部入口、返回“我的”页、完整最近播放页和 Desktop workspace 列表语义。
- 强化 `MusicAppControllerTest.meViewAllRecentPlayedOpensRecentPageAndReturnsToMe`，在既有“我的”进入最近播放页并返回的断言上，补齐普通二级页 chrome 回归断言：底部 Tab 隐藏、固定栏位置为 `MiniPlayerOnly`、内容底部避让为 `SecondaryWithMiniPlayer`、不覆盖底层 chrome、无 overlay、underlay 仍是 `RecentPlayed`。
- 同一测试补齐返回后的一级 chrome 断言：返回 `RootTab.Me`、二级页清空、底部 Tab 恢复、固定栏位置为 `TopLevel`、内容底部避让为 `TopLevel`。
- 未修改登录页、登录路由、底部 Tab、全局迷你播放器生产行为或 `prototypes/kmp-music-hi-fi`；本切片只补强回归测试和 issue 证据。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.meViewAllRecentPlayedOpensRecentPageAndReturnsToMe --tests com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest`：通过，覆盖本切片补强的 controller/chrome 测试和既有导航 reducer 测试。
- `./gradlew :composeApp:desktopTest`：通过，完整 Desktop/common 测试无回归。
- `git diff --check`：通过，无空白错误。
- 验证输出仍包含既有 Gradle deprecated property 提示；聚焦测试首次编译时仍提示 `MusicAppControllerTest.kt` 两处既有 `No cast needed` 警告，本切片未修改相关位置。

### Code Review 结论

- Spec：五条验收标准均已满足。移动端从“我的”进入最近播放页、返回“我的”、普通二级页语义、底部 Tab 策略和全局迷你播放器策略都有明确测试覆盖。
- Standards：改动集中在现有 controller commonTest 与当前 issue 文件；没有新增产品行为、平台 API、重复 chrome 判断或新抽象。
- 范围控制：未修改 `.agent-loop/*`、生产路由、登录、底部 Tab、全局迷你播放器和原型目录；测试断言面向用户可感知导航/chrome 行为，不依赖私有 Composable 结构。

### 对抗式审查

- 风险 1：最近播放页被误归为 root 或播放器 overlay。复核结果：测试断言 `secondaryScreen == RecentPlayed`、`fixedBarMode == SecondaryWithMiniPlayer`、`chromeOverlayScreen == null`。
- 风险 2：底部 Tab 在二级页误显示。复核结果：测试断言最近播放页 `showsBottomNavigation == false`，返回“我的”后恢复为 `true`。
- 风险 3：迷你播放器策略被无意改成隐藏或全屏避让。复核结果：测试断言最近播放页 `fixedBarPlacement == MiniPlayerOnly`、`contentBottomSpace == SecondaryWithMiniPlayer`、`coversUnderlyingChrome == false`。
- 风险 4：返回行为落到错误一级页。复核结果：测试从 `RootTab.Me` 调用 `openRecentPlayed()`，`navigateBack()` 后断言仍为 `RootTab.Me` 且二级页为空。
- 风险 5：测试只锁实现细节。复核结果：断言集中在导航状态和 chrome 策略这一公开 UI 状态，不检查具体 Composable 拆分、局部变量或绘制细节。

### 剩余风险或未完成项

- 无未完成验收项。
- 本切片未运行 Android Kotlin 编译，因为只修改 commonTest 和 issue 文件，未改 shared production 逻辑；已用聚焦 `desktopTest` 与完整 `desktopTest` 覆盖相关 common/Desktop 测试边界。
- 未做视觉截图验证；本切片目标是回归测试覆盖，不交付新的视觉或交互行为。
