Status: ready-for-human

# Desktop“我的”页改为新个人资料结构

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

把 Desktop/macOS “我的”页调整为与移动端同语义的个人中心信息结构：个人资料头、固定头像、用户名和副标题。桌面端应保持宽屏 workspace 适配，不把 390px 手机稿等比例拉伸。

## 验收标准

- [x] Desktop/macOS “我的”页显示个人资料头。
- [x] 个人资料头使用已补充的 Figma 静态头像资源。
- [x] 用户名显示为“高保真听众”，副标题显示为“音乐是我的灵魂”。
- [x] 旧登录卡片和“立即登录”入口不再出现在 Desktop/macOS “我的”页内容区。
- [x] 页面布局保持桌面 workspace 语义，不等比例拉伸手机稿。

## 依赖

- 01-figma-static-avatar-resource.md

## Comments

### 实现摘要

- 已将 Desktop/macOS “我的”页顶部旧登录卡片替换为桌面个人资料头，保留桌面 workspace 的全宽内容卡片和横向头像资料排布，没有照搬 390px 移动端稿。
- 个人资料头使用 `composeResources/drawable/me_profile_avatar.jpg`，文案固定为“高保真听众”和“音乐是我的灵魂”。
- 已移除桌面“我的”页调用链中的 `onLogin` 入口和旧 `DesktopProfilePanel` 卡片实现，因此“登录音乐账号”和“立即登录”不再出现在 Desktop/macOS “我的”页内容区。
- 未删除 `SecondaryScreen.Login`、移动端 `LoginScreen`、桌面 `DesktopLoginScreen` 或桌面二级登录路由；本切片也未实现统计、扫描音乐、设置菜单、桌面最近播放摘要/列表/动作反馈等后续 issue。

### 验证命令与结果

- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，确认 `commonMain` 共享 UI、Compose 资源访问和 Android 目标编译成功。
- `./gradlew :composeApp:desktopTest`：通过，确认 Desktop 目标编译和现有桌面/共享测试通过；仅出现既有 commonTest 的 `No cast needed` 警告。
- `git diff --check`：通过，没有补丁空白问题。
- `rg -n "登录音乐账号|立即登录|DesktopProfilePanel" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens/DesktopMeScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/components/DesktopCards.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation/DesktopRootScreenRoute.kt`：通过，未在桌面“我的”页相关文件发现旧登录文案或旧卡片组件。
- `rg -n "SecondaryScreen.Login|DesktopLoginScreen|LoginScreen\\(" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature`：通过，确认登录页和登录路由仍保留。

### Code review 结论

- Standards 轴通过。改动集中在 Desktop “我的”页 UI 和旧登录卡片清理，没有引入平台 API、后端、账号状态、持久化或跨层依赖；没有修改 `prototypes/kmp-music-hi-fi` 或 `.agent-loop`。
- Spec 轴通过。当前 issue 的五项验收标准均已满足；桌面资料头使用 Figma 静态头像资源和指定文案，旧登录卡片与“立即登录”入口已从桌面“我的”内容区移除，登录页面和路由仍存在。

### 对抗式审查

- 风险一：误删登录路由。复核结果：`SecondaryScreen.Login`、`MobileSecondaryScreenRoute`、`DesktopSecondaryScreenRoute`、`LoginScreen` 和 `DesktopLoginScreen` 均保留。
- 风险二：只换文案但仍保留旧登录卡片。复核结果：桌面“我的”页不再调用旧卡片，旧 `DesktopProfilePanel` 已移除，相关文件中未检出“登录音乐账号”或“立即登录”。
- 风险三：用专辑封面冒充头像。复核结果：桌面资料头直接引用 `Res.getUri("drawable/me_profile_avatar.jpg")`，没有继续使用 `CoverArt.AlbumTimeForest`。
- 风险四：把手机稿硬套到 Desktop。复核结果：桌面端使用 104dp 头像、28dp/26dp 卡片内边距、全宽 workspace 卡片和横向信息结构，没有套用移动端 78dp 头像和窄屏纵向节奏。
- 风险五：抢做后续 Desktop issue。复核结果：本切片没有新增桌面统计新语义、扫描入口、静态设置菜单、最近播放摘要/完整页或动作反馈。

### 剩余风险或未完成项

- 无未完成验收项。
- 剩余风险是本切片未启动 Desktop App 做截图核对；已通过代码审查和 Desktop 编译/测试确认结构、资源与文案落地。
