Status: ready-for-human

# 移动端“我的”页移除旧标题和登录卡片

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

把移动端“我的”页内容区从旧的页面标题栏和登录卡片结构，改为从个人资料区开始的基础结构。这个切片只移除页面内“我的”标题栏、旧登录卡片和“立即登录”入口，并保留登录页面与登录路由本身。

## 验收标准

- [x] 移动端“我的”页内容区不再显示页面内“我的”标题栏。
- [x] 移动端“我的”页内容区不再显示旧登录卡片。
- [x] 移动端“我的”页内容区不再显示“立即登录”按钮。
- [x] 现有登录页面和登录路由没有被删除或重构。
- [x] 底部 Tab 和全局迷你播放器行为不因本切片改变。

## 依赖

- 01-figma-static-avatar-resource.md

## Comments

### 实现摘要

已将移动端 `MeScreen` 从旧页面内标题栏和登录卡片结构改为基础个人资料区开头。页面内容不再渲染 `AppHeader(title = "我的")`、旧登录卡片、“登录音乐账号”文案或“立即登录”按钮。基础资料区使用前置 issue 已落库的 `me_profile_avatar.jpg` 头像资源，并只展示头像、用户名和副标题；青绿色描边、编辑浮层、统计重做、扫描音乐、设置菜单、最近播放和 Desktop 改造均未在本切片实现。

登录页面与登录路由保留在 `SecondaryScreen.Login`、`MobileSecondaryScreenRoute` 和 `LoginScreen` 中，没有删除或重构。移动端根路由只是不再从“我的”页传入登录或设置入口。底部 Tab 和全局迷你播放器相关布局、chrome 状态和 playerbar 文件未修改。

### 验证命令与结果

- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，Android Kotlin 编译成功，确认移动端路由、`MeScreen` 签名和头像资源引用可编译。
- `./gradlew :composeApp:desktopTest`：通过，桌面测试成功，覆盖共享 controller、导航和 chrome 相关回归用例，未发现登录路由或全局 chrome 破坏。
- `git diff --check`：通过，没有空白或补丁格式问题。
- `rg -n "SecondaryScreen.Login|LoginScreen|立即登录|登录音乐账号|AppHeader\\(title = \\\"我的\\\"" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic .scratch/mobile-desktop-me-redesign/issues/02-mobile-me-remove-old-title-login-card.md`：通过人工核对，移动端“我的”页不再包含旧标题和旧登录入口；登录路由和登录页仍存在。Desktop 端仍保留旧登录入口，属于后续 Desktop issue 范围，本切片未修改。

### Code review 结论

自审通过。Standards 轴：改动集中在移动端“我的”页和根路由调用点，没有跨层依赖、平台 API 污染或重复补丁；未触碰 `prototypes/kmp-music-hi-fi` 和 `.agent-loop`。Spec 轴：所有 issue 02 验收项均已满足，且未删除登录页、登录路由、底部 Tab 或全局迷你播放器行为。

### 对抗式审查

- 风险一：误删登录路由。已检查 `SecondaryScreen.Login`、`MobileSecondaryScreenRoute` 和 `LoginScreen` 仍保留，并通过编译验证。
- 风险二：只隐藏按钮但保留旧登录卡片。已移除整段旧登录 `Surface`，页面内不再渲染“登录音乐账号”和“立即登录”。
- 风险三：越界实现 issue 03 或后续能力。当前只放置基础资料区，没有头像描边、编辑浮层、统计重做、扫描入口、设置菜单、最近播放或 Desktop 改造。
- 风险四：破坏全局 chrome。改动没有触碰 `MobileContentLayout`、`MobileAppLayout`、底部 Tab 或迷你播放器；`desktopTest` 通过。
- 风险五：误提交分发会话运行状态。`.agent-loop/*` 仍是工作区既有未提交 diff，不纳入本任务提交。

### 剩余风险或未完成项

无未完成验收项。剩余视觉风险是本切片未做真机或截图核对；头像描边、编辑浮层和完整 Figma 视觉还原由后续 issue 继续完成。
