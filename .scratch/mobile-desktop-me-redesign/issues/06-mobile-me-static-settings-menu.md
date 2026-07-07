Status: ready-for-human

# 移动端显示静态设置菜单三行

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

在移动端“我的”页展示“存储管理”“主题与外观”“关于”三行设置菜单。三行可以显示右箭头视觉，但当前版本只作为静态 UI，不触发导航、设置、关于或其它半成品功能。

## 验收标准

- [x] 移动端“我的”页显示“存储管理”“主题与外观”“关于”三行。
- [x] 三行视觉与个人中心结构一致，并保留右箭头提示。
- [x] 点击三行不会触发导航或打开任何页面。
- [x] 不接入旧设置页、关于页或来源管理页。
- [x] 不修改现有设置页、关于页和对应路由。

## 依赖

- 02-mobile-me-remove-old-title-login-card.md

## Comments

### 实现摘要

- 在移动端 `MeScreen` 底部新增 `StaticSettingsMenuSection`，展示“存储管理”“主题与外观”“关于”三行。
- 每行只包含标题和右箭头图标，没有 `onClick` 参数、`clickable` 修饰符或 `Surface(onClick = ...)`。
- 未修改 `SettingsScreen`、`AboutScreen`、`MobileSecondaryScreenRoute`、`SecondaryScreen` 或来源管理相关路由。

### 验证命令与结果

- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，`BUILD SUCCESSFUL in 59s`。
- 静态检查：`rg -n "StaticSettingsMenu|存储管理|主题与外观|关于|clickable|onClick|navigateToSecondary|SecondaryScreen|openLocalMusic|openAudioScan" composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/MeScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/routes/MobileRootScreenRoute.kt`。结果显示新增静态菜单只出现在 `MeScreen.kt` 的静态组件和文案中，没有新增导航调用；文件内既有 `clickable` 仅属于收藏专辑封面，既有 `onClick` 仅属于“扫描音乐”入口。

### Code review 结论

- 范围符合 issue 06：只改移动端“我的”页静态展示和当前 issue 证据文件。
- 新增菜单没有向 `MeScreen` 函数签名加入设置、关于或来源管理回调，调用链无法从三行进入旧路由。
- 没有修改底部 Tab、全局迷你播放器、设置页、关于页、来源管理页或 Desktop 端实现。

### 对抗式审查

- 风险 1：右箭头可能暗示可点击。代码中未声明点击回调，当前行为仍是静态 UI。
- 风险 2：误接旧 `SettingsScreen` 或 `AboutScreen`。本次没有修改路由文件，也没有新增 `navigateToSecondary` 调用。
- 风险 3：误把“存储管理”接到来源管理。新增组件不引用 `LocalMusicSection.Sources` 或 `openLocalMusic`。
- 风险 4：扩大到 issue 07 或 Desktop 改造。本次没有新增最近播放逻辑，也没有修改 Desktop 文件。
- 风险 5：误提交分发会话 `.agent-loop` 运行状态。提交前会只 stage `MeScreen.kt` 和本 issue 文件。

### 剩余风险或未完成项

- 未做真机或截图视觉核对；本切片仅通过代码结构和 Android Kotlin 编译验证静态菜单可编译且不接路由。
