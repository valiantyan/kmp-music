Status: ready-for-human

# Desktop 显示静态设置菜单三行

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

在 Desktop/macOS “我的”页展示“存储管理”“主题与外观”“关于”三行设置菜单。三行只作为静态 UI 展示，不跳转设置、关于、本地来源或其它页面。

## 验收标准

- [x] Desktop/macOS “我的”页显示“存储管理”“主题与外观”“关于”三行。
- [x] 三行视觉与桌面个人中心结构一致。
- [x] 点击三行不会触发导航或打开页面。
- [x] 不接入旧设置页、关于页或来源管理入口。
- [x] 两端设置菜单静态展示语义保持一致。

## 依赖

- 18-desktop-me-new-profile-structure.md

## Comments

### 实现摘要

- 新增 `DesktopMeStaticSettingsMenu.kt`，在 Desktop/macOS “我的”页展示“存储管理”“主题与外观”“关于”三行静态设置菜单。
- 三行使用桌面个人中心的白色卡片、左侧图标、说明文案和右侧箭头视觉；展示模型中 `isNavigationEnabled` 固定为 `false`。
- `DesktopMeStaticSettingsMenuRow` 使用无 `onClick` 的 `Surface`，没有接入旧设置页、关于页或来源管理入口。
- 移除 Desktop “我的”页旧的 `onSettings` 参数和“同步与备份”设置跳转行，避免个人中心内容区继续通过设置类入口打开旧设置页。
- 保留 issue 20 的“扫描音乐”入口和既有“本地文件夹”入口行为；未实现 issue 22-24 的桌面最近播放摘要、列表或动作反馈。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.desktop.screens.DesktopMeScreenTest`：通过。新增测试覆盖三行标题为“存储管理 / 主题与外观 / 关于”，并断言三行 `isNavigationEnabled` 均为 `false`；既有桌面统计和扫描入口测试继续通过。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，确认新增 `commonMain` 桌面共享 UI 文件在 Android 目标编译成功。
- `./gradlew :composeApp:desktopTest`：通过，确认完整 Desktop/common 测试无回归。
- `git diff --cached --check`：通过，没有 staged 补丁空白问题。
- `rg -n "onSettings|SecondaryScreen\\.Settings|SecondaryScreen\\.About|DesktopContentRowSyncIcon|存储管理|主题与外观|关于|openLocalMusic\\(section = LocalMusicSection\\.Sources\\)" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens/DesktopMeScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/screens/DesktopMeStaticSettingsMenu.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop/navigation/DesktopRootScreenRoute.kt`：通过。桌面“我的”页相关文件不再包含 `onSettings`、旧设置页或关于页接线；`LocalMusicSection.Sources` 仅保留在 issue 20 要求不破坏的既有“本地文件夹”入口。
- 验证过程中仅出现既有 Gradle 弃用属性提示；首次定向测试编译时还出现既有 `MusicAppControllerTest` 的 `No cast needed` 警告，本切片未修改相关代码。

### Code review 结论

- Spec 轴通过。五项验收标准均已满足：桌面“我的”页显示三行静态设置菜单，视觉沿用桌面个人中心卡片结构，三行没有点击回调，也没有连接设置、关于或来源管理路由。
- Standards 轴通过。改动集中在 Desktop “我的”页静态展示组件、桌面根路由参数清理和对应 commonTest；没有新增 Repository、持久化、平台能力或跨层依赖。
- 范围轴通过。未修改登录页、登录路由、底部 Tab、全局迷你播放器或 `prototypes/kmp-music-hi-fi`；未实现 issue 22-24 的桌面最近播放摘要、完整列表或动作反馈；未修改 `.agent-loop/*`。

### 对抗式审查

- 风险一：三行误接 `onSettings` 或旧设置页。复核结果：Desktop “我的”页已移除 `onSettings` 参数和旧“同步与备份”设置跳转行，相关文件中未检出 `SecondaryScreen.Settings`。
- 风险二：“关于”行误接关于页。复核结果：静态菜单只构造展示模型并渲染无点击 `Surface`，相关文件中未检出 `SecondaryScreen.About`。
- 风险三：“存储管理”误接来源管理。复核结果：静态菜单没有引用 `LocalMusicSection.Sources`；该引用只保留在既有“本地文件夹”入口，用于不破坏 issue 20 已确认行为。
- 风险四：为静态菜单新增设置、关于、存储管理真实功能。复核结果：本切片没有新增领域模型、仓库、持久化、平台 API 或设置页逻辑。
- 风险五：抢做后续最近播放 issue。复核结果：未新增或修改桌面最近播放摘要、最近播放页列表、播放队列或更多菜单行为。

### 剩余风险或未完成项

- 无未完成验收项。
- 剩余视觉风险：本切片未启动真实 Desktop App 做截图核对；已通过桌面展示模型测试、Desktop/common 测试、Android 编译和静态接线复核确认结构与行为边界正确。
