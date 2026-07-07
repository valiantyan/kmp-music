Status: ready-for-human

# 移动端“查看全部”带箭头并进入最近播放页

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

让移动端“我的”页最近播放摘要区的“查看全部”后显示右箭头图标，并点击进入“最近播放页”。入口自身不显示三点更多按钮，避免和歌曲行操作混淆。

## 验收标准

- [x] “查看全部”文案后显示右箭头图标。
- [x] 点击“查看全部”进入“最近播放页”。
- [x] “查看全部”入口自身没有三点更多按钮。
- [x] 返回后仍回到“我的”页。
- [x] 底部 Tab 和全局迷你播放器行为不因本切片改变。
- [x] 更新或补充导航行为测试，覆盖从“我的”页进入最近播放页和返回。

## 依赖

- 08-recent-played-secondary-route.md
- 10-mobile-me-recent-played-summary-skeleton.md

## Comments

### 实现摘要

- 移动端 `MeScreen` 最近播放摘要标题行改为真实可点击入口，文案仍为“查看全部”，后接 `KeyboardArrowRight` 右箭头图标。
- `MobileRootScreenRoute` 在“我的”页分支传入 `controller::openRecentPlayed`，点击后复用 issue 08 已建立的 `SecondaryScreen.RecentPlayed` 普通二级页语义。
- 摘要入口只渲染文字和右箭头图标，没有加入三点更多按钮，也没有接入歌曲行播放、播放队列、更多菜单或完整列表。
- 新增 controller 行为测试覆盖从 `RootTab.Me` 进入最近播放页、普通二级页 chrome 策略、返回后仍回到“我的”页。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.screen.MeScreenTest --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest`：通过，`BUILD SUCCESSFUL in 4s`。存在既有 Gradle deprecated property 警告和既有 `No cast needed` 警告。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，`BUILD SUCCESSFUL in 7s`。存在既有 Gradle deprecated property 警告。
- `./gradlew :composeApp:desktopTest`：通过，`BUILD SUCCESSFUL in 2s`。存在既有 Gradle deprecated property 警告。
- `git diff --check`：通过，无 whitespace error。

### code review 结论

- Spec：六条验收标准均已满足；“查看全部”后显示右箭头图标，点击进入最近播放二级页，返回恢复 `RootTab.Me`，并通过 fixed-bar 断言证明底部 Tab 与全局迷你播放器策略仍由既有导航模型派生。
- Standards：改动集中在移动端“我的”页、移动端 root route 和 shared tests；未修改 Desktop/macOS、原型目录、播放队列、最近播放完整列表或全局 chrome 组件。
- 测试：新增 controller 导航行为测试，更新 `MeScreenTest` 锁住查看全部入口仅启用导航、不包含更多菜单或播放队列语义。

### 对抗式审查

- 风险 1：返回后落到错误一级页。测试先切到 `RootTab.Me`，调用 `openRecentPlayed()` 后再 `navigateBack()`，断言返回后 `rootTab == RootTab.Me` 且 `secondaryScreen == null`。
- 风险 2：破坏底部 Tab 或全局迷你播放器。测试断言最近播放页为 `SecondaryWithMiniPlayer` 且隐藏底部 Tab，返回后恢复 `TopLevel` 且显示底部 Tab；实现没有改 layout 或 playerbar。
- 风险 3：把“查看全部”做成歌曲行更多操作。实现只使用文字和右箭头图标，没有在入口使用 `MoreVert`、`openMore` 或三点文案；测试断言 action label 不包含“更多”或三点。
- 风险 4：抢做后续最近播放完整列表、点击播放或队列。`RecentPlayedScreen` 未改，摘要歌曲行仍是静态展示；本切片没有新增 `playSong`、队列或更多菜单调用。
- 风险 5：误改 Desktop/macOS、原型或 `.agent-loop`。任务 diff 只包含生产 KMP App 相关 Kotlin 文件、shared tests 和本 issue 文件；`.agent-loop/*` 保持为分发会话未提交状态，未纳入本切片。

### 剩余风险或未完成项

- 未做真机、模拟器或截图视觉核对；仍有轻微箭头间距和触控热区视觉风险，但 Android 编译与 shared tests 已覆盖行为。
- 完整最近播放列表、点击播放、播放队列、更多菜单、当前播放高亮和 Desktop 最近播放摘要仍按后续 issue 处理，不属于当前切片。
