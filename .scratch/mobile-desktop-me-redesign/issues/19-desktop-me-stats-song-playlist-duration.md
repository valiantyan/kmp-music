Status: ready-for-human

# Desktop 统计区改为歌曲、歌单和听歌时长

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

把 Desktop/macOS “我的”页统计区改为与移动端一致的三项：歌曲、歌单、听歌时长。歌曲数使用真实本地曲库歌曲数，歌单固定显示 `12`，听歌时长固定显示 `365`。

## 验收标准

- [x] Desktop/macOS 统计区只显示“歌曲”“歌单”“听歌时长”三项。
- [x] “歌曲”数来自真实本地曲库歌曲数。
- [x] “歌单”固定显示 `12`，不新增歌单领域能力。
- [x] “听歌时长”固定显示 `365`，不新增听歌时长统计能力。
- [x] 三项统计保持桌面端宽屏布局的自然间距。

## 依赖

- 18-desktop-me-new-profile-structure.md

## Comments

### 实现摘要

- Desktop/macOS “我的”页统计区已从旧的“本地专辑 / 歌手 / 收藏 / 最近播放”四项改为“歌曲 / 歌单 / 听歌时长”三项。
- 新增桌面统计展示模型 `DesktopMeStatDisplayModel` 和构造函数 `buildDesktopMeStatDisplayModels`，把真实歌曲数和静态展示值的边界固定在可测试函数里。
- “歌曲”使用 `LibraryStats.songCount`，继续来自现有真实本地曲库统计；“歌单”固定为 `12`，“听歌时长”固定为 `365`。
- 移除了 `DesktopMeRootScreen` 不再需要的 `favoriteCount` 入参，桌面根路由不再把收藏歌曲数传给“我的”页统计区。
- 三项统计仍使用桌面端横向等权卡片和 `18.dp` 间距，没有套用移动端窄屏统计卡；未实现 issue 20-24 的扫描入口、设置菜单、最近播放摘要/列表或动作反馈。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.desktop.screens.DesktopMeScreenTest`：通过，新增测试覆盖桌面统计区只显示三项，并确认歌曲数随 `LibraryStats.songCount` 变化，歌单和听歌时长保持静态值。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，确认 `commonMain` 桌面共享 UI 改动在 Android 目标编译成功。
- `./gradlew :composeApp:desktopTest`：通过，确认完整 Desktop/common 测试没有回归。
- `git diff --check`：通过，没有补丁空白问题。
- 验证过程中仅出现既有 Gradle 弃用属性提示；定向 Desktop 测试首次编译时还出现既有 `MusicAppControllerTest` 的 `No cast needed` 警告，本切片未修改相关代码。

### Code review 结论

- Spec 轴通过。五项验收标准均已满足：桌面统计区只保留“歌曲 / 歌单 / 听歌时长”，歌曲数来自 `LibraryStats.songCount`，另外两项是静态展示值，三项横向等权保留桌面宽屏间距。
- Standards 轴通过。改动集中在 Desktop “我的”页展示模型、桌面根路由参数清理和对应 commonTest；没有新增歌单、听歌时长、Repository、持久化、后台统计或平台 API。
- 范围轴通过。未修改登录页、登录路由、底部 Tab、全局迷你播放器、`prototypes/kmp-music-hi-fi` 或 `.agent-loop/*`；也没有实现后续 issue 20-24 的桌面扫描、设置菜单或最近播放能力。

### 对抗式审查

- 风险一：把“歌曲”写死成展示常量。复核结果：`buildDesktopMeStatDisplayModels` 的第一项直接读取 `libraryStats.songCount.toString()`，新增测试用 `128` 验证会随真实曲库统计变化。
- 风险二：误把旧收藏数、专辑数、歌手数或最近播放数留在统计区。复核结果：新增测试断言标题只能是“歌曲 / 歌单 / 听歌时长”，路由也不再传入 `favoriteCount`。
- 风险三：为了歌单或听歌时长新增真实领域能力。复核结果：本切片只新增静态常量和展示模型，没有新增领域模型、仓库、数据源、持久化或后台统计。
- 风险四：把移动端窄屏统计卡硬套到桌面。复核结果：桌面端仍使用 `DesktopStatCard` 横向等权卡片、`fillMaxWidth()` 和 `18.dp` 卡片间距。
- 风险五：抢做后续 issue。复核结果：没有接入桌面扫描入口、静态设置菜单、最近播放摘要、最近播放页或桌面最近播放动作反馈。

### 剩余风险或未完成项

- 无未完成验收项。
- 剩余视觉风险：本切片未启动 Desktop App 截图核对真实窗口效果；已通过展示模型测试、Desktop/common 测试和 Android 编译确认结构与数据来源正确。
