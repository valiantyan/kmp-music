Status: ready-for-human

# 移动端统计区改为歌曲、歌单和听歌时长

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

把移动端“我的”页统计区改为三项：歌曲、歌单、听歌时长。歌曲数使用真实本地曲库歌曲数，歌单固定显示 `12`，听歌时长固定显示 `365`，三项都只作为当前版本的信息展示。

## 验收标准

- [x] 统计区只显示“歌曲”“歌单”“听歌时长”三项。
- [x] “歌曲”数来自真实本地曲库歌曲数。
- [x] “歌单”固定显示 `12`，不新增歌单领域模型、数据源或页面。
- [x] “听歌时长”固定显示 `365`，不新增听歌时长统计模型或数据源。
- [x] 歌单数和听歌时长不可点击。

## 依赖

- 02-mobile-me-remove-old-title-login-card.md

## Comments

### 实现摘要

- 移动端 `MeScreen` 统计区改为只展示“歌曲”“歌单”“听歌时长”三项。
- “歌曲”取 `LibraryStats.songCount`，沿用现有真实本地曲库统计来源。
- “歌单”固定展示 `12`，“听歌时长”固定展示 `365`，未新增歌单或听歌时长领域模型、数据源、页面或持久化。
- 移动端根路由移除了 `MeScreen` 不再需要的 `favoriteCount` 入参；未修改 Desktop、底部 Tab、全局迷你播放器或原型目录。

### 验证命令与结果

- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，`BUILD SUCCESSFUL in 14s`。

### Code review 结论

- 已检查改动范围只包含 `MeScreen.kt`、`MobileRootScreenRoute.kt` 和本 issue 文件。
- 统计区仍由 `Surface` 和文本渲染，没有为“歌单”或“听歌时长”添加 `clickable` 或导航回调。
- 搜索确认本切片未新增 `Playlist` 模型、Repository、数据源或听歌时长统计相关实现。
- `.agent-loop/*` 存在分发会话运行态 diff，但本任务未修改、未纳入提交范围。

### 对抗式审查

- 风险 1：误把歌单数接成真实功能。结果：只写死展示 `12`，没有新增模型、仓库、页面或点击行为。
- 风险 2：误把听歌时长做成真实统计。结果：只写死展示 `365`，没有新增统计模型、数据源或持久化。
- 风险 3：歌曲数口径不真实。结果：使用现有 `LibraryStats.songCount`，该字段来自本地曲库统计来源，不用收藏数、专辑数或 demo 常量替代。
- 风险 4：静态统计被做成可点击。结果：统计 `Surface` 没有点击修饰符，`MeScreen` 的 `clickable` 仍只用于收藏专辑预览。
- 风险 5：顺手改动后续 issue 范围。结果：未实现扫描音乐、最近播放、设置菜单、Desktop 改造或原型目录改动。

### 剩余风险或未完成项

- 未做截图级视觉核对；本切片只调整统计区文案和数值来源，视觉布局沿用前置 issue 已建立的统计卡片结构。
