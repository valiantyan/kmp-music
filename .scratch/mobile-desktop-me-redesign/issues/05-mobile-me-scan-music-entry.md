Status: ready-for-human

# 移动端接入“扫描音乐”快速入口

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

在移动端“我的”页补充“快速功能 / 扫描音乐”入口，点击后复用现有扫描音乐二级页面。这个切片只打通入口到扫描页的真实路径，不在“我的”页内直接启动扫描。

## 验收标准

- [x] 移动端“我的”页显示“快速功能”标题。
- [x] 移动端“我的”页显示“扫描音乐”入口。
- [x] 点击“扫描音乐”进入现有扫描音乐二级页面。
- [x] 点击入口时不在“我的”页直接启动扫描动作。
- [x] 底部 Tab 和全局迷你播放器行为不因本切片改变。

## 依赖

- 02-mobile-me-remove-old-title-login-card.md

## Comments

### 实现摘要

- 在移动端 `MeScreen` 的统计区下方新增“快速功能”标题和“扫描音乐”入口卡片。
- 为 `MeScreen` 增加 `onScanMusic` 回调，并在 `MobileRootScreenRoute` 的“我的”根页面中接入现有 `controller.openAudioScan()`。
- 入口只负责导航到现有 `SecondaryScreen.AudioScan`，扫描动作仍由扫描音乐二级页面内部按钮触发。

### 验证命令与结果

- `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`：通过，`BUILD SUCCESSFUL`。
- 构建过程中仅出现仓库既有 Gradle deprecated property 警告，未出现本次 Kotlin 改动相关警告或失败。

### Code review 结论

- 已复查本次 diff：仅修改移动端“我的”页 UI 和移动端根路由参数接线。
- `MeScreen` 未调用 `requestLocalMusicScan`、`scanLocalMusic` 或扫描用例；点击入口只调用 `openAudioScan()`。
- 未修改底部 Tab、全局迷你播放器、`MobileContentLayout` chrome 策略或原型目录。

### 对抗式审查

- 风险 1：误在“我的”页直接启动扫描。检查结果：入口回调接入 `controller.openAudioScan()`，该方法只 `navigateToSecondary(screen = SecondaryScreen.AudioScan)`。
- 风险 2：破坏底部 Tab 或迷你播放器行为。检查结果：未改动 `MobileBottomNavigation`、`MobileMiniPlayer` 或固定底栏策略。
- 风险 3：误扩大到后续 issue。检查结果：未实现设置菜单、最近播放或 Desktop 改造。
- 风险 4：入口进入新页面而非现有扫描页。检查结果：复用现有 `SecondaryScreen.AudioScan` 和 `AudioScanScreen`。

### 剩余风险或未完成项

- 未进行真机或截图视觉核对；本切片已通过 Android Kotlin 编译和共享桌面测试，剩余风险主要是入口卡片与最终 Figma 像素级间距仍需后续视觉验收确认。
